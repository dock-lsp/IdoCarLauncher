package com.idocar.launcher.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.idocar.launcher.data.AppItem
import com.idocar.launcher.util.AppUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch

/**
 * 应用管理 ViewModel
 */
class AppManagerViewModel(application: Application) : AndroidViewModel(application) {

    private val _allApps = MutableStateFlow<List<AppItem>>(emptyList())
    private val _searchQuery = MutableStateFlow("")
    private val _selectedCategory = MutableStateFlow<AppItem.AppCategory?>(null)
    private val _hideSystemApps = MutableStateFlow(false)
    private val _sortType = MutableStateFlow(SortType.NAME)

    private val _filteredApps = MutableStateFlow<List<AppItem>>(emptyList())
    val filteredApps: StateFlow<List<AppItem>> = _filteredApps.asStateFlow()

    init {
        viewModelScope.launch {
            combine(
                _allApps,
                _searchQuery,
                _selectedCategory,
                _hideSystemApps,
                _sortType
            ) { apps, query, category, hideSystem, sort ->
                filterAndSortApps(apps, query, category, hideSystem, sort)
            }.collect { filtered ->
                _filteredApps.value = filtered
            }
        }

        loadApps()
    }

    private fun loadApps() {
        viewModelScope.launch {
            _allApps.value = com.idocar.launcher.util.AppUtils.getInstalledApps(getApplication())
        }
    }

    private fun filterAndSortApps(
        apps: List<AppItem>,
        query: String,
        category: AppItem.AppCategory?,
        hideSystem: Boolean,
        sort: SortType
    ): List<AppItem> {
        var result = apps

        // 搜索过滤
        if (query.isNotEmpty()) {
            result = result.filter {
                it.appName.contains(query, ignoreCase = true) ||
                it.packageName.contains(query, ignoreCase = true)
            }
        }

        // 分类过滤
        category?.let {
            result = result.filter { it.category == category }
        }

        // 隐藏系统应用
        if (hideSystem) {
            result = result.filter { !it.isSystemApp }
        }

        // 排序
        result = when (sort) {
            SortType.NAME -> result.sortedBy { it.appName }
            SortType.RECENT -> result.sortedByDescending { it.lastUsedTime }
            SortType.INSTALL_TIME -> result.sortedByDescending { it.sortOrder }
        }

        return result
    }

    fun searchApps(query: String) {
        _searchQuery.value = query
    }

    fun filterByCategory(category: AppItem.AppCategory?) {
        _selectedCategory.value = category
    }

    fun setHideSystemApps(hide: Boolean) {
        _hideSystemApps.value = hide
    }

    fun sortBy(sortType: SortType) {
        _sortType.value = sortType
    }

    fun toggleFavorite(app: AppItem) {
        viewModelScope.launch {
            // 更新数据库或偏好设置
            val updated = app.copy(isFavorite = !app.isFavorite)
            _allApps.value = _allApps.value.map {
                if (it.packageName == app.packageName) updated else it
            }
        }
    }

    fun refreshApps() {
        loadApps()
    }

    enum class SortType {
        NAME,
        RECENT,
        INSTALL_TIME
    }
}
