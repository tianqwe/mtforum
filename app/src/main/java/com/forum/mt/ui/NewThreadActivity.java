package com.forum.mt.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.appcompat.widget.Toolbar;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.forum.mt.R;
import com.forum.mt.api.ApiConfig;
import com.forum.mt.api.CookieManager;
import com.forum.mt.api.ForumApi;
import com.forum.mt.api.HttpClient;
import com.forum.mt.model.ApiResponse;
import com.forum.mt.model.NewThreadParams;
import com.forum.mt.model.Smiley;
import com.forum.mt.model.UploadResult;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 发帖Activity
 */
public class NewThreadActivity extends AppCompatActivity {

    private static final String EXTRA_FID = "fid";
    private static final String EXTRA_FORUM_NAME = "forum_name";
    private static final int REQUEST_CODE_PICK_IMAGE = 1001;
    private static final int REQUEST_CODE_SELECT_FORUM = 1002;
    private static final int REQUEST_CODE_PICK_ATTACHMENT = 1003;

    private ImageView backButton;
    private TextView configButton;
    private TextView publishButton;
    private TextView forumNameText;
    private LinearLayout forumSelectContainer;
    private EditText titleInput;
    private EditText contentInput;
    private ImageView emojiButton;
    private ImageView imageButton;
    private ImageView insertButton;
    private ImageView attachButton;
    private ImageView mentionButton;
    private ImageView permissionButton;
    private TextView charCount;
    private LinearLayout imagePreviewContainer;
    private LinearLayout attachPreviewContainer;
    private RecyclerView imagePreviewList;
    private RecyclerView attachPreviewList;
    private LinearLayout panelContainer;
    private View smileyPanel;
    private View insertPanel;
    private View permissionPanel;
    private View advancedPanel;
    
    // 当前显示的面板类型
    private static final int PANEL_NONE = 0;
    private static final int PANEL_SMILEY = 1;
    private static final int PANEL_INSERT = 2;
    private static final int PANEL_PERMISSION = 3;
    private static final int PANEL_ADVANCED = 4;
    private int currentPanel = PANEL_NONE;
    private boolean smileyAdapterInitialized = false;
    private boolean insertPanelListenersInitialized = false;

    private ForumApi forumApi;
    private CookieManager cookieManager;
    private ExecutorService executor;
    private Handler mainHandler;

    private int fid;
    private String forumName;
    private NewThreadParams params;
    private List<UploadedAttachment> uploadedAttachments = new ArrayList<>();
    private List<UploadedAttachment> uploadedFiles = new ArrayList<>(); // 非图片附件
    private ImagePreviewAdapter imageAdapter;
    private AttachmentPreviewAdapter attachAdapter;

    public static void start(Context context, int fid, String forumName) {
        Intent intent = new Intent(context, NewThreadActivity.class);
        intent.putExtra(EXTRA_FID, fid);
        intent.putExtra(EXTRA_FORUM_NAME, forumName);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_new_thread);

        // 获取参数
        fid = getIntent().getIntExtra(EXTRA_FID, 0);
        forumName = getIntent().getStringExtra(EXTRA_FORUM_NAME);

        initViews();
        initData();
        setupListeners();
        loadNewThreadParams();
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        configButton = findViewById(R.id.configButton);
        publishButton = findViewById(R.id.publishButton);
        forumNameText = findViewById(R.id.forumNameText);
        forumSelectContainer = findViewById(R.id.forumSelectContainer);
        titleInput = findViewById(R.id.titleInput);
        contentInput = findViewById(R.id.contentInput);
        emojiButton = findViewById(R.id.emojiButton);
        imageButton = findViewById(R.id.imageButton);
        insertButton = findViewById(R.id.insertButton);
        attachButton = findViewById(R.id.attachButton);
        mentionButton = findViewById(R.id.mentionButton);
        permissionButton = findViewById(R.id.permissionButton);
        charCount = findViewById(R.id.charCount);
        imagePreviewContainer = findViewById(R.id.imagePreviewContainer);
        attachPreviewContainer = findViewById(R.id.attachPreviewContainer);
        imagePreviewList = findViewById(R.id.imagePreviewList);
        attachPreviewList = findViewById(R.id.attachPreviewList);
        panelContainer = findViewById(R.id.panelContainer);
        smileyPanel = findViewById(R.id.smileyPanel);
        insertPanel = findViewById(R.id.insertPanel);
        permissionPanel = findViewById(R.id.permissionPanel);
        advancedPanel = findViewById(R.id.advancedPanel);

        // 设置版块名称
        if (forumName != null && !forumName.isEmpty()) {
            forumNameText.setText(forumName);
            forumNameText.setTextColor(getResources().getColor(R.color.text_dark));
        }

