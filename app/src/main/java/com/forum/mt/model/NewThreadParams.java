package com.forum.mt.model;

/**
 * 发帖页面参数
 * 从发帖页面HTML中提取的必要参数
 */
public class NewThreadParams {
    private String formhash;         // 表单验证hash
    private String uploadHash;       // 图片上传hash
    private String attachHash;       // 附件上传hash (通常与uploadHash相同)
    private int uid;                 // 当前用户ID
    private int fid;                 // 版块ID
    private String forumName;        // 版块名称
    private long posttime;           // 发帖时间戳
    private int allowPostImg;        // 是否允许上传图片
    private int maxPrice;            // 最大售价
    
    // 权限设置
    private int price;               // 帖子售价
    private String password;         // 帖子密码
    
    // 高级选项
    private boolean hiddenReplies;   // 回帖仅作者可见
    private boolean orderType;       // 回帖倒序排列
    private boolean allowNoticeAuthor = true; // 接收回复通知
    private boolean useSig = true;   // 使用个人签名
    
    // 投票相关
    private boolean isPoll;          // 是否是投票帖
    private int maxChoices;          // 最大可选项数
    
    public String getFormhash() {
        return formhash;
    }
    
    public void setFormhash(String formhash) {
        this.formhash = formhash;
    }
    
    public String getUploadHash() {
        return uploadHash;
    }
    
    public void setUploadHash(String uploadHash) {
        this.uploadHash = uploadHash;
    }
    
    public String getAttachHash() {
        return attachHash;
    }
    
    public void setAttachHash(String attachHash) {
        this.attachHash = attachHash;
    }
    
    public int getUid() {
        return uid;
    }
    
    public void setUid(int uid) {
        this.uid = uid;
    }
    
    public int getFid() {
        return fid;
    }
    
    public void setFid(int fid) {
        this.fid = fid;
    }
    
    public String getForumName() {
        return forumName;
    }
    
    public void setForumName(String forumName) {
        this.forumName = forumName;
    }
    
    public long getPosttime() {
        return posttime;
    }
    
    public void setPosttime(long posttime) {
        this.posttime = posttime;
    }
    
    public int getAllowPostImg() {
        return allowPostImg;
    }
    
    public void setAllowPostImg(int allowPostImg) {
        this.allowPostImg = allowPostImg;
    }
    
    public int getMaxPrice() {
        return maxPrice;
    }
    
    public void setMaxPrice(int maxPrice) {
        this.maxPrice = maxPrice;
    }
    
    public int getPrice() {
        return price;
    }
    
    public void setPrice(int price) {
        this.price = price;
    }
    
    public String getPassword() {
        return password;
    }
    
    public void setPassword(String password) {
        this.password = password;
    }
    
    public boolean isHiddenReplies() {
        return hiddenReplies;
    }
    
    public void setHiddenReplies(boolean hiddenReplies) {
        this.hiddenReplies = hiddenReplies;
    }
    
    public boolean isOrderType() {
        return orderType;
    }
    
    public void setOrderType(boolean orderType) {
        this.orderType = orderType;
    }
    
    public boolean isAllowNoticeAuthor() {
        return allowNoticeAuthor;
    }
    
    public void setAllowNoticeAuthor(boolean allowNoticeAuthor) {
        this.allowNoticeAuthor = allowNoticeAuthor;
    }
    
    public boolean isUseSig() {
        return useSig;
    }
    
    public void setUseSig(boolean useSig) {
        this.useSig = useSig;
    }
    
    public boolean isPoll() {
        return isPoll;
    }
    
    public void setPoll(boolean poll) {
        isPoll = poll;
    }
    
    public int getMaxChoices() {
        return maxChoices;
    }
    
    public void setMaxChoices(int maxChoices) {
        this.maxChoices = maxChoices;
    }
    
    /**
     * 检查参数是否有效
     */
    public boolean isValid() {
        return formhash != null && !formhash.isEmpty();
    }
}
