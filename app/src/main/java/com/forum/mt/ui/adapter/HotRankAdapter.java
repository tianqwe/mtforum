package com.forum.mt.ui.adapter;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.forum.mt.R;
import com.forum.mt.model.Post;
import com.forum.mt.util.FontUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 热帖排行适配器
 */
public class HotRankAdapter extends RecyclerView.Adapter<HotRankAdapter.ViewHolder> {

    private List<Post> posts = new ArrayList<>();
    private OnPostClickListener listener;

    // 前三名颜色
    private static final int[] RANK_COLORS = {
            Color.parseColor("#FF705E"),  // 红色 - 第1名
            Color.parseColor("#FFB900"),  // 黄色 - 第2名
            Color.parseColor("#A8C500")   // 绿色 - 第3名
    };

    public interface OnPostClickListener {
        void onPostClick(Post post);
    }

    public void setOnPostClickListener(OnPostClickListener listener) {
        this.listener = listener;
    }

    public void setData(List<Post> posts) {
        this.posts = posts != null ? posts : new ArrayList<>();
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_hot_rank, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Post post = posts.get(position);
        
        // 设置排名序号
        int rank = position + 1;
        holder.rankNumber.setText(String.valueOf(rank));
        
        // 设置排名背景色
        if (rank <= 3) {
            holder.rankNumber.setBackgroundTintList(ColorStateList.valueOf(RANK_COLORS[rank - 1]));
        } else {
            holder.rankNumber.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#CCCCCC")));
        }
        
        // 设置标题
        holder.postTitle.setText(post.getTitle());
        
        // 应用字体大小设置
        FontUtils.applyFontSize(holder.itemView.getContext(), holder.postTitle, FontUtils.SIZE_SUBTITLE);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onPostClick(post);
            }
        });
    }

    @Override
    public int getItemCount() {
        return Math.min(posts.size(), 5); // 最多显示5条
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView rankNumber;
        TextView postTitle;

        ViewHolder(View itemView) {
            super(itemView);
            rankNumber = itemView.findViewById(R.id.rankNumber);
            postTitle = itemView.findViewById(R.id.postTitle);
        }
    }
}
