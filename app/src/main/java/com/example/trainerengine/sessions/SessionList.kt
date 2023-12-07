package com.example.trainerengine.sessions

import android.Manifest
import android.content.Intent
import android.os.Bundle
import android.view.*
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import com.chaquo.python.Python
import com.chaquo.python.android.AndroidPlatform
import com.example.trainerengine.R
import com.example.trainerengine.configs.ConfigData
import com.example.trainerengine.configs.ModuleConfig
import com.example.trainerengine.database.Database
import com.example.trainerengine.database.QueryHelper
import com.example.trainerengine.globalModules
import com.example.trainerengine.modules.MathModule.MathModuleStub
import com.example.trainerengine.modules.PercentModule.PercentModuleStub
import com.example.trainerengine.modules.PythonMathModule.PythonMathModuleStub
import com.example.trainerengine.ranInit
import com.example.trainerengine.skills.Skill
import com.example.trainerengine.skills.SkillSet
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.moandjiezana.toml.Toml
import kotlin.system.exitProcess


class SessionList : AppCompatActivity() {
    private lateinit var database: Database

    private val selectedSessions = mutableListOf<Session>()

    private var databaseConflict = ""
    private var databaseConflictUseNew = {}

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

        if (!Python.isStarted()) {
            Python.start(AndroidPlatform(this))
        }

        database = Database(QueryHelper(applicationContext))

