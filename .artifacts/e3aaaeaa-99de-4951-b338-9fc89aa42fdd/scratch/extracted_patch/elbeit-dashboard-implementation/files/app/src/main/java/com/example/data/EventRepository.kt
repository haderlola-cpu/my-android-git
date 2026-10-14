package com.example.data

import android.content.Context
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.combine
import org.json.JSONArray
import org.json.JSONObject
import java.util.UUID
import kotlin.random.Random

class EventRepository(private val dao: EventDao, private val context: Context? = null) {

    val eventsWithCounts: Flow<List<EventWithCounts>> = combine(
        dao.observeAllEvents(),
        dao.observeAllGuests()
    ) { events, allGuests ->
        val guestsByEvent = allGuests.groupBy { it.eventId }
        events.map { event ->
            val eventGuests = guestsByEvent[event.id].orEmpty()
            EventWithCounts(
                event = event,
                totalGuests = eventGuests.size,
                attendedGuests = eventGuests.count { it.attended }
            )
        }
    }

    /** كل الضيوف عبر كل المناسبات (للقراءة فقط، يستخدمه تبويب المدعوين). */
    val allGuests: Flow<List<GuestEntity>> = dao.observeAllGuests()

    /** طابور المزامنة الحالي (للقراءة فقط، يستخدمه تبويب المزامنة). */
    val syncQueue: Flow<List<SyncQueueEntity>> = dao.observeSyncQueue()

    fun observeEvent(eventId: Long): Flow<EventEntity?> = dao.observeEventById(eventId)

    fun observeGuests(eventId: Long): Flow<List<GuestEntity>> = dao.observeGuestsForEvent(eventId)

    suspend fun getEventById(eventId: Long): EventEntity? = dao.getEventById(eventId)

    suspend fun getGuestsOnce(eventId: Long): List<GuestEntity> = dao.getGuestsForEventOnce(eventId)

    suspend fun saveEvent(event: EventEntity): Long {
        val savedId = if (event.id == 0L) {
            val key = event.cloudKey.ifBlank { UUID.randomUUID().toString() }
            val e = event.copy(cloudKey = key, updatedAt = System.currentTimeMillis())
            val id = dao.insertEvent(e)
            val inserted = dao.getEventById(id) ?: e
            recordEventSync(inserted, "UPSERT")
            id
        } else {
            val stored = dao.getEventById(event.id)?.cloudKey?.takeIf { it.isNotBlank() }
            val key = stored
                ?: event.cloudKey.takeIf { it.isNotBlank() }
                ?: UUID.randomUUID().toString()
            val e = event.copy(cloudKey = key, updatedAt = System.currentTimeMillis())
            dao.updateEvent(e)
            recordEventSync(e, "UPSERT")
            event.id
        }
        return savedId
    }

    suspend fun deleteEvent(eventId: Long) {
        val event = dao.getEventById(eventId)
        if (event != null) {
            recordEventSync(event.copy(isDeleted = true, updatedAt = System.currentTimeMillis()), "DELETE")
        }
        dao.deleteEventById(eventId)
    }

    suspend fun deleteGuest(guestId: Long) {
        val guest = dao.getGuestById(guestId)
        if (guest != null) {
            recordGuestSync(guest.copy(isDeleted = true, updatedAt = System.currentTimeMillis()), "DELETE")
        }
        dao.deleteGuestById(guestId)
    }

    suspend fun deleteGuests(guestIds: List<Long>) {
        if (guestIds.isNotEmpty()) {
            val guests = guestIds.mapNotNull { dao.getGuestById(it) }
            for (g in guests) {
                recordGuestSync(g.copy(isDeleted = true, updatedAt = System.currentTimeMillis()), "DELETE")
            }
            dao.deleteGuestsByIds(guestIds)
        }
    }

