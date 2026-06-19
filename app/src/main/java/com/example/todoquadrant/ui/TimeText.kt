package com.example.todoquadrant.ui

import java.time.Instant
import java.time.LocalDate
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale

object TimeText {
    private val formatter: DateTimeFormatter =
        DateTimeFormatter.ofPattern("M月d日 HH:mm", Locale.CHINA)

    fun format(millis: Long): String =
        Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .format(formatter)

    fun isToday(millis: Long): Boolean {
        val date = Instant.ofEpochMilli(millis)
            .atZone(ZoneId.systemDefault())
            .toLocalDate()
        return date == LocalDate.now()
    }
}
