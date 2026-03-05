package com.forum.mt.model;

/**
 * 帖子内容块模型
 * 用于将HTML内容解析为原生Android组件可显示的数据结构
 */
public class ContentBlock {
    
    public static final int TYPE_TEXT = 0;
    public static final int TYPE_IMAGE = 1;
    public static final int TYPE_QUOTE = 2;
    public static final int TYPE_HIDDEN_CONTENT = 3;  // 隐藏内容提示
    public static final int TYPE_COLORED_TEXT = 4;    // 彩色文本
    public static final int TYPE_BOUNTY = 5;          // 悬赏内容
    public static final int TYPE_PAID_POST = 6;       // 付费帖子（未购买）
    public static final int TYPE_PAID_POST_INFO = 7;  // 付费主题信息（购买后）
    public static final int TYPE_ATTACHMENT = 8;      // 附件下载
    public static final int TYPE_SMILEY = 9;          // 表情符号
    public static final int TYPE_RICH_TEXT = 10;      // 富文本（含表情）
    public static final int TYPE_CODE = 11;           // 代码块
    public static final int TYPE_LINK = 12;           // 带链接的文字
    public static final int TYPE_STYLED_TEXT = 13;    // 带样式文本（加粗/斜体/下划线/中划线/颜色/背景色）
    public static final int TYPE_DIVIDER = 14;        // 分割线
    public static final int TYPE_TABLE = 15;         // 表格
    
    // 隐藏内容类型
    public static final int HIDDEN_TYPE_LOGIN = 1;    // 需要登录
    public static final int HIDDEN_TYPE_REPLY = 2;    // 需要回复
    public static final int HIDDEN_TYPE_BOUNTY = 3;   // 悬赏
    
    private int type;
    private String content;
    private String imageUrl;
    private int textColor;       // 彩色文本颜色（默认）
    private String textColorString; // 彩色文本颜色字符串（可能包含多个颜色）
    private int hiddenType;      // 隐藏类型
    private String hiddenHint;   // 隐藏内容提示文字
    private int bountyAmount;    // 悬赏金币数
    
    // 付费帖子相关字段
    private int paidPrice;           // 价格（金币数）
    private int paidBuyers;          // 购买人数
    private String paidDeadline;     // 购买截止日期
    private boolean hasPurchased;    // 是否已购买
    private String paidPostInfo;     // 付费主题信息（购买后显示）
    
    // 附件相关字段
    private Attachment attachment;
    
    // 代码块相关字段
    private String codeContent;       // 代码内容（纯文本）
    private String codeLanguage;      // 代码语言（如果有）
    private int codeLineCount;        // 代码行数
    
    // 链接相关字段
    private String linkUrl;           // 链接URL
    
    // 样式文本相关字段
    private String styledHtml;        // 带样式的HTML（用于富文本显示）
    
    /**
     * 附件信息类
     */
    public static class Attachment {
        private String fileName;      // 文件名
        private String downloadUrl;   // 下载链接
        private String fileSize;      // 文件大小 (如: 21.88 KB)
        private int downloadCount;    // 下载次数
        private int downloadCost;     // 下载积分 (负数表示扣减)
        private String uploadTime;    // 上传时间
        private String fileTypeIcon;  // 文件类型图标URL
        
        public Attachment() {}
        
        public String getFileName() { return fileName; }
        public void setFileName(String fileName) { this.fileName = fileName; }
        
        public String getDownloadUrl() { return downloadUrl; }
        public void setDownloadUrl(String downloadUrl) { this.downloadUrl = downloadUrl; }
        
        public String getFileSize() { return fileSize; }
        public void setFileSize(String fileSize) { this.fileSize = fileSize; }
        
        public int getDownloadCount() { return downloadCount; }
        public void setDownloadCount(int downloadCount) { this.downloadCount = downloadCount; }
        
