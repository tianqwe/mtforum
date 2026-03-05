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

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * 我的评论列表适配器
 * 显示用户回复过的帖子，包含评论摘要
 */
public class MyReplyAdapter extends ListAdapter<Post, MyReplyAdapter.ViewHolder> {
    
    public interface OnItemClickListener {
        void onItemClick(Post post, View avatarView);
    }
    
    public interface OnUserClickListener {
        void onUserClick(int uid, String username, String avatar, View avatarView);
    }
    
    private OnItemClickListener listener;
    private OnUserClickListener userClickListener;
    
    private static final DiffUtil.ItemCallback<Post> DIFF_CALLBACK = new DiffUtil.ItemCallback<Post>() {
        @Override
        public boolean areItemsTheSame(@NonNull Post oldItem, @NonNull Post newItem) {
            return oldItem.getTid() == newItem.getTid();
        }
        
        @Override
        public boolean areContentsTheSame(@NonNull Post oldItem, @NonNull Post newItem) {
            return safeEquals(oldItem.getTitle(), newItem.getTitle())
                    && safeEquals(oldItem.getMyReplyContent(), newItem.getMyReplyContent())
                    && safeEquals(oldItem.getDateStr(), newItem.getDateStr());
        }
        
        private boolean safeEquals(String a, String b) {
            if (a == null && b == null) return true;
            if (a == null || b == null) return false;
            return a.equals(b);
        }
    };
    
    public MyReplyAdapter() {
        super(DIFF_CALLBACK);
    }
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
    
    public void setOnUserClickListener(OnUserClickListener listener) {
        this.userClickListener = listener;
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_my_reply, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Post post = getItem(position);
        holder.bind(post, listener, userClickListener);
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        private final TextView titleView;
        private final TextView replyContentView;
        private final CircleImageView authorAvatar;
        private final TextView authorNameView;
        private final TextView authorLevelView;
        private final TextView forumNameView;
        private final TextView timeView;
        private final TextView statsView;
        
        ViewHolder(View itemView) {
            super(itemView);
            titleView = itemView.findViewById(R.id.postTitle);
            replyContentView = itemView.findViewById(R.id.myReplyContent);
            authorAvatar = itemView.findViewById(R.id.authorAvatar);
            authorNameView = itemView.findViewById(R.id.authorName);
            authorLevelView = itemView.findViewById(R.id.authorLevel);
            forumNameView = itemView.findViewById(R.id.forumName);
            timeView = itemView.findViewById(R.id.replyTime);
            statsView = itemView.findViewById(R.id.statsView);
        }
        
        void bind(Post post, OnItemClickListener listener, OnUserClickListener userClickListener) {
            // 帖子标题
            String title = post.getTitle();
            titleView.setText(title != null && !title.isEmpty() ? title : "无标题");
            
            // 我的评论内容摘要
            String replyContent = post.getMyReplyContent();
            if (replyContent != null && !replyContent.isEmpty()) {
                replyContentView.setText(replyContent);
                replyContentView.setVisibility(View.VISIBLE);
            } else {
                String summary = post.getSummary();
                if (summary != null && !summary.isEmpty()) {
                    replyContentView.setText(summary);
                    replyContentView.setVisibility(View.VISIBLE);
                } else {
                    replyContentView.setVisibility(View.GONE);
                }
            }
            
            // 原帖作者头像
            String avatar = post.getAuthorAvatar();
            if (avatar != null && !avatar.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(avatar)
                        .placeholder(R.drawable.ic_forum)
                        .error(R.drawable.ic_forum)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(authorAvatar);
            } else {
                authorAvatar.setImageResource(R.drawable.ic_forum);
            }
            
            // 原帖作者名
            String author = post.getAuthor();
            authorNameView.setText(author != null && !author.isEmpty() ? author : "匿名");
            
            // 原帖作者等级
            String level = post.getAuthorLevel();
            if (level != null && !level.isEmpty()) {
                authorLevelView.setText(level);
                authorLevelView.setVisibility(View.VISIBLE);
                setLevelBackground(authorLevelView, level);
            } else {
                authorLevelView.setVisibility(View.GONE);
            }
            
            // 版块名称
            String forumName = post.getForumName();
            if (forumName != null && !forumName.isEmpty()) {
                forumNameView.setText(forumName);
                forumNameView.setVisibility(View.VISIBLE);
            } else {
                forumNameView.setVisibility(View.GONE);
            }
            
            // 评论时间
            String dateStr = post.getDateStr();
            timeView.setText(dateStr != null && !dateStr.isEmpty() ? dateStr : "刚刚");
            
            // 统计信息
            int replies = post.getReplies();
            int views = post.getViews();
            int likes = post.getLikes();
            StringBuilder stats = new StringBuilder();
            if (replies > 0) {
                stats.append(replies).append("回复");
            }
            if (views > 0) {
                if (stats.length() > 0) stats.append(" · ");
                stats.append(formatCount(views)).append("浏览");
            }
            if (likes > 0) {
                if (stats.length() > 0) stats.append(" · ");
                stats.append(likes).append("赞");
            }
            statsView.setText(stats.toString());
            statsView.setVisibility(stats.length() > 0 ? View.VISIBLE : View.GONE);
            
            // 用户头像和用户名点击事件 - 都传递头像视图用于共享元素动画
            View.OnClickListener userClick = v -> {
                if (userClickListener != null) {
                    userClickListener.onUserClick(post.getAuthorId(), post.getAuthor(), post.getAuthorAvatar(), authorAvatar);
                }
            };
            authorAvatar.setOnClickListener(userClick);
            authorNameView.setOnClickListener(userClick);
            
            // 点击事件 - 传递头像视图用于共享元素动画
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
            FontUtils.applyFontSize(itemView.getContext(), replyContentView, FontUtils.SIZE_SUBTITLE);
            FontUtils.applyFontSize(itemView.getContext(), authorNameView, FontUtils.SIZE_USERNAME);
            FontUtils.applyFontSize(itemView.getContext(), authorLevelView, FontUtils.SIZE_SMALL);
            FontUtils.applyFontSize(itemView.getContext(), forumNameView, FontUtils.SIZE_SMALL);
            FontUtils.applyFontSize(itemView.getContext(), timeView, FontUtils.SIZE_TIME);
            FontUtils.applyFontSize(itemView.getContext(), statsView, FontUtils.SIZE_SMALL);
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
