package com.example.ui.screens.reception

import android.app.Application
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.data.AuthResult
import com.example.data.BackupImportResult
import com.example.data.EventEntity
import com.example.data.EventRepository
import com.example.data.EventWithCounts
import com.example.data.FirestoreSyncManager
import com.example.data.GuestEntity
import com.example.data.ScanOutcome
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

sealed class ReceptionScreen {
    data object Login : ReceptionScreen()
    data object EventSelection : ReceptionScreen()
    data object Scanner : ReceptionScreen()
    data object GuestList : ReceptionScreen()
}

enum class GuestFilter {
    ALL,
    ATTENDED,
    NOT_ARRIVED
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReceptionViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val repository = EventRepository(db.eventDao(), application)
    val syncManager = FirestoreSyncManager(db.eventDao())

    private val _currentScreen = MutableStateFlow<ReceptionScreen>(ReceptionScreen.Login)
    val currentScreen: StateFlow<ReceptionScreen> = _currentScreen.asStateFlow()

    private val _isLoggedIn = MutableStateFlow(false)
    val isLoggedIn: StateFlow<Boolean> = _isLoggedIn.asStateFlow()

    private val _userEmail = MutableStateFlow("")
    val userEmail: StateFlow<String> = _userEmail.asStateFlow()

    private val _loginError = MutableStateFlow<String?>(null)
    val loginError: StateFlow<String?> = _loginError.asStateFlow()

    private val _isLoggingIn = MutableStateFlow(false)
    val isLoggingIn: StateFlow<Boolean> = _isLoggingIn.asStateFlow()

    // Events list for selection
    val eventsList: StateFlow<List<EventWithCounts>> = repository.eventsWithCounts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _selectedEventId = MutableStateFlow<Long?>(null)
    val selectedEventId: StateFlow<Long?> = _selectedEventId.asStateFlow()

