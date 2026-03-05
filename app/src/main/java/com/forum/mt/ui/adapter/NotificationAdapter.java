package com.forum.mt.ui.adapter;

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
import com.forum.mt.R;
import com.forum.mt.model.Notification;
import com.forum.mt.util.FontUtils;

/**
 * 通知列表适配器
 */
public class NotificationAdapter extends ListAdapter<Notification, NotificationAdapter.NotificationViewHolder> {
    
    private OnNotificationClickListener listener;
    
    public NotificationAdapter() {
        super(DIFF_CALLBACK);
    }
    
    private static final DiffUtil.ItemCallback<Notification> DIFF_CALLBACK = 
            new DiffUtil.ItemCallback<Notification>() {
        @Override
        public boolean areItemsTheSame(@NonNull Notification oldItem, @NonNull Notification newItem) {
            return oldItem.getId() == newItem.getId();
        }
        
        @Override
        public boolean areContentsTheSame(@NonNull Notification oldItem, @NonNull Notification newItem) {
            return oldItem.getId() == newItem.getId() &&
                   oldItem.isRead() == newItem.isRead();
        }
    };
    
    @NonNull
    @Override
    public NotificationViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_notification, parent, false);
        return new NotificationViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull NotificationViewHolder holder, int position) {
        Notification notification = getItem(position);
        holder.bind(notification);
    }
    
    public void setOnNotificationClickListener(OnNotificationClickListener listener) {
        this.listener = listener;
    }
    
    class NotificationViewHolder extends RecyclerView.ViewHolder {
        private ImageView iconImage;
        private ImageView avatarImage;
        private TextView contentText;
        private TextView timeText;
        private View unreadIndicator;
        
        NotificationViewHolder(@NonNull View itemView) {
            super(itemView);
            iconImage = itemView.findViewById(R.id.icon_image);
            avatarImage = itemView.findViewById(R.id.avatar_image);
            contentText = itemView.findViewById(R.id.content_text);
            timeText = itemView.findViewById(R.id.time_text);
            unreadIndicator = itemView.findViewById(R.id.unread_indicator);
            
            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onNotificationClick(getItem(pos));
                }
            });
        }
        
        void bind(Notification notification) {
            contentText.setText(notification.getContent());
            timeText.setText(notification.getDateline());
            
            // 应用字体大小设置
            FontUtils.applyFontSize(itemView.getContext(), contentText, FontUtils.SIZE_SUBTITLE);
            FontUtils.applyFontSize(itemView.getContext(), timeText, FontUtils.SIZE_TIME);
            
            // 未读指示器
            unreadIndicator.setVisibility(notification.isRead() ? View.GONE : View.VISIBLE);
            
            // 根据类型设置图标
            int iconRes = getIconForType(notification.getType());
            iconImage.setImageResource(iconRes);
            
            // 头像（如果有）
            String avatarUrl = notification.getAvatarUrl();
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(avatarUrl)
                        .circleCrop()
                        .placeholder(R.drawable.ic_user_avatar_placeholder)
                        .error(R.drawable.ic_user_avatar_placeholder)
                        .into(avatarImage);
                avatarImage.setVisibility(View.VISIBLE);
            } else {
                avatarImage.setVisibility(View.GONE);
            }
        }
        
        private int getIconForType(String type) {
            if (type == null) return R.drawable.ic_message;
            switch (type) {
                case Notification.TYPE_REPLY:
                    return R.drawable.ic_reply;
                case Notification.TYPE_MENTION:
                    return R.drawable.ic_mention;
                case Notification.TYPE_LIKE:
                    return R.drawable.ic_like_filled;
                case Notification.TYPE_FRIEND:
                    return R.drawable.ic_user;
                case Notification.TYPE_SYSTEM:
                    return R.drawable.ic_message;
                case Notification.TYPE_FOLLOW:
                    return R.drawable.ic_user;
                case Notification.TYPE_DIGEST:
                    return R.drawable.ic_star_filled;
                default:
                    return R.drawable.ic_message;
            }
        }
    }
    
    public interface OnNotificationClickListener {
        void onNotificationClick(Notification notification);
    }
}
