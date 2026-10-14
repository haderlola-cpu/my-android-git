package com.example.ui.screens.reception

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel

@Composable
fun ReceptionHostScreen(
    receptionViewModel: ReceptionViewModel = viewModel(),
    onExitReception: () -> Unit
) {
    val currentScreen by receptionViewModel.currentScreen.collectAsStateWithLifecycle()
    val userEmail by receptionViewModel.userEmail.collectAsStateWithLifecycle()
    val isLoggingIn by receptionViewModel.isLoggingIn.collectAsStateWithLifecycle()
    val loginError by receptionViewModel.loginError.collectAsStateWithLifecycle()

    val eventsList by receptionViewModel.eventsList.collectAsStateWithLifecycle()
    val selectedEvent by receptionViewModel.selectedEvent.collectAsStateWithLifecycle()
    val filteredGuests by receptionViewModel.filteredGuests.collectAsStateWithLifecycle()

    val totalGuestsCount by receptionViewModel.totalGuestsCount.collectAsStateWithLifecycle()
    val attendedCount by receptionViewModel.attendedCount.collectAsStateWithLifecycle()
    val notArrivedCount by receptionViewModel.notArrivedCount.collectAsStateWithLifecycle()

    val isDownloadingGuests by receptionViewModel.isDownloadingGuests.collectAsStateWithLifecycle()
    val downloadStatusMessage by receptionViewModel.downloadStatusMessage.collectAsStateWithLifecycle()

    val continuousScan by receptionViewModel.continuousScan.collectAsStateWithLifecycle()
    val searchQuery by receptionViewModel.searchQuery.collectAsStateWithLifecycle()
    val selectedFilter by receptionViewModel.selectedFilter.collectAsStateWithLifecycle()

    // Handle back button inside reception flow
    BackHandler(enabled = true) {
        when (currentScreen) {
            ReceptionScreen.GuestList -> receptionViewModel.navigateTo(ReceptionScreen.Scanner)
            ReceptionScreen.Scanner -> receptionViewModel.navigateTo(ReceptionScreen.EventSelection)
            ReceptionScreen.EventSelection -> onExitReception()
            ReceptionScreen.Login -> onExitReception()
        }
    }

    when (currentScreen) {
        ReceptionScreen.Login -> {
            ReceptionLoginScreen(
                isLoggingIn = isLoggingIn,
                errorMessage = loginError,
                onLoginAttempt = { email, pass ->
                    receptionViewModel.signInWithEmail(email, pass)
                }
            )
        }

        ReceptionScreen.EventSelection -> {
            ReceptionEventSelectionScreen(
                userEmail = userEmail,
                events = eventsList,
                isDownloadingGuests = isDownloadingGuests,
                downloadStatusMessage = downloadStatusMessage,
                onSelectEvent = { event ->
                    receptionViewModel.selectEvent(event)
                },
                onLogout = {
                    receptionViewModel.logout()
                },
                onImportSecureEvent = { json, pass, callback ->
                    receptionViewModel.importSecureEvent(json, pass, callback)
                }
            )
        }

        ReceptionScreen.Scanner -> {
            val activeEvent = selectedEvent
            if (activeEvent != null) {
                ReceptionScannerScreen(
                    event = activeEvent,
                    attendedCount = attendedCount,
                    totalCount = totalGuestsCount,
                    continuousScan = continuousScan,
                    onToggleContinuousScan = { enabled ->
                        receptionViewModel.setContinuousScan(enabled)
                    },
                    onVerifyBarcode = { rawBarcode, onOutcome ->
                        receptionViewModel.verifyAndCheckInBarcode(rawBarcode, onOutcome)
                    },
                    onOpenGuestList = {
                        receptionViewModel.navigateTo(ReceptionScreen.GuestList)
                    },
                    onBack = {
                        receptionViewModel.navigateTo(ReceptionScreen.EventSelection)
                    }
                )
            }
        }

        ReceptionScreen.GuestList -> {
            val activeEvent = selectedEvent
            if (activeEvent != null) {
                ReceptionGuestListScreen(
                    event = activeEvent,
                    guests = filteredGuests,
                    totalCount = totalGuestsCount,
                    attendedCount = attendedCount,
                    notArrivedCount = notArrivedCount,
                    searchQuery = searchQuery,
                    selectedFilter = selectedFilter,
                    onSearchQueryChange = { receptionViewModel.setSearchQuery(it) },
                    onFilterChange = { receptionViewModel.setFilter(it) },
                    onBack = {
                        receptionViewModel.navigateTo(ReceptionScreen.Scanner)
                    }
                )
            }
        }
    }
}
