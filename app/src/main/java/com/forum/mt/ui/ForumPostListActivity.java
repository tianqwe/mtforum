package com.forum.mt.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.forum.mt.R;
import com.forum.mt.api.ForumApi;
import com.forum.mt.api.HttpClient;
import com.forum.mt.databinding.ActivityForumPostListBinding;
import com.forum.mt.model.ApiResponse;
import com.forum.mt.model.ForumInfo;
import com.forum.mt.model.Post;
import com.forum.mt.ui.adapter.ForumPostListAdapter;
import com.forum.mt.ui.UserDetailActivity;

import com.bumptech.glide.Glide;

import androidx.core.app.ActivityOptionsCompat;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 版块帖子列表页面
 * 显示指定版块的帖子列表，支持筛选和翻页
 */
public class ForumPostListActivity extends AppCompatActivity implements 
        ForumPostListAdapter.OnItemClickListener,
        ForumPostListAdapter.OnPaginationListener {
    
    public static final String EXTRA_FID = "fid";
    public static final String EXTRA_FORUM_NAME = "forum_name";
    public static final String EXTRA_FORUM_ICON = "forum_icon";

    private ActivityForumPostListBinding binding;
    private ForumPostListAdapter adapter;
    private ForumApi forumApi;
    private ExecutorService executor;
    
    private int fid;
    private String forumName;
    private String forumIcon;
    private int currentPage = 1;
    private int totalPages = 1;
    private String currentFilter = "all";
    
    private boolean isLoading = false;
    // 标志位：是否是首次加载
    private boolean isFirstLoad = true;
    // 标志位：是否正在下拉刷新
    private boolean isRefreshing = false;
    // 标志位：是否已启动进入过渡动画
    private boolean hasStartedEnterTransition = false;

    private ForumInfo forumInfo;
    private List<Post> cachedTopPosts = new ArrayList<>();
    private PopupWindow pageSelectorPopup;
    
    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 启用edge-to-edge显示
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        binding = ActivityForumPostListBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // 根据当前主题设置状态栏图标颜色
        setupStatusBar();

        fid = getIntent().getIntExtra(EXTRA_FID, 0);
        forumName = getIntent().getStringExtra(EXTRA_FORUM_NAME);
        forumIcon = getIntent().getStringExtra(EXTRA_FORUM_ICON);

        if (fid == 0) {
            Toast.makeText(this, "版块ID无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initToolbar();
        initViews();

        // 延迟启动进入过渡，等待数据加载完成（仅首次加载）
        if (isFirstLoad) {
            postponeEnterTransition();
        }

        // 先显示预览头部信息（名称、头像），确保过渡动画有目标
        showPreviewHeader();

        loadData();

        // 监听 RecyclerView 布局完成，启动过渡动画
        if (isFirstLoad) {
            binding.recyclerView.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            binding.recyclerView.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            if (!hasStartedEnterTransition) {
                                hasStartedEnterTransition = true;
                                supportStartPostponedEnterTransition();
                            }
                        }
                    });
        }
    }
    
    private void initToolbar() {
        setSupportActionBar(binding.toolbar);
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setTitle(forumName != null ? forumName : "版块");
        }
    }
    
    private void initViews() {
        forumApi = new ForumApi(
            HttpClient.getInstance(this).getOkHttpClient(),
            HttpClient.getInstance(this).getCookieManager()
        );
        executor = Executors.newSingleThreadExecutor();
        
        // 设置RecyclerView
        adapter = new ForumPostListAdapter();
        adapter.setOnItemClickListener(this);
        adapter.setOnPaginationListener(this);
        adapter.setOnUserClickListener((uid, username, avatar, avatarView) -> {
            UserDetailActivity.start(this, uid, username, avatar, avatarView);
        });
        
        // Tab切换监听
        adapter.setOnTabSelectedListener((position, filter) -> {
            if (!filter.equals(currentFilter)) {
                adapter.updateTabSelection(position);
                adapter.updateCurrentFilter(filter);
                currentFilter = filter;
                currentPage = 1; // 切换Tab时重置到第一页
                loadTabData("all".equals(filter));
            }
        });
        
        LinearLayoutManager layoutManager = new LinearLayoutManager(this);
        binding.recyclerView.setLayoutManager(layoutManager);
        binding.recyclerView.setAdapter(adapter);
        
        // 下拉刷新
        setupSwipeRefresh();
        
        // 滚动监听 - 控制返回顶部按钮和发帖按钮的联动动画
        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            private boolean isScrollToTopVisible = false;
            
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                
                LinearLayoutManager lm = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (lm != null) {
                    // 检查是否在顶部
                    int firstVisible = lm.findFirstVisibleItemPosition();
                    boolean isAtTop = firstVisible == 0;
                    
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
                    
                    // 只在状态变化时执行动画
                    if (shouldShow != isScrollToTopVisible) {
                        isScrollToTopVisible = shouldShow;
                        updateFabAnimations(shouldShow);
                    }
                }
            }
        });
        
        // 返回顶部
        binding.fabScrollToTop.setOnClickListener(v -> {
            binding.recyclerView.smoothScrollToPosition(0);
        });
        
        // 发帖按钮
        binding.fabNewPost.setOnClickListener(v -> {
            // 跳转到发帖页面
            NewThreadActivity.start(this, fid, forumName != null ? forumName : "版块");
        });
        
        // 重试按钮
        binding.retryButton.setOnClickListener(v -> loadData());
    }
    
    /**
     * 更新FAB按钮的动画效果
     * @param showScrollToTop 是否显示返回顶部按钮
     */
    private void updateFabAnimations(boolean showScrollToTop) {
        // 发帖按钮移动距离
        // 返回顶部按钮高度 56dp + 间距 12dp = 68dp
        float density = getResources().getDisplayMetrics().density;
        float translationY = 68 * density;
        
        if (showScrollToTop) {
            // 显示返回顶部按钮，发帖按钮上移
            binding.fabScrollToTop.setVisibility(View.VISIBLE);
            binding.fabScrollToTop.setAlpha(0f);
            binding.fabScrollToTop.setScaleX(0.5f);
            binding.fabScrollToTop.setScaleY(0.5f);
            
            // 返回顶部按钮淡入+放大动画
            binding.fabScrollToTop.animate()
                    .alpha(1f)
                    .scaleX(1f)
                    .scaleY(1f)
                    .setDuration(250)
                    .setInterpolator(new android.view.animation.OvershootInterpolator())
                    .start();
            
            // 发帖按钮上移动画
            binding.fabNewPost.animate()
                    .translationY(-translationY)
                    .setDuration(250)
                    .setInterpolator(new android.view.animation.DecelerateInterpolator())
                    .start();
        } else {
            // 隐藏返回顶部按钮，发帖按钮回位
            binding.fabScrollToTop.animate()
                    .alpha(0f)
                    .scaleX(0.5f)
                    .scaleY(0.5f)
                    .setDuration(200)
                    .setInterpolator(new android.view.animation.AccelerateInterpolator())
                    .withEndAction(() -> binding.fabScrollToTop.setVisibility(View.GONE))
                    .start();
            
            // 发帖按钮下移动画回到原位
            binding.fabNewPost.animate()
                    .translationY(0)
                    .setDuration(200)
                    .setInterpolator(new android.view.animation.AccelerateInterpolator())
                    .start();
        }
    }

    /**
     * 显示预览头部信息（用于共享元素动画）
     * 从 Intent 获取基本信息立即显示，然后异步加载完整数据
     */
    private void showPreviewHeader() {
        // 创建一个预览的 ForumInfo
        ForumInfo previewInfo = new ForumInfo(fid, forumName);
        previewInfo.setIcon(forumIcon);

        // 设置预览信息到 adapter
        adapter.setForumInfo(previewInfo);

        // 显示内容区域
        binding.swipeRefresh.setVisibility(View.VISIBLE);
        binding.progressBar.setVisibility(View.GONE);
        binding.errorView.setVisibility(View.GONE);
    }

    /**
     * 设置下拉刷新
     */
    private void setupSwipeRefresh() {
        // 禁用刷新动画显示
        binding.swipeRefresh.setColorSchemeColors(0x00000000);
        binding.swipeRefresh.setOnRefreshListener(() -> {
            // 标记正在下拉刷新
            isRefreshing = true;
            // 立即隐藏刷新动画
            binding.swipeRefresh.setRefreshing(false);
            // 确保内容可见，隐藏所有加载动画
            binding.swipeRefresh.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.GONE);
            binding.errorView.setVisibility(View.GONE);
            // 重新加载数据
            refreshData();
        });
    }

    // OnPaginationListener 实现
    @Override
    public void onPrevPage() {
        if (currentPage > 1 && !isLoading) {
            goToPage(currentPage - 1);
        }
    }
    
    @Override
    public void onNextPage() {
        if (currentPage < totalPages && !isLoading) {
            goToPage(currentPage + 1);
        }
    }
    
    @Override
    public void onPageIndicatorClick(View pageIndicatorView) {
        showPageSelector(pageIndicatorView);
    }
    
    /**
     * 跳转到指定页
     */
    private void goToPage(int page) {
        if (isLoading || page < 1 || page > totalPages) return;
        
        currentPage = page;
        loadPageData();
    }
    
    /**
     * 初始加载数据
     */
    private void loadData() {
        if (isLoading) return;

        showLoading();
        isLoading = true;

        final int pageToLoad = currentPage;

        executor.execute(() -> {
            ApiResponse<ForumApi.ForumPostListResult> response =
                forumApi.getForumPostList(fid, pageToLoad, currentFilter);

            runOnUiThread(() -> {
                isLoading = false;
                hideLoading();

                if (response.isSuccess() && response.getData() != null) {
                    handleDataResult(response.getData(), true);
                    // 首次加载完成，标记为非首次加载
                    isFirstLoad = false;
                } else {
                    // 加载失败时，启动过渡动画（如果已推迟，仅首次加载）
                    if (isFirstLoad && !hasStartedEnterTransition) {
                        hasStartedEnterTransition = true;
                        supportStartPostponedEnterTransition();
                    }
                    isFirstLoad = false;
                    // 加载失败时
                    boolean wasRefreshing = isRefreshing;
                    isRefreshing = false;
                    if (wasRefreshing) {
                        // 下拉刷新失败，显示 Toast 提示，不显示错误视图
                        Toast.makeText(this, "刷新失败", Toast.LENGTH_SHORT).show();
                    } else {
                        showError(response.getMessage());
                    }
                }
            });
        });
    }
    
    /**
     * 加载指定页数据（翻页时调用，不刷新头部和Tab）
     */
    private void loadPageData() {
        if (isLoading) return;
        
        binding.swipeRefresh.setRefreshing(true);
        isLoading = true;
        
        executor.execute(() -> {
            ApiResponse<ForumApi.ForumPostListResult> response = 
                forumApi.getForumPostList(fid, currentPage, currentFilter);
            
            runOnUiThread(() -> {
                isLoading = false;
                binding.swipeRefresh.setRefreshing(false);
                
                if (response.isSuccess() && response.getData() != null) {
                    handlePageResult(response.getData());
                } else {
                    Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
    
    /**
     * Tab切换加载数据
     */
    private void loadTabData(boolean showTopPosts) {
        if (isLoading) return;
        
        binding.swipeRefresh.setRefreshing(true);
        isLoading = true;
        
        executor.execute(() -> {
            ApiResponse<ForumApi.ForumPostListResult> response = 
                forumApi.getForumPostList(fid, currentPage, currentFilter);
            
            runOnUiThread(() -> {
                isLoading = false;
                binding.swipeRefresh.setRefreshing(false);
                
                if (response.isSuccess() && response.getData() != null) {
                    ForumApi.ForumPostListResult result = response.getData();
                    
                    // 更新版块信息
                    if (result.getForumInfo() != null) {
                        forumInfo = result.getForumInfo();
                        totalPages = forumInfo.getTotalPages();
                    }
                    
                    // 更新置顶帖子
                    if (showTopPosts && result.getTopPosts() != null) {
                        cachedTopPosts = result.getTopPosts();
                        adapter.setTopPosts(cachedTopPosts);
                    } else if (!showTopPosts) {
                        adapter.setTopPosts(new ArrayList<>());
                    }
                    
                    // 更新帖子列表
                    List<Post> posts = result.getPosts();
                    adapter.setPosts(posts != null ? new ArrayList<>(posts) : new ArrayList<>());
                    adapter.setPagination(currentPage, totalPages);
                    
                    showContent();
                } else {
                    Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }
    
    /**
     * 处理初始数据结果
     */
    private void handleDataResult(ForumApi.ForumPostListResult result, boolean isInitialLoad) {
        forumInfo = result.getForumInfo();
        List<Post> posts = result.getPosts();

        // 更新版块信息
        if (forumInfo != null) {
            adapter.setForumInfo(forumInfo);
            totalPages = forumInfo.getTotalPages();

            if (forumInfo.getName() != null && getSupportActionBar() != null) {
                getSupportActionBar().setTitle(forumInfo.getName());
            }
        }

        // 更新置顶帖子（仅第一页显示）
        if (currentPage == 1 && result.getTopPosts() != null) {
            cachedTopPosts = result.getTopPosts();
            adapter.setTopPosts(cachedTopPosts);
        } else if (currentPage > 1) {
            adapter.setTopPosts(new ArrayList<>());
        }

        // 更新帖子列表
        adapter.setPosts(posts != null ? new ArrayList<>(posts) : new ArrayList<>());
        adapter.setPagination(currentPage, totalPages);

        showContent();
    }
    
    /**
     * 处理翻页结果（只更新帖子区域）
     */
    private void handlePageResult(ForumApi.ForumPostListResult result) {
        // 更新分页信息
        if (result.getForumInfo() != null) {
            totalPages = result.getForumInfo().getTotalPages();
        }
        
        // 第二页开始不显示置顶
        if (currentPage > 1) {
            adapter.setTopPosts(new ArrayList<>());
        }
        
        // 只更新帖子列表和分页
        List<Post> posts = result.getPosts();
        adapter.setPosts(posts != null ? new ArrayList<>(posts) : new ArrayList<>());
        adapter.setPagination(currentPage, totalPages);
    }
    
    /**
     * 刷新数据
     */
    private void refreshData() {
        currentPage = 1;
        cachedTopPosts.clear();
        // loadData() 会被 setupSwipeRefresh 中的 isRefreshing = true 后调用
        loadData();
    }
    
    /**
     * 显示页码选择器 - 定位在页码区域上方
     */
    private void showPageSelector(View anchorView) {
        if (pageSelectorPopup != null && pageSelectorPopup.isShowing()) {
            pageSelectorPopup.dismiss();
            return;
        }
        
        View contentView = LayoutInflater.from(this).inflate(R.layout.popup_page_selector, null);
        NumberPicker numberPicker = contentView.findViewById(R.id.pageNumberPicker);
        View cancelBtn = contentView.findViewById(R.id.pageSelectorCancel);
        View confirmBtn = contentView.findViewById(R.id.pageSelectorConfirm);
        
        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(totalPages);
        numberPicker.setValue(currentPage);
        numberPicker.setWrapSelectorWheel(false);
        numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        
        try {
            java.lang.reflect.Field selectionDivider = NumberPicker.class.getDeclaredField("mSelectionDivider");
            selectionDivider.setAccessible(true);
            selectionDivider.set(numberPicker, new ColorDrawable(Color.parseColor("#EEEEEE")));
        } catch (Exception e) {
            // 忽略
        }
        
        cancelBtn.setOnClickListener(v -> {
            if (pageSelectorPopup != null) {
                pageSelectorPopup.dismiss();
            }
        });
        
        confirmBtn.setOnClickListener(v -> {
            int selectedPage = numberPicker.getValue();
            if (pageSelectorPopup != null) {
                pageSelectorPopup.dismiss();
            }
            if (selectedPage != currentPage) {
                goToPage(selectedPage);
            }
        });
        
        pageSelectorPopup = new PopupWindow(contentView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);
        
        pageSelectorPopup.setOutsideTouchable(true);
        pageSelectorPopup.setElevation(8f);
        pageSelectorPopup.setAnimationStyle(android.R.style.Animation_Dialog);
        pageSelectorPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        pageSelectorPopup.setOnDismissListener(() -> pageSelectorPopup = null);
        
        // 定位在页码区域上方 - 参考PostDetailActivity
        int[] location = new int[2];
        anchorView.getLocationOnScreen(location);
        int anchorX = location[0];
        int anchorY = location[1];
        
        // 测量内容大小
        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupWidth = contentView.getMeasuredWidth();
        int popupHeight = contentView.getMeasuredHeight();
        
        // 计算位置：水平居中于页码指示器，在其上方
        int x = anchorX + (anchorView.getWidth() - popupWidth) / 2;
        int spacing = (int) (12 * getResources().getDisplayMetrics().density);
        int y = anchorY - popupHeight - spacing;
        
        pageSelectorPopup.showAtLocation(binding.getRoot(), android.view.Gravity.NO_GRAVITY, x, y);
    }
    
    @Override
    public void onItemClick(Post post, View avatarView) {
        // 跳转到帖子详情页面
        Intent intent = new Intent(this, PostDetailActivity.class);
        intent.putExtra(PostDetailActivity.EXTRA_TID, post.getTid());
        intent.putExtra(PostDetailActivity.EXTRA_TITLE, post.getTitle());
        intent.putExtra(PostDetailActivity.EXTRA_FORUM_NAME, forumName);
        intent.putExtra(PostDetailActivity.EXTRA_AUTHOR, post.getAuthor());
        intent.putExtra(PostDetailActivity.EXTRA_AUTHOR_AVATAR, post.getAuthorAvatar());
        intent.putExtra(PostDetailActivity.EXTRA_AUTHOR_LEVEL, post.getAuthorLevel());

        // 如果有头像View，使用共享元素动画
        if (avatarView != null) {
            ActivityOptionsCompat options =
                ActivityOptionsCompat.makeSceneTransitionAnimation(
                    this,
                    avatarView,
                    "avatar_transition"
                );
            startActivity(intent, options.toBundle());
        } else {
            // 没有头像View（如置顶帖子），普通跳转
            startActivity(intent);
        }
    }
    
    private void showLoading() {
        // 如果正在下拉刷新，保持内容可见，不显示加载指示器
        if (isRefreshing) {
            binding.swipeRefresh.setVisibility(View.VISIBLE);
            binding.progressBar.setVisibility(View.GONE);
            binding.errorView.setVisibility(View.GONE);
            return;
        }
        // 首次加载不显示加载指示器，因为已经有预览头部了
        if (isFirstLoad) {
            return;
        }
        // 非首次加载时显示加载指示器
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.errorView.setVisibility(View.GONE);
        binding.swipeRefresh.setVisibility(View.GONE);
    }
    
    private void hideLoading() {
        binding.progressBar.setVisibility(View.GONE);
        binding.swipeRefresh.setRefreshing(false);
    }
    
    private void showContent() {
        binding.errorView.setVisibility(View.GONE);
        binding.swipeRefresh.setVisibility(View.VISIBLE);
    }
    
    private void showError(String message) {
        binding.progressBar.setVisibility(View.GONE);
        binding.swipeRefresh.setVisibility(View.GONE);
        binding.errorView.setVisibility(View.VISIBLE);
        binding.errorText.setText(message != null ? message : "加载失败");
    }
    
    @Override
    public boolean onOptionsItemSelected(@NonNull MenuItem item) {
        if (item.getItemId() == android.R.id.home) {
            onBackPressed();
            return true;
        }
        return super.onOptionsItemSelected(item);
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
        if (executor != null) {
            executor.shutdown();
        }
        if (pageSelectorPopup != null) {
            pageSelectorPopup.dismiss();
        }
        binding = null;
    }
}
