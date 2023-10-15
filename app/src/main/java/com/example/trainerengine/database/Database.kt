package com.example.trainerengine.database

import com.example.trainerengine.ColumnType
import com.example.trainerengine.sessions.Session
import com.example.trainerengine.getTimestamp
import com.example.trainerengine.globalModules
import com.example.trainerengine.configs.ConfigData
import com.example.trainerengine.modules.Module
import com.example.trainerengine.configs.ModuleConfig
import com.example.trainerengine.modules.ModuleTask
import com.example.trainerengine.modules.MathModule.MathModuleStub
import com.example.trainerengine.modules.PercentModule.PercentModuleStub
import com.example.trainerengine.modules.PythonMathModule.PythonMathModuleStub

class Database(private val queryHelper: QueryHelper) {
    companion object {
        const val taskID = "TaskID"
        const val attemptID = "AttemptID"
        const val answerID = "AnswerID"
        const val sessionID = "SessionID"
        const val moduleID = "ModuleID"
        const val configID = "ConfigID"
        const val answer = "Answer"
        const val userAnswer = "UserAnswer"
        const val question = "Question"
        const val judgment = "Judgement"
        const val timestamp = "Timestamp"
        const val points = "Points"
        const val configIDs = "ConfigIDs"
        const val repeatable = "Repeatable"
        const val reset = "Reset"
        const val penaltyPoints = "PenaltyPoints"
        const val targetPoints = "TargetPoints"
        const val sessionName = "SessionName"
        const val moduleName = "ModuleName"
        const val configName = "ConfigName"
        const val configType = "ConfigType"
        const val configValue = "ConfigValue"

        const val sessionsTable = "Sessions"
        const val modulesTable = "Modules"
        const val configsTable = "Configs"
        const val configDataTable = "ConfigData"
        const val taskTable = "Tasks"
        const val attemptsTable = "Attempts"
        const val answersTable = "Answers"
    }

    init {
        initializeSessionsTable()
        initializeModulesTable()
        initializeTasksTable()
        initializeAttemptsTable()
        initializeAnswersTable()
        initializeConfigsTable()
        initializeConfigsDataTable()
    }

    private fun initializeSessionsTable() {
        val columns = mutableMapOf(
            sessionID to ColumnType.INT,
            sessionName to ColumnType.TEXT,
            configIDs to ColumnType.TEXT,
            points to ColumnType.INT,
            reset to ColumnType.BOOL,
            penaltyPoints to ColumnType.INT,
            targetPoints to ColumnType.INT,
            repeatable to ColumnType.BOOL,
            timestamp to ColumnType.TIMESTAMP
        )
        queryHelper.initializeTable(sessionsTable, columns)
    }

    fun saveSession(session: Session) {
        val data = mapOf(
            sessionID to session.getSessionID(),
            sessionName to session.getName(),
            configIDs to session.getConfigIDs().joinToString(","),
            points to session.getPoints(),
            reset to session.getReset(),
            penaltyPoints to session.getPointPenalty(),
            targetPoints to session.getTargetPoints(),
            repeatable to session.getRepeatable(),
            timestamp to getTimestamp()
        )
        queryHelper.insertRow(sessionsTable, data)
    }

    fun loadSession(sessionID: Int): Session {
        val data = queryHelper.getRow(sessionsTable, Companion.sessionID, sessionID)
        return fillSessionFromMap(data)
    }

    fun loadSessions(): List<Session> {
        val data = queryHelper.getAll(sessionsTable, true)
        val sessions = mutableListOf<Session>()
        for (row in data) {
            sessions.add(fillSessionFromMap(row))
        }
        return sessions
    }

    fun updateSession(session: Session) {
        val data = mapOf(
            sessionName to session.getName(),
            configIDs to session.getConfigIDs().joinToString(","),
            points to session.getPoints(),
            reset to session.getReset(),
            penaltyPoints to session.getPointPenalty(),
            targetPoints to session.getTargetPoints(),
            repeatable to session.getRepeatable()
        )
        queryHelper.updateRow(sessionsTable, sessionID, session.getSessionID(), data)
    }

