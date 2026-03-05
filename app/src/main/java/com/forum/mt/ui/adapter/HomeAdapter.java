package com.forum.mt.ui.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.forum.mt.R;
import com.forum.mt.databinding.ItemHeaderBinding;
import com.forum.mt.databinding.ItemHotRankBinding;
import com.forum.mt.databinding.ItemHotRankContainerBinding;
import com.forum.mt.databinding.ItemLoadMoreBinding;
import com.forum.mt.databinding.ItemPostBinding;
import com.forum.mt.model.Forum;
import com.forum.mt.model.Post;
import com.forum.mt.util.AppSettings;
import com.forum.mt.util.FontUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 首页多类型Adapter - 谷歌推荐方式
 * 使用单一RecyclerView展示所有内容
 */
public class HomeAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    // Item类型
    private static final int TYPE_FORUM_HEADER = 0;
    private static final int TYPE_FORUM_GRID = 1;
    private static final int TYPE_HOT_RANK_CONTAINER = 2;
    private static final int TYPE_POST_HEADER = 4;
    private static final int TYPE_POST = 5;
    private static final int TYPE_LOAD_MORE = 7;

    // 数据
    private List<Forum> forums = new ArrayList<>();
    private List<Post> hotRanks = new ArrayList<>();
    private List<Post> posts = new ArrayList<>();
    private boolean showLoadMore = false;
    private boolean hasMore = true;

    // 监听器
    private OnForumClickListener forumClickListener;
    private OnPostClickListener postClickListener;
    private OnLoadMoreListener loadMoreListener;
    private OnUserClickListener userClickListener;

    public interface OnForumClickListener {
        void onForumClick(Forum forum, View iconView);
    }

    public interface OnPostClickListener {
        void onPostClick(Post post, View avatarView);
    }

    public interface OnLoadMoreListener {
        void onLoadMore();
    }
    
    public interface OnUserClickListener {
        void onUserClick(int uid, String username, String avatar, View avatarView);
    }

    public void setOnForumClickListener(OnForumClickListener listener) {
        this.forumClickListener = listener;
    }

    public void setOnPostClickListener(OnPostClickListener listener) {
        this.postClickListener = listener;
    }

    public void setOnLoadMoreListener(OnLoadMoreListener listener) {
        this.loadMoreListener = listener;
    }
    
    public void setOnUserClickListener(OnUserClickListener listener) {
        this.userClickListener = listener;
    }

    // 数据更新方法
    public void setForums(List<Forum> forums) {
        this.forums = forums != null ? forums : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setHotRanks(List<Post> hotRanks) {
        this.hotRanks = hotRanks != null ? hotRanks : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void setPosts(List<Post> posts) {
        this.posts = posts != null ? posts : new ArrayList<>();
        notifyDataSetChanged();
    }

    public void addPosts(List<Post> newPosts) {
        if (newPosts != null && !newPosts.isEmpty()) {
            int startPos = this.posts.size();
            this.posts.addAll(newPosts);
            int adapterStartPos = getAdapterPositionForPost(startPos);
            notifyItemRangeInserted(adapterStartPos, newPosts.size());
        }
    }

    public void setShowLoadMore(boolean show) {
        if (this.showLoadMore != show) {
            this.showLoadMore = show;
            if (show) {
                notifyItemInserted(getItemCount() - 1);
            } else {
                notifyItemRemoved(getItemCount());
            }
        }
    }

    public void setHasMore(boolean hasMore) {
        this.hasMore = hasMore;
    }

    @Override
    public int getItemViewType(int position) {
        int forumCount = forums.isEmpty() ? 0 : 2; // header + grid
        int hotRankCount = hotRanks.isEmpty() ? 0 : 1; // container
        int postCount = posts.isEmpty() ? 0 : 1 + posts.size(); // header + items
        
        if (position == 0 && !forums.isEmpty()) return TYPE_FORUM_HEADER;
        if (position == 1 && !forums.isEmpty()) return TYPE_FORUM_GRID;
        if (position == forumCount && !hotRanks.isEmpty()) return TYPE_HOT_RANK_CONTAINER;
        
        int postStart = forumCount + hotRankCount;
        if (position == postStart && !posts.isEmpty()) return TYPE_POST_HEADER;
        if (position > postStart && position < postStart + postCount) {
            return TYPE_POST;
        }
        
        return TYPE_LOAD_MORE;
    }

    @Override
    public int getItemCount() {
        int count = 0;
        
        // 板块区域
        if (!forums.isEmpty()) {
            count += 2; // header + grid
        }
        
        // 热帖排行容器
        if (!hotRanks.isEmpty()) {
            count += 1;
        }
        
        // 帖子列表 - 只有有帖子时才显示 header
        if (!posts.isEmpty()) {
            count += 1 + posts.size(); // header + items
        }
        
        // 加载更多
        if (showLoadMore) {
            count += 1;
        }
        
        return count;
    }

    private int getPostPosition(int adapterPosition) {
        int forumCount = forums.isEmpty() ? 0 : 2;
        int hotRankCount = hotRanks.isEmpty() ? 0 : 1;
        return adapterPosition - forumCount - hotRankCount - 1;
    }

    private int getAdapterPositionForPost(int postPosition) {
        int forumCount = forums.isEmpty() ? 0 : 2;
        int hotRankCount = hotRanks.isEmpty() ? 0 : 1;
        return forumCount + hotRankCount + 1 + postPosition;
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        
        switch (viewType) {
            case TYPE_FORUM_HEADER:
            case TYPE_POST_HEADER:
                return new HeaderViewHolder(ItemHeaderBinding.inflate(inflater, parent, false));
                
            case TYPE_FORUM_GRID:
                View forumGridView = inflater.inflate(R.layout.item_forum_grid_container, parent, false);
                return new ForumGridViewHolder(forumGridView);
                
            case TYPE_HOT_RANK_CONTAINER:
                return new HotRankContainerViewHolder(ItemHotRankContainerBinding.inflate(inflater, parent, false));
                
            case TYPE_POST:
                return new PostViewHolder(ItemPostBinding.inflate(inflater, parent, false));
                
            case TYPE_LOAD_MORE:
                return new LoadMoreViewHolder(ItemLoadMoreBinding.inflate(inflater, parent, false));
                
            default:
                throw new IllegalArgumentException("Unknown view type: " + viewType);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        switch (holder.getItemViewType()) {
            case TYPE_FORUM_HEADER:
                ((HeaderViewHolder) holder).bind("热门版块", false);
                break;
                
            case TYPE_FORUM_GRID:
                ((ForumGridViewHolder) holder).bind(forums, forumClickListener);
                break;
                
            case TYPE_HOT_RANK_CONTAINER:
                ((HotRankContainerViewHolder) holder).bind(hotRanks, postClickListener);
                break;
                
            case TYPE_POST_HEADER:
                ((HeaderViewHolder) holder).bind("热门推荐", false);
                break;
                
            case TYPE_POST:
                int postPos = getPostPosition(position);
                if (postPos >= 0 && postPos < posts.size()) {
                    ((PostViewHolder) holder).bind(posts.get(postPos), postPos, postClickListener, userClickListener);
                }
                break;
                
            case TYPE_LOAD_MORE:
                ((LoadMoreViewHolder) holder).bind(hasMore, loadMoreListener);
                break;
        }
    }

    // ==================== ViewHolders ====================

    static class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final ItemHeaderBinding binding;

        HeaderViewHolder(ItemHeaderBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(String title, boolean showMore) {
            binding.headerTitle.setText(title);
            binding.moreButton.setVisibility(showMore ? View.VISIBLE : View.GONE);
        }
    }

    static class ForumGridViewHolder extends RecyclerView.ViewHolder {
        private final RecyclerView recyclerView;

        ForumGridViewHolder(View itemView) {
            super(itemView);
            recyclerView = itemView.findViewById(R.id.forumGridRecycler);
        }

        void bind(List<Forum> forums, OnForumClickListener listener) {
            if (recyclerView.getAdapter() == null) {
                recyclerView.setLayoutManager(new GridLayoutManager(itemView.getContext(), 4));
                ForumGridAdapter adapter = new ForumGridAdapter();
                adapter.setOnForumClickListener((forum, iconView) -> {
                    if (listener != null) listener.onForumClick(forum, iconView);
                });
                recyclerView.setAdapter(adapter);
            }
            ((ForumGridAdapter) recyclerView.getAdapter()).setData(forums);
        }
    }

    static class HotRankContainerViewHolder extends RecyclerView.ViewHolder {
        private final ItemHotRankContainerBinding binding;
        private HotRankItemAdapter adapter;

        HotRankContainerViewHolder(ItemHotRankContainerBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(List<Post> hotRanks, OnPostClickListener listener) {
            if (adapter == null) {
                adapter = new HotRankItemAdapter();
                adapter.setOnPostClickListener(listener);
                binding.hotRankRecycler.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
                binding.hotRankRecycler.setAdapter(adapter);
            }
            adapter.setData(hotRanks);
        }
    }

    static class PostViewHolder extends RecyclerView.ViewHolder {
        private final ItemPostBinding binding;
        private Post currentPost; // 保存当前绑定的 post

        PostViewHolder(ItemPostBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(Post post, int position, OnPostClickListener clickListener, OnUserClickListener userClickListener) {
            // 保存当前 post 引用
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
            binding.postTitle.setText(post.getTitle());
            
            // 摘要
            String summary = post.getSummary();
            if (summary != null && !summary.isEmpty()) {
                binding.postSummary.setText(summary);
                binding.postSummary.setVisibility(View.VISIBLE);
            } else {
                binding.postSummary.setVisibility(View.GONE);
            }
            
            // 作者头像
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
            
            // 作者
            binding.authorName.setText(post.getAuthor());
            
            // 等级
            if (post.getAuthorLevel() != null && !post.getAuthorLevel().isEmpty()) {
                binding.authorLevel.setText(post.getAuthorLevel());
                binding.authorLevel.setVisibility(View.VISIBLE);
                setLevelBackground(binding.authorLevel, post.getAuthorLevel());
            } else {
                binding.authorLevel.setVisibility(View.GONE);
            }
            
            // 性别图标
            if (post.getAuthorGender() == 1) {
                binding.genderIcon.setImageResource(R.drawable.ic_gender_male);
                binding.genderIcon.setVisibility(View.VISIBLE);
            } else if (post.getAuthorGender() == 2) {
                binding.genderIcon.setImageResource(R.drawable.ic_gender_female);
                binding.genderIcon.setVisibility(View.VISIBLE);
            } else {
                binding.genderIcon.setVisibility(View.GONE);
            }
            
            // 时间
            String dateStr = post.getDateStr();
            if (dateStr != null && !dateStr.isEmpty()) {
                binding.postTime.setText(dateStr);
            } else {
                binding.postTime.setText("刚刚");
            }
            
            // 板块名称
            String forumName = post.getForumName();
            if (forumName != null && !forumName.isEmpty()) {
                binding.forumName.setText(forumName);
                binding.forumName.setVisibility(View.VISIBLE);
            } else {
                binding.forumName.setVisibility(View.GONE);
            }
            
            // 统计信息
            binding.viewCount.setText(formatCount(post.getViews()));
            binding.replyCount.setText(formatCount(post.getReplies()));
            binding.likeCount.setText(formatCount(post.getLikes()));
            
            // 用户头像和用户名点击事件 - 都传递头像视图用于共享元素动画
            View.OnClickListener userClick = v -> {
                if (userClickListener != null) {
                    userClickListener.onUserClick(post.getAuthorId(), post.getAuthor(), post.getAuthorAvatar(), binding.authorAvatar);
                }
            };
            binding.authorAvatar.setOnClickListener(userClick);
            binding.authorName.setOnClickListener(userClick);
            
            // 缩略图网格
            List<String> thumbnails = post.getThumbnails();
            
            // 检查是否应该加载缩略图
            boolean shouldLoadImages = AppSettings.getInstance(itemView.getContext()).shouldLoadThumbnail();
            
            // 不加载图片时，直接隐藏缩略图容器
            if (!shouldLoadImages) {
                binding.thumbnailsContainer.setVisibility(View.GONE);
            } else if (thumbnails != null && !thumbnails.isEmpty()) {
                // 设置GridLayoutManager，每排3张
                int spanCount = 3;
                GridLayoutManager gridLayoutManager = new GridLayoutManager(itemView.getContext(), spanCount);
                binding.thumbnailsGrid.setLayoutManager(gridLayoutManager);
                
                // 创建缩略图Adapter
                ThumbnailGridAdapter thumbnailAdapter = new ThumbnailGridAdapter(thumbnails, true);
                binding.thumbnailsGrid.setAdapter(thumbnailAdapter);
                binding.thumbnailsContainer.setVisibility(View.VISIBLE);
                
                // 使用透明触摸层拦截所有点击事件
                binding.touchInterceptor.setOnClickListener(v -> {
                    if (clickListener != null && currentPost != null) {
                        clickListener.onPostClick(currentPost, binding.authorAvatar);
                    }
                });
            } else {
                binding.thumbnailsContainer.setVisibility(View.GONE);
            }
            
            // 整个卡片点击事件 - 传递头像View用于共享元素动画
            itemView.setOnClickListener(v -> {
                if (clickListener != null) {
                    clickListener.onPostClick(post, binding.authorAvatar);
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
            FontUtils.applyFontSize(itemView.getContext(), binding.forumName, FontUtils.SIZE_SMALL);
            FontUtils.applyFontSize(itemView.getContext(), binding.postTime, FontUtils.SIZE_TIME);
            FontUtils.applyFontSize(itemView.getContext(), binding.viewCount, FontUtils.SIZE_SMALL);
            FontUtils.applyFontSize(itemView.getContext(), binding.replyCount, FontUtils.SIZE_SMALL);
            FontUtils.applyFontSize(itemView.getContext(), binding.likeCount, FontUtils.SIZE_SMALL);
        }

        private void setLevelBackground(TextView view, String level) {
            try {
                String numStr = level.replaceAll("[^0-9]", "");
                int lv = Integer.parseInt(numStr);
                int color;
                if (lv <= 2) {
                    color = Color.parseColor("#999999");
                } else if (lv <= 4) {
                    color = Color.parseColor("#4CAF50");
                } else if (lv <= 6) {
                    color = Color.parseColor("#2196F3");
                } else if (lv <= 8) {
                    color = Color.parseColor("#9C27B0");
                } else {
                    color = Color.parseColor("#FF9800");
                }
                view.setBackgroundTintList(android.content.res.ColorStateList.valueOf(color));
            } catch (Exception e) {
                view.setBackgroundTintList(android.content.res.ColorStateList.valueOf(Color.parseColor("#999999")));
            }
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

    static class LoadMoreViewHolder extends RecyclerView.ViewHolder {
        private final ItemLoadMoreBinding binding;

        LoadMoreViewHolder(ItemLoadMoreBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }

        void bind(boolean hasMore, OnLoadMoreListener listener) {
            if (hasMore) {
                binding.progressBar.setVisibility(View.VISIBLE);
                binding.loadMoreText.setText("加载中...");
                if (listener != null) {
                    listener.onLoadMore();
                }
            } else {
                binding.progressBar.setVisibility(View.GONE);
                binding.loadMoreText.setText("没有更多了");
            }
        }
    }
    
    // 热帖排行内部Adapter
    static class HotRankItemAdapter extends RecyclerView.Adapter<HotRankItemAdapter.HotItemViewHolder> {
        private List<Post> items = new ArrayList<>();
        private OnPostClickListener listener;

        void setOnPostClickListener(OnPostClickListener listener) {
            this.listener = listener;
        }

        void setData(List<Post> items) {
            this.items = items != null ? items : new ArrayList<>();
            notifyDataSetChanged();
        }

        @NonNull
        @Override
        public HotItemViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            return new HotItemViewHolder(ItemHotRankBinding.inflate(
                LayoutInflater.from(parent.getContext()), parent, false));
        }

        @Override
        public void onBindViewHolder(@NonNull HotItemViewHolder holder, int position) {
            holder.bind(items.get(position), position + 1, listener);
        }

        @Override
        public int getItemCount() {
            return items.size();
        }

        static class HotItemViewHolder extends RecyclerView.ViewHolder {
            private final ItemHotRankBinding binding;

            HotItemViewHolder(ItemHotRankBinding binding) {
                super(binding.getRoot());
                this.binding = binding;
            }

            void bind(Post post, int rank, OnPostClickListener listener) {
                binding.rankNumber.setText(String.valueOf(rank));
                
                int bgColor;
                if (rank <= 3) {
                    bgColor = Color.parseColor("#FF6B4A");
                    binding.rankNumber.setTextColor(Color.WHITE);
                } else {
                    bgColor = Color.parseColor("#EEEEEE");
                    binding.rankNumber.setTextColor(Color.parseColor("#666666"));
                }
                binding.rankNumber.setBackgroundTintList(android.content.res.ColorStateList.valueOf(bgColor));
                
                binding.postTitle.setText(post.getTitle());
                binding.postTitle.setOnClickListener(v -> {
                    if (listener != null) listener.onPostClick(post, null);
                });
            }
        }
    }
    
    /**
     * 缩略图网格Adapter - 显示帖子图片缩略图
     */
    static class ThumbnailGridAdapter extends RecyclerView.Adapter<ThumbnailGridAdapter.ThumbnailViewHolder> {
        
        private final List<String> thumbnails;
        private final boolean shouldLoadImages;
        private static final int MAX_THUMBNAILS = 9; // 最多显示9张
        
        ThumbnailGridAdapter(List<String> thumbnails, boolean shouldLoadImages) {
            // 限制最多显示9张
            if (thumbnails.size() > MAX_THUMBNAILS) {
                this.thumbnails = thumbnails.subList(0, MAX_THUMBNAILS);
            } else {
                this.thumbnails = thumbnails;
            }
            this.shouldLoadImages = shouldLoadImages;
        }
        
        @NonNull
        @Override
        public ThumbnailViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            ImageView imageView = new ImageView(parent.getContext());
            int size = (parent.getMeasuredWidth() - 12) / 3; // 3列，减去padding
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
}
