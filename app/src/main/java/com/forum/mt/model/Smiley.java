package com.forum.mt.model;

import java.util.ArrayList;
import java.util.List;

/**
 * 表情模型
 * MT论坛使用 Discuz 表情系统
 * 表情代码格式: [#滑稽] [呵呵] [doge] 等
 * 表情URL格式: 
 * - QQ表情: https://cdn-bbs.mt2.cn/static/image/smiley/qq/qqXXX.gif
 * - 淘宝表情: https://cdn-bbs.mt2.cn/static/image/smiley/comiis_tb/tb_XX.png
 * - Doge表情: https://cdn-bbs.mt2.cn/static/image/smiley/doge/X.png
 * 
 * 数据来源: common_smilies_var.js
 */
public class Smiley {
    private String id;          // 表情ID (如 "250", "1240", "1392")
    private String name;        // 表情名称 (如 "滑稽", "呵呵", "doge")
    private String imageUrl;    // 图片URL
    private String code;        // 表情代码 (如 "[#滑稽]", "[呵呵]", "[doge]")
    private String category;    // 分类名称
    private int categoryId;     // 分类ID (12=QQ, 5=淘宝, 14=Doge)
    
    // 表情分类常量
    public static final int CATEGORY_QQ = 12;      // 默认(QQ)表情
    public static final int CATEGORY_TB = 5;       // 滑稽(淘宝)表情
    public static final int CATEGORY_DOGE = 14;    // Doge表情
    
    // CDN基础URL
    private static final String CDN_BASE = "https://cdn-bbs.mt2.cn/static/image/smiley/";
    
    public Smiley() {}
    
