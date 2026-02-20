package com.vigipro.core.data.extensions

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

/**
 * Formats an ISO timestamp string to "dd/MM/yyyy HH:mm" in local timezone.
 */
fun String.toLocalDateTime(): String =
    try {
        val instant = Instant.parse(this)
        val formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
            .withZone(ZoneId.systemDefault())
        formatter.format(instant)
    } catch (_: Exception) {
        this
    }

/**
 * Formats an ISO timestamp to relative time: "agora", "5 min", "2h", "3 dias"
 */
fun String.toRelativeTime(): String =
    try {
        val instant = Instant.parse(this)
        val now = Instant.now()
        val seconds = now.epochSecond - instant.epochSecond

        when {
            seconds < 60 -> "agora"
            seconds < 3600 -> "${seconds / 60} min"
            seconds < 86400 -> "${seconds / 3600}h"
            seconds < 2592000 -> "${seconds / 86400} dias"
            else -> toLocalDateTime()
        }
    } catch (_: Exception) {
        this
    }

/**
 * Validates basic RTSP URL format.
 */
fun String.isValidRtspUrl(): Boolean =
    matches(Regex("""^rtsp://[^\s]+$""", RegexOption.IGNORE_CASE))

/**
 * Validates basic email format.
 */
fun String.isValidEmail(): Boolean =
    matches(Regex("""^[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\.[A-Za-z]{2,}$"""))
