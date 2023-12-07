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
import com.example.trainerengine.skills.Skill
import com.example.trainerengine.skills.SkillSet

class Database(private val queryHelper: QueryHelper) {
    companion object {
        const val sessionID = "SessionID"
        const val sessionName = "SessionName"
        const val repeatable = "Repeatable"
        const val reset = "Reset"
        const val penaltyPoints = "PenaltyPoints"
        const val targetPoints = "TargetPoints"
        const val configIDs = "ConfigIDs"
        const val points = "Points"

        const val moduleID = "ModuleID"
        const val moduleName = "ModuleName"

        const val configID = "ConfigID"

        const val configDataID = "ConfigDataID"
        const val configName = "ConfigName"
        const val configType = "ConfigType"
        const val configValue = "ConfigValue"

        const val taskID = "TaskID"
        const val question = "Question"

        const val attemptID = "AttemptID"
        const val userAnswer = "UserAnswer"
        const val judgment = "Judgement"

        const val answerID = "AnswerID"
        const val answer = "Answer"

        const val skillSetID = "SkillSetID"

        const val skillID = "SkillID"
        const val skillName = "SkillName"
        const val skillDescription = "SkillDescription"
        const val skillScore = "SkillScore"
        const val skillVisibility = "SkillVisibility"

        const val timestamp = "Timestamp"

        const val sessionsTable = "Sessions"
        const val modulesTable = "Modules"
        const val configsTable = "Configs"
        const val configDataTable = "ConfigData"
        const val taskTable = "Tasks"
        const val attemptsTable = "Attempts"
        const val answersTable = "Answers"
        const val skillSetsTable = "SkillSets"
        const val skillsTable = "Skills"
    }

