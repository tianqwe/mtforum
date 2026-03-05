package com.forum.mt.ui;

import android.content.Intent;
import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.View;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.lifecycle.ViewModelProvider;
import androidx.recyclerview.widget.LinearLayoutManager;

import com.forum.mt.R;
import com.forum.mt.databinding.ActivityBrowseHistoryBinding;
import com.forum.mt.model.BrowseHistory;
import com.forum.mt.ui.adapter.BrowseHistoryAdapter;
import com.forum.mt.ui.UserDetailActivity;
import com.forum.mt.viewmodel.BrowseHistoryViewModel;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Set;

/**
 * 浏览历史页面
 * 显示用户浏览过的帖子列表
 * 支持搜索、删除、清空等操作
 * 与应用整体风格保持一致
 */
public class BrowseHistoryActivity extends AppCompatActivity {

    private ActivityBrowseHistoryBinding binding;
    private BrowseHistoryViewModel viewModel;
    private BrowseHistoryAdapter adapter;
    private List<BrowseHistory> currentHistoryList = new ArrayList<>();
    
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        // 启用edge-to-edge显示
        androidx.core.view.WindowCompat.setDecorFitsSystemWindows(getWindow(), false);
        
        binding = ActivityBrowseHistoryBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // 根据当前主题设置状态栏图标颜色
        setupStatusBar();
        
