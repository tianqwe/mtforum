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
import com.forum.mt.model.Forum;

import java.util.ArrayList;
import java.util.List;

/**
 * 板块列表适配器
 * 支持分类标题和板块项两种类型
 * 九宫格布局：每行3个板块
 */
public class ForumListAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<Forum> forums = new ArrayList<>();
    private OnForumClickListener listener;

    public interface OnForumClickListener {
        void onForumClick(Forum forum, View iconView);
    }

    public void setOnForumClickListener(OnForumClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<Forum> forums) {
        this.forums = forums != null ? forums : new ArrayList<>();
        notifyDataSetChanged();
    }

    @Override
    public int getItemViewType(int position) {
        return forums.get(position).getItemType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        if (viewType == Forum.TYPE_CATEGORY) {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_forum_category, parent, false);
            return new CategoryViewHolder(view);
        } else {
            View view = LayoutInflater.from(parent.getContext())
                    .inflate(R.layout.item_forum_list, parent, false);
            return new ForumViewHolder(view);
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        Forum forum = forums.get(position);
        
        if (holder instanceof CategoryViewHolder) {
            ((CategoryViewHolder) holder).bind(forum);
        } else if (holder instanceof ForumViewHolder) {
            ((ForumViewHolder) holder).bind(forum, listener);
        }
    }

    @Override
    public int getItemCount() {
        return forums.size();
    }

    /**
     * 分类标题ViewHolder
     */
    static class CategoryViewHolder extends RecyclerView.ViewHolder {
        TextView categoryName;

        CategoryViewHolder(View itemView) {
            super(itemView);
            categoryName = itemView.findViewById(R.id.categoryName);
        }

        void bind(Forum forum) {
            categoryName.setText(forum.getCategoryName());
        }
    }

    /**
     * 板块项ViewHolder - 九宫格样式
     */
    static class ForumViewHolder extends RecyclerView.ViewHolder {
        ImageView forumIcon;
        TextView forumName;
        TextView todayPosts;

        ForumViewHolder(View itemView) {
            super(itemView);
            forumIcon = itemView.findViewById(R.id.forumIcon);
            forumName = itemView.findViewById(R.id.forumName);
            todayPosts = itemView.findViewById(R.id.todayPosts);
        }

        void bind(Forum forum, OnForumClickListener listener) {
            forumName.setText(forum.getName());
            
            // 加载图标
            String iconUrl = forum.getIcon();
            if (iconUrl != null && !iconUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(iconUrl)
                        .placeholder(R.drawable.ic_forum)
                        .error(R.drawable.ic_forum)
                        .into(forumIcon);
            } else {
                forumIcon.setImageResource(R.drawable.ic_forum);
            }
            
            // 显示今日帖子数
            if (forum.getTodayPosts() > 0) {
                todayPosts.setText("今日 " + forum.getTodayPosts());
                todayPosts.setVisibility(View.VISIBLE);
            } else {
                todayPosts.setVisibility(View.GONE);
            }
            
            itemView.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onForumClick(forum, forumIcon);
                }
            });
        }
    }
}