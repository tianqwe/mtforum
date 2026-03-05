package com.forum.mt.ui;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.graphics.Typeface;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.annotation.Nullable;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.forum.mt.R;
import com.forum.mt.api.ForumApi;
import com.forum.mt.api.HttpClient;
import com.forum.mt.databinding.ActivityEditPostBinding;
import com.forum.mt.model.ApiResponse;
import com.forum.mt.model.Smiley;
import com.forum.mt.model.UploadResult;
import com.forum.mt.ui.adapter.SmileyAdapter;

import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 编辑帖子页面
 */
public class EditPostActivity extends AppCompatActivity {

    public static final String EXTRA_TID = "tid";
    public static final String EXTRA_FID = "fid";
    public static final String EXTRA_PID = "pid";
    public static final String EXTRA_TITLE = "title";
    public static final String EXTRA_CONTENT = "content";
    private static final int REQUEST_CODE_PICK_ATTACHMENT = 1003;

    private ActivityEditPostBinding binding;
    
    private int tid;
    private int fid;
    private int pid;
    private String originalTitle;
    private String originalContent;
    
    // 编辑数据
    private String formhash;
    private String posttime;
    private String uploadHash;
    private int currentUid = 0; // 当前用户ID
    
    // 已有附件列表
    private List<ForumApi.AttachmentInfo> existingAttachments = new ArrayList<>();
    // 新上传的图片列表（用于预览和插入）
    private List<NewUploadItem> newImageItems = new ArrayList<>();
    // 新上传的附件列表（非图片）
    private List<NewUploadItem> newAttachItems = new ArrayList<>();
    
    // 新上传项数据类
    private static class NewUploadItem {
        Uri localUri;      // 本地URI（用于预览）
        int aid;           // 附件ID
        String filename;   // 文件名
        boolean isImage;   // 是否图片
        
        NewUploadItem(Uri uri, int aid, String filename, boolean isImage) {
            this.localUri = uri;
            this.aid = aid;
            this.filename = filename;
            this.isImage = isImage;
        }
    }
    
    // 面板视图
    private LinearLayout panelContainer;
    private View smileyPanel;
    private View insertPanel;
    
    // 当前显示的面板类型
    private static final int PANEL_NONE = 0;
    private static final int PANEL_SMILEY = 1;
    private static final int PANEL_INSERT = 2;
    private int currentPanel = PANEL_NONE;
    private boolean smileyAdapterInitialized = false;
    private boolean insertPanelListenersInitialized = false;

    // 图片选择器
    private ActivityResultLauncher<Intent> imagePickerLauncher;
    // 附件选择器
    private ActivityResultLauncher<Intent> attachPickerLauncher;
    
    private ForumApi forumApi;
    private ExecutorService executor;
    private Handler mainHandler;

    public static void start(Context context, int tid, int fid, int pid, String title, String content) {
        Intent intent = new Intent(context, EditPostActivity.class);
        intent.putExtra(EXTRA_TID, tid);
        intent.putExtra(EXTRA_FID, fid);
        intent.putExtra(EXTRA_PID, pid);
        intent.putExtra(EXTRA_TITLE, title);
        intent.putExtra(EXTRA_CONTENT, content);
        context.startActivity(intent);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityEditPostBinding.inflate(LayoutInflater.from(this));
        setContentView(binding.getRoot());

        // 获取传入的参数
        tid = getIntent().getIntExtra(EXTRA_TID, 0);
        fid = getIntent().getIntExtra(EXTRA_FID, 0);
        pid = getIntent().getIntExtra(EXTRA_PID, 0);
        originalTitle = getIntent().getStringExtra(EXTRA_TITLE);
        originalContent = getIntent().getStringExtra(EXTRA_CONTENT);

        // 只检查 tid 和 pid，fid 可以从编辑页面获取
        if (tid == 0 || pid == 0) {
            Toast.makeText(this, "参数错误: tid=" + tid + ", pid=" + pid, Toast.LENGTH_LONG).show();
            finish();
            return;
        }

        // 初始化
        initData();
        initViews();
        setupListeners();

        // 如果有传入的内容，直接显示
        if (originalTitle != null) {
            binding.titleInput.setText(originalTitle);
        }
        if (originalContent != null) {
            binding.contentInput.setText(originalContent);
        }
        
        // 加载编辑数据（获取formhash等）
        loadEditData();
    }
    
