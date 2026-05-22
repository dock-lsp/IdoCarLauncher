package com.idocar.launcher.data.database

import androidx.lifecycle.LiveData
import androidx.room.*
import com.idocar.launcher.data.AppItem
import kotlinx.coroutines.flow.Flow

@Dao
interface AppDao {
    
    @Query("SELECT * FROM apps ORDER BY sortOrder ASC, appName ASC")
    fun getAllApps(): Flow<List<AppItem>>
    
    @Query("SELECT * FROM apps WHERE category = :category ORDER BY sortOrder ASC")
    fun getAppsByCategory(category: AppItem.AppCategory): Flow<List<AppItem>>
    
    @Query("SELECT * FROM apps WHERE isFavorite = 1 ORDER BY sortOrder ASC")
    fun getFavoriteApps(): Flow<List<AppItem>>
    
    @Query("SELECT * FROM apps WHERE packageName = :packageName LIMIT 1")
    suspend fun getAppByPackageName(packageName: String): AppItem?
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApp(app: AppItem)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertApps(apps: List<AppItem>)
    
    @Update
    suspend fun updateApp(app: AppItem)
    
    @Delete
    suspend fun deleteApp(app: AppItem)
    
    @Query("DELETE FROM apps WHERE packageName = :packageName")
    suspend fun deleteAppByPackageName(packageName: String)
    
    @Query("UPDATE apps SET isFavorite = :isFavorite WHERE packageName = :packageName")
    suspend fun updateFavoriteStatus(packageName: String, isFavorite: Boolean)
    
    @Query("UPDATE apps SET lastUsedTime = :timestamp WHERE packageName = :packageName")
    suspend fun updateLastUsedTime(packageName: String, timestamp: Long = System.currentTimeMillis())
    
    @Query("SELECT COUNT(*) FROM apps")
    suspend fun getAppCount(): Int
}
