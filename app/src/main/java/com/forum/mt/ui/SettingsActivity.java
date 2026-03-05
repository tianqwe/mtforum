package com.forum.mt.ui;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.text.format.Formatter;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;

import com.forum.mt.R;
import com.forum.mt.api.HttpClient;
import com.forum.mt.databinding.ActivitySettingsBinding;
import com.forum.mt.util.AppSettings;

import java.io.File;

/**
 * 设置页面
 */
public class SettingsActivity extends AppCompatActivity {

    private ActivitySettingsBinding binding;

    public static void start(Context context) {
        Intent intent = new Intent(context, SettingsActivity.class);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 启用edge-to-edge显示
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        binding = ActivitySettingsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // 根据当前主题设置状态栏图标颜色
        setupStatusBar();

        setupToolbar();
        setupViews();
        updateCacheSize();
        updateVersionInfo();
        updateCookieStatus();
    }

    private void setupToolbar() {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
    }

    private void setupViews() {
        // 清除缓存
        binding.menuClearCache.setOnClickListener(v -> showClearCacheDialog());

        // 图片加载设置
        binding.menuImageLoad.setOnClickListener(v -> showImageLoadDialog());
        // 显示当前图片加载设置
        binding.imageLoadSummary.setText(AppSettings.getInstance(this).getImageLoadModeDesc());

        // 深色模式
        binding.menuDarkMode.setOnClickListener(v -> showDarkModeDialog());
        // 显示当前深色模式设置
        binding.darkModeSummary.setText(AppSettings.getInstance(this).getDarkModeDesc());

        // 字体大小
        binding.menuFontSize.setOnClickListener(v -> showFontSizeDialog());
        // 显示当前字体大小设置
        binding.fontSizeSummary.setText(AppSettings.getInstance(this).getFontSizeDesc());

        // Cookie 管理
        binding.menuCookieManage.setOnClickListener(v -> showCookieDialog());

        // 退出登录
        binding.menuLogout.setOnClickListener(v -> showLogoutDialog());

        // 版本信息
        binding.menuVersion.setOnClickListener(v -> showVersionDialog());

        // 检查更新
        binding.menuCheckUpdate.setOnClickListener(v -> {
            Toast.makeText(this, "当前已是最新版本", Toast.LENGTH_SHORT).show();
        });

        // 开源许可
        binding.menuOpenSource.setOnClickListener(v -> {
            new AlertDialog.Builder(this)
                    .setTitle("开源许可")
                    .setMessage("本项目使用以下开源库：\n\n" +
                            "• OkHttp - Square\n" +
                            "• Retrofit - Square\n" +
                            "• Glide - Bump Technologies\n" +
                            "• Jsoup - jhy\n" +
                            "• Gson - Google\n" +
                            "• Material Components - Google")
                    .setPositiveButton("确定", null)
                    .show();
        });
    }

    /**
     * 计算并显示缓存大小
     */
    private void updateCacheSize() {
        try {
            File cacheDir = getCacheDir();
            long size = getDirSize(cacheDir);
            String sizeStr = Formatter.formatFileSize(this, size);
            binding.cacheSizeText.setText(sizeStr);
        } catch (Exception e) {
            binding.cacheSizeText.setText("0 MB");
        }
    }

    /**
     * 递归计算目录大小
     */
    private long getDirSize(File dir) {
        if (dir == null || !dir.exists()) return 0;
        long size = 0;
        File[] files = dir.listFiles();
        if (files != null) {
            for (File file : files) {
                if (file.isDirectory()) {
                    size += getDirSize(file);
                } else {
                    size += file.length();
                }
            }
        }
        return size;
    }

