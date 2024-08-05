package com.thend03.xiaobot.spider.service;

import cn.hutool.extra.qrcode.QrCodeUtil;
import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
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
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
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

    public static void main(String[] args) {
        String url = "https://mp.weixin.qq.com/s?__biz=Mzg4MTc0MDcyOA==&mid=2247484196&idx=1&sn=7034ef1197b8658d86cef8db1d04f3a9&chksm=cf601df3f81794e5e074a30892c95c54e1b33fcef5cbea7a4590a95f37652e0f890e72265efb&scene=178&cur_album_id=2262723582487429130#rd";
        List<String> urlList = getUrlList(url);
        System.out.println(JSON.toJSONString(urlList));
    }

    public static final AtomicInteger COUNT = new AtomicInteger(1);

    public static List<String> getUrlList(String url) {
        List<String> strings = parseImageUrlList(url);
        return strings.stream().filter(StringUtils::isNotBlank).map(s -> {

            try {
                return hutoolOcr(s);
            } catch (IOException | URISyntaxException e) {
                throw new RuntimeException(e);
            }

        }).filter(StringUtils::isNotBlank).collect(Collectors.toList());
    }

    public static List<String> parseImageUrlList(String wechatUrl) {
        if (StringUtils.isBlank(wechatUrl)) {
            return Collections.emptyList();
        }
        List<String> result = new ArrayList<>();
        String content = HttpUtil.downloadString(wechatUrl, StandardCharsets.UTF_8);
        Document parse = Jsoup.parse(content);

        Elements imageList = parse.select("img[data-src]");
        for (Element image : imageList) {
            String attr = image.attr("data-src");
            if (StringUtils.isNotBlank(attr) && attr.startsWith("http")) {
                result.add(attr);
            }
        }
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
        if (StringUtils.isBlank(finalUrl)) {
            return "";
        }
        if (!finalUrl.startsWith("http")) {
            return "";
        }
        String[] split = finalUrl.split("\\?refer");
        finalUrl = split[0];
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
        articleDetail.setAvatarBase64(articleAvatar);
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
        String driverPath = getDriverPath();
        // 设置 ChromeDriver 路径
        System.setProperty("webdriver.chrome.driver", driverPath);

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
        String avatarUrl = element.attributes().get("src");
        byte[] bytes = HttpUtil.downloadBytes(avatarUrl);
        return "data:image/jpeg;base64," + Base64.getEncoder().encodeToString(bytes);
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

    /**
     * 根据操作系统选择相应的 chromedriver 文件路径
     *
     * @return chromedriver 文件路径
     */
    private static String getDriverPath() {

        String osName = System.getProperty("os.name").toLowerCase();
        String driverFileName;

        String osPath = "";

        if (osName.contains("win")) {
            driverFileName = "chromedriver.exe";
            osPath = "chromedriver-win64";
        } else if (osName.contains("nix") || osName.contains("nux")) {
            driverFileName = "chromedriver";
            osPath = "chromedriver-linux64";
        } else if (osName.contains("mac")) {
            driverFileName = "chromedriver";
            osPath = "chromedriver-mac-arm64";
        } else {
            throw new UnsupportedOperationException("Unsupported operating system: " + osName);
        }

        // 从类路径加载驱动程序文件
        try (InputStream in = WechatDetailService.class.getResourceAsStream("/chromedriver/" + osPath + "/" + driverFileName)) {
            if (in == null) {
                throw new IOException("Driver not found in classpath");
            }
            // 创建临时文件
            Path tempFile = Files.createTempFile(null, null);
            tempFile.toFile().deleteOnExit();
            try (FileOutputStream out = new FileOutputStream(tempFile.toFile())) {
                byte[] buffer = new byte[1024];
                int bytesRead;
                while ((bytesRead = in.read(buffer)) != -1) {
                    out.write(buffer, 0, bytesRead);
                }
            }
            // 设置临时文件为可执行
            File file = tempFile.toFile();
            if (osName.contains("nix") || osName.contains("nux") || osName.contains("mac")) {
                file.setExecutable(true);
            }
            return file.getAbsolutePath();
        } catch (IOException e) {
            throw new RuntimeException("Failed to load driver", e);
        }
    }
}
