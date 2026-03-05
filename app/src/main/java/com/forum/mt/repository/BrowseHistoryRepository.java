package com.forum.mt.repository;

import android.content.Context;
import android.content.SharedPreferences;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;
import com.forum.mt.model.BrowseHistory;
import com.forum.mt.model.Post;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

/**
 * 浏览历史仓库
 * 使用SharedPreferences + JSON存储，适配AndroidIDE环境
 * 遵循谷歌推荐的架构模式，作为数据源和UI层之间的抽象层
 */
public class BrowseHistoryRepository {
    
    private static final String PREF_NAME = "browse_history";
    private static final String KEY_HISTORY = "history_list";
    private static final int MAX_HISTORY_SIZE = 200; // 最多保存200条
    
    private final SharedPreferences prefs;
    private final Gson gson;
    private final MutableLiveData<List<BrowseHistory>> allHistoryLiveData;
    
    /**
     * 单例实例
     */
    private static volatile BrowseHistoryRepository INSTANCE;
    
    /**
     * 获取Repository单例实例
     * @param context 应用上下文
     * @return Repository实例
     */
    public static BrowseHistoryRepository getInstance(Context context) {
        if (INSTANCE == null) {
            synchronized (BrowseHistoryRepository.class) {
                if (INSTANCE == null) {
                    INSTANCE = new BrowseHistoryRepository(context.getApplicationContext());
                }
            }
        }
        return INSTANCE;
    }
    
    /**
     * 私有构造函数，使用单例模式
     * @param context 应用上下文
     */
    private BrowseHistoryRepository(Context context) {
        this.prefs = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
        this.gson = new Gson();
        this.allHistoryLiveData = new MutableLiveData<>();
        
        // 初始化时加载数据并设置到LiveData
        allHistoryLiveData.setValue(loadHistory());
    }
    
    /**
     * 从SharedPreferences加载浏览历史
     */
    private List<BrowseHistory> loadHistory() {
        String json = prefs.getString(KEY_HISTORY, "[]");
        Type type = new TypeToken<List<BrowseHistory>>() {}.getType();
        List<BrowseHistory> history = gson.fromJson(json, type);
        if (history == null) {
            history = new ArrayList<>();
        }
        // 按浏览时间降序排序
        Collections.sort(history, (h1, h2) -> Long.compare(h2.getViewTime(), h1.getViewTime()));
        return history;
    }
    
    /**
     * 保存浏览历史到SharedPreferences
     */
    private void saveHistory(List<BrowseHistory> history) {
        String json = gson.toJson(history);
        prefs.edit().putString(KEY_HISTORY, json).apply();
        // 更新LiveData
        allHistoryLiveData.postValue(history);
    }
    
    /**
     * 添加或更新浏览历史
     * @param post 帖子对象
     */
    public void addOrUpdateHistory(Post post) {
        List<BrowseHistory> history = loadHistory();
        
        // 查找是否已存在
        BrowseHistory existing = null;
        int existingIndex = -1;
        for (int i = 0; i < history.size(); i++) {
            if (history.get(i).getTid() == post.getTid()) {
                existing = history.get(i);
                existingIndex = i;
                break;
            }
        }
        
        if (existing != null) {
            // 更新现有记录
            existing.updateViewTime();
            existing.setTitle(post.getTitle());
            existing.setAuthor(post.getAuthor());
            existing.setAuthorAvatar(post.getAuthorAvatar());
            existing.setThumbnail(post.getThumbnail());
            existing.setForumName(post.getForumName());
            existing.setReplyCount(post.getReplies());
            existing.setViewCount(post.getViews());
            
            // 移到最前面
            history.remove(existingIndex);
            history.add(0, existing);
        } else {
            // 添加新记录
            BrowseHistory newHistory = BrowseHistory.fromPost(post);
            history.add(0, newHistory);
            
            // 限制数量
            if (history.size() > MAX_HISTORY_SIZE) {
                history = new ArrayList<>(history.subList(0, MAX_HISTORY_SIZE));
            }
        }
        
        saveHistory(history);
    }
    
