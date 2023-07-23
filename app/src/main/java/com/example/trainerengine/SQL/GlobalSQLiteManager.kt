package com.example.trainerengine.SQL

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
        const val selectedModules = "SelectedModules"
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
    }

    private fun initializeSessionsTable() {
        val columns = mutableMapOf(
            sessionID to ColumnType.INT,
            sessionName to ColumnType.TEXT,
            selectedModules to ColumnType.TEXT, // TODO configID (they hold module id and config)
            points to ColumnType.INT,
            reset to ColumnType.BOOL,
            penaltyPoints to ColumnType.INT,
            targetPoints to ColumnType.INT,
            repeatable to ColumnType.BOOL,
            timestamp to ColumnType.TIMESTAMP
        )
        database.initializeTable(sessionsTable, columns)
    }

    fun saveSession(values: Map<String, Any>): Int {
        return database.insertRow(sessionsTable, values)
    }

    fun getSession(sessionID: Int): Map<String, Any>? {
        return database.getRow(sessionsTable, Companion.sessionID, sessionID)
    }

    fun removeSession(sessionID: Int) {
        database.removeRow(sessionsTable, Companion.sessionID, sessionID)
        val tasks = getTasks(sessionID)
        for (task in tasks) {
            removeTask(task[taskID] as Int)
        }
    }

    fun getSessions(): List<Map<String, Any>> {
        return database.loadOrderedByTimestamp(sessionsTable)
    }

    fun updateSessionPoints(points: Int, sessionID: Int) {
        database.updateColumn(sessionsTable, Companion.sessionID, sessionID, Companion.points, points)
    }

    fun updateSession(values: Map<String, Any>, sessionID: Int) {
        database.updateRow(sessionsTable, Companion.sessionID, sessionID, values)
    }

    fun getNewSessionID(): Int {
        return database.getNewID(sessionsTable, sessionID)
    }

    private fun initializeModulesTable() {
        val columns = mutableMapOf(
            moduleID to ColumnType.INT, moduleName to ColumnType.TEXT, timestamp to ColumnType.TIMESTAMP
        )
        database.initializeTable(modulesTable, columns)
    }

    fun saveModule(values: Map<String, Any>): Int {
        return database.insertRow(modulesTable, values)
    }

    fun getModules(): List<Map<String, Any>> {
        return database.loadOrderedByTimestamp(modulesTable)
    }

    fun getNewModuleID(): Int {
        return database.getNewID(modulesTable, moduleID)
    }

    fun initializeModuleConfigTables(moduleId: Int, settings: List<Triple<String, String, Any>>) {
        val columns = mutableMapOf(
            configID to ColumnType.INT,
            configName to ColumnType.TEXT,
            configType to ColumnType.TEXT,
            configValue to ColumnType.TEXT,
            moduleID to ColumnType.INT
        )
        database.initialize2KeyTable(configsTable, columns)
        for (setting in settings) {
            val defaults = mutableMapOf(
                configID to 0,
                configName to setting.first,
                configType to setting.second,
                configValue to setting.third,
                moduleID to moduleId
            )
            database.set2KeyRow(configsTable, defaults)
        }
    }

    fun getModuleConfig(configID: Int, moduleId: Int): Map<String, Any> {
        val data = database.getAllByID(configsTable, Companion.configID, configID, false)
        val config = mutableMapOf<String, Any>()
        for (row in data) {
            if(row[moduleID] != moduleId) continue
            if (row[configType] == "bool") {
                config[row[configName] as String] = (row[configValue] as String).toBoolean()
            } else if (row[configType] == "int") {
                config[row[configName] as String] = (row[configValue] as String).toInt()
            } else if (row[configType] == "float") {
                config[row[configName] as String] = (row[configValue] as String).toFloat()
            } else if (row[configType] == "string") {
                config[row[configName] as String] = row[configValue] as String
            }
        }
        return config
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

    fun saveTask(values: Map<String, Any>): Int {
        return database.insertRow(taskTable, values)
    }

    fun getTasks(sessionID: Int): List<Map<String, Any>> {
        return database.getAllByID(taskTable, Companion.sessionID, sessionID)
    }

    private fun removeTask(taskID: Int) {
        database.removeRow(taskTable, Companion.taskID, taskID)
        database.removeRow(attemptsTable, Companion.taskID, taskID)
        database.removeRow(answersTable, Companion.taskID, taskID)
    }

    fun getNewTaskID(): Int {
        return database.getNewID(taskTable, taskID)
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

    fun saveAttempt(values: Map<String, Any>): Int {
        return database.insertRow(attemptsTable, values)
    }

    fun getAttempts(taskID: Int): List<Map<String, Any>> {
        return database.getAllByID(attemptsTable, Companion.taskID, taskID)
    }

    fun getNewAttemptID(): Int {
        return database.getNewID(attemptsTable, attemptID)
    }

    fun getAmountOfAttemptedTasks(sessionID: Int): Int {
        return database.getAllByIDFromTable(attemptsTable, taskTable, taskID, Companion.sessionID, sessionID).size
    }

    private fun initializeAnswersTable() {
        val columns = mutableMapOf(
            answerID to ColumnType.INT,
            taskID to ColumnType.INT,
            answer to ColumnType.TEXT,
            timestamp to ColumnType.TIMESTAMP
        )
        database.initializeTable(answersTable, columns)
    }

    fun saveAnswer(values: Map<String, Any>): Int {
        return database.insertRow(answersTable, values)
    }

    fun getAnswers(taskID: Int): List<Map<String, Any>> {
        return database.getAllByID(answersTable, Companion.taskID, taskID)
    }

    fun getNewAnswerID(): Int {
        return database.getNewID(answersTable, answerID)
    }
}