package com.example

import com.example.data.EventEntity
import com.example.data.EventRepository
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun arabicDigitsAndNameNormalization_worksAsSpecified() {
        assertEquals("01012345678", EventRepository.normalizeDigits("٠١٠١٢٣٤٥٦٧٨"))
        // "أحمد" and "أحمد علي" are distinct names
        assertNotEquals(
            EventRepository.normalizeNameForComparison("أحمد"),
            EventRepository.normalizeNameForComparison("أحمد علي")
        )
        // Extra spaces, case, and Arabic digits normalize identically
        assertEquals(
            EventRepository.normalizeNameForComparison("طاولة ١"),
            EventRepository.normalizeNameForComparison("  طاولة   1 ")
        )
    }

    @Test
    fun extractNameAndOptionalPhone_parsesCorrectly() {
        val (name1, phone1) = EventRepository.extractNameAndOptionalPhone("أحمد محمد 01012345678")
        assertEquals("أحمد محمد", name1)
        assertEquals("01012345678", phone1)

        val (name2, phone2) = EventRepository.extractNameAndOptionalPhone("منى السيد")
        assertEquals("منى السيد", name2)
        assertEquals("", phone2)
    }

    @Test
    fun whatsappEgyptianPhoneFormatting_adds20Prefix() {
        assertEquals("201012345678", EventRepository.formatWhatsAppPhone("01012345678"))
        assertEquals("201123456789", EventRepository.formatWhatsAppPhone("٠١١٢٣٤٥٦٧٨٩"))
    }

    @Test
    fun unique8DigitCodes_areAlways8DigitsAndUnique() {
        val used = mutableSetOf<String>()
        repeat(200) {
            val code = EventRepository.generateUnique8DigitCode(used)
            assertEquals(8, code.length)
            assertTrue(code.all { it.isDigit() })
        }
        assertEquals(200, used.size)
    }

    @Test
    fun welcomeMessageTemplate_replacesPlaceholders() {
        val event = EventEntity(
            id = 5L,
            name = "حفل زفاف",
            date = "2026-11-20",
            timeFrom = "08:00 م",
            timeTo = "12:00 ص"
        )
        val formatted = EventRepository.formatWelcomeMessage(
            template = EventEntity.DEFAULT_WHATSAPP_TEMPLATE_AR,
            guestName = "أحمد محمد",
            event = event
        )
        assertTrue(formatted.contains("أحمد محمد"))
        assertTrue(formatted.contains("حفل زفاف"))
        assertTrue(formatted.contains("2026-11-20"))
        assertTrue(formatted.contains("08:00 م – 12:00 ص"))
    }
}
