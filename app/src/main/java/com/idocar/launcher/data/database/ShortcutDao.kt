package com.idocar.launcher.data.database

import androidx.room.*
import com.idocar.launcher.data.ShortcutItem
import kotlinx.coroutines.flow.Flow

@Dao
interface ShortcutDao {
    
    @Query("SELECT * FROM shortcuts ORDER BY sortOrder ASC")
    fun getAllShortcuts(): Flow<List<ShortcutItem>>
    
    @Query("SELECT * FROM shortcuts WHERE actionType = :type ORDER BY sortOrder ASC")
    fun getShortcutsByType(type: ShortcutItem.ActionType): Flow<List<ShortcutItem>>
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShortcut(shortcut: ShortcutItem): Long
    
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShortcuts(shortcuts: List<ShortcutItem>)
    
    @Update
    suspend fun updateShortcut(shortcut: ShortcutItem)
    
    @Delete
    suspend fun deleteShortcut(shortcut: ShortcutItem)
    
    @Query("DELETE FROM shortcuts WHERE id = :id")
    suspend fun deleteShortcutById(id: Long)
    
    @Query("SELECT * FROM shortcuts WHERE id = :id LIMIT 1")
    suspend fun getShortcutById(id: Long): ShortcutItem?
}