    /**
     * 添加或更新浏览历史
     * @param history 浏览历史对象
     */
    public void addOrUpdateHistory(BrowseHistory history) {
        List<BrowseHistory> historyList = loadHistory();
        
        // 查找是否已存在
        BrowseHistory existing = null;
        int existingIndex = -1;
        for (int i = 0; i < historyList.size(); i++) {
            if (historyList.get(i).getTid() == history.getTid()) {
                existing = historyList.get(i);
                existingIndex = i;
                break;
            }
        }
        
        if (existing != null) {
            // 更新现有记录
            existing.updateViewTime();
            existing.setTitle(history.getTitle());
            existing.setAuthor(history.getAuthor());
            existing.setAuthorAvatar(history.getAuthorAvatar());
            existing.setThumbnail(history.getThumbnail());
            existing.setForumName(history.getForumName());
            existing.setReplyCount(history.getReplyCount());
            existing.setViewCount(history.getViewCount());
            
            // 移到最前面
            historyList.remove(existingIndex);
            historyList.add(0, existing);
        } else {
            // 添加新记录
            historyList.add(0, history);
            
            // 限制数量
            if (historyList.size() > MAX_HISTORY_SIZE) {
                historyList = new ArrayList<>(historyList.subList(0, MAX_HISTORY_SIZE));
            }
        }
        
        saveHistory(historyList);
    }
    
    /**
     * 删除指定的浏览历史
     * @param history 浏览历史对象
     */
    public void deleteHistory(BrowseHistory history) {
        List<BrowseHistory> historyList = loadHistory();
        historyList.removeIf(h -> h.getTid() == history.getTid());
        saveHistory(historyList);
    }
    
    /**
     * 根据ID删除浏览历史
     * @param id 浏览历史ID
     */
    public void deleteHistoryById(int id) {
        List<BrowseHistory> historyList = loadHistory();
        historyList.removeIf(h -> h.getId() == id);
        saveHistory(historyList);
    }
    
    /**
     * 根据帖子ID删除浏览历史
     * @param tid 帖子ID
     */
    public void deleteHistoryByTid(int tid) {
        List<BrowseHistory> historyList = loadHistory();
        historyList.removeIf(h -> h.getTid() == tid);
        saveHistory(historyList);
    }
    
    /**
     * 清空所有浏览历史
     */
    public void deleteAllHistory() {
        prefs.edit().clear().apply();
        allHistoryLiveData.postValue(new ArrayList<>());
    }
    
    /**
     * 清空指定时间之前的浏览历史
     * @param timestamp 时间戳（毫秒）
     */
    public void deleteOlderThan(long timestamp) {
        List<BrowseHistory> historyList = loadHistory();
        historyList.removeIf(h -> h.getViewTime() < timestamp);
        saveHistory(historyList);
    }
    
    /**
     * 根据ID获取浏览历史
     */
    public BrowseHistory getById(int id) {
        List<BrowseHistory> historyList = loadHistory();
        for (BrowseHistory h : historyList) {
            if (h.getId() == id) {
                return h;
            }
        }
        return null;
    }
    
    /**
     * 根据帖子ID获取浏览历史
     */
    public BrowseHistory getByTid(int tid) {
        List<BrowseHistory> historyList = loadHistory();
        for (BrowseHistory h : historyList) {
            if (h.getTid() == tid) {
                return h;
            }
        }
        return null;
    }
    
    /**
     * 获取所有浏览历史（LiveData）
     * @return LiveData包装的浏览历史列表
     */
    public LiveData<List<BrowseHistory>> getAllHistory() {
        return allHistoryLiveData;
    }
    
    /**
     * 获取所有浏览历史（普通List）
     * @return 浏览历史列表
     */
    public List<BrowseHistory> getAllHistorySync() {
        return loadHistory();
    }
    
