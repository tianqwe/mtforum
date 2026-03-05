package com.forum.mt.model;

import java.util.List;

/**
 * 用户搜索结果 - 包含分页信息
 */
public class SearchUserResult {
    private List<User> users;      // 用户列表
    private String searchId;       // 搜索ID（用于分页）
    private int currentPage;       // 当前页码
    private int totalPages;        // 总页数

    public SearchUserResult() {}

    public SearchUserResult(List<User> users, String searchId, int currentPage, int totalPages) {
        this.users = users;
        this.searchId = searchId;
        this.currentPage = currentPage;
        this.totalPages = totalPages;
    }

    // Getters and Setters
    public List<User> getUsers() { return users; }
    public void setUsers(List<User> users) { this.users = users; }

    public String getSearchId() { return searchId; }
    public void setSearchId(String searchId) { this.searchId = searchId; }

    public int getCurrentPage() { return currentPage; }
    public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }

    public int getTotalPages() { return totalPages; }
    public void setTotalPages(int totalPages) { this.totalPages = totalPages; }

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