package com.forum.mt.ui;

import android.os.Build;
import android.os.Bundle;
import android.view.View;
import android.view.WindowInsets;
import android.view.WindowInsetsController;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;

import com.forum.mt.R;
import com.forum.mt.ui.adapter.ImageViewerAdapter;

import java.util.List;

/**
 * 图片查看器Activity
 * 支持滑动切换和双指缩放
 */
public class ImageViewerActivity extends AppCompatActivity {

    public static final String EXTRA_IMAGE_URLS = "image_urls";
    public static final String EXTRA_CURRENT_POSITION = "current_position";

    private ViewPager2 viewPager;
    private TextView imageCounter;
    private TextView hintText;

    private List<String> imageUrls;
    private int currentPosition;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        // 获取传入的图片数据
        imageUrls = getIntent().getStringArrayListExtra(EXTRA_IMAGE_URLS);
        currentPosition = getIntent().getIntExtra(EXTRA_CURRENT_POSITION, 0);

        if (imageUrls == null || imageUrls.isEmpty()) {
            finish();
            return;
        }

        setContentView(R.layout.activity_image_viewer);
        setFullScreen();

        initViews();
        setupViewPager();
        updateCounter();
    }

    private void initViews() {
        viewPager = findViewById(R.id.viewPager);
        imageCounter = findViewById(R.id.imageCounter);
        hintText = findViewById(R.id.hintText);
        ImageView backButton = findViewById(R.id.backButton);

        backButton.setOnClickListener(v -> finish());

        // 显示提示并自动隐藏
        hintText.setAlpha(1f);
        hintText.animate()
                .alpha(0f)
                .setStartDelay(2000)
                .setDuration(500)
                .start();
    }

    private void setupViewPager() {
        ImageViewerAdapter adapter = new ImageViewerAdapter(this, imageUrls);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(currentPosition, false);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                currentPosition = position;
                updateCounter();
            }
        });
    }

    private void updateCounter() {
        imageCounter.setText((currentPosition + 1) + " / " + imageUrls.size());
    }

    private void setFullScreen() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            getWindow().setDecorFitsSystemWindows(false);
            WindowInsetsController controller = getWindow().getInsetsController();
            if (controller != null) {
                controller.hide(WindowInsets.Type.statusBars() | WindowInsets.Type.navigationBars());
                controller.setSystemBarsBehavior(WindowInsetsController.BEHAVIOR_SHOW_TRANSIENT_BARS_BY_SWIPE);
            }
        } else {
            getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN,
                    WindowManager.LayoutParams.FLAG_FULLSCREEN);
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                    | View.SYSTEM_UI_FLAG_FULLSCREEN
                    | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    }

    @Override
    public void onBackPressed() {
        finish();
    }
}