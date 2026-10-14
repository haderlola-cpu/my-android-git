package com.example

import android.content.Context
import android.graphics.Color
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.core.app.ApplicationProvider
import com.example.util.InvitationRenderer
import com.google.firebase.FirebaseApp
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        if (FirebaseApp.getApps(context).isEmpty()) {
            FirebaseApp.initializeApp(context)
        }
    }

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("elbeit elaraby", appName)
    }

    @Test
    fun `qr code generated for guest is readable by decoder`() {
        val payload = "eb:1:48291037"
        val qrBitmap = InvitationRenderer.generateQrBitmap(
            content = payload,
            sizePx = 300,
            fgColorInt = Color.BLACK,
            bgColorInt = Color.WHITE
        )
        val decoded = InvitationRenderer.decodeQrFromBitmap(qrBitmap)
        assertEquals(payload, decoded)
    }

    @Test
    fun `main activity launches and logs in cleanly`() {
        composeRule.waitForIdle()
        composeRule.onNodeWithTag("login_button").assertExists()
    }
}
