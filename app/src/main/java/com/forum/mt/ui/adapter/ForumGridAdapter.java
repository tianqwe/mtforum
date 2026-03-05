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
 * 板块宫格适配器
 */
public class ForumGridAdapter extends RecyclerView.Adapter<ForumGridAdapter.ViewHolder> {

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

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_forum_grid, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Forum forum = forums.get(position);
        
        holder.forumName.setText(forum.getName());
        
        // 加载图标
        String iconUrl = forum.getIcon();
        if (iconUrl != null && !iconUrl.isEmpty()) {
            Glide.with(holder.itemView.getContext())
                    .load(iconUrl)
                    .placeholder(R.drawable.ic_forum)
                    .error(R.drawable.ic_forum)
                    .dontAnimate()
                    .into(holder.forumIcon);
        } else {
            holder.forumIcon.setImageResource(R.drawable.ic_forum);
        }
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onForumClick(forum, holder.forumIcon);
            }
        });
    }

    @Override
    public int getItemCount() {
        return forums.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView forumIcon;
        TextView forumName;

        ViewHolder(View itemView) {
            super(itemView);
            forumIcon = itemView.findViewById(R.id.forumIcon);
            forumName = itemView.findViewById(R.id.forumName);
        }
    }
}
