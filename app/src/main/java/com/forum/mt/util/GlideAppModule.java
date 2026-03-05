package com.forum.mt.util;

import android.content.Context;

import androidx.annotation.NonNull;

import com.bumptech.glide.GlideBuilder;
import com.bumptech.glide.annotation.GlideModule;
import com.bumptech.glide.load.engine.cache.InternalCacheDiskCacheFactory;
import com.bumptech.glide.load.engine.cache.LruResourceCache;
import com.bumptech.glide.module.AppGlideModule;

/**
 * Glide 全局优化配置
 */
@GlideModule
public class GlideAppModule extends AppGlideModule {
    
    @Override
    public void applyOptions(@NonNull Context context, @NonNull GlideBuilder builder) {
        // 内存缓存：使用可用内存的1/8
        int memoryCacheSizeBytes = (int) (Runtime.getRuntime().maxMemory() / 8);
        builder.setMemoryCache(new LruResourceCache(memoryCacheSizeBytes));
        
        // 磁盘缓存：256MB
        int diskCacheSizeBytes = 256 * 1024 * 1024;
        builder.setDiskCache(new InternalCacheDiskCacheFactory(context, diskCacheSizeBytes));
        
        // 日志级别：Release版本关闭日志
        builder.setLogLevel(android.util.Log.WARN);
    }
    
    @Override
    public boolean isManifestParsingEnabled() {
        // 禁用清单解析，提升性能
        return false;
    }
}
