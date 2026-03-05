package com.forum.mt.model;

import java.util.List;

/**
 * 帖子数据模型
 */
public class Post {
    private int tid;           // 帖子ID
    private String title;      // 标题
    private String content;    // 内容
    private String summary;    // 摘要/简介
    private String author;     // 作者名
    private int authorId;      // 作者ID
    private String authorAvatar; // 作者头像
    private String forumName;  // 所属版块
    private int forumId;       // 版块ID
    private long dateline;     // 发帖时间戳
    private String dateStr;    // 发帖时间字符串
    private int replies;       // 回复数
    private int views;         // 查看数
    private int likes;         // 点赞数
    private String thumbnail;  // 缩略图URL (第一张)
    private List<String> thumbnails; // 缩略图列表（多图）
    private String imageUrl;   // 图片key (用于获取图片)
    private int aid;           // 附件ID
    private boolean isTop;     // 是否置顶
    private boolean isDigest;  // 是否精华
    private boolean isHot;     // 是否热门
    private String lastPost;   // 最后回复时间
    private String lastPoster; // 最后回复者
    private boolean isFollowed; // 是否已关注作者
    private String authorLevel; // 作者等级 (如 "Lv.4")
    private int authorGender;   // 作者性别 (0未知, 1男, 2女)
    private String location;    // 发帖地点 (如 "广东")
    private String editTime;    // 最后编辑时间
    private int commentTotal;   // 评论总数
    private boolean canRate;    // 是否可以赞赏
    private int firstPid;       // 楼主帖子的pid（用于赞赏楼主）
    private List<LikeUser> likeUsers; // 点赞用户列表
    private boolean isLiked;    // 是否已点赞
    private boolean isFavorited; // 是否已收藏
    private int favId;           // 收藏ID（用于删除收藏）
    
    // 回复相关字段
    private String formhash;      // 表单验证hash（用于回复）
    private String noticeAuthor;  // 通知作者token（用于回复楼主）
    private String uploadHash;    // 图片上传hash（用于上传附件）
    private int uploadUid;        // 上传用户ID
    
    // 悬赏相关字段
    private String bountyType;   // 悬赏类型：悬赏、精华等
    private int bountyAmount;    // 悬赏数量（金币数）
    private String bountyText;   // 完整悬赏文本（如"5金币"）
    
    // 付费帖子相关字段
    private boolean isPaidPost;      // 是否是付费帖子
    private int paidPrice;           // 价格（金币数）
    private int paidBuyers;          // 购买人数
    private String paidDeadline;     // 购买截止日期（如"2026-3-11 15:55"）
    private boolean hasPurchased;    // 当前用户是否已购买
    
    // 我的回复相关字段
    private boolean isMyReply;       // 是否是"我的回复"列表中的项
    private String myReplyContent;   // 我的回复内容摘要
    
    // 踢帖相关字段
    private int kickPostCount;       // 当前踢帖人数
    private String kickPostMParam;   // 踢帖m参数（用于提交踢帖）
    private static final int KICK_POST_THRESHOLD = 5; // 踢帖阈值（5人隐藏帖子）
    
    public Post() {}
    
    public Post(int tid, String title) {
        this.tid = tid;
        this.title = title;
    }
    
    /**
     * 点赞用户信息
     */
    public static class LikeUser {
        private int uid;
        private String username;
        private String avatar;
        
        public LikeUser() {}
        
        public LikeUser(int uid, String username, String avatar) {
            this.uid = uid;
            this.username = username;
            this.avatar = avatar;
        }
        
        public int getUid() { return uid; }
        public void setUid(int uid) { this.uid = uid; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getAvatar() { return avatar; }
        public void setAvatar(String avatar) { this.avatar = avatar; }
    }
    
    // Getters and Setters
    public int getTid() { return tid; }
    public void setTid(int tid) { this.tid = tid; }
    
