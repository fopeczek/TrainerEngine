package com.example.trainerengine.database

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import com.example.trainerengine.ColumnType
import java.io.File

class QueryHelper(context: Context) {
    private var database: SQLiteDatabase

    init {
        val filename = "database.db"
        val dir: File = context.filesDir
        val file = File(dir, filename)
        if (!file.exists()) {
            file.createNewFile()
        }

        database = SQLiteDatabase.openOrCreateDatabase(file.absolutePath, null)
    }

    fun initializeTable(tableName: String, columns: MutableMap<String, ColumnType>) {
        val key = columns.keys.first()
        val type = columns.values.first()
        var sql = "CREATE TABLE IF NOT EXISTS $tableName ($key $type PRIMARY KEY,"
        for (i in 1 until columns.size) {
            sql += columns.keys.elementAt(i) + " " + columns.values.elementAt(i)
            if (i < columns.size - 1) {
                sql += ", "
            }
        }
        sql += ");"
        database.execSQL(sql)
    }

    fun initialize2KeyTable(tableName: String, columns: MutableMap<String, ColumnType>) {
        var sql = "CREATE TABLE IF NOT EXISTS $tableName ("
        for (i in 0 until columns.size) {
            sql += columns.keys.elementAt(i) + " " + columns.values.elementAt(i)
            sql += ", "
        }
        sql += "PRIMARY KEY (" + columns.keys.elementAt(0) + ", " + columns.keys.elementAt(1) + ")"
        sql += ");"
        database.execSQL(sql)
    }

    fun insertRow(tableName: String, values: Map<String, Any>) {
        var sql = "INSERT INTO $tableName ("
        for (i in 0 until values.size) {
            sql += values.keys.elementAt(i)
            if (i < values.size - 1) {
                sql += ", "
            }
        }
        sql += ") VALUES ("
        for (i in 0 until values.size) {
            val value = values.values.elementAt(i)
            if (value is String) {
                sql += "'$value'"
            } else {
                sql += value.toString()
            }
            if (i < values.size - 1) {
                sql += ", "
            }
        }
        sql += ");"
        database.execSQL(sql)
    }

    fun getRow(tableName: String, idName: String, id: Int): Map<String, Any> {
        val sql = "SELECT * FROM $tableName WHERE $idName = $id;"
        val cursor = database.rawQuery(sql, null)
        var result: Map<String, Any>? = null
        if (cursor.moveToFirst()) {
            result = fillMapFromCursor(cursor)
        }
        cursor.close()
        return result!!
    }

    fun getRow(tableName: String, data: Map<String, Any>): Map<String, Any>? {
        var sql = "SELECT * FROM $tableName WHERE "
        for (i in 0 until data.size) {
            sql += data.keys.elementAt(i) + " = "
            if (data.values.elementAt(i) is String) {
                sql += "'" + data.values.elementAt(i) + "'"
            } else {
                sql += data.values.elementAt(i)
            }
            if (i < data.size - 1) {
                sql += " AND "
            } else {
                sql += ";"
            }
        }
        val cursor = database.rawQuery(sql, null)
        var result: Map<String, Any>? = null
        if (cursor.moveToFirst()) {
            result = fillMapFromCursor(cursor)
        }
        cursor.close()
        return result
    }

    fun getAll(tableName: String, orderByTimestamp: Boolean = false): List<Map<String, Any>> {
        var sql = "SELECT * FROM $tableName"
        if (orderByTimestamp) {
            sql += " ORDER BY Timestamp;"
        } else {
            sql += ";"
        }
        val cursor = database.rawQuery(sql, null)
        val result = mutableListOf<Map<String, Any>>()
        if (cursor.moveToFirst()) {
            do {
                val row = fillMapFromCursor(cursor)
                result.add(row)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return result
    }

    fun getAllFiltered(
        tableName: String, filterName: String, filter: Any, orderByTimestamp: Boolean = false
    ): List<Map<String, Any>> {
        var sql = "SELECT * FROM $tableName WHERE $filterName = $filter"
        if (orderByTimestamp) {
            sql += " ORDER BY Timestamp;"
        } else {
            sql += ";"
        }
        val cursor = database.rawQuery(sql, null)
        val result = mutableListOf<Map<String, Any>>()
        if (cursor.moveToFirst()) {
            do {
                val row = fillMapFromCursor(cursor)
                result.add(row)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return result
    }

    fun getAllByIDFromTable(
        tableName: String, idTableName: String, bridgeIdName: String, idName: String, id: Int
    ): List<Map<String, Any>> {
        val sql =
            "SELECT * FROM $tableName INNER JOIN $idTableName ON $tableName.$bridgeIdName = $idTableName.$bridgeIdName WHERE $idTableName.$idName = $id ORDER BY Timestamp;"
        val cursor = database.rawQuery(sql, null)
        val result = mutableListOf<Map<String, Any>>()
        if (cursor.moveToFirst()) {
            do {
                val row = fillMapFromCursor(cursor)
                result.add(row)
            } while (cursor.moveToNext())
        }
        cursor.close()
        return result
    }

    fun updateRow(tableName: String, idName: String, id: Int, values: Map<String, Any>) {
        var sql = "UPDATE $tableName SET "
        for (i in 0 until values.size) {
            sql += values.keys.elementAt(i) + " = "
            val value = values.values.elementAt(i)
            if (value is String) {
                sql += "'$value'"
            } else {
                sql += value.toString()
            }
            if (i < values.size - 1) {
                sql += ", "
            }
        }
        sql += " WHERE $idName = $id;"
        database.execSQL(sql)
    }

    fun updateColumn(tableName: String, idName: String, id: Int, valueName: String, value: Any) {
        val sql = "UPDATE $tableName SET $valueName = $value WHERE $idName = $id;"
        database.execSQL(sql)
    }

    fun removeRow(tableName: String, idName: String, id: Int) {
        val sql = "DELETE FROM $tableName WHERE $idName = $id;"
        database.execSQL(sql)
    }

    fun makeNewID(tableName: String, idName: String): Int {
        val sql = "SELECT MAX($idName) FROM $tableName;"
        val cursor = database.rawQuery(sql, null)
        var result = 0
        if (cursor.moveToFirst()) {
            result = cursor.getInt(0) + 1
        }
        cursor.close()
        return result
    }

    private fun fillMapFromCursor(cursor: Cursor): Map<String, Any> {
        val row = mutableMapOf<String, Any>()
        for (i in 0 until cursor.columnCount) {
            when (cursor.getType(i)) {
                Cursor.FIELD_TYPE_NULL -> assert(false, { "Type is NULL" })
                Cursor.FIELD_TYPE_INTEGER -> row[cursor.getColumnName(i)] = cursor.getInt(i)
                Cursor.FIELD_TYPE_FLOAT -> row[cursor.getColumnName(i)] = cursor.getFloat(i)
                Cursor.FIELD_TYPE_STRING -> row[cursor.getColumnName(i)] = cursor.getString(i)
                Cursor.FIELD_TYPE_BLOB -> row[cursor.getColumnName(i)] = cursor.getBlob(i)
            }
        }
        return row
    }
}