package com.forum.mt.viewmodel;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.forum.mt.model.BrowseHistory;
import com.forum.mt.model.Post;
import com.forum.mt.repository.BrowseHistoryRepository;

import java.util.List;

/**
 * 浏览历史ViewModel
 * 遵循谷歌推荐的架构模式，负责管理浏览历史的UI数据
 * 提供LiveData供UI观察，自动处理数据变化通知
 */
public class BrowseHistoryViewModel extends AndroidViewModel {
    
    private final BrowseHistoryRepository repository;
    
    /**
     * 所有浏览历史的LiveData
     */
    private final LiveData<List<BrowseHistory>> allHistory;
    
    /**
     * 最近浏览历史的LiveData
     */
    private final LiveData<List<BrowseHistory>> recentHistory;
    
    /**
     * 浏览历史总数的LiveData
     */
    private final LiveData<Integer> historyCount;
    
    /**
     * 搜索结果的LiveData
     */
    private final MutableLiveData<List<BrowseHistory>> searchResults;
    
    /**
     * 删除状态LiveData
     */
    private final MutableLiveData<Boolean> deleteSuccess;
    
    /**
     * 清空状态LiveData
     */
    private final MutableLiveData<Boolean> clearSuccess;
    
    /**
     * 消息LiveData（用于显示Toast等）
     */
    private final MutableLiveData<String> message;
    
    /**
     * 构造函数
     * @param application 应用实例
     */
    public BrowseHistoryViewModel(@NonNull Application application) {
        super(application);
        this.repository = BrowseHistoryRepository.getInstance(application);
        this.allHistory = repository.getAllHistory();
        this.recentHistory = repository.getRecentHistory(50); // 默认获取最近50条
        this.historyCount = repository.getCountLiveData();
        this.searchResults = new MutableLiveData<>();
        this.deleteSuccess = new MutableLiveData<>();
        this.clearSuccess = new MutableLiveData<>();
        this.message = new MutableLiveData<>();
    }
    
    /**
     * 获取所有浏览历史
     * @return LiveData包装的浏览历史列表
     */
    public LiveData<List<BrowseHistory>> getAllHistory() {
        return allHistory;
    }
    
    /**
     * 获取最近浏览历史
     * @return LiveData包装的浏览历史列表
     */
    public LiveData<List<BrowseHistory>> getRecentHistory() {
        return recentHistory;
    }
    
    /**
     * 获取浏览历史总数
     * @return LiveData包装的总数
     */
    public LiveData<Integer> getHistoryCount() {
        return historyCount;
    }
    
    /**
     * 获取搜索结果
     * @return LiveData包装的搜索结果列表
     */
    public LiveData<List<BrowseHistory>> getSearchResults() {
        return searchResults;
    }
    
    /**
     * 获取删除状态
     * @return LiveData包装的删除状态
     */
    public LiveData<Boolean> getDeleteSuccess() {
        return deleteSuccess;
    }
    
    /**
     * 获取清空状态
     * @return LiveData包装的清空状态
     */
    public LiveData<Boolean> getClearSuccess() {
        return clearSuccess;
    }
    
    /**
     * 获取消息
     * @return LiveData包装的消息
     */
    public LiveData<String> getMessage() {
        return message;
    }
    
    /**
     * 添加或更新浏览历史
     * @param post 帖子对象
     */
    public void addOrUpdateHistory(Post post) {
        repository.addOrUpdateHistory(post);
    }
    
    /**
     * 添加或更新浏览历史
     * @param history 浏览历史对象
     */
    public void addOrUpdateHistory(BrowseHistory history) {
        repository.addOrUpdateHistory(history);
    }
    
    /**
     * 删除指定的浏览历史
     * @param history 浏览历史对象
     */
    public void deleteHistory(BrowseHistory history) {
        repository.deleteHistory(history);
        deleteSuccess.setValue(true);
        message.setValue("已删除浏览记录");
    }
    
    /**
     * 根据ID删除浏览历史
     * @param id 浏览历史ID
     */
    public void deleteHistoryById(int id) {
        repository.deleteHistoryById(id);
        deleteSuccess.setValue(true);
        message.setValue("已删除浏览记录");
    }
    
    /**
     * 根据帖子ID删除浏览历史
     * @param tid 帖子ID
     */
    public void deleteHistoryByTid(int tid) {
        repository.deleteHistoryByTid(tid);
        deleteSuccess.setValue(true);
        message.setValue("已删除浏览记录");
    }
    
    /**
     * 清空所有浏览历史
     */
    public void clearAllHistory() {
        repository.deleteAllHistory();
        clearSuccess.setValue(true);
        message.setValue("已清空所有浏览记录");
    }
    
    /**
     * 清理超过30天的浏览历史
     */
    public void cleanOldHistory() {
        repository.cleanOldHistory();
        message.setValue("已清理旧浏览记录");
    }
    
    /**
     * 搜索浏览历史
     * @param keyword 搜索关键词
     */
    public void searchHistory(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            // 关键词为空时，显示所有历史
            searchResults.setValue(repository.getAllHistorySync());
        } else {
            // 执行搜索
            List<BrowseHistory> results = repository.searchHistorySync(keyword);
            searchResults.setValue(results);
            
            if (results.isEmpty()) {
                message.setValue("未找到相关记录");
            } else {
                message.setValue("找到 " + results.size() + " 条记录");
            }
        }
    }
    
    /**
     * 根据版块筛选浏览历史
     * @param forumName 版块名称
     * @return LiveData包装的浏览历史列表
     */
    public LiveData<List<BrowseHistory>> getHistoryByForum(String forumName) {
        return repository.getHistoryByForum(forumName);
    }
    
    /**
     * 检查帖子是否在浏览历史中
     * @param tid 帖子ID
     * @return true如果存在，false如果不存在
     */
    public boolean checkHistoryExists(int tid) {
        return repository.exists(tid);
    }
    
    /**
     * 批量删除浏览历史
     * @param histories 要删除的浏览历史列表
     */
    public void deleteMultipleHistory(List<BrowseHistory> histories) {
        if (histories == null || histories.isEmpty()) {
            message.setValue("请选择要删除的记录");
            return;
        }
        
        for (BrowseHistory history : histories) {
            repository.deleteHistory(history);
        }
        deleteSuccess.setValue(true);
        message.setValue("已删除 " + histories.size() + " 条记录");
    }
    
    /**
     * 获取指定数量的最近浏览历史
     * @param limit 限制数量
     * @return 浏览历史列表
     */
    public List<BrowseHistory> getRecentHistorySync(int limit) {
        return repository.getRecentHistorySync(limit);
    }
    
    /**
     * 重置删除状态
     */
    public void resetDeleteSuccess() {
        deleteSuccess.setValue(false);
    }
    
    /**
     * 重置清空状态
     */
    public void resetClearSuccess() {
        clearSuccess.setValue(false);
    }
    
    /**
     * 清除消息
     */
    public void clearMessage() {
        message.setValue(null);
    }
    
    @Override
    protected void onCleared() {
        super.onCleared();
        // ViewModel销毁时的清理工作
        // Repository是单例，不需要关闭
    }
}