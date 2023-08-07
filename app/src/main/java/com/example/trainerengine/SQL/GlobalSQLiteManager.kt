package com.example.trainerengine.SQL

import com.example.trainerengine.Session
import com.example.trainerengine.getTimestamp
import com.example.trainerengine.globalModules
import com.example.trainerengine.module.ConfigData
import com.example.trainerengine.module.Module
import com.example.trainerengine.module.ModuleConfig
import com.example.trainerengine.module.ModuleTask
import com.example.trainerengine.modules.MathModule.MathModuleStub
import com.example.trainerengine.modules.PercentModule.PercentModuleStub
import com.example.trainerengine.modules.PythonMathModule.PythonMathModuleStub

class GlobalSQLiteManager(private val database: SQLiteHelper) {
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
        database.initializeTable(sessionsTable, columns)
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
        database.insertRow(sessionsTable, data)
    }

    fun loadSession(sessionID: Int): Session {
        val data = database.getRow(sessionsTable, Companion.sessionID, sessionID)
        return fillSessionFromMap(data)
    }

    fun loadSessions(): List<Session> {
        val data = database.getAll(sessionsTable, true)
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
            repeatable to session.getRepeatable(),
            timestamp to getTimestamp()
        )
        database.updateRow(sessionsTable, Companion.sessionID, session.getSessionID(), data)
    }

    fun removeSession(sessionID: Int) {
        database.removeRow(sessionsTable, Companion.sessionID, sessionID)
        val tasks = loadTasks(sessionID)
        for (task in tasks) {
            removeTask(task.getTaskID())
        }
    }

    fun makeNewSessionID(): Int {
        return database.makeNewID(sessionsTable, sessionID)
    }

    private fun fillSessionFromMap(data: Map<String, Any>): Session {
        val sessionID = data[Companion.sessionID].toString().toInt()
        val sessionName = data[Companion.sessionName].toString()
        val configIDs = data[Companion.configIDs].toString().split(",").map { it.toInt() }.toMutableList()
        val points = data[Companion.points].toString().toInt()
        val targetPoints = data[Companion.targetPoints].toString().toInt()
        val reset = data[Companion.reset].toString().toBoolean()
        val pointPenalty = data[Companion.penaltyPoints] as Int
        val repeatable = data[Companion.repeatable].toString().toBoolean()
        val answeredTaskAmount = loadAmountOfAttemptedTasks(sessionID)
        return Session(
            sessionID, this, configIDs, sessionName, repeatable, reset, pointPenalty, targetPoints, answeredTaskAmount, points
        )
    }

    private fun initializeModulesTable() {
        val columns = mutableMapOf(
            moduleID to ColumnType.INT, moduleName to ColumnType.TEXT, timestamp to ColumnType.TIMESTAMP
        )
        database.initializeTable(modulesTable, columns)
    }

    fun saveModule(module: Module) {
        val data = mapOf(
            Companion.moduleID to module.getModuleID(),
            Companion.moduleName to module.getStub().databaseName,
            Companion.timestamp to getTimestamp()
        )
        database.insertRow(modulesTable, data)
    }

    fun loadModules(): List<Module> {
        val data = database.getAll(modulesTable, true)
        val modules = mutableListOf<Module>()
        for (row in data) {
            val moduleID = row[Companion.moduleID] as Int
            val moduleName = row[Companion.moduleName] as String
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
        return database.makeNewID(modulesTable, moduleID)
    }

    private fun initializeConfigsTable() {
        val columns = mutableMapOf(
            configID to ColumnType.INT, configName to ColumnType.TEXT, moduleID to ColumnType.INT
        )
        database.initializeTable(configsTable, columns)
    }

    fun saveConfig(config: ModuleConfig) {
        val data = mapOf(
            Companion.configID to config.getConfigID(), Companion.configName to config.getName(), Companion.moduleID to config.getModuleID()
        )
        database.insertRow(configsTable, data)
    }

    fun loadConfig(configID: Int): ModuleConfig {
        val data = database.getRow(configsTable, Companion.configID, configID)
        val configName = data[Companion.configName] as String
        val moduleID = data[Companion.moduleID] as Int
        val configData = loadConfigData(configID)
        return ModuleConfig(configID, moduleID, configName, configData)
    }

    fun loadConfig(moduleID: Int, configName: String): ModuleConfig? {
        val data = mapOf(
            Companion.configName to configName, Companion.moduleID to moduleID
        )
        val result = database.getRow(configsTable, data)
        if (result == null) {
            return null
        }
        val configID = result[Companion.configID] as Int
        val configData = loadConfigData(configID)
        return ModuleConfig(configID, moduleID, configName, configData)
    }

    fun loadConfigs(moduleID: Int): MutableList<ModuleConfig> {
        val data = database.getAllFiltered(configsTable, Companion.moduleID, moduleID)
        val configs = mutableListOf<ModuleConfig>()
        for (row in data) {
            val configID = row[Companion.configID] as Int
            val configName = row[Companion.configName] as String
            val configData = loadConfigData(configID)
            configs.add(ModuleConfig(configID, moduleID, configName, configData))
        }
        return configs
    }

    fun loadModuleIDFromConfig(configID: Int): Int {
        val data = database.getRow(configsTable, Companion.configID, configID)
        return data[Companion.moduleID] as Int
    }

    fun makeNewConfigID(): Int {
        return database.makeNewID(configsTable, configID)
    }

    private fun initializeConfigsDataTable() {
        val columns = mutableMapOf(
            configID to ColumnType.INT, configName to ColumnType.TEXT, configType to ColumnType.TEXT, configValue to ColumnType.TEXT
        )
        database.initialize2KeyTable(configDataTable, columns)
    }

    fun saveConfigData(configData: ConfigData) {
        val data = mapOf(
            Companion.configID to configData.getConfigID(),
            Companion.configName to configData.getName(),
            Companion.configType to configData.getType(),
            Companion.configValue to configData.getValue().toString()
        )
        database.insertRow(configDataTable, data)
    }

    fun loadConfigData(): MutableList<ConfigData> {
        val data = database.getAll(configDataTable)
        val configData = mutableListOf<ConfigData>()
        for (row in data) {
            configData.add(fillConfigDataFromMap(row)!!)
        }
        return configData
    }

    fun loadConfigData(configID: Int): MutableList<ConfigData> {
        val data = database.getAllFiltered(configDataTable, Companion.configID, configID)
        val configData = mutableListOf<ConfigData>()
        for (row in data) {
            configData.add(fillConfigDataFromMap(row)!!)
        }
        return configData
    }

    fun loadConfigData(configName: String, configType: String, configValue: Any): ConfigData? {
        val data = mapOf(
            Companion.configName to configName, Companion.configType to configType, Companion.configValue to configValue.toString()
        )
        val result = database.getRow(configDataTable, data)
        if (result == null) {
            return null
        }
        return fillConfigDataFromMap(result)
    }

    fun makeNewConfigDataID(): Int {
        return database.makeNewID(configDataTable, configID)
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
            question to ColumnType.TEXT,
            timestamp to ColumnType.TIMESTAMP
        )
        database.initializeTable(taskTable, columns)
    }

    fun saveTask(taskID: Int, moduleID: Int, sessionID: Int, question: String) {
        val data = mapOf(
            Companion.taskID to taskID,
            Companion.moduleID to moduleID,
            Companion.sessionID to sessionID,
            Companion.question to question,
            Companion.timestamp to getTimestamp()
        )
        database.insertRow(taskTable, data)
    }

    fun loadTasks(sessionID: Int): List<ModuleTask> {
        val data = database.getAllFiltered(taskTable, Companion.sessionID, sessionID, true)
        val tasks = mutableListOf<ModuleTask>()
        for (row in data) {
            val moduleID = row[Companion.moduleID] as Int
            val module = globalModules[moduleID]
            val taskID = row[Companion.taskID] as Int
            val question = row[Companion.question] as String
            val answers = loadAnswers(taskID)
            val attempts = loadAttempts(taskID)
            val task = module!!.deserializeTask(question, answers, attempts, taskID)
            tasks.add(task)
        }
        return tasks
    }

    private fun removeTask(taskID: Int) {
        database.removeRow(taskTable, Companion.taskID, taskID)
        database.removeRow(attemptsTable, Companion.taskID, taskID)
        database.removeRow(answersTable, Companion.taskID, taskID)
    }

    fun makeNewTaskID(): Int {
        return database.makeNewID(taskTable, taskID)
    }

    private fun initializeAttemptsTable() {
        val columns = mutableMapOf(
            attemptID to ColumnType.INT,
            taskID to ColumnType.INT,
            userAnswer to ColumnType.TEXT,
            judgment to ColumnType.BOOL,
            timestamp to ColumnType.TIMESTAMP
        )
        database.initializeTable(attemptsTable, columns)
    }

    fun saveAttempt(attemptID: Int, taskID: Int, userAnswer: String, judgment: Boolean) {
        val data = mapOf(
            Companion.attemptID to attemptID,
            Companion.taskID to taskID,
            Companion.userAnswer to userAnswer,
            Companion.judgment to judgment,
            Companion.timestamp to getTimestamp()
        )
        database.insertRow(attemptsTable, data)
    }

    private fun loadAttempts(taskID: Int): List<Triple<Int, Any, Boolean>> {
        val data = database.getAllFiltered(attemptsTable, Companion.taskID, taskID, true)
        val attempts = mutableListOf<Triple<Int, Any, Boolean>>()
        for (row in data) {
            val attemptID = row[Companion.attemptID] as Int
            val userAnswer = row[Companion.userAnswer] as String
            val judgment = row[Companion.judgment] as Int == 1
            attempts.add(Triple(attemptID, userAnswer, judgment))
        }
        return attempts
    }

    fun loadAmountOfAttemptedTasks(sessionID: Int): Int {
        return database.getAllByIDFromTable(attemptsTable, taskTable, taskID, Companion.sessionID, sessionID).size
    }

    fun makeNewAttemptID(): Int {
        return database.makeNewID(attemptsTable, attemptID)
    }

    private fun initializeAnswersTable() {
        val columns = mutableMapOf(
            answerID to ColumnType.INT, taskID to ColumnType.INT, answer to ColumnType.TEXT, timestamp to ColumnType.TIMESTAMP
        )
        database.initializeTable(answersTable, columns)
    }

    fun saveAnswer(answerID: Int, taskID: Int, answer: String) {
        val data = mapOf(
            Companion.answerID to answerID, Companion.taskID to taskID, Companion.answer to answer, Companion.timestamp to getTimestamp()
        )
        database.insertRow(answersTable, data)
    }

    private fun loadAnswers(taskID: Int): List<Pair<Int, String>> {
        val data = database.getAllFiltered(answersTable, Companion.taskID, taskID, true)
        val answers = mutableListOf<Pair<Int, String>>()
        for (row in data) {
            val answerID = row[Companion.answerID] as Int
            val answer = row[Companion.answer] as String
            answers.add(Pair(answerID, answer))
        }
        return answers
    }

    fun makeNewAnswerID(): Int {
        return database.makeNewID(answersTable, answerID)
    }
}