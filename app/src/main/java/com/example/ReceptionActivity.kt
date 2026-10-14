package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.material3.MaterialTheme
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.AppPreferences
import com.example.data.PreferencesRepository
import com.example.ui.screens.reception.ReceptionHostScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.LocalAppStrings
import com.example.util.getAppStrings

class ReceptionActivity : ComponentActivity() {
    private lateinit var prefsRepo: PreferencesRepository

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        prefsRepo = PreferencesRepository(applicationContext)
        setContent {
            val preferences by prefsRepo.preferencesFlow.collectAsStateWithLifecycle()
            MyApplicationTheme(darkTheme = preferences.isDarkMode) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    ReceptionStandaloneApp()
                }
            }
        }
    }
}

@Composable
fun ReceptionStandaloneApp() {
    val appStrings = getAppStrings("ar")
    val layoutDirection = if (appStrings.isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr

    CompositionLocalProvider(
        LocalAppStrings provides appStrings,
        LocalLayoutDirection provides layoutDirection
    ) {
        ReceptionHostScreen(
            onExitReception = {}
        )
    }
}
