package com.example.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.data.model.Bookmark
import com.example.data.model.HistoryItem
import com.example.data.model.KopiExtension
import com.example.data.model.SavedSite
import com.example.data.model.TabItem

@Database(
    entities = [
        TabItem::class,
        Bookmark::class,
        HistoryItem::class,
        KopiExtension::class,
        SavedSite::class
    ],
    version = 1,
    exportSchema = false
)
abstract class KopiDatabase : RoomDatabase() {
    abstract fun kopiDao(): KopiDao

    companion object {
        @Volatile
        private var INSTANCE: KopiDatabase? = null

        fun getDatabase(context: Context): KopiDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    KopiDatabase::class.java,
                    "kopi_browser.db"
                )
                .fallbackToDestructiveMigration()
                .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
