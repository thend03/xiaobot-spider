package com.thend03.xiaobot.spider.service;

import cn.hutool.http.HttpUtil;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.thend03.xiaobot.spider.constants.Constants;
import com.thend03.xiaobot.spider.model.ArticleDetail;
import com.thend03.xiaobot.spider.model.WechatModel;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.lang3.StringUtils;
import org.jsoup.Jsoup;
import org.jsoup.nodes.Attributes;
import org.jsoup.nodes.Document;
import org.jsoup.nodes.Element;
import org.jsoup.select.Elements;

import java.net.MalformedURLException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;

/**
 * 小报童专栏精选订阅号列表解析服务
 *
 * @author since
 * @date 2024-07-17 08:12
 */
public class WechatSubscribeListService {

    public static List<ArticleDetail> parseArticleList() {

        String content = HttpUtil.downloadString(Constants.XIAOBOT_WECHAT_SUBSCRIBE_LIST_URL, StandardCharsets.UTF_8);

        Document parse = Jsoup.parse(content);
        Elements albumListItem = parse.getElementsByClass("album__list-item");
        System.out.println(1);
        List<WechatModel> list = new ArrayList<>();
        boolean hasNext = true;
        for (Element element : albumListItem) {
            Attributes attributes = element.attributes();
            System.out.println(2);
            if (attributes.isEmpty()) {
                continue;
            }
            String s = attributes.get("data-msgid");
            String s1 = attributes.get("data-itemidx");
            String s2 = attributes.get("data-link");
            String s3 = attributes.get("data-title");
            String s4 = attributes.get("data-pos_num");

            WechatModel wechatModel = new WechatModel();
            wechatModel.setDataMsgId(parseLong(s));
            wechatModel.setDataItemIndex(parseInt(s1));
            wechatModel.setDataLink(s2);
            wechatModel.setDataTitle(s3);
            wechatModel.setDataPosNum(parseInt(s4));
            list.add(wechatModel);
            if (wechatModel.getDataPosNum() == 1) {
                hasNext = false;
            }
        }
        while (hasNext) {
            List<WechatModel> list1 = nextParse(list.get(list.size() - 1));
            if (CollectionUtils.isNotEmpty(list1)) {
                list.addAll(list1);

            }
            if (list1.get(list1.size() - 1).getDataPosNum() == 1) {
                hasNext = false;
            }
        }

        List<String> urlList = new ArrayList<>();
        list.stream().filter(Objects::nonNull).forEach(s -> {
            List<String> urlList1 = WechatDetailService.getUrlList(s.getDataLink());
            if (CollectionUtils.isNotEmpty(urlList1)) {
                urlList.addAll(urlList1);
            }
        });

        List<String> collect = urlList.stream().filter(StringUtils::isNotBlank).filter(s -> s.contains("http") && s.contains("xiaobot.net/p")).map(s -> {
            String[] split = s.split("\\?");
            return split[0];
        }).distinct().collect(Collectors.toList());
        List<ArticleDetail> collect1 = collect.stream().filter(Objects::nonNull).map(url -> {
            String host = null;
            String uniqueId = null;
            try {
                host = WechatDetailService.getHost(url);
            } catch (MalformedURLException e) {


            }
            try {
                uniqueId = WechatDetailService.getArticleUniqueId(url);
            } catch (MalformedURLException e) {
                throw new RuntimeException(e);
            }
            return WechatDetailService.getArticleDetail(host, uniqueId);
        }).collect(Collectors.toList());
        return collect1;
    }

    public static long parseLong(String dataMsgId) {
        try {
            return Long.parseLong(dataMsgId);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static int parseInt(String itemIndex) {
        try {
            return Integer.parseInt(itemIndex);
        } catch (Exception e) {
            e.printStackTrace();
        }
        return -1;
    }

    public static List<WechatModel> nextParse(WechatModel wechatModel) {
        List<WechatModel> list = new ArrayList<>();
        String format = String.format(Constants.LIST_NEXT_URL_FORMAT, wechatModel.getDataMsgId(), wechatModel.getDataItemIndex());
        String content = HttpUtil.downloadString(format, StandardCharsets.UTF_8);
        JSONObject jsonObject = JSON.parseObject(content);
        JSONObject getalbumResp = jsonObject.getJSONObject("getalbum_resp");
        JSONArray articleList = getalbumResp.getJSONArray("article_list");
        for (int i = 0; i < articleList.size(); i++) {
            Object o = articleList.get(i);
            if (o instanceof JSONObject) {
                JSONObject json = (JSONObject) o;
                long msgid = json.getLongValue("msgid");
                int itemidx = json.getIntValue("itemidx");
                int posNum = json.getIntValue("pos_num");
                String title = json.getString("title");
                String link = json.getString("url");
                WechatModel nextWechatModel = new WechatModel();
                nextWechatModel.setDataMsgId(msgid);
                nextWechatModel.setDataItemIndex(itemidx);
                nextWechatModel.setDataLink(link);
                nextWechatModel.setDataTitle(title);
                nextWechatModel.setDataPosNum(posNum);
                list.add(nextWechatModel);
            }
        }
        return list;
    }
}
