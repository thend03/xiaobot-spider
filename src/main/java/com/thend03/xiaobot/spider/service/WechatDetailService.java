package com.thend03.xiaobot.spider.service;

import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.http.HttpUtil;
import com.thend03.xiaobot.spider.constants.Constants;
import com.thend03.xiaobot.spider.model.ArticleDetail;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.*;
import org.jsoup.select.Elements;
import org.openqa.selenium.WebDriver;
import org.openqa.selenium.chrome.ChromeDriver;
import org.openqa.selenium.chrome.ChromeOptions;
import java.io.File;
import java.io.IOException;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.stream.Collectors;

/**
 * 获取专栏推荐详情
 *
 * @author since
 * @date 2024-07-17 08:22
 */
public class WechatDetailService {

    public static final AtomicInteger COUNT = new AtomicInteger(1);

    public static List<String> getUrlList(String url) {
        List<String> strings = parseImageUrlList(url);
        return strings.stream().filter(StringUtils::isNotBlank).map(s -> {

            try {
                return hutoolOcr(s);
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }

        }).collect(Collectors.toList());
    }

    public static List<String> parseImageUrlList(String wechatUrl) {
        if (StringUtils.isBlank(wechatUrl)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        String content = HttpUtil.downloadString(wechatUrl, StandardCharsets.UTF_8);
        Document parse = Jsoup.parse(content);
        Elements scripts = parse.getElementsByTag("script");
        for (Element script : scripts) {
            List<Node> nodes = script.childNodes();
            for (Node node : nodes) {
                if (node instanceof DataNode) {
                    DataNode dataNode = (DataNode) node;
                    String wholeData = dataNode.getWholeData();
                    if (wholeData.contains("cdn_url")) {
                        String[] split = wholeData.split("var");
                        for (int i = 0; i < split.length; i++) {
                            String s = split[i];
                            if (s.contains("picturePageInfoList") && s.contains("http")) {
                                String[] split1 = s.split("picturePageInfoList");
                                String cdnUrl = split1[1];
                                String s1 = cdnUrl.replaceAll("\\[", "")
                                        .replaceAll("]", "")
                                        .replaceAll("\\{", "")
                                        .replaceAll("}", "")
                                        .replaceAll("'", "")
                                        .replaceAll("\"", "");
                                String[] split2 = s1.split(",");
                                for (int j = 0; j < split2.length; j++) {
                                    if (split2[j].contains("cdn_url")) {
                                        String[] split3 = split2[j].split("cdn_url:");
                                        result.add(split3[1]);
                                    }
                                }
                            }
                        }
                    }
                }
            }
        }
        System.out.println("end#####");
        return result;
    }

    public static String hutoolOcr(String imageUrl) throws IOException, URISyntaxException {
        String finalUrl = "";
        File file = HttpUtil.downloadFileFromUrl(imageUrl, "static/" + COUNT.getAndIncrement() + ".png");
        try {
            finalUrl = QrCodeUtil.decode(file);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return finalUrl;
    }

    public static ArticleDetail getArticleDetail(String host, String uniqueId) {
        String url = "https://" + host + "/p/" + uniqueId;

        String articleFullPage = getArticleFullPage(url);
        Document document = Jsoup.parse(articleFullPage);
        Elements title = document.getElementsByTag("title");
        Elements intro = document.getElementsByClass("intro");
        Elements description = document.getElementsByClass("description");
        Elements posts = document.getElementsByClass("posts");
        Elements elementsByClass = document.getElementsByClass("paper");
        Elements num = document.getElementsByClass("num");
        Elements avatar = document.getElementsByClass("avatar");

        String articleTitle = getArticleTitle(title);
        String articleDescription = getArticleDescription(description);
        String articleIntro = getArticleIntro(intro);
        Map<String, Integer> articleNum = getArticleNum(num);
        String articleAvatar = getArticleAvatar(avatar);

        ArticleDetail articleDetail = new ArticleDetail();
        articleDetail.setAvatarUrl(articleAvatar);
//        articleDetail.setGmtCreate(createdAt);
        articleDetail.setIntroduction(articleIntro);
//        articleDetail.setFreePostCount(freePostCount);
        articleDetail.setPostCount(articleNum.get("post"));
        articleDetail.setTitle(articleTitle);
        articleDetail.setSubscriberCount(articleNum.get("reader"));
        articleDetail.setUrl(url);

        System.out.println(1);
        return articleDetail;
    }

    public static String getHost(String articleUrl) throws MalformedURLException {
        java.net.URL url = new URL(articleUrl);
        return url.getHost();
    }

    public static String getArticleUniqueId(String articleUrl) throws MalformedURLException {
        java.net.URL url = new URL(articleUrl);
        String path = url.getPath();

        return path.replaceAll("/p/", "");
    }


    public static String getArticleFullPage(String url) {
        // 设置 ChromeDriver 路径
        System.setProperty("webdriver.chrome.driver", Constants.LOCAL_CHROME_DRIVER_PATH);

        // 设置 Chrome 选项
        ChromeOptions options = new ChromeOptions();
        options.addArguments("--headless"); // 无头模式

        // 启动 Chrome 浏览器
        WebDriver driver = new ChromeDriver(options);

        // 访问目标网页
        driver.get(url);

        String pageContent = null;

        // 等待网页加载完成（根据需要调整等待时间）
        try {
            Thread.sleep(2000);
            // 获取网页内容
            pageContent = driver.getPageSource();

            // 打印网页内容
            System.out.println(pageContent);
        } catch (InterruptedException e) {
            e.printStackTrace();
        } finally {
            driver.quit();
        }
        return pageContent;
    }

    /**
     * 专栏的标题
     * @param title title element
     * @return title
     */
    public static String getArticleTitle(Elements title) {
        if (Objects.isNull(title)) {
            return null;
        }
        if (title.isEmpty()) {
            return null;
        }
        List<String> result = new ArrayList<>();
        for (Element element : title) {
            List<Node> nodes = element.childNodes();
            if (CollectionUtils.isEmpty(nodes)) {
                continue;
            }
            List<String> collect = nodes.stream().filter(Objects::nonNull).map(node -> {
                if (node instanceof TextNode) {
                    return ((TextNode) node).text();
                }
                return null;
            }).collect(Collectors.toList());
            result.addAll(collect);
        }
        return StringUtils.join(result, ",");
    }

    public static String getArticleDescription(Elements description) {
        if (Objects.isNull(description)) {
            return null;
        }
        if (description.isEmpty()) {
            return null;
        }
        List<String> result = new ArrayList<>();
        Iterator<Element> iterator = description.iterator();
        while (iterator.hasNext()) {
            Element next = iterator.next();
            List<String> list = parseNode(next);
            result.add(StringUtils.join(list, ""));
        }
        String join = StringUtils.join(result, "");
        return join.replaceAll("<p></p>", "\r\n\n");
    }

    public static String getArticleIntro(Elements intro) {
        if (Objects.isNull(intro)) {
            return null;
        }
        if (intro.isEmpty()) {
            return null;
        }
        List<String> result = new ArrayList<>();
        for (Element next : intro) {
            List<String> list = parseNode(next);
            result.add(StringUtils.join(list, ""));
        }
        String join = StringUtils.join(result, "");
        return join.replaceAll("<br>", "\r\n");
    }

    public static Map<String, Integer> getArticleNum(Elements num) {
        if (Objects.isNull(num)) {
            return Collections.emptyMap();
        }
        if (num.isEmpty()) {
            return Collections.emptyMap();
        }
        Element reader = num.get(0);
        Element post = num.get(1);

        TextNode node = (TextNode) reader.childNodes().get(0);
        String text = node.text();


        TextNode postNode = (TextNode) post.childNodes().get(0);
        String text1 = postNode.text();
        Map<String, Integer> map = new HashMap<>(4);
        map.put("reader", Integer.parseInt(text));
        map.put("post", Integer.parseInt(text1));
        return map;
    }

    public static String getArticleAvatar(Elements avatar) {
        if (Objects.isNull(avatar)) {
            return null;
        }
        if (avatar.isEmpty()) {
            return null;
        }
        Element element = avatar.get(0);
        return element.attributes().get("src");
    }

    /**
     * 解析node详情，需要递归，text node/element
     * @param element element
     * @return list
     */
    public static List<String> parseNode(Element element) {
        if (Objects.isNull(element)) {
            return null;
        }
        List<Node> nodes = element.childNodes();
        if (CollectionUtils.isEmpty(nodes)) {
            return null;
        }
        List<String> result = new ArrayList<>();
        for (Node node : nodes) {
            List<Node> textNodeList = node.childNodes();
            if (CollectionUtils.isEmpty(textNodeList)) {
                if (node instanceof TextNode) {
                    result.add(((TextNode) node).text());
                } else {
                    result.add(node.toString());
                }
                continue;
            }
            List<String> subList = new ArrayList<>();
            for (Node text : textNodeList) {
                if (Objects.isNull(text)) {
                    continue;
                }
                if (text instanceof TextNode) {
                    subList.add(((TextNode) text).text());
                } else if (text instanceof Element) {
                    List<String> list = parseNode((Element) text);
                    if (CollectionUtils.isNotEmpty(list)) {
                        subList.add(StringUtils.join(list, ""));
                    }
                } else {
                    subList.add(text.toString());
                }
            }
            result.add(StringUtils.join(subList, ""));
        }
        return result;
    }
}
