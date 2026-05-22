package com.idocar.launcher.data.database

import androidx.room.TypeConverter
import com.idocar.launcher.data.AppItem
import com.idocar.launcher.data.ShortcutItem

class Converters {
    
    @TypeConverter
    fun fromAppCategory(category: AppItem.AppCategory): String {
        return category.name
    }
    
    @TypeConverter
    fun toAppCategory(category: String): AppItem.AppCategory {
        return try {
            AppItem.AppCategory.valueOf(category)
        } catch (e: IllegalArgumentException) {
            AppItem.AppCategory.OTHER
        }
    }
    
    @TypeConverter
    fun fromActionType(type: ShortcutItem.ActionType): String {
        return type.name
    }
    
    @TypeConverter
    fun toActionType(type: String): ShortcutItem.ActionType {
        return try {
            ShortcutItem.ActionType.valueOf(type)
        } catch (e: IllegalArgumentException) {
            ShortcutItem.ActionType.CUSTOM
        }
    }
}
