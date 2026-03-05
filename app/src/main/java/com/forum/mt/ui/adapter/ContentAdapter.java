package com.forum.mt.ui.adapter;

import android.graphics.drawable.Drawable;
import android.text.Html;
import android.text.Spanned;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.DataSource;
import com.bumptech.glide.load.engine.DiskCacheStrategy;
import com.bumptech.glide.load.engine.GlideException;
import com.bumptech.glide.request.RequestListener;
import com.bumptech.glide.request.target.Target;
import com.forum.mt.R;
import com.forum.mt.model.ContentBlock;
import com.forum.mt.util.FontUtils;

import java.util.ArrayList;
import java.util.List;

/**
 * 帖子内容适配器
 * 支持文本、图片、引用、隐藏内容等多种内容类型
 */
public class ContentAdapter extends RecyclerView.Adapter<RecyclerView.ViewHolder> {

    private List<ContentBlock> contentBlocks = new ArrayList<>();
    private OnImageClickListener imageClickListener;
    private OnBuyPostClickListener buyPostClickListener;
    private boolean textSelectable = true; // 文字是否可选中复制
    private View.OnLongClickListener textLongClickListener; // 文字长按监听器

    public interface OnImageClickListener {
        void onImageClick(String imageUrl, int position, List<String> allImages);
    }
    
    /**
     * 购买帖子点击监听器
     */
    public interface OnBuyPostClickListener {
        void onBuyPostClick(ContentBlock block, int position);
    }

    public void setContentBlocks(List<ContentBlock> blocks) {
        List<ContentBlock> newBlocks = blocks != null ? blocks : new ArrayList<>();

        // 检查内容是否真的发生了变化
        if (newBlocks.size() != contentBlocks.size()) {
            this.contentBlocks = newBlocks;
            notifyDataSetChanged();
            return;
        }

        // 检查每个内容块是否相同
        boolean hasChanged = false;
        for (int i = 0; i < newBlocks.size(); i++) {
            ContentBlock newBlock = newBlocks.get(i);
            ContentBlock oldBlock = contentBlocks.get(i);

            // 简单检查：类型或内容是否不同
            if (newBlock.getType() != oldBlock.getType() ||
                !android.text.TextUtils.equals(newBlock.getContent(), oldBlock.getContent()) ||
                !android.text.TextUtils.equals(newBlock.getImageUrl(), oldBlock.getImageUrl())) {
                hasChanged = true;
                break;
            }
        }

        // 只有内容真正变化时才刷新
        if (hasChanged) {
            this.contentBlocks = newBlocks;
            notifyDataSetChanged();
        }
    }

    public void setImageClickListener(OnImageClickListener listener) {
        this.imageClickListener = listener;
    }
    
    public void setBuyPostClickListener(OnBuyPostClickListener listener) {
        this.buyPostClickListener = listener;
    }
    
    /**
     * 设置文字是否可选中复制
     * 评论区域应该禁用文字选中，使用弹窗菜单复制
     */
    public void setTextSelectable(boolean selectable) {
        this.textSelectable = selectable;
    }
    
    /**
     * 设置文字长按监听器
     * 用于评论区长按文字弹出菜单
     */
    public void setTextLongClickListener(View.OnLongClickListener listener) {
        this.textLongClickListener = listener;
    }

    /**
     * 获取所有图片URL列表
     */
    public List<String> getAllImageUrls() {
        List<String> urls = new ArrayList<>();
        for (ContentBlock block : contentBlocks) {
            if (block.isImage() && block.getImageUrl() != null) {
                urls.add(block.getImageUrl());
            }
        }
        return urls;
    }

