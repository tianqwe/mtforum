package com.forum.mt.model;

import androidx.annotation.NonNull;

/**
 * 浏览历史实体
 * 使用SharedPreferences + JSON持久化存储
 * 遵循谷歌推荐的现代Android架构
 */
public class BrowseHistory {
    
    private int id;
    
    /**
     * 帖子ID
     * 设置唯一索引，避免同一帖子重复记录
     */
    private int tid;
    
    /**
     * 帖子标题
     */
    private String title;
    
    /**
     * 作者名
     */
    private String author;
    
    /**
     * 作者ID
     */
    private int authorId;
    
    /**
     * 作者头像URL
     */
    private String authorAvatar;
    
    /**
     * 缩略图URL
     */
    private String thumbnail;
    
    /**
     * 版块名称
     */
    private String forumName;
    
    /**
     * 浏览时间（时间戳，毫秒）
     */
    private long viewTime;
    
    /**
     * 回复数
     */
    private int replyCount;
    
    /**
     * 浏览数
     */
    private int viewCount;
    
    /**
     * 最后阅读位置（用于记住阅读进度）
     * 可选字段，未来扩展
     */
    private int lastReadPosition;
    
    /**
     * 构造函数
     */
    public BrowseHistory() {}
    
    /**
     * 带参数的构造函数
     */
    public BrowseHistory(int tid, String title, String author, String authorAvatar, 
                         String thumbnail, String forumName, long viewTime, 
                         int replyCount, int viewCount) {
        this.tid = tid;
        this.title = title;
        this.author = author;
        this.authorAvatar = authorAvatar;
        this.thumbnail = thumbnail;
        this.forumName = forumName;
        this.viewTime = viewTime;
        this.replyCount = replyCount;
        this.viewCount = viewCount;
        this.lastReadPosition = 0;
    }
    
    /**
     * 从Post对象转换为BrowseHistory对象
     * 便捷方法，方便记录浏览历史
     */
    public static BrowseHistory fromPost(Post post) {
        BrowseHistory history = new BrowseHistory();
        history.setTid(post.getTid());
        history.setTitle(post.getTitle());
        history.setAuthor(post.getAuthor());
        history.setAuthorId(post.getAuthorId());
        history.setAuthorAvatar(post.getAuthorAvatar());
        history.setThumbnail(post.getThumbnail());
        history.setForumName(post.getForumName());
        history.setViewTime(System.currentTimeMillis());
        history.setReplyCount(post.getReplies());
        history.setViewCount(post.getViews());
        history.setLastReadPosition(0);
        return history;
    }
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public int getTid() { return tid; }
    public void setTid(int tid) { this.tid = tid; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    
    public int getAuthorId() { return authorId; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }
    
    public String getAuthorAvatar() { return authorAvatar; }
    public void setAuthorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; }
    
    public String getThumbnail() { return thumbnail; }
    public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }
    
    public String getForumName() { return forumName; }
    public void setForumName(String forumName) { this.forumName = forumName; }
    
    public long getViewTime() { return viewTime; }
    public void setViewTime(long viewTime) { this.viewTime = viewTime; }
    
    public int getReplyCount() { return replyCount; }
    public void setReplyCount(int replyCount) { this.replyCount = replyCount; }
    
    public int getViewCount() { return viewCount; }
    public void setViewCount(int viewCount) { this.viewCount = viewCount; }
    
    public int getLastReadPosition() { return lastReadPosition; }
    public void setLastReadPosition(int lastReadPosition) { this.lastReadPosition = lastReadPosition; }
    
    /**
     * 更新浏览时间（重复访问时使用）
     */
    public void updateViewTime() {
        this.viewTime = System.currentTimeMillis();
    }
    
    /**
     * 格式化显示时间（如"2小时前"）
     * 可选的UI展示方法
     */
    public String getFormattedTime() {
        long now = System.currentTimeMillis();
        long diff = now - viewTime;
        
        long seconds = diff / 1000;
        long minutes = seconds / 60;
        long hours = minutes / 60;
        long days = hours / 24;
        
        if (days > 0) {
            return days + "天前";
        } else if (hours > 0) {
            return hours + "小时前";
        } else if (minutes > 0) {
            return minutes + "分钟前";
        } else {
            return "刚刚";
        }
    }
    
    /**
     * 获取统计文本（回复数/浏览数）
     */
    public String getStatsText() {
        return replyCount + "回复 / " + formatCount(viewCount) + "浏览";
    }
    
    private String formatCount(int count) {
        if (count >= 10000) {
            return String.format("%.1f万", count / 10000.0);
        } else if (count >= 1000) {
            return String.format("%.1fk", count / 1000.0);
        }
        return String.valueOf(count);
    }
    
    @NonNull
    @Override
    public String toString() {
        return "BrowseHistory{" +
                "id=" + id +
                ", tid=" + tid +
                ", title='" + title + '\'' +
                ", author='" + author + '\'' +
                ", viewTime=" + viewTime +
                '}';
    }
}