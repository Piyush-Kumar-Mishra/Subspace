package com.example.linkit.util

import java.time.Instant
import java.time.LocalDate
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

    fun formatProjectDate(date: String?): String {
        return try {
            LocalDate.parse(date?.take(10))
                .format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        } catch (e: Exception) {
            ""
        }
    }

}
