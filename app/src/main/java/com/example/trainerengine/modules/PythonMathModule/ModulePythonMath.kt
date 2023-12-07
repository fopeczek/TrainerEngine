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
import com.example.trainerengine.configs.ModuleConfig
import com.example.trainerengine.modules.*

class PythonMathModule(moduleID: Int, stub: ModuleStub) : Module(moduleID,
    stub,
    { module, question, answers, taskID, config, attempt -> PythonMathTask(module, question, answers, taskID, config, attempt) },
    { attempt, id, userAnswer, judgement -> PythonMathAttempt(attempt, id, userAnswer, judgement) },
    { text -> PythonMathQuestion(text) },
    { id, text -> PythonMathAnswer(id, text) },
    { attempt, loadedUserAnswer -> PythonMathUserAnswer(attempt, loadedUserAnswer) },
    { attempt, loadedJudgement -> PythonMathJudgment(attempt, loadedJudgement) },
    { task -> PythonMathFragment(task) }) {
    val pythonModule = Python.getInstance().getModule("MikModule/module")

    override fun makeTask(taskID: Int, config: ModuleConfig): ModuleTask {
        val task = pythonModule.callAttr("make_task", config).asList()
        return PythonMathTask(this, PythonMathQuestion(task[0].toString()), listOf(PythonMathAnswer(0, task[1].toString())), taskID, config)
    }

    override fun getAllSkills(): MutableMap<String, String> {
        val skills = pythonModule.callAttr("get_all_skills").asMap()
        val result = mutableMapOf<String, String>()
        for (skill in skills) {
            result[skill.key.toString()] = skill.value.toString()
        }
        return result
    }
}

class PythonMathTask(
    module: Module,
    question: TaskQuestion,
    answers: List<TaskAnswer>,
    taskID: Int,
    config: ModuleConfig,
    loadedAttempt: Triple<Int, Any, Boolean>? = null
) : ModuleTask(module, question, answers, taskID, config, loadedAttempt)

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
            attempt.task().getQuestion().getQuestion(),
            attempt.task().getAnswers().first().getAnswer().toInt(),
            attempt.userAnswer.getUserAnswer().toString().toInt(),
            attempt.task().getConfig()
        ).toBoolean()
    }
}

class PythonMathFragment(task: ModuleTask) : TaskFragment(task) {
    private lateinit var view: View
    private lateinit var answerInput: EditText
    private lateinit var answerPreview: TextView
    private lateinit var questionView: TextView

    override fun onCreateView(
        inflater: LayoutInflater, container: ViewGroup?, savedInstanceState: Bundle?
    ): View {
        view = inflater.inflate(R.layout.module_math, container, false)

        answerPreview = view.findViewById(R.id.module_math_text_preview) as TextView
        questionView = view.findViewById(R.id.module_math_text_question) as TextView
        answerInput = view.findViewById(R.id.module_math_input_answer) as EditText
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
        questionView.text = getTask().getQuestion().getQuestion()
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
    override val descriptionName: String = "Python Math Module"
    override val databaseName: String = "PythonMath"
    override val moduleDirectory: String = "PythonMathModule"

    override fun createModule(moduleID: Int): Module {
        return PythonMathModule(moduleID, this)
    }
}
