package com.idocar.launcher.ui

import android.os.Bundle
import android.view.MenuItem
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.idocar.launcher.adapter.ThemeAdapter
import com.idocar.launcher.data.ThemeItem
import com.idocar.launcher.databinding.ActivityThemeBinding
import com.idocar.launcher.ui.viewmodel.ThemeViewModel
import kotlinx.coroutines.launch

/**
 * 主题设置界面
 */
class ThemeActivity : AppCompatActivity() {

    private lateinit var binding: ActivityThemeBinding
    private val viewModel: ThemeViewModel by viewModels()
    private lateinit var themeAdapter: ThemeAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityThemeBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = "主题设置"
        }

        setupRecyclerView()
        setupListeners()
        observeThemes()
    }

    private fun setupRecyclerView() {
        themeAdapter = ThemeAdapter(
            onThemeSelected = { theme ->
                applyTheme(theme)
            },
            onThemeDownload = { theme ->
                downloadTheme(theme)
            }
        )

        binding.recyclerThemes.apply {
            layoutManager = GridLayoutManager(this@ThemeActivity, 2)
            adapter = themeAdapter
        }
    }

    private fun setupListeners() {
        binding.btnApply.setOnClickListener {
            viewModel.selectedTheme.value?.let {
                applyTheme(it)
            }
        }

        binding.btnReset.setOnClickListener {
            resetToDefaultTheme()
        }
    }

    private fun observeThemes() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.themes.collect { themes ->
                    themeAdapter.submitList(themes)
                }
            }
        }

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.selectedTheme.collect { theme ->
                    theme?.let {
                        themeAdapter.setSelectedTheme(it.id)
                    }
                }
            }
        }
    }

    private fun applyTheme(theme: ThemeItem) {
        viewModel.selectTheme(theme)
        
        // 应用主题
        val sharedPref = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        sharedPref.edit().apply {
            putString("current_theme_id", theme.id)
            putInt("primary_color", theme.primaryColor)
            putInt("accent_color", theme.accentColor)
            putBoolean("is_dark_theme", theme.isDarkTheme)
            apply()
        }

        // 应用壁纸
        theme.wallpaperUrl.takeIf { it.isNotEmpty() }?.let { url ->
            // 加载并设置壁纸
        }

        Toast.makeText(this, "主题已应用", Toast.LENGTH_SHORT).show()
        
        // 重新创建 Activity 以应用主题
        recreate()
    }

    private fun downloadTheme(theme: ThemeItem) {
        // 下载主题资源
        Toast.makeText(this, "正在下载主题...", Toast.LENGTH_SHORT).show()
        viewModel.downloadTheme(theme)
    }

    private fun resetToDefaultTheme() {
        val sharedPref = getSharedPreferences("theme_prefs", MODE_PRIVATE)
        sharedPref.edit().clear().apply()
        
        Toast.makeText(this, "已恢复默认主题", Toast.LENGTH_SHORT).show()
        recreate()
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }
}
