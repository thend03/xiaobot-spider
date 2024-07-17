package com.thend03.xiaobot.spider;

import com.alibaba.fastjson2.JSON;
import com.thend03.xiaobot.spider.model.ArticleDetail;
import com.thend03.xiaobot.spider.service.WechatSubscribeListService;

import java.util.List;

/**
 * main application
 *
 * @author since
 * @date 2024-07-17 08:09
 */
public class Application {
    public static void main(String[] args) {
        List<ArticleDetail> articleDetails = WechatSubscribeListService.parseArticleList();
        System.out.println(JSON.toJSONString(articleDetails));
    }
}