        initViewModel();
        setupRecyclerView();
        setupSearch();
        setupClickListeners();
        observeViewModel();
    }
    
    /**
     * 初始化ViewModel
     */
    private void initViewModel() {
        viewModel = new ViewModelProvider(this)
                .get(BrowseHistoryViewModel.class);
    }
    
    /**
     * 设置RecyclerView
     */
    private void setupRecyclerView() {
        adapter = new BrowseHistoryAdapter();
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(this));
        binding.recyclerView.setAdapter(adapter);
        
        // 设置点击监听器
        adapter.setOnItemClickListener(new BrowseHistoryAdapter.OnItemClickListener() {
            @Override
            public void onItemClick(BrowseHistory history, int position) {
                // 跳转到帖子详情页
                PostDetailActivity.start(BrowseHistoryActivity.this, 
                        history.getTid(), 
                        history.getTitle());
            }
            
            @Override
            public void onDeleteClick(BrowseHistory history, int position) {
                // 显示删除确认对话框
                showDeleteConfirmDialog(history);
            }
        });
        
        // 设置用户点击监听器
        adapter.setOnUserClickListener((uid, username, avatar, avatarView) -> {
            UserDetailActivity.start(this, uid, username, avatar, avatarView);
        });
    }
    
    /**
     * 设置搜索功能
     */
    private void setupSearch() {
        binding.searchInput.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            
            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                // 显示或隐藏清空按钮
                binding.clearSearchButton.setVisibility(s.length() > 0 ? View.VISIBLE : View.GONE);
            }
            
            @Override
            public void afterTextChanged(Editable s) {
                // 执行搜索
                performSearch(s.toString());
            }
        });
        
        // 清空按钮点击事件
        binding.clearSearchButton.setOnClickListener(v -> {
            binding.searchInput.setText("");
            binding.searchInput.clearFocus();
        });
    }
    
    /**
     * 执行搜索
     */
    private void performSearch(String keyword) {
        if (keyword == null || keyword.trim().isEmpty()) {
            // 显示所有历史
            adapter.submitList(currentHistoryList);
            updateEmptyState(currentHistoryList.isEmpty(), false);
        } else {
            // 执行搜索
            List<BrowseHistory> results = new ArrayList<>();
            String lowerKeyword = keyword.toLowerCase().trim();
            
            for (BrowseHistory history : currentHistoryList) {
                if (history.getTitle() != null && history.getTitle().toLowerCase().contains(lowerKeyword)) {
                    results.add(history);
                } else if (history.getAuthor() != null && history.getAuthor().toLowerCase().contains(lowerKeyword)) {
                    results.add(history);
                } else if (history.getForumName() != null && history.getForumName().toLowerCase().contains(lowerKeyword)) {
                    results.add(history);
                }
            }
            
            adapter.submitList(results);
            updateEmptyState(results.isEmpty(), true);
        }
    }
    
    /**
     * 设置点击监听器
     */
    private void setupClickListeners() {
        // Toolbar返回按钮
        binding.toolbar.setNavigationOnClickListener(v -> {
            if (adapter.isSelectionMode()) {
                exitSelectionMode();
            } else {
                finish();
            }
        });
        
        // 清空历史按钮
        binding.clearAllFab.setOnClickListener(v -> {
            showClearAllConfirmDialog();
        });
        
        // 取消按钮
        binding.cancelButton.setOnClickListener(v -> {
            exitSelectionMode();
        });
        
        // 删除按钮
        binding.deleteButton.setOnClickListener(v -> {
            deleteSelectedItems();
        });
        
        // 下拉刷新
        binding.swipeRefresh.setOnRefreshListener(() -> {
            binding.swipeRefresh.setRefreshing(false);
        });
    }
    
    /**
     * 观察ViewModel的数据变化
     */
    private void observeViewModel() {
        // 观察所有浏览历史
        viewModel.getAllHistory().observe(this, histories -> {
            if (histories != null) {
                currentHistoryList.clear();
                currentHistoryList.addAll(histories);
                
                // 判断是否需要搜索
                String keyword = binding.searchInput.getText().toString();
                if (keyword.isEmpty()) {
                    adapter.submitList(histories);
                    updateEmptyState(histories.isEmpty(), false);
                } else {
                    performSearch(keyword);
                }
                
                // 根据历史记录数量显示/隐藏清空按钮
                binding.clearAllFab.setVisibility(histories.isEmpty() ? View.GONE : View.VISIBLE);
            }
        });
        
        // 观察消息
        viewModel.getMessage().observe(this, message -> {
            if (message != null && !message.isEmpty()) {
                Toast.makeText(this, message, Toast.LENGTH_SHORT).show();
                viewModel.clearMessage();
            }
        });
    }
    
    /**
     * 更新空状态显示
     * @param isEmpty 是否为空
     * @param isSearch 是否是搜索结果
     */
    private void updateEmptyState(boolean isEmpty, boolean isSearch) {
        if (isEmpty) {
            binding.recyclerView.setVisibility(View.GONE);
            binding.emptyLayout.setVisibility(View.VISIBLE);
            
            if (isSearch) {
                binding.emptyText.setText("未找到相关记录");
                binding.emptySubText.setText("试试其他关键词");
            } else {
                binding.emptyText.setText("暂无浏览历史");
                binding.emptySubText.setText("您浏览过的帖子会记录在这里");
            }
        } else {
            binding.recyclerView.setVisibility(View.VISIBLE);
            binding.emptyLayout.setVisibility(View.GONE);
        }
    }
    
    /**
     * 显示删除确认对话框
     */
    private void showDeleteConfirmDialog(BrowseHistory history) {
        new AlertDialog.Builder(this)
                .setTitle("删除浏览记录")
                .setMessage("确定要删除这条浏览记录吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    viewModel.deleteHistory(history);
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 显示清空所有历史确认对话框
     */
    private void showClearAllConfirmDialog() {
        new AlertDialog.Builder(this)
                .setTitle("清空浏览历史")
                .setMessage("确定要清空所有浏览历史吗？此操作不可撤销。")
                .setPositiveButton("清空", (dialog, which) -> {
                    viewModel.clearAllHistory();
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 删除选中的项目
     */
    private void deleteSelectedItems() {
        Set<Integer> selectedIds = adapter.getSelectedItems();
        
        if (selectedIds.isEmpty()) {
            Toast.makeText(this, "请先选择要删除的记录", Toast.LENGTH_SHORT).show();
            return;
        }
        
        // 从当前列表中找到选中的项目
        List<BrowseHistory> selectedHistories = new ArrayList<>();
        List<BrowseHistory> historyList = adapter.getHistoryList();
        
        for (BrowseHistory history : historyList) {
            if (selectedIds.contains(history.getTid())) {
                selectedHistories.add(history);
            }
        }
        
        // 显示确认对话框
        new AlertDialog.Builder(this)
                .setTitle("删除浏览记录")
                .setMessage("确定要删除选中的 " + selectedHistories.size() + " 条记录吗？")
                .setPositiveButton("删除", (dialog, which) -> {
                    viewModel.deleteMultipleHistory(selectedHistories);
                    exitSelectionMode();
                })
                .setNegativeButton("取消", null)
                .show();
    }
    
    /**
     * 退出选择模式
     */
    private void exitSelectionMode() {
        adapter.setSelectionMode(false);
        binding.bottomBar.setVisibility(View.GONE);
        if (!currentHistoryList.isEmpty()) {
            binding.clearAllFab.setVisibility(View.VISIBLE);
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
    public void onBackPressed() {
        if (adapter.isSelectionMode()) {
            exitSelectionMode();
        } else {
            super.onBackPressed();
        }
    }
}