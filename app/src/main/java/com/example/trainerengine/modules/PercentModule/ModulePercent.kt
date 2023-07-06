package com.example.trainerengine.modules.PercentModule

import android.content.Context
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import com.example.trainerengine.R
import com.example.trainerengine.module.*
import java.lang.Math.*
import kotlin.math.ln

class PercentModule(moduleID: Int, stub: ModuleStub) : Module(moduleID, stub,
    { module, question, answers, taskID, attempt -> PercentTask(module, question, answers, taskID, attempt) },
    { attempt, id, userAnswer, judgement -> PercentAttempt(attempt, id, userAnswer, judgement) },
    { text -> PercentQuestion(text) },
    { id, text -> PercentAnswer(id, text.toInt()) },
    { attempt, loadedUserAnswer -> PercentUserAnswer(attempt, loadedUserAnswer) },
    { attempt, loadedJudgement -> PercentJudgment(attempt, loadedJudgement) },
    { task -> PercentFragment(task) }) {
    
    override fun makeTask(taskID: Int, selectedConfig: Map<String, Any>): ModuleTask {
        val rand = (0..100).random()
        val question = "$rand%"
        return PercentTask(this, PercentQuestion(question), listOf(PercentAnswer(0, rand)), taskID)
    }
}

class PercentTask(
    module: Module,
    question: TaskQuestion,
    answers: List<TaskAnswer>,
    taskID: Int,
    loadedAttemptID: Triple<Int, Any, Boolean>? = null
) : ModuleTask(module, question, answers, taskID, loadedAttemptID)

class PercentAttempt(
    task: ModuleTask, loadedAttemptID: Int? = null, loadedUserAnswer: Any? = null, loadedJudgment: Boolean? = null
) : TaskAttempt(task, loadedAttemptID, loadedUserAnswer, loadedJudgment)

class PercentQuestion(question: String) : TaskQuestion(question)

class PercentAnswer(answerID: Int, answer: Int) : TaskAnswer(answerID, answer.toString())

class PercentUserAnswer(attempt: TaskAttempt, loadedUserAnswer: Any? = null) : TaskUserAnswer(attempt, loadedUserAnswer)

class PercentJudgment(attempt: TaskAttempt, loadedJudgment: Boolean?) : TaskJudgment(attempt, loadedJudgment) {
    override fun checkAnswer() {
        var x = attempt.userAnswer.getUserAnswer().toString().toDouble() / 100.0
        val userLogIntAnswer = ln(x / (1 - x))
        x = attempt.task().getAnswers()[0].getAnswer().toString().toDouble() / 100.0
        x = x / 0.999 + 0.0005
        val logIntAnswer = ln(x / (1 - x))
        val scoreContinuous = abs(logIntAnswer - userLogIntAnswer)
        val score = 1 - min(max(scoreContinuous - 0.1, 0.0) / 0.3, 1.0)
        judgement = score > 0 || attempt.userAnswer.getUserAnswer().toString().toInt() == attempt.task()
            .getAnswers()[0].getAnswer().toInt()
    }
}

class PercentFragment(task: ModuleTask) : TaskFragment(task) {
    private lateinit var view: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        view = inflater.inflate(R.layout.module_percent, container, false)

        val answerInput = view.findViewById(R.id.PercentAnswer) as SeekBar
        answerInput.setOnSeekBarChangeListener(object : OnSeekBarChangeListener {
            override fun onStopTrackingTouch(seekBar: SeekBar) {}
            override fun onStartTrackingTouch(seekBar: SeekBar) {}

            override fun onProgressChanged(seekBar: SeekBar, progress: Int, fromUser: Boolean) {
                getTask().getCurrentAttempt().userAnswer.setUserAnswer(progress)
            }
        })
        updateUI()
        return view
    }

    override fun updateUI() {
        val questionView = view.findViewById(R.id.PercentQuestion) as TextView
        val answerInput = view.findViewById(R.id.PercentAnswer) as SeekBar

        questionView.text = getTask().question().getQuestion().toString()
        if (getTask().getCurrentAttempt().userAnswer.getUserAnswer() != null) {
            answerInput.progress = getTask().getCurrentAttempt().userAnswer.getUserAnswer().toString().toInt()
        }

        if (getTask().getState() == TaskState.AWAITING) {
            answerInput.isEnabled = true
        } else if (getTask().getState() == TaskState.LOCKED) {
            answerInput.isEnabled = false
        }
    }
}

class PercentModuleStub : ModuleStub() {
    override val descriptionName: String = "Percent Module"
    override val databasePrefix: String = "Percent"
    override val moduleDirectory: String = "PercentModule"

    override fun createModule(moduleID: Int): Module {
        return PercentModule(moduleID, this)
    }

    override fun getSkillSet(): SkillSet {
        TODO("Not yet implemented")
    }
}