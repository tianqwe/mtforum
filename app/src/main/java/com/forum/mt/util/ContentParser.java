package com.forum.mt.util;

import android.text.TextUtils;

import com.forum.mt.model.ContentBlock;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.nodes.Node;
import org.jsoup.nodes.TextNode;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;

/**
 * HTML内容解析器
 * 将HTML内容解析为原生Android组件可显示的内容块列表
 */
public class ContentParser {

    /**
     * 解析HTML内容为内容块列表
     */
    public static List<ContentBlock> parse(String html) {
        List<ContentBlock> blocks = new ArrayList<>();
        
        if (html == null || html.trim().isEmpty()) {
            blocks.add(ContentBlock.createTextBlock("暂无内容"));
            return blocks;
        }

        try {
            Document doc = Jsoup.parse(html);
            Element body = doc.body();
            
            // 递归解析所有节点（隐藏内容在 parseNode 中处理）
            parseNode(body, blocks);
            
            // 如果没有解析出任何内容，显示原始文本
            if (blocks.isEmpty()) {
                String text = doc.text();
                if (!TextUtils.isEmpty(text)) {
                    blocks.add(ContentBlock.createTextBlock(text));
                }
            }
            
            // 合并相邻的文本和表情块为富文本块
            blocks = mergeTextAndSmileys(blocks);
            
            // 合并相邻的文本块
            blocks = mergeTextBlocks(blocks);
            
        } catch (Exception e) {
            // 解析失败，尝试提取纯文本
            String text = Jsoup.parse(html).text();
            if (!TextUtils.isEmpty(text)) {
                blocks.add(ContentBlock.createTextBlock(text));
            } else {
                blocks.add(ContentBlock.createTextBlock("内容解析失败"));
            }
        }
        
        return blocks;
    }
    
    /**
     * 从文本中提取隐藏内容提示
     */
    private static String extractHiddenHint(String text) {
        // 尝试提取更有意义的提示
        if (text.contains("如果您要查看本帖隐藏内容请回复")) {
            return "如果您要查看本帖隐藏内容请回复";
        }
        if (text.contains("回复可见")) {
            return "本帖内容需要回复后才能查看";
        }
        if (text.contains("隐藏内容")) {
            return "本帖包含隐藏内容";
        }
        return "内容已隐藏";
    }

