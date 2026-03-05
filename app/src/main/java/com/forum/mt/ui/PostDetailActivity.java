package com.forum.mt.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewTreeObserver;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.NumberPicker;
import android.widget.PopupMenu;
import android.widget.PopupWindow;
import android.widget.RadioGroup;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;

import com.google.android.material.bottomsheet.BottomSheetDialog;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.forum.mt.R;
import com.forum.mt.api.ForumApi;
import com.forum.mt.api.HttpClient;
import com.forum.mt.model.ApiResponse;
import com.forum.mt.databinding.ActivityPostDetailBinding;
import com.forum.mt.model.Comment;
import com.forum.mt.model.ContentBlock;
import com.forum.mt.model.Post;
import com.forum.mt.model.User;
import com.forum.mt.model.UploadResult;
import com.forum.mt.ui.adapter.CommentAdapter;
import com.forum.mt.ui.adapter.ContentAdapter;
import com.forum.mt.ui.UserDetailActivity;
import com.forum.mt.util.ContentParser;
import com.forum.mt.util.ShareHelper;
import com.forum.mt.viewmodel.BrowseHistoryViewModel;
import com.forum.mt.viewmodel.PostDetailUiState;
import com.forum.mt.viewmodel.PostDetailViewModel;

import java.util.ArrayList;
import java.util.List;

/**
 * 帖子详情页面
 * 使用 ViewModel + LiveData 实现，遵循谷歌推荐的现代Android架构
 * 使用原生Android组件显示内容（非WebView）
 */
public class PostDetailActivity extends AppCompatActivity {

    public static final String EXTRA_TID = "tid";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_AUTHOR = "author";
    public static final String EXTRA_AUTHOR_AVATAR = "author_avatar";
    public static final String EXTRA_AUTHOR_LEVEL = "author_level";
    public static final String EXTRA_FORUM_NAME = "forum_name";
    private static final String EXTRA_HIGHLIGHT_AUTHOR_ID = "highlight_author_id"; // 高亮评论的用户ID

    // 评论排序类型 (对应服务端 ordertype 参数)
    // ordertype=1 是最新（倒序），ordertype=2 是最早（正序）
    private static final int ORDER_LATEST = 1;   // 最新
    private static final int ORDER_EARLIEST = 2; // 最早
    private static final int ORDER_AUTHOR = 3;   // 楼主（只看楼主）
    
    // 图片选择器请求码
    private static final int REQUEST_CODE_PICK_IMAGE = 1001;

    private ActivityPostDetailBinding binding;
    private PostDetailViewModel viewModel;
    private BrowseHistoryViewModel browseHistoryViewModel;
    private CommentAdapter commentAdapter;
    private ContentAdapter contentAdapter;
    private com.forum.mt.ui.adapter.ImagePreviewAdapter imagePreviewAdapter; // 图片预览适配器
    private View currentImagePreviewContainer; // 当前图片预览容器引用
    private EditText currentReplyInput; // 当前回复输入框引用
    private int currentTid = 0;
    private int highlightAuthorId = 0; // 需要高亮的评论作者ID
    private Post currentPost; // 当前帖子数据（用于回复等操作）
    private int currentUserId = 0; // 当前登录用户ID
    
    // 标志位：跟踪是否是用户主动操作
    private boolean isUserLikeAction = false;
    private boolean isUserFavoriteAction = false;
    // 标志位：是否已显示基本信息（用于共享元素动画）
    private boolean hasShownBasicInfo = false;

    public static void start(Context context, int tid, String title) {
        Intent intent = new Intent(context, PostDetailActivity.class);
        intent.putExtra(EXTRA_TID, tid);
        intent.putExtra(EXTRA_TITLE, title);
        context.startActivity(intent);
    }

