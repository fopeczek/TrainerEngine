package com.example.trainerengine.sessions

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.trainerengine.R
import com.example.trainerengine.database.Database
import com.example.trainerengine.database.QueryHelper
import com.example.trainerengine.globalModules
import com.example.trainerengine.configs.ModuleConfig
import com.example.trainerengine.configs.ModuleConfigAdapter
import com.example.trainerengine.modules.Module


class SessionEditor : AppCompatActivity() {
    private lateinit var database: Database
    private lateinit var session: Session
    private var configs = mutableListOf<ModuleConfig>()
    private var newSession = false

    private lateinit var sessionName: EditText
    private lateinit var penalty: EditText
    private lateinit var target: EditText
    private lateinit var repeatable: CheckBox
    private lateinit var reset: CheckBox
    private lateinit var moduleList: ExpandableListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_session_editor)

        database = Database(QueryHelper(applicationContext))

        if (intent.hasExtra(Database.sessionID)) {
            val sessionID = intent.getIntExtra(Database.sessionID, -1)
            session = database.loadSession(sessionID)
        } else {
            val sessionID = database.makeNewSessionID()
            session = Session(sessionID, database)
            newSession = true
        }

        moduleList = findViewById(R.id.list_modules)
        sessionName = findViewById(R.id.input_session_name)
        penalty = findViewById(R.id.input_penalty)
        target = findViewById(R.id.input_target)
        repeatable = findViewById(R.id.checkbox_repeatable)
        reset = findViewById(R.id.checkbox_reset)

        sessionName.setText(session.getName())
        penalty.setText(session.getPointPenalty().toString())
        target.setText(session.getTargetPoints().toString())
        repeatable.isChecked = session.getRepeatable()
        reset.isChecked = session.getReset()

        for (configID in session.getConfigIDs()) {
            configs.add(database.loadConfig(configID))
        }

        val allConfigs: MutableMap<Module, MutableList<ModuleConfig>> = mutableMapOf()
        val selectedConfigs: MutableMap<Module, MutableList<ModuleConfig>> = mutableMapOf()
        for (module in globalModules.values) {
            allConfigs[module] = database.loadConfigs(module.getModuleID())
            selectedConfigs[module] = mutableListOf()
        }
        for (config in configs) {
            selectedConfigs[globalModules[config.getModuleID()]]!!.add(config)
        }

        val adapter = ModuleConfigAdapter(this, allConfigs, selectedConfigs) { checkBox, config -> onConfigClicked(checkBox, config) }
        moduleList.setAdapter(adapter)

        val done = findViewById<Button>(R.id.done_session_create)
        done.setOnClickListener { onDone() }
    }

    private fun onConfigClicked(checkBox: CheckBox, config: ModuleConfig) {
        if (checkBox.isChecked) {
            configs.add(config)
        } else {
            for (i in 0 until configs.size) {
                if (configs[i].getConfigID() == config.getConfigID()) {
                    configs.removeAt(i)
                    break
                }
            }
        }
    }

    private fun onDone() {
        if (sessionName.text.toString() == "" || configs.size == 0 || penalty.text.toString() == "" || target.text.toString() == "") {
            return
        }

        if (target.text.toString().toInt() < 1) {
            return
        }

        if (penalty.text.toString().toInt() < 0) {
            return
        }

        if (target.text.toString().toInt() <= penalty.text.toString().toInt()) {
            return
        }

        if (target.text.toString().toInt() <= session.getPoints()) {
            return
        }

        if (newSession) {
            database.saveSession(session)
        }

        session.setName(sessionName.text.toString())
        session.setPointPenalty(penalty.text.toString().toInt())
        session.setTargetPoints(target.text.toString().toInt())
        session.setRepeatable(repeatable.isChecked)
        session.setReset(reset.isChecked)
        val configIDs = mutableListOf<Int>()
        for (config in configs) {
            configIDs.add(config.getConfigID())
        }
        session.setConfigIDs(configIDs)
        if (!newSession) {
            val intent = Intent(this, SessionViewer::class.java)
            intent.putExtra(Database.sessionID, session.getSessionID())
            startActivity(intent)
        }
        finish()
    }
}
