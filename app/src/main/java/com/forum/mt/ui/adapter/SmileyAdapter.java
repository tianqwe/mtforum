package com.forum.mt.ui.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.forum.mt.R;
import com.forum.mt.model.Smiley;

import java.util.ArrayList;
import java.util.List;

/**
 * 表情选择适配器
 */
public class SmileyAdapter extends RecyclerView.Adapter<SmileyAdapter.SmileyViewHolder> {
    
    private List<Smiley> smileys = new ArrayList<>();
    private OnSmileyClickListener listener;
    
    public interface OnSmileyClickListener {
        void onSmileyClick(Smiley smiley);
        void onDeleteClick();
    }
    
    public SmileyAdapter(OnSmileyClickListener listener) {
        this.listener = listener;
    }
    
    public void setSmileys(List<Smiley> smileys) {
        this.smileys = smileys != null ? smileys : new ArrayList<>();
        notifyDataSetChanged();
    }
    
    @NonNull
    @Override
    public SmileyViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_smiley, parent, false);
        return new SmileyViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull SmileyViewHolder holder, int position) {
        if (position >= 0 && position < smileys.size()) {
            holder.bind(smileys.get(position));
        }
    }
    
    @Override
    public int getItemCount() {
        return smileys.size();
    }
    
    class SmileyViewHolder extends RecyclerView.ViewHolder {
        ImageView smileyImage;
        
        SmileyViewHolder(@NonNull View itemView) {
            super(itemView);
            smileyImage = itemView.findViewById(R.id.smileyImage);
        }
        
        void bind(Smiley smiley) {
            if (smiley == null) {
                smileyImage.setImageResource(R.drawable.ic_emoji);
                return;
            }
            
            String imageUrl = smiley.getImageUrl();
            if (imageUrl != null && !imageUrl.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(imageUrl)
                        .placeholder(R.drawable.ic_emoji)
                        .error(R.drawable.ic_emoji)
                        .into(smileyImage);
            } else {
                smileyImage.setImageResource(R.drawable.ic_emoji);
            }
            
            itemView.setOnClickListener(v -> {
                if (listener != null && smiley != null) {
                    listener.onSmileyClick(smiley);
                }
            });
            
            // 长按删除最后一个表情
            itemView.setOnLongClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick();
                }
                return true;
            });
        }
    }
}
