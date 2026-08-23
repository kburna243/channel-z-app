package com.example.data.model

/**
 * State of the WebQueue Magic Login flow.
 */
sealed interface WebQueueOtpState {
    data object Idle : WebQueueOtpState
    data object RequestingOtp : WebQueueOtpState
    data class WaitingForCode(val username: String) : WebQueueOtpState
    data class Verifying(val username: String, val code: String) : WebQueueOtpState
    data class Success(val username: String) : WebQueueOtpState
    data class Failed(val error: String) : WebQueueOtpState
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
