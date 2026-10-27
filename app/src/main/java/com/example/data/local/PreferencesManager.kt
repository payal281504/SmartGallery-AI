package com.example.data.local

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.security.MessageDigest

private val Context.dataStore by preferencesDataStore(name = "aura_gallery_prefs")

class PreferencesManager(private val context: Context) {

    companion object {
        val KEY_GRID_COLUMNS = intPreferencesKey("grid_columns")
        val KEY_AI_ENABLED = booleanPreferencesKey("ai_categorization_enabled")
        val KEY_PEOPLE_RECOGNITION = booleanPreferencesKey("people_recognition_enabled")
        val KEY_THEME_MODE = stringPreferencesKey("theme_mode") // "SYSTEM", "DARK", "LIGHT"
        val KEY_VAULT_PIN_HASH = stringPreferencesKey("vault_pin_hash")
        val KEY_LAST_SCAN_TIME = longPreferencesKey("last_scan_time")
        val KEY_SAMPLE_SEEDED = booleanPreferencesKey("sample_seeded")

        fun hashPin(pin: String): String {
            val md = MessageDigest.getInstance("SHA-256")
            val digest = md.digest(pin.toByteArray())
            return digest.fold("") { str, it -> str + "%02x".format(it) }
        }
    }

    val gridColumns: Flow<Int> = context.dataStore.data.map { prefs ->
        prefs[KEY_GRID_COLUMNS] ?: 3
    }

    val isAiEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_AI_ENABLED] ?: true
    }

    val isPeopleRecognitionEnabled: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_PEOPLE_RECOGNITION] ?: true
    }

    val themeMode: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_THEME_MODE] ?: "SYSTEM"
    }

    val vaultPinHash: Flow<String> = context.dataStore.data.map { prefs ->
        prefs[KEY_VAULT_PIN_HASH] ?: hashPin("1234")
    }

    val lastScanTime: Flow<Long> = context.dataStore.data.map { prefs ->
        prefs[KEY_LAST_SCAN_TIME] ?: 0L
    }

    val isSampleSeeded: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[KEY_SAMPLE_SEEDED] ?: false
    }

    suspend fun setGridColumns(cols: Int) {
        context.dataStore.edit { it[KEY_GRID_COLUMNS] = cols }
    }

    suspend fun setAiEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_AI_ENABLED] = enabled }
    }

    suspend fun setPeopleRecognitionEnabled(enabled: Boolean) {
        context.dataStore.edit { it[KEY_PEOPLE_RECOGNITION] = enabled }
    }

    suspend fun setThemeMode(mode: String) {
        context.dataStore.edit { it[KEY_THEME_MODE] = mode }
    }

    suspend fun setVaultPin(newPin: String) {
        context.dataStore.edit { it[KEY_VAULT_PIN_HASH] = hashPin(newPin) }
    }

    suspend fun setLastScanTime(timestamp: Long) {
        context.dataStore.edit { it[KEY_LAST_SCAN_TIME] = timestamp }
    }

    suspend fun setSampleSeeded(seeded: Boolean) {
        context.dataStore.edit { it[KEY_SAMPLE_SEEDED] = seeded }
    }
}
