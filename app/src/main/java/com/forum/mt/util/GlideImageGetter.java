package com.forum.mt.util;

import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Spanned;
import android.util.DisplayMetrics;
import android.view.View;
import android.widget.TextView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.target.CustomTarget;
import com.bumptech.glide.request.transition.Transition;
import com.forum.mt.R;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.text.HtmlCompat;

import java.lang.ref.WeakReference;
import java.util.HashSet;
import java.util.Set;

/**
 * Glide图片加载器，用于Html.fromHtml()中的img标签
 * 支持表情符号等网络图片的异步加载
 * 表情图片会显示在文本的正确位置
 * 普通图片会显示较大尺寸
 */
public class GlideImageGetter implements Html.ImageGetter {

    private final WeakReference<TextView> textViewRef;
    private final int smileySize;      // 表情图片尺寸
    private final int normalImageWidth; // 普通图片宽度
    private final Set<ImageLoadTarget> activeTargets = new HashSet<>();

    public GlideImageGetter(TextView textView) {
        this.textViewRef = new WeakReference<>(textView);
        // 表情图片宽度根据文字大小设置，略大于文字以便清晰显示
        this.smileySize = (int) (textView.getTextSize() * 1.3f);
        
        // 普通图片宽度：屏幕宽度减去边距（左右各16dp）
        DisplayMetrics metrics = textView.getContext().getResources().getDisplayMetrics();
        int paddingDp = 32; // 左右边距总和
        int paddingPx = (int) (paddingDp * metrics.density);
        this.normalImageWidth = metrics.widthPixels - paddingPx;
    }

    @Override
    public Drawable getDrawable(String source) {
        if (source == null || source.isEmpty()) {
            return null;
        }

        // 判断是否是表情图片
        boolean isSmiley = isSmileyUrl(source);
        
        // 创建占位Drawable
        final UrlDrawable urlDrawable = new UrlDrawable();
        int placeholderSize = isSmiley ? smileySize : normalImageWidth;
        urlDrawable.setBounds(0, 0, placeholderSize, placeholderSize);

        // 处理相对路径
        String url = source;
        if (url.startsWith("//")) {
            url = "https:" + url;
        } else if (url.startsWith("/")) {
            url = "https://cdn-bbs.mt2.cn" + url;
        }

        // 使用Glide异步加载图片
        ImageLoadTarget target = new ImageLoadTarget(urlDrawable, isSmiley);
        activeTargets.add(target);
        
        TextView textView = textViewRef.get();
        if (textView != null) {
            Glide.with(textView.getContext().getApplicationContext())
                    .load(url)
                    .placeholder(R.drawable.ic_forum)
                    .error(R.drawable.ic_forum)
                    .into(target);
        }

        return urlDrawable;
    }
    
    /**
     * 判断URL是否是表情图片
     * 表情图片URL通常包含smiley或emoticon
     */
    private boolean isSmileyUrl(String url) {
        if (url == null) return false;
        String lowerUrl = url.toLowerCase();
        return lowerUrl.contains("smiley") || lowerUrl.contains("emoticon");
    }
    
    /**
     * 取消所有正在进行的图片加载
     */
    public void cancelLoads() {
        TextView textView = textViewRef.get();
        if (textView != null) {
            for (ImageLoadTarget target : activeTargets) {
                Glide.with(textView.getContext().getApplicationContext()).clear(target);
            }
        }
        activeTargets.clear();
    }

    /**
     * 图片加载Target
     */
    private class ImageLoadTarget extends CustomTarget<Drawable> {
        private final UrlDrawable urlDrawable;
        private final boolean isSmiley;

        ImageLoadTarget(UrlDrawable urlDrawable, boolean isSmiley) {
            this.urlDrawable = urlDrawable;
            this.isSmiley = isSmiley;
        }

