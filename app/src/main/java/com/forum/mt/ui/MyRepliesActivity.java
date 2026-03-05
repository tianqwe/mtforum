package com.forum.mt.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.forum.mt.R;
import com.forum.mt.api.ForumApi;
import com.forum.mt.api.HttpClient;
import com.forum.mt.databinding.ActivityMyRepliesBinding;
import com.forum.mt.model.ApiResponse;
import com.forum.mt.model.Post;
import com.forum.mt.ui.adapter.MyReplyAdapter;
import com.forum.mt.ui.UserDetailActivity;
import com.forum.mt.util.LogToFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 我的评论页面
 * 显示用户回复过的帖子列表
 */
public class MyRepliesActivity extends AppCompatActivity implements MyReplyAdapter.OnItemClickListener {

    private ActivityMyRepliesBinding binding;
    private ForumApi forumApi;
    private MyReplyAdapter adapter;
    private HttpClient httpClient;
    
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private List<Post> allPosts = new ArrayList<>();
    private int currentUid = 0; // 当前登录用户ID

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 启用edge-to-edge显示
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        binding = ActivityMyRepliesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // 根据当前主题设置状态栏图标颜色
        setupStatusBar();
        
        // 初始化API
        httpClient = HttpClient.getInstance(this);
        forumApi = new ForumApi(httpClient.getOkHttpClient(), httpClient.getCookieManager());
        
        // 设置Toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("我的评论");
        }
        
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        
        // 初始化RecyclerView
        setupRecyclerView();
        
        // 设置下拉刷新
        binding.swipeRefresh.setColorSchemeResources(R.color.primary);
        binding.swipeRefresh.setOnRefreshListener(this::refreshPosts);
        
        // 检查登录状态
        if (!httpClient.isLoggedIn()) {
            showEmptyState("请先登录");
            return;
        }
        
        // 获取当前用户UID
        loadCurrentUserUid();
        
        // 加载数据
        loadPosts();
    }
    
    /**
     * 获取当前登录用户UID
     */
    private void loadCurrentUserUid() {
        new Thread(() -> {
            try {
                ApiResponse<com.forum.mt.model.User> response = forumApi.getUserProfile();
                if (response.isSuccess() && response.getData() != null) {
                    currentUid = response.getData().getUid();
                }
            } catch (Exception e) {
                LogToFile.e("MyRepliesActivity", "获取用户UID失败: " + e.getMessage());
            }
        }).start();
    }
    
    private void setupRecyclerView() {
        adapter = new MyReplyAdapter();
        adapter.setOnItemClickListener(this);
        adapter.setOnUserClickListener((uid, username, avatar, avatarView) -> {
            UserDetailActivity.start(this, uid, username, avatar, avatarView);
        });
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.recyclerView.setLayoutManager(layoutManager);
        binding.recyclerView.setAdapter(adapter);
        
        // 滚动加载更多
        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                if (dy > 0) { // 向下滚动
                    int totalItemCount = layoutManager.getItemCount();
                    int lastVisibleItem = layoutManager.findLastVisibleItemPosition();
                    
                    if (!isLoading && hasMore && totalItemCount <= lastVisibleItem + 2) {
                        loadMorePosts();
                    }
                }
            }
        });
    }
    
    private void loadPosts() {
        if (isLoading) return;
        
        isLoading = true;
        showLoading();
        
        new Thread(() -> {
            try {
                ApiResponse<List<Post>> response = forumApi.getMyReplies(currentPage);
                
                runOnUiThread(() -> {
                    isLoading = false;
                    hideLoading();
                    
                    if (response.isSuccess() && response.getData() != null) {
                        List<Post> posts = response.getData();
                        
                        if (posts.isEmpty()) {
                            if (currentPage == 1) {
                                showEmptyState("还没有发表过评论");
                            }
                            hasMore = false;
                        } else {
                            allPosts.addAll(posts);
                            adapter.submitList(new ArrayList<>(allPosts));
                            hasMore = posts.size() >= 20; // 每页约20条
                            
                            showContent();
                        }
                    } else {
                        if (currentPage == 1) {
                            showEmptyState(response.getMessage() != null ? 
                                response.getMessage() : "加载失败");
                        }
                        hasMore = false;
                    }
                });
            } catch (Exception e) {
                LogToFile.e("MyRepliesActivity", "加载评论异常: " + e.getMessage());
                runOnUiThread(() -> {
                    isLoading = false;
                    hideLoading();
                    showEmptyState("加载失败: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private void loadMorePosts() {
        if (!hasMore || isLoading) return;
        
        currentPage++;
        loadPosts();
    }
    
    private void refreshPosts() {
        currentPage = 1;
        hasMore = true;
        allPosts.clear();
        adapter.submitList(new ArrayList<>());
        loadPosts();
    }
    
    private void showLoading() {
        // 首次加载不显示空白加载页面，只显示下拉刷新
        binding.swipeRefresh.setRefreshing(true);
    }

    private void hideLoading() {
        binding.progressBar.setVisibility(View.GONE);
        binding.swipeRefresh.setRefreshing(false);
    }

    private void showContent() {
        binding.progressBar.setVisibility(View.GONE);
        binding.recyclerView.setVisibility(View.VISIBLE);
        binding.emptyLayout.setVisibility(View.GONE);
    }
    
    private void showEmptyState(String message) {
        binding.progressBar.setVisibility(View.GONE);
        binding.recyclerView.setVisibility(View.GONE);
        binding.emptyLayout.setVisibility(View.VISIBLE);
        binding.emptyText.setText(message);
        binding.emptyImage.setImageResource(R.drawable.ic_forum);
    }
    
    @Override
    public void onItemClick(Post post, View avatarView) {
        // 跳转到帖子详情页，传递当前用户UID用于高亮评论
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra("tid", post.getTid());
        intent.putExtra("title", post.getTitle());
        intent.putExtra("highlight_author_id", currentUid); // 传递用户UID用于高亮
        // 传递基本信息用于共享元素动画
        intent.putExtra("forum_name", post.getForumName());
        intent.putExtra("author", post.getAuthor());
        intent.putExtra("author_avatar", post.getAuthorAvatar());
        intent.putExtra("author_level", post.getAuthorLevel());

        if (avatarView != null) {
            // 使用共享元素动画启动Activity，头像平移到帖子详情页
            androidx.core.app.ActivityOptionsCompat options =
                androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this,
                    avatarView,
                    "avatar_transition"
                );
            startActivity(intent, options.toBundle());
        } else {
            startActivity(intent);
        }
    }
    
    /**
     * 根据当前主题设置状态栏图标颜色
     */
    private void setupStatusBar() {
        getWindow().getDecorView().post(() -> {
            int currentNightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                // 深色模式：状态栏使用浅色图标
                getWindow().getDecorView().setSystemUiVisibility(0);
            } else {
                // 浅色模式：状态栏使用深色图标
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
