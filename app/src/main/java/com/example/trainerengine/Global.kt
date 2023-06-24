package com.example.trainerengine

import android.icu.text.DateFormat
import com.example.trainerengine.module.Module
import java.util.*

val modules = mutableListOf<Module>()

fun getTimestamp(): String {
    val dateFormat = DateFormat.getDateTimeInstance()
    return dateFormat.format(Date())
}