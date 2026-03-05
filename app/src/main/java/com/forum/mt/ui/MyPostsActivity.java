package com.forum.mt.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.forum.mt.R;
import com.forum.mt.api.ForumApi;
import com.forum.mt.api.HttpClient;
import com.forum.mt.databinding.ActivityMyPostsBinding;
import com.forum.mt.model.ApiResponse;
import com.forum.mt.model.Post;
import com.forum.mt.ui.adapter.PostAdapter;
import com.forum.mt.ui.UserDetailActivity;
import com.forum.mt.util.LogToFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 我的帖子页面
 * 显示当前用户发布的所有帖子
 */
public class MyPostsActivity extends AppCompatActivity implements PostAdapter.OnItemClickListener {

    private ActivityMyPostsBinding binding;
    private ForumApi forumApi;
    private PostAdapter adapter;
    
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private List<Post> allPosts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 启用edge-to-edge显示
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        binding = ActivityMyPostsBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // 根据当前主题设置状态栏图标颜色
        setupStatusBar();
        
        // 初始化API
        HttpClient httpClient = HttpClient.getInstance(this);
        forumApi = new ForumApi(httpClient.getOkHttpClient(), httpClient.getCookieManager());
        
        // 设置Toolbar
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle("我的帖子");
        }
        
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        
        // 初始化RecyclerView
        setupRecyclerView();
        
        // 设置下拉刷新
        binding.swipeRefresh.setColorSchemeResources(R.color.primary);
        binding.swipeRefresh.setOnRefreshListener(() -> {
            refreshPosts();
        });
        
        // 检查登录状态
        if (!httpClient.isLoggedIn()) {
            showEmptyState("请先登录");
            return;
        }
        
        // 加载数据
        loadPosts();
    }
    
    private void setupRecyclerView() {
        adapter = new PostAdapter(this);
        adapter.setOnUserClickListener((uid, username, avatar, avatarView) -> {
            // 打开用户详情页，带共享元素动画
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
                ApiResponse<List<Post>> response = forumApi.getMyThreads(currentPage);
                
                runOnUiThread(() -> {
                    isLoading = false;
                    hideLoading();
                    
                    if (response.isSuccess() && response.getData() != null) {
                        List<Post> posts = response.getData();
                        
                        if (posts.isEmpty()) {
                            if (currentPage == 1) {
                                showEmptyState("还没有发布过帖子");
                            }
                            hasMore = false;
                        } else {
                            allPosts.addAll(posts);
                            adapter.submitList(new ArrayList<>(allPosts));
                            hasMore = posts.size() >= 20; // 每页20条
                            
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
                LogToFile.e("MyPostsActivity", "加载帖子异常: " + e.getMessage());
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
        // 跳转到帖子详情页，使用共享元素动画
        if (avatarView != null) {
            androidx.core.app.ActivityOptionsCompat options =
                androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this,
                    avatarView,
                    "avatar_transition"
                );
            PostDetailActivity.start(this,
                post.getTid(),
                post.getTitle(),
                post.getForumName(),
                post.getAuthor(),
                post.getAuthorAvatar(),
                post.getAuthorLevel(),
                options.toBundle());
        } else {
            // 无头像View，使用普通方式启动
            PostDetailActivity.startWithBasicInfo(this,
                post.getTid(),
                post.getTitle(),
                post.getForumName(),
                post.getAuthor(),
                post.getAuthorAvatar(),
                post.getAuthorLevel());
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