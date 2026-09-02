package com.succh.unifeed

import android.app.Application
import androidx.room.Room
import com.succh.unifeed.data.db.AppDatabase
import com.succh.unifeed.data.repository.FeedRepository

class UniFeedApp : Application() {

    val database: AppDatabase by lazy {
        Room.databaseBuilder(this, AppDatabase::class.java, "unifeed.db")
            .fallbackToDestructiveMigration()
            .build()
    }

    val repository: FeedRepository by lazy {
        FeedRepository(database)
    }
}
