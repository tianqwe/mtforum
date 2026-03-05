package com.forum.mt.ui.adapter;

import android.net.Uri;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.forum.mt.R;

import java.util.ArrayList;
import java.util.List;

/**
 * 图片预览适配器
 * 用于回复弹窗中显示已选择的图片
 */
public class ImagePreviewAdapter extends RecyclerView.Adapter<ImagePreviewAdapter.ImagePreviewViewHolder> {
    
    private List<ImageItem> images = new ArrayList<>();
    private OnImageActionListener listener;
    
    public interface OnImageActionListener {
        void onDeleteClick(int position);
        void onImageClick(int position);
        void onInsertClick(int position);
    }
    
    public ImagePreviewAdapter(OnImageActionListener listener) {
        this.listener = listener;
    }
    
    public void addImage(ImageItem image) {
        images.add(image);
        notifyItemInserted(images.size() - 1);
    }
    
    public void removeImage(int position) {
        if (position >= 0 && position < images.size()) {
            images.remove(position);
            notifyItemRemoved(position);
            notifyItemRangeChanged(position, images.size());
        }
    }
    
    public void updateImageStatus(int position, boolean uploading, boolean success, Integer aid, String attachCode) {
        if (position >= 0 && position < images.size()) {
            ImageItem item = images.get(position);
            item.uploading = uploading;
            item.uploadSuccess = success;
            if (aid != null) {
                item.aid = aid;
            }
            if (attachCode != null) {
                item.attachCode = attachCode;
            }
            notifyItemChanged(position);
        }
    }
    
    public List<ImageItem> getImages() {
        return images;
    }
    
    public List<Integer> getUploadedAids() {
        List<Integer> aids = new ArrayList<>();
        for (ImageItem item : images) {
            if (item.uploadSuccess && item.aid > 0) {
                aids.add(item.aid);
            }
        }
        return aids;
    }
    
    public boolean hasUploadingImages() {
        for (ImageItem item : images) {
            if (item.uploading) {
                return true;
            }
        }
        return false;
    }
    
    public boolean hasFailedImages() {
        for (ImageItem item : images) {
            if (!item.uploading && !item.uploadSuccess && item.aid == 0) {
                return true;
            }
        }
        return false;
    }
    
    public void clear() {
        images.clear();
        notifyDataSetChanged();
    }
    
    @Override
    public int getItemCount() {
        return images.size();
    }
    
    @NonNull
    @Override
    public ImagePreviewViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_image_preview, parent, false);
        return new ImagePreviewViewHolder(view);
    }
    
    @Override
    public void onBindViewHolder(@NonNull ImagePreviewViewHolder holder, int position) {
        holder.bind(images.get(position), position);
    }
    
    class ImagePreviewViewHolder extends RecyclerView.ViewHolder {
        ImageView imagePreview;
        ProgressBar uploadProgress;
        ImageView deleteButton;
        TextView insertButton;
        
        ImagePreviewViewHolder(@NonNull View itemView) {
            super(itemView);
            imagePreview = itemView.findViewById(R.id.imagePreview);
            uploadProgress = itemView.findViewById(R.id.uploadProgress);
            deleteButton = itemView.findViewById(R.id.deleteButton);
            insertButton = itemView.findViewById(R.id.insertButton);
        }
        
        void bind(ImageItem item, int position) {
            // 显示图片
            if (item.uri != null) {
                Glide.with(itemView.getContext())
                        .load(item.uri)
                        .centerCrop()
                        .into(imagePreview);
            } else if (item.filePath != null) {
                Glide.with(itemView.getContext())
                        .load(item.filePath)
                        .centerCrop()
                        .into(imagePreview);
            }
            
            // 上传状态
            if (item.uploading) {
                uploadProgress.setVisibility(View.VISIBLE);
                deleteButton.setVisibility(View.GONE);
                insertButton.setVisibility(View.GONE);
            } else {
                uploadProgress.setVisibility(View.GONE);
                deleteButton.setVisibility(View.VISIBLE);
                
                // 插入按钮 - 上传成功后显示
                boolean uploadSuccess = item.attachCode != null 
                    && !item.attachCode.isEmpty() 
                    && item.aid > 0;
                
                if (uploadSuccess) {
                    insertButton.setVisibility(View.VISIBLE);
                    insertButton.setOnClickListener(v -> {
                        if (listener != null) {
                            int adapterPosition = getAdapterPosition();
                            if (adapterPosition != RecyclerView.NO_POSITION) {
                                listener.onInsertClick(adapterPosition);
                            }
                        }
                    });
                } else {
                    insertButton.setVisibility(View.GONE);
                }
            }
            
            // 删除按钮
            deleteButton.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onDeleteClick(position);
                }
            });
            
            // 点击预览
            imagePreview.setOnClickListener(v -> {
                if (listener != null) {
                    listener.onImageClick(position);
                }
            });
        }
    }
    
    /**
     * 图片项
     */
    public static class ImageItem {
        public Uri uri;             // 图片URI
        public String filePath;     // 文件路径
        public boolean uploading;   // 是否正在上传
        public boolean uploadSuccess; // 是否上传成功
        public int aid;             // 附件ID（上传成功后）
        public String imageUrl;     // 图片URL（上传成功后）
        public String attachCode;   // 插入代码（上传成功后）
        
        public ImageItem(Uri uri) {
            this.uri = uri;
        }
        
        public ImageItem(String filePath) {
            this.filePath = filePath;
        }
    }
}
