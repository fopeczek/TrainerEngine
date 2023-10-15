package com.example.trainerengine.sessions

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.Menu
import android.view.MenuItem
import android.view.Window
import android.widget.CheckBox
import android.widget.LinearLayout
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.trainerengine.R
import com.example.trainerengine.database.Database
import com.example.trainerengine.database.QueryHelper
import com.example.trainerengine.globalModules
import com.example.trainerengine.configs.ConfigData
import com.example.trainerengine.configs.ModuleConfig
import com.example.trainerengine.modules.MathModule.MathModuleStub
import com.example.trainerengine.modules.PercentModule.PercentModuleStub
import com.example.trainerengine.modules.PythonMathModule.PythonMathModuleStub
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.moandjiezana.toml.Toml

class SessionList : AppCompatActivity() {
    private lateinit var database: Database

    private val selectedSessions = mutableListOf<Session>()

    companion object {
        const val settingName = "text"
        const val settingType = "type"
        const val settingDefault = "default"
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        window.requestFeature(Window.FEATURE_ACTION_BAR)
        setContentView(R.layout.activity_session_list)
        supportActionBar?.title = "Session List"

        // INIT

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 2)

        database = Database(QueryHelper(applicationContext))

        val loadedModules = database.loadModules()
        val toBeLoadedModules = mutableListOf(
            MathModuleStub().databaseName, PercentModuleStub().databaseName, PythonMathModuleStub().databaseName
        ) // list of must be loaded modules
        globalModules.clear() // TODO: This is a hack to prevent modules from being added twice
        for (module in loadedModules) {
            globalModules[module.getModuleID()] = module
            toBeLoadedModules.remove(module.getStub().databaseName)
        }

        for (moduleName in toBeLoadedModules) { // load and save unsaved modules
            val moduleID = database.makeNewModuleID()
            when (moduleName) {
                MathModuleStub().databaseName -> {
                    globalModules[moduleID] = MathModuleStub().createModule(moduleID)
                }

                PercentModuleStub().databaseName -> {
                    globalModules[moduleID] = PercentModuleStub().createModule(moduleID)
                }

                PythonMathModuleStub().databaseName -> {
                    globalModules[moduleID] = PythonMathModuleStub().createModule(moduleID)
                }
            }
            database.saveModule(globalModules[moduleID]!!)
        }

        for (module in globalModules.values) { // load modules config
            val configStream = assets.open("modules/${module.getStub().moduleDirectory}/config.toml")
            val toml = Toml().read(configStream)
            if (toml.getList<Toml>("settings") == null) {
                continue
            }

            var configID = database.makeNewConfigID()
            val defaultConfig = database.loadConfig(module.getModuleID(), "Default")
            if (defaultConfig != null) {
                configID = defaultConfig.getConfigID()
            }
            var savedNewConfigData = false
            val config = ModuleConfig(configID, module.getModuleID(), database, "Default", mutableListOf())
            for (i in 0 until toml.getList<Toml>("settings").size) {
                val setting = toml.getTable("settings[$i]")
                var configData: ConfigData? = null
                if (setting.getString("type") == "int") {
                    configData = ConfigData(
                        configID, setting.getString(settingName), setting.getString(settingType), setting.getLong(settingDefault)
                    )
                } else if (setting.getString("type") == "float") {
                    configData = ConfigData(
                        configID, setting.getString(settingName), setting.getString(settingType), setting.getDouble(settingDefault)
                    )
                } else if (setting.getString("type") == "string") {
                    configData = ConfigData(
                        configID, setting.getString(settingName), setting.getString(settingType), setting.getString(settingDefault)
                    )
                } else if (setting.getString("type") == "bool") {
                    configData = ConfigData(
                        configID, setting.getString(settingName), setting.getString(settingType), setting.getBoolean(settingDefault)
                    )
                }
                if (configData != null) {
                    if (!database.isConfigDataSaved(configData)) {
                        database.saveConfigData(configData)
                        config.addConfigData(configData)
                        savedNewConfigData = true
                    }
                }
            }
            if (savedNewConfigData) {
                if (defaultConfig == null) {
                    database.saveConfig(config)
                }
            }
        }

        // END - INIT


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

    private fun editListSessions() {
        val sessionList = findViewById<LinearLayout>(R.id.session_list)
        sessionList.removeAllViews()
        invalidateOptionsMenu()
        for (session in database.loadSessions()) {
            val sessionCheck = CheckBox(this)
            sessionCheck.text = session.getName()
            sessionCheck.textSize = 22f
            if (session.isFinished()) {
                sessionCheck.setTextColor(getColor(R.color.green))
            }
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
        for (session in database.loadSessions()) {
            val sessionText = TextView(this)
            sessionText.text = session.getName()
            sessionText.textSize = 22f
            if (session.isFinished()) {
                sessionText.setTextColor(getColor(R.color.green))
            }
            sessionText.setOnClickListener {
                val intent = Intent(this, SessionViewer::class.java)
                intent.putExtra(Database.sessionID, session.getSessionID())
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
