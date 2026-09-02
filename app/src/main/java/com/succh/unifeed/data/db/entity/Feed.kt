package com.succh.unifeed.data.db.entity

import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "feeds")
data class Feed(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val title: String,
    val url: String,
    val siteUrl: String? = null,
    val description: String? = null,
    val faviconUrl: String? = null,
    val folderId: Long? = null,
    val lastUpdated: Long = 0L,
    val unreadCount: Int = 0,
    val createdAt: Long = System.currentTimeMillis()
)
