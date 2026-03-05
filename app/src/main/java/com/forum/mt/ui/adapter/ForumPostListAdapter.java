package com.forum.mt.ui.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.forum.mt.R;
import com.forum.mt.databinding.ItemPaginationBinding;
import com.forum.mt.databinding.ItemPostBinding;
import com.forum.mt.model.ForumInfo;
import com.forum.mt.model.Post;
import com.forum.mt.util.AppSettings;
import com.forum.mt.util.FontUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.List;

/**
 * 版块帖子列表适配器 - 支持多种Item类型
 * 类型：版块头部、导航栏、置顶区、普通帖子、分页控件
 */
public class ForumPostListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private static final int TYPE_HEADER = 0;      // 版块信息头部
    private static final int TYPE_TABS = 1;        // 横向分类导航栏
    private static final int TYPE_TOP_SECTION = 2; // 置顶区域
    private static final int TYPE_POST = 3;        // 普通帖子
    private static final int TYPE_PAGINATION = 4;  // 分页控件
    
    private ForumInfo forumInfo;
    private List<Post> topPosts = new ArrayList<>();
    private List<Post> posts = new ArrayList<>();
    private int currentTab = 0;
    private String currentFilter = "all";
    private int currentPage = 1;
    private int totalPages = 1;
    
    private OnItemClickListener itemClickListener;
    private OnTabSelectedListener tabSelectedListener;
    private OnFollowClickListener followClickListener;
    private OnPaginationListener paginationListener;
    private OnUserClickListener userClickListener;
    
    public interface OnItemClickListener {
        void onItemClick(Post post, View avatarView);
    }
    
    public interface OnTabSelectedListener {
        void onTabSelected(int position, String filter);
    }
    
    public interface OnFollowClickListener {
        void onFollowClick();
    }
    
    public interface OnPaginationListener {
        void onPrevPage();
        void onNextPage();
        void onPageIndicatorClick(View pageIndicatorView);
    }
    
    public interface OnUserClickListener {
        void onUserClick(int uid, String username, String avatar, View avatarView);
    }

    public void setOnItemClickListener(OnItemClickListener listener) {
        this.itemClickListener = listener;
    }
    
    public void setOnTabSelectedListener(OnTabSelectedListener listener) {
        this.tabSelectedListener = listener;
    }
    
    public void setOnFollowClickListener(OnFollowClickListener listener) {
        this.followClickListener = listener;
    }
    
    public void setOnPaginationListener(OnPaginationListener listener) {
        this.paginationListener = listener;
    }
    
    public void setOnUserClickListener(OnUserClickListener listener) {
        this.userClickListener = listener;
    }
    
    // ==================== 数据设置方法 ====================
    
    /**
     * 设置版块信息
     */
    public void setForumInfo(ForumInfo info) {
        this.forumInfo = info;
        notifyItemChanged(0);  // header
        notifyItemChanged(1);  // tabs
    }
    
    /**
     * 设置置顶帖子
     */
    public void setTopPosts(List<Post> topPosts) {
        boolean wasEmpty = this.topPosts.isEmpty();
        this.topPosts = topPosts != null ? topPosts : new ArrayList<>();
        boolean nowEmpty = this.topPosts.isEmpty();
        
        if (wasEmpty && !nowEmpty) {
            notifyItemInserted(2);
        } else if (!wasEmpty && nowEmpty) {
            notifyItemRemoved(2);
        } else if (!nowEmpty) {
            notifyItemChanged(2);
        }
    }
    
    /**
     * 设置帖子列表（翻页时只更新帖子区域）
     */
    public void setPosts(List<Post> posts) {
        int oldSize = this.posts.size();
        int newSize = posts != null ? posts.size() : 0;
        this.posts = posts != null ? new ArrayList<>(posts) : new ArrayList<>();
        
        int postStartIndex = getPostStartIndex();
        int postCount = Math.min(oldSize, newSize);
        
        // 只更新帖子区域
        if (postCount > 0) {
            notifyItemRangeChanged(postStartIndex, postCount);
        }
        if (newSize > oldSize) {
            notifyItemRangeInserted(postStartIndex + oldSize, newSize - oldSize);
        } else if (newSize < oldSize) {
            notifyItemRangeRemoved(postStartIndex + newSize, oldSize - newSize);
        }
        
        // 更新分页控件
        notifyItemChanged(getPaginationIndex());
    }
    
    /**
     * 设置分页信息
     */
    public void setPagination(int currentPage, int totalPages) {
        this.currentPage = currentPage;
        this.totalPages = totalPages;
        // 只更新分页控件
        if (totalPages > 1) {
            notifyItemChanged(getPaginationIndex());
        }
    }
    
    /**
     * 更新Tab选中状态
     */
    public void updateTabSelection(int newTab) {
        if (this.currentTab != newTab) {
            this.currentTab = newTab;
            notifyItemChanged(1);
        }
    }

    /**
     * 更新当前筛选类型
     */
    public void updateCurrentFilter(String filter) {
        this.currentFilter = filter;
        notifyItemChanged(1);
    }

    /**
     * 获取默认分类标签
     */
    private List<ForumInfo.ForumTab> getDefaultTabs() {
        List<ForumInfo.ForumTab> tabs = new ArrayList<>();
        tabs.add(new ForumInfo.ForumTab("全部", "all"));
        tabs.add(new ForumInfo.ForumTab("最新", "lastpost"));
        tabs.add(new ForumInfo.ForumTab("热门", "heat"));
        tabs.add(new ForumInfo.ForumTab("精华", "digest"));
        return tabs;
    }

    // ==================== 辅助方法 ====================
    
    private int getPostStartIndex() {
        return topPosts.isEmpty() ? 2 : 3;
    }
    
    private int getPaginationIndex() {
        return getPostStartIndex() + posts.size();
    }
    
    private int getPostPosition(int adapterPosition) {
        return adapterPosition - getPostStartIndex();
    }
    
    // ==================== Adapter方法 ====================
    
    @Override
    public int getItemViewType(int position) {
        if (position == 0) return TYPE_HEADER;
        if (position == 1) return TYPE_TABS;
        if (position == 2 && !topPosts.isEmpty()) return TYPE_TOP_SECTION;
        if (position == getPaginationIndex()) return TYPE_PAGINATION;
        return TYPE_POST;
    }
    
    @Override
    public int getItemCount() {
        int count = 2; // header + tabs
        if (!topPosts.isEmpty()) count++;
        count += posts.size();
        count++; // pagination (始终显示)
        return count;
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        
        switch (viewType) {
            case TYPE_HEADER:
                return new HeaderViewHolder(inflater.inflate(R.layout.item_forum_header, parent, false));
            case TYPE_TABS:
                return new TabsViewHolder(inflater.inflate(R.layout.item_forum_tabs, parent, false));
            case TYPE_TOP_SECTION:
                return new TopSectionViewHolder(inflater.inflate(R.layout.item_top_posts_section, parent, false));
            case TYPE_PAGINATION:
                return new PaginationViewHolder(ItemPaginationBinding.inflate(inflater, parent, false));
            default:
                ItemPostBinding binding = ItemPostBinding.inflate(inflater, parent, false);
                return new PostViewHolder(binding);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case TYPE_HEADER:
                ((HeaderViewHolder) holder).bind(forumInfo);
                break;
            case TYPE_TABS:
                List<ForumInfo.ForumTab> tabs = forumInfo != null && forumInfo.getTabs() != null
                    ? forumInfo.getTabs()
                    : getDefaultTabs();
                ((TabsViewHolder) holder).bind(tabs, currentFilter);
                break;
            case TYPE_TOP_SECTION:
                ((TopSectionViewHolder) holder).bind(topPosts);
                break;
            case TYPE_POST:
                int postIndex = getPostPosition(position);
                if (postIndex >= 0 && postIndex < posts.size()) {
                    ((PostViewHolder) holder).bind(posts.get(postIndex), postIndex);
                }
                break;
            case TYPE_PAGINATION:
                ((PaginationViewHolder) holder).bind(currentPage, totalPages);
                break;
        }
    }
    
    // ==================== ViewHolders ====================
    
    class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final ImageView forumIcon;
        private final TextView forumName;
        private final TextView todayPosts;
        private final TextView totalPosts;
        private final TextView followers;
        private final TextView forumDesc;
        private final FrameLayout btnFollow;
        
        HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            forumIcon = itemView.findViewById(R.id.forumIcon);
            forumName = itemView.findViewById(R.id.forumName);
            todayPosts = itemView.findViewById(R.id.todayPosts);
            totalPosts = itemView.findViewById(R.id.totalPosts);
            followers = itemView.findViewById(R.id.followers);
            forumDesc = itemView.findViewById(R.id.forumDesc);
            btnFollow = itemView.findViewById(R.id.btnFollow);
        }
        
        void bind(ForumInfo info) {
            if (info == null) return;

            forumName.setText(info.getName());

            // 只有数据有效时才显示统计信息
            if (info.getTodayPosts() > 0) {
                todayPosts.setText("今日 " + info.getTodayPosts());
                todayPosts.setVisibility(View.VISIBLE);
            } else {
                todayPosts.setVisibility(View.GONE);
            }

            if (info.getTotalPosts() > 0) {
                totalPosts.setText("帖子 " + formatNumber(info.getTotalPosts()));
                totalPosts.setVisibility(View.VISIBLE);
            } else {
                totalPosts.setVisibility(View.GONE);
            }

            if (info.getFollowers() > 0) {
                followers.setText("关注 " + info.getFollowers());
                followers.setVisibility(View.VISIBLE);
            } else {
                followers.setVisibility(View.GONE);
            }

            if (info.getDescription() != null && !info.getDescription().isEmpty()) {
                forumDesc.setText(info.getDescription());
                forumDesc.setVisibility(View.VISIBLE);
            } else {
                forumDesc.setVisibility(View.GONE);
            }

            if (info.getIcon() != null && !info.getIcon().isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(info.getIcon())
                        .placeholder(R.drawable.ic_forum)
                        .error(R.drawable.ic_forum)
                        .dontAnimate()
                        .into(forumIcon);
            }

            btnFollow.setOnClickListener(v -> {
                if (followClickListener != null) {
                    followClickListener.onFollowClick();
                }
            });
        }
    }
    
    class TabsViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout tabsContainer;

        TabsViewHolder(@NonNull View itemView) {
            super(itemView);
            tabsContainer = itemView.findViewById(R.id.tabsContainer);
        }

        void bind(List<ForumInfo.ForumTab> tabs, String currentFilter) {
            tabsContainer.removeAllViews();

            if (tabs == null || tabs.isEmpty()) {
                return;
            }

            for (int i = 0; i < tabs.size(); i++) {
                ForumInfo.ForumTab tab = tabs.get(i);
                View tabView = createTabView(tab, i, currentFilter);
                tabsContainer.addView(tabView);
            }
        }

        private View createTabView(ForumInfo.ForumTab tab, int index, String currentFilter) {
            // 使用LinearLayout作为根布局，更容易控制对齐
            LinearLayout tabLayout = new LinearLayout(tabsContainer.getContext());
            tabLayout.setOrientation(LinearLayout.VERTICAL);
            tabLayout.setGravity(android.view.Gravity.CENTER);
            LinearLayout.LayoutParams rootParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.MATCH_PARENT
            );
            rootParams.gravity = android.view.Gravity.CENTER;
            tabLayout.setLayoutParams(rootParams);
            tabLayout.setMinimumWidth((int) (64 * tabsContainer.getResources().getDisplayMetrics().density));
            tabLayout.setClickable(true);
            tabLayout.setFocusable(true);
            tabLayout.setBackgroundColor(android.graphics.Color.TRANSPARENT);

            // 文本
            TextView textView = new TextView(tabsContainer.getContext());
            textView.setText(tab.getName());
            textView.setTextSize(14);
            LinearLayout.LayoutParams textParams = new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.WRAP_CONTENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            );
            textParams.gravity = android.view.Gravity.CENTER;
            textView.setLayoutParams(textParams);

            // 根据 currentFilter 判断是否选中
            boolean isSelected = tab.getFilter().equals(currentFilter);
            if (isSelected) {
                textView.setTextColor(Color.parseColor("#3B82F6"));
                textView.setTypeface(null, android.graphics.Typeface.BOLD);
            } else {
                textView.setTextColor(Color.parseColor("#6B7280"));
                textView.setTypeface(null, android.graphics.Typeface.NORMAL);
            }
            tabLayout.addView(textView);

            // 指示器
            View indicator = new View(tabsContainer.getContext());
            int indicatorSize = (int) (3 * tabsContainer.getResources().getDisplayMetrics().density);
            int indicatorWidth = (int) (16 * tabsContainer.getResources().getDisplayMetrics().density);
            LinearLayout.LayoutParams indicatorParams = new LinearLayout.LayoutParams(
                indicatorWidth, indicatorSize
            );
            indicatorParams.gravity = android.view.Gravity.CENTER;
            indicatorParams.setMargins(0, (int) (4 * tabsContainer.getResources().getDisplayMetrics().density), 0, 0);
            indicator.setBackgroundColor(Color.parseColor("#3B82F6"));
            indicator.setVisibility(isSelected ? View.VISIBLE : View.INVISIBLE);
            tabLayout.addView(indicator, indicatorParams);

            // 点击事件
            int finalIndex = index;
            tabLayout.setOnClickListener(v -> {
                if (tabSelectedListener != null) {
                    tabSelectedListener.onTabSelected(finalIndex, tab.getFilter());
                }
            });

            return tabLayout;
        }
    }
    
    class TopSectionViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout container;
        
        TopSectionViewHolder(@NonNull View itemView) {
            super(itemView);
            container = itemView.findViewById(R.id.topPostsContainer);
        }
        
        void bind(List<Post> topPosts) {
            container.removeAllViews();
            
            for (int i = 0; i < topPosts.size(); i++) {
                Post post = topPosts.get(i);
                View itemView = LayoutInflater.from(container.getContext())
                        .inflate(R.layout.item_top_post, container, false);
                
                TextView titleView = itemView.findViewById(R.id.postTitle);
                View divider = itemView.findViewById(R.id.divider);
                
                titleView.setText(post.getTitle());
                
                if (i < 2) {
                    titleView.setTextColor(Color.parseColor("#EF4444"));
                    titleView.setTypeface(null, android.graphics.Typeface.BOLD);
                } else {
                    titleView.setTextColor(Color.parseColor("#3B82F6"));
                    titleView.setTypeface(null, android.graphics.Typeface.NORMAL);
                }
                
                divider.setVisibility(i == topPosts.size() - 1 ? View.GONE : View.VISIBLE);
                
                itemView.setOnClickListener(v -> {
                    if (itemClickListener != null) {
                        // 置顶帖子没有头像，传递 null
                        itemClickListener.onItemClick(post, null);
                    }
                });
                
                container.addView(itemView);
            }
        }
    }
    
    class PaginationViewHolder extends RecyclerView.ViewHolder {
        private final LinearLayout prevPageBtn;
        private final LinearLayout nextPageBtn;
        private final LinearLayout pageIndicator;
        private final TextView currentPageText;
        private final TextView totalPageText;
        
        PaginationViewHolder(@NonNull ItemPaginationBinding binding) {
            super(binding.getRoot());
            prevPageBtn = binding.prevPageBtn;
            nextPageBtn = binding.nextPageBtn;
            pageIndicator = binding.pageIndicator;
            currentPageText = binding.currentPageText;
            totalPageText = binding.totalPageText;
        }
        
        void bind(int currentPage, int totalPages) {
            // 只有一页时隐藏
            if (totalPages <= 1) {
                itemView.setVisibility(View.GONE);
                return;
            }
            
            itemView.setVisibility(View.VISIBLE);
            currentPageText.setText(String.valueOf(currentPage));
            totalPageText.setText(String.valueOf(totalPages));
            
            // 上一页按钮状态
            boolean canGoPrev = currentPage > 1;
            prevPageBtn.setEnabled(canGoPrev);
            prevPageBtn.setAlpha(canGoPrev ? 1.0f : 0.4f);
            prevPageBtn.setClickable(canGoPrev);
            prevPageBtn.setOnClickListener(v -> {
                if (paginationListener != null && canGoPrev) {
                    paginationListener.onPrevPage();
                }
            });
            
            // 下一页按钮状态
            boolean canGoNext = currentPage < totalPages;
            nextPageBtn.setEnabled(canGoNext);
            nextPageBtn.setAlpha(canGoNext ? 1.0f : 0.4f);
            nextPageBtn.setClickable(canGoNext);
            nextPageBtn.setOnClickListener(v -> {
                if (paginationListener != null && canGoNext) {
                    paginationListener.onNextPage();
                }
            });
            
            // 页码点击 - 传递View用于定位PopupWindow
            pageIndicator.setOnClickListener(v -> {
                if (paginationListener != null) {
                    paginationListener.onPageIndicatorClick(pageIndicator);
                }
            });
        }
    }
    
    class PostViewHolder extends RecyclerView.ViewHolder {
        private final ItemPostBinding binding;
        private Post currentPost;
        
        PostViewHolder(@NonNull ItemPostBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        
        void bind(Post post, int position) {
            currentPost = post;
            
            // 悬赏标签
            if (post.hasBounty()) {
                if (post.getBountyType() != null && !post.getBountyType().isEmpty()) {
                    binding.bountyTypeTag.setText(post.getBountyType());
                    binding.bountyTypeTag.setVisibility(View.VISIBLE);
                } else {
                    binding.bountyTypeTag.setVisibility(View.GONE);
                }
                
                if (post.getBountyText() != null && !post.getBountyText().isEmpty()) {
                    binding.bountyAmountTag.setText(post.getBountyText());
                    binding.bountyAmountTag.setVisibility(View.VISIBLE);
                } else if (post.getBountyAmount() > 0) {
                    binding.bountyAmountTag.setText(post.getBountyAmount() + "金币");
                    binding.bountyAmountTag.setVisibility(View.VISIBLE);
                } else {
                    binding.bountyAmountTag.setVisibility(View.GONE);
                }
            } else {
                binding.bountyTypeTag.setVisibility(View.GONE);
                binding.bountyAmountTag.setVisibility(View.GONE);
            }
            
            // 标题
            String title = post.getTitle();
            binding.postTitle.setText(title != null && !title.isEmpty() ? title : "无标题");
            
            // 摘要
            String summary = post.getSummary();
            if (summary != null && !summary.isEmpty()) {
                binding.postSummary.setText(summary);
                binding.postSummary.setVisibility(View.VISIBLE);
            } else {
                binding.postSummary.setVisibility(View.GONE);
            }
            
            // 作者
            String author = post.getAuthor();
            binding.authorName.setText(author != null && !author.isEmpty() ? author : "匿名");
            
            // 等级
            String level = post.getAuthorLevel();
            if (level != null && !level.isEmpty()) {
                binding.authorLevel.setText(level);
                binding.authorLevel.setVisibility(View.VISIBLE);
                setLevelBackground(binding.authorLevel, level);
            } else {
                binding.authorLevel.setVisibility(View.GONE);
            }
            
            // 性别
            int gender = post.getAuthorGender();
            if (gender == 1) {
                binding.genderIcon.setImageResource(R.drawable.ic_gender_male);
                binding.genderIcon.setVisibility(View.VISIBLE);
            } else if (gender == 2) {
                binding.genderIcon.setImageResource(R.drawable.ic_gender_female);
                binding.genderIcon.setVisibility(View.VISIBLE);
            } else {
                binding.genderIcon.setVisibility(View.GONE);
            }
            
            // 版块名称（版块列表页隐藏）
            binding.forumName.setVisibility(View.GONE);
            
            // 时间
            String dateStr = post.getDateStr();
            binding.postTime.setText(dateStr != null && !dateStr.isEmpty() ? dateStr : "刚刚");
            
            // 统计
            binding.replyCount.setText(formatCount(post.getReplies()));
            binding.viewCount.setText(formatCount(post.getViews()));
            binding.likeCount.setText(post.getLikes() > 0 ? formatCount(post.getLikes()) : "赞");
            
            // 头像
            String avatar = post.getAuthorAvatar();
            if (avatar != null && !avatar.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(avatar)
                        .circleCrop()
                        .placeholder(R.drawable.ic_forum)
                        .error(R.drawable.ic_forum)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(binding.authorAvatar);
            } else {
                binding.authorAvatar.setImageResource(R.drawable.ic_forum);
            }
            
            // 缩略图
            setupThumbnails(post);
            
            // 关注按钮
            

            // 用户头像和用户名点击事件 - 都传递头像视图用于共享元素动画
            View.OnClickListener userClick = v -> {
                if (userClickListener != null) {
                    userClickListener.onUserClick(post.getAuthorId(), post.getAuthor(), post.getAuthorAvatar(), binding.authorAvatar);
                }
            };
            binding.authorAvatar.setOnClickListener(userClick);
            binding.authorName.setOnClickListener(userClick);

            // 点击事件 - 传递头像View用于共享元素动画
            itemView.setOnClickListener(v -> {
                if (itemClickListener != null) {
                    itemClickListener.onItemClick(post, binding.authorAvatar);
                }
            });
            
            // 应用字体大小设置
            applyFontSize();
        }
        
        /**
         * 应用字体大小设置到各TextView
         */
        private void applyFontSize() {
            FontUtils.applyFontSize(itemView.getContext(), binding.postTitle, FontUtils.SIZE_TITLE);
            FontUtils.applyFontSize(itemView.getContext(), binding.postSummary, FontUtils.SIZE_SUBTITLE);
            FontUtils.applyFontSize(itemView.getContext(), binding.authorName, FontUtils.SIZE_USERNAME);
            FontUtils.applyFontSize(itemView.getContext(), binding.authorLevel, FontUtils.SIZE_SMALL);
            FontUtils.applyFontSize(itemView.getContext(), binding.postTime, FontUtils.SIZE_TIME);
            FontUtils.applyFontSize(itemView.getContext(), binding.replyCount, FontUtils.SIZE_SMALL);
            FontUtils.applyFontSize(itemView.getContext(), binding.viewCount, FontUtils.SIZE_SMALL);
            FontUtils.applyFontSize(itemView.getContext(), binding.likeCount, FontUtils.SIZE_SMALL);
        }
        
        private void setupThumbnails(Post post) {
            List<String> thumbnails = post.getThumbnails();
            if (thumbnails == null || thumbnails.isEmpty()) {
                String singleThumbnail = post.getThumbnail();
                if (singleThumbnail != null && !singleThumbnail.isEmpty()) {
                    thumbnails = new ArrayList<>();
                    thumbnails.add(singleThumbnail);
                }
            }

            // 检查是否应该加载缩略图
            boolean shouldLoadImages = AppSettings.getInstance(itemView.getContext()).shouldLoadThumbnail();
            
            // 不加载图片时，直接隐藏缩略图容器
            if (!shouldLoadImages) {
                binding.thumbnailsContainer.setVisibility(View.GONE);
                return;
            }

            if (thumbnails != null && !thumbnails.isEmpty()) {
                binding.thumbnailsContainer.setVisibility(View.VISIBLE);

                if (thumbnails.size() > 9) {
                    thumbnails = thumbnails.subList(0, 9);
                }

                GridLayoutManager gridLayoutManager = new GridLayoutManager(itemView.getContext(), 3);
                binding.thumbnailsGrid.setLayoutManager(gridLayoutManager);

                ThumbnailGridAdapter thumbnailAdapter = new ThumbnailGridAdapter(thumbnails, true);
                binding.thumbnailsGrid.setAdapter(thumbnailAdapter);

                // 使用透明触摸层拦截所有点击事件，点击缩略图区域也跳转到帖子详情
                binding.touchInterceptor.setOnClickListener(v -> {
                    if (itemClickListener != null && currentPost != null) {
                        itemClickListener.onItemClick(currentPost, binding.authorAvatar);
                    }
                });
            } else {
                binding.thumbnailsContainer.setVisibility(View.GONE);
            }
        }
        
        private void setLevelBackground(TextView levelView, String level) {
            int color;
            try {
                int levelNum = Integer.parseInt(level.replace("Lv.", "").trim());
                if (levelNum <= 2) {
                    color = Color.parseColor("#9E9E9E");
                } else if (levelNum <= 4) {
                    color = Color.parseColor("#4CAF50");
                } else if (levelNum <= 6) {
                    color = Color.parseColor("#2196F3");
                } else if (levelNum <= 8) {
                    color = Color.parseColor("#9C27B0");
                } else {
                    color = Color.parseColor("#FF9800");
                }
            } catch (Exception e) {
                color = Color.parseColor("#9E9E9E");
            }
            
            levelView.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
        }
    }
    
    /**
     * 缩略图网格Adapter
     */
    static class ThumbnailGridAdapter extends RecyclerView.Adapter<ThumbnailGridAdapter.ThumbnailViewHolder> {
        
        private final List<String> thumbnails;
        private final boolean shouldLoadImages;
        
        ThumbnailGridAdapter(List<String> thumbnails, boolean shouldLoadImages) {
            this.thumbnails = thumbnails;
            this.shouldLoadImages = shouldLoadImages;
        }
        
        @NonNull
        @Override
        public ThumbnailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(parent.getContext());
            int size = (parent.getMeasuredWidth() - 12) / 3;
            RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(size, size);
            params.setMargins(2, 2, 2, 2);
            imageView.setLayoutParams(params);
            imageView.setScaleType(ImageView.ScaleType.CENTER_CROP);
            return new ThumbnailViewHolder(imageView);
        }
        
        @Override
        public void onBindViewHolder(@NonNull ThumbnailViewHolder holder, int position) {
            if (shouldLoadImages) {
                String url = thumbnails.get(position);
                Glide.with(holder.imageView.getContext())
                        .load(url)
                        .centerCrop()
                        .placeholder(R.drawable.bg_image_placeholder)
                        .error(R.drawable.bg_image_placeholder)
                        .into(holder.imageView);
            } else {
                // 不加载图片时显示占位图
                holder.imageView.setImageResource(R.drawable.bg_image_placeholder);
            }
        }
        
        @Override
        public int getItemCount() {
            return thumbnails.size();
        }
        
        static class ThumbnailViewHolder extends RecyclerView.ViewHolder {
            ImageView imageView;
            
            ThumbnailViewHolder(ImageView imageView) {
                super(imageView);
                this.imageView = imageView;
            }
        }
    }
    
    // ==================== 工具方法 ====================
    
    private String formatNumber(int num) {
        if (num >= 10000) {
            return String.format("%.1f万", num / 10000.0);
        }
        return String.valueOf(num);
    }
    
    private String formatCount(int count) {
        if (count >= 10000) {
            return String.format("%.1f万", count / 10000.0);
        } else if (count >= 1000) {
            return String.format("%.1fk", count / 1000.0);
        }
        return String.valueOf(count);
    }
}