        public int getDownloadCost() { return downloadCost; }
        public void setDownloadCost(int downloadCost) { this.downloadCost = downloadCost; }
        
        public String getUploadTime() { return uploadTime; }
        public void setUploadTime(String uploadTime) { this.uploadTime = uploadTime; }
        
        public String getFileTypeIcon() { return fileTypeIcon; }
        public void setFileTypeIcon(String fileTypeIcon) { this.fileTypeIcon = fileTypeIcon; }
        
        /**
         * 获取格式化的下载信息
         */
        public String getFormattedDownloadInfo() {
            StringBuilder sb = new StringBuilder();
            sb.append(fileSize != null ? fileSize : "未知大小");
            sb.append(" , 下载次数: ").append(downloadCount);
            if (downloadCost != 0) {
                sb.append(" , 下载积分: 金币 ").append(downloadCost);
            }
            return sb.toString();
        }
    }
    
    public ContentBlock() {}
    
    public ContentBlock(int type, String content) {
        this.type = type;
        this.content = content;
    }
    
    public static ContentBlock createTextBlock(String text) {
        ContentBlock block = new ContentBlock(TYPE_TEXT, text);
        return block;
    }
    
    public static ContentBlock createImageBlock(String imageUrl) {
        ContentBlock block = new ContentBlock(TYPE_IMAGE, null);
        block.setImageUrl(imageUrl);
        return block;
    }
    
    public static ContentBlock createQuoteBlock(String text) {
        ContentBlock block = new ContentBlock(TYPE_QUOTE, text);
        return block;
    }
    
    /**
     * 创建隐藏内容块
     * @param hiddenType 隐藏类型 (HIDDEN_TYPE_LOGIN 或 HIDDEN_TYPE_REPLY)
     * @param hint 提示文字
     */
    public static ContentBlock createHiddenBlock(int hiddenType, String hint) {
        ContentBlock block = new ContentBlock(TYPE_HIDDEN_CONTENT, null);
        block.setHiddenType(hiddenType);
        block.setHiddenHint(hint);
        return block;
    }
    
    /**
     * 创建彩色文本块
     * @param text 文本内容
     * @param color 颜色值（如 #FF0000）
     */
    public static ContentBlock createColoredTextBlock(String text, String color) {
        ContentBlock block = new ContentBlock(TYPE_COLORED_TEXT, text);
        block.setTextColor(color);
        return block;
    }
    
    /**
     * 创建悬赏内容块
     * @param content 悬赏内容描述
     * @param bountyAmount 悬赏金币数
     */
    public static ContentBlock createBountyBlock(String content, int bountyAmount) {
        ContentBlock block = new ContentBlock(TYPE_BOUNTY, content);
        block.setBountyAmount(bountyAmount);
        return block;
    }
    
    /**
     * 创建付费帖子内容块
     * @param price 价格（金币数）
     * @param buyers 购买人数
     * @param deadline 截止日期
     * @param purchased 是否已购买
     */
    public static ContentBlock createPaidPostBlock(int price, int buyers, String deadline, boolean purchased) {
        ContentBlock block = new ContentBlock(TYPE_PAID_POST, null);
        block.setPaidPrice(price);
        block.setPaidBuyers(buyers);
        block.setPaidDeadline(deadline);
        block.setHasPurchased(purchased);
        return block;
    }
    
    /**
     * 创建付费主题信息块（购买后显示）
     * @param price 价格（金币数）
     */
    public static ContentBlock createPaidPostInfoBlock(int price) {
        ContentBlock block = new ContentBlock(TYPE_PAID_POST_INFO, null);
        block.setPaidPrice(price);
        return block;
    }
    
    /**
     * 创建附件内容块
     * @param attachment 附件信息
     */
    public static ContentBlock createAttachmentBlock(Attachment attachment) {
        ContentBlock block = new ContentBlock(TYPE_ATTACHMENT, null);
        block.setAttachment(attachment);
        return block;
    }
    
