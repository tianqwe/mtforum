package com.forum.mt.ui.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.forum.mt.R;
import com.forum.mt.model.Post;
import com.forum.mt.util.FontUtils;

/**
 * 帖子列表适配器 - 使用 ListAdapter + DiffUtil (谷歌推荐方式)
 */
public class PostAdapter extends ListAdapter<Post, PostAdapter.ViewHolder> {
    
    // 静态缓存等级颜色，避免重复解析
    private static final android.util.LruCache<String, Integer> LEVEL_COLOR_CACHE = new android.util.LruCache<>(20);
    
    public interface OnItemClickListener {
        void onItemClick(Post post, View avatarView);
    }
    
    public interface OnUserClickListener {
        void onUserClick(int uid, String username, String avatar, View avatarView);
    }
    
    private final OnItemClickListener listener;
    private OnUserClickListener userClickListener;
    
    private static final DiffUtil.ItemCallback<Post> DIFF_CALLBACK = new DiffUtil.ItemCallback<Post>() {
        @Override
        public boolean areItemsTheSame(@NonNull Post oldItem, @NonNull Post newItem) {
            return oldItem.getTid() == newItem.getTid();
        }
        
        @Override
        public boolean areContentsTheSame(@NonNull Post oldItem, @NonNull Post newItem) {
            return oldItem.getTitle() != null && oldItem.getTitle().equals(newItem.getTitle())
                    && oldItem.getSummary() != null && oldItem.getSummary().equals(newItem.getSummary())
                    && oldItem.getLikes() == newItem.getLikes()
                    && oldItem.getReplies() == newItem.getReplies();
        }
    };
    
    public PostAdapter(OnItemClickListener listener) {
        super(DIFF_CALLBACK);
        this.listener = listener;
    }
    
