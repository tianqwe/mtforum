package com.forum.mt.api;

import java.util.HashMap;
import java.util.Map;

/**
 * API常量配置
 */
public class ApiConfig {
    // 基础URL
    public static final String BASE_URL = "https://bbs.binmt.cc/";
    
    // OSS图片URL
    public static final String OSS_URL = "https://oss3-bbs.mt2.cn/";
    public static final String CDN_URL = "https://cdn-bbs.mt2.cn/";
    
    // Cookie前缀
    public static final String COOKIE_PREFIX = "cQWy_2132_";
    
    // 请求超时(秒)
    public static final int CONNECT_TIMEOUT = 15;
    public static final int READ_TIMEOUT = 30;
    public static final int WRITE_TIMEOUT = 30;
    
    // User-Agent
    public static final String USER_AGENT = "Mozilla/5.0 (Linux; Android 16; MTForumApp) AppleWebKit/537.36";
    
    /**
     * 构建完整URL
     */
    public static String buildUrl(String path) {
        if (path.startsWith("http")) return path;
        return BASE_URL + (path.startsWith("/") ? path.substring(1) : path);
    }
    
    /**
     * 获取图片URL
     */
    public static String getImageUrl(int aid, String size, String key) {
        return BASE_URL + "forum.php?mod=image&aid=" + aid + "&size=" + size + "&key=" + key;
    }
    
    /**
     * 获取头像URL
     */
    public static String getAvatarUrl(int uid, String size) {
        return BASE_URL + "uc_server/avatar.php?uid=" + uid + "&size=" + size;
    }
    
    /**
     * 获取帖子URL
     */
    public static String getThreadUrl(int tid) {
        return BASE_URL + "thread-" + tid + "-1-1.html";
    }
    
    /**
     * 获取帖子详情URL (带分页和排序参数)
     * @param tid 帖子ID
     * @param page 页码 (从1开始)
     * @param orderType 排序类型: 1=最新, 2=最早, 3=只看楼主
     * @param authorId 作者ID (仅当orderType=3时使用)
     */
    public static String getThreadUrl(int tid, int page, int orderType, int authorId) {
        StringBuilder url = new StringBuilder(BASE_URL + "forum.php?mod=viewthread&tid=" + tid);
        
        if (orderType == 3 && authorId > 0) {
            // 只看楼主：只需要authorid参数
            url.append("&page=").append(page);
            url.append("&authorid=").append(authorId);
        } else {
            // 普通浏览：带extra参数和ordertype
            url.append("&extra=page%3D1");
            if (page > 1) {
                url.append("&page=").append(page);
            }
            // ordertype=1 是最新（倒序），ordertype=2 是最早（正序）
            url.append("&ordertype=").append(orderType);
        }
        return url.toString();
    }
    
    /**
     * 获取帖子详情URL (带分页和排序参数) - 兼容旧方法
     */
    public static String getThreadUrl(int tid, int page, int orderType) {
        return getThreadUrl(tid, page, orderType, 0);
    }
    
    /**
     * 获取版块URL
     */
    public static String getForumUrl(int fid) {
        return BASE_URL + "forum.php?mod=forumdisplay&fid=" + fid;
    }
    
    /**
     * 获取签到页面URL
     */
    public static String getSignPageUrl() {
        return BASE_URL + "k_misign-sign.html";
    }
    
    /**
     * 获取签到提交URL
     * @param formhash 表单验证码
     */
    public static String getSignSubmitUrl(String formhash) {
        return BASE_URL + "plugin.php?id=k_misign:sign&operation=qiandao&formhash=" + formhash + "&format=text";
    }
    
    /**
     * 获取签到状态URL
     */
    public static String getSignStatusUrl() {
        return BASE_URL + "plugin.php?id=k_misign:sign&operation=list&op=today";
    }
    
    /**
     * 获取登录页面URL (AJAX)
     */
    public static String getLoginPageUrl() {
        return BASE_URL + "member.php?mod=logging&action=login&infloat=yes&handlekey=login&inajax=1&ajaxtarget=fwin_content_login";
    }
    
    /**
     * 获取登录页面URL (移动端)
     */
    public static String getLoginPageMobileUrl() {
        return BASE_URL + "member.php?mod=logging&action=login&mobile=2";
    }
    
    /**
     * 获取登录提交URL
     */
    public static String getLoginSubmitUrl() {
        return BASE_URL + "member.php?mod=logging&action=login&loginsubmit=yes&inajax=1";
    }
    
    /**
     * 获取登录提交URL (带loginhash)
     */
    public static String getLoginSubmitUrl(String loginhash) {
        return BASE_URL + "member.php?mod=logging&action=login&loginsubmit=yes&loginhash=" + loginhash + "&handlekey=loginform&inajax=1";
    }
    
    /**
     * 获取默认请求头
     */
    public static Map<String, String> getDefaultHeaders() {
        Map<String, String> headers = new HashMap<>();
        headers.put("User-Agent", USER_AGENT);
        headers.put("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8");
        headers.put("Accept-Language", "zh-CN,zh;q=0.9");
        // 不设置Accept-Encoding，让OkHttp自动处理gzip解压
        return headers;
    }
    
    // ========== 消息相关URL ==========
    
    /**
     * 获取私信列表URL
     */
    public static String getPmListUrl(int page) {
        String url = BASE_URL + "home.php?mod=space&do=pm&filter=privatepm";
        if (page > 1) {
            url += "&page=" + page;
        }
        return url;
    }
    
    /**
     * 获取私信详情URL
     */
    public static String getPmDetailUrl(int pmId, int page) {
        String url = BASE_URL + "home.php?mod=space&do=pm&subop=view&pmid=" + pmId + "&mobile=2";
        if (page > 1) {
            url += "&page=" + page;
        }
        return url;
    }
    
    /**
     * 获取通知列表URL (公共消息)
     */
    public static String getNoticeListUrl(String type, int page) {
        String url = BASE_URL + "home.php?mod=space&do=pm&filter=announcepm";
        if (page > 1) {
            url += "&page=" + page;
        }
        return url;
    }
    
    /**
     * 检查新私信URL
     */
    public static String getCheckNewPmUrl() {
        return BASE_URL + "home.php?mod=spacecp&ac=pm&op=checknewpm";
    }
    
    /**
     * 发送私信URL
     */
    public static String getSendPmUrl() {
        return BASE_URL + "home.php?mod=spacecp&ac=pm&op=send&touid=0&pmid=0&do=reply&mobile=2&inajax=1";
    }
    
    /**
     * 发送私信URL (指定用户)
     */
    public static String getSendPmUrl(int toUid) {
        return BASE_URL + "home.php?mod=spacecp&ac=pm&op=send&touid=" + toUid + "&pmid=0&do=reply&mobile=2&inajax=1";
    }
}
