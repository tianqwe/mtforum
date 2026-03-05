package com.forum.mt.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.forum.mt.R;
import com.forum.mt.databinding.ItemPostBinding;
import com.forum.mt.model.Post;
import com.forum.mt.util.FontUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索帖子结果适配器
 */
public class SearchPostAdapter extends RecyclerView.Adapter<SearchPostAdapter.ViewHolder> {
    
    private List<Post> posts = new ArrayList<>();
    private OnPostClickListener listener;
    private OnUserClickListener userClickListener;
    
    public interface OnPostClickListener {
        void onPostClick(Post post, View avatarView);
    }
    
    public interface OnUserClickListener {
        void onUserClick(int uid, String username, String avatar, View avatarView);
    }
    
    public void setOnPostClickListener(OnPostClickListener listener) {
        this.listener = listener;
    }
    
    public void setOnUserClickListener(OnUserClickListener listener) {
        this.userClickListener = listener;
    }
    
    public void setPosts(List<Post> posts) {
        this.posts = posts != null ? posts : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public void addPosts(List<Post> newPosts) {
        if (newPosts != null && !newPosts.isEmpty()) {
            int startPos = this.posts.size();
            this.posts.addAll(newPosts);
            notifyItemRangeInserted(startPos, newPosts.size());
        }
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemPostBinding binding = ItemPostBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Post post = posts.get(position);
        holder.bind(post);
    }
    
    @Override
    public int getItemCount() {
        return posts.size();
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemPostBinding binding;
        
        ViewHolder(ItemPostBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        
        void bind(Post post) {
            // 标题
            binding.postTitle.setText(post.getTitle() != null ? post.getTitle() : "无标题");
            
            // 摘要
            if (post.getSummary() != null && !post.getSummary().isEmpty()) {
                binding.postSummary.setText(post.getSummary());
                binding.postSummary.setVisibility(View.VISIBLE);
            } else {
                binding.postSummary.setVisibility(View.GONE);
            }
            
            // 作者
            binding.authorName.setText(post.getAuthor() != null ? post.getAuthor() : "未知");
            
            // 等级
            if (post.getAuthorLevel() != null && !post.getAuthorLevel().isEmpty()) {
                binding.authorLevel.setText(post.getAuthorLevel());
                binding.authorLevel.setVisibility(View.VISIBLE);
            } else {
                binding.authorLevel.setVisibility(View.GONE);
            }
            
            // 板块
            binding.forumName.setText(post.getForumName() != null ? post.getForumName() : "");
            
            // 时间
            binding.postTime.setText(post.getDateStr() != null ? post.getDateStr() : "");
            
            // 统计
            binding.viewCount.setText(formatCount(post.getViews()));
            binding.replyCount.setText(formatCount(post.getReplies()));
            binding.likeCount.setText(formatCount(post.getLikes()));
            
            // 头像
            Glide.with(binding.authorAvatar.getContext())
                .load(post.getAuthorAvatar())
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .circleCrop()
                .into(binding.authorAvatar);
            
            // 隐藏缩略图容器
            binding.thumbnailsContainer.setVisibility(View.GONE);
            
            // 隐藏悬赏标签
            binding.bountyTypeTag.setVisibility(View.GONE);
            binding.bountyAmountTag.setVisibility(View.GONE);
            
            // 用户头像和用户名点击事件 - 都传递头像视图用于共享元素动画
            View.OnClickListener userClick = v -> {
                if (userClickListener != null) {
                    userClickListener.onUserClick(post.getAuthorId(), post.getAuthor(), post.getAuthorAvatar(), binding.authorAvatar);
                }
            };
            binding.authorAvatar.setOnClickListener(userClick);
            binding.authorName.setOnClickListener(userClick);
            
            // 点击事件 - 传递头像视图用于共享元素动画
            binding.getRoot().setOnClickListener(v -> {
                if (listener != null) {
                    listener.onPostClick(post, binding.authorAvatar);
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
        
        private String formatCount(int count) {
            if (count >= 10000) {
                return String.format("%.1fw", count / 10000.0);
            } else if (count >= 1000) {
                return String.format("%.1fk", count / 1000.0);
            }
            return String.valueOf(count);
        }
    }
}
