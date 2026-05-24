package com.idocar.launcher.ui.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.idocar.launcher.data.AppItem
import com.idocar.launcher.data.ShortcutItem
import com.idocar.launcher.data.database.AppDatabase
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class HomeViewModel(private val database: AppDatabase) : ViewModel() {

    val apps: Flow<List<AppItem>> = database.appDao().getAllApps()
    val shortcuts: Flow<List<ShortcutItem>> = database.shortcutDao().getAllShortcuts()
    val favoriteApps: Flow<List<AppItem>> = database.appDao().getFavoriteApps()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedCategory = MutableStateFlow<AppItem.AppCategory?>(null)
    val selectedCategory: StateFlow<AppItem.AppCategory?> = _selectedCategory.asStateFlow()

    fun updateSearchQuery(query: String) {
        _searchQuery.value = query
    }

    fun selectCategory(category: AppItem.AppCategory?) {
        _selectedCategory.value = category
    }

    fun updateAppUsage(packageName: String) {
        viewModelScope.launch {
            database.appDao().updateLastUsedTime(packageName)
        }
    }

    fun toggleFavorite(app: AppItem) {
        viewModelScope.launch {
            database.appDao().updateFavoriteStatus(app.packageName, !app.isFavorite)
        }
    }

    fun updateAppSortOrder(apps: List<AppItem>) {
        viewModelScope.launch {
            apps.forEachIndexed { index, app ->
                database.appDao().updateApp(app.copy(sortOrder = index))
            }
        }
    }

    fun addShortcut(shortcut: ShortcutItem) {
        viewModelScope.launch {
            database.shortcutDao().insertShortcut(shortcut)
        }
    }

    fun removeShortcut(shortcut: ShortcutItem) {
        viewModelScope.launch {
            database.shortcutDao().deleteShortcut(shortcut)
        }
    }

    class Factory(private val database: AppDatabase) : ViewModelProvider.Factory {
        @Suppress("UNCHECKED_CAST")
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            if (modelClass.isAssignableFrom(HomeViewModel::class.java)) {
                return HomeViewModel(database) as T
            }
            throw IllegalArgumentException("Unknown ViewModel class")
        }
    }
}
