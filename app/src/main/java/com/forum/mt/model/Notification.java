package com.forum.mt.model;

/**
 * 通知数据模型
 */
public class Notification {
    // 通知类型
    public static final String TYPE_REPLY = "reply";           // 回复
    public static final String TYPE_MENTION = "mention";       // @提及
    public static final String TYPE_SYSTEM = "system";         // 系统消息
    public static final String TYPE_FRIEND = "friend";         // 好友请求
    public static final String TYPE_LIKE = "like";             // 点赞
    public static final String TYPE_FOLLOW = "follow";         // 关注
    public static final String TYPE_DIGEST = "digest";         // 精华
    public static final String TYPE_BOUNTY = "bounty";         // 悬赏
    
    private int id;               // 通知ID
    private String type;          // 通知类型
    private int uid;              // 触发用户ID
    private String username;      // 触发用户名
    private String avatar;        // 触发用户头像
    private String content;       // 通知内容
    private String note;          // 附加说明
    private int relatedId;        // 相关ID (帖子ID/评论ID等)
    private int fromId;           // 来源ID
    private String url;           // 跳转链接
    private String dateline;      // 时间
    private boolean isRead;       // 是否已读
    private boolean isNew;        // 是否新通知
    
    public Notification() {}
    
    // Getters and Setters
    public int getId() { return id; }
    public void setId(int id) { this.id = id; }
    
    public String getType() { return type; }
    public void setType(String type) { this.type = type; }
    
    public int getUid() { return uid; }
    public void setUid(int uid) { this.uid = uid; }
    
    public String getUsername() { return username; }
    public void setUsername(String username) { this.username = username; }
    
    public String getAvatar() { return avatar; }
    public void setAvatar(String avatar) { this.avatar = avatar; }
    
    public String getContent() { return content; }
    public void setContent(String content) { this.content = content; }
    
    public String getNote() { return note; }
    public void setNote(String note) { this.note = note; }
    
    public int getRelatedId() { return relatedId; }
    public void setRelatedId(int relatedId) { this.relatedId = relatedId; }
    
    public int getFromId() { return fromId; }
    public void setFromId(int fromId) { this.fromId = fromId; }
    
    public String getUrl() { return url; }
    public void setUrl(String url) { this.url = url; }
    
    public String getDateline() { return dateline; }
    public void setDateline(String dateline) { this.dateline = dateline; }
    
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    
    public boolean isNew() { return isNew; }
    public void setNew(boolean aNew) { isNew = aNew; }
    
    /**
     * 获取头像URL
     */
    public String getAvatarUrl() {
        if (avatar != null && !avatar.isEmpty()) {
            if (avatar.startsWith("http")) {
                return avatar;
            }
            return "https://bbs.binmt.cc/" + avatar;
        }
        return "https://bbs.binmt.cc/uc_server/avatar.php?uid=" + uid + "&size=middle";
    }
    
    /**
     * 获取通知类型显示名称
     */
    public String getTypeDisplayName() {
        switch (type) {
            case TYPE_REPLY:
                return "回复了我的帖子";
            case TYPE_MENTION:
                return "在帖子中提到了我";
            case TYPE_SYSTEM:
                return "系统消息";
            case TYPE_FRIEND:
                return "好友请求";
            case TYPE_LIKE:
                return "赞了我的帖子";
            case TYPE_FOLLOW:
                return "关注了我";
            case TYPE_DIGEST:
                return "帖子被设为精华";
            case TYPE_BOUNTY:
                return "悬赏相关";
            default:
                return "通知";
        }
    }
}
