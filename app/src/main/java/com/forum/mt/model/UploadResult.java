package com.forum.mt.model;

/**
 * 上传响应结果
 * 响应格式: DISCUZUPLOAD|status|extra|aid|isImage|serverPath|originalName|flag
 * 示例: DISCUZUPLOAD|1|0|354030|1|202602/25/195142nyguffgdy661shz1.jpg|1000215627.jpg|0
 */
public class UploadResult {
    private int status;         // 状态码: 0=成功
    private int extra;          // 附加状态
    private int aid;            // 附件ID
    private int isImage;        // 是否图片: 1=是
    private String serverPath;  // 服务器存储路径
    private String originalName; // 原始文件名
    private int flag;           // 附加标志
    private String errorMessage; // 错误信息
    
    // 错误码对应的消息
    private static final String[] STATUS_MESSAGES = {
        "上传成功",           // 0
        "不支持此类扩展名",   // 1
        "服务器限制无法上传那么大的附件", // 2
        "用户组限制无法上传那么大的附件", // 3
        "不支持此类扩展名",   // 4
        "文件类型限制无法上传那么大的附件", // 5
        "今日您已无法上传更多的附件", // 6
        "请选择图片文件",     // 7
        "附件文件无法保存",   // 8
        "没有合法的文件被上传", // 9
        "非法操作",           // 10
        "今日您已无法上传那么大的附件"  // 11
    };
    
    /**
     * 解析上传响应字符串
     * @param response 响应字符串
     * @return UploadResult对象
     */
    public static UploadResult parse(String response) {
        UploadResult result = new UploadResult();
        
        if (response == null || response.isEmpty()) {
            result.status = -1;
            result.errorMessage = "上传响应为空";
            return result;
        }
        
        // 检查是否是DISCUZUPLOAD格式
        if (!response.startsWith("DISCUZUPLOAD")) {
            result.status = -1;
            result.errorMessage = "未知的响应格式";
            return result;
        }
        
        try {
            String[] parts = response.split("\\|");
            if (parts.length >= 7) {
                // parts[0] = "DISCUZUPLOAD"
                // parts[1] = 状态（通常为1）
                // parts[2] = 错误码（0表示成功）
                // parts[3] = 附件ID
                // parts[4] = 是否图片
                // parts[5] = 服务器路径
                // parts[6] = 原始文件名
                result.status = Integer.parseInt(parts[2]);  // 错误码在parts[2]
                result.extra = Integer.parseInt(parts[1]);   // 状态在parts[1]
                result.aid = Integer.parseInt(parts[3]);
                result.isImage = Integer.parseInt(parts[4]);
                result.serverPath = parts[5];
                result.originalName = parts[6];
                if (parts.length >= 8) {
                    result.flag = Integer.parseInt(parts[7]);
                }
                
                // 设置错误信息
                if (result.status != 0) {
                    if (result.status > 0 && result.status < STATUS_MESSAGES.length) {
                        result.errorMessage = STATUS_MESSAGES[result.status];
                    } else {
                        result.errorMessage = "上传失败，错误码: " + result.status;
                    }
                }
            } else {
                result.status = -1;
                result.errorMessage = "响应格式不完整";
            }
        } catch (NumberFormatException e) {
            result.status = -1;
            result.errorMessage = "解析响应失败: " + e.getMessage();
        }
        
        return result;
    }
    
    /**
     * 是否上传成功
     */
    public boolean isSuccess() {
        return status == 0;
    }
    
    /**
     * 获取图片URL
     */
    public String getImageUrl() {
        if (serverPath != null && !serverPath.isEmpty()) {
            return "https://cdn-bbs.mt2.cn/data/attachment/forum/" + serverPath;
        }
        return null;
    }
    
    /**
     * 获取BBCode插入代码
     */
    public String getAttachCode() {
        if (isImage == 1) {
            return "[attachimg]" + aid + "[/attachimg]";
        } else {
            return "[attach]" + aid + "[/attach]";
        }
    }
    
    // Getters and Setters
    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }
    public int getExtra() { return extra; }
    public void setExtra(int extra) { this.extra = extra; }
    public int getAid() { return aid; }
    public void setAid(int aid) { this.aid = aid; }
    public int getIsImage() { return isImage; }
    public void setIsImage(int isImage) { this.isImage = isImage; }
    public String getServerPath() { return serverPath; }
    public void setServerPath(String serverPath) { this.serverPath = serverPath; }
    public String getOriginalName() { return originalName; }
    public void setOriginalName(String originalName) { this.originalName = originalName; }
    public int getFlag() { return flag; }
    public void setFlag(int flag) { this.flag = flag; }
    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
}