    public void setOnUserClickListener(OnUserClickListener listener) {
        this.userClickListener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
            .inflate(R.layout.item_post, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Post post = getItem(position);
        holder.bind(post, position, listener, userClickListener);
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleView;
        private final TextView summaryView;
        private final TextView authorView;
        private final TextView levelView;
        private final ImageView genderIcon;
        private final TextView forumNameView;
        private final TextView timeView;
        private final TextView replyCountView;
        private final TextView viewCountView;
        private final TextView likeCountView;
        private final ViewGroup thumbnailsContainer;
        private final ImageView authorAvatar;
        private final TextView bountyTypeTag;
        private final TextView bountyAmountTag;
        
        ViewHolder(View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.postTitle);
            summaryView = itemView.findViewById(R.id.postSummary);
            authorView = itemView.findViewById(R.id.authorName);
            levelView = itemView.findViewById(R.id.authorLevel);
            genderIcon = itemView.findViewById(R.id.genderIcon);
            forumNameView = itemView.findViewById(R.id.forumName);
            timeView = itemView.findViewById(R.id.postTime);
            replyCountView = itemView.findViewById(R.id.replyCount);
            viewCountView = itemView.findViewById(R.id.viewCount);
            likeCountView = itemView.findViewById(R.id.likeCount);
            thumbnailsContainer = itemView.findViewById(R.id.thumbnailsContainer);
            authorAvatar = itemView.findViewById(R.id.authorAvatar);
            bountyTypeTag = itemView.findViewById(R.id.bountyTypeTag);
            bountyAmountTag = itemView.findViewById(R.id.bountyAmountTag);
        }
        
        void bind(Post post, int position, OnItemClickListener listener, OnUserClickListener userClickListener) {
            // 悬赏标签
            if (post.hasBounty()) {
                if (post.getBountyType() != null && !post.getBountyType().isEmpty()) {
                    bountyTypeTag.setText(post.getBountyType());
                    bountyTypeTag.setVisibility(View.VISIBLE);
                } else {
                    bountyTypeTag.setVisibility(View.GONE);
                }
                
                if (post.getBountyText() != null && !post.getBountyText().isEmpty()) {
                    bountyAmountTag.setText(post.getBountyText());
                    bountyAmountTag.setVisibility(View.VISIBLE);
                } else if (post.getBountyAmount() > 0) {
                    bountyAmountTag.setText(post.getBountyAmount() + "金币");
                    bountyAmountTag.setVisibility(View.VISIBLE);
                } else {
                    bountyAmountTag.setVisibility(View.GONE);
                }
            } else {
                bountyTypeTag.setVisibility(View.GONE);
                bountyAmountTag.setVisibility(View.GONE);
            }
            
            // 标题
            String title = post.getTitle();
            titleView.setText(title != null && !title.isEmpty() ? title : "无标题");
            
            // 摘要
            String summary = post.getSummary();
            if (summary != null && !summary.isEmpty()) {
                summaryView.setText(summary);
                summaryView.setVisibility(View.VISIBLE);
            } else {
                summaryView.setVisibility(View.GONE);
            }
            
            // 作者
            String author = post.getAuthor();
            authorView.setText(author != null && !author.isEmpty() ? author : "匿名");
            
            // 等级和性别
            String level = post.getAuthorLevel();
            int gender = post.getAuthorGender();
            
            if (level != null && !level.isEmpty()) {
                levelView.setText(level);
                levelView.setVisibility(View.VISIBLE);
                setLevelBackground(levelView, level);
            } else {
                levelView.setVisibility(View.GONE);
            }
            
            // 性别图标
            if (gender == 1) {
                genderIcon.setImageResource(R.drawable.ic_gender_male);
                genderIcon.setVisibility(View.VISIBLE);
            } else if (gender == 2) {
                genderIcon.setImageResource(R.drawable.ic_gender_female);
                genderIcon.setVisibility(View.VISIBLE);
            } else {
                genderIcon.setVisibility(View.GONE);
            }
            
            // 版块名称
            String forumName = post.getForumName();
            if (forumName != null && !forumName.isEmpty()) {
                forumNameView.setText(forumName);
                forumNameView.setVisibility(View.VISIBLE);
            } else {
                forumNameView.setVisibility(View.GONE);
            }
            
            // 时间
            String dateStr = post.getDateStr();
            if (dateStr != null && !dateStr.isEmpty()) {
                timeView.setText(dateStr);
            } else {
                timeView.setText("刚刚");
            }
            
            // 回复数
            int replies = post.getReplies();
            replyCountView.setText(formatCount(replies));
            
            // 浏览数
            int views = post.getViews();
            viewCountView.setText(formatCount(views));
            
            // 点赞数
            int likes = post.getLikes();
            if (likes > 0) {
                likeCountView.setText(formatCount(likes));
            } else {
                likeCountView.setText("赞");
            }
            
            // 作者头像 - 使用磁盘缓存优化滑动流畅度
            String avatar = post.getAuthorAvatar();
            if (avatar != null && !avatar.isEmpty()) {
                Glide.with(itemView.getContext())
                    .load(avatar)
                    .circleCrop()
                    .placeholder(R.drawable.ic_forum)
                    .error(R.drawable.ic_forum)
                    .diskCacheStrategy(DiskCacheStrategy.AUTOMATIC) // 启用自动缓存
                    .into(authorAvatar);
            } else {
                authorAvatar.setImageResource(R.drawable.ic_forum);
            }
            
            // 缩略图容器 - 简单显示/隐藏
            // 注意：这里只处理单张缩略图的显示，多图网格需要更复杂的处理
            String thumbnail = post.getThumbnail();
            if (thumbnail != null && !thumbnail.isEmpty() && thumbnailsContainer != null) {
                thumbnailsContainer.setVisibility(View.VISIBLE);
            } else if (thumbnailsContainer != null) {
                thumbnailsContainer.setVisibility(View.GONE);
            }
            
            // 用户头像和名称点击事件 - 都传递头像视图用于共享元素动画
            View.OnClickListener userClickListenerImpl = v -> {
                if (userClickListener != null) {
                    userClickListener.onUserClick(post.getAuthorId(), post.getAuthor(), post.getAuthorAvatar(), authorAvatar);
                }
            };
            authorAvatar.setOnClickListener(userClickListenerImpl);
            authorView.setOnClickListener(userClickListenerImpl);
            
            // 整个卡片点击事件 - 传递头像视图用于共享元素动画
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(post, authorAvatar);
                }
            });
            
            // 应用字体大小设置
            applyFontSize();
        }
        
        /**
         * 应用字体大小设置到各TextView
         */
        private void applyFontSize() {
            FontUtils.applyFontSize(itemView.getContext(), titleView, FontUtils.SIZE_TITLE);
            FontUtils.applyFontSize(itemView.getContext(), summaryView, FontUtils.SIZE_SUBTITLE);
            FontUtils.applyFontSize(itemView.getContext(), authorView, FontUtils.SIZE_USERNAME);
            FontUtils.applyFontSize(itemView.getContext(), levelView, FontUtils.SIZE_SMALL);
            FontUtils.applyFontSize(itemView.getContext(), forumNameView, FontUtils.SIZE_SMALL);
            FontUtils.applyFontSize(itemView.getContext(), timeView, FontUtils.SIZE_TIME);
            FontUtils.applyFontSize(itemView.getContext(), replyCountView, FontUtils.SIZE_SMALL);
            FontUtils.applyFontSize(itemView.getContext(), viewCountView, FontUtils.SIZE_SMALL);
            FontUtils.applyFontSize(itemView.getContext(), likeCountView, FontUtils.SIZE_SMALL);
        }
        
        private void setLevelBackground(TextView levelView, String level) {
            // 从缓存获取颜色
            Integer cachedColor = LEVEL_COLOR_CACHE.get(level);
            if (cachedColor != null) {
                setLevelDrawable(levelView, cachedColor);
                return;
            }
            
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
            
            // 缓存颜色
            LEVEL_COLOR_CACHE.put(level, color);
            setLevelDrawable(levelView, color);
        }
        
        private void setLevelDrawable(TextView levelView, int color) {
            GradientDrawable drawable = new GradientDrawable();
            drawable.setCornerRadius(8f);
            drawable.setColor(color);
            levelView.setBackground(drawable);
        }
        
        private String formatCount(int count) {
            if (count >= 10000) {
                return String.format("%.1f万", count / 10000.0);
            } else if (count >= 1000) {
                return String.format("%.1f千", count / 1000.0);
            }
            return String.valueOf(count);
        }
    }
}