    suspend fun markWhatsappSent(guestId: Long) {
        dao.markWhatsappSent(guestId)
        val guest = dao.getGuestById(guestId)
        if (guest != null) {
            val updated = guest.copy(whatsappSent = true, updatedAt = System.currentTimeMillis())
            dao.updateGuest(updated)
            recordGuestSync(updated, "UPSERT")
        }
    }

    private suspend fun recordEventSync(event: EventEntity, actionType: String) {
        val jsonObj = JSONObject().apply {
            put("cloudKey", event.cloudKey)
            put("legacyLocalId", event.id)
            put("name", event.name)
            put("date", event.date)
            put("timeFrom", event.timeFrom)
            put("timeTo", event.timeTo)
            put("backgroundType", event.backgroundType)
            put("backgroundPresetId", event.backgroundPresetId)
            put("backgroundAspectRatio", event.backgroundAspectRatio.toDouble())
            put("nameColorHex", event.nameColorHex)
            put("qrColorHex", event.qrColorHex)
            put("qrBgColorHex", event.qrBgColorHex)
            put("fontIndex", event.fontIndex)
            put("showGuestName", event.showGuestName)
            put("showDate", event.showDate)
            put("showTime", event.showTime)
            put("qrSizeRatio", event.qrSizeRatio.toDouble())
            put("nameSizeSp", event.nameSizeSp.toDouble())
            put("qrPosX", event.qrPosX.toDouble())
            put("qrPosY", event.qrPosY.toDouble())
            put("namePosX", event.namePosX.toDouble())
            put("namePosY", event.namePosY.toDouble())
            put("datePosX", event.datePosX.toDouble())
            put("datePosY", event.datePosY.toDouble())
            put("whatsappTemplate", event.whatsappTemplate)
            put("createdAt", event.createdAt)
            put("groomName", event.groomName)
            put("brideName", event.brideName)
            put("headerVerse", event.headerVerse)
            put("hostsText", event.hostsText)
            put("dayName", event.dayName)
            put("venueName", event.venueName)
            put("venueAddress", event.venueAddress)
            put("locationUrl", event.locationUrl)
            put("showLocationQr", event.showLocationQr)
            put("notesText", event.notesText)
            put("closingText", event.closingText)
            put("updatedAt", event.updatedAt)
            put("isDeleted", event.isDeleted)
        }
        dao.insertSyncQueue(
            SyncQueueEntity(
                entityType = "EVENT",
                entityCloudKey = event.cloudKey,
                parentCloudKey = null,
                actionType = actionType,
                payloadJson = jsonObj.toString()
            )
        )
        context?.let { SyncWorker.enqueue(it) }
    }

    private suspend fun recordGuestSync(guest: GuestEntity, actionType: String) {
        val event = dao.getEventById(guest.eventId)
        val jsonObj = JSONObject().apply {
            put("cloudKey", guest.cloudKey)
            put("eventCloudKey", event?.cloudKey ?: "")
            put("name", guest.name)
            put("normalizedName", guest.normalizedName)
            put("phone", guest.phone)
            put("normalizedPhone", guest.normalizedPhone)
            put("code", guest.code)
            put("sequenceNumber", guest.sequenceNumber)
            put("whatsappSent", guest.whatsappSent)
            put("attended", guest.attended)
            if (guest.attendedAt != null) put("attendedAt", guest.attendedAt) else put("attendedAt", JSONObject.NULL)
            put("createdAt", guest.createdAt)
            put("updatedAt", guest.updatedAt)
            put("isDeleted", guest.isDeleted)
        }
        dao.insertSyncQueue(
            SyncQueueEntity(
                entityType = "GUEST",
                entityCloudKey = guest.cloudKey,
                parentCloudKey = event?.cloudKey,
                actionType = actionType,
                payloadJson = jsonObj.toString()
            )
        )
        context?.let { SyncWorker.enqueue(it) }
    }

