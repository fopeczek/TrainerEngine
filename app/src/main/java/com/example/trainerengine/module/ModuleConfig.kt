package com.example.trainerengine.module

class ModuleConfig(
    private val configID: Int,
    private val moduleID: Int,
    private val name: String,
    private val configData: MutableList<ConfigData>
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
    }
}

class ConfigData(
    private val configID: Int,
    private val name: String,
    private val type: String,
    private val value: Any
) {
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