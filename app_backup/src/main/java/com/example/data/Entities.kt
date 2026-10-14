package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey
import java.util.UUID

@Entity(
    tableName = "events",
    indices = [Index(value = ["cloudKey"], unique = true)]
)
data class EventEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val name: String,
    val date: String,
    val timeFrom: String,
    val timeTo: String,
    val backgroundType: String = "PRESET", // PRESET, IMAGE, GIF, VIDEO
    val backgroundPresetId: Int = 0, // 0..5
    val backgroundUri: String? = null,
    val backgroundAspectRatio: Float = 0.714f, // Default 5:7 portrait card ratio
    val nameColorHex: String = "#FFFDF6E2",
    val qrColorHex: String = "#FF121826",
    val qrBgColorHex: String = "#FFFFFFFF",
    val fontIndex: Int = 0, // 0..7
    val showGuestName: Boolean = true,
    val showDate: Boolean = true,
    val showTime: Boolean = true,
    val qrSizeRatio: Float = 0.28f, // Relative to card width (0.15f..0.45f)
    val nameSizeSp: Float = 22f, // 12f..42f
    val qrPosX: Float = 0.50f, // Normalized 0f..1f
    val qrPosY: Float = 0.48f, // Normalized 0f..1f
    val namePosX: Float = 0.50f, // Normalized 0f..1f
    val namePosY: Float = 0.76f, // Normalized 0f..1f
    val datePosX: Float = 0.50f, // Normalized 0f..1f
    val datePosY: Float = 0.88f, // Normalized 0f..1f
    val whatsappTemplate: String = DEFAULT_WHATSAPP_TEMPLATE_AR,
    val createdAt: Long = System.currentTimeMillis(),

    // Comprehensive Wedding Details & Location QR Code
    val groomName: String = "",
    val brideName: String = "",
    val headerVerse: String = "",
    val hostsText: String = "",
    val dayName: String = "",
    val venueName: String = "",
    val venueAddress: String = "",
    val locationUrl: String = "",
    val showLocationQr: Boolean = true,
    val notesText: String = "",
    val closingText: String = "",

    // Stable cross-device identifier (UUID v4). Generated once per record; never derived from the local Room id.
    // Prepared for future cloud sync. Local `id` stays the primary key and is still what QR payloads use.
    @ColumnInfo(defaultValue = "''")
    val cloudKey: String = UUID.randomUUID().toString()
) {
    companion object {
        const val DEFAULT_WHATSAPP_TEMPLATE_AR =
            "أهلًا {name} \uD83C\uDF39\nيسعدنا دعوتك إلى {event}\n{date}\n{time}\nمرفق دعوتك الخاصة، برجاء إظهار الباركود عند الدخول. نتشرف بحضورك ✨"
        const val DEFAULT_WHATSAPP_TEMPLATE_EN =
            "Hello {name} \uD83C\uDF39\nWe are delighted to invite you to {event}\n{date}\n{time}\nYour personal invitation is attached. Please show the QR code at the entrance ✨"
    }
}

@Entity(
    tableName = "guests",
    foreignKeys = [
        ForeignKey(
            entity = EventEntity::class,
            parentColumns = ["id"],
            childColumns = ["eventId"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index(value = ["eventId"]),
        Index(value = ["code"], unique = true),
        Index(value = ["cloudKey"], unique = true)
    ]
)
data class GuestEntity(
    @PrimaryKey(autoGenerate = true) val id: Long = 0L,
    val eventId: Long,
    val name: String,
    val normalizedName: String,
    val phone: String = "",
    val normalizedPhone: String = "",
    val code: String, // Unique 8-digit code across all events
    val sequenceNumber: Int,
    val attended: Boolean = false,
    val attendedAt: Long? = null,
    val whatsappSent: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),

    // Stable cross-device identifier (UUID v4), generated once. See EventEntity.cloudKey.
    @ColumnInfo(defaultValue = "''")
    val cloudKey: String = UUID.randomUUID().toString()
) {
    fun qrPayload(): String = "eb:$eventId:$code"
}

data class EventWithCounts(
    val event: EventEntity,
    val totalGuests: Int,
    val attendedGuests: Int
)

data class SkippedGuestItem(
    val inputLine: String,
    val reasonAr: String,
    val reasonEn: String
)

data class AddGuestsResult(
    val addedCount: Int,
    val skippedItems: List<SkippedGuestItem>
)

sealed class ScanOutcome {
    data class CheckedInSuccess(
        val guest: GuestEntity,
        val checkInTimeMillis: Long
    ) : ScanOutcome()

    data class AlreadyCheckedIn(
        val guest: GuestEntity,
        val firstCheckInTimeMillis: Long
    ) : ScanOutcome()

    data class WrongEvent(
        val guestCode: String,
        val otherEventId: Long,
        val otherEventName: String?
    ) : ScanOutcome()

    data class UnknownInvitation(
        val rawBarcode: String
    ) : ScanOutcome()
}

/**
 * Outcome of importing a JSON backup. Import never wipes existing data:
 * it either merges safely, or stops with a clear [Conflict] and changes nothing.
 */
sealed class BackupImportResult {
    data class Success(
        val eventsAdded: Int,
        val eventsMerged: Int,
        val guestsAdded: Int,
        val guestsMerged: Int
    ) : BackupImportResult()

    data class Conflict(
        val reasonAr: String,
        val reasonEn: String
    ) : BackupImportResult()

    data object Invalid : BackupImportResult()
}
