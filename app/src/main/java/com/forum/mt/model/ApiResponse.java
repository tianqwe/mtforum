package com.forum.mt.model;

/**
 * API响应包装类
 * @param <T> 数据类型
 */
public class ApiResponse<T> {
    private int code;
    private String message;
    private T data;
    private boolean success;
    private String rawHtml;  // 原始HTML响应
    
    public ApiResponse() {}
    
    public ApiResponse(int code, String message, T data) {
        this.code = code;
        this.message = message;
        this.data = data;
        this.success = code == 0 || code == 200;
    }
    
    // 静态工厂方法
    public static <T> ApiResponse<T> success(T data) {
        return new ApiResponse<>(200, "success", data);
    }
    
    public static <T> ApiResponse<T> success(T data, String rawHtml) {
        ApiResponse<T> response = new ApiResponse<>(200, "success", data);
        response.setRawHtml(rawHtml);
        return response;
    }
    
    public static <T> ApiResponse<T> error(int code, String message) {
        return new ApiResponse<>(code, message, null);
    }
    
    public static <T> ApiResponse<T> error(String message) {
        return new ApiResponse<>(-1, message, null);
    }
    
    // Getters and Setters
    public int getCode() { return code; }
    public void setCode(int code) { this.code = code; }
    
    public String getMessage() { return message; }
    public void setMessage(String message) { this.message = message; }
    
    public T getData() { return data; }
    public void setData(T data) { this.data = data; }
    
    public boolean isSuccess() { return success; }
    public void setSuccess(boolean success) { this.success = success; }
    
    public String getRawHtml() { return rawHtml; }
    public void setRawHtml(String rawHtml) { this.rawHtml = rawHtml; }
    
    public boolean hasData() {
        if (data == null) {
            return false;
        }
        // 如果是列表，检查是否为空
        if (data instanceof java.util.List) {
            return !((java.util.List<?>) data).isEmpty();
        }
        return true;
    }
}