    public String getTitle() { return title; }
    public void setTitle(String title) { this.title = title; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getSummary() { return summary; }
    public void setSummary(String summary) { this.summary = summary; }
    
    public String getAuthor() { return author; }
    public void setAuthor(String author) { this.author = author; }
    
    public int getAuthorId() { return authorId; }
    public void setAuthorId(int authorId) { this.authorId = authorId; }
    
    public String getAuthorAvatar() { return authorAvatar; }
    public void setAuthorAvatar(String authorAvatar) { this.authorAvatar = authorAvatar; }
    
    public String getForumName() { return forumName; }
    public void setForumName(String forumName) { this.forumName = forumName; }
    
    public int getForumId() { return forumId; }
    public void setForumId(int forumId) { this.forumId = forumId; }
    
    public long getDateline() { return dateline; }
    public void setDateline(long dateline) { this.dateline = dateline; }
    
    public String getDateStr() { return dateStr; }
    public void setDateStr(String dateStr) { this.dateStr = dateStr; }
    
    public int getReplies() { return replies; }
    public void setReplies(int replies) { this.replies = replies; }
    
    public int getViews() { return views; }
    public void setViews(int views) { this.views = views; }
    
    public int getLikes() { return likes; }
    public void setLikes(int likes) { this.likes = likes; }
    
    public String getThumbnail() { return thumbnail; }
    public void setThumbnail(String thumbnail) { this.thumbnail = thumbnail; }
    
    public List<String> getThumbnails() { return thumbnails; }
    public void setThumbnails(List<String> thumbnails) { this.thumbnails = thumbnails; }
    
    public String getImageUrl() { return imageUrl; }
    public void setImageUrl(String imageUrl) { this.imageUrl = imageUrl; }
    
    public int getAid() { return aid; }
    public void setAid(int aid) { this.aid = aid; }
    
    public boolean isTop() { return isTop; }
    public void setTop(boolean top) { isTop = top; }
    
    public boolean isDigest() { return isDigest; }
    public void setDigest(boolean digest) { isDigest = digest; }
    
    public boolean isHot() { return isHot; }
    public void setHot(boolean hot) { isHot = hot; }
    
    public String getLastPost() { return lastPost; }
    public void setLastPost(String lastPost) { this.lastPost = lastPost; }
    
    public String getLastPoster() { return lastPoster; }
    public void setLastPoster(String lastPoster) { this.lastPoster = lastPoster; }
    
    public boolean isFollowed() { return isFollowed; }
    public void setFollowed(boolean followed) { isFollowed = followed; }
    
    public String getAuthorLevel() { return authorLevel; }
    public void setAuthorLevel(String level) { this.authorLevel = level; }
    
    public int getAuthorGender() { return authorGender; }
    public void setAuthorGender(int gender) { this.authorGender = gender; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public String getEditTime() { return editTime; }
    public void setEditTime(String editTime) { this.editTime = editTime; }
    
    public int getCommentTotal() { return commentTotal; }
    public void setCommentTotal(int commentTotal) { this.commentTotal = commentTotal; }
    
    public boolean isCanRate() { return canRate; }
    public void setCanRate(boolean canRate) { this.canRate = canRate; }
    
    public int getFirstPid() { return firstPid; }
    public void setFirstPid(int firstPid) { this.firstPid = firstPid; }
    
    public List<LikeUser> getLikeUsers() { return likeUsers; }
    public void setLikeUsers(List<LikeUser> likeUsers) { this.likeUsers = likeUsers; }
    
    public boolean isLiked() { return isLiked; }
    public void setLiked(boolean liked) { isLiked = liked; }
    
    public boolean isFavorited() { return isFavorited; }
    public void setFavorited(boolean favorited) { isFavorited = favorited; }
    
    public int getFavId() { return favId; }
    public void setFavId(int favId) { this.favId = favId; }
    
    // 回复相关
    public String getFormhash() { return formhash; }
    public void setFormhash(String formhash) { this.formhash = formhash; }
    
    public String getNoticeAuthor() { return noticeAuthor; }
    public void setNoticeAuthor(String noticeAuthor) { this.noticeAuthor = noticeAuthor; }
    
    public String getUploadHash() { return uploadHash; }
    public void setUploadHash(String uploadHash) { this.uploadHash = uploadHash; }
    
    public int getUploadUid() { return uploadUid; }
    public void setUploadUid(int uploadUid) { this.uploadUid = uploadUid; }
    
    // 悬赏相关
    public String getBountyType() { return bountyType; }
    public void setBountyType(String bountyType) { this.bountyType = bountyType; }
    
    public int getBountyAmount() { return bountyAmount; }
    public void setBountyAmount(int bountyAmount) { this.bountyAmount = bountyAmount; }
    
    public String getBountyText() { return bountyText; }
    public void setBountyText(String bountyText) { this.bountyText = bountyText; }
    
    // 付费帖子相关
    public boolean isPaidPost() { return isPaidPost; }
    public void setPaidPost(boolean paidPost) { isPaidPost = paidPost; }
    
    public int getPaidPrice() { return paidPrice; }
    public void setPaidPrice(int paidPrice) { this.paidPrice = paidPrice; }
    
    public int getPaidBuyers() { return paidBuyers; }
    public void setPaidBuyers(int paidBuyers) { this.paidBuyers = paidBuyers; }
    
    public String getPaidDeadline() { return paidDeadline; }
    public void setPaidDeadline(String paidDeadline) { this.paidDeadline = paidDeadline; }
    
    public boolean hasPurchased() { return hasPurchased; }
    public void setHasPurchased(boolean hasPurchased) { this.hasPurchased = hasPurchased; }
    
    // 我的回复相关
    public boolean isMyReply() { return isMyReply; }
    public void setMyReply(boolean myReply) { isMyReply = myReply; }
    
    public String getMyReplyContent() { return myReplyContent; }
    public void setMyReplyContent(String myReplyContent) { this.myReplyContent = myReplyContent; }
    
    // 踢帖相关
    public int getKickPostCount() { return kickPostCount; }
    public void setKickPostCount(int kickPostCount) { this.kickPostCount = kickPostCount; }
    
    public String getKickPostMParam() { return kickPostMParam; }
    public void setKickPostMParam(String kickPostMParam) { this.kickPostMParam = kickPostMParam; }
    
    public static int getKickPostThreshold() { return KICK_POST_THRESHOLD; }
    
    /**
     * 是否有悬赏
     */
    public boolean hasBounty() {
        return bountyType != null && !bountyType.isEmpty();
    }
    
    /**
     * 获取帖子详情URL
     */
    public String getPostUrl() {
        return "https://bbs.binmt.cc/thread-" + tid + "-1-1.html";
    }
    
    /**
     * 格式化显示回复/浏览数
     */
    public String getStatsText() {
        int r = replies;
        int v = views;
        return r + "回复 / " + formatCount(v) + "浏览";
    }
    
    private String formatCount(int count) {
        if (count >= 10000) {
            return String.format("%.1f万", count / 10000.0);
        } else if (count >= 1000) {
            return String.format("%.1fk", count / 1000.0);
        }
        return String.valueOf(count);
    }
}
