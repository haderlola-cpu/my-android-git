package com.example.data

import android.util.Base64
import org.json.JSONArray
import org.json.JSONObject
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.SecretKeyFactory
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec

object SecureEventExporter {

    const val CURRENT_SECURE_EXPORT_VERSION = 1

    /**
     * Exports a single event and its guests to a secure export format, encrypted using AES-GCM and PBKDF2.
     * Reverts custom image/video background URIs to PRESET (backgroundType = "PRESET", backgroundUri = null).
     * Includes cloudKey, legacyLocalId (event.id), and all event design and wedding details.
     */
    fun exportSecureEventJson(event: EventEntity, guests: List<GuestEntity>, password: String): String {
        val root = JSONObject()
        root.put("app", "elbeit elaraby")
        root.put("exportType", "SINGLE_EVENT_SECURE")
        root.put("version", CURRENT_SECURE_EXPORT_VERSION)
        root.put("exportedAt", System.currentTimeMillis())

        val eventObj = JSONObject()
        eventObj.put("legacyLocalId", event.id)
        eventObj.put("cloudKey", event.cloudKey)
        eventObj.put("name", event.name)
        eventObj.put("date", event.date)
        eventObj.put("timeFrom", event.timeFrom)
        eventObj.put("timeTo", event.timeTo)
        // Revert custom image/video background URIs to PRESET as specified
        eventObj.put("backgroundType", "PRESET")
        eventObj.put("backgroundPresetId", event.backgroundPresetId)
        eventObj.put("backgroundUri", JSONObject.NULL)
        eventObj.put("backgroundAspectRatio", event.backgroundAspectRatio.toDouble())
        eventObj.put("nameColorHex", event.nameColorHex)
        eventObj.put("qrColorHex", event.qrColorHex)
        eventObj.put("qrBgColorHex", event.qrBgColorHex)
        eventObj.put("fontIndex", event.fontIndex)
        eventObj.put("showGuestName", event.showGuestName)
        eventObj.put("showDate", event.showDate)
        eventObj.put("showTime", event.showTime)
        eventObj.put("qrSizeRatio", event.qrSizeRatio.toDouble())
        eventObj.put("nameSizeSp", event.nameSizeSp.toDouble())
        eventObj.put("qrPosX", event.qrPosX.toDouble())
        eventObj.put("qrPosY", event.qrPosY.toDouble())
        eventObj.put("namePosX", event.namePosX.toDouble())
        eventObj.put("namePosY", event.namePosY.toDouble())
        eventObj.put("datePosX", event.datePosX.toDouble())
        eventObj.put("datePosY", event.datePosY.toDouble())
        eventObj.put("whatsappTemplate", event.whatsappTemplate)
        eventObj.put("createdAt", event.createdAt)
        eventObj.put("groomName", event.groomName)
        eventObj.put("brideName", event.brideName)
        eventObj.put("headerVerse", event.headerVerse)
        eventObj.put("hostsText", event.hostsText)
        eventObj.put("dayName", event.dayName)
        eventObj.put("venueName", event.venueName)
        eventObj.put("venueAddress", event.venueAddress)
        eventObj.put("locationUrl", event.locationUrl)
        eventObj.put("showLocationQr", event.showLocationQr)
        eventObj.put("notesText", event.notesText)
        eventObj.put("closingText", event.closingText)
        eventObj.put("updatedAt", event.updatedAt)
        eventObj.put("isDeleted", event.isDeleted)
        root.put("event", eventObj)

        val guestsArray = JSONArray()
        for (g in guests) {
            val obj = JSONObject()
            obj.put("id", g.id)
            obj.put("cloudKey", g.cloudKey)
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
            } else {
                obj.put("attendedAt", JSONObject.NULL)
            }
            obj.put("whatsappSent", g.whatsappSent)
            obj.put("createdAt", g.createdAt)
            obj.put("updatedAt", g.updatedAt)
            obj.put("isDeleted", g.isDeleted)
            guestsArray.put(obj)
        }
        root.put("guests", guestsArray)

        val jsonPlainBytes = root.toString(2).toByteArray(Charsets.UTF_8)

        val salt = ByteArray(16)
        val iv = ByteArray(12)
        SecureRandom().nextBytes(salt)
        SecureRandom().nextBytes(iv)