        @Override
        public void onResourceReady(@NonNull Drawable resource, @Nullable Transition<? super Drawable> transition) {
            int width, height;
            
            if (isSmiley) {
                // 表情图片：使用小尺寸，保持宽高比
                width = smileySize;
                int intrinsicWidth = resource.getIntrinsicWidth();
                int intrinsicHeight = resource.getIntrinsicHeight();
                if (intrinsicWidth > 0 && intrinsicHeight > 0) {
                    float ratio = (float) intrinsicHeight / intrinsicWidth;
                    height = (int) (width * ratio);
                } else {
                    height = smileySize;
                }
            } else {
                // 普通图片：使用屏幕宽度，保持宽高比，最大高度不超过屏幕
                width = normalImageWidth;
                int intrinsicWidth = resource.getIntrinsicWidth();
                int intrinsicHeight = resource.getIntrinsicHeight();
                
                DisplayMetrics metrics = textViewRef.get() != null ? 
                    textViewRef.get().getContext().getResources().getDisplayMetrics() : null;
                int maxHeight = metrics != null ? metrics.heightPixels / 2 : 800; // 最大高度为屏幕一半
                
                if (intrinsicWidth > 0 && intrinsicHeight > 0) {
                    float ratio = (float) intrinsicHeight / intrinsicWidth;
                    height = (int) (width * ratio);
                    // 如果高度过大，按比例缩小
                    if (height > maxHeight) {
                        float scale = (float) maxHeight / height;
                        width = (int) (width * scale);
                        height = maxHeight;
                    }
                } else {
                    height = width; // 默认正方形
                }
            }
            
            resource.setBounds(0, 0, width, height);
            urlDrawable.setDrawable(resource);
            urlDrawable.setBounds(0, 0, width, height);
            
            // 通知TextView重绘（不需要重新设置文本）
            TextView textView = textViewRef.get();
            if (textView != null) {
                textView.post(() -> {
                    textView.invalidate();
                    textView.requestLayout();
                });
            }
            
            activeTargets.remove(this);
        }

        @Override
        public void onLoadCleared(@Nullable Drawable placeholder) {
            activeTargets.remove(this);
        }

        @Override
        public void onLoadFailed(@Nullable Drawable errorDrawable) {
            if (errorDrawable != null) {
                int size = isSmiley ? smileySize : normalImageWidth;
                errorDrawable.setBounds(0, 0, size, size);
                urlDrawable.setDrawable(errorDrawable);
                
                TextView textView = textViewRef.get();
                if (textView != null) {
                    textView.post(textView::invalidate);
                }
            }
            activeTargets.remove(this);
        }
    }

    /**
     * 用于承载异步加载图片的Drawable
     */
    private static class UrlDrawable extends Drawable {
        private Drawable drawable;

        @Override
        public void draw(@NonNull android.graphics.Canvas canvas) {
            if (drawable != null) {
                drawable.draw(canvas);
            }
        }

        @Override
        public void setAlpha(int alpha) {
            if (drawable != null) {
                drawable.setAlpha(alpha);
            }
        }

        @Override
        public void setColorFilter(@Nullable android.graphics.ColorFilter colorFilter) {
            if (drawable != null) {
                drawable.setColorFilter(colorFilter);
            }
        }

        @Override
        public int getOpacity() {
            if (drawable != null) {
                return drawable.getOpacity();
            }
            return android.graphics.PixelFormat.TRANSPARENT;
        }

        public void setDrawable(Drawable drawable) {
            this.drawable = drawable;
        }
    }
    
    /**
     * 便捷方法：设置带表情的HTML内容到TextView
     * 表情图片会显示在文本的正确位置
     */
    public static void setHtmlWithImages(TextView textView, String html) {
        if (html == null || html.isEmpty()) {
            textView.setText("");
            return;
        }
        
        GlideImageGetter imageGetter = new GlideImageGetter(textView);
        Spanned spanned = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT, imageGetter, null);
        textView.setText(spanned);
    }
}