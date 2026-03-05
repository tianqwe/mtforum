package com.forum.mt.ui.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.forum.mt.R;
import com.github.chrisbanes.photoview.PhotoView;

import java.util.List;

/**
 * 图片查看器适配器
 */
public class ImageViewerAdapter extends RecyclerView.Adapter<ImageViewerAdapter.ImageViewHolder> {

    private final Context context;
    private final List<String> imageUrls;

    public ImageViewerAdapter(Context context, List<String> imageUrls) {
        this.context = context;
        this.imageUrls = imageUrls;
    }

    @NonNull
    @Override
    public ImageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_image_viewer, parent, false);
        return new ImageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ImageViewHolder holder, int position) {
        String url = imageUrls.get(position);
        holder.progressBar.setVisibility(View.VISIBLE);

        // 使用ApplicationContext避免Activity生命周期问题
        Glide.with(context.getApplicationContext())
                .load(url)
                .listener(new RequestListener<android.graphics.drawable.Drawable>() {
                    @Override
                    public boolean onLoadFailed(GlideException e, Object model, 
                            Target<android.graphics.drawable.Drawable> target, boolean isFirstResource) {
                        holder.progressBar.setVisibility(View.GONE);
                        return false;
                    }

                    @Override
                    public boolean onResourceReady(android.graphics.drawable.Drawable resource, 
                            Object model, Target<android.graphics.drawable.Drawable> target, 
                            DataSource dataSource, boolean isFirstResource) {
                        holder.progressBar.setVisibility(View.GONE);
                        return false;
                    }
                })
                .into(holder.photoView);
    }

    @Override
    public int getItemCount() {
        return imageUrls.size();
    }

    static class ImageViewHolder extends RecyclerView.ViewHolder {
        PhotoView photoView;
        ProgressBar progressBar;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            photoView = itemView.findViewById(R.id.photoView);
            progressBar = itemView.findViewById(R.id.progressBar);
        }
    }
}