    /**
     * 递归解析节点
     */
    private static void parseNode(Node node, List<ContentBlock> blocks) {
        if (node == null) return;
        
        for (Node child : node.childNodes()) {
            if (child instanceof TextNode) {
                TextNode textNode = (TextNode) child;
                String text = textNode.text().trim();
                if (!TextUtils.isEmpty(text)) {
                    blocks.add(ContentBlock.createTextBlock(text));
                }
            } else if (child instanceof Element) {
                Element element = (Element) child;
                String tagName = element.tagName().toLowerCase();
                
                switch (tagName) {
                    case "img":
                        // 图片
                        String src = element.attr("src");
                        if (!TextUtils.isEmpty(src)) {
                            // 处理相对路径
                            if (src.startsWith("//")) {
                                src = "https:" + src;
                            } else if (src.startsWith("/")) {
                                src = "https://bbs.binmt.cc" + src;
                            }
                            
                            // 过滤头像
                            if (src.contains("avatar.php")) {
                                break;
                            }
                            
                            // 表情符号使用特殊类型，显示为内联小图片
                            if (src.contains("smiley") || src.contains("emoticon")) {
                                blocks.add(ContentBlock.createSmileyBlock(src));
                            } else {
                                // 普通图片
                                blocks.add(ContentBlock.createImageBlock(src));
                            }
                        }
                        break;
                        
                    case "br":
                        // 换行符，添加空行
                        blocks.add(ContentBlock.createTextBlock(""));
                        break;
                        
                    case "blockquote":
                    case "quote":
                        // 引用块
                        String quoteText = element.text().trim();
                        if (!TextUtils.isEmpty(quoteText)) {
                            blocks.add(ContentBlock.createQuoteBlock(quoteText));
                        }
                        break;
                        
                    case "p":
                    case "div":
                    case "span":
                        // 检查是否是代码块区域
                        if (tagName.equals("div")) {
                            ContentBlock codeBlock = checkCodeBlock(element);
                            if (codeBlock != null) {
                                blocks.add(codeBlock);
                                break; // 跳过此div，不递归解析子节点
                            }
                            // 检查是否是隐藏内容区域
                            ContentBlock hiddenBlock = checkHiddenContent(element);
                            if (hiddenBlock != null) {
                                blocks.add(hiddenBlock);
                                break; // 跳过此div，不递归解析子节点
                            }
                            // 检查是否是悬赏内容区域
                            ContentBlock bountyBlock = checkBountyContent(element);
                            if (bountyBlock != null) {
                                blocks.add(bountyBlock);
                                break; // 跳过此div，不递归解析子节点
                            }
                            // 检查是否是附件区域（普通帖子的附件直接是div.comiis_attach）
                            List<ContentBlock> attachBlocks = checkAttachment(element);
                            if (attachBlocks != null) {
                                blocks.addAll(attachBlocks);
                                break; // 跳过此div，不递归解析子节点
                            }
                        }
                        // 容器元素，递归解析子节点
                        parseNode(element, blocks);
                        // 段落后添加空行
                        if (tagName.equals("p")) {
                            blocks.add(ContentBlock.createTextBlock(""));
                        }
                        break;
                        
                    case "table":
                    case "thead":
                    case "tbody":
                    case "tr":
                    case "th":
                    case "td":
                        // 表格元素，递归解析子节点（图片可能在td中）
                        parseNode(element, blocks);
                        break;
                        
                    case "a":
                        // 链接，显示文本并保存URL
                        String linkText = element.text().trim();
                        String linkHref = element.attr("href");
                        
                        // 处理相对路径
                        if (!TextUtils.isEmpty(linkHref)) {
                            if (linkHref.startsWith("//")) {
                                linkHref = "https:" + linkHref;
                            } else if (linkHref.startsWith("/")) {
                                linkHref = "https://bbs.binmt.cc" + linkHref;
                            } else if (!linkHref.startsWith("http://") && !linkHref.startsWith("https://")) {
                                // 相对路径
                                linkHref = "https://bbs.binmt.cc/" + linkHref;
                            }
                        }
                        
                        // 过滤掉内部链接（如 forum.php, member.php, home.php 等）
                        boolean isInternalLink = !TextUtils.isEmpty(linkHref) && 
                            (linkHref.contains("member.php") || 
                             linkHref.contains("home.php?mod=space") ||
                             linkHref.contains("forum.php?mod=post") ||
                             linkHref.contains("javascript:"));
                        
                        if (!TextUtils.isEmpty(linkText)) {
                            if (!TextUtils.isEmpty(linkHref) && !isInternalLink) {
                                // 有外部链接，创建链接块
                                blocks.add(ContentBlock.createLinkBlock(linkText, linkHref));
                            } else {
                                // 无链接或内部链接，只显示文本
                                blocks.add(ContentBlock.createTextBlock(linkText));
                            }
                        }
                        break;
                        
                    case "pre":
                    case "code":
                        // 代码块
                        String code = element.text();
                        if (!TextUtils.isEmpty(code)) {
                            blocks.add(ContentBlock.createTextBlock(code));
                        }
                        break;
                        
                    case "strong":
                    case "b":
                    case "i":
                    case "em":
                    case "u":
                    case "strike":
                    case "s":
                        // 格式化文本 - 检查是否包含样式，生成富文本块
                        parseStyledElement(element, blocks, tagName);
                        break;
                        
                    case "font":
                        // 字体标签 - 支持颜色、背景色、大小等
                        parseFontElement(element, blocks);
                        break;
                        
                    case "hr":
                        // 分割线
                        blocks.add(ContentBlock.createDividerBlock());
                        break;
                        
                    case "ol":
                    case "ul":
                        // 检查是否是附件列表
                        if (tagName.equals("ul")) {
                            List<ContentBlock> attachBlocks = checkAttachment(element);
                            if (attachBlocks != null) {
                                blocks.addAll(attachBlocks);
                                break; // 跳过递归解析
                            }
                        }
                        // 列表元素，递归解析子节点
                        parseNode(element, blocks);
                        break;
                        
                    case "li":
                        // 列表项，递归解析子节点
                        parseNode(element, blocks);
                        break;
                        
                    default:
                        // 其他元素，递归解析子节点
                        parseNode(element, blocks);
                        break;
                }
            }
        }
    }

