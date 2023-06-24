package com.example.trainerengine.modules.PythonMathModule

import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.EditText
import android.widget.TextView
import androidx.core.widget.addTextChangedListener
import com.chaquo.python.Python
import com.example.trainerengine.R
import com.example.trainerengine.module.*

class PythonMathModule(moduleID:Int, stub: ModuleStub) : Module(moduleID, stub,
    { module, question, answers, taskID, attempt -> PythonMathTask(module, question, answers, taskID, attempt) },
    { attempt, id, userAnswer, judgement -> PythonMathAttempt(attempt, id, userAnswer, judgement) },
    { text -> PythonMathQuestion(text) },
    { id, text -> PythonMathAnswer(id, text) },
    { attempt, loadedUserAnswer -> PythonMathUserAnswer(attempt, loadedUserAnswer) },
    { attempt, loadedJudgement -> PythonMathJudgment(attempt, loadedJudgement) },
    { task -> PythonMathFragment(task) }) {
    val pythonModule = Python.getInstance().getModule("multiplyModule")

    override fun makeTask(taskID: Int): ModuleTask {
        pythonModule.callAttr("make_task")
        val question = pythonModule.callAttr("get_question").toString()
        val answer = pythonModule.callAttr("get_answer").toString()
        return PythonMathTask(
            this, PythonMathQuestion(question), listOf(PythonMathAnswer(1, answer)), taskID
        )
    }
}

class PythonMathTask(
    module: Module,
    question: TaskQuestion,
    answers: List<TaskAnswer>,
    taskID: Int,
    loadedAttempt: Triple<Int, Any, Boolean>? = null
) : ModuleTask(module, question, answers, taskID, loadedAttempt)

class PythonMathAttempt(
    task: ModuleTask, loadedAttemptID: Int? = null, loadedUserAnswer: Any? = null, loadedJudgment: Boolean? = null
) : TaskAttempt(task, loadedAttemptID, loadedUserAnswer, loadedJudgment)

class PythonMathQuestion(question: String) : TaskQuestion(question)

class PythonMathAnswer(id: Int, answer: String) : TaskAnswer(id, answer)

class PythonMathUserAnswer(attempt: TaskAttempt, loadedUserAnswer: Any?) : TaskUserAnswer(attempt, loadedUserAnswer)

class PythonMathJudgment(attempt: TaskAttempt, loadedJudgment: Boolean?) : TaskJudgment(attempt, loadedJudgment) {
    override fun checkAnswer() {
        val module = attempt.task().module() as PythonMathModule
        judgement = module.pythonModule.callAttr(
            "check_answer",
            attempt.task().getAnswers().first().getAnswer(),
            attempt.userAnswer.getUserAnswer().toString()
        ).toBoolean()
    }
}

class PythonMathFragment(task: ModuleTask) : TaskFragment(task) {
    private lateinit var view: View

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        view = inflater.inflate(R.layout.module_math, container, false)

        val answerInput = view.findViewById(R.id.MathAnswer) as EditText
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
        if (!this::view.isInitialized) {
            return
        }
        val answerPreview = view.findViewById(R.id.MathPreview) as TextView
        val questionView = view.findViewById(R.id.MathQuestion) as TextView
        val answerInput = view.findViewById(R.id.MathAnswer) as EditText

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

class PythonMathModuleStub : ModuleStub() {
    override val descriptionName: String = "Python Module"
    override val databaseName: String = "Python"
    override val supportsMultipleAttempts: Boolean = false
    override val extraAnswerTable: Boolean = false

    override fun createModule(moduleID: Int): Module {
        return PythonMathModule(moduleID, this)
    }

    override fun getSkillSet(): SkillSet {
        TODO("Not yet implemented")
    }
}