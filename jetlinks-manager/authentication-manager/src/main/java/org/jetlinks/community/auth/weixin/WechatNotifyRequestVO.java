package org.jetlinks.community.auth.weixin;

import com.thoughtworks.xstream.annotations.XStreamAlias;

import io.swagger.annotations.ApiModelProperty;

import lombok.Data;

import lombok.experimental.Accessors;

/**

 * 详情见微信官方文档

 * https://developers.weixin.qq.com/doc/offiaccount/Message_Management/Receiving_event_pushes.html

 */


@Accessors(chain = true)

@XStreamAlias(value = "xml")

public class WechatNotifyRequestVO {
    @ApiModelProperty("开发者微信号")

    @XStreamAlias(value = "ToUserName")

    private String toUserName;

    public String getToUserName() {
        return toUserName;
    }

    public void setToUserName(String toUserName) {
        this.toUserName = toUserName;
    }

    public String getFromUserName() {
        return fromUserName;
    }

    public void setFromUserName(String fromUserName) {
        this.fromUserName = fromUserName;
    }

    public Integer getCreateTime() {
        return createTime;
    }

    public void setCreateTime(Integer createTime) {
        this.createTime = createTime;
    }

    public String getMessageType() {
        return messageType;
    }

    public void setMessageType(String messageType) {
        this.messageType = messageType;
    }

    public String getEvent() {
        return event;
    }

    public void setEvent(String event) {
        this.event = event;
    }

    @ApiModelProperty("发送方帐号(一个OpenID)")

    @XStreamAlias(value = "FromUserName")

    private String fromUserName;

    @ApiModelProperty("消息创建时间 (整型)")

    @XStreamAlias(value = "CreateTime")

    private Integer createTime;

    @ApiModelProperty("消息类型，event")

    @XStreamAlias(value = "MsgType")

    private String messageType;

    @ApiModelProperty("事件类型，subscribe(订阅)、unsubscribe(取消订阅)")

    @XStreamAlias(value = "Event")

    private String event;

}

