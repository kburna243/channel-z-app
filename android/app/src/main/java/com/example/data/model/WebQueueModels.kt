package com.example.data.model

/**
 * State of the WebQueue Magic Login flow.
 */
sealed interface WebQueueLinkState {
    data object Idle : WebQueueLinkState
    data object RequestingLink : WebQueueLinkState
    data class Verifying(val code: String) : WebQueueLinkState
    data class Success(val username: String) : WebQueueLinkState
    data class Failed(val error: String) : WebQueueLinkState
}

/**
 * Incoming Private Message from CyTube chat.
 */
data class PrivateMessage(
    val from: String,
    val text: String,
    val timestamp: Long = System.currentTimeMillis(),
    val to: String = ""
)

/**
 * Catalog item metadata from WebQueue / MediaCMS database.
 */
data class CatalogItem(
    val friendlyToken: String,
    val title: String,
    val description: String? = null,
    val durationSec: Int = 0,
    val imdbTt: String? = null,
    val contentType: String? = null,
    val lookupYear: Int? = null,
    val category: String? = null,
    val posterUrl: String? = null
)
