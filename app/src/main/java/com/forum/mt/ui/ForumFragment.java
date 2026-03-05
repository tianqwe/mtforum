package com.forum.mt.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.app.ActivityOptionsCompat;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.forum.mt.R;
import com.forum.mt.api.ForumApi;
import com.forum.mt.api.HttpClient;
import com.forum.mt.model.ApiResponse;
import com.forum.mt.databinding.FragmentForumBinding;
import com.forum.mt.model.Forum;
import com.forum.mt.ui.adapter.ForumListAdapter;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 版块Fragment - 显示论坛板块列表
 */
public class ForumFragment extends Fragment implements ForumListAdapter.OnForumClickListener {
    private FragmentForumBinding binding;
    private ForumListAdapter adapter;
    private ForumApi forumApi;
    private ExecutorService executor;
    private List<Forum> allForums = new ArrayList<>();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        forumApi = new ForumApi(
            HttpClient.getInstance(requireContext()).getOkHttpClient(),
            HttpClient.getInstance(requireContext()).getCookieManager()
        );
        executor = Executors.newSingleThreadExecutor();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentForumBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        setupRecyclerView();
        setupSwipeRefresh();
        setupErrorRetry();
        loadData();
    }

    private void setupRecyclerView() {
        adapter = new ForumListAdapter();
        adapter.setOnForumClickListener(this);
        
        // 九宫格布局：每行3个
        GridLayoutManager layoutManager = new GridLayoutManager(requireContext(), 3);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                // 分类标题横跨整行（3列）
                if (adapter.getItemViewType(position) == Forum.TYPE_CATEGORY) {
                    return 3;
                }
                // 板块项占1列
                return 1;
            }
        });
        
        binding.recyclerView.setLayoutManager(layoutManager);
        binding.recyclerView.setAdapter(adapter);
        binding.recyclerView.setHasFixedSize(true);
    }

    private void setupSwipeRefresh() {
        binding.swipeRefresh.setColorSchemeResources(
                R.color.huluxia_primary,
                R.color.huluxia_accent,
                R.color.huluxia_orange
        );
        binding.swipeRefresh.setOnRefreshListener(this::refreshData);
    }

    private void setupErrorRetry() {
        binding.retryBtn.setOnClickListener(v -> loadData());
    }

    private void loadData() {
        showLoading();
        
        executor.execute(() -> {
            ApiResponse<List<Forum>> response = forumApi.getForumList();
            
            // 解析论坛统计信息
            String statsHtml = response.getRawHtml();
            int[] stats = parseForumStats(statsHtml);
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    hideLoading();
                    
                    // 更新统计信息
                    if (stats != null) {
                        updateStats(stats);
                    }
                    
                    if (response.isSuccess() && response.hasData()) {
                        allForums.clear();
                        allForums.addAll(response.getData());
                        adapter.setData(allForums);
                        
                        if (allForums.isEmpty()) {
                            showEmpty();
                        } else {
                            showContent();
                        }
                    } else {
                        showError(response.getMessage());
                    }
                });
            }
        });
    }

    private void refreshData() {
        executor.execute(() -> {
            ApiResponse<List<Forum>> response = forumApi.getForumList();
            
            // 解析论坛统计信息
            String statsHtml = response.getRawHtml();
            int[] stats = parseForumStats(statsHtml);
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    hideLoading();
                    
                    // 更新统计信息
                    if (stats != null) {
                        updateStats(stats);
                    }
                    
                    if (response.isSuccess() && response.hasData()) {
                        allForums.clear();
                        allForums.addAll(response.getData());
                        adapter.setData(allForums);
                        showContent();
                    } else {
                        if (allForums.isEmpty()) {
                            showError(response.getMessage());
                        }
                    }
                });
            }
        });
    }

    /**
     * 解析论坛统计信息
     * 格式: <li class="b_r"><span class="f_c">今日</span><em class="f_b">86</em></li>
     */
    private int[] parseForumStats(String html) {
        if (html == null || html.isEmpty()) {
            return null;
        }
        
        try {
            int[] stats = new int[4]; // 今日, 昨日, 帖子, 会员
            
            // 使用正则提取数字
            java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(
                    "<em class=\"f_b\">(\\d+)</em>"
            );
            java.util.regex.Matcher matcher = pattern.matcher(html);
            
            int index = 0;
            while (matcher.find() && index < 4) {
                stats[index] = Integer.parseInt(matcher.group(1));
                index++;
            }
            
            return index == 4 ? stats : null;
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * 更新统计信息显示
     */
    private void updateStats(int[] stats) {
        if (stats != null && stats.length >= 4) {
            binding.todayPostsCount.setText(formatNumber(stats[0]));
            binding.yesterdayPostsCount.setText(formatNumber(stats[1]));
            binding.totalPostsCount.setText(formatNumber(stats[2]));
            binding.totalMembersCount.setText(formatNumber(stats[3]));
        }
    }

    /**
     * 格式化数字显示
     */
    private String formatNumber(int num) {
        if (num >= 10000) {
            return String.format("%.1f万", num / 10000.0);
        }
        return String.valueOf(num);
    }

    @Override
    public void onForumClick(Forum forum, View iconView) {
        if (forum.isCategory()) {
            // 分类标题不响应点击
            return;
        }

        // 使用共享元素动画启动Activity，板块头像平移效果
        ActivityOptionsCompat options =
            ActivityOptionsCompat.makeSceneTransitionAnimation(
                requireActivity(),
                iconView,
                "forum_icon_transition"
            );

        // 跳转到板块帖子列表页面
        Intent intent = new Intent(requireContext(), ForumPostListActivity.class);
        intent.putExtra(ForumPostListActivity.EXTRA_FID, forum.getFid());
        intent.putExtra(ForumPostListActivity.EXTRA_FORUM_NAME, forum.getName());
        intent.putExtra(ForumPostListActivity.EXTRA_FORUM_ICON, forum.getIcon());
        startActivity(intent, options.toBundle());
    }

    private void showLoading() {
        binding.loadingLayout.setVisibility(View.VISIBLE);
        binding.errorLayout.setVisibility(View.GONE);
        binding.emptyLayout.setVisibility(View.GONE);
        binding.swipeRefresh.setVisibility(View.GONE);
    }

    private void hideLoading() {
        binding.loadingLayout.setVisibility(View.GONE);
        binding.swipeRefresh.setRefreshing(false);
    }

    private void showContent() {
        binding.errorLayout.setVisibility(View.GONE);
        binding.emptyLayout.setVisibility(View.GONE);
        binding.swipeRefresh.setVisibility(View.VISIBLE);
    }

    private void showError(String message) {
        binding.loadingLayout.setVisibility(View.GONE);
        binding.emptyLayout.setVisibility(View.GONE);
        binding.swipeRefresh.setVisibility(View.GONE);
        binding.errorLayout.setVisibility(View.VISIBLE);
        binding.errorText.setText(message != null ? message : "加载失败");
    }

    private void showEmpty() {
        binding.loadingLayout.setVisibility(View.GONE);
        binding.errorLayout.setVisibility(View.GONE);
        binding.swipeRefresh.setVisibility(View.GONE);
        binding.emptyLayout.setVisibility(View.VISIBLE);
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