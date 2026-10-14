package com.example.data

import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FieldValue
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.firestore.SetOptions
import kotlinx.coroutines.tasks.await
import org.json.JSONObject

sealed class AuthResult {
    data object Success : AuthResult()
    data object InvalidCredentials : AuthResult()
    data object AccountDisabled : AuthResult()
    data object NetworkError : AuthResult()
    data class Error(val message: String) : AuthResult()
}

class FirestoreSyncManager(private val dao: EventDao) {
    private val firestore = FirebaseFirestore.getInstance()
    private val auth = FirebaseAuth.getInstance()

    val currentUserUid: String?
        get() = auth.currentUser?.uid

    suspend fun getUserRole(uid: String): String {
        return try {
            val doc = firestore.collection("users").document(uid).get().await()
            val role = doc.getString("role")?.trim()?.lowercase()
            if (!role.isNullOrEmpty()) role else "admin"
        } catch (_: Exception) {
            "admin"
        }
    }

    suspend fun signInWithEmail(email: String, pass: String): AuthResult {
        return try {
            auth.signInWithEmailAndPassword(email, pass).await()
            AuthResult.Success
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidCredentialsException) {
            AuthResult.InvalidCredentials
        } catch (e: com.google.firebase.auth.FirebaseAuthInvalidUserException) {
            AuthResult.InvalidCredentials
        } catch (e: com.google.firebase.auth.FirebaseAuthUserCollisionException) {
            AuthResult.AccountDisabled
        } catch (e: com.google.firebase.FirebaseNetworkException) {
            AuthResult.NetworkError
        } catch (e: Exception) {
            val msg = e.message?.lowercase() ?: ""
            if (msg.contains("invalid") || msg.contains("password") || msg.contains("user-not-found") || msg.contains("credential") || msg.contains("malformed") || msg.contains("no user record")) {
                AuthResult.InvalidCredentials
            } else if (msg.contains("disabled") || msg.contains("blocked")) {
                AuthResult.AccountDisabled
            } else if (msg.contains("network") || msg.contains("connect") || msg.contains("host") || msg.contains("timeout") || e is java.io.IOException || e is java.net.UnknownHostException || e is java.net.SocketTimeoutException) {
                AuthResult.NetworkError
            } else {
                AuthResult.Error(e.message ?: "Unknown error")
            }
        }
    }

    fun signOut() {
        auth.signOut()
    }

    suspend fun pushEvent(event: EventEntity) {
        val eventMap = mapOf(
            "cloudKey" to event.cloudKey,
            "legacyLocalId" to event.id,
            "name" to event.name,
            "date" to event.date,
            "timeFrom" to event.timeFrom,
            "timeTo" to event.timeTo,
            "backgroundType" to event.backgroundType,
            "backgroundPresetId" to event.backgroundPresetId,
            "backgroundAspectRatio" to event.backgroundAspectRatio,
            "nameColorHex" to event.nameColorHex,
            "qrColorHex" to event.qrColorHex,
            "qrBgColorHex" to event.qrBgColorHex,
            "fontIndex" to event.fontIndex,
            "showGuestName" to event.showGuestName,
            "showDate" to event.showDate,
            "showTime" to event.showTime,
            "qrSizeRatio" to event.qrSizeRatio,
            "nameSizeSp" to event.nameSizeSp,
            "qrPosX" to event.qrPosX,
            "qrPosY" to event.qrPosY,
            "namePosX" to event.namePosX,
            "namePosY" to event.namePosY,
            "datePosX" to event.datePosX,
            "datePosY" to event.datePosY,
            "whatsappTemplate" to event.whatsappTemplate,
            "createdAt" to event.createdAt,
            "groomName" to event.groomName,
            "brideName" to event.brideName,
            "headerVerse" to event.headerVerse,
            "hostsText" to event.hostsText,
            "dayName" to event.dayName,
            "venueName" to event.venueName,
            "venueAddress" to event.venueAddress,
            "locationUrl" to event.locationUrl,
            "showLocationQr" to event.showLocationQr,
            "notesText" to event.notesText,
            "closingText" to event.closingText,
            "updatedAt" to FieldValue.serverTimestamp(),
            "isDeleted" to event.isDeleted,
            "createdBy" to (currentUserUid ?: "")
        )
        firestore.collection("events").document(event.cloudKey)
            .set(eventMap, SetOptions.merge())
            .await()
    }

