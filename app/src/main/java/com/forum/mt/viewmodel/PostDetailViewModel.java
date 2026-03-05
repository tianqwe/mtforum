package com.forum.mt.viewmodel;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;
import androidx.lifecycle.ViewModel;
import androidx.lifecycle.ViewModelProvider;

import com.forum.mt.api.ForumApi;
import com.forum.mt.model.ApiResponse;
import com.forum.mt.model.Comment;
import com.forum.mt.model.Post;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 帖子详情页面 ViewModel
 * 使用 LiveData 管理状态，遵循谷歌推荐的架构
 */
public class PostDetailViewModel extends ViewModel {

    private final ForumApi forumApi;
    private final ExecutorService executor;

    // UI 状态
    private final MutableLiveData<PostDetailUiState> uiState = new MutableLiveData<>(new PostDetailUiState.Loading());

    // 点赞状态
    private final MutableLiveData<Boolean> isLiked = new MutableLiveData<>(false);

    // 收藏状态
    private final MutableLiveData<Boolean> isFavorited = new MutableLiveData<>(false);

    // 错误消息（用于通知UI显示Toast）
    private final MutableLiveData<String> errorMessage = new MutableLiveData<>();

    // 当前帖子ID
    private int currentTid = 0;

    // 当前帖子数据
    private Post currentPost;

    // 分页和排序状态
    private int currentPage = 1;
    private int totalPages = 1;
    private int currentOrderType = 1; // 默认最新（ordertype=1是最新，ordertype=2是最早，3=只看楼主）
    private int currentAuthorId = 0;  // 帖子作者ID，用于"只看楼主"
    private List<Comment> allComments = new java.util.ArrayList<>();

    // 评论加载状态
    private final MutableLiveData<Boolean> isLoadingComments = new MutableLiveData<>(false);
    
    // 内容更新通知（回复后检测隐藏内容）
    private final MutableLiveData<Post> contentUpdated = new MutableLiveData<>();

    public PostDetailViewModel(ForumApi forumApi) {
        this.forumApi = forumApi;
        this.executor = Executors.newSingleThreadExecutor();
    }

    public LiveData<PostDetailUiState> getUiState() {
        return uiState;
    }

    public LiveData<Boolean> getIsLiked() {
        return isLiked;
    }

    public LiveData<Boolean> getIsFavorited() {
        return isFavorited;
    }

    public LiveData<String> getErrorMessage() {
        return errorMessage;
    }

    public LiveData<Boolean> isLoadingComments() {
        return isLoadingComments;
    }
    
    public LiveData<Post> getContentUpdated() {
        return contentUpdated;
    }

    public int getCurrentPage() {
        return currentPage;
    }

    public int getTotalPages() {
        return totalPages;
    }

    public int getCurrentOrderType() {
        return currentOrderType;
    }

    public List<Comment> getAllComments() {
        return allComments;
    }

    /**
     * 加载帖子详情
     */
    public void loadPostDetail(int tid) {
        currentTid = tid;
        uiState.setValue(new PostDetailUiState.Loading());

        executor.execute(() -> {
            try {
                ApiResponse<Post> response = forumApi.getPostDetail(tid);

                if (response.isSuccess() && response.hasData()) {
                    currentPost = response.getData();
                    // 保存作者ID，用于"只看楼主"功能
                    if (currentPost != null) {
                        currentAuthorId = currentPost.getAuthorId();
                        // 更新点赞和收藏状态
                        isLiked.postValue(currentPost.isLiked());
                        isFavorited.postValue(currentPost.isFavorited());
                    }
                    uiState.postValue(new PostDetailUiState.Success(response.getData()));

                    // 额外检查点赞和收藏状态（通过API获取更准确的状态）
                    checkLikeAndFavoriteStatus(tid);

                    // 加载评论
                    loadComments(tid);
                } else {
                    uiState.postValue(new PostDetailUiState.Error(response.getMessage() != null ? 
                        response.getMessage() : "加载失败"));
                }
            } catch (Exception e) {
                uiState.postValue(new PostDetailUiState.Error(e.getMessage() != null ? 
                    e.getMessage() : "网络错误"));
            }
        });
    }

    /**
     * 加载评论
     */
    private void loadComments(int tid) {
        loadComments(tid, 1, currentOrderType);
    }