    /**
     * 检查元素是否是代码块区域
     * HTML结构:
     * <div class="comiis_blockcode comiis_bodybg b_ok f_b">
     *   <div class="bg_f b_l">
     *     <ol>
     *       <li>// 代码内容<br /></li>
     *       <li>// 第二行代码<br /></li>
     *     </ol>
     *   </div>
     * </div>
     * 
     * @return 如果是代码块，返回对应的 ContentBlock；否则返回 null
     */
    private static ContentBlock checkCodeBlock(Element element) {
        String className = element.className();
        
        // 检测代码块容器
        if (className.contains("comiis_blockcode")) {
            StringBuilder codeBuilder = new StringBuilder();
            
            // 获取 ol 中的所有 li
            Elements liElements = element.select("ol > li");
            for (int i = 0; i < liElements.size(); i++) {
                Element li = liElements.get(i);
                // 获取每行的代码内容
                String line = li.html();
                // 处理<br>标签转换为换行
                line = line.replaceAll("<br\\s*/?>", "");
                // 处理HTML实体
                line = line.replace("&nbsp;", " ")
                          .replace("&amp;", "&")
                          .replace("&lt;", "<")
                          .replace("&gt;", ">")
                          .replace("&quot;", "\"")
                          .replace("&#39;", "'");
                // 移除其他HTML标签（如URL链接等）
                line = line.replaceAll("<[^>]+>", "");
                
                codeBuilder.append(line);
                // 每行末尾添加换行（最后一行不添加）
                if (i < liElements.size() - 1) {
                    codeBuilder.append("\n");
                }
            }
            
            String code = codeBuilder.toString().trim();
            if (!TextUtils.isEmpty(code)) {
                return ContentBlock.createCodeBlock(code, null);
            }
        }
        
        return null;
    }

    /**
     * 检查元素是否是隐藏内容区域
     * @return 如果是隐藏内容，返回对应的 ContentBlock；否则返回 null
     */
    private static ContentBlock checkHiddenContent(Element element) {
        String className = element.className();
        String divText = element.text();
        
        // 检测"登录可见资源"类型
        // 格式: <div class="comiis_p10 bg_e ...">...</div>
        if (className.contains("comiis_p10")) {
            if (divText.contains("登录") && divText.contains("查看")) {
                String hint = "本帖子中包含更多精彩资源";
                Element h3 = element.selectFirst("h3");
                if (h3 != null) {
                    hint = h3.text();
                }
                return ContentBlock.createHiddenBlock(ContentBlock.HIDDEN_TYPE_LOGIN, hint);
            }
        }
        
        // 检测回复可见类型
        // 格式: <div class="comiis_quote bg_h f_c">...隐藏内容...回复...</div>
        if (className.contains("comiis_quote")) {
            // 检测付费主题信息（购买后显示）
            // 格式: <div class="comiis_quote comiis_qianglou bg_h">付费主题, 价格: <strong>2 金币</strong></div>
            if (className.contains("comiis_qianglou") && divText.contains("付费主题") && divText.contains("价格")) {
                return parsePaidPostInfoBlock(element);
            }
            
            // 检测付费帖子内容块（包含"购买"和"金币"，未购买状态）
            if (divText.contains("购买") && divText.contains("金币") && !divText.contains("付费主题, 价格")) {
                return parsePaidPostBlock(element);
            }
            
            // 检测已解锁的隐藏内容（包含"本帖隐藏的内容"标题，表示用户已回复可见）
            // 这种情况不应该返回隐藏块，而是让调用者继续解析内容
            if (divText.contains("本帖隐藏的内容") || divText.contains("隐藏的内容")) {
                // 返回 null，让调用者继续解析这个 div 的子节点
                // 这样里面的链接就能被正确识别
                return null;
            }
            
            // 只有短文本才是隐藏提示，避免误判长引用块
            if (divText.length() < 200 && divText.contains("隐藏内容") && divText.contains("回复")) {
                String hint = extractHiddenHint(divText);
                return ContentBlock.createHiddenBlock(ContentBlock.HIDDEN_TYPE_REPLY, hint);
            }
        }
        
        return null;
    }
    
