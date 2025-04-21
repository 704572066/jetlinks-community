package org.jetlinks.community.video.service;


import com.alibaba.fastjson.JSONObject;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.community.video.entity.TokenEntity;
import org.jetlinks.community.video.entity.VideoDeviceInstanceEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;


@Service
@Slf4j
public class TokenService extends GenericReactiveCrudService<TokenEntity, String> {

    private final WebClient webClient = WebClient.create();
//    public Flux<VideoDeviceInstanceEntity> findByDeviceId(String deviceId) {
//        return createQuery()
//            .and(VideoDeviceInstanceEntity::getId, deviceId)
//            .fetch();
//    }

    public Mono<String> getYSDeviceTrustToken(String access_token, String auth_code) {
//        Token token = null;
        String token_url = "https://open.ys7.com/api/lapp/trust/device/token/get?access_token=ACCESS_TOKEN&auth_code=AUTH_CODE";
        String url = token_url.replace("ACCESS_TOKEN", access_token).replace("AUTH_CODE", auth_code);
        // 使用 WebClient 发起异步 HTTP GET 请求
//        return webClient.get()
//                        .uri(url)
//                        .retrieve()
//                        .bodyToMono(JSONObject.class) // 将响应体解析为 JSONObject
//                        .flatMap(obj -> {
//                            access_token = obj.getString("access_token"); // 提取 unionid
//                            if (access_token != null) {
//                                return Mono.just(access_token); // 正常返回 unionid
//                            } else {
//                                return Mono.error(new RuntimeException("access_token 不存在")); // 返回错误
//                            }
//                        });
        return Mono.just("ff");
        // 发起GET请求获取凭证
//        JSONObject jsonObject = httpsRequest(requestUrl, "GET", null);
//
//        if (null != jsonObject) {
//            try {
//                token = new Token();
//                token.setAccessToken(jsonObject.getString("access_token"));
//                token.setExpiresIn(jsonObject.getInt("expires_in"));
//            } catch (JSONException e) {
//                token = null;
//                // 获取token失败
//                log.error("获取token失败 errcode:{} errmsg:{}", jsonObject.getInt("errcode"), jsonObject.getString("errmsg"));
//            }
//        }
//        return token;
    }








}
