package com.example.trainerengine.session

import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.Window
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.trainerengine.R
import com.example.trainerengine.SQL.GlobalSQLiteManager
import com.example.trainerengine.SQL.SQLiteHelper
import com.example.trainerengine.Session
import com.example.trainerengine.modules
import com.example.trainerengine.modules.MathModule.MathModuleStub
import com.example.trainerengine.modules.PercentModule.PercentModuleStub
import com.example.trainerengine.modules.PythonMathModule.PythonMathModuleStub
import com.google.android.material.floatingactionbutton.FloatingActionButton

class SessionList : AppCompatActivity() {
    private lateinit var sqLiteHelper: SQLiteHelper
    private lateinit var database: GlobalSQLiteManager

    private val selectedSessions = mutableListOf<Session>()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_ACTION_BAR)
        setContentView(R.layout.activity_session_list)
        supportActionBar?.title = "Session List"

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        sqLiteHelper = SQLiteHelper(applicationContext)
        database = GlobalSQLiteManager(sqLiteHelper)

        //load modules
        modules.add(MathModuleStub().createModule(database.getNewModuleID()))
        modules.add(PythonMathModuleStub().createModule(database.getNewModuleID()))
        modules.add(PercentModuleStub().createModule(database.getNewModuleID()))

        val newSession = findViewById<FloatingActionButton>(R.id.addSession)
        newSession.setOnClickListener {
            val intent = Intent(this, SessionEditor::class.java)
            startActivity(intent)
        }

        listSessions()
    }

    override fun onCreateOptionsMenu(menu: Menu?): Boolean {
        if (selectedSessions.size != 0) {
            menuInflater.inflate(R.menu.session_delete, menu)
        }
        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        if (item.itemId == R.id.sessionDelete) {
            if (selectedSessions.size == 0) {
                listSessions()
            } else {
                for (session in selectedSessions) {
                    database.removeSession(session.getSessionID())

                }
                selectedSessions.clear()
                listSessions()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        listSessions()
    }

    private fun onSessionCheck(session: Session, sessionCheck: CheckBox) {
        if (sessionCheck.isChecked) {
            if (!selectedSessions.map { it.getSessionID() }.contains(session.getSessionID())) {
                selectedSessions.add(session)
            }
        } else {
            selectedSessions.removeIf { it.getSessionID() == session.getSessionID() }
        }
        if (selectedSessions.size == 0) {
            listSessions()
        }
    }

    private fun editListSessions(){
        val sessionList = findViewById<LinearLayout>(R.id.session_list)
        sessionList.removeAllViews()
        invalidateOptionsMenu()
        for (sessionData in database.getSessions()) {
            val session = Session(sessionData, database)
            val sessionCheck = CheckBox(this)
            sessionCheck.text = session.getConfig().name
            sessionCheck.setTextAppearance(androidx.appcompat.R.style.TextAppearance_AppCompat_Large)
            sessionCheck.setOnClickListener {
                onSessionCheck(session, sessionCheck)
            }
            if (selectedSessions.map { it.getSessionID() }.contains(session.getSessionID())) {
                sessionCheck.isChecked = true
            }
            sessionList.addView(sessionCheck)
        }
    }

    private fun listSessions() {
        val sessionList = findViewById<LinearLayout>(R.id.session_list)
        sessionList.removeAllViews()
        invalidateOptionsMenu()
        for (sessionData in database.getSessions()) {
            val session = Session(sessionData, database)
            val sessionText = TextView(this)
            sessionText.text = session.getConfig().name
            sessionText.setTextAppearance(androidx.appcompat.R.style.TextAppearance_AppCompat_Large)
            if (session.isFinished()){
                sessionText.setTextColor(getColor(R.color.green))
            }
            sessionText.setOnClickListener {
                val intent = Intent(this, SessionViewer::class.java)
                intent.putExtra(GlobalSQLiteManager.sessionID, session.getSessionID())
                startActivity(intent)
            }
            sessionText.setOnLongClickListener {
                selectedSessions.add(session)
                editListSessions()
                true
            }
            sessionList.addView(sessionText)
        }
    }
}