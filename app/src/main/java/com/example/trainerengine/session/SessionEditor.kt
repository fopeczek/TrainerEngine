package com.example.trainerengine.session

import android.os.Bundle
import android.view.View
import android.widget.*
import androidx.appcompat.app.AppCompatActivity
import com.example.trainerengine.R
import com.example.trainerengine.SQL.GlobalSQLiteManager
import com.example.trainerengine.SQL.SQLiteHelper
import com.example.trainerengine.Session
import com.example.trainerengine.getModule
import com.example.trainerengine.globalModules
import com.example.trainerengine.module.ModuleConfig


class SessionEditor : AppCompatActivity() {
    private lateinit var sqLiteHelper: SQLiteHelper
    private lateinit var database: GlobalSQLiteManager
    private lateinit var session: Session
    private var configIDs = mutableListOf<Int>()
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

        sqLiteHelper = SQLiteHelper(applicationContext)
        database = GlobalSQLiteManager(sqLiteHelper)

        if (intent.hasExtra(GlobalSQLiteManager.sessionID)) {
            val sessionID = intent.getIntExtra(GlobalSQLiteManager.sessionID, -1)
            session = database.loadSession(sessionID)
        } else {
            val sessionID = database.makeNewSessionID()
            session = Session(sessionID, database)
            newSession = true
        }

        moduleList = findViewById<ExpandableListView>(R.id.list_modules)
        sessionName = findViewById(R.id.input_session_name)
        penalty = findViewById(R.id.input_penalty)
        target = findViewById(R.id.input_target)
        repeatable = findViewById(R.id.checkbox_repeatable)
        reset = findViewById(R.id.checkbox_reset)

        val detail = HashMap<String, List<String>>()
        for (module in globalModules.values) {
            val list = ArrayList<String>()
            for (config in database.loadConfigs(module.getModuleID())) {
                list.add(config.getName())
            }
            detail[module.getStub().descriptionName] = list
        }
        val titleList = ArrayList(detail.keys)
        val adapter = ModuleConfigAdapter(this, titleList, detail, { groupPosition, childPosition, checkBox ->
            onConfigClicked(groupPosition, childPosition, checkBox)})
        moduleList.setAdapter(adapter)

        sessionName.setText(session.getName())
        penalty.setText(session.getPointPenalty().toString())
        target.setText(session.getTargetPoints().toString())
        repeatable.isChecked = session.getRepeatable()
        reset.isChecked = session.getReset()


        val done = findViewById<Button>(R.id.done_session_create)
        done.setOnClickListener { onDone() }
    }

    private fun onConfigClicked(groupPosition: Int, childPosition: Int, checkBox: CheckBox) {
        val module = getModule(moduleList.expandableListAdapter.getGroup(groupPosition).toString())
        val configs = database.loadConfigs(module.getModuleID())
        var config: ModuleConfig? = null
        for (conf in configs) {
            if (conf.getName() == moduleList.expandableListAdapter.getChild(groupPosition, childPosition).toString()) {
                config = conf
                break
            }
        }
        if (checkBox.isChecked) {
            configIDs.add(config!!.getConfigID())
        } else {
            configIDs.remove(config!!.getConfigID())
        }
    }

    private fun onDone() {
        if (sessionName.text.toString() == "" || configIDs.size == 0 || penalty.text.toString() == "" || target.text.toString() == "") {
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
        session.setConfigIDs(configIDs)

        finish()
    }
}
