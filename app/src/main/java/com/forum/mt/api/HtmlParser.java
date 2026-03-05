package com.forum.mt.api;

import com.forum.mt.model.Comment;
import com.forum.mt.model.Forum;
import com.forum.mt.model.ForumInfo;
import com.forum.mt.model.Notification;
import com.forum.mt.model.Post;
import com.forum.mt.model.PrivateMessage;
import com.forum.mt.model.User;

import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * HTML解析器 - 解析论坛页面
 */
public class HtmlParser {
    
    /**
     * 清理HTML实体，将&nbsp;等转换为普通字符
     */
    private static String cleanHtmlEntities(String text) {
        if (text == null || text.isEmpty()) {
            return text;
        }
        // 替换常见HTML实体
        return text
            .replace("\u00A0", " ")  // Jsoup将&nbsp;转为\u00A0(不换行空格)
            .replace("&nbsp;", " ")  // 标准HTML实体
            .replace("&nbsp", " ")   // 不标准写法(无分号)
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replaceAll("[\\uE000-\\uF8FF]", "")  // 过滤私有区域字符（字体图标）
            .replaceAll("\\s+", " ")  // 多个空格合并为一个
            .trim();
    }
    
    /**
     * 解析帖子列表页面
     * 支持多种格式：comiis移动端格式、我的帖子页面、PC端格式
     */
    public static List<Post> parsePostList(String html) {
        List<Post> posts = new ArrayList<>();
        
        if (html == null || html.isEmpty()) {
            return posts;
        }
        
        Document doc = Jsoup.parse(html);
        
        // 优先尝试comiis移动端格式 (li.forumlist_li.comiis_znalist)
        // 这种格式用于首页、热门帖子、我的帖子等页面
        Elements postElements = doc.select("li.forumlist_li.comiis_znalist");
        
        if (!postElements.isEmpty()) {
            for (Element postElem : postElements) {
                Post post = new Post();
                
                // 解析帖子链接和ID
                Element titleLink = postElem.selectFirst("div.mmlist_li_box h2 a");
                if (titleLink != null) {
                    String href = titleLink.attr("href");
                    String tid = extractTid(href);
                    if (tid != null) {
                        try {
                            post.setTid(Integer.parseInt(tid));
                        } catch (NumberFormatException ignored) {}
                    }
                    
                    // 解析悬赏标签 (在标题前面的span)
                    // 格式: <span class="bg_0 f_f">悬赏</span><span class="bg_a f_f">5金币</span>
                    Elements bountySpans = titleLink.select("span.bg_0, span.bg_a");
                    if (!bountySpans.isEmpty()) {
                        for (Element span : bountySpans) {
                            String spanText = cleanHtmlEntities(span.text());
                            if (spanText.contains("悬赏") || spanText.contains("精华")) {
                                post.setBountyType(spanText);
                            } else if (spanText.contains("金币")) {
                                post.setBountyText(spanText);
                                // 提取金币数量
                                String numStr = spanText.replaceAll("[^0-9]", "");
                                if (!numStr.isEmpty()) {
                                    try {
                                        post.setBountyAmount(Integer.parseInt(numStr));
                                    } catch (NumberFormatException ignored) {}
                                }
                            }
                        }
                        // 移除悬赏标签后获取真实标题
                        String fullText = cleanHtmlEntities(titleLink.text());
                        // 标题在最后，去掉前面的悬赏文本
                        for (Element span : bountySpans) {
                            fullText = fullText.replace(cleanHtmlEntities(span.text()), "");
                        }
                        post.setTitle(fullText.trim());
                    } else {
                        post.setTitle(cleanHtmlEntities(titleLink.text()));
                    }
                }
                
                // 解析摘要
                Element summaryElem = postElem.selectFirst("div.list_body a");
                if (summaryElem != null) {
                    post.setSummary(cleanHtmlEntities(summaryElem.text()));
                }
                
                // 解析作者信息
                Element authorLink = postElem.selectFirst("a.top_user");
                if (authorLink != null) {
                    post.setAuthor(cleanHtmlEntities(authorLink.text()));
                }
                
                // 解析作者头像
                Element avatarImg = postElem.selectFirst("img.top_tximg, img[src*='avatar.php']");
                if (avatarImg != null) {
                    String avatarUrl = avatarImg.attr("src");
                    if (avatarUrl.startsWith("uc_server/avatar.php")) {
                        avatarUrl = "https://bbs.binmt.cc/" + avatarUrl;
                    }
                    post.setAuthorAvatar(avatarUrl);
                    
                    // 从头像URL提取作者ID
                    String uid = extractUid(avatarUrl);
                    if (uid != null) {
                        try {
                            post.setAuthorId(Integer.parseInt(uid));
                        } catch (NumberFormatException ignored) {}
                    }
                }
                
                // 解析作者等级
                Element levelElem = postElem.selectFirst("span.top_lev");
                if (levelElem != null) {
                    String level = cleanHtmlEntities(levelElem.text()); // 如 "Lv.4"
                    post.setAuthorLevel(level);
                }
                
                // 解析作者性别
                Element genderElem = postElem.selectFirst("i.top_gender");
                if (genderElem != null) {
                    String genderClass = genderElem.className();
                    if (genderClass.contains("bg_boy")) {
                        post.setAuthorGender(1); // 男
                    } else if (genderClass.contains("bg_girl")) {
                        post.setAuthorGender(2); // 女
                    }
                }
                
                // 解析时间
                Element timeElem = postElem.selectFirst("div.forumlist_li_time span");
                if (timeElem != null) {
                    post.setDateStr(cleanHtmlEntities(timeElem.text()));
                }
                
                // 解析版块名称
                Element forumLink = postElem.selectFirst("div.comiis_xznalist_bk a");
                if (forumLink != null) {
                    String forumName = cleanHtmlEntities(forumLink.text());
                    post.setForumName(forumName);
                    
                    // 从链接提取版块ID
                    String fid = extractFid(forumLink.attr("href"));
                    if (fid != null) {
                        try {
                            post.setForumId(Integer.parseInt(fid));
                        } catch (NumberFormatException ignored) {}
                    }
                }
                
                // 解析统计信息 (点赞、评论、浏览)
                // 结构: <li id="comiis_listzhan_xxx">点赞数</li><li>评论数</li><li>浏览数</li>
                Elements statElems = postElem.select("div.comiis_xznalist_bottom ul li");
                int statIndex = 0;
                for (Element statElem : statElems) {
                    // 点赞数：li有id属性 "comiis_listzhan_xxx"，或者第一个li
                    String liId = statElem.attr("id");
                    if (liId != null && liId.startsWith("comiis_listzhan_")) {
                        Element numSpan = statElem.selectFirst("span.comiis_tm");
                        if (numSpan != null) {
                            String numText = numSpan.text().trim();
                            try {
                                post.setLikes(Integer.parseInt(numText));
                            } catch (NumberFormatException ignored) {}
                        }
                    } else if (statIndex == 0) {
                        Element numSpan = statElem.selectFirst("span.comiis_tm");
                        if (numSpan != null) {
                            String numText = numSpan.text().trim();
                            try {
                                post.setLikes(Integer.parseInt(numText));
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                    
                    // 评论数：包含thread链接
                    if (statElem.selectFirst("a[href*='thread-']") != null) {
                        Element numSpan = statElem.selectFirst("span.comiis_tm");
                        if (numSpan != null) {
                            String numText = numSpan.text().trim();
                            try {
                                post.setReplies(Integer.parseInt(numText));
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                    
                    // 浏览数：没有链接的li
                    if (statElem.selectFirst("a") == null && !statElem.hasAttr("id")) {
                        Element numSpan = statElem.selectFirst("span.comiis_tm");
                        if (numSpan != null) {
                            String numText = numSpan.text().trim();
                            try {
                                post.setViews(Integer.parseInt(numText));
                            } catch (NumberFormatException ignored) {}
                        }
                    }
                    statIndex++;
                }
                
                // 解析缩略图 - 与首页解析逻辑一致
                List<String> thumbnails = new ArrayList<>();
                
                // 主图片区域
                Elements mainImgElems = postElem.select("div.comiis_pyqlist_img img");
                for (Element img : mainImgElems) {
                    String src = img.attr("src");
                    if (!src.isEmpty() && !src.startsWith("http")) {
                        src = "https://bbs.binmt.cc/" + src;
                    }
                    if (!src.isEmpty()) {
                        thumbnails.add(src);
                    }
                }
                
                // 备用图片选择器
                if (thumbnails.isEmpty()) {
                    Elements altImgElems = postElem.select("div.comiis_pyqlist_imgs img");
                    for (Element img : altImgElems) {
                        String src = img.attr("src");
                        if (!src.isEmpty() && !src.startsWith("http")) {
                            src = "https://bbs.binmt.cc/" + src;
                        }
                        if (!src.isEmpty() && !src.contains("avatar.php") && !src.contains("smiley")) {
                            thumbnails.add(src);
                        }
                    }
                }
                
                // 第三重备选 - OSS和论坛图片
                if (thumbnails.isEmpty()) {
                    Elements ossImgElems = postElem.select("img[src*='oss3-bbs.mt2.cn'], img[src*='forum.php?mod=image']");
                    for (Element img : ossImgElems) {
                        String src = img.attr("src");
                        if (!src.isEmpty() && !src.startsWith("http")) {
                            src = "https://bbs.binmt.cc/" + src;
                        }
                        if (!src.isEmpty() && !src.contains("avatar.php") && !src.contains("smiley")) {
                            thumbnails.add(src);
                        }
                    }
                }
                
                if (!thumbnails.isEmpty()) {
                    post.setThumbnails(thumbnails);
                    post.setThumbnail(thumbnails.get(0));
                }
                
                if (post.getTid() > 0 && post.getTitle() != null) {
                    posts.add(post);
                }
            }
            
            // 如果成功解析到帖子，直接返回
            if (!posts.isEmpty()) {
                return posts;
            }
        }
        
        // 回退到PC端tbody格式解析
        Elements threadElements = doc.select("tbody[id^=normalthread_]");
        
        for (Element thread : threadElements) {
            Post post = new Post();
            
            // 帖子ID
            String id = thread.id().replace("normalthread_", "");
            try {
                post.setTid(Integer.parseInt(id));
            } catch (NumberFormatException e) {
                continue;
            }
            
            // 标题
            Element titleLink = thread.selectFirst("a.s.xst, a.xst");
            if (titleLink != null) {
                post.setTitle(cleanHtmlEntities(titleLink.text()));
            }
            
            // 作者
            Element authorElem = thread.selectFirst("td.by a[href*='uid='], a.author");
            if (authorElem != null) {
                post.setAuthor(cleanHtmlEntities(authorElem.text()));
                String uidMatch = extractUid(authorElem.attr("href"));
                if (uidMatch != null) {
                    post.setAuthorId(Integer.parseInt(uidMatch));
                }
            }
            
            // 回复数和查看数
            Elements nums = thread.select("td.num em, td.num span");
            if (nums.size() >= 2) {
                try {
                    post.setReplies(Integer.parseInt(nums.get(0).text().trim()));
                    post.setViews(Integer.parseInt(nums.get(1).text().trim()));
                } catch (NumberFormatException ignored) {}
            }
            
            // 版块
            Element forumElem = thread.selectFirst("a[href*='forumdisplay']");
            if (forumElem != null) {
                post.setForumName(cleanHtmlEntities(forumElem.text()));
            }
            
            // 最后回复时间
            Element lastPost = thread.selectFirst("td.by:last-child em, td.lastpost em");
            if (lastPost != null) {
                post.setLastPost(cleanHtmlEntities(lastPost.text()));
            }
            
            // 缩略图 (如果有)
            Element img = thread.selectFirst("img.zoom, img[src*='attachment']");
            if (img != null) {
                post.setThumbnail(img.attr("src"));
            }
            
            posts.add(post);
        }
        
        return posts;
    }
    
    /**
     * 解析版块帖子列表页面
     * 接口: forum-{fid}-{page}.html 或 forum.php?mod=forumdisplay&fid={fid}
     * 返回ForumInfo包含版块信息和帖子列表
     * 
     * HTML结构:
     * - 版块信息: div.comiis_forumlist_heads
     * - 筛选标签: #forumlist_time_li ul li
     * - 置顶帖子: div.comiis_forumlist_top ul li
     * - 普通帖子: div.comiis_forumlist ul li.forumlist_li
     * - 分页: div.pg
     */
    public static ForumInfo parseForumPostList(String html) {
        ForumInfo forumInfo = new ForumInfo();
        List<Post> posts = new ArrayList<>();
        
        if (html == null || html.isEmpty()) {
            return forumInfo;
        }
        
        Document doc = Jsoup.parse(html);
        
        // === 1. 解析版块信息 ===
        Element headerElem = doc.selectFirst("div.comiis_forumlist_heads");
        if (headerElem != null) {
            // 版块图标
            Element iconElem = headerElem.selectFirst("div.top_ico img");
            if (iconElem != null) {
                String iconSrc = iconElem.attr("src");
                if (!iconSrc.startsWith("http")) {
                    iconSrc = "https://bbs.binmt.cc/" + iconSrc;
                }
                forumInfo.setIcon(iconSrc);
            }
            
            // 版块名称
            Element nameElem = headerElem.selectFirst("h2.f_f");
            if (nameElem != null) {
                forumInfo.setName(cleanHtmlEntities(nameElem.text()));
            }
            
            // 统计信息 - 格式: "今日 33   帖子 2085600   关注 5257"
            Element statsElem = headerElem.selectFirst("p.comiis_tm8");
            if (statsElem != null) {
                String statsText = statsElem.text();
                // 解析今日发帖数
                Pattern todayPattern = Pattern.compile("今日\\s*(\\d+)");
                Matcher todayMatcher = todayPattern.matcher(statsText);
                if (todayMatcher.find()) {
                    forumInfo.setTodayPosts(Integer.parseInt(todayMatcher.group(1)));
                }
                // 解析总帖子数
                Pattern postsPattern = Pattern.compile("帖子\\s*(\\d+)");
                Matcher postsMatcher = postsPattern.matcher(statsText);
                if (postsMatcher.find()) {
                    forumInfo.setTotalPosts(Integer.parseInt(postsMatcher.group(1)));
                }
                // 解析关注数
                Pattern followPattern = Pattern.compile("关注\\s*(\\d+)");
                Matcher followMatcher = followPattern.matcher(statsText);
                if (followMatcher.find()) {
                    forumInfo.setFollowers(Integer.parseInt(followMatcher.group(1)));
                }
            }
            
            // 版块描述
            Element descElem = headerElem.selectFirst("p.comiis_tm8:last-child");
            if (descElem != null && !descElem.text().contains("今日")) {
                forumInfo.setDescription(cleanHtmlEntities(descElem.text()));
            }
        }

        // === 2. 解析分类标签 ===
        Element tabsContainer = doc.selectFirst("#forumlist_time_li");
        if (tabsContainer != null) {
            List<ForumInfo.ForumTab> tabs = new ArrayList<>();
            Elements tabElems = tabsContainer.select("ul li a");
            for (Element tabElem : tabElems) {
                String tabName = tabElem.text();
                String href = tabElem.attr("href");
                String filter = extractFilterFromHref(href);

                ForumInfo.ForumTab tab = new ForumInfo.ForumTab(tabName, filter);
                // 检查是否选中（有 class="a" 或父级 li 有 class="a"）
                if (tabElem.hasClass("a") || tabElem.parent() != null && tabElem.parent().hasClass("a")) {
                    tab.setSelected(true);
                }
                tabs.add(tab);
            }
            forumInfo.setTabs(tabs);
        } else {
            // 如果没有解析到分类标签，使用默认分类
            List<ForumInfo.ForumTab> defaultTabs = new ArrayList<>();
            defaultTabs.add(new ForumInfo.ForumTab("全部", "all"));
            defaultTabs.add(new ForumInfo.ForumTab("最新", "lastpost"));
            defaultTabs.add(new ForumInfo.ForumTab("热门", "heat"));
            defaultTabs.add(new ForumInfo.ForumTab("精华", "digest"));
            defaultTabs.get(0).setSelected(true);
            forumInfo.setTabs(defaultTabs);
        }

        // === 3. 解析置顶帖子 ===
        Element titleElem = doc.selectFirst("title");
        if (titleElem != null) {
            String title = titleElem.text();
            // 格式: "逆向交流 今日: 294 /主题: 11680/排名: 2"
            Pattern rankPattern = Pattern.compile("排名[:\\s]*(\\d+)");
            Matcher rankMatcher = rankPattern.matcher(title);
            if (rankMatcher.find()) {
                forumInfo.setRank(Integer.parseInt(rankMatcher.group(1)));
            }
        }
        
        // === 2. 解析置顶帖子 ===
        Element topPostsContainer = doc.selectFirst("div.comiis_forumlist_top ul");
        if (topPostsContainer != null) {
            Elements topPostElems = topPostsContainer.select("li.b_t");
            for (Element topPostElem : topPostElems) {
                Post post = new Post();
                post.setTop(true);
                
                Element linkElem = topPostElem.selectFirst("a[href*='thread-']");
                if (linkElem != null) {
                    // 提取tid
                    String href = linkElem.attr("href");
                    String tid = extractTid(href);
                    if (tid != null) {
                        post.setTid(Integer.parseInt(tid));
                    }
                    
                    // 标题 - 直接使用链接文本，去掉置顶标签
                    String title = cleanHtmlEntities(linkElem.text());
                    if (title.startsWith("置顶")) {
                        title = title.substring(2).trim();
                    }
                    post.setTitle(title);
                    
                    // 标题样式可能包含颜色
                    String style = linkElem.attr("style");
                    if (style != null && style.contains("color")) {
                        // 可以提取颜色值用于显示
                    }
                }
                
                if (post.getTid() > 0 && post.getTitle() != null) {
                    posts.add(post);
                }
            }
        }
        
        // === 3. 解析普通帖子列表 ===
        Elements postElements = doc.select("li.forumlist_li.comiis_znalist");
        
        for (Element postElem : postElements) {
            Post post = new Post();
            
            // 解析帖子链接和ID
            Element titleLink = postElem.selectFirst("div.mmlist_li_box h2 a");
            if (titleLink != null) {
                String href = titleLink.attr("href");
                String tid = extractTid(href);
                if (tid != null) {
                    post.setTid(Integer.parseInt(tid));
                }
                
                // 解析热度标签
                // 格式: <span class="bg_a f_f" title="热度: 170"><i class="comiis_font kmhotico">icon</i></span>
                Element hotSpan = titleLink.selectFirst("span[title*='热度']");
                if (hotSpan != null) {
                    post.setHot(true);
                }
                
                // 标题
                post.setTitle(cleanHtmlEntities(titleLink.text()));
            }
            
            // 解析摘要
            Element summaryElem = postElem.selectFirst("div.list_body a");
            if (summaryElem != null) {
                post.setSummary(cleanHtmlEntities(summaryElem.text()));
            }
            
            // 解析作者信息
            Element authorLink = postElem.selectFirst("a.top_user");
            if (authorLink != null) {
                post.setAuthor(cleanHtmlEntities(authorLink.text()));
            }
            
            // 解析作者头像和UID
            Element avatarImg = postElem.selectFirst("img.top_tximg, img[src*='avatar.php']");
            if (avatarImg != null) {
                String avatarUrl = avatarImg.attr("src");
                if (!avatarUrl.startsWith("http")) {
                    avatarUrl = "https://bbs.binmt.cc/" + avatarUrl;
                }
                post.setAuthorAvatar(avatarUrl);
                
                // 从头像URL或作者链接提取UID
                String uid = extractUid(avatarUrl);
                if (uid == null) {
                    // 尝试从作者链接提取
                    Element authorHref = postElem.selectFirst("a.wblist_tximg");
                    if (authorHref != null) {
                        uid = extractUid(authorHref.attr("href"));
                    }
                }
                if (uid != null) {
                    post.setAuthorId(Integer.parseInt(uid));
                }
            }
            
            // 解析作者等级
            Element levelElem = postElem.selectFirst("span.top_lev");
            if (levelElem != null) {
                post.setAuthorLevel(cleanHtmlEntities(levelElem.text()));
            }
            
            // 解析作者性别
            Element genderElem = postElem.selectFirst("i.top_gender");
            if (genderElem != null) {
                String genderClass = genderElem.className();
                if (genderClass.contains("bg_boy")) {
                    post.setAuthorGender(1);
                } else if (genderClass.contains("bg_girl")) {
                    post.setAuthorGender(2);
                }
            }
            
            // 解析时间
            Element timeElem = postElem.selectFirst("div.forumlist_li_time span");
            if (timeElem != null) {
                post.setDateStr(cleanHtmlEntities(timeElem.text()));
            }
            
            // 解析统计信息 (点赞、回复、浏览)
            Elements statElems = postElem.select("div.comiis_xznalist_bottom li");
            int statIndex = 0;
            for (Element statElem : statElems) {
                String statText = statElem.text();
                int num = extractNumber(statText);
                if (statIndex == 0) {
                    // 第一个是点赞数
                    post.setLikes(num);
                } else if (statIndex == 1) {
                    // 第二个是回复数
                    post.setReplies(num);
                } else if (statIndex == 2) {
                    // 第三个是浏览数
                    post.setViews(num);
                }
                statIndex++;
            }
            
            // 解析缩略图 - 与首页解析逻辑一致
            List<String> thumbnails = new ArrayList<>();
            
            // 主图片区域
            Elements imgElems = postElem.select("div.comiis_pyqlist_img img");
            for (Element img : imgElems) {
                String src = img.attr("src");
                if (!src.isEmpty() && !src.startsWith("http")) {
                    src = "https://bbs.binmt.cc/" + src;
                }
                if (!src.isEmpty()) {
                    thumbnails.add(src);
                }
            }
            
            // 备用图片选择器
            if (thumbnails.isEmpty()) {
                Elements altImgElems = postElem.select("div.comiis_pyqlist_imgs img");
                for (Element altImg : altImgElems) {
                    String src = altImg.attr("src");
                    if (!src.isEmpty() && !src.startsWith("http")) {
                        src = "https://bbs.binmt.cc/" + src;
                    }
                    if (!src.isEmpty() && !src.contains("avatar.php") && !src.contains("smiley")) {
                        thumbnails.add(src);
                    }
                }
            }
            
            // 第三重备选
            if (thumbnails.isEmpty()) {
                Elements altImgElems = postElem.select("img[src*='oss3-bbs.mt2.cn'], img[src*='forum'], img[src*='image']");
                for (Element altImg : altImgElems) {
                    String src = altImg.attr("src");
                    if (!src.isEmpty() && !src.startsWith("http")) {
                        src = "https://bbs.binmt.cc/" + src;
                    }
                    if (!src.isEmpty() && !src.contains("avatar.php") && !src.contains("smiley")) {
                        thumbnails.add(src);
                    }
                }
            }
            
            if (!thumbnails.isEmpty()) {
                post.setThumbnails(thumbnails);
                post.setThumbnail(thumbnails.get(0));
            }
            
            if (post.getTid() > 0 && post.getTitle() != null) {
                posts.add(post);
            }
        }
        
        // === 4. 解析分页信息 ===
        Element pageElem = doc.selectFirst("div.pg");
        if (pageElem != null) {
            // 当前页
            Element currentPageElem = pageElem.selectFirst("strong");
            if (currentPageElem != null) {
                try {
                    forumInfo.setCurrentPage(Integer.parseInt(currentPageElem.text().trim()));
                } catch (NumberFormatException ignored) {}
            }
            
            // 总页数 - 从 span[title*="共"] 提取
            Element totalPagesElem = pageElem.selectFirst("span[title*='共']");
            if (totalPagesElem != null) {
                String title = totalPagesElem.attr("title");
                Pattern pagePattern = Pattern.compile("共\\s*(\\d+)\\s*页");
                Matcher pageMatcher = pagePattern.matcher(title);
                if (pageMatcher.find()) {
                    forumInfo.setTotalPages(Integer.parseInt(pageMatcher.group(1)));
                }
            }
            
            // 备用：从.last链接提取最大页数
            if (forumInfo.getTotalPages() == 0) {
                Element lastPageElem = pageElem.selectFirst("a.last");
                if (lastPageElem != null) {
                    String href = lastPageElem.attr("href");
                    // 格式: forum-41-99.html
                    Pattern fidPagePattern = Pattern.compile("forum-(\\d+)-(\\d+)");
                    Matcher fidPageMatcher = fidPagePattern.matcher(href);
                    if (fidPageMatcher.find()) {
                        forumInfo.setFid(Integer.parseInt(fidPageMatcher.group(1)));
                        forumInfo.setTotalPages(Integer.parseInt(fidPageMatcher.group(2)));
                    }
                }
            }
        }
        
        // 默认页码
        if (forumInfo.getCurrentPage() == 0) {
            forumInfo.setCurrentPage(1);
        }
        
        // 设置帖子列表
        forumInfo.setTopPosts(new ArrayList<>());
        for (Post post : posts) {
            if (post.isTop()) {
                forumInfo.getTopPosts().add(post);
            }
        }
        
        // 将所有帖子（包括置顶）返回
        forumInfo.setFollowers(forumInfo.getFollowers()); // 保留关注数
        
        return forumInfo;
    }
    
    /**
     * 仅获取版块帖子列表（不包括置顶）
     */
    public static List<Post> parseForumPosts(String html) {
        ForumInfo forumInfo = parseForumPostList(html);
        List<Post> posts = new ArrayList<>();
        for (Post post : forumInfo.getTopPosts()) {
            posts.add(post); // 先添加置顶帖
        }
        // 添加普通帖子需要从返回的html重新解析
        // 这里简化处理，直接使用parsePostList
        return parsePostList(html);
    }
    
    /**
     * 解析首页AJAX内容 (comiis_app_portal插件格式)
     * 接口: plugin.php?id=comiis_app_portal&pid=1&istab=yes&inajax=1
     * 注意: 返回的是XML格式，内容被CDATA包裹
     */
    public static List<Post> parseHomePage(String html) {
        List<Post> posts = new ArrayList<>();
        
        if (html == null || html.isEmpty()) {
            return posts;
        }
        
        // 提取CDATA内的HTML内容
        // 格式: <?xml version="1.0" encoding="utf-8"?><root><![CDATA[...]]></root>
        String content = html;
        
        // 尝试提取CDATA内容
        Pattern cdataPattern = Pattern.compile("<\\!\\[CDATA\\[(.*?)\\]\\]>", Pattern.DOTALL);
        Matcher cdataMatcher = cdataPattern.matcher(html);
        if (cdataMatcher.find()) {
            content = cdataMatcher.group(1);
        }
        
        Document doc = Jsoup.parse(content);
        Elements postElements = doc.select("li.forumlist_li.comiis_znalist");
        
        for (Element postElem : postElements) {
            Post post = new Post();
            
            // 解析帖子链接和ID
            Element titleLink = postElem.selectFirst("div.mmlist_li_box h2 a");
            if (titleLink != null) {
                String href = titleLink.attr("href");
                String tid = extractTid(href);
                if (tid != null) {
                    try {
                        post.setTid(Integer.parseInt(tid));
                    } catch (NumberFormatException ignored) {}
                }
                
                // 解析悬赏标签 (在标题前面的span)
                // 格式: <span class="bg_0 f_f">悬赏</span><span class="bg_a f_f">5金币</span>
                Elements bountySpans = titleLink.select("span.bg_0, span.bg_a");
                if (!bountySpans.isEmpty()) {
                    for (Element span : bountySpans) {
                        String spanText = cleanHtmlEntities(span.text());
                        if (spanText.contains("悬赏") || spanText.contains("精华")) {
                            post.setBountyType(spanText);
                        } else if (spanText.contains("金币")) {
                            post.setBountyText(spanText);
                            // 提取金币数量
                            String numStr = spanText.replaceAll("[^0-9]", "");
                            if (!numStr.isEmpty()) {
                                try {
                                    post.setBountyAmount(Integer.parseInt(numStr));
                                } catch (NumberFormatException ignored) {}
                            }
                        }
                    }
                    // 移除悬赏标签后获取真实标题
                    String fullText = cleanHtmlEntities(titleLink.text());
                    for (Element span : bountySpans) {
                        fullText = fullText.replace(cleanHtmlEntities(span.text()), "");
                    }
                    post.setTitle(fullText.trim());
                } else {
                    post.setTitle(cleanHtmlEntities(titleLink.text()));
                }
            }
            
            // 解析摘要
            Element summaryElem = postElem.selectFirst("div.list_body a");
            if (summaryElem != null) {
                post.setSummary(cleanHtmlEntities(summaryElem.text()));
            }
            
            // 解析作者信息
            Element authorLink = postElem.selectFirst("a.top_user");
            if (authorLink != null) {
                post.setAuthor(cleanHtmlEntities(authorLink.text()));
            }
            
            // 解析作者头像
            Element avatarImg = postElem.selectFirst("img.top_tximg");
            if (avatarImg != null) {
                String avatarUrl = avatarImg.attr("src");
                if (avatarUrl.startsWith("uc_server/avatar.php")) {
                    avatarUrl = "https://bbs.binmt.cc/" + avatarUrl;
                }
                post.setAuthorAvatar(avatarUrl);
                
                // 从头像URL提取作者ID
                String uid = extractUid(avatarUrl);
                if (uid != null) {
                    try {
                        post.setAuthorId(Integer.parseInt(uid));
                    } catch (NumberFormatException ignored) {}
                }
            }
            
            // 解析作者等级
            Element levelElem = postElem.selectFirst("span.top_lev");
            if (levelElem != null) {
                String level = cleanHtmlEntities(levelElem.text()); // 如 "Lv.4"
                post.setAuthorLevel(level);
            }
            
            // 解析作者性别
            Element genderElem = postElem.selectFirst("i.top_gender");
            if (genderElem != null) {
                String genderClass = genderElem.className();
                if (genderClass.contains("bg_boy")) {
                    post.setAuthorGender(1); // 男
                } else if (genderClass.contains("bg_girl")) {
                    post.setAuthorGender(2); // 女
                }
            }
            
            // 解析时间
            Element timeElem = postElem.selectFirst("div.forumlist_li_time span");
            if (timeElem != null) {
                post.setDateStr(cleanHtmlEntities(timeElem.text()));
            }
            
            // 解析版块
            Element forumLink = postElem.selectFirst("div.forumlist_li_time a[href*='forum-']");
            if (forumLink != null) {
                String forumName = cleanHtmlEntities(forumLink.text());
                if (forumName.startsWith("来自 ")) {
                    forumName = forumName.substring(3);
                }
                post.setForumName(forumName);
                
                // 从链接提取版块ID
                String fid = extractFid(forumLink.attr("href"));
                if (fid != null) {
                    try {
                        post.setForumId(Integer.parseInt(fid));
                    } catch (NumberFormatException ignored) {}
                }
            }
            
            // 解析所有图片到缩略图列表
            List<String> thumbnailList = new ArrayList<>();
            
            // 主图片区域
            Elements imgElems = postElem.select("div.comiis_pyqlist_img img");
            for (Element imgElem : imgElems) {
                String imgSrc = imgElem.attr("src");
                if (!imgSrc.isEmpty() && !imgSrc.startsWith("http")) {
                    imgSrc = "https://bbs.binmt.cc/" + imgSrc;
                }
                if (!imgSrc.isEmpty()) {
                    thumbnailList.add(imgSrc);
                }
            }
            
            // 备用图片选择器（如果没有找到图片）
            if (thumbnailList.isEmpty()) {
                Elements altImgElems = postElem.select("img[src*='oss3-bbs.mt2.cn'], img[src*='forum'], img[src*='image']");
                for (Element altImgElem : altImgElems) {
                    String imgSrc = altImgElem.attr("src");
                    if (!imgSrc.isEmpty() && !imgSrc.startsWith("http")) {
                        imgSrc = "https://bbs.binmt.cc/" + imgSrc;
                    }
                    // 过滤头像和表情
                    if (!imgSrc.isEmpty() && !imgSrc.contains("avatar.php") && !imgSrc.contains("smiley")) {
                        thumbnailList.add(imgSrc);
                    }
                }
            }
            
            // 设置缩略图列表和第一张缩略图
            if (!thumbnailList.isEmpty()) {
                post.setThumbnails(thumbnailList);
                post.setThumbnail(thumbnailList.get(0));
            }
            
            // 解析统计信息 (阅读、评论、赞)
            Elements statElems = postElem.select("div.comiis_znalist_bottom li");
            for (Element statElem : statElems) {
                String statText = statElem.text();
                if (statText.contains("阅读")) {
                    post.setViews(extractNumber(statText));
                } else if (statText.contains("评论")) {
                    post.setReplies(extractNumber(statText));
                } else if (statText.contains("赞")) {
                    // 点赞数格式: "2赞" 或 "赞"
                    int likes = extractNumber(statText);
                    post.setLikes(likes);
                }
            }
            
            if (post.getTid() > 0 && post.getTitle() != null) {
                posts.add(post);
            }
        }
        
        return posts;
    }
    
    /**
     * 解析板块列表页面
     * 接口: forum.php?forumlist=1
     * 移动端HTML结构:
     * <div class="comiis_forumlist comiis_km{id}">
     *   <div class="comiis_bbs_show"><h2><a>分类名称</a></h2></div>
     *   <div id="sub_forum_{id}" class="comiis_forum_nbox">
     *     <ul><li><a href="forum-{fid}-1.html"><em><img src="图标"/></em><p>板块名</p></a></li></ul>
     *   </div>
     * </div>
     */
    public static List<Forum> parseForumList(String html) {
        List<Forum> forums = new ArrayList<>();
        
        if (html == null || html.isEmpty()) {
            return forums;
        }
        
        Document doc = Jsoup.parse(html);
        
        // 解析所有板块分类组
        Elements categoryDivs = doc.select("div.comiis_forumlist");
        
        for (Element categoryDiv : categoryDivs) {
            // 从类名中提取分类ID (如 comiis_km1 -> 1)
            int categoryId = 0;
            String className = categoryDiv.className();
            Pattern kmPattern = Pattern.compile("comiis_km(\\d+)");
            Matcher kmMatcher = kmPattern.matcher(className);
            if (kmMatcher.find()) {
                categoryId = Integer.parseInt(kmMatcher.group(1));
            }
            
            // 解析分类名称
            String categoryName = null;
            Element categoryTitle = categoryDiv.selectFirst("div.comiis_bbs_show h2 a");
            if (categoryTitle != null) {
                categoryName = cleanHtmlEntities(categoryTitle.text());
            }
            
            // 添加分类标题项
            if (categoryName != null && !categoryName.isEmpty()) {
                Forum categoryItem = Forum.createCategory(categoryId, categoryName);
                forums.add(categoryItem);
            }
            
            // 解析该分类下的所有板块
            Elements forumItems = categoryDiv.select("div.comiis_forum_nbox ul li a[href*='forum-']");
            for (Element forumItem : forumItems) {
                Forum forum = new Forum();
                forum.setCategoryId(categoryId);
                forum.setCategoryName(categoryName);
                
                // 解析板块ID
                String href = forumItem.attr("href");
                String fid = extractFid(href);
                if (fid != null) {
                    forum.setFid(Integer.parseInt(fid));
                }
                
                // 解析板块名称
                Element nameElem = forumItem.selectFirst("p");
                if (nameElem != null) {
                    forum.setName(cleanHtmlEntities(nameElem.text()));
                }
                
                // 解析板块图标
                Element imgElem = forumItem.selectFirst("img");
                if (imgElem != null) {
                    String iconSrc = imgElem.attr("src");
                    if (iconSrc != null && !iconSrc.isEmpty()) {
                        if (!iconSrc.startsWith("http")) {
                            iconSrc = "https://cdn-bbs.mt2.cn" + iconSrc;
                        }
                        forum.setIcon(iconSrc);
                    }
                }
                
                // 解析今日帖子数 (如果有)
                Element todayPostsElem = forumItem.selectFirst("em span.bg_a");
                if (todayPostsElem != null) {
                    String postsText = todayPostsElem.text();
                    try {
                        forum.setTodayPosts(Integer.parseInt(postsText));
                    } catch (NumberFormatException ignored) {}
                }
                
                // 设置板块URL
                if (forum.getFid() > 0) {
                    forum.setUrl("https://bbs.binmt.cc/forum-" + forum.getFid() + "-1.html");
                }
                
                // 添加有效板块
                if (forum.getFid() > 0 && forum.getName() != null && !forum.getName().isEmpty()) {
                    forums.add(forum);
                }
            }
        }
        
        return forums;
    }
    
    /**
     * 解析热帖排行 (从首页HTML中提取)
     * 格式: <div class="comiis_mh_txtlist_phb"><li class="b_t"><a href="thread-xxx"><em>1</em>标题</a></li></div>
     */
    public static List<Post> parseHotRank(String html) {
        List<Post> posts = new ArrayList<>();
        
        if (html == null || html.isEmpty()) {
            return posts;
        }
        
        Document doc = Jsoup.parse(html);
        
        // 查找热帖排行区域 - 使用正确的class
        Elements rankItems = doc.select("div.comiis_mh_txtlist_phb li.b_t a[href*='thread-']");
        
        for (Element item : rankItems) {
            Post post = new Post();
            
            // 提取帖子ID
            String href = item.attr("href");
            String tid = extractTid(href);
            if (tid != null) {
                try {
                    post.setTid(Integer.parseInt(tid));
                } catch (NumberFormatException ignored) {}
            }
            
            // 提取标题 (直接使用title属性)
            String title = item.attr("title");
            if (title == null || title.isEmpty()) {
                title = item.text();
                // 移除开头的数字序号
                title = title.replaceFirst("^[0-9]+", "").trim();
            }
            post.setTitle(cleanHtmlEntities(title));
            
            // 设置热度排名
            post.setHot(true);
            
            if (post.getTid() > 0 && post.getTitle() != null && !post.getTitle().isEmpty()) {
                posts.add(post);
                if (posts.size() >= 7) break; // 最多取7条
            }
        }
        
        return posts;
    }
    
    /**
     * 解析帖子详情页面（支持PC端和移动端）
     */
    public static Post parsePostDetail(String html) {
        Document doc = Jsoup.parse(html);
        Post post = new Post();
        
        // 判断是否是移动端页面
        boolean isMobile = html.contains("comiis_viewtit") || html.contains("comiis_message_table");
        
        // 提取社交媒体预览图 (og:image)
        Element ogImage = doc.selectFirst("meta[property=og:image]");
        if (ogImage != null) {
            String imageUrl = ogImage.attr("content");
            if (imageUrl != null && !imageUrl.isEmpty()) {
                // 处理URL格式: https://bbs.binmt.cc/https://oss3-bbs.mt2.cn/...
                if (imageUrl.contains("https://bbs.binmt.cc/https://")) {
                    imageUrl = imageUrl.replace("https://bbs.binmt.cc/", "");
                }
                post.setThumbnail(imageUrl);
            }
        }
        
        // 也尝试提取 itemprop="image"
        if (post.getThumbnail() == null) {
            Element itemPropImage = doc.selectFirst("meta[itemprop=image]");
            if (itemPropImage != null) {
                String imageUrl = itemPropImage.attr("content");
                if (imageUrl != null && !imageUrl.isEmpty()) {
                    if (imageUrl.contains("https://bbs.binmt.cc/https://")) {
                        imageUrl = imageUrl.replace("https://bbs.binmt.cc/", "");
                    }
                    post.setThumbnail(imageUrl);
                }
            }
        }
        
        if (isMobile) {
            // 移动端页面解析
            parseMobilePostDetail(doc, post);
        } else {
            // PC端页面解析
            parsePCPostDetail(doc, post);
        }
        
        return post;
    }
    
    /**
     * 解析移动端帖子详情
     */
    private static void parseMobilePostDetail(Document doc, Post post) {
        // 标题 - 先尝试.km_tits，再尝试.comiis_viewtit h2
        Element titleElem = doc.selectFirst(".km_tits");
        
        if (titleElem == null) {
            titleElem = doc.selectFirst(".comiis_viewtit h2");
        }
        
        if (titleElem != null) {
            post.setTitle(cleanHtmlEntities(titleElem.text().trim()));
        }
        
        // 板块名和板块ID
        Element forumElem = doc.selectFirst("a.kmtit");
        if (forumElem != null) {
            post.setForumName(forumElem.text());
            
            // 从链接提取板块ID: href="forum.php?mod=forumdisplay&fid=41"
            String forumHref = forumElem.attr("href");
            if (forumHref != null) {
                String fid = extractFid(forumHref);
                if (fid != null) {
                    try {
                        post.setForumId(Integer.parseInt(fid));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        
        // 如果没有从板块链接获取到fid，尝试从页面其他位置获取
        if (post.getForumId() == 0) {
            // 尝试从面包屑导航获取: forum.php?mod=forumdisplay&fid=41
            Element breadcrumbFid = doc.selectFirst("a[href*='fid=']");
            if (breadcrumbFid != null) {
                String href = breadcrumbFid.attr("href");
                String fid = extractFid(href);
                if (fid != null) {
                    try {
                        post.setForumId(Integer.parseInt(fid));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        
        // 如果还是没有，尝试从JS变量中获取: var fid = '41';
        if (post.getForumId() == 0) {
            Pattern fidPattern = Pattern.compile("var\\s+fid\\s*=\\s*['\"]?(\\d+)['\"]?");
            Matcher fidMatcher = fidPattern.matcher(doc.html());
            if (fidMatcher.find()) {
                try {
                    post.setForumId(Integer.parseInt(fidMatcher.group(1)));
                } catch (NumberFormatException ignored) {}
            }
        }
        
        // 内容 - 获取主帖（楼主）的内容
        // 主帖容器: div.comiis_message.view_one (有多个类名)
        // 回复容器: div.comiis_message.view_all
        // 内容容器: div.comiis_messages (在主帖/回复容器内部)
        // 注意：悬赏帖的内容可能在pcb容器中，不在comiis_messages中
        
        Element messagesContainer = null;
        String content = null;
        
        // 方案1: 精确选择主帖内的内容容器
        Elements mainPostContainers = doc.select("div[class*='view_one']");
        for (Element container : mainPostContainers) {
            if (container.className().contains("comiis_message")) {
                Element msg = container.selectFirst("div.comiis_messages");
                if (msg != null) {
                    messagesContainer = msg;
                    break;
                }
            }
        }
        
        // 方案2: 备用 - 直接选择第一个 comiis_messages
        if (messagesContainer == null) {
            messagesContainer = doc.selectFirst("div.comiis_messages");
        }
        
        // 先尝试从 comiis_messages 获取内容
        if (messagesContainer != null) {
            content = messagesContainer.html();
            content = content.replace("src=\"//", "src=\"https://");
            content = content.replace("src='//", "src='https://");
            content = content.replace("src=\"/", "src=\"https://bbs.binmt.cc/");
            content = content.replace("src='/", "src='https://bbs.binmt.cc/");
        }
        
        // 方案3: 如果内容太短或不含悬赏/隐藏内容，尝试获取pcb容器
        // 悬赏帖的内容在 pcb 容器中，不在 comiis_messages 中
        if (content == null || content.length() < 500 || 
            (!content.contains("rwd") && !content.contains("隐藏内容") && !content.contains("comiis_xstop"))) {
            Elements pcbElems = doc.select("div.pcb");
            if (!pcbElems.isEmpty()) {
                String pcbContent = pcbElems.first().html();
                pcbContent = pcbContent.replace("src=\"//", "src=\"https://");
                pcbContent = pcbContent.replace("src='//", "src='https://");
                pcbContent = pcbContent.replace("src=\"/", "src=\"https://bbs.binmt.cc/");
                pcbContent = pcbContent.replace("src='/", "src='https://bbs.binmt.cc/");
                // 如果pcb内容更长，使用pcb内容
                if (pcbContent.length() > (content != null ? content.length() : 0)) {
                    content = pcbContent;
                }
            }
        }
        
        // 方案4: 最后备用 - 只获取 comiis_message_table
        if (content == null || content.isEmpty()) {
            Elements contentElems = doc.select("div.comiis_message_table");
            if (!contentElems.isEmpty()) {
                content = contentElems.first().html();
                content = content.replace("src=\"//", "src=\"https://");
                content = content.replace("src='//", "src='https://");
                content = content.replace("src=\"/", "src=\"https://bbs.binmt.cc/");
                content = content.replace("src='/", "src='https://bbs.binmt.cc/");
            }
        }
        
        if (content != null && !content.isEmpty()) {
            post.setContent(content);
        }
        
        // === 解析付费帖子信息 ===
        // HTML结构:
        // <div class="comiis_quote bg_h">
        //   <i class="comiis_font f_a">&#xe61d</i>&nbsp;<span class="kmren">已有 33 人购买</span>
        //   本主题需向作者支付 <strong>2金币</strong> 才能浏览
        //   本主题购买截止日期为 2026-3-11 15:55，到期后将免费
        //   <div class="cl"><a href="javascript:;" class="comiis_openrebox y f_a">
        //     <i class="comiis_font">&#xe65b</i> 购买主题</a>
        //   </div>
        // </div>
        Elements quoteElems = doc.select("div.comiis_quote");
        for (Element quoteElem : quoteElems) {
            String quoteText = quoteElem.text();
            
            // 检测是否是付费帖子（包含"购买"和"金币"）
            if (quoteText.contains("购买") && quoteText.contains("金币")) {
                post.setPaidPost(true);
                
                // 解析购买人数: "已有 33 人购买"
                Element buyersElem = quoteElem.selectFirst("span.kmren");
                if (buyersElem != null) {
                    String buyersText = buyersElem.text();
                    Pattern buyersPattern = Pattern.compile("(\\d+)");
                    Matcher buyersMatcher = buyersPattern.matcher(buyersText);
                    if (buyersMatcher.find()) {
                        post.setPaidBuyers(Integer.parseInt(buyersMatcher.group(1)));
                    }
                }
                
                // 解析价格: "本主题需向作者支付 2金币 才能浏览"
                Element priceElem = quoteElem.selectFirst("strong");
                if (priceElem != null) {
                    String priceText = priceElem.text();
                    Pattern pricePattern = Pattern.compile("(\\d+)");
                    Matcher priceMatcher = pricePattern.matcher(priceText);
                    if (priceMatcher.find()) {
                        post.setPaidPrice(Integer.parseInt(priceMatcher.group(1)));
                    }
                }
                
                // 解析截止日期: "本主题购买截止日期为 2026-3-11 15:55，到期后将免费"
                Pattern deadlinePattern = Pattern.compile("截止日期为\\s*(\\d{4}-\\d{1,2}-\\d{1,2}\\s*\\d{1,2}:\\d{1,2})");
                Matcher deadlineMatcher = deadlinePattern.matcher(quoteText);
                if (deadlineMatcher.find()) {
                    post.setPaidDeadline(deadlineMatcher.group(1));
                }
                
                // 检查是否有购买按钮（未购买则显示购买按钮）
                Element buyBtn = quoteElem.selectFirst("a:contains(购买主题)");
                post.setHasPurchased(buyBtn == null);
                
                break; // 只处理第一个付费信息块
            }
        }
        
        // 作者头像和名字 - 从 a.kmimg 获取
        Element authorElem = doc.selectFirst("a.kmimg");
        
        if (authorElem == null) {
            // 尝试其他选择器
            authorElem = doc.selectFirst(".comiis_view_topuser a[href*='uid']");
        }
        
        if (authorElem != null) {
            // 作者名是 a.kmimg 的文本内容
            String authorName = authorElem.text().trim();
            // 如果包含em标签，去掉它
            Element emTag = authorElem.selectFirst("em");
            if (emTag != null) {
                authorName = authorElem.text().replace(emTag.text(), "").trim();
            }
            post.setAuthor(cleanHtmlEntities(authorName));
            
            // 头像
            Element avatarImg = authorElem.selectFirst("img");
            if (avatarImg != null) {
                String avatarUrl = avatarImg.attr("src");
                if (avatarUrl.startsWith("/")) {
                    avatarUrl = "https://bbs.binmt.cc" + avatarUrl;
                }
                post.setAuthorAvatar(avatarUrl);
            }
            
            // 提取UID
            String href = authorElem.attr("href");
            String uidMatch = extractUid(href);
            if (uidMatch != null) {
                post.setAuthorId(Integer.parseInt(uidMatch));
            }
        }
        
        // 编辑信息
        Element editElem = doc.selectFirst("i.pstatus");
        if (editElem != null) {
            String editText = editElem.text();
            Pattern editTimePattern = Pattern.compile("(\\d{4}-\\d{1,2}-\\d{1,2}[\\s]+\\d{1,2}:\\d{1,2})");
            Matcher editMatcher = editTimePattern.matcher(editText);
            if (editMatcher.find()) {
                post.setEditTime(editMatcher.group(1));
            }
        }
        
        // 回复数 - 从"全部评论"标题中提取
        // HTML结构: <h2>全部评论<span class="f_d">503</span></h2>
        Element commentTitleElem = doc.selectFirst(".comiis_pltit h2");
        if (commentTitleElem != null) {
            String titleText = commentTitleElem.text();
            // 提取数字
            Pattern numPattern = Pattern.compile("(\\d+)");
            Matcher numMatcher = numPattern.matcher(titleText);
            if (numMatcher.find()) {
                try {
                    post.setReplies(Integer.parseInt(numMatcher.group(1)));
                } catch (NumberFormatException ignored) {}
            }
        }
        
        // 浏览数 - 从底部工具栏提取
        // HTML结构: <li><a class="comiis_position_key"><span class="comiis_kmvnum">503</span></a></li>
        Element viewElem = doc.selectFirst(".comiis_position_key .comiis_kmvnum");
        if (viewElem != null) {
            try {
                String viewText = viewElem.text().replaceAll("[^0-9]", "");
                if (!viewText.isEmpty()) {
                    post.setViews(Integer.parseInt(viewText));
                }
            } catch (NumberFormatException ignored) {}
        }
        
        // === 新增字段解析 ===
        
        // 作者等级 - 从第一个评论块获取 (top_lev)
        Element levelElem = doc.selectFirst(".comiis_postli .top_lev");
        if (levelElem != null) {
            post.setAuthorLevel(cleanHtmlEntities(levelElem.text()));
        }
        
        // 关注状态 - 从关注按钮判断
        Element followBtn = doc.selectFirst(".comiis_postli .followmod");
        if (followBtn != null) {
            // 如果按钮文本是"关注"则未关注，如果是"已关注"则已关注
            String followText = followBtn.text();
            post.setFollowed(followText.contains("已关注") || followText.contains("取消"));
        }
        
        // 发布时间和地区 - 从楼主帖子获取
        Element timeElem = doc.selectFirst(".kmtime");
        if (timeElem != null) {
            String timeText = timeElem.text();
            // 分离时间和地区
            int fromIndex = timeText.indexOf("来自");
            if (fromIndex > 0) {
                String timePart = timeText.substring(0, fromIndex).trim();
                post.setDateStr(timePart);
            } else {
                post.setDateStr(timeText.trim());
            }
        }
        
        // 发布地区
        Element locationElem = doc.selectFirst(".comiis_iplocality font");
        if (locationElem != null) {
            String location = locationElem.text().trim();
            if (location.startsWith("来自 ")) {
                location = location.substring(3);
            }
            post.setLocation(location);
        }
        
        // 解析楼主帖子的pid（用于赞赏楼主）
        Elements postBlocks = doc.select("div.comiis_postli");
        if (!postBlocks.isEmpty()) {
            Element firstBlock = postBlocks.first();
            String id = firstBlock.attr("id");
            if (id != null && id.startsWith("pid")) {
                try {
                    post.setFirstPid(Integer.parseInt(id.substring(3)));
                } catch (NumberFormatException ignored) {}
            }
        }
        
        // 赞赏按钮 - 检查是否存在
        Element rateBtn = doc.selectFirst(".comiis_postli .rate_btn a");
        if (rateBtn != null) {
            post.setCanRate(true);
        }
        
        // 点赞数
        Element likeNumElem = doc.selectFirst(".comiis_recommend_num");
        if (likeNumElem != null) {
            try {
                String likeText = likeNumElem.text().replaceAll("[^0-9]", "");
                if (!likeText.isEmpty()) {
                    post.setLikes(Integer.parseInt(likeText));
                }
            } catch (NumberFormatException ignored) {}
        }
        
        // 点赞用户列表
        Elements likeUserElems = doc.select(".comiis_recommend_list_a li a img");
        if (!likeUserElems.isEmpty()) {
            List<Post.LikeUser> likeUsers = new ArrayList<>();
            for (Element img : likeUserElems) {
                Element parentA = img.parent();
                if (parentA != null) {
                    String href = parentA.attr("href");
                    String uid = extractUid(href);
                    if (uid != null) {
                        String avatar = img.attr("src");
                        if (avatar.startsWith("/")) {
                            avatar = "https://bbs.binmt.cc" + avatar;
                        }
                        likeUsers.add(new Post.LikeUser(Integer.parseInt(uid), "", avatar));
                    }
                }
            }
            post.setLikeUsers(likeUsers);
        }
        
        // 评论总数 - 从"全部评论"标题中提取
        Element commentTitle = doc.selectFirst(".comiis_pltit h2");
        if (commentTitle != null) {
            String titleText = commentTitle.text();
            Pattern countPattern = Pattern.compile("(\\d+)");
            Matcher countMatcher = countPattern.matcher(titleText);
            if (countMatcher.find()) {
                post.setCommentTotal(Integer.parseInt(countMatcher.group(1)));
            }
        }
        
        // 点赞状态 - 检查是否已点赞
        // 方法1: 从JS变量中提取当前用户uid，检查点赞用户列表中是否包含该用户
        String htmlContent = doc.html();
        String currentUid = extractCurrentUid(htmlContent);
        boolean likedByList = false;
        boolean likedByText = false;
        
        if (currentUid != null) {
            // 检查点赞用户列表中是否有当前用户 (id="comiis_recommend_list_a{uid}")
            Element userInLikeList = doc.selectFirst("#comiis_recommend_list_a" + currentUid);
            likedByList = userInLikeList != null;
        }
        
        // 方法2: 检查页面是否包含"您已评价过本主题"（备选方案）
        if (htmlContent.contains("您已评价过本主题") || htmlContent.contains("已评价")) {
            likedByText = true;
        }
        
        // 只要任一方法判断为已点赞，则认为已点赞
        // 这样可以处理点赞用户列表尚未更新的情况
        post.setLiked(likedByList || likedByText);
        
        // 收藏状态 - 检查是否已收藏
        // 方法1: 检查页面是否包含"您已收藏"
        if (htmlContent.contains("您已收藏") || htmlContent.contains("已收藏")) {
            post.setFavorited(true);
        } else {
            // 方法2: 检查收藏按钮的class或text
            Element favoriteBtn = doc.selectFirst("a[href*='ac=favorite'], .favorite_btn");
            if (favoriteBtn != null) {
                String btnText = favoriteBtn.text();
                if (btnText.contains("已收藏")) {
                    post.setFavorited(true);
                } else {
                    post.setFavorited(false);
                }
            }
        }
        
        // 解析 formhash - 用于表单提交（回复、点赞等）
        String formhash = parseFormhash(htmlContent);
        if (formhash != null) {
            post.setFormhash(formhash);
        }
        
        // 解析 noticeauthor - 用于回复楼主时的通知token
        // 格式: name="noticeauthor" value="xxx"
        Pattern noticeAuthorPattern = Pattern.compile(
            "name=[\"']noticeauthor[\"'][^>]*value=[\"']([^\"']+)[\"']",
            Pattern.CASE_INSENSITIVE
        );
        Matcher noticeAuthorMatcher = noticeAuthorPattern.matcher(htmlContent);
        if (noticeAuthorMatcher.find()) {
            post.setNoticeAuthor(noticeAuthorMatcher.group(1));
        }
        
        // 解析图片上传hash - 用于上传附件
        // 格式: uploadformdata:{uid:"1398", hash:"xxx"} 或 data:{uid:"1398", hash:"xxx"}
        Pattern uploadHashPattern = Pattern.compile(
            "(?:uploadformdata|data):\\s*\\{\\s*uid:\\s*[\"']?(\\d+)[\"']?\\s*,\\s*hash:\\s*[\"']([a-f0-9]+)[\"']\\s*\\}",
            Pattern.CASE_INSENSITIVE
        );
        Matcher uploadHashMatcher = uploadHashPattern.matcher(htmlContent);
        if (uploadHashMatcher.find()) {
            try {
                post.setUploadUid(Integer.parseInt(uploadHashMatcher.group(1)));
            } catch (NumberFormatException ignored) {}
            post.setUploadHash(uploadHashMatcher.group(2));
        }
        
        // 解析踢帖信息 - 提取m参数和踢帖人数
        // 格式: bin_post_report&tid=xxx&m=xxx
        // 注意: HTML中可能使用&或&amp;，需要兼容两种格式
        Pattern kickPostMPattern = Pattern.compile(
            "bin_post_report(?:&|&amp;)tid=" + post.getTid() + "(?:&|&amp;)m=([a-f0-9]+)"
        );
        Matcher kickPostMMatcher = kickPostMPattern.matcher(htmlContent);
        if (kickPostMMatcher.find()) {
            post.setKickPostMParam(kickPostMMatcher.group(1));
        }
    }
    
    /**
     * 解析PC端帖子详情（也作为移动端的备选解析）
     */
    private static void parsePCPostDetail(Document doc, Post post) {
        // 标题 - 如果还没有解析到，尝试PC端选择器
        if (post.getTitle() == null || post.getTitle().isEmpty()) {
            Element titleElem = doc.selectFirst("span#thread_subject, h1.ts, .comiis_title");
            if (titleElem != null) {
                post.setTitle(cleanHtmlEntities(titleElem.text()));
            }
        }
        
        // 内容 - 如果还没有解析到，尝试多种选择器
        if (post.getContent() == null || post.getContent().isEmpty()) {
            // 先尝试移动端选择器
            Element contentElem = doc.selectFirst("div.comiis_a.comiis_message_table, .comiis_message_table");
            if (contentElem == null) {
                // 尝试获取整个pcb容器（包含悬赏等特殊内容）
                // 主帖是第一个pcb容器
                Elements pcbElems = doc.select("div.pcb");
                if (!pcbElems.isEmpty()) {
                    contentElem = pcbElems.first();
                }
            }
            if (contentElem == null) {
                // 最后尝试PC端选择器
                contentElem = doc.selectFirst("td.t_f[id^=postmessage]");
            }
            if (contentElem != null) {
                // 处理图片相对路径
                String content = contentElem.html();
                content = content.replace("src=\"/", "src=\"https://bbs.binmt.cc/");
                content = content.replace("src='/", "src='https://bbs.binmt.cc/");
                post.setContent(content);
            }
        }
        
        // 回复数和查看数 - 如果还没有解析到
        if (post.getReplies() == 0) {
            // 从"全部评论"标题中提取
            Element commentTitleElem = doc.selectFirst(".comiis_pltit h2");
            if (commentTitleElem != null) {
                String titleText = commentTitleElem.text();
                Pattern numPattern = Pattern.compile("(\\d+)");
                Matcher numMatcher = numPattern.matcher(titleText);
                if (numMatcher.find()) {
                    try {
                        post.setReplies(Integer.parseInt(numMatcher.group(1)));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        if (post.getViews() == 0) {
            Element viewElem = doc.selectFirst(".comiis_position_key .comiis_kmvnum");
            if (viewElem != null) {
                try {
                    String viewText = viewElem.text().replaceAll("[^0-9]", "");
                    if (!viewText.isEmpty()) {
                        post.setViews(Integer.parseInt(viewText));
                    }
                } catch (NumberFormatException ignored) {}
            }
        }
        
        // 作者头像 - 如果还没有解析到
        if (post.getAuthorAvatar() == null || post.getAuthorAvatar().isEmpty()) {
            // 先尝试移动端选择器
            Element authorElem = doc.selectFirst("a.kmimg");
            if (authorElem != null) {
                Element avatarImg = authorElem.selectFirst("img");
                if (avatarImg != null) {
                    String avatarUrl = avatarImg.attr("src");
                    if (avatarUrl.startsWith("/")) {
                        avatarUrl = "https://bbs.binmt.cc" + avatarUrl;
                    }
                    post.setAuthorAvatar(avatarUrl);
                }
            } else {
                // 再尝试PC端选择器
                Elements avatarImgs = doc.select(".avatar img");
                if (!avatarImgs.isEmpty()) {
                    Element avatarElem = avatarImgs.first();
                    String avatarUrl = avatarElem.attr("src");
                    if (avatarUrl.startsWith("/")) {
                        avatarUrl = "https://bbs.binmt.cc" + avatarUrl;
                    }
                    post.setAuthorAvatar(avatarUrl);
                }
            }
        }
        
        // 作者信息 - 取第一个帖子的作者
        Elements authiLinks = doc.select(".authi a[href*='space-uid']");
        if (!authiLinks.isEmpty()) {
            Element authorElem = authiLinks.first();
            post.setAuthor(cleanHtmlEntities(authorElem.text()));
            String uidMatch = extractUid(authorElem.attr("href"));
            if (uidMatch != null) {
                post.setAuthorId(Integer.parseInt(uidMatch));
            }
        }
        
        // 作者等级 - 取第一个
        Elements levelLinks = doc.select("p em a[href*='usergroup']");
        if (!levelLinks.isEmpty()) {
            Element levelElem = levelLinks.first();
            post.setAuthorLevel(levelElem.text());
        }
        
        // 发布时间
        String htmlStr = doc.html();
        Pattern timePattern = Pattern.compile("发表于[\\s]*</em>[\\s]*([0-9]{4}-[0-9]{1,2}-[0-9]{1,2}[\\s]+[0-9:]+)");
        Matcher timeMatcher = timePattern.matcher(htmlStr);
        if (timeMatcher.find()) {
            post.setDateStr(timeMatcher.group(1).trim());
        } else {
            // 尝试另一种格式 (span title)
            Element timeElem = doc.selectFirst("span[title]");
            if (timeElem != null) {
                post.setDateStr(timeElem.attr("title"));
            }
        }
        
        // 来源（来自手机/来自XX）
        Element locationElem = doc.selectFirst(".comiis_iplocality font");
        if (locationElem != null) {
            String location = locationElem.text().trim();
            if (location.startsWith("来自 ")) {
                location = location.substring(3);
            }
            post.setLocation(location);
        }
        
        // 编辑信息
        Element editElem = doc.selectFirst("i.pstatus");
        if (editElem != null) {
            String editText = editElem.text();
            // 提取编辑时间
            Pattern editTimePattern = Pattern.compile("(\\d{4}-\\d{1,2}-\\d{1,2}[\\s]+\\d{1,2}:\\d{1,2})");
            Matcher editMatcher = editTimePattern.matcher(editText);
            if (editMatcher.find()) {
                post.setEditTime(editMatcher.group(1));
            }
        }
    }
    
    /**
     * 解析评论列表
     */
    public static List<Comment> parseComments(String html) {
        List<Comment> comments = new ArrayList<>();
        Document doc = Jsoup.parse(html);
        
        // 提取fid（版块ID）用于举报功能
        // 从举报链接中提取: misc.php?mod=report&rtype=post&rid=xxx&tid=xxx&fid=50&mobile=2
        int fid = 0;
        Element reportLink = doc.selectFirst("a[href*='mod=report'][href*='fid=']");
        if (reportLink != null) {
            String href = reportLink.attr("href");
            Pattern fidPattern = Pattern.compile("fid=(\\d+)");
            java.util.regex.Matcher fidMatcher = fidPattern.matcher(href);
            if (fidMatcher.find()) {
                fid = Integer.parseInt(fidMatcher.group(1));
            }
        }
        
        // 移动端评论解析
        Elements commentBlocks = doc.select("div.comiis_postli");
        
        // 跳过第一个（楼主）
        for (int i = 1; i < commentBlocks.size(); i++) {
            Element block = commentBlocks.get(i);
            Comment comment = new Comment();
            
            // 设置fid（版块ID）
            comment.setFid(fid);
            
            // 评论ID
            String id = block.attr("id");
            if (id != null && id.startsWith("pid")) {
                try {
                    comment.setId(Integer.parseInt(id.substring(3)));
                } catch (NumberFormatException ignored) {}
            }
            
            // 作者头像
            Element avatarElem = block.selectFirst(".postli_top_tximg img, .top_tximg");
            if (avatarElem != null) {
                String avatarUrl = avatarElem.attr("src");
                if (avatarUrl.startsWith("/")) {
                    avatarUrl = "https://bbs.binmt.cc" + avatarUrl;
                }
                comment.setAuthorAvatar(avatarUrl);
            }
            
            // 作者名
            Element authorElem = block.selectFirst("a.top_user");
            if (authorElem != null) {
                comment.setAuthor(cleanHtmlEntities(authorElem.text().trim()));
            }
            
            // 作者ID
            Element authorLink = block.selectFirst("a[href*='space-uid'], a[href*='space&uid']");
            if (authorLink != null) {
                String uid = extractUid(authorLink.attr("href"));
                if (uid != null) {
                    comment.setAuthorId(Integer.parseInt(uid));
                }
            }
            
            // 作者等级
            Element levelElem = block.selectFirst(".top_lev");
            if (levelElem != null) {
                comment.setAuthorLevel(cleanHtmlEntities(levelElem.text()));
            }
            
            // 评论内容 - 获取父容器以包含图片等
            // HTML结构: <div class="comiis_messages"><div class="comiis_message_table">文本</div><ul class="comiis_img_one">图片</ul></div>
            Element messagesElem = block.selectFirst(".comiis_messages");
            Element contentElem = block.selectFirst(".comiis_message_table");
            if (messagesElem != null) {
                // 先处理引用回复
                Element quoteElem = messagesElem.selectFirst(".comiis_quote, .comiis_quotes, .quote");
                if (quoteElem != null) {
                    // 从blockquote中提取内容
                    Element blockquote = quoteElem.selectFirst("blockquote");
                    if (blockquote != null) {
                        // 获取所有font标签
                        Elements allFonts = blockquote.select("font");
                        
                        // 找到灰色字体的标签 (color="#999999" 或类似)
                        java.util.List<Element> grayFonts = new java.util.ArrayList<>();
                        for (Element font : allFonts) {
                            String color = font.attr("color");
                            if (color != null && (color.contains("999999") || color.contains("gray") || color.equalsIgnoreCase("#999999"))) {
                                grayFonts.add(font);
                            }
                        }
                        
                        // 提取作者信息（第一个灰色font）
                        if (!grayFonts.isEmpty()) {
                            String authorText = grayFonts.get(0).text();
                            // 格式: "lufeey 发表于 2026-2-26 11:45"
                            Pattern authorPattern = Pattern.compile("([^\\s]+)\\s+发表于\\s*.*");
                            java.util.regex.Matcher authorMatcher = authorPattern.matcher(authorText);
                            if (authorMatcher.find()) {
                                comment.setQuoteAuthor(authorMatcher.group(1).trim());
                            }
                        }
                        
                        // 提取引用内容（第二个灰色font）
                        if (grayFonts.size() >= 2) {
                            String quoteContent = grayFonts.get(1).text().trim();
                            if (!quoteContent.isEmpty()) {
                                comment.setQuoteContent(quoteContent);
                            }
                        }
                        
                        // 如果没有找到引用内容，尝试从整个blockquote文本中提取
                        if (comment.getQuoteContent() == null || comment.getQuoteContent().isEmpty()) {
                            String blockquoteText = blockquote.text();
                            // 移除 "回复 xxx 发表于..." 部分
                            Pattern contentPattern = Pattern.compile("回复\\s+[^\\s]+\\s+发表于\\s+\\d{4}-\\d{1,2}-\\d{1,2}\\s+\\d{1,2}:\\d{1,2}\\s*(.*)");
                            java.util.regex.Matcher contentMatcher = contentPattern.matcher(blockquoteText);
                            if (contentMatcher.find()) {
                                String quoteContent = contentMatcher.group(1).trim();
                                if (!quoteContent.isEmpty()) {
                                    comment.setQuoteContent(quoteContent);
                                }
                            }
                        }
                    }
                    
                    // 从父容器中移除引用块，避免重复显示
                    quoteElem.remove();
                }
                
                // 获取父容器的完整HTML（包含文本和图片，不含引用块）
                String content = messagesElem.html();
                content = content.replace("src=\"/", "src=\"https://bbs.binmt.cc/");
                content = content.replace("src='/", "src='https://bbs.binmt.cc/");
                
                comment.setContent(content);
                
                // 解析评论内容为内容块列表，支持图片等独立显示
                List<com.forum.mt.model.ContentBlock> contentBlocks = 
                        com.forum.mt.util.ContentParser.parse(content);
                comment.setContentBlocks(contentBlocks);
            } else if (contentElem != null) {
                // 兼容旧格式：只有 .comiis_message_table
                String content = contentElem.html();
                content = content.replace("src=\"/", "src=\"https://bbs.binmt.cc/");
                content = content.replace("src='/", "src='https://bbs.binmt.cc/");
                
                comment.setContent(content);
                
                List<com.forum.mt.model.ContentBlock> contentBlocks = 
                        com.forum.mt.util.ContentParser.parse(content);
                comment.setContentBlocks(contentBlocks);
            }
            
            // 楼层标签 (沙发、椅子、板凳等)
            Element floorLabelElem = block.selectFirst(".comiis_postli_top .f_d.y");
            if (floorLabelElem != null) {
                String floorText = floorLabelElem.text().trim();
                // 过滤掉私有区域字符（字体图标 U+E000-U+F8FF）
                floorText = floorText.replaceAll("[\\uE000-\\uF8FF]", "");
                // 提取楼层标签（沙发、椅子、板凳、#数字）
                comment.setFloorLabel(floorText);
                // 同时解析楼层号
                if (floorText.startsWith("#")) {
                    try {
                        comment.setFloor(Integer.parseInt(floorText.substring(1)));
                    } catch (NumberFormatException e) {
                        // 忽略
                    }
                } else if (floorText.equals("沙发")) {
                    comment.setFloor(1);
                } else if (floorText.equals("椅子")) {
                    comment.setFloor(2);
                } else if (floorText.equals("板凳")) {
                    comment.setFloor(3);
                }
            }
            
            // 发布时间和地区
            Element timeElem = block.selectFirst(".comiis_postli_times .comiis_tm");
            if (timeElem != null) {
                String timeText = timeElem.text();
                // 分离时间和地区
                int fromIndex = timeText.indexOf("来自");
                if (fromIndex > 0) {
                    comment.setDateStr(timeText.substring(0, fromIndex).trim());
                    String location = timeText.substring(fromIndex + 2).trim();
                    comment.setLocation(location);
                } else {
                    comment.setDateStr(timeText.trim());
                }
            }
            
            // 是否可以赞赏
            Element rateLink = block.selectFirst("a[href*='action=rate']");
            if (rateLink != null) {
                comment.setCanRate(true);
            }
            
            // 点赞数和点赞状态
            Element likeNumElem = block.selectFirst(".comiis_recommend_num");
            if (likeNumElem != null) {
                try {
                    String likeText = likeNumElem.text().replaceAll("[^0-9]", "");
                    if (!likeText.isEmpty()) {
                        comment.setLikes(Integer.parseInt(likeText));
                    }
                } catch (NumberFormatException ignored) {}
            }
            
            // 点赞状态 - 检查是否已点赞
            // 点赞成功后评论会有 f_wb class，或者点赞按钮有特定样式
            Element hotReplyElem = block.selectFirst(".comiis_hotreply.f_wb, .bottom_zhan .f_wb");
            if (hotReplyElem != null) {
                comment.setLiked(true);
            }
            
            // 另一种判断方式：检查点赞按钮图标（&#xe654 是已点赞，&#xe63b 是未点赞）
            Element likeIconElem = block.selectFirst(".comiis_hotreply i");
            if (likeIconElem != null) {
                String iconHtml = likeIconElem.html();
                // 已点赞图标是 &#xe654 或 
                if (iconHtml.contains("e654") || iconHtml.contains("")) {
                    comment.setLiked(true);
                }
            }
            
            // 过滤无效评论（作者或内容为空）
            if (comment.getAuthor() != null && !comment.getAuthor().isEmpty() &&
                comment.getContent() != null && !comment.getContent().trim().isEmpty()) {
                comments.add(comment);
            }
        }
        
        return comments;
    }
    
    /**
     * 解析用户信息
     * 页面: home.php?mod=space&do=profile&mobile=2
     * 移动端个人中心页面解析
     */
    public static User parseUserInfo(String html) {
        Document doc = Jsoup.parse(html);
        User user = new User();
        String htmlStr = doc.html();
        
        // formhash - 从JS变量提取（这个是会话级别的，可以使用）
        String formhash = parseFormhash(htmlStr);
        if (formhash != null) {
            user.setFormhash(formhash);
        }
        
        // 注意：discuz_uid 是当前登录用户的UID，不是被查看用户的UID
        // 所以不从 discuz_uid 提取UID，而是从页面"用户ID"字段提取
        
        // === 解析移动端个人中心页面 ===
        
        // 1. 用户基本信息区域 (.comiis_space_tx)
        // <div class="comiis_space_tx">
        //   <div class="user_img"><img src="头像URL"/></div>
        //   <h2 class="fyy">用户名</h2>
        //   <p><span>2 人气</span><span>0 关注</span><span>1 粉丝</span></p>
        //   <p><span class="kmlevs">Lv.4</span><span class="kmlev">高中生</span></p>
        // </div>
        Element spaceTx = doc.selectFirst("div.comiis_space_tx");
        if (spaceTx != null) {
            // 头像
            Element avatarImg = spaceTx.selectFirst("div.user_img img");
            if (avatarImg != null) {
                String avatarUrl = avatarImg.attr("src");
                if (!avatarUrl.startsWith("http")) {
                    avatarUrl = "https://bbs.binmt.cc/" + avatarUrl;
                }
                user.setAvatar(avatarUrl);
            }
            
            // 用户名
            Element usernameElem = spaceTx.selectFirst("h2.fyy");
            if (usernameElem != null) {
                user.setUsername(cleanHtmlEntities(usernameElem.text()));
            }
            
            // 人气、关注、粉丝
            Element statsP = spaceTx.selectFirst("p");  // 第一个p标签
            if (statsP != null) {
                Elements spans = statsP.select("span.z");
                for (Element span : spans) {
                    String text = cleanHtmlEntities(span.text());
                    if (text.contains("人气")) {
                        String num = text.replaceAll("[^0-9]", "");
                        if (!num.isEmpty()) {
                            user.setPopularity(Integer.parseInt(num));
                        }
                    } else if (text.contains("关注")) {
                        String num = text.replaceAll("[^0-9]", "");
                        if (!num.isEmpty()) {
                            user.setFollowing(Integer.parseInt(num));
                        }
                    } else if (text.contains("粉丝")) {
                        String num = text.replaceAll("[^0-9]", "");
                        if (!num.isEmpty()) {
                            user.setFollowers(Integer.parseInt(num));
                        }
                    }
                }
            }
            
            // 等级和用户组
            Element levelElem = spaceTx.selectFirst("span.kmlevs");
            if (levelElem != null) {
                user.setLevel(cleanHtmlEntities(levelElem.text()));
            }
            Element groupElem = spaceTx.selectFirst("span.kmlev");
            if (groupElem != null) {
                user.setGroupName(cleanHtmlEntities(groupElem.text()));
            }
        }
        
        // 2. 积分金币区域 (.comiis_space_profilejf)
        // <div class="comiis_space_profilejf">
        //   <ul>
        //     <li><span class="f_0">1160</span>积分</li>
        //     <li><span class="f_0">0</span>好评</li>
        //     <li><span class="f_0">597</span>金币</li>
        //     <li><span class="f_0">100</span>信誉</li>
        //   </ul>
        // </div>
        Element profilejf = doc.selectFirst("div.comiis_space_profilejf");
        if (profilejf != null) {
            Elements lis = profilejf.select("li");
            for (Element li : lis) {
                String text = cleanHtmlEntities(li.text());
                Element numSpan = li.selectFirst("span.f_0");
                String num = numSpan != null ? cleanHtmlEntities(numSpan.text()).replaceAll("[^0-9]", "") : "0";
                
                if (text.contains("积分")) {
                    if (!num.isEmpty()) user.setCredits(Integer.parseInt(num));
                } else if (text.contains("好评")) {
                    if (!num.isEmpty()) user.setGoodRate(Integer.parseInt(num));
                } else if (text.contains("金币")) {
                    if (!num.isEmpty()) user.setGoldCoin(Integer.parseInt(num));
                } else if (text.contains("信誉")) {
                    if (!num.isEmpty()) user.setReputation(Integer.parseInt(num));
                }
            }
        }
        
        // 3. 统计信息区域 (.comiis_space_profileico)
        // <div class="comiis_space_profileico">
        //   <ul>
        //     <li><span>帖子 1</span></li>
        //     <li><span>回复 282</span></li>
        //     <li><span>好友 0</span></li>
        //     <li><span>粉丝 1</span></li>
        //     <li><span>人气 2</span></li>
        //   </ul>
        // </div>
        Element profileico = doc.selectFirst("div.comiis_space_profileico");
        if (profileico != null) {
            Elements lis = profileico.select("li span");
            for (Element span : lis) {
                String text = cleanHtmlEntities(span.text());
                String num = text.replaceAll("[^0-9]", "");
                
                if (text.contains("帖子") && !num.isEmpty()) {
                    user.setThreads(Integer.parseInt(num));
                } else if (text.contains("回复") && !num.isEmpty()) {
                    user.setReplies(Integer.parseInt(num));
                } else if (text.contains("好友") && !num.isEmpty()) {
                    user.setFriends(Integer.parseInt(num));
                } else if (text.contains("粉丝") && !num.isEmpty() && user.getFollowers() == 0) {
                    user.setFollowers(Integer.parseInt(num));
                } else if (text.contains("人气") && !num.isEmpty() && user.getPopularity() == 0) {
                    user.setPopularity(Integer.parseInt(num));
                }
            }
        }
        
        // 4. 详细资料区域 (.comiis_space_profile)
        // <div class="comiis_space_profile">
        //   <ul>
        //     <li><div class="profile_rs">1398</div><span>用户ID</span></li>
        //     <li><div class="profile_rs">签名</div><span>个人签名</span></li>
        //     <li><div class="profile_rs">170 小时</div><span>在线时间</span></li>
        //     <li><div class="profile_rs">2018-3-15</div><span>注册时间</span></li>
        //     <li><div class="profile_rs">2026-2-23</div><span>最后访问</span></li>
        //   </ul>
        // </div>
        // 注意：页面中有多个div.comiis_space_profile区域，需要遍历所有区域
        // 第一个包含用户组、个人签名等，第二个包含用户ID、在线时间、注册时间等
        Elements profileDetails = doc.select("div.comiis_space_profile");
        for (Element profileDetail : profileDetails) {
            Elements lis = profileDetail.select("li");
            for (Element li : lis) {
                String label = li.selectFirst("span") != null ? li.selectFirst("span").text() : "";
                String value = li.selectFirst("div.profile_rs") != null ? 
                    cleanHtmlEntities(li.selectFirst("div.profile_rs").text()) : "";
                
                if (label.contains("在线时间")) {
                    user.setOnlineTime(value);
                } else if (label.contains("注册时间")) {
                    user.setRegDate(value);
                } else if (label.contains("最后访问")) {
                    user.setLastVisit(value);
                } else if (label.contains("个人签名")) {
                    user.setSignature(value);
                } else if (label.contains("性别")) {
                    user.setGender(value);
                } else if (label.contains("生日")) {
                    user.setBirthday(value);
                } else if (label.contains("用户ID")) {
                    // 从页面"用户ID"字段获取被查看用户的UID
                    try {
                        user.setUid(Integer.parseInt(value.replaceAll("[^0-9]", "")));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        
        // === 备用解析 - 从侧边栏 ===
        
        // 如果没有获取到用户名，尝试从侧边栏获取
        if (user.getUsername() == null || user.getUsername().isEmpty()) {
            Element usernameElem = doc.selectFirst(".sidenv_user .user_tit, span.user_tit.fyy");
            if (usernameElem != null) {
                user.setUsername(cleanHtmlEntities(usernameElem.text()));
            }
        }
        
        // 如果没有获取到头像，尝试从侧边栏获取
        if (user.getAvatar() == null || user.getAvatar().isEmpty()) {
            Element avatarElem = doc.selectFirst(".sidenv_user em img, img[src*='avatar.php']");
            if (avatarElem != null) {
                String avatarUrl = avatarElem.attr("src");
                if (!avatarUrl.startsWith("http")) {
                    avatarUrl = "https://bbs.binmt.cc/" + avatarUrl;
                }
                user.setAvatar(avatarUrl);
            }
        }
        
        // 如果没有获取到UID，从头像URL提取
        if (user.getUid() == 0) {
            String uidFromAvatar = extractUid(user.getAvatar());
            if (uidFromAvatar != null) {
                user.setUid(Integer.parseInt(uidFromAvatar));
            }
        }
        
        // 如果没有获取到等级和用户组，尝试从侧边栏获取
        if (user.getLevel() == null) {
            Element levelElem = doc.selectFirst(".sidenv_user span.user_lev");
            if (levelElem != null) {
                user.setLevel(cleanHtmlEntities(levelElem.text()));
            }
        }
        
        // 从页面关键词中提取用户名
        if (user.getUsername() == null || user.getUsername().isEmpty()) {
            Element keywordsMeta = doc.selectFirst("meta[name=keywords]");
            if (keywordsMeta != null) {
                String keywords = keywordsMeta.attr("content");
                // 格式: "tianxishuo的个人资料"
                if (keywords.contains("的个人资料")) {
                    user.setUsername(keywords.replace("的个人资料", "").trim());
                }
            }
        }
        
        // 解析关注状态 - 从用户详情页面的关注按钮判断
        // HTML结构: <div class="comiis_space_flw"><a id="followmod" href="...op=add...">关注</a></div>
        // 已关注: href包含op=del，或文本为"已关注"/"取消关注"
        Element followBtn = doc.selectFirst("div.comiis_space_flw a#followmod");
        if (followBtn != null) {
            String href = followBtn.attr("href");
            String followText = followBtn.text();
            // 如果URL包含op=del或文本是"已关注"/"取消关注"，则已关注
            boolean isFollowed = href.contains("op=del") || 
                                 followText.contains("已关注") || 
                                 followText.contains("取消");
            user.setFollowed(isFollowed);
        }
        
        // 解析空间背景封面图片 - 从style标签中的.comiis_space_box样式中提取
        // 格式: <style>.comiis_space_box{background:url(./source/plugin/.../home_bg/tianyuan/ty006.jpg) no-repeat 0 0 / cover;}</style>
        // 注意：背景图片定义在.comiis_space_box类中，需要精确匹配该类的样式规则
        Elements styleTags = doc.select("style");
        for (Element styleTag : styleTags) {
            String styleContent = styleTag.html();
            // 查找包含.comiis_space_box的样式（背景定义在这个类中）
            if (styleContent.contains(".comiis_space_box") || styleContent.contains("comiis_space_box")) {
                // 先尝试精确匹配 .comiis_space_box{...} 规则中的background
                // 这样避免匹配到其他规则中的background
                Pattern spaceBoxPattern = Pattern.compile(
                    "\\.comiis_space_box\\s*\\{[^}]*background(?:-image)?\\s*:\\s*url\\s*\\(\\s*['\"]?([^'\"()]+)['\"]?\\s*\\)",
                    Pattern.CASE_INSENSITIVE
                );
                Matcher spaceBoxMatcher = spaceBoxPattern.matcher(styleContent);
                if (spaceBoxMatcher.find()) {
                    String bgUrl = spaceBoxMatcher.group(1).trim();
                    // 转换为完整URL
                    if (bgUrl.startsWith("./")) {
                        bgUrl = "https://bbs.binmt.cc/" + bgUrl.substring(2);
                    } else if (bgUrl.startsWith("/") && !bgUrl.startsWith("//")) {
                        bgUrl = "https://bbs.binmt.cc" + bgUrl;
                    } else if (!bgUrl.startsWith("http")) {
                        bgUrl = "https://bbs.binmt.cc/" + bgUrl;
                    }
                    user.setCoverImage(bgUrl);
                    break;
                }
                
                // 如果精确匹配失败，尝试更宽松的匹配（查找.comiis_space_box后面的background）
                // 格式可能是: .comiis_sidenv_box, .comiis_space_box { background:url(...) }
                Pattern fallbackPattern = Pattern.compile(
                    "comiis_space_box[^{]*\\{[^}]*background(?:-image)?\\s*:\\s*url\\s*\\(\\s*['\"]?([^'\"()]+)['\"]?\\s*\\)",
                    Pattern.CASE_INSENSITIVE
                );
                Matcher fallbackMatcher = fallbackPattern.matcher(styleContent);
                if (fallbackMatcher.find()) {
                    String bgUrl = fallbackMatcher.group(1).trim();
                    if (bgUrl.startsWith("./")) {
                        bgUrl = "https://bbs.binmt.cc/" + bgUrl.substring(2);
                    } else if (bgUrl.startsWith("/") && !bgUrl.startsWith("//")) {
                        bgUrl = "https://bbs.binmt.cc" + bgUrl;
                    } else if (!bgUrl.startsWith("http")) {
                        bgUrl = "https://bbs.binmt.cc/" + bgUrl;
                    }
                    user.setCoverImage(bgUrl);
                    break;
                }
            }
        }
        
        return user;
    }
    
    /**
     * 解析关注/粉丝列表中的统计数字
     * 从标题中提取: <h3><i class="comiis_font">...</i>粉丝 1</h3>
     */
    public static int parseFollowCount(String html, String type) {
        if (html == null || html.isEmpty()) return 0;
        
        // 格式: "粉丝 1" 或 "关注 2"
        Pattern pattern = Pattern.compile(type + "\\s*(\\d+)");
        Matcher matcher = pattern.matcher(html);
        if (matcher.find()) {
            return Integer.parseInt(matcher.group(1));
        }
        return 0;
    }
    
    /**
     * 提取formhash - 多种格式匹配
     */
    public static String parseFormhash(String html) {
        if (html == null || html.isEmpty()) {
            return null;
        }
        
        // 尝试多种格式匹配
        String[] patterns = {
            "value=['\"]([a-f0-9]+)['\"]\\s+name=['\"]formhash['\"]",
            "name=['\"]formhash['\"]\\s+value=['\"]([a-f0-9]+)['\"]",
            "formhash['\"]?\\s*[:=]\\s*['\"]([a-f0-9]+)['\"]",
            "formhash=([a-f0-9]+)",
            "\"formhash\":\"([a-f0-9]+)\"",
            "'formhash':'([a-f0-9]+)'"
        };
        
        for (String pattern : patterns) {
            Pattern p = Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
            Matcher m = p.matcher(html);
            if (m.find()) {
                String formhash = m.group(1);
                if (formhash != null && formhash.length() >= 6) {
                    return formhash;
                }
            }
        }
        
        return null;
    }
    
    /**
     * 从URL提取帖子ID
     */
    private static String extractTid(String url) {
        Pattern p = Pattern.compile("thread-(\\d+)|tid=(\\d+)");
        Matcher m = p.matcher(url);
        if (m.find()) {
            return m.group(1) != null ? m.group(1) : m.group(2);
        }
        return null;
    }

    /**
     * 从分类标签URL提取筛选类型
     * URL格式: forum.php?mod=forumdisplay&fid=1&filter=typeid&typeid=8
     * 或: forum.php?mod=forumdisplay&fid=1&filter=lastpost
     */
    private static String extractFilterFromHref(String url) {
        if (url == null || url.isEmpty()) {
            return "all";
        }

        // 检查是否有 typeid 参数（主题分类）
        Pattern typePattern = Pattern.compile("typeid=(\\d+)");
        Matcher typeMatcher = typePattern.matcher(url);
        if (typeMatcher.find()) {
            return "typeid:" + typeMatcher.group(1);
        }

        // 检查 filter 参数
        Pattern filterPattern = Pattern.compile("filter=([^&]+)");
        Matcher filterMatcher = filterPattern.matcher(url);
        if (filterMatcher.find()) {
            String filter = filterMatcher.group(1);
            // 映射一些常见的筛选类型
            switch (filter) {
                case "lastpost":
                    return "lastpost";
                case "heat":
                    return "heat";
                case "digest":
                    return "digest";
                case "typeid":
                    return "typeid";
                default:
                    return "all";
            }
        }

        return "all";
    }

    /**
     * 从HTML中提取当前登录用户ID
     * 使用 discuz_uid JS变量，这是Discuz框架存储当前登录用户UID的标准方式
     */
    private static String extractCurrentUid(String html) {
        if (html == null || html.isEmpty()) return null;
        // 优先匹配 discuz_uid 变量（当前登录用户的UID）
        // 格式: var discuz_uid = '1398' 或 discuz_uid = "1398"
        Pattern discuzUidPattern = Pattern.compile("discuz_uid\\s*=\\s*['\"](\\d+)['\"]");
        Matcher discuzUidMatcher = discuzUidPattern.matcher(html);
        if (discuzUidMatcher.find()) {
            return discuzUidMatcher.group(1);
        }
        return null;
    }
    
    /**
     * 从URL提取用户ID
     */
    private static String extractUid(String url) {
        Pattern p = Pattern.compile("uid=(\\d+)|space-uid-(\\d+)");
        Matcher m = p.matcher(url);
        if (m.find()) {
            return m.group(1) != null ? m.group(1) : m.group(2);
        }
        return null;
    }
    
    /**
     * 从URL提取版块ID
     */
    private static String extractFid(String url) {
        if (url == null || url.isEmpty()) return null;
        Pattern p = Pattern.compile("fid=(\\d+)|forum-(\\d+)|forum\\.php\\?mod=forumdisplay&fid=(\\d+)");
        Matcher m = p.matcher(url);
        if (m.find()) {
            if (m.group(1) != null) return m.group(1);
            if (m.group(2) != null) return m.group(2);
            if (m.group(3) != null) return m.group(3);
        }
        return null;
    }
    
    /**
     * 从文本中提取数字
     */
    private static int extractNumber(String text) {
        if (text == null || text.isEmpty()) return 0;
        Pattern p = Pattern.compile("(\\d+)");
        Matcher m = p.matcher(text);
        if (m.find()) {
            try {
                return Integer.parseInt(m.group(1));
            } catch (NumberFormatException ignored) {}
        }
        return 0;
    }
    
    /**
     * 解析收藏列表
     * 从收藏页面解析帖子信息，并提取收藏ID（favid）
     * HTML结构:
     * <li class="mysclist_li b_t">
     *   <a href="...&op=delete&favid=641542&type=all" class="dialog bg_b f_c y">
     *     <i class="comiis_font">&#xe67f;</i>
     *   </a>
     *   <h2>
     *     <img src="static/image/feed/thread.gif" alt="thread" class="t" />
     *     <a href="https://bbs.binmt.cc/thread-162927-1-1.html">签校杀手Pro</a>
     *   </h2>
     * </li>
     */
    public static List<Post> parseFavoriteList(String html) {
        List<Post> posts = new ArrayList<>();
        
        if (html == null || html.isEmpty()) {
            return posts;
        }
        
        Document doc = Jsoup.parse(html);
        
        // 收藏列表格式: li.mysclist_li
        Elements postElements = doc.select("li.mysclist_li");
        
        if (!postElements.isEmpty()) {
            for (Element postElem : postElements) {
                Post post = new Post();
                
                // 解析帖子链接和标题
                Element titleLink = postElem.selectFirst("h2 a[href*='thread-']");
                if (titleLink != null) {
                    String href = titleLink.attr("href");
                    String tid = extractTid(href);
                    if (tid != null) {
                        try {
                            post.setTid(Integer.parseInt(tid));
                        } catch (NumberFormatException ignored) {}
                    }
                    
                    // 解析标题
                    String title = cleanHtmlEntities(titleLink.text());
                    post.setTitle(title);
                }
                
                // 解析收藏ID（favid）
                // 收藏ID在删除链接中: href="home.php?mod=spacecp&ac=favorite&op=delete&favid=641542&type=all"
                Element deleteLink = postElem.selectFirst("a[href*='op=delete']");
                if (deleteLink != null) {
                    String href = deleteLink.attr("href");
                    Pattern favIdPattern = Pattern.compile("favid=(\\d+)");
                    Matcher favIdMatcher = favIdPattern.matcher(href);
                    if (favIdMatcher.find()) {
                        try {
                            int favId = Integer.parseInt(favIdMatcher.group(1));
                            post.setFavId(favId);
                        } catch (NumberFormatException ignored) {}
                    }
                }
                
                // 检查是否是帖子类型（thread.gif）
                Element typeImg = postElem.selectFirst("h2 img[alt='thread']");
                if (typeImg != null) {
                    // 这是帖子类型的收藏
                }
                
                // 只添加有效的收藏项
                if (post.getTid() > 0 && post.getFavId() > 0) {
                    posts.add(post);
                }
            }
        }
        
        return posts;
    }
    
    /**
     * 解析我的回复列表
     * 接口: home.php?mod=space&uid={uid}&do=thread&view=me&type=reply&from=space
     * HTML结构与帖子列表类似，但包含用户回复的摘要
     * 
     * 结构:
     * <li class="forumlist_li comiis_znalist">
     *   <div class="forumlist_li_top cl">
     *     <a href="space-uid-xxx" class="wblist_tximg"><img class="top_tximg" src="avatar"></a>
     *     <h2>
     *       <a class="top_user">原作者</a>
     *       <span class="top_lev bg_a f_f">Lv.5</span>
     *     </h2>
     *     <div class="forumlist_li_time"><span class="f_d">回复时间</span></div>
     *   </div>
     *   <div class="mmlist_li_box cl">
     *     <h2><a href="thread-xxx">帖子标题</a></h2>
     *     <div class="list_body cl"><a class="f_b">我的回复内容摘要...</a></div>
     *   </div>
     *   <div class="comiis_xznalist_bk cl">
     *     <a href="forum-xxx">版块名</a>
     *   </div>
     * </li>
     */
    public static List<Post> parseMyReplies(String html) {
        List<Post> posts = new ArrayList<>();
        
        if (html == null || html.isEmpty()) {
            return posts;
        }
        
        Document doc = Jsoup.parse(html);
        
        // 使用与帖子列表相同的选择器
        Elements postElements = doc.select("li.forumlist_li.comiis_znalist");
        
        if (postElements.isEmpty()) {
            // 备用选择器
            postElements = doc.select("li.forumlist_li");
        }
        
        for (Element postElem : postElements) {
            Post post = new Post();
            
            // 解析帖子链接和ID
            Element titleLink = postElem.selectFirst("div.mmlist_li_box h2 a");
            if (titleLink != null) {
                String href = titleLink.attr("href");
                String tid = extractTid(href);
                if (tid != null) {
                    try {
                        post.setTid(Integer.parseInt(tid));
                    } catch (NumberFormatException ignored) {}
                }
                
                // 解析标题 (可能有状态标签如"关闭"、"精华"等)
                String title = cleanHtmlEntities(titleLink.text());
                post.setTitle(title);
            }
            
            // 解析我的回复内容摘要 (这是关键 - 区别于普通帖子列表)
            Element summaryElem = postElem.selectFirst("div.list_body a.f_b");
            if (summaryElem != null) {
                String replySummary = cleanHtmlEntities(summaryElem.text());
                post.setSummary(replySummary);
                // 标记这是回复内容
                post.setMyReplyContent(replySummary);
            }
            
            // 解析原帖作者信息 (顶部区域)
            Element topArea = postElem.selectFirst("div.forumlist_li_top");
            if (topArea != null) {
                // 原帖作者
                Element authorLink = topArea.selectFirst("a.top_user");
                if (authorLink != null) {
                    post.setAuthor(cleanHtmlEntities(authorLink.text()));
                }
                
                // 原帖作者头像
                Element avatarImg = topArea.selectFirst("img.top_tximg");
                if (avatarImg != null) {
                    String avatarUrl = avatarImg.attr("src");
                    if (avatarUrl.startsWith("/")) {
                        avatarUrl = "https://bbs.binmt.cc" + avatarUrl;
                    } else if (!avatarUrl.startsWith("http")) {
                        avatarUrl = "https://bbs.binmt.cc/" + avatarUrl;
                    }
                    post.setAuthorAvatar(avatarUrl);
                }
                
                // 原帖作者等级
                Element levelElem = topArea.selectFirst("span.top_lev");
                if (levelElem != null) {
                    String level = cleanHtmlEntities(levelElem.text());
                    post.setAuthorLevel(level);
                }
                
                // 我的回复时间 (显示在顶部时间区域)
                Element timeElem = topArea.selectFirst("div.forumlist_li_time span");
                if (timeElem != null) {
                    post.setDateStr(cleanHtmlEntities(timeElem.text()));
                }
            }
            
            // 解析版块
            Element forumLink = postElem.selectFirst("div.comiis_xznalist_bk a");
            if (forumLink != null) {
                String forumName = cleanHtmlEntities(forumLink.text());
                post.setForumName(forumName);
                
                String fid = extractFid(forumLink.attr("href"));
                if (fid != null) {
                    try {
                        post.setForumId(Integer.parseInt(fid));
                    } catch (NumberFormatException ignored) {}
                }
            }
            
            // 解析统计信息 (点赞、评论、浏览)
            Elements statElems = postElem.select("div.comiis_xznalist_bottom li");
            for (Element statElem : statElems) {
                String statText = statElem.text();
                if (statText.contains("赞")) {
                    int likes = extractNumber(statText);
                    post.setLikes(likes);
                } else if (statText.contains("评论") || statText.contains("回复")) {
                    int replies = extractNumber(statText);
                    post.setReplies(replies);
                } else {
                    int views = extractNumber(statText);
                    if (views > 0) {
                        post.setViews(views);
                    }
                }
            }
            
            // 标记这是我的回复列表中的帖子
            post.setMyReply(true);
            
            if (post.getTid() > 0 && post.getTitle() != null) {
                posts.add(post);
            }
        }
        
        return posts;
    }
    
    /**
     * 解析帖子搜索结果
     * 页面格式: search.php?mod=forum&searchid=xxx&kw=xxx&mobile=2
     * HTML结构: .comiis_forumlist .forumlist_li.comiis_znalist
     */
    public static List<Post> parseSearchPostResults(String html) {
        List<Post> posts = new ArrayList<>();

        if (html == null || html.isEmpty()) {
            return posts;
        }

        Document doc = Jsoup.parse(html);

        // 优先使用移动端选择器: li.forumlist_li.comiis_znalist
        Elements postElements = doc.select("li.forumlist_li.comiis_znalist");

        if (postElements.isEmpty()) {
            // 备用选择器: li.forumlist_li
            postElements = doc.select("li.forumlist_li");
        }

        if (postElements.isEmpty()) {
            // 最后尝试: .comiis_forumlist li
            postElements = doc.select(".comiis_forumlist li");
        }

        for (Element postElem : postElements) {
            Post post = new Post();
            
            // 解析帖子链接和ID
            Element titleLink = postElem.selectFirst("div.mmlist_li_box h2 a");
            if (titleLink != null) {
                String href = titleLink.attr("href");
                String tid = extractTid(href);
                if (tid != null) {
                    try {
                        post.setTid(Integer.parseInt(tid));
                    } catch (NumberFormatException ignored) {}
                }
                
                // 标题 (可能包含搜索高亮 <strong><font color="#ff0000">关键词</font></strong>)
                String title = cleanHtmlEntities(titleLink.text());
                post.setTitle(title);
            }
            
            // 摘要
            Element summaryElem = postElem.selectFirst("div.list_body a");
            if (summaryElem != null) {
                post.setSummary(cleanHtmlEntities(summaryElem.text()));
            }
            
            // 图片
            Element imgElem = postElem.selectFirst("div.comiis_pyqlist_img img");
            if (imgElem != null) {
                String imgSrc = imgElem.attr("src");
                if (!imgSrc.startsWith("http")) {
                    imgSrc = "https://bbs.binmt.cc/" + imgSrc;
                }
                post.setThumbnail(imgSrc);
            }
            
            // 作者信息
            Element topArea = postElem.selectFirst("div.forumlist_li_top");
            if (topArea != null) {
                // 作者名
                Element authorLink = topArea.selectFirst("a.top_user");
                if (authorLink != null) {
                    post.setAuthor(cleanHtmlEntities(authorLink.text()));
                }
                
                // 作者头像
                Element avatarImg = topArea.selectFirst("img.top_tximg");
                if (avatarImg != null) {
                    String avatarUrl = avatarImg.attr("src");
                    if (avatarUrl.startsWith("/")) {
                        avatarUrl = "https://bbs.binmt.cc" + avatarUrl;
                    } else if (!avatarUrl.startsWith("http")) {
                        avatarUrl = "https://bbs.binmt.cc/" + avatarUrl;
                    }
                    post.setAuthorAvatar(avatarUrl);
                    
                    // 从头像URL提取作者ID
                    String uid = extractUid(avatarUrl);
                    if (uid != null) {
                        try {
                            post.setAuthorId(Integer.parseInt(uid));
                        } catch (NumberFormatException ignored) {}
                    }
                }
                
                // 作者等级
                Element levelElem = topArea.selectFirst("span.top_lev");
                if (levelElem != null) {
                    post.setAuthorLevel(cleanHtmlEntities(levelElem.text()));
                }
                
                // 时间
                Element timeElem = topArea.selectFirst("div.forumlist_li_time span");
                if (timeElem != null) {
                    post.setDateStr(cleanHtmlEntities(timeElem.text()));
                }
            }
            
            // 解析统计信息 (点赞、评论、浏览)
            // 结构: <li id="comiis_listzhan_xxx">点赞数</li><li>评论数</li><li>浏览数</li>
            Elements statElems = postElem.select("div.comiis_xznalist_bottom ul li");
            int statIndex = 0;
            for (Element statElem : statElems) {
                // 点赞数：li有id属性 "comiis_listzhan_xxx"，或者第一个li
                String liId = statElem.attr("id");
                if (liId != null && liId.startsWith("comiis_listzhan_")) {
                    // 这是点赞数
                    Element numSpan = statElem.selectFirst("span.comiis_tm");
                    if (numSpan != null) {
                        String numText = numSpan.text().trim();
                        try {
                            post.setLikes(Integer.parseInt(numText));
                        } catch (NumberFormatException ignored) {}
                    }
                } else if (statIndex == 0) {
                    // 第一个li且没有id，可能是点赞
                    Element numSpan = statElem.selectFirst("span.comiis_tm");
                    if (numSpan != null) {
                        String numText = numSpan.text().trim();
                        try {
                            post.setLikes(Integer.parseInt(numText));
                        } catch (NumberFormatException ignored) {}
                    }
                }
                
                // 评论数：第二个li（包含thread链接）
                if (statElem.selectFirst("a[href*='thread-']") != null) {
                    Element numSpan = statElem.selectFirst("span.comiis_tm");
                    if (numSpan != null) {
                        String numText = numSpan.text().trim();
                        try {
                            post.setReplies(Integer.parseInt(numText));
                        } catch (NumberFormatException ignored) {}
                    }
                }
                
                // 浏览数：最后一个li，没有链接
                if (statElem.selectFirst("a") == null && !statElem.hasAttr("id")) {
                    Element numSpan = statElem.selectFirst("span.comiis_tm");
                    if (numSpan != null) {
                        String numText = numSpan.text().trim();
                        try {
                            post.setViews(Integer.parseInt(numText));
                        } catch (NumberFormatException ignored) {}
                    }
                }
                statIndex++;
            }
            
            // 解析版块名称
            Element forumLink = postElem.selectFirst("div.comiis_xznalist_bk a");
            if (forumLink != null) {
                String forumName = cleanHtmlEntities(forumLink.text());
                post.setForumName(forumName);
                
                String fid = extractFid(forumLink.attr("href"));
                if (fid != null) {
                    try {
                        post.setForumId(Integer.parseInt(fid));
                    } catch (NumberFormatException ignored) {}
                }
            }
            
            if (post.getTid() > 0 && post.getTitle() != null) {
                posts.add(post);
            }
        }
        
        return posts;
    }

    /**
     * 解析搜索结果分页信息
     * @param html 搜索结果HTML
     * @return 包含当前页和总页数的int数组，[0]=当前页，[1]=总页数，[2]=是否还有下一页
     */
    public static int[] parseSearchPagination(String html) {
        int[] pageInfo = new int[3]; // [current, total, hasNext]
        pageInfo[0] = 1; // 默认当前页
        pageInfo[1] = 1; // 默认总页数
        pageInfo[2] = 0; // 默认没有下一页

        if (html == null || html.isEmpty()) {
            return pageInfo;
        }

        Document doc = Jsoup.parse(html);

        // 查找分页元素 div.pg
        Element pageElem = doc.selectFirst("div.pg");
        if (pageElem != null) {
            // 当前页 - 从strong元素获取
            Element currentPageElem = pageElem.selectFirst("strong");
            if (currentPageElem != null) {
                try {
                    pageInfo[0] = Integer.parseInt(currentPageElem.text().trim());
                } catch (NumberFormatException ignored) {}
            }

            // 总页数 - 从span[title*="共"]提取
            Element totalPagesElem = pageElem.selectFirst("span[title*='共']");
            if (totalPagesElem != null) {
                String title = totalPagesElem.attr("title");
                Pattern pagePattern = Pattern.compile("共\\s*(\\d+)\\s*页");
                Matcher pageMatcher = pagePattern.matcher(title);
                if (pageMatcher.find()) {
                    pageInfo[1] = Integer.parseInt(pageMatcher.group(1));
                }
            }

            // 备用：从.next链接判断是否有下一页
            Element nextPageElem = pageElem.selectFirst("a.next");
            if (nextPageElem != null && !nextPageElem.hasAttr("onclick")) {
                pageInfo[2] = 1; // 有下一页
            }
        }

        return pageInfo;
    }

    /**
     * 解析用户搜索结果
     * 页面格式: home.php?mod=spacecp&ac=search&username=xxx&mobile=2
     * HTML结构: .comiis_userlist01 li
     */
    public static List<User> parseSearchUserResults(String html) {
        List<User> users = new ArrayList<>();
        
        if (html == null || html.isEmpty()) {
            return users;
        }
        
        Document doc = Jsoup.parse(html);
        
        // 用户搜索结果
        Elements userElements = doc.select(".comiis_userlist01 li");
        
        if (userElements.isEmpty()) {
            // 尝试其他选择器
            userElements = doc.select(".comiis_userlist li");
        }
        
        for (Element userElem : userElements) {
            User user = new User();
            
            // 用户名
            Element nameLink = userElem.selectFirst("p.tit a");
            if (nameLink != null) {
                user.setUsername(cleanHtmlEntities(nameLink.text()));
                
                // 从链接提取uid
                String href = nameLink.attr("href");
                String uid = extractUidFromUrl(href);
                if (uid != null) {
                    try {
                        user.setUid(Integer.parseInt(uid));
                    } catch (NumberFormatException ignored) {}
                }
            }
            
            // 用户组和积分
            Element txtElem = userElem.selectFirst("p.txt");
            if (txtElem != null) {
                String txt = txtElem.text();
                // 格式: "高中生 积分: 571"
                
                // 提取用户组
                Element groupElem = txtElem.selectFirst("font");
                if (groupElem != null) {
                    user.setGroupName(cleanHtmlEntities(groupElem.text()));
                }
                
                // 提取积分
                if (txt.contains("积分")) {
                    try {
                        String[] parts = txt.split("积分");
                        if (parts.length > 1) {
                            String creditStr = parts[1].replaceAll("[^0-9]", "");
                            if (!creditStr.isEmpty()) {
                                user.setCredits(Integer.parseInt(creditStr));
                            }
                        }
                    } catch (Exception ignored) {}
                }
            }
            
            // 头像
            Element avatarImg = userElem.selectFirst("img");
            if (avatarImg != null) {
                String avatarSrc = avatarImg.attr("src");
                if (avatarSrc.startsWith("/")) {
                    avatarSrc = "https://bbs.binmt.cc" + avatarSrc;
                } else if (!avatarSrc.startsWith("http")) {
                    avatarSrc = "https://bbs.binmt.cc/" + avatarSrc;
                }
                user.setAvatar(avatarSrc);
            }
            
            // 解析关注状态 - 从 ytit 区域的链接判断
            Element ytitElem = userElem.selectFirst("p.ytit");
            if (ytitElem != null) {
                // 查找关注链接
                Element followLink = ytitElem.selectFirst("a[href*='ac=follow']");
                if (followLink != null) {
                    String href = followLink.attr("href");
                    // 如果链接包含 op=del 表示已关注，op=add 表示未关注
                    boolean isFollowed = href.contains("op=del") || href.contains("op=delete");
                    user.setFollowed(isFollowed);
                }
            }
            
            // 如果没有从链接提取到uid，从头像URL提取
            if (user.getUid() == 0 && user.getAvatar() != null) {
                String uid = extractUidFromUrl(user.getAvatar());
                if (uid != null) {
                    try {
                        user.setUid(Integer.parseInt(uid));
                    } catch (NumberFormatException ignored) {}
                }
            }
            
            if (user.getUid() > 0 && user.getUsername() != null) {
                users.add(user);
            }
        }
        
        return users;
    }
    
    /**
     * 从URL中提取uid
     * 格式: home.php?mod=space&uid=1264 或 avatar.php?uid=1264
     */
    private static String extractUidFromUrl(String url) {
        if (url == null) return null;
        try {
            // 查找 uid= 参数
            int uidIndex = url.indexOf("uid=");
            if (uidIndex != -1) {
                String sub = url.substring(uidIndex + 4);
                // 提取数字部分
                StringBuilder sb = new StringBuilder();
                for (char c : sub.toCharArray()) {
                    if (Character.isDigit(c)) {
                        sb.append(c);
                    } else {
                        break;
                    }
                }
                return sb.length() > 0 ? sb.toString() : null;
            }
            
            // 也支持 space-uid-1264 格式
            if (url.contains("space-uid-")) {
                String[] parts = url.split("space-uid-");
                if (parts.length > 1) {
                    String sub = parts[1].split("[^0-9]")[0];
                    return sub.isEmpty() ? null : sub;
                }
            }
        } catch (Exception e) {
            // 忽略解析错误
        }
        return null;
    }
    
    // ========== 消息解析方法 ==========
    
    /**
     * 解析私信列表
     * HTML结构：
     * <a href="...touid=xxx" class="b_b">
     *   <img src="头像URL">
     *   <h2><span class="f_d">时间</span>用户名</h2>
     *   <p class="f_c">消息内容</p>
     * </a>
     */
    public static ForumApi.PmListResult parsePmList(String html) {
        ForumApi.PmListResult result = new ForumApi.PmListResult();
        List<PrivateMessage> messages = new ArrayList<>();

        if (html == null || html.isEmpty()) {
            result.setMessages(messages);
            return result;
        }

        Document doc = Jsoup.parse(html);

        // 解析私信列表项 - MT论坛使用a标签，href包含touid
        Elements pmElems = doc.select("a[href*='touid=']");

        for (Element pmElem : pmElems) {
            PrivateMessage pm = new PrivateMessage();
            
            String href = pmElem.attr("href");

            // 1. 提取touid
            Matcher m = Pattern.compile("touid=(\\d+)").matcher(href);
            if (m.find()) {
                int touid = Integer.parseInt(m.group(1));
                pm.setToUid(touid);
                pm.setFromUid(touid); // 对方用户ID
            }

            // 2. 提取头像
            Element avatarImg = pmElem.selectFirst("img");
            if (avatarImg != null) {
                String avatarSrc = avatarImg.attr("src");
                if (avatarSrc != null && !avatarSrc.isEmpty()) {
                    if (!avatarSrc.startsWith("http")) {
                        avatarSrc = "https://bbs.binmt.cc/" + avatarSrc;
                    }
                    pm.setFromAvatar(avatarSrc);
                }
                // 从头像URL提取uid
                Matcher uidMatcher = Pattern.compile("uid=(\\d+)").matcher(avatarSrc);
                if (uidMatcher.find()) {
                    pm.setFromUid(Integer.parseInt(uidMatcher.group(1)));
                }
            }

            // 3. 提取用户名和时间 - 在h2标签中
            Element h2 = pmElem.selectFirst("h2");
            if (h2 != null) {
                // 时间在span.f_d中
                Element timeSpan = h2.selectFirst("span.f_d");
                if (timeSpan != null) {
                    pm.setDateline(cleanHtmlEntities(timeSpan.text().trim()));
                }
                // 用户名是h2的直属文本（去掉span后的文本）
                String h2Text = h2.text();
                String timeText = timeSpan != null ? timeSpan.text() : "";
                String username = h2Text.replace(timeText, "").trim();
                pm.setFromUsername(cleanHtmlEntities(username));
            }

            // 4. 提取消息内容 - 在p.f_c中
            Element contentP = pmElem.selectFirst("p.f_c");
            if (contentP != null) {
                pm.setMessage(cleanHtmlEntities(contentP.text().trim()));
            }

            // 5. 判断是否已读 - 默认已读（可根据class判断）
            pm.setRead(true);

            // 只添加有效的私信（有用户名）
            if (pm.getFromUsername() != null && !pm.getFromUsername().isEmpty()) {
                messages.add(pm);
            }
        }

        // 解析分页信息
        parsePageInfo(doc, result);

        result.setMessages(messages);
        return result;
    }
    
    /**
     * 解析私信详情（对话列表）
     */
    public static ForumApi.PmDetailResult parsePmDetail(String html) {
        ForumApi.PmDetailResult result = new ForumApi.PmDetailResult();
        List<PrivateMessage> messages = new ArrayList<>();
        
        if (html == null || html.isEmpty()) {
            result.setMessages(messages);
            return result;
        }
        
        Document doc = Jsoup.parse(html);
        
        // 解析对方用户信息 - 从标题中获取
        User otherUser = new User();
        Element titleElem = doc.selectFirst(".header_z + h2, .comiis_head h2");
        if (titleElem != null) {
            String titleText = titleElem.text();
            // 移除在线状态等额外信息
            String username = titleText.replaceAll("\\(在线\\)", "").replaceAll("\\(离线\\)", "").trim();
            otherUser.setUsername(username);
        }
        
        // 从URL中获取对方UID或从消息中提取
        Elements friendMsgElems = doc.select(".comiis_friend_msg");
        if (!friendMsgElems.isEmpty()) {
            Element firstFriendMsg = friendMsgElems.first();
            Element avatarLink = firstFriendMsg.selectFirst("a[href*='uid=']");
            if (avatarLink != null) {
                String uid = extractUidFromUrl(avatarLink.attr("href"));
                if (uid != null) {
                    try {
                        otherUser.setUid(Integer.parseInt(uid));
                    } catch (NumberFormatException ignored) {}
                }
            }
            Element avatarImg = firstFriendMsg.selectFirst("img.msg_avt");
            if (avatarImg != null) {
                String avatarSrc = avatarImg.attr("src");
                if (!avatarSrc.startsWith("http")) {
                    avatarSrc = "https://bbs.binmt.cc/" + avatarSrc;
                }
                otherUser.setAvatar(avatarSrc);
            }
        }
        result.setOtherUser(otherUser);
        
        // 解析消息列表 - 按HTML中出现的顺序解析
        // 同时选中自己发送和对方发送的消息元素
        Elements allMsgElems = doc.select(".comiis_self_msg, .comiis_friend_msg");
        
        for (Element msgElem : allMsgElems) {
            PrivateMessage pm = new PrivateMessage();
            
            // 判断消息类型
            boolean isSelfMsg = msgElem.hasClass("comiis_self_msg");
            
            // 消息内容
            Element contentElem = msgElem.selectFirst(".msg_mes");
            if (contentElem != null) {
                pm.setMessage(cleanHtmlEntities(contentElem.text()));
            }
            
            // 时间
            Element timeElem = msgElem.selectFirst(".msg_time");
            if (timeElem != null) {
                pm.setDateline(cleanHtmlEntities(timeElem.text()));
            }
            
            // 设置发送者
            if (isSelfMsg) {
                // 自己发送的消息，fromUid设为0表示当前用户
                pm.setFromUid(0);
            } else {
                // 对方发送的消息
                pm.setFromUid(otherUser.getUid());
                pm.setFromUsername(otherUser.getUsername());
            }
            
            if (pm.getMessage() != null && !pm.getMessage().isEmpty()) {
                messages.add(pm);
            }
        }
        
        // 解析分页信息
        parsePageInfo(doc, result);
        
        result.setMessages(messages);
        return result;
    }
    
    /**
     * 解析增量获取的新消息（实时更新）
     * 接口返回XML格式: <?xml version="1.0" encoding="utf-8"?><root><![CDATA[...]]></root>
     * CDATA内容包含HTML片段，需要解析消息元素
     * 
     * @param xml XML响应内容
     * @return 新消息列表
     */
    public static List<PrivateMessage> parseNewPmMessages(String xml) {
        List<PrivateMessage> messages = new ArrayList<>();
        
        if (xml == null || xml.isEmpty()) {
            return messages;
        }
        
        // 提取CDATA内容
        String htmlContent = xml;
        Pattern cdataPattern = Pattern.compile("<\\!\\[CDATA\\[(.*?)\\]\\]>", Pattern.DOTALL);
        Matcher cdataMatcher = cdataPattern.matcher(xml);
        if (cdataMatcher.find()) {
            htmlContent = cdataMatcher.group(1);
        }
        
        if (htmlContent == null || htmlContent.isEmpty()) {
            return messages;
        }
        
        Document doc = Jsoup.parse(htmlContent);
        
        // 解析消息列表 - 与parsePmDetail类似的选择器
        Elements allMsgElems = doc.select(".comiis_self_msg, .comiis_friend_msg");
        
        for (Element msgElem : allMsgElems) {
            PrivateMessage pm = new PrivateMessage();
            
            // 判断消息类型
            boolean isSelfMsg = msgElem.hasClass("comiis_self_msg");
            
            // 消息内容
            Element contentElem = msgElem.selectFirst(".msg_mes");
            if (contentElem != null) {
                pm.setMessage(cleanHtmlEntities(contentElem.text()));
            }
            
            // 时间
            Element timeElem = msgElem.selectFirst(".msg_time");
            if (timeElem != null) {
                pm.setDateline(cleanHtmlEntities(timeElem.text()));
            }
            
            // 设置发送者
            if (isSelfMsg) {
                // 自己发送的消息，fromUid设为0表示当前用户
                pm.setFromUid(0);
            } else {
                // 对方发送的消息，尝试从头像链接提取uid
                Element avatarLink = msgElem.selectFirst("a[href*='uid=']");
                if (avatarLink != null) {
                    String uid = extractUidFromUrl(avatarLink.attr("href"));
                    if (uid != null) {
                        try {
                            pm.setFromUid(Integer.parseInt(uid));
                        } catch (NumberFormatException ignored) {}
                    }
                }
            }
            
            // 标记为新消息
            pm.setNew(true);
            
            if (pm.getMessage() != null && !pm.getMessage().isEmpty()) {
                messages.add(pm);
            }
        }
        
        return messages;
    }
    
        /**
         * 解析通知列表 (公共消息)
         * HTML结构：
         * <li>
         *   <a href="...pmid=xxx" class="b_b">
         *     <div class="systempm">图标</div>
         *     <h2><span class="f_d">时间</span>发送者</h2>
         *     <p class="f_c">消息内容</p>
         *   </a>
         * </li>
         */
        public static ForumApi.NotificationListResult parseNoticeList(String html) {
            ForumApi.NotificationListResult result = new ForumApi.NotificationListResult();
            List<Notification> notifications = new ArrayList<>();
            
            if (html == null || html.isEmpty()) {
                result.setNotifications(notifications);
                return result;
            }
            
            Document doc = Jsoup.parse(html);
    
            // 解析通知列表项 - 使用a标签，href包含pmid
            Elements noticeElems = doc.select("a[href*='pmid=']");
    
            for (Element noticeElem : noticeElems) {
                Notification notification = new Notification();
                
                String href = noticeElem.attr("href");
                
                // 提取pmid
                Matcher m = Pattern.compile("pmid=(\\d+)").matcher(href);
                if (m.find()) {
                    notification.setId(Integer.parseInt(m.group(1)));
                }
                
                // 设置URL
                if (href.startsWith("http")) {
                    notification.setUrl(href);
                } else {
                    notification.setUrl("https://bbs.binmt.cc/" + href);
                }
    
                // 通知类型 - 系统消息
                notification.setType(Notification.TYPE_SYSTEM);
    
                // 提取时间和发送者 - 在h2标签中
                Element h2 = noticeElem.selectFirst("h2");
                if (h2 != null) {
                    // 时间在span.f_d中
                    Element timeSpan = h2.selectFirst("span.f_d");
                    if (timeSpan != null) {
                        notification.setDateline(cleanHtmlEntities(timeSpan.text().trim()));
                    }
                    // 发送者是h2的直属文本（去掉span后的文本）
                    String h2Text = h2.text();
                    String timeText = timeSpan != null ? timeSpan.text() : "";
                    String sender = h2Text.replace(timeText, "").trim();
                    notification.setUsername(cleanHtmlEntities(sender));
                }
    
                // 提取消息内容 - 在p.f_c中
                Element contentP = noticeElem.selectFirst("p.f_c");
                if (contentP != null) {
                    notification.setContent(cleanHtmlEntities(contentP.text().trim()));
                }
    
                // 是否已读 - 检查是否有未读标记
                Element unreadSpan = noticeElem.selectFirst("span.kmnums");
                notification.setRead(unreadSpan == null);
    
                if (notification.getContent() != null && !notification.getContent().isEmpty()) {
                    notifications.add(notification);
                }
            }
    
            // 解析分页信息
            parsePageInfo(doc, result);
            
            result.setNotifications(notifications);
            return result;
        }
    
    /**
     * 解析分页信息
     */
    private static void parsePageInfo(Document doc, Object result) {
        Element pageElem = doc.selectFirst("div.pg, .pagination");
        if (pageElem != null) {
            // 当前页
            Element currentPageElem = pageElem.selectFirst("strong");
            if (currentPageElem != null) {
                try {
                    int page = Integer.parseInt(currentPageElem.text().trim());
                    if (result instanceof ForumApi.PmListResult) {
                        ((ForumApi.PmListResult) result).setCurrentPage(page);
                    } else if (result instanceof ForumApi.PmDetailResult) {
                        ((ForumApi.PmDetailResult) result).setCurrentPage(page);
                    } else if (result instanceof ForumApi.NotificationListResult) {
                        ((ForumApi.NotificationListResult) result).setCurrentPage(page);
                    }
                } catch (NumberFormatException ignored) {}
            }
            
            // 总页数
            Element lastPageElem = pageElem.selectFirst("a.last, a[href*='page=']");
            if (lastPageElem != null) {
                String href = lastPageElem.attr("href");
                Matcher m = Pattern.compile("page=(\\d+)").matcher(href);
                if (m.find()) {
                    try {
                        int totalPages = Integer.parseInt(m.group(1));
                        if (result instanceof ForumApi.PmListResult) {
                            ((ForumApi.PmListResult) result).setTotalPages(totalPages);
                        } else if (result instanceof ForumApi.PmDetailResult) {
                            ((ForumApi.PmDetailResult) result).setTotalPages(totalPages);
                        } else if (result instanceof ForumApi.NotificationListResult) {
                            ((ForumApi.NotificationListResult) result).setTotalPages(totalPages);
                        }
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        
        // 默认值
        if (result instanceof ForumApi.PmListResult) {
            if (((ForumApi.PmListResult) result).getCurrentPage() == 0) {
                ((ForumApi.PmListResult) result).setCurrentPage(1);
            }
        } else if (result instanceof ForumApi.PmDetailResult) {
            if (((ForumApi.PmDetailResult) result).getCurrentPage() == 0) {
                ((ForumApi.PmDetailResult) result).setCurrentPage(1);
            }
        } else if (result instanceof ForumApi.NotificationListResult) {
            if (((ForumApi.NotificationListResult) result).getCurrentPage() == 0) {
                ((ForumApi.NotificationListResult) result).setCurrentPage(1);
            }
        }
    }
    
    /**
     * 解析发帖页面
     * 提取formhash、uploadHash等必要参数
     * @param html 发帖页面HTML
     * @return NewThreadParams 发帖参数
     */
    public static com.forum.mt.model.NewThreadParams parseNewThreadPage(String html) {
        com.forum.mt.model.NewThreadParams params = new com.forum.mt.model.NewThreadParams();
        
        if (html == null || html.isEmpty()) {
            return params;
        }
        
        Document doc = Jsoup.parse(html);
        
        // 解析formhash - 从隐藏字段或JS变量中提取
        Element formhashInput = doc.selectFirst("input[name=formhash]");
        if (formhashInput != null) {
            params.setFormhash(formhashInput.attr("value"));
        } else {
            // 从JS变量中提取
            Pattern formhashPattern = Pattern.compile("formhash\\s*=\\s*['\"]([a-f0-9]{8})['\"]");
            Matcher formhashMatcher = formhashPattern.matcher(html);
            if (formhashMatcher.find()) {
                params.setFormhash(formhashMatcher.group(1));
            }
        }
        
        // 解析uploadHash - 从上传JS代码中提取
        // 格式: uploadformdata:{uid:"1398", hash:"9a86e64dd79ce5a61249cc3236495492"}
        Pattern hashPattern = Pattern.compile("hash:\\s*[\"']([a-f0-9]{32})[\"']");
        Matcher hashMatcher = hashPattern.matcher(html);
        if (hashMatcher.find()) {
            params.setUploadHash(hashMatcher.group(1));
        }
        
        // 解析UID
        Pattern uidPattern = Pattern.compile("uid:\\s*[\"']?(\\d+)[\"']?");
        Matcher uidMatcher = uidPattern.matcher(html);
        if (uidMatcher.find()) {
            try {
                params.setUid(Integer.parseInt(uidMatcher.group(1)));
            } catch (NumberFormatException ignored) {}
        } else {
            // 从discuz_uid变量提取
            Pattern discuzUidPattern = Pattern.compile("discuz_uid\\s*=\\s*['\"]?(\\d+)['\"]?");
            Matcher discuzUidMatcher = discuzUidPattern.matcher(html);
            if (discuzUidMatcher.find()) {
                try {
                    params.setUid(Integer.parseInt(discuzUidMatcher.group(1)));
                } catch (NumberFormatException ignored) {}
            }
        }
        
        // 解析posttime
        Element posttimeInput = doc.selectFirst("input[name=posttime]");
        if (posttimeInput != null) {
            try {
                params.setPosttime(Long.parseLong(posttimeInput.attr("value")));
            } catch (NumberFormatException ignored) {}
        }
        
        // 解析版块名称
        Element titleElem = doc.selectFirst("title");
        if (titleElem != null) {
            String title = titleElem.text();
            // 格式: "发帖 - MT论坛"
            if (title.contains("发帖")) {
                // 版块名称可能在页面的其他位置
                Element forumLink = doc.selectFirst("a[href*='forum.php?mod=forumdisplay&fid=']");
                if (forumLink != null) {
                    params.setForumName(cleanHtmlEntities(forumLink.text()));
                }
            }
        }
        
        // 解析最大售价
        Element priceInput = doc.selectFirst("input[name=price]");
        if (priceInput != null) {
            String placeholder = priceInput.attr("placeholder");
            if (placeholder != null && placeholder.contains("最高")) {
                Pattern pricePattern = Pattern.compile("(\\d+)");
                Matcher priceMatcher = pricePattern.matcher(placeholder);
                if (priceMatcher.find()) {
                    try {
                        params.setMaxPrice(Integer.parseInt(priceMatcher.group(1)));
                    } catch (NumberFormatException ignored) {}
                }
            }
        }
        
        // 检查是否允许上传图片
        Element imgUploadDiv = doc.selectFirst("div.comiis_allowpostimg");
        if (imgUploadDiv != null) {
            params.setAllowPostImg(1);
        }
        
        return params;
    }
    
    /**
     * 解析发帖结果
     * @param html 响应HTML/XML
     * @return 是否成功，失败时返回错误信息
     */
    public static ForumApi.NewThreadResult parseNewThreadResult(String html) {
        ForumApi.NewThreadResult result = new ForumApi.NewThreadResult();
        
        if (html == null || html.isEmpty()) {
            result.setSuccess(false);
            result.setMessage("响应为空");
            return result;
        }
        
        // 提取CDATA内容
        Pattern cdataPattern = Pattern.compile("<\\!\\[CDATA\\[(.*?)\\]\\]>", Pattern.DOTALL);
        Matcher cdataMatcher = cdataPattern.matcher(html);
        String content = html;
        if (cdataMatcher.find()) {
            content = cdataMatcher.group(1);
        }
        
        // 检查是否发帖成功
        if (content.contains("发布成功") || content.contains("感谢您发表主题") || content.contains("成功发布")) {
            result.setSuccess(true);
            result.setMessage("发帖成功");
            
            // 尝试提取帖子ID
            Pattern tidPattern = Pattern.compile("thread-(\\d+)");
            Matcher tidMatcher = tidPattern.matcher(content);
            if (tidMatcher.find()) {
                try {
                    result.setTid(Integer.parseInt(tidMatcher.group(1)));
                } catch (NumberFormatException ignored) {}
            }
            
            // 尝试提取跳转URL
            Pattern urlPattern = Pattern.compile("href=\"([^\"]+viewthread[^\"]+)\"");
            Matcher urlMatcher = urlPattern.matcher(content);
            if (urlMatcher.find()) {
                result.setRedirectUrl(urlMatcher.group(1).replace("&amp;", "&"));
            }
        } else if (content.contains("需要先登录") || content.contains("请先登录")) {
            result.setSuccess(false);
            result.setMessage("请先登录");
        } else if (content.contains("没有权限")) {
            result.setSuccess(false);
            result.setMessage("没有权限在此版块发帖");
        } else if (content.contains("标题")) {
            result.setSuccess(false);
            result.setMessage("请输入标题");
        } else if (content.contains("内容")) {
            result.setSuccess(false);
            result.setMessage("请输入内容");
        } else if (content.contains("过快") || content.contains("太快")) {
            result.setSuccess(false);
            result.setMessage("操作过快，请稍后再试");
        } else {
            // 尝试从messagetext区域提取错误信息
            // 格式: <dt class="f_b" id="messagetext"><p>错误信息<script>...</script></p></dt>
            Pattern messageTextPattern = Pattern.compile("id=\"messagetext\"[^>]*>\\s*<p>([^<]+)", Pattern.DOTALL);
            Matcher messageTextMatcher = messageTextPattern.matcher(content);
            if (messageTextMatcher.find()) {
                String errorMsg = messageTextMatcher.group(1).trim();
                if (!errorMsg.isEmpty()) {
                    result.setSuccess(false);
                    result.setMessage(errorMsg);
                    return result;
                }
            }
            
            // 尝试提取<p>标签中的文本（在script标签之前）
            // 使用更宽松的正则，提取<p>到第一个<之间的内容
            Pattern errorPattern = Pattern.compile("<p>([^<]+)", Pattern.DOTALL);
            Matcher errorMatcher = errorPattern.matcher(content);
            if (errorMatcher.find()) {
                String errorMsg = errorMatcher.group(1).trim();
                if (!errorMsg.isEmpty() && !errorMsg.startsWith("if(") && !errorMsg.contains("function")) {
                    result.setSuccess(false);
                    result.setMessage(errorMsg);
                    return result;
                }
            }
            
            // 尝试提取comiis_tip中的文本
            Pattern tipPattern = Pattern.compile("class=\"comiis_tip[^>]*>[^<]*<[^>]*>[^<]*<p>([^<]+)", Pattern.DOTALL);
            Matcher tipMatcher = tipPattern.matcher(content);
            if (tipMatcher.find()) {
                String errorMsg = tipMatcher.group(1).trim();
                if (!errorMsg.isEmpty()) {
                    result.setSuccess(false);
                    result.setMessage(errorMsg);
                    return result;
                }
            }
            
            result.setSuccess(false);
            result.setMessage("发帖失败，请重试");
        }
        
        return result;
    }
}