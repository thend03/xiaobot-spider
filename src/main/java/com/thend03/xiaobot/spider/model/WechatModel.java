package com.thend03.xiaobot.spider.model;

import lombok.Data;

import java.io.Serializable;

/**
 * wechat model
 *
 * @author since
 * @date 2024-07-01 14:17
 */
@Data
public class WechatModel implements Serializable {
    private Long dataMsgId;
    private String dataTitle;
    private Integer dataItemIndex;
    private String dataLink;
    private Integer dataPosNum;
}
