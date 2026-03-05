package com.forum.mt.ui.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.forum.mt.R;
import com.forum.mt.model.Post;
import com.forum.mt.util.FontUtils;

/**
 * 收藏列表适配器
 * 与浏览历史卡片样式保持一致
 */
public class FavoriteAdapter extends ListAdapter<Post, FavoriteAdapter.ViewHolder> {
    
    public interface OnItemClickListener {
        void onItemClick(Post post, int position, View avatarView);
        void onUnfavoriteClick(Post post, int position);
    }
    
    public interface OnUserClickListener {
        void onUserClick(int uid, String username, String avatar, View avatarView);
    }
    
    private OnItemClickListener listener;
    private OnUserClickListener userClickListener;
    private boolean showUnfavoriteButton = true; // 是否显示取消收藏按钮
    
    private static final DiffUtil.ItemCallback<Post> DIFF_CALLBACK = new DiffUtil.ItemCallback<Post>() {
        @Override
        public boolean areItemsTheSame(@NonNull Post oldItem, @NonNull Post newItem) {
            return oldItem.getTid() == newItem.getTid();
        }
        
        @Override
        public boolean areContentsTheSame(@NonNull Post oldItem, @NonNull Post newItem) {
            return oldItem.getTitle() != null && oldItem.getTitle().equals(newItem.getTitle())
                    && oldItem.getSummary() != null && oldItem.getSummary().equals(newItem.getSummary())
                    && oldItem.getReplies() == newItem.getReplies()
                    && oldItem.getViews() == newItem.getViews();
        }
    };
    
    public FavoriteAdapter() {
        super(DIFF_CALLBACK);
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
    
    public void setOnUserClickListener(OnUserClickListener listener) {
        this.userClickListener = listener;
    }
    
    /**
     * 设置是否显示取消收藏按钮
     * 用户详情页的收藏列表不显示此按钮
     */
    public void setShowUnfavoriteButton(boolean show) {
        this.showUnfavoriteButton = show;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_favorite, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Post post = getItem(position);
        holder.bind(post, position, listener, userClickListener);
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        private final ImageView authorAvatar;
        private final TextView author;
        private final ImageView genderIcon;
        private final TextView authorLevel;
        private final TextView favTime;
        private final TextView forumName;
        private final TextView bountyTypeTag;
        private final TextView bountyAmountTag;
        private final TextView title;
        private final TextView summary;
        private final ImageView thumbnail;
        private final CardView thumbnailCard;
        private final TextView viewCount;
        private final TextView replyCount;
        private final TextView tidText;
        private final ImageView unfavoriteButton;
        
        ViewHolder(View itemView) {
            super(itemView);
            authorAvatar = itemView.findViewById(R.id.authorAvatar);
            author = itemView.findViewById(R.id.author);
            genderIcon = itemView.findViewById(R.id.genderIcon);
            authorLevel = itemView.findViewById(R.id.authorLevel);
            favTime = itemView.findViewById(R.id.favTime);
            forumName = itemView.findViewById(R.id.forumName);
            bountyTypeTag = itemView.findViewById(R.id.bountyTypeTag);
            bountyAmountTag = itemView.findViewById(R.id.bountyAmountTag);
            title = itemView.findViewById(R.id.title);
            summary = itemView.findViewById(R.id.summary);
            thumbnail = itemView.findViewById(R.id.thumbnail);
            thumbnailCard = itemView.findViewById(R.id.thumbnailCard);
            viewCount = itemView.findViewById(R.id.viewCount);
            replyCount = itemView.findViewById(R.id.replyCount);
            tidText = itemView.findViewById(R.id.tidText);
            unfavoriteButton = itemView.findViewById(R.id.unfavoriteButton);
        }
        
        void bind(Post post, int position, OnItemClickListener listener, OnUserClickListener userClickListener) {
            // 作者头像
            String avatar = post.getAuthorAvatar();
            if (avatar != null && !avatar.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(avatar)
                        .circleCrop()
                        .placeholder(R.drawable.ic_forum)
                        .error(R.drawable.ic_forum)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(authorAvatar);
            } else {
                authorAvatar.setImageResource(R.drawable.ic_forum);
            }
            
            // 作者名
            String authorName = post.getAuthor();
            author.setText(authorName != null && !authorName.isEmpty() ? authorName : "匿名");
            
            // 性别图标
            int gender = post.getAuthorGender();
            if (gender == 1) {
                genderIcon.setImageResource(R.drawable.ic_gender_male);
                genderIcon.setVisibility(View.VISIBLE);
            } else if (gender == 2) {
                genderIcon.setImageResource(R.drawable.ic_gender_female);
                genderIcon.setVisibility(View.VISIBLE);
            } else {
                genderIcon.setVisibility(View.GONE);
            }
            
            // 等级标签
            String level = post.getAuthorLevel();
            if (level != null && !level.isEmpty()) {
                authorLevel.setText(level);
                authorLevel.setVisibility(View.VISIBLE);
                setLevelBackground(authorLevel, level);
            } else {
                authorLevel.setVisibility(View.GONE);
            }
            
            // 收藏时间（使用帖子发布时间）
            String dateStr = post.getDateStr();
            favTime.setText(dateStr != null && !dateStr.isEmpty() ? dateStr : "未知时间");
            
            // 版块名称
            String forum = post.getForumName();
            forumName.setText(forum != null && !forum.isEmpty() ? forum : "未知版块");
            
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
            String titleText = post.getTitle();
            title.setText(titleText != null && !titleText.isEmpty() ? titleText : "无标题");
            
            // 摘要
            String summaryText = post.getSummary();
            if (summaryText != null && !summaryText.isEmpty()) {
                summary.setText(summaryText);
                summary.setVisibility(View.VISIBLE);
            } else {
                summary.setVisibility(View.GONE);
            }
            
            // 缩略图
            String thumbUrl = post.getThumbnail();
            if (thumbUrl != null && !thumbUrl.isEmpty()) {
                thumbnailCard.setVisibility(View.VISIBLE);
                Glide.with(itemView.getContext())
                        .load(thumbUrl)
                        .centerCrop()
                        .placeholder(R.drawable.ic_forum)
                        .error(R.drawable.ic_forum)
                        .into(thumbnail);
            } else {
                thumbnailCard.setVisibility(View.GONE);
            }
            
            // 统计
            viewCount.setText(formatCount(post.getViews()));
            replyCount.setText(String.valueOf(post.getReplies()));
            tidText.setText("TID: " + post.getTid());
            
            // 用户头像和用户名点击事件 - 都传递头像视图用于共享元素动画
            View.OnClickListener userClick = v -> {
                if (userClickListener != null) {
                    userClickListener.onUserClick(post.getAuthorId(), post.getAuthor(), post.getAuthorAvatar(), authorAvatar);
                }
            };
            authorAvatar.setOnClickListener(userClick);
            author.setOnClickListener(userClick);
            
            // 点击事件 - 传递头像视图用于共享元素动画
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onItemClick(post, getAdapterPosition(), authorAvatar);
                }
            });
            
