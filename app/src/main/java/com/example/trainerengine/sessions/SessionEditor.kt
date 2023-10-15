package com.example.trainerengine.sessions

import android.content.Intent
import android.os.Bundle
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.trainerengine.R
import com.example.trainerengine.configs.ConfigEditor
import com.example.trainerengine.database.Database
import com.example.trainerengine.database.QueryHelper
import com.example.trainerengine.globalModules
import com.example.trainerengine.configs.ModuleConfig
import com.example.trainerengine.configs.ModuleConfigAdapter
import com.example.trainerengine.modules.Module


class SessionEditor : AppCompatActivity() {
    private lateinit var database: Database
    private lateinit var session: Session
    private var newSession = false

    private var sessionName = ""
    private var penalty = 0
    private var target = 0
    private var repeatable = false
    private var reset = false
    private var configs = mutableListOf<ModuleConfig>()

    private lateinit var sessionNameInput: EditText
    private lateinit var penaltyInput: EditText
    private lateinit var targetInput: EditText
    private lateinit var repeatableInput: CheckBox
    private lateinit var resetInput: CheckBox
    private lateinit var configList: ExpandableListView

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        setContentView(R.layout.activity_session_module_selector)
        database = Database(QueryHelper(applicationContext))

        if (intent.hasExtra(Database.sessionID)) {
            val sessionID = intent.getIntExtra(Database.sessionID, -1)
            session = database.loadSession(sessionID)
        } else {
            val sessionID = database.makeNewSessionID()
            session = Session(sessionID, database)
            newSession = true
        }

        penalty = session.getPointPenalty()
        target = session.getTargetPoints()
        repeatable = session.getRepeatable()
        reset = session.getReset()

        configList = findViewById(R.id.list_modules)
        sessionNameInput = findViewById(R.id.input_session_name)

        sessionNameInput.setText(session.getName())

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

        val adapter = ModuleConfigAdapter(this,
            allConfigs,
            selectedConfigs,
            { checkBox: CheckBox, config: ModuleConfig -> onConfigClicked(checkBox, config) },
            { checkBox: CheckBox, config: ModuleConfig -> onConfigHeld(checkBox, config) })

        configList.setAdapter(adapter)

        val next = findViewById<Button>(R.id.next_session_edit)
        next.setOnClickListener { onNext() }
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

    private fun onConfigHeld(checkBox: CheckBox, config: ModuleConfig): Boolean {
        val intent = Intent(this, ConfigEditor::class.java)
        intent.putExtra(Database.configID, config.getConfigID())
        startActivity(intent)
        return true
    }

    private fun onBack() {
        sessionName = sessionNameInput.text.toString()
        if (penaltyInput.text.toString() == "" || targetInput.text.toString() == "") {
            return
        }
        penalty = penaltyInput.text.toString().toInt()
        target = targetInput.text.toString().toInt()
        repeatable = repeatableInput.isChecked
        reset = resetInput.isChecked

        setContentView(R.layout.activity_session_module_selector)
        configList = findViewById(R.id.list_modules)
        sessionNameInput = findViewById(R.id.input_session_name)

        sessionNameInput.setText(sessionName)
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

        val adapter = ModuleConfigAdapter(this,
            allConfigs,
            selectedConfigs,
            { checkBox, config -> onConfigClicked(checkBox, config) },
            { checkBox, config -> onConfigHeld(checkBox, config) })
        configList.setAdapter(adapter)

        val next = findViewById<Button>(R.id.next_session_edit)
        next.setOnClickListener { onNext() }
    }

    private fun onNext() {
        if (configs.size == 0) {
            return
        }
        sessionName = sessionNameInput.text.toString()

        setContentView(R.layout.activity_session_settings)
        sessionNameInput = findViewById(R.id.input_session_name2)
        penaltyInput = findViewById(R.id.input_penalty)
        targetInput = findViewById(R.id.input_target)
        repeatableInput = findViewById(R.id.checkbox_repeatable)
        resetInput = findViewById(R.id.checkbox_reset)
        resetInput.setOnCheckedChangeListener { _, isChecked ->
            if (isChecked) {
                penaltyInput.isEnabled = false
                findViewById<TextView>(R.id.text_penalty).isEnabled = false
            } else {
                penaltyInput.isEnabled = true
                findViewById<TextView>(R.id.text_penalty).isEnabled = true
            }
        }

        sessionNameInput.setText(sessionName)
        penaltyInput.setText(penalty.toString())
        targetInput.setText(target.toString())
        repeatableInput.isChecked = repeatable
        resetInput.isChecked = reset

        val back = findViewById<Button>(R.id.back_session_edit)
        back.setOnClickListener { onBack() }
        val done = findViewById<Button>(R.id.done_session_edit)
        done.setOnClickListener { onDone() }
    }

    private fun onDone() {
        if (penaltyInput.text.toString() == "" || targetInput.text.toString() == "") {
            return
        }
        sessionName = sessionNameInput.text.toString()
        penalty = penaltyInput.text.toString().toInt()
        target = targetInput.text.toString().toInt()
        repeatable = repeatableInput.isChecked
        reset = resetInput.isChecked
        if (sessionName == "" || configs.size == 0) {
            return
        }

        if (targetInput.text.toString().toInt() < 1) {
            return
        }

        if (penaltyInput.text.toString().toInt() < 0) {
            return
        }

        if (targetInput.text.toString().toInt() <= penaltyInput.text.toString().toInt()) {
            return
        }

        if (targetInput.text.toString().toInt() <= session.getPoints()) {
            return
        }

        if (newSession) {
            database.saveSession(session)
        }

        session.setName(sessionNameInput.text.toString())
        session.setPointPenalty(penaltyInput.text.toString().toInt())
        session.setTargetPoints(targetInput.text.toString().toInt())
        session.setRepeatable(repeatableInput.isChecked)
        session.setReset(resetInput.isChecked)

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