    /**
     * Inserts guests without ever replacing existing rows. A guest that collides on a unique value
     * (code / cloudKey) is skipped and reported, instead of silently deleting the colliding guest.
     */
    private suspend fun insertGuestsReportingConflicts(
        toInsert: List<GuestEntity>,
        skipped: MutableList<SkippedGuestItem>
    ): Int {
        if (toInsert.isEmpty()) return 0
        val prepared = toInsert.map { g ->
            if (g.cloudKey.isBlank()) g.copy(cloudKey = UUID.randomUUID().toString()) else g
        }
        val rowIds = dao.insertGuests(prepared)
        var inserted = 0
        rowIds.forEachIndexed { index, rowId ->
            if (rowId == -1L) {
                skipped.add(
                    SkippedGuestItem(
                        inputLine = prepared[index].name,
                        reasonAr = "تعذرت الإضافة بسبب تعارض في كود الدعوة",
                        reasonEn = "Could not add: invitation code conflict"
                    )
                )
            } else {
                inserted++
                val insertedGuest = dao.getGuestById(rowId) ?: prepared[index]
                recordGuestSync(insertedGuest, "UPSERT")
            }
        }
        return inserted
    }

    /**
     * Method 1: Add guests by names (one per line, optional phone number after name).
     */
    suspend fun addGuestsByNames(eventId: Long, rawText: String): AddGuestsResult {
        val existingGuests = dao.getGuestsForEventOnce(eventId)
        val existingNames = existingGuests.map { it.normalizedName }.toMutableSet()
        val existingPhones = existingGuests
            .map { it.normalizedPhone }
            .filter { it.isNotEmpty() }
            .toMutableSet()
        val usedCodes = dao.getAllGuestCodes().toMutableSet()
        var nextSeq = (dao.getMaxSequenceForEvent(eventId) ?: 0) + 1

        val toInsert = mutableListOf<GuestEntity>()
        val skipped = mutableListOf<SkippedGuestItem>()

        val lines = rawText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        for (line in lines) {
            val normalizedLineDigits = normalizeDigits(line)
            val parsed = extractNameAndOptionalPhone(normalizedLineDigits)
            val rawName = parsed.first.trim()
            val rawPhone = parsed.second.trim()

            if (rawName.isEmpty()) {
                skipped.add(
                    SkippedGuestItem(
                        inputLine = line,
                        reasonAr = "سطر بدون اسم واضح",
                        reasonEn = "Line has no valid guest name"
                    )
                )
                continue
            }

            val normName = normalizeNameForComparison(rawName)
            if (normName in existingNames) {
                skipped.add(
                    SkippedGuestItem(
                        inputLine = line,
                        reasonAr = "الاسم مكرر في نفس المناسبة",
                        reasonEn = "Duplicate guest name in this event"
                    )
                )
                continue
            }

            var cleanPhone = ""
            var normPhone = ""
            if (rawPhone.isNotEmpty()) {
                val digitsOnly = rawPhone.filter { it.isDigit() || it == '+' }
                val pureDigits = digitsOnly.filter { it.isDigit() }
                if (pureDigits.length < 8) {
                    skipped.add(
                        SkippedGuestItem(
                            inputLine = line,
                            reasonAr = "رقم الموبايل أقل من 8 أرقام",
                            reasonEn = "Phone number has fewer than 8 digits"
                        )
                    )
                    continue
                }
                normPhone = normalizePhoneForComparison(pureDigits)
                if (normPhone in existingPhones) {
                    skipped.add(
                        SkippedGuestItem(
                            inputLine = line,
                            reasonAr = "رقم الموبايل مكرر في المناسبة",
                            reasonEn = "Duplicate phone number in this event"
                        )
                    )
                    continue
                }
                cleanPhone = digitsOnly
                existingPhones.add(normPhone)
            }

            existingNames.add(normName)
            val code = generateUnique8DigitCode(usedCodes)
            toInsert.add(
                GuestEntity(
                    eventId = eventId,
                    name = rawName.replace(Regex("\\s+"), " "),
                    normalizedName = normName,
                    phone = cleanPhone,
                    normalizedPhone = normPhone,
                    code = code,
                    sequenceNumber = nextSeq++
                )
            )
        }

        val added = insertGuestsReportingConflicts(toInsert, skipped)
        return AddGuestsResult(addedCount = added, skippedItems = skipped)
    }

