package com.example.trainerengine.configs

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Button
import com.example.trainerengine.R
import com.example.trainerengine.database.Database
import com.example.trainerengine.database.QueryHelper
import com.example.trainerengine.sessions.Session

class ConfigEditor : AppCompatActivity() {
    private lateinit var database: Database
    private lateinit var config: ModuleConfig
    private var newConfig = false



    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_config_editor)

        database = Database(QueryHelper(applicationContext))

        if (intent.hasExtra(Database.configID)) {

            val configId = intent.getIntExtra(Database.configID, -1)
            config = database.loadConfig(configId)
        } else {
            if (intent.hasExtra(Database.moduleID)) {
                val configId = database.makeNewConfigID()
                val moduleID = intent.getIntExtra(Database.moduleID, -1)
                config = ModuleConfig(configId, moduleID, database)
                newConfig = true
            } else {
                finish()
            }
        }

        val done = findViewById<Button>(R.id.done_config_edit)
        done.setOnClickListener { onDone() }
    }

    private fun onDone(){
        finish()
    }
}