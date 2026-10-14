package com.example.data

import android.content.Context
import androidx.room.Dao
import androidx.room.Database
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.Transaction
import androidx.room.Update
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow

@Dao
interface EventDao {
    @Query("SELECT * FROM events ORDER BY createdAt DESC")
    fun observeAllEvents(): Flow<List<EventEntity>>

    @Query("SELECT * FROM events ORDER BY createdAt DESC")
    suspend fun getAllEventsOnce(): List<EventEntity>

    @Query("SELECT * FROM events WHERE id = :eventId LIMIT 1")
    fun observeEventById(eventId: Long): Flow<EventEntity?>

    @Query("SELECT * FROM events WHERE id = :eventId LIMIT 1")
    suspend fun getEventById(eventId: Long): EventEntity?

    // ABORT (not REPLACE): replacing an events row would delete it first and, through
    // ON DELETE CASCADE, silently delete all of its guests. A conflict must fail loudly instead.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertEvent(event: EventEntity): Long

    @Update
    suspend fun updateEvent(event: EventEntity)

    @Query("DELETE FROM events WHERE id = :eventId")
    suspend fun deleteEventById(eventId: Long)

    @Query("DELETE FROM events")
    suspend fun deleteAllEvents()

    @Query("SELECT * FROM guests WHERE eventId = :eventId ORDER BY sequenceNumber ASC, id ASC")
    fun observeGuestsForEvent(eventId: Long): Flow<List<GuestEntity>>

    @Query("SELECT * FROM guests WHERE eventId = :eventId ORDER BY sequenceNumber ASC, id ASC")
    suspend fun getGuestsForEventOnce(eventId: Long): List<GuestEntity>

    @Query("SELECT * FROM guests")
    fun observeAllGuests(): Flow<List<GuestEntity>>

    @Query("SELECT * FROM guests")
    suspend fun getAllGuestsOnce(): List<GuestEntity>

    @Query("SELECT code FROM guests")
    suspend fun getAllGuestCodes(): List<String>

    @Query("SELECT MAX(sequenceNumber) FROM guests WHERE eventId = :eventId")
    suspend fun getMaxSequenceForEvent(eventId: Long): Int?

    @Query("SELECT * FROM guests WHERE eventId = :eventId AND code = :code LIMIT 1")
    suspend fun getGuestByEventAndCode(eventId: Long, code: String): GuestEntity?

    @Query("SELECT * FROM guests WHERE code = :code LIMIT 1")
    suspend fun getGuestByCode(code: String): GuestEntity?

    @Query("SELECT * FROM events WHERE cloudKey = :cloudKey LIMIT 1")
    suspend fun getEventByCloudKey(cloudKey: String): EventEntity?

    @Query("SELECT * FROM guests WHERE cloudKey = :cloudKey LIMIT 1")
    suspend fun getGuestByCloudKey(cloudKey: String): GuestEntity?

    @Query("SELECT * FROM guests WHERE id = :guestId LIMIT 1")
    suspend fun getGuestById(guestId: Long): GuestEntity?

    // IGNORE (not REPLACE): a conflicting guest (same code / cloudKey / id) is skipped and reported
    // as -1 in the returned list, instead of silently deleting the row it collides with.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertGuests(guests: List<GuestEntity>): List<Long>

    // ABORT variant used by backup import: any conflict rolls the whole import transaction back.
    @Insert(onConflict = OnConflictStrategy.ABORT)
    suspend fun insertGuestsStrict(guests: List<GuestEntity>): List<Long>

    @Update
    suspend fun updateGuest(guest: GuestEntity)

    @Query("UPDATE guests SET whatsappSent = 1 WHERE id = :guestId")
    suspend fun markWhatsappSent(guestId: Long)

    @Query("DELETE FROM guests WHERE id = :guestId")
    suspend fun deleteGuestById(guestId: Long)

    @Query("DELETE FROM guests WHERE id IN (:guestIds)")
    suspend fun deleteGuestsByIds(guestIds: List<Long>)

    /**
     * DESTRUCTIVE: wipes every event (and, via CASCADE, every guest) before inserting.
     * No longer used by the app: backup import now merges via [applyBackupImport].
     * Kept only so existing code is not removed; do not call it for restore or for cloud data.
     */
    @Deprecated("Destructive. Use applyBackupImport (non-destructive merge) instead.")
    @Transaction
    suspend fun replaceAllData(events: List<EventEntity>, guests: List<GuestEntity>) {
        deleteAllEvents()
        for (event in events) {
            insertEvent(event)
        }
        insertGuests(guests)
    }

    /**
     * Applies a pre-validated backup import atomically: adds new events/guests and updates merged
     * guests. Never deletes anything. Any failure rolls the whole transaction back.
     */
    @Transaction
    suspend fun applyBackupImport(
        newEvents: List<EventEntity>,
        newGuests: List<GuestEntity>,
        mergedGuests: List<GuestEntity>
    ) {
        for (event in newEvents) {
            insertEvent(event)
        }
        if (newGuests.isNotEmpty()) {
            insertGuestsStrict(newGuests)
        }
        for (guest in mergedGuests) {
            updateGuest(guest)
        }
    }

