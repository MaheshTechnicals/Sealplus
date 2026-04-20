package com.junkfood.seal.util

import android.media.MediaScannerConnection
import android.provider.MediaStore
import androidx.room.Room
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.junkfood.seal.App.Companion.applicationScope
import com.junkfood.seal.App.Companion.context
import com.junkfood.seal.database.AppDatabase
import com.junkfood.seal.database.backup.Backup
import com.junkfood.seal.database.backup.BackupUtil.BackupType
import com.junkfood.seal.database.backup.BackupUtil.decodeToBackup
import com.junkfood.seal.database.objects.CommandTemplate
import com.junkfood.seal.database.objects.CookieProfile
import com.junkfood.seal.database.objects.DownloadedVideoInfo
import com.junkfood.seal.database.objects.OptionShortcut
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

private val MIGRATION_5_6 = object : Migration(5, 6) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE DownloadedVideoInfo ADD COLUMN downloadTimeMillis INTEGER NOT NULL DEFAULT -1"
        )
        database.execSQL(
            "ALTER TABLE DownloadedVideoInfo ADD COLUMN averageSpeedBytesPerSec INTEGER NOT NULL DEFAULT -1"
        )
    }
}

private val MIGRATION_6_7 = object : Migration(6, 7) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            "ALTER TABLE DownloadedVideoInfo ADD COLUMN isHidden INTEGER NOT NULL DEFAULT 0"
        )
    }
}

object DatabaseUtil {
    private const val DATABASE_NAME = "app_database"
    private val db =
        Room.databaseBuilder(context, AppDatabase::class.java, DATABASE_NAME)
            .addMigrations(MIGRATION_5_6, MIGRATION_6_7)
            .build()
    private val dao = db.videoInfoDao()

    fun insertInfo(vararg infoList: DownloadedVideoInfo) {
        applicationScope.launch(Dispatchers.IO) {
            infoList.forEach { dao.insertInfoDistinctByPath(it) }
        }
    }

    init {
        applicationScope.launch {
            getTemplateFlow().collect {
                if (it.isEmpty()) PreferenceUtil.initializeTemplateSample()
            }
        }
    }

    fun getDownloadHistoryFlow() = dao.getDownloadHistoryFlow()

    fun getVisibleDownloadHistoryFlow() = dao.getVisibleDownloadHistoryFlow()

    fun getHiddenDownloadHistoryFlow() = dao.getHiddenDownloadHistoryFlow()

    suspend fun hideItem(info: DownloadedVideoInfo) {
        val sourceFile = File(info.videoPath)
        if (sourceFile.exists()) {
            // Move the file into the hidden private folder
            val hiddenDir = FileUtil.getHiddenPrivateDirectory()
            val destFile = File(hiddenDir, sourceFile.name).let { candidate ->
                // Avoid name collisions
                if (!candidate.exists()) candidate
                else File(hiddenDir, "${info.id}_${sourceFile.name}")
            }
            // Prefer atomic rename (same filesystem); fall back to copy+delete across volumes
            val moved = sourceFile.renameTo(destFile)
            if (!moved) {
                sourceFile.copyTo(destFile, overwrite = true)
                sourceFile.delete()
            }
            // Remove old path from MediaStore (index-only; file is already gone)
            removeFromMediaStore(info.videoPath)
            // Update DB: mark hidden + save new path so we can move it back later
            dao.setHiddenAndPath(info.id, true, destFile.absolutePath)
        } else {
            // File doesn't exist on disk — just mark hidden in DB
            dao.setHidden(info.id, true)
        }
    }

    suspend fun unhideItem(info: DownloadedVideoInfo) {
        val hiddenFile = File(info.videoPath)
        if (hiddenFile.exists()) {
            // Restore to the original SealPlus download directory
            val downloadDir = FileUtil.getExternalDownloadDirectory()
            val destFile = File(downloadDir, hiddenFile.name).let { candidate ->
                if (!candidate.exists()) candidate
                else File(downloadDir, "${info.id}_${hiddenFile.name}")
            }
            // Prefer atomic rename; fall back to copy+delete
            val moved = hiddenFile.renameTo(destFile)
            if (!moved) {
                hiddenFile.copyTo(destFile, overwrite = true)
                hiddenFile.delete()
            }
            MediaScannerConnection.scanFile(context, arrayOf(destFile.absolutePath), null, null)
            // Update DB: mark visible + save restored path
            dao.setHiddenAndPath(info.id, false, destFile.absolutePath)
        } else {
            dao.setHidden(info.id, false)
        }
    }

