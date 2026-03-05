package com.forum.mt.api;

import android.content.Context;

import okhttp3.Cache;
import okhttp3.ConnectionPool;
import okhttp3.OkHttpClient;
import okhttp3.logging.HttpLoggingInterceptor;
import retrofit2.Retrofit;
import retrofit2.converter.gson.GsonConverterFactory;
import retrofit2.converter.scalars.ScalarsConverterFactory;

import java.io.File;
import java.util.concurrent.TimeUnit;

/**
 * HTTP客户端单例
 */
public class HttpClient {
    private static HttpClient instance;
    private final OkHttpClient okHttpClient;
    private final Retrofit retrofit;
    private final CookieManager cookieManager;
    
    private HttpClient(Context context) {
        // Cookie管理
        cookieManager = new CookieManager(context);
        
        // 日志拦截器
        HttpLoggingInterceptor logging = new HttpLoggingInterceptor();
        logging.setLevel(HttpLoggingInterceptor.Level.BASIC);
        
        // 网络缓存目录 (50MB)
        File cacheDir = new File(context.getCacheDir(), "http_cache");
        Cache cache = new Cache(cacheDir, 50 * 1024 * 1024);
        
        // 连接池：最大空闲连接数10个，保持5分钟
        ConnectionPool connectionPool = new ConnectionPool(10, 5, TimeUnit.MINUTES);
        
        // OkHttp客户端
        okHttpClient = new OkHttpClient.Builder()
            .cookieJar(cookieManager)
            .cache(cache)
            .connectionPool(connectionPool)
            .addInterceptor(logging)
            .addInterceptor(chain -> {
                okhttp3.Request original = chain.request();
                okhttp3.Request.Builder builder = original.newBuilder()
                    .header("User-Agent", ApiConfig.USER_AGENT)
                    .header("Referer", ApiConfig.BASE_URL)
                    .header("Connection", "keep-alive");
                // 不手动设置Accept-Encoding，让OkHttp自动处理gzip
                return chain.proceed(builder.build());
            })
            .connectTimeout(ApiConfig.CONNECT_TIMEOUT, TimeUnit.SECONDS)
            .readTimeout(ApiConfig.READ_TIMEOUT, TimeUnit.SECONDS)
            .writeTimeout(ApiConfig.WRITE_TIMEOUT, TimeUnit.SECONDS)
            .followRedirects(true)
            .followSslRedirects(true)
            // 重试机制
            .retryOnConnectionFailure(true)
            .build();
        
        // 配置 Dispatcher 并发请求数
        okHttpClient.dispatcher().setMaxRequests(128);
        okHttpClient.dispatcher().setMaxRequestsPerHost(10);
        
        // Retrofit
        retrofit = new Retrofit.Builder()
            .baseUrl(ApiConfig.BASE_URL)
            .client(okHttpClient)
            .addConverterFactory(ScalarsConverterFactory.create())
            .addConverterFactory(GsonConverterFactory.create())
            .build();
    }
    
    public static synchronized HttpClient getInstance(Context context) {
        if (instance == null) {
            instance = new HttpClient(context.getApplicationContext());
        }
        return instance;
    }
    
    public OkHttpClient getOkHttpClient() {
        return okHttpClient;
    }
    
    public Retrofit getRetrofit() {
        return retrofit;
    }
    
    public CookieManager getCookieManager() {
        return cookieManager;
    }
    
    public <T> T createService(Class<T> serviceClass) {
        return retrofit.create(serviceClass);
    }
    
    /**
     * 设置Cookie (用于导入抓包获取的Cookie)
     */
    public void setCookies(String cookieString) {
        cookieManager.setCookieString(cookieString);
    }
    
    /**
     * 检查是否已登录
     */
    public boolean isLoggedIn() {
        return cookieManager.isLoggedIn();
    }
    
    /**
     * 清除所有Cookie (退出登录)
     */
    public void clearCookies() {
        cookieManager.clearCookies();
    }
}
