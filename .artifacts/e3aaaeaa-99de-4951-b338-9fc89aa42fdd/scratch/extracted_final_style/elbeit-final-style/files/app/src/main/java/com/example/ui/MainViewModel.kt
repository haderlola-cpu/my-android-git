package com.example.ui

import android.app.Application
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.content.FileProvider
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AddGuestsResult
import com.example.data.AppDatabase
import com.example.data.AppPreferences
import com.example.data.AuthResult
import com.example.data.BackupImportResult
import com.example.data.EventEntity
import com.example.data.EventRepository
import com.example.data.EventWithCounts
import com.example.data.FirestoreSyncManager
import com.example.data.GuestEntity
import com.example.data.PreferencesRepository
import com.example.data.ScanOutcome
import com.example.util.InvitationRenderer
import com.google.firebase.auth.FirebaseAuth
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

sealed class AppScreen {
    data object Login : AppScreen()
    data object Home : AppScreen()
    data object Events : AppScreen()
    data object Guests : AppScreen()
    data object Sync : AppScreen()
    data class Designer(val eventId: Long? = null) : AppScreen()
    data class EventDetail(val eventId: Long) : AppScreen()
    data class Scanner(val eventId: Long) : AppScreen()
    data object Settings : AppScreen()
    data object Reception : AppScreen()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val repository = EventRepository(db.eventDao(), application)
    private val prefsRepo = PreferencesRepository(application)

    val preferences: StateFlow<AppPreferences> = prefsRepo.preferencesFlow

    val eventsWithCounts: StateFlow<List<EventWithCounts>> = repository.eventsWithCounts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _screenStack = MutableStateFlow<List<AppScreen>>(listOf(AppScreen.Login))
    val screenStack: StateFlow<List<AppScreen>> = _screenStack.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _zipProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val zipProgress: StateFlow<Pair<Int, Int>?> = _zipProgress.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.cleanupLegacyDemoDataIfPresent()

            val currentUser = FirebaseAuth.getInstance().currentUser
            val isPrefsLoggedIn = prefsRepo.preferencesFlow.value.isLoggedIn