    val selectedEvent: StateFlow<EventEntity?> = _selectedEventId.flatMapLatest { id ->
        if (id == null) flowOf(null) else repository.observeEvent(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = null
    )

    // Raw guests flow for selected event
    private val rawGuestsFlow: StateFlow<List<GuestEntity>> = _selectedEventId.flatMapLatest { id ->
        if (id == null) flowOf(emptyList()) else repository.observeGuests(id)
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Search & Filter
    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedFilter = MutableStateFlow(GuestFilter.ALL)
    val selectedFilter: StateFlow<GuestFilter> = _selectedFilter.asStateFlow()

    // Filtered guests - Strictly ensure NO phone numbers exposed in guest UI
    val filteredGuests: StateFlow<List<GuestEntity>> = combine(
        rawGuestsFlow,
        _searchQuery,
        _selectedFilter
    ) { guests, query, filter ->
        val trimmed = query.trim().lowercase()
        guests.filter { guest ->
            val matchesFilter = when (filter) {
                GuestFilter.ALL -> true
                GuestFilter.ATTENDED -> guest.attended
                GuestFilter.NOT_ARRIVED -> !guest.attended
            }
            val matchesSearch = trimmed.isEmpty() ||
                guest.name.lowercase().contains(trimmed) ||
                guest.sequenceNumber.toString().contains(trimmed) ||
                guest.code.contains(trimmed)

            matchesFilter && matchesSearch
        }.map { guest ->
            // Enforce privacy: clear phone numbers before passing to reception UI
            guest.copy(phone = "", normalizedPhone = "")
        }
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    // Attendance counters
    val totalGuestsCount: StateFlow<Int> = rawGuestsFlow.map { it.size }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val attendedCount: StateFlow<Int> = rawGuestsFlow.map { list -> list.count { it.attended } }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    val notArrivedCount: StateFlow<Int> = rawGuestsFlow.map { list -> list.count { !it.attended } }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = 0
    )

    private val _isDownloadingGuests = MutableStateFlow(false)
    val isDownloadingGuests: StateFlow<Boolean> = _isDownloadingGuests.asStateFlow()

    private val _downloadStatusMessage = MutableStateFlow<String?>(null)
    val downloadStatusMessage: StateFlow<String?> = _downloadStatusMessage.asStateFlow()

    private val _continuousScan = MutableStateFlow(true)
    val continuousScan: StateFlow<Boolean> = _continuousScan.asStateFlow()

    init {
        // Check if user is already signed in via Firebase Auth
        val currentUser = FirebaseAuth.getInstance().currentUser
        if (currentUser != null) {
            _isLoggedIn.value = true
            _userEmail.value = currentUser.email ?: ""
            _currentScreen.value = ReceptionScreen.EventSelection
        }
    }

    fun navigateTo(screen: ReceptionScreen) {
        _currentScreen.value = screen
    }

    fun setSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun setFilter(filter: GuestFilter) {
        _selectedFilter.value = filter
    }

    fun setContinuousScan(enabled: Boolean) {
        _continuousScan.value = enabled
    }

    fun signInWithEmail(email: String, pass: String) {
        val cleanEmail = email.trim()
        val cleanPass = pass.trim()
        if (cleanEmail.isEmpty() || cleanPass.isEmpty()) {
            _loginError.value = "يرجى إدخال البريد الإلكتروني وكلمة المرور"
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            _isLoggingIn.value = true
            _loginError.value = null

            val result = syncManager.signInWithEmail(cleanEmail, cleanPass)
            _isLoggingIn.value = false

            when (result) {
                is AuthResult.Success -> {
                    _isLoggedIn.value = true
                    _userEmail.value = cleanEmail
                    syncManager.pullAndMergeFromCloud()
                    withContext(Dispatchers.Main) {
                        _currentScreen.value = ReceptionScreen.EventSelection
                    }
                }
                is AuthResult.InvalidCredentials -> {
                    _loginError.value = "بيانات الدخول غير صحيحة. يرجى التحقق وإعادة المحاولة."
                }
                is AuthResult.AccountDisabled -> {
                    _loginError.value = "الحساب معطل أو غير مصرح له بالدخول."
                }
                is AuthResult.NetworkError -> {
                    val cachedUser = FirebaseAuth.getInstance().currentUser
                    if (cachedUser != null) {
                        _isLoggedIn.value = true
                        _userEmail.value = cachedUser.email ?: cleanEmail
                        withContext(Dispatchers.Main) {
                            _currentScreen.value = ReceptionScreen.EventSelection
                        }
                    } else {
                        _loginError.value = "خطأ في الاتصال بالشبكة. يرجى التحقق من اتصال الإنترنت."
                    }
                }
                is AuthResult.Error -> {
                    _loginError.value = result.message
                }
            }
        }
    }

    fun selectEvent(event: EventEntity) {
        _selectedEventId.value = event.id
        viewModelScope.launch(Dispatchers.IO) {
            _isDownloadingGuests.value = true
            _downloadStatusMessage.value = "جاري تحميل قائمة ضيوف المناسبة للفحص السريع..."
            try {
                if (event.cloudKey.isNotBlank()) {
                    val downloaded = syncManager.fetchSingleEventGuestsForReception(event.cloudKey)
                    _downloadStatusMessage.value = "تم تحميل ${downloaded.size} ضيف جاهز للفحص أوفلاين"
                }
            } catch (e: Exception) {
                _downloadStatusMessage.value = "تم الاعتماد على بيانات الحفظ المحلي"
            } finally {
                _isDownloadingGuests.value = false
                withContext(Dispatchers.Main) {
                    _currentScreen.value = ReceptionScreen.Scanner
                }
            }
        }
    }

    fun verifyAndCheckInBarcode(rawBarcode: String, onOutcome: (ScanOutcome) -> Unit) {
        val eventId = _selectedEventId.value ?: return
        viewModelScope.launch(Dispatchers.IO) {
            val outcome = repository.verifyAndCheckInBarcode(eventId, rawBarcode)

            // Push attendance to cloud if success
            if (outcome is ScanOutcome.CheckedInSuccess) {
                val activeEvent = repository.getEventById(eventId)
                if (activeEvent != null && activeEvent.cloudKey.isNotBlank() && outcome.guest.cloudKey.isNotBlank()) {
                    val deviceId = try {
                        Settings.Secure.getString(
                            getApplication<Application>().contentResolver,
                            Settings.Secure.ANDROID_ID
                        ) ?: "reception_device"
                    } catch (_: Exception) {
                        "reception_device"
                    }
                    try {
                        syncManager.pushAttendance(
                            eventCloudKey = activeEvent.cloudKey,
                            guestCloudKey = outcome.guest.cloudKey,
                            attendedAt = outcome.checkInTimeMillis,
                            deviceId = deviceId
                        )
                    } catch (_: Exception) {
                        // Background sync worker will catch up
                    }
                }
            }

            withContext(Dispatchers.Main) {
                onOutcome(outcome)
            }
        }
    }

    fun manualCheckInGuest(guestId: Long) {
        viewModelScope.launch(Dispatchers.IO) {
            val guest = db.eventDao().getGuestById(guestId) ?: return@launch
            if (!guest.attended) {
                val now = System.currentTimeMillis()
                val updated = guest.copy(attended = true, attendedAt = now, updatedAt = now)
                db.eventDao().updateGuest(updated)

                val activeEvent = _selectedEventId.value?.let { repository.getEventById(it) }
                if (activeEvent != null && activeEvent.cloudKey.isNotBlank() && updated.cloudKey.isNotBlank()) {
                    val deviceId = try {
                        Settings.Secure.getString(
                            getApplication<Application>().contentResolver,
                            Settings.Secure.ANDROID_ID
                        ) ?: "reception_device"
                    } catch (_: Exception) {
                        "reception_device"
                    }
                    try {
                        syncManager.pushAttendance(
                            eventCloudKey = activeEvent.cloudKey,
                            guestCloudKey = updated.cloudKey,
                            attendedAt = now,
                            deviceId = deviceId
                        )
                    } catch (_: Exception) { }
                }
            }
        }
    }

    fun logout() {
        syncManager.signOut()
        _isLoggedIn.value = false
        _userEmail.value = ""
        _selectedEventId.value = null
        _currentScreen.value = ReceptionScreen.Login
    }

    fun clearDownloadStatus() {
        _downloadStatusMessage.value = null
    }

    fun importSecureEvent(encryptedJson: String, pass: String, onResult: (BackupImportResult) -> Unit) {
        viewModelScope.launch(Dispatchers.IO) {
            val result = repository.importSingleEventSecure(encryptedJson, pass)
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }
}
