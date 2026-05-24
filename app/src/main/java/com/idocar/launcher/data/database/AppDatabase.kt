package com.idocar.launcher.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import com.idocar.launcher.data.AppItem
import com.idocar.launcher.data.ShortcutItem
import com.idocar.launcher.data.ThemeItem

@Database(
    entities = [AppItem::class, ShortcutItem::class, ThemeItem::class],
    version = 1,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    
    abstract fun appDao(): AppDao
    abstract fun shortcutDao(): ShortcutDao
    abstract fun themeDao(): ThemeDao
    
    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null
        
        fun getDatabase(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "car_launcher_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
