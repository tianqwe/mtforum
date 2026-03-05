package com.forum.mt.ui;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.forum.mt.R;
import com.forum.mt.api.ForumApi;
import com.forum.mt.api.HttpClient;
import com.forum.mt.databinding.FragmentNotificationListBinding;
import com.forum.mt.model.Notification;
import com.forum.mt.model.ApiResponse;
import com.forum.mt.ui.adapter.NotificationAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 通知列表Fragment
 */
public class NotificationListFragment extends Fragment implements NotificationAdapter.OnNotificationClickListener {
    
    private FragmentNotificationListBinding binding;
    private NotificationAdapter adapter;
    private ForumApi forumApi;
    private ExecutorService executor;
    
    // 分页相关
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private final List<Notification> allNotifications = new ArrayList<>();
    private String currentType = ""; // 空表示全部
    
    public static NotificationListFragment newInstance() {
        return new NotificationListFragment();
    }
    
    public static NotificationListFragment newInstance(String type) {
        NotificationListFragment fragment = new NotificationListFragment();
        Bundle args = new Bundle();
        args.putString("type", type);
        fragment.setArguments(args);
        return fragment;
    }
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        executor = Executors.newSingleThreadExecutor();
        forumApi = new ForumApi(
            HttpClient.getInstance(requireContext()).getOkHttpClient(),
            HttpClient.getInstance(requireContext()).getCookieManager()
        );
        
        if (getArguments() != null) {
            currentType = getArguments().getString("type", "");
        }
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentNotificationListBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupRecyclerView();
        setupSwipeRefresh();
        
        // 加载数据
        loadData();
    }
    
    private void setupRecyclerView() {
        adapter = new NotificationAdapter();
        adapter.setOnNotificationClickListener(this);
        
        binding.recyclerView.setLayoutManager(new LinearLayoutManager(requireContext()));
        binding.recyclerView.setAdapter(adapter);
        
        // 滚动监听 - 加载更多
        binding.recyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                super.onScrolled(recyclerView, dx, dy);
                LinearLayoutManager layoutManager = (LinearLayoutManager) recyclerView.getLayoutManager();
                if (layoutManager != null) {
                    int lastVisiblePosition = layoutManager.findLastVisibleItemPosition();
                    int totalItemCount = adapter.getItemCount();
                    
                    // 滚动到倒数第3个时开始加载更多
                    if (lastVisiblePosition >= totalItemCount - 3 && !isLoading && hasMore) {
                        loadMore();
                    }
                }
            }
        });
    }
    
    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(
            com.google.android.material.R.color.design_default_color_primary
        );
        binding.swipeRefresh.setOnRefreshListener(this::refreshData);
    }
    
    private void loadData() {
        currentPage = 1;
        hasMore = true;
        allNotifications.clear();
        
        showLoading();
        
        executor.execute(() -> {
            ApiResponse<ForumApi.NotificationListResult> response = 
                forumApi.getNoticeList(currentType, currentPage);
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    hideLoading();
                    
                    if (response.isSuccess() && response.hasData()) {
                        ForumApi.NotificationListResult result = response.getData();
                        allNotifications.clear();
                        allNotifications.addAll(result.getNotifications());
                        adapter.submitList(new ArrayList<>(allNotifications));
                        
                        if (result.getCurrentPage() >= result.getTotalPages()) {
                            hasMore = false;
                        }
                        
                        updateEmptyView();
                    } else {
                        showError(response.getMessage());
                    }
                });
            }
        });
    }
    
    private void refreshData() {
        currentPage = 1;
        hasMore = true;
        
        executor.execute(() -> {
            ApiResponse<ForumApi.NotificationListResult> response = 
                forumApi.getNoticeList(currentType, currentPage);
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    hideLoading();
                    
                    if (response.isSuccess() && response.hasData()) {
                        ForumApi.NotificationListResult result = response.getData();
                        allNotifications.clear();
                        allNotifications.addAll(result.getNotifications());
                        adapter.submitList(new ArrayList<>(allNotifications));
                        
                        if (result.getCurrentPage() >= result.getTotalPages()) {
                            hasMore = false;
                        }
                        
                        updateEmptyView();
                    } else {
                        showError(response.getMessage());
                    }
                });
            }
        });
    }
    
    private void loadMore() {
        if (isLoading || !hasMore) return;
        
        isLoading = true;
        
        executor.execute(() -> {
            currentPage++;
            ApiResponse<ForumApi.NotificationListResult> response = 
                forumApi.getNoticeList(currentType, currentPage);
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    isLoading = false;
                    
                    if (response.isSuccess() && response.hasData()) {
                        ForumApi.NotificationListResult result = response.getData();
                        List<Notification> newNotifications = result.getNotifications();
                        
                        if (newNotifications.isEmpty()) {
                            hasMore = false;
                        } else {
                            allNotifications.addAll(newNotifications);
                            adapter.submitList(new ArrayList<>(allNotifications));
                            
                            if (result.getCurrentPage() >= result.getTotalPages()) {
                                hasMore = false;
                            }
                        }
                    } else {
                        hasMore = false;
                        currentPage--;
                    }
                });
            }
        });
    }
    
    private void showLoading() {
        binding.swipeRefresh.setRefreshing(true);
    }
    
    private void hideLoading() {
        binding.swipeRefresh.setRefreshing(false);
    }
    
    private void updateEmptyView() {
        binding.emptyView.setVisibility(allNotifications.isEmpty() ? View.VISIBLE : View.GONE);
    }
    
    private void showError(String message) {
        binding.emptyView.setVisibility(View.VISIBLE);
        android.widget.Toast.makeText(requireContext(), 
            message != null ? message : "加载失败", 
            android.widget.Toast.LENGTH_SHORT).show();
    }
    
    // NotificationAdapter.OnNotificationClickListener 实现
    @Override
    public void onNotificationClick(Notification notification) {
        // 根据通知类型跳转到不同页面
        String url = notification.getUrl();
        if (url != null && !url.isEmpty()) {
            // 跳转到相关帖子
            if (url.contains("tid=") || url.contains("thread-")) {
                // 解析帖子ID
                int tid = parseTidFromUrl(url);
                if (tid > 0) {
                    PostDetailActivity.startWithBasicInfo(requireContext(), 
                        tid, 
                        notification.getContent(),
                        null, null, null, null);
                }
            }
        }
    }
    
    private int parseTidFromUrl(String url) {
        try {
            // 格式: thread-{tid}-1-1.html 或 ?tid={tid}
            if (url.contains("thread-")) {
                String[] parts = url.split("thread-");
                if (parts.length > 1) {
                    String tidStr = parts[1].split("-")[0];
                    return Integer.parseInt(tidStr);
                }
            } else if (url.contains("tid=")) {
                String[] parts = url.split("tid=");
                if (parts.length > 1) {
                    String tidStr = parts[1].split("&")[0];
                    return Integer.parseInt(tidStr);
                }
            }
        } catch (Exception ignored) {}
        return 0;
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
