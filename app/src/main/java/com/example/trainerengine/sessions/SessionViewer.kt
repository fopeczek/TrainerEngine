package com.example.trainerengine.sessions

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.CountDownTimer
import android.view.*
import android.view.inputmethod.InputMethodManager
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.vectordrawable.graphics.drawable.SeekableAnimatedVectorDrawable
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.trainerengine.R
import com.example.trainerengine.database.Database
import com.example.trainerengine.database.QueryHelper
import com.example.trainerengine.confettiOnFinishSession
import com.example.trainerengine.globalModules
import com.example.trainerengine.modules.ModuleTask
import com.example.trainerengine.modules.TaskFragment
import com.example.trainerengine.modules.TaskState

class SessionManager(sessionId: Int, private val activity: SessionViewer) {
    private val database: Database = Database(QueryHelper(activity.baseContext))

    private var currentSession: Session

    init {
        currentSession = database.loadSession(sessionId)
        if (currentSession.getTasks().size == 0 || (currentSession.getAnsweredTaskAmount() == currentSession.getTasks().size && !currentSession.isFinished())) {
            makeTask()
        }
    }

    fun submitAnswer() {
        val task = activity.getCurrentTask()
        if (task.getState() == TaskState.LOCKED) {
            return
        }
        val attempt = task.getCurrentAttempt()
        val judgement = attempt.checkAnswer()!!
        if (judgement.isCorrect()) {
            activity.playCorrectAnimation()
        } else {
            activity.playWrongAnimation()
        }

        database.saveAttempt(
            database.makeNewAttemptID(), task.getTaskID(), attempt.userAnswer.getUserAnswer().toString(), judgement.isCorrect()
        )

        currentSession.answerTask()

        if (!currentSession.getRepeatable()) {
            activity.getCurrentTask().setState(TaskState.LOCKED)
            activity.onTaskAnswered()
        }

        updatePoints(judgement.isCorrect())

        if (!currentSession.isFinished()) {
            makeTask()
        }
    }

    private fun updatePoints(answer: Boolean?) {
        if (answer != null) {
            if (answer) {
                currentSession.setPoints(currentSession.getPoints() + 1)
            } else {
                if (currentSession.getReset()) {
                    currentSession.setPoints(0)
                } else {
                    currentSession.setPoints(currentSession.getPoints() - currentSession.getPointPenalty())
                }
            }
        }
        activity.setUserProgress(currentSession.getPoints())

        if (currentSession.isFinished()) {
            activity.onSessionFinished()

            val keyboardTimer = object : CountDownTimer(500, 500) {
                override fun onTick(millisUntilFinished: Long) {}
                override fun onFinish() {
                    confettiOnFinishSession(activity)
                }
            }
            keyboardTimer.start()

            val timer = object : CountDownTimer(3000, 3000) {
                override fun onTick(millisUntilFinished: Long) {}
                override fun onFinish() {
                    val intent = Intent(activity, SessionList::class.java)
                    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
                    activity.startActivity(intent)
                }
            }
            timer.start()
        }
    }

    private fun makeTask(): ModuleTask {
        val rand = (0 until currentSession.getConfigIDs().size).random()
        val configID = currentSession.getConfigIDs().elementAt(rand)
        val moduleID = database.loadModuleIDFromConfig(configID)
        val module = globalModules[moduleID]!!
        val taskID = database.makeNewTaskID()
        val taskConfig = database.loadConfig(configID)
        val task = module.makeTask(taskID, taskConfig)
        currentSession.addTask(task)
        database.saveTask(taskID, moduleID, currentSession.getSessionID(), task.question().getQuestion())
        for (answer in task.getAnswers()) {
            database.saveAnswer(database.makeNewAnswerID(), taskID, answer.getAnswer())
        }
        return task
    }

    fun getTaskByPosition(position: Int): ModuleTask {
        return database.loadTasks(currentSession.getSessionID())[position]
    }

    fun getTargetPoints(): Int {
        return currentSession.getTargetPoints()
    }

    fun getPoints(): Int {
        return currentSession.getPoints()
    }

    fun getAnsweredTaskAmount(): Int {
        return currentSession.getAnsweredTaskAmount()
    }

    fun getAmountLoadedTasks(): Int {
        return database.loadTasks(currentSession.getSessionID()).size
    }

    fun getCurrentSession(): Session {
        return currentSession
    }
}

