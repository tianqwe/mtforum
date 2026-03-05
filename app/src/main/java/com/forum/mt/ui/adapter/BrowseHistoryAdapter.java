package com.forum.mt.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CheckBox;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.cardview.widget.CardView;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.forum.mt.R;
import com.forum.mt.model.BrowseHistory;
import com.forum.mt.util.FontUtils;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * 浏览历史适配器 - 支持时间分组
 * 与应用其他页面风格保持一致
 */
public class BrowseHistoryAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {
    
    private static final int TYPE_HEADER = 0;
    private static final int TYPE_ITEM = 1;
    
    /**
     * 点击监听器
     */
    public interface OnItemClickListener {
        void onItemClick(BrowseHistory history, int position);
        void onDeleteClick(BrowseHistory history, int position);
    }
    
    /**
     * 用户点击监听器
     */
    public interface OnUserClickListener {
        void onUserClick(int uid, String username, String avatar, View avatarView);
    }
    
    private OnItemClickListener listener;
    private OnUserClickListener userClickListener;
    private Set<Integer> selectedItems = new HashSet<>();
    private boolean isSelectionMode = false;
    
    // 分组数据
    private List<ListItem> items = new ArrayList<>();
    
    /**
     * 列表项包装类
     */
    public static class ListItem {
        public static final int TYPE_HEADER = 0;
        public static final int TYPE_ITEM = 1;
        
        public int type;
        public String headerTitle;
        public int headerCount;
        public BrowseHistory history;
        
        // 头部构造函数
        public static ListItem createHeader(String title, int count) {
            ListItem item = new ListItem();
            item.type = TYPE_HEADER;
            item.headerTitle = title;
            item.headerCount = count;
            return item;
        }
        
        // 数据项构造函数
        public static ListItem createItem(BrowseHistory history) {
            ListItem item = new ListItem();
            item.type = TYPE_ITEM;
            item.history = history;
            return item;
        }
    }
    
    public BrowseHistoryAdapter() {}
    
    public void setOnItemClickListener(OnItemClickListener listener) {
        this.listener = listener;
    }
    
    public void setOnUserClickListener(OnUserClickListener listener) {
        this.userClickListener = listener;
    }
    
    /**
     * 设置数据并自动分组
     */
    public void submitList(List<BrowseHistory> histories) {
        items.clear();
        
        if (histories == null || histories.isEmpty()) {
            notifyDataSetChanged();
            return;
        }
        
        // 按时间分组
        Calendar today = Calendar.getInstance();
        today.set(Calendar.HOUR_OF_DAY, 0);
        today.set(Calendar.MINUTE, 0);
        today.set(Calendar.SECOND, 0);
        today.set(Calendar.MILLISECOND, 0);
        long todayStart = today.getTimeInMillis();
        
        Calendar yesterday = Calendar.getInstance();
        yesterday.add(Calendar.DAY_OF_YEAR, -1);
        yesterday.set(Calendar.HOUR_OF_DAY, 0);
        yesterday.set(Calendar.MINUTE, 0);
        yesterday.set(Calendar.SECOND, 0);
        yesterday.set(Calendar.MILLISECOND, 0);
        long yesterdayStart = yesterday.getTimeInMillis();
        
        Calendar weekAgo = Calendar.getInstance();
        weekAgo.add(Calendar.DAY_OF_YEAR, -7);
        weekAgo.set(Calendar.HOUR_OF_DAY, 0);
        weekAgo.set(Calendar.MINUTE, 0);
        weekAgo.set(Calendar.SECOND, 0);
        weekAgo.set(Calendar.MILLISECOND, 0);
        long weekStart = weekAgo.getTimeInMillis();
        
        // 分组统计
        List<BrowseHistory> todayList = new ArrayList<>();
        List<BrowseHistory> yesterdayList = new ArrayList<>();
        List<BrowseHistory> weekList = new ArrayList<>();
        List<BrowseHistory> olderList = new ArrayList<>();
        
        for (BrowseHistory history : histories) {
            long time = history.getViewTime();
            if (time >= todayStart) {
                todayList.add(history);
            } else if (time >= yesterdayStart) {
                yesterdayList.add(history);
            } else if (time >= weekStart) {
                weekList.add(history);
            } else {
                olderList.add(history);
            }
        }
        
        // 构建分组列表
        if (!todayList.isEmpty()) {
            items.add(ListItem.createHeader("今天", todayList.size()));
            for (BrowseHistory h : todayList) {
                items.add(ListItem.createItem(h));
            }
        }
        
        if (!yesterdayList.isEmpty()) {
            items.add(ListItem.createHeader("昨天", yesterdayList.size()));
            for (BrowseHistory h : yesterdayList) {
                items.add(ListItem.createItem(h));
            }
        }
        
        if (!weekList.isEmpty()) {
            items.add(ListItem.createHeader("本周", weekList.size()));
            for (BrowseHistory h : weekList) {
                items.add(ListItem.createItem(h));
            }
        }
        
        if (!olderList.isEmpty()) {
            items.add(ListItem.createHeader("更早", olderList.size()));
            for (BrowseHistory h : olderList) {
                items.add(ListItem.createItem(h));
            }
        }
        
        notifyDataSetChanged();
    }
    