    @Override
    public int getItemViewType(int position) {
        return contentBlocks.get(position).getType();
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        switch (viewType) {
            case ContentBlock.TYPE_IMAGE:
                return new ImageViewHolder(inflater.inflate(R.layout.item_content_image, parent, false));
            case ContentBlock.TYPE_QUOTE:
                return new QuoteViewHolder(inflater.inflate(R.layout.item_content_quote, parent, false));
            case ContentBlock.TYPE_HIDDEN_CONTENT:
                return new HiddenContentViewHolder(inflater.inflate(R.layout.item_content_hidden, parent, false));
            case ContentBlock.TYPE_COLORED_TEXT:
                return new ColoredTextViewHolder(inflater.inflate(R.layout.item_content_colored_text, parent, false));
            case ContentBlock.TYPE_BOUNTY:
                return new BountyViewHolder(inflater.inflate(R.layout.item_content_bounty, parent, false));
            case ContentBlock.TYPE_PAID_POST:
                return new PaidPostViewHolder(inflater.inflate(R.layout.item_content_paid_post, parent, false));
            case ContentBlock.TYPE_PAID_POST_INFO:
                return new PaidPostInfoViewHolder(inflater.inflate(R.layout.item_content_paid_info, parent, false));
            case ContentBlock.TYPE_ATTACHMENT:
                return new AttachmentViewHolder(inflater.inflate(R.layout.item_content_attachment, parent, false));
            case ContentBlock.TYPE_SMILEY:
                return new SmileyViewHolder(inflater.inflate(R.layout.item_content_smiley, parent, false));
            case ContentBlock.TYPE_RICH_TEXT:
                return new RichTextViewHolder(inflater.inflate(R.layout.item_content_text, parent, false));
            case ContentBlock.TYPE_CODE:
                return new CodeViewHolder(inflater.inflate(R.layout.item_content_code, parent, false));
            case ContentBlock.TYPE_LINK:
                return new LinkViewHolder(inflater.inflate(R.layout.item_content_text, parent, false));
            case ContentBlock.TYPE_STYLED_TEXT:
                return new StyledTextViewHolder(inflater.inflate(R.layout.item_content_styled_text, parent, false));
            case ContentBlock.TYPE_DIVIDER:
                return new DividerViewHolder(inflater.inflate(R.layout.item_content_divider, parent, false));
            default:
                return new TextViewHolder(inflater.inflate(R.layout.item_content_text, parent, false));
        }
    }

    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        ContentBlock block = contentBlocks.get(position);
        
