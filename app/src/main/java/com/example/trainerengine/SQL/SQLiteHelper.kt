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

    fun initialize2KeyTable(tableName: String, columns: MutableMap<String, ColumnType>){
        var sql = "CREATE TABLE IF NOT EXISTS $tableName ("
        for (i in 0 until columns.size) {
            sql += columns.keys.elementAt(i) + " " + columns.values.elementAt(i)
            sql += ", "
        }
        sql += "PRIMARY KEY (" + columns.keys.elementAt(0) + ", " + columns.keys.elementAt(1) + ")"
        sql += ");"
        database.execSQL(sql)
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

    fun set2KeyRow(tableName: String, values: MutableMap<String, Any>) { // TODO when updating, remove old with different key2
        val key1 = values.keys.first()
        val value1 = values.values.first()
        val key2 = values.keys.elementAt(1)
        val value2 = values.values.elementAt(1)
        var sql = "SELECT * FROM $tableName WHERE $key1 = $value1 AND $key2 = '$value2';"
        val cursor = database.rawQuery(sql, null)

        if (cursor.moveToFirst()) {
            sql = "UPDATE $tableName SET "
            for (i in 2 until values.size) {
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
            sql += " WHERE $key1 = $value1 AND $key2 = '$value2';"
            database.execSQL(sql)
        } else {
            sql = "INSERT INTO $tableName ("
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

    fun getAllByID(tableName: String, idName: String, id: Int): List<Map<String, Any>> {
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