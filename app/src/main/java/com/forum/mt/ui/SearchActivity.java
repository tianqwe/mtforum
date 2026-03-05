package com.forum.mt.ui;

import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.widget.NumberPicker;
import android.widget.PopupWindow;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.forum.mt.R;
import com.forum.mt.api.ForumApi;
import com.forum.mt.api.HttpClient;
import com.forum.mt.databinding.ActivitySearchBinding;
import com.forum.mt.model.ApiResponse;
import com.forum.mt.model.Post;
import com.forum.mt.model.SearchPostResult;
import com.forum.mt.model.User;
import com.forum.mt.ui.adapter.SearchPostAdapter;
import com.forum.mt.ui.adapter.SearchUserAdapter;
import com.forum.mt.ui.UserDetailActivity;
import com.google.android.material.tabs.TabLayout;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 搜索页面 - 支持搜索帖子和用户，带分页功能
 */
public class SearchActivity extends AppCompatActivity implements
        SearchPostAdapter.OnPostClickListener,
        SearchUserAdapter.OnUserClickListener {

    public static final String EXTRA_KEYWORD = "keyword";

    private ActivitySearchBinding binding;
    private SearchPostAdapter postAdapter;
    private SearchUserAdapter userAdapter;
    private ForumApi forumApi;
    private ExecutorService executor;

    private int currentTab = 0; // 0=帖子, 1=用户
    private String currentKeyword = "";
    private int postPage = 1;
    private int userPage = 1;
    private int postTotalPages = 1;
    private int userTotalPages = 1;
    private boolean isLoading = false;

    private PopupWindow pageSelectorPopup;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 启用edge-to-edge显示
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivitySearchBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 根据当前主题设置状态栏图标颜色
        setupStatusBar();

        forumApi = new ForumApi(
            HttpClient.getInstance(this).getOkHttpClient(),
            HttpClient.getInstance(this).getCookieManager()
        );
        executor = Executors.newSingleThreadExecutor();

        setupViews();
        setupListeners();

        // 如果有传入关键词，直接搜索
        String keyword = getIntent().getStringExtra(EXTRA_KEYWORD);
        if (keyword != null && !keyword.isEmpty()) {
            binding.editSearch.setText(keyword);
            performSearch(keyword);
        }
    }

    private void setupViews() {
        // 帖子列表
        postAdapter = new SearchPostAdapter();
        postAdapter.setOnPostClickListener(this);
        postAdapter.setOnUserClickListener((uid, username, avatar, avatarView) -> {
            UserDetailActivity.start(this, uid, username, avatar, avatarView);
        });
        binding.recyclerPosts.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerPosts.setAdapter(postAdapter);
        binding.recyclerPosts.setNestedScrollingEnabled(false);

        // 用户列表
        userAdapter = new SearchUserAdapter();
        userAdapter.setOnUserClickListener(this);
        binding.recyclerUsers.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerUsers.setAdapter(userAdapter);
        binding.recyclerUsers.setNestedScrollingEnabled(false);

        // 默认显示帖子Tab
        updateTabVisibility(0);
    }

    private void setupListeners() {
        // 返回按钮
        binding.btnBack.setOnClickListener(v -> finish());

        // 搜索按钮
        binding.btnSearch.setOnClickListener(v -> {
            String keyword = binding.editSearch.getText().toString().trim();
            if (!keyword.isEmpty()) {
                performSearch(keyword);
            }
        });

        // 输入框回车搜索
        binding.editSearch.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_SEARCH ||
                (event != null && event.getKeyCode() == KeyEvent.KEYCODE_ENTER)) {
                String keyword = binding.editSearch.getText().toString().trim();
                if (!keyword.isEmpty()) {
                    performSearch(keyword);
                }
                return true;
            }
            return false;
        });

        // Tab切换
        binding.tabLayout.addOnTabSelectedListener(new TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(TabLayout.Tab tab) {
                currentTab = tab.getPosition();
                updateTabVisibility(currentTab);

                // 如果切换到一个还没有搜索结果的tab，执行搜索
                if (currentTab == 0 && postAdapter.getItemCount() == 0 && !currentKeyword.isEmpty()) {
                    searchPosts(currentKeyword, 1);
                } else if (currentTab == 1 && userAdapter.getItemCount() == 0 && !currentKeyword.isEmpty()) {
                    searchUsers(currentKeyword, 1);
                }
            }

            @Override
            public void onTabUnselected(TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(TabLayout.Tab tab) {}
        });

        // 帖子分页控件
        setupPostPaginationListeners();
    }

    private void setupPostPaginationListeners() {
        binding.prevPageBtn.setOnClickListener(v -> {
            if (postPage > 1) {
                searchPosts(currentKeyword, postPage - 1);
            }
        });

        binding.nextPageBtn.setOnClickListener(v -> {
            if (postPage < postTotalPages) {
                searchPosts(currentKeyword, postPage + 1);
            }
        });

        binding.pageIndicator.setOnClickListener(v -> showPostPageSelector());
    }

    private void updateTabVisibility(int tab) {
        if (tab == 0) {
            binding.scrollViewPosts.setVisibility(View.VISIBLE);
            binding.scrollViewUsers.setVisibility(View.GONE);
        } else {
            binding.scrollViewPosts.setVisibility(View.GONE);
            binding.scrollViewUsers.setVisibility(View.VISIBLE);
        }
    }

    private void performSearch(String keyword) {
        currentKeyword = keyword;
        postPage = 1;
        userPage = 1;
        postTotalPages = 1;
        userTotalPages = 1;

        // 根据当前Tab执行搜索
        if (currentTab == 0) {
            postAdapter.setPosts(new ArrayList<>());
            searchPosts(keyword, 1);
        } else {
            userAdapter.setUsers(new ArrayList<>());
            searchUsers(keyword, 1);
        }
    }

    private void searchPosts(String keyword, int page) {
        if (isLoading) return;

        isLoading = true;
        showLoading(true);

        executor.execute(() -> {
            ApiResponse<SearchPostResult> response = forumApi.searchPosts(keyword, page);

            runOnUiThread(() -> {
                isLoading = false;
                showLoading(false);

                if (response.isSuccess() && response.hasData()) {
                    SearchPostResult result = response.getData();
                    postPage = result.getCurrentPage();
                    postTotalPages = result.getTotalPages();
                    postAdapter.setPosts(result.getPosts());

                    updatePostPagination();
                    updateEmptyView();
                } else {
                    if (page == 1) {
                        Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    updateEmptyView();
                }
            });
        });
    }

    private void searchUsers(String keyword, int page) {
        if (isLoading) return;

        isLoading = true;
        showLoading(true);

        executor.execute(() -> {
            ApiResponse<List<User>> response = forumApi.searchUsers(keyword, page);

            runOnUiThread(() -> {
                isLoading = false;
                showLoading(false);

                if (response.isSuccess() && response.hasData()) {
                    List<User> users = response.getData();
                    userPage = page;
                    userAdapter.setUsers(users);

                    // 用户搜索暂不支持分页，默认总页数为1
                    userTotalPages = 1;
                    updateUserPagination();
                    updateEmptyView();
                } else {
                    if (page == 1) {
                        Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                    updateEmptyView();
                }
            });
        });
    }

    private void updatePostPagination() {
        if (postTotalPages <= 1) {
            binding.postPagination.setVisibility(View.GONE);
            return;
        }

        binding.postPagination.setVisibility(View.VISIBLE);
        binding.currentPageText.setText(String.valueOf(postPage));
        binding.totalPageText.setText(String.valueOf(postTotalPages));

        // 上一页按钮状态
        boolean canGoPrev = postPage > 1;
        binding.prevPageBtn.setEnabled(canGoPrev);
        binding.prevPageBtn.setAlpha(canGoPrev ? 1.0f : 0.4f);
        binding.prevPageBtn.setClickable(canGoPrev);

        // 下一页按钮状态
        boolean canGoNext = postPage < postTotalPages;
        binding.nextPageBtn.setEnabled(canGoNext);
        binding.nextPageBtn.setAlpha(canGoNext ? 1.0f : 0.4f);
        binding.nextPageBtn.setClickable(canGoNext);
    }

    private void updateUserPagination() {
        // 用户搜索暂不支持分页，隐藏分页控件
        binding.userPagination.setVisibility(View.GONE);
    }

    private void showPostPageSelector() {
        if (pageSelectorPopup != null && pageSelectorPopup.isShowing()) {
            pageSelectorPopup.dismiss();
            return;
        }

        View contentView = LayoutInflater.from(this).inflate(R.layout.popup_page_selector, null);
        NumberPicker numberPicker = contentView.findViewById(R.id.pageNumberPicker);
        View cancelBtn = contentView.findViewById(R.id.pageSelectorCancel);
        View confirmBtn = contentView.findViewById(R.id.pageSelectorConfirm);

        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(postTotalPages);
        numberPicker.setValue(postPage);
        numberPicker.setWrapSelectorWheel(false);

        // 设置NumberPicker样式
        numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        try {
            Field selectionDivider = NumberPicker.class.getDeclaredField("mSelectionDivider");
            selectionDivider.setAccessible(true);
            selectionDivider.set(numberPicker, new ColorDrawable(Color.parseColor("#EEEEEE")));
        } catch (Exception e) {
            // 忽略样式设置失败
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
            searchPosts(currentKeyword, selectedPage);
        });

        // 创建PopupWindow
        pageSelectorPopup = new PopupWindow(contentView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true);

        pageSelectorPopup.setOutsideTouchable(true);
        pageSelectorPopup.setElevation(8f);
        pageSelectorPopup.setAnimationStyle(android.R.style.Animation_Dialog);
        pageSelectorPopup.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));

        // 定位在页码区域上方
        int[] location = new int[2];
        binding.pageIndicator.getLocationOnScreen(location);
        int anchorX = location[0];
        int anchorY = location[1];

        // 测量内容大小
        contentView.measure(View.MeasureSpec.UNSPECIFIED, View.MeasureSpec.UNSPECIFIED);
        int popupWidth = contentView.getMeasuredWidth();
        int popupHeight = contentView.getMeasuredHeight();

        // 计算位置：水平居中于页码指示器，在其上方
        int x = anchorX + (binding.pageIndicator.getWidth() - popupWidth) / 2;
        int spacing = (int) (12 * getResources().getDisplayMetrics().density);
        int y = anchorY - popupHeight - spacing;

        pageSelectorPopup.showAtLocation(binding.getRoot(), Gravity.NO_GRAVITY, x, y);
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void updateEmptyView() {
        boolean hasResults = (currentTab == 0 && postAdapter.getItemCount() > 0) ||
                            (currentTab == 1 && userAdapter.getItemCount() > 0);
        binding.emptyView.setVisibility(hasResults ? View.GONE : View.VISIBLE);
    }

    // SearchPostAdapter.OnPostClickListener
    @Override
    public void onPostClick(Post post, View avatarView) {
        if (avatarView != null) {
            // 使用共享元素动画启动Activity，头像平移到帖子详情页
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
            // 无头像View，使用普通方式启动，但仍传递基本信息
            PostDetailActivity.startWithBasicInfo(this,
                post.getTid(),
                post.getTitle(),
                post.getForumName(),
                post.getAuthor(),
                post.getAuthorAvatar(),
                post.getAuthorLevel()
            );
        }
    }

    // SearchUserAdapter.OnUserClickListener
    @Override
    public void onUserClick(User user, View avatarView) {
        // 跳转到用户详情页，带共享元素动画
        UserDetailActivity.start(this, user.getUid(), user.getUsername(), user.getAvatarUrl(), avatarView);
    }

    @Override
    public void onFollowClick(User user, int position) {
        // 关注/取消关注用户
        if (!forumApi.isLoggedIn()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }

        // 先获取当前关注状态
        boolean isFollowed = user.isFollowed();

        executor.execute(() -> {
            ApiResponse<Boolean> response;
            if (isFollowed) {
                // 已关注，执行取消关注
                response = forumApi.unfollowUser(user.getUid());
            } else {
                // 未关注，执行关注
                response = forumApi.followUser(user.getUid());
            }

            runOnUiThread(() -> {
                if (response.isSuccess()) {
                    // 更新用户关注状态
                    user.setFollowed(!isFollowed);
                    // 通知适配器更新指定位置
                    userAdapter.notifyItemChanged(position);
                    // 显示提示
                    Toast.makeText(this, isFollowed ? "已取消关注" : "关注成功", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
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
        if (pageSelectorPopup != null && pageSelectorPopup.isShowing()) {
            pageSelectorPopup.dismiss();
        }
    }
}
