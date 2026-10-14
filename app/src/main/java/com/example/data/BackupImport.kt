package com.example.data

import org.json.JSONObject
import java.util.UUID

/**
 * Non-destructive JSON backup import (supports backup v1 without cloudKey, and v2 with cloudKey).
 *
 * Design:
 * - [parse] converts the JSON into entities without touching the database.
 * - [plan] compares the backup with the CURRENT database contents and either produces an
 *   [ImportPlan] (add new records, merge attendance into matching guests) or a [PlanOutcome.Conflict].
 * - The plan is applied atomically by [EventDao.applyBackupImport]. Nothing is ever deleted.
 *
 * Matching rules:
 * - Event: by cloudKey when the backup has one; legacy (v1) events without cloudKey are matched by
 *   the fingerprint name + date + createdAt. A backup event whose id is already used by a different
 *   event stops the import (the local event id is part of printed QR codes, so it must not change).
 * - Guest: by invitation `code` (globally unique). Same code under the same event => merge
 *   (attended / attendedAt / whatsappSent are combined, never reverted). Same code under another
 *   event => conflict.
 * - Existing records are never overwritten with backup values, except the attendance/whatsapp merge.
 * - Guest codes, QR payload format and local ids of new events are preserved exactly as in the file.
 */
object BackupImporter {

    const val CURRENT_BACKUP_VERSION = 2

    data class ParsedBackup(
        val version: Int,
        val events: List<EventEntity>,
        val guests: List<GuestEntity>
    )

    data class ImportPlan(
        val newEvents: List<EventEntity>,
        val newGuests: List<GuestEntity>,
        val mergedGuests: List<GuestEntity>,
        val eventsMerged: Int,
        val guestsMerged: Int
    )

    sealed class PlanOutcome {
        data class Ready(val plan: ImportPlan) : PlanOutcome()
        data class Conflict(val reasonAr: String, val reasonEn: String) : PlanOutcome()
    }

    fun isValidUuid(value: String): Boolean {
        return try {
            UUID.fromString(value).toString().equals(value, ignoreCase = true)
        } catch (_: Exception) {
            false
        }
    }

    private fun readCloudKey(obj: JSONObject): String {
        val raw = obj.optString("cloudKey", "").trim().lowercase()
        return if (raw.isNotEmpty() && isValidUuid(raw)) raw else ""
    }

