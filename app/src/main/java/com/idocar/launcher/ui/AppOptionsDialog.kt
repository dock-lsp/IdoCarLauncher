package com.idocar.launcher.ui

import android.app.Dialog
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.idocar.launcher.R
import com.idocar.launcher.data.AppItem
import com.idocar.launcher.util.AppUtils

/**
 * 应用选项对话框
 * 显示应用的操作选项（收藏、卸载、应用信息等）
 */
class AppOptionsDialog : DialogFragment() {

    companion object {
        private const val ARG_APP = "app_item"

        fun newInstance(app: AppItem): AppOptionsDialog {
            return AppOptionsDialog().apply {
                arguments = Bundle().apply {
                    putParcelable(ARG_APP, app)
                }
            }
        }
    }

    private var onFavoriteClick: ((AppItem) -> Unit)? = null
    private var onUninstallClick: ((AppItem) -> Unit)? = null
    private var onAppInfoClick: ((AppItem) -> Unit)? = null
    private var onAddToHomeClick: ((AppItem) -> Unit)? = null
    private var onHideAppClick: ((AppItem) -> Unit)? = null

    fun setOnFavoriteClick(listener: (AppItem) -> Unit) {
        onFavoriteClick = listener
    }

    fun setOnUninstallClick(listener: (AppItem) -> Unit) {
        onUninstallClick = listener
    }

    fun setOnAppInfoClick(listener: (AppItem) -> Unit) {
        onAppInfoClick = listener
    }

    fun setOnAddToHomeClick(listener: (AppItem) -> Unit) {
        onAddToHomeClick = listener
    }

    fun setOnHideAppClick(listener: (AppItem) -> Unit) {
        onHideAppClick = listener
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val app = arguments?.getParcelable<AppItem>(ARG_APP)
            ?: throw IllegalArgumentException("AppItem is required")

        val options = mutableListOf<String>()
        val actions = mutableListOf<() -> Unit>()

        // 收藏/取消收藏
        val favoriteText = if (app.isFavorite) "取消收藏" else "收藏"
        options.add(favoriteText)
        actions.add {
            onFavoriteClick?.invoke(app)
            dismiss()
        }

        // 添加到主屏幕
        options.add("添加到主屏幕")
        actions.add {
            onAddToHomeClick?.invoke(app)
            dismiss()
        }

        // 隐藏应用
        options.add("隐藏应用")
        actions.add {
            onHideAppClick?.invoke(app)
            dismiss()
        }

        // 卸载（仅非系统应用）
        if (!app.isSystemApp) {
            options.add("卸载")
            actions.add {
                onUninstallClick?.invoke(app)
                dismiss()
            }
        }

        // 应用信息
        options.add("应用信息")
        actions.add {
            onAppInfoClick?.invoke(app)
            dismiss()
        }

        return AlertDialog.Builder(requireContext())
            .setTitle(app.appName)
            .setItems(options.toTypedArray()) { _, which ->
                actions[which].invoke()
            }
            .setNegativeButton("取消") { _, _ ->
                dismiss()
            }
            .create()
    }

    /**
     * 显示卸载确认对话框
     */
    fun showUninstallConfirmDialog(app: AppItem) {
        AlertDialog.Builder(requireContext())
            .setTitle("卸载应用")
            .setMessage("确定要卸载 ${app.appName} 吗？")
            .setPositiveButton("卸载") { _, _ ->
                val intent = Intent(Intent.ACTION_DELETE).apply {
                    data = Uri.parse("package:${app.packageName}")
                }
                startActivity(intent)
            }
            .setNegativeButton("取消", null)
            .show()
    }

    /**
     * 打开应用信息页面
     */
    fun openAppInfo(app: AppItem) {
        val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
            data = Uri.parse("package:${app.packageName}")
        }
        startActivity(intent)
    }

    /**
     * 分享应用
     */
    fun shareApp(app: AppItem) {
        val shareIntent = Intent(Intent.ACTION_SEND).apply {
            type = "text/plain"
            putExtra(Intent.EXTRA_SUBJECT, "分享应用: ${app.appName}")
            putExtra(Intent.EXTRA_TEXT, "推荐你使用 ${app.appName}")
        }
        startActivity(Intent.createChooser(shareIntent, "分享"))
    }

    /**
     * 创建快捷方式
     */
    fun createShortcut(app: AppItem) {
        val shortcutIntent = AppUtils.getLaunchIntent(requireContext(), app.packageName)
        if (shortcutIntent == null) {
            Toast.makeText(requireContext(), "无法创建快捷方式", Toast.LENGTH_SHORT).show()
            return
        }

        val addIntent = Intent("com.android.launcher.action.INSTALL_SHORTCUT").apply {
            putExtra(Intent.EXTRA_SHORTCUT_INTENT, shortcutIntent)
            putExtra(Intent.EXTRA_SHORTCUT_NAME, app.appName)
            app.icon?.let { icon ->
                putExtra(Intent.EXTRA_SHORTCUT_ICON, icon)
            }
        }

        requireContext().sendBroadcast(addIntent)
        Toast.makeText(requireContext(), "快捷方式已创建", Toast.LENGTH_SHORT).show()
    }
}
