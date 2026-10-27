package com.example.domain

import com.example.data.local.MediaDao
import com.example.data.model.DuplicateGroup
import com.example.data.model.MediaItemEntity
import com.example.data.model.SimilarGroup
import com.example.data.model.StorageBreakdown
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.withContext

class CleanerManager(private val mediaDao: MediaDao) {

    suspend fun getStorageBreakdown(): StorageBreakdown = withContext(Dispatchers.IO) {
        val allVisible = mediaDao.getAllVisibleMedia().first()
        val trashItems = mediaDao.getTrashMedia().first()

        var totalBytes = 0L
        var photosBytes = 0L
        var photosCount = 0
        var videosBytes = 0L
        var videosCount = 0
        var docsBytes = 0L
        var docsCount = 0

        for (item in allVisible) {
            totalBytes += item.size
            when {
                item.isVideo -> {
                    videosBytes += item.size
                    videosCount++
                }
                item.isDocument -> {
                    docsBytes += item.size
                    docsCount++
                }
                else -> {
                    photosBytes += item.size
                    photosCount++
                }
            }
        }

        var trashBytes = 0L
        for (item in trashItems) {
            trashBytes += item.size
        }

        // Calculate potential duplicate savings
        val duplicates = findExactDuplicates()
        var duplicateSavings = 0L
        var duplicateItemCount = 0
        for (group in duplicates) {
            // All copies except one can be saved
            if (group.items.size > 1) {
                val redundantCopies = group.items.size - 1
                val itemSize = group.items.first().size
                duplicateSavings += (redundantCopies * itemSize)
                duplicateItemCount += redundantCopies
            }
        }

        StorageBreakdown(
            totalBytes = totalBytes,
            photosBytes = photosBytes,
            photosCount = photosCount,
            videosBytes = videosBytes,
            videosCount = videosCount,
            documentsBytes = docsBytes,
            documentsCount = docsCount,
            duplicatesBytes = duplicateSavings,
            duplicatesCount = duplicateItemCount,
            trashBytes = trashBytes,
            trashCount = trashItems.size
        )
    }

    suspend fun findExactDuplicates(): List<DuplicateGroup> = withContext(Dispatchers.IO) {
        val items = mediaDao.getAllItemsWithChecksum()
        val grouped = items.groupBy { it.checksum }
            .filter { it.value.size > 1 && it.key.isNotEmpty() }

        grouped.map { (checksum, list) ->
            DuplicateGroup(
                checksum = checksum,
                totalSize = list.sumOf { it.size },
                items = list
            )
        }
    }

    suspend fun findSimilarPhotos(): List<SimilarGroup> = withContext(Dispatchers.IO) {
        val items = mediaDao.getAllItemsWithPerceptualHash()
        val result = mutableListOf<SimilarGroup>()
        val processed = mutableSetOf<Long>()

        for (i in items.indices) {
            val item1 = items[i]
            if (item1.id in processed) continue

            val similarList = mutableListOf(item1)
            for (j in i + 1 until items.size) {
                val item2 = items[j]
                if (item2.id in processed) continue

                val dist = AiCategorizer.hammingDistance(item1.perceptualHash, item2.perceptualHash)
                if (dist in 1..8) { // distance 0 is exact duplicate, 1..8 is visually similar
                    similarList.add(item2)
                    processed.add(item2.id)
                }
            }

            if (similarList.size > 1) {
                processed.add(item1.id)
                result.add(SimilarGroup(hash = item1.perceptualHash, items = similarList))
            }
        }

        result
    }

    suspend fun getLargeFiles(minBytes: Long = 50 * 1024 * 1024): List<MediaItemEntity> = withContext(Dispatchers.IO) {
        mediaDao.getLargeFiles(minBytes).first()
    }

    suspend fun getOldDownloads(days: Int = 30): List<MediaItemEntity> = withContext(Dispatchers.IO) {
        val cutoff = System.currentTimeMillis() - (days * 86_400_000L)
        mediaDao.getOldDownloads(cutoff).first()
    }
}