    /**
     * 解析付费主题信息块（购买后显示）
     * HTML结构:
     * <div class="comiis_quote comiis_qianglou bg_h">
     *   <a href="forum.php?mod=misc&action=viewpayments&tid=xxx" class="y f_a">记录</a>
     *   <i class="comiis_font f_a">&#xe61d;</i>&nbsp;付费主题, 价格: <strong>2 金币</strong>
     * </div>
     */
    private static ContentBlock parsePaidPostInfoBlock(Element element) {
        // 解析价格: "付费主题, 价格: 2 金币"
        int price = 0;
        Element priceElem = element.selectFirst("strong");
        if (priceElem != null) {
            String priceText = priceElem.text();
            java.util.regex.Pattern pricePattern = java.util.regex.Pattern.compile("(\\d+)");
            java.util.regex.Matcher priceMatcher = pricePattern.matcher(priceText);
            if (priceMatcher.find()) {
                price = Integer.parseInt(priceMatcher.group(1));
            }
        }
        
        return ContentBlock.createPaidPostInfoBlock(price);
    }
    
    /**
     * 检查元素是否是附件区域
     * 
     * HTML结构有两种情况:
     * 
     * 情况1 (付费帖子): 外层有 comiis_attach_box 包裹
     * <ul class="comiis_attach_box cl">
     *   <div class="comiis_attach bg_e b_ok cl">
     *     <a href="forum.php?mod=attachment&aid=xxx">
     *       <i class="comiis_font f_ok">&#xe649;</i>
     *       <p class="attach_tit">
     *         <img src="static/image/filetype/text.gif" />
     *         <span class="f_ok">牛精灵分析报告.txt</span>
     *         <em class="f_d">5 小时前上传</em>
     *       </p>
     *       <p class="attach_size f_c">21.88 KB , 下载次数: 43, 下载积分: 金币 -1</p>
     *     </a>
     *   </div>
     * </ul>
     * 
     * 情况2 (普通帖子): 直接是 comiis_attach，没有外层包裹
     * <div class="comiis_attach bg_e b_ok cl">
     *   <a href="forum.php?mod=attachment&aid=xxx">
     *     <p class="attach_tit">...</p>
     *     <p class="attach_size f_c">...</p>
     *   </a>
     * </div>
     * 
     * @return 如果是附件区域，返回对应的 ContentBlock 列表；否则返回 null
     */
    private static List<ContentBlock> checkAttachment(Element element) {
        String className = element.className();
        
        // 情况1: 检测外层附件容器 comiis_attach_box
        if (className.contains("comiis_attach_box")) {
            List<ContentBlock> blocks = new ArrayList<>();
            Elements attachDivs = element.select("div.comiis_attach");
            
            for (Element attachDiv : attachDivs) {
                ContentBlock.Attachment attachment = parseAttachment(attachDiv);
                if (attachment != null) {
                    blocks.add(ContentBlock.createAttachmentBlock(attachment));
                }
            }
            
            return blocks.isEmpty() ? null : blocks;
        }
        
        // 情况2: 直接检测 comiis_attach (普通帖子)
        if (className.contains("comiis_attach") && !className.contains("comiis_attach_box")) {
            ContentBlock.Attachment attachment = parseAttachment(element);
            if (attachment != null) {
                List<ContentBlock> blocks = new ArrayList<>();
                blocks.add(ContentBlock.createAttachmentBlock(attachment));
                return blocks;
            }
        }
        
        return null;
    }
    
