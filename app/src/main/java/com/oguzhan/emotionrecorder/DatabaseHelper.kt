package com.oguzhan.emotionrecorder

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

/**
 * SQLite Database Helper for storing emotion data.
 * Written by Burak.
 */
class DatabaseHelper(
    context: Context?,
    factory: SQLiteDatabase.CursorFactory?
) : SQLiteOpenHelper(context, DATABASE_NAME, factory, 1) {

    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            "create table $TABLE_NAME ($ID INT PRIMARY KEY AUTOINCREMENT," +
                    "$APP_NAME STRING, $HAPPINESS FLOAT, $SADNESS FLOAT, $ANGER FLOAT, $NEUTRAL FLOAT," +
                    " $DISGUST FLOAT, $SCARED FLOAT, $SURPRISED FLOAT, $TIME_STAMP FLOAT)"
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        db.execSQL("DROP TABLE IF EXISTS $TABLE_NAME")
        onCreate(db)
    }

    fun insertEmotions(app_name: String, emotion: HashMap<String, Float>, weight: Float, time: Float): Boolean {
        val db = this.writableDatabase
        val contentValues = ContentValues()
        contentValues.put(APP_NAME, app_name)
        val emotions = arrayOf(
            HAPPINESS,
            SADNESS,
            ANGER,
            NEUTRAL,
            DISGUST,
            SCARED,
            SURPRISED,
            TIME_STAMP
        )
        for (s in emotions) {
            if (s in emotion) {
                contentValues.put(s, weight)
                contentValues.put(TIME_STAMP, time)
            }
        }
        val result = db.insert(TABLE_NAME, null, contentValues)
        return if (result.toInt() == -1) false else true
    }

    companion object {
        val DATABASE_NAME = "emotions_database.db"
        val TABLE_NAME = "EmotionTable"
        val APP_NAME = "Application Name"
        val ID = "ID"
        val HAPPINESS = "HAPPINESS"
        val SADNESS = "SADNESS"
        val ANGER = "ANGER"
        val NEUTRAL = "NEUTRAL"
        val DISGUST = "DISGUST"
        val SCARED = "SCARED"
        val SURPRISED = "SURPRISED"
        val TIME_STAMP = "TIME_STAMP"
    }
}