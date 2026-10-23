package com.succh.unifeed.data.db

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.succh.unifeed.data.db.dao.EntryDao
import com.succh.unifeed.data.db.dao.FeedDao
import com.succh.unifeed.data.db.dao.FolderDao
import com.succh.unifeed.data.db.entity.Entry
import com.succh.unifeed.data.db.entity.Feed
import com.succh.unifeed.data.db.entity.Folder

@Database(
    entities = [Feed::class, Entry::class, Folder::class],
    version = 3,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun feedDao(): FeedDao
    abstract fun entryDao(): EntryDao
    abstract fun folderDao(): FolderDao

    companion object {
        /** v1 → v2：文章封面图 + 订阅源刷新状态字段 */
        val MIGRATION_1_2: Migration = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE entries ADD COLUMN imageUrl TEXT")
                db.execSQL("ALTER TABLE feeds ADD COLUMN lastRefreshTime INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE feeds ADD COLUMN lastRefreshError TEXT")
            }
        }

    /** v2 → v3：阅读进度比例 + 最近阅读时间 */
    val MIGRATION_2_3: Migration = object : Migration(2, 3) {
        override fun migrate(db: SupportSQLiteDatabase) {
            db.execSQL("ALTER TABLE entries ADD COLUMN lastReadPosition REAL NOT NULL DEFAULT 0")
            db.execSQL("ALTER TABLE entries ADD COLUMN lastReadAt INTEGER NOT NULL DEFAULT 0")
        }
    }
    }
}
