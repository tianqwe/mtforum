package com.forum.mt.util;

import android.content.ClipData;
import android.content.ClipboardManager;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.text.TextUtils;
import android.widget.Toast;

import com.forum.mt.R;
import com.forum.mt.model.Post;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * 分享工具类
 * 基于MT论坛移动端JS分享逻辑实现
 */
public class ShareHelper {

    private Context context;
    private Post post;
    private String shareUrl;
    private String shareTitle;
    private String shareDesc;
    private String shareImage;

    public ShareHelper(Context context) {
        this.context = context;
    }

    /**
     * 设置要分享的帖子
     */
    public ShareHelper setPost(Post post) {
        this.post = post;
        if (post != null) {
            this.shareUrl = post.getPostUrl();
            this.shareTitle = post.getTitle();
            this.shareDesc = generateShareDesc(post);
            this.shareImage = post.getThumbnail();
        }
        return this;
    }

    /**
     * 自定义分享内容
     */
    public ShareHelper setShareContent(String url, String title, String desc, String imageUrl) {
        this.shareUrl = url;
        this.shareTitle = title;
        this.shareDesc = desc;
        this.shareImage = imageUrl;
        return this;
    }

    /**
     * 生成分享描述
     */
    private String generateShareDesc(Post post) {
        if (post == null) return "";
        
        StringBuilder sb = new StringBuilder();
        
        // 添加作者信息
        if (!TextUtils.isEmpty(post.getAuthor())) {
            sb.append("作者: ").append(post.getAuthor());
        }
        
        // 添加版块信息
        if (!TextUtils.isEmpty(post.getForumName())) {
            if (sb.length() > 0) sb.append(" | ");
            sb.append(post.getForumName());
        }
        
        // 添加摘要
        if (!TextUtils.isEmpty(post.getSummary())) {
            if (sb.length() > 0) sb.append("\n");
            // 截取前100个字符作为摘要
            String summary = post.getSummary();
            if (summary.length() > 100) {
                summary = summary.substring(0, 100) + "...";
            }
            sb.append(summary);
        }
        
        return sb.toString();
    }

