@file:Suppress("ReplaceJavaStaticMethodWithKotlinAnalog")

package com.example.trainerengine.modules.PercentModule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.SeekBar
import android.widget.SeekBar.OnSeekBarChangeListener
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.view.updateLayoutParams
import com.example.trainerengine.R
import com.example.trainerengine.configs.ModuleConfig
import com.example.trainerengine.modules.*
import java.lang.Math.*
import kotlin.math.ln

class PercentModule(moduleID: Int, stub: ModuleStub) : Module(moduleID, stub,
    { module, question, answers, taskID, config, attempt -> PercentTask(module, question, answers, taskID, config, attempt) },
    { attempt, id, userAnswer, judgement -> PercentAttempt(attempt, id, userAnswer, judgement) },
    { text -> PercentQuestion(text) },
    { id, text -> PercentAnswer(id, text.toInt()) },
    { attempt, loadedUserAnswer -> PercentUserAnswer(attempt, loadedUserAnswer) },
    { attempt, loadedJudgement -> PercentJudgment(attempt, loadedJudgement) },
    { task -> PercentFragment(task) }) {

    override fun makeTask(taskID: Int, config: ModuleConfig): ModuleTask {
        val minValue = config.getConfigData("Min value")!!.getValue() as Int
        val maxValue = config.getConfigData("Max value")!!.getValue() as Int
        val value = (minValue..maxValue).random()
        val question = "$value%"
        return PercentTask(this, PercentQuestion(question), listOf(PercentAnswer(0, value)), taskID, config)
    }
}

class PercentTask(
    module: Module,
    question: TaskQuestion,
    answers: List<TaskAnswer>,
    taskID: Int,
    config: ModuleConfig,
    loadedAttemptID: Triple<Int, Any, Boolean>? = null
) : ModuleTask(module, question, answers, taskID, config, loadedAttemptID)

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
        x = attempt.task().getAnswers()[0].getAnswer().toDouble() / 100.0
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
        if (getTask().getCurrentAttempt().userAnswer.getUserAnswer() == null) {
            getTask().getCurrentAttempt().userAnswer.setUserAnswer(50)
        }
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
        val answerView = view.findViewById(R.id.AnswerPreview) as TextView

        questionView.text = getTask().getQuestion().getQuestion()
        answerInput.progress = getTask().getCurrentAttempt().userAnswer.getUserAnswer()!!.toString().toInt()
        answerView.text = getTask().getCurrentAttempt().userAnswer.getUserAnswer()!!.toString() + "%"
        answerView.updateLayoutParams<ConstraintLayout.LayoutParams> { horizontalBias = answerInput.progress / 100.0f }

        if (getTask().getState() == TaskState.AWAITING) {
            answerInput.isEnabled = true
            answerView.visibility = View.INVISIBLE
        } else if (getTask().getState() == TaskState.LOCKED) {
            answerInput.isEnabled = false
            answerView.visibility = View.VISIBLE
        }
    }
}

class PercentModuleStub : ModuleStub() {
    override val descriptionName: String = "Percent Module"
    override val databaseName: String = "Percent"
    override val moduleDirectory: String = "PercentModule"

    override fun createModule(moduleID: Int): Module {
        return PercentModule(moduleID, this)
    }

    override fun getSkillSet(): SkillSet {
        TODO("Not yet implemented")
    }
}