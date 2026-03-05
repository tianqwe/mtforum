package com.forum.mt.ui;

import android.app.AlertDialog;
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
import com.forum.mt.databinding.ActivityMyFavoritesBinding;
import com.forum.mt.model.ApiResponse;
import com.forum.mt.model.Post;
import com.forum.mt.ui.adapter.FavoriteAdapter;
import com.forum.mt.ui.UserDetailActivity;
import com.forum.mt.util.LogToFile;

import java.util.ArrayList;
import java.util.List;

/**
 * 我的收藏页面
 * 显示当前用户收藏的所有帖子
 * 与浏览历史卡片样式保持一致
 */
public class MyFavoritesActivity extends AppCompatActivity implements FavoriteAdapter.OnItemClickListener {

    private ActivityMyFavoritesBinding binding;
    private ForumApi forumApi;
    private FavoriteAdapter adapter;
    
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private List<Post> allPosts = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 启用edge-to-edge显示
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        binding = ActivityMyFavoritesBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // 根据当前主题设置状态栏图标颜色
        setupStatusBar();
        
        // 初始化API
        HttpClient httpClient = HttpClient.getInstance(this);
        forumApi = new ForumApi(httpClient.getOkHttpClient(), httpClient.getCookieManager());
        
        // 设置Toolbar
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        
        // 初始化RecyclerView
        setupRecyclerView();
        
        // 设置下拉刷新
        binding.swipeRefresh.setOnRefreshListener(() -> {
            refreshFavorites();
        });
        
        // 检查登录状态
        if (!httpClient.isLoggedIn()) {
            showEmptyState("请先登录");
            return;
        }
        
        // 加载数据
        loadFavorites();
    }
    
    private void setupRecyclerView() {
        adapter = new FavoriteAdapter();
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
                        loadMoreFavorites();
                    }
                }
            }
        });
    }
    
    @Override
    public void onItemClick(Post post, int position, View avatarView) {
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
    
    @Override
    public void onUnfavoriteClick(Post post, int position) {
        showDeleteFavoriteDialog(post, position);
    }
    
    /**
     * 显示删除收藏确认对话框
     */
    private void showDeleteFavoriteDialog(Post post, int position) {
        new AlertDialog.Builder(this)
                .setTitle("取消收藏")
                .setMessage("确定要取消收藏 \"" + post.getTitle() + "\" 吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    deleteFavorite(post, position);
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 删除收藏
     */
    private void deleteFavorite(Post post, int position) {
        if (isLoading) return;
        
        int favId = post.getFavId();
        if (favId == 0) {
            Toast.makeText(this, "收藏ID不存在，无法取消收藏", Toast.LENGTH_SHORT).show();
            return;
        }
        
        isLoading = true;
        Toast.makeText(this, "正在取消收藏...", Toast.LENGTH_SHORT).show();
        
        new Thread(() -> {
            try {
                ApiResponse<Boolean> response = forumApi.deleteFavorite(favId);
                
                runOnUiThread(() -> {
                    isLoading = false;
                    
                    if (response.isSuccess()) {
                        // 从列表中移除
                        allPosts.remove(position);
                        adapter.submitList(new ArrayList<>(allPosts));
                        Toast.makeText(this, "已取消收藏", Toast.LENGTH_SHORT).show();
                        
                        // 如果列表为空，显示空状态
                        if (allPosts.isEmpty()) {
                            showEmptyState("还没有收藏过帖子");
                        }
                    } else {
                        Toast.makeText(this, "取消收藏失败: " + response.getMessage(), 
                            Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                LogToFile.e("MyFavoritesActivity", "删除收藏异常: " + e.getMessage());
                runOnUiThread(() -> {
                    isLoading = false;
                    Toast.makeText(this, "取消收藏失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    
    private void loadFavorites() {
        if (isLoading) return;
        
        isLoading = true;
        showLoading();
        
        new Thread(() -> {
            try {
                ApiResponse<List<Post>> response = forumApi.getMyFavorites(currentPage);
                
                runOnUiThread(() -> {
                    isLoading = false;
                    hideLoading();
                    
                    if (response.isSuccess() && response.getData() != null) {
                        List<Post> posts = response.getData();
                        
                        if (posts.isEmpty()) {
                            if (currentPage == 1) {
                                showEmptyState("还没有收藏过帖子");
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
                LogToFile.e("MyFavoritesActivity", "加载收藏异常: " + e.getMessage());
                runOnUiThread(() -> {
                    isLoading = false;
                    hideLoading();
                    showEmptyState("加载失败: " + e.getMessage());
                });
            }
        }).start();
    }
    
    private void loadMoreFavorites() {
        if (!hasMore || isLoading) return;
        
        currentPage++;
        loadFavorites();
    }
    
    private void refreshFavorites() {
        currentPage = 1;
        hasMore = true;
        allPosts.clear();
        adapter.submitList(new ArrayList<>());
        loadFavorites();
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