    /**
     * 获取原始历史记录列表
     */
    public List<BrowseHistory> getHistoryList() {
        List<BrowseHistory> result = new ArrayList<>();
        for (ListItem item : items) {
            if (item.type == ListItem.TYPE_ITEM) {
                result.add(item.history);
            }
        }
        return result;
    }
    
    /**
     * 设置选择模式
     */
    public void setSelectionMode(boolean selectionMode) {
        if (this.isSelectionMode != selectionMode) {
            this.isSelectionMode = selectionMode;
            if (!selectionMode) {
                selectedItems.clear();
            }
            notifyDataSetChanged();
        }
    }
    
    /**
     * 是否在选择模式
     */
    public boolean isSelectionMode() {
        return isSelectionMode;
    }
    
    /**
     * 获取选中的项目
     */
    public Set<Integer> getSelectedItems() {
        return selectedItems;
    }
    
    /**
     * 切换项目的选中状态
     */
    public void toggleSelection(int position) {
        if (position < 0 || position >= items.size()) return;
        
        ListItem item = items.get(position);
        if (item.type == ListItem.TYPE_ITEM && item.history != null) {
            int tid = item.history.getTid();
            if (selectedItems.contains(tid)) {
                selectedItems.remove(tid);
            } else {
                selectedItems.add(tid);
            }
            notifyItemChanged(position);
        }
    }
    
    /**
     * 全选
     */
    public void selectAll() {
        selectedItems.clear();
        for (ListItem item : items) {
            if (item.type == ListItem.TYPE_ITEM && item.history != null) {
                selectedItems.add(item.history.getTid());
            }
        }
        notifyDataSetChanged();
    }
    
    /**
     * 清空选择
     */
    public void clearSelection() {
        selectedItems.clear();
        notifyDataSetChanged();
    }
    
    /**
     * 获取选中数量
     */
    public int getSelectedCount() {
        return selectedItems.size();
    }
    
