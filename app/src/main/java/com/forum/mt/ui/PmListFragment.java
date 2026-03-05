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
import com.forum.mt.databinding.FragmentPmListBinding;
import com.forum.mt.model.PrivateMessage;
import com.forum.mt.model.ApiResponse;
import com.forum.mt.ui.adapter.MessageAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 私信列表Fragment
 */
public class PmListFragment extends Fragment implements MessageAdapter.OnMessageClickListener {
    
    private FragmentPmListBinding binding;
    private MessageAdapter adapter;
    private ForumApi forumApi;
    private ExecutorService executor;
    
    // 分页相关
    private int currentPage = 1;
    private boolean isLoading = false;
    private boolean hasMore = true;
    private final List<PrivateMessage> allMessages = new ArrayList<>();
    
    public static PmListFragment newInstance() {
        return new PmListFragment();
    }
    
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
        binding = FragmentPmListBinding.inflate(inflater, container, false);
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
        adapter = new MessageAdapter();
        adapter.setOnMessageClickListener(this);
        
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
        allMessages.clear();

        showLoading();

        executor.execute(() -> {
            ApiResponse<ForumApi.PmListResult> response = forumApi.getPmList(currentPage);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    hideLoading();

                    if (response.isSuccess() && response.hasData()) {
                        ForumApi.PmListResult result = response.getData();
                        allMessages.clear();

                        // 获取消息列表
                        java.util.List<PrivateMessage> messages = result.getMessages();
                        
                        if (messages != null && !messages.isEmpty()) {
                            allMessages.addAll(messages);
                            adapter.submitList(new ArrayList<>(allMessages));

                            // 更新是否有更多数据
                            if (result.getCurrentPage() >= result.getTotalPages()) {
                                hasMore = false;
                            }
                        } else {
                            // 消息列表为空
                            adapter.submitList(new ArrayList<>());
                        }

                        updateEmptyView();
                    } else {
                        // 请求失败或数据为空
                        android.widget.Toast.makeText(requireContext(), 
                            "加载失败: " + response.getMessage(), 
                            android.widget.Toast.LENGTH_LONG).show();
                        adapter.submitList(new ArrayList<>());
                        updateEmptyView();
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
            ApiResponse<ForumApi.PmListResult> response = forumApi.getPmList(currentPage);

            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    hideLoading();

                    if (response.isSuccess() && response.hasData()) {
                        ForumApi.PmListResult result = response.getData();
                        allMessages.clear();

                        java.util.List<PrivateMessage> messages = result.getMessages();
                        if (messages != null && !messages.isEmpty()) {
                            allMessages.addAll(messages);
                            adapter.submitList(new ArrayList<>(allMessages));

                            if (result.getCurrentPage() >= result.getTotalPages()) {
                                hasMore = false;
                            }
                        } else {
                            adapter.submitList(new ArrayList<>());
                        }

                        updateEmptyView();
                    } else {
                        adapter.submitList(new ArrayList<>());
                        updateEmptyView();
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
            ApiResponse<ForumApi.PmListResult> response = forumApi.getPmList(currentPage);
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    isLoading = false;
                    
                    if (response.isSuccess() && response.hasData()) {
                        ForumApi.PmListResult result = response.getData();
                        List<PrivateMessage> newMessages = result.getMessages();
                        
                        if (newMessages.isEmpty()) {
                            hasMore = false;
                        } else {
                            allMessages.addAll(newMessages);
                            adapter.submitList(new ArrayList<>(allMessages));
                            
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
        binding.emptyView.setVisibility(allMessages.isEmpty() ? View.VISIBLE : View.GONE);
    }
    
    private void showError(String message) {
        binding.emptyView.setVisibility(View.VISIBLE);
        android.widget.Toast.makeText(requireContext(), 
            message != null ? message : "加载失败", 
            android.widget.Toast.LENGTH_SHORT).show();
    }
    
    // MessageAdapter.OnMessageClickListener 实现
    @Override
    public void onMessageClick(PrivateMessage message) {
        // 跳转到私信详情页
        if (message != null) {
            int touid = message.getFromUid() > 0 ? message.getFromUid() : message.getToUid();
            if (touid > 0) {
                PmChatActivity.start(requireContext(),
                    touid,
                    message.getFromUsername(),
                    message.getAvatarUrl());
            } else {
                android.widget.Toast.makeText(requireContext(),
                    "无法获取用户ID",
                    android.widget.Toast.LENGTH_SHORT).show();
            }
        }
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