    /**
     * 检查收藏状态（通过API获取，HTML中无法判断）
     * 注意：点赞状态通过HTML解析已能准确识别，无需额外API检查
     */
    private void checkLikeAndFavoriteStatus(int tid) {
        executor.execute(() -> {
            try {
                // 只检查收藏状态（HTML中无法判断收藏状态）
                ApiResponse<Boolean> favoriteResponse = forumApi.checkFavoriteStatus(tid);
                if (favoriteResponse.isSuccess() && favoriteResponse.getData() != null) {
                    isFavorited.postValue(favoriteResponse.getData());
                    if (currentPost != null) {
                        currentPost.setFavorited(favoriteResponse.getData());
                    }
                }
            } catch (Exception e) {
                // 检查失败时保持HTML解析的状态
            }
        });
    }

    /**
     * 加载评论 (带分页和排序)
     * @param tid 帖子ID
     * @param page 页码
     * @param orderType 排序类型: 1=最新, 2=最早, 3=只看楼主
     */
    public void loadComments(int tid, int page, int orderType) {
        if (isLoadingComments.getValue() == Boolean.TRUE) {
            return;
        }

        // 如果是只看楼主但作者ID无效，回退到默认排序
        if (orderType == 3 && currentAuthorId <= 0) {
            orderType = 1;
        }
        
        currentOrderType = orderType;
        
        // 使用final变量传递给lambda
        final int finalOrderType = orderType;
        final int authorId = (finalOrderType == 3) ? currentAuthorId : 0;

        isLoadingComments.postValue(true);

        executor.execute(() -> {
            try {
                ApiResponse<List<Comment>> response = forumApi.getComments(tid, page, finalOrderType, authorId);

                if (response.isSuccess()) {
                    List<Comment> comments = response.getData();
                    // 即使评论为空也要更新UI（比如"只看楼主"可能返回空列表）
                    if (comments == null) {
                        comments = new java.util.ArrayList<>();
                    }

                    // 计算总页数 (每页约10条评论)
                    if (currentPost != null) {
                        int total = currentPost.getCommentTotal() > 0 ? 
                            currentPost.getCommentTotal() : currentPost.getReplies();
                        totalPages = (int) Math.ceil(total / 10.0);
                        if (totalPages < 1) totalPages = 1;
                    }

                    // 每次都替换评论列表（分页显示）
                    allComments = new java.util.ArrayList<>(comments);
                    currentPage = page;

                    // 更新状态
                    if (currentPost != null) {
                        uiState.postValue(new PostDetailUiState.Success(currentPost, allComments));
                    }
                }
            } catch (Exception e) {
                // ignore
            } finally {
                isLoadingComments.postValue(false);
            }
        });
    }

    /**
     * 加载更多评论 (下一页)
     */
    public void loadMoreComments() {
        if (currentPage < totalPages && currentTid > 0) {
            loadComments(currentTid, currentPage + 1, currentOrderType);
        }
    }

    /**
     * 切换排序方式并重新加载评论
     * @param orderType 排序类型: 1=最新, 2=最早
     */
    public void changeOrderType(int orderType) {
        if (currentTid <= 0) {
            return;
        }
        
        if (orderType != currentOrderType) {
            currentOrderType = orderType;
            currentPage = 1;
            // 不在这里清空列表，在 loadComments 成功后替换
            loadComments(currentTid, 1, orderType);
        }
    }
    
    /**
     * 刷新评论列表（回复成功后调用）
     */
    public void refreshComments() {
        if (currentTid <= 0) {
            return;
        }
        // 重新加载第一页评论
        currentPage = 1;
        loadComments(currentTid, 1, currentOrderType);
    }
    
