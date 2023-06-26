package com.example.trainerengine.session

import android.content.Intent
import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import android.widget.CheckBox
import android.widget.EditText
import android.widget.LinearLayout
import com.example.trainerengine.R
import com.example.trainerengine.SQL.GlobalSQLiteManager
import com.example.trainerengine.SQL.SQLiteHelper
import com.example.trainerengine.Session
import com.example.trainerengine.getTimestamp
import com.example.trainerengine.module.ModuleEditor
import com.example.trainerengine.globalModules
import com.example.trainerengine.modules.MathModule.MathModuleStub
import com.example.trainerengine.modules.PercentModule.PercentModuleStub
import com.example.trainerengine.modules.PythonMathModule.PythonMathModuleStub

class SessionEditor : AppCompatActivity() {
    private lateinit var sqLiteHelper: SQLiteHelper
    private lateinit var database: GlobalSQLiteManager
    private var sessionId = -1
    private var newSession = true

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_editor)

        sqLiteHelper = SQLiteHelper(applicationContext)
        database = GlobalSQLiteManager(sqLiteHelper)

        if (intent.hasExtra(GlobalSQLiteManager.sessionID)) {
            sessionId = intent.getIntExtra(GlobalSQLiteManager.sessionID, -1)
            newSession = false
        }

        val moduleList = findViewById<LinearLayout>(R.id.list_modules)
        val sessionName = findViewById<EditText>(R.id.input_session_name)
        val penalty = findViewById<EditText>(R.id.input_penalty)
        val target = findViewById<EditText>(R.id.input_target)
        val repeatable = findViewById<CheckBox>(R.id.check_repeatable)
        val reset = findViewById<CheckBox>(R.id.check_reset)

        for (module in globalModules){
            val checkBox = CheckBox(this)
            checkBox.text = module.getStub().descriptionName
            checkBox.setOnLongClickListener {
                val intent = Intent(this, ModuleEditor::class.java)
                intent.putExtra(GlobalSQLiteManager.moduleID, module.getModuleID())
                startActivity(intent)
                true
            }
            moduleList.addView(checkBox)
        }

        if (newSession) {
            sessionId = database.getNewSessionID()
            sessionName.setText("Session")
        } else {
            val session = Session(database.getSession(sessionId)!!, database)
            sessionName.setText(session.getConfig().name)
            penalty.setText(session.getConfig().pointPenalty.toString())
            target.setText(session.getConfig().targetPoints.toString())
            repeatable.isChecked = session.getConfig().repeatable
            reset.isChecked = session.getConfig().reset

            for (module in session.getModules()) {
                for (i in 0 until moduleList.childCount) {
                    val checkBox = moduleList.getChildAt(i) as CheckBox
                    if (checkBox.text == module.getStub().descriptionName) {
                        checkBox.isChecked = true
                    }
                }
            }
        }

        val done = findViewById<Button>(R.id.done_session_create)
        done.setOnClickListener { onDone() }
    }

    private fun onDone() {
        val sessionName = findViewById<EditText>(R.id.input_session_name)
        if (sessionName.text.toString() == "") {
            return
        }

        val moduleList = findViewById<LinearLayout>(R.id.list_modules)
        val modules = mutableListOf<String>()
        for (i in 0 until moduleList.childCount) {
            val checkBox = moduleList.getChildAt(i) as CheckBox
            if (checkBox.isChecked) {
                when (checkBox.text) {
                    MathModuleStub().descriptionName -> {
                        modules.add(MathModuleStub().databasePrefix)
                    }

                    PercentModuleStub().descriptionName -> {
                        modules.add(PercentModuleStub().databasePrefix)
                    }

                    PythonMathModuleStub().descriptionName -> {
                        modules.add(PythonMathModuleStub().databasePrefix)
                    }
                }
            }
        }
        if (modules.size == 0) {
            return
        }

        val penalty = findViewById<EditText>(R.id.input_penalty)
        if (penalty.text.toString() == "") {
            return
        }

        val target = findViewById<EditText>(R.id.input_target)
        if (target.text.toString() == "") {
            return
        }

        val repeatable = findViewById<CheckBox>(R.id.check_repeatable)
        val reset = findViewById<CheckBox>(R.id.check_reset)
        val session: MutableMap<String, Any> = mutableMapOf()
        session[GlobalSQLiteManager.sessionID] = sessionId
        session[GlobalSQLiteManager.sessionName] = sessionName.text.toString()
        session[GlobalSQLiteManager.selectedModules] = modules.joinToString(",")
        session[GlobalSQLiteManager.repeatable] = repeatable.isChecked
        session[GlobalSQLiteManager.reset] = reset.isChecked
        session[GlobalSQLiteManager.penaltyPoints] = penalty.text.toString().toInt()
        session[GlobalSQLiteManager.targetPoints] = target.text.toString().toInt()
        if (newSession) {
            session[GlobalSQLiteManager.points] = 0
            session[GlobalSQLiteManager.timestamp] = getTimestamp()
            database.saveSession(session)
        } else {
            database.updateSession(session, sessionId)
        }
        finish()
        if (newSession) {
            val intent = Intent(this, SessionViewer::class.java)
            intent.putExtra(GlobalSQLiteManager.sessionID, sessionId)
            startActivity(intent)
        }
    }
}