    init {
        initializeSessionsTable()
        initializeModulesTable()
        initializeConfigsTable()
        initializeConfigsDataTable()
        initializeTasksTable()
        initializeAttemptsTable()
        initializeAnswersTable()
        initializeSkillSetsTable()
        initializeSkillsTable()
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
        val sessionID = data[sessionID] as Int
        val sessionName = data[sessionName] as String
        val configIDs = (data[configIDs] as String).split(",").map { it.toInt() }.toMutableList()
        val points = data[points] as Int
        val targetPoints = data[targetPoints] as Int
        val reset = (data[reset] as Int) > 0
        val pointPenalty = data[penaltyPoints] as Int
        val repeatable = (data[repeatable] as Int) > 0
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
            modules.add(fillModuleFromMap(row))
        }
        return modules
    }

    fun makeNewModuleID(): Int {
        return queryHelper.makeNewID(modulesTable, moduleID)
    }

    private fun fillModuleFromMap(data: Map<String, Any>): Module {
        val moduleID = data[moduleID].toString().toInt()
        val moduleName = data[moduleName].toString()
        when (moduleName) {
            MathModuleStub().databaseName -> {
                return MathModuleStub().createModule(moduleID)
            }

            PercentModuleStub().databaseName -> {
                return PercentModuleStub().createModule(moduleID)
            }

            PythonMathModuleStub().databaseName -> {
                return PythonMathModuleStub().createModule(moduleID)
            }
        }
        throw Exception("Module not found")
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
        return fillConfigFromMap(data)
    }

    fun loadConfig(moduleID: Int, configName: String): ModuleConfig? {
        val data = mapOf(
            Companion.configName to configName, Companion.moduleID to moduleID
        )
        val result = queryHelper.getRow(configsTable, data)
        if (result == null) {
            return null
        }
        return fillConfigFromMap(result)
    }

    fun loadConfigs(): MutableList<ModuleConfig> {
        val data = queryHelper.getAll(configsTable)
        val configs = mutableListOf<ModuleConfig>()
        for (row in data) {
            configs.add(fillConfigFromMap(row))
        }
        return configs
    }

    fun loadConfigs(moduleID: Int): MutableList<ModuleConfig> {
        val data = queryHelper.getAllFiltered(configsTable, Companion.moduleID, moduleID)
        val configs = mutableListOf<ModuleConfig>()
        for (row in data) {
            configs.add(fillConfigFromMap(row))
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

    fun removeJustConfig(configID: Int) {
        queryHelper.removeRow(configsTable, Companion.configID, configID)
    }

    fun makeNewConfigID(): Int {
        return queryHelper.makeNewID(configsTable, configID)
    }

    private fun fillConfigFromMap(data: Map<String, Any>): ModuleConfig {
        val configID = data[configID] as Int
        val configName = data[configName] as String
        val moduleID = data[moduleID] as Int
        val configData = loadConfigData(configID)
        return ModuleConfig(configID, moduleID, this, configName, configData)
    }

    private fun initializeConfigsDataTable() {
        val columns = mutableMapOf(
            configDataID to ColumnType.INT,
            configID to ColumnType.INT,
            configName to ColumnType.TEXT,
            configType to ColumnType.TEXT,
            configValue to ColumnType.TEXT
        )
        queryHelper.initializeTable(configDataTable, columns)
    }

    fun saveConfigData(configData: ConfigData) {
        val data = mapOf(
            configDataID to configData.getConfigDataID(),
            configID to configData.getConfigID(),
            configName to configData.getName(),
            configType to configData.getType(),
            configValue to configData.getValue().toString()
        )
        queryHelper.insertRow(configDataTable, data)
    }

    fun loadConfigData(configID: Int, configName: String): ConfigData? {
        val data = mapOf(
            Companion.configID to configID, Companion.configName to configName
        )
        val result = queryHelper.getRow(configDataTable, data)
        if (result == null) {
            return null
        }
        return fillConfigDataFromMap(result)
    }

    fun loadConfigData(): MutableList<ConfigData> {
        val data = queryHelper.getAll(configDataTable)
        val configData = mutableListOf<ConfigData>()
        for (row in data) {
            configData.add(fillConfigDataFromMap(row))
        }
        return configData
    }

    private fun loadConfigData(configID: Int): MutableList<ConfigData> {
        val data = queryHelper.getAllFiltered(configDataTable, Companion.configID, configID)
        val configData = mutableListOf<ConfigData>()
        for (row in data) {
            configData.add(fillConfigDataFromMap(row))
        }
        return configData
    }

    private fun updateConfigData(configData: ConfigData) {
        val data = mapOf(
            configID to configData.getConfigID(),
            configName to configData.getName(),
            configType to configData.getType(),
            configValue to configData.getValue()
        )
        queryHelper.updateRow(configDataTable, configDataID, configData.getConfigDataID(), data)
    }

    fun removeConfigData(configDataID: Int) {
        queryHelper.removeRow(configDataTable, Companion.configDataID, configDataID)
    }

    fun isConfigDataSaved(configData: ConfigData): Boolean { // Warning: checks only for configID and configName should be used only in init
        val data = mapOf(
            configID to configData.getConfigID(),
            configName to configData.getName(), // two configData can't have the same name
        )
        val result = queryHelper.getRow(configDataTable, data)
        if (result == null) {
            return false
        }
        return true
    }

    fun makeNewConfigDataID(): Int {
        return queryHelper.makeNewID(configDataTable, configDataID)
    }

    private fun fillConfigDataFromMap(data: Map<String, Any>): ConfigData {
        val configDataID = data[configDataID] as Int
        val configID = data[configID] as Int
        val configName = data[configName] as String
        val configType = data[configType] as String
        var configValue: Any? = null
        if (data[Companion.configType] == "bool") {
            configValue = (data[Companion.configValue] as String).toInt() == 1
        } else if (data[Companion.configType] == "int") {
            configValue = (data[Companion.configValue] as String).toInt()
        } else if (data[Companion.configType] == "float") {
            configValue = (data[Companion.configValue] as String).toFloat()
        } else if (data[Companion.configType] == "string") {
            configValue = data[Companion.configValue] as String
        }
        if (configValue == null) {
            throw Exception("Config value is null")
        }
        return ConfigData(configDataID, configID, configName, configType, configValue)
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
            tasks.add(fillTaskFromMap(row))
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

    private fun fillTaskFromMap(data: Map<String, Any>): ModuleTask {
        val moduleID = data[moduleID] as Int
        val module = globalModules[moduleID]
        val taskID = data[taskID] as Int
        val question = data[question] as String
        val answers = loadAnswers(taskID)
        val attempts = loadAttempts(taskID)
        val config = loadConfig(data[configID] as Int)
        return module!!.deserializeTask(question, answers, attempts, taskID, config)
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
            attempts.add(fillAttemptFromMap(row))
        }
        return attempts
    }

    private fun loadAmountOfAttemptedTasks(sessionID: Int): Int {
        return queryHelper.getAllByIDFromTable(attemptsTable, taskTable, taskID, Companion.sessionID, sessionID).size
    }

    fun makeNewAttemptID(): Int {
        return queryHelper.makeNewID(attemptsTable, attemptID)
    }

    private fun fillAttemptFromMap(data: Map<String, Any>): Triple<Int, Any, Boolean> {
        val attemptID = data[attemptID] as Int
        val userAnswer = data[userAnswer] as String
        val judgment = data[judgment] as Int == 1
        return Triple(attemptID, userAnswer, judgment)
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
            answers.add(fillAnswerFromMap(row))
        }
        return answers
    }

    fun makeNewAnswerID(): Int {
        return queryHelper.makeNewID(answersTable, answerID)
    }

    fun fillAnswerFromMap(data: Map<String, Any>): Pair<Int, String> {
        val answerID = data[answerID] as Int
        val answer = data[answer] as String
        return Pair(answerID, answer)
    }

    private fun initializeSkillSetsTable() {
        val columns = mutableMapOf(
            skillSetID to ColumnType.INT, moduleID to ColumnType.INT, sessionID to ColumnType.INT
        )
        queryHelper.initializeTable(skillSetsTable, columns)
    }

    fun saveSkillSet(skillSet: SkillSet) {
        val data = mapOf(
            skillSetID to skillSet.getSkillSetID(), moduleID to skillSet.getModuleID()
        )
        queryHelper.insertRow(skillSetsTable, data)
    }

    fun loadSkillSet(skillSetID: Int): SkillSet {
        val data = queryHelper.getRow(skillSetsTable, Companion.skillSetID, skillSetID)
        return fillSkillSetFromMap(data)
    }

    fun loadSkillSet(moduleID: Int, sessionID: Int): SkillSet? {
        val data = mapOf(
            Companion.moduleID to moduleID, Companion.sessionID to sessionID
        )
        val result = queryHelper.getRow(skillSetsTable, data)
        if (result == null) {
            return null
        }
        return fillSkillSetFromMap(result)
    }

    fun loadSkillSets(moduleID: Int): MutableList<SkillSet> {
        val data = queryHelper.getAllFiltered(skillSetsTable, Companion.moduleID, moduleID)
        val skillSets = mutableListOf<SkillSet>()
        for (row in data) {
            skillSets.add(fillSkillSetFromMap(row))
        }
        return skillSets
    }

    fun updateSkillSet(skillSet: SkillSet) {
        val data = mapOf(
            moduleID to skillSet.getModuleID()
        )
        queryHelper.updateRow(skillSetsTable, skillSetID, skillSet.getSkillSetID(), data)
        for (skill in skillSet.getSkills()) {
            updateSkill(skill)
        }
    }

    fun makeNewSkillSetID(): Int {
        return queryHelper.makeNewID(skillSetsTable, skillSetID)
    }

    private fun fillSkillSetFromMap(data: Map<String, Any>): SkillSet {
        val skillSetID = data[skillSetID] as Int
        val moduleID = data[moduleID] as Int
        val sessionID = data[sessionID] as Int
        val skills = loadSkills(skillSetID)
        return SkillSet(skillSetID, moduleID, sessionID, this, skills)
    }

    private fun initializeSkillsTable() {
        val columns = mutableMapOf(
            skillID to ColumnType.INT,
            skillSetID to ColumnType.INT,
            skillName to ColumnType.TEXT,
            skillDescription to ColumnType.TEXT,
            skillScore to ColumnType.FLOAT,
            skillVisibility to ColumnType.BOOL,
        )
        queryHelper.initializeTable(skillsTable, columns)
    }

    fun saveSkill(skill: Skill) {
        val data = mapOf(
            skillID to skill.getSkillID(),
            skillSetID to skill.getSkillSetID(),
            skillName to skill.getName(),
            skillDescription to skill.getDescription(),
            skillScore to skill.getScore()
        )
        queryHelper.insertRow(skillsTable, data)
    }

    fun loadSkill(skillSetID: Int, skillName: String): Skill? {
        val data = mapOf(
            Companion.skillSetID to skillSetID, Companion.skillName to skillName
        )
        val result = queryHelper.getRow(skillsTable, data)
        if (result == null) {
            return null
        }
        return fillSkillFromMap(result)
    }

    fun loadSkills(): MutableList<Skill> {
        val data = queryHelper.getAll(skillsTable)
        val skills = mutableListOf<Skill>()
        for (row in data) {
            skills.add(fillSkillFromMap(row))
        }
        return skills
    }

    fun loadSkills(skillSetID: Int): MutableList<Skill> {
        val data = queryHelper.getAllFiltered(skillsTable, Companion.skillSetID, skillSetID)
        val skills = mutableListOf<Skill>()
        for (row in data) {
            skills.add(fillSkillFromMap(row))
        }
        return skills
    }

    fun updateSkill(skill: Skill) {
        val data = mapOf(
            skillSetID to skill.getSkillSetID(), skillName to skill.getName(), skillDescription to skill.getDescription()
        )
        queryHelper.updateRow(skillsTable, skillID, skill.getSkillID(), data)
    }

    fun removeSkill(skillID: Int) {
        queryHelper.removeRow(skillsTable, Companion.skillID, skillID)
    }

    fun isSkillSaved(skill: Skill): Boolean { //Warning: checks only for skillSetID and skillName should be used only in init
        val data = mapOf(
            skillSetID to skill.getSkillSetID(), // should be equal -1
            skillName to skill.getName() // two skills can't have the same name
        )
        val result = queryHelper.getRow(skillsTable, data)
        if (result == null) {
            return false
        }
        return true
    }

    fun makeNewSkillID(): Int {
        return queryHelper.makeNewID(skillsTable, skillID)
    }

    private fun fillSkillFromMap(data: Map<String, Any>): Skill {
        val skillID = data[skillID] as Int
        val skillSetID = data[skillSetID] as Int
        val skillName = data[skillName] as String
        val skillDescription = data[skillDescription] as String
        val skillScore = data[skillScore] as Float
        val skillVisibility = data[skillVisibility] as Int == 1
        return Skill(skillID, skillSetID, skillName, skillDescription, skillScore, skillVisibility, this)
    }
}