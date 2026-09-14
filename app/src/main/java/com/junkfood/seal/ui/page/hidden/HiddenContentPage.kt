package com.junkfood.seal.ui.page.hidden

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.ModalBottomSheetValue
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Visibility
import androidx.compose.material.rememberModalBottomSheetState
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.LargeTopAppBar
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.material3.rememberTopAppBarState
import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.input.nestedscroll.nestedScroll
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.koin.androidx.compose.koinViewModel
import com.junkfood.seal.App
import com.junkfood.seal.R
import com.junkfood.seal.database.objects.DownloadedVideoInfo
import com.junkfood.seal.ui.common.HapticFeedback.slightHapticFeedback
import com.junkfood.seal.ui.component.BackButton
import com.junkfood.seal.ui.component.ConfirmButton
import com.junkfood.seal.ui.component.DismissButton
import com.junkfood.seal.ui.component.MediaListItem
import com.junkfood.seal.ui.component.SealDialog
import com.junkfood.seal.ui.component.SealModalBottomSheetM2
import com.junkfood.seal.ui.svg.DynamicColorImageVectors
import com.junkfood.seal.ui.svg.drawablevectors.videoSteaming
import com.junkfood.seal.util.DatabaseUtil
import com.junkfood.seal.util.FileUtil
import com.junkfood.seal.util.makeToast
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HiddenContentPage(
    viewModel: HiddenContentViewModel = koinViewModel(),
    onNavigateBack: () -> Unit,
) {
    val hiddenList by viewModel.hiddenVideoListFlow.collectAsStateWithLifecycle(emptyList())
    val fileSizeMap by viewModel.fileSizeMapFlow.collectAsStateWithLifecycle(emptyMap())

    val scrollBehavior =
        TopAppBarDefaults.exitUntilCollapsedScrollBehavior(rememberTopAppBarState())

    val scope = rememberCoroutineScope()
    val view = LocalView.current
    val hostState = remember { SnackbarHostState() }

    val sheetState = rememberModalBottomSheetState(
        initialValue = ModalBottomSheetValue.Hidden,
        skipHalfExpanded = true,
    )
    var showBottomSheet by remember { mutableStateOf(false) }
    var showRemoveDialog by remember { mutableStateOf(false) }
    var currentInfo by remember { mutableStateOf(DownloadedVideoInfo()) }

    BackHandler(sheetState.targetValue == ModalBottomSheetValue.Expanded) {
        scope.launch { sheetState.hide(); showBottomSheet = false }
    }

    Scaffold(
        modifier = Modifier.nestedScroll(scrollBehavior.nestedScrollConnection),
        topBar = {
            LargeTopAppBar(
                title = { Text(stringResource(R.string.hidden_content)) },
                navigationIcon = { BackButton { onNavigateBack() } },
                scrollBehavior = scrollBehavior,
            )
        },
        snackbarHost = { SnackbarHost(hostState = hostState) },
    ) { innerPadding ->
        if (hiddenList.isEmpty()) {
            Box(modifier = Modifier.fillMaxSize()) {
                val painter = rememberVectorPainter(image = DynamicColorImageVectors.videoSteaming())
                Column(
                    modifier = Modifier
                        .align(Alignment.Center)
                        .widthIn(max = 360.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Image(
                        painter = painter,
                        contentDescription = null,
                        modifier = Modifier
                            .padding(vertical = 20.dp)
                            .fillMaxWidth(0.5f)
                            .widthIn(max = 240.dp),
                    )
                    Text(
                        text = stringResource(R.string.no_hidden_content),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        } else {
            LazyColumn(contentPadding = innerPadding) {
                for (info in hiddenList) {
                    item(key = info.id) {
                        MediaListItem(
                            title = info.videoTitle,
                            author = info.videoAuthor,
                            thumbnailUrl = info.thumbnailUrl,
                            videoPath = info.videoPath,
                            videoUrl = info.videoUrl,
                            videoFileSize = fileSizeMap.getOrElse(info.id) { 0L },
                            onClick = {
                                FileUtil.openFile(path = info.videoPath) {
                                    App.applicationScope.launch(Dispatchers.Main) {
                                        App.context.makeToast(R.string.file_unavailable)
                                    }
                                }
                            },
                            onShowContextMenu = {
                                view.slightHapticFeedback()
                                currentInfo = info
                                scope.launch {
                                    showBottomSheet = true
                                    delay(50)
                                    sheetState.show()
                                }
                            },
                        )
                    }
                }
            }
        }
    }

    if (showBottomSheet) {
        SealModalBottomSheetM2(
            sheetState = sheetState,
            contentPadding = PaddingValues(horizontal = 0.dp),
            sheetContent = {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp)
                        .padding(bottom = 24.dp),
                ) {
                    Text(
                        text = currentInfo.videoTitle,
                        style = MaterialTheme.typography.titleMedium,
                        maxLines = 2,
                        modifier = Modifier.padding(top = 8.dp, bottom = 16.dp),
                    )
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        OutlinedButton(
                            onClick = {
                                scope.launch {
                                    sheetState.hide()
                                    showBottomSheet = false
                                    showRemoveDialog = true
                                }
                            },
                            modifier = Modifier
                                .height(52.dp)
                                .weight(1f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.outlinedButtonColors(
                                contentColor = MaterialTheme.colorScheme.error,
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Delete,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = stringResource(R.string.remove),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                        Spacer(modifier = Modifier.width(12.dp))
                        Button(
                            onClick = {
                                val info = currentInfo
                                scope.launch {
                                    sheetState.hide()
                                    showBottomSheet = false
                                    viewModel.unhideItem(info)
                                }
                            },
                            modifier = Modifier
                                .height(52.dp)
                                .weight(2f),
                            shape = RoundedCornerShape(14.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary,
                            ),
                            contentPadding = PaddingValues(horizontal = 12.dp),
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Visibility,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp),
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = stringResource(R.string.unhide),
                                style = MaterialTheme.typography.labelLarge,
                            )
                        }
                    }
                }
            },
        )
    }

    if (showRemoveDialog) {
        SealDialog(
            onDismissRequest = { showRemoveDialog = false },
            icon = {
                Icon(
                    Icons.Outlined.Delete,
                    null,
                    tint = MaterialTheme.colorScheme.error,
                )
            },
            title = { Text(stringResource(R.string.delete_info)) },
            text = {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 24.dp),
                    text = stringResource(R.string.delete_info_msg).format(currentInfo.videoTitle),
                )
            },
            confirmButton = {
                ConfirmButton {
                    scope.launch(Dispatchers.IO) {
                        DatabaseUtil.deleteInfoList(listOf(currentInfo), deleteFile = true)
                    }
                    showRemoveDialog = false
                }
            },
            dismissButton = { DismissButton { showRemoveDialog = false } },
        )
    }
}
