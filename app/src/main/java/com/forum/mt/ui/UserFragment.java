package com.forum.mt.ui;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.forum.mt.R;
import com.forum.mt.api.ApiConfig;
import com.forum.mt.api.ForumApi;
import com.forum.mt.api.HttpClient;
import com.forum.mt.databinding.FragmentUserBinding;
import com.forum.mt.model.User;
import com.forum.mt.ui.UserDetailActivity;
import com.forum.mt.util.LogToFile;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.resource.drawable.DrawableTransitionOptions;

import okhttp3.OkHttpClient;

/**
 * 用户中心Fragment
 */
public class UserFragment extends Fragment {
    private FragmentUserBinding binding;
    private ForumApi forumApi;
    private User currentUser;
    private boolean wasLoggedIn = false; // 记录之前的登录状态

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        binding = FragmentUserBinding.inflate(inflater, container, false);
        return binding.getRoot();
    }

    @Override
    public void onViewCreated(@NonNull View view, @Nullable Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        HttpClient httpClient = HttpClient.getInstance(requireContext());
        OkHttpClient okHttpClient = httpClient.getOkHttpClient();
        forumApi = new ForumApi(okHttpClient, httpClient.getCookieManager());

        // 初始化登录状态记录
        wasLoggedIn = httpClient.isLoggedIn();

        // 初始化视图
        initViews();

        if (httpClient.isLoggedIn()) {
            // 已登录状态，直接加载用户资料，不显示临时的"已登录"文字
            loadUserProfile();
        } else {
            showGuestState();
        }
    }

    private void initViews() {
        // 头部区域点击 - 登录状态打开用户详情，未登录跳转登录
        binding.userHeaderLayout.setOnClickListener(v -> {
            HttpClient httpClient = HttpClient.getInstance(requireContext());
            if (httpClient.isLoggedIn()) {
                // 已登录，打开用户详情页，带共享元素动画
                if (currentUser != null) {
                    UserDetailActivity.start(requireActivity(), currentUser.getUid(), currentUser.getUsername(), currentUser.getAvatarUrl(), binding.avatarImage);
                }
            } else {
                // 未登录，跳转登录页
                startActivity(new Intent(requireContext(), LoginActivity.class));
            }
        });
        
        // 签到按钮
        binding.signInButton.setOnClickListener(v -> {
            performSign();
        });
        
        // 功能菜单点击事件
        binding.menuMyPosts.setOnClickListener(v -> {
            // 跳转到我的帖子页面
            HttpClient httpClient = HttpClient.getInstance(requireContext());
            if (httpClient.isLoggedIn()) {
                startActivity(new Intent(requireContext(), MyPostsActivity.class));
            } else {
                startActivity(new Intent(requireContext(), LoginActivity.class));
            }
        });
        
        binding.menuMyComments.setOnClickListener(v -> {
            // 跳转到我的评论页面
            HttpClient httpClient = HttpClient.getInstance(requireContext());
            if (httpClient.isLoggedIn()) {
                startActivity(new Intent(requireContext(), MyRepliesActivity.class));
            } else {
                startActivity(new Intent(requireContext(), LoginActivity.class));
            }
        });
        
        binding.menuMyFavorites.setOnClickListener(v -> {
            // 跳转到我的收藏页面
            HttpClient httpClient = HttpClient.getInstance(requireContext());
            if (httpClient.isLoggedIn()) {
                startActivity(new Intent(requireContext(), MyFavoritesActivity.class));
            } else {
                startActivity(new Intent(requireContext(), LoginActivity.class));
            }
        });
        
        binding.menuHistory.setOnClickListener(v -> {
            // 跳转到浏览历史页面
            startActivity(new Intent(requireContext(), BrowseHistoryActivity.class));
        });
        
        binding.menuSettings.setOnClickListener(v -> {
            // 跳转到设置页面
            SettingsActivity.start(requireContext());
        });
        
        binding.menuLogout.setOnClickListener(v -> {
            // 退出登录
            showLogoutConfirmDialog();
        });
    }
    
    /**
     * 显示退出登录确认对话框
     */
    private void showLogoutConfirmDialog() {
        HttpClient httpClient = HttpClient.getInstance(requireContext());
        if (!httpClient.isLoggedIn()) {
            android.widget.Toast.makeText(requireContext(), "您还未登录", android.widget.Toast.LENGTH_SHORT).show();
            return;
        }
        
        new androidx.appcompat.app.AlertDialog.Builder(requireContext())
            .setTitle("退出登录")
            .setMessage("确定要退出登录吗？")
            .setPositiveButton("确定", (dialog, which) -> performLogout())
            .setNegativeButton("取消", null)
            .show();
    }
    
    /**
     * 执行退出登录
     */
    private void performLogout() {
        new Thread(() -> {
            try {
                var response = forumApi.logout();
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // 清除本地Cookie
                        HttpClient httpClient = HttpClient.getInstance(requireContext());
                        httpClient.clearCookies();
                        
                        // 清除当前用户
                        currentUser = null;
                        
                        // 重置登录状态记录
                        wasLoggedIn = false;
                        
                        // 显示游客状态
                        showGuestState();
                        
                        android.widget.Toast.makeText(requireContext(), "已退出登录", android.widget.Toast.LENGTH_SHORT).show();
                    });
                }
            } catch (Exception e) {
                LogToFile.e("UserFragment", "退出登录失败: " + e.getMessage());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // 即使服务器请求失败，也清除本地状态
                        HttpClient httpClient = HttpClient.getInstance(requireContext());
                        httpClient.clearCookies();
                        currentUser = null;
                        wasLoggedIn = false;
                        showGuestState();
                        android.widget.Toast.makeText(requireContext(), "已退出登录", android.widget.Toast.LENGTH_SHORT).show();
                    });
                }
            }
        }).start();
    }



    @Override
    public void onResume() {
        super.onResume();
        // 刷新登录状态
        HttpClient httpClient = HttpClient.getInstance(requireContext());
        boolean isLoggedIn = httpClient.isLoggedIn();
        
        // 检测登录状态变化：从未登录变为已登录，需要强制刷新
        if (isLoggedIn && !wasLoggedIn) {
            // 登录状态变化，清除旧数据并重新加载
            currentUser = null;
            loadUserProfile();
        } else if (isLoggedIn) {
            // 已登录状态，只在用户资料为空时才重新加载
            if (currentUser == null) {
                loadUserProfile();
            }
        } else {
            showGuestState();
        }
        
        // 更新登录状态记录
        wasLoggedIn = isLoggedIn;
    }

    private void loadUserProfile() {
        // 先设置已登录状态（隐藏游客标签，显示退出登录按钮）
        binding.guestTag.setVisibility(View.GONE);
        binding.menuLogout.setVisibility(View.VISIBLE);

        // 在子线程中获取用户资料
        new Thread(() -> {
            try {
                var response = forumApi.getUserProfile();
                if (response.isSuccess() && response.getData() != null) {
                    currentUser = response.getData();
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> displayUserInfo(currentUser));
                    }
                } else {
                    LogToFile.e("UserFragment", "获取用户资料失败: " + response.getMessage());
                    if (getActivity() != null) {
                        getActivity().runOnUiThread(() -> {
                            // 加载失败，显示"已登录"作为fallback
                            binding.usernameText.setText("已登录");
                        });
                    }
                }
            } catch (Exception e) {
                LogToFile.e("UserFragment", "获取用户资料异常: " + e.getMessage());
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        // 异常情况，显示"已登录"作为fallback
                        binding.usernameText.setText("已登录");
                    });
                }
            }
        }).start();
    }

    private void displayUserInfo(User user) {
        if (user == null) return;
        
        // 用户名
        if (user.getUsername() != null && !user.getUsername().isEmpty()) {
            binding.usernameText.setText(user.getUsername());
        }
        
        // 隐藏游客标签，显示已登录状态
        binding.guestTag.setVisibility(View.GONE);
        
        // 头像
        String avatarUrl = user.getAvatarUrl();
        if (avatarUrl != null && !avatarUrl.isEmpty()) {
            Glide.with(this)
                .load(avatarUrl)
                .placeholder(R.drawable.ic_user_avatar_placeholder)
                .error(R.drawable.ic_user_avatar_placeholder)
                .circleCrop()
                .diskCacheStrategy(DiskCacheStrategy.NONE)
                .transition(DrawableTransitionOptions.withCrossFade())
                .into(binding.avatarImage);
        }
        
        // 用户等级
        String level = user.getLevel();
        if (level != null && !level.isEmpty()) {
            binding.levelText.setText(level);
        } else {
            binding.levelText.setText("Lv.0");
        }
        
        // 关注数和粉丝数 - 使用User对象中的数据（如果已有）
        binding.followsText.setText(String.valueOf(user.getFollowing()));
        binding.fansText.setText(String.valueOf(user.getFollowers()));
        
        // 积分
        binding.creditsText.setText(String.valueOf(user.getCredits()));
        
        // 金币
        binding.goldText.setText(String.valueOf(user.getGoldCoin()));
        
        // 如果关注/粉丝数为0，尝试从AJAX接口获取
        if (user.getFollowing() == 0 && user.getFollowers() == 0 && user.getUid() > 0) {
            loadFollowStats(user.getUid());
        }
        
        // 检查签到状态
        checkSignStatus();
    }
    
    /**
     * 检查签到状态
     */
    private void checkSignStatus() {
        new Thread(() -> {
            try {
                var response = forumApi.checkSignStatus();
                if (getActivity() != null && response.isSuccess() && response.getData() != null) {
                    ForumApi.SignStatus status = response.getData();
                    getActivity().runOnUiThread(() -> updateSignButton(status));
                }
            } catch (Exception e) {
                LogToFile.e("UserFragment", "检查签到状态失败: " + e.getMessage());
            }
        }).start();
    }
    
    /**
     * 更新签到按钮状态
     */
    private void updateSignButton(ForumApi.SignStatus status) {
        if (status.isSignedToday()) {
            binding.signInButton.setEnabled(false);
            binding.signInButton.setText("已签到");
            binding.signInButton.setBackgroundResource(R.drawable.bg_sign_in_button_disabled);
        } else {
            binding.signInButton.setEnabled(true);
            binding.signInButton.setText("签到");
            binding.signInButton.setBackgroundResource(R.drawable.bg_sign_in_button);
        }
    }
    
    /**
     * 从AJAX接口获取关注/粉丝统计
     */
    private void loadFollowStats(int uid) {
        new Thread(() -> {
            try {
                // 获取关注数
                var followingResponse = forumApi.getFollowing(uid);
                int followingCount = 0;
                if (followingResponse.isSuccess() && followingResponse.getData() != null) {
                    followingCount = followingResponse.getData().getCount();
                }
                
                // 获取粉丝数
                var followersResponse = forumApi.getFollowers(uid);
                int followersCount = 0;
                if (followersResponse.isSuccess() && followersResponse.getData() != null) {
                    followersCount = followersResponse.getData().getCount();
                }
                
                final int finalFollowing = followingCount;
                final int finalFollowers = followersCount;
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        binding.followsText.setText(String.valueOf(finalFollowing));
                        binding.fansText.setText(String.valueOf(finalFollowers));
                        
                        // 更新currentUser对象
                        if (currentUser != null) {
                            currentUser.setFollowing(finalFollowing);
                            currentUser.setFollowers(finalFollowers);
                        }
                    });
                }
            } catch (Exception e) {
                LogToFile.e("UserFragment", "获取关注/粉丝数失败: " + e.getMessage());
            }
        }).start();
    }

    private void showLoggedInState() {
        binding.usernameText.setText("已登录");
        binding.guestTag.setVisibility(View.GONE);
        binding.menuLogout.setVisibility(View.VISIBLE);
    }

    private void showGuestState() {
        binding.usernameText.setText("未登录");
        binding.guestTag.setVisibility(View.VISIBLE);
        binding.guestTag.setText("游客你好");
        
        // 重置统计数据
        binding.levelText.setText("Lv.0");
        binding.followsText.setText("0");
        binding.fansText.setText("0");
        binding.creditsText.setText("0");
        binding.goldText.setText("0");
        
        // 重置头像
        binding.avatarImage.setImageResource(R.drawable.ic_user_avatar_placeholder);
        
        // 重置签到按钮
        binding.signInButton.setEnabled(true);
        binding.signInButton.setText("签到");
        binding.signInButton.setBackgroundResource(R.drawable.bg_sign_in_button);
        
        binding.menuLogout.setVisibility(View.GONE);
    }
    
    /**
     * 执行签到
     */
    private void performSign() {
        // 检查登录状态
        HttpClient httpClient = HttpClient.getInstance(requireContext());
        if (!httpClient.isLoggedIn()) {
            android.widget.Toast.makeText(requireContext(), "请先登录", android.widget.Toast.LENGTH_SHORT).show();
            startActivity(new Intent(requireContext(), LoginActivity.class));
            return;
        }
        
        // 禁用签到按钮，防止重复点击
        binding.signInButton.setEnabled(false);
        binding.signInButton.setText("签到中...");
        
        // 在子线程中执行签到
        new Thread(() -> {
            try {
                var response = forumApi.performSign();
                
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        if (response.isSuccess() && response.getData() != null) {
                            com.forum.mt.api.ForumApi.SignResult result = response.getData();
                            
                            if (result.isSuccess()) {
                                String message = result.getMessage();
                                if (result.getReward() != null && !result.getReward().isEmpty()) {
                                    message += "，获得 " + result.getReward();
                                }
                                android.widget.Toast.makeText(requireContext(), message, android.widget.Toast.LENGTH_LONG).show();
                                
                                // 签到成功后刷新用户信息
                                loadUserProfile();
                                
                                // 更新签到按钮状态
                                updateSignButtonAfterSign(result.isAlreadySigned());
                            } else {
                                android.widget.Toast.makeText(requireContext(), result.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                                // 恢复签到按钮
                                resetSignInButton();
                            }
                        } else {
                            android.widget.Toast.makeText(requireContext(), response.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                            // 恢复签到按钮
                            resetSignInButton();
                        }
                    });
                }
            } catch (Exception e) {
                if (getActivity() != null) {
                    getActivity().runOnUiThread(() -> {
                        android.widget.Toast.makeText(requireContext(), "签到失败: " + e.getMessage(), android.widget.Toast.LENGTH_SHORT).show();
                        // 恢复签到按钮
                        resetSignInButton();
                    });
                }
            }
        }).start();
    }
    
    /**
     * 签到后更新按钮状态
     */
    private void updateSignButtonAfterSign(boolean alreadySigned) {
        binding.signInButton.setEnabled(false);
        binding.signInButton.setText("已签到");
        binding.signInButton.setBackgroundResource(R.drawable.bg_sign_in_button_disabled);
    }
    
    /**
     * 重置签到按钮
     */
    private void resetSignInButton() {
        binding.signInButton.setEnabled(true);
        binding.signInButton.setText("签到");
        binding.signInButton.setBackgroundResource(R.drawable.bg_sign_in_button);
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        binding = null;
    }
}
