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
import com.example.data.BackupImportResult
import com.example.data.EventEntity
import com.example.data.EventRepository
import com.example.data.EventWithCounts
import com.example.data.GuestEntity
import com.example.data.PreferencesRepository
import com.example.data.ScanOutcome
import com.example.util.InvitationRenderer
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
    data class Designer(val eventId: Long? = null) : AppScreen()
    data class EventDetail(val eventId: Long) : AppScreen()
    data class Scanner(val eventId: Long) : AppScreen()
    data object Settings : AppScreen()
}

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getInstance(application)
    val repository = EventRepository(db.eventDao())
    private val prefsRepo = PreferencesRepository(application)

    val preferences: StateFlow<AppPreferences> = prefsRepo.preferencesFlow

    val eventsWithCounts: StateFlow<List<EventWithCounts>> = repository.eventsWithCounts.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5000),
        initialValue = emptyList()
    )

    private val _screenStack = MutableStateFlow<List<AppScreen>>(
        if (prefsRepo.preferencesFlow.value.isLoggedIn) {
            listOf(AppScreen.Home)
        } else {
            listOf(AppScreen.Login)
        }
    )
    val screenStack: StateFlow<List<AppScreen>> = _screenStack.asStateFlow()

    private val _statusBanner = MutableStateFlow<String?>(null)
    val statusBanner: StateFlow<String?> = _statusBanner.asStateFlow()

    private val _zipProgress = MutableStateFlow<Pair<Int, Int>?>(null)
    val zipProgress: StateFlow<Pair<Int, Int>?> = _zipProgress.asStateFlow()

    init {
        viewModelScope.launch(Dispatchers.IO) {
            repository.cleanupLegacyDemoDataIfPresent()
        }
    }

    fun currentScreen(): AppScreen = _screenStack.value.lastOrNull() ?: AppScreen.Home

    fun navigateTo(screen: AppScreen) {
        _screenStack.value = _screenStack.value + screen
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
        val prefs = preferences.value
        val cleanUser = username.trim()
        val cleanPass = password.trim()
        if (cleanUser.equals(prefs.adminUsername, ignoreCase = true) && cleanPass == prefs.adminPassword) {
            prefsRepo.setLoggedIn(true)
            _screenStack.value = listOf(AppScreen.Home)
        } else {
            onError()
        }
    }

    fun logout() {
        prefsRepo.setLoggedIn(false)
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

    fun setContinuousScan(enabled: Boolean) {
        prefsRepo.setContinuousScan(enabled)
    }

    fun updateAdminPassword(newPass: String, isArabic: Boolean) {
        prefsRepo.updateAdminPassword(newPass)
        _statusBanner.value = if (isArabic) {
            "تم تحديث كلمة المرور بنجاح"
        } else {
            "Password updated successfully"
        }
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
}
