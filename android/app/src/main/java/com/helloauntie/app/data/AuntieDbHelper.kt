package com.helloauntie.app.data

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper

class AuntieDbHelper(context: Context) : SQLiteOpenHelper(
    context,
    "hello_auntie.db",
    null,
    DATABASE_VERSION,
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE person(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                name TEXT NOT NULL,
                gender TEXT NOT NULL DEFAULT 'U',
                birth_year INTEGER,
                birth_order INTEGER,
                is_self INTEGER NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE relation(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                from_id INTEGER NOT NULL,
                to_id INTEGER NOT NULL,
                type TEXT NOT NULL,
                sibling_age TEXT NOT NULL DEFAULT 'UNKNOWN',
                confidence REAL NOT NULL DEFAULT 1.0,
                UNIQUE(from_id, to_id, type)
            )
            """.trimIndent(),
        )
        db.execSQL(
            """
            CREATE TABLE face_sample(
                id INTEGER PRIMARY KEY AUTOINCREMENT,
                person_id INTEGER NOT NULL,
                embedding BLOB NOT NULL,
                quality REAL NOT NULL DEFAULT 0,
                created_at INTEGER NOT NULL
            )
            """.trimIndent(),
        )
        db.execSQL("CREATE INDEX idx_relation_from ON relation(from_id)")
        db.execSQL("CREATE INDEX idx_relation_to ON relation(to_id)")
        db.execSQL("CREATE INDEX idx_sample_person ON face_sample(person_id)")
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        // MVP: destructive upgrade. Replace with migrations before shipping.
        db.execSQL("DROP TABLE IF EXISTS face_sample")
        db.execSQL("DROP TABLE IF EXISTS relation")
        db.execSQL("DROP TABLE IF EXISTS person")
        onCreate(db)
    }

    companion object {
        private const val DATABASE_VERSION = 1
    }
}
