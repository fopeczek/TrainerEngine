package com.example.trainerengine.module

import android.content.Context
import androidx.fragment.app.Fragment
import com.moandjiezana.toml.Toml
import java.io.File

enum class TaskState {
    AWAITING, LOCKED,
}

abstract class Module(
    private val moduleID: Int,
    private val stub: ModuleStub,
    val taskFactory: (Module, TaskQuestion, List<TaskAnswer>, Int, Triple<Int, Any, Boolean>?) -> ModuleTask,
    val attemptFactory: (ModuleTask, Int?, Any?, Boolean?) -> TaskAttempt,
    val questionFactory: (String) -> TaskQuestion,
    val answerFactory: (Int, String) -> TaskAnswer,
    val userAnswerFactory: (TaskAttempt, Any?) -> TaskUserAnswer,
    val judgmentFactory: (TaskAttempt, Boolean?) -> TaskJudgment,
    val fragmentFactory: (ModuleTask) -> TaskFragment
) {
    fun getModuleID(): Int {
        return moduleID
    }

    fun getStub(): ModuleStub {
        return stub
    }

    abstract fun makeTask(taskID: Int): ModuleTask

    fun deserializeTask(
        question: String,
        answers: List<Pair<Int, Any>>,
        attempts: List<Triple<Int, Any, Boolean>>,
        taskID: Int
    ): ModuleTask {
        val answersList = mutableListOf<TaskAnswer>()
        for (answer in answers) {
            answersList.add(answerFactory(answer.first, answer.second.toString()))
        }
        return taskFactory(this, questionFactory(question), answersList, taskID, attempts.lastOrNull())
    }
}

abstract class ModuleTask(
    private val module: Module,
    private val question: TaskQuestion,
    private val answers: List<TaskAnswer>,
    private val taskID: Int,
    loadedAttempt: Triple<Int, Any, Boolean>?
) {
    private var currentAttempt: TaskAttempt
    private val fragment: TaskFragment = module.fragmentFactory(this)
    private var state: TaskState = TaskState.AWAITING

    init {
        if (loadedAttempt != null) {
            currentAttempt = module.attemptFactory(this, loadedAttempt.first, loadedAttempt.second, loadedAttempt.third)
            setState(TaskState.LOCKED)
        } else {
            currentAttempt = module.attemptFactory(this, null, null, null)
        }
    }

    fun fragment(): TaskFragment {
        return fragment
    }

    fun getTaskID(): Int {
        return taskID
    }

    fun module(): Module {
        return module
    }

    fun getCurrentAttempt(): TaskAttempt {
        return currentAttempt
    }

    fun question(): TaskQuestion {
        return question
    }

    fun getAnswers(): List<TaskAnswer> {
        return answers
    }

    fun setState(newState: TaskState) {
        state = newState
        fragment.updateUI()
    }

    fun getState(): TaskState {
        return state
    }

    fun serialize(): String {
        return ""
    }
}

abstract class TaskAttempt(
    private val task: ModuleTask,
    loadedAttemptID: Int? = null,
    loadedUserAnswer: Any? = null,
    loadedJudgment: Boolean? = null
) {
    private var attemptID: Int = -1
    val userAnswer: TaskUserAnswer = task.module().userAnswerFactory(this, loadedUserAnswer)
    private val judgment: TaskJudgment = task.module().judgmentFactory(this, loadedJudgment)

    init {
        if (loadedAttemptID != null) {
            attemptID = loadedAttemptID
        }
    }

    fun task(): ModuleTask {
        return task
    }

    fun getAttemptID(): Int {
        return attemptID
    }

    fun checkAnswer(): TaskJudgment? {
        if (userAnswer.getUserAnswer() == null) {
            return null
        }
        judgment.checkAnswer()
        return judgment
    }
}

abstract class TaskQuestion(private val question: String) {
    fun getQuestion(): String {
        return question
    }
}

abstract class TaskAnswer(private val answerID: Int, private val answer: String) {
    fun getAnswer(): String {
        return answer
    }

    fun getAnswerID(): Int {
        return answerID
    }
}

abstract class TaskUserAnswer(protected val attempt: TaskAttempt, loadedAnswer: Any?) {
    private var userAnswer: Any? = null

    init {
        if (loadedAnswer != null) {
            userAnswer = loadedAnswer
        }
    }

    fun getUserAnswer(): Any? {
        return userAnswer
    }

    fun setUserAnswer(answer: Any?) {
        userAnswer = answer
    }
}

abstract class TaskJudgment(protected val attempt: TaskAttempt, loadedJudgment: Boolean?) {
    protected var judgement: Boolean = false

    init {
        if (loadedJudgment != null) {
            judgement = loadedJudgment
        }
    }

    abstract fun checkAnswer()

    fun isCorrect(): Boolean {
        return judgement
    }

    fun grade(): Double {
        return 0.0
    }

    fun specificGrade(skill: Skill): Double {
        return 0.0
    }
}

abstract class TaskFragment(private val task: ModuleTask) : Fragment() {
    fun getTask(): ModuleTask {
        return task
    }

    abstract fun updateUI()
}

abstract class SkillSet {
    private val skills: MutableList<Skill> = mutableListOf()

    fun size(): Int {
        return skills.size
    }

    fun addSkill(skill: Skill) {
        skills.add(skill)
    }

    fun contains(skill: Skill): Boolean {
        return skills.contains(skill)
    }

    fun contains(subSet: SkillSet): Boolean {
        return skills.containsAll(subSet.skills)
    }
}

abstract class Skill(private val name: String, private val description: String, private val skillID: Int) {
    fun getName(): String {
        return name
    }

    fun getDescription(): String {
        return description
    }

    fun getID(): Int {
        return skillID
    }
}

abstract class ModuleStub {
    abstract val descriptionName: String
    abstract val databasePrefix: String
    abstract val moduleDirectory: String

    abstract fun createModule(moduleID: Int): Module

    abstract fun getSkillSet(): SkillSet
}