    /**
     * Returns null when the file is not a valid backup (malformed JSON, unknown future version,
     * duplicate ids/codes inside the file, guest pointing to a missing event, blank code...).
     * Entities returned here may have an empty cloudKey (legacy v1 files); [plan] fills it in.
     */
    fun parse(jsonString: String): ParsedBackup? {
        return try {
            val root = JSONObject(jsonString)
            val version = root.optInt("version", 1)
            if (version < 1 || version > CURRENT_BACKUP_VERSION) return null

            val eventsArray = root.getJSONArray("events")
            val guestsArray = root.getJSONArray("guests")

            val events = mutableListOf<EventEntity>()
            for (i in 0 until eventsArray.length()) {
                val obj = eventsArray.getJSONObject(i)
                events.add(
                    EventEntity(
                        id = obj.getLong("id"),
                        name = obj.getString("name"),
                        date = obj.optString("date", ""),
                        timeFrom = obj.optString("timeFrom", ""),
                        timeTo = obj.optString("timeTo", ""),
                        backgroundType = "PRESET",
                        backgroundPresetId = obj.optInt("backgroundPresetId", 0),
                        backgroundUri = null,
                        backgroundAspectRatio = obj.optDouble("backgroundAspectRatio", 0.714).toFloat(),
                        nameColorHex = obj.optString("nameColorHex", "#FFFDF6E2"),
                        qrColorHex = obj.optString("qrColorHex", "#FF121826"),
                        qrBgColorHex = obj.optString("qrBgColorHex", "#FFFFFFFF"),
                        fontIndex = obj.optInt("fontIndex", 0),
                        showGuestName = obj.optBoolean("showGuestName", true),
                        showDate = obj.optBoolean("showDate", true),
                        showTime = obj.optBoolean("showTime", true),
                        qrSizeRatio = obj.optDouble("qrSizeRatio", 0.28).toFloat(),
                        nameSizeSp = obj.optDouble("nameSizeSp", 22.0).toFloat(),
                        qrPosX = obj.optDouble("qrPosX", 0.5).toFloat(),
                        qrPosY = obj.optDouble("qrPosY", 0.48).toFloat(),
                        namePosX = obj.optDouble("namePosX", 0.5).toFloat(),
                        namePosY = obj.optDouble("namePosY", 0.76).toFloat(),
                        datePosX = obj.optDouble("datePosX", 0.5).toFloat(),
                        datePosY = obj.optDouble("datePosY", 0.88).toFloat(),
                        whatsappTemplate = obj.optString(
                            "whatsappTemplate",
                            EventEntity.DEFAULT_WHATSAPP_TEMPLATE_AR
                        ),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        groomName = obj.optString("groomName", ""),
                        brideName = obj.optString("brideName", ""),
                        headerVerse = obj.optString("headerVerse", ""),
                        hostsText = obj.optString("hostsText", ""),
                        dayName = obj.optString("dayName", ""),
                        venueName = obj.optString("venueName", ""),
                        venueAddress = obj.optString("venueAddress", ""),
                        locationUrl = obj.optString("locationUrl", ""),
                        showLocationQr = obj.optBoolean("showLocationQr", true),
                        notesText = obj.optString("notesText", ""),
                        closingText = obj.optString("closingText", ""),
                        cloudKey = readCloudKey(obj)
                    )
                )
            }

            val guests = mutableListOf<GuestEntity>()
            for (i in 0 until guestsArray.length()) {
                val obj = guestsArray.getJSONObject(i)
                val attendedAtVal = if (obj.has("attendedAt") && !obj.isNull("attendedAt")) {
                    obj.getLong("attendedAt")
                } else null
                val name = obj.getString("name")
                val phone = obj.optString("phone", "")
                guests.add(
                    GuestEntity(
                        id = obj.getLong("id"),
                        eventId = obj.getLong("eventId"),
                        name = name,
                        normalizedName = obj.optString(
                            "normalizedName",
                            EventRepository.normalizeNameForComparison(name)
                        ),
                        phone = phone,
                        normalizedPhone = obj.optString(
                            "normalizedPhone",
                            if (phone.isEmpty()) "" else EventRepository.normalizePhoneForComparison(phone)
                        ),
                        code = obj.getString("code").trim(),
                        sequenceNumber = obj.optInt("sequenceNumber", i + 1),
                        attended = obj.optBoolean("attended", false),
                        attendedAt = attendedAtVal,
                        whatsappSent = obj.optBoolean("whatsappSent", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        cloudKey = readCloudKey(obj)
                    )
                )
            }

            // Structural validation of the file itself.
            if (events.any { it.id <= 0L }) return null
            if (events.map { it.id }.toSet().size != events.size) return null
            val eventKeys = events.map { it.cloudKey }.filter { it.isNotEmpty() }
            if (eventKeys.toSet().size != eventKeys.size) return null
            if (guests.any { it.code.isEmpty() }) return null
            if (guests.map { it.code }.toSet().size != guests.size) return null
            val guestKeys = guests.map { it.cloudKey }.filter { it.isNotEmpty() }
            if (guestKeys.toSet().size != guestKeys.size) return null
            val eventIds = events.map { it.id }.toSet()
            if (guests.any { it.eventId !in eventIds }) return null

            ParsedBackup(version = version, events = events, guests = guests)
        } catch (_: Exception) {
            null
        }
    }

    fun plan(
        parsed: ParsedBackup,
        existingEvents: List<EventEntity>,
        existingGuests: List<GuestEntity>
    ): PlanOutcome {
        val eventsById = existingEvents.associateBy { it.id }
        val eventsByKey = existingEvents.filter { it.cloudKey.isNotBlank() }.associateBy { it.cloudKey }
        val usedEventKeys = existingEvents.map { it.cloudKey }.filter { it.isNotBlank() }.toMutableSet()

        val eventTarget = HashMap<Long, Long>() // backup event id -> local event id
        val newEvents = mutableListOf<EventEntity>()
        var eventsMerged = 0

        for (be in parsed.events) {
            val byFingerprint = existingEvents.firstOrNull {
                it.name == be.name && it.date == be.date && it.createdAt == be.createdAt
            }
            val match: EventEntity? = if (be.cloudKey.isNotEmpty()) {
                val byKey = eventsByKey[be.cloudKey]
                if (byKey == null && byFingerprint != null) {
                    return PlanOutcome.Conflict(
                        reasonAr = "المناسبة «${be.name}» موجودة بالفعل بمعرّف سحابي مختلف عن الموجود في الملف.",
                        reasonEn = "Event \"${be.name}\" already exists with a different cloud identifier than the one in the file."
                    )
                }
                byKey
            } else {
                byFingerprint
            }

            if (match != null) {
                eventTarget[be.id] = match.id
                eventsMerged++
                continue
            }

            // New event: keep its local id (it is embedded in printed QR codes) or stop.
            if (eventsById.containsKey(be.id)) {
                return PlanOutcome.Conflict(
                    reasonAr = "رقم المناسبة المحلي ${be.id} («${be.name}») مستخدم بالفعل لمناسبة مختلفة. تغيير الرقم سيُبطل رموز QR المطبوعة.",
                    reasonEn = "Local event id ${be.id} (\"${be.name}\") is already used by a different event. Changing it would invalidate printed QR codes."
                )
            }
            var key = be.cloudKey
            if (key.isEmpty() || key in usedEventKeys) {
                do {
                    key = UUID.randomUUID().toString()
                } while (key in usedEventKeys)
            }
            usedEventKeys.add(key)
            newEvents.add(be.copy(cloudKey = key))
            eventTarget[be.id] = be.id
        }

        val existingByCode = existingGuests.associateBy { it.code }
        val usedGuestKeys = existingGuests.map { it.cloudKey }.filter { it.isNotBlank() }.toMutableSet()
        val usedGuestIds = existingGuests.map { it.id }.toMutableSet()

        val newGuests = mutableListOf<GuestEntity>()
        val mergedGuests = mutableListOf<GuestEntity>()
        var guestsMerged = 0

        for (bg in parsed.guests) {
            val targetEventId = eventTarget[bg.eventId]
                ?: return PlanOutcome.Conflict(
                    reasonAr = "ضيف في الملف يشير إلى مناسبة غير موجودة.",
                    reasonEn = "A guest in the file references an event that is not in the file."
                )
            val existing = existingByCode[bg.code]
            if (existing != null) {
                if (existing.eventId != targetEventId) {
                    return PlanOutcome.Conflict(
                        reasonAr = "كود الدعوة ${bg.code} مسجّل بالفعل لضيف في مناسبة أخرى.",
                        reasonEn = "Invitation code ${bg.code} already belongs to a guest in a different event."
                    )
                }
                guestsMerged++
                val existingAt = existing.attendedAt
                val backupAt = bg.attendedAt
                val attended = existing.attended || bg.attended
                val attendedAt: Long? = when {
                    existing.attended && bg.attended ->
                        if (existingAt != null && backupAt != null) minOf(existingAt, backupAt) else (existingAt ?: backupAt)
                    existing.attended -> existingAt
                    bg.attended -> backupAt
                    else -> null
                }
                val whatsapp = existing.whatsappSent || bg.whatsappSent
                if (attended != existing.attended || attendedAt != existingAt || whatsapp != existing.whatsappSent) {
                    mergedGuests.add(
                        existing.copy(attended = attended, attendedAt = attendedAt, whatsappSent = whatsapp)
                    )
                }
                continue
            }

            var key = bg.cloudKey
            if (key.isNotEmpty() && key in usedGuestKeys) {
                return PlanOutcome.Conflict(
                    reasonAr = "المعرّف السحابي لضيف في الملف (${bg.code}) مستخدم بالفعل لضيف بكود مختلف.",
                    reasonEn = "The cloud identifier of a guest in the file (${bg.code}) is already used by a guest with a different code."
                )
            }
            if (key.isEmpty()) {
                do {
                    key = UUID.randomUUID().toString()
                } while (key in usedGuestKeys)
            }
            usedGuestKeys.add(key)

            // Guest local ids are not part of QR payloads: keep the file's id when free, otherwise auto-assign.
            val id = if (bg.id > 0L && bg.id !in usedGuestIds) bg.id else 0L
            if (id > 0L) usedGuestIds.add(id)

            newGuests.add(bg.copy(id = id, eventId = targetEventId, cloudKey = key))
        }

        return PlanOutcome.Ready(
            ImportPlan(
                newEvents = newEvents,
                newGuests = newGuests,
                mergedGuests = mergedGuests,
                eventsMerged = eventsMerged,
                guestsMerged = guestsMerged
            )
        )
    }
}