    /**
     * Method 2: Add guests by number sequence.
     */
    suspend fun addGuestsBySequence(
        eventId: Long,
        fromNumber: Int,
        toNumber: Int,
        optionalPrefix: String
    ): AddGuestsResult {
        val start = minOf(fromNumber, toNumber).coerceAtLeast(1)
        val end = maxOf(fromNumber, toNumber)
        val count = (end - start + 1).coerceAtMost(5000)
        val actualEnd = start + count - 1

        val existingGuests = dao.getGuestsForEventOnce(eventId)
        val existingNames = existingGuests.map { it.normalizedName }.toMutableSet()
        val usedCodes = dao.getAllGuestCodes().toMutableSet()
        var nextSeq = (dao.getMaxSequenceForEvent(eventId) ?: 0) + 1

        val cleanPrefix = optionalPrefix.trim()
        val toInsert = mutableListOf<GuestEntity>()
        val skipped = mutableListOf<SkippedGuestItem>()

        for (num in start..actualEnd) {
            val guestName = if (cleanPrefix.isEmpty()) "$num" else "$cleanPrefix $num"
            val normName = normalizeNameForComparison(guestName)
            if (normName in existingNames) {
                skipped.add(
                    SkippedGuestItem(
                        inputLine = guestName,
                        reasonAr = "الاسم/الرقم موجود بالفعل",
                        reasonEn = "Name/number already exists"
                    )
                )
                continue
            }
            existingNames.add(normName)
            val code = generateUnique8DigitCode(usedCodes)
            toInsert.add(
                GuestEntity(
                    eventId = eventId,
                    name = guestName,
                    normalizedName = normName,
                    phone = "",
                    normalizedPhone = "",
                    code = code,
                    sequenceNumber = nextSeq++
                )
            )
        }

        val added = insertGuestsReportingConflicts(toInsert, skipped)
        return AddGuestsResult(addedCount = added, skippedItems = skipped)
    }

    /**
     * Method 3: Add guests by phone numbers list.
     */
    suspend fun addGuestsByPhones(eventId: Long, rawPhonesText: String): AddGuestsResult {
        val existingGuests = dao.getGuestsForEventOnce(eventId)
        val existingNames = existingGuests.map { it.normalizedName }.toMutableSet()
        val existingPhones = existingGuests
            .map { it.normalizedPhone }
            .filter { it.isNotEmpty() }
            .toMutableSet()
        val usedCodes = dao.getAllGuestCodes().toMutableSet()
        var nextSeq = (dao.getMaxSequenceForEvent(eventId) ?: 0) + 1

        val toInsert = mutableListOf<GuestEntity>()
        val skipped = mutableListOf<SkippedGuestItem>()

        var autoNumber = nextSeq
        val lines = rawPhonesText.lines().map { it.trim() }.filter { it.isNotEmpty() }
        for (line in lines) {
            val normalizedDigitsLine = normalizeDigits(line)
            val digitsAndPlus = normalizedDigitsLine.filter { it.isDigit() || it == '+' }
            val pureDigits = digitsAndPlus.filter { it.isDigit() }

            if (pureDigits.length < 8) {
                skipped.add(
                    SkippedGuestItem(
                        inputLine = line,
                        reasonAr = "رقم أقل من 8 أرقام",
                        reasonEn = "Phone number has fewer than 8 digits"
                    )
                )
                continue
            }

            val normPhone = normalizePhoneForComparison(pureDigits)
            if (normPhone in existingPhones) {
                skipped.add(
                    SkippedGuestItem(
                        inputLine = line,
                        reasonAr = "رقم موبايل مكرر",
                        reasonEn = "Duplicate phone number"
                    )
                )
                continue
            }

            while (normalizeNameForComparison(autoNumber.toString()) in existingNames) {
                autoNumber++
            }
            val guestName = autoNumber.toString()
            val normName = normalizeNameForComparison(guestName)
            autoNumber++

            existingNames.add(normName)
            existingPhones.add(normPhone)
            val code = generateUnique8DigitCode(usedCodes)

            toInsert.add(
                GuestEntity(
                    eventId = eventId,
                    name = guestName,
                    normalizedName = normName,
                    phone = digitsAndPlus,
                    normalizedPhone = normPhone,
                    code = code,
                    sequenceNumber = nextSeq++
                )
            )
        }

        val added = insertGuestsReportingConflicts(toInsert, skipped)
        return AddGuestsResult(addedCount = added, skippedItems = skipped)
    }

