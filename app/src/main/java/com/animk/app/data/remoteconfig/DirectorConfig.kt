package com.animk.app.data.remoteconfig

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class DirectorConfig(
    @SerialName("app_name") val appName: String = "AnimK",
    val version: String = "0.0.0-beta",
    @SerialName("force_update") val forceUpdate: Boolean = false,
    @SerialName("update_url") val updateUrl: String? = null,
    val maintenance: Boolean = false,
    @SerialName("maintenance_msg") val maintenanceMessage: String? = null,
    val providers: Map<String, ProviderConfig> = emptyMap()
)

@Serializable
data class ProviderConfig(
    val active: Boolean = false,
    val priority: Int = Int.MAX_VALUE,
    val domain: String = "",
    @SerialName("search_path") val searchPath: String = "",
    val selectors: ProviderSelectors = ProviderSelectors()
)

@Serializable
data class ProviderSelectors(
    /** Selector for search result list container */
    val list: String = "",
    /** Selector for title element inside a search result item */
    val title: String = "",
    /** Selector for link element inside a search result item */
    val link: String = "a",
    /** Selector for image element inside a search result item */
    val image: String = "img",
    /** Selector for episode list container */
    @SerialName("episode_list") val episodeList: String = "",
    /** Selector for episode link elements */
    @SerialName("episode_link") val episodeLink: String = "",
    /** Selector for iframe stream elements */
    @SerialName("stream_iframe") val streamIframe: String = "",
    /** Selector for video source elements */
    @SerialName("stream_video") val streamVideo: String = ""
)