    fun removeSession(sessionID: Int) {
        queryHelper.removeRow(sessionsTable, Companion.sessionID, sessionID)
        val tasks = loadTasks(sessionID)
        for (task in tasks) {
            removeTask(task.getTaskID())
        }
    }

    fun makeNewSessionID(): Int {
        return queryHelper.makeNewID(sessionsTable, sessionID)
    }

    private fun fillSessionFromMap(data: Map<String, Any>): Session {
        val sessionID = data[sessionID].toString().toInt()
        val sessionName = data[sessionName].toString()
        val configIDs = data[configIDs].toString().split(",").map { it.toInt() }.toMutableList()
        val points = data[points].toString().toInt()
        val targetPoints = data[targetPoints].toString().toInt()
        val reset = data[reset].toString().toBoolean()
        val pointPenalty = data[penaltyPoints] as Int
        val repeatable = data[repeatable].toString().toBoolean()
        val answeredTaskAmount = loadAmountOfAttemptedTasks(sessionID)
        val tasks = loadTasks(sessionID)
        return Session(
            sessionID, this, configIDs, tasks, sessionName, repeatable, reset, pointPenalty, targetPoints, answeredTaskAmount, points
        )
    }

    private fun initializeModulesTable() {
        val columns = mutableMapOf(
            moduleID to ColumnType.INT, moduleName to ColumnType.TEXT, timestamp to ColumnType.TIMESTAMP
        )
        queryHelper.initializeTable(modulesTable, columns)
    }

    fun saveModule(module: Module) {
        val data = mapOf(
            moduleID to module.getModuleID(), moduleName to module.getStub().databaseName, timestamp to getTimestamp()
        )
        queryHelper.insertRow(modulesTable, data)
    }

    fun loadModules(): List<Module> {
        val data = queryHelper.getAll(modulesTable, true)
        val modules = mutableListOf<Module>()
        for (row in data) {
            val moduleID = row[moduleID] as Int
            val moduleName = row[moduleName] as String
            when (moduleName) {
                MathModuleStub().databaseName -> {
                    modules.add(MathModuleStub().createModule(moduleID))
                }

                PercentModuleStub().databaseName -> {
                    modules.add(PercentModuleStub().createModule(moduleID))
                }

                PythonMathModuleStub().databaseName -> {
                    modules.add(PythonMathModuleStub().createModule(moduleID))
                }
            }
        }
        return modules
    }

    fun makeNewModuleID(): Int {
        return queryHelper.makeNewID(modulesTable, moduleID)
    }

    private fun initializeConfigsTable() {
        val columns = mutableMapOf(
            configID to ColumnType.INT, configName to ColumnType.TEXT, moduleID to ColumnType.INT
        )
        queryHelper.initializeTable(configsTable, columns)
    }

    fun saveConfig(config: ModuleConfig) {
        val data = mapOf(
            configID to config.getConfigID(), configName to config.getName(), moduleID to config.getModuleID()
        )
        queryHelper.insertRow(configsTable, data)
    }

    fun loadConfig(configID: Int): ModuleConfig {
        val data = queryHelper.getRow(configsTable, Companion.configID, configID)
        val configName = data[configName] as String
        val moduleID = data[moduleID] as Int
        val configData = loadConfigData(configID)
        return ModuleConfig(configID, moduleID, this, configName, configData)
    }

    fun loadConfig(moduleID: Int, configName: String): ModuleConfig? {
        val data = mapOf(
            Companion.configName to configName, Companion.moduleID to moduleID
        )
        val result = queryHelper.getRow(configsTable, data)
        if (result == null) {
            return null
        }
        val configID = result[configID] as Int
        val configData = loadConfigData(configID)
        return ModuleConfig(configID, moduleID, this, configName, configData)
    }

    fun loadConfigs(): MutableList<ModuleConfig> {
        val data = queryHelper.getAll(configsTable)
        val configs = mutableListOf<ModuleConfig>()
        for (row in data) {
            val configID = row[configID] as Int
            val configName = row[configName] as String
            val moduleID = row[moduleID] as Int
            val configData = loadConfigData(configID)
            configs.add(ModuleConfig(configID, moduleID, this, configName, configData))
        }
        return configs
    }

