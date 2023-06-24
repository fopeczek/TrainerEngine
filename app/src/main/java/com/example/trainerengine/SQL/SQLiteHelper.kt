package com.example.trainerengine.SQL

import android.content.Context
import android.database.Cursor
import android.database.sqlite.SQLiteDatabase
import java.io.File


enum class ColumnType {
    INT, TEXT, BOOL, FLOAT, TIMESTAMP, BLOB
}

class SQLiteHelper(context: Context) {
    private var database: SQLiteDatabase

    init {
        val filename = "database.db"
        val dir: File = context.getFilesDir()
        val file = File(dir, filename)

        try {
            file.createNewFile()
        } catch (e: Exception) {
            e.printStackTrace()
        }
        database = SQLiteDatabase.openOrCreateDatabase(file.absolutePath, null)
    }

    fun initializeTable(tableName: String, columns: Map<String, ColumnType>, configTable : Boolean = false) {
        val key = columns.keys.first()
        val keyType = columns.values.first()
        var sql = "CREATE TABLE IF NOT EXISTS $tableName ($key $keyType PRIMARY KEY, "
        for (i in 1 until columns.size) {
            sql += columns.keys.elementAt(i) + " " + columns.values.elementAt(i)
            if (i < columns.size - 1) {
                sql += ", "
            }
        }
        sql += ");"
        database.execSQL(sql)
        if (configTable) {
            insertConfigRow(tableName, columns)
        }
    }

    private fun insertConfigRow(tableName: String, columns: Map<String, ColumnType>){
        var sql = "SELECT * FROM $tableName;"
        val cursor = database.rawQuery(sql, null)
        if (!cursor.moveToFirst()) {
            sql = "INSERT INTO $tableName ("
            for (i in 0 until columns.size) {
                sql += columns.keys.elementAt(i)
                if (i < columns.size - 1) {
                    sql += ", "
                }
            }
            sql += ") VALUES ("
            for (i in 0 until columns.size) {
                sql += "NULL"
                if (i < columns.size - 1) {
                    sql += ", "
                }
            }
            sql += ");"
            database.execSQL(sql)
        }
    }

    fun insertRow(tableName: String, values: Map<String, Any>): Int {
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
        return 0
    }

    fun getLastRow(tableName: String): Map<String, Any>? {
        val sql = "SELECT * FROM $tableName;"
        val cursor = database.rawQuery(sql, null)
        var result: Map<String, Any>? = null
        if (cursor.moveToLast()) {
            result = fillMapFromCursor(cursor)
        }
        cursor.close()
        return result
    }

    fun updateLastRow(tableName: String, values: Map<String, Any>){
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
    }

    fun getRow(tableName: String, idName: String, keyID: Int): Map<String, Any>? {
        val sql = "SELECT * FROM $tableName WHERE $idName = $keyID;"
        val cursor = database.rawQuery(sql, null)
        var result: Map<String, Any>? = null
        if (cursor.moveToFirst()) {
            result = fillMapFromCursor(cursor)
        }
        cursor.close()
        return result
    }

    fun getNewID(tableName: String, idName: String): Int {
        val sql = "SELECT MAX($idName) FROM $tableName;"
        val cursor = database.rawQuery(sql, null)
        var result = 0
        if (cursor.moveToFirst()) {
            result = cursor.getInt(0) + 1
        }
        cursor.close()
        return result
    }

    fun updateColumn(tableName: String, idName: String, id: Int, columnName: String, value: Any) {
        val sql = "UPDATE $tableName SET $columnName = $value WHERE $idName = $id;"
        database.execSQL(sql)
    }

    fun loadOrderedByTimestamp(tableName: String): List<Map<String, Any>> {
        val sql = "SELECT * FROM $tableName ORDER BY Timestamp;"
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

    fun loadByID(tableName: String, idName: String, id: Int): List<Map<String, Any>> {
        val sql = "SELECT * FROM $tableName WHERE $idName = $id ORDER BY Timestamp;"
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

    fun loadByIDFromTable(
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

    fun removeRow(tableName: String, idName: String, id: Int) {
        val sql = "DELETE FROM $tableName WHERE $idName = $id;"
        database.execSQL(sql)
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
}