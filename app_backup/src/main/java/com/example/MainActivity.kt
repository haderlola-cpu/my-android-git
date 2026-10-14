package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.ui.AppScreen
import com.example.ui.MainViewModel
import com.example.ui.screens.DesignerScreen
import com.example.ui.screens.EventDetailScreen
import com.example.ui.screens.HomeScreen
import com.example.ui.screens.LoginScreen
import com.example.ui.screens.ScannerScreen
import com.example.ui.screens.SettingsScreen
import com.example.ui.theme.MyApplicationTheme
import com.example.util.LocalAppStrings
import com.example.util.getAppStrings

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ElbeitElarabyApp()
            }
        }
    }
}

@Composable
fun ElbeitElarabyApp(
    mainViewModel: MainViewModel = viewModel()
) {
    val context = LocalContext.current
    val preferences by mainViewModel.preferences.collectAsStateWithLifecycle()
    val screenStack by mainViewModel.screenStack.collectAsStateWithLifecycle()
    val eventsWithCounts by mainViewModel.eventsWithCounts.collectAsStateWithLifecycle()
    val statusBanner by mainViewModel.statusBanner.collectAsStateWithLifecycle()
    val zipProgress by mainViewModel.zipProgress.collectAsStateWithLifecycle()

    val appStrings = getAppStrings(preferences.languageCode)
    val layoutDirection = if (appStrings.isArabic) LayoutDirection.Rtl else LayoutDirection.Ltr
    val currentScreen = screenStack.lastOrNull() ?: AppScreen.Home

    // Hardware Back Button support across all screens (Section 3.10)
    BackHandler(enabled = screenStack.size > 1) {
        mainViewModel.navigateBack()
    }

    CompositionLocalProvider(
        LocalAppStrings provides appStrings,
        LocalLayoutDirection provides layoutDirection
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            when (val screen = currentScreen) {
                is AppScreen.Login -> {
                    LoginScreen(
                        onLoginAttempt = { user, pass, onError ->
                            mainViewModel.attemptLogin(user, pass, onError)
                        },
                        onToggleLanguage = { mainViewModel.toggleLanguage() }
                    )
                }

                is AppScreen.Home -> {
                    HomeScreen(
                        events = eventsWithCounts,
                        onOpenEvent = { eventId ->
                            mainViewModel.navigateTo(AppScreen.EventDetail(eventId))
                        },
                        onCreateNewEvent = {
                            mainViewModel.navigateTo(AppScreen.Designer(null))
                        },
                        onQuickScanEvent = { eventId ->
                            mainViewModel.navigateTo(AppScreen.Scanner(eventId))
                        },
                        onDeleteEvent = { eventId ->
                            mainViewModel.deleteEvent(eventId)
                        },
                        onOpenSettings = {
                            mainViewModel.navigateTo(AppScreen.Settings)
                        },
                        onToggleLanguage = {
                            mainViewModel.toggleLanguage()
                        }
                    )
                }

                is AppScreen.Designer -> {
                    val existingEvent = if (screen.eventId != null) {
                        val eventFlow = mainViewModel.repository.observeEvent(screen.eventId)
                        val loaded by eventFlow.collectAsStateWithLifecycle(initialValue = null)
                        loaded
                    } else null

                    DesignerScreen(
                        existingEvent = existingEvent,
                        onSaveEvent = { savedEvent ->
                            mainViewModel.saveEventAndOpenDetail(
                                event = savedEvent,
                                openDetailOnSave = true
                            )
                        },
                        onBack = { mainViewModel.navigateBack() }
                    )
                }

                is AppScreen.EventDetail -> {
                    val event by mainViewModel.repository.observeEvent(screen.eventId)
                        .collectAsStateWithLifecycle(initialValue = null)
                    val guests by mainViewModel.repository.observeGuests(screen.eventId)
                        .collectAsStateWithLifecycle(initialValue = emptyList())

                    val activeEvent = event
                    if (activeEvent != null) {
                        EventDetailScreen(
                            event = activeEvent,
                            guests = guests,
                            zipProgress = zipProgress,
                            viewModel = mainViewModel,
                            onOpenScanner = {
                                mainViewModel.navigateTo(AppScreen.Scanner(activeEvent.id))
                            },
                            onEditDesign = {
                                mainViewModel.navigateTo(AppScreen.Designer(activeEvent.id))
                            },
                            onBack = { mainViewModel.navigateBack() }
                        )
                    }
                }

                is AppScreen.Scanner -> {
                    val event by mainViewModel.repository.observeEvent(screen.eventId)
                        .collectAsStateWithLifecycle(initialValue = null)
                    val guests by mainViewModel.repository.observeGuests(screen.eventId)
                        .collectAsStateWithLifecycle(initialValue = emptyList())

                    val activeEvent = event
                    if (activeEvent != null) {
                        ScannerScreen(
                            event = activeEvent,
                            guests = guests,
                            continuousScan = preferences.continuousScan,
                            onToggleContinuousScan = { enabled ->
                                mainViewModel.setContinuousScan(enabled)
                            },
                            onVerifyBarcode = { rawBarcode, onOutcome ->
                                mainViewModel.verifyBarcodeAtDoor(
                                    eventId = activeEvent.id,
                                    rawBarcode = rawBarcode,
                                    onOutcome = onOutcome
                                )
                            },
                            onBack = { mainViewModel.navigateBack() }
                        )
                    }
                }

                is AppScreen.Settings -> {
                    SettingsScreen(
                        preferences = preferences,
                        onSetLanguage = { lang -> mainViewModel.setLanguage(lang) },
                        onSetOriginalQuality = { enabled -> mainViewModel.setOriginalExportQuality(enabled) },
                        onUpdatePassword = { newPass ->
                            mainViewModel.updateAdminPassword(newPass, appStrings.isArabic)
                        },
                        onExportBackup = {
                            mainViewModel.exportBackupAndShare(context, appStrings.isArabic)
                        },
                        onRestoreBackupUri = { uri ->
                            mainViewModel.restoreBackupFromUri(context, uri, appStrings.isArabic)
                        },
                        onLogout = { mainViewModel.logout() },
                        onBack = { mainViewModel.navigateBack() }
                    )
                }
            }

            if (statusBanner != null) {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { mainViewModel.clearStatusBanner() }) {
                            Text(appStrings.close)
                        }
                    }
                ) {
                    Text(statusBanner ?: "")
                }
            }
        }
    }
}