    suspend fun pushGuest(eventCloudKey: String, guest: GuestEntity) {
        // Public guest verification document for reception (without phone numbers)
        val guestMap = mapOf(
            "cloudKey" to guest.cloudKey,
            "eventCloudKey" to eventCloudKey,
            "name" to guest.name,
            "normalizedName" to guest.normalizedName,
            "code" to guest.code,
            "sequenceNumber" to guest.sequenceNumber,
            "whatsappSent" to guest.whatsappSent,
            "attended" to guest.attended,
            "attendedAt" to guest.attendedAt,
            "updatedAt" to FieldValue.serverTimestamp(),
            "isDeleted" to guest.isDeleted
        )
        firestore.collection("events").document(eventCloudKey)
            .collection("guests").document(guest.cloudKey)
            .set(guestMap, SetOptions.merge())
            .await()

        // Private contact document for admin only
        if (guest.phone.isNotBlank() || guest.normalizedPhone.isNotBlank()) {
            val contactMap = mapOf(
                "cloudKey" to guest.cloudKey,
                "phone" to guest.phone,
                "normalizedPhone" to guest.normalizedPhone,
                "updatedAt" to FieldValue.serverTimestamp()
            )
            firestore.collection("events").document(eventCloudKey)
                .collection("guest_contacts").document(guest.cloudKey)
                .set(contactMap, SetOptions.merge())
                .await()
        }
    }

    /**
     * Downloads guests for a single active event selected for reception check-in.
     * Downloads public guest verification fields (without phone numbers) and merges non-destructively.
     */
    suspend fun fetchSingleEventGuestsForReception(eventCloudKey: String): List<GuestEntity> {
        val snapshot = firestore.collection("events").document(eventCloudKey)
            .collection("guests").get().await()

        val localEvent = dao.getEventByCloudKey(eventCloudKey) ?: return emptyList()
        val downloadedGuests = mutableListOf<GuestEntity>()

        for (gDoc in snapshot.documents) {
            val isDeleted = gDoc.getBoolean("isDeleted") ?: false
            if (isDeleted) continue

            val gCloudKey = gDoc.getString("cloudKey") ?: gDoc.id
            val code = gDoc.getString("code") ?: continue
            val name = gDoc.getString("name") ?: continue
            val attended = gDoc.getBoolean("attended") ?: false
            val attendedAt = gDoc.getLong("attendedAt")

            val existingLocal = dao.getGuestByCloudKey(gCloudKey) ?: dao.getGuestByCode(code)
            if (existingLocal != null) {
                // Non-destructive merge: preserve local attendance if already attended
                val finalAttended = existingLocal.attended || attended
                val finalAttendedAt = if (existingLocal.attended) existingLocal.attendedAt else (attendedAt ?: existingLocal.attendedAt)
                val updated = existingLocal.copy(
                    attended = finalAttended,
                    attendedAt = finalAttendedAt,
                    updatedAt = System.currentTimeMillis()
                )
                dao.updateGuest(updated)
                downloadedGuests.add(updated)
            } else {
                val newGuest = GuestEntity(
                    eventId = localEvent.id,
                    name = name,
                    normalizedName = gDoc.getString("normalizedName") ?: EventRepository.normalizeNameForComparison(name),
                    phone = "", // Excluded for reception
                    normalizedPhone = "",
                    code = code,
                    sequenceNumber = (gDoc.getLong("sequenceNumber") ?: 1L).toInt(),
                    attended = attended,
                    attendedAt = attendedAt,
                    whatsappSent = gDoc.getBoolean("whatsappSent") ?: false,
                    createdAt = System.currentTimeMillis(),
                    cloudKey = gCloudKey,
                    updatedAt = System.currentTimeMillis(),
                    isDeleted = false
                )
                dao.insertGuests(listOf(newGuest))
                downloadedGuests.add(newGuest)
            }
        }
        return downloadedGuests
    }

    suspend fun pushAttendance(eventCloudKey: String, guestCloudKey: String, attendedAt: Long, deviceId: String) {
        val updateMap = mapOf(
            "attended" to true,
            "attendedAt" to attendedAt,
            "attendedByUid" to (currentUserUid ?: ""),
            "checkInDeviceId" to deviceId,
            "updatedAt" to FieldValue.serverTimestamp()
        )
        firestore.collection("events").document(eventCloudKey)
            .collection("guests").document(guestCloudKey)
            .update(updateMap)
            .await()
    }

