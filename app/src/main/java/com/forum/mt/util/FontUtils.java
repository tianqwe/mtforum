package com.forum.mt.util;

import android.content.Context;
import android.widget.TextView;

/**
 * 字体工具类
 * 用于根据设置调整文本视图的字体大小
 */
public class FontUtils {
    
    /**
     * 应用字体大小设置到TextView
     * @param context 上下文
     * @param textView 文本视图
     * @param originalSizeSp 原始字体大小 (sp)
     */
    public static void applyFontSize(Context context, TextView textView, float originalSizeSp) {
        if (context == null || textView == null) return;
        
        float scaledSize = AppSettings.getInstance(context).scaleTextSize(originalSizeSp);
        textView.setTextSize(scaledSize);
    }
    
    /**
     * 获取缩放后的字体大小
     * @param context 上下文
     * @param originalSizeSp 原始字体大小 (sp)
     * @return 缩放后的字体大小 (sp)
     */
    public static float getScaledSize(Context context, float originalSizeSp) {
        if (context == null) return originalSizeSp;
        return AppSettings.getInstance(context).scaleTextSize(originalSizeSp);
    }
    
    /**
     * 字体大小常量 - 内容正文
     */
    public static final float SIZE_CONTENT = 15f;      // 正文内容
    public static final float SIZE_TITLE = 17f;        // 标题
    public static final float SIZE_SUBTITLE = 14f;     // 副标题/描述
    public static final float SIZE_SMALL = 12f;        // 小字
    public static final float SIZE_TINY = 10f;         // 极小字
    public static final float SIZE_LARGE = 18f;        // 大字
    public static final float SIZE_USERNAME = 15f;     // 用户名
    public static final float SIZE_TIME = 12f;         // 时间
    public static final float SIZE_FLOOR = 12f;        // 楼层
}
