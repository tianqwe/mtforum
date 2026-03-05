package com.forum.mt;

import android.os.Bundle;
import android.view.View;
import android.widget.OverScroller;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.app.AppCompatDelegate;
import androidx.core.graphics.Insets;
import androidx.core.view.ViewCompat;
import androidx.core.view.WindowCompat;
import androidx.core.view.WindowInsetsCompat;
import androidx.recyclerview.widget.RecyclerView;
import androidx.viewpager2.widget.ViewPager2;

import com.forum.mt.adapter.MainViewPagerAdapter;
import com.forum.mt.databinding.ActivityMainBinding;
import com.forum.mt.util.AppSettings;
import com.forum.mt.util.LogToFile;

public class MainActivity extends AppCompatActivity {
    private ActivityMainBinding binding;
    private MainViewPagerAdapter adapter;
    private boolean isFromViewPager = false; // 标记是否来自ViewPager滑动，避免循环调用

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        // 应用深色模式设置（需在super.onCreate之前调用）
        applyDarkMode();

        super.onCreate(savedInstanceState);

        // 初始化日志系统
        LogToFile.init(this);
        LogToFile.i("MainActivity", "应用启动");

        // 启用edge-to-edge显示
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // 根据当前主题设置状态栏图标颜色
        int currentNightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        if (currentNightMode != android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            // 浅色模式：状态栏使用深色图标
            getWindow().getDecorView().post(() -> {
                getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                );
            });
        }

        binding = ActivityMainBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 处理 WindowInsets，解决底部导航栏遮挡问题
        setupWindowInsets();

        setupViewPager();
        setupBottomNavigation();
    }

    private void setupWindowInsets() {
        ViewCompat.setOnApplyWindowInsetsListener(binding.getRoot(), (v, windowInsets) -> {
            Insets insets = windowInsets.getInsets(WindowInsetsCompat.Type.systemBars());

            // 获取系统导航栏高度
            int navBarHeight = insets.bottom;

            // 为底部导航栏设置底部 padding，防止被系统导航栏遮挡
            binding.bottomNavigation.setPadding(
                binding.bottomNavigation.getPaddingLeft(),
                binding.bottomNavigation.getPaddingTop(),
                binding.bottomNavigation.getPaddingRight(),
                navBarHeight > 0 ? navBarHeight : dpToPx(16)
            );

            // 不要完全消费 WindowInsets，让子 View 能获取到状态栏 insets
            return windowInsets;
        });
    }

    private int dpToPx(int dp) {
        float density = getResources().getDisplayMetrics().density;
        return Math.round(dp * density);
    }

    private void setupViewPager() {
        adapter = new MainViewPagerAdapter(this);
        binding.viewPager.setAdapter(adapter);

        // 预加载所有页面（4个Fragment），避免切换时卡顿
        binding.viewPager.setOffscreenPageLimit(3);
        
        // 禁用过度滚动效果，提升滚动性能
        try {
            RecyclerView recyclerView = (RecyclerView) binding.viewPager.getChildAt(0);
            recyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
            // 优化RecyclerView缓存
            recyclerView.setItemViewCacheSize(20);
            recyclerView.setDrawingCacheEnabled(true);
            recyclerView.setDrawingCacheQuality(View.DRAWING_CACHE_QUALITY_HIGH);
        } catch (Exception ignored) {}

        // ViewPager2 页面改变回调
        binding.viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                super.onPageSelected(position);
                // ViewPager页面改变时，同步更新底部导航栏
                isFromViewPager = true;
                updateBottomNavigation(position);
                isFromViewPager = false;
            }
        });
    }

    private void updateBottomNavigation(int position) {
        switch (position) {
            case 0:
                binding.bottomNavigation.setSelectedItemId(R.id.nav_home);
                break;
            case 1:
                binding.bottomNavigation.setSelectedItemId(R.id.nav_forum);
                break;
            case 2:
                binding.bottomNavigation.setSelectedItemId(R.id.nav_message);
                break;
            case 3:
                binding.bottomNavigation.setSelectedItemId(R.id.nav_user);
                break;
        }
    }

    private void setupBottomNavigation() {
        binding.bottomNavigation.setOnItemSelectedListener(item -> {
            // 如果是ViewPager触发的，直接返回 true，避免循环
            if (isFromViewPager) {
                return true;
            }

            int itemId = item.getItemId();
            int targetPosition = 0;

            if (itemId == R.id.nav_home) {
                targetPosition = 0;
            } else if (itemId == R.id.nav_forum) {
                targetPosition = 1;
            } else if (itemId == R.id.nav_message) {
                targetPosition = 2;
            } else if (itemId == R.id.nav_user) {
                targetPosition = 3;
            } else {
                return false;
            }

            // 使用平滑动画切换页面
            int currentPosition = binding.viewPager.getCurrentItem();
            if (currentPosition != targetPosition) {
                binding.viewPager.setCurrentItem(targetPosition, true);
            }
            return true;
        });
    }

    /**
     * 应用深色模式设置
     */
    private void applyDarkMode() {
        int mode = AppSettings.getInstance(this).getDarkMode();
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
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}