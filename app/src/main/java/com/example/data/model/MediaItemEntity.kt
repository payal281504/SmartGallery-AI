package com.example.data.model

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "media_items",
    indices = [
        Index(value = ["uri"], unique = true),
        Index(value = ["isTrash", "isVault", "dateTaken"]),
        Index(value = ["sourceAlbum"]),
        Index(value = ["aiCategory"]),
        Index(value = ["isFavorite"]),
        Index(value = ["checksum"]),
        Index(value = ["personName"])
    ]
)
data class MediaItemEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    val mediaStoreId: Long = 0,
    val uri: String,
    val path: String = "",
    val displayName: String = "",
    val mimeType: String = "image/jpeg",
    val size: Long = 0,
    val dateTaken: Long = System.currentTimeMillis(),
    val dateModified: Long = System.currentTimeMillis(),
    val width: Int = 0,
    val height: Int = 0,
    val durationMs: Long = 0,
    val isVideo: Boolean = false,
    val isDocument: Boolean = false,
    val isFavorite: Boolean = false,
    val isTrash: Boolean = false,
    val isVault: Boolean = false,
    val trashTimestamp: Long = 0,
    val sourceAlbum: String = "Camera Photos",
    val aiCategory: String = "Other Images",
    val perceptualHash: Long = 0L,
    val checksum: String = "",
    val detectedFaces: Int = 0,
    val personName: String? = null,
    val extractedText: String? = null,
    val rotationDegrees: Int = 0,
    val isSample: Boolean = false
) {
    val isImage: Boolean
        get() = !isVideo && !isDocument

    fun formattedDuration(): String {
        if (durationMs <= 0) return ""
        val totalSec = durationMs / 1000
        val min = totalSec / 60
        val sec = totalSec % 60
        return if (min > 59) {
            val hr = min / 60
            val remMin = min % 60
            String.format("%d:%02d:%02d", hr, remMin, sec)
        } else {
            String.format("%d:%02d", min, sec)
        }
    }

    fun formattedSize(): String {
        return when {
            size >= 1024 * 1024 * 1024 -> String.format("%.2f GB", size / (1024.0 * 1024.0 * 1024.0))
            size >= 1024 * 1024 -> String.format("%.1f MB", size / (1024.0 * 1024.0))
            size >= 1024 -> String.format("%.1f KB", size / 1024.0)
            else -> "$size B"
        }
    }
}
