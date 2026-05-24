package com.idocar.launcher.data.database

import androidx.room.*
import com.idocar.launcher.data.ThemeItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ThemeDao {
    
    @Query("SELECT * FROM themes")
    fun getAllThemes(): Flow<List<ThemeItem>>
    
    @Query("SELECT * FROM themes WHERE isDownloaded = 1")
    fun getDownloadedThemes(): Flow<List<ThemeItem>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTheme(theme: ThemeItem)
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertThemes(themes: List<ThemeItem>)
    
    @Update
    suspend fun updateTheme(theme: ThemeItem)
    
    @Delete
    suspend fun deleteTheme(theme: ThemeItem)
    
    @Query("UPDATE themes SET isDownloaded = :downloaded WHERE id = :themeId")
    suspend fun updateDownloadStatus(themeId: String, downloaded: Boolean)
    
    @Query("SELECT * FROM themes WHERE id = :themeId LIMIT 1")
    suspend fun getThemeById(themeId: String): ThemeItem?
}
