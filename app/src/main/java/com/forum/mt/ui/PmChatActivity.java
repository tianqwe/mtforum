package com.forum.mt.ui;

import android.content.Context;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.view.WindowManager;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.view.WindowCompat;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.forum.mt.R;
import com.forum.mt.api.ForumApi;
import com.forum.mt.api.HttpClient;
import com.forum.mt.databinding.ActivityPmChatBinding;
import com.forum.mt.model.ApiResponse;
import com.forum.mt.model.PrivateMessage;
import com.forum.mt.model.User;
import com.forum.mt.ui.adapter.PmChatAdapter;

import java.util.ArrayList;
import java.util.List;

/**
 * 私信对话页面
 * 支持实时轮询更新消息（每10秒检查一次）
 */
public class PmChatActivity extends AppCompatActivity {

    public static final String EXTRA_TO_UID = "toUid";
    public static final String EXTRA_USERNAME = "username";
    public static final String EXTRA_AVATAR = "avatar";

    private ActivityPmChatBinding binding;
    private ForumApi forumApi;
    private PmChatAdapter adapter;
    private LinearLayoutManager layoutManager;

    private int toUid;
    private String username;
    private String avatar;
    private int currentUid;
    private User otherUser;

    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private List<PrivateMessage> allMessages = new ArrayList<>();

    private static final int MAX_MESSAGE_LENGTH = 500;
    
    // 实时轮询相关
    private static final long POLLING_INTERVAL = 10000; // 10秒轮询间隔
    private Handler pollingHandler;
    private boolean isPolling = false;
    private long lastMessageTime = 0; // 用于增量获取的时间戳
    
    // 私信会话ID（用于增量获取）
    private int pmId = 0;

    public static void start(Context context, int toUid) {
        Intent intent = new Intent(context, PmChatActivity.class);
        intent.putExtra(EXTRA_TO_UID, toUid);
        context.startActivity(intent);
    }

    public static void start(Context context, int toUid, String username, String avatar) {
        Intent intent = new Intent(context, PmChatActivity.class);
        intent.putExtra(EXTRA_TO_UID, toUid);
        intent.putExtra(EXTRA_USERNAME, username);
        intent.putExtra(EXTRA_AVATAR, avatar);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        WindowCompat.setDecorFitsSystemWindows(getWindow(), false);

        binding = ActivityPmChatBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());

        // 设置状态栏颜色
        getWindow().setStatusBarColor(getResources().getColor(R.color.surface));
        
        // 根据当前主题设置状态栏图标颜色
        int nightModeFlags = getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK;
        if (nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES) {
            // 深色模式 - 状态栏图标使用浅色
            getWindow().getDecorView().setSystemUiVisibility(0);
        } else {
            // 浅色模式 - 状态栏图标使用深色
            getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
        }

        // 获取传入参数
        toUid = getIntent().getIntExtra(EXTRA_TO_UID, 0);
        username = getIntent().getStringExtra(EXTRA_USERNAME);
        avatar = getIntent().getStringExtra(EXTRA_AVATAR);

        if (toUid == 0) {
            Toast.makeText(this, "用户ID无效", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        HttpClient httpClient = HttpClient.getInstance(this);
        forumApi = new ForumApi(httpClient.getOkHttpClient(), httpClient.getCookieManager());

        setupViews();
        setupRecyclerView();
        setupInput();

        // 显示用户信息
        showUserInfo();

        // 加载对话
        loadConversation();

        // 获取当前用户ID
        getCurrentUserInfo();
        
        // 初始化轮询Handler
        pollingHandler = new Handler(Looper.getMainLooper());
    }
    
    @Override
    protected void onResume() {
        super.onResume();
        // 开始轮询
        startPolling();
    }
    
    @Override
    protected void onPause() {
        super.onPause();
        // 停止轮询
        stopPolling();
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        // 确保停止轮询
        stopPolling();
    }
    
    /**
     * 开始轮询新消息
     */
    private void startPolling() {
        if (isPolling) return;
        isPolling = true;
        
        // 延迟一段时间后开始轮询（等待初始加载完成）
        pollingHandler.postDelayed(pollingRunnable, POLLING_INTERVAL);
    }
    
    /**
     * 停止轮询
     */
    private void stopPolling() {
        isPolling = false;
        pollingHandler.removeCallbacks(pollingRunnable);
    }
    
    /**
     * 轮询Runnable
     */
    private final Runnable pollingRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isPolling) return;
            
