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

    fun formatProjectDate(dateString: String?): String {
        if (dateString.isNullOrBlank()) return ""

        return try {
            // 1. Parse the full ISO string (e.g., "2026-01-21T20:30:00Z")
            val instant = Instant.parse(dateString)

            // 2. Convert to User's Local Timezone (e.g., "2026-01-22...")
            val localDate = instant.atZone(ZoneId.systemDefault()).toLocalDate()

            // 3. Format correctly
            localDate.format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
        } catch (e: Exception) {
            // If it's just a plain date string like "2026-01-22"
            try {
                LocalDate.parse(dateString.take(10))
                    .format(DateTimeFormatter.ofPattern("dd MMM yyyy"))
            } catch (e2: Exception) {
                ""
            }
        }
    }

}
