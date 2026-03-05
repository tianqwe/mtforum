package com.forum.mt.api;

import com.forum.mt.model.ApiResponse;
import com.forum.mt.model.Comment;
import com.forum.mt.model.Forum;
import com.forum.mt.model.ForumInfo;
import com.forum.mt.model.NewThreadParams;
import com.forum.mt.model.Notification;
import com.forum.mt.model.Post;
import com.forum.mt.model.PrivateMessage;
import com.forum.mt.model.UploadResult;
import com.forum.mt.model.User;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;

/**
 * 论坛API服务
 */
public class ForumApi {
    private final OkHttpClient client;
    private final CookieManager cookieManager;

    // 搜索状态
    private String currentSearchKeyword;
    private String currentSearchId;
    private int currentSearchTotalPages;

    public ForumApi(OkHttpClient client, CookieManager cookieManager) {
        this.client = client;
        this.cookieManager = cookieManager;
    }

    /**
     * 获取当前搜索关键词
     */
    public String getCurrentSearchKeyword() {
        return currentSearchKeyword;
    }

    /**
     * 获取当前搜索ID
     */
    public String getCurrentSearchId() {
        return currentSearchId;
    }

    /**
     * 获取当前搜索总页数
     */
    public int getCurrentSearchTotalPages() {
        return currentSearchTotalPages;
    }
    
