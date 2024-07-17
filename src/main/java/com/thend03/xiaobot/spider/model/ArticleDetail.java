package com.thend03.xiaobot.spider.model;

import lombok.Data;

import java.util.Date;

/**
 * article detail
 *
 * @author since
 * @date 2024-07-09 11:18
 */
@Data
public class ArticleDetail {
    /**
     * 专栏地址
     */
    private String url;

    /**
     * 文章标题
     */
    private String title;

    /**
     * 文章详情
     */
    private DescriptionInfo description;

    /**
     * 专栏创建时间
     */
    private Date gmtCreate;

    /**
     * 专栏介绍
     */
    private String introduction;

    /**
     * 专栏头像地址
     */
    private String avatarBase64;

    /**
     * 免费文章数量
     */
    private Integer freePostCount;

    /**
     * 专栏文章数量
     */
    private Integer postCount;

//    private List<PinnedInfo> pinnedInfoList;

    /**
     * 订阅数量
     */
    private Integer subscriberCount;

    /**
     * 订阅
     */
    private String subscription;


}
