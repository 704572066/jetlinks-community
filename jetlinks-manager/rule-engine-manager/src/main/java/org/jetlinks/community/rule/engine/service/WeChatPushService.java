package org.jetlinks.community.rule.engine.service;

import com.alibaba.fastjson.JSONObject;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.rule.engine.entity.UniPushEntity;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Component
@Slf4j
public class WeChatPushService {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
//    private final LocalDeviceInstanceService instanceService;

    public WeChatPushService() {
        this.webClient = WebClient.builder().build();  // 创建 WebClient 实例
        this.objectMapper = new ObjectMapper();        // 创建 ObjectMapper 实例
//        this.localDeviceInstanceService = new LocalDeviceInstanceService();
    }

    public Mono<Void> sendPostRequest(String openId, String alarmTime, String sourceName, String sourceId, String alarmName) {

        Map<String, String> data = new HashMap<>();
        data.put("first", "设备告警通知");
        data.put("time3", alarmTime);
        data.put("character_string11", sourceId);
        data.put("thing33", alarmName);
        data.put("thing23", sourceName);
        data.put("remark", "点击查看详情");
//        Map<String, String> miniprogram = new HashMap<>();
//        miniprogram.put("appid", "wxe861fc6383450e16");

        String messageJson = buildTemplateMessage(openId, "A9sE1bUz-rXKhuGXferRKX42E6a7reQaaYccLmAbDOU", "", data);
//            String result = weChat.sendTemplateMessage(accessToken, messageJson);
        return getToken("wx22e05e45ecb42885", "da4f9951b24d2bdf03439a26f68efb76")
        .flatMap(token->{
            log.warn("token: "+token);
            log.warn("messageJson: "+messageJson);
            String url = "https://api.weixin.qq.com/cgi-bin/message/template/send?access_token=" + token;
            return webClient.post()
                            .uri(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(messageJson)
                            .retrieve()
                            .bodyToMono(Void.class) // 不处理响应，返回 Mono<Void>
                            .doOnError(error -> log.error("Error occurred: {}", error.getMessage()));

        });

    }

    public Mono<String> getToken(String appid, String appsecret) {
//        Token token = null;
        String token_url = "https://api.weixin.qq.com/cgi-bin/token?grant_type=client_credential&appid=APPID&secret=APPSECRET";
        String url = token_url.replace("APPID", appid).replace("APPSECRET", appsecret);
        // 使用 WebClient 发起异步 HTTP GET 请求
        return webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(JSONObject.class) // 将响应体解析为 JSONObject
                        .flatMap(obj -> {
                            String access_token = obj.getString("access_token"); // 提取 unionid
                            if (access_token != null) {
                                return Mono.just(access_token); // 正常返回 unionid
                            } else {
                                return Mono.error(new RuntimeException("access_token 不存在")); // 返回错误
                            }
                        });
    }

    public String buildTemplateMessage(String toUser, String templateId, String url, Map<String, String> data) {
        JSONObject messageJson = new JSONObject();
        messageJson.put("touser", toUser);
        messageJson.put("template_id", templateId);
        messageJson.put("url", url);

        JSONObject dataJson = new JSONObject();
        for (Map.Entry<String, String> entry : data.entrySet()) {
            JSONObject valueJson = new JSONObject();
            valueJson.put("value", entry.getValue());
            valueJson.put("color", "#173177");  // 默认颜色，可以自定义
            dataJson.put(entry.getKey(), valueJson);
        }

        JSONObject miniprogram = new JSONObject();
        miniprogram.put("appid","wxe861fc6383450e16");


        messageJson.put("data", dataJson);
        messageJson.put("miniprogram", miniprogram);
        return messageJson.toString();
    }

}
