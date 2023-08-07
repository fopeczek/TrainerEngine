package com.example.trainerengine

import android.icu.text.DateFormat
import com.example.trainerengine.module.Module
import java.util.*

val globalModules = mutableMapOf<Int, Module>() // ModuleID -> Module

fun getModule(moduleName: String): Module {
    for (module in globalModules.values) {
        if (module.getStub().descriptionName == moduleName) {
            return module
        }
    }
    throw Exception("Module $moduleName not found")
}

fun getTimestamp(): String {
    val dateFormat = DateFormat.getDateTimeInstance()
    return dateFormat.format(Date())
}
