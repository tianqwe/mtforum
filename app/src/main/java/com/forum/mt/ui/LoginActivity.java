package com.forum.mt.ui;

import android.os.Bundle;
import android.text.TextUtils;
import android.text.method.HideReturnsTransformationMethod;
import android.text.method.PasswordTransformationMethod;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.forum.mt.R;
import com.forum.mt.api.ForumApi;
import com.forum.mt.api.HttpClient;
import com.forum.mt.databinding.ActivityLoginBinding;

import okhttp3.OkHttpClient;

/**
 * 登录页面
 * 支持两种登录方式：账号密码登录、Cookie导入
 */
public class LoginActivity extends AppCompatActivity {
    private ActivityLoginBinding binding;
    private ForumApi forumApi;
    private boolean isPasswordVisible = false;
    private int currentTab = 0; // 0: 密码登录, 1: Cookie导入

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        binding = ActivityLoginBinding.inflate(getLayoutInflater());
        setContentView(binding.getRoot());
        
        // 根据当前主题设置状态栏图标颜色
        setupStatusBar();
        
        HttpClient httpClient = HttpClient.getInstance(this);
        OkHttpClient okHttpClient = httpClient.getOkHttpClient();
        forumApi = new ForumApi(okHttpClient, httpClient.getCookieManager());
        
        setupViews();
    }
    
    private void setupViews() {
        // 返回按钮
        binding.backButton.setOnClickListener(v -> finish());
        
        // Tab切换
        binding.tabPassword.setOnClickListener(v -> switchTab(0));
        binding.tabCookie.setOnClickListener(v -> switchTab(1));
        
        // 密码显示/隐藏
        binding.togglePassword.setOnClickListener(v -> togglePasswordVisibility());
        
        // 登录按钮
        binding.loginButton.setOnClickListener(v -> performPasswordLogin());
        
        // Cookie导入按钮
        binding.importCookieButton.setOnClickListener(v -> performCookieImport());
    }
    
    /**
     * 切换Tab
     */
    private void switchTab(int tab) {
        if (currentTab == tab) return;
        currentTab = tab;
        
        if (tab == 0) {
            // 账号密码登录
            binding.tabPassword.setTextColor(getResources().getColor(R.color.primary));
            binding.tabPassword.getPaint().setFakeBoldText(true);
            binding.tabCookie.setTextColor(getResources().getColor(R.color.text_secondary));
            binding.tabCookie.getPaint().setFakeBoldText(false);
            
            binding.passwordLoginLayout.setVisibility(View.VISIBLE);
            binding.cookieLoginLayout.setVisibility(View.GONE);
        } else {
            // Cookie导入
            binding.tabCookie.setTextColor(getResources().getColor(R.color.primary));
            binding.tabCookie.getPaint().setFakeBoldText(true);
            binding.tabPassword.setTextColor(getResources().getColor(R.color.text_secondary));
            binding.tabPassword.getPaint().setFakeBoldText(false);
            
            binding.passwordLoginLayout.setVisibility(View.GONE);
            binding.cookieLoginLayout.setVisibility(View.VISIBLE);
        }
    }
    
    /**
     * 切换密码显示/隐藏
     */
    private void togglePasswordVisibility() {
        isPasswordVisible = !isPasswordVisible;
        if (isPasswordVisible) {
            binding.passwordInput.setTransformationMethod(HideReturnsTransformationMethod.getInstance());
            binding.togglePassword.setImageResource(R.drawable.ic_eye);
        } else {
            binding.passwordInput.setTransformationMethod(PasswordTransformationMethod.getInstance());
            binding.togglePassword.setImageResource(R.drawable.ic_eye_off);
        }
        // 保持光标在末尾
        binding.passwordInput.setSelection(binding.passwordInput.getText().length());
    }
    
    /**
     * 执行账号密码登录
     */
    private void performPasswordLogin() {
        String username = binding.usernameInput.getText().toString().trim();
        String password = binding.passwordInput.getText().toString().trim();
        
        if (TextUtils.isEmpty(username)) {
            Toast.makeText(this, "请输入用户名", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (TextUtils.isEmpty(password)) {
            Toast.makeText(this, "请输入密码", Toast.LENGTH_SHORT).show();
            return;
        }
        
        if (password.length() < 6 || password.length() > 16) {
            Toast.makeText(this, "密码长度需为6-16位", Toast.LENGTH_SHORT).show();
            return;
        }
        
        setLoading(true);
        
        new Thread(() -> {
            try {
                var response = forumApi.login(username, password);
                
                runOnUiThread(() -> {
                    setLoading(false);
                    
                    if (response.isSuccess() && response.getData() != null) {
                        ForumApi.LoginResult result = response.getData();
                        
                        if (result.isSuccess()) {
                            String msg = "登录成功";
                            if (result.getUsername() != null) {
                                msg = "欢迎，" + result.getUsername();
                            }
                            Toast.makeText(this, msg, Toast.LENGTH_SHORT).show();
                            setResult(RESULT_OK);
                            finish();
                        } else {
                            Toast.makeText(this, result.getMessage(), Toast.LENGTH_LONG).show();
                        }
                    } else {
                        Toast.makeText(this, response.getMessage(), Toast.LENGTH_LONG).show();
                    }
                });
            } catch (Exception e) {
                runOnUiThread(() -> {
                    setLoading(false);
                    Toast.makeText(this, "登录失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
            }
        }).start();
    }
    
    /**
     * 执行Cookie导入
     */
    private void performCookieImport() {
        String cookieString = binding.cookieInput.getText().toString().trim();
        if (cookieString.isEmpty()) {
            Toast.makeText(this, "请输入Cookie", Toast.LENGTH_SHORT).show();
            return;
        }
        
        HttpClient.getInstance(this).setCookies(cookieString);
        
        if (HttpClient.getInstance(this).isLoggedIn()) {
            Toast.makeText(this, "登录成功", Toast.LENGTH_SHORT).show();
            setResult(RESULT_OK);
            finish();
        } else {
            Toast.makeText(this, "Cookie无效，请检查格式", Toast.LENGTH_SHORT).show();
        }
    }
    
    /**
     * 设置加载状态
     */
    private void setLoading(boolean loading) {
        binding.loginProgress.setVisibility(loading ? View.VISIBLE : View.GONE);
        binding.loginButton.setEnabled(!loading);
        binding.loginButton.setText(loading ? "登录中..." : "立即登录");
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
                getWindow().setStatusBarColor(android.graphics.Color.TRANSPARENT);
            } else {
                // 浅色模式：状态栏使用深色图标
                getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_LIGHT_STATUS_BAR);
                getWindow().setStatusBarColor(android.graphics.Color.WHITE);
            }
        });
    }
    
    @Override
    protected void onDestroy() {
        super.onDestroy();
        binding = null;
    }
}