    private fun removeFromMediaStore(filePath: String) {
        val selection = "${MediaStore.MediaColumns.DATA} = ?"
        val args = arrayOf(filePath)
        listOf(
            MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Audio.Media.EXTERNAL_CONTENT_URI,
            MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
        ).forEach { uri ->
            runCatching { context.contentResolver.delete(uri, selection, args) }
        }
    }

    private suspend fun getDownloadHistory() = dao.getDownloadHistory()

    fun getTemplateFlow() = dao.getTemplateFlow()

    fun getCookiesFlow() = dao.getCookieProfileFlow()

    fun getShortcuts() = dao.getOptionShortcuts()

    suspend fun deleteShortcut(shortcut: OptionShortcut) = dao.deleteShortcut(shortcut)

    suspend fun insertShortcut(shortcut: OptionShortcut) = dao.insertShortcut(shortcut)

    suspend fun getCookieById(id: Int) = dao.getCookieById(id)

    suspend fun deleteCookieProfile(profile: CookieProfile) = dao.deleteCookieProfile(profile)

    suspend fun insertCookieProfile(profile: CookieProfile) = dao.insertCookieProfile(profile)

    suspend fun updateCookieProfile(profile: CookieProfile) = dao.updateCookieProfile(profile)

    suspend fun getTemplateList() = dao.getTemplateList()

    suspend fun getShortcutList() = dao.getShortcutList()

    suspend fun deleteInfoList(infoList: List<DownloadedVideoInfo>, deleteFile: Boolean = false) =
        withContext(Dispatchers.IO) {
            dao.deleteInfoList(infoList)
            infoList.forEach { info -> if (deleteFile) FileUtil.deleteFile(info.videoPath) }
        }

    suspend fun getInfoById(id: Int): DownloadedVideoInfo = dao.getInfoById(id)

    suspend fun deleteInfoById(id: Int) = dao.deleteInfoById(id)

    suspend fun insertTemplate(commandTemplate: CommandTemplate) =
        dao.insertTemplate(commandTemplate)

    suspend fun updateTemplate(commandTemplate: CommandTemplate) {
        dao.updateTemplate(commandTemplate)
    }

    suspend fun importBackup(backup: Backup, types: Set<BackupType>): Int {
        var cnt = 0
        backup.run {
            if (types.contains(BackupType.DownloadHistory)) {
                val itemList = getDownloadHistory()

                if (!downloadHistory.isNullOrEmpty()) {
                    dao.insertAll(
                        downloadHistory
                            .filterNot { itemList.contains(it) }
                            .map { it.copy(id = 0) }
                            .also { cnt += it.size }
                    )
                }
            }
            if (types.contains(BackupType.CommandTemplate)) {
                if (templates != null) {
                    val templateList = getTemplateList()
                    dao.importTemplates(
                        templates
                            .filterNot { templateList.contains(it) }
                            .map { it.copy(id = 0) }
                            .also { cnt += it.size }
                    )
                }
            }
            if (types.contains(BackupType.CommandShortcut)) {
                val shortcutList = getShortcutList()
                if (shortcuts != null) {
                    dao.insertAllShortcuts(
                        shortcuts
                            .filterNot { shortcutList.contains(it) }
                            .map { it.copy(id = 0) }
                            .also { cnt += it.size }
                    )
                }
            }
        }
        return cnt
    }

    suspend fun importTemplatesFromJson(json: String): Int {
        json
            .decodeToBackup()
            .onSuccess { backup ->
                return importBackup(
                    backup = backup,
                    types = setOf(BackupType.CommandTemplate, BackupType.CommandShortcut),
                )
            }
            .onFailure { it.printStackTrace() }
        return 0
    }

    suspend fun deleteTemplateById(id: Int) = dao.deleteTemplateById(id)

    suspend fun deleteTemplates(templates: List<CommandTemplate>) = dao.deleteTemplates(templates)

    private const val TAG = "DatabaseUtil"
}
