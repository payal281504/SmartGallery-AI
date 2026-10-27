package com.example.data.local

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.example.data.model.MediaItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface MediaDao {

    @Query("SELECT * FROM media_items WHERE isTrash = 0 AND isVault = 0 ORDER BY dateTaken DESC")
    fun getAllVisibleMedia(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isTrash = 0 AND isVault = 0 AND isVideo = 0 AND isDocument = 0 ORDER BY dateTaken DESC")
    fun getPhotos(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isTrash = 0 AND isVault = 0 AND isVideo = 1 ORDER BY dateTaken DESC")
    fun getVideos(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isTrash = 0 AND isVault = 0 AND isDocument = 1 ORDER BY dateTaken DESC")
    fun getDocuments(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isTrash = 0 AND isVault = 0 AND isFavorite = 1 ORDER BY dateTaken DESC")
    fun getFavorites(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isTrash = 1 ORDER BY trashTimestamp DESC")
    fun getTrashMedia(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isVault = 1 ORDER BY dateTaken DESC")
    fun getVaultMedia(): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE id = :id LIMIT 1")
    suspend fun getById(id: Long): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE uri = :uri LIMIT 1")
    suspend fun getByUri(uri: String): MediaItemEntity?

    @Query("SELECT * FROM media_items WHERE isTrash = 0 AND isVault = 0 AND sourceAlbum = :sourceAlbum ORDER BY dateTaken DESC")
    fun getBySourceAlbum(sourceAlbum: String): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isTrash = 0 AND isVault = 0 AND aiCategory = :category ORDER BY dateTaken DESC")
    fun getByAiCategory(category: String): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isTrash = 0 AND isVault = 0 AND personName = :personName ORDER BY dateTaken DESC")
    fun getByPersonName(personName: String): Flow<List<MediaItemEntity>>

    @Query("""
        SELECT * FROM media_items 
        WHERE isTrash = 0 AND isVault = 0 
        AND (
            displayName LIKE '%' || :query || '%' 
            OR sourceAlbum LIKE '%' || :query || '%' 
            OR aiCategory LIKE '%' || :query || '%' 
            OR personName LIKE '%' || :query || '%' 
            OR extractedText LIKE '%' || :query || '%'
        )
        ORDER BY dateTaken DESC
    """)
    fun search(query: String): Flow<List<MediaItemEntity>>

    @Query("SELECT DISTINCT sourceAlbum FROM media_items WHERE isTrash = 0 AND isVault = 0")
    fun getDistinctSourceAlbums(): Flow<List<String>>

    @Query("SELECT DISTINCT aiCategory FROM media_items WHERE isTrash = 0 AND isVault = 0")
    fun getDistinctAiCategories(): Flow<List<String>>

    @Query("SELECT DISTINCT personName FROM media_items WHERE isTrash = 0 AND isVault = 0 AND personName IS NOT NULL")
    fun getDistinctPeople(): Flow<List<String>>

    @Query("SELECT * FROM media_items WHERE isTrash = 0 AND isVault = 0 AND checksum != ''")
    suspend fun getAllItemsWithChecksum(): List<MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE isTrash = 0 AND isVault = 0 AND perceptualHash != 0")
    suspend fun getAllItemsWithPerceptualHash(): List<MediaItemEntity>

    @Query("SELECT * FROM media_items WHERE isTrash = 0 AND isVault = 0 AND size > :minBytes ORDER BY size DESC")
    fun getLargeFiles(minBytes: Long = 50 * 1024 * 1024): Flow<List<MediaItemEntity>>

    @Query("SELECT * FROM media_items WHERE isTrash = 0 AND isVault = 0 AND (sourceAlbum = 'Downloads' OR sourceAlbum = 'Screenshots') AND dateTaken < :cutoffDate ORDER BY dateTaken ASC")
    fun getOldDownloads(cutoffDate: Long): Flow<List<MediaItemEntity>>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(items: List<MediaItemEntity>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(item: MediaItemEntity): Long

    @Update
    suspend fun update(item: MediaItemEntity)

    @Query("UPDATE media_items SET isFavorite = :isFavorite WHERE id = :id")
    suspend fun setFavorite(id: Long, isFavorite: Boolean)

    @Query("UPDATE media_items SET isTrash = 1, trashTimestamp = :timestamp WHERE id = :id")
    suspend fun moveToTrash(id: Long, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE media_items SET isTrash = 1, trashTimestamp = :timestamp WHERE id IN (:ids)")
    suspend fun moveToTrashBatch(ids: List<Long>, timestamp: Long = System.currentTimeMillis())

    @Query("UPDATE media_items SET isTrash = 0, trashTimestamp = 0 WHERE id = :id")
    suspend fun restoreFromTrash(id: Long)

    @Query("UPDATE media_items SET isTrash = 0, trashTimestamp = 0 WHERE id IN (:ids)")
    suspend fun restoreFromTrashBatch(ids: List<Long>)

    @Query("UPDATE media_items SET isVault = :isVault WHERE id = :id")
    suspend fun setVault(id: Long, isVault: Boolean)

    @Query("UPDATE media_items SET isVault = :isVault WHERE id IN (:ids)")
    suspend fun setVaultBatch(ids: List<Long>, isVault: Boolean)

    @Query("UPDATE media_items SET rotationDegrees = (rotationDegrees + :degrees) % 360 WHERE id = :id")
    suspend fun rotateMedia(id: Long, degrees: Int)

    @Query("UPDATE media_items SET displayName = :newName WHERE id = :id")
    suspend fun renameMedia(id: Long, newName: String)

    @Query("UPDATE media_items SET personName = :personName WHERE id = :id")
    suspend fun updatePersonName(id: Long, personName: String?)

    @Query("UPDATE media_items SET personName = :newName WHERE personName = :oldName")
    suspend fun renamePersonCluster(oldName: String, newName: String)

    @Delete
    suspend fun delete(item: MediaItemEntity)

    @Query("DELETE FROM media_items WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("DELETE FROM media_items WHERE id IN (:ids)")
    suspend fun deleteByIds(ids: List<Long>)

    @Query("DELETE FROM media_items WHERE isTrash = 1")
    suspend fun emptyTrash()

    @Query("SELECT COUNT(*) FROM media_items")
    suspend fun getCount(): Int
}
