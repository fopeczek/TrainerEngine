package com.example.trainerengine.session

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.Window
import android.widget.Button
import android.widget.ProgressBar
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import androidx.lifecycle.Lifecycle
import androidx.viewpager2.adapter.FragmentStateAdapter
import androidx.viewpager2.widget.ViewPager2
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.trainerengine.R
import com.example.trainerengine.SQL.GlobalSQLiteManager
import com.example.trainerengine.SQL.SQLiteHelper
import com.example.trainerengine.Session
import com.example.trainerengine.getTimestamp
import com.example.trainerengine.module.ModuleTask
import com.example.trainerengine.module.TaskFragment
import com.example.trainerengine.module.TaskState

class SessionManager(sessionId: Int, private val activity: SessionViewer) {
    private val sqLiteHelper: SQLiteHelper = SQLiteHelper(activity.baseContext)
    private val database: GlobalSQLiteManager = GlobalSQLiteManager(sqLiteHelper)

    private var currentSession: Session

    init {
        currentSession = Session(database.getSession(sessionId)!!, database)
    }

    fun submitAnswer() {
        val task = activity.getCurrentTask()
        if (task.getState() == TaskState.LOCKED) {
            return
        }
        val attempt = task.getCurrentAttempt()
        val judgement = attempt.checkAnswer()!!
        if (judgement.isCorrect()) {
            Toast.makeText(activity, "Correct!", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(activity, "Incorrect!", Toast.LENGTH_SHORT).show()
        }

        val serializedAttempt = mapOf<String, Any>(
            GlobalSQLiteManager.attemptID to database.getNewAttemptID(),
            GlobalSQLiteManager.taskID to task.getTaskID(),
            GlobalSQLiteManager.userAnswer to attempt.userAnswer.getUserAnswer().toString(),
            GlobalSQLiteManager.judgment to judgement.isCorrect(),
            GlobalSQLiteManager.timestamp to getTimestamp()
        )
        database.saveAttempt(serializedAttempt)
        updatePoints(judgement.isCorrect())

        if (!currentSession.getConfig().repeatable) {
            activity.getCurrentTask().setState(TaskState.LOCKED)
        }
        currentSession.answerTask()
    }

    private fun updatePoints(answer: Boolean?) {
        if (answer != null) {
            if (answer) {
                currentSession.setPoints(currentSession.getPoints() + 1)
            } else {
                if (currentSession.getConfig().reset) {
                    currentSession.setPoints(0)
                } else {
                    currentSession.setPoints(currentSession.getPoints() - currentSession.getConfig().pointPenalty)
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
        val rand = (0 until currentSession.getModules().size).random()
        val module = currentSession.getModules().elementAt(rand)
        val taskID = database.getNewTaskID()
        val config = database.getModuleConfig(0)
        val task = module.makeTask(taskID, config)
        val serializedTask = mapOf<String, Any>(
            GlobalSQLiteManager.taskID to taskID,
            GlobalSQLiteManager.moduleID to rand,
            GlobalSQLiteManager.sessionID to currentSession.getSessionID(),
            GlobalSQLiteManager.question to task.question().getQuestion(),
            GlobalSQLiteManager.timestamp to getTimestamp()
        )
        database.saveTask(serializedTask)
        for (answer in task.getAnswers()) {
            val serializedAnswer = mapOf(
                GlobalSQLiteManager.answerID to database.getNewAnswerID(),
                GlobalSQLiteManager.taskID to taskID,
                GlobalSQLiteManager.answer to answer.getAnswer(),
                GlobalSQLiteManager.timestamp to getTimestamp()
            )
            database.saveAnswer(serializedAnswer)
        }
        return task
    }

    fun deserializeTask(serializedTask: Map<String, Any>): ModuleTask {
        if (currentSession.getModules().size < serializedTask["ModuleID"].toString().toInt()) {
            throw Exception("ModuleID out of range")
        }
        val module = currentSession.getModules().elementAt(serializedTask["ModuleID"].toString().toInt())
        val taskID = serializedTask[GlobalSQLiteManager.taskID] as Int
        val question = serializedTask[GlobalSQLiteManager.question] as String

        val answers = database.getAnswers(taskID)
        val loadedAnswers = mutableListOf<Pair<Int, Any>>()
        for (answer in answers) {
            val answerID = answer[GlobalSQLiteManager.answerID].toString().toInt()
            val correctAnswer = answer[GlobalSQLiteManager.answer]!!
            loadedAnswers.add(Pair(answerID, correctAnswer))
        }

        val attempts = database.getAttempts(taskID)
        val loadedAttempts = mutableListOf<Triple<Int, Any, Boolean>>()
        if (attempts.isNotEmpty()) {
            val attempt: Map<String, Any> = attempts.last()
            val attemptID = attempt[GlobalSQLiteManager.attemptID].toString().toInt()
            val userAnswer = attempt[GlobalSQLiteManager.userAnswer].toString()
            val judgement = attempt[GlobalSQLiteManager.judgment].toString().toBoolean()
            loadedAttempts.add(Triple(attemptID, userAnswer, judgement))
        }

        return module.deserializeTask(question, loadedAnswers, loadedAttempts, taskID)
    }

    fun getTargetPoints(): Int {
        return currentSession.getConfig().targetPoints
    }

    fun getPoints(): Int {
        return currentSession.getPoints()
    }

    fun getAnsweredTaskAmount(): Int {
        return currentSession.getAnsweredTaskAmount()
    }

    fun getDatabase(): GlobalSQLiteManager {
        return database
    }

    fun getCurrentSession(): Session {
        return currentSession
    }
}

class SessionViewer : AppCompatActivity() {
    private lateinit var sessionManager: SessionManager
    private lateinit var pager: ViewPager2
    private lateinit var progressBar: ProgressBar

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_ACTION_BAR);
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

        pager.registerOnPageChangeCallback(object : ViewPager2.OnPageChangeCallback() {
            override fun onPageSelected(position: Int) {
                super.onPageSelected(position)
                supportActionBar?.title = getCurrentTask().module().getStub().descriptionName
            }
        })

        findViewById<Button>(R.id.Submit).setOnClickListener { sessionManager.submitAnswer() }

        progressBar = findViewById(R.id.Points)
        progressBar.max = sessionManager.getTargetPoints()
        progressBar.progress = sessionManager.getPoints()
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
            val loadedTasks = sessionManager.getDatabase().getTasks(sessionManager.getCurrentSession().getSessionID())
            return if (position > loadedTasks.size - 1 || loadedTasks.isEmpty()) {
                sessionManager.makeTask().fragment() as Fragment
            } else {
                sessionManager.deserializeTask(loadedTasks[position]).fragment() as Fragment
            }
        }
    }
}