    /**
     * Barcode format: eb:<eventId>:<guestCode>
     */
    suspend fun verifyAndCheckInBarcode(currentEventId: Long, rawBarcode: String): ScanOutcome {
        val trimmed = normalizeDigits(rawBarcode.trim())
        if (trimmed.isEmpty()) {
            return ScanOutcome.UnknownInvitation(rawBarcode)
        }

        val parsedEventId: Long?
        val parsedCode: String

        if (trimmed.startsWith("eb:", ignoreCase = true)) {
            val parts = trimmed.split(":")
            if (parts.size != 3) {
                return ScanOutcome.UnknownInvitation(rawBarcode)
            }
            parsedEventId = parts[1].toLongOrNull()
            parsedCode = parts[2].trim()
            if (parsedEventId == null || parsedCode.isEmpty()) {
                return ScanOutcome.UnknownInvitation(rawBarcode)
            }
        } else if (trimmed.length == 8 && trimmed.all { it.isDigit() }) {
            val foundGuest = dao.getGuestByCode(trimmed)
                ?: return ScanOutcome.UnknownInvitation(rawBarcode)
            parsedEventId = foundGuest.eventId
            parsedCode = foundGuest.code
        } else {
            return ScanOutcome.UnknownInvitation(rawBarcode)
        }

        val activeEvent = dao.getEventById(currentEventId)
            ?: return ScanOutcome.UnknownInvitation(rawBarcode)

        val guest = dao.getGuestByCode(parsedCode)
            ?: return ScanOutcome.UnknownInvitation(rawBarcode)

        val guestParentEvent = dao.getEventById(guest.eventId)
        val isSameEvent = (guest.eventId == activeEvent.id) ||
                (guestParentEvent != null && guestParentEvent.cloudKey.isNotBlank() && guestParentEvent.cloudKey == activeEvent.cloudKey)

        if (!isSameEvent) {
            return ScanOutcome.WrongEvent(
                guestCode = parsedCode,
                otherEventId = guest.eventId,
                otherEventName = guestParentEvent?.name ?: activeEvent.name
            )
        }

        if (guest.attended) {
            return ScanOutcome.AlreadyCheckedIn(
                guest = guest,
                firstCheckInTimeMillis = guest.attendedAt ?: System.currentTimeMillis()
            )
        }

        val now = System.currentTimeMillis()
        val updated = guest.copy(attended = true, attendedAt = now, updatedAt = now)
        dao.updateGuest(updated)
        recordGuestSync(updated, "UPDATE_ATTENDANCE")
        return ScanOutcome.CheckedInSuccess(guest = updated, checkInTimeMillis = now)
    }

    suspend fun exportSingleEventSecure(eventId: Long, password: String): String? {
        val event = dao.getEventById(eventId) ?: return null
        val guests = dao.getGuestsForEventOnce(eventId)
        return SecureEventExporter.exportSecureEventJson(event, guests, password)
    }

