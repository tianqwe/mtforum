package com.forum.mt.ui;

import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.viewpager2.widget.ViewPager2;

import com.forum.mt.R;
import com.forum.mt.api.ForumApi;
import com.forum.mt.api.HttpClient;
import com.forum.mt.databinding.FragmentMessageBinding;
import com.forum.mt.model.ApiResponse;
import com.forum.mt.ui.adapter.MessagePagerAdapter;
import com.google.android.material.tabs.TabLayout;
import com.google.android.material.tabs.TabLayoutMediator;

/**
 * 消息Fragment - 主消息中心页面
 * 包含私信和通知两个Tab
 * 支持新消息红点提示
 */
public class MessageFragment extends Fragment {
    
    private FragmentMessageBinding binding;
    private MessagePagerAdapter pagerAdapter;
    private ForumApi forumApi;
    
    // 新消息检查相关
    private Handler checkHandler;
    private static final long CHECK_INTERVAL = 30000; // 30秒检查一次新消息
    private boolean isChecking = false;
    private TabLayout.Tab pmTab;
    
    private final Runnable checkNewPmRunnable = new Runnable() {
        @Override
        public void run() {
            if (!isChecking || !isAdded()) return;
            checkNewPm();
            checkHandler.postDelayed(this, CHECK_INTERVAL);
        }
    };
    
    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        
        HttpClient httpClient = HttpClient.getInstance(requireContext());
        forumApi = new ForumApi(httpClient.getOkHttpClient(), httpClient.getCookieManager());
        checkHandler = new Handler(Looper.getMainLooper());
    }
    
    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        binding = FragmentMessageBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }
    
    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);
        
        // 检查登录状态
        checkLoginState();
    }
    
    @Override
    public void onResume() {
        super.onResume();
        // 每次显示时检查登录状态
        checkLoginState();
        // 开始检查新消息
        startCheckingNewPm();
    }
    
    @Override
    public void onPause() {
        super.onPause();
        // 停止检查新消息
        stopCheckingNewPm();
    }
    
    @Override
    public void onDestroyView() {
        super.onDestroyView();
        stopCheckingNewPm();
        binding = null;
    }
    
    private void checkLoginState() {
        boolean isLoggedIn = HttpClient.getInstance(requireContext())
            .getCookieManager().isLoggedIn();
        
        if (isLoggedIn) {
            showMessages();
        } else {
            showLoginPrompt();
        }
    }
    
    private void showMessages() {
        binding.loginPrompt.setVisibility(View.GONE);
        binding.appBarLayout.setVisibility(View.VISIBLE);
        binding.viewPager.setVisibility(View.VISIBLE);
        binding.progressBar.setVisibility(View.GONE);
        
        setupViewPager();
    }
    
    private void showLoginPrompt() {
        binding.loginPrompt.setVisibility(View.VISIBLE);
        binding.appBarLayout.setVisibility(View.GONE);
        binding.viewPager.setVisibility(View.GONE);
        binding.progressBar.setVisibility(View.GONE);
        
        // 登录按钮点击事件
        binding.btnLogin.setOnClickListener(v -> {
            startActivity(new Intent(requireContext(), LoginActivity.class));
        });
    }
    
    private void setupViewPager() {
        if (pagerAdapter == null) {
            pagerAdapter = new MessagePagerAdapter(requireActivity());
            binding.viewPager.setAdapter(pagerAdapter);
            
            // 设置TabLayout和ViewPager联动
            new TabLayoutMediator(binding.tabLayout, binding.viewPager,
                (tab, position) -> {
                    switch (position) {
                        case 0:
                            tab.setText(R.string.private_message);
                            pmTab = tab;
                            break;
                        case 1:
                            tab.setText(R.string.notification);
                            break;
                    }
                }
            ).attach();
        }
    }
    
    /**
     * 开始检查新私信
     */
    private void startCheckingNewPm() {
        if (isChecking) return;
        
        boolean isLoggedIn = HttpClient.getInstance(requireContext())
            .getCookieManager().isLoggedIn();
        if (!isLoggedIn) return;
        
        isChecking = true;
        // 立即检查一次
        checkNewPm();
        // 然后定时检查
        checkHandler.postDelayed(checkNewPmRunnable, CHECK_INTERVAL);
    }
    
    /**
     * 停止检查新私信
     */
    private void stopCheckingNewPm() {
        isChecking = false;
        if (checkHandler != null) {
            checkHandler.removeCallbacks(checkNewPmRunnable);
        }
    }
    
    /**
     * 检查是否有新私信
     */
    private void checkNewPm() {
        if (forumApi == null || !isAdded()) return;
        
        new Thread(() -> {
            ApiResponse<Boolean> response = forumApi.checkNewPm();
            
            if (getActivity() != null) {
                getActivity().runOnUiThread(() -> {
                    if (response.isSuccess() && Boolean.TRUE.equals(response.getData())) {
                        // 有新私信，显示红点
                        showPmBadge();
                    } else {
                        // 没有新私信，隐藏红点
                        hidePmBadge();
                    }
                });
            }
        }).start();
    }
    
    /**
     * 显示私信红点
     */
    private void showPmBadge() {
        if (pmTab != null && isAdded()) {
            // 使用Tab的view来显示红点
            View tabView = ((View) pmTab.getCustomView());
            if (tabView == null) {
                // 如果没有自定义view，给Tab文字添加圆点符号
                pmTab.setText("私信 ●");
            }
        }
    }
    
    /**
     * 隐藏私信红点
     */
    private void hidePmBadge() {
        if (pmTab != null && isAdded()) {
            pmTab.setText(R.string.private_message);
        }
    }
}