class SessionViewer : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private lateinit var pager: ViewPager2
    private lateinit var progressBar: ProgressBar
    private lateinit var submitButton: Button
    private lateinit var correct: ImageView
    private lateinit var wrong: ImageView
    private lateinit var correctAvd: SeekableAnimatedVectorDrawable
    private lateinit var wrongAvd: SeekableAnimatedVectorDrawable

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_ACTION_BAR)
        setContentView(R.layout.activity_session_viewer)
        supportActionBar?.title = "Module Manager"

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        val sessionId = intent.getIntExtra(Database.sessionID, -1)
        if (sessionId == -1) {
            throw Exception("Session ID not found")
        }
        sessionManager = SessionManager(sessionId, this)

        pager = findViewById(R.id.Pager)
        val adapter = TaskPagerAdapter(supportFragmentManager, lifecycle, sessionManager)
        pager.adapter = adapter

        correct = findViewById(R.id.Correct)
        correct.setImageDrawable(SeekableAnimatedVectorDrawable.create(this, R.drawable.avd_correct))
        correctAvd = correct.drawable as SeekableAnimatedVectorDrawable
        wrong = findViewById(R.id.Wrong)
        wrong.setImageDrawable(SeekableAnimatedVectorDrawable.create(this, R.drawable.avd_wrong))
        wrongAvd = wrong.drawable as SeekableAnimatedVectorDrawable

        submitButton = findViewById(R.id.Submit)
        submitButton.setOnClickListener { sessionManager.submitAnswer() }

//        val viewTreeObserver: ViewTreeObserver = submitButton.viewTreeObserver
//        if (viewTreeObserver.isAlive) {
//            viewTreeObserver.addOnGlobalLayoutListener(object : OnGlobalLayoutListener {
//                override fun onGlobalLayout() {
//                    submitButton.viewTreeObserver.removeOnGlobalLayoutListener(this)
//                    if (sessionManager.getCurrentSession().isFinished()) {
//                        confettiFinishSession(this@SessionViewer)
//                    }
//                }
//            })
//        }

        if (sessionManager.getAmountLoadedTasks() != 0) {
            val lastTaskPos = sessionManager.getAmountLoadedTasks() - 1
            pager.setCurrentItem(lastTaskPos, false)
            val task = sessionManager.getTaskByPosition(lastTaskPos)
            if (task.getState() == TaskState.LOCKED) {
                if (task.getCurrentAttempt().checkAnswer()!!.isCorrect()) {
                    correctAvd.currentPlayTime = correctAvd.totalDuration
                } else {
                    wrongAvd.currentPlayTime = wrongAvd.totalDuration
                }
                submitButton.isEnabled = false
            }
        }

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                supportActionBar?.title = getCurrentTask().module().getStub().descriptionName
                val correctAvd = correct.drawable as SeekableAnimatedVectorDrawable
                correctAvd.stop()
                correctAvd.currentPlayTime = 0
                val wrongAvd = wrong.drawable as SeekableAnimatedVectorDrawable
                wrongAvd.stop()
                wrongAvd.currentPlayTime = 0
                if (getCurrentTask().getState() == TaskState.LOCKED) {
                    submitButton.isEnabled = false
                    if (getCurrentTask().getCurrentAttempt().checkAnswer()!!.isCorrect()) {
                        correctAvd.currentPlayTime = correctAvd.totalDuration
                    } else {
                        wrongAvd.currentPlayTime = wrongAvd.totalDuration
                    }
                } else {
                    submitButton.isEnabled = true
                }
            }
        })

        progressBar = findViewById(R.id.Points)
        progressBar.max = sessionManager.getTargetPoints()
        progressBar.progress = sessionManager.getPoints()
    }

    fun playCorrectAnimation() {
        correctAvd.start()
    }

    fun playWrongAnimation() {
        wrongAvd.start()
    }

    fun onTaskAnswered() {
        submitButton.isEnabled = false
    }

    fun onSessionFinished() {
        val imm: InputMethodManager = getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
        imm.hideSoftInputFromWindow(submitButton.windowToken, 0)
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.session_settings, menu)
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.sessionSettings) {
            val intent = Intent(this, SessionEditor::class.java)
            intent.putExtra(Database.sessionID, sessionManager.getCurrentSession().getSessionID())
            startActivity(intent)
            finish()
            return true
        }
        return super.onOptionsItemSelected(item)
    }

    fun getCurrentTask(): ModuleTask {
        val taskFragment = supportFragmentManager.findFragmentByTag("f" + pager.currentItem)!! as TaskFragment
        return taskFragment.getTask()
    }

    fun setUserProgress(progress: Int) {
        progressBar.progress = progress
    }

    private inner class TaskPagerAdapter(
        fragmentManager: FragmentManager, lifecycle: Lifecycle, private var sessionManager: SessionManager
    ) : FragmentStateAdapter(fragmentManager, lifecycle) {
        override fun getItemCount(): Int {
            return if (sessionManager.getCurrentSession().isFinished()) {
                sessionManager.getAnsweredTaskAmount()
            } else {
                sessionManager.getAnsweredTaskAmount() + 1
            }
        }

        override fun createFragment(position: Int): Fragment {
            return sessionManager.getTaskByPosition(position).fragment()
        }
    }
}