    suspend fun importSingleEventSecure(encryptedJson: String, password: String): BackupImportResult {
        val pair = SecureEventExporter.decryptAndParseSecureEvent(encryptedJson, password)
            ?: return BackupImportResult.Invalid
        val (event, guests) = pair
        val backupJson = JSONObject().apply {
            put("app", "elbeit elaraby")
            put("version", BackupImporter.CURRENT_BACKUP_VERSION)
            put("exportedAt", System.currentTimeMillis())
            put("events", JSONArray().put(JSONObject().apply {
                put("id", event.id)
                put("name", event.name)
                put("date", event.date)
                put("timeFrom", event.timeFrom)
                put("timeTo", event.timeTo)
                put("backgroundType", "PRESET")
                put("backgroundPresetId", event.backgroundPresetId)
                put("backgroundAspectRatio", 0.714)
                put("nameColorHex", event.nameColorHex)
                put("qrColorHex", event.qrColorHex)
                put("qrBgColorHex", event.qrBgColorHex)
                put("fontIndex", event.fontIndex)
                put("showGuestName", event.showGuestName)
                put("showDate", event.showDate)
                put("showTime", event.showTime)
                put("qrSizeRatio", event.qrSizeRatio.toDouble())
                put("nameSizeSp", event.nameSizeSp.toDouble())
                put("qrPosX", event.qrPosX.toDouble())
                put("qrPosY", event.qrPosY.toDouble())
                put("namePosX", event.namePosX.toDouble())
                put("namePosY", event.namePosY.toDouble())
                put("datePosX", event.datePosX.toDouble())
                put("datePosY", event.datePosY.toDouble())
                put("whatsappTemplate", event.whatsappTemplate)
                put("createdAt", event.createdAt)
                put("groomName", event.groomName)
                put("brideName", event.brideName)
                put("headerVerse", event.headerVerse)
                put("hostsText", event.hostsText)
                put("dayName", event.dayName)
                put("venueName", event.venueName)
                put("venueAddress", event.venueAddress)
                put("locationUrl", event.locationUrl)
                put("showLocationQr", event.showLocationQr)
                put("notesText", event.notesText)
                put("closingText", event.closingText)
                put("cloudKey", event.cloudKey)
            }))
            val guestsArr = JSONArray()
            for (g in guests) {
                guestsArr.put(JSONObject().apply {
                    put("id", g.id)
                    put("eventId", g.eventId)
                    put("name", g.name)
                    put("normalizedName", g.normalizedName)
                    put("phone", g.phone)
                    put("normalizedPhone", g.normalizedPhone)
                    put("code", g.code)
                    put("sequenceNumber", g.sequenceNumber)
                    put("attended", g.attended)
                    if (g.attendedAt != null) put("attendedAt", g.attendedAt)
                    put("whatsappSent", g.whatsappSent)
                    put("createdAt", g.createdAt)
                    put("cloudKey", g.cloudKey)
                })
            }
            put("guests", guestsArr)
        }.toString()

        return importBackupJson(backupJson)
    }

