package com.succh.unifeed.data.db.dao

import androidx.room.*
import com.succh.unifeed.data.db.entity.Entry
import kotlinx.coroutines.flow.Flow

@Dao
interface EntryDao {
    @Query("SELECT * FROM entries WHERE feedId = :feedId ORDER BY publishedAt DESC")
    fun observeByFeed(feedId: Long): Flow<List<Entry>>

    @Query("SELECT * FROM entries ORDER BY publishedAt DESC")
    fun observeAll(): Flow<List<Entry>>

    @Query("SELECT * FROM entries WHERE isRead = 0 ORDER BY publishedAt DESC")
    fun observeUnread(): Flow<List<Entry>>

    @Query("SELECT * FROM entries WHERE isStarred = 1 ORDER BY publishedAt DESC")
    fun observeStarred(): Flow<List<Entry>>

    @Query("SELECT * FROM entries WHERE id = :id")
    suspend fun getById(id: Long): Entry?

    @Query("SELECT * FROM entries WHERE feedId = :feedId AND guid = :guid LIMIT 1")
    suspend fun getByGuid(feedId: Long, guid: String): Entry?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(entry: Entry): Long

    @Update
    suspend fun update(entry: Entry)

    @Query("UPDATE entries SET isRead = 1 WHERE id = :id")
    suspend fun markRead(id: Long)

    @Query("UPDATE entries SET isRead = 0 WHERE id = :id")
    suspend fun markUnread(id: Long)

    @Query("UPDATE entries SET isRead = 1 WHERE feedId = :feedId AND isRead = 0")
    suspend fun markAllRead(feedId: Long)

    @Query("UPDATE entries SET isStarred = :starred WHERE id = :id")
    suspend fun setStarred(id: Long, starred: Boolean)

    @Query("SELECT COUNT(*) FROM entries WHERE isRead = 0")
    fun observeUnreadCount(): Flow<Int>

    @Query("DELETE FROM entries WHERE feedId = :feedId")
    suspend fun deleteByFeed(feedId: Long)
}
