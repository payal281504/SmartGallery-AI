package com.example.data.model

enum class AlbumType {
    SOURCE,
    AI_CONTENT,
    CUSTOM,
    SYSTEM
}

data class Album(
    val id: String,
    val title: String,
    val type: AlbumType,
    val coverUri: String? = null,
    val itemCount: Int = 0,
    val iconName: String = "photo",
    val description: String = ""
)

data class PersonGroup(
    val id: String,
    val name: String,
    val coverUri: String? = null,
    val photoCount: Int = 0,
    val isConfirmed: Boolean = false
)

data class StorageBreakdown(
    val totalBytes: Long = 0,
    val photosBytes: Long = 0,
    val photosCount: Int = 0,
    val videosBytes: Long = 0,
    val videosCount: Int = 0,
    val documentsBytes: Long = 0,
    val documentsCount: Int = 0,
    val duplicatesBytes: Long = 0,
    val duplicatesCount: Int = 0,
    val trashBytes: Long = 0,
    val trashCount: Int = 0
) {
    fun formatBytes(bytes: Long): String {
        return when {
            bytes >= 1024 * 1024 * 1024 -> String.format("%.2f GB", bytes / (1024.0 * 1024.0 * 1024.0))
            bytes >= 1024 * 1024 -> String.format("%.1f MB", bytes / (1024.0 * 1024.0))
            bytes >= 1024 -> String.format("%.1f KB", bytes / 1024.0)
            else -> "$bytes B"
        }
    }
}

data class DuplicateGroup(
    val checksum: String,
    val totalSize: Long,
    val items: List<MediaItemEntity>
)

data class SimilarGroup(
    val hash: Long,
    val items: List<MediaItemEntity>
)
