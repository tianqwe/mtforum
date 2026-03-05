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
import com.forum.mt.databinding.ItemSearchUserBinding;
import com.forum.mt.model.User;
import com.forum.mt.util.FontUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 搜索用户结果适配器
 */
public class SearchUserAdapter extends RecyclerView.Adapter<SearchUserAdapter.ViewHolder> {
    
    private List<User> users = new ArrayList<>();
    private OnUserClickListener listener;
    
    public interface OnUserClickListener {
        void onUserClick(User user, View avatarView);
        void onFollowClick(User user, int position);
    }
    
    public void setOnUserClickListener(OnUserClickListener listener) {
        this.listener = listener;
    }
    
    public void setUsers(List<User> users) {
        this.users = users != null ? users : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    public void addUsers(List<User> newUsers) {
        if (newUsers != null && !newUsers.isEmpty()) {
            int startPos = this.users.size();
            this.users.addAll(newUsers);
            notifyItemRangeInserted(startPos, newUsers.size());
        }
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ItemSearchUserBinding binding = ItemSearchUserBinding.inflate(
            LayoutInflater.from(parent.getContext()), parent, false);
        return new ViewHolder(binding);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        User user = users.get(position);
        holder.bind(user);
    }
    
    @Override
    public int getItemCount() {
        return users.size();
    }
    
    class ViewHolder extends RecyclerView.ViewHolder {
        private final ItemSearchUserBinding binding;
        
        ViewHolder(ItemSearchUserBinding binding) {
            super(binding.getRoot());
            this.binding = binding;
        }
        
        void bind(User user) {
            // 用户名
            binding.tvUsername.setText(user.getUsername());
            
            // 用户组
            binding.tvGroupName.setText(user.getGroupName() != null ? user.getGroupName() : "会员");
            
            // 积分
            binding.tvCredits.setText(String.valueOf(user.getCredits()));
            
            // 头像
            Glide.with(binding.imgAvatar.getContext())
                .load(user.getAvatarUrl())
                .placeholder(R.drawable.ic_user)
                .error(R.drawable.ic_user)
                .circleCrop()
                .into(binding.imgAvatar);
            
            // 用户头像和用户名点击事件 - 都传递头像视图用于共享元素动画
            View.OnClickListener userClick = v -> {
                if (listener != null) {
                    listener.onUserClick(user, binding.imgAvatar);
                }
            };
            binding.imgAvatar.setOnClickListener(userClick);
            binding.tvUsername.setOnClickListener(userClick);
            binding.getRoot().setOnClickListener(userClick);
            
            // 关注按钮状态
            updateFollowButton(user.isFollowed());
            
            // 关注按钮
            binding.btnFollow.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onFollowClick(user, getAdapterPosition());
                }
            });
            
            // 应用字体大小设置
            applyFontSize();
        }
        
        /**
         * 应用字体大小设置到各TextView
         */
        private void applyFontSize() {
            FontUtils.applyFontSize(itemView.getContext(), binding.tvUsername, FontUtils.SIZE_USERNAME);
            FontUtils.applyFontSize(itemView.getContext(), binding.tvGroupName, FontUtils.SIZE_SMALL);
            FontUtils.applyFontSize(itemView.getContext(), binding.tvCredits, FontUtils.SIZE_SMALL);
        }
        
        /**
         * 更新关注按钮状态
         */
        private void updateFollowButton(boolean isFollowed) {
            if (isFollowed) {
                binding.btnFollow.setText("已关注");
                binding.btnFollow.setTextColor(itemView.getContext().getResources().getColor(R.color.text_gray));
                binding.btnFollow.setStrokeColorResource(R.color.text_gray);
            } else {
                binding.btnFollow.setText("关注");
                binding.btnFollow.setTextColor(itemView.getContext().getResources().getColor(R.color.link_color));
                binding.btnFollow.setStrokeColorResource(R.color.link_color);
            }
        }
        
        /**
         * 更新用户关注状态（供外部调用）
         */
        public void updateFollowStatus(boolean followed) {
            updateFollowButton(followed);
        }
    }
}
