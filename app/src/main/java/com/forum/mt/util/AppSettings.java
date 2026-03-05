package com.forum.mt.util;

import android.content.Context;
import android.content.SharedPreferences;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;

/**
 * 应用设置管理类
 */
public class AppSettings {
    private static final String PREF_NAME = "mt_forum_settings";
    
    // 帖子列表缩略图设置
    public static final int IMAGE_LOAD_ALWAYS = 0;      // 始终加载
    public static final int IMAGE_LOAD_WIFI_ONLY = 1;   // 仅WiFi加载
    public static final int IMAGE_LOAD_NEVER = 2;       // 不加载
    
    // 深色模式设置
    public static final int DARK_MODE_FOLLOW_SYSTEM = 0;  // 跟随系统
    public static final int DARK_MODE_LIGHT = 1;           // 浅色模式
    public static final int DARK_MODE_DARK = 2;            // 深色模式
    
    // 字体大小设置
    public static final int FONT_SIZE_SMALL = 0;    // 小
    public static final int FONT_SIZE_NORMAL = 1;   // 标准
    public static final int FONT_SIZE_LARGE = 2;    // 大
    public static final int FONT_SIZE_EXTRA_LARGE = 3;  // 特大
    
    private static final String KEY_IMAGE_LOAD_MODE = "image_load_mode";
    private static final String KEY_DARK_MODE = "dark_mode";
    private static final String KEY_FONT_SIZE = "font_size";
    
    private static AppSettings instance;
    private final SharedPreferences prefs;
    private final Context context;
    
    private AppSettings(Context context) {
        this.context = context.getApplicationContext();
        this.prefs = this.context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
    }
    
    public static synchronized AppSettings getInstance(Context context) {
        if (instance == null) {
            instance = new AppSettings(context);
        }
        return instance;
    }
    
    // ==================== 缩略图设置 ====================
    
    /**
     * 获取缩略图加载模式
     */
    public int getImageLoadMode() {
        return prefs.getInt(KEY_IMAGE_LOAD_MODE, IMAGE_LOAD_ALWAYS);
    }
    
    /**
     * 设置缩略图加载模式
     */
    public void setImageLoadMode(int mode) {
        prefs.edit().putInt(KEY_IMAGE_LOAD_MODE, mode).apply();
    }
    
    /**
     * 判断是否应该加载缩略图
     */
    public boolean shouldLoadThumbnail() {
        int mode = getImageLoadMode();
        
        switch (mode) {
            case IMAGE_LOAD_ALWAYS:
                return true;
            case IMAGE_LOAD_NEVER:
                return false;
            case IMAGE_LOAD_WIFI_ONLY:
                return isWifiConnected();
            default:
                return true;
        }
    }
    
    /**
     * 获取缩略图加载模式描述
     */
    public String getImageLoadModeDesc() {
        int mode = getImageLoadMode();
        switch (mode) {
            case IMAGE_LOAD_ALWAYS:
                return "始终加载";
            case IMAGE_LOAD_WIFI_ONLY:
                return "仅WiFi加载";
            case IMAGE_LOAD_NEVER:
                return "不加载";
            default:
                return "始终加载";
        }
    }
    
    // ==================== 深色模式设置 ====================
    
    /**
     * 获取深色模式设置
     */
    public int getDarkMode() {
        return prefs.getInt(KEY_DARK_MODE, DARK_MODE_FOLLOW_SYSTEM);
    }
    
    /**
     * 设置深色模式
     */
    public void setDarkMode(int mode) {
        prefs.edit().putInt(KEY_DARK_MODE, mode).apply();
    }
    
    /**
     * 获取深色模式描述
     */
    public String getDarkModeDesc() {
        int mode = getDarkMode();
        switch (mode) {
            case DARK_MODE_FOLLOW_SYSTEM:
                return "跟随系统";
            case DARK_MODE_LIGHT:
                return "浅色模式";
            case DARK_MODE_DARK:
                return "深色模式";
            default:
                return "跟随系统";
        }
    }
    
    // ==================== 字体大小设置 ====================
    
    /**
     * 获取字体大小设置
     */
    public int getFontSize() {
        return prefs.getInt(KEY_FONT_SIZE, FONT_SIZE_NORMAL);
    }
    
    /**
     * 设置字体大小
     */
    public void setFontSize(int size) {
        prefs.edit().putInt(KEY_FONT_SIZE, size).apply();
    }
    
    /**
     * 获取字体大小描述
     */
    public String getFontSizeDesc() {
        int size = getFontSize();
        switch (size) {
            case FONT_SIZE_SMALL:
                return "小";
            case FONT_SIZE_NORMAL:
                return "标准";
            case FONT_SIZE_LARGE:
                return "大";
            case FONT_SIZE_EXTRA_LARGE:
                return "特大";
            default:
                return "标准";
        }
    }
    
    /**
     * 获取字体缩放比例
     * @return 缩放比例 (0.85, 1.0, 1.15, 1.3)
     */
    public float getFontScale() {
        int size = getFontSize();
        switch (size) {
            case FONT_SIZE_SMALL:
                return 0.85f;
            case FONT_SIZE_NORMAL:
                return 1.0f;
            case FONT_SIZE_LARGE:
                return 1.15f;
            case FONT_SIZE_EXTRA_LARGE:
                return 1.3f;
            default:
                return 1.0f;
        }
    }
    
    /**
     * 根据字体设置缩放文字大小
     * @param originalSize 原始文字大小 (sp)
     * @return 缩放后的文字大小 (sp)
     */
    public float scaleTextSize(float originalSize) {
        return originalSize * getFontScale();
    }
    
    // ==================== 工具方法 ====================
    
    /**
     * 判断当前是否连接WiFi
     */
    private boolean isWifiConnected() {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        if (cm == null) return false;
        
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        return activeNetwork != null && activeNetwork.getType() == ConnectivityManager.TYPE_WIFI;
    }
}
