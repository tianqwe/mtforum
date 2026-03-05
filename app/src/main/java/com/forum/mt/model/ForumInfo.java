package com.forum.mt.model;

import java.util.List;

/**
 * 版块详情信息模型
 */
public class ForumInfo {
    private int fid;              // 版块ID
    private String name;          // 版块名称
    private String description;   // 版块描述
    private String icon;          // 版块图标URL
    private int todayPosts;       // 今日发帖数
    private int totalPosts;       // 总帖子数
    private int totalThreads;     // 总主题数
    private int followers;        // 关注数
    private int rank;             // 版块排名
    private List<String> moderators; // 版主列表
    private List<Post> topPosts;  // 置顶帖子列表
    private int currentPage;      // 当前页码
    private int totalPages;       // 总页数
    private List<ForumTab> tabs;  // 分类标签列表
    
    public ForumInfo() {}
    
    public ForumInfo(int fid, String name) {
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
    
    public int getTodayPosts() { return todayPosts; }
    public void setTodayPosts(int todayPosts) { this.todayPosts = todayPosts; }
    
    public int getTotalPosts() { return totalPosts; }
    public void setTotalPosts(int totalPosts) { this.totalPosts = totalPosts; }
    
    public int getTotalThreads() { return totalThreads; }
    public void setTotalThreads(int totalThreads) { this.totalThreads = totalThreads; }
    
    public int getFollowers() { return followers; }
    public void setFollowers(int followers) { this.followers = followers; }
    
    public int getRank() { return rank; }
    public void setRank(int rank) { this.rank = rank; }
    
    public List<String> getModerators() { return moderators; }
    public void setModerators(List<String> moderators) { this.moderators = moderators; }
    
    public List<Post> getTopPosts() { return topPosts; }
    public void setTopPosts(List<Post> topPosts) { this.topPosts = topPosts; }
    
    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
    
    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

    public List<ForumTab> getTabs() { return tabs; }
    public void setTabs(List<ForumTab> tabs) { this.tabs = tabs; }

    /**
     * 分类标签模型
     */
    public static class ForumTab {
        private String name;      // 标签名称，如"全部"、"最新"
        private String filter;    // 筛选类型，如"all"、"lastpost"
        private boolean selected; // 是否选中

        public ForumTab() {}

        public ForumTab(String name, String filter) {
            this.name = name;
            this.filter = filter;
        }

        public String getName() { return name; }
        public void setName(String name) { this.name = name; }

        public String getFilter() { return filter; }
        public void setFilter(String filter) { this.filter = filter; }

        public boolean isSelected() { return selected; }
        public void setSelected(boolean selected) { this.selected = selected; }
    }

    /**
     * 格式化显示统计信息
     */
    public String getStatsText() {
        return "今日 " + todayPosts + "  ·  帖子 " + formatNumber(totalPosts) + "  ·  关注 " + followers;
    }
    
    private String formatNumber(int num) {
        if (num >= 10000) {
            return String.format("%.1f万", num / 10000.0);
        }
        return String.valueOf(num);
    }
    
    /**
     * 是否有下一页
     */
    public boolean hasNextPage() {
        return currentPage < totalPages;
    }
    
    /**
     * 是否有上一页
     */
    public boolean hasPrevPage() {
        return currentPage > 1;
    }
}
