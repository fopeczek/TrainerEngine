package com.example.trainerengine.session

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
import com.example.trainerengine.SQL.GlobalSQLiteManager
import com.example.trainerengine.SQL.SQLiteHelper
import com.example.trainerengine.Session
import com.example.trainerengine.getTimestamp
import com.example.trainerengine.globalModules
import com.example.trainerengine.modules.MathModule.MathModuleStub
import com.example.trainerengine.modules.PercentModule.PercentModuleStub
import com.example.trainerengine.modules.PythonMathModule.PythonMathModuleStub
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.moandjiezana.toml.Toml
import java.io.File

class SessionList : AppCompatActivity() {
    private lateinit var sqLiteHelper: SQLiteHelper
    private lateinit var database: GlobalSQLiteManager

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

        sqLiteHelper = SQLiteHelper(applicationContext)
        database = GlobalSQLiteManager(sqLiteHelper)

        val savedModules = database.getModules()
        val toBeLoadedModules = mutableListOf("Math", "PythonMath", "Percent")
        globalModules.clear() // TODO: This is a hack to prevent modules from being added twice
        for (module in savedModules) { // load saved modules
            when (module[GlobalSQLiteManager.moduleName]) {
                "Math" -> {
                    globalModules.add(MathModuleStub().createModule(module[GlobalSQLiteManager.moduleID] as Int))
                    toBeLoadedModules.remove("Math")
                }

                "Percent" -> {
                    globalModules.add(PercentModuleStub().createModule(module[GlobalSQLiteManager.moduleID] as Int))
                    toBeLoadedModules.remove("Percent")
                }

                "PythonMath" -> {
                    globalModules.add(PythonMathModuleStub().createModule(module[GlobalSQLiteManager.moduleID] as Int))
                    toBeLoadedModules.remove("PythonMath")
                }
            }
        }

        for (moduleName in toBeLoadedModules) { // load remaining modules
            when (moduleName) {
                "Math" -> {
                    val moduleID = database.getNewModuleID()
                    val module = mapOf(
                        GlobalSQLiteManager.moduleID to moduleID,
                        GlobalSQLiteManager.moduleName to moduleName,
                        GlobalSQLiteManager.timestamp to getTimestamp()
                    )
                    database.saveModule(module)
                    globalModules.add(MathModuleStub().createModule(moduleID))
                }

                "Percent" -> {
                    val moduleID = database.getNewModuleID()
                    val module = mapOf(
                        GlobalSQLiteManager.moduleID to moduleID,
                        GlobalSQLiteManager.moduleName to moduleName,
                        GlobalSQLiteManager.timestamp to getTimestamp()
                    )
                    database.saveModule(module)
                    globalModules.add(PercentModuleStub().createModule(moduleID))
                }

                "PythonMath" -> {
                    val moduleID = database.getNewModuleID()
                    val module = mapOf(
                        GlobalSQLiteManager.moduleID to moduleID,
                        GlobalSQLiteManager.moduleName to moduleName,
                        GlobalSQLiteManager.timestamp to getTimestamp()
                    )
                    database.saveModule(module)
                    globalModules.add(PythonMathModuleStub().createModule(moduleID))
                }
            }
        }

        val modulesDir = File(filesDir, "modules")
        if (!modulesDir.exists()) {
            modulesDir.mkdir()
        }

        for (module in globalModules) { // load modules config
            val moduleDir = File(modulesDir, module.getStub().moduleDirectory)
            if (!moduleDir.exists()) {
                moduleDir.mkdir()
            }
            val configFile = File(moduleDir, "config.toml")
            if (!configFile.exists()) {
                configFile.createNewFile()
                continue
            }
            val settings = mutableListOf<Triple<String, String, Any>>()
            val toml = Toml().read(configFile)
            if (toml.getList<Toml>("settings") == null) {
                continue
            }
            for (i in 0 until toml.getList<Toml>("settings").size) {
                val setting = toml.getTable("settings[$i]")
                if (setting.getString("type") == "int") {
                    settings.add(
                        Triple(
                            setting.getString(settingName),
                            setting.getString(settingType),
                            setting.getLong(settingDefault)
                        )
                    )
                } else if (setting.getString("type") == "float") {
                    settings.add(
                        Triple(
                            setting.getString(settingName),
                            setting.getString(settingType),
                            setting.getDouble(settingDefault)
                        )
                    )
                } else if (setting.getString("type") == "string") {
                    settings.add(
                        Triple(
                            setting.getString(settingName),
                            setting.getString(settingType),
                            setting.getString(settingDefault)
                        )
                    )
                } else if (setting.getString("type") == "bool") {
                    settings.add(
                        Triple(
                            setting.getString(settingName),
                            setting.getString(settingType),
                            setting.getBoolean(settingDefault)
                        )
                    )
                }
            }
            if (settings.size == 0) {
                continue
            }
            database.initializeModuleConfigTables(module.getModuleID(), settings)
        }

        // INIT

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
            if (session.isFinished()) {
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