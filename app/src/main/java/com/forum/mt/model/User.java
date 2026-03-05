package com.forum.mt.model;

/**
 * 用户数据模型
 */
public class User {
    private int uid;              // 用户ID
    private String username;      // 用户名
    private String avatar;        // 头像URL
    private String signature;     // 个性签名
    private int posts;            // 发帖数
    private int threads;          // 主题数
    private int credits;          // 积分
    private String groupId;       // 用户组ID
    private String lastVisit;     // 最后访问时间
    
    // 新增字段
    private String level;         // 用户等级 (如 "Lv.4")
    private String groupName;     // 用户组名称 (如 "高中生")
    private int following;        // 关注数
    private int followers;        // 粉丝数
    private int friends;          // 好友数
    private String email;         // 邮箱
    private String gender;        // 性别
    private String birthday;      // 生日
    private String location;      // 所在地
    private String regDate;       // 注册时间
    private int digestPosts;      // 精华帖数
    private String formhash;      // 表单哈希（用于提交操作）
    private int goldCoin;         // 金币
    private int popularity;       // 人气
    private int reputation;       // 信誉
    private int goodRate;         // 好评
    private int replies;          // 回复数
    private String onlineTime;    // 在线时间
    private boolean followed;     // 是否已关注
    private String coverImage;    // 空间背景封面图片URL
    
    public User() {}
    
    public User(int uid, String username) {
        this.uid = uid;
        this.username = username;
    }
    
    // Getters and Setters
    public int getUid() { return uid; }
    public void setUid(int uid) { this.uid = uid; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    
    public String getSignature() { return signature; }
    public void setSignature(String signature) { this.signature = signature; }
    
    public int getPosts() { return posts; }
    public void setPosts(int posts) { this.posts = posts; }
    
    public int getThreads() { return threads; }
    public void setThreads(int threads) { this.threads = threads; }
    
    public int getCredits() { return credits; }
    public void setCredits(int credits) { this.credits = credits; }
    
    public String getGroupId() { return groupId; }
    public void setGroupId(String groupId) { this.groupId = groupId; }
    
    public String getLastVisit() { return lastVisit; }
    public void setLastVisit(String lastVisit) { this.lastVisit = lastVisit; }
    
    // 新增字段的Getters和Setters
    public String getLevel() { return level; }
    public void setLevel(String level) { this.level = level; }
    
    public String getGroupName() { return groupName; }
    public void setGroupName(String groupName) { this.groupName = groupName; }
    
    public int getFollowing() { return following; }
    public void setFollowing(int following) { this.following = following; }
    
    public int getFollowers() { return followers; }
    public void setFollowers(int followers) { this.followers = followers; }
    
    public int getFriends() { return friends; }
    public void setFriends(int friends) { this.friends = friends; }
    
    public String getEmail() { return email; }
    public void setEmail(String email) { this.email = email; }
    
    public String getGender() { return gender; }
    public void setGender(String gender) { this.gender = gender; }
    
    public String getBirthday() { return birthday; }
    public void setBirthday(String birthday) { this.birthday = birthday; }
    
    public String getLocation() { return location; }
    public void setLocation(String location) { this.location = location; }
    
    public String getRegDate() { return regDate; }
    public void setRegDate(String regDate) { this.regDate = regDate; }
    
    public int getDigestPosts() { return digestPosts; }
    public void setDigestPosts(int digestPosts) { this.digestPosts = digestPosts; }
    
    public String getFormhash() { return formhash; }
    public void setFormhash(String formhash) { this.formhash = formhash; }

    public int getGoldCoin() { return goldCoin; }
    public void setGoldCoin(int goldCoin) { this.goldCoin = goldCoin; }
    
    public int getPopularity() { return popularity; }
    public void setPopularity(int popularity) { this.popularity = popularity; }
    
    public int getReputation() { return reputation; }
    public void setReputation(int reputation) { this.reputation = reputation; }
    
    public int getGoodRate() { return goodRate; }
    public void setGoodRate(int goodRate) { this.goodRate = goodRate; }
    
    public int getReplies() { return replies; }
    public void setReplies(int replies) { this.replies = replies; }
    
    public String getOnlineTime() { return onlineTime; }
    public void setOnlineTime(String onlineTime) { this.onlineTime = onlineTime; }
    
    public boolean isFollowed() { return followed; }
    public void setFollowed(boolean followed) { this.followed = followed; }
    
    public String getCoverImage() { return coverImage; }
    public void setCoverImage(String coverImage) { this.coverImage = coverImage; }
    
    /**
     * 获取头像URL (自动处理大小)
     */
    public String getAvatarUrl() {
        if (avatar == null || avatar.isEmpty()) {
            return "https://bbs.binmt.cc/uc_server/images/noavatar_middle.gif";
        }
        if (avatar.startsWith("http")) {
            return avatar;
        }
        return "https://bbs.binmt.cc/uc_server/avatar.php?uid=" + uid + "&size=middle";
    }
    
    /**
     * 获取等级数字 (从 "Lv.4" 提取 4)
     */
    public int getLevelNumber() {
        if (level == null || level.isEmpty()) return 0;
        try {
            return Integer.parseInt(level.replaceAll("[^0-9]", ""));
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