    /**
     * 解析单个附件
     */
    private static ContentBlock.Attachment parseAttachment(Element attachDiv) {
        ContentBlock.Attachment attachment = new ContentBlock.Attachment();
        
        // 获取下载链接
        Element linkElem = attachDiv.selectFirst("a[href*='mod=attachment']");
        if (linkElem != null) {
            String href = linkElem.attr("href");
            if (href.startsWith("//")) {
                href = "https:" + href;
            } else if (href.startsWith("/")) {
                href = "https://bbs.binmt.cc" + href;
            }
            attachment.setDownloadUrl(href);
        }
        
        // 获取文件名
        Element fileNameElem = attachDiv.selectFirst("p.attach_tit span.f_ok");
        if (fileNameElem != null) {
            attachment.setFileName(fileNameElem.text().trim());
        }
        
        // 获取上传时间
        Element uploadTimeElem = attachDiv.selectFirst("p.attach_tit em.f_d");
        if (uploadTimeElem != null) {
            attachment.setUploadTime(uploadTimeElem.text().trim());
        }
        
        // 获取文件大小和下载信息
        // 格式: "21.88 KB , 下载次数: 43, 下载积分: 金币 -1"
        Element sizeElem = attachDiv.selectFirst("p.attach_size");
        if (sizeElem != null) {
            String sizeText = sizeElem.text();
            
            // 解析文件大小
            java.util.regex.Pattern sizePattern = java.util.regex.Pattern.compile("([\\d.]+\\s*[KMGT]?B)");
            java.util.regex.Matcher sizeMatcher = sizePattern.matcher(sizeText);
            if (sizeMatcher.find()) {
                attachment.setFileSize(sizeMatcher.group(1));
            }
            
            // 解析下载次数
            java.util.regex.Pattern countPattern = java.util.regex.Pattern.compile("下载次数:\\s*(\\d+)");
            java.util.regex.Matcher countMatcher = countPattern.matcher(sizeText);
            if (countMatcher.find()) {
                attachment.setDownloadCount(Integer.parseInt(countMatcher.group(1)));
            }
            
            // 解析下载积分
            java.util.regex.Pattern costPattern = java.util.regex.Pattern.compile("下载积分:\\s*金币\\s*(-?\\d+)");
            java.util.regex.Matcher costMatcher = costPattern.matcher(sizeText);
            if (costMatcher.find()) {
                attachment.setDownloadCost(Integer.parseInt(costMatcher.group(1)));
            }
        }
        
        // 获取文件类型图标
        Element iconElem = attachDiv.selectFirst("p.attach_tit img");
        if (iconElem != null) {
            String iconSrc = iconElem.attr("src");
            if (iconSrc.startsWith("//")) {
                iconSrc = "https:" + iconSrc;
            } else if (iconSrc.startsWith("/")) {
                iconSrc = "https://cdn-bbs.mt2.cn" + iconSrc;
            }
            attachment.setFileTypeIcon(iconSrc);
        }
        
        // 至少要有文件名
        return attachment.getFileName() != null ? attachment : null;
    }
    
    /**
     * 解析付费帖子内容块
     * HTML结构:
     * <div class="comiis_quote bg_h">
     *   <i class="comiis_font f_a">&#xe61d</i>&nbsp;<span class="kmren">已有 33 人购买</span>
     *   本主题需向作者支付 <strong>2金币</strong> 才能浏览
     *   本主题购买截止日期为 2026-3-11 15:55，到期后将免费
     *   <div class="cl"><a href="javascript:;">购买主题</a></div>
     * </div>
     */
    private static ContentBlock parsePaidPostBlock(Element element) {
        String divText = element.text();
        
        // 解析购买人数: "已有 33 人购买"
        int buyers = 0;
        Element buyersElem = element.selectFirst("span.kmren");
        if (buyersElem != null) {
            String buyersText = buyersElem.text();
            java.util.regex.Pattern buyersPattern = java.util.regex.Pattern.compile("(\\d+)");
            java.util.regex.Matcher buyersMatcher = buyersPattern.matcher(buyersText);
            if (buyersMatcher.find()) {
                buyers = Integer.parseInt(buyersMatcher.group(1));
            }
        }
        
        // 解析价格: "本主题需向作者支付 2金币 才能浏览"
        int price = 0;
        Element priceElem = element.selectFirst("strong");
        if (priceElem != null) {
            String priceText = priceElem.text();
            java.util.regex.Pattern pricePattern = java.util.regex.Pattern.compile("(\\d+)");
            java.util.regex.Matcher priceMatcher = pricePattern.matcher(priceText);
            if (priceMatcher.find()) {
                price = Integer.parseInt(priceMatcher.group(1));
            }
        }
        
        // 解析截止日期: "本主题购买截止日期为 2026-3-11 15:55，到期后将免费"
        String deadline = null;
        java.util.regex.Pattern deadlinePattern = java.util.regex.Pattern.compile(
            "截止日期为\\s*(\\d{4}-\\d{1,2}-\\d{1,2}\\s*\\d{1,2}:\\d{1,2})"
        );
        java.util.regex.Matcher deadlineMatcher = deadlinePattern.matcher(divText);
        if (deadlineMatcher.find()) {
            deadline = deadlineMatcher.group(1);
        }
        
        // 检查是否已购买（未购买则显示购买按钮）
        Element buyBtn = element.selectFirst("a:contains(购买主题)");
        boolean hasPurchased = (buyBtn == null);
        
        return ContentBlock.createPaidPostBlock(price, buyers, deadline, hasPurchased);
    }
    
