package com.example.trainerengine.configs

import com.example.trainerengine.database.Database

class ModuleConfig(
    private val configID: Int,
    private val moduleID: Int,
    private val database: Database,
    private var name: String = "Config $configID",
    private var configData: MutableList<ConfigData> = mutableListOf()
) {
    fun getConfigID(): Int {
        return configID
    }

    fun getModuleID(): Int {
        return moduleID
    }

    fun getName(): String {
        return name
    }

    fun setName(name: String) {
        this.name = name
        database.updateConfig(this)
    }

    fun getConfigData(): MutableList<ConfigData> {
        return configData
    }

    fun getConfigData(name: String): ConfigData? {
        for (data in configData) {
            if (data.getName() == name) {
                return data
            }
        }
        return null
    }

    fun addConfigData(configData: ConfigData) {
        this.configData.add(configData)
        database.updateConfig(this)
    }

    fun setConfigData(configData: MutableList<ConfigData>) {
        this.configData = configData
        database.updateConfig(this)
    }
}

class ConfigData(
    private val configDataID: Int,
    private val configID: Int,
    private val name: String,
    private val type: String,
    private val value: Any
) {
    fun getConfigDataID(): Int {
        return configDataID
    }

    fun getConfigID(): Int {
        return configID
    }

    fun getName(): String {
        return name
    }

    fun getType(): String {
        return type
    }

    fun getValue(): Any {
        return value
    }
}