        if (holder instanceof TextViewHolder) {
            ((TextViewHolder) holder).bind(block);
        } else if (holder instanceof ImageViewHolder) {
            ((ImageViewHolder) holder).bind(block, position);
        } else if (holder instanceof QuoteViewHolder) {
            ((QuoteViewHolder) holder).bind(block);
        } else if (holder instanceof HiddenContentViewHolder) {
            ((HiddenContentViewHolder) holder).bind(block);
        } else if (holder instanceof ColoredTextViewHolder) {
            ((ColoredTextViewHolder) holder).bind(block);
        } else if (holder instanceof BountyViewHolder) {
            ((BountyViewHolder) holder).bind(block);
        } else if (holder instanceof PaidPostViewHolder) {
            ((PaidPostViewHolder) holder).bind(block, position);
        } else if (holder instanceof PaidPostInfoViewHolder) {
            ((PaidPostInfoViewHolder) holder).bind(block);
        } else if (holder instanceof AttachmentViewHolder) {
            ((AttachmentViewHolder) holder).bind(block);
        } else if (holder instanceof StyledTextViewHolder) {
            ((StyledTextViewHolder) holder).bind(block);
        } else if (holder instanceof DividerViewHolder) {
            ((DividerViewHolder) holder).bind(block);
        } else if (holder instanceof SmileyViewHolder) {
            ((SmileyViewHolder) holder).bind(block);
        } else if (holder instanceof RichTextViewHolder) {
            ((RichTextViewHolder) holder).bind(block);
        } else if (holder instanceof CodeViewHolder) {
            ((CodeViewHolder) holder).bind(block);
        } else if (holder instanceof LinkViewHolder) {
            ((LinkViewHolder) holder).bind(block);
        }
    }

    @Override
    public int getItemCount() {
        return contentBlocks.size();
    }

    class TextViewHolder extends RecyclerView.ViewHolder {
        TextView textContent;

        TextViewHolder(@NonNull View itemView) {
            super(itemView);
            textContent = itemView.findViewById(R.id.textContent);
        }

        void bind(ContentBlock block) {
            String text = block.getContent();
            
            // 先设置文本
            textContent.setText(text);
            
            // 应用字体大小设置
            FontUtils.applyFontSize(itemView.getContext(), textContent, FontUtils.SIZE_CONTENT);
            
            // 检查是否包含URL链接，如果有则使其可点击
            if (hasUrls(text)) {
                makeLinksClickable(textContent, text);
            } else {
                // 无链接：支持选择
                textContent.setTextIsSelectable(textSelectable);
            }
            
            // 设置文字长按监听
            if (textLongClickListener != null) {
                textContent.setOnLongClickListener(textLongClickListener);
            }
        }
    }
    
    /**
     * 链接ViewHolder
     * 用于显示带链接的文字（如"下载地址"）
     */
    class LinkViewHolder extends RecyclerView.ViewHolder {
        TextView textContent;

        LinkViewHolder(@NonNull View itemView) {
            super(itemView);
            textContent = itemView.findViewById(R.id.textContent);
        }

        void bind(ContentBlock block) {
            String text = block.getContent();
            final String url = block.getLinkUrl();
            
            textContent.setText(text);
            
            // 设置链接样式：蓝色、下划线
            textContent.setTextColor(0xFF4FC3F7); // 链接蓝色
            textContent.setPaintFlags(textContent.getPaintFlags() | android.graphics.Paint.UNDERLINE_TEXT_FLAG);
            
            // 点击跳转
            textContent.setOnClickListener(v -> {
                if (url != null && !url.isEmpty()) {
                    try {
                        android.content.Intent intent = new android.content.Intent(
                            android.content.Intent.ACTION_VIEW, 
                            android.net.Uri.parse(url)
                        );
                        v.getContext().startActivity(intent);
                    } catch (Exception e) {
                        android.widget.Toast.makeText(
                            v.getContext(), 
                            "无法打开链接", 
                            android.widget.Toast.LENGTH_SHORT
                        ).show();
                    }
                }
            });
            
            // 长按复制链接
            textContent.setOnLongClickListener(v -> {
                if (url != null && !url.isEmpty()) {
                    android.content.ClipboardManager clipboard = (android.content.ClipboardManager)
                            v.getContext().getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    if (clipboard != null) {
                        android.content.ClipData clip = android.content.ClipData.newPlainText("链接", url);
                        clipboard.setPrimaryClip(clip);
                        android.widget.Toast.makeText(v.getContext(), "链接已复制", android.widget.Toast.LENGTH_SHORT).show();
                    }
                }
                return true;
            });
        }
    }
    
    /**
     * 检查文本是否包含URL链接
     */
    private boolean hasUrls(String text) {
        if (text == null || text.isEmpty()) {
            return false;
        }
        java.util.regex.Pattern urlPattern = java.util.regex.Pattern.compile(
            "(https?://[a-zA-Z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)",
            java.util.regex.Pattern.CASE_INSENSITIVE
        );
        return urlPattern.matcher(text).find();
    }
    
    /**
     * 使TextView中的URL链接可点击
     * 使用外部浏览器打开链接
     * 保留现有的颜色样式
     */
    private void makeLinksClickable(TextView textView, String text) {
        if (text == null || text.isEmpty()) {
            return;
        }
        
        try {
            // URL正则表达式（匹配 http/https 开头的链接）
            java.util.regex.Pattern urlPattern = java.util.regex.Pattern.compile(
                "(https?://[a-zA-Z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)",
                java.util.regex.Pattern.CASE_INSENSITIVE
            );
            
            // 获取当前TextView的文本（可能是Spanned）
            CharSequence currentText = textView.getText();
            android.text.Spannable spannable;
            
            if (currentText instanceof android.text.Spannable) {
                spannable = (android.text.Spannable) currentText;
            } else {
                spannable = new android.text.SpannableString(text);
                textView.setText(spannable);
            }
            
            // 使用纯文本匹配位置
            String plainText = spannable.toString();
            java.util.regex.Matcher matcher = urlPattern.matcher(plainText);
            
            while (matcher.find()) {
                final String url = matcher.group(1);
                int start = matcher.start(1);
                int end = matcher.end(1);
                
                // 边界检查，防止越界
                if (start < 0 || end > spannable.length() || start >= end) {
                    continue;
                }
                
                // 检查是否已有ClickableSpan（避免重复）
                android.text.style.ClickableSpan[] existingClicks = 
                    spannable.getSpans(start, end, android.text.style.ClickableSpan.class);
                if (existingClicks != null && existingClicks.length > 0) {
                    continue;
                }
                
                // 获取链接区域的现有前景色（如果有）
                android.text.style.ForegroundColorSpan[] colorSpans = 
                    spannable.getSpans(start, end, android.text.style.ForegroundColorSpan.class);
                final int existingColor = (colorSpans != null && colorSpans.length > 0) 
                    ? colorSpans[0].getForegroundColor() : 0;
                
                android.text.style.ClickableSpan clickableSpan = new android.text.style.ClickableSpan() {
                    @Override
                    public void onClick(@NonNull View widget) {
                        // 使用外部浏览器打开链接
                        try {
                            android.content.Intent intent = new android.content.Intent(
                                android.content.Intent.ACTION_VIEW, 
                                android.net.Uri.parse(url)
                            );
                            widget.getContext().startActivity(intent);
                        } catch (Exception e) {
                            android.widget.Toast.makeText(
                                widget.getContext(), 
                                "无法打开链接", 
                                android.widget.Toast.LENGTH_SHORT
                            ).show();
                        }
                    }
                    
                    @Override
                    public void updateDrawState(@NonNull android.text.TextPaint ds) {
                        // 不调用 super，避免覆盖颜色
                        // 只设置下划线，保留原有颜色或使用链接蓝
                        if (existingColor != 0) {
                            ds.setColor(existingColor);
                        } else {
                            ds.setColor(0xFF4FC3F7); // 默认链接颜色
                        }
                        ds.setUnderlineText(true);
                    }
                };
                
                spannable.setSpan(
                    clickableSpan, 
                    start, 
                    end, 
                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE
                );
            }
            
            textView.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
            textView.setHighlightColor(0x334FC3F7); // 点击高亮颜色
        } catch (Exception e) {
            // 出错时不处理链接，保持原样
        }
    }

    class ColoredTextViewHolder extends RecyclerView.ViewHolder {
        TextView coloredTextContent;

        ColoredTextViewHolder(@NonNull View itemView) {
            super(itemView);
            coloredTextContent = itemView.findViewById(R.id.coloredTextContent);
        }

        void bind(ContentBlock block) {
            String text = block.getContent();
            int[] colors = block.getTextColors();
            int mainColor = colors.length > 0 ? colors[0] : 0xFF000000;
            
            // 检查是否包含URL
            boolean hasLinks = hasUrls(text);
            
            if (hasLinks) {
                // 有链接：创建 Spannable 并直接设置颜色和链接
                android.text.SpannableStringBuilder builder = new android.text.SpannableStringBuilder(text);
                
                // 设置颜色 span
                android.text.style.ForegroundColorSpan colorSpan = 
                        new android.text.style.ForegroundColorSpan(mainColor);
                builder.setSpan(colorSpan, 0, text.length(), 
                        android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                
                // 设置链接
                java.util.regex.Pattern urlPattern = java.util.regex.Pattern.compile(
                    "(https?://[a-zA-Z0-9\\-._~:/?#\\[\\]@!$&'()*+,;=%]+)",
                    java.util.regex.Pattern.CASE_INSENSITIVE
                );
                java.util.regex.Matcher matcher = urlPattern.matcher(text);
                
                while (matcher.find()) {
                    final String url = matcher.group(1);
                    int start = matcher.start(1);
                    int end = matcher.end(1);
                    
                    final int linkColor = mainColor;
                    android.text.style.ClickableSpan clickableSpan = new android.text.style.ClickableSpan() {
                        @Override
                        public void onClick(@NonNull View widget) {
                            try {
                                android.content.Intent intent = new android.content.Intent(
                                    android.content.Intent.ACTION_VIEW, 
                                    android.net.Uri.parse(url)
                                );
                                widget.getContext().startActivity(intent);
                            } catch (Exception e) {
                                android.widget.Toast.makeText(
                                    widget.getContext(), 
                                    "无法打开链接", 
                                    android.widget.Toast.LENGTH_SHORT
                                ).show();
                            }
                        }
                        
                        @Override
                        public void updateDrawState(@NonNull android.text.TextPaint ds) {
                            // 不调用 super，保留原有颜色
                            ds.setColor(linkColor);
                            ds.setUnderlineText(true);
                            ds.bgColor = 0; // 清除背景色
                        }
                    };
                    
                    builder.setSpan(clickableSpan, start, end, 
                            android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                }
                
                coloredTextContent.setText(builder);
                coloredTextContent.setMovementMethod(android.text.method.LinkMovementMethod.getInstance());
                coloredTextContent.setHighlightColor(0x334FC3F7);
            } else {
                // 无链接：使用原来的逻辑
                coloredTextContent.setText(text);
                if (colors.length == 1) {
                    coloredTextContent.setTextColor(mainColor);
                } else {
                    // 多个颜色
                    android.text.SpannableString spannableString = new android.text.SpannableString(text);
                    int colorIndex = 0;
                    for (int i = 0; i < text.length(); i++) {
                        if (colorIndex < colors.length) {
                            android.text.style.ForegroundColorSpan colorSpan = 
                                    new android.text.style.ForegroundColorSpan(colors[colorIndex]);
                            spannableString.setSpan(colorSpan, i, i + 1, 
                                    android.text.Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
                            colorIndex++;
                        }
                    }
                    coloredTextContent.setText(spannableString);
                }
            }
            
            // 应用字体大小设置
            FontUtils.applyFontSize(itemView.getContext(), coloredTextContent, FontUtils.SIZE_CONTENT);
        }
    }

    class ImageViewHolder extends RecyclerView.ViewHolder {
        ImageView imageContent;
        ProgressBar progressBar;

        ImageViewHolder(@NonNull View itemView) {
            super(itemView);
            imageContent = itemView.findViewById(R.id.imageContent);
            progressBar = itemView.findViewById(R.id.progressBar);
        }

        void bind(ContentBlock block, int position) {
            String url = block.getImageUrl();
            progressBar.setVisibility(View.VISIBLE);

            // 获取屏幕宽度，设置合理的图片加载尺寸
            int screenWidth = itemView.getResources().getDisplayMetrics().widthPixels;
            int targetWidth = screenWidth - (int)(32 * itemView.getResources().getDisplayMetrics().density);
            int targetHeight = (int)(400 * itemView.getResources().getDisplayMetrics().density);

            Glide.with(itemView.getContext())
                    .load(url)
                    .placeholder(R.drawable.ic_forum)
                    .error(R.drawable.ic_forum)
                    .override(targetWidth, targetHeight)
                    .diskCacheStrategy(DiskCacheStrategy.ALL)
                    .skipMemoryCache(false)
                    .listener(new RequestListener<Drawable>() {
                        @Override
                        public boolean onLoadFailed(GlideException e, Object model,
                                Target<Drawable> target, boolean isFirstResource) {
                            progressBar.setVisibility(View.GONE);
                            return false;
                        }

                        @Override
                        public boolean onResourceReady(Drawable resource,
                                Object model, Target<Drawable> target,
                                DataSource dataSource, boolean isFirstResource) {
                            progressBar.setVisibility(View.GONE);
                            return false;
                        }
                    })
                    .into(imageContent);

            // 设置图片点击监听
            imageContent.setOnClickListener(v -> {
                if (imageClickListener != null) {
                    List<String> allImages = getAllImageUrls();
                    int imageIndex = 0;
                    // 计算当前图片在图片列表中的位置
                    for (int i = 0; i <= position; i++) {
                        if (contentBlocks.get(i).isImage()) {
                            if (i == position) break;
                            imageIndex++;
                        }
                    }
                    imageClickListener.onImageClick(url, imageIndex, allImages);
                }
            });
        }
    }

    class QuoteViewHolder extends RecyclerView.ViewHolder {
        TextView quoteContent;

        QuoteViewHolder(@NonNull View itemView) {
            super(itemView);
            quoteContent = itemView.findViewById(R.id.quoteContent);
        }

        void bind(ContentBlock block) {
            quoteContent.setText(block.getContent());
            // 引用文字稍小
            FontUtils.applyFontSize(itemView.getContext(), quoteContent, FontUtils.SIZE_SUBTITLE);
        }
    }
    
    /**
     * 隐藏内容ViewHolder
     * 用于显示需要登录或回复才能查看的内容提示（一行紧凑显示）
     */
    class HiddenContentViewHolder extends RecyclerView.ViewHolder {
        ImageView hiddenIcon;
        TextView hiddenHint;
        TextView hiddenAction;

        HiddenContentViewHolder(@NonNull View itemView) {
            super(itemView);
            hiddenIcon = itemView.findViewById(R.id.hiddenIcon);
            hiddenHint = itemView.findViewById(R.id.hiddenHint);
            hiddenAction = itemView.findViewById(R.id.hiddenAction);
        }

        void bind(ContentBlock block) {
            int hiddenType = block.getHiddenType();
            String hint = block.getHiddenHint();
            
            // 根据隐藏类型设置不同的提示和按钮文字
            if (hiddenType == ContentBlock.HIDDEN_TYPE_LOGIN) {
                hiddenHint.setText(hint != null ? hint : "本帖子中包含更多精彩资源");
                hiddenAction.setText("登录查看");
            } else {
                hiddenHint.setText(hint != null ? hint : "回复后查看隐藏内容");
                hiddenAction.setText("去回复");
            }
            
            // 点击操作按钮
            hiddenAction.setOnClickListener(v -> {
                if (hiddenType == ContentBlock.HIDDEN_TYPE_LOGIN) {
                    Toast.makeText(itemView.getContext(), "请先登录", Toast.LENGTH_SHORT).show();
                } else {
                    Toast.makeText(itemView.getContext(), "请回复帖子后查看", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    
    /**
     * 悬赏内容ViewHolder
     * 用于显示悬赏金币和回答按钮
     */
    class BountyViewHolder extends RecyclerView.ViewHolder {
        ImageView bountyIcon;
        TextView bountyAmountText;
        TextView bountyContent;
        TextView bountyAnswerBtn;

        BountyViewHolder(@NonNull View itemView) {
            super(itemView);
            bountyIcon = itemView.findViewById(R.id.bountyIcon);
            bountyAmountText = itemView.findViewById(R.id.bountyAmountText);
            bountyContent = itemView.findViewById(R.id.bountyContent);
            bountyAnswerBtn = itemView.findViewById(R.id.bountyAnswerBtn);
        }

        void bind(ContentBlock block) {
            int amount = block.getBountyAmount();
            
            // 设置悬赏金币数
            bountyAmountText.setText("悬赏 " + amount + " 金币");
            
            // 设置悬赏问题描述
            String content = block.getContent();
            if (content != null && !content.isEmpty()) {
                bountyContent.setText(content);
                bountyContent.setVisibility(View.VISIBLE);
            } else {
                bountyContent.setVisibility(View.GONE);
            }
            
            // 点击回答按钮
            bountyAnswerBtn.setOnClickListener(v -> {
                Toast.makeText(itemView.getContext(), "您的回答被采纳后将获得" + amount + "金币", Toast.LENGTH_LONG).show();
            });
        }
    }
    
    /**
     * 付费帖子ViewHolder
     * 用于显示付费内容提示和购买按钮
     */
    class PaidPostViewHolder extends RecyclerView.ViewHolder {
        TextView buyersText;
        TextView priceText;
        TextView deadlineText;
        com.google.android.material.button.MaterialButton buyButton;
        TextView purchasedText;

        PaidPostViewHolder(@NonNull View itemView) {
            super(itemView);
            buyersText = itemView.findViewById(R.id.paidBuyersText);
            priceText = itemView.findViewById(R.id.paidPriceText);
            deadlineText = itemView.findViewById(R.id.paidDeadlineText);
            buyButton = itemView.findViewById(R.id.buyPostButton);
            purchasedText = itemView.findViewById(R.id.purchasedText);
        }

        void bind(ContentBlock block, int position) {
            // 购买人数
            int buyers = block.getPaidBuyers();
            buyersText.setText("已有 " + buyers + " 人购买");
            
            // 价格
            int price = block.getPaidPrice();
            priceText.setText("需支付 " + price + " 金币");
            
            // 截止日期
            String deadline = block.getPaidDeadline();
            if (deadline != null && !deadline.isEmpty()) {
                deadlineText.setText("截止: " + deadline);
                deadlineText.setVisibility(View.VISIBLE);
            } else {
                deadlineText.setVisibility(View.GONE);
            }
            
            // 已购买/未购买状态
            if (block.hasPurchased()) {
                buyButton.setVisibility(View.GONE);
                purchasedText.setVisibility(View.VISIBLE);
            } else {
                buyButton.setVisibility(View.VISIBLE);
                purchasedText.setVisibility(View.GONE);
                buyButton.setOnClickListener(v -> {
                    if (buyPostClickListener != null) {
                        buyPostClickListener.onBuyPostClick(block, position);
                    }
                });
            }
        }
    }
    
    /**
     * 付费主题信息ViewHolder（购买后显示）
     * 显示"付费主题, 价格: X 金币"
     */
    class PaidPostInfoViewHolder extends RecyclerView.ViewHolder {
        TextView priceText;

        PaidPostInfoViewHolder(@NonNull View itemView) {
            super(itemView);
            priceText = itemView.findViewById(R.id.paidPriceText);
        }

        void bind(ContentBlock block) {
            int price = block.getPaidPrice();
            priceText.setText(price + " 金币");
        }
    }
    
    /**
     * 附件ViewHolder
     * 用于显示文件下载信息
     */
    class AttachmentViewHolder extends RecyclerView.ViewHolder {
        ImageView fileTypeIcon;
        TextView fileNameText;
        TextView uploadTimeText;
        TextView downloadInfoText;

        AttachmentViewHolder(@NonNull View itemView) {
            super(itemView);
            fileTypeIcon = itemView.findViewById(R.id.fileTypeIcon);
            fileNameText = itemView.findViewById(R.id.fileNameText);
            uploadTimeText = itemView.findViewById(R.id.uploadTimeText);
            downloadInfoText = itemView.findViewById(R.id.downloadInfoText);
        }

        void bind(ContentBlock block) {
            ContentBlock.Attachment attachment = block.getAttachment();
            if (attachment == null) return;
            
            // 文件名
            fileNameText.setText(attachment.getFileName());
            
            // 上传时间
            String uploadTime = attachment.getUploadTime();
            if (uploadTime != null) {
                uploadTimeText.setVisibility(View.VISIBLE);
                uploadTimeText.setText(uploadTime);
            } else {
                uploadTimeText.setVisibility(View.GONE);
            }
            
            // 下载信息
            downloadInfoText.setText(attachment.getFormattedDownloadInfo());
            
            // 文件类型图标
            String iconUrl = attachment.getFileTypeIcon();
            if (iconUrl != null) {
                Glide.with(itemView.getContext().getApplicationContext())
                        .load(iconUrl)
                        .placeholder(R.drawable.ic_file_document)
                        .error(R.drawable.ic_file_document)
                        .into(fileTypeIcon);
            } else {
                fileTypeIcon.setImageResource(R.drawable.ic_file_document);
            }
            
            // 点击下载
            itemView.setOnClickListener(v -> {
                String url = attachment.getDownloadUrl();
                if (url != null) {
                    android.content.Intent intent = new android.content.Intent(android.content.Intent.ACTION_VIEW);
                    intent.setData(android.net.Uri.parse(url));
                    intent.addFlags(android.content.Intent.FLAG_ACTIVITY_NEW_TASK);
                    try {
                        itemView.getContext().startActivity(intent);
                    } catch (Exception e) {
                        Toast.makeText(itemView.getContext(), "无法打开链接", Toast.LENGTH_SHORT).show();
                    }
                }
            });
        }
    }
    
    /**
     * 表情符号ViewHolder
     * 用于显示行内表情图片
     */
    class SmileyViewHolder extends RecyclerView.ViewHolder {
        ImageView smileyImage;

        SmileyViewHolder(@NonNull View itemView) {
            super(itemView);
            smileyImage = (ImageView) itemView;
        }

        void bind(ContentBlock block) {
            String url = block.getImageUrl();
            if (url != null) {
                Glide.with(itemView.getContext().getApplicationContext())
                        .load(url)
                        .placeholder(R.drawable.ic_forum)
                        .error(R.drawable.ic_forum)
                        .into(smileyImage);
            }
        }
    }
    
    /**
     * 富文本ViewHolder
     * 用于显示包含表情的文本内容，表情在正确位置显示
     */
    class RichTextViewHolder extends RecyclerView.ViewHolder {
        TextView textContent;

        RichTextViewHolder(@NonNull View itemView) {
            super(itemView);
            textContent = itemView.findViewById(R.id.textContent);
        }

        void bind(ContentBlock block) {
            String html = block.getContent();
            if (html != null && !html.isEmpty()) {
                // 使用 GlideImageGetter 支持表情图片在文本正确位置显示
                com.forum.mt.util.GlideImageGetter.setHtmlWithImages(textContent, html);
                
                // 应用字体大小设置
                FontUtils.applyFontSize(itemView.getContext(), textContent, FontUtils.SIZE_CONTENT);
                
                // 获取显示后的文本，检查是否包含URL并处理
                // 注意：需要在设置GlideImageGetter之后处理，保留表情图片
                String displayText = textContent.getText().toString();
                if (hasUrls(displayText)) {
                    makeLinksClickable(textContent, displayText);
                }
            } else {
                textContent.setText("");
            }
            // 设置文字长按监听
            if (textLongClickListener != null) {
                textContent.setOnLongClickListener(textLongClickListener);
            }
        }
    }
    
    /**
     * 代码块ViewHolder
     * 用于显示代码内容，支持复制功能
     */
    class CodeViewHolder extends RecyclerView.ViewHolder {
        TextView tvCodeLanguage;
        TextView tvCopyCode;
        TextView tvLineNumbers;
        TextView tvCodeContent;
        TextView tvLineCount;

        CodeViewHolder(@NonNull View itemView) {
            super(itemView);
            tvCodeLanguage = itemView.findViewById(R.id.tvCodeLanguage);
            tvCopyCode = itemView.findViewById(R.id.tvCopyCode);
            tvLineNumbers = itemView.findViewById(R.id.tvLineNumbers);
            tvCodeContent = itemView.findViewById(R.id.tvCodeContent);
            tvLineCount = itemView.findViewById(R.id.tvLineCount);
        }

        void bind(ContentBlock block) {
            String code = block.getCodeContent();
            String language = block.getCodeLanguage();
            int lineCount = block.getCodeLineCount();
            
            // 设置代码语言标签
            if (language != null && !language.isEmpty()) {
                tvCodeLanguage.setText(language);
            } else {
                tvCodeLanguage.setText("Code");
            }
            
            // 设置代码内容
            if (code != null) {
                tvCodeContent.setText(code);
                // 代码块使用较小的字体
                FontUtils.applyFontSize(itemView.getContext(), tvCodeContent, FontUtils.SIZE_SMALL);
                
                // 生成行号
                StringBuilder lineNumbers = new StringBuilder();
                String[] lines = code.split("\n");
                for (int i = 1; i <= lines.length; i++) {
                    if (i > 1) {
                        lineNumbers.append("\n");
                    }
                    lineNumbers.append(i);
                }
                tvLineNumbers.setText(lineNumbers.toString());
                FontUtils.applyFontSize(itemView.getContext(), tvLineNumbers, FontUtils.SIZE_SMALL);
                
                // 设置行数统计
                if (lineCount > 20) {
                    tvLineCount.setText("共 " + lineCount + " 行");
                    tvLineCount.setVisibility(View.VISIBLE);
                } else {
                    tvLineCount.setVisibility(View.GONE);
                }
            } else {
                tvCodeContent.setText("");
                tvLineNumbers.setText("");
                tvLineCount.setVisibility(View.GONE);
            }
            
            // 复制按钮点击事件
            tvCopyCode.setOnClickListener(v -> {
                if (code != null && !code.isEmpty()) {
                    android.content.ClipboardManager clipboard = 
                        (android.content.ClipboardManager) itemView.getContext()
                            .getSystemService(android.content.Context.CLIPBOARD_SERVICE);
                    android.content.ClipData clip = 
                        android.content.ClipData.newPlainText("code", code);
                    clipboard.setPrimaryClip(clip);
                    Toast.makeText(itemView.getContext(), "代码已复制", Toast.LENGTH_SHORT).show();
                }
            });
        }
    }
    
    /**
     * 富文本样式ViewHolder
     * 用于显示带样式的文本（加粗、斜体、下划线、删除线、颜色、背景色等）
     */
    class StyledTextViewHolder extends RecyclerView.ViewHolder {
        TextView textContent;

        StyledTextViewHolder(@NonNull View itemView) {
            super(itemView);
            textContent = itemView.findViewById(R.id.textContent);
        }

        void bind(ContentBlock block) {
            String styledHtml = block.getStyledHtml();
            String plainText = block.getContent();
            
            if (styledHtml != null && !styledHtml.isEmpty()) {
                // 使用 Html.fromHtml 解析带样式的HTML
                // 支持 <b>, <i>, <u>, <strike>, <font color=""> 等标签
                Spanned spanned;
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                    spanned = Html.fromHtml(styledHtml, Html.FROM_HTML_MODE_COMPACT);
                } else {
                    spanned = Html.fromHtml(styledHtml);
                }
                
                // 使用 GlideImageGetter 支持表情图片
                com.forum.mt.util.GlideImageGetter.setHtmlWithImages(textContent, styledHtml);
                
                // 应用字体大小设置
                FontUtils.applyFontSize(itemView.getContext(), textContent, FontUtils.SIZE_CONTENT);
                
            } else if (plainText != null && !plainText.isEmpty()) {
                textContent.setText(plainText);
                FontUtils.applyFontSize(itemView.getContext(), textContent, FontUtils.SIZE_CONTENT);
            } else {
                textContent.setText("");
            }
            
            // 设置文字长按监听
            if (textLongClickListener != null) {
                textContent.setOnLongClickListener(textLongClickListener);
            }
        }
    }
    
    /**
     * 分割线ViewHolder
     * 用于显示水平分割线
     */
    class DividerViewHolder extends RecyclerView.ViewHolder {
        View dividerLine;

        DividerViewHolder(@NonNull View itemView) {
            super(itemView);
            dividerLine = itemView.findViewById(R.id.dividerLine);
        }

        void bind(ContentBlock block) {
            // 分割线不需要额外数据绑定
        }
    }
}