    @Override
    public int getItemViewType(int position) {
        return items.get(position).type;
    }
    
    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == TYPE_HEADER) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_history_header, parent, false);
            return new HeaderViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_browse_history, parent, false);
            return new HistoryViewHolder(view);
        }
    }
    
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ListItem item = items.get(position);
        
        if (holder instanceof HeaderViewHolder) {
            ((HeaderViewHolder) holder).bind(item.headerTitle, item.headerCount);
        } else if (holder instanceof HistoryViewHolder) {
            BrowseHistory history = item.history;
            if (history == null) return;
            
            boolean isSelected = selectedItems.contains(history.getTid());
            ((HistoryViewHolder) holder).bind(history, position, isSelected, isSelectionMode);
        }
    }
    
    @Override
    public int getItemCount() {
        return items.size();
    }
    
    /**
     * 头部ViewHolder
     */
    class HeaderViewHolder extends RecyclerView.ViewHolder {
        private final TextView headerTitle;
        private final TextView headerCount;
        
        public HeaderViewHolder(@NonNull View itemView) {
            super(itemView);
            headerTitle = itemView.findViewById(R.id.headerTitle);
            headerCount = itemView.findViewById(R.id.headerCount);
        }
        
        public void bind(String title, int count) {
            headerTitle.setText(title);
            headerCount.setText("(" + count + ")");
        }
    }
    
    /**
     * 历史记录ViewHolder
     */
    class HistoryViewHolder extends RecyclerView.ViewHolder {
        private final View rootView;
        private final CheckBox checkbox;
        private final ImageView authorAvatar;
        private final TextView author;
        private final TextView viewTime;
        private final TextView forumName;
        private final TextView title;
        private final ImageView thumbnail;
        private final CardView thumbnailCard;
        private final TextView viewCount;
        private final TextView replyCount;
        private final TextView tidText;
        private final ImageView deleteButton;
        
        public HistoryViewHolder(@NonNull View itemView) {
            super(itemView);
            rootView = itemView;
            checkbox = itemView.findViewById(R.id.checkbox);
            authorAvatar = itemView.findViewById(R.id.authorAvatar);
            author = itemView.findViewById(R.id.author);
            viewTime = itemView.findViewById(R.id.viewTime);
            forumName = itemView.findViewById(R.id.forumName);
            title = itemView.findViewById(R.id.title);
            thumbnail = itemView.findViewById(R.id.thumbnail);
            thumbnailCard = itemView.findViewById(R.id.thumbnailCard);
            viewCount = itemView.findViewById(R.id.viewCount);
            replyCount = itemView.findViewById(R.id.replyCount);
            tidText = itemView.findViewById(R.id.tidText);
            deleteButton = itemView.findViewById(R.id.deleteButton);
        }
        
        public void bind(BrowseHistory history, int position, boolean isSelected, boolean selectionMode) {
            // 设置作者头像
            String avatarUrl = history.getAuthorAvatar();
            if (avatarUrl != null && !avatarUrl.isEmpty()) {
                Glide.with(rootView.getContext())
                        .load(avatarUrl)
                        .placeholder(R.drawable.ic_forum)
                        .error(R.drawable.ic_forum)
                        .circleCrop()
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(authorAvatar);
            } else {
                authorAvatar.setImageResource(R.drawable.ic_forum);
            }
            
            // 设置作者名
            author.setText(history.getAuthor() != null ? history.getAuthor() : "匿名用户");
            
            // 用户头像和用户名点击事件 - 都传递头像视图用于共享元素动画
            View.OnClickListener userClick = v -> {
                if (userClickListener != null) {
                    userClickListener.onUserClick(history.getAuthorId(), history.getAuthor(), history.getAuthorAvatar(), authorAvatar);
                }
            };
            authorAvatar.setOnClickListener(userClick);
            author.setOnClickListener(userClick);
            
            // 设置浏览时间
            viewTime.setText(history.getFormattedTime());
            
            // 设置版块名称
            forumName.setText(history.getForumName() != null ? history.getForumName() : "未知版块");
            
            // 设置标题
            title.setText(history.getTitle() != null ? history.getTitle() : "无标题");
            
            // 设置缩略图
            String thumbUrl = history.getThumbnail();
            if (thumbUrl != null && !thumbUrl.isEmpty()) {
                thumbnailCard.setVisibility(View.VISIBLE);
                Glide.with(rootView.getContext())
                        .load(thumbUrl)
                        .placeholder(R.drawable.ic_forum)
                        .error(R.drawable.ic_forum)
                        .centerCrop()
                        .into(thumbnail);
            } else {
                thumbnailCard.setVisibility(View.GONE);
            }
            
            // 设置统计信息
            viewCount.setText(formatCount(history.getViewCount()));
            replyCount.setText(String.valueOf(history.getReplyCount()));
            
            // 设置TID
            tidText.setText("TID: " + history.getTid());
            
            // 设置选择模式
            if (selectionMode) {
                checkbox.setVisibility(View.VISIBLE);
                checkbox.setChecked(isSelected);
                deleteButton.setVisibility(View.GONE);
            } else {
                checkbox.setVisibility(View.GONE);
                deleteButton.setVisibility(View.VISIBLE);
            }
            
            // 设置点击事件
            rootView.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                
                if (selectionMode) {
                    toggleSelection(pos);
                } else if (listener != null) {
                    listener.onItemClick(history, pos);
                }
            });
            
            // 设置长按事件（进入选择模式）
            rootView.setOnLongClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return false;
                
                if (!isSelectionMode && listener != null) {
                    setSelectionMode(true);
                    toggleSelection(pos);
                    return true;
                }
                return false;
            });
            
            // 设置删除按钮点击事件
            deleteButton.setOnClickListener(v -> {
                int pos = getAdapterPosition();
                if (pos == RecyclerView.NO_POSITION) return;
                
                if (listener != null) {
                    listener.onDeleteClick(history, pos);
                }
            });
            
            // 应用字体大小设置
            applyFontSize();
        }
        
        /**
         * 应用字体大小设置到各TextView
         */
        private void applyFontSize() {
            FontUtils.applyFontSize(itemView.getContext(), title, FontUtils.SIZE_TITLE);
            FontUtils.applyFontSize(itemView.getContext(), author, FontUtils.SIZE_USERNAME);
            FontUtils.applyFontSize(itemView.getContext(), forumName, FontUtils.SIZE_SMALL);
            FontUtils.applyFontSize(itemView.getContext(), viewTime, FontUtils.SIZE_TIME);
            FontUtils.applyFontSize(itemView.getContext(), viewCount, FontUtils.SIZE_SMALL);
            FontUtils.applyFontSize(itemView.getContext(), replyCount, FontUtils.SIZE_SMALL);
            FontUtils.applyFontSize(itemView.getContext(), tidText, FontUtils.SIZE_SMALL);
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