            // 检查新消息
            checkNewMessages();
            
            // 继续下一次轮询
            pollingHandler.postDelayed(this, POLLING_INTERVAL);
        }
    };
    
    /**
     * 检查新消息（增量获取）
     */
    private void checkNewMessages() {
        if (toUid == 0 || isLoading) return;
        
        new Thread(() -> {
            // 使用增量获取API
            ApiResponse<List<PrivateMessage>> response = forumApi.getNewPmMessages(toUid, pmId, lastMessageTime);
            
            if (response.isSuccess() && response.getData() != null && !response.getData().isEmpty()) {
                List<PrivateMessage> newMessages = response.getData();
                
                runOnUiThread(() -> {
                    // 添加新消息到列表末尾
                    for (PrivateMessage pm : newMessages) {
                        // 避免重复消息
                        if (!containsMessage(pm)) {
                            allMessages.add(pm);
                        }
                    }
                    
                    // 更新适配器
                    adapter.submitList(new ArrayList<>(allMessages));
                    
                    // 滚动到底部
                    if (!newMessages.isEmpty()) {
                        binding.messageList.post(() ->
                            binding.messageList.scrollToPosition(allMessages.size() - 1));
                    }
                    
                    // 更新最后消息时间戳
                    updateLastMessageTime();
                });
            }
        }).start();
    }
    
    /**
     * 检查消息是否已存在
     */
    private boolean containsMessage(PrivateMessage pm) {
        if (pm.getMessage() == null) return true;
        for (PrivateMessage existing : allMessages) {
            if (pm.getMessage().equals(existing.getMessage()) && 
                pm.getDateline() != null && pm.getDateline().equals(existing.getDateline())) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * 更新最后消息时间戳
     */
    private void updateLastMessageTime() {
        lastMessageTime = System.currentTimeMillis() / 1000;
    }

    private void setupViews() {
        binding.btnBack.setOnClickListener(v -> finish());
        binding.btnUserInfo.setOnClickListener(v -> {
            UserDetailActivity.start(this, toUid, username, avatar);
        });
    }

    private void setupRecyclerView() {
        adapter = new PmChatAdapter();
        layoutManager = new LinearLayoutManager(this);
        layoutManager.setStackFromEnd(true); // 从底部开始显示

        binding.messageList.setLayoutManager(layoutManager);
        binding.messageList.setAdapter(adapter);

        // 滚动到顶部加载更多
        binding.messageList.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                int firstVisiblePosition = layoutManager.findFirstVisibleItemPosition();
                if (firstVisiblePosition <= 2 && !isLoading && hasMore) {
                    loadMoreMessages();
                }
            }
        });
    }

    private void setupInput() {
        // 设置输入框文本变化监听
        binding.messageInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateSendButtonState(s.length());
            }
        });

        // 表情按钮
        binding.btnEmoji.setOnClickListener(v -> toggleEmojiPanel());

        // 发送按钮
        binding.btnSend.setOnClickListener(v -> sendMessage());
        
        // 初始化发送按钮状态
        updateSendButtonState(0);
    }

    /**
     * 更新发送按钮状态
     */
    private void updateSendButtonState(int length) {
        boolean hasText = length > 0;
        binding.btnSend.setEnabled(hasText);
        if (hasText) {
            binding.btnSend.setBackgroundResource(R.drawable.bg_send_button);
        } else {
            binding.btnSend.setBackgroundResource(R.drawable.bg_send_button_disabled);
        }
    }

    private void showUserInfo() {
        if (username != null && !username.isEmpty()) {
            binding.userName.setText(username);
        }
        if (avatar != null && !avatar.isEmpty()) {
            Glide.with(this)
                    .load(avatar)
                    .placeholder(R.drawable.ic_user)
                    .error(R.drawable.ic_user)
                    .circleCrop()
                    .into(binding.userAvatar);
        }

        // 设置otherUser用于适配器显示头像
        otherUser = new User();
        otherUser.setUid(toUid);
        otherUser.setUsername(username);
        otherUser.setAvatar(avatar);
        adapter.setOtherUser(otherUser);
    }

    private void getCurrentUserInfo() {
        new Thread(() -> {
            ApiResponse<User> response = forumApi.getUserProfile();
            if (response.isSuccess() && response.getData() != null) {
                currentUid = response.getData().getUid();
                runOnUiThread(() -> adapter.setCurrentUid(currentUid));
            }
        }).start();
    }

    private void loadConversation() {
        showLoading(true);

        new Thread(() -> {
            ApiResponse<ForumApi.PmDetailResult> response = forumApi.getPmChat(toUid, currentPage);

            runOnUiThread(() -> {
                showLoading(false);

                if (response.isSuccess() && response.getData() != null) {
                    ForumApi.PmDetailResult result = response.getData();
                    allMessages.clear();
                    allMessages.addAll(result.getMessages());
                    adapter.submitList(new ArrayList<>(allMessages));

                    // 滚动到底部
                    if (!allMessages.isEmpty()) {
                        binding.messageList.post(() ->
                            binding.messageList.scrollToPosition(allMessages.size() - 1));
                    }

                    // 更新用户信息
                    if (result.getOtherUser() != null) {
                        otherUser = result.getOtherUser();
                        if (otherUser.getUsername() != null) {
                            binding.userName.setText(otherUser.getUsername());
                        }
                        if (otherUser.getAvatar() != null) {
                            Glide.with(this)
                                    .load(otherUser.getAvatarUrl())
                                    .placeholder(R.drawable.ic_user)
                                    .error(R.drawable.ic_user)
                                    .circleCrop()
                                    .into(binding.userAvatar);
                        }
                        adapter.setOtherUser(otherUser);
                    }

                    hasMore = result.getCurrentPage() < result.getTotalPages();
                    
                    // 更新最后消息时间戳，用于增量获取
                    updateLastMessageTime();
                } else {
                    Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void loadMoreMessages() {
        if (isLoading || !hasMore) return;

        isLoading = true;

        new Thread(() -> {
            currentPage++;
            ApiResponse<ForumApi.PmDetailResult> response = forumApi.getPmChat(toUid, currentPage);

            runOnUiThread(() -> {
                isLoading = false;

                if (response.isSuccess() && response.getData() != null) {
                    ForumApi.PmDetailResult result = response.getData();
                    List<PrivateMessage> newMessages = result.getMessages();

                    if (newMessages.isEmpty()) {
                        hasMore = false;
                        currentPage--;
                    } else {
                        // 在前面插入历史消息
                        allMessages.addAll(0, newMessages);
                        adapter.submitList(new ArrayList<>(allMessages));

                        // 保持滚动位置
                        binding.messageList.post(() -> {
                            layoutManager.scrollToPositionWithOffset(newMessages.size(), 0);
                        });

                        hasMore = result.getCurrentPage() < result.getTotalPages();
                    }
                } else {
                    currentPage--;
                }
            });
        }).start();
    }

    private void toggleEmojiPanel() {
        if (binding.emojiPanelContainer.getVisibility() == View.VISIBLE) {
            binding.emojiPanelContainer.setVisibility(View.GONE);
        } else {
            setupEmojiPanel();
            binding.emojiPanelContainer.setVisibility(View.VISIBLE);
        }
    }

    private void setupEmojiPanel() {
        RecyclerView recyclerView = binding.emojiPanelContainer.findViewById(R.id.smileyGrid);
        if (recyclerView == null) return;

        // 设置固定高度，否则表情面板不会显示
        recyclerView.getLayoutParams().height = getResources().getDimensionPixelSize(R.dimen.smiley_panel_height);
        recyclerView.requestLayout();

        // 设置网格布局
        recyclerView.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 7));

        // 创建适配器
        java.util.List<com.forum.mt.model.Smiley> smileys = com.forum.mt.model.Smiley.getMtForumSmileys();
        com.forum.mt.ui.adapter.SmileyAdapter adapter = new com.forum.mt.ui.adapter.SmileyAdapter(
                new com.forum.mt.ui.adapter.SmileyAdapter.OnSmileyClickListener() {
                    @Override
                    public void onSmileyClick(com.forum.mt.model.Smiley smiley) {
                        insertSmileyToInput(smiley);
                    }

                    @Override
                    public void onDeleteClick() {
                        deleteLastCharacter();
                    }
                });
        adapter.setSmileys(smileys);
        recyclerView.setAdapter(adapter);

        // 设置删除按钮
        View deleteButton = binding.emojiPanelContainer.findViewById(R.id.deleteButton);
        if (deleteButton != null) {
            deleteButton.setOnClickListener(v -> deleteLastCharacter());
        }
    }

    /**
     * 插入表情到输入框
     */
    private void insertSmileyToInput(com.forum.mt.model.Smiley smiley) {
        int start = binding.messageInput.getSelectionStart();
        int end = binding.messageInput.getSelectionEnd();

        // 获取表情代码
        String smileyCode = smiley.getCode();

        // 插入表情代码
        android.text.Editable editable = binding.messageInput.getText();
        editable.replace(start, end, smileyCode);

        // 移动光标到表情代码后面
        binding.messageInput.setSelection(start + smileyCode.length());
    }

    /**
     * 删除最后一个字符
     */
    private void deleteLastCharacter() {
        android.text.Editable editable = binding.messageInput.getText();
        int length = editable.length();
        if (length > 0) {
            // 如果光标在末尾，删除最后一个字符
            int start = binding.messageInput.getSelectionStart();
            int end = binding.messageInput.getSelectionEnd();
            if (start == end && start > 0) {
                editable.delete(start - 1, start);
            } else {
                editable.delete(Math.min(start, end), Math.max(start, end));
            }
        }
    }

    private void sendMessage() {
        String message = binding.messageInput.getText().toString().trim();
        if (message.isEmpty()) {
            Toast.makeText(this, "请输入消息内容", Toast.LENGTH_SHORT).show();
            return;
        }

        if (message.length() > MAX_MESSAGE_LENGTH) {
            Toast.makeText(this, "消息内容不能超过" + MAX_MESSAGE_LENGTH + "字", Toast.LENGTH_SHORT).show();
            return;
        }

        binding.btnSend.setEnabled(false);
        binding.btnSend.setText("发送中...");

        new Thread(() -> {
            ApiResponse<Boolean> response = forumApi.sendPm(toUid, message);

            runOnUiThread(() -> {
                binding.btnSend.setEnabled(true);
                binding.btnSend.setText("发送");

                if (response.isSuccess() && Boolean.TRUE.equals(response.getData())) {
                    // 清空输入框
                    binding.messageInput.setText("");
                    
                    // 隐藏表情面板
                    binding.emojiPanelContainer.setVisibility(View.GONE);

                    // 立即检查新消息（不等待轮询）
                    // 延迟500ms让服务器处理完成
                    pollingHandler.postDelayed(() -> checkNewMessages(), 500);
                } else {
                    Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        }).start();
    }

    private void refreshConversation() {
        currentPage = 1;
        loadConversation();
    }

    private void showLoading(boolean show) {
        binding.progressBar.setVisibility(show ? View.VISIBLE : View.GONE);
    }
}