    /**
     * 检查元素是否是悬赏内容区域
     * 格式: <div class="rwd cl"><div class="rusld z"><cite>5</cite>金币</div>...</div>
     * @return 如果是悬赏内容，返回对应的 ContentBlock；否则返回 null
     */
    private static ContentBlock checkBountyContent(Element element) {
        String className = element.className();
        
        // 检测悬赏区域
        // PC端格式: <div class="rwd cl">
        // 移动端格式: <div class="comiis_xstop bg_h cl">
        
        // PC端检测
        boolean isRwdContainer = className.equals("rwd cl") || 
                                 className.equals("rwd") ||
                                 (className.contains("rwd ") && className.contains("cl"));
        
        // 移动端检测
        boolean isMobileBounty = className.contains("comiis_xstop");
        
        if (isRwdContainer) {
            // PC端悬赏格式
            Element citeElem = element.selectFirst("div.rusld cite");
            if (citeElem != null) {
                String amountText = citeElem.text().trim();
                try {
                    int bountyAmount = Integer.parseInt(amountText);
                    Element contentElem = element.selectFirst("div.rwdn td.t_f");
                    String content = contentElem != null ? contentElem.text().trim() : "";
                    return ContentBlock.createBountyBlock(content, bountyAmount);
                } catch (NumberFormatException ignored) {
                }
            }
        } else if (isMobileBounty) {
            // 移动端悬赏格式
            Element emElem = element.selectFirst("div.f_a em");
            if (emElem != null) {
                String amountText = emElem.text().trim();
                try {
                    int bountyAmount = Integer.parseInt(amountText);
                    // 获取提示文字
                    Element hintElem = element.selectFirst("div.comiis_xsbtn span");
                    String hint = hintElem != null ? hintElem.text().trim() : "";
                    return ContentBlock.createBountyBlock(hint, bountyAmount);
                } catch (NumberFormatException ignored) {
                }
            }
        }
        
        return null;
    }
    
    /**
     * 合并相邻的文本块和表情块为富文本块
     * 表情图片会显示在文本的正确位置
     */
    private static List<ContentBlock> mergeTextAndSmileys(List<ContentBlock> blocks) {
        List<ContentBlock> merged = new ArrayList<>();
        StringBuilder htmlBuilder = new StringBuilder();
        boolean hasSmiley = false;
        
        for (int i = 0; i < blocks.size(); i++) {
            ContentBlock block = blocks.get(i);
            
            if (block.isText() || block.isSmiley()) {
                // 收集文本和表情块
                if (block.isText()) {
                    // 转义HTML特殊字符并保留换行
                    String text = block.getContent();
                    if (text != null) {
                        if (text.isEmpty()) {
                            htmlBuilder.append("<br/>");
                        } else {
                            htmlBuilder.append(android.text.TextUtils.htmlEncode(text));
                        }
                    }
                } else if (block.isSmiley()) {
                    // 表情转为img标签
                    hasSmiley = true;
                    htmlBuilder.append("<img src=\"").append(block.getImageUrl()).append("\"/>");
                }
            } else {
                // 遇到非文本/表情块，先保存累积的富文本
                if (htmlBuilder.length() > 0) {
                    if (hasSmiley) {
                        merged.add(ContentBlock.createRichTextBlock(htmlBuilder.toString()));
                    } else {
                        // 纯文本块
                        merged.add(ContentBlock.createTextBlock(
                                android.text.Html.fromHtml(htmlBuilder.toString()).toString()));
                    }
                    htmlBuilder = new StringBuilder();
                    hasSmiley = false;
                }
                // 添加当前块
                merged.add(block);
            }
        }
        
        // 保存最后累积的内容
        if (htmlBuilder.length() > 0) {
            if (hasSmiley) {
                merged.add(ContentBlock.createRichTextBlock(htmlBuilder.toString()));
            } else {
                merged.add(ContentBlock.createTextBlock(
                        android.text.Html.fromHtml(htmlBuilder.toString()).toString()));
            }
        }
        
        return merged;
    }

