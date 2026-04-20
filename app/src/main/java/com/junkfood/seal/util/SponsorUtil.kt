package com.junkfood.seal.util

import android.util.Log
import androidx.annotation.CheckResult
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request
import java.util.concurrent.TimeUnit

/**
 * A sponsor as stored in the public sponsors.json file.
 * Tier information can be added to sponsors.json as needed.
 */
@Serializable
data class Sponsor(val id: Int = 0, val name: String = "")

/** Top-level wrapper matching the sponsors.json schema. */
@Serializable
data class SponsorsResponse(val sponsors: List<Sponsor> = emptyList())

// ---------------------------------------------------------------------------
// UI model classes used by SponsorPage to display sponsor cards and dialogs.
// These mirror the shape of the old GitHub Sponsors GraphQL response so the
// existing composable layer does not need changes.
// ---------------------------------------------------------------------------

data class SocialAccount(val displayName: String, val url: String)

data class SocialAccounts(val nodes: List<SocialAccount>)

data class SponsorEntity(
    val login: String,
    val name: String? = null,
    val websiteUrl: String? = null,
    val socialAccounts: SocialAccounts? = null,
)

data class Tier(val monthlyPriceInDollars: Int)

data class SponsorShip(val sponsorEntity: SponsorEntity, val tier: Tier? = null)

object SponsorUtil {
    private const val TAG = "SponsorUtil"

    /**
     * Public, unauthenticated URL for sponsor data.
     * The file is maintained in the repository and updated manually or via CI.
     * No API token or secret is required to read it.
     */
    private const val SPONSORS_URL =
        "https://raw.githubusercontent.com/MaheshTechnicals/Sealplus/main/sponsors.json"

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .apply { ProxyManager.getActiveProxy()?.let { proxy(it) } }
        .build()

    private fun getClient(): OkHttpClient = httpClient

    private val jsonFormat = Json { ignoreUnknownKeys = true }
    @Volatile
    private var cachedResponse: SponsorsResponse? = null

    @CheckResult
    fun getSponsors(): Result<SponsorsResponse> = runCatching {
        cachedResponse ?: synchronized(this) {
            // Double-checked locking: recheck under lock after acquiring it
            cachedResponse ?: run {
                val request = Request.Builder().url(SPONSORS_URL).get().build()
                val body = getClient().newCall(request).execute().use { response ->
                    response.body?.string() ?: error("Empty response body from sponsors endpoint")
                }
                Log.d(TAG, "Sponsors fetched successfully")
                jsonFormat.decodeFromString<SponsorsResponse>(body).also { cachedResponse = it }
            }
        }
    }.onFailure { it.printStackTrace() }
}