    fun loadConfigs(moduleID: Int): MutableList<ModuleConfig> {
        val data = queryHelper.getAllFiltered(configsTable, Companion.moduleID, moduleID)
        val configs = mutableListOf<ModuleConfig>()
        for (row in data) {
            val configID = row[configID] as Int
            val configName = row[configName] as String
            val configData = loadConfigData(configID)
            configs.add(ModuleConfig(configID, moduleID, this, configName, configData))
        }
        return configs
    }

    fun loadModuleIDFromConfig(configID: Int): Int {
        val data = queryHelper.getRow(configsTable, Companion.configID, configID)
        return data[moduleID] as Int
    }

    fun updateConfig(config: ModuleConfig) {
        val data = mapOf(
            configName to config.getName()
        )
        queryHelper.updateRow(configsTable, configID, config.getConfigID(), data)
        for (configData in config.getConfigData()) {
            updateConfigData(configData)
        }
    }

    fun makeNewConfigID(): Int {
        return queryHelper.makeNewID(configsTable, configID)
    }

    private fun initializeConfigsDataTable() {
        val columns = mutableMapOf(
            configID to ColumnType.INT, configName to ColumnType.TEXT, configType to ColumnType.TEXT, configValue to ColumnType.TEXT
        )
        queryHelper.initialize2KeyTable(configDataTable, columns)
    }

    fun saveConfigData(configData: ConfigData) {
        val data = mapOf(
            configID to configData.getConfigID(),
            configName to configData.getName(),
            configType to configData.getType(),
            configValue to configData.getValue().toString()
        )
        queryHelper.insertRow(configDataTable, data)
    }

    fun loadConfigData(): MutableList<ConfigData> {
        val data = queryHelper.getAll(configDataTable)
        val configData = mutableListOf<ConfigData>()
        for (row in data) {
            configData.add(fillConfigDataFromMap(row)!!)
        }
        return configData
    }

    private fun loadConfigData(configID: Int): MutableList<ConfigData> {
        val data = queryHelper.getAllFiltered(configDataTable, Companion.configID, configID)
        val configData = mutableListOf<ConfigData>()
        for (row in data) {
            configData.add(fillConfigDataFromMap(row)!!)
        }
        return configData
    }

    private fun updateConfigData(configData: ConfigData) {
        val data = mapOf(
            configName to configData.getName(),
            configType to configData.getType(),
            configValue to configData.getValue()
        )
        queryHelper.updateRow(configDataTable, configID, configData.getConfigID(), data)
    }

    fun isConfigDataSaved(configData: ConfigData): Boolean {
        val data = mapOf(
            configID to configData.getConfigID(),
            configName to configData.getName(),
            configType to configData.getType(),
            configValue to configData.getValue().toString()
        )
        val result = queryHelper.getRow(configDataTable, data)
        if (result == null) {
            return false
        }
        return true
    }

    private fun fillConfigDataFromMap(data: Map<String, Any>): ConfigData? {
        if (data[configType] == "bool") {
            return ConfigData(
                data[configID].toString().toInt(),
                data[configName].toString(),
                data[configType].toString(),
                data[configValue].toString().toBoolean()
            )
        } else if (data[configType] == "int") {
            return ConfigData(
                data[configID].toString().toInt(),
                data[configName].toString(),
                data[configType].toString(),
                data[configValue].toString().toInt()
            )
        } else if (data[configType] == "float") {
            return ConfigData(
                data[configID].toString().toInt(),
                data[configName].toString(),
                data[configType].toString(),
                data[configValue].toString().toFloat()
            )
        } else if (data[configType] == "string") {
            return ConfigData(
                data[configID].toString().toInt(), data[configName].toString(), data[configType].toString(), data[configValue].toString()
            )
        }
        return null
    }

    private fun initializeTasksTable() {
        val columns = mutableMapOf(
            taskID to ColumnType.INT,
            moduleID to ColumnType.INT,
            sessionID to ColumnType.INT,
            configID to ColumnType.INT,
            question to ColumnType.TEXT,
            timestamp to ColumnType.TIMESTAMP
        )
        queryHelper.initializeTable(taskTable, columns)
    }

