package com.example.linkit.util

import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TimeUtils {

    private val timeFormatter =
        DateTimeFormatter.ofPattern("hh:mm a")

    private val dateFormatter =
        DateTimeFormatter.ofPattern("dd MMM yyyy")

    fun safeInstant(utc: String?): Instant {
        return try {
            if (utc.isNullOrBlank()) Instant.now()
            else Instant.parse(utc)
        } catch (e: Exception) {
            Instant.now()
        }
    }

    fun formatTime(utc: String?): String {
        return safeInstant(utc)
            .atZone(ZoneId.systemDefault())
            .format(timeFormatter)
    }

    fun formatDateHeader(utc: String?): String {
        return safeInstant(utc)
            .atZone(ZoneId.systemDefault())
            .format(dateFormatter)
    }

    fun getCurrentIsoTime(): String {
        return Instant.now().toString()
    }

}
