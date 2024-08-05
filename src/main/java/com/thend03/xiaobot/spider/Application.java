package com.thend03.xiaobot.spider;

import com.alibaba.fastjson2.JSON;
import com.thend03.xiaobot.spider.model.ArticleDetail;
import com.thend03.xiaobot.spider.service.WechatSubscribeListService;
import org.apache.commons.io.FileUtils;

import java.io.File;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;

/**
 * main application
 *
 * @author since
 * @date 2024-07-17 08:09
 */
public class Application {
    public static void main(String[] args) throws IOException {
        List<ArticleDetail> articleDetails = WechatSubscribeListService.parseArticleList();
        FileUtils.writeStringToFile(new File("/tmp/detail.json"),JSON.toJSONString(articleDetails), StandardCharsets.UTF_8);
    }
}
