package com.example.trainerengine.module

import androidx.appcompat.app.AppCompatActivity
import android.os.Bundle
import android.widget.Toast
import com.example.trainerengine.R
import com.example.trainerengine.SQL.GlobalSQLiteManager
import com.example.trainerengine.globalModules

class ModuleEditor : AppCompatActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_module_editor)

        val moduleID = intent.getIntExtra(GlobalSQLiteManager.moduleID, -1)
        val module: Module? = globalModules[moduleID]
        if (module == null){
            Toast.makeText(this, "Module ID not found", Toast.LENGTH_SHORT).show() //TODO assert?
            finish()
        }


    }
}