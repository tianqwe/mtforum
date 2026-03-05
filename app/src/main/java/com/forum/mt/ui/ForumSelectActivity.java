package com.forum.mt.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.forum.mt.R;
import com.forum.mt.api.ForumApi;
import com.forum.mt.api.HttpClient;
import com.forum.mt.model.ApiResponse;
import com.forum.mt.model.Forum;
import com.forum.mt.ui.adapter.ForumListAdapter;

import java.util.List;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/**
 * 版块选择Activity
 */
public class ForumSelectActivity extends AppCompatActivity implements ForumListAdapter.OnForumClickListener {

    public static final String EXTRA_SELECTED_FID = "selected_fid";
    public static final String EXTRA_SELECTED_FORUM_NAME = "selected_forum_name";
    public static final int REQUEST_CODE_SELECT_FORUM = 1002;

    private ImageView backButton;
    private RecyclerView recyclerView;
    private ProgressBar progressBar;
    private LinearLayout errorView;
    private TextView errorText;
    private TextView retryButton;

    private ForumListAdapter adapter;
    private ForumApi forumApi;
    private ExecutorService executor;
    private Handler mainHandler;

    public static void start(Activity activity) {
        Intent intent = new Intent(activity, ForumSelectActivity.class);
        activity.startActivityForResult(intent, REQUEST_CODE_SELECT_FORUM);
    }

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_forum_select);

        initViews();
        initData();
        setupListeners();
        loadData();
    }

    private void initViews() {
        backButton = findViewById(R.id.backButton);
        recyclerView = findViewById(R.id.recyclerView);
        progressBar = findViewById(R.id.progressBar);
        errorView = findViewById(R.id.errorView);
        errorText = findViewById(R.id.errorText);
        retryButton = findViewById(R.id.retryButton);

        // 设置RecyclerView
        adapter = new ForumListAdapter();
        adapter.setOnForumClickListener(this);

        GridLayoutManager layoutManager = new GridLayoutManager(this, 3);
        layoutManager.setSpanSizeLookup(new GridLayoutManager.SpanSizeLookup() {
            @Override
            public int getSpanSize(int position) {
                if (adapter.getItemViewType(position) == Forum.TYPE_CATEGORY) {
                    return 3;
                }
                return 1;
            }
        });

        recyclerView.setLayoutManager(layoutManager);
        recyclerView.setAdapter(adapter);
        recyclerView.setHasFixedSize(true);
    }

    private void initData() {
        HttpClient httpClient = HttpClient.getInstance(this);
        forumApi = new ForumApi(httpClient.getOkHttpClient(), httpClient.getCookieManager());
        executor = Executors.newSingleThreadExecutor();
        mainHandler = new Handler(Looper.getMainLooper());
    }

    private void setupListeners() {
        backButton.setOnClickListener(v -> finish());
        retryButton.setOnClickListener(v -> loadData());
    }

    private void loadData() {
        showLoading();

        executor.execute(() -> {
            ApiResponse<List<Forum>> response = forumApi.getForumList();

            mainHandler.post(() -> {
                if (response.isSuccess() && response.getData() != null && !response.getData().isEmpty()) {
                    showContent(response.getData());
                } else {
                    showError(response.getMessage());
                }
            });
        });
    }

    private void showLoading() {
        progressBar.setVisibility(View.VISIBLE);
        recyclerView.setVisibility(View.GONE);
        errorView.setVisibility(View.GONE);
    }

    private void showContent(List<Forum> forums) {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.VISIBLE);
        errorView.setVisibility(View.GONE);

        adapter.setData(forums);
    }

    private void showError(String message) {
        progressBar.setVisibility(View.GONE);
        recyclerView.setVisibility(View.GONE);
        errorView.setVisibility(View.VISIBLE);

        errorText.setText(message != null ? message : "加载失败");
    }

    @Override
    public void onForumClick(Forum forum, View iconView) {
        // 返回选择的版块
        Intent resultIntent = new Intent();
        resultIntent.putExtra(EXTRA_SELECTED_FID, forum.getFid());
        resultIntent.putExtra(EXTRA_SELECTED_FORUM_NAME, forum.getName());
        setResult(RESULT_OK, resultIntent);
        finish();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (executor != null && !executor.isShutdown()) {
            executor.shutdown();
        }
    }
}