package com.example.trainerengine.sessions

import com.example.trainerengine.database.Database
import com.example.trainerengine.modules.ModuleTask

class Session( //Here are the defaults for session creation
    private var sessionID: Int,
    private var database: Database,
    private var configIDs: MutableList<Int> = mutableListOf(),
    private var tasks: MutableList<ModuleTask> = mutableListOf(),
    private var name: String = "Session $sessionID",
    private var repeatable: Boolean = false,
    private var reset: Boolean = false,
    private var pointPenalty: Int = 2,
    private var targetPoints: Int = 10,
    private var answeredTaskAmount: Int = 0,
    private var points: Int = 0
) {
    fun answerTask() {
        answeredTaskAmount++
        database.updateSession(this)
    }

    fun isFinished(): Boolean {
        return points >= targetPoints
    }

    fun getConfigIDs(): MutableList<Int> {
        return configIDs
    }

    fun setConfigIDs(configIDs: MutableList<Int>) {
        this.configIDs = configIDs
        database.updateSession(this)
    }

    fun getTasks(): MutableList<ModuleTask> {
        return tasks
    }

    fun addTask(task: ModuleTask) {
        tasks.add(task)
    }

    fun getSessionID(): Int {
        return sessionID
    }

    fun getName(): String {
        return name
    }

    fun setName(name: String) {
        this.name = name
        database.updateSession(this)
    }

    fun getRepeatable(): Boolean {
        return repeatable
    }

    fun setRepeatable(repeatable: Boolean) {
        this.repeatable = repeatable
        database.updateSession(this)
    }

    fun getReset(): Boolean {
        return reset
    }

    fun setReset(reset: Boolean) {
        this.reset = reset
        database.updateSession(this)
    }

    fun getPointPenalty(): Int {
        return pointPenalty
    }

    fun setPointPenalty(pointPenalty: Int) {
        this.pointPenalty = pointPenalty
        database.updateSession(this)
    }

    fun getTargetPoints(): Int {
        return targetPoints
    }

    fun setTargetPoints(targetPoints: Int) {
        this.targetPoints = targetPoints
        database.updateSession(this)
    }

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
        database.updateSession(this)
    }
}