    /**
     * 合并相邻的文本块
     */
    private static List<ContentBlock> mergeTextBlocks(List<ContentBlock> blocks) {
        List<ContentBlock> merged = new ArrayList<>();
        
        for (int i = 0; i < blocks.size(); i++) {
            ContentBlock block = blocks.get(i);
            
            // 如果是彩色文本块，尝试合并相邻的彩色文本
            if (block.isColoredText()) {
                // 收集相邻的彩色文本块
                List<ContentBlock> coloredBlocks = new ArrayList<>();
                coloredBlocks.add(block);
                
                // 向后查找相邻的彩色文本块
                int j = i + 1;
                while (j < blocks.size() && blocks.get(j).isColoredText()) {
                    coloredBlocks.add(blocks.get(j));
                    j++;
                }
                
                // 合并为一个彩色文本块
                if (coloredBlocks.size() > 1) {
                    ContentBlock mergedBlock = mergeColoredTextBlocks(coloredBlocks);
                    merged.add(mergedBlock);
                    i = j - 1; // 跳过已合并的块
                } else {
                    merged.add(block);
                }
            } else if (block.isText()) {
                // 普通文本，继续原有的合并逻辑
                String text = block.getContent();
                if (TextUtils.isEmpty(text)) {
                    merged.add(ContentBlock.createTextBlock(""));
                } else {
                    merged.add(block);
                }
            } else {
                // 非文本块，直接添加
                merged.add(block);
            }
        }
        
        // 进一步合并相邻的普通文本块
        return mergeAdjacentTextBlocks(merged);
    }
    
    /**
     * 合并多个彩色文本块为一个块
     */
    private static ContentBlock mergeColoredTextBlocks(List<ContentBlock> coloredBlocks) {
        StringBuilder fullText = new StringBuilder();
        for (ContentBlock block : coloredBlocks) {
            fullText.append(block.getContent());
        }
        
        // 创建一个新的彩色文本块，包含所有颜色信息
        ContentBlock mergedBlock = new ContentBlock();
        mergedBlock.setType(ContentBlock.TYPE_COLORED_TEXT);
        mergedBlock.setContent(fullText.toString());
        
        // 存储每个字符的颜色信息（用分隔符连接）
        StringBuilder colorsBuilder = new StringBuilder();
        for (int i = 0; i < coloredBlocks.size(); i++) {
            ContentBlock block = coloredBlocks.get(i);
            int color = block.getTextColor();
            String colorHex = String.format("#%06X", (0xFFFFFF & color));
            int textLength = block.getContent().length();
            
            // 为每个字符记录颜色
            for (int j = 0; j < textLength; j++) {
                if (colorsBuilder.length() > 0) {
                    colorsBuilder.append(",");
                }
                colorsBuilder.append(colorHex);
            }
        }
        
        mergedBlock.setTextColor(colorsBuilder.toString());
        return mergedBlock;
    }
    
