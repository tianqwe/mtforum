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
import com.forum.mt.model.PrivateMessage;
import com.forum.mt.util.FontUtils;

/**
 * 私信列表适配器
 */
public class MessageAdapter extends ListAdapter<PrivateMessage, MessageAdapter.MessageViewHolder> {
    
    private OnMessageClickListener listener;
    
    public MessageAdapter() {
        super(DIFF_CALLBACK);
    }
    
    private static final DiffUtil.ItemCallback<PrivateMessage> DIFF_CALLBACK = 
            new DiffUtil.ItemCallback<PrivateMessage>() {
        @Override
        public boolean areItemsTheSame(@NonNull PrivateMessage oldItem, @NonNull PrivateMessage newItem) {
            return oldItem.getPmId() == newItem.getPmId();
        }
        
        @Override
        public boolean areContentsTheSame(@NonNull PrivateMessage oldItem, @NonNull PrivateMessage newItem) {
            return oldItem.getPmId() == newItem.getPmId() &&
                   oldItem.isRead() == newItem.isRead() &&
                   (oldItem.getMessage() != null ? oldItem.getMessage().equals(newItem.getMessage()) : newItem.getMessage() == null);
        }
    };
    
    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_message, parent, false);
        return new MessageViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        PrivateMessage message = getItem(position);
        holder.bind(message);
    }
    
    public void setOnMessageClickListener(OnMessageClickListener listener) {
        this.listener = listener;
    }
    
    class MessageViewHolder extends RecyclerView.ViewHolder {
        private ImageView avatarImage;
        private TextView usernameText;
        private TextView messageText;
        private TextView timeText;
        private View unreadIndicator;
        
        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarImage = itemView.findViewById(R.id.avatar_image);
            usernameText = itemView.findViewById(R.id.username_text);
            messageText = itemView.findViewById(R.id.message_text);
            timeText = itemView.findViewById(R.id.time_text);
            unreadIndicator = itemView.findViewById(R.id.unread_indicator);
            
            itemView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos != RecyclerView.NO_POSITION && listener != null) {
                    listener.onMessageClick(getItem(pos));
                }
            });
        }
        
        void bind(PrivateMessage message) {
            usernameText.setText(message.getFromUsername());
            messageText.setText(message.getMessage());
            timeText.setText(message.getDateline());
            
            // 应用字体大小设置
            FontUtils.applyFontSize(itemView.getContext(), usernameText, FontUtils.SIZE_USERNAME);
            FontUtils.applyFontSize(itemView.getContext(), messageText, FontUtils.SIZE_SUBTITLE);
            FontUtils.applyFontSize(itemView.getContext(), timeText, FontUtils.SIZE_TIME);
            
            // 未读指示器
            unreadIndicator.setVisibility(message.isRead() ? View.GONE : View.VISIBLE);
            
            // 头像
            String avatarUrl = message.getAvatarUrl();
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(avatarUrl)
                        .circleCrop()
                        .placeholder(R.drawable.ic_user_avatar_placeholder)
                        .error(R.drawable.ic_user_avatar_placeholder)
                        .into(avatarImage);
            } else {
                avatarImage.setImageResource(R.drawable.ic_user_avatar_placeholder);
            }
        }
    }
    
    public interface OnMessageClickListener {
        void onMessageClick(PrivateMessage message);
    }
}
