package com.forum.mt.model;

/**
 * 版块数据模型
 */
public class Forum {
    private int fid;           // 版块ID
    private String name;       // 版块名称
    private String description; // 版块描述
    private String icon;       // 版块图标
    private String url;        // 版块URL
    private int threads;       // 主题数
    private int posts;         // 帖子数
    private int todayPosts;    // 今日发帖
    private int lastPostTid;   // 最后帖子ID
    private String lastPostTitle; // 最后帖子标题
    private String lastPostAuthor; // 最后发帖者
    private long lastPostTime; // 最后发帖时间
    
    // 分类相关字段
    private int categoryId;       // 分类ID (如 1=MT专区, 36=交流与讨论, 45=论坛事务)
    private String categoryName;  // 分类名称
    
    // 用于列表显示的类型标识
    private int itemType = TYPE_FORUM;  // 默认为板块类型
    public static final int TYPE_CATEGORY = 0;  // 分类标题
    public static final int TYPE_FORUM = 1;     // 板块项
    
    public Forum() {}
    
    public Forum(int fid, String name) {
        this.fid = fid;
        this.name = name;
    }
    
    // Getters and Setters
    public int getFid() { return fid; }
    public void setFid(int fid) { this.fid = fid; }
    
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    
    public String getIcon() { return icon; }
    public void setIcon(String icon) { this.icon = icon; }
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public int getThreads() { return threads; }
    public void setThreads(int threads) { this.threads = threads; }
    
    public int getPosts() { return posts; }
    public void setPosts(int posts) { this.posts = posts; }
    
    public int getTodayPosts() { return todayPosts; }
    public void setTodayPosts(int todayPosts) { this.todayPosts = todayPosts; }
    
    public int getLastPostTid() { return lastPostTid; }
    public void setLastPostTid(int lastPostTid) { this.lastPostTid = lastPostTid; }
    
    public String getLastPostTitle() { return lastPostTitle; }
    public void setLastPostTitle(String lastPostTitle) { this.lastPostTitle = lastPostTitle; }
    
    public String getLastPostAuthor() { return lastPostAuthor; }
    public void setLastPostAuthor(String lastPostAuthor) { this.lastPostAuthor = lastPostAuthor; }
    
    public long getLastPostTime() { return lastPostTime; }
    public void setLastPostTime(long lastPostTime) { this.lastPostTime = lastPostTime; }
    
    public int getCategoryId() { return categoryId; }
    public void setCategoryId(int categoryId) { this.categoryId = categoryId; }
    
    public String getCategoryName() { return categoryName; }
    public void setCategoryName(String categoryName) { this.categoryName = categoryName; }
    
    public int getItemType() { return itemType; }
    public void setItemType(int itemType) { this.itemType = itemType; }
    
    /**
     * 判断是否为分类标题项
     */
    public boolean isCategory() {
        return itemType == TYPE_CATEGORY;
    }
    
    /**
     * 创建分类标题项
     */
    public static Forum createCategory(int categoryId, String categoryName) {
        Forum forum = new Forum();
        forum.setCategoryId(categoryId);
        forum.setCategoryName(categoryName);
        forum.setItemType(TYPE_CATEGORY);
        return forum;
    }
    
    /**
     * 获取版块URL
     */
    public String getForumUrl() {
        return "https://bbs.binmt.cc/forum.php?mod=forumdisplay&fid=" + fid;
    }
}
