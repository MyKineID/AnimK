package com.animk.app.data.remoteconfig

import android.content.Context
import android.util.Log
import com.animk.app.data.network.OkHttpClientBuilder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.json.Json
import okhttp3.Request

/**
 * Singleton manager for remote Director config.
 * Fetches provider configuration from VPS, caches locally, and provides
 * fallback when network is unavailable.
 */
object DirectorConfigProvider {
    private const val PREF_NAME = "animk_director_config"
    private const val KEY_CONFIG_JSON = "config_json"
    private const val DIRECTOR_URL = "http://209.17.118.146:3050/api/v1/config"

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    private var prefs: android.content.SharedPreferences? = null
    private var memoryConfig: DirectorConfig? = null
    private val configMutex = Mutex()
    private var currentSource: String = "none"

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    /**
     * Returns the active DirectorConfig.
     *
     * A saved config is used immediately so first paint never waits for the VPS.
     * A forced refresh (started by MainActivity) updates it in the background.
     */
    suspend fun getConfig(forceRefresh: Boolean = false): DirectorConfig {
        if (!forceRefresh) {
            memoryConfig?.let { return it }
            getCachedConfig()?.let { cached ->
                memoryConfig = cached
                currentSource = "cache"
                logSource()
                return cached
            }
        }

        return configMutex.withLock {
            if (!forceRefresh) {
                memoryConfig?.let { return@withLock it }
                getCachedConfig()?.let { cached ->
                    memoryConfig = cached
                    currentSource = "cache"
                    logSource()
                    return@withLock cached
                }
            }

            try {
                val config = fetchFromNetwork()
                cacheConfig(config)
                memoryConfig = config
                currentSource = "network"
                logSource()
                config
            } catch (e: Exception) {
                getCachedConfig()?.let { cached ->
                    memoryConfig = cached
                    currentSource = "cache"
                    logSource()
                    return@withLock cached
                }
                getFallbackConfig().also { fallback ->
                    memoryConfig = fallback
                    currentSource = "fallback"
                    logSource()
                }
            }
        }
    }

    /**
     * Returns active providers sorted by priority (ascending).
     * Only providers with `active == true` are included.
     */
    suspend fun getActiveProviders(): List<Pair<String, ProviderConfig>> = withContext(Dispatchers.Default) {
        val config = getConfig()
        config.providers
            .filter { (_, v) -> v.active && v.domain.isNotBlank() }
            .entries
            .sortedBy { it.value.priority }
            .map { it.key to it.value }
    }

    val configSource: String get() = currentSource

    private suspend fun fetchFromNetwork(): DirectorConfig = withContext(Dispatchers.IO) {
        val client = OkHttpClientBuilder.buildUnsafeClient()
        val request = Request.Builder()
            .url(DIRECTOR_URL)
            .header("User-Agent", "AnimK/1.0")
            .build()
        val response = client.newCall(request).execute()
        val body = response.body?.string() ?: throw Exception("Empty response from Director")
        json.decodeFromString<DirectorConfig>(body)
    }

    private fun cacheConfig(config: DirectorConfig) {
        try {
            val jsonStr = json.encodeToString(DirectorConfig.serializer(), config)
            prefs?.edit()?.putString(KEY_CONFIG_JSON, jsonStr)?.apply()
        } catch (e: Exception) {
            Log.w("DirectorConfig", "Failed to cache config", e)
        }
    }

    private fun getCachedConfig(): DirectorConfig? {
        return try {
            val jsonStr = prefs?.getString(KEY_CONFIG_JSON, null) ?: return null
            json.decodeFromString<DirectorConfig>(jsonStr)
        } catch (e: Exception) {
            null
        }
    }

    private fun getFallbackConfig(): DirectorConfig {
        return DirectorConfig(
            providers = mapOf(
                "otakudesu" to ProviderConfig(
                    active = true,
                    priority = 1,
                    domain = "https://otakudesu.blog",
                    searchPath = "/?s=",
                    selectors = ProviderSelectors(
                        list = ".venz ul li",
                        title = "h2",
                        link = "a",
                        image = "img"
                    )
                )
            )
        )
    }

    private fun logSource() {
        Log.i("DirectorConfig", "source=$currentSource, version=${memoryConfig?.version ?: "?"}, " +
                "providers=${memoryConfig?.providers?.size ?: 0}")
    }
}