        val iterations = 10000
        val keySpec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
        val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
        val derivedKey = keyFactory.generateSecret(keySpec).encoded
        val secretKey = SecretKeySpec(derivedKey, "AES")

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, iv)
        cipher.init(Cipher.ENCRYPT_MODE, secretKey, gcmSpec)
        val cipherText = cipher.doFinal(jsonPlainBytes)

        val container = JSONObject()
        container.put("crypto", "AES-GCM-PBKDF2")
        container.put("version", CURRENT_SECURE_EXPORT_VERSION)
        container.put("iterations", iterations)
        container.put("salt", Base64.encodeToString(salt, Base64.NO_WRAP))
        container.put("iv", Base64.encodeToString(iv, Base64.NO_WRAP))
        container.put("ciphertext", Base64.encodeToString(cipherText, Base64.NO_WRAP))

        return container.toString(2)
    }

    /**
     * Decrypts and parses a secure single-event export JSON using the provided password.
     * Validates version and integrity (AES-GCM auth tag).
     */
    fun decryptAndParseSecureEvent(encryptedJson: String, password: String): Pair<EventEntity, List<GuestEntity>>? {
        return try {
            val container = JSONObject(encryptedJson)
            if (container.optString("crypto") != "AES-GCM-PBKDF2") return null
            val version = container.optInt("version", 1)
            if (version > CURRENT_SECURE_EXPORT_VERSION) return null
            val iterations = container.optInt("iterations", 10000)
            val salt = Base64.decode(container.getString("salt"), Base64.NO_WRAP)
            val iv = Base64.decode(container.getString("iv"), Base64.NO_WRAP)
            val cipherText = Base64.decode(container.getString("ciphertext"), Base64.NO_WRAP)

            val keySpec = PBEKeySpec(password.toCharArray(), salt, iterations, 256)
            val keyFactory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA256")
            val derivedKey = keyFactory.generateSecret(keySpec).encoded
            val secretKey = SecretKeySpec(derivedKey, "AES")

            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            val gcmSpec = GCMParameterSpec(128, iv)
            cipher.init(Cipher.DECRYPT_MODE, secretKey, gcmSpec)
            val plainBytes = cipher.doFinal(cipherText)

            val jsonString = String(plainBytes, Charsets.UTF_8)
            val root = JSONObject(jsonString)

            val eventObj = root.getJSONObject("event")
            val legacyLocalId = eventObj.optLong("legacyLocalId", 0L)
            val event = EventEntity(
                id = legacyLocalId,
                name = eventObj.getString("name"),
                date = eventObj.optString("date", ""),
                timeFrom = eventObj.optString("timeFrom", ""),
                timeTo = eventObj.optString("timeTo", ""),
                backgroundType = "PRESET",
                backgroundPresetId = eventObj.optInt("backgroundPresetId", 0),
                backgroundUri = null,
                backgroundAspectRatio = eventObj.optDouble("backgroundAspectRatio", 0.714).toFloat(),
                nameColorHex = eventObj.optString("nameColorHex", "#FFFDF6E2"),
                qrColorHex = eventObj.optString("qrColorHex", "#FF121826"),
                qrBgColorHex = eventObj.optString("qrBgColorHex", "#FFFFFFFF"),
                fontIndex = eventObj.optInt("fontIndex", 0),
                showGuestName = eventObj.optBoolean("showGuestName", true),
                showDate = eventObj.optBoolean("showDate", true),
                showTime = eventObj.optBoolean("showTime", true),
                qrSizeRatio = eventObj.optDouble("qrSizeRatio", 0.28).toFloat(),
                nameSizeSp = eventObj.optDouble("nameSizeSp", 22.0).toFloat(),
                qrPosX = eventObj.optDouble("qrPosX", 0.5).toFloat(),
                qrPosY = eventObj.optDouble("qrPosY", 0.48).toFloat(),
                namePosX = eventObj.optDouble("namePosX", 0.5).toFloat(),
                namePosY = eventObj.optDouble("namePosY", 0.76).toFloat(),
                datePosX = eventObj.optDouble("datePosX", 0.5).toFloat(),
                datePosY = eventObj.optDouble("datePosY", 0.88).toFloat(),
                whatsappTemplate = eventObj.optString("whatsappTemplate", EventEntity.DEFAULT_WHATSAPP_TEMPLATE_AR),
                createdAt = eventObj.optLong("createdAt", System.currentTimeMillis()),
                groomName = eventObj.optString("groomName", ""),
                brideName = eventObj.optString("brideName", ""),
                headerVerse = eventObj.optString("headerVerse", ""),
                hostsText = eventObj.optString("hostsText", ""),
                dayName = eventObj.optString("dayName", ""),
                venueName = eventObj.optString("venueName", ""),
                venueAddress = eventObj.optString("venueAddress", ""),
                locationUrl = eventObj.optString("locationUrl", ""),
                showLocationQr = eventObj.optBoolean("showLocationQr", true),
                notesText = eventObj.optString("notesText", ""),
                closingText = eventObj.optString("closingText", ""),
                cloudKey = eventObj.optString("cloudKey", "").ifBlank { java.util.UUID.randomUUID().toString() },
                updatedAt = eventObj.optLong("updatedAt", 0L),
                isDeleted = eventObj.optBoolean("isDeleted", false)
            )

            val guestsArray = root.getJSONArray("guests")
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
                        id = obj.optLong("id", 0L),
                        eventId = event.id,
                        name = name,
                        normalizedName = obj.optString("normalizedName", EventRepository.normalizeNameForComparison(name)),
                        phone = phone,
                        normalizedPhone = obj.optString("normalizedPhone", if (phone.isEmpty()) "" else EventRepository.normalizePhoneForComparison(phone)),
                        code = obj.getString("code").trim(),
                        sequenceNumber = obj.optInt("sequenceNumber", i + 1),
                        attended = obj.optBoolean("attended", false),
                        attendedAt = attendedAtVal,
                        whatsappSent = obj.optBoolean("whatsappSent", false),
                        createdAt = obj.optLong("createdAt", System.currentTimeMillis()),
                        cloudKey = obj.optString("cloudKey", "").ifBlank { java.util.UUID.randomUUID().toString() },
                        updatedAt = obj.optLong("updatedAt", 0L),
                        isDeleted = obj.optBoolean("isDeleted", false)
                    )
                )
            }
            event to guests
        } catch (_: Exception) {
            null
        }
    }
}