    /**
     * 获取最近的浏览历史（LiveData）
     * @param limit 限制数量
     */
    public LiveData<List<BrowseHistory>> getRecentHistory(int limit) {
        List<BrowseHistory> historyList = loadHistory();
        MutableLiveData<List<BrowseHistory>> liveData = new MutableLiveData<>();
        
        if (historyList.size() > limit) {
            liveData.setValue(new ArrayList<>(historyList.subList(0, limit)));
        } else {
            liveData.setValue(new ArrayList<>(historyList));
        }
        
        return liveData;
    }
    
    /**
     * 获取最近的浏览历史（普通List）
     * @param limit 限制数量
     */
    public List<BrowseHistory> getRecentHistorySync(int limit) {
        List<BrowseHistory> historyList = loadHistory();
        if (historyList.size() > limit) {
            return new ArrayList<>(historyList.subList(0, limit));
        }
        return new ArrayList<>(historyList);
    }
    
    /**
     * 搜索浏览历史（LiveData）
     * @param keyword 搜索关键词
     */
    public LiveData<List<BrowseHistory>> searchHistory(String keyword) {
        MutableLiveData<List<BrowseHistory>> liveData = new MutableLiveData<>();
        List<BrowseHistory> historyList = loadHistory();
        
        List<BrowseHistory> result = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();
        
        for (BrowseHistory h : historyList) {
            boolean titleMatch = h.getTitle() != null && h.getTitle().toLowerCase().contains(lowerKeyword);
            boolean authorMatch = h.getAuthor() != null && h.getAuthor().toLowerCase().contains(lowerKeyword);
            if (titleMatch || authorMatch) {
                result.add(h);
            }
        }
        
        liveData.setValue(result);
        return liveData;
    }
    
    /**
     * 搜索浏览历史（普通List）
     * @param keyword 搜索关键词
     */
    public List<BrowseHistory> searchHistorySync(String keyword) {
        List<BrowseHistory> historyList = loadHistory();
        List<BrowseHistory> result = new ArrayList<>();
        String lowerKeyword = keyword.toLowerCase();
        
        for (BrowseHistory h : historyList) {
            boolean titleMatch = h.getTitle() != null && h.getTitle().toLowerCase().contains(lowerKeyword);
            boolean authorMatch = h.getAuthor() != null && h.getAuthor().toLowerCase().contains(lowerKeyword);
            if (titleMatch || authorMatch) {
                result.add(h);
            }
        }
        
        return result;
    }
    
    /**
     * 根据版块获取浏览历史（LiveData）
     * @param forumName 版块名称
     */
    public LiveData<List<BrowseHistory>> getHistoryByForum(String forumName) {
        MutableLiveData<List<BrowseHistory>> liveData = new MutableLiveData<>();
        List<BrowseHistory> historyList = loadHistory();
        
        List<BrowseHistory> result = new ArrayList<>();
        for (BrowseHistory h : historyList) {
            if (forumName.equals(h.getForumName())) {
                result.add(h);
            }
        }
        
        liveData.setValue(result);
        return liveData;
    }
    
    /**
     * 获取统计信息：总浏览历史数量
     */
    public int getCount() {
        return loadHistory().size();
    }
    
    /**
     * 获取统计信息：总浏览历史数量（LiveData）
     */
    public LiveData<Integer> getCountLiveData() {
        MutableLiveData<Integer> liveData = new MutableLiveData<>();
        liveData.setValue(loadHistory().size());
        return liveData;
    }
    
    /**
     * 检查帖子是否在浏览历史中
     * @param tid 帖子ID
     * @return true如果存在，false如果不存在
     */
    public boolean exists(int tid) {
        List<BrowseHistory> historyList = loadHistory();
        for (BrowseHistory h : historyList) {
            if (h.getTid() == tid) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 清理超过30天的浏览历史
     */
    public void cleanOldHistory() {
        long thirtyDaysAgo = System.currentTimeMillis() - (30L * 24 * 60 * 60 * 1000);
        deleteOlderThan(thirtyDaysAgo);
    }
}