    public static void startWithBasicInfo(Context context, int tid, String title, String forumName,
                                          String author, String authorAvatar, String authorLevel) {
        Intent intent = new Intent(context, PostDetailActivity.class);
        intent.putExtra(EXTRA_TID, tid);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_FORUM_NAME, forumName);
        intent.putExtra(EXTRA_AUTHOR, author);
        intent.putExtra(EXTRA_AUTHOR_AVATAR, authorAvatar);
        intent.putExtra(EXTRA_AUTHOR_LEVEL, authorLevel);
        context.startActivity(intent);
    }

    public static void start(Context context, int tid, String title, String forumName, String author, 
                             String authorAvatar, String authorLevel, Bundle options) {
        Intent intent = new Intent(context, PostDetailActivity.class);
        intent.putExtra(EXTRA_TID, tid);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_FORUM_NAME, forumName);
        intent.putExtra(EXTRA_AUTHOR, author);
        intent.putExtra(EXTRA_AUTHOR_AVATAR, authorAvatar);
        intent.putExtra(EXTRA_AUTHOR_LEVEL, authorLevel);
        context.startActivity(intent, options);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        binding = ActivityPostDetailBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        // 设置状态栏图标为深色，与白色AppBar背景形成良好对比
        setStatusBarLight();

        currentTid = getIntent().getIntExtra(EXTRA_TID, 0);
        highlightAuthorId = getIntent().getIntExtra(EXTRA_HIGHLIGHT_AUTHOR_ID, 0); // 获取高亮作者ID
        String title = getIntent().getStringExtra(EXTRA_TITLE);
        String author = getIntent().getStringExtra(EXTRA_AUTHOR);
        String authorAvatar = getIntent().getStringExtra(EXTRA_AUTHOR_AVATAR);
        String authorLevel = getIntent().getStringExtra(EXTRA_AUTHOR_LEVEL);
        String forumName = getIntent().getStringExtra(EXTRA_FORUM_NAME);

        if (currentTid == 0) {
            Toast.makeText(this, "无效的帖子ID", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        initViewModel();
        setupToolbar(title);
        setupContentList();
        setupCommentList();
        setupClickListeners();
        observeViewModel();

        // 先显示基本信息，保证动画流畅
        if (title != null || author != null) {
            // 延迟共享元素动画，等待布局完成
            supportPostponeEnterTransition();
            showBasicInfo(title, forumName, author, authorAvatar, authorLevel);
            
            // 使用 OnGlobalLayoutListener 确保布局完成后开始动画
            binding.authorAvatar.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            binding.authorAvatar.getViewTreeObserver().removeOnGlobalLayoutListener(this);
                            supportStartPostponedEnterTransition();
                        }
                    });
        }

        // 然后加载详细内容
        viewModel.loadPostDetail(currentTid);
    }

    /**
     * 显示基本信息，用于共享元素动画时先展示内容
     */
    private void showBasicInfo(String title, String forumName, String author, String authorAvatar, String authorLevel) {
        hasShownBasicInfo = true;
        
        // 显示内容区域
        binding.contentScrollView.setVisibility(View.VISIBLE);
        binding.bottomBar.setVisibility(View.VISIBLE);
        binding.progressBar.setVisibility(View.GONE);
        binding.errorView.setVisibility(View.GONE);

        // 标题栏显示板块名称
        binding.toolbarTitle.setText(forumName != null && !forumName.isEmpty() ? forumName : "帖子详情");

        // 标题
        if (title != null && !title.isEmpty()) {
            binding.postTitle.setText(title);
        }

        // 作者
        if (author != null && !author.isEmpty()) {
            binding.authorName.setText(author);
        }

        // 头像 - 使用 dontAnimate() 避免干扰共享元素动画
        // 禁用磁盘缓存以便头像更新后能及时显示
        if (authorAvatar != null && !authorAvatar.isEmpty()) {
            Glide.with(this)
                    .load(authorAvatar)
                    .placeholder(R.drawable.ic_forum)
                    .error(R.drawable.ic_forum)
                    .dontAnimate()
                    .diskCacheStrategy(DiskCacheStrategy.NONE)
                    .into(binding.authorAvatar);
        }

        // 等级
        if (authorLevel != null && !authorLevel.isEmpty()) {
            binding.authorLevel.setText(authorLevel);
            binding.authorLevel.setVisibility(View.VISIBLE);
            setLevelBackground(binding.authorLevel, authorLevel);
        }
    }

    private void initViewModel() {
        ForumApi forumApi = new ForumApi(
                HttpClient.getInstance(this).getOkHttpClient(),
                HttpClient.getInstance(this).getCookieManager()
        );
        viewModel = new ViewModelProvider(this, new PostDetailViewModel.Factory(forumApi))
                .get(PostDetailViewModel.class);
        
        // 初始化浏览历史ViewModel
        browseHistoryViewModel = new ViewModelProvider(this)
                .get(BrowseHistoryViewModel.class);
    }

    /**
     * 根据当前主题设置状态栏图标颜色
     */
    private void setStatusBarLight() {
        getWindow().getDecorView().post(() -> {
            int currentNightMode = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
            if (currentNightMode == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
                // 深色模式：状态栏使用浅色图标
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            } else {
                // 浅色模式：状态栏使用深色图标
                getWindow().getDecorView().setSystemUiVisibility(
                        View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR |
                        View.SYSTEM_UI_FLAG_LAYOUT_STABLE |
                        View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN);
            }
            getWindow().setStatusBarColor(Color.TRANSPARENT);
        });
    }

    private void setupToolbar(String title) {
        binding.toolbar.setNavigationOnClickListener(v -> finish());
        // 默认显示"帖子详情"，等数据加载后显示板块名称
        binding.toolbarTitle.setText("帖子详情");
    }

    private void setupContentList() {
        contentAdapter = new ContentAdapter();
        binding.contentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.contentRecyclerView.setNestedScrollingEnabled(false);
        // 注意：不使用 setHasFixedSize(true)，因为 RecyclerView 高度是 wrap_content
        // 禁用 item 变化动画，避免数据更新时的闪烁
        binding.contentRecyclerView.setItemAnimator(null);
        binding.contentRecyclerView.setAdapter(contentAdapter);

        // 设置图片点击监听
        contentAdapter.setImageClickListener((imageUrl, position, allImages) -> {
            Intent intent = new Intent(PostDetailActivity.this, ImageViewerActivity.class);
            intent.putStringArrayListExtra(ImageViewerActivity.EXTRA_IMAGE_URLS, new ArrayList<>(allImages));
            intent.putExtra(ImageViewerActivity.EXTRA_CURRENT_POSITION, position);
            startActivity(intent);
        });

        // 设置购买帖子点击监听
        contentAdapter.setBuyPostClickListener((block, position) -> {
            buyPaidPost();
        });
    }

    private void setupCommentList() {
        commentAdapter = new CommentAdapter();
        // 设置需要高亮的评论作者ID（从"我的评论"页面进入时）
        if (highlightAuthorId > 0) {
            commentAdapter.setHighlightAuthorId(highlightAuthorId);
        }
        
        // 设置评论操作监听器（长按复制/回复/删除/点赞）
        commentAdapter.setOnCommentActionListener(new CommentAdapter.OnCommentActionListener() {
            @Override
            public void onReplyComment(Comment comment) {
                // 回复指定评论
                showReplyBottomSheet(comment);
            }
            
            @Override
            public void onCopyContent(String content) {
                // 复制评论内容到剪贴板
                copyToClipboard(content);
            }
            
            @Override
            public void onDeleteComment(Comment comment) {
                // 删除评论
                showDeleteCommentDialog(comment);
            }
            
            @Override
            public void onLikeComment(Comment comment) {
                if (!viewModel.isLoggedIn()) {
                    Toast.makeText(PostDetailActivity.this, "请先登录", Toast.LENGTH_SHORT).show();
                    return;
                }
                // 点赞/取消点赞评论（踢贴）
                viewModel.toggleCommentLike(comment);
            }
            
            @Override
            public void onRewardComment(Comment comment) {
                if (!viewModel.isLoggedIn()) {
                    Toast.makeText(PostDetailActivity.this, "请先登录", Toast.LENGTH_SHORT).show();
                    return;
                }
                // 打赏评论
                showRewardDialog(comment);
            }
            
            @Override
            public void onReportComment(Comment comment) {
                if (!viewModel.isLoggedIn()) {
                    Toast.makeText(PostDetailActivity.this, "请先登录", Toast.LENGTH_SHORT).show();
                    return;
                }
                // 举报评论
                showReportCommentDialog(comment);
            }
        });
        
        // 设置用户点击监听器
        commentAdapter.setOnUserClickListener((uid, username, avatar, avatarView) -> {
            UserDetailActivity.start(this, uid, username, avatar, avatarView);
        });
        
        binding.commentRecyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.commentRecyclerView.setNestedScrollingEnabled(false);
        binding.commentRecyclerView.setAdapter(commentAdapter);
    }
    
    /**
     * 复制文本到剪贴板
     */
    private void copyToClipboard(String text) {
        android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            android.content.ClipData clip = android.content.ClipData.newPlainText("评论内容", text);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(this, "已复制到剪贴板", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 显示删除评论确认对话框
     */
    private void showDeleteCommentDialog(Comment comment) {
        if (!viewModel.isLoggedIn()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        new androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("删除评论")
                .setMessage("确定要删除这条评论吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    deleteComment(comment);
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 删除评论
     */
    private void deleteComment(Comment comment) {
        if (currentPost == null) {
            Toast.makeText(this, "帖子信息加载中，请稍后重试", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int fid = currentPost.getForumId();
        int tid = currentTid;
        int pid = comment.getId();
        
        // 异步执行删除
        new Thread(() -> {
            HttpClient httpClient = HttpClient.getInstance(PostDetailActivity.this);
            ForumApi forumApi = new ForumApi(httpClient.getOkHttpClient(), httpClient.getCookieManager());
            ApiResponse<Boolean> response = forumApi.deleteComment(fid, tid, pid);
            
            runOnUiThread(() -> {
                if (response.isSuccess() && response.getData() != null && response.getData()) {
                    Toast.makeText(PostDetailActivity.this, "删除成功", Toast.LENGTH_SHORT).show();
                    // 刷新评论列表
                    viewModel.refreshComments();
                    // 重新获取帖子内容
                    viewModel.refreshPostContent();
                } else {
                    String errorMsg = response.getMessage();
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = "删除失败";
                    }
                    Toast.makeText(PostDetailActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void setupClickListeners() {
        // 注意：followButton 的点击监听在 updateFollowButton 方法中设置
        // 因为需要根据是否为自己的帖子显示不同的按钮（编辑/关注）

        binding.likeButton.setOnClickListener(v -> {
            isUserLikeAction = true;
            viewModel.toggleLike();
        });

        // 踢帖按钮
        binding.kickPostButton.setOnClickListener(v -> {
            if (!viewModel.isLoggedIn()) {
                Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
                return;
            }
            showKickPostDialog();
        });

        // 底部点赞按钮
        binding.bottomLikeButton.setOnClickListener(v -> {
            isUserLikeAction = true;
            viewModel.toggleLike();
        });

        // 底部收藏按钮
        binding.bottomFavoriteButton.setOnClickListener(v -> {
            isUserFavoriteAction = true;
            viewModel.toggleFavorite();
        });

        // 底部分享按钮
        binding.bottomShareButton.setOnClickListener(v -> {
            showShareBottomSheet();
        });

        binding.commentInput.setOnClickListener(v -> showReplyBottomSheet());

        binding.moreButton.setOnClickListener(v -> showShareBottomSheet());

        binding.retryButton.setOnClickListener(v -> viewModel.retry());

        // 评论排序标签点击事件
        setupSortTabs();

        // 分页控件
        setupPaginationListeners();
    }

    /**
     * 设置排序标签点击监听
     */
    private void setupSortTabs() {
        binding.sortTabLatest.setOnClickListener(v -> {
            if (viewModel != null && viewModel.getCurrentOrderType() != ORDER_LATEST) {
                updateSortTabSelection(ORDER_LATEST);
                viewModel.changeOrderType(ORDER_LATEST);
            }
        });

        binding.sortTabEarliest.setOnClickListener(v -> {
            if (viewModel != null && viewModel.getCurrentOrderType() != ORDER_EARLIEST) {
                updateSortTabSelection(ORDER_EARLIEST);
                viewModel.changeOrderType(ORDER_EARLIEST);
            }
        });

        binding.sortTabAuthor.setOnClickListener(v -> {
            if (viewModel != null && viewModel.getCurrentOrderType() != ORDER_AUTHOR) {
                updateSortTabSelection(ORDER_AUTHOR);
                viewModel.changeOrderType(ORDER_AUTHOR);
            }
        });
        
        // 设置默认选中"最新"
        updateSortTabSelection(ORDER_LATEST);
    }

    /**
     * 更新踢帖按钮显示
     * 显示踢帖人数格式: 踢帖(1/5)
     */
    private void updateKickPostButton(Post post) {
        // 始终显示踢帖按钮
        binding.kickPostButton.setVisibility(View.VISIBLE);
        
        // 异步获取踢帖信息更新人数显示
        new Thread(() -> {
            HttpClient httpClient = HttpClient.getInstance(PostDetailActivity.this);
            ForumApi forumApi = new ForumApi(httpClient.getOkHttpClient(), httpClient.getCookieManager());
            
            ForumApi.KickPostInfo info = forumApi.getKickPostInfo(currentTid);
            
            runOnUiThread(() -> {
                if (info != null && info.getKickCount() >= 0) {
                    binding.kickPostButton.setText("踢帖(" + info.getKickCount() + "/5)");
                } else {
                    binding.kickPostButton.setText("踢帖");
                }
            });
        }).start();
    }

    /**
     * 显示举报评论对话框
     */
    private void showReportCommentDialog(Comment comment) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_report_comment, null);
        bottomSheetDialog.setContentView(sheetView);

        // 设置 Dialog 的软键盘模式
        bottomSheetDialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        RadioGroup reportTypeGroup = sheetView.findViewById(R.id.reportTypeGroup);
        EditText reasonInput = sheetView.findViewById(R.id.reportReasonInput);
        TextView submitButton = sheetView.findViewById(R.id.reportSubmitButton);
        View closeButton = sheetView.findViewById(R.id.reportCloseButton);
        TextView reportTip = sheetView.findViewById(R.id.reportTipText);
        View reasonInputContainer = sheetView.findViewById(R.id.reasonInputContainer);

        // 保存当前选择的举报类型
        final int[] selectedType = {0}; // 0表示未选择

        // 设置提示信息
        if (reportTip != null) {
            reportTip.setText("举报 " + comment.getAuthor() + " 的评论");
        }

        // 举报类型选择监听
        reportTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.reportType1) {
                selectedType[0] = 1;
                reasonInputContainer.setVisibility(View.GONE);
            } else if (checkedId == R.id.reportType2) {
                selectedType[0] = 2;
                reasonInputContainer.setVisibility(View.GONE);
            } else if (checkedId == R.id.reportType3) {
                selectedType[0] = 3;
                reasonInputContainer.setVisibility(View.GONE);
            } else if (checkedId == R.id.reportType4) {
                selectedType[0] = 4;
                reasonInputContainer.setVisibility(View.GONE);
            } else if (checkedId == R.id.reportType5) {
                selectedType[0] = 5;
                reasonInputContainer.setVisibility(View.VISIBLE);
            }
        });

        // 关闭按钮
        closeButton.setOnClickListener(v -> bottomSheetDialog.dismiss());

        // 提交按钮
        submitButton.setOnClickListener(v -> {
            // 检查是否选择了举报类型
            if (selectedType[0] == 0) {
                Toast.makeText(this, "请选择举报类型", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 获取举报理由
            String reason;
            if (selectedType[0] == 5) {
                // 其他违规内容需要填写理由
                reason = reasonInput.getText().toString().trim();
                if (reason.isEmpty()) {
                    Toast.makeText(this, "请填写举报理由", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (reason.length() < 5) {
                    Toast.makeText(this, "举报理由至少5个字符", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                // 预设类型使用固定理由
                String[] typeReasons = {
                    "", // 占位
                    "广告/垃圾内容",
                    "恶意灌水/无意义内容",
                    "辱骂/人身攻击",
                    "抄袭/侵权内容"
                };
                reason = typeReasons[selectedType[0]];
            }
            
            submitButton.setText("提交中...");
            submitButton.setEnabled(false);
            
            // 执行举报
            performReportComment(comment, reason, bottomSheetDialog, submitButton);
        });

        bottomSheetDialog.show();
    }

    /**
     * 执行举报评论
     */
    private void performReportComment(Comment comment, String reason, BottomSheetDialog dialog, TextView submitButton) {
        new Thread(() -> {
            HttpClient httpClient = HttpClient.getInstance(PostDetailActivity.this);
            ForumApi forumApi = new ForumApi(httpClient.getOkHttpClient(), httpClient.getCookieManager());
            
            int fid = comment.getFid();
            int tid = currentTid;
            int pid = comment.getId();
            
            // 如果评论没有fid，使用帖子的fid
            if (fid <= 0 && currentPost != null) {
                fid = currentPost.getForumId();
            }
            
            ForumApi.ReportCommentResult result = forumApi.reportComment(fid, tid, pid, reason);
            
            runOnUiThread(() -> {
                if (result.isSuccess()) {
                    Toast.makeText(PostDetailActivity.this, result.getMessage(), Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(PostDetailActivity.this, result.getMessage(), Toast.LENGTH_SHORT).show();
                    submitButton.setText("提交举报");
                    submitButton.setEnabled(true);
                }
            });
        }).start();
    }

    /**
     * 显示举报帖子对话框
     */
    private void showReportPostDialog() {
        if (currentPost == null) {
            Toast.makeText(this, "帖子信息加载中", Toast.LENGTH_SHORT).show();
            return;
        }
        
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_report_post, null);
        bottomSheetDialog.setContentView(sheetView);

        // 设置 Dialog 的软键盘模式
        bottomSheetDialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        RadioGroup reportTypeGroup = sheetView.findViewById(R.id.reportTypeGroup);
        EditText reasonInput = sheetView.findViewById(R.id.reportReasonInput);
        TextView submitButton = sheetView.findViewById(R.id.reportSubmitButton);
        View closeButton = sheetView.findViewById(R.id.reportCloseButton);
        TextView reportTip = sheetView.findViewById(R.id.reportTipText);
        View reasonInputContainer = sheetView.findViewById(R.id.reasonInputContainer);

        // 保存当前选择的举报类型
        final int[] selectedType = {0}; // 0表示未选择

        // 设置提示信息
        if (reportTip != null) {
            reportTip.setText("举报帖子: " + currentPost.getTitle());
        }

        // 举报类型选择监听
        reportTypeGroup.setOnCheckedChangeListener((group, checkedId) -> {
            if (checkedId == R.id.reportType1) {
                selectedType[0] = 1;
                reasonInputContainer.setVisibility(View.GONE);
            } else if (checkedId == R.id.reportType2) {
                selectedType[0] = 2;
                reasonInputContainer.setVisibility(View.GONE);
            } else if (checkedId == R.id.reportType3) {
                selectedType[0] = 3;
                reasonInputContainer.setVisibility(View.GONE);
            } else if (checkedId == R.id.reportType4) {
                selectedType[0] = 4;
                reasonInputContainer.setVisibility(View.GONE);
            } else if (checkedId == R.id.reportType5) {
                selectedType[0] = 5;
                reasonInputContainer.setVisibility(View.VISIBLE);
            }
        });

        // 关闭按钮
        closeButton.setOnClickListener(v -> bottomSheetDialog.dismiss());

        // 提交按钮
        submitButton.setOnClickListener(v -> {
            // 检查是否选择了举报类型
            if (selectedType[0] == 0) {
                Toast.makeText(this, "请选择举报类型", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 获取举报理由
            String reason;
            if (selectedType[0] == 5) {
                // 其他违规内容需要填写理由
                reason = reasonInput.getText().toString().trim();
                if (reason.isEmpty()) {
                    Toast.makeText(this, "请填写举报理由", Toast.LENGTH_SHORT).show();
                    return;
                }
                if (reason.length() < 5) {
                    Toast.makeText(this, "举报理由至少5个字符", Toast.LENGTH_SHORT).show();
                    return;
                }
            } else {
                // 预设类型使用固定理由
                String[] typeReasons = {
                    "", // 占位
                    "广告/垃圾内容",
                    "恶意灌水/无意义内容",
                    "辱骂/人身攻击",
                    "抄袭/侵权内容"
                };
                reason = typeReasons[selectedType[0]];
            }
            
            submitButton.setText("提交中...");
            submitButton.setEnabled(false);
            
            // 执行举报帖子
            performReportPost(reason, bottomSheetDialog, submitButton);
        });

        bottomSheetDialog.show();
    }

    /**
     * 执行举报帖子
     */
    private void performReportPost(String reason, BottomSheetDialog dialog, TextView submitButton) {
        new Thread(() -> {
            HttpClient httpClient = HttpClient.getInstance(PostDetailActivity.this);
            ForumApi forumApi = new ForumApi(httpClient.getOkHttpClient(), httpClient.getCookieManager());
            
            int tid = currentTid;
            
            // 获取踢帖信息以获取m参数
            ForumApi.KickPostInfo info = forumApi.getKickPostInfo(tid);
            String mParam = info != null ? info.getMParam() : null;
            
            if (mParam == null || mParam.isEmpty()) {
                runOnUiThread(() -> {
                    Toast.makeText(PostDetailActivity.this, "无法获取举报参数，请稍后重试", Toast.LENGTH_SHORT).show();
                    submitButton.setText("提交举报");
                    submitButton.setEnabled(true);
                });
                return;
            }
            
            // 执行踢帖（举报帖子）
            ForumApi.KickPostResult result = forumApi.kickPost(tid, mParam, reason);
            
            runOnUiThread(() -> {
                if (result.isSuccess()) {
                    Toast.makeText(PostDetailActivity.this, "举报成功，感谢您的反馈", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                } else {
                    Toast.makeText(PostDetailActivity.this, result.getMessage(), Toast.LENGTH_SHORT).show();
                    submitButton.setText("提交举报");
                    submitButton.setEnabled(true);
                }
            });
        }).start();
    }

    /**
     * 显示踢帖对话框
     */
    private void showKickPostDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_kick_post, null);
        bottomSheetDialog.setContentView(sheetView);

        // 设置 Dialog 的软键盘模式
        bottomSheetDialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        EditText reasonInput = sheetView.findViewById(R.id.kickReasonInput);
        TextView submitButton = sheetView.findViewById(R.id.kickSubmitButton);
        View closeButton = sheetView.findViewById(R.id.kickCloseButton);
        TextView kickCountText = sheetView.findViewById(R.id.kickCountText);
        LinearLayout kickUsersContainer = sheetView.findViewById(R.id.kickUsersContainer);
        LinearLayout kickUsersAvatars = sheetView.findViewById(R.id.kickUsersAvatars);
        TextView kickUsersMore = sheetView.findViewById(R.id.kickUsersMore);

        // 关闭按钮
        closeButton.setOnClickListener(v -> bottomSheetDialog.dismiss());

        // 异步获取踢帖信息
        new Thread(() -> {
            HttpClient httpClient = HttpClient.getInstance(PostDetailActivity.this);
            ForumApi forumApi = new ForumApi(httpClient.getOkHttpClient(), httpClient.getCookieManager());
            
            ForumApi.KickPostInfo info = forumApi.getKickPostInfo(currentTid);
            
            runOnUiThread(() -> {
                if (info != null && info.getKickCount() >= 0) {
                    kickCountText.setText("当前踢帖人数：" + info.getKickCount() + "/5");
                    kickCountText.setVisibility(View.VISIBLE);
                    
                    // 显示踢帖人列表
                    java.util.List<ForumApi.KickPostUser> kickUsers = info.getKickUsers();
                    if (kickUsers != null && !kickUsers.isEmpty()) {
                        kickUsersContainer.setVisibility(View.VISIBLE);
                        kickUsersAvatars.removeAllViews();
                        
                        int maxAvatars = Math.min(kickUsers.size(), 5);
                        for (int i = 0; i < maxAvatars; i++) {
                            ForumApi.KickPostUser user = kickUsers.get(i);
                            de.hdodenhof.circleimageview.CircleImageView avatarView =
                                    new de.hdodenhof.circleimageview.CircleImageView(PostDetailActivity.this);
                            int size = (int) (24 * getResources().getDisplayMetrics().density);
                            LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                            if (i > 0) {
                                params.setMarginStart((int) (-8 * getResources().getDisplayMetrics().density));
                            }
                            avatarView.setLayoutParams(params);
                            avatarView.setBorderColor(Color.WHITE);
                            avatarView.setBorderWidth(2);

                            Glide.with(PostDetailActivity.this)
                                    .load(user.getAvatar())
                                    .placeholder(R.drawable.ic_forum)
                                    .error(R.drawable.ic_forum)
                                    .into(avatarView);
                            
                            // 设置点击跳转到用户详情页
                            avatarView.setOnClickListener(v -> {
                                UserDetailActivity.start(PostDetailActivity.this, user.getUid(), 
                                        user.getUsername(), user.getAvatar(), avatarView);
                            });

                            kickUsersAvatars.addView(avatarView);
                        }
                        
                        if (kickUsers.size() > 5) {
                            kickUsersMore.setVisibility(View.VISIBLE);
                            kickUsersMore.setText("等" + kickUsers.size() + "人");
                        } else {
                            kickUsersMore.setVisibility(View.GONE);
                        }
                    } else {
                        kickUsersContainer.setVisibility(View.GONE);
                    }
                } else {
                    kickCountText.setVisibility(View.GONE);
                    kickUsersContainer.setVisibility(View.GONE);
                }
            });
        }).start();

        // 提交按钮
        submitButton.setOnClickListener(v -> {
            String reason = reasonInput.getText().toString().trim();
            if (reason.isEmpty()) {
                Toast.makeText(this, "请填写踢帖理由", Toast.LENGTH_SHORT).show();
                return;
            }
            if (reason.length() < 5) {
                Toast.makeText(this, "踢帖理由至少5个字符", Toast.LENGTH_SHORT).show();
                return;
            }

            // 禁用按钮
            submitButton.setEnabled(false);
            submitButton.setText("提交中...");

            // 执行踢帖
            performKickPost(reason, bottomSheetDialog, submitButton);
        });

        bottomSheetDialog.show();

        // 自动弹出键盘
        reasonInput.postDelayed(() -> {
            reasonInput.requestFocus();
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(reasonInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200);
    }

    /**
     * 执行踢帖
     */
    private void performKickPost(String reason, BottomSheetDialog dialog, TextView submitButton) {
        new Thread(() -> {
            HttpClient httpClient = HttpClient.getInstance(PostDetailActivity.this);
            ForumApi forumApi = new ForumApi(httpClient.getOkHttpClient(), httpClient.getCookieManager());

            // 先获取踢帖信息（包含m参数）
            ForumApi.KickPostInfo info = forumApi.getKickPostInfo(currentTid);

            if (info.getMParam() == null || info.getMParam().isEmpty()) {
                runOnUiThread(() -> {
                    Toast.makeText(PostDetailActivity.this, "无法获取踢帖参数", Toast.LENGTH_SHORT).show();
                    submitButton.setEnabled(true);
                    submitButton.setText("确定踢帖");
                });
                return;
            }

            // 执行踢帖
            String mParam = info.getMParam();
            ForumApi.KickPostResult kickResult = forumApi.kickPost(currentTid, mParam, reason);

            runOnUiThread(() -> {
                if (kickResult.isSuccess()) {
                    Toast.makeText(PostDetailActivity.this, "踢帖成功", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    // 刷新踢帖人数显示
                    if (currentPost != null) {
                        updateKickPostButton(currentPost);
                    }
                } else {
                    Toast.makeText(PostDetailActivity.this, kickResult.getMessage(), Toast.LENGTH_SHORT).show();
                    submitButton.setEnabled(true);
                    submitButton.setText("确定踢帖");
                }
            });
        }).start();
    }

    /**
     * 显示分享底部弹窗
     */
    private void showShareBottomSheet() {
        if (currentPost == null) {
            Toast.makeText(this, "帖子信息加载中", Toast.LENGTH_SHORT).show();
            return;
        }
        
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_share, null);
        bottomSheetDialog.setContentView(sheetView);
        
        // 初始化分享工具
        ShareHelper shareHelper = new ShareHelper(this).setPost(currentPost);
        
        // 预览标题和链接
        TextView titlePreview = sheetView.findViewById(R.id.shareTitlePreview);
        TextView urlPreview = sheetView.findViewById(R.id.shareUrlPreview);
        titlePreview.setText(currentPost.getTitle());
        urlPreview.setText(currentPost.getPostUrl());
        
        // 微信好友
        sheetView.findViewById(R.id.shareWechat).setOnClickListener(v -> {
            shareHelper.shareToWechat();
            bottomSheetDialog.dismiss();
        });
        
        // 朋友圈
        sheetView.findViewById(R.id.shareMoments).setOnClickListener(v -> {
            shareHelper.shareToWechatMoments();
            bottomSheetDialog.dismiss();
        });
        
        // QQ好友
        sheetView.findViewById(R.id.shareQQ).setOnClickListener(v -> {
            shareHelper.shareToQQ();
            bottomSheetDialog.dismiss();
        });
        
        // QQ空间
        sheetView.findViewById(R.id.shareQZone).setOnClickListener(v -> {
            shareHelper.shareToQZone();
            bottomSheetDialog.dismiss();
        });
        
        // 新浪微博
        sheetView.findViewById(R.id.shareWeibo).setOnClickListener(v -> {
            shareHelper.shareToWeibo();
            bottomSheetDialog.dismiss();
        });
        
        // 复制链接
        sheetView.findViewById(R.id.shareCopyLink).setOnClickListener(v -> {
            shareHelper.copyShareContent();
            bottomSheetDialog.dismiss();
        });
        
        // 更多（系统分享）
        sheetView.findViewById(R.id.shareMore).setOnClickListener(v -> {
            shareHelper.shareGeneric();
            bottomSheetDialog.dismiss();
        });
        
        // 举报帖子
        sheetView.findViewById(R.id.shareReport).setOnClickListener(v -> {
            bottomSheetDialog.dismiss();
            if (!viewModel.isLoggedIn()) {
                Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
                return;
            }
            showReportPostDialog();
        });
        
        bottomSheetDialog.show();
    }

    /**
     * 显示回复底部弹窗
     */
    private void showReplyBottomSheet() {
        // 检查登录状态
        if (!viewModel.isLoggedIn()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_reply, null);
        bottomSheetDialog.setContentView(sheetView);

        // 设置 Dialog 的软键盘模式为 ADJUST_RESIZE，使键盘弹出时内容区域自动调整
        bottomSheetDialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        // 获取 BottomSheetBehavior 以控制弹窗高度
        android.view.View bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        com.google.android.material.bottomsheet.BottomSheetBehavior<android.view.View> behavior = null;
        if (bottomSheet != null) {
            behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet);
            behavior.setPeekHeight(android.view.ViewGroup.LayoutParams.MATCH_PARENT);
            behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        }

        // 获取视图
        EditText replyInput = sheetView.findViewById(R.id.replyInput);
        TextView publishButton = sheetView.findViewById(R.id.publishButton);
        TextView charCount = sheetView.findViewById(R.id.charCount);
        View emojiButton = sheetView.findViewById(R.id.emojiButton);
        View imageButton = sheetView.findViewById(R.id.imageButton);
        View mentionButton = sheetView.findViewById(R.id.mentionButton);
        View smileyPanelContainer = sheetView.findViewById(R.id.smileyPanelContainer);
        View imagePreviewContainer = sheetView.findViewById(R.id.imagePreviewContainer);
        androidx.recyclerview.widget.RecyclerView imagePreviewList = sheetView.findViewById(R.id.imagePreviewList);

        // 初始化表情面板（支持分类切换）
        setupSmileyPanel(smileyPanelContainer, replyInput);
        
        // 保存当前输入框引用
        currentReplyInput = replyInput;
        
        // 初始化图片预览
        setupImagePreview(imagePreviewList, imagePreviewContainer);

        // 字数统计
        replyInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s.length();
                charCount.setText(length + "/500");
                charCount.setTextColor(length > 450 ? Color.parseColor("#FF6B4A") : Color.parseColor("#CCCCCC"));
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 发布按钮
        BottomSheetDialog finalDialog = bottomSheetDialog;
        publishButton.setOnClickListener(v -> {
            String content = replyInput.getText().toString().trim();
            
            // 检查是否有内容或图片
            boolean hasContent = !content.isEmpty();
            boolean hasImages = imagePreviewAdapter != null && !imagePreviewAdapter.getImages().isEmpty();
            
            if (!hasContent && !hasImages) {
                Toast.makeText(this, "请输入评论内容或选择图片", Toast.LENGTH_SHORT).show();
                return;
            }
            if (content.length() > 500) {
                Toast.makeText(this, "评论内容不能超过500字", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 检查是否有正在上传的图片
            if (imagePreviewAdapter != null && imagePreviewAdapter.hasUploadingImages()) {
                Toast.makeText(this, "图片上传中，请稍候", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 执行回复
            submitReplyWithImages(content, finalDialog, publishButton, replyInput);
        });

        // 表情按钮 - 切换表情面板显示
        View finalSmileyPanelContainer = smileyPanelContainer;
        com.google.android.material.bottomsheet.BottomSheetBehavior<android.view.View> finalBehavior = behavior;
        emojiButton.setOnClickListener(v -> {
            if (finalSmileyPanelContainer.getVisibility() == View.VISIBLE) {
                finalSmileyPanelContainer.setVisibility(View.GONE);
                // 重新显示键盘
                replyInput.requestFocus();
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(replyInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                }
            } else {
                // 隐藏键盘
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(replyInput.getWindowToken(), 0);
                }
                // 显示表情面板
                finalSmileyPanelContainer.setVisibility(View.VISIBLE);
                // 确保弹窗保持展开状态
                if (finalBehavior != null) {
                    finalBehavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
                }
            }
        });

        // 图片按钮 - 打开图片选择器
        com.google.android.material.bottomsheet.BottomSheetBehavior<android.view.View> finalBehavior2 = behavior;
        imageButton.setOnClickListener(v -> {
            // 隐藏表情面板
            smileyPanelContainer.setVisibility(View.GONE);
            // 隐藏键盘
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(replyInput.getWindowToken(), 0);
            }
            // 确保弹窗保持展开状态，以便图片预览可见
            if (finalBehavior2 != null) {
                finalBehavior2.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            }
            // 检查上传凭证
            if (currentPost == null || currentPost.getUploadHash() == null || currentPost.getUploadHash().isEmpty()) {
                Toast.makeText(this, "上传凭证获取失败，请刷新页面", Toast.LENGTH_SHORT).show();
                return;
            }
            // 检查图片数量限制
            if (imagePreviewAdapter != null && imagePreviewAdapter.getImages().size() >= 9) {
                Toast.makeText(this, "最多添加9张图片", Toast.LENGTH_SHORT).show();
                return;
            }
            // 打开图片选择器
            openImagePicker();
        });

        // @按钮
        mentionButton.setOnClickListener(v ->
                Toast.makeText(this, "@功能开发中", Toast.LENGTH_SHORT).show());

        // 监听图片预览容器的可见性变化
        final com.google.android.material.bottomsheet.BottomSheetBehavior<android.view.View> finalBehavior3 = behavior;
        if (finalBehavior3 != null) {
            imagePreviewContainer.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            if (imagePreviewContainer.getVisibility() == View.VISIBLE) {
                                // 图片预览显示时，确保弹窗展开
                                finalBehavior3.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
                            }
                        }
                    });
        }

        bottomSheetDialog.show();

        // 自动弹出键盘
        replyInput.postDelayed(() -> {
            replyInput.requestFocus();
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) 
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(replyInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200);
    }
    
    /**
     * 显示回复底部弹窗（回复指定评论）
     * @param replyToComment 要回复的评论
     */
    private void showReplyBottomSheet(Comment replyToComment) {
        // 检查登录状态
        if (!viewModel.isLoggedIn()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.bottom_sheet_reply, null);
        bottomSheetDialog.setContentView(sheetView);

        // 设置 Dialog 的软键盘模式为 ADJUST_RESIZE，使键盘弹出时内容区域自动调整
        bottomSheetDialog.getWindow().setSoftInputMode(android.view.WindowManager.LayoutParams.SOFT_INPUT_ADJUST_RESIZE);

        // 获取 BottomSheetBehavior 以控制弹窗高度
        android.view.View bottomSheet = bottomSheetDialog.findViewById(com.google.android.material.R.id.design_bottom_sheet);
        com.google.android.material.bottomsheet.BottomSheetBehavior<android.view.View> behavior = null;
        if (bottomSheet != null) {
            behavior = com.google.android.material.bottomsheet.BottomSheetBehavior.from(bottomSheet);
            behavior.setPeekHeight(android.view.ViewGroup.LayoutParams.MATCH_PARENT);
            behavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            behavior.setSkipCollapsed(true);
        }

        // 获取视图
        EditText replyInput = sheetView.findViewById(R.id.replyInput);
        TextView publishButton = sheetView.findViewById(R.id.publishButton);
        TextView charCount = sheetView.findViewById(R.id.charCount);
        View emojiButton = sheetView.findViewById(R.id.emojiButton);
        View imageButton = sheetView.findViewById(R.id.imageButton);
        View mentionButton = sheetView.findViewById(R.id.mentionButton);
        View smileyPanelContainer = sheetView.findViewById(R.id.smileyPanelContainer);
        View imagePreviewContainer = sheetView.findViewById(R.id.imagePreviewContainer);
        androidx.recyclerview.widget.RecyclerView imagePreviewList = sheetView.findViewById(R.id.imagePreviewList);
        
        // 获取引用容器视图
        View replyToContainer = sheetView.findViewById(R.id.replyToContainer);
        TextView replyToAuthor = sheetView.findViewById(R.id.replyToAuthor);
        TextView replyToFloor = sheetView.findViewById(R.id.replyToFloor);
        TextView replyToContent = sheetView.findViewById(R.id.replyToContent);
        View clearReplyTo = sheetView.findViewById(R.id.clearReplyTo);

        // 显示回复目标信息
        if (replyToContainer != null) {
            replyToContainer.setVisibility(View.VISIBLE);
            
            // 设置回复目标作者
            if (replyToAuthor != null) {
                String authorName = replyToComment.getAuthor();
                replyToAuthor.setText(authorName != null ? authorName : "匿名用户");
            }
            
            // 设置楼层
            if (replyToFloor != null) {
                int floor = replyToComment.getFloor();
                String floorLabel = floor > 0 ? Comment.getFloorLabel(floor) : "";
                replyToFloor.setText(floorLabel);
            }
            
            // 设置引用内容预览（截取前50个字符）
            if (replyToContent != null) {
                String content = getCommentPlainText(replyToComment);
                if (content != null && content.length() > 50) {
                    content = content.substring(0, 50) + "...";
                }
                replyToContent.setText(content != null ? content : "");
            }
            
            // 清除回复目标按钮
            if (clearReplyTo != null) {
                clearReplyTo.setOnClickListener(v -> {
                    replyToContainer.setVisibility(View.GONE);
                    // 重新设置发布按钮为普通回复（不是回复评论）
                    // 注意：这里仍然保持回复评论模式，只是隐藏引用显示
                });
            }
        }

        // 初始化表情面板（支持分类切换）
        setupSmileyPanel(smileyPanelContainer, replyInput);
        
        // 保存当前输入框引用
        currentReplyInput = replyInput;
        
        // 初始化图片预览
        setupImagePreview(imagePreviewList, imagePreviewContainer);

        // 字数统计
        replyInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                int length = s.length();
                charCount.setText(length + "/500");
                charCount.setTextColor(length > 450 ? Color.parseColor("#FF6B4A") : Color.parseColor("#CCCCCC"));
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        // 发布按钮 - 回复指定评论
        BottomSheetDialog finalDialog = bottomSheetDialog;
        Comment finalReplyToComment = replyToComment;
        publishButton.setOnClickListener(v -> {
            String content = replyInput.getText().toString().trim();
            
            // 检查是否有内容或图片
            boolean hasContent = !content.isEmpty();
            boolean hasImages = imagePreviewAdapter != null && !imagePreviewAdapter.getImages().isEmpty();
            
            if (!hasContent && !hasImages) {
                Toast.makeText(this, "请输入评论内容或选择图片", Toast.LENGTH_SHORT).show();
                return;
            }
            if (content.length() > 500) {
                Toast.makeText(this, "评论内容不能超过500字", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 检查是否有正在上传的图片
            if (imagePreviewAdapter != null && imagePreviewAdapter.hasUploadingImages()) {
                Toast.makeText(this, "图片上传中，请稍候", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 执行回复（带引用）
            submitReplyToComment(content, finalReplyToComment, finalDialog, publishButton, replyInput);
        });

        // 表情按钮 - 切换表情面板显示
        View finalSmileyPanelContainer = smileyPanelContainer;
        com.google.android.material.bottomsheet.BottomSheetBehavior<android.view.View> finalBehavior = behavior;
        emojiButton.setOnClickListener(v -> {
            if (finalSmileyPanelContainer.getVisibility() == View.VISIBLE) {
                finalSmileyPanelContainer.setVisibility(View.GONE);
                // 重新显示键盘
                replyInput.requestFocus();
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.showSoftInput(replyInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
                }
            } else {
                // 隐藏键盘
                android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                        getSystemService(Context.INPUT_METHOD_SERVICE);
                if (imm != null) {
                    imm.hideSoftInputFromWindow(replyInput.getWindowToken(), 0);
                }
                // 显示表情面板
                finalSmileyPanelContainer.setVisibility(View.VISIBLE);
                // 确保弹窗保持展开状态
                if (finalBehavior != null) {
                    finalBehavior.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
                }
            }
        });

        // 图片按钮 - 打开图片选择器
        com.google.android.material.bottomsheet.BottomSheetBehavior<android.view.View> finalBehavior2 = behavior;
        imageButton.setOnClickListener(v -> {
            // 隐藏表情面板
            smileyPanelContainer.setVisibility(View.GONE);
            // 隐藏键盘
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.hideSoftInputFromWindow(replyInput.getWindowToken(), 0);
            }
            // 确保弹窗保持展开状态，以便图片预览可见
            if (finalBehavior2 != null) {
                finalBehavior2.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
            }
            // 检查上传凭证
            if (currentPost == null || currentPost.getUploadHash() == null || currentPost.getUploadHash().isEmpty()) {
                Toast.makeText(this, "上传凭证获取失败，请刷新页面", Toast.LENGTH_SHORT).show();
                return;
            }
            // 检查图片数量限制
            if (imagePreviewAdapter != null && imagePreviewAdapter.getImages().size() >= 9) {
                Toast.makeText(this, "最多添加9张图片", Toast.LENGTH_SHORT).show();
                return;
            }
            // 打开图片选择器
            openImagePicker();
        });

        // @按钮
        mentionButton.setOnClickListener(v ->
                Toast.makeText(this, "@功能开发中", Toast.LENGTH_SHORT).show());

        // 监听图片预览容器的可见性变化
        final com.google.android.material.bottomsheet.BottomSheetBehavior<android.view.View> finalBehavior3 = behavior;
        if (finalBehavior3 != null) {
            imagePreviewContainer.getViewTreeObserver().addOnGlobalLayoutListener(
                    new ViewTreeObserver.OnGlobalLayoutListener() {
                        @Override
                        public void onGlobalLayout() {
                            if (imagePreviewContainer.getVisibility() == View.VISIBLE) {
                                // 图片预览显示时，确保弹窗展开
                                finalBehavior3.setState(com.google.android.material.bottomsheet.BottomSheetBehavior.STATE_EXPANDED);
                            }
                        }
                    });
        }

        bottomSheetDialog.show();

        // 自动弹出键盘
        replyInput.postDelayed(() -> {
            replyInput.requestFocus();
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager) 
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(replyInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        }, 200);
    }
    
    /**
     * 获取评论的纯文本内容
     */
    private String getCommentPlainText(Comment comment) {
        StringBuilder sb = new StringBuilder();
        
        // 从内容块中提取文本
        java.util.List<ContentBlock> blocks = comment.getDisplayContentBlocks();
        if (blocks != null && !blocks.isEmpty()) {
            for (ContentBlock block : blocks) {
                if (block.isText() || block.isRichText()) {
                    String text = block.getContent();
                    if (text != null && !text.isEmpty()) {
                        if (sb.length() > 0) {
                            sb.append("\n");
                        }
                        sb.append(text);
                    }
                } else if (block.isQuote()) {
                    String quote = block.getContent();
                    if (quote != null && !quote.isEmpty()) {
                        if (sb.length() > 0) {
                            sb.append("\n");
                        }
                        sb.append("【引用】").append(quote);
                    }
                }
                // 图片和表情暂时忽略
                else if (block.isImage()) {
                    if (sb.length() > 0) {
                        sb.append("\n");
                    }
                    sb.append("[图片]");
                }
            }
        }
        
        // 如果没有从内容块获取到内容，尝试原始内容
        if (sb.length() == 0) {
            String content = comment.getContent();
            if (content != null && !content.isEmpty()) {
                // 简单去除HTML标签
                sb.append(android.text.Html.fromHtml(content).toString().trim());
            }
        }
        
        return sb.toString();
    }
    
    /**
     * 提交回复（回复指定评论，带引用）
     * 使用两步流程：
     * 1. 先请求回复页面获取noticeauthor等参数
     * 2. 然后提交回复
     */
    private void submitReplyToComment(String content, Comment replyToComment, 
                                       BottomSheetDialog dialog, TextView publishButton, EditText replyInput) {
        if (currentPost == null) {
            Toast.makeText(this, "帖子信息加载中，请稍后重试", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 禁用发布按钮
        publishButton.setEnabled(false);
        publishButton.setText("发布中...");
        
        // 检查是否有需要上传的图片
        java.util.List<Integer> attachIds = null;
        if (imagePreviewAdapter != null && !imagePreviewAdapter.getImages().isEmpty()) {
            attachIds = imagePreviewAdapter.getUploadedAids();
            // 检查是否有上传失败的图片
            if (imagePreviewAdapter.hasFailedImages()) {
                Toast.makeText(this, "部分图片上传失败，请删除后重试", Toast.LENGTH_SHORT).show();
                publishButton.setEnabled(true);
                publishButton.setText("发布");
                return;
            }
        }
        
        // 获取参数
        int fid = currentPost.getForumId();
        int tid = currentTid;
        int replyPid = (replyToComment != null) ? replyToComment.getId() : 0;
        
        final java.util.List<Integer> finalAttachIds = attachIds;
        final String finalContent = content;
        final BottomSheetDialog finalDialog = dialog;
        
        // 异步执行回复
        new Thread(() -> {
            HttpClient httpClient = HttpClient.getInstance(PostDetailActivity.this);
            ForumApi forumApi = new ForumApi(httpClient.getOkHttpClient(), httpClient.getCookieManager());
            
            // 如果是回复楼层评论（replyPid > 0），需要先获取回复参数
            if (replyPid > 0) {
                // 第一步：获取回复参数
                ApiResponse<ForumApi.ReplyQuoteParams> paramsResponse = 
                        forumApi.getReplyQuoteParams(fid, tid, replyPid);
                
                if (!paramsResponse.isSuccess() || paramsResponse.getData() == null) {
                    runOnUiThread(() -> {
                        Toast.makeText(PostDetailActivity.this, 
                                "获取回复参数失败: " + paramsResponse.getMessage(), 
                                Toast.LENGTH_SHORT).show();
                        publishButton.setEnabled(true);
                        publishButton.setText("发布");
                    });
                    return;
                }
                
                // 第二步：使用参数提交回复
                ForumApi.ReplyQuoteParams params = paramsResponse.getData();
                ApiResponse<ForumApi.ReplyResult> response = forumApi.postReplyToComment(
                        fid, tid, finalContent, params, finalAttachIds);
                
                runOnUiThread(() -> handleReplyResponse(response, finalDialog, publishButton));
            } else {
                // 普通回复楼主，使用原有方法
                String formhash = currentPost.getFormhash();
                String noticeauthor = currentPost.getNoticeAuthor();
                ApiResponse<ForumApi.ReplyResult> response = forumApi.postReply(
                        fid, tid, finalContent, formhash, noticeauthor, finalAttachIds, 0);
                
                runOnUiThread(() -> handleReplyResponse(response, finalDialog, publishButton));
            }
        }).start();
    }
    
    /**
     * 处理回复响应
     */
    private void handleReplyResponse(ApiResponse<ForumApi.ReplyResult> response, 
                                      BottomSheetDialog dialog, TextView publishButton) {
        if (response.isSuccess() && response.getData() != null && response.getData().isSuccess()) {
            Toast.makeText(this, "回复成功", Toast.LENGTH_SHORT).show();
            dialog.dismiss();
            // 清空图片
            if (imagePreviewAdapter != null) {
                imagePreviewAdapter.clear();
            }
            // 刷新评论列表
            viewModel.refreshComments();
            // 重新获取帖子内容
            viewModel.refreshPostContent();
        } else {
            String errorMsg = response.getMessage();
            if (errorMsg == null || errorMsg.isEmpty()) {
                errorMsg = response.getData() != null ? response.getData().getMessage() : "回复失败";
            }
            Toast.makeText(this, errorMsg, Toast.LENGTH_SHORT).show();
            publishButton.setEnabled(true);
            publishButton.setText("发布");
        }
    }

    /**
     * 设置表情面板（支持分类切换）
     */
    private void setupSmileyPanel(View smileyPanelContainer, EditText replyInput) {
        // 获取视图
        androidx.recyclerview.widget.RecyclerView smileyGrid = smileyPanelContainer.findViewById(R.id.smileyGrid);
        View deleteButton = smileyPanelContainer.findViewById(R.id.deleteButton);
        TextView tabTb = smileyPanelContainer.findViewById(R.id.tabTb);
        TextView tabQQ = smileyPanelContainer.findViewById(R.id.tabQQ);
        TextView tabDoge = smileyPanelContainer.findViewById(R.id.tabDoge);
        
        // 设置网格布局
        androidx.recyclerview.widget.GridLayoutManager layoutManager = 
                new androidx.recyclerview.widget.GridLayoutManager(this, 7);
        smileyGrid.setLayoutManager(layoutManager);
        
        // 创建适配器
        com.forum.mt.ui.adapter.SmileyAdapter adapter = new com.forum.mt.ui.adapter.SmileyAdapter(
                new com.forum.mt.ui.adapter.SmileyAdapter.OnSmileyClickListener() {
                    @Override
                    public void onSmileyClick(com.forum.mt.model.Smiley smiley) {
                        // 插入表情到输入框
                        insertSmileyToInput(replyInput, smiley);
                    }
                    
                    @Override
                    public void onDeleteClick() {
                        // 删除最后一个字符
                        deleteLastCharacter(replyInput);
                    }
                });
        
        // 默认加载淘宝表情（滑稽）
        adapter.setSmileys(com.forum.mt.model.Smiley.getSmileysByCategory(com.forum.mt.model.Smiley.CATEGORY_TB));
        smileyGrid.setAdapter(adapter);
        
        // 当前选中的分类标签
        final TextView[] currentTab = {tabTb};
        final int[] currentCategory = {com.forum.mt.model.Smiley.CATEGORY_TB};
        
        // 分类标签点击事件
        View.OnClickListener tabClickListener = v -> {
            TextView clickedTab = (TextView) v;
            int newCategory;
            
            if (v == tabTb) {
                newCategory = com.forum.mt.model.Smiley.CATEGORY_TB;
            } else if (v == tabQQ) {
                newCategory = com.forum.mt.model.Smiley.CATEGORY_QQ;
            } else {
                newCategory = com.forum.mt.model.Smiley.CATEGORY_DOGE;
            }
            
            // 如果点击的是当前分类，不做处理
            if (newCategory == currentCategory[0]) {
                return;
            }
            
            // 更新标签样式
            if (currentTab[0] != null) {
                currentTab[0].setTextColor(getResources().getColor(R.color.text_light_gray));
                currentTab[0].setTypeface(null, Typeface.NORMAL);
            }
            clickedTab.setTextColor(getResources().getColor(R.color.text_dark));
            clickedTab.setTypeface(null, Typeface.BOLD);
            
            // 更新当前标签和分类
            currentTab[0] = clickedTab;
            currentCategory[0] = newCategory;
            
            // 加载对应分类的表情
            java.util.List<com.forum.mt.model.Smiley> smileys = com.forum.mt.model.Smiley.getSmileysByCategory(newCategory);
            adapter.setSmileys(smileys);
            
            // 滚动到顶部
            smileyGrid.scrollToPosition(0);
        };
        
        tabTb.setOnClickListener(tabClickListener);
        tabQQ.setOnClickListener(tabClickListener);
        tabDoge.setOnClickListener(tabClickListener);
        
        // 设置默认选中状态
        tabTb.setTextColor(getResources().getColor(R.color.text_dark));
        tabTb.setTypeface(null, Typeface.BOLD);
        
        // 删除按钮
        deleteButton.setOnClickListener(v -> deleteLastCharacter(replyInput));
    }
    
    /**
     * 插入表情到输入框
     */
    private void insertSmileyToInput(EditText editText, com.forum.mt.model.Smiley smiley) {
        int start = editText.getSelectionStart();
        int end = editText.getSelectionEnd();
        
        // 获取表情代码，格式: [s:表情ID]
        String smileyCode = smiley.getCode();
        
        // 插入表情代码
        Editable editable = editText.getText();
        editable.replace(start, end, smileyCode);
        
        // 移动光标到表情代码后面
        editText.setSelection(start + smileyCode.length());
    }
    
    /**
     * 删除最后一个字符（用于表情面板的删除按钮）
     */
    private void deleteLastCharacter(EditText editText) {
        Editable editable = editText.getText();
        int length = editable.length();
        
        if (length > 0) {
            String text = editable.toString();
            int lastOpenBracket = text.lastIndexOf("[");
            
            // 检查是否是表情代码 [xxx] 或 [#xxx]
            if (lastOpenBracket >= 0 && lastOpenBracket < length) {
                int closeBracket = text.indexOf("]", lastOpenBracket);
                // 检查是否有完整的表情代码（中间没有其他 [ 或 ]）
                if (closeBracket > lastOpenBracket && closeBracket < length) {
                    String possibleSmiley = text.substring(lastOpenBracket, closeBracket + 1);
                    // 验证是否是有效表情格式: [#xxx] 或 [xxx]
                    if (possibleSmiley.matches("\\[#?[^\\[\\]]+\\]")) {
                        // 删除整个表情代码
                        editable.delete(lastOpenBracket, closeBracket + 1);
                        return;
                    }
                }
            }
            
            // 删除最后一个字符
            editable.delete(length - 1, length);
        }
    }
    
    /**
     * 设置图片预览
     */
    private void setupImagePreview(androidx.recyclerview.widget.RecyclerView imagePreviewList, 
                                    View imagePreviewContainer) {
        currentImagePreviewContainer = imagePreviewContainer;
        imagePreviewAdapter = new com.forum.mt.ui.adapter.ImagePreviewAdapter(
                new com.forum.mt.ui.adapter.ImagePreviewAdapter.OnImageActionListener() {
                    @Override
                    public void onDeleteClick(int position) {
                        if (imagePreviewAdapter != null) {
                            imagePreviewAdapter.removeImage(position);
                            // 如果没有图片了，隐藏容器
                            if (imagePreviewAdapter.getImages().isEmpty()) {
                                imagePreviewContainer.setVisibility(View.GONE);
                            }
                        }
                    }
                    
                    @Override
                    public void onImageClick(int position) {
                        // 可以添加预览大图功能
                    }
                    
                    @Override
                    public void onInsertClick(int position) {
                        // 插入图片代码到输入框
                        if (imagePreviewAdapter != null && currentReplyInput != null) {
                            List<com.forum.mt.ui.adapter.ImagePreviewAdapter.ImageItem> images = imagePreviewAdapter.getImages();
                            if (position >= 0 && position < images.size()) {
                                com.forum.mt.ui.adapter.ImagePreviewAdapter.ImageItem item = images.get(position);
                                if (item.attachCode != null && !item.attachCode.isEmpty()) {
                                    int start = currentReplyInput.getSelectionStart();
                                    Editable editable = currentReplyInput.getText();
                                    if (editable != null) {
                                        editable.insert(start, item.attachCode + "\n");
                                        Toast.makeText(PostDetailActivity.this, "图片已插入", Toast.LENGTH_SHORT).show();
                                    }
                                }
                            }
                        }
                    }
                });
        
        imagePreviewList.setLayoutManager(
                new androidx.recyclerview.widget.LinearLayoutManager(
                        this, androidx.recyclerview.widget.LinearLayoutManager.HORIZONTAL, false));
        imagePreviewList.setAdapter(imagePreviewAdapter);
    }
    
    /**
     * 打开图片选择器
     */
    private void openImagePicker() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "选择图片"), REQUEST_CODE_PICK_IMAGE);
    }
    
    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        
        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == RESULT_OK && data != null) {
            handleSelectedImages(data);
        }
    }
    
    /**
     * 处理选择的图片
     */
    private void handleSelectedImages(Intent data) {
        if (currentImagePreviewContainer == null || imagePreviewAdapter == null) {
            return;
        }
        
        android.content.ClipData clipData = data.getClipData();
        
        if (clipData != null) {
            // 多选
            for (int i = 0; i < clipData.getItemCount(); i++) {
                if (imagePreviewAdapter.getImages().size() >= 9) {
                    Toast.makeText(this, "最多添加9张图片", Toast.LENGTH_SHORT).show();
                    break;
                }
                android.net.Uri uri = clipData.getItemAt(i).getUri();
                addImageToPreview(uri);
            }
        } else if (data.getData() != null) {
            // 单选
            android.net.Uri uri = data.getData();
            addImageToPreview(uri);
        }
        
        // 显示图片预览容器
        if (!imagePreviewAdapter.getImages().isEmpty()) {
            currentImagePreviewContainer.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * 添加图片到预览并开始上传
     */
    private void addImageToPreview(android.net.Uri uri) {
        // 创建图片项
        com.forum.mt.ui.adapter.ImagePreviewAdapter.ImageItem imageItem = 
                new com.forum.mt.ui.adapter.ImagePreviewAdapter.ImageItem(uri);
        imageItem.uploading = true;
        
        // 添加到列表
        final int position = imagePreviewAdapter.getImages().size();
        imagePreviewAdapter.addImage(imageItem);
        
        // 获取文件路径并上传
        String filePath = getFilePathFromUri(uri);
        if (filePath != null) {
            uploadImage(filePath, position);
        } else {
            // 无法获取文件路径，使用URI
            uploadImageFromUri(uri, position);
        }
    }
    
    /**
     * 从URI获取文件路径
     */
    private String getFilePathFromUri(android.net.Uri uri) {
        String[] projection = {android.provider.MediaStore.Images.Media.DATA};
        try (android.database.Cursor cursor = getContentResolver().query(uri, projection, null, null, null)) {
            if (cursor != null && cursor.moveToFirst()) {
                int columnIndex = cursor.getColumnIndexOrThrow(android.provider.MediaStore.Images.Media.DATA);
                return cursor.getString(columnIndex);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }
    
    /**
     * 上传图片（使用文件路径）
     */
    private void uploadImage(String filePath, final int position) {
        if (currentPost == null || currentPost.getUploadHash() == null) {
            updateImageUploadFailed(position);
            return;
        }
        
        java.io.File imageFile = new java.io.File(filePath);
        if (!imageFile.exists()) {
            updateImageUploadFailed(position);
            return;
        }
        
        new Thread(() -> {
            try {
                // 统一处理：解码图片后重新编码为JPEG
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeFile(filePath);
                
                if (bitmap == null) {
                    runOnUiThread(() -> {
                        updateImageUploadFailed(position);
                        Toast.makeText(this, "无法读取图片", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                
                // 保存为JPEG格式
                java.io.File tempFile = java.io.File.createTempFile("img_", ".jpg", getCacheDir());
                java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, fos);
                fos.close();
                bitmap.recycle();
                
                // 上传转换后的文件
                HttpClient httpClient = HttpClient.getInstance(PostDetailActivity.this);
                ForumApi forumApi = new ForumApi(httpClient.getOkHttpClient(), httpClient.getCookieManager());
                ApiResponse<UploadResult> response = forumApi.uploadImage(
                        tempFile, currentPost.getUploadUid(), currentPost.getUploadHash());
                
                // 删除临时文件
                tempFile.delete();
                
                runOnUiThread(() -> {
                    if (response.isSuccess() && response.getData() != null && response.getData().isSuccess()) {
                        // 上传成功
                        UploadResult uploadResult = response.getData();
                        imagePreviewAdapter.updateImageStatus(position, false, true, uploadResult.getAid(), uploadResult.getAttachCode());
                    } else {
                        // 上传失败
                        imagePreviewAdapter.updateImageStatus(position, false, false, null, null);
                        String error = response.getMessage();
                        Toast.makeText(this, error != null ? error : "图片上传失败", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> updateImageUploadFailed(position));
            }
        }).start();
    }
    
    /**
     * 上传图片（使用URI）
     */
    private void uploadImageFromUri(android.net.Uri uri, final int position) {
        if (currentPost == null || currentPost.getUploadHash() == null) {
            updateImageUploadFailed(position);
            return;
        }
        
        new Thread(() -> {
            try {
                // 统一处理：解码图片后重新编码为JPEG
                // 这样可以确保上传的文件格式正确（避免WebP等不支持格式被误判）
                android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(
                        getContentResolver().openInputStream(uri));
                
                if (bitmap == null) {
                    runOnUiThread(() -> {
                        updateImageUploadFailed(position);
                        Toast.makeText(this, "无法读取图片", Toast.LENGTH_SHORT).show();
                    });
                    return;
                }
                
                // 保存为JPEG格式
                java.io.File tempFile = java.io.File.createTempFile("img_", ".jpg", getCacheDir());
                java.io.FileOutputStream fos = new java.io.FileOutputStream(tempFile);
                bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, fos);
                fos.close();
                bitmap.recycle();
                
                // 上传
                final java.io.File finalTempFile = tempFile;
                HttpClient httpClient = HttpClient.getInstance(PostDetailActivity.this);
                ForumApi forumApi = new ForumApi(httpClient.getOkHttpClient(), httpClient.getCookieManager());
                ApiResponse<UploadResult> response = forumApi.uploadImage(
                        finalTempFile, currentPost.getUploadUid(), currentPost.getUploadHash());
                
                // 删除临时文件
                finalTempFile.delete();
                
                runOnUiThread(() -> {
                    if (response.isSuccess() && response.getData() != null && response.getData().isSuccess()) {
                        UploadResult uploadResult = response.getData();
                        imagePreviewAdapter.updateImageStatus(position, false, true, uploadResult.getAid(), uploadResult.getAttachCode());
                    } else {
                        imagePreviewAdapter.updateImageStatus(position, false, false, null, null);
                        String error = response.getMessage();
                        Toast.makeText(this, error != null ? error : "图片上传失败", Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                runOnUiThread(() -> updateImageUploadFailed(position));
            }
        }).start();
    }
    
    /**
     * 更新图片上传失败状态
     */
    private void updateImageUploadFailed(int position) {
        if (imagePreviewAdapter != null) {
            imagePreviewAdapter.updateImageStatus(position, false, false, null, null);
        }
        Toast.makeText(this, "图片上传失败", Toast.LENGTH_SHORT).show();
    }
    
    /**
     * 提交回复（带图片）
     */
    private void submitReplyWithImages(String content, BottomSheetDialog dialog, 
                                        TextView publishButton, EditText replyInput) {
        if (currentPost == null) {
            Toast.makeText(this, "帖子信息加载中，请稍后重试", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 禁用发布按钮
        publishButton.setEnabled(false);
        publishButton.setText("发布中...");
        
        // 检查是否有需要上传的图片
        List<Integer> attachIds = null;
        if (imagePreviewAdapter != null && !imagePreviewAdapter.getImages().isEmpty()) {
            attachIds = imagePreviewAdapter.getUploadedAids();
            // 检查是否有上传失败的图片
            if (imagePreviewAdapter.hasFailedImages()) {
                Toast.makeText(this, "部分图片上传失败，请删除后重试", Toast.LENGTH_SHORT).show();
                publishButton.setEnabled(true);
                publishButton.setText("发布");
                return;
            }
        }
        
        // 获取参数
        int fid = currentPost.getForumId();
        int tid = currentTid;
        String formhash = currentPost.getFormhash();
        String noticeauthor = currentPost.getNoticeAuthor();
        
        final List<Integer> finalAttachIds = attachIds;
        
        // 异步执行回复
        new Thread(() -> {
            HttpClient httpClient = HttpClient.getInstance(PostDetailActivity.this);
            ForumApi forumApi = new ForumApi(httpClient.getOkHttpClient(), httpClient.getCookieManager());
            ApiResponse<ForumApi.ReplyResult> response = forumApi.postReply(
                    fid, tid, content, formhash, noticeauthor, finalAttachIds);
            
            runOnUiThread(() -> {
                if (response.isSuccess() && response.getData() != null && response.getData().isSuccess()) {
                    Toast.makeText(PostDetailActivity.this, "回复成功", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    // 清空图片
                    if (imagePreviewAdapter != null) {
                        imagePreviewAdapter.clear();
                    }
                    // 刷新评论列表
                    viewModel.refreshComments();
                    // 重新获取帖子内容
                    viewModel.refreshPostContent();
                } else {
                    String errorMsg = response.getMessage();
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = response.getData() != null ? response.getData().getMessage() : "回复失败";
                    }
                    Toast.makeText(PostDetailActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                    publishButton.setEnabled(true);
                    publishButton.setText("发布");
                }
            });
        }).start();
    }

    private void observeViewModel() {
        viewModel.getUiState().observe(this, this::handleUiState);
        viewModel.getIsLiked().observe(this, this::updateLikeUI);
        viewModel.getIsFavorited().observe(this, this::updateFavoriteUI);
        
        // 观察错误消息
        viewModel.getErrorMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_LONG).show();
            }
        });
        
        // 观察评论加载状态
        viewModel.isLoadingComments().observe(this, isLoading -> {
            binding.loadMoreComments.setVisibility(isLoading ? View.VISIBLE : View.GONE);
        });
        
        // 观察内容更新（回复后检测隐藏内容）
        viewModel.getContentUpdated().observe(this, post -> {
            if (post != null && contentAdapter != null) {
                // 解析新内容并更新
                List<ContentBlock> blocks = ContentParser.parse(post.getContent());
                contentAdapter.setContentBlocks(blocks);
            }
        });
    }

    private void handleUiState(PostDetailUiState state) {
        if (state instanceof PostDetailUiState.Loading) {
            // 如果已经显示了基本信息，保持内容可见，不显示加载指示器（避免干扰动画）
            if (!hasShownBasicInfo) {
                showLoading();
            }
        } else if (state instanceof PostDetailUiState.Success) {
            PostDetailUiState.Success success = (PostDetailUiState.Success) state;
            showPost(success.getPost(), success.getComments());
            // 更新排序按钮文本
            updateSortTabSelection(viewModel.getCurrentOrderType());
        } else if (state instanceof PostDetailUiState.Error) {
            PostDetailUiState.Error error = (PostDetailUiState.Error) state;
            showError(error.getMessage());
        }
    }

    private void showLoading() {
        binding.progressBar.setVisibility(View.VISIBLE);
        binding.contentScrollView.setVisibility(View.GONE);
        binding.errorView.setVisibility(View.GONE);
        binding.bottomBar.setVisibility(View.GONE);
    }

    private void showPost(Post post, List<Comment> comments) {
        // 保存当前帖子数据（用于回复等操作）
        this.currentPost = post;
        
        // 设置楼主ID到评论适配器
        if (post.getAuthorId() > 0) {
            commentAdapter.setPostAuthorId(post.getAuthorId());
        }
        
        // 如果已登录但还没获取当前用户ID，异步获取
        if (currentUserId == 0 && HttpClient.getInstance(this).isLoggedIn()) {
            loadCurrentUserId();
        }
        
        binding.progressBar.setVisibility(View.GONE);
        binding.contentScrollView.setVisibility(View.VISIBLE);
        binding.errorView.setVisibility(View.GONE);
        binding.bottomBar.setVisibility(View.VISIBLE);

        // 保存到浏览历史
        browseHistoryViewModel.addOrUpdateHistory(post);

        // 如果已经显示了基本信息，只更新其他内容
        if (hasShownBasicInfo) {
            // 标题栏显示版块名
            binding.toolbarTitle.setText(safeString(post.getForumName(), "帖子详情"));
            
            // 更新时间
            binding.postTime.setText(safeString(post.getDateStr(), ""));
            
            // 更新作者信息（详情页会返回完整的作者数据）
            if (post.getAuthor() != null && !post.getAuthor().isEmpty()) {
                binding.authorName.setText(post.getAuthor());
            }
            
            // 更新作者头像
            String authorAvatar = post.getAuthorAvatar();
            if (authorAvatar != null && !authorAvatar.isEmpty()) {
                Glide.with(this)
                        .load(authorAvatar)
                        .placeholder(R.drawable.ic_forum)
                        .error(R.drawable.ic_forum)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(binding.authorAvatar);
            }
            
            // 更新等级标签
            String authorLevel = post.getAuthorLevel();
            if (authorLevel != null && !authorLevel.isEmpty()) {
                binding.authorLevel.setText(authorLevel);
                binding.authorLevel.setVisibility(View.VISIBLE);
                setLevelBackground(binding.authorLevel, authorLevel);
            }
        } else {
            // 设置标题
            binding.toolbarTitle.setText(safeString(post.getForumName(), "帖子详情"));
            binding.postTitle.setText(safeString(post.getTitle(), "无标题"));

            // 作者信息
            binding.authorName.setText(safeString(post.getAuthor(), "匿名用户"));

            String authorAvatar = post.getAuthorAvatar();
            if (authorAvatar != null && !authorAvatar.isEmpty()) {
                Glide.with(this)
                        .load(authorAvatar)
                        .placeholder(R.drawable.ic_forum)
                        .error(R.drawable.ic_forum)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(binding.authorAvatar);
            } else {
                binding.authorAvatar.setImageResource(R.drawable.ic_forum);
            }

            // 等级标签
            String authorLevel = post.getAuthorLevel();
            if (authorLevel != null && !authorLevel.isEmpty()) {
                binding.authorLevel.setText(authorLevel);
                binding.authorLevel.setVisibility(View.VISIBLE);
                setLevelBackground(binding.authorLevel, authorLevel);
            } else {
                binding.authorLevel.setVisibility(View.GONE);
            }

            // 时间
            binding.postTime.setText(safeString(post.getDateStr(), ""));
        }

        // 来源地点
        String location = post.getLocation();
        if (location != null && !location.isEmpty()) {
            binding.postLocation.setText("来自 " + location);
            binding.postLocation.setVisibility(View.VISIBLE);
        } else {
            binding.postLocation.setVisibility(View.GONE);
        }

        // 编辑信息
        String editTime = post.getEditTime();
        if (editTime != null && !editTime.isEmpty()) {
            binding.editInfo.setText("最后编辑于 " + editTime);
            binding.editInfo.setVisibility(View.VISIBLE);
        } else {
            binding.editInfo.setVisibility(View.GONE);
        }

        // 帖子内容 - 使用原生组件
        String content = post.getContent();
        if (content != null && !content.trim().isEmpty()) {
            List<ContentBlock> blocks = ContentParser.parse(content);
            contentAdapter.setContentBlocks(blocks);
        } else {
            List<ContentBlock> blocks = new ArrayList<>();
            blocks.add(ContentBlock.createTextBlock("暂无内容"));
            contentAdapter.setContentBlocks(blocks);
        }

        // 点赞数
        binding.likeCount.setText(String.valueOf(post.getLikes() > 0 ? post.getLikes() : 0));

        // 评论总数
        int commentTotal = post.getCommentTotal() > 0 ? post.getCommentTotal() : post.getReplies();
        binding.commentCount.setText("(" + commentTotal + ")");

        // 关注/编辑按钮状态（传入作者ID用于判断是否为自己的帖子）
        updateFollowButton(post.isFollowed(), post.getAuthorId());

        // 赞赏按钮
        if (post.isCanRate()) {
            binding.rewardButton.setVisibility(View.VISIBLE);
            binding.rewardButton.setOnClickListener(v -> showRewardDialogForPost(post));
        } else {
            binding.rewardButton.setVisibility(View.GONE);
        }
        
        // 踢帖按钮 - 显示踢帖人数
        updateKickPostButton(post);

        // 点赞用户头像列表
        List<Post.LikeUser> likeUsers = post.getLikeUsers();
        if (likeUsers != null && !likeUsers.isEmpty()) {
            binding.likeUsersContainer.setVisibility(View.VISIBLE);
            binding.likeUsersAvatars.removeAllViews();

            int maxAvatars = Math.min(likeUsers.size(), 5);
            for (int i = 0; i < maxAvatars; i++) {
                Post.LikeUser user = likeUsers.get(i);
                de.hdodenhof.circleimageview.CircleImageView avatarView =
                        new de.hdodenhof.circleimageview.CircleImageView(this);
                int size = (int) (24 * getResources().getDisplayMetrics().density);
                LinearLayout.LayoutParams params = new LinearLayout.LayoutParams(size, size);
                if (i > 0) {
                    params.setMarginStart((int) (-8 * getResources().getDisplayMetrics().density));
                }
                avatarView.setLayoutParams(params);
                avatarView.setBorderColor(Color.WHITE);
                avatarView.setBorderWidth(2);

                Glide.with(this)
                        .load(user.getAvatar())
                        .placeholder(R.drawable.ic_forum)
                        .error(R.drawable.ic_forum)
                        .into(avatarView);

                binding.likeUsersAvatars.addView(avatarView);
            }

            if (likeUsers.size() > 5) {
                binding.likeUsersMore.setVisibility(View.VISIBLE);
                binding.likeUsersMore.setText("等" + likeUsers.size() + "人");
            } else {
                binding.likeUsersMore.setVisibility(View.GONE);
            }
        } else {
            binding.likeUsersContainer.setVisibility(View.GONE);
        }

        // 评论列表
        if (comments == null || comments.isEmpty()) {
            // 判断是否是"只看楼主"模式
            if (viewModel != null && viewModel.getCurrentOrderType() == ORDER_AUTHOR) {
                // 只看楼主模式下，显示"楼主暂无回复"
                binding.emptyCommentTip.setVisibility(View.VISIBLE);
                binding.emptyCommentView.setVisibility(View.GONE);
            } else {
                binding.emptyCommentTip.setVisibility(View.GONE);
                binding.emptyCommentView.setVisibility(View.VISIBLE);
            }
            binding.commentRecyclerView.setVisibility(View.GONE);
        } else {
            binding.emptyCommentTip.setVisibility(View.GONE);
            binding.emptyCommentView.setVisibility(View.GONE);
            binding.commentRecyclerView.setVisibility(View.VISIBLE);
            commentAdapter.submitList(comments);
        }

        // 更新分页控件
        updatePagination();
        
        // 设置作者头像点击监听（跳转到用户详情页）
        setupAuthorClickListener();
    }

    /**
     * 设置作者头像/名称点击监听，跳转到用户详情页
     */
    private void setupAuthorClickListener() {
        if (currentPost == null || currentPost.getAuthorId() <= 0) return;
        
        // 头像点击 - 带共享元素动画
        binding.authorAvatar.setOnClickListener(v -> {
            UserDetailActivity.start(
                PostDetailActivity.this,
                currentPost.getAuthorId(),
                currentPost.getAuthor(),
                currentPost.getAuthorAvatar(),
                binding.authorAvatar
            );
        });
        
        // 用户名点击 - 也传递头像视图用于共享元素动画
        binding.authorName.setOnClickListener(v -> {
            UserDetailActivity.start(
                PostDetailActivity.this,
                currentPost.getAuthorId(),
                currentPost.getAuthor(),
                currentPost.getAuthorAvatar(),
                binding.authorAvatar
            );
        });
    }

    private void showError(String message) {
        binding.progressBar.setVisibility(View.GONE);
        binding.contentScrollView.setVisibility(View.GONE);
        binding.errorView.setVisibility(View.VISIBLE);
        binding.bottomBar.setVisibility(View.GONE);
        binding.errorText.setText(message != null ? message : "加载失败");
    }

    private void updateLikeUI(Boolean isLiked) {
        if (isLiked == null) return;

        int iconRes = isLiked ? R.drawable.ic_thumb_up_filled : R.drawable.ic_thumb_up_outline;
        int color = isLiked ? Color.parseColor("#FF6B4A") : Color.parseColor("#999999");

        // 更新内容区域的点赞图标
        binding.likeIcon.setImageResource(iconRes);
        binding.likeIcon.setColorFilter(color);

        // 更新底部栏的点赞图标
        binding.bottomLikeIcon.setImageResource(iconRes);
        binding.bottomLikeIcon.setColorFilter(color);

        // Toast 通过 viewModel.getErrorMessage() 统一处理
        isUserLikeAction = false;
    }

    private void updateFavoriteUI(Boolean isFavorited) {
        if (isFavorited == null) return;

        int iconRes = isFavorited ? R.drawable.ic_star_filled : R.drawable.ic_star_outline;
        int color = isFavorited ? Color.parseColor("#FF6B4A") : Color.parseColor("#999999");

        // 更新底部栏的收藏图标
        binding.bottomFavoriteIcon.setImageResource(iconRes);
        binding.bottomFavoriteIcon.setColorFilter(color);

        // 只在用户主动操作时显示Toast
        if (isUserFavoriteAction) {
            Toast.makeText(this, isFavorited ? "已收藏" : "取消收藏", Toast.LENGTH_SHORT).show();
            isUserFavoriteAction = false;
        }
    }

    /**
     * 更新关注/编辑按钮状态
     * 如果是自己的帖子，显示"编辑"按钮
     * 否则显示"关注/已关注"按钮
     */
    private void updateFollowButton(boolean isFollowed) {
        updateFollowButton(isFollowed, 0);
    }
    
    /**
     * 更新关注/编辑按钮状态
     * @param isFollowed 是否已关注
     * @param authorId 帖子作者ID
     */
    private void updateFollowButton(boolean isFollowed, int authorId) {
        // 判断是否为自己的帖子
        if (currentUserId > 0 && authorId > 0 && currentUserId == authorId) {
            // 是自己的帖子，显示编辑按钮
            binding.followButton.setText("编辑");
            binding.followButton.setTextColor(Color.parseColor("#FF6B4A"));
            binding.followButton.setBackgroundResource(R.drawable.bg_follow_button);
            // 移除之前的点击监听，设置编辑点击监听
            binding.followButton.setOnClickListener(v -> {
                startEditPost();
            });
        } else {
            // 不是自己的帖子，显示关注按钮
            if (isFollowed) {
                binding.followButton.setText("已关注");
                binding.followButton.setTextColor(Color.parseColor("#999999"));
                binding.followButton.setBackgroundResource(R.drawable.bg_followed_button);
            } else {
                binding.followButton.setText("关注");
                binding.followButton.setTextColor(Color.parseColor("#FF6B4A"));
                binding.followButton.setBackgroundResource(R.drawable.bg_follow_button);
            }
            // 设置关注点击监听
            binding.followButton.setOnClickListener(v -> {
                if (!viewModel.isLoggedIn()) {
                    Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
                    return;
                }
                viewModel.toggleFollow();
            });
        }
    }
    
    /**
     * 启动编辑帖子页面
     */
    private void startEditPost() {
        if (currentPost == null) {
            Toast.makeText(this, "帖子信息加载中", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int fid = currentPost.getForumId();
        int pid = currentPost.getFirstPid();
        String title = currentPost.getTitle();
        String content = currentPost.getContent();
        
        // 调试日志
        android.util.Log.d("PostDetailActivity", "Edit post params: tid=" + currentTid + ", fid=" + fid + ", pid=" + pid);
        
        if (fid == 0) {
            Toast.makeText(this, "无法获取版块ID，请刷新页面后重试", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (pid == 0) {
            Toast.makeText(this, "无法获取帖子PID，请刷新页面后重试", Toast.LENGTH_SHORT).show();
            return;
        }
        
        EditPostActivity.start(this, currentTid, fid, pid, title, content);
    }

    private String safeString(String value, String defaultValue) {
        return value != null && !value.isEmpty() ? value : defaultValue;
    }

    /**
     * 更新排序标签选中状态
     */
    private void updateSortTabSelection(int orderType) {
        binding.sortTabLatest.setSelected(orderType == ORDER_LATEST);
        binding.sortTabEarliest.setSelected(orderType == ORDER_EARLIEST);
        binding.sortTabAuthor.setSelected(orderType == ORDER_AUTHOR);
        
        // 更新字体样式
        binding.sortTabLatest.setTypeface(null, orderType == ORDER_LATEST ? Typeface.BOLD : Typeface.NORMAL);
        binding.sortTabEarliest.setTypeface(null, orderType == ORDER_EARLIEST ? Typeface.BOLD : Typeface.NORMAL);
        binding.sortTabAuthor.setTypeface(null, orderType == ORDER_AUTHOR ? Typeface.BOLD : Typeface.NORMAL);
    }

    /**
     * 更新分页控件
     */
    private void updatePagination() {
        int currentPage = viewModel.getCurrentPage();
        int totalPages = viewModel.getTotalPages();
        List<Comment> comments = viewModel.getAllComments();

        // 评论为空或只有一页时隐藏分页
        if (totalPages <= 1 || comments == null || comments.isEmpty()) {
            binding.commentPagination.setVisibility(View.GONE);
            return;
        }

        binding.commentPagination.setVisibility(View.VISIBLE);
        binding.currentPageText.setText(String.valueOf(currentPage));
        binding.totalPageText.setText(String.valueOf(totalPages));

        // 上一页按钮状态
        boolean canGoPrev = currentPage > 1;
        binding.prevPageBtn.setEnabled(canGoPrev);
        binding.prevPageBtn.setAlpha(canGoPrev ? 1.0f : 0.4f);
        binding.prevPageBtn.setClickable(canGoPrev);

        // 下一页按钮状态
        boolean canGoNext = currentPage < totalPages;
        binding.nextPageBtn.setEnabled(canGoNext);
        binding.nextPageBtn.setAlpha(canGoNext ? 1.0f : 0.4f);
        binding.nextPageBtn.setClickable(canGoNext);
    }

    /**
     * 设置分页点击事件
     */
    private void setupPaginationListeners() {
        binding.prevPageBtn.setOnClickListener(v -> {
            int currentPage = viewModel.getCurrentPage();
            if (currentPage > 1) {
                viewModel.goToPage(currentPage - 1);
            }
        });

        binding.nextPageBtn.setOnClickListener(v -> {
            int currentPage = viewModel.getCurrentPage();
            if (currentPage < viewModel.getTotalPages()) {
                viewModel.goToPage(currentPage + 1);
            }
        });

        binding.pageIndicator.setOnClickListener(v -> showPageSelector());
    }

    private PopupWindow pageSelectorPopup;

    private void showPageSelector() {
        if (pageSelectorPopup != null && pageSelectorPopup.isShowing()) {
            pageSelectorPopup.dismiss();
            return;
        }

        int totalPages = viewModel.getTotalPages();
        int currentPage = viewModel.getCurrentPage();

        // 创建PopupWindow内容
        View contentView = LayoutInflater.from(this).inflate(R.layout.popup_page_selector, null);
        NumberPicker numberPicker = contentView.findViewById(R.id.pageNumberPicker);
        View cancelBtn = contentView.findViewById(R.id.pageSelectorCancel);
        View confirmBtn = contentView.findViewById(R.id.pageSelectorConfirm);

        numberPicker.setMinValue(1);
        numberPicker.setMaxValue(totalPages);
        numberPicker.setValue(currentPage);
        numberPicker.setWrapSelectorWheel(false);

        // 设置NumberPicker样式
        numberPicker.setDescendantFocusability(NumberPicker.FOCUS_BLOCK_DESCENDANTS);
        try {
            java.lang.reflect.Field selectionDivider = NumberPicker.class.getDeclaredField("mSelectionDivider");
            selectionDivider.setAccessible(true);
            selectionDivider.set(numberPicker, new android.graphics.drawable.ColorDrawable(Color.parseColor("#EEEEEE")));
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
            viewModel.goToPage(selectedPage);
        });

        // 创建PopupWindow
        pageSelectorPopup = new PopupWindow(contentView,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                ViewGroup.LayoutParams.WRAP_CONTENT,
                true); // focusable=true，点击外部自动关闭

        pageSelectorPopup.setOutsideTouchable(true);
        pageSelectorPopup.setElevation(8f);
        pageSelectorPopup.setAnimationStyle(android.R.style.Animation_Dialog);
        pageSelectorPopup.setBackgroundDrawable(new android.graphics.drawable.ColorDrawable(Color.TRANSPARENT));

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

        pageSelectorPopup.showAtLocation(binding.getRoot(), android.view.Gravity.NO_GRAVITY, x, y);
    }
    
    /**
     * 加载当前登录用户ID（用于标识"我的评论"）
     */
    private void loadCurrentUserId() {
        new Thread(() -> {
            try {
                HttpClient httpClient = HttpClient.getInstance(this);
                ForumApi forumApi = new ForumApi(httpClient.getOkHttpClient(), httpClient.getCookieManager());
                ApiResponse<User> response = forumApi.getUserProfile();
                
                if (response.isSuccess() && response.getData() != null) {
                    currentUserId = response.getData().getUid();
                    runOnUiThread(() -> {
                        commentAdapter.setCurrentUserId(currentUserId);
                    });
                }
            } catch (Exception e) {
                // ignore
            }
        }).start();
    }
    
    /**
     * 购买付费帖子
     */
    private void buyPaidPost() {
        // 检查登录状态
        if (!HttpClient.getInstance(this).isLoggedIn()) {
            Toast.makeText(this, "请先登录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (currentTid == 0) {
            Toast.makeText(this, "帖子信息无效", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 显示确认对话框
        new androidx.appcompat.app.AlertDialog.Builder(this)
            .setTitle("购买主题")
            .setMessage("确认购买此付费内容吗？")
            .setPositiveButton("确认购买", (dialog, which) -> {
                performBuyPost();
            })
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 执行购买帖子
     */
    private void performBuyPost() {
        // 显示加载提示
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("购买中...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        new Thread(() -> {
            HttpClient httpClient = HttpClient.getInstance(PostDetailActivity.this);
            ForumApi forumApi = new ForumApi(httpClient.getOkHttpClient(), httpClient.getCookieManager());
            ApiResponse<ForumApi.BuyPostResult> response = forumApi.buyPost(currentTid);
            
            runOnUiThread(() -> {
                progressDialog.dismiss();
                
                if (response.isSuccess() && response.getData() != null && response.getData().isSuccess()) {
                    Toast.makeText(PostDetailActivity.this, 
                        response.getData().getMessage() != null ? response.getData().getMessage() : "购买成功", 
                        Toast.LENGTH_SHORT).show();
                    // 重新加载帖子内容
                    viewModel.loadPostDetail(currentTid);
                } else {
                    String errorMsg = response.getMessage();
                    if (errorMsg == null || errorMsg.isEmpty()) {
                        errorMsg = response.getData() != null ? response.getData().getMessage() : "购买失败";
                    }
                    Toast.makeText(PostDetailActivity.this, errorMsg, Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }
    
    /**
     * 显示打赏楼主对话框
     */
    private void showRewardDialogForPost(Post post) {
        if (post == null) {
            Toast.makeText(this, "帖子信息加载中，请稍后重试", Toast.LENGTH_SHORT).show();
            return;
        }
        
        int firstPid = post.getFirstPid();
        if (firstPid == 0) {
            Toast.makeText(this, "无法获取帖子信息", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 使用BottomSheetDialog显示打赏选项
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet = 
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.dialog_reward, null);
        bottomSheet.setContentView(sheetView);
        
        // 获取视图
        de.hdodenhof.circleimageview.CircleImageView avatarImage = sheetView.findViewById(R.id.avatarImage);
        TextView usernameText = sheetView.findViewById(R.id.usernameText);
        ImageView btnClose = sheetView.findViewById(R.id.btnClose);
        ImageView btnScore1Minus = sheetView.findViewById(R.id.btnScore1Minus);
        ImageView btnScore1Plus = sheetView.findViewById(R.id.btnScore1Plus);
        TextView score1Text = sheetView.findViewById(R.id.score1Text);
        ImageView btnScore2Minus = sheetView.findViewById(R.id.btnScore2Minus);
        ImageView btnScore2Plus = sheetView.findViewById(R.id.btnScore2Plus);
        TextView score2Text = sheetView.findViewById(R.id.score2Text);
        android.widget.EditText reasonInput = sheetView.findViewById(R.id.reasonInput);
        com.google.android.material.button.MaterialButton btnSubmit = sheetView.findViewById(R.id.btnSubmit);
        
        // 设置楼主信息
        if (post.getAuthorAvatar() != null && !post.getAuthorAvatar().isEmpty()) {
            Glide.with(this)
                    .load(post.getAuthorAvatar())
                    .placeholder(R.drawable.ic_forum)
                    .error(R.drawable.ic_forum)
                    .into(avatarImage);
        }
        usernameText.setText(post.getAuthor());
        
        // 打赏数值
        final int[] score1 = {1};  // 好评，默认+1
        final int[] score2 = {1};  // 金币，默认+1
        
        // 更新显示
        score1Text.setText("+" + score1[0]);
        score2Text.setText("+" + score2[0]);
        
        // 好评加减按钮
        btnScore1Minus.setOnClickListener(v -> {
            if (score1[0] > 0) {
                score1[0]--;
                score1Text.setText("+" + score1[0]);
            }
        });
        btnScore1Plus.setOnClickListener(v -> {
            if (score1[0] < 1) {
                score1[0]++;
                score1Text.setText("+" + score1[0]);
            }
        });
        
        // 金币加减按钮
        btnScore2Minus.setOnClickListener(v -> {
            if (score2[0] > 0) {
                score2[0]--;
                score2Text.setText("+" + score2[0]);
            }
        });
        btnScore2Plus.setOnClickListener(v -> {
            if (score2[0] < 1) {
                score2[0]++;
                score2Text.setText("+" + score2[0]);
            }
        });
        
        // 关闭按钮
        btnClose.setOnClickListener(v -> bottomSheet.dismiss());
        
        // 提交按钮
        btnSubmit.setOnClickListener(v -> {
            if (score1[0] == 0 && score2[0] == 0) {
                Toast.makeText(this, "请选择打赏金额", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String reason = reasonInput.getText().toString().trim();
            bottomSheet.dismiss();
            performRewardForPost(post, score1[0], score2[0], reason);
        });
        
        bottomSheet.show();
    }
    
    /**
     * 执行打赏楼主
     */
    private void performRewardForPost(Post post, int score1, int score2, String reason) {
        // 显示加载提示
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("打赏中...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        int firstPid = post.getFirstPid();
        int tid = post.getTid();
        
        new Thread(() -> {
            HttpClient httpClient = HttpClient.getInstance(PostDetailActivity.this);
            ForumApi forumApi = new ForumApi(httpClient.getOkHttpClient(), httpClient.getCookieManager());
            ForumApi.RewardResult result = forumApi.rateComment(tid, firstPid, score1, score2, reason);
            
            runOnUiThread(() -> {
                progressDialog.dismiss();
                
                if (result.isSuccess()) {
                    Toast.makeText(PostDetailActivity.this, 
                        result.getMessage() != null ? result.getMessage() : "打赏成功", 
                        Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PostDetailActivity.this, 
                        result.getMessage() != null ? result.getMessage() : "打赏失败", 
                        Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    /**
     * 显示打赏对话框
     */
    private void showRewardDialog(Comment comment) {
        if (currentPost == null) {
            Toast.makeText(this, "帖子信息加载中，请稍后重试", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 使用BottomSheetDialog显示打赏选项
        com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet = 
                new com.google.android.material.bottomsheet.BottomSheetDialog(this);
        View sheetView = LayoutInflater.from(this).inflate(R.layout.dialog_reward, null);
        bottomSheet.setContentView(sheetView);
        
        // 获取视图
        de.hdodenhof.circleimageview.CircleImageView avatarImage = sheetView.findViewById(R.id.avatarImage);
        TextView usernameText = sheetView.findViewById(R.id.usernameText);
        ImageView btnClose = sheetView.findViewById(R.id.btnClose);
        ImageView btnScore1Minus = sheetView.findViewById(R.id.btnScore1Minus);
        ImageView btnScore1Plus = sheetView.findViewById(R.id.btnScore1Plus);
        TextView score1Text = sheetView.findViewById(R.id.score1Text);
        ImageView btnScore2Minus = sheetView.findViewById(R.id.btnScore2Minus);
        ImageView btnScore2Plus = sheetView.findViewById(R.id.btnScore2Plus);
        TextView score2Text = sheetView.findViewById(R.id.score2Text);
        android.widget.EditText reasonInput = sheetView.findViewById(R.id.reasonInput);
        com.google.android.material.button.MaterialButton btnSubmit = sheetView.findViewById(R.id.btnSubmit);
        
        // 设置用户信息
        if (comment.getAuthorAvatar() != null && !comment.getAuthorAvatar().isEmpty()) {
            Glide.with(this)
                    .load(comment.getAuthorAvatar())
                    .placeholder(R.drawable.ic_forum)
                    .error(R.drawable.ic_forum)
                    .into(avatarImage);
        }
        usernameText.setText(comment.getAuthor());
        
        // 打赏数值
        final int[] score1 = {1};  // 好评，默认+1
        final int[] score2 = {1};  // 金币，默认+1
        
        // 更新显示
        score1Text.setText("+" + score1[0]);
        score2Text.setText("+" + score2[0]);
        
        // 好评加减按钮
        btnScore1Minus.setOnClickListener(v -> {
            if (score1[0] > 0) {
                score1[0]--;
                score1Text.setText("+" + score1[0]);
            }
        });
        btnScore1Plus.setOnClickListener(v -> {
            if (score1[0] < 1) {
                score1[0]++;
                score1Text.setText("+" + score1[0]);
            }
        });
        
        // 金币加减按钮
        btnScore2Minus.setOnClickListener(v -> {
            if (score2[0] > 0) {
                score2[0]--;
                score2Text.setText("+" + score2[0]);
            }
        });
        btnScore2Plus.setOnClickListener(v -> {
            if (score2[0] < 1) {
                score2[0]++;
                score2Text.setText("+" + score2[0]);
            }
        });
        
        // 关闭按钮
        btnClose.setOnClickListener(v -> bottomSheet.dismiss());
        
        // 提交按钮
        btnSubmit.setOnClickListener(v -> {
            if (score1[0] == 0 && score2[0] == 0) {
                Toast.makeText(this, "请选择打赏金额", Toast.LENGTH_SHORT).show();
                return;
            }
            
            String reason = reasonInput.getText().toString().trim();
            bottomSheet.dismiss();
            performReward(comment, score1[0], score2[0], reason);
        });
        
        bottomSheet.show();
    }
    
    /**
     * 执行打赏
     */
    private void performReward(Comment comment, int score1, int score2, String reason) {
        // 显示加载提示
        android.app.ProgressDialog progressDialog = new android.app.ProgressDialog(this);
        progressDialog.setMessage("打赏中...");
        progressDialog.setCancelable(false);
        progressDialog.show();
        
        new Thread(() -> {
            HttpClient httpClient = HttpClient.getInstance(PostDetailActivity.this);
            ForumApi forumApi = new ForumApi(httpClient.getOkHttpClient(), httpClient.getCookieManager());
            ForumApi.RewardResult result = forumApi.rateComment(currentTid, comment.getId(), score1, score2, reason);
            
            runOnUiThread(() -> {
                progressDialog.dismiss();
                
                if (result.isSuccess()) {
                    Toast.makeText(PostDetailActivity.this, 
                        result.getMessage() != null ? result.getMessage() : "打赏成功", 
                        Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(PostDetailActivity.this, 
                        result.getMessage() != null ? result.getMessage() : "打赏失败", 
                        Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void setLevelBackground(android.widget.TextView levelView, String level) {
        int color;
        try {
            String levelNum = level.replace("Lv.", "").trim().split(" ")[0];
            int num = Integer.parseInt(levelNum);
            if (num <= 2) {
                color = Color.parseColor("#9E9E9E");
            } else if (num <= 4) {
                color = Color.parseColor("#4CAF50");
            } else if (num <= 6) {
                color = Color.parseColor("#2196F3");
            } else if (num <= 8) {
                color = Color.parseColor("#9C27B0");
            } else {
                color = Color.parseColor("#FF9800");
            }
        } catch (Exception e) {
            color = Color.parseColor("#9E9E9E");
        }

        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(8f);
        drawable.setColor(color);
        levelView.setBackground(drawable);
    }
}
