package com.example.trainerengine.modules.MathModule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import com.example.trainerengine.R
import com.example.trainerengine.configs.ModuleConfig
import com.example.trainerengine.modules.*

class MathModule(moduleID: Int, stub: ModuleStub) : Module(moduleID,
    stub,
    { module, question, answers, taskID, attempt -> MathTask(module, question, answers, taskID, attempt) },
    { attempt, id, userAnswer, judgement -> MathAttempt(attempt, id, userAnswer, judgement) },
    { text -> MathQuestion(text) },
    { id, text -> MathAnswer(id, text.toInt()) },
    { attempt, loadedUserAnswer -> MathUserAnswer(attempt, loadedUserAnswer) },
    { attempt, loadedJudgement -> MathJudgment(attempt, loadedJudgement) },
    { task -> MathFragment(task) }) {

    override fun makeTask(taskID: Int, config: ModuleConfig): ModuleTask {
        val maxVal = config.getConfigData("Max value")!!.getValue() as Int
        val doNeg = config.getConfigData("Do negation")!!.getValue()
        val rand1 = (0..maxVal).random()
        val rand2 = (0..maxVal).random()
        var neg = (0..1).random()
        if (doNeg == false) {
            neg = 0
        }
        val question: String = if (neg == 1) {
            "$rand1-$rand2="
        } else {
            "$rand1+$rand2="
        }
        val answer: Int = if (neg == 1) {
            rand1 - rand2
        } else {
            rand1 + rand2
        }
        return MathTask(this, MathQuestion(question), listOf(MathAnswer(1, answer)), taskID)
    }
}

class MathTask(
    module: Module,
    question: TaskQuestion,
    answers: List<TaskAnswer>,
    taskID: Int,
    loadedAttempt: Triple<Int, Any, Boolean>? = null
) : ModuleTask(module, question, answers, taskID, loadedAttempt)

class MathAttempt(
    task: ModuleTask, loadedAttemptID: Int? = null, loadedUserAnswer: Any? = null, loadedJudgment: Boolean? = null
) : TaskAttempt(task, loadedAttemptID, loadedUserAnswer, loadedJudgment)

class MathQuestion(question: String) : TaskQuestion(question)

class MathAnswer(answerID: Int, answer: Int) : TaskAnswer(answerID, answer.toString())

class MathUserAnswer(attempt: TaskAttempt, loadedUserAnswer: Any?) : TaskUserAnswer(attempt, loadedUserAnswer)

class MathJudgment(attempt: TaskAttempt, loadedJudgment: Boolean?) : TaskJudgment(attempt, loadedJudgment) {
    override fun checkAnswer() {
        judgement =
            attempt.userAnswer.getUserAnswer().toString().toInt() == attempt.task().getAnswers()[0].getAnswer().toInt()
    }
}

class MathFragment(task: ModuleTask) : TaskFragment(task) {
    private lateinit var view: View
    private lateinit var answerInput: EditText
    private lateinit var answerPreview: TextView
    private lateinit var questionView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        view = inflater.inflate(R.layout.module_math, container, false)

        answerPreview = view.findViewById(R.id.MathPreview) as TextView
        questionView = view.findViewById(R.id.MathQuestion) as TextView
        answerInput = view.findViewById(R.id.MathAnswer) as EditText
        answerInput.addTextChangedListener { text ->
            if (text.toString() == "" || text.toString() == "-") {
                getTask().getCurrentAttempt().userAnswer.setUserAnswer(null)
                return@addTextChangedListener
            }
            getTask().getCurrentAttempt().userAnswer.setUserAnswer(text.toString().toInt())
        }

        updateUI()

        return view
    }

    override fun updateUI() {
        questionView.text = getTask().question().getQuestion().toString()
        val userAnswer = getTask().getCurrentAttempt().userAnswer.getUserAnswer()
        if (userAnswer != null) {
            answerInput.setText(userAnswer.toString())
            answerPreview.text = userAnswer.toString()
        }

        if (getTask().getState() == TaskState.AWAITING) {
            answerInput.visibility = View.VISIBLE
            answerPreview.visibility = View.INVISIBLE
        } else if (getTask().getState() == TaskState.LOCKED) {
            answerInput.visibility = View.INVISIBLE
            answerPreview.visibility = View.VISIBLE
        }
    }
}

class MathModuleStub : ModuleStub() {
    override val descriptionName: String = "Math Module"
    override val databaseName: String = "Math"
    override val moduleDirectory: String = "MathModule"

    override fun createModule(moduleID: Int): Module {
        return MathModule(moduleID, this)
    }

    override fun getSkillSet(): SkillSet {
        TODO("Not yet implemented")
    }
}
