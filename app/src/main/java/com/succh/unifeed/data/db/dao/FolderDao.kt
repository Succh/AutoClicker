package com.succh.unifeed.data.db.dao

import androidx.room.*
import com.succh.unifeed.data.db.entity.Folder
import kotlinx.coroutines.flow.Flow

@Dao
interface FolderDao {
    @Query("SELECT * FROM folders ORDER BY name ASC")
    fun observeAll(): Flow<List<Folder>>

    @Query("SELECT * FROM folders WHERE id = :id")
    suspend fun getById(id: Long): Folder?

    @Insert
    suspend fun insert(folder: Folder): Long

    @Delete
    suspend fun delete(folder: Folder)

    @Query("DELETE FROM folders WHERE id = :id")
    suspend fun deleteById(id: Long)
}