    fun saveTask(taskID: Int, moduleID: Int, sessionID: Int, configID: Int, question: String) {
        val data = mapOf(
            Companion.taskID to taskID,
            Companion.moduleID to moduleID,
            Companion.sessionID to sessionID,
            Companion.configID to configID,
            Companion.question to question,
            timestamp to getTimestamp()
        )
        queryHelper.insertRow(taskTable, data)
    }

    fun loadTasks(sessionID: Int): MutableList<ModuleTask> {
        val data = queryHelper.getAllFiltered(taskTable, Companion.sessionID, sessionID, true)
        val tasks = mutableListOf<ModuleTask>()
        for (row in data) {
            val moduleID = row[moduleID] as Int
            val module = globalModules[moduleID]
            val taskID = row[taskID] as Int
            val question = row[question] as String
            val answers = loadAnswers(taskID)
            val attempts = loadAttempts(taskID)
            val config = loadConfig(row[configID] as Int)
            val task = module!!.deserializeTask(question, answers, attempts, taskID, config)
            tasks.add(task)
        }
        return tasks
    }

    private fun removeTask(taskID: Int) {
        queryHelper.removeRow(taskTable, Companion.taskID, taskID)
        queryHelper.removeRow(attemptsTable, Companion.taskID, taskID)
        queryHelper.removeRow(answersTable, Companion.taskID, taskID)
    }

    fun makeNewTaskID(): Int {
        return queryHelper.makeNewID(taskTable, taskID)
    }

    private fun initializeAttemptsTable() {
        val columns = mutableMapOf(
            attemptID to ColumnType.INT,
            taskID to ColumnType.INT,
            userAnswer to ColumnType.TEXT,
            judgment to ColumnType.BOOL,
            timestamp to ColumnType.TIMESTAMP
        )
        queryHelper.initializeTable(attemptsTable, columns)
    }

    fun saveAttempt(attemptID: Int, taskID: Int, userAnswer: String, judgment: Boolean) {
        val data = mapOf(
            Companion.attemptID to attemptID,
            Companion.taskID to taskID,
            Companion.userAnswer to userAnswer,
            Companion.judgment to judgment,
            timestamp to getTimestamp()
        )
        queryHelper.insertRow(attemptsTable, data)
    }

    private fun loadAttempts(taskID: Int): List<Triple<Int, Any, Boolean>> {
        val data = queryHelper.getAllFiltered(attemptsTable, Companion.taskID, taskID, true)
        val attempts = mutableListOf<Triple<Int, Any, Boolean>>()
        for (row in data) {
            val attemptID = row[attemptID] as Int
            val userAnswer = row[userAnswer] as String
            val judgment = row[judgment] as Int == 1
            attempts.add(Triple(attemptID, userAnswer, judgment))
        }
        return attempts
    }

    private fun loadAmountOfAttemptedTasks(sessionID: Int): Int {
        return queryHelper.getAllByIDFromTable(attemptsTable, taskTable, taskID, Companion.sessionID, sessionID).size
    }

    fun makeNewAttemptID(): Int {
        return queryHelper.makeNewID(attemptsTable, attemptID)
    }

    private fun initializeAnswersTable() {
        val columns = mutableMapOf(
            answerID to ColumnType.INT, taskID to ColumnType.INT, answer to ColumnType.TEXT, timestamp to ColumnType.TIMESTAMP
        )
        queryHelper.initializeTable(answersTable, columns)
    }

    fun saveAnswer(answerID: Int, taskID: Int, answer: String) {
        val data = mapOf(
            Companion.answerID to answerID, Companion.taskID to taskID, Companion.answer to answer, timestamp to getTimestamp()
        )
        queryHelper.insertRow(answersTable, data)
    }

    private fun loadAnswers(taskID: Int): List<Pair<Int, String>> {
        val data = queryHelper.getAllFiltered(answersTable, Companion.taskID, taskID, true)
        val answers = mutableListOf<Pair<Int, String>>()
        for (row in data) {
            val answerID = row[answerID] as Int
            val answer = row[answer] as String
            answers.add(Pair(answerID, answer))
        }
        return answers
    }

    fun makeNewAnswerID(): Int {
        return queryHelper.makeNewID(answersTable, answerID)
    }
}