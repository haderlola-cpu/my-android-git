package com.example.data

import android.content.ContentValues
import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.room.testing.MigrationTestHelper
import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.test.core.app.ApplicationProvider
import androidx.test.platform.app.InstrumentationRegistry
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

/**
 * Room migration 2 -> 3 (cloudKey). Uses schemas/com.example.data.AppDatabase/2.json (reconstructed by hand,
 * see schemas/README.txt) and the Room-generated 3.json.
 *
 * NOTE: written for the Gradle build; it was NOT executed in the environment where it was authored
 * (no Gradle / Android SDK there). The same migration SQL was verified separately against SQLite.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class AppDatabaseMigrationTest {

    private val dbName = "migration-test.db"
    private val uuidV4 = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")

    @get:Rule
    val helper = MigrationTestHelper(
        InstrumentationRegistry.getInstrumentation(),
        AppDatabase::class.java
    )

    private fun insertEventV2(db: SupportSQLiteDatabase, id: Long, name: String) {
        val v = ContentValues().apply {
            put("id", id); put("name", name); put("date", "2026-11-20")
            put("timeFrom", "18:00"); put("timeTo", "23:00")
            put("backgroundType", "PRESET"); put("backgroundPresetId", 1)
            put("backgroundAspectRatio", 0.714)
            put("nameColorHex", "#FFFDF6E2"); put("qrColorHex", "#FF121826"); put("qrBgColorHex", "#FFFFFFFF")
            put("fontIndex", 0)
            put("showGuestName", 1); put("showDate", 1); put("showTime", 1)
            put("qrSizeRatio", 0.28); put("nameSizeSp", 22.0)
            put("qrPosX", 0.5); put("qrPosY", 0.48); put("namePosX", 0.5); put("namePosY", 0.76)
            put("datePosX", 0.5); put("datePosY", 0.88)
            put("whatsappTemplate", "template"); put("createdAt", 1_700_000_000_000L + id)
            put("groomName", ""); put("brideName", ""); put("headerVerse", ""); put("hostsText", "")
            put("dayName", ""); put("venueName", ""); put("venueAddress", ""); put("locationUrl", "")
            put("showLocationQr", 1); put("notesText", ""); put("closingText", "")
        }
        db.insert("events", SQLiteDatabase.CONFLICT_NONE, v)
    }

    private fun insertGuestV2(
        db: SupportSQLiteDatabase, id: Long, eventId: Long, code: String,
        attended: Boolean, attendedAt: Long?, whatsappSent: Boolean
    ) {
        val v = ContentValues().apply {
            put("id", id); put("eventId", eventId); put("name", "ضيف $id"); put("normalizedName", "ضيف $id")
            put("phone", ""); put("normalizedPhone", ""); put("code", code); put("sequenceNumber", id.toInt())
            put("attended", if (attended) 1 else 0)
            if (attendedAt != null) put("attendedAt", attendedAt)
            put("whatsappSent", if (whatsappSent) 1 else 0)
            put("createdAt", 1_700_000_000_000L + id)
        }
        db.insert("guests", SQLiteDatabase.CONFLICT_NONE, v)
    }

    @Test
    fun migrate2To3_keepsAllData_andBackfillsStableUuids() {
        helper.createDatabase(dbName, 2).use { db ->
            insertEventV2(db, 1, "حفل أ")
            insertEventV2(db, 5, "حفل ب") // non-contiguous local id on purpose
            insertGuestV2(db, 10, 1, "11111111", attended = true, attendedAt = 1_700_000_999_000L, whatsappSent = true)
            insertGuestV2(db, 11, 1, "22222222", attended = false, attendedAt = null, whatsappSent = false)
            insertGuestV2(db, 20, 5, "33333333", attended = false, attendedAt = null, whatsappSent = true)
            insertGuestV2(db, 21, 5, "44444444", attended = true, attendedAt = 1_700_000_888_000L, whatsappSent = false)
        }

        helper.runMigrationsAndValidate(dbName, 3, true, MIGRATION_2_3).use { db ->
            db.query("SELECT id, name, cloudKey FROM events ORDER BY id").use { c ->
                assertEquals(2, c.count)
                val keys = mutableListOf<String>()
                while (c.moveToNext()) {
                    keys.add(c.getString(2))
                }
                assertTrue(keys.all { uuidV4.matches(it) })
                assertEquals(2, keys.toSet().size)
            }
            db.query("SELECT id, eventId, code, attended, attendedAt, whatsappSent, cloudKey FROM guests ORDER BY id").use { c ->
                assertEquals(4, c.count)
                val keys = mutableListOf<String>()
                val rows = mutableListOf<List<Any?>>()
                while (c.moveToNext()) {
                    rows.add(listOf(c.getLong(0), c.getLong(1), c.getString(2), c.getInt(3), if (c.isNull(4)) null else c.getLong(4), c.getInt(5)))
                    keys.add(c.getString(6))
                }
                assertEquals(
                    listOf(
                        listOf<Any?>(10L, 1L, "11111111", 1, 1_700_000_999_000L, 1),
                        listOf<Any?>(11L, 1L, "22222222", 0, null, 0),
                        listOf<Any?>(20L, 5L, "33333333", 0, null, 1),
                        listOf<Any?>(21L, 5L, "44444444", 1, 1_700_000_888_000L, 0)
                    ),
                    rows
                )
                assertTrue(keys.all { uuidV4.matches(it) })
                assertEquals(4, keys.toSet().size)
            }
        }
    }

    @Test
    fun migratedDatabase_opensThroughRoom_withoutRegeneratingKeys_andKeepsRelations() = runBlocking {
        helper.createDatabase(dbName, 2).use { db ->
            insertEventV2(db, 1, "حفل")
            insertGuestV2(db, 10, 1, "11111111", attended = true, attendedAt = 123L, whatsappSent = false)
            insertGuestV2(db, 11, 1, "22222222", attended = false, attendedAt = null, whatsappSent = false)
        }
        helper.runMigrationsAndValidate(dbName, 3, true, MIGRATION_2_3).close()

        val context = ApplicationProvider.getApplicationContext<Context>()
        fun open() = Room.databaseBuilder(context, AppDatabase::class.java, dbName)
            .addMigrations(MIGRATION_2_3)
            .allowMainThreadQueries()
            .build()

        val first = open()
        val events1 = first.eventDao().getAllEventsOnce()
        val guests1 = first.eventDao().getAllGuestsOnce()
        first.close()

        val second = open() // re-open: must not regenerate anything
        val events2 = second.eventDao().getAllEventsOnce()
        val guests2 = second.eventDao().getAllGuestsOnce()
        // The event can still be updated without touching its guests (no REPLACE / CASCADE side effect).
        second.eventDao().updateEvent(events2.first().copy(name = "حفل معدّل"))
        val guestsAfterUpdate = second.eventDao().getGuestsForEventOnce(1L)
        second.close()

        assertEquals(events1.map { it.cloudKey }, events2.map { it.cloudKey })
        assertEquals(guests1.map { it.cloudKey }, guests2.map { it.cloudKey })
        assertEquals(listOf("11111111", "22222222"), guests2.map { it.code }.sorted())
        assertTrue(guests2.all { it.eventId == 1L })
        assertEquals(2, guestsAfterUpdate.size)
    }
}