    /**
     * 重新获取帖子内容（回复后检测隐藏内容）
     * 如果内容有变化则通知UI更新
     */
    public void refreshPostContent() {
        if (currentTid <= 0 || currentPost == null) {
            return;
        }
        
        String oldContent = currentPost.getContent();
        
        executor.execute(() -> {
            try {
                ApiResponse<Post> response = forumApi.getPostDetail(currentTid);
                
                if (response.isSuccess() && response.hasData()) {
                    Post newPost = response.getData();
                    String newContent = newPost.getContent();
                    
                    // 检查内容是否有变化
                    if (newContent != null && !newContent.equals(oldContent)) {
                        // 更新当前帖子数据
                        currentPost.setContent(newContent);
                        // 更新其他可能变化的字段
                        if (newPost.getFormhash() != null) {
                            currentPost.setFormhash(newPost.getFormhash());
                        }
                        if (newPost.getNoticeAuthor() != null) {
                            currentPost.setNoticeAuthor(newPost.getNoticeAuthor());
                        }
                        
                        // 通知UI内容已更新
                        contentUpdated.postValue(currentPost);
                    }
                }
            } catch (Exception e) {
                // ignore
            }
        });
    }

    /**
     * 跳转到指定页
     */
    public void goToPage(int page) {
        if (currentTid <= 0) {
            return;
        }
        
        if (page >= 1 && page <= totalPages && page != currentPage) {
            loadComments(currentTid, page, currentOrderType);
        }
    }

    /**
     * 是否有更多评论
     */
    public boolean hasMoreComments() {
        return currentPage < totalPages;
    }

    /**
     * 切换点赞状态
     */
    public void toggleLike() {
        if (currentTid <= 0) {
            return;
        }
        
        if (!forumApi.isLoggedIn()) {
            // 未登录时只更新UI状态
            Boolean current = isLiked.getValue();
            isLiked.setValue(current == null ? true : !current);
            return;
        }
        
        Boolean current = isLiked.getValue();
        boolean isCurrentlyLiked = current != null && current;
        
        executor.execute(() -> {
            try {
                com.forum.mt.api.ForumApi.RecommendResult result;
                
                if (!isCurrentlyLiked) {
                    // 点赞
                    com.forum.mt.model.ApiResponse<com.forum.mt.api.ForumApi.RecommendResult> response =
                        forumApi.recommendPost(currentTid);
                    
                    if (response.isSuccess() && response.hasData()) {
                        result = response.getData();
                        // 更新点赞数和状态
                        if (currentPost != null) {
                            if (result.getCount() > 0) {
                                currentPost.setLikes(result.getCount());
                            }
                            currentPost.setLiked(true);
                        }
                        isLiked.postValue(true);
                        errorMessage.postValue("点赞成功");
                    }
                } else {
                    // 取消点赞
                    com.forum.mt.model.ApiResponse<Boolean> response =
                        forumApi.unrecommendPost(currentTid);
                    
                    if (response.isSuccess()) {
                        // 更新点赞数和状态
                        if (currentPost != null) {
                            if (currentPost.getLikes() > 0) {
                                currentPost.setLikes(currentPost.getLikes() - 1);
                            }
                            currentPost.setLiked(false);
                        }
                        isLiked.postValue(false);
                        errorMessage.postValue("取消点赞成功");
                    } else {
                        // 取消点赞失败，恢复点赞状态并提示用户
                        isLiked.postValue(true);
                        errorMessage.postValue(response.getMessage());
                    }
                }
                
                // 通知UI更新（更新点赞数）
                if (currentPost != null) {
                    uiState.postValue(new PostDetailUiState.Success(currentPost, 
                            ((PostDetailUiState.Success) uiState.getValue()) != null ? 
                            ((PostDetailUiState.Success) uiState.getValue()).getComments() : 
                            new java.util.ArrayList<>()));
                }
            } catch (Exception e) {
                // 忽略错误
            }
        });
    }

    /**
     * 切换收藏状态
     */
    public void toggleFavorite() {
        if (currentTid <= 0) {
            return;
        }
        
        if (!forumApi.isLoggedIn()) {
            // 未登录时只更新UI状态
            Boolean current = isFavorited.getValue();
            isFavorited.setValue(current == null ? true : !current);
            return;
        }
        
        Boolean current = isFavorited.getValue();
        boolean isCurrentlyFavorited = current != null && current;
        
        executor.execute(() -> {
            try {
                com.forum.mt.model.ApiResponse<Boolean> response;
                
                if (!isCurrentlyFavorited) {
                    // 收藏
                    response = forumApi.favoritePost(currentTid, "手机收藏");
                    
                    if (response.isSuccess()) {
                        isFavorited.postValue(true);
                    }
                } else {
                    // 取消收藏
                    response = forumApi.unfavoritePost(currentTid);
                    
                    if (response.isSuccess()) {
                        isFavorited.postValue(false);
                    }
                }
            } catch (Exception e) {
                // 忽略错误
            }
        });
    }
    
