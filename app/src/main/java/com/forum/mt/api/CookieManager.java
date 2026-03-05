package com.forum.mt.api;

import android.content.Context;
import android.content.SharedPreferences;
import android.text.TextUtils;

import okhttp3.Cookie;
import okhttp3.CookieJar;
import okhttp3.HttpUrl;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Cookie管理器 - 处理论坛登录状态
 */
public class CookieManager implements CookieJar {
    private static final String PREF_NAME = "mt_forum_cookies";
    private static final String KEY_COOKIES = "cookies";
    
    private final SharedPreferences prefs;
    private final Map<String, List<Cookie>> cookieStore = new HashMap<>();
    
    public CookieManager(Context context) {
        prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        loadCookies();
    }
    
    @Override
    public void saveFromResponse(HttpUrl url, List<Cookie> cookies) {
        String host = url.host();
        List<Cookie> existingCookies = cookieStore.get(host);
        if (existingCookies == null) {
            existingCookies = new ArrayList<>();
            cookieStore.put(host, existingCookies);
        }
        
        // 更新或添加cookie
        for (Cookie newCookie : cookies) {
            boolean updated = false;
            for (int i = 0; i < existingCookies.size(); i++) {
                if (existingCookies.get(i).name().equals(newCookie.name())) {
                    existingCookies.set(i, newCookie);
                    updated = true;
                    break;
                }
            }
            if (!updated) {
                existingCookies.add(newCookie);
            }
        }
        
        persistCookies();
    }
    
    @Override
    public List<Cookie> loadForRequest(HttpUrl url) {
        List<Cookie> cookies = cookieStore.get(url.host());
        return cookies != null ? cookies : new ArrayList<>();
    }
    
    /**
     * 保存Cookie到SharedPreferences
     */
    private void persistCookies() {
        Set<String> cookieStrings = new HashSet<>();
        for (List<Cookie> cookies : cookieStore.values()) {
            for (Cookie cookie : cookies) {
                cookieStrings.add(cookie.toString());
            }
        }
        prefs.edit().putStringSet(KEY_COOKIES, cookieStrings).apply();
    }
    
    /**
     * 从SharedPreferences加载Cookie
     */
    private void loadCookies() {
        Set<String> cookieStrings = prefs.getStringSet(KEY_COOKIES, new HashSet<>());
        
        for (String cookieStr : cookieStrings) {
            try {
                Cookie cookie = Cookie.parse(HttpUrl.get(ApiConfig.BASE_URL), cookieStr);
                if (cookie != null) {
                    String host = cookie.domain();
                    List<Cookie> cookies = cookieStore.get(host);
                    if (cookies == null) {
                        cookies = new ArrayList<>();
                        cookieStore.put(host, cookies);
                    }
                    cookies.add(cookie);
                }
            } catch (Exception ignored) {}
        }
    }
    
    /**
     * 获取Cookie字符串
     */
    public String getCookieString() {
        StringBuilder sb = new StringBuilder();
        for (List<Cookie> cookies : cookieStore.values()) {
            for (Cookie cookie : cookies) {
                if (sb.length() > 0) sb.append("; ");
                sb.append(cookie.name()).append("=").append(cookie.value());
            }
        }
        return sb.toString();
    }
    
    /**
     * 设置Cookie字符串 (从抓包获取)
     */
    public void setCookieString(String cookieString) {
        if (TextUtils.isEmpty(cookieString)) return;
        
        String host = "bbs.binmt.cc";
        List<Cookie> cookies = new ArrayList<>();
        
        String[] pairs = cookieString.split("; ");
        
        for (String pair : pairs) {
            String[] kv = pair.split("=", 2);
            if (kv.length == 2) {
                Cookie cookie = new Cookie.Builder()
                    .name(kv[0].trim())
                    .value(kv[1].trim())
                    .domain(host)
                    .path("/")
                    .build();
                cookies.add(cookie);
            }
        }
        
        cookieStore.put(host, cookies);
        persistCookies();
    }
    
    /**
     * 检查是否已登录
     */
    public boolean isLoggedIn() {
        String authCookie = getCookieValue(ApiConfig.COOKIE_PREFIX + "auth");
        return !TextUtils.isEmpty(authCookie);
    }
    
    /**
     * 获取指定Cookie值
     */
    public String getCookieValue(String name) {
        for (List<Cookie> cookies : cookieStore.values()) {
            for (Cookie cookie : cookies) {
                if (cookie.name().equals(name)) {
                    return cookie.value();
                }
            }
        }
        return null;
    }
    
    /**
     * 获取formhash
     */
    public String getFormhash() {
        return getCookieValue("formhash");
    }
    
    /**
     * 清除所有Cookie
     */
    public void clear() {
        cookieStore.clear();
        prefs.edit().clear().apply();
    }
    
    /**
     * 清除所有Cookie (别名方法)
     */
    public void clearCookies() {
        clear();
    }
}