    suspend fun syncQueueItem(item: SyncQueueEntity) {
        when (item.entityType) {
            "EVENT" -> {
                if (item.actionType == "DELETE") {
                    firestore.collection("events").document(item.entityCloudKey)
                        .update("isDeleted", true, "updatedAt", FieldValue.serverTimestamp())
                        .await()
                } else {
                    val jsonObj = JSONObject(item.payloadJson)
                    val map = mutableMapOf<String, Any?>()
                    val keys = jsonObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        map[key] = if (jsonObj.isNull(key)) null else jsonObj.get(key)
                    }
                    map["updatedAt"] = FieldValue.serverTimestamp()
                    map["createdBy"] = (currentUserUid ?: "")
                    firestore.collection("events").document(item.entityCloudKey)
                        .set(map, SetOptions.merge())
                        .await()
                }
            }
            "GUEST" -> {
                val parentKey = item.parentCloudKey ?: return
                if (item.actionType == "DELETE") {
                    firestore.collection("events").document(parentKey)
                        .collection("guests").document(item.entityCloudKey)
                        .update("isDeleted", true, "updatedAt", FieldValue.serverTimestamp())
                        .await()
                } else {
                    val jsonObj = JSONObject(item.payloadJson)
                    val map = mutableMapOf<String, Any?>()
                    val keys = jsonObj.keys()
                    while (keys.hasNext()) {
                        val key = keys.next()
                        map[key] = if (jsonObj.isNull(key)) null else jsonObj.get(key)
                    }
                    map["updatedAt"] = FieldValue.serverTimestamp()
                    firestore.collection("events").document(parentKey)
                        .collection("guests").document(item.entityCloudKey)
                        .set(map, SetOptions.merge())
                        .await()
                }
            }
        }
    }

    private fun optLong(doc: com.google.firebase.firestore.DocumentSnapshot, field: String, default: Long = 0L): Long {
        val raw = doc.get(field) ?: return default
        return when (raw) {
            is Number -> raw.toLong()
            is com.google.firebase.Timestamp -> raw.toDate().time
            is String -> raw.toLongOrNull() ?: default
            else -> default
        }
    }

    private fun optInt(doc: com.google.firebase.firestore.DocumentSnapshot, field: String, default: Int = 0): Int {
        val raw = doc.get(field) ?: return default
        return when (raw) {
            is Number -> raw.toInt()
            is String -> raw.toIntOrNull() ?: default
            else -> default
        }
    }

    private fun optDouble(doc: com.google.firebase.firestore.DocumentSnapshot, field: String, default: Double = 0.0): Double {
        val raw = doc.get(field) ?: return default
        return when (raw) {
            is Number -> raw.toDouble()
            is String -> raw.toDoubleOrNull() ?: default
            else -> default
        }
    }

    suspend fun pullAndMergeFromCloud() {
        try {
            val snapshot = firestore.collection("events").get().await()
            if (snapshot.isEmpty) return

            val localEvents = dao.getAllEventsOnce()
            val localEventsByKey = localEvents.associateBy { it.cloudKey }

            for (doc in snapshot.documents) {
                val cloudKey = doc.getString("cloudKey") ?: doc.id
                val name = doc.getString("name") ?: continue
                val legacyLocalId = optLong(doc, "legacyLocalId", 0L)
                val isDeleted = doc.getBoolean("isDeleted") ?: false

                val existingEvent = localEventsByKey[cloudKey] ?: localEvents.firstOrNull { it.id == legacyLocalId }
                if (existingEvent != null) {
                    if (isDeleted) continue
                } else {
                    if (!isDeleted) {
                        val newEvent = EventEntity(
                            id = if (legacyLocalId > 0 && localEvents.none { it.id == legacyLocalId }) legacyLocalId else 0L,
                            name = name,
                            date = doc.getString("date") ?: "",
                            timeFrom = doc.getString("timeFrom") ?: "",
                            timeTo = doc.getString("timeTo") ?: "",
                            backgroundType = doc.getString("backgroundType") ?: "PRESET",
                            backgroundPresetId = optInt(doc, "backgroundPresetId", 0),
                            backgroundAspectRatio = optDouble(doc, "backgroundAspectRatio", 0.714).toFloat(),
                            nameColorHex = doc.getString("nameColorHex") ?: "#FFFDF6E2",
                            qrColorHex = doc.getString("qrColorHex") ?: "#FF121826",
                            qrBgColorHex = doc.getString("qrBgColorHex") ?: "#FFFFFFFF",
                            fontIndex = optInt(doc, "fontIndex", 0),
                            showGuestName = doc.getBoolean("showGuestName") ?: true,
                            showDate = doc.getBoolean("showDate") ?: true,
                            showTime = doc.getBoolean("showTime") ?: true,
                            qrSizeRatio = optDouble(doc, "qrSizeRatio", 0.28).toFloat(),
                            nameSizeSp = optDouble(doc, "nameSizeSp", 22.0).toFloat(),
                            qrPosX = optDouble(doc, "qrPosX", 0.5).toFloat(),
                            qrPosY = optDouble(doc, "qrPosY", 0.48).toFloat(),
                            namePosX = optDouble(doc, "namePosX", 0.5).toFloat(),
                            namePosY = optDouble(doc, "namePosY", 0.76).toFloat(),
                            datePosX = optDouble(doc, "datePosX", 0.5).toFloat(),
                            datePosY = optDouble(doc, "datePosY", 0.88).toFloat(),
                            whatsappTemplate = doc.getString("whatsappTemplate") ?: EventEntity.DEFAULT_WHATSAPP_TEMPLATE_AR,
                            createdAt = optLong(doc, "createdAt", System.currentTimeMillis()),
                            groomName = doc.getString("groomName") ?: "",
                            brideName = doc.getString("brideName") ?: "",
                            headerVerse = doc.getString("headerVerse") ?: "",
                            hostsText = doc.getString("hostsText") ?: "",
                            dayName = doc.getString("dayName") ?: "",
                            venueName = doc.getString("venueName") ?: "",
                            venueAddress = doc.getString("venueAddress") ?: "",
                            locationUrl = doc.getString("locationUrl") ?: "",
                            showLocationQr = doc.getBoolean("showLocationQr") ?: true,
                            notesText = doc.getString("notesText") ?: "",
                            closingText = doc.getString("closingText") ?: "",
                            cloudKey = cloudKey,
                            updatedAt = optLong(doc, "updatedAt", 0L),
                            isDeleted = false
                        )
                        val insertedId = dao.insertEvent(newEvent)

                        val guestSnapshot = firestore.collection("events").document(cloudKey).collection("guests").get().await()
                        val guestsToInsert = mutableListOf<GuestEntity>()
                        for (gDoc in guestSnapshot.documents) {
                            val gCloudKey = gDoc.getString("cloudKey") ?: gDoc.id
                            val gName = gDoc.getString("name") ?: continue
                            val attended = gDoc.getBoolean("attended") ?: false
                            val attendedAt = if (gDoc.get("attendedAt") != null) optLong(gDoc, "attendedAt", 0L) else null
                            guestsToInsert.add(
                                GuestEntity(
                                    eventId = insertedId,
                                    name = gName,
                                    normalizedName = gDoc.getString("normalizedName") ?: EventRepository.normalizeNameForComparison(gName),
                                    phone = gDoc.getString("phone") ?: "",
                                    normalizedPhone = gDoc.getString("normalizedPhone") ?: "",
                                    code = gDoc.getString("code") ?: "",
                                    sequenceNumber = optInt(gDoc, "sequenceNumber", 1),
                                    attended = attended,
                                    attendedAt = attendedAt,
                                    whatsappSent = gDoc.getBoolean("whatsappSent") ?: false,
                                    createdAt = optLong(gDoc, "createdAt", System.currentTimeMillis()),
                                    cloudKey = gCloudKey,
                                    updatedAt = optLong(gDoc, "updatedAt", 0L),
                                    isDeleted = gDoc.getBoolean("isDeleted") ?: false
                                )
                            )
                        }
                        if (guestsToInsert.isNotEmpty()) {
                            dao.insertGuests(guestsToInsert)
                        }
                    }
                }
            }
        } catch (_: Exception) {
        }
    }
}
