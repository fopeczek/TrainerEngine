package com.example.trainerengine

import com.example.trainerengine.SQL.GlobalSQLiteManager
import com.example.trainerengine.module.Module
import com.example.trainerengine.modules.MathModule.MathModuleStub
import com.example.trainerengine.modules.PercentModule.PercentModuleStub
import com.example.trainerengine.modules.PythonMathModule.PythonMathModuleStub

data class SessionConfig(
    var name: String = "",
    var repeatable: Boolean = false,
    var reset: Boolean = false,
    var pointPenalty: Int = 2,
    var targetPoints: Int = 10
)

class Session(
    private var sessionID: Int,
    private var modules: MutableList<Module>,
    private var config: SessionConfig,
    private var database: GlobalSQLiteManager,
    private var answeredTaskAmount: Int = 0,
    private var points: Int = 0
) {
    constructor(serializedSession: Map<String, Any>, database: GlobalSQLiteManager) : this(
        0, mutableListOf(), SessionConfig(), database
    ) {
        sessionID = serializedSession[GlobalSQLiteManager.sessionID] as Int
        points = serializedSession[GlobalSQLiteManager.points] as Int
        config.name = serializedSession[GlobalSQLiteManager.sessionName] as String
        config.targetPoints = serializedSession[GlobalSQLiteManager.targetPoints] as Int
        config.reset = serializedSession[GlobalSQLiteManager.reset].toString().toBoolean()
        config.pointPenalty = serializedSession[GlobalSQLiteManager.penaltyPoints] as Int
        config.repeatable = serializedSession[GlobalSQLiteManager.repeatable].toString().toBoolean()
        answeredTaskAmount = database.getAmountOfAttemptedTasks(sessionID)
        val modules = mutableListOf<Module>()
        val serializedModules = (serializedSession[GlobalSQLiteManager.modules] as String).split(",")
        for (serializedModule in serializedModules) {
            when (serializedModule) {
                for
            }
        }
        this.modules = modules
    }

//    fun serialize(): Map<String, Any> {
//        val serializedModules = mutableListOf<String>()
//        for (module in modules) {
//            when (module.getStub().databasePrefix) {
//                "Math" -> {
//                    serializedModules.add("MathModule")
//                }
//
//                "Percent" -> {
//                    serializedModules.add("PercentModule")
//                }
//
//                "Python" -> {
//                    serializedModules.add("PythonMathModule")
//                }
//            }
//        }
//        val modules = serializedModules.joinToString(separator = ",")
//        return mapOf(
//            GlobalSQLiteManager.sessionID to sessionID,
//            GlobalSQLiteManager.modules to modules,
//            GlobalSQLiteManager.points to points,
//            GlobalSQLiteManager.reset to config.reset,
//            GlobalSQLiteManager.penaltyPoints to config.pointPenalty,
//            GlobalSQLiteManager.targetPoints to config.targetPoints,
//            GlobalSQLiteManager.repeatable to config.repeatable,
//            GlobalSQLiteManager.timestamp to getTimestamp()
//        )
//    }
    
    fun answerTask() {
        answeredTaskAmount++
    }

    fun isFinished(): Boolean {
        return points >= config.targetPoints
    }

    fun getModules(): MutableList<Module> {
        return modules
    }

    fun getSessionID(): Int {
        return sessionID
    }

    fun getConfig(): SessionConfig {
        return config
    }

//    fun setConfig(config: SessionConfig) {
//        this.config = config
//        database.updateSessionConfig(sessionID, config)
//    }

    fun getAnsweredTaskAmount(): Int {
        return answeredTaskAmount
    }

    fun getPoints(): Int {
        return points
    }

    fun setPoints(points: Int) {
        this.points = points
        if (this.points < 0) {
            this.points = 0
        }
        database.updateSessionPoints(this.points, sessionID)
    }
}