            if (currentUser != null && isPrefsLoggedIn) {
                val syncManager = FirestoreSyncManager(db.eventDao())
                val role = syncManager.getUserRole(currentUser.uid)
                if (role != "admin") {
                    try { FirebaseAuth.getInstance().signOut() } catch (_: Exception) {}
                    prefsRepo.setLoggedIn(false)
                    withContext(Dispatchers.Main) {
                        _screenStack.value = listOf(AppScreen.Login)
                    }
                } else {
                    syncManager.pullAndMergeFromCloud()
                    repository.seedAllLocalDataToSyncQueue()
                    withContext(Dispatchers.Main) {
                        _screenStack.value = listOf(AppScreen.Home)
                    }
                }
            } else {
                withContext(Dispatchers.Main) {
                    prefsRepo.setLoggedIn(false)
                    _screenStack.value = listOf(AppScreen.Login)
                }
            }
        }
    }

    fun currentScreen(): AppScreen = _screenStack.value.lastOrNull() ?: AppScreen.Home

    fun navigateTo(screen: AppScreen) {
        _screenStack.value = _screenStack.value + screen
    }

    /** التنقل بين تبويبات الشريط السفلي: يستبدل المكدس بدل ما يكدّس. */
    fun selectTab(screen: AppScreen) {
        _screenStack.value = listOf(screen)
    }

    fun navigateBack(): Boolean {
        val current = _screenStack.value
        return if (current.size > 1) {
            _screenStack.value = current.dropLast(1)
            true
        } else {
            false
        }
    }

    fun clearStatusBanner() {
        _statusBanner.value = null
    }

    fun showStatus(msg: String) {
        _statusBanner.value = msg
    }

    fun attemptLogin(username: String, password: String, onError: () -> Unit) {
        val cleanUser = username.trim()
        val cleanPass = password.trim()
        if (cleanUser.isEmpty() || cleanPass.isEmpty()) {
            onError()
            return
        }

        viewModelScope.launch(Dispatchers.IO) {
            val syncManager = FirestoreSyncManager(db.eventDao())
            val authResult = syncManager.signInWithEmail(cleanUser, cleanPass)

            when (authResult) {
                is AuthResult.Success -> {
                    val uid = syncManager.currentUserUid
                    val role = if (uid != null) syncManager.getUserRole(uid) else "admin"
                    if (role != "admin") {
                        try { FirebaseAuth.getInstance().signOut() } catch (_: Exception) {}
                        withContext(Dispatchers.Main) {
                            onError()
                        }
                        return@launch
                    }
                    prefsRepo.setLastAuthEmail(cleanUser)
                    syncManager.pullAndMergeFromCloud()
                    repository.seedAllLocalDataToSyncQueue()
                    withContext(Dispatchers.Main) {
                        prefsRepo.setLoggedIn(true)
                        _screenStack.value = listOf(AppScreen.Home)
                    }
                }
                is AuthResult.InvalidCredentials, is AuthResult.AccountDisabled -> {
                    // Immediate failure for invalid credentials or security denials - do not allow offline fallback
                    withContext(Dispatchers.Main) {
                        onError()
                    }
                }
                is AuthResult.NetworkError, is AuthResult.Error -> {
                    // Allow offline login only if an existing valid cached Firebase user session is present on device
                    val cachedUser = FirebaseAuth.getInstance().currentUser
                    val hasCachedSession = cachedUser != null && (cachedUser.email == null || cachedUser.email.equals(cleanUser, ignoreCase = true))

                    if (hasCachedSession) {
                        repository.seedAllLocalDataToSyncQueue()
                        withContext(Dispatchers.Main) {
                            prefsRepo.setLoggedIn(true)
                            showStatus("وضع عدم الاتصال - جلسة مخزنة مؤقتة")
                            _screenStack.value = listOf(AppScreen.Home)
                        }
                    } else {
                        withContext(Dispatchers.Main) {
                            onError()
                        }
                    }
                }
            }
        }
    }

    fun logout() {
        try {
            FirebaseAuth.getInstance().signOut()
        } catch (_: Exception) {}
        prefsRepo.clearAuthSession()
        _screenStack.value = listOf(AppScreen.Login)
    }

    fun toggleLanguage() {
        prefsRepo.toggleLanguage(preferences.value.languageCode)
    }

    fun setLanguage(lang: String) {
        prefsRepo.setLanguage(lang)
    }

    fun setOriginalExportQuality(enabled: Boolean) {
        prefsRepo.setOriginalExportQuality(enabled)
    }

    fun setDarkMode(enabled: Boolean) {
        prefsRepo.setDarkMode(enabled)
    }

    fun setContinuousScan(enabled: Boolean) {
        prefsRepo.setContinuousScan(enabled)
    }

    fun saveEventAndOpenDetail(event: EventEntity, openDetailOnSave: Boolean = true) {
        viewModelScope.launch {
            val savedId = repository.saveEvent(event)
            val stackWithoutDesigner = _screenStack.value.filterNot { it is AppScreen.Designer }
            if (openDetailOnSave) {
                val base = stackWithoutDesigner.ifEmpty { listOf(AppScreen.Home) }
                val alreadyOnDetail = (base.lastOrNull() as? AppScreen.EventDetail)?.eventId == savedId
                _screenStack.value = if (alreadyOnDetail) base else base + AppScreen.EventDetail(savedId)
            } else {
                _screenStack.value = stackWithoutDesigner.ifEmpty { listOf(AppScreen.Home) }
            }
        }
    }

    fun updateEventMetadata(event: EventEntity) {
        viewModelScope.launch {
            repository.saveEvent(event)
        }
    }

    fun deleteEvent(eventId: Long) {
        viewModelScope.launch {
            repository.deleteEvent(eventId)
            _screenStack.value = _screenStack.value.filterNot {
                (it is AppScreen.EventDetail && it.eventId == eventId) ||
                    (it is AppScreen.Designer && it.eventId == eventId) ||
                    (it is AppScreen.Scanner && it.eventId == eventId)
            }.ifEmpty { listOf(AppScreen.Home) }
        }
    }

    fun addGuestsByNames(
        eventId: Long,
        rawText: String,
        onResult: (AddGuestsResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.addGuestsByNames(eventId, rawText)
            onResult(result)
        }
    }

    fun addGuestsBySequence(
        eventId: Long,
        fromNum: Int,
        toNum: Int,
        prefix: String,
        onResult: (AddGuestsResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.addGuestsBySequence(eventId, fromNum, toNum, prefix)
            onResult(result)
        }
    }

    fun addGuestsByPhones(
        eventId: Long,
        rawPhones: String,
        onResult: (AddGuestsResult) -> Unit
    ) {
        viewModelScope.launch {
            val result = repository.addGuestsByPhones(eventId, rawPhones)
            onResult(result)
        }
    }

    fun deleteGuest(guestId: Long) {
        viewModelScope.launch {
            repository.deleteGuest(guestId)
        }
    }

    fun deleteSelectedGuests(guestIds: List<Long>, onDone: () -> Unit) {
        viewModelScope.launch {
            repository.deleteGuests(guestIds)
            onDone()
        }
    }

    fun sendGuestWhatsApp(context: Context, event: EventEntity, guest: GuestEntity) {
        viewModelScope.launch {
            repository.markWhatsappSent(guest.id)
            InvitationRenderer.dispatchGuestWhatsApp(
                context = context,
                event = event,
                guest = guest,
                originalQuality = preferences.value.originalExportQuality
            )
        }
    }

    fun downloadSingleGuestImage(context: Context, event: EventEntity, guest: GuestEntity) {
        viewModelScope.launch {
            InvitationRenderer.shareSingleInvitationImage(
                context = context,
                event = event,
                guest = guest,
                originalQuality = preferences.value.originalExportQuality
            )
        }
    }

    fun shareSelectedBatch(context: Context, event: EventEntity, guests: List<GuestEntity>) {
        viewModelScope.launch {
            InvitationRenderer.shareSelectedInvitationsBatch(
                context = context,
                event = event,
                guests = guests,
                originalQuality = preferences.value.originalExportQuality
            )
        }
    }

    fun exportEventZip(context: Context, event: EventEntity, guests: List<GuestEntity>) {
        if (guests.isEmpty()) return
        viewModelScope.launch {
            _zipProgress.value = 0 to guests.size
            try {
                InvitationRenderer.exportAllAsZipAndShare(
                    context = context,
                    event = event,
                    guests = guests,
                    originalQuality = preferences.value.originalExportQuality,
                    onProgress = { current, total ->
                        _zipProgress.value = current to total
                    }
                )
            } finally {
                _zipProgress.value = null
            }
        }
    }

    fun verifyBarcodeAtDoor(
        eventId: Long,
        rawBarcode: String,
        onOutcome: (ScanOutcome) -> Unit
    ) {
        viewModelScope.launch {
            val outcome = repository.verifyAndCheckInBarcode(eventId, rawBarcode)
            onOutcome(outcome)
        }
    }

    fun exportBackupAndShare(context: Context, isArabic: Boolean) {
        viewModelScope.launch {
            val json = repository.exportBackupJson()
            val exportsDir = File(context.cacheDir, "exports").apply { mkdirs() }
            val backupFile = File(exportsDir, "elbeit_elaraby_backup_${System.currentTimeMillis()}.json")
            withContext(Dispatchers.IO) {
                backupFile.writeText(json, Charsets.UTF_8)
            }
            val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", backupFile)
            val intent = Intent(Intent.ACTION_SEND).apply {
                type = "application/json"
                putExtra(Intent.EXTRA_STREAM, uri)
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
            }
            context.startActivity(
                Intent.createChooser(
                    intent,
                    if (isArabic) "حفظ النسخة الاحتياطية" else "Save Backup File"
                )
            )
        }
    }

    fun restoreBackupFromUri(context: Context, uri: Uri, isArabic: Boolean) {
        viewModelScope.launch {
            val json = withContext(Dispatchers.IO) {
                try {
                    context.contentResolver.openInputStream(uri)?.bufferedReader(Charsets.UTF_8)?.use {
                        it.readText()
                    }
                } catch (e: Exception) {
                    null
                }
            }
            if (json.isNullOrBlank()) {
                _statusBanner.value = if (isArabic) {
                    "تعذر قراءة ملف النسخة الاحتياطية"
                } else {
                    "Could not read backup file"
                }
                return@launch
            }
            // Non-destructive import: merges into existing data, or stops with a clear conflict.
            _statusBanner.value = when (val result = repository.importBackupJson(json)) {
                is BackupImportResult.Success -> if (isArabic) {
                    "تمت استعادة النسخة الاحتياطية بنجاح ✨ (مناسبات جديدة: ${result.eventsAdded}، ضيوف جدد: ${result.guestsAdded}، ضيوف مدموجون: ${result.guestsMerged})"
                } else {
                    "Backup restored successfully ✨ (new events: ${result.eventsAdded}, new guests: ${result.guestsAdded}, merged guests: ${result.guestsMerged})"
                }

                is BackupImportResult.Conflict -> if (isArabic) {
                    "تعذّر الدمج الآمن: ${result.reasonAr} لم يتم تغيير أي بيانات."
                } else {
                    "Safe merge not possible: ${result.reasonEn} No data was changed."
                }

                is BackupImportResult.Invalid -> if (isArabic) {
                    "ملف النسخة الاحتياطية غير صالح"
                } else {
                    "Invalid backup file format"
                }
            }
        }
    }

    suspend fun exportSingleEventSecure(eventId: Long, pass: String): String? {
        return withContext(Dispatchers.IO) {
            repository.exportSingleEventSecure(eventId, pass)
        }
    }

    fun exportSingleEvent(eventId: Long, pass: String, onResult: (String?) -> Unit) {
        viewModelScope.launch {
            val result = repository.exportSingleEventSecure(eventId, pass)
            onResult(result)
        }
    }

    fun importSingleEvent(encryptedJson: String, pass: String) {
        val isArabic = preferences.value.languageCode == "ar"
        viewModelScope.launch {
            _statusBanner.value = when (val result = repository.importSingleEventSecure(encryptedJson, pass)) {
                is BackupImportResult.Success -> if (isArabic) {
                    "تم استيراد المناسبة بنجاح ✨ (جديد: ${result.eventsAdded} مناسبة، ${result.guestsAdded} ضيف)"
                } else {
                    "Single event imported successfully ✨ (${result.eventsAdded} event, ${result.guestsAdded} guests)"
                }
                is BackupImportResult.Conflict -> if (isArabic) {
                    "تعذر الدمج: ${result.reasonAr}"
                } else {
                    "Import conflict: ${result.reasonEn}"
                }
                is BackupImportResult.Invalid -> if (isArabic) {
                    "كلمة المرور خاطئة أو الملف غير صالح"
                } else {
                    "Incorrect password or invalid file format"
                }
            }
        }
    }
}
