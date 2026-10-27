package com.example.domain

import android.content.Context
import com.example.data.local.MediaDao
import com.example.data.local.PreferencesManager
import com.example.data.model.MediaItemEntity
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import java.io.File

class VaultManager(
    private val context: Context,
    private val mediaDao: MediaDao,
    private val preferencesManager: PreferencesManager
) {
    private val vaultDir = File(context.filesDir, "vault_secure").apply { mkdirs() }

    val vaultMedia: Flow<List<MediaItemEntity>> = mediaDao.getVaultMedia()

    suspend fun verifyPin(enteredPin: String): Boolean {
        val currentHash = preferencesManager.vaultPinHash.first()
        val enteredHash = PreferencesManager.hashPin(enteredPin)
        return currentHash == enteredHash
    }

    suspend fun setPin(newPin: String) {
        preferencesManager.setVaultPin(newPin)
    }

    suspend fun moveToVault(mediaItem: MediaItemEntity) {
        mediaDao.setVault(mediaItem.id, true)
    }

    suspend fun moveToVaultBatch(ids: List<Long>) {
        mediaDao.setVaultBatch(ids, true)
    }

    suspend fun restoreFromVault(mediaItem: MediaItemEntity) {
        mediaDao.setVault(mediaItem.id, false)
    }

    suspend fun restoreFromVaultBatch(ids: List<Long>) {
        mediaDao.setVaultBatch(ids, false)
    }
}