    public Smiley(String id, String name, String imageUrl, String code, String category) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.code = code;
        this.category = category;
    }
    
    public Smiley(String id, String name, String imageUrl, String code, String category, int categoryId) {
        this.id = id;
        this.name = name;
        this.imageUrl = imageUrl;
        this.code = code;
        this.category = category;
        this.categoryId = categoryId;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getImageUrl() {
        return imageUrl;
    }
    
    public void setImageUrl(String imageUrl) {
        this.imageUrl = imageUrl;
    }
    
    public String getCode() {
        return code;
    }
    
    public void setCode(String code) {
        this.code = code;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public int getCategoryId() {
        return categoryId;
    }
    
    public void setCategoryId(int categoryId) {
        this.categoryId = categoryId;
    }
    
    /**
     * 获取QQ默认表情列表（完整107个）
     * 分类ID: 12, 目录: qq
     */
    public static List<Smiley> getQQSmileys() {
        List<Smiley> smileys = new ArrayList<>();
        String baseUrl = CDN_BASE + "qq/";
        String category = "默认";
        int categoryId = CATEGORY_QQ;
        
        // QQ表情数据: [id, code, filename]
        // 第一页 (1-40)
        String[][] qqSmileys = {
            // 第一页
            {"1240", "[呵呵]", "qq001.gif"},
            {"1287", "[撇嘴]", "qq002.gif"},
            {"1265", "[色]", "qq003.gif"},
            {"1267", "[发呆]", "qq004.gif"},
            {"1237", "[得意]", "qq005.gif"},
            {"1270", "[流泪]", "qq006.gif"},
            {"1209", "[害羞]", "qq007.gif"},
            {"1291", "[闭嘴]", "qq008.gif"},
            {"1263", "[睡]", "qq009.gif"},
            {"1196", "[大哭]", "qq010.gif"},
            {"1268", "[尴尬]", "qq011.gif"},
            {"1235", "[发怒]", "qq012.gif"},
            {"1212", "[调皮]", "qq013.gif"},
            {"1229", "[呲牙]", "qq014.gif"},
            {"1206", "[惊讶]", "qq015.gif"},
            {"1195", "[难过]", "qq016.gif"},
            {"1248", "[酷]", "qq017.gif"},
            {"1215", "[冷汗]", "qq018.gif"},
            {"1262", "[抓狂]", "qq019.gif"},
            {"1283", "[吐]", "qq020.gif"},
            {"1243", "[偷笑]", "qq021.gif"},
            {"1272", "[可爱]", "qq022.gif"},
            {"1277", "[白眼]", "qq023.gif"},
            {"1238", "[傲慢]", "qq024.gif"},
            {"1223", "[饥饿]", "qq025.gif"},
            {"1251", "[困]", "qq026.gif"},
            {"1218", "[惊恐]", "qq027.gif"},
            {"1249", "[流汗]", "qq028.gif"},
            {"1197", "[憨笑]", "qq029.gif"},
            {"1255", "[装逼]", "qq030.gif"},
            {"1205", "[奋斗]", "qq031.gif"},
            {"1273", "[咒骂]", "qq032.gif"},
            {"1214", "[疑问]", "qq033.gif"},
            {"1260", "[嘘]", "qq034.gif"},
            {"1282", "[晕]", "qq035.gif"},
            {"1239", "[折磨]", "qq036.gif"},
            {"1228", "[衰]", "qq037.gif"},
            {"1219", "[骷髅]", "qq038.gif"},
            {"1242", "[敲打]", "qq039.gif"},
            {"1298", "[再见]", "qq040.gif"},
            // 第二页
            {"1257", "[擦汗]", "qq041.gif"},
            {"1220", "[抠鼻]", "qq042.gif"},
            {"1211", "[鼓掌]", "qq043.gif"},
            {"1213", "[糗大了]", "qq044.gif"},
            {"1264", "[坏笑]", "qq045.gif"},
            {"1224", "[左哼哼]", "qq046.gif"},
            {"1269", "[右哼哼]", "qq047.gif"},
            {"1225", "[哈欠]", "qq048.gif"},
            {"1297", "[鄙视]", "qq049.gif"},
            {"1194", "[委屈]", "qq050.gif"},
            {"1247", "[快哭了]", "qq051.gif"},
            {"1208", "[阴脸]", "qq052.gif"},
            {"1289", "[亲亲]", "qq053.gif"},
            {"1221", "[吓]", "qq054.gif"},
            {"1233", "[可怜]", "qq055.gif"},
            {"1311", "[眨眼睛]", "qq056.gif"},
            {"1300", "[笑哭]", "qq057.gif"},
            {"1304", "[dogeQQ]", "qq058.gif"},
            {"1303", "[泪奔]", "qq059.gif"},
            {"1302", "[无奈]", "qq060.gif"},
            {"1308", "[托腮]", "qq061.gif"},
            {"1284", "[卖萌]", "qq062.png"},
            {"1305", "[斜眼笑]", "qq063.gif"},
            {"1306", "[喷血]", "qq064.gif"},
            {"1310", "[惊喜]", "qq065.gif"},
            {"1309", "[骚扰]", "qq066.gif"},
            {"1307", "[小纠结]", "qq067.gif"},
            {"1301", "[我最美]", "qq068.gif"},
            {"1202", "[菜刀]", "qq069.gif"},
            {"1254", "[西瓜]", "qq070.gif"},
            {"1241", "[啤酒]", "qq071.gif"},
            {"1245", "[篮球]", "qq072.gif"},
            {"1207", "[乒乓]", "qq073.gif"},
            {"1280", "[咖啡]", "qq074.gif"},
            {"1199", "[饭]", "qq075.gif"},
            {"1226", "[猪]", "qq076.gif"},
            {"1271", "[玫瑰]", "qq077.gif"},
            {"1236", "[凋谢]", "qq078.gif"},
            {"1244", "[示爱]", "qq079.gif"},
            {"1201", "[爱心]", "qq080.gif"},
            // 第三页
            {"1250", "[心碎]", "qq081.gif"},
            {"1276", "[蛋糕]", "qq082.gif"},
            {"1193", "[闪电]", "qq083.gif"},
            {"1288", "[炸弹]", "qq084.gif"},
            {"1299", "[刀]", "qq085.gif"},
            {"1294", "[足球]", "qq086.gif"},
            {"1258", "[瓢虫]", "qq087.gif"},
            {"1266", "[便便]", "qq088.gif"},
            {"1204", "[月亮]", "qq089.gif"},
            {"1203", "[太阳]", "qq090.gif"},
            {"1274", "[礼物]", "qq091.gif"},
            {"1261", "[抱抱]", "qq092.gif"},
            {"1227", "[喝彩]", "qq93.gif"},
            {"1281", "[祈祷]", "qq94.gif"},
            {"1230", "[棒棒糖]", "qq95.gif"},
            {"1278", "[药]", "qq96.gif"},
            {"1232", "[赞]", "qq097.gif"},
            {"1290", "[差劲]", "qq098.gif"},
            {"1210", "[握手]", "qq099.gif"},
            {"1275", "[胜利]", "qq100.gif"},
            {"1256", "[抱拳]", "qq101.gif"},
            {"1234", "[勾引]", "qq102.gif"},
            {"1216", "[拳头]", "qq103.gif"},
            {"1231", "[差劲]", "qq104.gif"},
            {"1293", "[爱你]", "qq105.gif"},
            {"1246", "[NO]", "qq106.gif"},
            {"1200", "[OK]", "qq107.gif"}
        };
        
        for (String[] data : qqSmileys) {
            String id = data[0];
            String code = data[1];
            String filename = data[2];
            String url = baseUrl + filename;
            String name = code.replace("[", "").replace("]", "");
            smileys.add(new Smiley(id, name, url, code, category, categoryId));
        }
        
        return smileys;
    }
    
    /**
     * 获取淘宝(滑稽)表情列表（完整69个）
     * 分类ID: 5, 目录: comiis_tb
     */
    public static List<Smiley> getTbSmileys() {
        List<Smiley> smileys = new ArrayList<>();
        String baseUrl = CDN_BASE + "comiis_tb/";
        String category = "滑稽";
        int categoryId = CATEGORY_TB;
        
        // 淘宝表情数据: [id, code, filename]
        // 第一页 (40个)
        String[][] tbSmileys1 = {
            {"244", "[#呵呵]", "tb_1.png"},
            {"250", "[#滑稽]", "tb_10.png"},
            {"253", "[#哈哈]", "tb_2.png"},
            {"289", "[#吐舌]", "tb_3.png"},
            {"229", "[#啊]", "tb_23.png"},
            {"263", "[#酷]", "tb_22.png"},
            {"272", "[#怒]", "tb_13.png"},
            {"240", "[#开心]", "tb_39.png"},
            {"269", "[#汗]", "tb_14.png"},
            {"261", "[#泪]", "tb_16.png"},
            {"294", "[#黑线]", "tb_15.png"},
            {"271", "[#鄙视]", "tb_21.png"},
            {"252", "[#不高兴]", "tb_12.png"},
            {"248", "[#真棒]", "tb_17.png"},
            {"243", "[#钱]", "tb_40.png"},
            {"258", "[#疑问]", "tb_26.png"},
            {"228", "[#阴险]", "tb_20.png"},
            {"281", "[#吐]", "tb_34.png"},
            {"270", "[#咦]", "tb_41.png"},
            {"278", "[#委屈]", "tb_29.png"},
            {"245", "[#花心]", "tb_6.png"},
            {"232", "[#呼～]", "tb_42.png"},
            {"257", "[#激动]", "tb_5.png"},
            {"275", "[#冷]", "tb_43.png"},
            {"251", "[#可爱]", "tb_4.png"},
            {"267", "[#What？]", "tb_25.png"},
            {"242", "[#勉强]", "tb_38.png"},
            {"259", "[#酸爽]", "tb_27.png"},
            {"265", "[#狂汗]", "tb_24.png"},
            {"249", "[#雅美蝶]", "tb_28.png"},
            {"284", "[#乖]", "tb_8.png"},
            {"254", "[#睡觉]", "tb_31.png"},
            {"247", "[#惊哭]", "tb_19.png"},
            {"255", "[#哼]", "tb_44.png"},
            {"236", "[#笑尿]", "tb_32.png"},
            {"283", "[#惊讶]", "tb_30.png"},
            {"230", "[#小乖]", "tb_7.png"},
            {"237", "[#喷]", "tb_18.png"},
            {"279", "[#抠鼻]", "tb_33.png"},
            {"286", "[#捂嘴笑]", "tb_9.png"}
        };
        
        // 第二页 (29个)
        String[][] tbSmileys2 = {
            {"234", "[#犀利]", "tb_35.png"},
            {"276", "[#你懂的]", "tb_11.png"},
            {"274", "[#小红脸]", "tb_36.png"},
            {"241", "[#懒得理]", "tb_37.png"},
            {"290", "[#爱心]", "tb_45.png"},
            {"262", "[#心碎]", "tb_46.png"},
            {"233", "[#玫瑰]", "tb_47.png"},
            {"227", "[#礼物]", "tb_48.png"},
            {"293", "[#彩虹]", "tb_49.png"},
            {"226", "[#太阳]", "tb_50.png"},
            {"282", "[#月亮]", "tb_51.png"},
            {"268", "[#钱币]", "tb_52.png"},
            {"266", "[#咖啡]", "tb_53.png"},
            {"277", "[#蛋糕]", "tb_54.png"},
            {"264", "[#大拇指]", "tb_55.png"},
            {"273", "[#胜利]", "tb_56.png"},
            {"287", "[#爱你]", "tb_57.png"},
            {"288", "[#OK]", "tb_58.png"},
            {"246", "[#弱]", "tb_59.png"},
            {"231", "[#沙发]", "tb_60.png"},
            {"235", "[#纸巾]", "tb_61.png"},
            {"239", "[#香蕉]", "tb_62.png"},
            {"238", "[#便便]", "tb_63.png"},
            {"256", "[#药丸]", "tb_64.png"},
            {"291", "[#红领巾]", "tb_65.png"},
            {"280", "[#蜡烛]", "tb_66.png"},
            {"285", "[#三道杠]", "tb_67.png"},
            {"260", "[#音乐]", "tb_68.png"},
            {"292", "[#灯泡]", "tb_69.png"}
        };
        
        for (String[] data : tbSmileys1) {
            String id = data[0];
            String code = data[1];
            String filename = data[2];
            String url = baseUrl + filename;
            String name = code.replace("[#", "").replace("]", "");
            smileys.add(new Smiley(id, name, url, code, category, categoryId));
        }
        
        for (String[] data : tbSmileys2) {
            String id = data[0];
            String code = data[1];
            String filename = data[2];
            String url = baseUrl + filename;
            String name = code.replace("[#", "").replace("]", "");
            smileys.add(new Smiley(id, name, url, code, category, categoryId));
        }
        
        return smileys;
    }
    
    /**
     * 获取Doge表情列表（完整80个）
     * 分类ID: 14, 目录: doge
     */
    public static List<Smiley> getDogeSmileys() {
        List<Smiley> smileys = new ArrayList<>();
        String baseUrl = CDN_BASE + "doge/";
        String category = "Doge";
        int categoryId = CATEGORY_DOGE;
        
        // Doge表情数据: [id, code, filename]
        // 第一页 (40个)
        String[][] dogeSmileys1 = {
            {"1392", "[doge]", "1.png"},
            {"1416", "[doge思考]", "2.png"},
            {"1403", "[doge再见]", "3.png"},
            {"1351", "[doge生气]", "4.png"},
            {"1370", "[doge气哭]", "5.png"},
            {"1359", "[doge笑哭]", "7.png"},
            {"1362", "[doge调皮]", "6.png"},
            {"1344", "[doge啊哈]", "8.png"},
            {"1342", "[doge原谅TA]", "9.png"},
            {"1375", "[miao]", "10.png"},
            {"1373", "[miao思考]", "11.png"},
            {"1420", "[miao拜拜]", "12.png"},
            {"1390", "[miao生气]", "13.png"},
            {"1383", "[miao气哭]", "14.png"},
            {"1349", "[二哈]", "15.png"},
            {"1408", "[摊手]", "19.png"},
            {"1414", "[w并不简单]", "20.png"},
            {"1382", "[w滑稽]", "21.png"},
            {"1417", "[w色]", "22.png"},
            {"1423", "[w爱你]", "23.png"},
            {"1407", "[w拜拜]", "24.png"},
            {"1355", "[w悲伤]", "25.png"},
            {"1393", "[w鄙视]", "26.png"},
            {"1365", "[w馋嘴]", "27.png"},
            {"1357", "[w冷汗]", "28.png"},
            {"1360", "[w打哈欠]", "29.png"},
            {"1402", "[w打脸]", "30.png"},
            {"1398", "[w敲打]", "31.png"},
            {"1394", "[w生病]", "32.png"},
            {"1399", "[w闭嘴]", "33.png"},
            {"1419", "[w鼓掌]", "34.png"},
            {"1384", "[w哈哈]", "35.png"},
            {"1391", "[w害羞]", "36.png"},
            {"1421", "[w呵呵]", "37.png"},
            {"1381", "[w黑线]", "38.png"},
            {"1352", "[w哼哼]", "39.png"},
            {"1374", "[w调皮]", "40.png"},
            {"1386", "[w可爱]", "41.png"},
            {"1358", "[w可怜]", "42.png"},
            {"1413", "[w酷]", "43.png"}
        };
        
        // 第二页 (40个)
        String[][] dogeSmileys2 = {
            {"1346", "[w困]", "44.png"},
            {"1343", "[w懒得理你]", "45.png"},
            {"1401", "[w流泪]", "46.png"},
            {"1348", "[w怒]", "47.png"},
            {"1371", "[w怒骂]", "48.png"},
            {"1347", "[w钱]", "49.png"},
            {"1410", "[w亲亲]", "50.png"},
            {"1426", "[w傻眼]", "51.png"},
            {"1412", "[w便秘]", "52.png"},
            {"1364", "[w失望]", "53.png"},
            {"1353", "[w衰]", "54.png"},
            {"1345", "[w睡觉]", "55.png"},
            {"1378", "[w思考]", "56.png"},
            {"1388", "[w开心]", "57.png"},
            {"1361", "[w色舔]", "58.png"},
            {"1366", "[w偷笑]", "59.png"},
            {"1425", "[w吐]", "60.png"},
            {"1367", "[w抠鼻]", "61.png"},
            {"1368", "[w委屈]", "62.png"},
            {"1376", "[w笑哭]", "63.png"},
            {"1411", "[w嘻嘻]", "64.png"},
            {"1354", "[w嘘]", "65.png"},
            {"1385", "[w阴险]", "66.png"},
            {"1405", "[w疑问]", "67.png"},
            {"1415", "[w抓狂]", "70.png"},
            {"1372", "[w晕]", "69.png"},
            {"1363", "[w右哼哼]", "68.png"},
            {"1380", "[w左哼哼]", "71.png"},
            {"1406", "[w肥皂]", "77.png"},
            {"1387", "[w奥特曼]", "78.png"},
            {"1395", "[w草泥马]", "79.png"},
            {"1377", "[w兔子]", "80.png"},
            {"1424", "[w熊猫]", "81.png"},
            {"1404", "[w猪头]", "82.png"},
            {"1379", "[w→_→]", "83.png"},
            {"1400", "[w给力]", "84.png"},
            {"1409", "[w囧]", "85.png"},
            {"1350", "[w萌]", "86.png"},
            {"1389", "[w神马]", "87.png"},
            {"1356", "[w威武]", "88.png"}
        };
        
        for (String[] data : dogeSmileys1) {
            String id = data[0];
            String code = data[1];
            String filename = data[2];
            String url = baseUrl + filename;
            String name = code.replace("[", "").replace("]", "");
            smileys.add(new Smiley(id, name, url, code, category, categoryId));
        }
        
        for (String[] data : dogeSmileys2) {
            String id = data[0];
            String code = data[1];
            String filename = data[2];
            String url = baseUrl + filename;
            String name = code.replace("[", "").replace("]", "");
            smileys.add(new Smiley(id, name, url, code, category, categoryId));
        }
        
        return smileys;
    }
    
    /**
     * 根据分类ID获取表情列表
     */
    public static List<Smiley> getSmileysByCategory(int categoryId) {
        switch (categoryId) {
            case CATEGORY_QQ:
                return getQQSmileys();
            case CATEGORY_TB:
                return getTbSmileys();
            case CATEGORY_DOGE:
                return getDogeSmileys();
            default:
                return getTbSmileys(); // 默认返回淘宝表情
        }
    }
    
    /**
     * 获取所有表情分类
     */
    public static List<SmileyCategory> getCategories() {
        List<SmileyCategory> categories = new ArrayList<>();
        categories.add(new SmileyCategory(CATEGORY_TB, "滑稽", "淘宝表情"));
        categories.add(new SmileyCategory(CATEGORY_QQ, "默认", "QQ表情"));
        categories.add(new SmileyCategory(CATEGORY_DOGE, "Doge", "Doge表情"));
        return categories;
    }
    
    /**
     * 表情分类模型
     */
    public static class SmileyCategory {
        private int id;
        private String name;
        private String description;
        
        public SmileyCategory(int id, String name, String description) {
            this.id = id;
            this.name = name;
            this.description = description;
        }
        
        public int getId() {
            return id;
        }
        
        public String getName() {
            return name;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    /**
     * 获取MT论坛表情列表（兼容旧代码）
     * 返回淘宝表情
     */
    public static List<Smiley> getMtForumSmileys() {
        return getTbSmileys();
    }
    
    /**
     * 获取所有表情（合并所有分类）
     */
    public static List<Smiley> getAllSmileys() {
        List<Smiley> all = new ArrayList<>();
        all.addAll(getTbSmileys());  // 默认显示淘宝表情
        return all;
    }
}