            // 取消收藏按钮
            FavoriteAdapter adapter = (FavoriteAdapter) getBindingAdapter();
            if (adapter != null && adapter.showUnfavoriteButton) {
                unfavoriteButton.setVisibility(View.VISIBLE);
                unfavoriteButton.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onUnfavoriteClick(post, getAdapterPosition());
                    }
                });
            } else {
                unfavoriteButton.setVisibility(View.GONE);
            }
            
            // 应用字体大小设置
            applyFontSize();
        }
        
        /**
         * 应用字体大小设置到各TextView
         */
        private void applyFontSize() {
            FontUtils.applyFontSize(itemView.getContext(), title, FontUtils.SIZE_TITLE);
            FontUtils.applyFontSize(itemView.getContext(), summary, FontUtils.SIZE_SUBTITLE);
            FontUtils.applyFontSize(itemView.getContext(), author, FontUtils.SIZE_USERNAME);
            FontUtils.applyFontSize(itemView.getContext(), authorLevel, FontUtils.SIZE_SMALL);
            FontUtils.applyFontSize(itemView.getContext(), forumName, FontUtils.SIZE_SMALL);
            FontUtils.applyFontSize(itemView.getContext(), favTime, FontUtils.SIZE_TIME);
            FontUtils.applyFontSize(itemView.getContext(), viewCount, FontUtils.SIZE_SMALL);
            FontUtils.applyFontSize(itemView.getContext(), replyCount, FontUtils.SIZE_SMALL);
            FontUtils.applyFontSize(itemView.getContext(), tidText, FontUtils.SIZE_SMALL);
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
            
            GradientDrawable drawable = new GradientDrawable();
            drawable.setCornerRadius(8f);
            drawable.setColor(color);
            levelView.setBackground(drawable);
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
}