    /**
     * 分享到微信好友
     */
    public void shareToWechat() {
        if (!isAppInstalled("com.tencent.mm")) {
            Toast.makeText(context, "未安装微信", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.tencent.mm", 
                    "com.tencent.mm.ui.tools.ShareImgUI"));
            intent.setAction(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, buildShareText());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            // 降级为通用分享
            shareGeneric();
        }
    }

    /**
     * 分享到微信朋友圈
     */
    public void shareToWechatMoments() {
        if (!isAppInstalled("com.tencent.mm")) {
            Toast.makeText(context, "未安装微信", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            Intent intent = new Intent();
            intent.setComponent(new ComponentName("com.tencent.mm", 
                    "com.tencent.mm.ui.tools.ShareToTimeLineUI"));
            intent.setAction(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, buildShareText());
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            // 降级为通用分享
            shareGeneric();
        }
    }

    /**
     * 分享到QQ好友
     */
    public void shareToQQ() {
        if (!isAppInstalled("com.tencent.mobileqq")) {
            Toast.makeText(context, "未安装QQ", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, shareTitle);
            intent.putExtra(Intent.EXTRA_TEXT, buildShareText());
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            
            Intent chooser = Intent.createChooser(intent, "分享到QQ");
            
            // 过滤只显示QQ
            List<Intent> targetedShareIntents = new ArrayList<>();
            List<ResolveInfo> resInfo = context.getPackageManager()
                    .queryIntentActivities(intent, 0);
            
            for (ResolveInfo info : resInfo) {
                String packageName = info.activityInfo.packageName;
                if (packageName.contains("tencent") || packageName.contains("qq")) {
                    Intent targeted = new Intent(intent);
                    targeted.setPackage(packageName);
                    targetedShareIntents.add(targeted);
                }
            }
            
            if (!targetedShareIntents.isEmpty()) {
                chooser.putExtra(Intent.EXTRA_INITIAL_INTENTS, 
                        targetedShareIntents.toArray(new Intent[]{}));
            }
            
            context.startActivity(chooser);
        } catch (Exception e) {
            shareGeneric();
        }
    }

    /**
     * 分享到QQ空间
     */
    public void shareToQZone() {
        if (!isAppInstalled("com.qzone")) {
            Toast.makeText(context, "未安装QQ空间", Toast.LENGTH_SHORT).show();
            return;
        }
        
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_SUBJECT, shareTitle);
            intent.putExtra(Intent.EXTRA_TEXT, buildShareText());
            intent.setPackage("com.qzone");
            intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            // 尝试使用Web方式分享到QQ空间
            shareToQZoneWeb();
        }
    }

    /**
     * 通过Web方式分享到QQ空间
     */
    private void shareToQZoneWeb() {
        try {
            String url = "https://h5.qzone.qq.com/q/qzs/open/connect/widget/mobile/qzshare/index.html" +
                    "?page=qzshare.html" +
                    "&loginpage=loginindex.html" +
                    "&logintype=qzone" +
                    "&title=" + Uri.encode(shareTitle) +
                    "&summary=" + Uri.encode(shareDesc) +
                    "&url=" + Uri.encode(shareUrl) +
                    "&site=" + Uri.encode("MT论坛");
            
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "分享失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 分享到新浪微博
     */
    public void shareToWeibo() {
        if (!isAppInstalled("com.sina.weibo")) {
            // 使用Web方式分享
            shareToWeiboWeb();
            return;
        }
        
        try {
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("text/plain");
            intent.putExtra(Intent.EXTRA_TEXT, buildShareText());
            intent.setPackage("com.sina.weibo");
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            shareToWeiboWeb();
        }
    }

    /**
     * 通过Web方式分享到新浪微博
     */
    private void shareToWeiboWeb() {
        try {
            String url = "http://service.weibo.com/share/share.php" +
                    "?url=" + Uri.encode(shareUrl) +
                    "&title=" + Uri.encode(shareTitle) +
                    "&appkey=" +
                    "&pic=" + Uri.encode(shareImage != null ? shareImage : "") +
                    "&language=zh_cn";
            
            Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
            intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(intent);
        } catch (Exception e) {
            Toast.makeText(context, "分享失败", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 复制链接到剪贴板
     */
    public void copyLink() {
        ClipboardManager clipboard = (ClipboardManager) 
                context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            ClipData clip = ClipData.newPlainText("帖子链接", shareUrl);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "链接已复制", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 复制分享内容（带标题和链接）
     */
    public void copyShareContent() {
        ClipboardManager clipboard = (ClipboardManager) 
                context.getSystemService(Context.CLIPBOARD_SERVICE);
        if (clipboard != null) {
            String content = shareTitle + "\n" + shareUrl;
            ClipData clip = ClipData.newPlainText("分享内容", content);
            clipboard.setPrimaryClip(clip);
            Toast.makeText(context, "内容已复制", Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * 通用分享（系统分享面板）
     */
    public void shareGeneric() {
        Intent intent = new Intent(Intent.ACTION_SEND);
        intent.setType("text/plain");
        intent.putExtra(Intent.EXTRA_SUBJECT, shareTitle);
        intent.putExtra(Intent.EXTRA_TEXT, buildShareText());
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        Intent chooser = Intent.createChooser(intent, "分享到");
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(chooser);
    }

    /**
     * 构建分享文本
     */
    private String buildShareText() {
        StringBuilder sb = new StringBuilder();
        
        // 标题
        if (!TextUtils.isEmpty(shareTitle)) {
            sb.append("【").append(shareTitle).append("】\n");
        }
        
        // 描述
        if (!TextUtils.isEmpty(shareDesc)) {
            sb.append(shareDesc).append("\n");
        }
        
        // 链接
        if (!TextUtils.isEmpty(shareUrl)) {
            sb.append("\n链接: ").append(shareUrl);
        }
        
        // 来源
        sb.append("\n——来自MT论坛");
        
        return sb.toString();
    }

    /**
     * 检查应用是否安装
     */
    public boolean isAppInstalled(String packageName) {
        try {
            context.getPackageManager().getPackageInfo(packageName, 
                    PackageManager.GET_ACTIVITIES);
            return true;
        } catch (PackageManager.NameNotFoundException e) {
            return false;
        }
    }

    /**
     * 打开帖子链接
     */
    public void openInBrowser() {
        if (TextUtils.isEmpty(shareUrl)) return;
        
        Intent intent = new Intent(Intent.ACTION_VIEW, Uri.parse(shareUrl));
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        
        // 让用户选择浏览器
        Intent chooser = Intent.createChooser(intent, "使用浏览器打开");
        chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(chooser);
    }

    /**
     * 分享图片到指定应用
     */
    public void shareImageToApp(Drawable drawable, String packageName) {
        try {
            // 将Drawable转换为Bitmap
            Bitmap bitmap = drawableToBitmap(drawable);
            if (bitmap == null) {
                Toast.makeText(context, "图片处理失败", Toast.LENGTH_SHORT).show();
                return;
            }
            
            // 保存到临时文件
            File cachePath = new File(context.getCacheDir(), "share_images");
            cachePath.mkdirs();
            File imageFile = new File(cachePath, "share_" + System.currentTimeMillis() + ".jpg");
            
            FileOutputStream fos = new FileOutputStream(imageFile);
            bitmap.compress(Bitmap.CompressFormat.JPEG, 90, fos);
            fos.flush();
            fos.close();
            
            // 分享
            Uri uri = getUriForFile(imageFile);
            
            Intent intent = new Intent(Intent.ACTION_SEND);
            intent.setType("image/jpeg");
            intent.putExtra(Intent.EXTRA_STREAM, uri);
            intent.putExtra(Intent.EXTRA_TEXT, buildShareText());
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            
            if (!TextUtils.isEmpty(packageName)) {
                intent.setPackage(packageName);
            }
            
            Intent chooser = Intent.createChooser(intent, "分享图片");
            chooser.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            context.startActivity(chooser);
            
        } catch (IOException e) {
            Toast.makeText(context, "分享失败: " + e.getMessage(), Toast.LENGTH_SHORT).show();
        }
    }

    /**
     * Drawable转Bitmap
     */
    private Bitmap drawableToBitmap(Drawable drawable) {
        if (drawable instanceof BitmapDrawable) {
            return ((BitmapDrawable) drawable).getBitmap();
        }
        
        int width = drawable.getIntrinsicWidth();
        int height = drawable.getIntrinsicHeight();
        
        if (width <= 0 || height <= 0) {
            width = 1;
            height = 1;
        }
        
        Bitmap bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bitmap);
        drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
        drawable.draw(canvas);
        
        return bitmap;
    }

    /**
     * 获取文件的Uri（兼容Android 7.0+）
     */
    private Uri getUriForFile(File file) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            String authority = context.getPackageName() + ".fileprovider";
            return androidx.core.content.FileProvider.getUriForFile(context, authority, file);
        } else {
            return Uri.fromFile(file);
        }
    }

    // Getters
    public String getShareUrl() { return shareUrl; }
    public String getShareTitle() { return shareTitle; }
    public String getShareDesc() { return shareDesc; }
    public String getShareImage() { return shareImage; }
}
