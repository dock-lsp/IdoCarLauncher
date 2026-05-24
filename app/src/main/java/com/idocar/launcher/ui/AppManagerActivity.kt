package com.idocar.launcher.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.LayoutInflater
import android.view.Menu
import android.view.MenuItem
import android.view.View
import android.widget.ImageView
import android.widget.PopupMenu
import android.widget.TextView
import android.widget.Toast
import androidx.activity.viewModels
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.widget.SearchView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.idocar.launcher.R
import com.idocar.launcher.adapter.AppGridAdapter
import com.idocar.launcher.data.AppItem
import com.idocar.launcher.databinding.ActivityAppManagerBinding
import com.idocar.launcher.ui.viewmodel.AppManagerViewModel
import com.idocar.launcher.util.AppUtils
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch

/**
 * 应用管理界面
 */
class AppManagerActivity : AppCompatActivity() {

    private lateinit var binding: ActivityAppManagerBinding
    private val viewModel: AppManagerViewModel by viewModels()
    private lateinit var appAdapter: AppGridAdapter

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityAppManagerBinding.inflate(layoutInflater)
        setContentView(binding.root)

        supportActionBar?.apply {
            setDisplayHomeAsUpEnabled(true)
            title = getString(R.string.app_manager)
        }

        setupRecyclerView()
        setupListeners()
        observeApps()
    }

    private fun setupRecyclerView() {
        appAdapter = AppGridAdapter(
            onAppClick = { app ->
                showAppActionsDialog(app)
            },
            onAppLongClick = { app ->
                showAppOptions(app)
            }
        )

        binding.recyclerApps.apply {
            layoutManager = GridLayoutManager(this@AppManagerActivity, 4)
            adapter = appAdapter
        }
    }

    private fun setupListeners() {
        // 分类筛选
        binding.chipGroupCategory.setOnCheckedStateChangeListener { _, checkedIds ->
            when (checkedIds.firstOrNull()) {
                R.id.chip_all -> viewModel.filterByCategory(null)
                R.id.chip_navigation -> viewModel.filterByCategory(AppItem.AppCategory.NAVIGATION)
                R.id.chip_music -> viewModel.filterByCategory(AppItem.AppCategory.MUSIC)
                R.id.chip_video -> viewModel.filterByCategory(AppItem.AppCategory.VIDEO)
                R.id.chip_tools -> viewModel.filterByCategory(AppItem.AppCategory.TOOLS)
            }
        }

        // 刷新按钮
        binding.btnRefresh.setOnClickListener {
            refreshApps()
        }

        // 隐藏系统应用开关
        binding.switchHideSystem.setOnCheckedChangeListener { _, isChecked ->
            viewModel.setHideSystemApps(isChecked)
        }
    }

    private fun observeApps() {
        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.filteredApps.collectLatest { apps ->
                    appAdapter.submitList(apps)
                    updateEmptyState(apps.isEmpty())
                }
            }
        }
    }

    private fun updateEmptyState(isEmpty: Boolean) {
        binding.emptyView.visibility = if (isEmpty) View.VISIBLE else View.GONE
        binding.recyclerApps.visibility = if (isEmpty) View.GONE else View.VISIBLE
    }

    private fun launchApp(app: AppItem) {
        val intent = AppUtils.getLaunchIntent(this, app.packageName)
        if (intent != null) {
            startActivity(intent)
        } else {
            Toast.makeText(this, R.string.cannot_open_app, Toast.LENGTH_SHORT).show()
        }
    }

    private fun showAppActionsDialog(app: AppItem) {
        val dialogView = LayoutInflater.from(this).inflate(R.layout.dialog_app_actions, null)
        val ivAppIcon = dialogView.findViewById<ImageView>(R.id.iv_app_icon)
        val tvAppName = dialogView.findViewById<TextView>(R.id.tv_app_name)
        val tvPackageName = dialogView.findViewById<TextView>(R.id.tv_package_name)

        ivAppIcon.setImageDrawable(app.icon)
        tvAppName.text = app.appName
        tvPackageName.text = app.packageName

        val dialog = AlertDialog.Builder(this)
            .setTitle("应用操作")
            .setView(dialogView)
            .create()

        // 启动应用
        dialogView.findViewById<View>(R.id.btn_launch_app).setOnClickListener {
            launchApp(app)
            dialog.dismiss()
        }

        // 卸载应用
        dialogView.findViewById<View>(R.id.btn_uninstall_app).setOnClickListener {
            uninstallApp(app)
            dialog.dismiss()
        }

        // 应用详情
        dialogView.findViewById<View>(R.id.btn_app_info).setOnClickListener {
            showAppInfo(app)
            dialog.dismiss()
        }

        // 复制包名
        dialogView.findViewById<View>(R.id.btn_copy_package).setOnClickListener {
            val clipboard = getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
            val clip = ClipData.newPlainText("package_name", app.packageName)
            clipboard.setPrimaryClip(clip)
            Toast.makeText(this, "包名已复制: ${app.packageName}", Toast.LENGTH_SHORT).show()
            dialog.dismiss()
        }

        // 添加到主页应用网格
        dialogView.findViewById<View>(R.id.btn_add_to_grid).setOnClickListener {
            addToHomeGrid(app)
            dialog.dismiss()
        }

        dialog.show()
    }

    private fun addToHomeGrid(app: AppItem) {
        val prefs = getSharedPreferences("home_grid", MODE_PRIVATE)
        val apps = prefs.getStringSet("added_apps", mutableSetOf())?.toMutableSet() ?: mutableSetOf()
        if (apps.size >= 10) {
            Toast.makeText(this, "主页应用网格最多添加10个应用", Toast.LENGTH_SHORT).show()
            return
        }
        if (apps.contains(app.packageName)) {
            Toast.makeText(this, "该应用已在主页网格中", Toast.LENGTH_SHORT).show()
            return
        }
        apps.add(app.packageName)
        prefs.edit().putStringSet("added_apps", apps).apply()
        Toast.makeText(this, "已添加 ${app.appName} 到主页网格", Toast.LENGTH_SHORT).show()
    }

    private fun showAppOptions(app: AppItem) {
        val popup = PopupMenu(this, binding.recyclerApps)
        popup.menuInflater.inflate(R.menu.menu_app_options, popup.menu)

        // 更新收藏菜单项
        val favoriteItem = popup.menu.findItem(R.id.action_favorite)
        favoriteItem?.title = if (app.isFavorite) "取消收藏" else "收藏"

        popup.setOnMenuItemClickListener { item ->
            when (item.itemId) {
                R.id.action_favorite -> {
                    viewModel.toggleFavorite(app)
                    true
                }
                R.id.action_uninstall -> {
                    uninstallApp(app)
                    true
                }
                R.id.action_app_info -> {
                    showAppInfo(app)
                    true
                }
                else -> false
            }
        }

        popup.show()
    }

    private fun uninstallApp(app: AppItem) {
        if (app.isSystemApp) {
            Toast.makeText(this, "系统应用无法卸载", Toast.LENGTH_SHORT).show()
            return
        }

        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("卸载应用")
            .setMessage("确定要卸载 ${app.appName} 吗？")
            .setPositiveButton("卸载") { _, _ ->
                val intent = Intent(android.content.Intent.ACTION_DELETE).apply {
                    data = android.net.Uri.parse("package:${app.packageName}")
                }
                startActivity(intent)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    private fun showAppInfo(app: AppItem) {
        val intent = Intent(android.provider.Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = android.net.Uri.parse("package:${app.packageName}")
        }
        startActivity(intent)
    }

    private fun refreshApps() {
        binding.progressBar.visibility = View.VISIBLE
        viewModel.refreshApps()
        binding.progressBar.visibility = View.GONE
        Toast.makeText(this, "应用列表已刷新", Toast.LENGTH_SHORT).show()
    }

    override fun onCreateOptionsMenu(menu: Menu): Boolean {
        menuInflater.inflate(R.menu.menu_app_manager, menu)

        val searchItem = menu.findItem(R.id.action_search)
        val searchView = searchItem?.actionView as? SearchView

        searchView?.setOnQueryTextListener(object : SearchView.OnQueryTextListener {
            override fun onQueryTextSubmit(query: String?): Boolean = false

            override fun onQueryTextChange(newText: String?): Boolean {
                viewModel.searchApps(newText ?: "")
                return true
            }
        })

        return true
    }

    override fun onOptionsItemSelected(item: MenuItem): Boolean {
        return when (item.itemId) {
            android.R.id.home -> {
                finish()
                true
            }
            R.id.action_sort -> {
                showSortOptions()
                true
            }
            else -> super.onOptionsItemSelected(item)
        }
    }

    private fun showSortOptions() {
        val options = arrayOf("名称", "最近使用", "安装时间")
        androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("排序方式")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> viewModel.sortBy(AppManagerViewModel.SortType.NAME)
                    1 -> viewModel.sortBy(AppManagerViewModel.SortType.RECENT)
                    2 -> viewModel.sortBy(AppManagerViewModel.SortType.INSTALL_TIME)
                }
            }
            .show()
    }
}