    private void initData() {
        HttpClient httpClient = HttpClient.getInstance(this);
        forumApi = new ForumApi(httpClient.getOkHttpClient(), httpClient.getCookieManager());
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
        
        // 初始化图片选择器
        imagePickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        handleImageSelection(result.getData());
                    }
                });
        
        // 初始化附件选择器
        attachPickerLauncher = registerForActivityResult(
                new ActivityResultContracts.StartActivityForResult(),
                result -> {
                    if (result.getResultCode() == Activity.RESULT_OK && result.getData() != null) {
                        handleAttachmentSelection(result.getData());
                    }
                });
    }

    private void initViews() {
        // 获取面板容器引用
        panelContainer = findViewById(R.id.panelContainer);
        smileyPanel = findViewById(R.id.smileyPanel);
        insertPanel = findViewById(R.id.insertPanel);
    }

    private void setupListeners() {
        // 返回按钮
        binding.backButton.setOnClickListener(v -> {
            checkAndConfirmExit();
        });

        // 保存按钮
        binding.saveButton.setOnClickListener(v -> {
            savePost();
        });

        // 删除按钮
        binding.deleteButton.setOnClickListener(v -> {
            showDeleteConfirmDialog();
        });

        // 表情按钮
        binding.emojiButton.setOnClickListener(v -> togglePanel(PANEL_SMILEY));

        // 图片按钮
        binding.imageButton.setOnClickListener(v -> pickImage());

        // 附件按钮
        binding.attachButton.setOnClickListener(v -> pickAttachment());

        // @提及按钮
        binding.mentionButton.setOnClickListener(v -> insertText("@"));

        // 插入按钮
        binding.insertButton.setOnClickListener(v -> togglePanel(PANEL_INSERT));
        
        // 内容变化监听
        TextWatcher charCountWatcher = new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {}

            @Override
            public void afterTextChanged(Editable s) {
                updateCharCount();
            }
        };

        binding.titleInput.addTextChangedListener(charCountWatcher);
        binding.contentInput.addTextChangedListener(charCountWatcher);
        
        // 输入框焦点监听：当输入框获取焦点时自动隐藏面板
        View.OnFocusChangeListener focusChangeListener = (v, hasFocus) -> {
            if (hasFocus && currentPanel != PANEL_NONE) {
                hideAllPanels();
                currentPanel = PANEL_NONE;
            }
        };
        
        binding.titleInput.setOnFocusChangeListener(focusChangeListener);
        binding.contentInput.setOnFocusChangeListener(focusChangeListener);
    }

    private void updateCharCount() {
        int titleLen = binding.titleInput.getText().length();
        int contentLen = binding.contentInput.getText().length();
        binding.charCount.setText((titleLen + contentLen) + "/10000");
    }

    /**
     * 切换面板
     */
    private void togglePanel(int panelType) {
        // 确保视图已初始化
        if (panelContainer == null) panelContainer = findViewById(R.id.panelContainer);
        if (smileyPanel == null) smileyPanel = findViewById(R.id.smileyPanel);
        if (insertPanel == null) insertPanel = findViewById(R.id.insertPanel);

        // 如果点击的是当前显示的面板，则隐藏
        if (currentPanel == panelType) {
            hideAllPanels();
            currentPanel = PANEL_NONE;
            showKeyboard();
            return;
        }

        // 显示面板前，先隐藏键盘
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
        }

        currentPanel = panelType;
    }
    
    /**
     * 隐藏键盘
     */
    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view == null) {
            view = binding.contentInput;
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
        binding.contentInput.requestFocus();
        android.view.inputmethod.InputMethodManager imm = (android.view.inputmethod.InputMethodManager)
                getSystemService(Context.INPUT_METHOD_SERVICE);
        if (imm != null) {
            imm.showSoftInput(binding.contentInput, android.view.inputmethod.InputMethodManager.SHOW_IMPLICIT);
        }
    }
    
    private void hideAllPanelsInternal() {
        if (smileyPanel != null) smileyPanel.setVisibility(View.GONE);
        if (insertPanel != null) insertPanel.setVisibility(View.GONE);
    }

    private void hideAllPanels() {
        if (smileyPanel == null) smileyPanel = findViewById(R.id.smileyPanel);
        if (insertPanel == null) insertPanel = findViewById(R.id.insertPanel);
        if (panelContainer == null) panelContainer = findViewById(R.id.panelContainer);

        if (smileyPanel != null) smileyPanel.setVisibility(View.GONE);
        if (insertPanel != null) insertPanel.setVisibility(View.GONE);
        if (panelContainer != null) panelContainer.setVisibility(View.GONE);
    }

    /**
     * 设置表情面板
     */
    private void setupSmileyPanel() {
        if (smileyPanel == null) {
            smileyPanel = findViewById(R.id.smileyPanel);
        }
        if (smileyPanel == null || smileyAdapterInitialized) return;
        
        // 获取视图
        RecyclerView smileyGrid = smileyPanel.findViewById(R.id.smileyGrid);
        View deleteBtn = smileyPanel.findViewById(R.id.deleteButton);
        TextView tabTb = smileyPanel.findViewById(R.id.tabTb);
        TextView tabQQ = smileyPanel.findViewById(R.id.tabQQ);
        TextView tabDoge = smileyPanel.findViewById(R.id.tabDoge);
        
        if (smileyGrid == null) return;
        
        // 设置网格布局
        if (smileyGrid.getLayoutManager() == null) {
            smileyGrid.setLayoutManager(new GridLayoutManager(this, 7));
        }
        
        // 创建适配器
        SmileyAdapter adapter = new SmileyAdapter(new SmileyAdapter.OnSmileyClickListener() {
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
                currentTab[0].setTypeface(null, Typeface.NORMAL);
            }
            clickedTab.setTextColor(getResources().getColor(R.color.text_dark));
            clickedTab.setTypeface(null, Typeface.BOLD);
            
            // 更新当前标签和分类
            currentTab[0] = clickedTab;
            currentCategory[0] = newCategory;
            
            // 加载对应分类的表情
            List<Smiley> smileys = Smiley.getSmileysByCategory(newCategory);
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
            tabTb.setTypeface(null, Typeface.BOLD);
        }

        // 删除按钮
        if (deleteBtn != null) {
            deleteBtn.setOnClickListener(v -> deleteLastCharacter());
        }
        
        smileyAdapterInitialized = true;
    }
    
    /**
     * 插入表情
     */
    private void insertSmiley(Smiley smiley) {
        if (smiley == null) return;
        String code = smiley.getCode();
        if (code == null || code.isEmpty()) return;
        insertText(code);
    }
    
    /**
     * 删除最后一个字符（智能识别表情代码）
     */
    private void deleteLastCharacter() {
        Editable editable = binding.contentInput.getText();
        int length = editable != null ? editable.length() : 0;
        
        if (length > 0) {
            String text = editable.toString();
            int lastOpenBracket = text.lastIndexOf("[");
            
            // 检查是否是表情代码 [xxx] 或 [#xxx] 或 [doge]
            if (lastOpenBracket >= 0 && lastOpenBracket < length) {
                int closeBracket = text.indexOf("]", lastOpenBracket);
                // 检查是否有完整的表情代码
                if (closeBracket > lastOpenBracket && closeBracket < length) {
                    String possibleSmiley = text.substring(lastOpenBracket, closeBracket + 1);
                    // 验证是否是有效表情格式
                    if (possibleSmiley.matches("\\[#?[^\\[\\]]+\\]")) {
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
     * 设置插入面板监听器
     */
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
                            tab.setTextColor(ContextCompat.getColor(this, R.color.text_gray));
                            tab.setBackgroundResource(R.drawable.bg_tab_unselected);
                        }
                    }
                    if (tabs[index] != null) {
                        tabs[index].setTextColor(ContextCompat.getColor(this, R.color.primary));
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
    
    /**
     * 插入文本到内容输入框
     */
    private void insertText(String text) {
        if (text == null || text.isEmpty()) return;
        try {
            int start = binding.contentInput.getSelectionStart();
            Editable editable = binding.contentInput.getText();
            if (editable != null) {
                editable.insert(Math.max(0, start), text);
            }
        } catch (Exception e) {
            // 忽略插入错误
        }
    }

    /**
     * 加载编辑数据
     */
    private void loadEditData() {
        showLoading(true);

        executor.execute(() -> {
            try {
                HttpClient httpClient = HttpClient.getInstance(this);
                ForumApi forumApi = new ForumApi(httpClient.getOkHttpClient(), httpClient.getCookieManager());
                
                ForumApi.EditPostData data = forumApi.getEditPostData(fid, tid, pid);
                
                mainHandler.post(() -> {
                    showLoading(false);
                    
                    if (data.isSuccess()) {
                        formhash = data.getFormhash();
                        posttime = data.getPosttime();
                        uploadHash = data.getUploadHash();
                        
                        // 从编辑页面获取 uid（用于上传）
                        if (data.getUid() > 0) {
                            currentUid = data.getUid();
                        }
                        
                        // 如果原始 fid 为 0，使用从编辑页面获取的 fid
                        if (fid == 0 && data.getFid() > 0) {
                            fid = data.getFid();
                        }
                        
                        // 更新标题和内容（如果服务端返回的数据更完整）
                        if (data.getSubject() != null && !data.getSubject().isEmpty()) {
                            binding.titleInput.setText(data.getSubject());
                            originalTitle = data.getSubject();
                        }
                        if (data.getContentMessage() != null && !data.getContentMessage().isEmpty()) {
                            binding.contentInput.setText(data.getContentMessage());
                            originalContent = data.getContentMessage();
                        }
                        
                        // 显示已有附件
                        if (data.getAttachments() != null && !data.getAttachments().isEmpty()) {
                            existingAttachments = data.getAttachments();
                            showExistingImages();
                        }
                        
                        binding.contentScrollView.setVisibility(View.VISIBLE);
                    } else {
                        Toast.makeText(this, data.getErrorMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    showLoading(false);
                    Toast.makeText(this, "加载失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * 显示已有图片
     */
    private void showExistingImages() {
        if (existingAttachments.isEmpty()) {
            binding.existingImageContainer.setVisibility(View.GONE);
            return;
        }

        binding.existingImageContainer.setVisibility(View.VISIBLE);
        binding.existingImageList.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        RecyclerView.Adapter<ExistingImageHolder> adapter = new RecyclerView.Adapter<ExistingImageHolder>() {
            @Override
            public ExistingImageHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_image_preview, parent, false);
                return new ExistingImageHolder(view);
            }

            @Override
            public void onBindViewHolder(ExistingImageHolder holder, int position) {
                ForumApi.AttachmentInfo attach = existingAttachments.get(position);
                
                // 加载图片预览
                Glide.with(holder.itemView.getContext())
                        .load(attach.getUrl())
                        .centerCrop()
                        .placeholder(R.drawable.ic_image_placeholder)
                        .into(holder.imagePreview);
                
                // 显示插入按钮
                holder.insertButton.setVisibility(View.VISIBLE);
                
                // 插入按钮点击
                holder.insertButton.setOnClickListener(v -> {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        insertImageTag(existingAttachments.get(pos).getAid());
                    }
                });
                
                // 删除按钮点击（已有附件只从列表移除，不从服务器删除）
                holder.deleteButton.setOnClickListener(v -> {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        existingAttachments.remove(pos);
                        notifyItemRemoved(pos);
                        updateExistingImageVisibility();
                    }
                });
            }

            @Override
            public int getItemCount() {
                return existingAttachments.size();
            }
        };

        binding.existingImageList.setAdapter(adapter);
    }
    
    private void updateExistingImageVisibility() {
        if (existingAttachments.isEmpty()) {
            binding.existingImageContainer.setVisibility(View.GONE);
        }
    }
    
    // 已有图片预览ViewHolder
    private static class ExistingImageHolder extends RecyclerView.ViewHolder {
        ImageView imagePreview;
        View deleteButton;
        TextView insertButton;
        
        ExistingImageHolder(View itemView) {
            super(itemView);
            imagePreview = itemView.findViewById(R.id.imagePreview);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            insertButton = itemView.findViewById(R.id.insertButton);
        }
    }

    /**
     * 插入图片标签
     */
    private void insertImageTag(int aid) {
        String tag = "[attachimg]" + aid + "[/attachimg]";
        insertText(tag);
    }

    /**
     * 选择图片
     */
    private void pickImage() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("image/*");
        intent.putExtra(Intent.EXTRA_ALLOW_MULTIPLE, true);
        imagePickerLauncher.launch(Intent.createChooser(intent, "选择图片"));
    }
    
    /**
     * 选择附件
     */
    private void pickAttachment() {
        Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
        intent.setType("*/*");
        attachPickerLauncher.launch(Intent.createChooser(intent, "选择附件"));
    }

    /**
     * 处理图片选择
     */
    private void handleImageSelection(Intent data) {
        List<Uri> selectedUris = new ArrayList<>();
        
        if (data.getClipData() != null) {
            // 多选
            int count = data.getClipData().getItemCount();
            for (int i = 0; i < count; i++) {
                selectedUris.add(data.getClipData().getItemAt(i).getUri());
            }
        } else if (data.getData() != null) {
            // 单选
            selectedUris.add(data.getData());
        }

        if (selectedUris.isEmpty()) {
            return;
        }

        // 上传图片
        for (Uri uri : selectedUris) {
            uploadImage(uri);
        }
    }
    
    /**
     * 处理附件选择
     */
    private void handleAttachmentSelection(Intent data) {
        Uri uri = data.getData();
        if (uri != null) {
            uploadAttachment(uri);
        }
    }

    /**
     * 上传图片
     */
    private void uploadImage(Uri uri) {
        if (uploadHash == null || uploadHash.isEmpty()) {
            Toast.makeText(this, "请等待页面加载完成", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (currentUid == 0) {
            Toast.makeText(this, "无法获取用户信息", Toast.LENGTH_SHORT).show();
            return;
        }

        Toast.makeText(this, "开始上传图片...", Toast.LENGTH_SHORT).show();

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
                ApiResponse<UploadResult> response = forumApi.uploadImage(imageFile, currentUid, uploadHash);
                
                mainHandler.post(() -> {
                    if (response.isSuccess() && response.getData() != null) {
                        UploadResult result = response.getData();
                        if (result.isSuccess()) {
                            // 添加到新上传图片列表（不自动插入）
                            NewUploadItem item = new NewUploadItem(uri, result.getAid(), imageFile.getName(), true);
                            newImageItems.add(item);
                            showNewImages();
                            
                            Toast.makeText(this, "图片上传成功，点击可插入正文", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "上传失败: " + result.getErrorMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        String msg = response.getMessage() != null ? response.getMessage() : "上传失败";
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    Toast.makeText(this, "上传出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    /**
     * 上传附件
     */
    private void uploadAttachment(Uri uri) {
        if (uploadHash == null || uploadHash.isEmpty()) {
            Toast.makeText(this, "请等待页面加载完成", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (currentUid == 0) {
            Toast.makeText(this, "无法获取用户信息", Toast.LENGTH_SHORT).show();
            return;
        }

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
                ApiResponse<UploadResult> response = forumApi.uploadAttachment(attachFile, currentUid, uploadHash, fid);
                
                mainHandler.post(() -> {
                    if (response.isSuccess() && response.getData() != null) {
                        UploadResult result = response.getData();
                        if (result.isSuccess()) {
                            // 添加到新上传附件列表（不自动插入）
                            NewUploadItem item = new NewUploadItem(uri, result.getAid(), attachFile.getName(), false);
                            newAttachItems.add(item);
                            showNewAttachments();
                            
                            Toast.makeText(this, "附件上传成功，点击可插入正文", Toast.LENGTH_SHORT).show();
                        } else {
                            Toast.makeText(this, "上传失败: " + result.getErrorMessage(), Toast.LENGTH_SHORT).show();
                        }
                    } else {
                        String msg = response.getMessage() != null ? response.getMessage() : "上传失败";
                        Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    Toast.makeText(this, "上传出错: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }
    
    /**
     * 将Uri转换为File
     */
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
            
            // 统一保存为JPEG格式
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

    /**
     * 显示新添加的图片
     */
    private void showNewImages() {
        if (newImageItems.isEmpty()) {
            binding.newImageContainer.setVisibility(View.GONE);
            return;
        }

        binding.newImageContainer.setVisibility(View.VISIBLE);
        binding.newImageList.setLayoutManager(
                new LinearLayoutManager(this, LinearLayoutManager.HORIZONTAL, false));

        RecyclerView.Adapter<NewImageHolder> adapter = new RecyclerView.Adapter<NewImageHolder>() {
            @Override
            public NewImageHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
                View view = LayoutInflater.from(parent.getContext())
                        .inflate(R.layout.item_image_preview, parent, false);
                return new NewImageHolder(view);
            }

            @Override
            public void onBindViewHolder(NewImageHolder holder, int position) {
                NewUploadItem item = newImageItems.get(position);
                
                // 加载图片预览
                Glide.with(holder.itemView.getContext())
                        .load(item.localUri)
                        .centerCrop()
                        .placeholder(R.drawable.ic_image_placeholder)
                        .into(holder.imagePreview);
                
                // 显示插入按钮
                holder.insertButton.setVisibility(View.VISIBLE);
                
                // 插入按钮点击
                holder.insertButton.setOnClickListener(v -> {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        insertImageTag(newImageItems.get(pos).aid);
                    }
                });
                
                // 删除按钮点击
                holder.deleteButton.setOnClickListener(v -> {
                    int pos = holder.getAdapterPosition();
                    if (pos != RecyclerView.NO_POSITION) {
                        newImageItems.remove(pos);
                        notifyItemRemoved(pos);
                        updateNewImageVisibility();
                    }
                });
            }

            @Override
            public int getItemCount() {
                return newImageItems.size();
            }
        };

        binding.newImageList.setAdapter(adapter);
    }
    
    private void updateNewImageVisibility() {
        if (newImageItems.isEmpty()) {
            binding.newImageContainer.setVisibility(View.GONE);
        }
    }
    
    /**
     * 显示新添加的附件
     */
    private void showNewAttachments() {
        if (newAttachItems.isEmpty()) {
            if (binding.newAttachContainer != null) {
                binding.newAttachContainer.setVisibility(View.GONE);
            }
            return;
        }

        if (binding.newAttachContainer != null) {
            binding.newAttachContainer.setVisibility(View.VISIBLE);
            binding.newAttachList.setLayoutManager(new LinearLayoutManager(this));

            RecyclerView.Adapter<NewAttachHolder> adapter = new RecyclerView.Adapter<NewAttachHolder>() {
                @Override
                public NewAttachHolder onCreateViewHolder(android.view.ViewGroup parent, int viewType) {
                    View view = LayoutInflater.from(parent.getContext())
                            .inflate(R.layout.item_attachment_preview, parent, false);
                    return new NewAttachHolder(view);
                }

                @Override
                public void onBindViewHolder(NewAttachHolder holder, int position) {
                    NewUploadItem item = newAttachItems.get(position);
                    
                    // 显示文件名
                    holder.fileName.setText(item.filename);
                    
                    // 插入按钮点击
                    holder.insertButton.setOnClickListener(v -> {
                        int pos = holder.getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            insertText("[attach]" + newAttachItems.get(pos).aid + "[/attach]");
                        }
                    });
                    
                    // 删除按钮点击
                    holder.deleteButton.setOnClickListener(v -> {
                        int pos = holder.getAdapterPosition();
                        if (pos != RecyclerView.NO_POSITION) {
                            newAttachItems.remove(pos);
                            notifyItemRemoved(pos);
                            updateNewAttachVisibility();
                        }
                    });
                }

                @Override
                public int getItemCount() {
                    return newAttachItems.size();
                }
            };

            binding.newAttachList.setAdapter(adapter);
        }
    }
    
    private void updateNewAttachVisibility() {
        if (binding.newAttachContainer != null && newAttachItems.isEmpty()) {
            binding.newAttachContainer.setVisibility(View.GONE);
        }
    }
    
    // 新图片预览ViewHolder
    private static class NewImageHolder extends RecyclerView.ViewHolder {
        ImageView imagePreview;
        View deleteButton;
        TextView insertButton;
        
        NewImageHolder(View itemView) {
            super(itemView);
            imagePreview = itemView.findViewById(R.id.imagePreview);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            insertButton = itemView.findViewById(R.id.insertButton);
        }
    }
    
    // 新附件预览ViewHolder
    private static class NewAttachHolder extends RecyclerView.ViewHolder {
        TextView fileName;
        TextView insertButton;
        View deleteButton;
        
        NewAttachHolder(View itemView) {
            super(itemView);
            fileName = itemView.findViewById(R.id.fileName);
            insertButton = itemView.findViewById(R.id.insertButton);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
    }

    /**
     * 保存帖子
     */
    private void savePost() {
        String title = binding.titleInput.getText().toString().trim();
        String content = binding.contentInput.getText().toString().trim();

        if (content.isEmpty()) {
            Toast.makeText(this, "请输入内容", Toast.LENGTH_SHORT).show();
            return;
        }

        showLoading(true);

        // 收集所有新上传附件的ID
        List<Integer> attachIds = new ArrayList<>();
        for (NewUploadItem item : newImageItems) {
            attachIds.add(item.aid);
        }
        for (NewUploadItem item : newAttachItems) {
            attachIds.add(item.aid);
        }

        executor.execute(() -> {
            try {
                HttpClient httpClient = HttpClient.getInstance(this);
                ForumApi forumApi = new ForumApi(httpClient.getOkHttpClient(), httpClient.getCookieManager());

                ApiResponse<Boolean> result = forumApi.editPost(fid, tid, pid, title, content, 
                        formhash, posttime, attachIds, 0);

                mainHandler.post(() -> {
                    showLoading(false);

                    if (result.isSuccess() && result.getData() != null && result.getData()) {
                        Toast.makeText(this, "保存成功", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    showLoading(false);
                    Toast.makeText(this, "保存失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    /**
     * 显示删除确认对话框
     */
    private void showDeleteConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("删除帖子")
                .setMessage("确定要删除这篇帖子吗？删除后无法恢复。")
                .setPositiveButton("删除", (dialog, which) -> deletePost())
                .setNegativeButton("取消", null)
                .show();
    }

    /**
     * 删除帖子
     */
    private void deletePost() {
        showLoading(true);

        executor.execute(() -> {
            try {
                HttpClient httpClient = HttpClient.getInstance(this);
                ForumApi forumApi = new ForumApi(httpClient.getOkHttpClient(), httpClient.getCookieManager());

                ApiResponse<Boolean> result = forumApi.deletePost(fid, tid, pid, formhash);

                mainHandler.post(() -> {
                    showLoading(false);

                    if (result.isSuccess() && result.getData() != null && result.getData()) {
                        Toast.makeText(this, "删除成功", Toast.LENGTH_SHORT).show();
                        setResult(RESULT_OK);
                        finish();
                    } else {
                        Toast.makeText(this, result.getMessage(), Toast.LENGTH_SHORT).show();
                    }
                });
            } catch (Exception e) {
                mainHandler.post(() -> {
                    showLoading(false);
                    Toast.makeText(this, "删除失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        });
    }

    private void showLoading(boolean show) {
        binding.loadingProgress.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.loadingText.setVisibility(show ? View.VISIBLE : View.GONE);
        binding.contentScrollView.setVisibility(show ? View.GONE : View.VISIBLE);
        binding.saveButton.setEnabled(!show);
        binding.deleteButton.setEnabled(!show);
    }

    /**
     * 检查是否有修改，确认是否退出
     */
    private void checkAndConfirmExit() {
        String currentTitle = binding.titleInput.getText().toString().trim();
        String currentContent = binding.contentInput.getText().toString().trim();

        boolean hasChanges = false;
        if (originalTitle != null && !originalTitle.equals(currentTitle)) {
            hasChanges = true;
        } else if (originalContent != null && !originalContent.equals(currentContent)) {
            hasChanges = true;
        } else if (!newImageItems.isEmpty() || !newAttachItems.isEmpty()) {
            hasChanges = true;
        }

        if (hasChanges) {
            new AlertDialog.Builder(this)
                    .setTitle("放弃修改")
                    .setMessage("有未保存的修改，确定要放弃吗？")
                    .setPositiveButton("放弃", (dialog, which) -> finish())
                    .setNegativeButton("取消", null)
                    .show();
        } else {
            finish();
        }
    }

    @Override
    public void onBackPressed() {
        if (currentPanel != PANEL_NONE) {
            hideAllPanels();
            currentPanel = PANEL_NONE;
            return;
        }
        checkAndConfirmExit();
    }
}