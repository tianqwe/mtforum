package com.forum.mt.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;

import com.bumptech.glide.Glide;
import com.forum.mt.R;
import com.forum.mt.model.ApiResponse;
import com.forum.mt.api.ForumApi;
import com.forum.mt.api.HttpClient;
import com.forum.mt.databinding.ActivityUserDetailBinding;
import com.forum.mt.model.Post;
import com.forum.mt.model.User;
import com.forum.mt.ui.adapter.PostAdapter;
import com.forum.mt.ui.adapter.FavoriteAdapter;
import com.forum.mt.util.FontUtils;
import com.google.android.material.bottomsheet.BottomSheetDialog;

import java.util.ArrayList;
import java.util.List;

/**
 * 用户详情页面
 * 二次元社区风格个人主页
 * 支持滚动折叠效果
 */
public class UserDetailActivity extends AppCompatActivity implements com.google.android.material.appbar.AppBarLayout.OnOffsetChangedListener {

    public static final String EXTRA_UID = "uid";
    public static final String EXTRA_USERNAME = "username";
    public static final String EXTRA_AVATAR = "avatar";

    private ActivityUserDetailBinding binding;
    private ForumApi forumApi;
    private int uid;
    private User currentUser;
    private boolean isFollowing = false;

    // Tab相关
    private int currentTab = 0;
    private PostAdapter threadAdapter;
    private PostAdapter replyAdapter;
    private FavoriteAdapter favoriteAdapter;
    private List<Post> threadList = new ArrayList<>();
    private List<Post> replyList = new ArrayList<>();
    private List<Post> favoriteList = new ArrayList<>();
    private int threadPage = 1;
    private int replyPage = 1;
    private int favoritePage = 1;
    private boolean threadHasMore = true;
    private boolean replyHasMore = true;
    private boolean favoriteHasMore = true;
    private boolean isLoading = false;

    // 折叠状态相关
    private boolean isCollapsed = false;
    private int statusBarHeight = 0;

    public static void start(Context context, int uid) {
        Intent intent = new Intent(context, UserDetailActivity.class);
        intent.putExtra(EXTRA_UID, uid);
        context.startActivity(intent);
    }

    public static void start(Context context, int uid, String username, String avatar) {
        Intent intent = new Intent(context, UserDetailActivity.class);
        intent.putExtra(EXTRA_UID, uid);
        intent.putExtra(EXTRA_USERNAME, username);
        intent.putExtra(EXTRA_AVATAR, avatar);
        context.startActivity(intent);
    }

