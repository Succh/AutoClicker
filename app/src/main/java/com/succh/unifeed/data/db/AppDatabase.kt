package com.succh.unifeed.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import com.succh.unifeed.data.db.dao.EntryDao
import com.succh.unifeed.data.db.dao.FeedDao
import com.succh.unifeed.data.db.dao.FolderDao
import com.succh.unifeed.data.db.entity.Entry
import com.succh.unifeed.data.db.entity.Feed
import com.succh.unifeed.data.db.entity.Folder

@Database(
    entities = [Feed::class, Entry::class, Folder::class],
    version = 1,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun feedDao(): FeedDao
    abstract fun entryDao(): EntryDao
    abstract fun folderDao(): FolderDao
}
