package com.succh.unifeed.data.db.dao

import androidx.room.*
import com.succh.unifeed.data.db.entity.Feed
import kotlinx.coroutines.flow.Flow

@Dao
interface FeedDao {
    @Query("SELECT * FROM feeds ORDER BY title ASC")
    fun observeAll(): Flow<List<Feed>>

    @Query("SELECT * FROM feeds WHERE id = :id")
    suspend fun getById(id: Long): Feed?

    @Query("SELECT * FROM feeds WHERE url = :url LIMIT 1")
    suspend fun getByUrl(url: String): Feed?

    @Query("SELECT * FROM feeds WHERE folderId = :folderId")
    suspend fun getByFolder(folderId: Long): List<Feed>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(feed: Feed): Long

    @Update
    suspend fun update(feed: Feed)

    @Delete
    suspend fun delete(feed: Feed)

    @Query("DELETE FROM feeds WHERE id = :id")
    suspend fun deleteById(id: Long)

    @Query("UPDATE feeds SET unreadCount = (SELECT COUNT(*) FROM entries WHERE feedId = :feedId AND isRead = 0) WHERE id = :feedId")
    suspend fun updateUnreadCount(feedId: Long)
}