    /**
     * 显示清除缓存对话框
     */
    private void showClearCacheDialog() {
        new AlertDialog.Builder(this)
                .setTitle("清除缓存")
                .setMessage("确定要清除所有缓存数据吗？")
                .setPositiveButton("清除", (dialog, which) -> {
                    clearCache();
                    updateCacheSize();
                    Toast.makeText(this, "缓存已清除", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 清除缓存
     */
    private void clearCache() {
        try {
            File cacheDir = getCacheDir();
            deleteDir(cacheDir);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    /**
     * 递归删除目录
     */
    private boolean deleteDir(File dir) {
        if (dir != null && dir.isDirectory()) {
            String[] children = dir.list();
            if (children != null) {
                for (String child : children) {
                    boolean success = deleteDir(new File(dir, child));
                    if (!success) return false;
                }
            }
            return dir.delete();
        } else if (dir != null && dir.isFile()) {
            return dir.delete();
        }
        return false;
    }

    /**
     * 显示图片加载设置对话框
     */
    private void showImageLoadDialog() {
        String[] items = {"始终加载", "仅WiFi加载", "不加载"};
        int currentMode = AppSettings.getInstance(this).getImageLoadMode();
        
        new AlertDialog.Builder(this)
                .setTitle("帖子列表缩略图")
                .setSingleChoiceItems(items, currentMode, (dialog, which) -> {
                    // 保存设置
                    AppSettings.getInstance(this).setImageLoadMode(which);
                    binding.imageLoadSummary.setText(items[which]);
                    dialog.dismiss();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 显示深色模式设置对话框
     */
    private void showDarkModeDialog() {
        String[] items = {"跟随系统", "浅色模式", "深色模式"};
        int currentMode = AppSettings.getInstance(this).getDarkMode();
        
        new AlertDialog.Builder(this)
                .setTitle("显示模式")
                .setSingleChoiceItems(items, currentMode, (dialog, which) -> {
                    // 保存设置
                    AppSettings.getInstance(this).setDarkMode(which);
                    // 应用主题
                    applyDarkMode(which);
                    dialog.dismiss();
                })
                .show();
    }
    
    /**
     * 应用深色模式
     */
    private void applyDarkMode(int mode) {
        switch (mode) {
            case AppSettings.DARK_MODE_FOLLOW_SYSTEM:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM);
                break;
            case AppSettings.DARK_MODE_LIGHT:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO);
                break;
            case AppSettings.DARK_MODE_DARK:
                AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES);
                break;
        }
        // 更新显示文字
        binding.darkModeSummary.setText(AppSettings.getInstance(this).getDarkModeDesc());
    }

    /**
     * 显示字体大小设置对话框
     */
    private void showFontSizeDialog() {
        String[] items = {"小", "标准", "大", "特大"};
        int currentSize = AppSettings.getInstance(this).getFontSize();
        new AlertDialog.Builder(this)
                .setTitle("字体大小")
                .setSingleChoiceItems(items, currentSize, (dialog, which) -> {
                    // 保存设置
                    AppSettings.getInstance(this).setFontSize(which);
                    binding.fontSizeSummary.setText(items[which]);
                    dialog.dismiss();
                    Toast.makeText(this, "字体大小已更改，重启应用后生效", Toast.LENGTH_SHORT).show();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 显示Cookie管理对话框
     */
    private void showCookieDialog() {
        boolean isLoggedIn = HttpClient.getInstance(this).isLoggedIn();
        new AlertDialog.Builder(this)
                .setTitle("Cookie 管理")
                .setMessage(isLoggedIn 
                        ? "当前已保存登录状态\n\n点击\"重新导入\"可以更新Cookie"
                        : "当前未登录\n\n请先导入Cookie以登录")
                .setPositiveButton("重新导入", (dialog, which) -> {
                    // 跳转到登录页面
                    startActivity(new Intent(this, LoginActivity.class));
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 显示退出登录对话框
     */
    private void showLogoutDialog() {
        new AlertDialog.Builder(this)
                .setTitle("退出登录")
                .setMessage("确定要退出登录吗？")
                .setPositiveButton("退出", (dialog, which) -> {
                    // 清除Cookie
                    HttpClient.getInstance(this).clearCookies();
                    Toast.makeText(this, "已退出登录", Toast.LENGTH_SHORT).show();
                    updateCookieStatus();
                    // 返回主页
                    finish();
                })
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 更新版本信息
     */
    private void updateVersionInfo() {
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            binding.versionText.setText("v" + info.versionName);
        } catch (PackageManager.NameNotFoundException e) {
            binding.versionText.setText("v1.0.0");
        }
    }

    /**
     * 显示版本信息对话框
     */
    private void showVersionDialog() {
        String versionName = "1.0.0";
        try {
            PackageInfo info = getPackageManager().getPackageInfo(getPackageName(), 0);
            versionName = info.versionName;
        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }

        new AlertDialog.Builder(this)
                .setTitle("版本信息")
                .setMessage("MT论坛客户端\n\n版本: v" + versionName + "\n\n仅供学习交流使用")
                .setPositiveButton("确定", null)
                .show();
    }

    /**
     * 更新Cookie状态显示
     */
    private void updateCookieStatus() {
        boolean isLoggedIn = HttpClient.getInstance(this).isLoggedIn();
        if (isLoggedIn) {
            binding.cookieStatusText.setText("已登录");
            binding.cookieStatusText.setTextColor(getColor(R.color.success));
            binding.menuLogout.setVisibility(View.VISIBLE);
        } else {
            binding.cookieStatusText.setText("未登录");
            binding.cookieStatusText.setTextColor(getColor(R.color.text_tertiary));
            binding.menuLogout.setVisibility(View.GONE);
        }
    }
    
    /**
     * 根据当前主题设置状态栏图标颜色
     */
    private void setupStatusBar() {
        getWindow().getDecorView().post(() -> {
            int currentNightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                // 深色模式：状态栏使用浅色图标
                getWindow().getDecorView().setSystemUiVisibility(0);
            } else {
                // 浅色模式：状态栏使用深色图标
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        });
    }
}
