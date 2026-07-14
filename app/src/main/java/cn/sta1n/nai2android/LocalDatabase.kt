package cn.sta1n.nai2android

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.database.sqlite.SQLiteOpenHelper
import java.util.UUID

class LocalDatabase(context: Context) : SQLiteOpenHelper(
    context,
    DATABASE_NAME,
    null,
    DATABASE_VERSION
) {
    override fun onCreate(db: SQLiteDatabase) {
        db.execSQL(
            """
            CREATE TABLE image_records (
                id TEXT PRIMARY KEY NOT NULL,
                local_uri TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                prompt TEXT NOT NULL,
                archive_tags TEXT NOT NULL,
                artist TEXT NOT NULL,
                negative_prompt TEXT NOT NULL,
                preset_name TEXT NOT NULL,
                favorite INTEGER NOT NULL DEFAULT 0,
                saved_to_device INTEGER NOT NULL DEFAULT 0,
                export_count INTEGER NOT NULL DEFAULT 0,
                job_id TEXT NOT NULL DEFAULT '',
                model TEXT NOT NULL DEFAULT '',
                size TEXT NOT NULL DEFAULT '',
                cost INTEGER NOT NULL DEFAULT 0,
                steps INTEGER NOT NULL DEFAULT 0,
                scale REAL NOT NULL DEFAULT 0,
                cfg REAL NOT NULL DEFAULT 0,
                sampler TEXT NOT NULL DEFAULT '',
                noise_schedule TEXT NOT NULL DEFAULT '',
                no_cache INTEGER NOT NULL DEFAULT 1
            )
            """.trimIndent()
        )
        db.execSQL(
            """
            CREATE TABLE presets (
                id TEXT PRIMARY KEY NOT NULL,
                name TEXT NOT NULL,
                tag TEXT NOT NULL,
                artist TEXT NOT NULL,
                negative_prompt TEXT NOT NULL,
                created_at INTEGER NOT NULL,
                updated_at INTEGER NOT NULL
            )
            """.trimIndent()
        )
    }

    override fun onUpgrade(db: SQLiteDatabase, oldVersion: Int, newVersion: Int) {
        if (oldVersion < 2) {
            db.execSQL(
                "ALTER TABLE image_records ADD COLUMN saved_to_device INTEGER NOT NULL DEFAULT 0"
            )
        }
        if (oldVersion < 3) {
            db.execSQL("ALTER TABLE image_records ADD COLUMN export_count INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE image_records ADD COLUMN job_id TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE image_records ADD COLUMN model TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE image_records ADD COLUMN size TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE image_records ADD COLUMN cost INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE image_records ADD COLUMN steps INTEGER NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE image_records ADD COLUMN scale REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE image_records ADD COLUMN cfg REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE image_records ADD COLUMN sampler TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE image_records ADD COLUMN noise_schedule TEXT NOT NULL DEFAULT ''")
            db.execSQL("ALTER TABLE image_records ADD COLUMN no_cache INTEGER NOT NULL DEFAULT 1")
            db.execSQL(
                "UPDATE image_records SET export_count = 1 " +
                    "WHERE saved_to_device = 1 OR local_uri LIKE 'content://%'"
            )
        }
    }

    fun listImages(): List<ImageRecord> {
        val result = mutableListOf<ImageRecord>()
        readableDatabase.query(
            "image_records",
            null,
            null,
            null,
            null,
            null,
            "created_at DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) result += cursor.toImageRecord()
        }
        return result
    }

    fun insertImage(record: ImageRecord) {
        writableDatabase.insertWithOnConflict(
            "image_records",
            null,
            record.toContentValues(),
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun setFavorite(id: String, favorite: Boolean) {
        writableDatabase.update(
            "image_records",
            ContentValues().apply { put("favorite", if (favorite) 1 else 0) },
            "id = ?",
            arrayOf(id)
        )
    }

    fun recordExport(id: String) {
        writableDatabase.execSQL(
            "UPDATE image_records SET saved_to_device = 1, export_count = export_count + 1 WHERE id = ?",
            arrayOf(id)
        )
    }

    fun recordExports(ids: Set<String>) {
        if (ids.isEmpty()) return
        writableDatabase.beginTransaction()
        try {
            ids.forEach(::recordExport)
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
    }

    fun deleteImages(ids: Set<String>) {
        if (ids.isEmpty()) return
        writableDatabase.beginTransaction()
        try {
            ids.forEach { id ->
                writableDatabase.delete("image_records", "id = ?", arrayOf(id))
            }
            writableDatabase.setTransactionSuccessful()
        } finally {
            writableDatabase.endTransaction()
        }
    }

    fun updateArchiveTags(id: String, tags: List<String>) {
        writableDatabase.update(
            "image_records",
            ContentValues().apply { put("archive_tags", encodeTags(tags)) },
            "id = ?",
            arrayOf(id)
        )
    }

    fun listPresets(): List<Preset> {
        val result = mutableListOf<Preset>()
        readableDatabase.query(
            "presets",
            null,
            null,
            null,
            null,
            null,
            "updated_at DESC"
        ).use { cursor ->
            while (cursor.moveToNext()) result += cursor.toPreset()
        }
        return result
    }

    fun upsertPreset(preset: Preset) {
        writableDatabase.insertWithOnConflict(
            "presets",
            null,
            preset.toContentValues(),
            SQLiteDatabase.CONFLICT_REPLACE
        )
    }

    fun deletePreset(id: String) {
        writableDatabase.delete("presets", "id = ?", arrayOf(id))
    }

    fun closeDatabase() {
        close()
    }

    private fun android.database.Cursor.toImageRecord(): ImageRecord {
        return ImageRecord(
            id = getString(getColumnIndexOrThrow("id")),
            localUri = getString(getColumnIndexOrThrow("local_uri")),
            createdAt = getLong(getColumnIndexOrThrow("created_at")),
            prompt = getString(getColumnIndexOrThrow("prompt")),
            archiveTags = decodeTags(getString(getColumnIndexOrThrow("archive_tags"))),
            artist = getString(getColumnIndexOrThrow("artist")),
            negativePrompt = getString(getColumnIndexOrThrow("negative_prompt")),
            presetName = getString(getColumnIndexOrThrow("preset_name")),
            favorite = getInt(getColumnIndexOrThrow("favorite")) != 0,
            savedToDevice = getInt(getColumnIndexOrThrow("saved_to_device")) != 0,
            exportCount = getInt(getColumnIndexOrThrow("export_count")),
            parameters = GenerationParameters(
                jobId = getString(getColumnIndexOrThrow("job_id")),
                model = getString(getColumnIndexOrThrow("model")),
                size = getString(getColumnIndexOrThrow("size")),
                cost = getInt(getColumnIndexOrThrow("cost")),
                steps = getInt(getColumnIndexOrThrow("steps")),
                scale = getDouble(getColumnIndexOrThrow("scale")),
                cfg = getDouble(getColumnIndexOrThrow("cfg")),
                sampler = getString(getColumnIndexOrThrow("sampler")),
                noiseSchedule = getString(getColumnIndexOrThrow("noise_schedule")),
                noCache = getInt(getColumnIndexOrThrow("no_cache")) != 0
            )
        )
    }

    private fun android.database.Cursor.toPreset(): Preset {
        return Preset(
            id = getString(getColumnIndexOrThrow("id")),
            name = getString(getColumnIndexOrThrow("name")),
            tag = getString(getColumnIndexOrThrow("tag")),
            artist = getString(getColumnIndexOrThrow("artist")),
            negativePrompt = getString(getColumnIndexOrThrow("negative_prompt")),
            createdAt = getLong(getColumnIndexOrThrow("created_at")),
            updatedAt = getLong(getColumnIndexOrThrow("updated_at"))
        )
    }

    private fun ImageRecord.toContentValues() = ContentValues().apply {
        put("id", id)
        put("local_uri", localUri)
        put("created_at", createdAt)
        put("prompt", prompt)
        put("archive_tags", encodeTags(archiveTags))
        put("artist", artist)
        put("negative_prompt", negativePrompt)
        put("preset_name", presetName)
        put("favorite", if (favorite) 1 else 0)
        put("saved_to_device", if (savedToDevice) 1 else 0)
        put("export_count", exportCount)
        put("job_id", parameters.jobId)
        put("model", parameters.model)
        put("size", parameters.size)
        put("cost", parameters.cost)
        put("steps", parameters.steps)
        put("scale", parameters.scale)
        put("cfg", parameters.cfg)
        put("sampler", parameters.sampler)
        put("noise_schedule", parameters.noiseSchedule)
        put("no_cache", if (parameters.noCache) 1 else 0)
    }

    private fun Preset.toContentValues() = ContentValues().apply {
        put("id", id)
        put("name", name)
        put("tag", tag)
        put("artist", artist)
        put("negative_prompt", negativePrompt)
        put("created_at", createdAt)
        put("updated_at", updatedAt)
    }

    private companion object {
        const val DATABASE_NAME = "nai2android.db"
        const val DATABASE_VERSION = 3
        const val TAG_SEPARATOR = "\u001F"

        fun encodeTags(tags: List<String>): String = tags.joinToString(TAG_SEPARATOR)

        fun decodeTags(value: String): List<String> = value
            .split(TAG_SEPARATOR)
            .map(String::trim)
            .filter(String::isNotEmpty)

        @Suppress("unused")
        fun newId(): String = UUID.randomUUID().toString()
    }
}