        if (!ranInit) {
            ranInit = true

            ActivityCompat.requestPermissions(this, arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 2)

            val loadedModules = database.loadModules()
            val toBeLoadedModules = mutableListOf(
                MathModuleStub().databaseName, PercentModuleStub().databaseName, PythonMathModuleStub().databaseName
            ) // list of must be loaded modules
            for (module in loadedModules) {
                globalModules[module.getModuleID()] = module
                toBeLoadedModules.remove(module.getStub().databaseName)
            }

            for (moduleName in toBeLoadedModules) { // load and save unsaved modules
                val moduleID = database.makeNewModuleID()
                when (moduleName) {
                    MathModuleStub().databaseName -> globalModules[moduleID] = MathModuleStub().createModule(moduleID)
                    PercentModuleStub().databaseName -> globalModules[moduleID] = PercentModuleStub().createModule(moduleID)
                    PythonMathModuleStub().databaseName -> globalModules[moduleID] = PythonMathModuleStub().createModule(moduleID)
                }
                database.saveModule(globalModules[moduleID]!!)
            }

            for (module in globalModules.values) { // load modules config and skills
                val configStream = assets.open("modules/${module.getStub().moduleDirectory}/config.toml")
                val toml = Toml().read(configStream)
                if (toml.getList<Toml>("settings") == null) {
                    continue
                }

                var configID = database.makeNewConfigID()
                val savedConfig = database.loadConfig(module.getModuleID(), "Default")
                if (savedConfig != null) {
                    configID = savedConfig.getConfigID()
                }
                var savedNewConfigData = false
                val config = ModuleConfig(configID, module.getModuleID(), database, "Default", mutableListOf())
                for (i in 0 until toml.getList<Toml>("settings").size) {
                    val setting = toml.getTable("settings[$i]")
                    val name = setting.getString(settingName)
                    val type = setting.getString(settingType)
                    var value: Any? = null
                    when (type) {
                        "int" -> value = setting.getLong(settingDefault).toInt()
                        "float" -> value = setting.getDouble(settingDefault).toFloat()
                        "string" -> value = setting.getString(settingDefault)
                        "bool" -> value = setting.getBoolean(settingDefault)
                    }
                    if (value != null) {
                        val configData = ConfigData(database.makeNewConfigDataID(), configID, name, type, value)
                        val savedConfigData = database.loadConfigData(configID, name)
                        if (savedConfigData == null) {
                            database.saveConfigData(configData)
                            config.addConfigData(configData)
                            savedNewConfigData = true
                        } else {
                            if (!(savedConfigData.getType() == configData.getType() && savedConfigData.getValue() == configData.getValue())) {
                                databaseConflict =
                                    "Config data \"${savedConfigData.getName()}\" from module \"${module.getStub().databaseName}\" already exists, old value is ${savedConfigData.getValue()}, new value = ${configData.getValue()}, what do you want to do?"
                                databaseConflictUseNew = {
                                    database.removeConfigData(savedConfigData.getConfigDataID())
                                    database.saveConfigData(configData)
                                }
                                break
                            }
                        }
                    } else {
                        throw Exception("ERROR in loading config value")
                    }
                }
                if (savedNewConfigData) {
                    if (savedConfig == null) {
                        database.saveConfig(config)
                    } else {
                        if (!savedConfig.contains(config)) {
                            Toast.makeText(
                                this, "WARNING: Config already exists, old config is prioritized over the new one", Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }

                val allSkills = module.getAllSkills()

                var skillSetID = database.makeNewSkillSetID()
                val savedSkillSet = database.loadSkillSet(module.getModuleID(), -1)
                if (savedSkillSet != null) {
                    skillSetID = savedSkillSet.getSkillSetID()
                }
                var savedNewSkill = false
                val skillSet = SkillSet(skillSetID, module.getModuleID(), -1, database, mutableListOf()) //sessionID = -1 is default
                for (skillData in allSkills) {
                    val skill = Skill(database.makeNewSkillID(), skillSetID, skillData.key, skillData.value, 0f, true, database)
                    val savedSkill = database.loadSkill(skillSetID, skillData.key)
                    if (savedSkill == null) {
                        database.saveSkill(skill)
                        skillSet.addSkill(skill)
                        savedNewSkill = true
                    } else {
                        if (!(savedSkill.getDescription() == skill.getDescription())) {
                            databaseConflict =
                                "Skill \"${savedSkill.getName()}\" from module \"${module.getStub().databaseName}\" already exists, old description is ${savedSkill.getDescription()}, new description = ${skill.getDescription()}, what do you want to do?"
                            databaseConflictUseNew = {
                                database.removeSkill(savedSkill.getSkillID())
                                database.saveSkill(skill)
                            }
                            break
                        }
                    }
                }
                if (savedNewSkill) {
                    if (savedSkillSet == null) {
                        database.saveSkillSet(skillSet)
                    } else {
                        if (!savedSkillSet.contains(skillSet)) {
                            Toast.makeText(
                                this, "WARNING: Skill set already exists, old set is prioritized over the new one", Toast.LENGTH_LONG
                            ).show()
                        }
                    }
                }
            }
        }


        val newSession = findViewById<FloatingActionButton>(R.id.session_list_button_add)
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
                    session.remove()
                }
                selectedSessions.clear()
                listSessions()
            }
        }
        return super.onOptionsItemSelected(item)
    }

    override fun onResume() {
        super.onResume()
        if (databaseConflict != "") { //TODO if there is more than one conflict, this will not work
            val newSession = findViewById<FloatingActionButton>(R.id.session_list_button_add)
            val inflater = getSystemService(LAYOUT_INFLATER_SERVICE) as LayoutInflater
            val popupView: View = inflater.inflate(R.layout.popup_database_conflict, null)
            val width = LinearLayout.LayoutParams.WRAP_CONTENT
            val height = LinearLayout.LayoutParams.WRAP_CONTENT
            val popupWindow = PopupWindow(popupView, width, height, false)
            newSession.viewTreeObserver.addOnGlobalLayoutListener {
                popupWindow.showAtLocation(newSession, Gravity.CENTER, 0, 0)
            }
            val conflictText = popupView.findViewById<TextView>(R.id.popup_database_conflict_text)
            conflictText.text = databaseConflict
            val closeButton = popupView.findViewById<Button>(R.id.popup_database_conflict_button_close)
            closeButton.setOnClickListener {
                finishAndRemoveTask()
                exitProcess(0)
            }
            val useOldButton = popupView.findViewById<Button>(R.id.popup_database_conflict_button_use_old)
            useOldButton.setOnClickListener {
                //Use old means do nothing for now
                popupWindow.dismiss()
            }
            val useNewButton = popupView.findViewById<Button>(R.id.popup_database_conflict_button_use_new)
            useNewButton.setOnClickListener {
                databaseConflictUseNew()
                popupWindow.dismiss()
            }
        }
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
        val sessionList = findViewById<LinearLayout>(R.id.session_list_list_session)
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
        val sessionList = findViewById<LinearLayout>(R.id.session_list_list_session)
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