        // 设置图片预览列表
        imageAdapter = new ImagePreviewAdapter(this, uploadedAttachments, this::removeAttachment, this::insertImageAttachment);
        imagePreviewList.setLayoutManager(new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));
        imagePreviewList.setAdapter(imageAdapter);

        // 设置附件预览列表
        attachAdapter = new AttachmentPreviewAdapter(this, uploadedFiles,
            position -> removeFileAttachment(position),
            position -> insertAttachmentCode(position));
        attachPreviewList.setLayoutManager(new LinearLayoutManager(this));
        attachPreviewList.setAdapter(attachAdapter);
    }

    private void initData() {
        HttpClient httpClient = HttpClient.getInstance(this);
        forumApi = new ForumApi(httpClient.getOkHttpClient(), httpClient.getCookieManager());
        cookieManager = httpClient.getCookieManager();
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    private void setupListeners() {
        // 返回按钮
        backButton.setOnClickListener(v -> finish());

        // 配置按钮 - 打开高级选项面板
        configButton.setOnClickListener(v -> togglePanel(PANEL_ADVANCED));

        // 发布按钮
        publishButton.setOnClickListener(v -> publishThread());

        // 选择版块
        if (forumSelectContainer != null) {
            forumSelectContainer.setOnClickListener(v -> {
                ForumSelectActivity.start(this);
            });
        }

        // 表情按钮
        emojiButton.setOnClickListener(v -> togglePanel(PANEL_SMILEY));

        // 图片按钮
        imageButton.setOnClickListener(v -> pickImages());

        // 插入按钮
        insertButton.setOnClickListener(v -> togglePanel(PANEL_INSERT));

        // 附件按钮
        attachButton.setOnClickListener(v -> pickAttachment());

        // @提及按钮
        mentionButton.setOnClickListener(v -> {
            // 在内容中插入@符号
            insertText("@");
        });

        // 权限按钮
        permissionButton.setOnClickListener(v -> togglePanel(PANEL_PERMISSION));

        // 设置插入面板的Tab点击监听
        setupInsertPanelListeners();

        // 内容变化监听
        contentInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateCharCount();
            }
        });

        titleInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateCharCount();
            }
        });

        // 输入框焦点监听：当输入框获取焦点时自动隐藏面板，避免面板遮挡输入区域
        View.OnFocusChangeListener focusChangeListener = (v, hasFocus) -> {
            if (hasFocus && currentPanel != PANEL_NONE) {
                hideAllPanels();
                currentPanel = PANEL_NONE;
            }
        };
        
        titleInput.setOnFocusChangeListener(focusChangeListener);
        contentInput.setOnFocusChangeListener(focusChangeListener);
    }

    private void updateCharCount() {
        int titleLen = titleInput.getText().length();
        int contentLen = contentInput.getText().length();
        charCount.setText(titleLen + contentLen + "/10000");
    }

    private void loadNewThreadParams() {
        if (fid <= 0) {
            Toast.makeText(this, "版块ID无效", Toast.LENGTH_SHORT).show();
            return;
        }

        setProgressVisible(true);
        executor.execute(() -> {
            ApiResponse<NewThreadParams> response = forumApi.getNewThreadParams(fid);
            mainHandler.post(() -> {
                setProgressVisible(false);
                if (response.isSuccess() && response.getData() != null) {
                    params = response.getData();
                } else {
                    Toast.makeText(this, response.getMessage(), Toast.LENGTH_SHORT).show();
                }
            });
        });
    }

    private void togglePanel(int panelType) {
        // 确保视图已初始化
        if (panelContainer == null) panelContainer = findViewById(R.id.panelContainer);
        if (smileyPanel == null) smileyPanel = findViewById(R.id.smileyPanel);
        if (insertPanel == null) insertPanel = findViewById(R.id.insertPanel);
        if (permissionPanel == null) permissionPanel = findViewById(R.id.permissionPanel);
        if (advancedPanel == null) advancedPanel = findViewById(R.id.advancedPanel);

        // 如果点击的是当前显示的面板，则隐藏
        if (currentPanel == panelType) {
            hideAllPanels();
            currentPanel = PANEL_NONE;
            // 隐藏面板后，重新显示键盘
            showKeyboard();
            return;
        }

        // 显示面板前，先隐藏键盘，确保面板不被键盘遮挡
        hideKeyboard();

        // 隐藏所有面板
        hideAllPanelsInternal();

        // 显示面板容器
        if (panelContainer != null) {
            panelContainer.setVisibility(View.VISIBLE);
        }

        // 显示选中的面板
        switch (panelType) {
            case PANEL_SMILEY:
                if (smileyPanel != null) {
                    smileyPanel.setVisibility(View.VISIBLE);
                    setupSmileyPanel();
                }
                break;
            case PANEL_INSERT:
                if (insertPanel != null) {
                    insertPanel.setVisibility(View.VISIBLE);
                    if (!insertPanelListenersInitialized) {
                        setupInsertPanelListeners();
                        insertPanelListenersInitialized = true;
                    }
                }
                break;
            case PANEL_PERMISSION:
                if (permissionPanel != null) {
                    permissionPanel.setVisibility(View.VISIBLE);
                    setupPermissionPanelListeners();
                }
                break;
            case PANEL_ADVANCED:
                if (advancedPanel != null) {
                    advancedPanel.setVisibility(View.VISIBLE);
                }
                break;
        }

        currentPanel = panelType;
    }
    
    /**
     * 隐藏键盘
     */
    private void hideKeyboard() {
        android.view.View view = getCurrentFocus();
        if (view == null) {
            view = contentInput != null ? contentInput : new android.view.View(this);
        }
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
    
    /**
     * 显示键盘
     */
    private void showKeyboard() {
        if (contentInput != null) {
            contentInput.requestFocus();
            android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                    getSystemService(Context.INPUT_METHOD_SERVICE);
            if (imm != null) {
                imm.showSoftInput(contentInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
            }
        }
    }
    
    private void hideAllPanelsInternal() {
        if (smileyPanel != null) smileyPanel.setVisibility(View.GONE);
        if (insertPanel != null) insertPanel.setVisibility(View.GONE);
        if (permissionPanel != null) permissionPanel.setVisibility(View.GONE);
        if (advancedPanel != null) advancedPanel.setVisibility(View.GONE);
    }

    private void hideAllPanels() {
        if (smileyPanel == null) smileyPanel = findViewById(R.id.smileyPanel);
        if (insertPanel == null) insertPanel = findViewById(R.id.insertPanel);
        if (permissionPanel == null) permissionPanel = findViewById(R.id.permissionPanel);
        if (advancedPanel == null) advancedPanel = findViewById(R.id.advancedPanel);
        if (panelContainer == null) panelContainer = findViewById(R.id.panelContainer);

        if (smileyPanel != null) smileyPanel.setVisibility(View.GONE);
        if (insertPanel != null) insertPanel.setVisibility(View.GONE);
        if (permissionPanel != null) permissionPanel.setVisibility(View.GONE);
        if (advancedPanel != null) advancedPanel.setVisibility(View.GONE);
        if (panelContainer != null) panelContainer.setVisibility(View.GONE);
    }

    private void setupInsertPanelListeners() {
        if (insertPanel == null) {
            insertPanel = findViewById(R.id.insertPanel);
        }
        if (insertPanel == null) return;
        
        // Tab切换 - 第一行
        TextView urlTab = insertPanel.findViewById(R.id.insertUrlTab);
        TextView imgTab = insertPanel.findViewById(R.id.insertImgTab);
        TextView audioTab = insertPanel.findViewById(R.id.insertAudioTab);
        TextView videoTab = insertPanel.findViewById(R.id.insertVideoTab);
        
        // Tab切换 - 第二行
        TextView flashTab = insertPanel.findViewById(R.id.insertFlashTab);
        TextView quoteTab = insertPanel.findViewById(R.id.insertQuoteTab);
        TextView codeTab = insertPanel.findViewById(R.id.insertCodeTab);
        TextView freeTab = insertPanel.findViewById(R.id.insertFreeTab);
        
        // Tab切换 - 第三行
        TextView hideTab = insertPanel.findViewById(R.id.insertHideTab);
        
        // 面板
        View urlPanel = insertPanel.findViewById(R.id.urlPanel);
        View imgPanel = insertPanel.findViewById(R.id.imgPanel);
        View audioPanel = insertPanel.findViewById(R.id.audioPanel);
        View videoPanel = insertPanel.findViewById(R.id.videoPanel);
        View flashPanel = insertPanel.findViewById(R.id.flashPanel);
        View quotePanel = insertPanel.findViewById(R.id.quotePanel);
        View codePanel = insertPanel.findViewById(R.id.codePanel);
        View freePanel = insertPanel.findViewById(R.id.freePanel);
        View hidePanel = insertPanel.findViewById(R.id.hidePanel);
        
        TextView[] tabs = {urlTab, imgTab, audioTab, videoTab, flashTab, quoteTab, codeTab, freeTab, hideTab};
        View[] panels = {urlPanel, imgPanel, audioPanel, videoPanel, flashPanel, quotePanel, codePanel, freePanel, hidePanel};
        
        for (int i = 0; i < tabs.length; i++) {
            final int index = i;
            if (tabs[i] != null) {
                tabs[i].setOnClickListener(v -> {
                    // 更新Tab样式
                    for (TextView tab : tabs) {
                        if (tab != null) {
                            tab.setTextColor(androidx.core.content.ContextCompat.getColor(NewThreadActivity.this, R.color.text_gray));
                            tab.setBackgroundResource(R.drawable.bg_tab_unselected);
                        }
                    }
                    // 安全访问 tabs[index]
                    if (tabs[index] != null) {
                        tabs[index].setTextColor(androidx.core.content.ContextCompat.getColor(NewThreadActivity.this, R.color.primary));
                        tabs[index].setBackgroundResource(R.drawable.bg_tab_selected);
                    }
                    
                    // 切换面板
                    for (View panel : panels) {
                        if (panel != null) panel.setVisibility(View.GONE);
                    }
                    if (panels[index] != null) panels[index].setVisibility(View.VISIBLE);
                });
            }
        }
        
        // 插入链接按钮
        TextView insertUrlBtn = insertPanel.findViewById(R.id.insertUrlButton);
        if (insertUrlBtn != null) {
            insertUrlBtn.setOnClickListener(v -> {
                EditText urlInput = insertPanel.findViewById(R.id.urlInput);
                EditText urlTextInput = insertPanel.findViewById(R.id.urlTextInput);
                String url = urlInput != null ? urlInput.getText().toString().trim() : "";
                String text = urlTextInput != null ? urlTextInput.getText().toString().trim() : "";
                if (!url.isEmpty()) {
                    String bbcode = "[url=" + url + "]" + (text.isEmpty() ? url : text) + "[/url]";
                    insertText(bbcode);
                    if (urlInput != null) urlInput.setText("");
                    if (urlTextInput != null) urlTextInput.setText("");
                } else {
                    Toast.makeText(this, "请输入链接网址", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // 插入图片按钮
        TextView insertImgBtn = insertPanel.findViewById(R.id.insertImgButton);
        if (insertImgBtn != null) {
            insertImgBtn.setOnClickListener(v -> {
                EditText imgUrlInput = insertPanel.findViewById(R.id.imgUrlInput);
                String url = imgUrlInput != null ? imgUrlInput.getText().toString().trim() : "";
                if (!url.isEmpty()) {
                    insertText("[img]" + url + "[/img]");
                    if (imgUrlInput != null) imgUrlInput.setText("");
                } else {
                    Toast.makeText(this, "请输入图片网址", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // 插入音乐按钮
        TextView insertAudioBtn = insertPanel.findViewById(R.id.insertAudioButton);
        if (insertAudioBtn != null) {
            insertAudioBtn.setOnClickListener(v -> {
                EditText audioUrlInput = insertPanel.findViewById(R.id.audioUrlInput);
                String url = audioUrlInput != null ? audioUrlInput.getText().toString().trim() : "";
                if (!url.isEmpty()) {
                    insertText("[audio]" + url + "[/audio]");
                    if (audioUrlInput != null) audioUrlInput.setText("");
                } else {
                    Toast.makeText(this, "请输入音乐网址", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // 插入视频按钮
        TextView insertVideoBtn = insertPanel.findViewById(R.id.insertVideoButton);
        if (insertVideoBtn != null) {
            insertVideoBtn.setOnClickListener(v -> {
                EditText videoUrlInput = insertPanel.findViewById(R.id.videoUrlInput);
                String url = videoUrlInput != null ? videoUrlInput.getText().toString().trim() : "";
                if (!url.isEmpty()) {
                    insertText("[media=x,500,375]" + url + "[/media]");
                    if (videoUrlInput != null) videoUrlInput.setText("");
                } else {
                    Toast.makeText(this, "请输入视频网址", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // 插入Flash按钮
        TextView insertFlashBtn = insertPanel.findViewById(R.id.insertFlashButton);
        if (insertFlashBtn != null) {
            insertFlashBtn.setOnClickListener(v -> {
                EditText flashUrlInput = insertPanel.findViewById(R.id.flashUrlInput);
                String url = flashUrlInput != null ? flashUrlInput.getText().toString().trim() : "";
                if (!url.isEmpty()) {
                    insertText("[flash]" + url + "[/flash]");
                    if (flashUrlInput != null) flashUrlInput.setText("");
                } else {
                    Toast.makeText(this, "请输入Flash网址", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // 插入引用按钮
        TextView insertQuoteBtn = insertPanel.findViewById(R.id.insertQuoteButton);
        if (insertQuoteBtn != null) {
            insertQuoteBtn.setOnClickListener(v -> {
                EditText quoteInput = insertPanel.findViewById(R.id.quoteInput);
                String quote = quoteInput != null ? quoteInput.getText().toString().trim() : "";
                if (!quote.isEmpty()) {
                    insertText("[quote]" + quote + "[/quote]");
                    if (quoteInput != null) quoteInput.setText("");
                } else {
                    Toast.makeText(this, "请输入引用内容", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // 插入代码按钮
        TextView insertCodeBtn = insertPanel.findViewById(R.id.insertCodeButton);
        if (insertCodeBtn != null) {
            insertCodeBtn.setOnClickListener(v -> {
                EditText codeInput = insertPanel.findViewById(R.id.codeInput);
                String code = codeInput != null ? codeInput.getText().toString().trim() : "";
                if (!code.isEmpty()) {
                    insertText("[code]" + code + "[/code]");
                    if (codeInput != null) codeInput.setText("");
                } else {
                    Toast.makeText(this, "请输入代码内容", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // 插入免费信息按钮
        TextView insertFreeBtn = insertPanel.findViewById(R.id.insertFreeButton);
        if (insertFreeBtn != null) {
            insertFreeBtn.setOnClickListener(v -> {
                EditText freeInput = insertPanel.findViewById(R.id.freeInput);
                String free = freeInput != null ? freeInput.getText().toString().trim() : "";
                if (!free.isEmpty()) {
                    insertText("[free]" + free + "[/free]");
                    if (freeInput != null) freeInput.setText("");
                } else {
                    Toast.makeText(this, "请输入免费信息内容", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // 插入隐藏内容按钮
        TextView insertHideBtn = insertPanel.findViewById(R.id.insertHideButton);
        if (insertHideBtn != null) {
            insertHideBtn.setOnClickListener(v -> {
                EditText hideInput = insertPanel.findViewById(R.id.hideInput);
                EditText hideCreditInput = insertPanel.findViewById(R.id.hideCreditInput);
                EditText hideDaysInput = insertPanel.findViewById(R.id.hideDaysInput);
                
                String hide = hideInput != null ? hideInput.getText().toString().trim() : "";
                String credit = hideCreditInput != null ? hideCreditInput.getText().toString().trim() : "";
                String days = hideDaysInput != null ? hideDaysInput.getText().toString().trim() : "";
                
                if (!hide.isEmpty()) {
                    // 构建hide标签
                    // 格式: [hide=d{天数},{积分}]{内容}[/hide]
                    // 如果设置了积分或天数，需要加=号
                    StringBuilder bbcode = new StringBuilder("[hide");
                    
                    boolean hasParams = !credit.isEmpty() || !days.isEmpty();
                    if (hasParams) {
                        bbcode.append("=");
                        if (!days.isEmpty()) {
                            bbcode.append("d").append(days);
                        }
                        if (!credit.isEmpty() && !days.isEmpty()) {
                            bbcode.append(",");
                        }
                        if (!credit.isEmpty()) {
                            bbcode.append(credit);
                        }
                    }
                    bbcode.append("]").append(hide).append("[/hide]");
                    
                    insertText(bbcode.toString());
                    if (hideInput != null) hideInput.setText("");
                    if (hideCreditInput != null) hideCreditInput.setText("");
                    if (hideDaysInput != null) hideDaysInput.setText("");
                } else {
                    Toast.makeText(this, "请输入隐藏内容", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    
    private boolean permissionPanelListenersInitialized = false;
    
    private void setupPermissionPanelListeners() {
        if (permissionPanel == null) {
            permissionPanel = findViewById(R.id.permissionPanel);
        }
        if (permissionPanel == null || permissionPanelListenersInitialized) return;
        
        // 密码设置按钮
        TextView setPasswordButton = permissionPanel.findViewById(R.id.setPasswordButton);
        EditText passwordInput = permissionPanel.findViewById(R.id.passwordInput);
        
        if (setPasswordButton != null && passwordInput != null) {
            setPasswordButton.setOnClickListener(v -> {
                String password = passwordInput.getText().toString().trim();
                if (!password.isEmpty()) {
                    try {
                        // 确保contentInput不为空
                        if (contentInput == null) {
                            contentInput = findViewById(R.id.contentInput);
                        }
                        if (contentInput == null) {
                            Toast.makeText(this, "内容输入框未初始化", Toast.LENGTH_SHORT).show();
                            return;
                        }
                        
                        // 插入密码BBCode
                        // 先移除已有的密码代码
                        String currentContent = contentInput.getText().toString();
                        String cleanedContent = currentContent.replaceAll("\\[password\\].*?\\[/password\\]", "");
                        
                        // 在内容末尾添加密码代码
                        String passwordCode = "[password]" + password + "[/password]";
                        contentInput.setText(cleanedContent + "\n" + passwordCode);
                        passwordInput.setText("");
                        Toast.makeText(this, "密码已插入内容末尾", Toast.LENGTH_SHORT).show();
                    } catch (Exception e) {
                        Toast.makeText(this, "插入密码失败", Toast.LENGTH_SHORT).show();
                    }
                } else {
                    Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show();
                }
            });
        }
        
        // 更新最大售价提示
        if (params != null && params.getMaxPrice() > 0) {
            TextView priceHint = permissionPanel.findViewById(R.id.priceHint);
            if (priceHint != null) {
                priceHint.setText("最高售价: " + params.getMaxPrice() + " 金币");
            }
        }
        
        permissionPanelListenersInitialized = true;
    }
    
    private void insertText(String text) {
        if (contentInput == null || text == null || text.isEmpty()) return;
        try {
            int start = contentInput.getSelectionStart();
            Editable editable = contentInput.getText();
            if (editable != null) {
                editable.insert(Math.max(0, start), text);
            }
        } catch (Exception e) {
            // 忽略插入错误
        }
    }
    
    private void pickAttachment() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        startActivityForResult(Intent.createChooser(intent, "选择附件"), REQUEST_CODE_PICK_ATTACHMENT);
    }

    private void setupSmileyPanel() {
        if (smileyPanel == null) {
            smileyPanel = findViewById(R.id.smileyPanel);
        }
        if (smileyPanel == null) return;
        
        // 获取视图
        RecyclerView smileyGrid = smileyPanel.findViewById(R.id.smileyGrid);
        View deleteBtn = smileyPanel.findViewById(R.id.deleteButton);
        TextView tabTb = smileyPanel.findViewById(R.id.tabTb);
        TextView tabQQ = smileyPanel.findViewById(R.id.tabQQ);
        TextView tabDoge = smileyPanel.findViewById(R.id.tabDoge);
        
        if (smileyGrid == null) return;
        
        // 设置网格布局
        if (smileyGrid.getLayoutManager() == null) {
            smileyGrid.setLayoutManager(new androidx.recyclerview.widget.GridLayoutManager(this, 7));
        }
        
        // 创建适配器
        com.forum.mt.ui.adapter.SmileyAdapter adapter = new com.forum.mt.ui.adapter.SmileyAdapter(
                new com.forum.mt.ui.adapter.SmileyAdapter.OnSmileyClickListener() {
                    @Override
                    public void onSmileyClick(Smiley smiley) {
                        insertSmiley(smiley);
                    }
                    
                    @Override
                    public void onDeleteClick() {
                        deleteLastCharacter();
                    }
                });
        
        // 默认加载淘宝表情（滑稽）
        adapter.setSmileys(Smiley.getSmileysByCategory(Smiley.CATEGORY_TB));
        smileyGrid.setAdapter(adapter);
        
        // 当前选中的分类标签
        final TextView[] currentTab = {tabTb};
        final int[] currentCategory = {Smiley.CATEGORY_TB};
        
        // 分类标签点击事件
        View.OnClickListener tabClickListener = v -> {
            TextView clickedTab = (TextView) v;
            int newCategory;
            
            if (v == tabTb) {
                newCategory = Smiley.CATEGORY_TB;
            } else if (v == tabQQ) {
                newCategory = Smiley.CATEGORY_QQ;
            } else {
                newCategory = Smiley.CATEGORY_DOGE;
            }
            
            // 如果点击的是当前分类，不做处理
            if (newCategory == currentCategory[0]) {
                return;
            }
            
            // 更新标签样式
            if (currentTab[0] != null) {
                currentTab[0].setTextColor(getResources().getColor(R.color.text_light_gray));
                currentTab[0].setTypeface(null, android.graphics.Typeface.NORMAL);
            }
            clickedTab.setTextColor(getResources().getColor(R.color.text_dark));
            clickedTab.setTypeface(null, android.graphics.Typeface.BOLD);
            
            // 更新当前标签和分类
            currentTab[0] = clickedTab;
            currentCategory[0] = newCategory;
            
            // 加载对应分类的表情
            java.util.List<Smiley> smileys = Smiley.getSmileysByCategory(newCategory);
            adapter.setSmileys(smileys);
            
            // 滚动到顶部
            smileyGrid.scrollToPosition(0);
        };
        
        if (tabTb != null) tabTb.setOnClickListener(tabClickListener);
        if (tabQQ != null) tabQQ.setOnClickListener(tabClickListener);
        if (tabDoge != null) tabDoge.setOnClickListener(tabClickListener);
        
        // 设置默认选中状态
        if (tabTb != null) {
            tabTb.setTextColor(getResources().getColor(R.color.text_dark));
            tabTb.setTypeface(null, android.graphics.Typeface.BOLD);
        }

        // 删除按钮
        if (deleteBtn != null) {
            deleteBtn.setOnClickListener(v -> deleteLastCharacter());
        }
        
        smileyAdapterInitialized = true;
    }
    
    /**
     * 删除最后一个字符（智能识别表情代码）
     */
    private void deleteLastCharacter() {
        if (contentInput == null) return;
        
        Editable editable = contentInput.getText();
        int length = editable != null ? editable.length() : 0;
        
        if (length > 0) {
            String text = editable.toString();
            int lastOpenBracket = text.lastIndexOf("[");
            
            // 检查是否是表情代码 [xxx] 或 [#xxx] 或 [doge]
            if (lastOpenBracket >= 0 && lastOpenBracket < length) {
                int closeBracket = text.indexOf("]", lastOpenBracket);
                // 检查是否有完整的表情代码（中间没有其他 [ 或 ]）
                if (closeBracket > lastOpenBracket && closeBracket < length) {
                    String possibleSmiley = text.substring(lastOpenBracket, closeBracket + 1);
                    // 验证是否是有效表情格式: [#xxx] 或 [xxx] 或 [doge]
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

    private void insertSmiley(Smiley smiley) {
        if (contentInput == null || smiley == null) return;
        String code = smiley.getCode();
        if (code == null || code.isEmpty()) return;
        
        try {
            int start = contentInput.getSelectionStart();
            int end = contentInput.getSelectionEnd();
            Editable editable = contentInput.getText();
            if (editable != null) {
                editable.replace(Math.max(0, start), Math.max(0, end), code);
            }
        } catch (Exception e) {
            // 忽略插入错误
        }
    }

    private void pickImages() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        startActivityForResult(Intent.createChooser(intent, "选择图片"), REQUEST_CODE_PICK_IMAGE);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
        super.onActivityResult(requestCode, resultCode, data);

        if (requestCode == REQUEST_CODE_PICK_IMAGE && resultCode == Activity.RESULT_OK && data != null) {
            List<Uri> imageUris = new ArrayList<>();
            
            android.content.ClipData clipData = data.getClipData();
            if (clipData != null && clipData.getItemCount() > 0) {
                // 多选图片
                int count = clipData.getItemCount();
                for (int i = 0; i < count; i++) {
                    imageUris.add(clipData.getItemAt(i).getUri());
                }
            } else if (data.getData() != null) {
                // 单选图片
                imageUris.add(data.getData());
            }

            if (!imageUris.isEmpty()) {
                uploadImages(imageUris);
            }
        } else if (requestCode == REQUEST_CODE_PICK_ATTACHMENT && resultCode == Activity.RESULT_OK && data != null) {
            // 处理附件选择
            Uri attachUri = data.getData();
            if (attachUri != null) {
                uploadAttachment(attachUri);
            }
        } else if (requestCode == ForumSelectActivity.REQUEST_CODE_SELECT_FORUM && resultCode == Activity.RESULT_OK && data != null) {
            // 处理版块选择结果
            int selectedFid = data.getIntExtra(ForumSelectActivity.EXTRA_SELECTED_FID, 0);
            String selectedForumName = data.getStringExtra(ForumSelectActivity.EXTRA_SELECTED_FORUM_NAME);

            if (selectedFid > 0 && selectedForumName != null) {
                fid = selectedFid;
                forumName = selectedForumName;
                forumNameText.setText(forumName);
                forumNameText.setTextColor(getResources().getColor(R.color.text_dark));

                // 重新加载该版块的参数
                loadNewThreadParams();
            }
        }
    }

    private void uploadImages(List<Uri> imageUris) {
        if (params == null || params.getUploadHash() == null) {
            Toast.makeText(this, "请等待页面加载完成", Toast.LENGTH_SHORT).show();
            return;
        }
        
        final int uid = params.getUid();
        final String uploadHash = params.getUploadHash();

        Toast.makeText(this, "开始上传图片...", Toast.LENGTH_SHORT).show();

        for (Uri uri : imageUris) {
            executor.execute(() -> {
                try {
                    // 将Uri转换为File
                    File imageFile = uriToFile(uri);
                    if (imageFile == null) {
                        mainHandler.post(() -> 
                            Toast.makeText(this, "无法读取图片文件", Toast.LENGTH_SHORT).show());
                        return;
                    }

                    // 上传图片
                    ApiResponse<UploadResult> response = forumApi.uploadImage(
                        imageFile, uid, uploadHash);

                    mainHandler.post(() -> {
                        if (response.isSuccess() && response.getData() != null) {
                            UploadResult result = response.getData();
                            if (result.isSuccess()) {
                                UploadedAttachment attachment = new UploadedAttachment(
                                    result.getAid(), uri, result.getImageUrl(), result.getAttachCode());
                                uploadedAttachments.add(attachment);
                                imageAdapter.notifyDataSetChanged();
                                updateImagePreviewVisibility();

                                Toast.makeText(this, "图片上传成功，点击插入", Toast.LENGTH_SHORT).show();
                            } else {
                                Toast.makeText(this, "上传失败: " + result.getErrorMessage(), Toast.LENGTH_SHORT).show();
                            }
                        } else {
                            Toast.makeText(this, "上传失败: " + response.getMessage(), Toast.LENGTH_SHORT).show();
                        }
                    });
                } catch (Exception e) {
                    e.printStackTrace();
                    mainHandler.post(() -> 
                        Toast.makeText(this, "上传出错: " + e.getMessage(), Toast.LENGTH_SHORT).show());
                }
            });
        }
    }

    private File uriToFile(Uri uri) {
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            if (uri == null) return null;
            inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            
            // 读取图片并解码
            android.graphics.Bitmap bitmap = android.graphics.BitmapFactory.decodeStream(inputStream);
            
            if (bitmap == null) {
                return null;
            }
            
            // 统一保存为JPEG格式，确保文件内容与扩展名一致
            String fileName = "upload_" + System.currentTimeMillis() + ".jpg";
            File tempFile = new File(getCacheDir(), fileName);
            
            outputStream = new FileOutputStream(tempFile);
            bitmap.compress(android.graphics.Bitmap.CompressFormat.JPEG, 90, outputStream);
            outputStream.flush();
            
            try {
                bitmap.recycle();
            } catch (Exception ignored) {}

            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (inputStream != null) inputStream.close();
            } catch (Exception ignored) {}
            try {
                if (outputStream != null) outputStream.close();
            } catch (Exception ignored) {}
        }
    }

    private void removeAttachment(int position) {
        if (position >= 0 && position < uploadedAttachments.size()) {
            uploadedAttachments.remove(position);
            imageAdapter.notifyDataSetChanged();
            updateImagePreviewVisibility();
        }
    }
    
    private void insertImageAttachment(int position) {
        if (contentInput == null) {
            contentInput = findViewById(R.id.contentInput);
        }
        if (contentInput == null) return;
        
        if (position >= 0 && position < uploadedAttachments.size()) {
            try {
                UploadedAttachment att = uploadedAttachments.get(position);
                String attachCode = att.attachCode;
                if (attachCode != null && !attachCode.isEmpty()) {
                    int start = contentInput.getSelectionStart();
                    Editable editable = contentInput.getText();
                    if (editable != null) {
                        editable.insert(Math.max(0, start), attachCode + "\n");
                    }
                    Toast.makeText(this, "图片已插入", Toast.LENGTH_SHORT).show();
                }
            } catch (Exception e) {
                // 忽略插入错误
            }
        }
    }
    
    private void removeFileAttachment(int position) {
        if (position >= 0 && position < uploadedFiles.size()) {
            uploadedFiles.remove(position);
            attachAdapter.notifyDataSetChanged();
            updateAttachPreviewVisibility();
        }
    }
    
    private void insertAttachmentCode(int position) {
        if (contentInput == null) {
            contentInput = findViewById(R.id.contentInput);
        }
        if (contentInput == null) return;
        
        if (position >= 0 && position < uploadedFiles.size()) {
            try {
                UploadedAttachment att = uploadedFiles.get(position);
                String attachCode = "[attach]" + att.aid + "[/attach]";
                int start = contentInput.getSelectionStart();
                Editable editable = contentInput.getText();
                if (editable != null) {
                    editable.insert(Math.max(0, start), attachCode + "\n");
                }
            } catch (Exception e) {
                // 忽略插入错误
            }
        }
    }

    private void updateImagePreviewVisibility() {
        if (imagePreviewContainer != null) {
            imagePreviewContainer.setVisibility(uploadedAttachments.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }

    private void updateAttachPreviewVisibility() {
        if (attachPreviewContainer != null) {
            attachPreviewContainer.setVisibility(uploadedFiles.isEmpty() ? View.GONE : View.VISIBLE);
        }
    }
    
    /**
     * 上传附件（非图片文件）
     */
    private void uploadAttachment(Uri uri) {
        if (params == null || params.getUploadHash() == null) {
            Toast.makeText(this, "请等待页面加载完成", Toast.LENGTH_SHORT).show();
            return;
        }
        
        final int uid = params.getUid();
        final String uploadHash = params.getUploadHash();

        Toast.makeText(this, "开始上传附件...", Toast.LENGTH_SHORT).show();

        executor.execute(() -> {
            try {
                // 将Uri转换为File
                File attachFile = uriToAttachmentFile(uri);
                if (attachFile == null) {
                    mainHandler.post(() -> 
                        Toast.makeText(this, "无法读取附件文件", Toast.LENGTH_SHORT).show());
                    return;
                }

                // 上传附件
                ApiResponse<UploadResult> response = forumApi.uploadAttachment(
                    attachFile, uid, uploadHash, fid);

                mainHandler.post(() -> {
                    if (response.isSuccess() && response.getData() != null) {
                        UploadResult result = response.getData();
                        if (result.isSuccess()) {
                            UploadedAttachment attachment = new UploadedAttachment(
                                result.getAid(), uri, result.getImageUrl(), result.getAttachCode());
                            attachment.fileName = getFileNameFromUri(uri);
                            uploadedFiles.add(attachment);
                            attachAdapter.notifyDataSetChanged();
                            updateAttachPreviewVisibility();

                            Toast.makeText(this, "附件上传成功", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "上传失败: " + result.getErrorMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        Toast.makeText(this, "上传失败: " + response.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                e.printStackTrace();
                mainHandler.post(() -> 
                    Toast.makeText(this, "上传出错: " + e.getMessage(), Toast.LENGTH_SHORT).show());
            }
        });
    }
    
    /**
     * 将Uri转换为附件文件
     */
    private File uriToAttachmentFile(Uri uri) {
        InputStream inputStream = null;
        FileOutputStream outputStream = null;
        try {
            if (uri == null) return null;
            inputStream = getContentResolver().openInputStream(uri);
            if (inputStream == null) return null;
            
            // 获取文件名
            String fileName = getFileNameFromUri(uri);
            if (fileName == null || fileName.isEmpty()) {
                fileName = "attachment_" + System.currentTimeMillis();
            }
            
            File tempFile = new File(getCacheDir(), fileName);
            
            outputStream = new FileOutputStream(tempFile);
            byte[] buffer = new byte[4096];
            int bytesRead;
            while ((bytesRead = inputStream.read(buffer)) != -1) {
                outputStream.write(buffer, 0, bytesRead);
            }
            outputStream.flush();

            return tempFile;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            try {
                if (inputStream != null) inputStream.close();
            } catch (Exception ignored) {}
            try {
                if (outputStream != null) outputStream.close();
            } catch (Exception ignored) {}
        }
    }
    
    /**
     * 从Uri获取文件名
     */
    private String getFileNameFromUri(Uri uri) {
        String fileName = null;
        String scheme = uri.getScheme();
        
        if (android.content.ContentResolver.SCHEME_CONTENT.equals(scheme)) {
            android.database.Cursor cursor = getContentResolver().query(uri, null, null, null, null);
            if (cursor != null) {
                try {
                    int nameIndex = cursor.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME);
                    if (cursor.moveToFirst() && nameIndex >= 0) {
                        fileName = cursor.getString(nameIndex);
                    }
                } finally {
                    cursor.close();
                }
            }
        } else if (android.content.ContentResolver.SCHEME_FILE.equals(scheme)) {
            fileName = uri.getLastPathSegment();
        }
        
        return fileName;
    }

    private void publishThread() {
        String subject = titleInput.getText().toString().trim();
        String message = contentInput.getText().toString().trim();

        // 验证输入
        if (subject.isEmpty()) {
            Toast.makeText(this, "请输入标题", Toast.LENGTH_SHORT).show();
            titleInput.requestFocus();
            return;
        }

        if (message.isEmpty() && uploadedAttachments.isEmpty() && uploadedFiles.isEmpty()) {
            Toast.makeText(this, "请输入内容", Toast.LENGTH_SHORT).show();
            contentInput.requestFocus();
            return;
        }

        if (fid <= 0) {
            Toast.makeText(this, "请选择版块", Toast.LENGTH_SHORT).show();
            return;
        }

        // 获取参数
        String formhashValue = params != null ? params.getFormhash() : null;
        if (formhashValue == null) {
            formhashValue = forumApi.getFormhash();
        }
        final String formhash = formhashValue;

        // 收集附件ID（图片和文件）
        List<Integer> attachIds = new ArrayList<>();
        for (UploadedAttachment att : uploadedAttachments) {
            attachIds.add(att.aid);
        }
        for (UploadedAttachment att : uploadedFiles) {
            attachIds.add(att.aid);
        }
        
        // 读取权限设置和高级选项
        ForumApi.ThreadPostOptions options = readPostOptions();

        // 显示进度
        setProgressVisible(true);
        publishButton.setEnabled(false);

        // 提交发帖
        executor.execute(() -> {
            ApiResponse<ForumApi.NewThreadResult> response = forumApi.postNewThread(
                fid, subject, message, formhash, attachIds, options);

            mainHandler.post(() -> {
                setProgressVisible(false);
                publishButton.setEnabled(true);

                if (response.isSuccess() && response.getData() != null) {
                    ForumApi.NewThreadResult result = response.getData();
                    if (result.isSuccess()) {
                        Toast.makeText(this, "发帖成功", Toast.LENGTH_SHORT).show();
                        
                        // 返回结果
                        Intent resultIntent = new Intent();
                        resultIntent.putExtra("tid", result.getTid());
                        setResult(RESULT_OK, resultIntent);
                        finish();
                    } else {
                        // 显示错误对话框
                        showErrorDialog("发帖失败", result.getMessage());
                    }
                } else {
                    // 显示错误对话框
                    showErrorDialog("发帖失败", response.getMessage());
                }
            });
        });
    }
    
    /**
     * 显示错误对话框
     */
    private void showErrorDialog(String title, String message) {
        new AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("确定", null)
            .show();
    }
    
    /**
     * 读取发帖选项（权限和高级设置）
     */
    private ForumApi.ThreadPostOptions readPostOptions() {
        ForumApi.ThreadPostOptions options = new ForumApi.ThreadPostOptions();
        
        // 读取权限设置
        if (permissionPanel != null) {
            // 帖子售价
            EditText priceInput = permissionPanel.findViewById(R.id.priceInput);
            if (priceInput != null) {
                String priceStr = priceInput.getText().toString().trim();
                if (!priceStr.isEmpty()) {
                    try {
                        int price = Integer.parseInt(priceStr);
                        // 检查最大售价限制
                        int maxPrice = params != null ? params.getMaxPrice() : 0;
                        if (maxPrice > 0 && price > maxPrice) {
                            price = maxPrice;
                        }
                        options.setPrice(price);
                    } catch (NumberFormatException e) {
                        // 忽略无效输入
                    }
                }
            }
        }
        
        // 读取高级选项
        if (advancedPanel != null) {
            // 回帖仅作者可见
            androidx.appcompat.widget.SwitchCompat hiddenRepliesSwitch = 
                advancedPanel.findViewById(R.id.hiddenRepliesSwitch);
            if (hiddenRepliesSwitch != null) {
                options.setHiddenReplies(hiddenRepliesSwitch.isChecked());
            }
            
            // 回帖倒序排列
            androidx.appcompat.widget.SwitchCompat orderTypeSwitch = 
                advancedPanel.findViewById(R.id.orderTypeSwitch);
            if (orderTypeSwitch != null) {
                options.setOrderType(orderTypeSwitch.isChecked());
            }
            
            // 接收回复通知
            androidx.appcompat.widget.SwitchCompat allowNoticeSwitch = 
                advancedPanel.findViewById(R.id.allowNoticeSwitch);
            if (allowNoticeSwitch != null) {
                options.setAllowNoticeAuthor(allowNoticeSwitch.isChecked());
            }
            
            // 使用个人签名
            androidx.appcompat.widget.SwitchCompat useSigSwitch = 
                advancedPanel.findViewById(R.id.useSigSwitch);
            if (useSigSwitch != null) {
                options.setUseSig(useSigSwitch.isChecked());
            }
        }
        
        return options;
    }

    private void setProgressVisible(boolean visible) {
        // 可以添加进度对话框
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null) {
            executor.shutdown();
        }
    }

    /**
     * 已上传的附件
     */
    private static class UploadedAttachment {
        int aid;
        Uri localUri;
        String remoteUrl;
        String attachCode;
        String fileName; // 附件文件名

        UploadedAttachment(int aid, Uri localUri, String remoteUrl, String attachCode) {
            this.aid = aid;
            this.localUri = localUri;
            this.remoteUrl = remoteUrl;
            this.attachCode = attachCode;
        }
    }

    /**
     * 图片预览适配器
     */
    private static class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.ViewHolder> {

        private final Context context;
        private final List<UploadedAttachment> attachments;
        private final OnRemoveListener removeListener;
        private final OnInsertListener insertListener;

        interface OnRemoveListener {
            void onRemove(int position);
        }
        
        interface OnInsertListener {
            void onInsert(int position);
        }

        ImagePreviewAdapter(Context context, List<UploadedAttachment> attachments, 
                           OnRemoveListener removeListener, OnInsertListener insertListener) {
            this.context = context;
            this.attachments = attachments;
            this.removeListener = removeListener;
            this.insertListener = insertListener;
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(context)
                .inflate(R.layout.item_image_preview, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            UploadedAttachment attachment = attachments.get(position);
            
            // 加载图片
            com.bumptech.glide.Glide.with(context)
                .load(attachment.localUri)
                .centerCrop()
                .into(holder.imageView);

            // 删除按钮
            holder.removeButton.setOnClickListener(v -> {
                if (removeListener != null) {
                    int adapterPosition = holder.getAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        removeListener.onRemove(adapterPosition);
                    }
                }
            });
            
            // 插入按钮 - 上传成功后显示
            boolean uploadSuccess = attachment.attachCode != null 
                && !attachment.attachCode.isEmpty() 
                && attachment.aid > 0;
            
            if (uploadSuccess) {
                holder.insertButton.setVisibility(View.VISIBLE);
                holder.insertButton.setOnClickListener(v -> {
                    if (insertListener != null) {
                        int adapterPosition = holder.getAdapterPosition();
                        if (adapterPosition != RecyclerView.NO_POSITION) {
                            insertListener.onInsert(adapterPosition);
                        }
                    }
                });
            } else {
                holder.insertButton.setVisibility(View.GONE);
            }
        }

        @Override
        public int getItemCount() {
            return attachments.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            View removeButton;
            TextView insertButton;

            ViewHolder(View itemView) {
                super(itemView);
                imageView = itemView.findViewById(R.id.imagePreview);
                removeButton = itemView.findViewById(R.id.deleteButton);
                insertButton = itemView.findViewById(R.id.insertButton);
            }
        }
    }
    
    /**
     * 附件预览适配器
     */
    private static class AttachmentPreviewAdapter extends RecyclerView.Adapter<AttachmentPreviewAdapter.ViewHolder> {

        private final Context context;
        private final List<UploadedAttachment> attachments;
        private final OnRemoveListener removeListener;
        private final OnInsertListener insertListener;

        interface OnRemoveListener {
            void onRemove(int position);
        }
        
        interface OnInsertListener {
            void onInsert(int position);
        }

        AttachmentPreviewAdapter(Context context, List<UploadedAttachment> attachments, 
                                OnRemoveListener removeListener, OnInsertListener insertListener) {
            this.context = context;
            this.attachments = attachments;
            this.removeListener = removeListener;
            this.insertListener = insertListener;
        }

        @Override
        public ViewHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
            View view = android.view.LayoutInflater.from(context)
                .inflate(R.layout.item_attachment_preview, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(ViewHolder holder, int position) {
            UploadedAttachment attachment = attachments.get(position);
            
            // 设置文件名
            String fileName = attachment.fileName != null ? attachment.fileName : "附件_" + attachment.aid;
            holder.fileName.setText(fileName);

            // 插入按钮
            holder.insertButton.setOnClickListener(v -> {
                if (insertListener != null) {
                    int adapterPosition = holder.getAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        insertListener.onInsert(adapterPosition);
                    }
                }
            });

            // 删除按钮
            holder.deleteButton.setOnClickListener(v -> {
                if (removeListener != null) {
                    int adapterPosition = holder.getAdapterPosition();
                    if (adapterPosition != RecyclerView.NO_POSITION) {
                        removeListener.onRemove(adapterPosition);
                    }
                }
            });
        }

        @Override
        public int getItemCount() {
            return attachments.size();
        }

        static class ViewHolder extends RecyclerView.ViewHolder {
            TextView fileName;
            TextView insertButton;
            ImageView deleteButton;

            ViewHolder(View itemView) {
                super(itemView);
                fileName = itemView.findViewById(R.id.fileName);
                insertButton = itemView.findViewById(R.id.insertButton);
                deleteButton = itemView.findViewById(R.id.deleteButton);
            }
        }
    }
}
