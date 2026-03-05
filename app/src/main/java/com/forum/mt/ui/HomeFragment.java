package com.forum.mt.ui;

import android.app.ActivityOptions;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.core.util.Pair;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.forum.mt.R;
import com.forum.mt.api.ForumApi;
import com.forum.mt.api.HttpClient;
import com.forum.mt.databinding.FragmentHomeBinding;
import com.forum.mt.model.ApiResponse;
import com.forum.mt.model.Forum;
import com.forum.mt.model.Post;
import com.forum.mt.ui.adapter.HomeAdapter;
import com.forum.mt.ui.UserDetailActivity;

import android.content.Intent;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 首页Fragment - 谷歌推荐方式
 * 使用单一RecyclerView + HomeAdapter
 */
public class HomeFragment extends Fragment implements 
        HomeAdapter.OnForumClickListener,
        HomeAdapter.OnPostClickListener,
        HomeAdapter.OnLoadMoreListener,
        HomeAdapter.OnUserClickListener {
    
    private FragmentHomeBinding binding;
    private HomeAdapter homeAdapter;
    private ForumApi forumApi;
    private ExecutorService executor;
    
    // 分页相关
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private final List<Post> allPosts = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        executor = Executors.newSingleThreadExecutor();
        forumApi = new ForumApi(
            HttpClient.getInstance(requireContext()).getOkHttpClient(),
            HttpClient.getInstance(requireContext()).getCookieManager()
        );
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentHomeBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupRecyclerViews();
        setupSwipeRefresh();
        setupToolbar();
        
        // 加载所有数据
        loadAllData();
    }

    private void setupRecyclerViews() {
        // 使用HomeAdapter（单一RecyclerView，谷歌推荐方式）
        homeAdapter = new HomeAdapter();
        homeAdapter.setOnForumClickListener(this);
        homeAdapter.setOnPostClickListener(this);
        homeAdapter.setOnLoadMoreListener(this);
        homeAdapter.setOnUserClickListener(this);
        
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(homeAdapter);
        
        // 滚动监听 - 控制返回顶部按钮显示
        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            private boolean isScrollToTopVisible = false;
            
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    // 检查是否在顶部
                    int firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();
                    boolean isAtTop = firstVisiblePosition == 0;
                    
                    boolean shouldShow;
                    if (isAtTop) {
                        // 在顶部，隐藏按钮
                        shouldShow = false;
                    } else if (dy < 0) {
                        // 手指从上往下滑，显示按钮
                        shouldShow = true;
                    } else if (dy > 0) {
                        // 手指从下往上滑，隐藏按钮
                        shouldShow = false;
                    } else {
                        // dy == 0，保持当前状态
                        shouldShow = isScrollToTopVisible;
                    }
                    
                    // 只在状态变化时更新
                    if (shouldShow != isScrollToTopVisible) {
                        isScrollToTopVisible = shouldShow;
                        if (shouldShow) {
                            // 显示动画：淡入+放大
                            binding.fabScrollToTop.setVisibility(View.VISIBLE);
                            binding.fabScrollToTop.setAlpha(0f);
                            binding.fabScrollToTop.setScaleX(0.5f);
                            binding.fabScrollToTop.setScaleY(0.5f);
                            binding.fabScrollToTop.animate()
                                .alpha(1f)
                                .scaleX(1f)
                                .scaleY(1f)
                                .setDuration(200)
                                .setInterpolator(new android.view.animation.OvershootInterpolator())
                                .start();
                        } else {
                            // 隐藏动画：淡出+缩小
                            binding.fabScrollToTop.animate()
                                .alpha(0f)
                                .scaleX(0.5f)
                                .scaleY(0.5f)
                                .setDuration(150)
                                .setInterpolator(new android.view.animation.AccelerateInterpolator())
                                .withEndAction(() -> binding.fabScrollToTop.setVisibility(View.GONE))
                                .start();
                        }
                    }
                }
            }
        });
        
        // 返回顶部按钮点击事件
        binding.fabScrollToTop.setOnClickListener(v -> {
            binding.recyclerView.smoothScrollToPosition(0);
        });
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(
            com.google.android.material.R.color.design_default_color_primary
        );
        binding.swipeRefresh.setOnRefreshListener(this::refreshData);
    }

    private void setupToolbar() {
        binding.toolbar.setOnMenuItemClickListener(item -> {
            if (item.getItemId() == R.id.action_search) {
                // 跳转到搜索页面
                startActivity(new Intent(requireContext(), SearchActivity.class));
                return true;
            }
            return false;
        });
    }

    private void loadAllData() {
        // 重置分页状态
        currentPage = 1;
        hasMore = true;
        allPosts.clear();
        
        showLoading();
        
        executor.execute(() -> {
            // 并行加载板块列表、首页帖子、热帖排行
            ApiResponse<List<Forum>> forumResponse = forumApi.getForumList();
            ApiResponse<List<Post>> homeResponse = forumApi.getHomePagePosts(currentPage);
            ApiResponse<List<Post>> hotRankResponse = forumApi.getHotRank();
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    hideLoading();
                    
                    // 更新板块 - 过滤掉分类标题，只取真正的板块
                    if (forumResponse.isSuccess() && forumResponse.hasData()) {
                        List<Forum> allForums = forumResponse.getData();
                        List<Forum> realForums = new ArrayList<>();
                        for (Forum forum : allForums) {
                            if (!forum.isCategory()) {
                                realForums.add(forum);
                            }
                        }
                        if (realForums.size() > 8) {
                            realForums = realForums.subList(0, 8);
                        }
                        homeAdapter.setForums(realForums);
                    }
                    
                    // 更新热帖排行
                    if (hotRankResponse.isSuccess() && hotRankResponse.hasData()) {
                        homeAdapter.setHotRanks(hotRankResponse.getData());
                    }
                    
                    // 更新帖子列表
                    if (homeResponse.isSuccess() && homeResponse.hasData()) {
                        allPosts.clear();
                        allPosts.addAll(homeResponse.getData());
                        homeAdapter.setPosts(allPosts);
                        homeAdapter.setShowLoadMore(true);
                        showContent();
                    } else {
                        if (allPosts.isEmpty()) {
                            showError("加载失败");
                        }
                    }
                });
            }
        });
    }
    
    private void refreshData() {
        // 重置分页状态
        currentPage = 1;
        hasMore = true;
        allPosts.clear();
        
        executor.execute(() -> {
            ApiResponse<List<Post>> homeResponse = forumApi.getHomePagePosts(currentPage);
            ApiResponse<List<Post>> hotRankResponse = forumApi.getHotRank();
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    hideLoading();
                    
                    if (hotRankResponse.isSuccess() && hotRankResponse.hasData()) {
                        homeAdapter.setHotRanks(hotRankResponse.getData());
                    }
                    
                    if (homeResponse.isSuccess() && homeResponse.hasData()) {
                        allPosts.addAll(homeResponse.getData());
                        homeAdapter.setPosts(allPosts);
                        homeAdapter.setShowLoadMore(true);
                        showContent();
                    } else {
                        if (allPosts.isEmpty()) {
                            showError("加载失败");
                        }
                    }
                });
            }
        });
    }
    
    // HomeAdapter.OnLoadMoreListener 实现
    @Override
    public void onLoadMore() {
        if (isLoading || !hasMore) return;
        
        isLoading = true;
        
        executor.execute(() -> {
            currentPage++;
            ApiResponse<List<Post>> response = forumApi.getHomePagePosts(currentPage);
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    isLoading = false;
                    
                    if (response.isSuccess() && response.hasData()) {
                        List<Post> newPosts = response.getData();
                        if (newPosts.isEmpty()) {
                            hasMore = false;
                            homeAdapter.setHasMore(false);
                        } else {
                            allPosts.addAll(newPosts);
                            homeAdapter.addPosts(newPosts);
                            // 少于10条说明没更多了
                            if (newPosts.size() < 10) {
                                hasMore = false;
                                homeAdapter.setHasMore(false);
                            }
                        }
                    } else {
                        hasMore = false;
                        homeAdapter.setHasMore(false);
                        currentPage--;
                    }
                });
            }
        });
    }

    // HomeAdapter.OnPostClickListener 实现
    @Override
    public void onPostClick(Post post, View avatarView) {
        if (getActivity() != null) {
            if (avatarView != null) {
                // 使用共享元素动画启动Activity
                ActivityOptionsCompat options = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        getActivity(),
                        avatarView,
                        "avatar_transition"
                );
                PostDetailActivity.start(getActivity(), 
                        post.getTid(), 
                        post.getTitle(),
                        post.getForumName(),
                        post.getAuthor(),
                        post.getAuthorAvatar(),
                        post.getAuthorLevel(),
                        options.toBundle());
            } else {
                // 无头像View，使用普通方式启动，但仍传递基本信息
                PostDetailActivity.startWithBasicInfo(getActivity(), 
                        post.getTid(), 
                        post.getTitle(),
                        post.getForumName(),
                        post.getAuthor(),
                        post.getAuthorAvatar(),
                        post.getAuthorLevel());
            }
        }
    }
    
    // HomeAdapter.OnUserClickListener 实现
    @Override
    public void onUserClick(int uid, String username, String avatar, View avatarView) {
        if (getActivity() != null) {
            UserDetailActivity.start(requireActivity(), uid, username, avatar, avatarView);
        }
    }

    // HomeAdapter.OnForumClickListener 实现
    @Override
    public void onForumClick(Forum forum, View iconView) {
        if (getActivity() != null) {
            // 使用共享元素动画启动Activity，板块头像平移效果
            ActivityOptionsCompat options =
                ActivityOptionsCompat.makeSceneTransitionAnimation(
                    requireActivity(),
                    iconView,
                    "forum_icon_transition"
                );

            // 跳转到板块帖子列表页面
            Intent intent = new Intent(requireContext(), ForumPostListActivity.class);
            intent.putExtra(ForumPostListActivity.EXTRA_FID, forum.getFid());
            intent.putExtra(ForumPostListActivity.EXTRA_FORUM_NAME, forum.getName());
            intent.putExtra(ForumPostListActivity.EXTRA_FORUM_ICON, forum.getIcon());
            startActivity(intent, options.toBundle());
        }
    }

    private void showLoading() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.errorView.setVisibility(View.GONE);
    }

    private void hideLoading() {
        binding.progressBar.setVisibility(View.GONE);
        binding.swipeRefresh.setRefreshing(false);
    }

    private void showContent() {
        binding.errorView.setVisibility(View.GONE);
    }

    private void showError(String message) {
        binding.errorView.setVisibility(View.VISIBLE);
        binding.errorText.setText(message != null ? message : "加载失败");
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }
}