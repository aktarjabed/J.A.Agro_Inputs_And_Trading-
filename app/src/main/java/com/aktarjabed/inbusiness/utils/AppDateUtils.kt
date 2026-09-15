package com.aktarjabed.inbusiness.utils

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.ZonedDateTime

object AppDateUtils {
    // Authoritative business timezone
    val businessZoneId: ZoneId = ZoneId.of("Asia/Kolkata")

    fun getTodayStart(): Long {
        return LocalDate.now(businessZoneId).atStartOfDay(businessZoneId).toInstant().toEpochMilli()
    }

    fun getTomorrowStart(): Long {
        return LocalDate.now(businessZoneId).plusDays(1).atStartOfDay(businessZoneId).toInstant().toEpochMilli()
    }

    fun getStartOfDay(date: LocalDate): Long {
        return date.atStartOfDay(businessZoneId).toInstant().toEpochMilli()
    }

    fun getStartOfNextDay(date: LocalDate): Long {
        return date.plusDays(1).atStartOfDay(businessZoneId).toInstant().toEpochMilli()
    }

    // Used for 7-day chart calculation
    fun getPastSevenDays(): List<LocalDate> {
        val today = LocalDate.now(businessZoneId)
        return (6 downTo 0).map { today.minusDays(it.toLong()) }
    }
}