    suspend fun exportBackupJson(): String {
        val events = dao.getAllEventsOnce()
        val guests = dao.getAllGuestsOnce()

        val root = JSONObject()
        root.put("app", "elbeit elaraby")
        root.put("version", BackupImporter.CURRENT_BACKUP_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        val eventsArray = JSONArray()
        for (e in events) {
            val obj = JSONObject()
            obj.put("id", e.id)
            obj.put("name", e.name)
            obj.put("date", e.date)
            obj.put("timeFrom", e.timeFrom)
            obj.put("timeTo", e.timeTo)
            obj.put("backgroundType", "PRESET")
            obj.put("backgroundPresetId", e.backgroundPresetId)
            obj.put("backgroundAspectRatio", 0.714)
            obj.put("nameColorHex", e.nameColorHex)
            obj.put("qrColorHex", e.qrColorHex)
            obj.put("qrBgColorHex", e.qrBgColorHex)
            obj.put("fontIndex", e.fontIndex)
            obj.put("showGuestName", e.showGuestName)
            obj.put("showDate", e.showDate)
            obj.put("showTime", e.showTime)
            obj.put("qrSizeRatio", e.qrSizeRatio.toDouble())
            obj.put("nameSizeSp", e.nameSizeSp.toDouble())
            obj.put("qrPosX", e.qrPosX.toDouble())
            obj.put("qrPosY", e.qrPosY.toDouble())
            obj.put("namePosX", e.namePosX.toDouble())
            obj.put("namePosY", e.namePosY.toDouble())
            obj.put("datePosX", e.datePosX.toDouble())
            obj.put("datePosY", e.datePosY.toDouble())
            obj.put("whatsappTemplate", e.whatsappTemplate)
            obj.put("createdAt", e.createdAt)
            obj.put("groomName", e.groomName)
            obj.put("brideName", e.brideName)
            obj.put("headerVerse", e.headerVerse)
            obj.put("hostsText", e.hostsText)
            obj.put("dayName", e.dayName)
            obj.put("venueName", e.venueName)
            obj.put("venueAddress", e.venueAddress)
            obj.put("locationUrl", e.locationUrl)
            obj.put("showLocationQr", e.showLocationQr)
            obj.put("notesText", e.notesText)
            obj.put("closingText", e.closingText)
            obj.put("cloudKey", e.cloudKey)
            eventsArray.put(obj)
        }
        root.put("events", eventsArray)

        val guestsArray = JSONArray()
        for (g in guests) {
            val obj = JSONObject()
            obj.put("id", g.id)
            obj.put("eventId", g.eventId)
            obj.put("name", g.name)
            obj.put("normalizedName", g.normalizedName)
            obj.put("phone", g.phone)
            obj.put("normalizedPhone", g.normalizedPhone)
            obj.put("code", g.code)
            obj.put("sequenceNumber", g.sequenceNumber)
            obj.put("attended", g.attended)
            if (g.attendedAt != null) {
                obj.put("attendedAt", g.attendedAt)
            }
            obj.put("whatsappSent", g.whatsappSent)
            obj.put("createdAt", g.createdAt)
            obj.put("cloudKey", g.cloudKey)
            guestsArray.put(obj)
        }
        root.put("guests", guestsArray)
        return root.toString(2)
    }

    suspend fun importBackupJson(jsonString: String): BackupImportResult {
        val parsed = BackupImporter.parse(jsonString) ?: return BackupImportResult.Invalid
        return try {
            when (val outcome = BackupImporter.plan(parsed, dao.getAllEventsOnce(), dao.getAllGuestsOnce())) {
                is BackupImporter.PlanOutcome.Conflict ->
                    BackupImportResult.Conflict(outcome.reasonAr, outcome.reasonEn)

                is BackupImporter.PlanOutcome.Ready -> {
                    val plan = outcome.plan
                    dao.applyBackupImport(plan.newEvents, plan.newGuests, plan.mergedGuests)
                    BackupImportResult.Success(
                        eventsAdded = plan.newEvents.size,
                        eventsMerged = plan.eventsMerged,
                        guestsAdded = plan.newGuests.size,
                        guestsMerged = plan.guestsMerged
                    )
                }
            }
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            BackupImportResult.Conflict(
                reasonAr = "تعذّر تطبيق الاستيراد وتم التراجع عنه دون تغيير أي بيانات.",
                reasonEn = "The import could not be applied and was rolled back; no data was changed."
            )
        }
    }

    suspend fun restoreFromBackupJson(jsonString: String): Boolean {
        return importBackupJson(jsonString) is BackupImportResult.Success
    }

    suspend fun cleanupLegacyDemoDataIfPresent() {
        val currentEvents = dao.getAllEventsOnce()
        for (event in currentEvents) {
            if (event.name == "حفل زفاف المهندس كريم والأستاذة سارة" && event.date == "2026-11-20") {
                dao.deleteEventById(event.id)
            }
        }
    }

    /**
     * Seeds all existing local events and guests into sync_queue if they are not already queued.
     * Re-runnable safely and idempotently without creating duplicate sync queue items.
     */
    suspend fun seedAllLocalDataToSyncQueue(): Int {
        val existingQueue = dao.getAllSyncQueueOnce()
        val queuedKeys = existingQueue.map { it.entityCloudKey }.toSet()

        val allEvents = dao.getAllEventsOnce()
        val allGuests = dao.getAllGuestsOnce()
        var seededCount = 0

        for (event in allEvents) {
            val key = if (event.cloudKey.isBlank()) {
                val newKey = UUID.randomUUID().toString()
                dao.updateEvent(event.copy(cloudKey = newKey))
                newKey
            } else event.cloudKey

            if (key !in queuedKeys) {
                val updatedEvent = dao.getEventById(event.id) ?: event
                recordEventSync(updatedEvent, "UPSERT")
                seededCount++
            }
        }

        for (guest in allGuests) {
            val key = if (guest.cloudKey.isBlank()) {
                val newKey = UUID.randomUUID().toString()
                dao.updateGuest(guest.copy(cloudKey = newKey))
                newKey
            } else guest.cloudKey

            if (key !in queuedKeys) {
                val updatedGuest = dao.getGuestById(guest.id) ?: guest
                recordGuestSync(updatedGuest, "UPSERT")
                seededCount++
            }
        }

        if (seededCount > 0) {
            context?.let { SyncWorker.enqueue(it) }
        }
        return seededCount
    }

    companion object {
        fun normalizeDigits(input: String): String {
            val sb = StringBuilder(input.length)
            for (ch in input) {
                when (ch) {
                    in '٠'..'٩' -> sb.append((ch - '٠' + '0'.code).toChar())
                    in '۰'..'۹' -> sb.append((ch - '۰' + '0'.code).toChar())
                    else -> sb.append(ch)
                }
            }
            return sb.toString()
        }

        fun normalizeNameForComparison(name: String): String {
            return normalizeDigits(name)
                .trim()
                .replace(Regex("\\s+"), " ")
                .lowercase()
        }

        fun normalizePhoneForComparison(phone: String): String {
            val digits = normalizeDigits(phone).filter { it.isDigit() }
            return when {
                digits.startsWith("20") && digits.length == 12 -> "0" + digits.substring(2)
                digits.startsWith("0020") && digits.length == 14 -> "0" + digits.substring(4)
                else -> digits
            }
        }

        fun formatWhatsAppPhone(phone: String): String {
            val digits = normalizeDigits(phone).filter { it.isDigit() }
            return when {
                digits.startsWith("00") -> digits.substring(2)
                digits.startsWith("0") -> "20" + digits.substring(1)
                else -> digits
            }
        }

        fun extractNameAndOptionalPhone(line: String): Pair<String, String> {
            val trimmed = line.trim()
            val regex = Regex("^(.*?)\\s+([+]?[0-9][0-9\\s\\-]{5,})$")
            val match = regex.find(trimmed)
            if (match != null) {
                val candidateName = match.groupValues[1].trim()
                val candidatePhone = match.groupValues[2].replace(Regex("[\\s\\-]"), "")
                if (candidateName.isNotEmpty()) {
                    return candidateName to candidatePhone
                }
            }
            return trimmed to ""
        }

        fun generateUnique8DigitCode(usedCodes: MutableSet<String>): String {
            var code: String
            do {
                val number = Random.nextInt(10_000_000, 100_000_000)
                code = number.toString()
            } while (!usedCodes.add(code))
            return code
        }

        fun formatWelcomeMessage(
            template: String,
            guestName: String,
            event: EventEntity
        ): String {
            val timeRange = buildString {
                if (event.timeFrom.isNotBlank()) append(event.timeFrom)
                if (event.timeTo.isNotBlank()) {
                    if (isNotEmpty()) append(" – ")
                    append(event.timeTo)
                }
            }
            return template
                .replace("{name}", guestName)
                .replace("{event}", event.name)
                .replace("{date}", event.date)
                .replace("{time}", timeRange)
        }
    }
}