    /**
     * 创建表情符号内容块
     * @param smileyUrl 表情图片URL
     */
    public static ContentBlock createSmileyBlock(String smileyUrl) {
        ContentBlock block = new ContentBlock(TYPE_SMILEY, null);
        block.setImageUrl(smileyUrl);
        return block;
    }
    
    /**
     * 创建富文本内容块（包含文本和表情的混合内容）
     * @param htmlContent HTML内容（可能包含img标签）
     */
    public static ContentBlock createRichTextBlock(String htmlContent) {
        ContentBlock block = new ContentBlock(TYPE_RICH_TEXT, null);
        block.setContent(htmlContent);
        return block;
    }
    
    /**
     * 创建代码块
     * @param code 代码内容
     * @param language 代码语言（可选）
     */
    public static ContentBlock createCodeBlock(String code, String language) {
        ContentBlock block = new ContentBlock(TYPE_CODE, null);
        block.setCodeContent(code);
        block.setCodeLanguage(language);
        // 计算行数
        if (code != null) {
            block.setCodeLineCount(code.split("\n").length);
        }
        return block;
    }
    
    /**
     * 创建链接内容块
     * @param text 链接文字
     * @param url 链接URL
     */
    public static ContentBlock createLinkBlock(String text, String url) {
        ContentBlock block = new ContentBlock(TYPE_LINK, text);
        block.setLinkUrl(url);
        return block;
    }
    
    /**
     * 创建带样式的文本内容块
     * @param text 纯文本内容
     * @param styledHtml 带样式的HTML
     */
    public static ContentBlock createStyledTextBlock(String text, String styledHtml) {
        ContentBlock block = new ContentBlock(TYPE_STYLED_TEXT, text);
        block.setStyledHtml(styledHtml);
        return block;
    }
    
    /**
     * 创建分割线内容块
     */
    public static ContentBlock createDividerBlock() {
        return new ContentBlock(TYPE_DIVIDER, null);
    }
    
    public int getType() {
        return type;
    }
    
    public void setType(int type) {
        this.type = type;
    }
    
    public String getContent() {
        return content;
    }
    
