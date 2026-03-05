package com.forum.mt.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.forum.mt.R;

/**
 * 简单字符串表情代码适配器
 * 用于私信等场景的表情选择
 */
public class SimpleSmileyAdapter extends RecyclerView.Adapter<SimpleSmileyAdapter.ViewHolder> {
    
    private String[] smileys;
    private OnSmileyClickListener listener;
    
    public interface OnSmileyClickListener {
        void onSmileyClick(String smileyCode);
    }
    
    public SimpleSmileyAdapter(String[] smileys, OnSmileyClickListener listener) {
        this.smileys = smileys != null ? smileys : new String[0];
        this.listener = listener;
    }
    
    /**
     * 设置表情列表
     */
    public void setSmileys(java.util.List<String> smileyList) {
        if (smileyList != null) {
            this.smileys = smileyList.toArray(new String[0]);
        } else {
            this.smileys = new String[0];
        }
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_simple_smiley, parent, false);
        return new ViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        String smiley = smileys[position];
        holder.smileyText.setText(smiley);
        
        holder.itemView.setOnClickListener(v -> {
            if (listener != null) {
                listener.onSmileyClick(smiley);
            }
        });
    }
    
    @Override
    public int getItemCount() {
        return smileys.length;
    }
    
    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView smileyText;
        
        ViewHolder(@NonNull View itemView) {
            super(itemView);
            smileyText = itemView.findViewById(R.id.smileyText);
        }
    }
}
