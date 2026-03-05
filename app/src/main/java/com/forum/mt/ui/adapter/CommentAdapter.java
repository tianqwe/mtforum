package com.forum.mt.ui.adapter;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.PopupMenu;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DiffUtil;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.ListAdapter;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.forum.mt.R;
import com.forum.mt.model.Comment;
import com.forum.mt.model.ContentBlock;
import com.forum.mt.util.FontUtils;

import java.util.List;

import de.hdodenhof.circleimageview.CircleImageView;

/**
 * 评论列表适配器
 * 使用 ListAdapter + DiffUtil，实现高效列表更新
 */
public class CommentAdapter extends ListAdapter<Comment, CommentAdapter.CommentViewHolder> {
    
    private int highlightAuthorId = 0; // 需要高亮的作者ID
    private int postAuthorId = 0;      // 楼主ID
    private int currentUserId = 0;     // 当前登录用户ID
    private OnCommentActionListener actionListener; // 评论操作监听器
    private OnUserClickListener userClickListener; // 用户点击监听器

    /**
     * 评论操作监听器接口
     */
    public interface OnCommentActionListener {
        /**
         * 回复评论
         * @param comment 要回复的评论
         */
        void onReplyComment(Comment comment);
        
        /**
         * 复制评论内容
         * @param content 评论内容
         */
        void onCopyContent(String content);
        
        /**
         * 删除评论
         * @param comment 要删除的评论
         */
        void onDeleteComment(Comment comment);
        
        /**
         * 点赞/取消点赞评论（踢贴）
         * @param comment 要点赞的评论
         */
        void onLikeComment(Comment comment);
        
        /**
         * 打赏评论
         * @param comment 要打赏的评论
         */
        void onRewardComment(Comment comment);
        
        /**
         * 举报评论
         * @param comment 要举报的评论
         */
        void onReportComment(Comment comment);
    }
    
    /**
     * 用户点击监听器接口
     */
    public interface OnUserClickListener {
        void onUserClick(int uid, String username, String avatar, View avatarView);
    }

    public CommentAdapter() {
        super(new CommentDiffCallback());
    }
    
    /**
     * 设置评论操作监听器
     */
    public void setOnCommentActionListener(OnCommentActionListener listener) {
        this.actionListener = listener;
    }
    
    /**
     * 设置用户点击监听器
     */
    public void setOnUserClickListener(OnUserClickListener listener) {
        this.userClickListener = listener;
    }
    
    /**
     * 设置需要高亮的作者ID（用于"我的评论"页面进入时高亮自己的评论）
     */
    public void setHighlightAuthorId(int authorId) {
        this.highlightAuthorId = authorId;
        notifyDataSetChanged();
    }
    
    /**
     * 设置楼主ID
     */
    public void setPostAuthorId(int authorId) {
        this.postAuthorId = authorId;
        notifyDataSetChanged();
    }
    
    /**
     * 设置当前登录用户ID
     */
    public void setCurrentUserId(int userId) {
        this.currentUserId = userId;
        notifyDataSetChanged();
    }
    
    /**
     * 更新单个评论的点赞状态
     * @param commentId 评论ID
     * @param isLiked 是否已点赞
     * @param likeCount 点赞数
     */
    public void updateCommentLikeStatus(int commentId, boolean isLiked, int likeCount) {
        // 查找并更新评论
        java.util.List<Comment> currentList = getCurrentList();
        for (int i = 0; i < currentList.size(); i++) {
            Comment comment = currentList.get(i);
            if (comment.getId() == commentId) {
                comment.setLiked(isLiked);
                comment.setLikes(likeCount);
                notifyItemChanged(i);
                break;
            }
        }
    }