    /**
     * 点赞评论（踢贴）
     * @param comment 要点赞的评论
     */
    public void toggleCommentLike(Comment comment) {
        if (currentTid <= 0 || comment == null) {
            return;
        }
        
        if (!forumApi.isLoggedIn()) {
            errorMessage.postValue("请先登录");
            return;
        }
        
        boolean isCurrentlyLiked = comment.isLiked();
        int currentLikes = comment.getLikes();
        
        executor.execute(() -> {
            try {
                if (!isCurrentlyLiked) {
                    // 点赞评论
                    com.forum.mt.model.ApiResponse<com.forum.mt.api.ForumApi.CommentLikeResult> response =
                            forumApi.recommendComment(currentTid, comment.getId());
                    
                    if (response.isSuccess()) {
                        com.forum.mt.api.ForumApi.CommentLikeResult result = response.getData();
                        if (result != null) {
                            // 更新评论状态
                            comment.setLiked(true);
                            if (result.getCount() > 0) {
                                comment.setLikes(result.getCount());
                            } else {
                                comment.setLikes(currentLikes + 1);
                            }
                            errorMessage.postValue("点赞成功");
                        }
                    } else {
                        errorMessage.postValue(response.getMessage());
                    }
                } else {
                    // 取消点赞评论
                    com.forum.mt.model.ApiResponse<Boolean> response =
                            forumApi.unrecommendComment(currentTid, comment.getId());
                    
                    if (response.isSuccess()) {
                        comment.setLiked(false);
                        if (currentLikes > 0) {
                            comment.setLikes(currentLikes - 1);
                        }
                        errorMessage.postValue("取消点赞");
                    } else {
                        errorMessage.postValue(response.getMessage());
                    }
                }
                
                // 通知UI更新评论列表
                if (currentPost != null) {
                    uiState.postValue(new PostDetailUiState.Success(currentPost, allComments));
                }
            } catch (Exception e) {
                errorMessage.postValue("网络错误: " + e.getMessage());
            }
        });
    }

    /**
     * 关注/取消关注作者
     */
    public void toggleFollow() {
        if (currentPost == null || currentPost.getAuthorId() == 0) {
            return;
        }
        
        int authorId = currentPost.getAuthorId();
        boolean isFollowed = currentPost.isFollowed();
        
        executor.execute(() -> {
            try {
                ApiResponse<Boolean> response;
                if (isFollowed) {
                    response = forumApi.unfollowUser(authorId);
                } else {
                    response = forumApi.followUser(authorId);
                }
                
                if (response.isSuccess()) {
                    currentPost.setFollowed(!isFollowed);
                    // 通知UI更新
                    uiState.postValue(new PostDetailUiState.Success(currentPost, 
                            ((PostDetailUiState.Success) uiState.getValue()).getComments()));
                }
            } catch (Exception e) {
                // ignore
            }
        });
    }

    /**
     * 重新加载
     */
    public void retry() {
        if (currentTid > 0) {
            loadPostDetail(currentTid);
        }
    }

    /**
     * 获取分享内容
     */
    public String getShareContent() {
        if (currentPost == null) return "";
        return currentPost.getTitle() + "\nhttps://bbs.binmt.cc/thread-" + currentTid + "-1-1.html";
    }

    /**
     * 检查是否已登录
     */
    public boolean isLoggedIn() {
        return forumApi.isLoggedIn();
    }

    /**
     * 获取当前帖子
     */
    public Post getCurrentPost() {
        return currentPost;
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        executor.shutdown();
    }

    /**
     * ViewModel 工厂
     */
    public static class Factory implements ViewModelProvider.Factory {
        private final ForumApi forumApi;

        public Factory(ForumApi forumApi) {
            this.forumApi = forumApi;
        }

        @SuppressWarnings("unchecked")
        @Override
        public <T extends ViewModel> T create(Class<T> modelClass) {
            if (modelClass.isAssignableFrom(PostDetailViewModel.class)) {
                return (T) new PostDetailViewModel(forumApi);
            }
            throw new IllegalArgumentException("Unknown ViewModel class");
        }
    }
}