    /**
     * 带共享元素动画的启动方法
     * @param activity 当前Activity
     * @param uid 用户ID
     * @param username 用户名
     * @param avatar 头像URL
     * @param avatarView 头像视图（用于共享元素过渡动画）
     */
    public static void start(android.app.Activity activity, int uid, String username, String avatar, View avatarView) {
        Intent intent = new Intent(activity, UserDetailActivity.class);
        intent.putExtra(EXTRA_UID, uid);
        intent.putExtra(EXTRA_USERNAME, username);
        intent.putExtra(EXTRA_AVATAR, avatar);

        if (avatarView != null) {
            // 使用共享元素动画
            androidx.core.app.ActivityOptionsCompat options =
                androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(
                    activity,
                    avatarView,
                    "avatar_transition"
                );
            activity.startActivity(intent, options.toBundle());
        } else {
            activity.startActivity(intent);
        }
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 启用edge-to-edge显示
        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        // 延迟共享元素过渡动画，等待图片加载完成
        supportPostponeEnterTransition();

        binding = ActivityUserDetailBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 设置状态栏为透明，图标为白色
        getWindow().setStatusBarColor(Color.TRANSPARENT);
        getWindow().getDecorView().setSystemUiVisibility(
            View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
        );

        // 获取传入的uid
        uid = getIntent().getIntExtra(EXTRA_UID, 0);
        if (uid == 0) {
            Toast.makeText(this, "用户ID无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        HttpClient httpClient = HttpClient.getInstance(this);
        forumApi = new ForumApi(httpClient.getOkHttpClient(), httpClient.getCookieManager());

        // 预先设置传入的用户名和头像（预览）
        String username = getIntent().getStringExtra(EXTRA_USERNAME);
        String avatar = getIntent().getStringExtra(EXTRA_AVATAR);
        if (username != null && !username.isEmpty()) {
            binding.userName.setText(username);
            binding.toolbarTitle.setText(username);
        }
        if (avatar != null && !avatar.isEmpty()) {
            Glide.with(this)
                    .load(avatar)
                    .placeholder(R.drawable.ic_user)
                    .error(R.drawable.ic_user)
                    .into(binding.userAvatar);
            Glide.with(this)
                    .load(avatar)
                    .placeholder(R.drawable.ic_user)
                    .error(R.drawable.ic_user)
                    .into(binding.toolbarAvatar);
        }

        setupViews();
        setupRecyclerViews();
        setupTabListener();

        // 监听头像布局完成，启动过渡动画
        binding.userAvatar.getViewTreeObserver().addOnGlobalLayoutListener(
                new android.view.ViewTreeObserver.OnGlobalLayoutListener() {
                    @Override
                    public void onGlobalLayout() {
                        binding.userAvatar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                        supportStartPostponedEnterTransition();
                    }
                });

        // 加载用户详情
        loadUserDetail();
    }

    private void setupViews() {
        // 动态获取状态栏高度
        statusBarHeight = getStatusBarHeight();
        
        // 设置 Toolbar 的高度 = 状态栏高度 + actionBarSize，确保内容在状态栏下方
        if (statusBarHeight > 0) {
            int actionBarSize = (int) (56 * getResources().getDisplayMetrics().density);
            int toolbarHeight = statusBarHeight + actionBarSize;
            
            // 设置 Toolbar 高度
            binding.toolbar.getLayoutParams().height = toolbarHeight;
            
            // 设置 toolbarContent 的 marginTop，紧贴状态栏下边缘
            android.widget.FrameLayout.LayoutParams contentParams = 
                (android.widget.FrameLayout.LayoutParams) binding.toolbarContent.getLayoutParams();
            contentParams.topMargin = statusBarHeight;
            contentParams.gravity = android.view.Gravity.TOP;
            binding.toolbarContent.setLayoutParams(contentParams);
        }

        // 添加 AppBarLayout 折叠监听
        binding.appBarLayout.addOnOffsetChangedListener(this);
        
        // 初始状态设置
        binding.toolbarAvatar.setVisibility(View.INVISIBLE);
        binding.toolbarTitle.setVisibility(View.INVISIBLE);
        binding.avatarContainer.setVisibility(View.VISIBLE);
        binding.btnBack.setColorFilter(Color.WHITE);

        // 返回按钮
        binding.btnBack.setOnClickListener(v -> finish());

        // 关注按钮
        binding.btnFollow.setOnClickListener(v -> {
            if (currentUser != null) {
                toggleFollow();
            }
        });

        // 私信按钮
        binding.btnMessage.setOnClickListener(v -> {
            if (currentUser != null) {
                // 跳转到私信对话页面
                PmChatActivity.start(this, currentUser.getUid(), currentUser.getUsername(), currentUser.getAvatarUrl());
            }
        });

        // 帖子数点击，切换到帖子Tab
        binding.threadLayout.setOnClickListener(v -> {
            binding.tabLayout.selectTab(binding.tabLayout.getTabAt(1));
        });
    }

    @Override
    public void onOffsetChanged(com.google.android.material.appbar.AppBarLayout appBarLayout, int verticalOffset) {
        // 计算折叠比例 (0 = 完全展开, 1 = 完全折叠)
        int totalScrollRange = appBarLayout.getTotalScrollRange();
        if (totalScrollRange == 0) return;
        
        float collapseRatio = Math.abs(verticalOffset) / (float) totalScrollRange;
        
        // 根据折叠比例显示/隐藏 Toolbar 中的头像和用户名
        boolean shouldShowToolbarContent = collapseRatio > 0.5f;
        
        if (shouldShowToolbarContent != isCollapsed) {
            isCollapsed = shouldShowToolbarContent;
            
            // 显示/隐藏 Toolbar 中的头像和用户名
            binding.toolbarAvatar.setVisibility(shouldShowToolbarContent ? View.VISIBLE : View.INVISIBLE);
            binding.toolbarTitle.setVisibility(shouldShowToolbarContent ? View.VISIBLE : View.INVISIBLE);
            
            // 隐藏/显示大头像
            binding.avatarContainer.setVisibility(shouldShowToolbarContent ? View.INVISIBLE : View.VISIBLE);
            
            // 隐藏/显示关注和私信按钮（与大头像同步）
            binding.buttonsContainer.setVisibility(shouldShowToolbarContent ? View.INVISIBLE : View.VISIBLE);
            
            // 隐藏/显示用户信息卡片（用户名、等级、UID、统计数据）
            binding.userInfoCard.setVisibility(shouldShowToolbarContent ? View.INVISIBLE : View.VISIBLE);
            
            // 隐藏/显示背景图
            binding.coverImageContainer.setVisibility(shouldShowToolbarContent ? View.INVISIBLE : View.VISIBLE);
            
            // 更新返回按钮图标颜色和状态栏图标颜色
            if (shouldShowToolbarContent) {
                binding.btnBack.setColorFilter(ContextCompat.getColor(this, R.color.text_primary));
                binding.toolbarTitle.setTextColor(ContextCompat.getColor(this, R.color.text_primary));
                // 折叠状态：根据主题设置状态栏图标颜色
                int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
                if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                    // 深色模式：使用浅色状态栏图标
                    getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                    );
                } else {
                    // 浅色模式：使用深色状态栏图标
                    getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR
                    );
                }
            } else {
                binding.btnBack.setColorFilter(ContextCompat.getColor(this, R.color.white));
                binding.toolbarTitle.setTextColor(ContextCompat.getColor(this, R.color.white));
                // 展开状态：深色背景，使用浅色状态栏图标
                getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                );
            }
        }
    }

    private void setupRecyclerViews() {
        // 帖子点击监听 - 使用共享元素动画启动帖子详情页面
        PostAdapter.OnItemClickListener clickListener = (post, avatarView) -> {
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
                // 无头像View，使用普通方式启动
                PostDetailActivity.start(this, post.getTid(), post.getTitle());
            }
        };

        // 创建共享的 RecyclerView 缓存池，提升性能
        RecyclerView.RecycledViewPool sharedPool = new RecyclerView.RecycledViewPool();
        sharedPool.setMaxRecycledViews(0, 15); // 增加 ViewHolder 缓存数量

        // 帖子列表 - 启用嵌套滚动以支持 AppBarLayout 折叠
        threadAdapter = new PostAdapter(clickListener);
        binding.threadRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.threadRecyclerView.setAdapter(threadAdapter);
        binding.threadRecyclerView.setNestedScrollingEnabled(true);
        binding.threadRecyclerView.setHasFixedSize(true); // 布局大小固定，优化性能
        binding.threadRecyclerView.setItemViewCacheSize(10); // 增加缓存大小
        binding.threadRecyclerView.setRecycledViewPool(sharedPool);
        // 优化滚动流畅度
        binding.threadRecyclerView.setScrollingTouchSlop(RecyclerView.TOUCH_SLOP_PAGING);
        binding.threadRecyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER); // 禁用边缘回弹，减少计算
        binding.threadRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();
                    if (lastVisiblePosition >= threadList.size() - 3 && threadHasMore && !isLoading) {
                        loadUserThreads();
                    }
                }
            }
        });

        // 回复列表 - 启用嵌套滚动
        replyAdapter = new PostAdapter(clickListener);
        binding.replyRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.replyRecyclerView.setAdapter(replyAdapter);
        binding.replyRecyclerView.setNestedScrollingEnabled(true);
        binding.replyRecyclerView.setHasFixedSize(true);
        binding.replyRecyclerView.setItemViewCacheSize(10);
        binding.replyRecyclerView.setRecycledViewPool(sharedPool);
        binding.replyRecyclerView.setScrollingTouchSlop(RecyclerView.TOUCH_SLOP_PAGING);
        binding.replyRecyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        binding.replyRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();
                    if (lastVisiblePosition >= replyList.size() - 3 && replyHasMore && !isLoading) {
                        loadUserReplies();
                    }
                }
            }
        });

        // 收藏列表 - 使用FavoriteAdapter，启用嵌套滚动
        favoriteAdapter = new FavoriteAdapter();
        favoriteAdapter.setShowUnfavoriteButton(false); // 隐藏取消收藏按钮
        favoriteAdapter.setOnItemClickListener(new FavoriteAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(Post post, int position, View avatarView) {
                // 使用共享元素动画启动帖子详情页面
                if (avatarView != null) {
                    androidx.core.app.ActivityOptionsCompat options =
                        androidx.core.app.ActivityOptionsCompat.makeSceneTransitionAnimation(
                            UserDetailActivity.this,
                            avatarView,
                            "avatar_transition"
                        );
                    PostDetailActivity.start(UserDetailActivity.this,
                        post.getTid(),
                        post.getTitle(),
                        post.getForumName(),
                        post.getAuthor(),
                        post.getAuthorAvatar(),
                        post.getAuthorLevel(),
                        options.toBundle());
                } else {
                    PostDetailActivity.start(UserDetailActivity.this, post.getTid(), post.getTitle());
                }
            }

            @Override
            public void onUnfavoriteClick(Post post, int position) {
                // 用户详情页的收藏列表不显示取消收藏按钮，此方法不会被调用
            }
        });
        favoriteAdapter.setOnUserClickListener((uid, username, avatar, avatarView) -> {
            // 点击用户头像跳转到用户详情页，带共享元素动画
            UserDetailActivity.start(this, uid, username, avatar, avatarView);
        });
        binding.favoriteRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.favoriteRecyclerView.setAdapter(favoriteAdapter);
        binding.favoriteRecyclerView.setNestedScrollingEnabled(true);
        binding.favoriteRecyclerView.setHasFixedSize(true);
        binding.favoriteRecyclerView.setItemViewCacheSize(10);
        binding.favoriteRecyclerView.setRecycledViewPool(sharedPool);
        binding.favoriteRecyclerView.setScrollingTouchSlop(RecyclerView.TOUCH_SLOP_PAGING);
        binding.favoriteRecyclerView.setOverScrollMode(View.OVER_SCROLL_NEVER);
        binding.favoriteRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();
                    if (lastVisiblePosition >= favoriteList.size() - 3 && favoriteHasMore && !isLoading) {
                        loadUserFavorites();
                    }
                }
            }
        });
    }

    private void setupTabListener() {
        binding.tabLayout.addOnTabSelectedListener(new com.google.android.material.tabs.TabLayout.OnTabSelectedListener() {
            @Override
            public void onTabSelected(com.google.android.material.tabs.TabLayout.Tab tab) {
                int position = tab.getPosition();
                switchTab(position);
            }

            @Override
            public void onTabUnselected(com.google.android.material.tabs.TabLayout.Tab tab) {}

            @Override
            public void onTabReselected(com.google.android.material.tabs.TabLayout.Tab tab) {}
        });
    }

    private void switchTab(int position) {
        currentTab = position;

        // 隐藏所有内容
        binding.profileScroll.setVisibility(View.GONE);
        binding.threadRecyclerView.setVisibility(View.GONE);
        binding.replyRecyclerView.setVisibility(View.GONE);
        binding.favoriteRecyclerView.setVisibility(View.GONE);
        binding.emptyView.setVisibility(View.GONE);

        // 显示对应内容
        switch (position) {
            case 0: // 资料
                binding.profileScroll.setVisibility(View.VISIBLE);
                break;
            case 1: // 帖子
                binding.threadRecyclerView.setVisibility(View.VISIBLE);
                if (threadList.isEmpty() && threadHasMore) {
                    loadUserThreads();
                }
                break;
            case 2: // 回复
                binding.replyRecyclerView.setVisibility(View.VISIBLE);
                if (replyList.isEmpty() && replyHasMore) {
                    loadUserReplies();
                }
                break;
            case 3: // 收藏
                binding.favoriteRecyclerView.setVisibility(View.VISIBLE);
                if (favoriteList.isEmpty() && favoriteHasMore) {
                    loadUserFavorites();
                }
                break;
        }
    }

    private void loadUserDetail() {
        binding.progressBar.setVisibility(View.VISIBLE);

        new Thread(() -> {
            ApiResponse<User> response = forumApi.getUserProfile(uid);

            runOnUiThread(() -> {
                binding.progressBar.setVisibility(View.GONE);

                if (response.isSuccess() && response.getData() != null) {
                    currentUser = response.getData();
                    bindUserData(currentUser);
                } else {
                    Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void bindUserData(User user) {
        // 用户名 - 粉红色
        String username = user.getUsername();
        if (username != null && !username.isEmpty()) {
            binding.userName.setText(username);
        }
        // 设置 Toolbar 标题（折叠后显示）
        binding.toolbarTitle.setText(user.getUsername());

        // UID
        binding.userUid.setText("UID: " + user.getUid());
        binding.textUid.setText(String.valueOf(user.getUid()));

        // 头像 - 大头像
        String avatar = user.getAvatar();
        Glide.with(this)
                .load(user.getAvatarUrl())
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .into(binding.userAvatar);
        // 同时设置 Toolbar 小头像（折叠后显示）
        Glide.with(this)
                .load(user.getAvatarUrl())
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .into(binding.toolbarAvatar);

        // 等级
        String level = user.getLevel();
        if (level != null && !level.isEmpty()) {
            binding.userLevel.setText(level);
            binding.userLevel.setVisibility(View.VISIBLE);
            setLevelBackground(binding.userLevel, level);
        } else {
            binding.userLevel.setVisibility(View.GONE);
        }

        // 用户组
        String groupName = user.getGroupName();
        if (groupName != null && !groupName.isEmpty()) {
            binding.userGroup.setText(groupName);
            binding.textUserGroup.setText(groupName);
        } else {
            binding.userGroup.setText("会员");
            binding.textUserGroup.setText("会员");
        }

        // 核心数据统计
        binding.threadCount.setText(String.valueOf(user.getThreads()));
        binding.followingCount.setText(String.valueOf(user.getFollowing()));
        binding.followersCount.setText(String.valueOf(user.getFollowers()));
        binding.creditsCount.setText(String.valueOf(user.getCredits()));

        // 积分信息
        binding.goldCount.setText(String.valueOf(user.getGoldCoin()));
        binding.popularityCount.setText(String.valueOf(user.getPopularity()));
        binding.reputationCount.setText(String.valueOf(user.getReputation()));
        binding.goodRateCount.setText(String.valueOf(user.getGoodRate()));

        // 详细资料
        binding.regDate.setText(user.getRegDate() != null ? user.getRegDate() : "未知");
        binding.lastVisit.setText(user.getLastVisit() != null ? user.getLastVisit() : "未知");
        binding.onlineTime.setText(user.getOnlineTime() != null ? user.getOnlineTime() : "未知");
        binding.friendsCount.setText(String.valueOf(user.getFriends()));
        binding.replyCount.setText(String.valueOf(user.getReplies()));

        // 个人签名
        String signature = user.getSignature();
        if (signature != null && !signature.isEmpty()) {
            binding.signature.setText("\"" + signature + "\"");
            binding.signature.setVisibility(View.VISIBLE);
        } else {
            binding.signature.setVisibility(View.GONE);
        }
        
        // 背景封面图片
        String coverImage = user.getCoverImage();
        if (coverImage != null && !coverImage.isEmpty()) {
            Glide.with(this)
                    .load(coverImage)
                    .placeholder(R.drawable.bg_profile_cover)
                    .error(R.drawable.bg_profile_cover)
                    .into(binding.coverImage);
        }
        
        // 更新关注状态
        isFollowing = user.isFollowed();
        updateFollowButton();

        // 检查是否是当前用户自己
        HttpClient httpClient = HttpClient.getInstance(this);
        if (httpClient.isLoggedIn()) {
            checkIfSelfUser(user);
        } else {
            // 未登录，显示关注按钮，隐藏收藏Tab
            binding.btnFollow.setVisibility(View.VISIBLE);
            binding.btnMessage.setVisibility(View.VISIBLE);
            hideFavoriteTab();
        }

        // 应用字体大小
        applyFontSize();
    }

    /**
     * 检查是否是当前用户自己
     */
    private void checkIfSelfUser(User user) {
        new Thread(() -> {
            ApiResponse<User> response = forumApi.getUserProfile();
            runOnUiThread(() -> {
                if (response.isSuccess() && response.getData() != null) {
                    int currentUid = response.getData().getUid();
                    if (currentUid == user.getUid()) {
                        // 是自己，隐藏关注和私信按钮
                        binding.btnFollow.setVisibility(View.GONE);
                        binding.btnMessage.setVisibility(View.GONE);
                        // 显示收藏Tab（只有自己才能看自己的收藏）
                        showFavoriteTab();
                    } else {
                        // 不是自己，显示关注按钮
                        binding.btnFollow.setVisibility(View.VISIBLE);
                        binding.btnMessage.setVisibility(View.VISIBLE);
                        // 隐藏收藏Tab（不能看别人的收藏）
                        hideFavoriteTab();
                    }
                } else {
                    // 获取失败，默认显示关注按钮
                    binding.btnFollow.setVisibility(View.VISIBLE);
                    binding.btnMessage.setVisibility(View.VISIBLE);
                    // 隐藏收藏Tab
                    hideFavoriteTab();
                }
            });
        }).start();
    }

    /**
     * 设置等级标签背景颜色
     */
    private void setLevelBackground(android.widget.TextView view, String level) {
        int color;
        try {
            int levelNum = Integer.parseInt(level.replace("Lv.", "").trim());
            if (levelNum <= 2) {
                color = Color.parseColor("#9E9E9E");
            } else if (levelNum <= 4) {
                color = Color.parseColor("#4CAF50");
            } else if (levelNum <= 6) {
                color = Color.parseColor("#2196F3");
            } else if (levelNum <= 8) {
                color = Color.parseColor("#9C27B0");
            } else {
                color = Color.parseColor("#FF9800");
            }
        } catch (Exception e) {
            color = Color.parseColor("#9E9E9E");
        }

        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(10f);
        drawable.setColor(color);
        view.setBackground(drawable);
    }

    /**
     * 加载用户帖子列表
     */
    private void loadUserThreads() {
        if (isLoading) return;
        isLoading = true;

        showTabLoading(true);

        new Thread(() -> {
            ApiResponse<List<Post>> response = forumApi.getUserThreads(uid, threadPage);

            runOnUiThread(() -> {
                showTabLoading(false);
                isLoading = false;

                if (response.isSuccess() && response.getData() != null) {
                    List<Post> posts = response.getData();
                    if (threadPage == 1) {
                        threadList.clear();
                    }
                    threadList.addAll(posts);
                    threadAdapter.submitList(new ArrayList<>(threadList));

                    // 判断是否还有更多
                    threadHasMore = posts.size() >= 20;
                    if (threadHasMore) {
                        threadPage++;
                    }

                    // 显示空状态
                    if (threadList.isEmpty()) {
                        showEmptyState("暂无帖子");
                    }
                } else {
                    Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    /**
     * 加载用户回复列表
     */
    private void loadUserReplies() {
        if (isLoading) return;
        isLoading = true;

        showTabLoading(true);

        new Thread(() -> {
            ApiResponse<List<Post>> response = forumApi.getUserReplies(uid, replyPage);

            runOnUiThread(() -> {
                showTabLoading(false);
                isLoading = false;

                if (response.isSuccess() && response.getData() != null) {
                    List<Post> posts = response.getData();
                    if (replyPage == 1) {
                        replyList.clear();
                    }
                    replyList.addAll(posts);
                    replyAdapter.submitList(new ArrayList<>(replyList));

                    // 判断是否还有更多
                    replyHasMore = posts.size() >= 20;
                    if (replyHasMore) {
                        replyPage++;
                    }

                    // 显示空状态
                    if (replyList.isEmpty()) {
                        showEmptyState("暂无回复");
                    }
                } else {
                    Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    /**
     * 加载用户收藏列表
     */
    private void loadUserFavorites() {
        if (isLoading) return;
        isLoading = true;

        showTabLoading(true);

        new Thread(() -> {
            ApiResponse<List<Post>> response = forumApi.getUserFavorites(uid, favoritePage);

            runOnUiThread(() -> {
                showTabLoading(false);
                isLoading = false;

                if (response.isSuccess() && response.getData() != null) {
                    List<Post> posts = response.getData();
                    if (favoritePage == 1) {
                        favoriteList.clear();
                    }
                    favoriteList.addAll(posts);
                    favoriteAdapter.submitList(new ArrayList<>(favoriteList));

                    // 判断是否还有更多
                    favoriteHasMore = posts.size() >= 20;
                    if (favoriteHasMore) {
                        favoritePage++;
                    }

                    // 显示空状态
                    if (favoriteList.isEmpty()) {
                        showEmptyState("暂无收藏");
                    }
                } else {
                    Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void showTabLoading(boolean show) {
        binding.tabProgressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }

    private void showEmptyState(String text) {
        binding.emptyView.setVisibility(View.VISIBLE);
        binding.emptyText.setText(text);
    }

    /**
     * 关注/取消关注
     */
    private void toggleFollow() {
        new Thread(() -> {
            ApiResponse<Boolean> response;
            if (isFollowing) {
                response = forumApi.unfollowUser(currentUser.getUid());
            } else {
                response = forumApi.followUser(currentUser.getUid());
            }

            runOnUiThread(() -> {
                if (response.isSuccess()) {
                    isFollowing = !isFollowing;
                    updateFollowButton();
                    Toast.makeText(this, isFollowing ? "关注成功" : "已取消关注", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    /**
     * 更新关注按钮状态
     */
    private void updateFollowButton() {
        if (isFollowing) {
            binding.btnFollow.setText("已关注");
            binding.btnFollow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.border_light)));
            binding.btnFollow.setTextColor(ContextCompat.getColor(this, R.color.text_secondary));
        } else {
            binding.btnFollow.setText("关注");
            binding.btnFollow.setBackgroundTintList(android.content.res.ColorStateList.valueOf(ContextCompat.getColor(this, R.color.highlight_orange)));
            binding.btnFollow.setTextColor(ContextCompat.getColor(this, R.color.white));
        }
    }
    
    /**
     * 显示发送私信对话框
     */
    private void showSendPmDialog() {
        // 创建底部对话框
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheetDialog = 
            new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        
        android.view.View view = getLayoutInflater().inflate(R.layout.bottom_sheet_send_pm, null);
        bottomSheetDialog.setContentView(view);
        
        // 获取视图组件
        android.widget.ImageView avatarImage = view.findViewById(R.id.avatarImage);
        android.widget.TextView usernameText = view.findViewById(R.id.usernameText);
        android.widget.EditText messageInput = view.findViewById(R.id.messageInput);
        android.widget.TextView sendButton = view.findViewById(R.id.sendButton);
        android.widget.TextView charCount = view.findViewById(R.id.charCount);
        android.widget.ImageView emojiButton = view.findViewById(R.id.emojiButton);
        android.widget.FrameLayout smileyPanelContainer = view.findViewById(R.id.smileyPanelContainer);
        
        // 设置收信人信息
        usernameText.setText(currentUser.getUsername());
        Glide.with(this)
            .load(currentUser.getAvatarUrl())
            .placeholder(R.drawable.ic_user)
            .error(R.drawable.ic_user)
            .into(avatarImage);
        
        // 字数统计
        messageInput.addTextChangedListener(new android.text.TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s.length();
                charCount.setText(length + "/500");
                if (length > 500) {
                    charCount.setTextColor(ContextCompat.getColor(UserDetailActivity.this, R.color.error));
                } else {
                    charCount.setTextColor(ContextCompat.getColor(UserDetailActivity.this, R.color.text_disabled));
                }
            }
            
            @Override
            public void afterTextChanged(android.text.Editable s) {}
        });
        
        // 表情按钮点击
        emojiButton.setOnClickListener(v -> {
            if (smileyPanelContainer.getVisibility() == View.VISIBLE) {
                smileyPanelContainer.setVisibility(View.GONE);
            } else {
                smileyPanelContainer.setVisibility(View.VISIBLE);
                setupSmileyPanel(smileyPanelContainer, messageInput);
            }
        });
        
        // 发送按钮点击
        sendButton.setOnClickListener(v -> {
            String message = messageInput.getText().toString().trim();
            if (message.isEmpty()) {
                Toast.makeText(this, "请输入私信内容", Toast.LENGTH_SHORT).show();
                return;
            }
            if (message.length() > 500) {
                Toast.makeText(this, "私信内容不能超过500字", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 发送私信
            sendPrivateMessage(currentUser.getUid(), message, bottomSheetDialog);
        });
        
        bottomSheetDialog.show();
    }
    
    /**
     * 设置表情面板
     */
    private void setupSmileyPanel(android.widget.FrameLayout container, android.widget.EditText messageInput) {
        androidx.recyclerview.widget.RecyclerView recyclerView = container.findViewById(R.id.smileyGrid);
        if (recyclerView == null) return;
        
        // 表情代码列表
        String[] smileys = {
            "[微笑]", "[撇嘴]", "[色]", "[发呆]", "[得意]", "[流泪]", "[害羞]", "[闭嘴]",
            "[睡]", "[大哭]", "[尴尬]", "[发怒]", "[调皮]", "[呲牙]", "[惊讶]", "[难过]",
            "[酷]", "[冷汗]", "[抓狂]", "[吐]", "[偷笑]", "[可爱]", "[白眼]", "[傲慢]",
            "[饥饿]", "[困]", "[惊恐]", "[流汗]", "[憨笑]", "[大兵]", "[奋斗]", "[咒骂]",
            "[疑问]", "[嘘]", "[晕]", "[折磨]", "[衰]", "[骷髅]", "[敲打]", "[再见]",
            "[擦汗]", "[抠鼻]", "[鼓掌]", "[糗大了]", "[坏笑]", "[左哼哼]", "[右哼哼]", "[哈欠]",
            "[鄙视]", "[委屈]", "[快哭了]", "[阴险]", "[亲亲]", "[吓]", "[可怜]", "[菜刀]",
            "[西瓜]", "[啤酒]", "[篮球]", "[乒乓]", "[咖啡]", "[饭]", "[猪头]", "[玫瑰]",
            "[凋谢]", "[示爱]", "[爱心]", "[心碎]", "[蛋糕]", "[闪电]", "[炸弹]", "[刀]",
            "[足球]", "[瓢虫]", "[便便]", "[月亮]", "[太阳]", "[礼物]", "[拥抱]", "[强]",
            "[弱]", "[握手]", "[胜利]", "[抱拳]", "[勾引]", "[拳头]", "[差劲]", "[爱你]",
            "[NO]", "[OK]", "[爱情]", "[飞吻]", "[跳跳]", "[发抖]", "[怄火]", "[转圈]"
        };
        
        recyclerView.setLayoutManager(new GridLayoutManager(this, 8));
        com.forum.mt.ui.adapter.SimpleSmileyAdapter adapter = 
            new com.forum.mt.ui.adapter.SimpleSmileyAdapter(smileys, smileyCode -> {
                int start = messageInput.getSelectionStart();
                int end = messageInput.getSelectionEnd();
                messageInput.getText().replace(Math.min(start, end), Math.max(start, end), smileyCode);
            });
        recyclerView.setAdapter(adapter);
    }
    
    /**
     * 发送私信
     */
    private void sendPrivateMessage(int toUid, String message, com.google.android.material.bottomsheet.BottomSheetDialog dialog) {
        android.widget.Toast loadingToast = android.widget.Toast.makeText(this, "发送中...", android.widget.Toast.LENGTH_SHORT);
        loadingToast.show();
        
        new Thread(() -> {
            ApiResponse<Boolean> response = forumApi.sendPm(toUid, message);
            
            runOnUiThread(() -> {
                loadingToast.cancel();
                
                if (response.isSuccess() && response.getData() != null && response.getData()) {
                    Toast.makeText(this, "发送成功", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    /**
     * 应用字体大小设置
     */
    private void applyFontSize() {
        // 标题
        FontUtils.applyFontSize(this, binding.userName, FontUtils.SIZE_TITLE);
        // 正文
        FontUtils.applyFontSize(this, binding.userGroup, FontUtils.SIZE_SUBTITLE);
        FontUtils.applyFontSize(this, binding.userUid, FontUtils.SIZE_SMALL);
        FontUtils.applyFontSize(this, binding.signature, FontUtils.SIZE_CONTENT);
        // 统计数字
        FontUtils.applyFontSize(this, binding.threadCount, FontUtils.SIZE_LARGE);
        FontUtils.applyFontSize(this, binding.followingCount, FontUtils.SIZE_LARGE);
        FontUtils.applyFontSize(this, binding.followersCount, FontUtils.SIZE_LARGE);
        FontUtils.applyFontSize(this, binding.creditsCount, FontUtils.SIZE_LARGE);
    }

    /**
     * 隐藏收藏Tab
     */
    private void hideFavoriteTab() {
        // 默认不显示收藏Tab（XML中已移除），此方法保留但不做任何操作
    }

    /**
     * 显示收藏Tab（动态添加）
     */
    private void showFavoriteTab() {
        try {
            // 检查是否已经有收藏Tab
            boolean hasFavoriteTab = false;
            for (int i = 0; i < binding.tabLayout.getTabCount(); i++) {
                com.google.android.material.tabs.TabLayout.Tab tab = binding.tabLayout.getTabAt(i);
                if (tab != null && "收藏".equals(tab.getText())) {
                    hasFavoriteTab = true;
                    break;
                }
            }
            // 如果没有，添加收藏Tab
            if (!hasFavoriteTab) {
                binding.tabLayout.addTab(binding.tabLayout.newTab().setText("收藏"));
            }
        } catch (Exception e) {
            // 忽略异常
        }
    }

    /**
     * 获取状态栏高度
     */
    private int getStatusBarHeight() {
        int result = 0;
        int resourceId = getResources().getIdentifier("status_bar_height", "dimen", "android");
        if (resourceId > 0) {
            result = getResources().getDimensionPixelSize(resourceId);
        }
        return result;
    }
}
