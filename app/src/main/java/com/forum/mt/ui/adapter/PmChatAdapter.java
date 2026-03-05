package com.forum.mt.ui.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.forum.mt.R;
import com.forum.mt.model.PrivateMessage;
import com.forum.mt.model.User;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * 私信消息适配器
 */
public class PmChatAdapter extends ListAdapter<PrivateMessage, PmChatAdapter.MessageViewHolder> {

    private static final int TYPE_SENT = 1;     // 发送的消息
    private static final int TYPE_RECEIVED = 2; // 接收的消息

    private User otherUser;
    private int currentUid;
    private SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm", Locale.getDefault());
    private SimpleDateFormat dateFormat = new SimpleDateFormat("MM-dd HH:mm", Locale.getDefault());

    private String lastDate = "";

    public PmChatAdapter() {
        super(DIFF_CALLBACK);
    }

    public void setOtherUser(User user) {
        this.otherUser = user;
    }

    public void setCurrentUid(int uid) {
        this.currentUid = uid;
    }

    private static final DiffUtil.ItemCallback<PrivateMessage> DIFF_CALLBACK =
            new DiffUtil.ItemCallback<PrivateMessage>() {
                @Override
                public boolean areItemsTheSame(@NonNull PrivateMessage oldItem, @NonNull PrivateMessage newItem) {
                    return oldItem.getPmId() == newItem.getPmId();
                }

                @Override
                public boolean areContentsTheSame(@NonNull PrivateMessage oldItem, @NonNull PrivateMessage newItem) {
                    return oldItem.getMessage().equals(newItem.getMessage());
                }
            };

    @Override
    public int getItemViewType(int position) {
        PrivateMessage msg = getItem(position);
        // 如果发送者是当前用户或fromUid为0（表示当前用户），显示为发送的消息
        // fromUid=0 是解析时标记的自己发送的消息
        if (msg.getFromUid() == currentUid || msg.getFromUid() == 0) {
            return TYPE_SENT;
        }
        return TYPE_RECEIVED;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_pm_message, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        PrivateMessage message = getItem(position);
        int viewType = getItemViewType(position);

        // 设置消息内容
        holder.messageContent.setText(message.getMessage());

        // 获取时间字符串
        String currentTime = formatTime(message.getDateline());

        // 判断是否需要显示时间戳（与上一条消息时间间隔超过5分钟）
        boolean showTime = shouldShowTime(position, message);
        holder.timeText.setVisibility(showTime ? View.VISIBLE : View.GONE);
        if (showTime) {
            holder.timeText.setText(currentTime);
        }

        // 根据消息类型调整布局
        if (viewType == TYPE_SENT) {
            // 发送的消息 - 右对齐
            holder.messageBubble.setLayoutDirection(View.LAYOUT_DIRECTION_RTL);
            holder.messageContent.setBackgroundResource(R.drawable.bg_message_sent);
            holder.messageContent.setTextColor(holder.itemView.getContext().getColor(R.color.message_sent_text));
            holder.avatarLeft.setVisibility(View.GONE);
            holder.avatarRight.setVisibility(View.VISIBLE);

            // 加载当前用户头像（圆形）
            if (otherUser != null && currentUid > 0) {
                Glide.with(holder.itemView.getContext())
                        .load("https://bbs.binmt.cc/uc_server/avatar.php?uid=" + currentUid + "&size=small")
                        .placeholder(R.drawable.ic_user)
                        .error(R.drawable.ic_user)
                        .circleCrop()
                        .into(holder.avatarRight);
            }
        } else {
            // 接收的消息 - 左对齐
            holder.messageBubble.setLayoutDirection(View.LAYOUT_DIRECTION_LTR);
            holder.messageContent.setBackgroundResource(R.drawable.bg_message_received);
            holder.messageContent.setTextColor(holder.itemView.getContext().getColor(R.color.text_dark));
            holder.avatarLeft.setVisibility(View.VISIBLE);
            holder.avatarRight.setVisibility(View.GONE);

            // 加载对方头像（圆形）
            if (otherUser != null) {
                Glide.with(holder.itemView.getContext())
                        .load(otherUser.getAvatarUrl())
                        .placeholder(R.drawable.ic_user)
                        .error(R.drawable.ic_user)
                        .circleCrop()
                        .into(holder.avatarLeft);
            }
        }
    }

    private boolean shouldShowTime(int position, PrivateMessage message) {
        if (position == 0) {
            return true;
        }

        // 总是显示第一条消息的时间
        // 这里简化处理，每5条消息显示一次时间
        if (position % 5 == 0) {
            return true;
        }

        // TODO: 更精确的时间间隔判断
        return false;
    }

    private String formatTime(String dateline) {
        if (dateline == null || dateline.isEmpty()) {
            return "";
        }
        // 如果是时间戳格式
        try {
            long timestamp = Long.parseLong(dateline);
            Date date = new Date(timestamp * 1000);
            return dateFormat.format(date);
        } catch (NumberFormatException e) {
            // 不是时间戳，直接返回
            return dateline;
        }
    }

    static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView timeText;
        ImageView avatarLeft;
        ImageView avatarRight;
        LinearLayout messageBubble;
        TextView messageContent;

        MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            timeText = itemView.findViewById(R.id.timeText);
            avatarLeft = itemView.findViewById(R.id.avatarLeft);
            avatarRight = itemView.findViewById(R.id.avatarRight);
            messageBubble = itemView.findViewById(R.id.messageBubble);
            messageContent = itemView.findViewById(R.id.messageContent);
        }
    }
}
