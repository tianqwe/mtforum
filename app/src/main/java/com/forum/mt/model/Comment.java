package com.forum.mt.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 评论模型
 */
public class Comment {
    private int id;
    private int pid; // 帖子ID
    private int floor; // 楼层
    private String author;
    private int authorId;
    private String authorAvatar;
    private String authorLevel;
    private String content; // 原始HTML内容
    private List<ContentBlock> contentBlocks; // 解析后的内容块列表
    private String dateStr;
    private String location;
    private int likes;
    private boolean isLiked;
    private boolean isAuthor; // 是否楼主
    private int replyCount; // 回复数
    private List<Comment> replies; // 回复列表
    private String floorLabel; // 楼层标签 (沙发、椅子、板凳等)
    private boolean canRate; // 是否可以赞赏
    
    // 引用回复相关
    private String quoteAuthor; // 被引用的作者名
    private String quoteContent; // 被引用的内容
    
    // 楼层回复相关
    private String noticeAuthor; // 通知作者token (用于回复该评论)
    private long dateline; // 发帖时间戳 (用于生成引用)
    private String rawDateline; // 原始时间字符串 (格式: 1738394450 发表于 2026-2-26 12:10)
    
    // 举报相关
    private int fid; // 版块ID (用于举报功能)
    
    public Comment() {
    }
    
    public int getId() {
        return id;
    }
    
    public void setId(int id) {
        this.id = id;
    }
    
    public int getPid() {
        return pid;
    }
    
    public void setPid(int pid) {
        this.pid = pid;
    }
    
    public int getFloor() {
        return floor;
    }
    
    public void setFloor(int floor) {
        this.floor = floor;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public int getAuthorId() {
        return authorId;
    }
    
    public void setAuthorId(int authorId) {
        this.authorId = authorId;
    }
    
    public String getAuthorAvatar() {
        return authorAvatar;
    }
    
    public void setAuthorAvatar(String authorAvatar) {
        this.authorAvatar = authorAvatar;
    }
    
    public String getAuthorLevel() {
        return authorLevel;
    }
    
    public void setAuthorLevel(String authorLevel) {
        this.authorLevel = authorLevel;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public List<ContentBlock> getContentBlocks() {
        return contentBlocks;
    }
    
    public void setContentBlocks(List<ContentBlock> contentBlocks) {
        this.contentBlocks = contentBlocks;
    }
    
    /**
     * 获取用于显示的内容块列表
     * 如果contentBlocks为空，则从content解析
     */
    public List<ContentBlock> getDisplayContentBlocks() {
        if (contentBlocks != null && !contentBlocks.isEmpty()) {
            return contentBlocks;
        }
        if (content != null && !content.isEmpty()) {
            return com.forum.mt.util.ContentParser.parse(content);
        }
        return new ArrayList<>();
    }
    
    public String getDateStr() {
        return dateStr;
    }
    
    public void setDateStr(String dateStr) {
        this.dateStr = dateStr;
    }
    
    public String getLocation() {
        return location;
    }
    
    public void setLocation(String location) {
        this.location = location;
    }
    
    public int getLikes() {
        return likes;
    }
    
    public void setLikes(int likes) {
        this.likes = likes;
    }
    
    public boolean isLiked() {
        return isLiked;
    }
    
    public void setLiked(boolean liked) {
        isLiked = liked;
    }
    
    public boolean isAuthor() {
        return isAuthor;
    }
    
    public void setAuthor(boolean author) {
        isAuthor = author;
    }
    
    public int getReplyCount() {
        return replyCount;
    }
    
    public void setReplyCount(int replyCount) {
        this.replyCount = replyCount;
    }
    
    public List<Comment> getReplies() {
        return replies;
    }
    
    public void setReplies(List<Comment> replies) {
        this.replies = replies;
    }
    
    public String getFloorLabel() {
        return floorLabel;
    }
    
    public void setFloorLabel(String floorLabel) {
        this.floorLabel = floorLabel;
    }
    
    public boolean isCanRate() {
        return canRate;
    }
    
    public void setCanRate(boolean canRate) {
        this.canRate = canRate;
    }
    
    public String getQuoteAuthor() {
        return quoteAuthor;
    }
    
    public void setQuoteAuthor(String quoteAuthor) {
        this.quoteAuthor = quoteAuthor;
    }
    
    public String getQuoteContent() {
        return quoteContent;
    }
    
    public void setQuoteContent(String quoteContent) {
        this.quoteContent = quoteContent;
    }
    
    public String getNoticeAuthor() {
        return noticeAuthor;
    }
    
    public void setNoticeAuthor(String noticeAuthor) {
        this.noticeAuthor = noticeAuthor;
    }
    
    public long getDateline() {
        return dateline;
    }
    
    public void setDateline(long dateline) {
        this.dateline = dateline;
    }
    
    public String getRawDateline() {
        return rawDateline;
    }
    
    public void setRawDateline(String rawDateline) {
        this.rawDateline = rawDateline;
    }
    
    public int getFid() {
        return fid;
    }
    
    public void setFid(int fid) {
        this.fid = fid;
    }
    
    /**
     * 根据楼层获取楼层标签
     */
    public static String getFloorLabel(int floor) {
        switch (floor) {
            case 1: return "沙发";
            case 2: return "椅子";
            case 3: return "板凳";
            default: return "#" + floor;
        }
    }
}
