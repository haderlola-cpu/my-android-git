package com.example.data

import android.content.Context
import android.database.sqlite.SQLiteConstraintException
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.runBlocking
import org.json.JSONArray
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

/**
 * Tests for: non-destructive JSON import (v1 legacy + v2), stable cloudKeys, QR payload format, and the
 * removal of destructive REPLACE behaviour.
 *
 * NOTE: written for the Gradle build; it was NOT executed in the environment where it was authored
 * (no Gradle / Android SDK there).
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class BackupImportAndSafetyTest {

    private lateinit var db: AppDatabase
    private lateinit var repo: EventRepository
    private val uuidV4 = Regex("^[0-9a-f]{8}-[0-9a-f]{4}-4[0-9a-f]{3}-[89ab][0-9a-f]{3}-[0-9a-f]{12}$")

    private fun newDb(): AppDatabase {
        val ctx = ApplicationProvider.getApplicationContext<Context>()
        return Room.inMemoryDatabaseBuilder(ctx, AppDatabase::class.java).allowMainThreadQueries().build()
    }

    @Before
    fun setUp() {
        db = newDb()
        repo = EventRepository(db.eventDao())
    }

    @After
    fun tearDown() {
        db.close()
    }

    private fun event(name: String = "حفل") = EventEntity(name = name, date = "2026-11-20", timeFrom = "18:00", timeTo = "23:00")

    /** Builds a legacy (v1) backup file: no "version" > 1 and no cloudKey anywhere. */
    private fun legacyJson(
        eventId: Long, eventName: String, createdAt: Long,
        guests: List<Triple<Long, String, Boolean>> // (guest id, code, attended)
    ): String {
        val root = JSONObject()
        root.put("app", "elbeit elaraby")
        root.put("version", 1)
        root.put(
            "events",
            JSONArray().put(
                JSONObject().put("id", eventId).put("name", eventName).put("date", "2026-11-20").put("createdAt", createdAt)
            )
        )
        val arr = JSONArray()
        for ((gid, code, attended) in guests) {
            val g = JSONObject().put("id", gid).put("eventId", eventId).put("name", "ضيف $gid")
                .put("code", code).put("attended", attended).put("whatsappSent", false)
            if (attended) g.put("attendedAt", 5000L)
            arr.put(g)
        }
        root.put("guests", arr)
        return root.toString()
    }

    @Test
    fun legacyBackup_importsIntoEmptyDb_keepingIdsCodesAndQrFormat() = runBlocking {
        val json = legacyJson(7, "حفل قديم", 1000L, listOf(Triple(10L, "12345678", true), Triple(11L, "87654321", false)))
        val result = repo.importBackupJson(json)
        assertTrue(result is BackupImportResult.Success)

        val events = db.eventDao().getAllEventsOnce()
        val guests = db.eventDao().getAllGuestsOnce().sortedBy { it.id }
        assertEquals(listOf(7L), events.map { it.id })
        assertEquals(listOf("12345678", "87654321"), guests.map { it.code })
        assertEquals("eb:7:12345678", guests.first().qrPayload()) // QR format unchanged
        assertTrue(guests.first().attended)
        assertEquals(5000L, guests.first().attendedAt)
        val keys = events.map { it.cloudKey } + guests.map { it.cloudKey }
        assertTrue(keys.all { uuidV4.matches(it) })
        assertEquals(keys.size, keys.toSet().size)
    }

    @Test
    fun importingTheSameLegacyBackupTwice_createsNoDuplicates_andKeepsKeys() = runBlocking {
        val json = legacyJson(7, "حفل قديم", 1000L, listOf(Triple(10L, "12345678", false)))
        repo.importBackupJson(json)
        val keysBefore = db.eventDao().getAllEventsOnce().map { it.cloudKey } + db.eventDao().getAllGuestsOnce().map { it.cloudKey }

        val second = repo.importBackupJson(json)
        assertTrue(second is BackupImportResult.Success)
        second as BackupImportResult.Success
        assertEquals(0, second.eventsAdded)
        assertEquals(0, second.guestsAdded)
        assertEquals(1, db.eventDao().getAllEventsOnce().size)
        assertEquals(1, db.eventDao().getAllGuestsOnce().size)
        val keysAfter = db.eventDao().getAllEventsOnce().map { it.cloudKey } + db.eventDao().getAllGuestsOnce().map { it.cloudKey }
        assertEquals(keysBefore, keysAfter)
    }

    @Test
    fun exportV2_thenImportIntoFreshDatabase_keepsCloudKeysCodesAndAttendance() = runBlocking {
        val eventId = repo.saveEvent(event("حفل أ"))
        repo.addGuestsByNames(eventId, "أحمد محمد\nمنى السيد")
        val guest = db.eventDao().getGuestsForEventOnce(eventId).first()
        repo.verifyAndCheckInBarcode(eventId, guest.qrPayload())

        val json = repo.exportBackupJson()
        assertEquals(2, JSONObject(json).getInt("version"))

        val otherDb = newDb()
        try {
            val result = EventRepository(otherDb.eventDao()).importBackupJson(json)
            assertTrue(result is BackupImportResult.Success)
            assertEquals(
                db.eventDao().getAllEventsOnce().map { it.cloudKey },
                otherDb.eventDao().getAllEventsOnce().map { it.cloudKey }
            )
            assertEquals(
                db.eventDao().getAllGuestsOnce().sortedBy { it.id }.map { Triple(it.cloudKey, it.code, it.attended) },
                otherDb.eventDao().getAllGuestsOnce().sortedBy { it.id }.map { Triple(it.cloudKey, it.code, it.attended) }
            )
        } finally {
            otherDb.close()
        }
    }

    @Test
    fun import_conflictingLocalEventId_stopsAndChangesNothing() = runBlocking {
        val existingId = repo.saveEvent(event("حفل موجود"))
        repo.addGuestsByNames(existingId, "ضيف واحد")
        val before = db.eventDao().getAllGuestsOnce()

        val json = legacyJson(existingId, "حفل آخر مختلف", 42L, listOf(Triple(99L, "55555555", false)))
        val result = repo.importBackupJson(json)

        assertTrue(result is BackupImportResult.Conflict)
        assertEquals(1, db.eventDao().getAllEventsOnce().size)
        assertEquals(before, db.eventDao().getAllGuestsOnce())
    }

    @Test
    fun import_codeBelongingToAnotherEvent_stopsAndAddsNothing() = runBlocking {
        val existingId = repo.saveEvent(event("حفل موجود"))
        repo.addGuestsByNames(existingId, "ضيف واحد")
        val code = db.eventDao().getGuestsForEventOnce(existingId).first().code

        val json = legacyJson(existingId + 100, "حفل جديد", 77L, listOf(Triple(500L, code, false)))
        val result = repo.importBackupJson(json)

        assertTrue(result is BackupImportResult.Conflict)
        assertEquals(1, db.eventDao().getAllEventsOnce().size) // the new event was NOT added either
        assertEquals(1, db.eventDao().getAllGuestsOnce().size)
    }

    @Test
    fun import_mergesAttendance_andNeverRevertsIt() = runBlocking {
        repo.importBackupJson(legacyJson(7, "حفل", 1000L, listOf(Triple(10L, "12345678", false))))
        assertEquals(false, db.eventDao().getAllGuestsOnce().first().attended)

        // A newer backup says the guest attended -> merged, no duplicate guest.
        repo.importBackupJson(legacyJson(7, "حفل", 1000L, listOf(Triple(10L, "12345678", true))))
        assertEquals(1, db.eventDao().getAllGuestsOnce().size)
        assertTrue(db.eventDao().getAllGuestsOnce().first().attended)

        // Re-importing an OLD backup (not attended) must not undo the attendance.
        repo.importBackupJson(legacyJson(7, "حفل", 1000L, listOf(Triple(10L, "12345678", false))))
        assertTrue(db.eventDao().getAllGuestsOnce().first().attended)
    }

    @Test
    fun unknownBackupVersion_isInvalid_andChangesNothing() = runBlocking {
        repo.saveEvent(event())
        val json = JSONObject(legacyJson(7, "حفل", 1L, emptyList())).put("version", 99).toString()
        assertEquals(BackupImportResult.Invalid, repo.importBackupJson(json))
        assertEquals(1, db.eventDao().getAllEventsOnce().size)
    }

    @Test
    fun saveEvent_update_keepsCloudKey_andKeepsGuests() = runBlocking {
        val id = repo.saveEvent(event("قبل"))
        repo.addGuestsByNames(id, "أحمد\nمنى")
        val stored = db.eventDao().getEventById(id)!!

        // The designer rebuilds EventEntity from scratch: simulate a fresh random cloudKey coming in.
        repo.saveEvent(stored.copy(name = "بعد", cloudKey = UUID.randomUUID().toString()))

        val after = db.eventDao().getEventById(id)!!
        assertEquals("بعد", after.name)
        assertEquals(stored.cloudKey, after.cloudKey)
        assertEquals(2, db.eventDao().getGuestsForEventOnce(id).size)
    }

    @Test
    fun insertGuests_duplicateCode_isIgnored_notReplaced() = runBlocking {
        val eventId = repo.saveEvent(event())
        val a = GuestEntity(eventId = eventId, name = "أ", normalizedName = "أ", code = "11111111", sequenceNumber = 1)
        val b = GuestEntity(eventId = eventId, name = "ب", normalizedName = "ب", code = "11111111", sequenceNumber = 2)
        db.eventDao().insertGuests(listOf(a))
        val result = db.eventDao().insertGuests(listOf(b))

        assertEquals(listOf(-1L), result)
        val stored = db.eventDao().getAllGuestsOnce()
        assertEquals(1, stored.size)
        assertEquals("أ", stored.first().name) // the original guest was not replaced or deleted
    }

    @Test
    fun insertEvent_conflict_failsLoudly_andDoesNotCascadeDeleteGuests() = runBlocking {
        val id = repo.saveEvent(event())
        repo.addGuestsByNames(id, "أحمد\nمنى")
        try {
            db.eventDao().insertEvent(event("نفس الرقم").copy(id = id))
            fail("expected a constraint failure instead of REPLACE")
        } catch (_: SQLiteConstraintException) {
            // expected
        }
        assertEquals(2, db.eventDao().getGuestsForEventOnce(id).size)
    }

    @Test
    fun deleteEvent_stillCascadesToItsOwnGuestsOnly() = runBlocking {
        val a = repo.saveEvent(event("أ"))
        val b = repo.saveEvent(event("ب"))
        repo.addGuestsByNames(a, "ضيف أ")
        repo.addGuestsByNames(b, "ضيف ب")
        repo.deleteEvent(a)
        assertEquals(0, db.eventDao().getGuestsForEventOnce(a).size)
        assertEquals(1, db.eventDao().getGuestsForEventOnce(b).size)
    }

    @Test
    fun newGuestsAndEvents_getUniqueUuidCloudKeys() = runBlocking {
        val a = repo.saveEvent(event("أ"))
        val b = repo.saveEvent(event("ب"))
        repo.addGuestsByNames(a, (1..50).joinToString("\n") { "ضيف $it" })
        repo.addGuestsBySequence(b, 1, 50, "طاولة")
        val keys = db.eventDao().getAllEventsOnce().map { it.cloudKey } + db.eventDao().getAllGuestsOnce().map { it.cloudKey }
        assertTrue(keys.all { uuidV4.matches(it) })
        assertEquals(keys.size, keys.toSet().size)
        assertNotEquals("", keys.first())
    }

    @Test
    fun qrScanVerification_worksAcrossMultiDeviceRoomIds_whenCloudKeyMatches() = runBlocking {
        val eventIdDeviceA = repo.saveEvent(event("حفل أ"))
        repo.addGuestsByNames(eventIdDeviceA, "أحمد محمد")
        val guestOnA = db.eventDao().getGuestsForEventOnce(eventIdDeviceA).first()
        val scannedBarcode = "eb:1:${guestOnA.code}" // printed on Device A with ID 1

        val otherDb = newDb()
        try {
            val otherRepo = EventRepository(otherDb.eventDao())
            val dummyEvent = otherRepo.saveEvent(event("مناسبة سابقة")) // consumes local ID 1 on Device B
            val eventIdDeviceB = otherRepo.saveEvent(event("حفل أ").copy(cloudKey = db.eventDao().getEventById(eventIdDeviceA)!!.cloudKey)) // local ID 2 on Device B
            otherDb.eventDao().insertGuests(listOf(guestOnA.copy(id = 0L, eventId = eventIdDeviceB)))

            // Verify barcode on Device B for eventIdDeviceB (ID 2) when barcode says eb:1:<code
            val outcome = otherRepo.verifyAndCheckInBarcode(eventIdDeviceB, scannedBarcode)
            assertTrue(outcome is ScanOutcome.CheckedInSuccess)
            assertEquals(guestOnA.code, (outcome as ScanOutcome.CheckedInSuccess).guest.code)
        } finally {
            otherDb.close()
        }
    }

    @Test
    fun qrScanVerification_rejectsWrongEvent() = runBlocking {
        val eventA = repo.saveEvent(event("حفل أ"))
        val eventB = repo.saveEvent(event("حفل ب"))
        repo.addGuestsByNames(eventA, "ضيف أ")
        repo.addGuestsByNames(eventB, "ضيف ب")

        val guestB = db.eventDao().getGuestsForEventOnce(eventB).first()
        val outcome = repo.verifyAndCheckInBarcode(eventA, guestB.qrPayload())
        assertTrue(outcome is ScanOutcome.WrongEvent)
    }

    @Test
    fun singleEventSecureExportAndImport_roundTrip_succeeds() = runBlocking {
        val eventId = repo.saveEvent(event("حفل تشفير"))
        repo.addGuestsByNames(eventId, "ضيف 1\nضيف 2")
        val pass = "P@ssw0rd123"

        val encrypted = repo.exportSingleEventSecure(eventId, pass)
        assertTrue(!encrypted.isNullOrBlank())

        val otherDb = newDb()
        try {
            val otherRepo = EventRepository(otherDb.eventDao())
            val result = otherRepo.importSingleEventSecure(encrypted!!, pass)
            assertTrue(result is BackupImportResult.Success)
            assertEquals(1, otherDb.eventDao().getAllEventsOnce().size)
            assertEquals(2, otherDb.eventDao().getAllGuestsOnce().size)
        } finally {
            otherDb.close()
        }
    }

    @Test
    fun seedAllLocalDataToSyncQueue_queuesAllEventsAndGuests_idempotently() = runBlocking {
        val eventA = repo.saveEvent(event("حفل أ"))
        val eventB = repo.saveEvent(event("حفل ب"))
        repo.addGuestsByNames(eventA, "ضيف 1\nضيف 2")
        repo.addGuestsByNames(eventB, "ضيف 3")

        val initialQueueSize = db.eventDao().getAllSyncQueueOnce().size
        assertTrue(initialQueueSize > 0)

        // Seeding again when already queued should add 0 new duplicate items
        val seeded = repo.seedAllLocalDataToSyncQueue()
        assertEquals(0, seeded)
        assertEquals(initialQueueSize, db.eventDao().getAllSyncQueueOnce().size)
    }
}