    @NonNull
    @Override
    public CommentViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_comment, parent, false);
        return new CommentViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull CommentViewHolder holder, int position) {
        holder.bind(getItem(position), highlightAuthorId, postAuthorId, currentUserId, actionListener, userClickListener);
    }

    static class CommentViewHolder extends RecyclerView.ViewHolder {
        private final CircleImageView avatarView;
        private final TextView authorView;
        private final TextView levelView;
        private final TextView floorView;
        private final RecyclerView contentList;
        private final TextView timeView;
        private final TextView locationView;
        private final View quoteContainer;
        private final TextView quoteAuthorView;
        private final TextView quoteContentView;
        
        // 点赞按钮
        private final View likeButton;
        private final ImageView likeIcon;
        private final TextView likeCountView;
        
        // 每个ViewHolder持有一个ContentAdapter实例
        private ContentAdapter contentAdapter;

        CommentViewHolder(@NonNull View itemView) {
            super(itemView);
            avatarView = itemView.findViewById(R.id.commentAvatar);
            authorView = itemView.findViewById(R.id.commentAuthor);
            levelView = itemView.findViewById(R.id.commentLevel);
            floorView = itemView.findViewById(R.id.floorNumber);
            contentList = itemView.findViewById(R.id.commentContentList);
            timeView = itemView.findViewById(R.id.commentTime);
            locationView = itemView.findViewById(R.id.commentLocation);
            quoteContainer = itemView.findViewById(R.id.quoteContainer);
            quoteAuthorView = itemView.findViewById(R.id.quoteAuthor);
            quoteContentView = itemView.findViewById(R.id.quoteContent);
            
            // 点赞按钮
            likeButton = itemView.findViewById(R.id.likeButton);
            likeIcon = itemView.findViewById(R.id.likeIcon);
            likeCountView = itemView.findViewById(R.id.likeCount);
            
            // 初始化内容列表
            if (contentList != null) {
                contentList.setLayoutManager(new LinearLayoutManager(itemView.getContext()));
                contentList.setNestedScrollingEnabled(false);
                contentAdapter = new ContentAdapter();
                contentAdapter.setTextSelectable(false); // 禁用文字长按选择，使用弹窗菜单复制
                contentList.setAdapter(contentAdapter);
            }
        }

        void bind(Comment comment, int highlightAuthorId, int postAuthorId, int currentUserId, 
                  OnCommentActionListener actionListener, OnUserClickListener userClickListener) {
            // 保存当前评论引用，用于长按操作
            final Comment currentComment = comment;
            final OnCommentActionListener currentListener = actionListener;
            final int userId = currentUserId;
            final OnUserClickListener userListener = userClickListener;
            
            // 长按显示操作菜单（整个item和文字区域都支持）
            View.OnLongClickListener longClickListener = v -> {
                showCommentActionMenu(v, currentComment, userId, currentListener);
                return true;
            };
            itemView.setOnLongClickListener(longClickListener);
            
            // 设置文字长按监听器（让ContentAdapter的文字区域也能触发菜单）
            if (contentAdapter != null) {
                contentAdapter.setTextLongClickListener(longClickListener);
            }
            
            // 作者名
            String author = comment.getAuthor();
            authorView.setText(author != null && !author.isEmpty() ? author : "匿名用户");

            // 作者等级
            String level = comment.getAuthorLevel();
            if (level != null && !level.isEmpty()) {
                levelView.setText(level);
                levelView.setVisibility(View.VISIBLE);
                // 设置等级背景颜色（与首页一致）
                setLevelBackground(levelView, level);
            } else {
                levelView.setVisibility(View.GONE);
            }

            // 楼层标签 (沙发、椅子、板凳等)
            String floorLabel = comment.getFloorLabel();
            if (floorLabel != null && !floorLabel.isEmpty()) {
                floorView.setText(floorLabel);
            } else {
                int floor = comment.getFloor();
                floorView.setText(floor > 0 ? Comment.getFloorLabel(floor) : "");
            }

            // 时间和地区
            String dateStr = comment.getDateStr();
            String location = comment.getLocation();
            timeView.setText(dateStr != null ? dateStr : "");
            if (location != null && !location.isEmpty()) {
                locationView.setText("来自 " + location);
                locationView.setVisibility(View.VISIBLE);
            } else {
                locationView.setVisibility(View.GONE);
            }

            // 引用回复
            String quoteAuthor = comment.getQuoteAuthor();
            String quoteContent = comment.getQuoteContent();
            if (quoteAuthor != null && !quoteAuthor.isEmpty()) {
                quoteContainer.setVisibility(View.VISIBLE);
                quoteAuthorView.setText(quoteAuthor);
                if (quoteContent != null && !quoteContent.isEmpty()) {
                    quoteContentView.setText(quoteContent);
                    quoteContentView.setVisibility(View.VISIBLE);
                } else {
                    quoteContentView.setVisibility(View.GONE);
                }
            } else {
                quoteContainer.setVisibility(View.GONE);
            }

            // 内容（使用ContentAdapter显示，支持图片等独立显示）
            if (contentAdapter != null) {
                java.util.List<ContentBlock> contentBlocks = comment.getDisplayContentBlocks();
                contentAdapter.setContentBlocks(contentBlocks);
                
                // 设置图片点击监听
                contentAdapter.setImageClickListener((imageUrl, imageIndex, allImages) -> {
                    // 可以跳转到图片查看器
                    android.content.Context context = itemView.getContext();
                    android.content.Intent intent = new android.content.Intent(context, 
                            com.forum.mt.ui.ImageViewerActivity.class);
                    intent.putStringArrayListExtra(com.forum.mt.ui.ImageViewerActivity.EXTRA_IMAGE_URLS, 
                            new java.util.ArrayList<>(allImages));
                    intent.putExtra(com.forum.mt.ui.ImageViewerActivity.EXTRA_CURRENT_POSITION, imageIndex);
                    context.startActivity(intent);
                });
            }

            // 判断身份标签
            boolean isPostAuthor = postAuthorId > 0 && comment.getAuthorId() == postAuthorId;
            boolean isMyComment = currentUserId > 0 && comment.getAuthorId() == currentUserId;
            boolean shouldHighlight = highlightAuthorId > 0 && comment.getAuthorId() == highlightAuthorId;
            
            // 构建楼层文本和样式
            StringBuilder floorText = new StringBuilder();
            floorText.append(floorView.getText());
            
            if (isPostAuthor && isMyComment) {
                // 既是楼主又是我的评论（应该很少见，除非楼主就是自己）
                floorText.append(" · 楼主 · 我");
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.comment_both_bg));
                floorView.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.comment_both_text));
            } else if (isPostAuthor) {
                // 楼主
                floorText.append(" · 楼主");
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.comment_author_bg));
                floorView.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.comment_author_text));
            } else if (isMyComment) {
                // 我的评论
                floorText.append(" · 我");
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.comment_me_bg));
                floorView.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.comment_me_text));
            } else if (shouldHighlight) {
                // 高亮（从"我的评论"页面进入）
                itemView.setBackgroundColor(ContextCompat.getColor(itemView.getContext(), R.color.comment_me_bg));
                floorView.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.comment_me_text));
            } else {
                // 普通评论
                itemView.setBackgroundColor(Color.TRANSPARENT);
                floorView.setTextColor(ContextCompat.getColor(itemView.getContext(), R.color.text_light_gray));
            }
            
            floorView.setText(floorText.toString());

            // 头像
            String avatar = comment.getAuthorAvatar();
            if (avatar != null && !avatar.isEmpty()) {
                Glide.with(itemView.getContext())
                        .load(avatar)
                        .placeholder(R.drawable.ic_forum)
                        .error(R.drawable.ic_forum)
                        .diskCacheStrategy(DiskCacheStrategy.NONE)
                        .into(avatarView);
            } else {
                avatarView.setImageResource(R.drawable.ic_forum);
            }
            
            // 用户头像和用户名点击事件 - 都传递头像视图用于共享元素动画
            View.OnClickListener userClick = v -> {
                if (userListener != null) {
                    userListener.onUserClick(comment.getAuthorId(), comment.getAuthor(), comment.getAuthorAvatar(), avatarView);
                }
            };
            avatarView.setOnClickListener(userClick);
            authorView.setOnClickListener(userClick);
            
            // 点赞按钮（踢贴）
            if (likeButton != null) {
                // 设置点赞状态图标
                boolean isLiked = comment.isLiked();
                if (isLiked) {
                    likeIcon.setImageResource(R.drawable.ic_thumb_up_filled);
                    likeIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.highlight_orange));
                } else {
                    likeIcon.setImageResource(R.drawable.ic_thumb_up_outline);
                    likeIcon.setColorFilter(ContextCompat.getColor(itemView.getContext(), R.color.text_light_gray));
                }
                
                // 设置点赞数
                int likes = comment.getLikes();
                if (likes > 0) {
                    likeCountView.setText(String.valueOf(likes));
                    likeCountView.setVisibility(View.VISIBLE);
                    likeCountView.setTextColor(isLiked ? 
                            ContextCompat.getColor(itemView.getContext(), R.color.highlight_orange) :
                            ContextCompat.getColor(itemView.getContext(), R.color.text_light_gray));
                } else {
                    likeCountView.setVisibility(View.GONE);
                }
                
                // 点赞点击事件
                likeButton.setOnClickListener(v -> {
                    if (actionListener != null) {
                        actionListener.onLikeComment(comment);
                    }
                });
            }
            
            // 应用字体大小设置
            applyFontSize();
        }
        
        /**
         * 应用字体大小设置到各TextView
         */
        private void applyFontSize() {
            FontUtils.applyFontSize(itemView.getContext(), authorView, FontUtils.SIZE_USERNAME);
            FontUtils.applyFontSize(itemView.getContext(), levelView, FontUtils.SIZE_SMALL);
            FontUtils.applyFontSize(itemView.getContext(), floorView, FontUtils.SIZE_SMALL);
            FontUtils.applyFontSize(itemView.getContext(), timeView, FontUtils.SIZE_TIME);
            FontUtils.applyFontSize(itemView.getContext(), locationView, FontUtils.SIZE_SMALL);
            if (quoteAuthorView != null) {
                FontUtils.applyFontSize(itemView.getContext(), quoteAuthorView, FontUtils.SIZE_SMALL);
            }
            if (quoteContentView != null) {
                FontUtils.applyFontSize(itemView.getContext(), quoteContentView, FontUtils.SIZE_SUBTITLE);
            }
        }
        
        /**
         * 显示评论操作菜单 (底部弹窗样式)
         */
        private void showCommentActionMenu(View anchor, Comment comment, int currentUserId, OnCommentActionListener listener) {
            android.content.Context context = anchor.getContext();
            
            // 使用BottomSheetDialog
            com.google.android.material.bottomsheet.BottomSheetDialog bottomSheet = 
                    new com.google.android.material.bottomsheet.BottomSheetDialog(context);
            View sheetView = LayoutInflater.from(context).inflate(R.layout.bottom_sheet_comment_action, null);
            bottomSheet.setContentView(sheetView);
            
            // 是否是自己的评论
            boolean isOwnComment = currentUserId > 0 && comment.getAuthorId() == currentUserId;
            
            // 获取视图
            View copyBtn = sheetView.findViewById(R.id.actionCopy);
            View replyBtn = sheetView.findViewById(R.id.actionReply);
            View deleteBtn = sheetView.findViewById(R.id.actionDelete);
            View rewardBtn = sheetView.findViewById(R.id.actionReward);
            View cancelBtn = sheetView.findViewById(R.id.actionCancel);
            
            // 删除按钮只对自己的评论显示
            if (deleteBtn != null) {
                deleteBtn.setVisibility(isOwnComment ? View.VISIBLE : View.GONE);
            }
            
            // 打赏按钮：不能打赏自己，且需要有打赏权限
            if (rewardBtn != null) {
                // 不能打赏自己的评论
                boolean canReward = !isOwnComment && currentUserId > 0 && comment.isCanRate();
                rewardBtn.setVisibility(canReward ? View.VISIBLE : View.GONE);
                
                if (canReward) {
                    rewardBtn.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onRewardComment(comment);
                        }
                        bottomSheet.dismiss();
                    });
                }
            }
            
            // 举报按钮
            View reportBtn = sheetView.findViewById(R.id.actionReport);
            if (reportBtn != null) {
                // 不能举报自己的评论
                boolean canReport = !isOwnComment && currentUserId > 0;
                reportBtn.setVisibility(canReport ? View.VISIBLE : View.GONE);
                
                if (canReport) {
                    reportBtn.setOnClickListener(v -> {
                        if (listener != null) {
                            listener.onReportComment(comment);
                        }
                        bottomSheet.dismiss();
                    });
                }
            }
            
            // 复制按钮
            if (copyBtn != null) {
                copyBtn.setOnClickListener(v -> {
                    String content = getPlainTextContent(comment);
                    if (listener != null) {
                        listener.onCopyContent(content);
                    } else {
                        copyToClipboard(context, content);
                    }
                    bottomSheet.dismiss();
                });
            }
            
            // 回复按钮
            if (replyBtn != null) {
                replyBtn.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onReplyComment(comment);
                    }
                    bottomSheet.dismiss();
                });
            }
            
            // 删除按钮
            if (deleteBtn != null) {
                deleteBtn.setOnClickListener(v -> {
                    if (listener != null) {
                        listener.onDeleteComment(comment);
                    }
                    bottomSheet.dismiss();
                });
            }
            
            // 取消按钮
            if (cancelBtn != null) {
                cancelBtn.setOnClickListener(v -> bottomSheet.dismiss());
            }
            
            bottomSheet.show();
        }
        
        /**
         * 获取评论的纯文本内容
         */
        private String getPlainTextContent(Comment comment) {
            StringBuilder sb = new StringBuilder();
            
            // 从内容块中提取文本
            List<ContentBlock> blocks = comment.getDisplayContentBlocks();
            if (blocks != null && !blocks.isEmpty()) {
                for (ContentBlock block : blocks) {
                    if (block.isText() || block.isRichText()) {
                        String text = block.getContent();
                        if (text != null && !text.isEmpty()) {
                            if (sb.length() > 0) {
                                sb.append("\n");
                            }
                            sb.append(text);
                        }
                    } else if (block.isQuote()) {
                        String quote = block.getContent();
                        if (quote != null && !quote.isEmpty()) {
                            if (sb.length() > 0) {
                                sb.append("\n");
                            }
                            sb.append("【引用】").append(quote);
                        }
                    }
                    // 图片和表情暂时忽略，可以添加占位符
                    else if (block.isImage()) {
                        if (sb.length() > 0) {
                            sb.append("\n");
                        }
                        sb.append("[图片]");
                    } else if (block.isSmiley()) {
                        // 表情可以忽略或添加占位符
                    }
                }
            }
            
            // 如果没有从内容块获取到内容，尝试原始内容
            if (sb.length() == 0) {
                String content = comment.getContent();
                if (content != null && !content.isEmpty()) {
                    // 简单去除HTML标签
                    sb.append(android.text.Html.fromHtml(content).toString().trim());
                }
            }
            
            return sb.toString();
        }
        
        /**
         * 复制文本到剪贴板
         */
        private void copyToClipboard(Context context, String text) {
            ClipboardManager clipboard = (ClipboardManager) 
                    context.getSystemService(Context.CLIPBOARD_SERVICE);
            if (clipboard != null) {
                ClipData clip = ClipData.newPlainText("评论内容", text);
                clipboard.setPrimaryClip(clip);
                Toast.makeText(context, "已复制到剪贴板", Toast.LENGTH_SHORT).show();
            }
        }
    }

    /**
     * DiffUtil 回调，用于高效更新列表
     */
    static class CommentDiffCallback extends DiffUtil.ItemCallback<Comment> {
        @Override
        public boolean areItemsTheSame(@NonNull Comment oldItem, @NonNull Comment newItem) {
            return oldItem.getId() == newItem.getId();
        }

        @Override
        public boolean areContentsTheSame(@NonNull Comment oldItem, @NonNull Comment newItem) {
            return safeEqual(oldItem.getAuthor(), newItem.getAuthor()) &&
                    safeEqual(oldItem.getContent(), newItem.getContent()) &&
                    oldItem.getFloor() == newItem.getFloor() &&
                    safeEqual(oldItem.getDateStr(), newItem.getDateStr());
        }

        private boolean safeEqual(String a, String b) {
            if (a == null && b == null) return true;
            if (a == null || b == null) return false;
            return a.equals(b);
        }
    }

    /**
     * 设置等级背景颜色（与首页保持一致）
     */
    private static void setLevelBackground(TextView levelView, String level) {
        int color;
        try {
            // 从 "Lv.4" 或 "Lv.5 大学生" 提取数字
            String levelNum = level.replace("Lv.", "").trim().split(" ")[0];
            int num = Integer.parseInt(levelNum);
            if (num <= 2) {
                color = Color.parseColor("#9E9E9E"); // 灰色 - 新手
            } else if (num <= 4) {
                color = Color.parseColor("#4CAF50"); // 绿色 - 初级
            } else if (num <= 6) {
                color = Color.parseColor("#2196F3"); // 蓝色 - 中级
            } else if (num <= 8) {
                color = Color.parseColor("#9C27B0"); // 紫色 - 高级
            } else {
                color = Color.parseColor("#FF9800"); // 橙色 - 资深
            }
        } catch (Exception e) {
            color = Color.parseColor("#9E9E9E");
        }

        GradientDrawable drawable = new GradientDrawable();
        drawable.setCornerRadius(8f);
        drawable.setColor(color);
        levelView.setBackground(drawable);
    }
}
