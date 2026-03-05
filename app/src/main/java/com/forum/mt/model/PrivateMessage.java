package com.forum.mt.model;

/**
 * 私信消息数据模型
 */
public class PrivateMessage {
    private int pmId;              // 私信ID
    private int fromUid;           // 发送者ID
    private String fromUsername;   // 发送者用户名
    private String fromAvatar;     // 发送者头像
    private int toUid;             // 接收者ID
    private String toUsername;     // 接收者用户名
    private String subject;        // 消息主题
    private String message;        // 消息内容
    private String dateline;       // 发送时间
    private boolean isRead;        // 是否已读
    private boolean isNew;         // 是否新消息
    private int replyId;           // 回复ID
    private String delStatus;      // 删除状态
    
    public PrivateMessage() {}
    
    // Getters and Setters
    public int getPmId() { return pmId; }
    public void setPmId(int pmId) { this.pmId = pmId; }
    
    public int getFromUid() { return fromUid; }
    public void setFromUid(int fromUid) { this.fromUid = fromUid; }
    
    public String getFromUsername() { return fromUsername; }
    public void setFromUsername(String fromUsername) { this.fromUsername = fromUsername; }
    
    public String getFromAvatar() { return fromAvatar; }
    public void setFromAvatar(String fromAvatar) { this.fromAvatar = fromAvatar; }
    
    public int getToUid() { return toUid; }
    public void setToUid(int toUid) { this.toUid = toUid; }
    
    public String getToUsername() { return toUsername; }
    public void setToUsername(String toUsername) { this.toUsername = toUsername; }
    
    public String getSubject() { return subject; }
    public void setSubject(String subject) { this.subject = subject; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public String getDateline() { return dateline; }
    public void setDateline(String dateline) { this.dateline = dateline; }
    
    public boolean isRead() { return isRead; }
    public void setRead(boolean read) { isRead = read; }
    
    public boolean isNew() { return isNew; }
    public void setNew(boolean aNew) { isNew = aNew; }
    
    public int getReplyId() { return replyId; }
    public void setReplyId(int replyId) { this.replyId = replyId; }
    
    public String getDelStatus() { return delStatus; }
    public void setDelStatus(String delStatus) { this.delStatus = delStatus; }
    
    /**
     * 获取头像URL
     */
    public String getAvatarUrl() {
        if (fromAvatar != null && !fromAvatar.isEmpty()) {
            if (fromAvatar.startsWith("http")) {
                return fromAvatar;
            }
            return "https://bbs.binmt.cc/" + fromAvatar;
        }
        return "https://bbs.binmt.cc/uc_server/avatar.php?uid=" + fromUid + "&size=middle";
    }
}