    @Query("SELECT * FROM sync_queue ORDER BY timestamp ASC")
    fun observeSyncQueue(): Flow<List<SyncQueueEntity>>

    @Query("SELECT * FROM sync_queue ORDER BY timestamp ASC")
    suspend fun getAllSyncQueueOnce(): List<SyncQueueEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSyncQueue(item: SyncQueueEntity): Long

    @Update
    suspend fun updateSyncQueue(item: SyncQueueEntity)

    @Query("DELETE FROM sync_queue WHERE queueId = :queueId")
    suspend fun deleteSyncQueueById(queueId: Long)

    @Query("DELETE FROM sync_queue")
    suspend fun clearSyncQueue()
}

/**
 * Room schema v2 -> v3: adds the stable cloud identifier `cloudKey` (UUID v4) to events and guests.
 *
 * Non-destructive by construction: only ALTER TABLE ADD COLUMN, UPDATE and CREATE INDEX.
 * No table is dropped or recreated (dropping `events` with foreign keys enabled would cascade-delete
 * every guest). Existing local ids, guest codes, attendance and relations are untouched.
 *
 * Idempotent backfill: only rows whose cloudKey is still '' receive a UUID, so an interrupted/repeated
 * run never regenerates an existing key. The unique indices are created AFTER the backfill.
 * The same statements are exercised by the migration tests.
 */
val MIGRATION_2_3_STATEMENTS: List<String> = listOf(
    "ALTER TABLE `events` ADD COLUMN `cloudKey` TEXT NOT NULL DEFAULT ''",
    "ALTER TABLE `guests` ADD COLUMN `cloudKey` TEXT NOT NULL DEFAULT ''",
    "UPDATE `events` SET `cloudKey` = lower(hex(randomblob(4)) || '-' || hex(randomblob(2)) || '-4' || substr(hex(randomblob(2)), 2) || '-' || substr('89ab', 1 + (random() & 3), 1) || substr(hex(randomblob(2)), 2) || '-' || hex(randomblob(6))) WHERE `cloudKey` = ''",
    "UPDATE `guests` SET `cloudKey` = lower(hex(randomblob(4)) || '-' || hex(randomblob(2)) || '-4' || substr(hex(randomblob(2)), 2) || '-' || substr('89ab', 1 + (random() & 3), 1) || substr(hex(randomblob(2)), 2) || '-' || hex(randomblob(6))) WHERE `cloudKey` = ''",
    "CREATE UNIQUE INDEX IF NOT EXISTS `index_events_cloudKey` ON `events` (`cloudKey`)",
    "CREATE UNIQUE INDEX IF NOT EXISTS `index_guests_cloudKey` ON `guests` (`cloudKey`)"
)

val MIGRATION_2_3: Migration = object : Migration(2, 3) {
    override fun migrate(db: SupportSQLiteDatabase) {
        for (sql in MIGRATION_2_3_STATEMENTS) {
            db.execSQL(sql)
        }
    }
}

val MIGRATION_3_4_STATEMENTS: List<String> = listOf(
    "ALTER TABLE `events` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0",
    "ALTER TABLE `events` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0",
    "ALTER TABLE `guests` ADD COLUMN `updatedAt` INTEGER NOT NULL DEFAULT 0",
    "ALTER TABLE `guests` ADD COLUMN `isDeleted` INTEGER NOT NULL DEFAULT 0",
    """
    CREATE TABLE IF NOT EXISTS `sync_queue` (
        `queueId` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL,
        `entityType` TEXT NOT NULL,
        `entityCloudKey` TEXT NOT NULL,
        `parentCloudKey` TEXT,
        `actionType` TEXT NOT NULL,
        `payloadJson` TEXT NOT NULL,
        `timestamp` INTEGER NOT NULL,
        `retryCount` INTEGER NOT NULL,
        `lastError` TEXT
    )
    """,
    "CREATE INDEX IF NOT EXISTS `index_sync_queue_timestamp` ON `sync_queue` (`timestamp`)"
)

val MIGRATION_3_4: Migration = object : Migration(3, 4) {
    override fun migrate(db: SupportSQLiteDatabase) {
        for (sql in MIGRATION_3_4_STATEMENTS) {
            db.execSQL(sql)
        }
    }
}

@Database(
    entities = [EventEntity::class, GuestEntity::class, SyncQueueEntity::class],
    version = 4,
    exportSchema = true
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun eventDao(): EventDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "elbeit_elaraby.db"
                )
                    // No destructive fallback: a database version without an explicit migration path
                    // makes Room throw instead of wiping user data. Add a Migration for every new version.
                    .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