    public void setContent(String content) {
        this.content = content;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public int getTextColor() {
        return textColor;
    }
    
    /**
     * 获取颜色字符串（可能包含多个颜色，用逗号分隔）
     */
    public String getTextColorString() {
        return textColorString;
    }
    
    /**
     * 设置颜色，支持单个颜色或多个颜色（逗号分隔）
     */
    public void setTextColor(String color) {
        if (color == null) {
            this.textColorString = null;
            this.textColor = android.graphics.Color.BLACK;
            return;
        }
        
        this.textColorString = color;
        
        // 如果包含逗号，说明是多个颜色，取第一个作为默认颜色
        if (color.contains(",")) {
            String firstColor = color.split(",")[0].trim();
            try {
                this.textColor = android.graphics.Color.parseColor(firstColor);
            } catch (IllegalArgumentException e) {
                this.textColor = android.graphics.Color.BLACK;
            }
        } else {
            // 单个颜色
            try {
                this.textColor = android.graphics.Color.parseColor(color);
            } catch (IllegalArgumentException e) {
                this.textColor = android.graphics.Color.BLACK;
            }
        }
    }
    
    /**
     * 获取所有颜色数组
     */
    public int[] getTextColors() {
        if (textColorString == null) {
            return new int[]{android.graphics.Color.BLACK};
        }
        
        String[] colorStrings = textColorString.split(",");
        int[] colors = new int[colorStrings.length];
        
        for (int i = 0; i < colorStrings.length; i++) {
            try {
                colors[i] = android.graphics.Color.parseColor(colorStrings[i].trim());
            } catch (IllegalArgumentException e) {
                colors[i] = android.graphics.Color.BLACK;
            }
        }
        
        return colors;
    }
    
    public boolean isText() {
        return type == TYPE_TEXT;
    }
    
    public boolean isImage() {
        return type == TYPE_IMAGE;
    }
    
    public boolean isQuote() {
        return type == TYPE_QUOTE;
    }
    
    public boolean isHidden() {
        return type == TYPE_HIDDEN_CONTENT;
    }
    
    public boolean isColoredText() {
        return type == TYPE_COLORED_TEXT;
    }
    
    public boolean isBounty() {
        return type == TYPE_BOUNTY;
    }
    
    public int getHiddenType() {
        return hiddenType;
    }
    
    public void setHiddenType(int hiddenType) {
        this.hiddenType = hiddenType;
    }
    
    public String getHiddenHint() {
        return hiddenHint;
    }
    
    public void setHiddenHint(String hiddenHint) {
        this.hiddenHint = hiddenHint;
    }
    
    public int getBountyAmount() {
        return bountyAmount;
    }
    
    public void setBountyAmount(int bountyAmount) {
        this.bountyAmount = bountyAmount;
    }
    
    // 付费帖子相关getter/setter
    public int getPaidPrice() {
        return paidPrice;
    }
    
    public void setPaidPrice(int paidPrice) {
        this.paidPrice = paidPrice;
    }
    
    public int getPaidBuyers() {
        return paidBuyers;
    }
    
    public void setPaidBuyers(int paidBuyers) {
        this.paidBuyers = paidBuyers;
    }
    
    public String getPaidDeadline() {
        return paidDeadline;
    }
    
    public void setPaidDeadline(String paidDeadline) {
        this.paidDeadline = paidDeadline;
    }
    
    public boolean hasPurchased() {
        return hasPurchased;
    }
    
    public void setHasPurchased(boolean hasPurchased) {
        this.hasPurchased = hasPurchased;
    }
    
    public String getPaidPostInfo() {
        return paidPostInfo;
    }
    
    public void setPaidPostInfo(String paidPostInfo) {
        this.paidPostInfo = paidPostInfo;
    }
    
    public boolean isPaidPost() {
        return type == TYPE_PAID_POST;
    }
    
    public boolean isPaidPostInfo() {
        return type == TYPE_PAID_POST_INFO;
    }
    
    public Attachment getAttachment() {
        return attachment;
    }
    
    public void setAttachment(Attachment attachment) {
        this.attachment = attachment;
    }
    
    public boolean isAttachment() {
        return type == TYPE_ATTACHMENT;
    }
    
    public boolean isSmiley() {
        return type == TYPE_SMILEY;
    }
    
    public boolean isRichText() {
        return type == TYPE_RICH_TEXT;
    }
    
    public boolean isCode() {
        return type == TYPE_CODE;
    }
    
    public boolean isLink() {
        return type == TYPE_LINK;
    }
    
    // 代码块相关getter/setter
    public String getCodeContent() {
        return codeContent;
    }
    
    public void setCodeContent(String codeContent) {
        this.codeContent = codeContent;
    }
    
    public String getCodeLanguage() {
        return codeLanguage;
    }
    
    public void setCodeLanguage(String codeLanguage) {
        this.codeLanguage = codeLanguage;
    }
    
    public int getCodeLineCount() {
        return codeLineCount;
    }
    
    public void setCodeLineCount(int codeLineCount) {
        this.codeLineCount = codeLineCount;
    }
    
    // 链接相关getter/setter
    public String getLinkUrl() {
        return linkUrl;
    }
    
    public void setLinkUrl(String linkUrl) {
        this.linkUrl = linkUrl;
    }
    
    // 样式文本相关getter/setter
    public String getStyledHtml() {
        return styledHtml;
    }
    
    public void setStyledHtml(String styledHtml) {
        this.styledHtml = styledHtml;
    }
    
    public boolean isStyledText() {
        return type == TYPE_STYLED_TEXT;
    }
    
    public boolean isDivider() {
        return type == TYPE_DIVIDER;
    }
}