    /**
     * 获取首页帖子 (AJAX接口)
     * 接口: plugin.php?id=comiis_app_portal&pid=1&istab=yes&inajax=1
     * @param page 页码，从1开始
     */
    public ApiResponse<List<Post>> getHomePagePosts(int page) {
        String url;
        if (page <= 1) {
            url = ApiConfig.BASE_URL + "plugin.php?id=comiis_app_portal&pid=1&istab=yes&inajax=1";
        } else {
            url = ApiConfig.BASE_URL + "plugin.php?id=comiis_app_portal&pid=1&page=" + page + "&inajax=1";
        }
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .header("X-Requested-With", "XMLHttpRequest")
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String html = response.body().string();
                
                if (html.isEmpty()) {
                    return ApiResponse.error("响应为空");
                }
                
                List<Post> posts = HtmlParser.parseHomePage(html);
                
                if (posts.isEmpty()) {
                    return ApiResponse.error("未找到帖子");
                }
                
                return ApiResponse.success(posts);
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取板块列表
     * 接口: forum.php?forumlist=1
     */
    public ApiResponse<List<Forum>> getForumList() {
        String url = ApiConfig.BASE_URL + "forum.php?forumlist=1";
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String html = response.body().string();
                
                if (html.isEmpty()) {
                    return ApiResponse.error("响应为空");
                }
                
                List<Forum> forums = HtmlParser.parseForumList(html);
                
                // 即使板块为空也返回原始HTML（用于解析统计信息）
                if (forums.isEmpty()) {
                    ApiResponse<List<Forum>> result = ApiResponse.error("未找到板块");
                    result.setRawHtml(html);
                    return result;
                }
                
                return ApiResponse.success(forums, html);
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取热帖排行 (从首页HTML解析)
     * 接口: forum.php?mod=guide&view=hot
     */
    public ApiResponse<List<Post>> getHotRank() {
        String url = ApiConfig.BASE_URL + "forum.php?mod=guide&view=hot";
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String html = response.body().string();
                
                if (html.isEmpty()) {
                    return ApiResponse.error("响应为空");
                }
                
                List<Post> posts = HtmlParser.parseHotRank(html);
                
                if (posts.isEmpty()) {
                    return ApiResponse.error("未找到热帖排行");
                }
                
                return ApiResponse.success(posts);
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取热门帖子列表
     */
    public ApiResponse<List<Post>> getHotPosts(int page) {
        return getPostList("hot", page);
    }
    
    /**
     * 获取最新帖子列表
     */
    public ApiResponse<List<Post>> getNewPosts(int page) {
        return getPostList("newthread", page);
    }
    
    /**
     * 获取精华帖子列表
     */
    public ApiResponse<List<Post>> getDigestPosts(int page) {
        return getPostList("digest", page);
    }
    
    /**
     * 获取帖子列表
     */
    private ApiResponse<List<Post>> getPostList(String view, int page) {
        String url = ApiConfig.BASE_URL + "forum.php?mod=guide&view=" + view;
        if (page > 1) {
            url += "&page=" + page;
        }
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String html = response.body().string();
                
                if (html.isEmpty()) {
                    return ApiResponse.error("响应为空");
                }
                
                List<Post> posts = HtmlParser.parsePostList(html);
                
                if (posts.isEmpty()) {
                    return ApiResponse.error("未找到帖子");
                }
                
                return ApiResponse.success(posts);
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取帖子详情
     */
    public ApiResponse<Post> getPostDetail(int tid) {
        String url = ApiConfig.getThreadUrl(tid);
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String html = response.body().string();
                
                Post post = HtmlParser.parsePostDetail(html);
                post.setTid(tid);
                return ApiResponse.success(post);
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取帖子评论列表
     * @param tid 帖子ID
     * @param page 页码 (从1开始)
     * @param orderType 排序类型: 1=最新, 2=最早, 3=只看楼主
     * @param authorId 作者ID (仅当orderType=3时使用)
     */
    public ApiResponse<List<Comment>> getComments(int tid, int page, int orderType, int authorId) {
        String url = ApiConfig.getThreadUrl(tid, page, orderType, authorId);
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String html = response.body().string();
                
                List<Comment> comments = HtmlParser.parseComments(html);
                
                return ApiResponse.success(comments);
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取帖子评论列表 (兼容旧方法)
     */
    public ApiResponse<List<Comment>> getComments(int tid, int page, int orderType) {
        return getComments(tid, page, orderType, 0);
    }
    
    /**
     * 获取帖子评论列表 (兼容旧方法)
     */
    public ApiResponse<List<Comment>> getComments(int tid, int page) {
        return getComments(tid, page, 1, 0); // 默认最新排序
    }
    
    /**
     * 获取图片URL (处理302重定向)
     */
    public String getImageUrl(int aid, String size, String key) {
        String url = ApiConfig.getImageUrl(aid, size, key);
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (response.isRedirect()) {
                    return response.header("Location");
                }
            }
        } catch (IOException ignored) {}
        
        // 返回原始URL，让Glide处理
        return url;
    }
    
    /**
     * 检查登录状态
     */
    public boolean isLoggedIn() {
        return cookieManager.isLoggedIn();
    }
    
    /**
     * 设置Cookie (导入抓包获取的Cookie)
     */
    public void setCookie(String cookieString) {
        cookieManager.setCookieString(cookieString);
    }
    
    /**
     * 获取formhash (用于表单提交)
     * 尝试从多个页面获取
     */
    public String getFormhash() {
        // 尝试从多个页面获取formhash
        String[] urls = {
            ApiConfig.BASE_URL + "home.php?mod=spacecp&ac=profile",
            ApiConfig.BASE_URL + "home.php?mod=space&do=profile&mycenter=1",
            ApiConfig.BASE_URL + "forum.php?mod=guide&view=hot"
        };
        
        for (String url : urls) {
            try {
                Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", ApiConfig.USER_AGENT)
                    .header("Referer", ApiConfig.BASE_URL)
                    .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        continue;
                    }
                    
                    String html = response.body().string();
                    String formhash = HtmlParser.parseFormhash(html);
                    
                    if (formhash != null) {
                        return formhash;
                    }
                }
            } catch (IOException e) {
                // 继续尝试下一个URL
            }
        }
        
        return null;
    }
    
    /**
     * 获取用户资料
     * 接口: home.php?mod=space&do=profile&mobile=2
     * 使用移动端格式，HTML结构更清晰，便于解析
     */
    public ApiResponse<User> getUserProfile() {
        String url = ApiConfig.BASE_URL + "home.php?mod=space&do=profile&mobile=2";
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String html = response.body().string();
                
                // 检查是否需要登录
                if (html.contains("请先登录") || html.contains("您需要登录")) {
                    return ApiResponse.error("请先登录");
                }
                
                if (html.isEmpty()) {
                    return ApiResponse.error("响应为空");
                }
                
                User user = HtmlParser.parseUserInfo(html);
                
                if (user.getUid() == 0) {
                    return ApiResponse.error("未获取到用户信息");
                }
                
                return ApiResponse.success(user);
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取指定用户的详细信息
     * 接口: home.php?mod=space&uid={uid}&do=profile&view=me&from=space&mobile=2
     * @param uid 用户ID
     */
    public ApiResponse<User> getUserProfile(int uid) {
        // 使用更详细的资料页面URL
        String url = ApiConfig.BASE_URL + "home.php?mod=space&uid=" + uid + "&do=profile&view=me&from=space&mobile=2";
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String html = response.body().string();
                
                // 检查是否需要登录
                if (html.contains("请先登录") || html.contains("您需要登录")) {
                    return ApiResponse.error("请先登录");
                }
                
                if (html.isEmpty()) {
                    return ApiResponse.error("响应为空");
                }
                
                User user = HtmlParser.parseUserInfo(html);
                
                // 如果解析没有获取到uid，使用传入的uid
                if (user.getUid() == 0) {
                    user.setUid(uid);
                }
                
                return ApiResponse.success(user);
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取指定用户的帖子列表
     * 接口: home.php?mod=space&uid={uid}&do=thread&view=me&from=space
     * @param uid 用户ID
     * @param page 页码
     */
    public ApiResponse<List<Post>> getUserThreads(int uid, int page) {
        String url = ApiConfig.BASE_URL + "home.php?mod=space&uid=" + uid + "&do=thread&view=me&from=space";
        if (page > 1) {
            url += "&page=" + page;
        }
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String html = response.body().string();
                if (html.isEmpty()) {
                    return ApiResponse.error("响应为空");
                }
                
                List<Post> posts = HtmlParser.parsePostList(html);
                return ApiResponse.success(posts);
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取指定用户的回复列表
     * 接口: home.php?mod=space&uid={uid}&do=thread&view=me&type=reply&from=space
     * @param uid 用户ID
     * @param page 页码
     */
    public ApiResponse<List<Post>> getUserReplies(int uid, int page) {
        String url = ApiConfig.BASE_URL + "home.php?mod=space&uid=" + uid + "&do=thread&view=me&type=reply&from=space";
        if (page > 1) {
            url += "&page=" + page;
        }
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String html = response.body().string();
                if (html.isEmpty()) {
                    return ApiResponse.error("响应为空");
                }
                
                List<Post> posts = HtmlParser.parsePostList(html);
                return ApiResponse.success(posts);
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取指定用户的收藏列表
     * 接口: home.php?mod=space&uid={uid}&do=favorite&view=me&type=all
     * @param uid 用户ID
     * @param page 页码
     */
    public ApiResponse<List<Post>> getUserFavorites(int uid, int page) {
        String url = ApiConfig.BASE_URL + "home.php?mod=space&uid=" + uid + "&do=favorite&view=me&type=all";
        if (page > 1) {
            url += "&page=" + page;
        }
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String html = response.body().string();
                if (html.isEmpty()) {
                    return ApiResponse.error("响应为空");
                }
                
                List<Post> posts = HtmlParser.parseFavoriteList(html);
                
                // 并发获取帖子详情（优化加载速度）
                fetchPostDetailsConcurrently(posts);
                
                return ApiResponse.success(posts);
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取我的帖子列表
     * 接口: home.php?mod=space&do=thread&view=me
     * @param page 页码
     */
    public ApiResponse<List<Post>> getMyThreads(int page) {
        String url = ApiConfig.BASE_URL + "home.php?mod=space&do=thread&view=me";
        if (page > 1) {
            url += "&page=" + page;
        }
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String html = response.body().string();
                if (html.isEmpty()) {
                    return ApiResponse.error("响应为空");
                }
                
                List<Post> posts = HtmlParser.parsePostList(html);
                return ApiResponse.success(posts);
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取我的收藏列表
     * 接口: home.php?mod=space&do=favorite&view=me&type=all
     * @param page 页码
     */
    public ApiResponse<List<Post>> getMyFavorites(int page) {
        String url = ApiConfig.BASE_URL + "home.php?mod=space&do=favorite&view=me&type=all";
        if (page > 1) {
            url += "&page=" + page;
        }
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String html = response.body().string();
                if (html.isEmpty()) {
                    return ApiResponse.error("响应为空");
                }
                
                // 使用专门的收藏列表解析方法，提取favid
                List<Post> posts = HtmlParser.parseFavoriteList(html);
                
                // 并发获取帖子详情（优化加载速度）
                fetchPostDetailsConcurrently(posts);
                
                return ApiResponse.success(posts);
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 并发获取帖子详情信息
     * 使用线程池同时请求多个帖子，大幅提升加载速度
     */
    private void fetchPostDetailsConcurrently(List<Post> posts) {
        if (posts == null || posts.isEmpty()) {
            return;
        }
        
        // 创建固定大小的线程池（最多10个并发请求）
        java.util.concurrent.ExecutorService executor = 
            java.util.concurrent.Executors.newFixedThreadPool(Math.min(posts.size(), 10));
        
        // 使用CountDownLatch等待所有请求完成
        java.util.concurrent.CountDownLatch latch = 
            new java.util.concurrent.CountDownLatch(posts.size());
        
        for (Post post : posts) {
            executor.submit(() -> {
                try {
                    ApiResponse<Post> detailResponse = getPostDetail(post.getTid());
                    if (detailResponse.isSuccess() && detailResponse.getData() != null) {
                        Post detail = detailResponse.getData();
                        // 保留收藏ID，更新其他信息
                        int favId = post.getFavId();
                        post.setAuthor(detail.getAuthor());
                        post.setAuthorId(detail.getAuthorId());
                        post.setAuthorAvatar(detail.getAuthorAvatar());
                        post.setAuthorLevel(detail.getAuthorLevel());
                        post.setAuthorGender(detail.getAuthorGender());
                        post.setForumName(detail.getForumName());
                        post.setForumId(detail.getForumId());
                        post.setDateStr(detail.getDateStr());
                        post.setDateline(detail.getDateline());
                        post.setReplies(detail.getReplies());
                        post.setViews(detail.getViews());
                        post.setLikes(detail.getLikes());
                        post.setSummary(detail.getSummary());
                        post.setThumbnail(detail.getThumbnail());
                        post.setFavId(favId);
                    }
                } catch (Exception e) {
                    // 单个帖子获取失败不影响整体
                } finally {
                    latch.countDown();
                }
            });
        }
        
        try {
            // 等待所有请求完成，最多等待15秒
            latch.await(15, java.util.concurrent.TimeUnit.SECONDS);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        } finally {
            executor.shutdown();
        }
    }
    
    /**
     * 删除收藏
     * 接口: POST home.php?mod=spacecp&ac=favorite&op=delete&favid={favid}&type=all&mobile=2&inajax=1
     * @param favId 收藏ID
     */
    public ApiResponse<Boolean> deleteFavorite(int favId) {
        String formhash = getFormhash();
        if (formhash == null) {
            return ApiResponse.error("获取formhash失败，请先登录");
        }
        
        String url = ApiConfig.BASE_URL + "home.php?mod=spacecp&ac=favorite&op=delete&favid=" + favId +
                "&type=all&mobile=2&handlekey=favoriteform_" + favId + "&inajax=1";
        
        String postData = "deletesubmit=true&formhash=" + formhash + "&handlekey=favoriteform_" + favId;
        
        try {
            okhttp3.RequestBody body = okhttp3.RequestBody.create(
                okhttp3.MediaType.parse("application/x-www-form-urlencoded; charset=UTF-8"),
                postData
            );
            
            Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .header("X-Requested-With", "XMLHttpRequest")
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String result = response.body().string();
                if (result.contains("操作成功") || result.contains("删除成功")) {
                    return ApiResponse.success(true);
                } else {
                    return ApiResponse.error("删除收藏失败");
                }
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取粉丝列表 (AJAX)
     * 接口: home.php?mod=follow&do=follower&uid={uid}&inajax=1
     * @param uid 用户ID
     */
    public ApiResponse<FriendListResult> getFollowers(int uid) {
        String url = ApiConfig.BASE_URL + "home.php?mod=follow&do=follower&uid=" + uid + "&inajax=1";
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .header("X-Requested-With", "XMLHttpRequest")
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String html = response.body().string();
                
                // 提取粉丝数
                int count = HtmlParser.parseFollowCount(html, "粉丝");
                
                // 解析用户列表
                List<User> users = parseUserListFromAjax(html);
                
                FriendListResult result = new FriendListResult();
                result.setCount(count);
                result.setUsers(users);
                
                return ApiResponse.success(result);
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取关注列表 (AJAX)
     * 接口: home.php?mod=follow&do=following&uid={uid}&inajax=1
     * @param uid 用户ID
     */
    public ApiResponse<FriendListResult> getFollowing(int uid) {
        String url = ApiConfig.BASE_URL + "home.php?mod=follow&do=following&uid=" + uid + "&inajax=1";
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .header("X-Requested-With", "XMLHttpRequest")
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String html = response.body().string();
                
                // 提取关注数
                int count = HtmlParser.parseFollowCount(html, "关注");
                
                // 解析用户列表
                List<User> users = parseUserListFromAjax(html);
                
                FriendListResult result = new FriendListResult();
                result.setCount(count);
                result.setUsers(users);
                
                return ApiResponse.success(result);
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取访客列表 (AJAX)
     * 接口: home.php?mod=space&do=friend&view=visitor&inajax=1
     */
    public ApiResponse<FriendListResult> getVisitors() {
        String url = ApiConfig.BASE_URL + "home.php?mod=space&do=friend&view=visitor&inajax=1";
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .header("X-Requested-With", "XMLHttpRequest")
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String html = response.body().string();
                
                // 解析用户列表
                List<User> users = parseUserListFromAjax(html);
                
                FriendListResult result = new FriendListResult();
                result.setCount(users.size());
                result.setUsers(users);
                
                return ApiResponse.success(result);
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取好友列表 (AJAX)
     * 接口: home.php?mod=space&do=friend&inajax=1
     */
    public ApiResponse<FriendListResult> getFriends() {
        String url = ApiConfig.BASE_URL + "home.php?mod=space&do=friend&inajax=1";
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .header("X-Requested-With", "XMLHttpRequest")
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String html = response.body().string();
                
                // 提取好友数
                int count = HtmlParser.parseFollowCount(html, "好友");
                
                // 解析用户列表
                List<User> users = parseUserListFromAjax(html);
                
                FriendListResult result = new FriendListResult();
                result.setCount(count > 0 ? count : users.size());
                result.setUsers(users);
                
                return ApiResponse.success(result);
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 检查新私信
     * 接口: home.php?mod=spacecp&ac=pm&op=checknewpm&rand={rand}
     * @return 是否有新私信
     */
    public ApiResponse<Boolean> checkNewPm() {
        String url = ApiConfig.BASE_URL + "home.php?mod=spacecp&ac=pm&op=checknewpm&rand=" + System.currentTimeMillis();
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .header("X-Requested-With", "XMLHttpRequest")
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.success(false);
                }
                
                String result = response.body().string();
                // 响应格式可能是数字或JSON
                // 如果返回非空且有内容，表示有新私信
                return ApiResponse.success(result != null && !result.trim().isEmpty() && !result.equals("0"));
            }
        } catch (IOException e) {
            return ApiResponse.success(false);
        }
    }
    
    /**
     * 用户注销登录
     * 接口: member.php?mod=logging&action=logout&formhash={formhash}&mobile=2
     */
    public ApiResponse<Boolean> logout() {
        String formhash = getFormhash();
        if (formhash == null) {
            // 即使没有formhash也尝试清除本地cookie
            cookieManager.clearCookies();
            return ApiResponse.success(true);
        }
        
        String url = ApiConfig.BASE_URL + "member.php?mod=logging&action=logout&formhash=" + formhash + "&mobile=2";
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                // 清除本地Cookie
                cookieManager.clearCookies();
                return ApiResponse.success(true);
            }
        } catch (IOException e) {
            // 即使请求失败也清除本地cookie
            cookieManager.clearCookies();
            return ApiResponse.success(true);
        }
    }
    
    /**
     * 用户登录结果
     */
    public static class LoginResult {
        private boolean success;
        private String message;
        private String username;
        private String usergroup;
        private int uid;
        private int groupid;
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getUsergroup() { return usergroup; }
        public void setUsergroup(String usergroup) { this.usergroup = usergroup; }
        public int getUid() { return uid; }
        public void setUid(int uid) { this.uid = uid; }
        public int getGroupid() { return groupid; }
        public void setGroupid(int groupid) { this.groupid = groupid; }
    }
    
    /**
     * 用户名密码登录
     * 流程：1.获取登录页面提取formhash 2.提交用户名密码
     * @param username 用户名
     * @param password 密码
     */
    public ApiResponse<LoginResult> login(String username, String password) {
        try {
            // 第一步：获取登录页面，提取formhash
            String formhash = getLoginFormhash();
            if (formhash == null) {
                // 尝试使用通用formhash获取方法
                formhash = getFormhash();
                if (formhash == null) {
                    return ApiResponse.error("获取登录页面失败，请检查网络连接");
                }
            }
            
            // 第二步：提交登录
            return submitLogin(username, password, formhash);
        } catch (Exception e) {
            return ApiResponse.error("登录失败: " + e.getMessage());
        }
    }
    
    /**
     * 获取登录页面的formhash
     */
    private String getLoginFormhash() {
        // 尝试多个登录页面URL
        String[] urls = {
            ApiConfig.getLoginPageMobileUrl(),  // 移动端版本优先
            ApiConfig.getLoginPageUrl(),        // AJAX版本
            ApiConfig.BASE_URL + "member.php?mod=logging&action=login"  // 普通版本
        };
        
        for (String url : urls) {
            try {
                Request request = new Request.Builder()
                    .url(url)
                    .header("User-Agent", ApiConfig.USER_AGENT)
                    .header("Accept", "text/html,application/xhtml+xml,application/xml;q=0.9,*/*;q=0.8")
                    .header("Referer", ApiConfig.BASE_URL)
                    .build();
                
                try (Response response = client.newCall(request).execute()) {
                    if (!response.isSuccessful()) {
                        continue;
                    }
                    
                    String html = response.body().string();
                    
                    String formhash = extractFormhash(html);
                    if (formhash != null) {
                        return formhash;
                    }
                }
            } catch (IOException e) {
                // 继续尝试下一个URL
            }
        }
        
        return null;
    }
    
    /**
     * 从HTML中提取formhash
     */
    private String extractFormhash(String html) {
        if (html == null || html.isEmpty()) {
            return null;
        }
        
        // 尝试多种匹配模式提取formhash
        // 注意：MT论坛使用单引号 value='xxx'
        String[] patterns = {
            "name=[\"']formhash[\"'][^>]*value=[\"']([a-f0-9]{8})[\"']",  // name在前
            "value=[\"']([a-f0-9]{8})[\"'][^>]*name=[\"']formhash[\"']",  // value在前
            "<input[^>]*name=[\"']formhash[\"'][^>]*value=[\"']([a-f0-9]{8})[\"']",  // 完整input标签
            "<input[^>]*value=[\"']([a-f0-9]{8})[\"'][^>]*name=[\"']formhash[\"']",  // value在前完整标签
            "formhash=([a-f0-9]{8})",  // URL参数格式
            "formhash\\s*=\\s*[\"']([a-f0-9]{8})[\"']"  // JS赋值格式
        };
        
        for (String pattern : patterns) {
            Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(html);
            if (m.find()) {
                return m.group(1);
            }
        }
        
        return null;
    }
    
    /**
     * 提交登录请求
     */
    private ApiResponse<LoginResult> submitLogin(String username, String password, String formhash) {
        // 使用正确的登录URL格式
        String loginhash = generateLoginhash();
        String url = ApiConfig.getLoginSubmitUrl(loginhash);
        
        try {
            // URL编码
            String encodedUsername = java.net.URLEncoder.encode(username, "UTF-8");
            String encodedPassword = java.net.URLEncoder.encode(password, "UTF-8");
            String encodedReferer = java.net.URLEncoder.encode(ApiConfig.BASE_URL + "forum.php?mod=guide&view=hot&mobile=2", "UTF-8");
            
            // 构建POST数据
            String postData = "formhash=" + formhash +
                "&referer=" + encodedReferer +
                "&fastloginfield=username" +
                "&cookietime=31104000" +
                "&username=" + encodedUsername +
                "&password=" + encodedPassword +
                "&questionid=0" +
                "&answer=";
            
            okhttp3.RequestBody body = okhttp3.RequestBody.create(
                postData,
                okhttp3.MediaType.parse("application/x-www-form-urlencoded; charset=UTF-8")
            );
            
            Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Accept", "application/xml, text/xml, */*; q=0.01")
                .header("Referer", ApiConfig.getLoginPageMobileUrl())
                .header("Origin", ApiConfig.BASE_URL.substring(0, ApiConfig.BASE_URL.length() - 1))
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                String html = response.body().string();
                
                LoginResult result = parseLoginResponse(html);
                
                if (result.isSuccess()) {
                    return ApiResponse.success(result);
                } else {
                    return ApiResponse.error(result.getMessage());
                }
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 生成随机loginhash
     */
    private String generateLoginhash() {
        String chars = "ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz0123456789";
        StringBuilder sb = new StringBuilder();
        java.util.Random random = new java.util.Random();
        for (int i = 0; i < 5; i++) {
            sb.append(chars.charAt(random.nextInt(chars.length())));
        }
        return sb.toString();
    }
    
    /**
     * 解析登录响应
     */
    private LoginResult parseLoginResponse(String html) {
        LoginResult result = new LoginResult();
        
        // 提取CDATA内容
        Pattern cdataPattern = Pattern.compile("<\\!\\[CDATA\\[(.*?)\\]\\]>", Pattern.DOTALL);
        Matcher cdataMatcher = cdataPattern.matcher(html);
        String content = html;
        if (cdataMatcher.find()) {
            content = cdataMatcher.group(1);
        }
        
        // 检查是否登录成功 - 解析loginStatus JSON
        Pattern statusPattern = Pattern.compile(
            "var\\s+loginStatus\\s*=\\s*\\{[^}]*['\"]username['\"]\\s*:\\s*['\"]([^'\"]*)['\"][^}]*" +
            "['\"]usergroup['\"]\\s*:\\s*['\"]([^'\"]*)['\"][^}]*" +
            "['\"]uid['\"]\\s*:\\s*['\"]([^'\"]*)['\"][^}]*" +
            "['\"]groupid['\"]\\s*:\\s*['\"]([^'\"]*)['\"]"
        );
        
        Matcher statusMatcher = statusPattern.matcher(content);
        if (statusMatcher.find()) {
            result.setSuccess(true);
            result.setUsername(statusMatcher.group(1));
            result.setUsergroup(statusMatcher.group(2));
            try {
                result.setUid(Integer.parseInt(statusMatcher.group(3)));
            } catch (NumberFormatException e) {
                result.setUid(0);
            }
            try {
                result.setGroupid(Integer.parseInt(statusMatcher.group(4)));
            } catch (NumberFormatException e) {
                result.setGroupid(0);
            }
            result.setMessage("登录成功");
            return result;
        }
        
        // 检查成功提示 - 移动端格式
        if (content.contains("欢迎您回来") || content.contains("登录成功") || 
            content.contains("欢迎回来") || html.contains("cQWy_2132_auth")) {
            result.setSuccess(true);
            result.setMessage("登录成功");
            return result;
        }
        
        // 检查错误信息 - 多种格式
        String[] errorPatterns = {
            "class=\"[^\"]*error[^\"]*\"[^>]*>([^<]+)<",
            "class='[^']*error[^']*'[^>]*>([^<]+)<",
            "alert\\s*\\(\\s*['\"]([^'\"]+)['\"]\\s*\\)",
            "<p[^>]*class=\"[^\"]*notice[^\"]*\"[^>]*>([^<]+)<",
            "提示[：:]\\s*([^<\\n]+)"
        };
        
        for (String pattern : errorPatterns) {
            Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(content);
            if (m.find()) {
                result.setSuccess(false);
                String errorMsg = m.group(1);
                if (errorMsg != null && !errorMsg.trim().isEmpty()) {
                    result.setMessage(errorMsg.trim());
                    return result;
                }
            }
        }
        
        // 检查常见的错误关键字
        String lowerContent = content.toLowerCase();
        if (lowerContent.contains("密码错误") || lowerContent.contains("用户名不存在")) {
            result.setSuccess(false);
            result.setMessage("用户名或密码错误");
            return result;
        }
        if (lowerContent.contains("密码不正确")) {
            result.setSuccess(false);
            result.setMessage("密码不正确");
            return result;
        }
        if (lowerContent.contains("验证码")) {
            result.setSuccess(false);
            result.setMessage("需要验证码，请使用Cookie方式登录");
            return result;
        }
        
        // 默认失败
        result.setSuccess(false);
        result.setMessage("登录失败，请检查用户名和密码");
        return result;
    }
    
    /**
     * 从AJAX响应中解析用户列表
     * 格式: <li><a href="space-uid-xxx"><img src="avatar"></a><p>用户名</p>...</li>
     */
    private List<User> parseUserListFromAjax(String html) {
        List<User> users = new ArrayList<>();
        
        if (html == null || html.isEmpty()) {
            return users;
        }
        
        // 提取CDATA内容
        Pattern cdataPattern = Pattern.compile("<\\!\\[CDATA\\[(.*?)\\]\\]>", Pattern.DOTALL);
        Matcher cdataMatcher = cdataPattern.matcher(html);
        String content = html;
        if (cdataMatcher.find()) {
            content = cdataMatcher.group(1);
        }
        
        // 解析用户列表项
        // 格式: <li class="b_t" id="comiis_friendbox_xxx">
        //   <p class="ytit">...<a href="space-uid-xxx">...</p>
        //   <a href="space-uid-xxx"><img src="avatar"></a>
        //   <p>用户名</p>
        // </li>
        Pattern userPattern = Pattern.compile(
            "<li[^>]*id=\"comiis_friendbox_(\\d+)\"[^>]*>.*?" +
            "img[^>]*src=\"([^\"]+)\".*?" +  // 头像
            "href=\"[^\"]*uid=(\\d+)[^\"]*\"[^>]*>([^<]+)</a>",  // 用户名
            Pattern.DOTALL
        );
        
        Matcher userMatcher = userPattern.matcher(content);
        while (userMatcher.find()) {
            User user = new User();
            user.setUid(Integer.parseInt(userMatcher.group(1)));
            
            String avatar = userMatcher.group(2);
            if (avatar != null && !avatar.startsWith("http")) {
                avatar = "https://bbs.binmt.cc/" + avatar;
            }
            user.setAvatar(avatar);
            
            user.setUsername(userMatcher.group(4).trim());
            
            users.add(user);
        }
        
        return users;
    }
    
    /**
     * 获取版块帖子列表
     * @param fid 版块ID
     * @param page 页码 (从1开始)
     * @param filter 筛选类型: "all"全部, "lastpost"最新, "heat"热门, "digest"精华, "typeid:xxx"主题分类
     */
    public ApiResponse<ForumPostListResult> getForumPostList(int fid, int page, String filter) {
        String url;
        // 移动端使用 forum.php?mod=forumdisplay 格式
        if (filter == null || filter.isEmpty() || filter.equals("all")) {
            // 全部帖子 - 使用移动端格式
            url = ApiConfig.BASE_URL + "forum.php?mod=forumdisplay&fid=" + fid + "&page=" + page;
        } else if (filter.equals("lastpost")) {
            // 最新帖子
            url = ApiConfig.BASE_URL + "forum.php?mod=forumdisplay&fid=" + fid +
                  "&filter=lastpost&orderby=lastpost&page=" + page;
        } else if (filter.equals("heat")) {
            // 热门帖子
            url = ApiConfig.BASE_URL + "forum.php?mod=forumdisplay&fid=" + fid +
                  "&filter=heat&orderby=heats&page=" + page;
        } else if (filter.equals("digest")) {
            // 精华帖子
            url = ApiConfig.BASE_URL + "forum.php?mod=forumdisplay&fid=" + fid +
                  "&filter=digest&digest=1&page=" + page;
        } else if (filter.startsWith("typeid:")) {
            // 主题分类 (typeid:xxx)
            String typeId = filter.substring("typeid:".length());
            url = ApiConfig.BASE_URL + "forum.php?mod=forumdisplay&fid=" + fid +
                  "&filter=typeid&typeid=" + typeId + "&page=" + page;
        } else {
            url = ApiConfig.BASE_URL + "forum.php?mod=forumdisplay&fid=" + fid + "&page=" + page;
        }

        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String html = response.body().string();
                
                if (html.isEmpty()) {
                    return ApiResponse.error("响应为空");
                }
                
                // 解析版块信息和帖子列表
                ForumInfo forumInfo = HtmlParser.parseForumPostList(html);
                
                // 同时获取普通帖子列表
                List<Post> allPosts = HtmlParser.parsePostList(html);
                
                ForumPostListResult result = new ForumPostListResult();
                result.setForumInfo(forumInfo);
                result.setPosts(allPosts);
                result.setTopPosts(forumInfo.getTopPosts());
                
                if (allPosts.isEmpty() && forumInfo.getTopPosts().isEmpty()) {
                    return ApiResponse.error("未找到帖子");
                }
                
                return ApiResponse.success(result);
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取版块帖子列表 (默认全部帖子)
     */
    public ApiResponse<ForumPostListResult> getForumPostList(int fid, int page) {
        return getForumPostList(fid, page, "all");
    }
    
    /**
     * 版块帖子列表结果
     */
    public static class ForumPostListResult {
        private ForumInfo forumInfo;
        private List<Post> posts;
        private List<Post> topPosts;
        
        public ForumInfo getForumInfo() { return forumInfo; }
        public void setForumInfo(ForumInfo forumInfo) { this.forumInfo = forumInfo; }
        
        public List<Post> getPosts() { return posts; }
        public void setPosts(List<Post> posts) { this.posts = posts; }
        
        public List<Post> getTopPosts() { return topPosts; }
        public void setTopPosts(List<Post> topPosts) { this.topPosts = topPosts; }
    }
    
    /**
     * 用户列表结果
     */
    public static class FriendListResult {
        private int count;
        private List<User> users;
        
        public int getCount() { return count; }
        public void setCount(int count) { this.count = count; }
        
        public List<User> getUsers() { return users; }
        public void setUsers(List<User> users) { this.users = users; }
    }
    
    /**
     * 关注用户
     * @param fuid 用户ID
     * @return 是否成功
     */
    public ApiResponse<Boolean> followUser(int fuid) {
        String formhash = getFormhash();
        if (formhash == null) {
            return ApiResponse.error("获取formhash失败，请先登录");
        }
        
        String url = ApiConfig.BASE_URL + "home.php?mod=spacecp&ac=follow&op=add" +
                "&fuid=" + fuid + "&hash=" + formhash + "&inajax=1";
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .header("X-Requested-With", "XMLHttpRequest")
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String result = response.body().string();
                
                if (result.contains("成功收听") || result.contains("关注成功")) {
                    return ApiResponse.success(true);
                } else if (result.contains("已关注") || result.contains("已经收听")) {
                    return ApiResponse.success(true);
                } else {
                    return ApiResponse.error("关注失败");
                }
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 取消关注用户
     * @param fuid 用户ID
     * @return 是否成功
     */
    public ApiResponse<Boolean> unfollowUser(int fuid) {
        String formhash = getFormhash();
        if (formhash == null) {
            return ApiResponse.error("获取formhash失败，请先登录");
        }
        
        String url = ApiConfig.BASE_URL + "home.php?mod=spacecp&ac=follow&op=del" +
                "&fuid=" + fuid + "&hash=" + formhash + "&inajax=1";
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .header("X-Requested-With", "XMLHttpRequest")
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String result = response.body().string();
                
                if (result.contains("取消成功") || result.contains("成功取消")) {
                    return ApiResponse.success(true);
                } else {
                    return ApiResponse.error("取消关注失败");
                }
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 点赞帖子
     * @param tid 帖子ID
     * @return 点赞结果，包含点赞数等信息
     */
    public ApiResponse<RecommendResult> recommendPost(int tid) {
        String formhash = getFormhash();
        if (formhash == null) {
            return ApiResponse.error("获取formhash失败，请先登录");
        }
        
        String url = ApiConfig.BASE_URL + "forum.php?mod=misc&action=recommend" +
                "&handlekey=recommend_add&do=add&tid=" + tid + "&hash=" + formhash + "&inajax=1";
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.getThreadUrl(tid))
                .header("X-Requested-With", "XMLHttpRequest")
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String result = response.body().string();
                
                // 解析响应
                RecommendResult recommendResult = new RecommendResult();
                
                // 检查是否已点赞
                if (result.contains("您已评价过本主题")) {
                    recommendResult.setAlreadyRecommended(true);
                    recommendResult.setCount(parseRecommendCount(result));
                    return ApiResponse.success(recommendResult);
                }
                
                // 检查是否成功点赞（放宽判断条件）
                if (result.contains("+1") || result.contains("评价指数") ||
                    result.contains("成功") || result.length() > 10) {
                    recommendResult.setAlreadyRecommended(true);
                    recommendResult.setCount(parseRecommendCount(result));
                    // 检查剩余次数
                    if (result.contains("您今天还能评价")) {
                        recommendResult.setRemaining(parseRemainingCount(result));
                    }
                    return ApiResponse.success(recommendResult);
                }
                
                // 其他情况视为失败
                return ApiResponse.error("点赞失败");
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 取消点赞帖子
     * 使用comiis_app插件的re_recommend接口
     * @param tid 帖子ID
     * @return 是否成功
     */
    public ApiResponse<Boolean> unrecommendPost(int tid) {
        String url = ApiConfig.BASE_URL + "plugin.php?id=comiis_app&comiis=re_recommend&tid=" + tid + "&inajax=1";
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.getThreadUrl(tid))
                .header("X-Requested-With", "XMLHttpRequest")
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String result = response.body().string();
                
                // 成功取消返回 "<p>ok</p>"
                if (result.contains("<p>ok</p>") || result.contains(">ok<") || result.length() > 10) {
                    return ApiResponse.success(true);
                }
                
                return ApiResponse.error("取消点赞失败");
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 收藏帖子
     * @param tid 帖子ID
     * @param description 收藏描述（可选）
     * @return 是否成功
     */
    public ApiResponse<Boolean> favoritePost(int tid, String description) {
        String formhash = getFormhash();
        if (formhash == null) {
            return ApiResponse.error("获取formhash失败，请先登录");
        }
        
        // 首先检查是否已收藏
        ApiResponse<Boolean> checkResult = checkFavoriteStatus(tid);
        if (checkResult.isSuccess() && checkResult.getData()) {
            return ApiResponse.error("您已收藏，请勿重复收藏");
        }
        
        // 构建POST数据
        String referer = ApiConfig.getThreadUrl(tid) + "?mobile=2";
        String postData;
        try {
            postData = "favoritesubmit=true&referer=" + java.net.URLEncoder.encode(referer, "UTF-8") +
                    "&formhash=" + formhash + "&handlekey=favorite_add";
            if (description != null && !description.isEmpty()) {
                postData += "&description=" + java.net.URLEncoder.encode(description, "UTF-8");
            }
        } catch (java.io.UnsupportedEncodingException e) {
            return ApiResponse.error("编码错误: " + e.getMessage());
        }
        
        String url = ApiConfig.BASE_URL + "home.php?mod=spacecp&ac=favorite&type=thread&id=" + tid +
                "&spaceuid=&mobile=2&handlekey=favoriteform_" + tid + "&inajax=1";
        
        try {
            okhttp3.RequestBody body = okhttp3.RequestBody.create(
                okhttp3.MediaType.parse("application/x-www-form-urlencoded; charset=UTF-8"),
                postData
            );
            
            Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", referer)
                .header("X-Requested-With", "XMLHttpRequest")
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String result = response.body().string();
                
                if (result.contains("收藏成功") || result.contains("信息收藏成功")) {
                    return ApiResponse.success(true);
                } else if (result.contains("已收藏")) {
                    return ApiResponse.error("您已收藏，请勿重复收藏");
                } else {
                    return ApiResponse.error("收藏失败");
                }
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        } catch (Exception e) {
            return ApiResponse.error("错误: " + e.getMessage());
        }
    }
    
    /**
     * 取消收藏帖子
     * @param tid 帖子ID
     * @return 是否成功
     */
    public ApiResponse<Boolean> unfavoritePost(int tid) {
        String formhash = getFormhash();
        if (formhash == null) {
            return ApiResponse.error("获取formhash失败，请先登录");
        }
        
        String url = ApiConfig.BASE_URL + "home.php?mod=spacecp&ac=favorite&op=del&type=thread&id=" + tid +
                "&hash=" + formhash + "&inajax=1";
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .header("X-Requested-With", "XMLHttpRequest")
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String result = response.body().string();
                
                if (result.contains("取消收藏") || result.contains("删除收藏")) {
                    return ApiResponse.success(true);
                } else {
                    return ApiResponse.error("取消收藏失败");
                }
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 检查收藏状态（公开方法）
     * @param tid 帖子ID
     * @return 是否已收藏
     */
    public ApiResponse<Boolean> checkFavoriteStatus(int tid) {
        String url = ApiConfig.BASE_URL + "home.php?mod=spacecp&ac=favorite&type=thread&id=" + tid +
                "&handlekey=favorite_thread&inajax=1";
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .header("X-Requested-With", "XMLHttpRequest")
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.success(false);
                }
                
                String result = response.body().string();
                
                // 如果返回"已收藏"信息，说明已收藏
                if (result.contains("您已收藏")) {
                    return ApiResponse.success(true);
                }
                
                return ApiResponse.success(false);
            }
        } catch (IOException e) {
            return ApiResponse.success(false);
        }
    }
    
    /**
     * 获取我的回复列表
     * 接口: home.php?mod=space&uid={uid}&do=thread&view=me&type=reply&from=space
     * @param page 页码
     * @return 回复列表（包含帖子信息和回复摘要）
     */
    public ApiResponse<List<Post>> getMyReplies(int page) {
        // 获取当前用户UID
        String uid = cookieManager.getCookieValue(ApiConfig.COOKIE_PREFIX + "auth");
        // 从页面获取UID更可靠
        String url = ApiConfig.BASE_URL + "home.php?mod=space&do=thread&view=me&type=reply&from=space";
        if (page > 1) {
            url += "&page=" + page;
        }
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String html = response.body().string();
                
                // 检查是否需要登录
                if (html.contains("请先登录") || html.contains("您需要登录")) {
                    return ApiResponse.error("请先登录");
                }
                
                if (html.isEmpty()) {
                    return ApiResponse.error("响应为空");
                }
                
                List<Post> posts = HtmlParser.parseMyReplies(html);
                
                return ApiResponse.success(posts);
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 检查点赞状态
     * 使用plugin API检查当前用户是否已点赞该帖子
     * @param tid 帖子ID
     * @return 是否已点赞
     */
    public ApiResponse<Boolean> checkRecommendStatus(int tid) {
        String url = ApiConfig.BASE_URL + "plugin.php?id=comiis_app&comiis=re_recommend&tid=" + tid + "&inajax=1";
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL + "thread-" + tid + "-1-1.html")
                .header("X-Requested-With", "XMLHttpRequest")
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.success(false);
                }
                
                String result = response.body().string();
                
                // 如果返回"ok"，说明已点赞
                if (result.contains(">ok<") || result.contains(">ok<")) {
                    return ApiResponse.success(true);
                }
                
                return ApiResponse.success(false);
            }
        } catch (IOException e) {
            return ApiResponse.success(false);
        }
    }
    
    /**
     * 解析点赞数
     */
    private int parseRecommendCount(String html) {
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("评价指数\\s*<[^>]*>(\\d+)");
            java.util.regex.Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        } catch (Exception e) {
            // ignore
        }
        return 0;
    }
    
    /**
     * 解析剩余点赞次数
     */
    private int parseRemainingCount(String html) {
        try {
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("您今天还能评价\\s*(\\d+)\\s*次");
            java.util.regex.Matcher matcher = pattern.matcher(html);
            if (matcher.find()) {
                return Integer.parseInt(matcher.group(1));
            }
        } catch (Exception e) {
            // ignore
        }
        return 0;
    }
    
    /**
     * 执行签到
     * 分两步：
     * 1. 获取签到页面，提取formhash
     * 2. 提交签到请求
     * 
     * @return 签到结果
     */
    public ApiResponse<SignResult> performSign() {
        // 第一步：获取签到页面，提取formhash
        String formhash = getSignFormhash();
        if (formhash == null) {
            return ApiResponse.error("获取签到参数失败，请先登录");
        }
        
        // 第二步：提交签到请求
        return submitSign(formhash);
    }
    
    /**
     * 获取签到页面的formhash
     */
    private String getSignFormhash() {
        String url = ApiConfig.getSignPageUrl();
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return null;
                }
                
                String html = response.body().string();
                if (html.isEmpty()) {
                    return null;
                }
                
                // 检查是否需要登录
                if (html.contains("请先登录") || html.contains("您需要登录") || html.contains("未登录")) {
                    return null;
                }
                
                // 尝试多种匹配模式提取formhash
                String[] patterns = {
                    "formhash=([a-f0-9]{8})",                            // URL参数格式（优先匹配）
                    "name=\"formhash\"\\s+value=\"([^\"]+)\"",           // 标准格式
                    "name='formhash'\\s+value='([^']+)'",               // 单引号格式
                    "formhash\\s*=\\s*['\"]([^'\"]+)['\"]",             // 简化格式
                    "value=\"([a-f0-9]{8})\".*?name=\"formhash\"",     // 值在前
                    "<input[^>]*name=\"formhash\"[^>]*value=\"([^\"]+)\""  // 完整input标签
                };
                
                for (String patternStr : patterns) {
                    try {
                        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(patternStr, java.util.regex.Pattern.CASE_INSENSITIVE);
                        java.util.regex.Matcher matcher = pattern.matcher(html);
                        if (matcher.find()) {
                            return matcher.group(1);
                        }
                    } catch (Exception e) {
                        // 继续尝试下一个模式
                    }
                }
                
                return null;
            }
        } catch (IOException e) {
            return null;
        }
    }
    
    /**
     * 提交签到请求
     */
    private ApiResponse<SignResult> submitSign(String formhash) {
        String url = ApiConfig.getSignSubmitUrl(formhash);
        
        try {
            // 使用GET方法
            Request request = new Request.Builder()
                .url(url)
                .get()
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.getSignPageUrl())
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "签到请求失败: " + response.code());
                }
                
                String result = response.body().string();
                
                // 解析签到结果
                SignResult signResult = parseSignResult(result);
                
                return ApiResponse.success(signResult);
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 检查签到状态
     * 直接检查签到页面是否有"已签到"提示
     * @return 签到状态
     */
    public ApiResponse<SignStatus> checkSignStatus() {
        String url = ApiConfig.getSignPageUrl();
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String html = response.body().string();
                
                // 解析签到状态
                SignStatus status = parseSignStatus(html);
                
                return ApiResponse.success(status);
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 解析签到状态
     * 从签到页面HTML中检查是否已签到
     */
    private SignStatus parseSignStatus(String html) {
        SignStatus status = new SignStatus();
        
        if (html == null || html.isEmpty()) {
            status.setSignedToday(false);
            return status;
        }
        
        // 检查是否需要登录
        if (html.contains("请先登录") || html.contains("您需要登录") || html.contains("未登录")) {
            status.setSignedToday(false);
            return status;
        }
        
        // 检查是否已签到 - 多种匹配模式
        boolean signedToday = html.contains("已签到") 
            || html.contains("今日已签")
            || html.contains("已经签到")
            || html.contains("您今天已签到")
            || html.contains("class=\"qd\")")
            || html.contains("class='qd')")
            || html.contains("qiandaoed")
            || html.contains("signed");
        
        status.setSignedToday(signedToday);
        
        // 尝试提取总签到天数
        if (signedToday) {
            try {
                java.util.regex.Pattern daysPattern = java.util.regex.Pattern.compile(
                    "(?:总天数|累计签到|连续签到)[^\\d]*(\\d+)");
                java.util.regex.Matcher matcher = daysPattern.matcher(html);
                if (matcher.find()) {
                    status.setTotalDays(Integer.parseInt(matcher.group(1)));
                }
            } catch (Exception e) {
                // 忽略解析错误
            }
        }
        
        return status;
    }
    
    /**
     * 签到状态
     */
    public static class SignStatus {
        private boolean signedToday;
        private int rank;
        private int totalDays;
        
        public boolean isSignedToday() {
            return signedToday;
        }
        
        public void setSignedToday(boolean signedToday) {
            this.signedToday = signedToday;
        }
        
        public int getRank() {
            return rank;
        }
        
        public void setRank(int rank) {
            this.rank = rank;
        }
        
        public int getTotalDays() {
            return totalDays;
        }
        
        public void setTotalDays(int totalDays) {
            this.totalDays = totalDays;
        }
    }
    
    /**
     * 解析签到结果
     * 响应格式可能是:
     * - XML格式: <?xml version="1.0" encoding="utf-8"?><root><![CDATA[签到成功，获得10积分！]]></root>
     * - 或: <root><签到成功>恭喜您签到成功！获得奖励！</root>
     * - 或: <root><已签>您今天已经签到过了！</root>
     */
    private SignResult parseSignResult(String xml) {
        SignResult result = new SignResult();
        
        if (xml == null || xml.isEmpty()) {
            result.setSuccess(false);
            result.setMessage("签到响应为空");
            return result;
        }
        
        // 清理XML声明
        String content = xml.replace("<?xml version=\"1.0\" encoding=\"utf-8\"?>", "").trim();
        
        // 提取CDATA内容（优先处理）
        java.util.regex.Pattern cdataPattern = java.util.regex.Pattern.compile(
            "<\\!\\[CDATA\\[(.*?)\\]\\]>", java.util.regex.Pattern.DOTALL);
        java.util.regex.Matcher cdataMatcher = cdataPattern.matcher(content);
        if (cdataMatcher.find()) {
            content = cdataMatcher.group(1).trim();
        } else {
            // 非CDATA格式，尝试提取root标签内容
            java.util.regex.Pattern rootPattern = java.util.regex.Pattern.compile(
                "<root>(.*?)</root>", java.util.regex.Pattern.DOTALL);
            java.util.regex.Matcher rootMatcher = rootPattern.matcher(content);
            if (rootMatcher.find()) {
                content = rootMatcher.group(1).trim();
            }
        }
        
        // 清理HTML标签
        content = content.replaceAll("<[^>]+>", "").trim();
        
        // 判断签到结果
        if (content.contains("今日已签") || content.contains("重复签到") || 
            content.contains("已经签到") || content.contains("已签到")) {
            result.setSuccess(true);
            result.setMessage("今日已签到");
            result.setAlreadySigned(true);
        } else if (content.contains("签到成功") || content.contains("获得") || content.contains("恭喜")) {
            result.setSuccess(true);
            result.setMessage("签到成功");
            
            // 提取奖励信息
            java.util.regex.Pattern rewardPattern = java.util.regex.Pattern.compile(
                "(\\d+\\s*积分|\\d+\\s*金币)"
            );
            java.util.regex.Matcher rewardMatcher = rewardPattern.matcher(content);
            if (rewardMatcher.find()) {
                result.setReward(rewardMatcher.group(1));
            }
        } else if (content.contains("登录") || content.contains("login")) {
            result.setSuccess(false);
            result.setMessage("请先登录");
        } else if (!content.isEmpty()) {
            // 如果有内容但无法判断，直接显示
            result.setSuccess(true);
            result.setMessage(content.length() > 50 ? content.substring(0, 50) : content);
        } else {
            result.setSuccess(false);
            result.setMessage("签到失败，请重试");
        }
        
        return result;
    }
    
    /**
     * 点赞结果
     */
    public static class RecommendResult {
        private boolean alreadyRecommended;
        private int count;
        private int remaining;
        
        public boolean isAlreadyRecommended() {
            return alreadyRecommended;
        }
        
        public void setAlreadyRecommended(boolean alreadyRecommended) {
            this.alreadyRecommended = alreadyRecommended;
        }
        
        public int getCount() {
            return count;
        }
        
        public void setCount(int count) {
            this.count = count;
        }
        
        public int getRemaining() {
            return remaining;
        }
        
        public void setRemaining(int remaining) {
            this.remaining = remaining;
        }
    }
    
    /**
     * 签到结果
     */
    public static class SignResult {
        private boolean success;
        private boolean alreadySigned;
        private String message;
        private String reward;
        
        public boolean isSuccess() {
            return success;
        }
        
        public void setSuccess(boolean success) {
            this.success = success;
        }
        
        public boolean isAlreadySigned() {
            return alreadySigned;
        }
        
        public void setAlreadySigned(boolean alreadySigned) {
            this.alreadySigned = alreadySigned;
        }
        
        public String getMessage() {
            return message;
        }
        
        public void setMessage(String message) {
            this.message = message;
        }
        
        public String getReward() {
            return reward;
        }
        
        public void setReward(String reward) {
            this.reward = reward;
        }
    }
    
    /**
     * 搜索帖子
     * 流程: POST提交搜索 -> 302重定向到搜索结果页 -> GET获取结果
     * 注意: OkHttp对POST的302重定向处理可能不一致，需要手动跟随
     * @param keyword 搜索关键词
     * @param page 页码
     * @return SearchPostResult 包含帖子列表、searchid和分页信息
     */
    public ApiResponse<com.forum.mt.model.SearchPostResult> searchPosts(String keyword, int page) {
        try {
            // 获取formhash
            String formhash = getFormhash();
            if (formhash == null) {
                formhash = "b0be9684"; // 使用默认值
            }

            // 如果是第一页，需要先提交搜索
            if (page <= 1) {
                // 重置搜索状态
                currentSearchKeyword = keyword;
                currentSearchId = null;
                currentSearchTotalPages = 1;

                String searchUrl = ApiConfig.BASE_URL + "search.php?mod=forum&mobile=2";
                String postData = "formhash=" + formhash + "&searchsubmit=yes&mod=forum&srchtxt=" +
                    java.net.URLEncoder.encode(keyword, "UTF-8");

                okhttp3.RequestBody body = okhttp3.RequestBody.create(
                    okhttp3.MediaType.parse("application/x-www-form-urlencoded; charset=UTF-8"),
                    postData
                );

                Request searchRequest = new Request.Builder()
                    .url(searchUrl)
                    .post(body)
                    .header("User-Agent", ApiConfig.USER_AGENT)
                    .header("Referer", ApiConfig.BASE_URL)
                    .build();

                String finalUrl = null;

                try (Response searchResponse = client.newCall(searchRequest).execute()) {
                    // 检查是否重定向
                    if (searchResponse.isRedirect() || searchResponse.code() == 302 || searchResponse.code() == 301) {
                        String location = searchResponse.header("Location");
                        if (location != null) {
                            // 处理相对URL
                            if (location.startsWith("/")) {
                                finalUrl = ApiConfig.BASE_URL + location.substring(1);
                            } else if (!location.startsWith("http")) {
                                finalUrl = ApiConfig.BASE_URL + location;
                            } else {
                                finalUrl = location;
                            }
                        }
                    } else if (searchResponse.isSuccessful()) {
                        // OkHttp 可能已经跟随重定向，直接解析响应
                        String html = searchResponse.body().string();
                        if (html.contains("comiis_forumlist") || html.contains("forumlist_li")) {
                            List<Post> posts = HtmlParser.parseSearchPostResults(html);
                            int[] pageInfo = HtmlParser.parseSearchPagination(html);
                            // 从实际响应的 URL 中提取 searchid
                            String actualSearchId = extractSearchId(searchResponse.request().url().toString());
                            if (actualSearchId != null) {
                                currentSearchId = actualSearchId;
                            }
                            currentSearchTotalPages = pageInfo[1];
                            com.forum.mt.model.SearchPostResult result = new com.forum.mt.model.SearchPostResult(
                                posts, actualSearchId, pageInfo[0], pageInfo[1]
                            );
                            return ApiResponse.success(result);
                        }
                    }
                }

                // 如果获取到了重定向URL，手动请求搜索结果
                if (finalUrl != null) {
                    return fetchSearchResultsByUrl(finalUrl);
                }

                return ApiResponse.error("搜索失败，请重试");
            } else {
                // 使用已保存的searchid进行分页搜索
                if (currentSearchId == null) {
                    return ApiResponse.error("请重新搜索");
                }
                if (!keyword.equals(currentSearchKeyword)) {
                    return ApiResponse.error("搜索关键词已变化，请重新搜索");
                }
                return fetchSearchResults(currentSearchId, keyword, page);
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 从URL中提取searchid
     */
    private String extractSearchId(String url) {
        if (url == null) return null;
        try {
            java.net.URL parsedUrl = new java.net.URL(url);
            String query = parsedUrl.getQuery();
            if (query != null) {
                String[] params = query.split("&");
                for (String param : params) {
                    if (param.startsWith("searchid=")) {
                        return param.substring(9);
                    }
                }
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
        return null;
    }
    
    /**
     * 通过searchid获取搜索结果
     */
    private ApiResponse<com.forum.mt.model.SearchPostResult> fetchSearchResults(String searchId, String keyword, int page) {
        String url = ApiConfig.BASE_URL + "search.php?mod=forum&searchid=" + searchId +
            "&orderby=lastpost&ascdesc=desc&searchsubmit=yes&kw=" +
            java.net.URLEncoder.encode(keyword, java.nio.charset.StandardCharsets.UTF_8) +
            "&mobile=2&page=" + page;

        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败");
                }

                String html = response.body().string();
                if (html.isEmpty()) {
                    return ApiResponse.error("响应为空");
                }

                List<Post> posts = HtmlParser.parseSearchPostResults(html);
                int[] pageInfo = HtmlParser.parseSearchPagination(html);

                if (posts.isEmpty()) {
                    return ApiResponse.error("未找到相关帖子");
                }

                com.forum.mt.model.SearchPostResult result = new com.forum.mt.model.SearchPostResult(
                    posts, searchId, pageInfo[0], pageInfo[1]
                );
                return ApiResponse.success(result);
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 直接通过URL获取搜索结果
     */
    private ApiResponse<com.forum.mt.model.SearchPostResult> fetchSearchResultsByUrl(String url) {
        try {
            // 确保URL包含 mobile=2 参数
            if (!url.contains("mobile=2")) {
                if (url.contains("?")) {
                    url += "&mobile=2";
                } else {
                    url += "?mobile=2";
                }
            }

            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .build();

            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败");
                }

                String html = response.body().string();
                if (html.isEmpty()) {
                    return ApiResponse.error("响应为空");
                }

                List<Post> posts = HtmlParser.parseSearchPostResults(html);
                int[] pageInfo = HtmlParser.parseSearchPagination(html);
                // 从实际响应的 URL 中提取 searchid（处理 OkHttp 可能自动跟随重定向的情况）
                String actualUrl = response.request().url().toString();
                String searchId = extractSearchId(actualUrl);

                // 保存搜索状态
                if (searchId != null) {
                    currentSearchId = searchId;
                    currentSearchTotalPages = pageInfo[1];
                }

                if (posts.isEmpty()) {
                    return ApiResponse.error("未找到相关帖子");
                }

                com.forum.mt.model.SearchPostResult result = new com.forum.mt.model.SearchPostResult(
                    posts, searchId, pageInfo[0], pageInfo[1]
                );
                return ApiResponse.success(result);
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 搜索用户
     * 流程: POST提交搜索 -> 302重定向 -> GET获取结果页
     * @param keyword 搜索关键词 (用户名)
     * @param page 页码 (暂不支持分页)
     */
    public ApiResponse<List<User>> searchUsers(String keyword, int page) {
        try {
            // 获取formhash
            String formhash = getFormhash();
            if (formhash == null) {
                formhash = "b0be9684"; // 使用默认值
            }
            
            // 提交用户搜索
            String searchUrl = ApiConfig.BASE_URL + "search.php?mod=forum";
            String postData = "formhash=" + formhash + "&searchsubmit=yes&mod=user&srchtxt=" + 
                java.net.URLEncoder.encode(keyword, "UTF-8");
            
            okhttp3.RequestBody body = okhttp3.RequestBody.create(
                okhttp3.MediaType.parse("application/x-www-form-urlencoded; charset=UTF-8"),
                postData
            );
            
            Request searchRequest = new Request.Builder()
                .url(searchUrl)
                .post(body)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .build();
            
            String resultUrl;
            try (Response searchResponse = client.newCall(searchRequest).execute()) {
                // 检查重定向
                if (searchResponse.isRedirect()) {
                    String location = searchResponse.header("Location");
                    if (location != null) {
                        if (location.startsWith("/")) {
                            resultUrl = ApiConfig.BASE_URL + location.substring(1);
                        } else {
                            resultUrl = location;
                        }
                    } else {
                        return ApiResponse.error("搜索失败");
                    }
                } else {
                    // 可能直接返回结果
                    String html = searchResponse.body().string();
                    List<User> users = HtmlParser.parseSearchUserResults(html);
                    if (users.isEmpty()) {
                        return ApiResponse.error("未找到相关用户");
                    }
                    return ApiResponse.success(users);
                }
            }
            
            // 获取搜索结果页
            Request resultRequest = new Request.Builder()
                .url(resultUrl)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .build();
            
            try (Response response = client.newCall(resultRequest).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败");
                }
                
                String html = response.body().string();
                if (html.isEmpty()) {
                    return ApiResponse.error("响应为空");
                }
                
                List<User> users = HtmlParser.parseSearchUserResults(html);
                
                if (users.isEmpty()) {
                    return ApiResponse.error("未找到相关用户");
                }
                
                return ApiResponse.success(users);
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 发表回复
     * 接口: POST /forum.php?mod=post&action=reply&fid={fid}&tid={tid}&replysubmit=yes&mobile=2&handlekey=fastpost&loc=1&inajax=1
     * @param fid 版块ID
     * @param tid 帖子ID
     * @param message 回复内容
     * @param formhash 表单验证hash
     * @param noticeauthor 通知作者token (可选，回复楼主时需要)
     * @return 回复结果
     */
    public ApiResponse<ReplyResult> postReply(int fid, int tid, String message, String formhash, String noticeauthor) {
        return postReply(fid, tid, message, formhash, noticeauthor, null, 0);
    }
    
    /**
     * 发表回复 (带附件)
     * @param fid 版块ID
     * @param tid 帖子ID
     * @param message 回复内容
     * @param formhash 表单验证hash
     * @param noticeauthor 通知作者token (可选，回复楼主时需要)
     * @param attachIds 附件ID列表 (可选，图片上传后获取)
     * @return 回复结果
     */
    public ApiResponse<ReplyResult> postReply(int fid, int tid, String message, String formhash, 
                                                String noticeauthor, java.util.List<Integer> attachIds) {
        return postReply(fid, tid, message, formhash, noticeauthor, attachIds, 0);
    }
    
    /**
     * 发表回复 (完整版，支持回复指定楼层)
     * @param fid 版块ID
     * @param tid 帖子ID
     * @param message 回复内容
     * @param formhash 表单验证hash
     * @param noticeauthor 通知作者token (可选，回复楼主时需要)
     * @param attachIds 附件ID列表 (可选，图片上传后获取)
     * @param replyPid 要回复的评论pid (可选，回复指定楼层时需要)
     * @return 回复结果
     */
    public ApiResponse<ReplyResult> postReply(int fid, int tid, String message, String formhash, 
                                                String noticeauthor, java.util.List<Integer> attachIds,
                                                int replyPid) {
        if (formhash == null || formhash.isEmpty()) {
            formhash = getFormhash();
            if (formhash == null) {
                return ApiResponse.error("获取formhash失败，请先登录");
            }
        }
        
        // 构建URL - 使用移动端标识 mobile=2
        StringBuilder urlBuilder = new StringBuilder(ApiConfig.BASE_URL + "forum.php?mod=post&action=reply");
        urlBuilder.append("&fid=").append(fid);
        urlBuilder.append("&tid=").append(tid);
        urlBuilder.append("&extra=page%3D1");
        // 添加repquote参数（回复指定楼层时需要）
        if (replyPid > 0) {
            urlBuilder.append("&repquote=").append(replyPid);
        }
        urlBuilder.append("&replysubmit=yes");
        urlBuilder.append("&mobile=2");
        urlBuilder.append("&handlekey=fastpost");
        urlBuilder.append("&loc=1");
        urlBuilder.append("&inajax=1");
        
        String url = urlBuilder.toString();
        
        // 构建POST数据
        StringBuilder postData = new StringBuilder();
        postData.append("formhash=").append(formhash);
        
        // 添加通知作者token (回复楼主时需要)
        if (noticeauthor != null && !noticeauthor.isEmpty()) {
            postData.append("&noticeauthor=").append(noticeauthor);
        }
        
        // 添加附件ID
        if (attachIds != null && !attachIds.isEmpty()) {
            for (Integer aid : attachIds) {
                // 格式: attachnew[aid][description]=
                postData.append("&attachnew[").append(aid).append("][description]=");
            }
            // 同时在消息中追加图片代码
            StringBuilder attachCode = new StringBuilder();
            for (Integer aid : attachIds) {
                attachCode.append("[attachimg]").append(aid).append("[/attachimg]");
            }
            message = message + "\n" + attachCode.toString();
        }
        
        // 添加回复内容
        try {
            postData.append("&message=").append(java.net.URLEncoder.encode(message, "UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            postData.append("&message=").append(message);
        }
        
        try {
            okhttp3.RequestBody body = okhttp3.RequestBody.create(
                postData.toString(),
                okhttp3.MediaType.parse("application/x-www-form-urlencoded; charset=UTF-8")
            );
            
            Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.getThreadUrl(tid))
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String result = response.body().string();
                
                // 解析响应
                ReplyResult replyResult = parseReplyResponse(result);
                
                if (replyResult.isSuccess()) {
                    return ApiResponse.success(replyResult);
                } else {
                    return ApiResponse.error(replyResult.getMessage());
                }
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 解析回复响应
     */
    private ReplyResult parseReplyResponse(String html) {
        ReplyResult result = new ReplyResult();
        
        // 提取CDATA内容
        Pattern cdataPattern = Pattern.compile("<\\!\\[CDATA\\[(.*?)\\]\\]>", Pattern.DOTALL);
        Matcher cdataMatcher = cdataPattern.matcher(html);
        String content = html;
        if (cdataMatcher.find()) {
            content = cdataMatcher.group(1);
        }
        
        // 检查是否回复成功
        if (content.contains("回复发布成功") || content.contains("发布成功")) {
            result.setSuccess(true);
            result.setMessage("回复成功");
            
            // 尝试提取新回复的pid
            Pattern pidPattern = Pattern.compile("pid=(\\d+)");
            Matcher pidMatcher = pidPattern.matcher(content);
            if (pidMatcher.find()) {
                try {
                    result.setPid(Integer.parseInt(pidMatcher.group(1)));
                } catch (NumberFormatException ignored) {}
            }
            
            // 尝试提取跳转URL
            Pattern urlPattern = Pattern.compile("href=\"([^\"]+viewthread[^\"]+)\"");
            Matcher urlMatcher = urlPattern.matcher(content);
            if (urlMatcher.find()) {
                result.setRedirectUrl(urlMatcher.group(1).replace("&amp;", "&"));
            }
        } else if (content.contains("需要先登录") || content.contains("请先登录")) {
            result.setSuccess(false);
            result.setMessage("请先登录");
        } else if (content.contains("您发表评论过快")) {
            result.setSuccess(false);
            result.setMessage("发表过快，请稍后再试");
        } else if (content.contains("内容太短")) {
            result.setSuccess(false);
            result.setMessage("回复内容太短");
        } else if (content.contains("已经锁定")) {
            result.setSuccess(false);
            result.setMessage("帖子已锁定，无法回复");
        } else {
            // 尝试提取其他错误信息
            Pattern errorPattern = Pattern.compile("<p[^>]*>([^<]+)</p>");
            Matcher errorMatcher = errorPattern.matcher(content);
            if (errorMatcher.find()) {
                String errorMsg = errorMatcher.group(1).trim();
                if (!errorMsg.isEmpty()) {
                    result.setSuccess(false);
                    result.setMessage(errorMsg);
                    return result;
                }
            }
            
            result.setSuccess(false);
            result.setMessage("回复失败，请重试");
        }
        
        return result;
    }
    
    /**
     * 回复结果
     */
    public static class ReplyResult {
        private boolean success;
        private String message;
        private int pid;           // 新回复的ID
        private String redirectUrl; // 跳转URL
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
        public int getPid() { return pid; }
        public void setPid(int pid) { this.pid = pid; }
        public String getRedirectUrl() { return redirectUrl; }
        public void setRedirectUrl(String redirectUrl) { this.redirectUrl = redirectUrl; }
    }
    
    /**
     * 楼层回复参数 (从回复页面获取)
     */
    public static class ReplyQuoteParams {
        private String noticeAuthor;     // 通知作者token
        private String noticeTrimStr;    // 引用内容
        private String noticeAuthorMsg;  // 被回复作者名
        private int repPid;              // 回复的评论ID
        private String formhash;         // 表单hash
        
        public String getNoticeAuthor() { return noticeAuthor; }
        public void setNoticeAuthor(String noticeAuthor) { this.noticeAuthor = noticeAuthor; }
        public String getNoticeTrimStr() { return noticeTrimStr; }
        public void setNoticeTrimStr(String noticeTrimStr) { this.noticeTrimStr = noticeTrimStr; }
        public String getNoticeAuthorMsg() { return noticeAuthorMsg; }
        public void setNoticeAuthorMsg(String noticeAuthorMsg) { this.noticeAuthorMsg = noticeAuthorMsg; }
        public int getRepPid() { return repPid; }
        public void setRepPid(int repPid) { this.repPid = repPid; }
        public String getFormhash() { return formhash; }
        public void setFormhash(String formhash) { this.formhash = formhash; }
    }
    
    /**
     * 获取楼层回复参数
     * 需要先请求回复页面获取noticeauthor等参数
     * 接口: GET /forum.php?mod=post&action=reply&fid={fid}&tid={tid}&repquote={pid}&page=1&inajax=1
     * @param fid 版块ID
     * @param tid 帖子ID
     * @param replyPid 要回复的评论pid
     * @return 回复参数
     */
    public ApiResponse<ReplyQuoteParams> getReplyQuoteParams(int fid, int tid, int replyPid) {
        String url = ApiConfig.BASE_URL + "forum.php?mod=post&action=reply" +
                "&fid=" + fid +
                "&tid=" + tid +
                "&repquote=" + replyPid +
                "&page=1&inajax=1";
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.getThreadUrl(tid))
                .header("X-Requested-With", "XMLHttpRequest")
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String html = response.body().string();
                
                // 解析参数
                ReplyQuoteParams params = new ReplyQuoteParams();
                
                // 提取CDATA内容
                Pattern cdataPattern = Pattern.compile("<\\!\\[CDATA\\[(.*?)\\]\\]>", Pattern.DOTALL);
                Matcher cdataMatcher = cdataPattern.matcher(html);
                String content = html;
                if (cdataMatcher.find()) {
                    content = cdataMatcher.group(1);
                }
                
                // 解析noticeauthor
                Pattern noticeAuthorPattern = Pattern.compile("name=\"noticeauthor\"[^>]*value=\"([^\"]+)\"");
                Matcher noticeAuthorMatcher = noticeAuthorPattern.matcher(content);
                if (noticeAuthorMatcher.find()) {
                    params.setNoticeAuthor(noticeAuthorMatcher.group(1));
                }
                
                // 解析noticetrimstr
                Pattern trimStrPattern = Pattern.compile("name=\"noticetrimstr\"[^>]*value=\"([^\"]+)\"");
                Matcher trimStrMatcher = trimStrPattern.matcher(content);
                if (trimStrMatcher.find()) {
                    params.setNoticeTrimStr(trimStrMatcher.group(1));
                }
                
                // 解析noticeauthormsg
                Pattern authormsgPattern = Pattern.compile("name=\"noticeauthormsg\"[^>]*value=\"([^\"]+)\"");
                Matcher authormsgMatcher = authormsgPattern.matcher(content);
                if (authormsgMatcher.find()) {
                    params.setNoticeAuthorMsg(authormsgMatcher.group(1));
                }
                
                // 解析reppid
                Pattern reppidPattern = Pattern.compile("name=\"reppid\"[^>]*value=\"(\\d+)\"");
                Matcher reppidMatcher = reppidPattern.matcher(content);
                if (reppidMatcher.find()) {
                    params.setRepPid(Integer.parseInt(reppidMatcher.group(1)));
                }
                
                // 解析formhash
                Pattern formhashPattern = Pattern.compile("name=\"formhash\"[^>]*value=\"([a-f0-9]{8})\"");
                Matcher formhashMatcher = formhashPattern.matcher(content);
                if (formhashMatcher.find()) {
                    params.setFormhash(formhashMatcher.group(1));
                }
                
                // 检查是否获取到必要参数
                if (params.getNoticeAuthor() == null || params.getNoticeAuthor().isEmpty()) {
                    return ApiResponse.error("获取回复参数失败");
                }
                
                return ApiResponse.success(params);
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 回复楼层评论 (完整版，支持引用)
     * 需要先调用getReplyQuoteParams获取参数
     * @param fid 版块ID
     * @param tid 帖子ID
     * @param message 回复内容
     * @param params 从getReplyQuoteParams获取的参数
     * @param attachIds 附件ID列表 (可选)
     * @return 回复结果
     */
    public ApiResponse<ReplyResult> postReplyToComment(int fid, int tid, String message, 
                                                        ReplyQuoteParams params,
                                                        java.util.List<Integer> attachIds) {
        if (params == null || params.getNoticeAuthor() == null) {
            return ApiResponse.error("缺少回复参数，请重试");
        }
        
        String formhash = params.getFormhash();
        if (formhash == null || formhash.isEmpty()) {
            formhash = getFormhash();
            if (formhash == null) {
                return ApiResponse.error("获取formhash失败，请先登录");
            }
        }
        
        // 构建URL
        String url = ApiConfig.BASE_URL + "forum.php?mod=post&action=reply" +
                "&fid=" + fid +
                "&tid=" + tid +
                "&extra=" +
                "&replysubmit=yes" +
                "&mobile=2" +
                "&handlekey=fastposts" +
                "&loc=1" +
                "&inajax=1";
        
        // 构建POST数据
        StringBuilder postData = new StringBuilder();
        postData.append("formhash=").append(formhash);
        postData.append("&posttime=").append(System.currentTimeMillis() / 1000);
        postData.append("&addfeed=1");
        postData.append("&noticeauthor=").append(params.getNoticeAuthor());
        
        // 添加引用内容
        if (params.getNoticeTrimStr() != null && !params.getNoticeTrimStr().isEmpty()) {
            try {
                postData.append("&noticetrimstr=").append(java.net.URLEncoder.encode(params.getNoticeTrimStr(), "UTF-8"));
            } catch (java.io.UnsupportedEncodingException e) {
                postData.append("&noticetrimstr=").append(params.getNoticeTrimStr());
            }
        }
        
        // 添加被回复作者名
        if (params.getNoticeAuthorMsg() != null && !params.getNoticeAuthorMsg().isEmpty()) {
            try {
                postData.append("&noticeauthormsg=").append(java.net.URLEncoder.encode(params.getNoticeAuthorMsg(), "UTF-8"));
            } catch (java.io.UnsupportedEncodingException e) {
                postData.append("&noticeauthormsg=").append(params.getNoticeAuthorMsg());
            }
        }
        
        // 添加回复pid
        if (params.getRepPid() > 0) {
            postData.append("&reppid=").append(params.getRepPid());
            postData.append("&reppost=").append(params.getRepPid());
        }
        
        postData.append("&replysubmit=yes");
        
        // 添加附件ID
        if (attachIds != null && !attachIds.isEmpty()) {
            for (Integer aid : attachIds) {
                postData.append("&attachnew[").append(aid).append("][description]=");
            }
        }
        
        // 添加回复内容
        try {
            postData.append("&message=").append(java.net.URLEncoder.encode(message, "UTF-8"));
        } catch (java.io.UnsupportedEncodingException e) {
            postData.append("&message=").append(message);
        }
        
        try {
            okhttp3.RequestBody body = okhttp3.RequestBody.create(
                postData.toString(),
                okhttp3.MediaType.parse("application/x-www-form-urlencoded; charset=UTF-8")
            );
            
            Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.getThreadUrl(tid))
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String result = response.body().string();
                ReplyResult replyResult = parseReplyResponse(result);
                
                if (replyResult.isSuccess()) {
                    return ApiResponse.success(replyResult);
                } else {
                    return ApiResponse.error(replyResult.getMessage());
                }
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 删除评论
     * 通过编辑接口实现删除
     * 接口: POST /forum.php?mod=post&action=edit&editsubmit=yes&mobile=2&inajax=1
     * @param fid 版块ID
     * @param tid 帖子ID
     * @param pid 评论ID
     * @return 是否成功
     */
    public ApiResponse<Boolean> deleteComment(int fid, int tid, int pid) {
        String formhash = getFormhash();
        if (formhash == null) {
            return ApiResponse.error("获取formhash失败，请先登录");
        }
        
        // 构建URL
        String url = ApiConfig.BASE_URL + "forum.php?mod=post&action=edit" +
                "&extra=" +
                "&editsubmit=yes" +
                "&mobile=2" +
                "&handlekey=postform" +
                "&inajax=1";
        
        // 构建POST数据
        String postData = "formhash=" + formhash +
                "&posttime=" + (System.currentTimeMillis() / 1000) +
                "&delete=1" +
                "&fid=" + fid +
                "&tid=" + tid +
                "&pid=" + pid +
                "&page=1" +
                "&editsubmit=yes" +
                "&subject=" +
                "&message=" +
                "&save=";
        
        try {
            okhttp3.RequestBody body = okhttp3.RequestBody.create(
                postData,
                okhttp3.MediaType.parse("application/x-www-form-urlencoded; charset=UTF-8")
            );
            
            Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.getThreadUrl(tid))
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String result = response.body().string();
                
                // 检查是否删除成功
                if (result.contains("删除成功") || result.contains("主题删除成功")) {
                    return ApiResponse.success(true);
                } else if (result.contains("没有权限")) {
                    return ApiResponse.error("没有权限删除此评论");
                } else if (result.contains("需要先登录") || result.contains("请先登录")) {
                    return ApiResponse.error("请先登录");
                } else {
                    return ApiResponse.error("删除失败");
                }
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 购买付费帖子
     * 接口: POST /forum.php?mod=misc&action=pay&paysubmit=yes&infloat=yes
     * @param tid 帖子ID
     * @return 购买结果
     */
    public ApiResponse<BuyPostResult> buyPost(int tid) {
        String formhash = getFormhash();
        if (formhash == null) {
            return ApiResponse.error("获取formhash失败，请先登录");
        }
        
        // 构建URL
        String url = ApiConfig.BASE_URL + "forum.php?mod=misc&action=pay&paysubmit=yes&infloat=yes";
        
        // 构建POST数据
        String referer = ApiConfig.getThreadUrl(tid);
        String postData = "formhash=" + formhash +
                "&referer=" + referer +
                "&tid=" + tid +
                "&handlekey=";
        
        try {
            okhttp3.RequestBody body = okhttp3.RequestBody.create(
                postData,
                okhttp3.MediaType.parse("application/x-www-form-urlencoded; charset=UTF-8")
            );
            
            Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", referer)
                .header("X-Requested-With", "XMLHttpRequest")
                .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String result = response.body().string();
                
                // 解析响应
                BuyPostResult buyResult = parseBuyPostResponse(result);
                
                if (buyResult.isSuccess()) {
                    return ApiResponse.success(buyResult);
                } else {
                    return ApiResponse.error(buyResult.getMessage());
                }
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 解析购买帖子响应
     */
    private BuyPostResult parseBuyPostResponse(String html) {
        BuyPostResult result = new BuyPostResult();
        
        // 提取CDATA内容
        Pattern cdataPattern = Pattern.compile("<\\!\\[CDATA\\[(.*?)\\]\\]>", Pattern.DOTALL);
        Matcher cdataMatcher = cdataPattern.matcher(html);
        String content = html;
        if (cdataMatcher.find()) {
            content = cdataMatcher.group(1);
        }
        
        // 检查是否购买成功
        if (content.contains("购买成功") || content.contains("支付成功")) {
            result.setSuccess(true);
            result.setMessage("购买成功");
        } else if (content.contains("您已购买") || content.contains("已经购买")) {
            result.setSuccess(true);
            result.setMessage("您已购买过此内容");
        } else if (content.contains("金币不足") || content.contains("余额不足")) {
            result.setSuccess(false);
            result.setMessage("金币不足，无法购买");
        } else if (content.contains("需要先登录") || content.contains("请先登录")) {
            result.setSuccess(false);
            result.setMessage("请先登录");
        } else if (content.contains("已关闭") || content.contains("已结束")) {
            result.setSuccess(false);
            result.setMessage("购买已结束");
        } else {
            // 尝试提取其他错误信息
            Pattern errorPattern = Pattern.compile("<p[^>]*>([^<]+)</p>");
            Matcher errorMatcher = errorPattern.matcher(content);
            if (errorMatcher.find()) {
                String errorMsg = errorMatcher.group(1).trim();
                if (!errorMsg.isEmpty()) {
                    result.setSuccess(false);
                    result.setMessage(errorMsg);
                    return result;
                }
            }
            
            // 检查是否返回成功页面（通过检查特定元素）
            if (content.contains("tip_tit") && content.contains("购买")) {
                // 可能是确认页面，再检查是否成功
                result.setSuccess(true);
                result.setMessage("购买成功");
            } else {
                result.setSuccess(false);
                result.setMessage("购买失败，请重试");
            }
        }
        
        return result;
    }
    
    /**
     * 购买帖子结果
     */
    public static class BuyPostResult {
        private boolean success;
        private String message;
        
        public boolean isSuccess() { return success; }
        public void setSuccess(boolean success) { this.success = success; }
        public String getMessage() { return message; }
        public void setMessage(String message) { this.message = message; }
    }
    
    /**
     * 上传图片
     * 接口: POST /misc.php?mod=swfupload&operation=upload&type=image&inajax=yes&infloat=yes&simple=2
     * @param imageFile 图片文件
     * @param uid 用户ID
     * @param uploadHash 上传hash (从帖子页面获取)
     * @return 上传结果
     */
    public ApiResponse<UploadResult> uploadImage(java.io.File imageFile, int uid, String uploadHash) {
        if (uploadHash == null || uploadHash.isEmpty()) {
            return ApiResponse.error("上传凭证获取失败，请刷新页面");
        }
        
        // 构建URL
        String url = ApiConfig.BASE_URL + "misc.php?mod=swfupload&operation=upload&type=image&inajax=yes&infloat=yes&simple=2";
        
        try {
            // 构建multipart请求体
            okhttp3.MultipartBody.Builder multipartBuilder = new okhttp3.MultipartBody.Builder()
                    .setType(okhttp3.MultipartBody.FORM)
                    .addFormDataPart("uid", String.valueOf(uid))
                    .addFormDataPart("hash", uploadHash);
            
            // 获取文件扩展名
            String filename = imageFile.getName();
            String extension = "";
            int dotIndex = filename.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = filename.substring(dotIndex + 1).toLowerCase();
            }
            
            // 确定文件扩展名（论坛只支持 jpg, jpeg, gif, png）
            String finalExtension = "jpg";
            if (extension.equals("png")) {
                finalExtension = "png";
            } else if (extension.equals("gif")) {
                finalExtension = "gif";
            } else if (extension.equals("jpeg")) {
                finalExtension = "jpeg";
            }
            
            // 使用简单文件名，避免特殊字符问题
            String uploadFilename = "image_" + System.currentTimeMillis() + "." + finalExtension;
            
            // 使用 application/octet-stream 作为Content-Type（与官方请求一致）
            okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/octet-stream");
            okhttp3.RequestBody fileBody = okhttp3.RequestBody.create(imageFile, mediaType);
            multipartBuilder.addFormDataPart("Filedata", uploadFilename, fileBody);
            
            okhttp3.RequestBody requestBody = multipartBuilder.build();
            
            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .header("User-Agent", ApiConfig.USER_AGENT)
                    .header("Referer", ApiConfig.BASE_URL)
                    .header("Origin", ApiConfig.BASE_URL.substring(0, ApiConfig.BASE_URL.length() - 1)) // 移除末尾斜杠
                    .header("Accept", "*/*")
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "上传失败: " + response.code());
                }
                
                String result = response.body().string();
                
                // 解析响应: DISCUZUPLOAD|status|extra|aid|isImage|serverPath|originalName|flag
                UploadResult uploadResult = UploadResult.parse(result);
                
                if (uploadResult.isSuccess()) {
                    return ApiResponse.success(uploadResult);
                } else {
                    return ApiResponse.error(uploadResult.getErrorMessage());
                }
            }
        } catch (IOException e) {
            return ApiResponse.error("上传失败: " + e.getMessage());
        }
    }
    
    /**
     * 上传附件（非图片文件）
     * 接口: POST /misc.php?mod=swfupload&action=swfupload&operation=upload&fid={fid}&inajax=1&simple=2
     * @param file 附件文件
     * @param uid 用户ID
     * @param uploadHash 上传hash
     * @param fid 版块ID
     * @return 上传结果
     */
    public ApiResponse<UploadResult> uploadAttachment(java.io.File file, int uid, String uploadHash, int fid) {
        if (uploadHash == null || uploadHash.isEmpty()) {
            return ApiResponse.error("上传凭证获取失败，请刷新页面");
        }
        
        // 构建URL - 附件上传使用不同的接口
        String url = ApiConfig.BASE_URL + "misc.php?mod=swfupload&action=swfupload&operation=upload&fid=" + fid + "&inajax=1&simple=2";
        
        try {
            // 获取文件名和扩展名
            String filename = file.getName();
            String extension = "";
            int dotIndex = filename.lastIndexOf('.');
            if (dotIndex > 0) {
                extension = filename.substring(dotIndex + 1).toLowerCase();
            }
            
            // 构建multipart请求体
            okhttp3.MultipartBody.Builder multipartBuilder = new okhttp3.MultipartBody.Builder()
                    .setType(okhttp3.MultipartBody.FORM)
                    .addFormDataPart("uid", String.valueOf(uid))
                    .addFormDataPart("hash", uploadHash);
            
            // 使用 application/octet-stream 作为Content-Type
            okhttp3.MediaType mediaType = okhttp3.MediaType.parse("application/octet-stream");
            okhttp3.RequestBody fileBody = okhttp3.RequestBody.create(file, mediaType);
            multipartBuilder.addFormDataPart("Filedata", filename, fileBody);
            
            okhttp3.RequestBody requestBody = multipartBuilder.build();
            
            Request request = new Request.Builder()
                    .url(url)
                    .post(requestBody)
                    .header("User-Agent", ApiConfig.USER_AGENT)
                    .header("Referer", ApiConfig.BASE_URL + "forum.php?mod=post&action=newthread&fid=" + fid)
                    .header("Origin", ApiConfig.BASE_URL.substring(0, ApiConfig.BASE_URL.length() - 1))
                    .header("Accept", "*/*")
                    .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "上传失败: " + response.code());
                }
                
                String result = response.body().string();
                
                // 解析响应: DISCUZUPLOAD|status|extra|aid|isImage|serverPath|originalName|flag|...
                UploadResult uploadResult = UploadResult.parse(result);
                
                if (uploadResult.isSuccess()) {
                    return ApiResponse.success(uploadResult);
                } else {
                    return ApiResponse.error(uploadResult.getErrorMessage());
                }
            }
        } catch (IOException e) {
            return ApiResponse.error("上传失败: " + e.getMessage());
        }
    }
    
    // ========== 消息相关方法 ==========
    
    /**
     * 获取私信列表
     * @param page 页码
     */
    public ApiResponse<PmListResult> getPmList(int page) {
        String url = ApiConfig.getPmListUrl(page);
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String html = response.body().string();
                
                if (html.isEmpty()) {
                    return ApiResponse.error("响应为空");
                }
                
                // 检查是否需要登录
                if (html.contains("请先登录") || html.contains("您需要登录") || html.contains("立即登录")) {
                    return ApiResponse.error("请先登录");
                }
                
                PmListResult result = HtmlParser.parsePmList(html);
                return ApiResponse.success(result);
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取私信详情（对话列表）
     * @param pmId 私信ID
     * @param page 页码
     */
    public ApiResponse<PmDetailResult> getPmDetail(int pmId, int page) {
        String url = ApiConfig.getPmDetailUrl(pmId, page);
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String html = response.body().string();
                if (html.isEmpty()) {
                    return ApiResponse.error("响应为空");
                }
                
                PmDetailResult result = HtmlParser.parsePmDetail(html);
                return ApiResponse.success(result);
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 获取与特定用户的私信对话
     * 接口: GET home.php?mod=space&do=pm&subop=view&touid={uid}&mobile=2
     * @param toUid 对方用户ID
     * @param page 页码
     */
    public ApiResponse<PmDetailResult> getPmChat(int toUid, int page) {
        String url = ApiConfig.BASE_URL + "home.php?mod=space&do=pm&subop=view&touid=" + toUid + "&mobile=2";
        if (page > 1) {
            url += "&page=" + page;
        }
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String html = response.body().string();
                if (html.isEmpty()) {
                    return ApiResponse.error("响应为空");
                }
                
                // 检查是否需要登录
                if (html.contains("请先登录") || html.contains("您需要登录")) {
                    return ApiResponse.error("请先登录");
                }
                
                PmDetailResult result = HtmlParser.parsePmDetail(html);
                return ApiResponse.success(result);
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 发送私信
     * 接口: POST home.php?mod=spacecp&ac=pm&op=send&pmid=0&daterange=2&pmsubmit=yes&mobile=2&handlekey=pmform&inajax=1
     * @param toUid 接收用户ID
     * @param message 消息内容
     */
    public ApiResponse<Boolean> sendPm(int toUid, String message) {
        String formhash = getFormhash();
        if (formhash == null) {
            return ApiResponse.error("获取formhash失败，请先登录");
        }
        
        // 使用正确URL格式
        String url = ApiConfig.BASE_URL + "home.php?mod=spacecp&ac=pm&op=send&pmid=0&daterange=2&pmsubmit=yes&mobile=2&handlekey=pmform&inajax=1";
        
        try {
            String encodedMessage = java.net.URLEncoder.encode(message, "UTF-8");
            // 参数格式: formhash=xxx&touid=xxx&message=xxx
            String postData = "formhash=" + formhash + 
                "&touid=" + toUid + 
                "&message=" + encodedMessage;
            
            okhttp3.RequestBody body = okhttp3.RequestBody.create(
                postData,
                okhttp3.MediaType.parse("application/x-www-form-urlencoded; charset=UTF-8")
            );
            
            Request request = new Request.Builder()
                .url(url)
                .post(body)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL + "home.php?mod=space&do=pm&subop=view&touid=" + toUid)
                .header("X-Requested-With", "XMLHttpRequest")
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "发送失败: " + response.code());
                }
                
                String result = response.body().string();
                if (result.contains("操作成功") || result.contains("发送成功") || result.contains("成功")) {
                    return ApiResponse.success(true);
                } else if (result.contains("太快")) {
                    return ApiResponse.error("发送太频繁，请稍后再试");
                } else if (result.contains("权限")) {
                    return ApiResponse.error("您没有发送私信的权限");
                } else {
                    // 尝试从响应中提取错误信息
                    String error = extractErrorMessage(result);
                    if (error != null) {
                        return ApiResponse.error(error);
                    }
                    return ApiResponse.error("发送失败");
                }
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 从XML响应中提取错误信息
     */
    private String extractErrorMessage(String xml) {
        if (xml == null) return null;
        // 尝试提取CDATA中的错误信息
        Pattern errorPattern = Pattern.compile("class=\"[^\"]*error[^\"]*\"[^>]*>([^<]+)<");
        Matcher matcher = errorPattern.matcher(xml);
        if (matcher.find()) {
            return matcher.group(1).trim();
        }
        // 尝试提取提示信息
        Pattern noticePattern = Pattern.compile("<p[^>]*>([^<]+)</p>");
        matcher = noticePattern.matcher(xml);
        if (matcher.find()) {
            String text = matcher.group(1).trim();
            if (text.length() > 0 && text.length() < 100) {
                return text;
            }
        }
        return null;
    }
    
    /**
     * 增量获取新消息（实时更新）
     * 接口: GET home.php?mod=spacecp&ac=pm&op=showmsg&msgonly=1&touid={uid}&pmid={pmid}&inajax=1&daterange=1&comiis_msg_endtime={endtime}
     * @param toUid 对方用户ID
     * @param pmId 私信会话ID（可选，传0）
     * @param endTime 时间戳基准，用于增量获取
     * @return 新消息列表
     */
    public ApiResponse<List<PrivateMessage>> getNewPmMessages(int toUid, int pmId, long endTime) {
        String url = ApiConfig.BASE_URL + "home.php?mod=spacecp&ac=pm&op=showmsg&msgonly=1" +
            "&touid=" + toUid + 
            "&pmid=" + pmId + 
            "&inajax=1" +
            "&daterange=1" +
            "&comiis_msg_endtime=" + endTime;
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL + "home.php?mod=space&do=pm&subop=view&touid=" + toUid)
                .header("X-Requested-With", "XMLHttpRequest")
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败");
                }
                
                String xml = response.body().string();
                if (xml == null || xml.isEmpty()) {
                    return ApiResponse.success(new java.util.ArrayList<>());
                }
                
                // 解析XML响应
                List<PrivateMessage> messages = HtmlParser.parseNewPmMessages(xml);
                return ApiResponse.success(messages);
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 新私信检查结果
     */
    public static class NewPmCheckResult {
        private boolean hasNewPm;
        private int unreadCount;
        
        public boolean isHasNewPm() { return hasNewPm; }
        public void setHasNewPm(boolean hasNewPm) { this.hasNewPm = hasNewPm; }
        public int getUnreadCount() { return unreadCount; }
        public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
    }
    
    /**
     * 获取通知列表
     * @param type 通知类型: "mypost"(我的帖子), "互动消息", "系统消息", 空=全部
     * @param page 页码
     */
    public ApiResponse<NotificationListResult> getNoticeList(String type, int page) {
        String url = ApiConfig.getNoticeListUrl(type, page);
        
        try {
            Request request = new Request.Builder()
                .url(url)
                .header("User-Agent", ApiConfig.USER_AGENT)
                .header("Referer", ApiConfig.BASE_URL)
                .build();
            
            try (Response response = client.newCall(request).execute()) {
                if (!response.isSuccessful()) {
                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                }
                
                String html = response.body().string();
                if (html.isEmpty()) {
                    return ApiResponse.error("响应为空");
                }
                
                // 检查是否需要登录
                if (html.contains("请先登录") || html.contains("您需要登录")) {
                    return ApiResponse.error("请先登录");
                }
                
                NotificationListResult result = HtmlParser.parseNoticeList(html);
                return ApiResponse.success(result);
            }
        } catch (IOException e) {
            return ApiResponse.error("网络错误: " + e.getMessage());
        }
    }
    
    /**
     * 私信列表结果
     */
    public static class PmListResult {
        private List<PrivateMessage> messages;
        private int unreadCount;
        private int currentPage;
        private int totalPages;
        
        public List<PrivateMessage> getMessages() { return messages; }
        public void setMessages(List<PrivateMessage> messages) { this.messages = messages; }
        public int getUnreadCount() { return unreadCount; }
        public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
        public int getCurrentPage() { return currentPage; }
        public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    }
    
    /**
     * 私信详情结果
     */
    public static class PmDetailResult {
        private List<PrivateMessage> messages;
        private User otherUser;
        private int currentPage;
        private int totalPages;
        
        public List<PrivateMessage> getMessages() { return messages; }
        public void setMessages(List<PrivateMessage> messages) { this.messages = messages; }
        public User getOtherUser() { return otherUser; }
        public void setOtherUser(User otherUser) { this.otherUser = otherUser; }
        public int getCurrentPage() { return currentPage; }
        public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    }
    
    /**
     * 通知列表结果
     */
    public static class NotificationListResult {
        private List<Notification> notifications;
        private int unreadCount;
        private int currentPage;
        private int totalPages;
        
        public List<Notification> getNotifications() { return notifications; }
        public void setNotifications(List<Notification> notifications) { this.notifications = notifications; }
        public int getUnreadCount() { return unreadCount; }
        public void setUnreadCount(int unreadCount) { this.unreadCount = unreadCount; }
        public int getCurrentPage() { return currentPage; }
        public void setCurrentPage(int currentPage) { this.currentPage = currentPage; }
        public int getTotalPages() { return totalPages; }
        public void setTotalPages(int totalPages) { this.totalPages = totalPages; }
    }
    
    /**
     * 发帖选项（权限和高级设置）
     */
    public static class ThreadPostOptions {
                private int price = 0;              // 帖子售价
                private boolean hiddenReplies = false;   // 回帖仅作者可见
                private boolean orderType = false;       // 回帖倒序排列
                private boolean allowNoticeAuthor = true; // 接收回复通知（默认开启）
                private boolean useSig = true;           // 使用个人签名（默认开启）
                
                public int getPrice() { return price; }
                public void setPrice(int price) { this.price = price; }
                public boolean isHiddenReplies() { return hiddenReplies; }
                public void setHiddenReplies(boolean hiddenReplies) { this.hiddenReplies = hiddenReplies; }
                public boolean isOrderType() { return orderType; }
                public void setOrderType(boolean orderType) { this.orderType = orderType; }
                public boolean isAllowNoticeAuthor() { return allowNoticeAuthor; }
                public void setAllowNoticeAuthor(boolean allowNoticeAuthor) { this.allowNoticeAuthor = allowNoticeAuthor; }
                public boolean isUseSig() { return useSig; }
                public void setUseSig(boolean useSig) { this.useSig = useSig; }
            }
            
            /**
             * 发帖结果
             */
            public static class NewThreadResult {
                private boolean success;
                private String message;
                private int tid;
                private String redirectUrl;
                
                public boolean isSuccess() { return success; }
                public void setSuccess(boolean success) { this.success = success; }
                public String getMessage() { return message; }
                public void setMessage(String message) { this.message = message; }
                public int getTid() { return tid; }
                public void setTid(int tid) { this.tid = tid; }
                public String getRedirectUrl() { return redirectUrl; }
                public void setRedirectUrl(String redirectUrl) { this.redirectUrl = redirectUrl; }
            }
            
            // ==================== 发帖相关API ====================
            
            /**
             * 获取发帖页面参数
             * 接口: GET forum.php?mod=post&action=newthread&fid={fid}&mobile=2
             * @param fid 版块ID
             * @return 发帖页面参数
             */
            public ApiResponse<NewThreadParams> getNewThreadParams(int fid) {
                String url = ApiConfig.BASE_URL + "forum.php?mod=post&action=newthread&fid=" + fid + "&mobile=2";
                
                try {
                    Request request = new Request.Builder()
                        .url(url)
                        .header("User-Agent", ApiConfig.USER_AGENT)
                        .header("Referer", ApiConfig.BASE_URL)
                        .build();
                    
                    try (Response response = client.newCall(request).execute()) {
                        if (!response.isSuccessful()) {
                            return ApiResponse.error(response.code(), "请求失败: " + response.code());
                        }
                        
                        String html = response.body().string();
                        
                        // 检查是否需要登录
                        if (html.contains("请先登录") || html.contains("您需要登录")) {
                            return ApiResponse.error("请先登录");
                        }
                        
                        // 检查是否有权限发帖
                        if (html.contains("没有权限") || html.contains("无法发帖")) {
                            return ApiResponse.error("没有权限在此版块发帖");
                        }
                        
                        NewThreadParams params = HtmlParser.parseNewThreadPage(html);
                        if (params == null || !params.isValid()) {
                            return ApiResponse.error("解析发帖页面失败");
                        }
                        
                        params.setFid(fid);
                        return ApiResponse.success(params);
                    }
                } catch (IOException e) {
                    return ApiResponse.error("网络错误: " + e.getMessage());
                }
            }
            
            /**
             * 发表新帖（简化版，不包含高级选项）
             */
            public ApiResponse<NewThreadResult> postNewThread(int fid, String subject, String message, 
                                                             String formhash, java.util.List<Integer> attachIds) {
                return postNewThread(fid, subject, message, formhash, attachIds, null);
            }
            
            /**
             * 发表新帖（完整版，包含权限和高级选项）
             * 接口: POST forum.php?mod=post&action=newthread&fid={fid}&topicsubmit=yes&mobile=2&inajax=1
             * @param fid 版块ID
             * @param subject 帖子标题
             * @param message 帖子内容
             * @param formhash 表单验证hash
             * @param attachIds 附件ID列表 (可选)
             * @param options 发帖选项 (可选，包含权限和高级设置)
             * @return 发帖结果
             */
            public ApiResponse<NewThreadResult> postNewThread(int fid, String subject, String message, 
                                                             String formhash, java.util.List<Integer> attachIds,
                                                             ThreadPostOptions options) {
                if (formhash == null || formhash.isEmpty()) {
                    formhash = getFormhash();
                    if (formhash == null) {
                        return ApiResponse.error("获取formhash失败，请先登录");
                    }
                }
                
                // 构建URL
                String url = ApiConfig.BASE_URL + "forum.php?mod=post&action=newthread&fid=" + fid +
                        "&extra=&topicsubmit=yes&mobile=2&handlekey=postform&inajax=1";
                
                // 构建POST数据
                StringBuilder postData = new StringBuilder();
                postData.append("formhash=").append(formhash);
                postData.append("&posttime=").append(System.currentTimeMillis() / 1000);
                postData.append("&wysiwyg=0");
                postData.append("&topicsubmit=yes");
                
                // 添加标题
                try {
                    postData.append("&subject=").append(java.net.URLEncoder.encode(subject, "UTF-8"));
                } catch (java.io.UnsupportedEncodingException e) {
                    postData.append("&subject=").append(subject);
                }
                
                // 添加附件ID (只需要提交attachnew参数，不需要重复插入代码，因为上传时已经插入)
                if (attachIds != null && !attachIds.isEmpty()) {
                    for (Integer aid : attachIds) {
                        try {
                            // attachnew[aid][description] 格式，description为空
                            postData.append("&attachnew[").append(aid).append("][description]=");
                        } catch (Exception e) {
                            // 忽略
                        }
                    }
                    // 注意：不要在这里追加 [attachimg] 代码，因为上传时已经自动插入了
                }
                
                // 添加权限和高级选项
                if (options != null) {
                    // 帖子售价
                    if (options.getPrice() > 0) {
                        postData.append("&price=").append(options.getPrice());
                    }
                    
                    // 回帖仅作者可见
                    if (options.isHiddenReplies()) {
                        postData.append("&hiddenreplies=1");
                    }
                    
                    // 回帖倒序排列
                    if (options.isOrderType()) {
                        postData.append("&ordertype=1");
                    }
                    
                    // 接收回复通知
                    if (options.isAllowNoticeAuthor()) {
                        postData.append("&allownoticeauthor=1");
                    }
                    
                    // 使用个人签名
                    if (options.isUseSig()) {
                        postData.append("&usesig=1");
                    }
                }
                
                // 添加内容
                try {
                    postData.append("&message=").append(java.net.URLEncoder.encode(message, "UTF-8"));
                } catch (java.io.UnsupportedEncodingException e) {
                    postData.append("&message=").append(message);
                }
                
                try {
                    okhttp3.RequestBody body = okhttp3.RequestBody.create(
                        postData.toString(),
                        okhttp3.MediaType.parse("application/x-www-form-urlencoded; charset=UTF-8")
                    );
                    
                    Request request = new Request.Builder()
                        .url(url)
                        .post(body)
                        .header("User-Agent", ApiConfig.USER_AGENT)
                        .header("Referer", ApiConfig.BASE_URL + "forum.php?mod=forumdisplay&fid=" + fid)
                        .header("X-Requested-With", "XMLHttpRequest")
                        .header("Content-Type", "application/x-www-form-urlencoded; charset=UTF-8")
                        .build();
                    
                    try (Response response = client.newCall(request).execute()) {
                        if (!response.isSuccessful()) {
                            return ApiResponse.error(response.code(), "请求失败: " + response.code());
                        }
                        
                        String result = response.body().string();
                        NewThreadResult threadResult = HtmlParser.parseNewThreadResult(result);
                        
                        if (threadResult.isSuccess()) {
                            return ApiResponse.success(threadResult);
                        } else {
                            return ApiResponse.error(threadResult.getMessage());
                        }
                    }
                                    } catch (IOException e) {
                                        return ApiResponse.error("网络错误: " + e.getMessage());
                                    }
                                }
                    
                    /**
                     * 点赞评论（踢贴）
                     * 接口: forum.php?mod=misc&action=postreview&do=support&tid={tid}&pid={pid}&hash={formhash}&inajax=1
                     * @param tid 帖子ID
                     * @param pid 评论ID
                     * @return 点赞结果
                     */
                    public ApiResponse<CommentLikeResult> recommendComment(int tid, int pid) {
                        String formhash = getFormhash();
                        if (formhash == null) {
                            return ApiResponse.error("获取formhash失败，请先登录");
                        }
                        
                        String url = ApiConfig.BASE_URL + "forum.php?mod=misc&action=postreview&do=support" +
                                "&tid=" + tid + "&pid=" + pid + "&hash=" + formhash + "&inajax=1";
                        
                        try {
                            Request request = new Request.Builder()
                                .url(url)
                                .header("User-Agent", ApiConfig.USER_AGENT)
                                .header("Referer", ApiConfig.getThreadUrl(tid))
                                .header("X-Requested-With", "XMLHttpRequest")
                                .build();
                            
                            try (Response response = client.newCall(request).execute()) {
                                if (!response.isSuccessful()) {
                                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                                }
                                
                                String result = response.body().string();
                                
                                CommentLikeResult likeResult = new CommentLikeResult();
                                
                                // 检查是否点赞自己的评论
                                if (result.contains("您不能对自己的回帖进行投票")) {
                                    return ApiResponse.error("不能点赞自己的评论");
                                }
                                
                                // 检查是否已经点赞过
                                if (result.contains("您已经对此回帖投过票了")) {
                                    likeResult.setAlreadyLiked(true);
                                    return ApiResponse.success(likeResult);
                                }
                                
                                // 检查是否成功点赞
                                if (result.contains("投票成功")) {
                                    likeResult.setSuccess(true);
                                    // 解析点赞数
                                    likeResult.setCount(parseCommentLikeCount(result));
                                    return ApiResponse.success(likeResult);
                                }
                                
                                // 检查是否需要登录
                                if (result.contains("member.php?mod=logging&action=login")) {
                                    return ApiResponse.error("请先登录");
                                }
                                
                                return ApiResponse.error("点赞失败");
                            }
                        } catch (IOException e) {
                            return ApiResponse.error("网络错误: " + e.getMessage());
                        }
                    }
                    
                    /**
                     * 取消点赞评论
                     * 接口: plugin.php?id=comiis_app&comiis=re_hotreply&tid={tid}&pid={pid}&inajax=1
                     * @param tid 帖子ID
                     * @param pid 评论ID
                     * @return 是否成功
                     */
                    public ApiResponse<Boolean> unrecommendComment(int tid, int pid) {
                        String url = ApiConfig.BASE_URL + "plugin.php?id=comiis_app&comiis=re_hotreply" +
                                "&tid=" + tid + "&pid=" + pid + "&inajax=1";
                        
                        try {
                            Request request = new Request.Builder()
                                .url(url)
                                .header("User-Agent", ApiConfig.USER_AGENT)
                                .header("Referer", ApiConfig.getThreadUrl(tid))
                                .header("X-Requested-With", "XMLHttpRequest")
                                .build();
                            
                            try (Response response = client.newCall(request).execute()) {
                                if (!response.isSuccessful()) {
                                    return ApiResponse.error(response.code(), "请求失败: " + response.code());
                                }
                                
                                String result = response.body().string();
                                
                                // 成功取消返回包含 "ok" 或点赞数减少
                                if (result.contains(">ok<") || result.contains("<p>ok</p>") || result.length() > 10) {
                                    return ApiResponse.success(true);
                                }
                                
                                return ApiResponse.error("取消点赞失败");
                            }
                        } catch (IOException e) {
                            return ApiResponse.error("网络错误: " + e.getMessage());
                        }
                    }
                    
                    /**
                     * 解析评论点赞数
                     */
                    private int parseCommentLikeCount(String html) {
                        try {
                            // 尝试从响应中解析点赞数
                            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile("(\\d+)");
                            java.util.regex.Matcher matcher = pattern.matcher(html);
                            if (matcher.find()) {
                                return Integer.parseInt(matcher.group(1));
                            }
                        } catch (Exception e) {
                            // ignore
                        }
                        return 0;
                    }
                    
                    /**
                     * 评论点赞结果
                     */
                    public static class CommentLikeResult {
                        private boolean success;
                        private boolean alreadyLiked;
                        private int count;
                        
                        public boolean isSuccess() {
                            return success;
                        }
                        
                        public void setSuccess(boolean success) {
                            this.success = success;
                        }
                        
                        public boolean isAlreadyLiked() {
                            return alreadyLiked;
                        }
                        
                        public void setAlreadyLiked(boolean alreadyLiked) {
                            this.alreadyLiked = alreadyLiked;
                        }
                        
                        public int getCount() {
                            return count;
                        }
                        
                        public void setCount(int count) {
                            this.count = count;
                        }
                    }
                    
                    /**
                     * 打赏评论
                     * @param tid 帖子ID
                     * @param pid 评论ID
                     * @param score1 好评数（+1 或 0）
                     * @param score2 金币数（+1 或 0）
                     * @param reason 打赏理由（可选）
                     * @return 打赏结果
                     */
                    public RewardResult rateComment(int tid, int pid, int score1, int score2, String reason) {
                        RewardResult result = new RewardResult();
                        
                        try {
                            // 获取formhash
                            String formhash = getFormhash();
                            if (formhash == null || formhash.isEmpty()) {
                                result.setSuccess(false);
                                result.setMessage("未登录或formhash获取失败");
                                return result;
                            }
                            
                            // 构建请求URL
                            String url = ApiConfig.BASE_URL + "/forum.php?mod=misc&action=rate&ratesubmit=yes&infloat=yes&handlekey=rateform&inajax=1";
                            
                            // 构建请求体
                            StringBuilder bodyBuilder = new StringBuilder();
                            bodyBuilder.append("formhash=").append(java.net.URLEncoder.encode(formhash, "UTF-8"));
                            bodyBuilder.append("&tid=").append(tid);
                            bodyBuilder.append("&pid=").append(pid);
                            bodyBuilder.append("&referer=").append(java.net.URLEncoder.encode(
                                    ApiConfig.BASE_URL + "/forum.php?mod=viewthread&tid=" + tid + "&page=0#pid" + pid, "UTF-8"));
                            bodyBuilder.append("&handlekey=rate");
                            bodyBuilder.append("&score1=").append(score1 > 0 ? "%2B1" : "0");
                            bodyBuilder.append("&score2=").append(score2 > 0 ? "%2B1" : "0");
                            bodyBuilder.append("&reason=").append(reason != null ? java.net.URLEncoder.encode(reason, "UTF-8") : "");
                            
                            RequestBody body = RequestBody.create(
                                    bodyBuilder.toString(),
                                    MediaType.parse("application/x-www-form-urlencoded")
                            );
                            
                            Request request = new Request.Builder()
                                    .url(url)
                                    .post(body)
                                    .header("X-Requested-With", "XMLHttpRequest")
                                    .header("Referer", ApiConfig.BASE_URL + "/forum.php?mod=viewthread&tid=" + tid)
                                    .build();
                            
                            try (Response response = client.newCall(request).execute()) {
                                if (response.isSuccessful()) {
                                    String content = response.body().string();
                                    
                                    // 解析XML响应
                                    if (content.contains("感谢您的参与") || content.contains("评分成功")) {
                                        result.setSuccess(true);
                                        result.setMessage("打赏成功");
                                    } else if (content.contains("您已评价过")) {
                                        result.setSuccess(false);
                                        result.setAlreadyRated(true);
                                        result.setMessage("您已经打赏过了");
                                    } else if (content.contains("权限")) {
                                        result.setSuccess(false);
                                        result.setMessage("没有打赏权限");
                                    } else if (content.contains("登录")) {
                                        result.setSuccess(false);
                                        result.setMessage("请先登录");
                                    } else {
                                        result.setSuccess(false);
                                        // 从响应中提取错误消息
                                        // 格式: <p>错误信息<script...> 或 errorhandle_rate('错误信息'
                                        String errorMsg = extractRateErrorMessage(content);
                                        if (errorMsg != null && !errorMsg.isEmpty()) {
                                            result.setMessage(errorMsg);
                                        } else {
                                            result.setMessage("打赏失败，请稍后重试");
                                        }
                                    }
                                } else {
                                    result.setSuccess(false);
                                    result.setMessage("网络请求失败");
                                }
                            }
                            
                        } catch (Exception e) {
                            android.util.Log.e("ForumApi", "rateComment error", e);
                            result.setSuccess(false);
                            result.setMessage("发生错误: " + e.getMessage());
                        }
                        
                        return result;
                    }
                    
                    /**
                     * 从评分响应中提取错误消息
                     * 格式: <p>错误信息<script...> 或 errorhandle_rate('错误信息'
                     */
                    private String extractRateErrorMessage(String content) {
                        // 方法1: 从 <p> 标签提取（去掉 <script 部分）
                        java.util.regex.Pattern pPattern = java.util.regex.Pattern.compile(
                                "<p[^>]*>([^<]+)");
                        java.util.regex.Matcher pMatcher = pPattern.matcher(content);
                        if (pMatcher.find()) {
                            String msg = pMatcher.group(1).trim();
                            if (!msg.isEmpty()) {
                                return msg;
                            }
                        }
                        
                        // 方法2: 从 errorhandle_rate('错误信息' 提取
                        java.util.regex.Pattern errorPattern = java.util.regex.Pattern.compile(
                                "errorhandle_rate\\s*\\(\\s*['\"]([^'\"]+)");
                        java.util.regex.Matcher errorMatcher = errorPattern.matcher(content);
                        if (errorMatcher.find()) {
                            return errorMatcher.group(1).trim();
                        }
                        
                        return null;
                    }
                    
                    /**
                     * 打赏结果
                     */
                    public static class RewardResult {
                        private boolean success;
                        private boolean alreadyRated;
                        private String message;
                        
                        public boolean isSuccess() {
                            return success;
                        }
                        
                        public void setSuccess(boolean success) {
                            this.success = success;
                        }
                        
                        public boolean isAlreadyRated() {
                            return alreadyRated;
                        }
                        
                        public void setAlreadyRated(boolean alreadyRated) {
                            this.alreadyRated = alreadyRated;
                        }
                        
                        public String getMessage() {
                            return message;
                        }
                        
                        public void setMessage(String message) {
                            this.message = message;
                        }
                    }
                    
                    /**
                     * 踢帖结果
                     */
                    public static class KickPostResult {
                        private boolean success;
                        private String message;
                        private int currentCount; // 当前踢帖人数
                        
                        public boolean isSuccess() {
                            return success;
                        }
                        
                        public void setSuccess(boolean success) {
                            this.success = success;
                        }
                        
                        public String getMessage() {
                            return message;
                        }
                        
                        public void setMessage(String message) {
                            this.message = message;
                        }
                        
                        public int getCurrentCount() {
                            return currentCount;
                        }
                        
                        public void setCurrentCount(int currentCount) {
                            this.currentCount = currentCount;
                        }
                    }
                    
                    /**
                     * 获取踢帖信息
                     * 接口: GET plugin.php?id=bin_post_report&tid={tid}&m={m}&inajax=1
                     * @param tid 帖子ID
                     * @return 踢帖信息（包含m参数、当前踢帖人数、踢帖人列表）
                     */
                    public KickPostInfo getKickPostInfo(int tid) {
                        KickPostInfo info = new KickPostInfo();
                        info.setKickCount(-1); // 默认-1表示获取失败
                        
                        try {
                            // 先获取帖子页面提取m参数
                            String pageUrl = ApiConfig.BASE_URL + "forum.php?mod=viewthread&tid=" + tid + "&mobile=2";
                            Request pageRequest = new Request.Builder()
                                    .url(pageUrl)
                                    .header("User-Agent", ApiConfig.USER_AGENT)
                                    .header("Referer", ApiConfig.BASE_URL)
                                    .build();
                            
                            String mParam = null;
                            try (Response pageResponse = client.newCall(pageRequest).execute()) {
                                if (pageResponse.isSuccessful()) {
                                    String html = pageResponse.body().string();
                                    // 提取m参数: bin_post_report&tid=xxx&m=xxx
                                    // 注意: HTML中可能使用&或&amp;，需要兼容两种格式
                                    java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                                            "bin_post_report(?:&|&amp;)tid=" + tid + "(?:&|&amp;)m=([a-f0-9]+)");
                                    java.util.regex.Matcher matcher = pattern.matcher(html);
                                    if (matcher.find()) {
                                        mParam = matcher.group(1);
                                    }
                                }
                            }
                            
                            if (mParam == null) {
                                return info;
                            }
                            
                            info.setMParam(mParam);
                            
                            // 获取踢帖表单信息
                            String url = ApiConfig.BASE_URL + "plugin.php?id=bin_post_report&tid=" + tid + "&m=" + mParam + "&inajax=1";
                            Request request = new Request.Builder()
                                    .url(url)
                                    .header("User-Agent", ApiConfig.USER_AGENT)
                                    .header("Referer", ApiConfig.BASE_URL + "forum.php?mod=viewthread&tid=" + tid + "&mobile=2")
                                    .header("X-Requested-With", "XMLHttpRequest")
                                    .build();
                            
                            try (Response response = client.newCall(request).execute()) {
                                if (response.isSuccessful()) {
                                    String content = response.body().string();
                                    
                                    // 提取当前踢帖人数
                                    java.util.regex.Pattern countPattern = java.util.regex.Pattern.compile(
                                            "当前踢帖人数：(\\d+)");
                                    java.util.regex.Matcher countMatcher = countPattern.matcher(content);
                                    if (countMatcher.find()) {
                                        info.setKickCount(Integer.parseInt(countMatcher.group(1)));
                                    } else {
                                        info.setKickCount(0); // 未找到人数则默认0
                                    }
                                    
                                    // 解析踢帖人列表
                                    // 格式: <a href="home.php?mod=space&uid=xxx">用户名</a>
                                    java.util.List<KickPostUser> kickUsers = new java.util.ArrayList<>();
                                    java.util.regex.Pattern userPattern = java.util.regex.Pattern.compile(
                                            "<a[^>]*href=\"[^\"]*uid=(\\d+)[^\"]*\"[^>]*>([^<]+)</a>");
                                    java.util.regex.Matcher userMatcher = userPattern.matcher(content);
                                    while (userMatcher.find()) {
                                        KickPostUser user = new KickPostUser();
                                        user.setUid(Integer.parseInt(userMatcher.group(1)));
                                        user.setUsername(userMatcher.group(2).trim());
                                        user.setAvatar(ApiConfig.BASE_URL + "uc_server/avatar.php?uid=" + user.getUid() + "&size=small");
                                        kickUsers.add(user);
                                    }
                                    info.setKickUsers(kickUsers);
                                }
                            }
                            
                        } catch (Exception e) {
                            android.util.Log.e("ForumApi", "getKickPostInfo error", e);
                        }
                        
                        return info;
                    }
                    
                    /**
                     * 踢帖
                     * 接口: POST plugin.php?id=bin_post_report&tid={tid}&m={m}&submit_btn=true&inajax=1
                     * @param tid 帖子ID
                     * @param mParam m参数（从页面提取）
                     * @param reason 踢帖理由
                     * @return 踢帖结果
                     */
                    public KickPostResult kickPost(int tid, String mParam, String reason) {
                        KickPostResult result = new KickPostResult();
                        
                        try {
                            // 获取formhash
                            String formhash = getFormhash();
                            if (formhash == null || formhash.isEmpty()) {
                                result.setSuccess(false);
                                result.setMessage("未登录或formhash获取失败");
                                return result;
                            }
                            
                            // 构建请求URL
                            String url = ApiConfig.BASE_URL + "plugin.php?id=bin_post_report&tid=" + tid + 
                                    "&m=" + mParam + "&submit_btn=true&inajax=1";
                            
                            // 构建请求体
                            StringBuilder bodyBuilder = new StringBuilder();
                            bodyBuilder.append("formhash=").append(java.net.URLEncoder.encode(formhash, "UTF-8"));
                            bodyBuilder.append("&tid=").append(tid);
                            bodyBuilder.append("&report_text=").append(java.net.URLEncoder.encode(reason, "UTF-8"));
                            
                            RequestBody body = RequestBody.create(
                                    bodyBuilder.toString(),
                                    MediaType.parse("application/x-www-form-urlencoded")
                            );
                            
                            Request request = new Request.Builder()
                                    .url(url)
                                    .post(body)
                                    .header("X-Requested-With", "XMLHttpRequest")
                                    .header("Referer", ApiConfig.BASE_URL + "forum.php?mod=viewthread&tid=" + tid + "&mobile=2")
                                    .build();
                            
                            try (Response response = client.newCall(request).execute()) {
                                if (response.isSuccessful()) {
                                    String content = response.body().string();
                                    
                                    // 解析XML响应
                                    if (content.contains("举报成功") || content.contains("踢帖成功")) {
                                        result.setSuccess(true);
                                        result.setMessage("踢帖成功");
                                    } else if (content.contains("已踢帖") || content.contains("已经踢帖")) {
                                        result.setSuccess(false);
                                        result.setMessage("您已经踢过这个帖子了");
                                    } else if (content.contains("权限")) {
                                        result.setSuccess(false);
                                        result.setMessage("没有踢帖权限");
                                    } else if (content.contains("登录")) {
                                        result.setSuccess(false);
                                        result.setMessage("请先登录");
                                    } else {
                                        result.setSuccess(false);
                                        result.setMessage("踢帖失败，请稍后重试");
                                    }
                                } else {
                                    result.setSuccess(false);
                                    result.setMessage("网络请求失败");
                                }
                            }
                            
                        } catch (Exception e) {
                            android.util.Log.e("ForumApi", "kickPost error", e);
                            result.setSuccess(false);
                            result.setMessage("发生错误: " + e.getMessage());
                        }
                        
                        return result;
                    }
                    
                    /**
                     * 踢帖信息类（用于显示踢帖人数和踢帖人列表）
                     */
                    public static class KickPostInfo {
                        private int kickCount;    // 当前踢帖人数
                        private String mParam;    // m参数
                        private java.util.List<KickPostUser> kickUsers; // 踢帖人列表
                        
                        public int getKickCount() { return kickCount; }
                        public void setKickCount(int kickCount) { this.kickCount = kickCount; }
                        public String getMParam() { return mParam; }
                        public void setMParam(String mParam) { this.mParam = mParam; }
                        public java.util.List<KickPostUser> getKickUsers() { return kickUsers; }
                        public void setKickUsers(java.util.List<KickPostUser> kickUsers) { this.kickUsers = kickUsers; }
                    }
                    
                    /**
                     * 踢帖用户信息
                     */
                    public static class KickPostUser {
                        private int uid;
                        private String username;
                        private String avatar;
                        
                        public int getUid() { return uid; }
                        public void setUid(int uid) { this.uid = uid; }
                        public String getUsername() { return username; }
                        public void setUsername(String username) { this.username = username; }
                        public String getAvatar() { return avatar; }
                        public void setAvatar(String avatar) { this.avatar = avatar; }
                    }
                    
                    /**
                     * 举报评论
                     * 接口: GET misc.php?mod=report&rtype=post&rid={pid}&tid={tid}&fid={fid}&mobile=2&inajax=1
                     *       POST misc.php?mod=report... (提交举报)
                     * @param fid 版块ID
                     * @param tid 帖子ID
                     * @param pid 评论ID (rid)
                     * @param reason 举报理由
                     * @return 举报结果
                     */
                    public ReportCommentResult reportComment(int fid, int tid, int pid, String reason) {
                        ReportCommentResult result = new ReportCommentResult();
                        
                        try {
                            // 获取formhash
                            String formhash = getFormhash();
                            if (formhash == null || formhash.isEmpty()) {
                                result.setSuccess(false);
                                result.setMessage("未登录或formhash获取失败");
                                return result;
                            }
                            
                            // 步骤1: 获取举报表单
                            String getUrl = ApiConfig.BASE_URL + "misc.php?mod=report&rtype=post&rid=" + pid 
                                    + "&tid=" + tid + "&fid=" + fid + "&mobile=2&inajax=1";
                            
                            Request getRequest = new Request.Builder()
                                    .url(getUrl)
                                    .header("User-Agent", ApiConfig.USER_AGENT)
                                    .header("Referer", ApiConfig.BASE_URL + "forum.php?mod=viewthread&tid=" + tid + "&mobile=2")
                                    .header("X-Requested-With", "XMLHttpRequest")
                                    .build();
                            
                            String reportUrl = null;
                            try (Response getResponse = client.newCall(getRequest).execute()) {
                                if (getResponse.isSuccessful()) {
                                    String content = getResponse.body().string();
                                    // 从表单中提取action URL
                                    // 格式: <form method="post" action="misc.php?mod=report&rtype=post&rid=xxx...">
                                    java.util.regex.Pattern actionPattern = java.util.regex.Pattern.compile(
                                            "action=\"([^\"]*mod=report[^\"]*)\"");
                                    java.util.regex.Matcher actionMatcher = actionPattern.matcher(content);
                                    if (actionMatcher.find()) {
                                        reportUrl = actionMatcher.group(1);
                                        // 处理相对URL
                                        if (reportUrl.startsWith("/")) {
                                            reportUrl = ApiConfig.BASE_URL + reportUrl.substring(1);
                                        } else if (!reportUrl.startsWith("http")) {
                                            reportUrl = ApiConfig.BASE_URL + reportUrl;
                                        }
                                    }
                                    
                                    // 如果没有找到action，使用默认URL
                                    if (reportUrl == null) {
                                        reportUrl = ApiConfig.BASE_URL + "misc.php?mod=report&rtype=post&rid=" + pid 
                                                + "&tid=" + tid + "&fid=" + fid + "&mobile=2";
                                    }
                                }
                            }
                            
                            // 步骤2: 提交举报
                            StringBuilder bodyBuilder = new StringBuilder();
                            bodyBuilder.append("formhash=").append(java.net.URLEncoder.encode(formhash, "UTF-8"));
                            bodyBuilder.append("&reportsubmit=true");
                            bodyBuilder.append("&rtype=post");
                            bodyBuilder.append("&rid=").append(pid);
                            bodyBuilder.append("&tid=").append(tid);
                            bodyBuilder.append("&fid=").append(fid);
                            bodyBuilder.append("&reason=").append(java.net.URLEncoder.encode(reason, "UTF-8"));
                            
                            RequestBody body = RequestBody.create(
                                    bodyBuilder.toString(),
                                    MediaType.parse("application/x-www-form-urlencoded")
                            );
                            
                            // 添加inajax参数获取XML响应
                            String postUrl = reportUrl;
                            if (!postUrl.contains("inajax=")) {
                                postUrl += (postUrl.contains("?") ? "&" : "?") + "inajax=1";
                            }
                            
                            Request postRequest = new Request.Builder()
                                    .url(postUrl)
                                    .post(body)
                                    .header("User-Agent", ApiConfig.USER_AGENT)
                                    .header("X-Requested-With", "XMLHttpRequest")
                                    .header("Referer", ApiConfig.BASE_URL + "forum.php?mod=viewthread&tid=" + tid + "&mobile=2")
                                    .build();
                            
                            try (Response postResponse = client.newCall(postRequest).execute()) {
                                if (postResponse.isSuccessful()) {
                                    String content = postResponse.body().string();
                                    
                                    // 解析响应
                                    if (content.contains("举报成功") || content.contains("感谢您的举报")) {
                                        result.setSuccess(true);
                                        result.setMessage("举报成功，感谢您的反馈");
                                    } else if (content.contains("已经举报") || content.contains("已举报")) {
                                        result.setSuccess(false);
                                        result.setMessage("您已经举报过此内容");
                                    } else if (content.contains("权限") || content.contains("无权")) {
                                        result.setSuccess(false);
                                        result.setMessage("没有举报权限");
                                    } else if (content.contains("登录")) {
                                        result.setSuccess(false);
                                        result.setMessage("请先登录");
                                    } else if (content.contains("太快")) {
                                        result.setSuccess(false);
                                        result.setMessage("操作太快，请稍后再试");
                                    } else {
                                        // 尝试从响应中提取错误消息
                                        java.util.regex.Pattern msgPattern = java.util.regex.Pattern.compile(
                                                "<p[^>]*>([^<]+)</p>");
                                        java.util.regex.Matcher msgMatcher = msgPattern.matcher(content);
                                        if (msgMatcher.find()) {
                                            String msg = msgMatcher.group(1).trim();
                                            if (!msg.isEmpty()) {
                                                result.setMessage(msg);
                                            } else {
                                                result.setMessage("举报失败，请稍后重试");
                                            }
                                        } else {
                                            result.setSuccess(false);
                                            result.setMessage("举报失败，请稍后重试");
                                        }
                                    }
                                } else {
                                    result.setSuccess(false);
                                    result.setMessage("网络请求失败");
                                }
                            }
                            
                        } catch (Exception e) {
                            android.util.Log.e("ForumApi", "reportComment error", e);
                            result.setSuccess(false);
                            result.setMessage("发生错误: " + e.getMessage());
                        }
                        
                        return result;
                    }
                    
                    /**
                     * 举报评论结果
                     */
                    public static class ReportCommentResult {
                        private boolean success;
                        private String message;
                        
                        public boolean isSuccess() { return success; }
                        public void setSuccess(boolean success) { this.success = success; }
                        public String getMessage() { return message; }
                        public void setMessage(String message) { this.message = message; }
                    }
                    
                    // ==================== 编辑帖子相关 API ====================
                    
                    /**
                     * 获取帖子编辑页面数据
                     * @param fid 版块ID
                     * @param tid 帖子ID
                     * @param pid 楼层ID（主帖的pid）
                     * @return 编辑页面数据
                     */
                    public EditPostData getEditPostData(int fid, int tid, int pid) {
                        EditPostData data = new EditPostData();
                        
                        try {
                            // 构建请求URL
                            String url = ApiConfig.BASE_URL + "forum.php?mod=post&action=edit&fid=" + fid 
                                    + "&tid=" + tid + "&pid=" + pid + "&mobile=2";
                            
                            Request request = new Request.Builder()
                                    .url(url)
                                    .get()
                                    .build();
                            
                            Response response = client.newCall(request).execute();
                            
                            if (response.isSuccessful()) {
                                String html = response.body().string();
                                parseEditPostData(html, data);
                            }
                        } catch (Exception e) {
                            android.util.Log.e("ForumApi", "getEditPostData error", e);
                            data.setSuccess(false);
                            data.setErrorMessage("获取编辑数据失败: " + e.getMessage());
                        }
                        
                        return data;
                    }
                    
                    /**
                     * 解析编辑页面HTML数据
                     */
                    private void parseEditPostData(String html, EditPostData data) {
                        try {
                            org.jsoup.nodes.Document doc = org.jsoup.Jsoup.parse(html);
                            
                            // 解析 fid - 从隐藏字段获取
                            org.jsoup.nodes.Element fidInput = doc.selectFirst("input[name=fid]");
                            if (fidInput != null) {
                                try {
                                    data.setFid(Integer.parseInt(fidInput.attr("value")));
                                } catch (NumberFormatException ignored) {}
                            }
                            
                            // 如果没有从隐藏字段获取到，尝试从URL参数获取
                            if (data.getFid() == 0) {
                                java.util.regex.Pattern fidPattern = java.util.regex.Pattern.compile("fid=(\\d+)");
                                java.util.regex.Matcher fidMatcher = fidPattern.matcher(html);
                                if (fidMatcher.find()) {
                                    try {
                                        data.setFid(Integer.parseInt(fidMatcher.group(1)));
                                    } catch (NumberFormatException ignored) {}
                                }
                            }
                            
                            // 解析 formhash
                            org.jsoup.nodes.Element formhashInput = doc.selectFirst("input[name=formhash]");
                            if (formhashInput != null) {
                                data.setFormhash(formhashInput.attr("value"));
                            }
                            
                            // 解析 posttime
                            org.jsoup.nodes.Element posttimeInput = doc.selectFirst("input[name=posttime]");
                            if (posttimeInput != null) {
                                data.setPosttime(posttimeInput.attr("value"));
                            }
                            
                            // 解析标题
                            org.jsoup.nodes.Element subjectInput = doc.selectFirst("input[name=subject]");
                            if (subjectInput != null) {
                                data.setSubject(subjectInput.attr("value"));
                            }
                            
                            // 解析内容
                            org.jsoup.nodes.Element messageTextarea = doc.selectFirst("textarea[name=message]");
                            if (messageTextarea != null) {
                                data.setContentMessage(messageTextarea.text());
                            }
                            
                            // 解析已有附件 - aid属性在span元素上
                            // 结构: <li><span aid="354832">...</span><span class="p_img"><a><img src="..." title="filename"></a></span>...</li>
                            org.jsoup.select.Elements attachItems = doc.select("#imglist li");
                            for (org.jsoup.nodes.Element li : attachItems) {
                                // 跳过上传按钮
                                if (li.hasClass("up_btn")) continue;
                                
                                // aid在span元素上
                                org.jsoup.nodes.Element aidSpan = li.selectFirst("span[aid]");
                                if (aidSpan == null) continue;
                                
                                int aid = Integer.parseInt(aidSpan.attr("aid"));
                                
                                // 图片信息在.p_img下的img
                                org.jsoup.nodes.Element img = li.selectFirst(".p_img img");
                                String imgSrc = img != null ? img.attr("src") : "";
                                String title = img != null ? img.attr("title") : "";
                                
                                AttachmentInfo attach = new AttachmentInfo();
                                attach.setAid(aid);
                                attach.setUrl(imgSrc);
                                attach.setFilename(title);
                                data.addAttachment(attach);
                                
                                android.util.Log.d("ForumApi", "Found attachment: aid=" + aid + ", url=" + imgSrc);
                            }
                            
                            // 解析 uploadHash 和 uid（用于上传新附件）
                            // 格式: uploadformdata:{uid:"1398", hash:"xxx"}
                            String pageHtml = doc.html();
                            
                            // 解析 uid
                            java.util.regex.Pattern uidPattern = java.util.regex.Pattern.compile(
                                    "uid:\\s*[\"']?(\\d+)[\"']?");
                            java.util.regex.Matcher uidMatcher = uidPattern.matcher(pageHtml);
                            if (uidMatcher.find()) {
                                try {
                                    data.setUid(Integer.parseInt(uidMatcher.group(1)));
                                    android.util.Log.d("ForumApi", "Found uid: " + uidMatcher.group(1));
                                } catch (NumberFormatException e) {
                                    android.util.Log.w("ForumApi", "Failed to parse uid");
                                }
                            }
                            
                            // 解析 hash
                            java.util.regex.Pattern hashPattern = java.util.regex.Pattern.compile(
                                    "hash:\\s*[\"']([^\"']+)[\"']");
                            java.util.regex.Matcher hashMatcher = hashPattern.matcher(pageHtml);
                            if (hashMatcher.find()) {
                                data.setUploadHash(hashMatcher.group(1));
                                android.util.Log.d("ForumApi", "Found uploadHash: " + hashMatcher.group(1));
                            } else {
                                android.util.Log.w("ForumApi", "uploadHash not found in page");
                            }
                            
                            data.setSuccess(true);
                        } catch (Exception e) {
                            android.util.Log.e("ForumApi", "parseEditPostData error", e);
                            data.setSuccess(false);
                            data.setErrorMessage("解析编辑数据失败");
                        }
                    }
                    
                    /**
                     * 提交编辑帖子
                     * @param fid 版块ID
                     * @param tid 帖子ID
                     * @param pid 楼层ID（主帖的pid）
                     * @param subject 标题
                     * @param message 内容
                     * @param formhash 表单验证
                     * @param posttime 发布时间戳
                     * @return 编辑结果
                     */
                    public ApiResponse<Boolean> editPost(int fid, int tid, int pid, String subject, 
                            String message, String formhash, String posttime) {
                        return editPost(fid, tid, pid, subject, message, formhash, posttime, null, 0);
                    }
                    
                    /**
                     * 提交编辑帖子（支持删除）
                     * @param fid 版块ID
                     * @param tid 帖子ID
                     * @param pid 楼层ID
                     * @param subject 标题
                     * @param message 内容
                     * @param formhash 表单验证
                     * @param posttime 发布时间戳
                     * @param newAttachIds 新上传的附件ID列表
                     * @param delete 是否删除帖子（1=删除）
                     * @return 编辑结果
                     */
                    public ApiResponse<Boolean> editPost(int fid, int tid, int pid, String subject, 
                            String message, String formhash, String posttime, 
                            java.util.List<Integer> newAttachIds, int delete) {
                        ApiResponse<Boolean> result = new ApiResponse<>();
                        result.setData(false);
                        
                        try {
                            // 构建请求URL
                            String url = ApiConfig.BASE_URL + "forum.php?mod=post&action=edit&extra=&editsubmit=yes&mobile=2&inajax=1";
                            
                            // 构建表单数据
                            okhttp3.FormBody.Builder formBuilder = new okhttp3.FormBody.Builder()
                                    .add("formhash", formhash != null ? formhash : "")
                                    .add("posttime", posttime != null ? posttime : String.valueOf(System.currentTimeMillis() / 1000))
                                    .add("delete", String.valueOf(delete))
                                    .add("fid", String.valueOf(fid))
                                    .add("tid", String.valueOf(tid))
                                    .add("pid", String.valueOf(pid))
                                    .add("page", "1")
                                    .add("editsubmit", "yes");
                            
                            // 添加标题（如果有）
                            if (subject != null && !subject.isEmpty()) {
                                formBuilder.add("subject", subject);
                            }
                            
                            // 添加内容
                            formBuilder.add("message", message != null ? message : "");
                            
                            // 添加新上传的附件
                            if (newAttachIds != null) {
                                for (Integer aid : newAttachIds) {
                                    formBuilder.add("attachnew[" + aid + "][description]", "");
                                    formBuilder.add("attachnew[" + aid + "][readperm]", "");
                                    formBuilder.add("attachnew[" + aid + "][price]", "");
                                }
                            }
                            
                            Request request = new Request.Builder()
                                    .url(url)
                                    .post(formBuilder.build())
                                    .build();
                            
                            Response response = client.newCall(request).execute();
                            
                            if (response.isSuccessful()) {
                                String xml = response.body().string();
                                
                                // 判断编辑是否成功
                                if (xml.contains("编辑成功") || xml.contains("已编辑") || 
                                        xml.contains("操作成功") || xml.contains("succeed")) {
                                    result.setSuccess(true);
                                    result.setData(true);
                                    result.setMessage(delete == 1 ? "帖子已删除" : "编辑成功");
                                } else if (xml.contains("太快")) {
                                    result.setSuccess(false);
                                    result.setMessage("操作太快，请稍后再试");
                                } else if (xml.contains("权限")) {
                                    result.setSuccess(false);
                                    result.setMessage("没有编辑权限");
                                } else {
                                    // 尝试提取错误消息
                                    java.util.regex.Pattern msgPattern = java.util.regex.Pattern.compile(
                                            "<p[^>]*>([^<]+)</p>");
                                    java.util.regex.Matcher msgMatcher = msgPattern.matcher(xml);
                                    if (msgMatcher.find()) {
                                        String msg = msgMatcher.group(1).trim();
                                        result.setMessage(msg);
                                    } else {
                                        result.setMessage("编辑失败");
                                    }
                                }
                            } else {
                                result.setSuccess(false);
                                result.setMessage("网络请求失败");
                            }
                        } catch (Exception e) {
                            android.util.Log.e("ForumApi", "editPost error", e);
                            result.setSuccess(false);
                            result.setMessage("编辑出错: " + e.getMessage());
                        }
                        
                        return result;
                    }
                    
                    /**
                     * 删除帖子（实际上是编辑帖子并设置delete=1）
                     */
                    public ApiResponse<Boolean> deletePost(int fid, int tid, int pid, String formhash) {
                        return editPost(fid, tid, pid, null, "", formhash, 
                                String.valueOf(System.currentTimeMillis() / 1000), null, 1);
                    }
                    
                    // ==================== 编辑帖子数据模型 ====================
                    
                    /**
                     * 编辑帖子数据
                     */
                    public static class EditPostData {
                        private boolean success;
                        private String errorMessage;
                        private int fid;
                        private int uid;  // 用户ID（用于上传）
                        private String formhash;
                        private String posttime;
                        private String subject;
                        private String contentMessage;
                        private String uploadHash;
                        private java.util.List<AttachmentInfo> attachments = new java.util.ArrayList<>();
                        
                        public boolean isSuccess() { return success; }
                        public void setSuccess(boolean success) { this.success = success; }
                        public String getErrorMessage() { return errorMessage; }
                        public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }
                        public int getFid() { return fid; }
                        public void setFid(int fid) { this.fid = fid; }
                        public int getUid() { return uid; }
                        public void setUid(int uid) { this.uid = uid; }
                        public String getFormhash() { return formhash; }
                        public void setFormhash(String formhash) { this.formhash = formhash; }
                        public String getPosttime() { return posttime; }
                        public void setPosttime(String posttime) { this.posttime = posttime; }
                        public String getSubject() { return subject; }
                        public void setSubject(String subject) { this.subject = subject; }
                        public String getContentMessage() { return contentMessage; }
                        public void setContentMessage(String contentMessage) { this.contentMessage = contentMessage; }
                        public String getUploadHash() { return uploadHash; }
                        public void setUploadHash(String uploadHash) { this.uploadHash = uploadHash; }
                        public java.util.List<AttachmentInfo> getAttachments() { return attachments; }
                        public void addAttachment(AttachmentInfo attach) { this.attachments.add(attach); }
                    }
                    
                    /**
                     * 附件信息
                     */
                    public static class AttachmentInfo {
                        private int aid;
                        private String url;
                        private String filename;
                        private String description;
                        
                        public int getAid() { return aid; }
                        public void setAid(int aid) { this.aid = aid; }
                        public String getUrl() { return url; }
                        public void setUrl(String url) { this.url = url; }
                        public String getFilename() { return filename; }
                        public void setFilename(String filename) { this.filename = filename; }
                        public String getDescription() { return description; }
                        public void setDescription(String description) { this.description = description; }
                    }
                }
