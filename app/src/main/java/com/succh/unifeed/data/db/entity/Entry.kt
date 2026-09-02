package com.succh.unifeed.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "entries")
data class Entry(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val feedId: Long,
    val guid: String,
    val title: String,
    val link: String? = null,
    val content: String? = null,
    val summary: String? = null,
    val author: String? = null,
    val publishedAt: Long = 0L,
    val isRead: Boolean = false,
    val isStarred: Boolean = false,
    val fetchedAt: Long = System.currentTimeMillis()
)
