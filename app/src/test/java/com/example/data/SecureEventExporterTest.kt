package com.example.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class SecureEventExporterTest {

    @Test
    fun secureExportAndImport_encryptsAndDecryptsSuccessfully() {
        val event = EventEntity(
            id = 1L,
            name = "حفل زفاف تجريبي",
            date = "2026-12-25",
            timeFrom = "18:00",
            timeTo = "23:00",
            backgroundType = "IMAGE",
            backgroundUri = "content://media/external/images/media/123",
            cloudKey = "test-event-cloud-key-uuid"
        )
        val guests = listOf(
            GuestEntity(
                id = 10L,
                eventId = 1L,
                name = "أحمد محمد",
                normalizedName = "أحمد محمد",
                code = "87654321",
                sequenceNumber = 1,
                attended = true,
                cloudKey = "test-guest-cloud-key-uuid"
            )
        )
        val password = "SecureWeddingPassword123!"

        val encryptedJson = SecureEventExporter.exportSecureEventJson(event, guests, password)
        assertNotNull(encryptedJson)

        val decrypted = SecureEventExporter.decryptAndParseSecureEvent(encryptedJson, password)
        assertNotNull(decrypted)

        val (decryptedEvent, decryptedGuests) = decrypted!!
        assertEquals(event.id, decryptedEvent.id)
        assertEquals(event.name, decryptedEvent.name)
        assertEquals(event.cloudKey, decryptedEvent.cloudKey)
        // Background URI should be reverted to PRESET and null as specified
        assertEquals("PRESET", decryptedEvent.backgroundType)
        assertNull(decryptedEvent.backgroundUri)

        assertEquals(1, decryptedGuests.size)
        assertEquals(guests.first().name, decryptedGuests.first().name)
        assertEquals(guests.first().code, decryptedGuests.first().code)
        assertEquals(guests.first().attended, decryptedGuests.first().attended)
    }

    @Test
    fun secureExport_wrongPassword_failsToDecrypt() {
        val event = EventEntity(id = 1L, name = "حفل", date = "2026-12-25", timeFrom = "18:00", timeTo = "23:00")
        val guests = emptyList<GuestEntity>()
        val encryptedJson = SecureEventExporter.exportSecureEventJson(event, guests, "CorrectPassword")

        val decrypted = SecureEventExporter.decryptAndParseSecureEvent(encryptedJson, "WrongPassword")
        assertNull(decrypted)
    }
}
