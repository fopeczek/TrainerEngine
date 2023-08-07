package com.example.trainerengine.session

import android.content.Intent
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.view.Menu
import android.view.Window
import android.widget.Button
import android.widget.ImageView
import android.widget.ProgressBar
import android.widget.Toast
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
import com.example.trainerengine.SQL.GlobalSQLiteManager
import com.example.trainerengine.SQL.SQLiteHelper
import com.example.trainerengine.Session
import com.example.trainerengine.globalModules
import com.example.trainerengine.module.ModuleTask
import com.example.trainerengine.module.TaskFragment
import com.example.trainerengine.module.TaskState

class SessionManager(sessionId: Int, private val activity: SessionViewer) {
    private val sqLiteHelper: SQLiteHelper = SQLiteHelper(activity.baseContext)
    private val database: GlobalSQLiteManager = GlobalSQLiteManager(sqLiteHelper)

    private var currentSession: Session

    init {
        currentSession = database.loadSession(sessionId)
    }

    fun submitAnswer() {
        val task = activity.getCurrentTask()
        if (task.getState() == TaskState.LOCKED) {
            return
        }
        val attempt = task.getCurrentAttempt()
        val judgement = attempt.checkAnswer()!!
        if (judgement.isCorrect()) {
            val avd = activity.getCorrectDrawable() as SeekableAnimatedVectorDrawable
            avd.start()
        } else {
            val avd = activity.getWrongDrawable() as SeekableAnimatedVectorDrawable
            avd.start()
        }

        database.saveAttempt(
            database.makeNewAttemptID(),
            task.getTaskID(),
            attempt.userAnswer.getUserAnswer().toString(),
            judgement.isCorrect()
        )
        updatePoints(judgement.isCorrect())

        if (!currentSession.getRepeatable()) {
            activity.getCurrentTask().setState(TaskState.LOCKED)
        }
        currentSession.answerTask()
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
            Toast.makeText(activity, "Done!", Toast.LENGTH_LONG).show()
            val intent = Intent(activity, SessionList::class.java)
            intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP)
            activity.startActivity(intent)
        }
    }

    fun makeTask(): ModuleTask {
        val rand = (0 until currentSession.getConfigIDs().size).random()
        val configID = currentSession.getConfigIDs().elementAt(rand)
        val moduleID = database.loadModuleIDFromConfig(configID)
        val module = globalModules[moduleID]!!
        val taskID = database.makeNewTaskID()
        val taskConfig = database.loadConfig(configID)
        val task = module.makeTask(taskID, taskConfig)
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

        val sessionId = intent.getIntExtra(GlobalSQLiteManager.sessionID, -1)
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
                    findViewById<Button>(R.id.Submit).isEnabled = false
                    if (getCurrentTask().getCurrentAttempt().checkAnswer()!!.isCorrect()) {
                        correctAvd.currentPlayTime = correctAvd.totalDuration
                    } else {
                        wrongAvd.currentPlayTime = wrongAvd.totalDuration
                    }
                } else {
                    findViewById<Button>(R.id.Submit).isEnabled = true
                }
            }
        })

        findViewById<Button>(R.id.Submit).setOnClickListener { sessionManager.submitAnswer() }

        progressBar = findViewById(R.id.Points)
        progressBar.max = sessionManager.getTargetPoints()
        progressBar.progress = sessionManager.getPoints()
    }

    fun getCorrectDrawable(): Drawable {
        return correct.drawable
    }

    fun getWrongDrawable(): Drawable {
        return wrong.drawable
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        menuInflater.inflate(R.menu.session_settings, menu)
        return true
    }

//    override fun onOptionsItemSelected(item: MenuItem): Boolean {
//        if (item.itemId == R.id.sessionSettings){
//            val intent = Intent(this, SessionEditor::class.java)
//            intent.putExtra(GlobalSQLiteManager.sessionID, sessionManager.getCurrentSession().getSessionID())
//            startActivity(intent)
//            return true
//        }
//        return super.onOptionsItemSelected(item)
//    }

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
            return if (position > sessionManager.getAmountLoadedTasks() - 1) {
                sessionManager.makeTask().fragment()
            } else {
                sessionManager.getTaskByPosition(position).fragment()
            }
        }
    }
}