    /**
     * 合并相邻的普通文本块
     */
    private static List<ContentBlock> mergeAdjacentTextBlocks(List<ContentBlock> blocks) {
        List<ContentBlock> merged = new ArrayList<>();
        StringBuilder textBuilder = new StringBuilder();
        
        for (ContentBlock block : blocks) {
            if (block.isText() && !block.isColoredText()) {
                String text = block.getContent();
                if (TextUtils.isEmpty(text)) {
                    if (textBuilder.length() > 0) {
                        merged.add(ContentBlock.createTextBlock(textBuilder.toString().trim()));
                        textBuilder = new StringBuilder();
                    }
                    merged.add(ContentBlock.createTextBlock(""));
                } else {
                    if (textBuilder.length() > 0) {
                        textBuilder.append("\n");
                    }
                    textBuilder.append(text);
                }
            } else {
                // 非普通文本块，先保存累积的文本
                if (textBuilder.length() > 0) {
                    merged.add(ContentBlock.createTextBlock(textBuilder.toString().trim()));
                    textBuilder = new StringBuilder();
                }
                merged.add(block);
            }
        }
        
        // 保存最后累积的文本
        if (textBuilder.length() > 0) {
            merged.add(ContentBlock.createTextBlock(textBuilder.toString().trim()));
        }
        
        // 移除开头和结尾的空文本块
        while (!merged.isEmpty() && merged.get(0).isText() && TextUtils.isEmpty(merged.get(0).getContent())) {
            merged.remove(0);
        }
        while (!merged.isEmpty() && merged.get(merged.size() - 1).isText() && TextUtils.isEmpty(merged.get(merged.size() - 1).getContent())) {
            merged.remove(merged.size() - 1);
        }
        
        return merged;
    }

    /**
     * 从HTML中提取所有图片URL
     */
    public static List<String> extractImageUrls(String html) {
        List<String> urls = new ArrayList<>();
        if (html == null) return urls;
        
        try {
            Document doc = Jsoup.parse(html);
            Elements imgs = doc.select("img");
            for (Element img : imgs) {
                String src = img.attr("src");
                if (!TextUtils.isEmpty(src) && !src.contains("avatar.php") && !src.contains("smiley")) {
                    urls.add(src);
                }
            }
        } catch (Exception e) {
            // ignore
        }
        
        return urls;
    }
    
    /**
     * 解析带样式元素 (strong, b, i, em, u, strike, s)
     * 将带样式的HTML转换为富文本块
     */
    private static void parseStyledElement(Element element, List<ContentBlock> blocks, String tagName) {
        // 获取元素的完整HTML（保留样式标签）
        String html = element.html();
        String text = element.text().trim();
        
        if (TextUtils.isEmpty(text)) {
            return;
        }
        
        // 检查是否包含子元素（嵌套样式）
        if (hasNestedStyles(element)) {
            // 有嵌套样式，生成富文本块
            String outerHtml = element.outerHtml();
            blocks.add(ContentBlock.createStyledTextBlock(text, outerHtml));
        } else {
            // 简单样式，创建带样式标记的文本块
            String styleInfo = getStyleInfo(tagName);
            blocks.add(ContentBlock.createStyledTextBlock(text, element.outerHtml()));
        }
    }
    
    /**
     * 解析font元素
     * 支持: color属性、style背景色、size大小、face字体
     */
    private static void parseFontElement(Element element, List<ContentBlock> blocks) {
        String text = element.text().trim();
        if (TextUtils.isEmpty(text)) {
            return;
        }
        
        String color = element.attr("color");
        String style = element.attr("style");
        String size = element.attr("size");
        String face = element.attr("face");
        
        // 检查是否有任何样式属性
        boolean hasStyle = !TextUtils.isEmpty(color) || 
                          !TextUtils.isEmpty(style) || 
                          !TextUtils.isEmpty(size) || 
                          !TextUtils.isEmpty(face);
        
        if (hasStyle) {
            // 有样式，创建带样式的富文本块
            // 保留原始HTML以便在显示时应用样式
            String outerHtml = element.outerHtml();
            blocks.add(ContentBlock.createStyledTextBlock(text, outerHtml));
        } else {
            // 无样式，递归解析子节点
            parseNode(element, blocks);
        }
    }
    
    /**
     * 检查元素是否包含嵌套样式
     */
    private static boolean hasNestedStyles(Element element) {
        Elements styledElements = element.select("strong, b, i, em, u, strike, s, font[color], font[style], font[size]");
        return !styledElements.isEmpty();
    }
    
    /**
     * 获取样式信息字符串
     */
    private static String getStyleInfo(String tagName) {
        switch (tagName.toLowerCase()) {
            case "strong":
            case "b":
                return "bold";
            case "i":
            case "em":
                return "italic";
            case "u":
                return "underline";
            case "strike":
            case "s":
                return "strikethrough";
            default:
                return "";
        }
    }
}
