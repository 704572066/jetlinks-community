package org.jetlinks.community.auth.weixin;

import com.alibaba.fastjson.JSONObject;
import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.dom4j.DocumentException;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.events.AbstractAuthorizationEvent;
import org.hswebframework.web.authorization.events.AuthorizationDecodeEvent;
import org.hswebframework.web.authorization.events.AuthorizationFailedEvent;
import org.hswebframework.web.authorization.exception.AccessDenyException;
import org.hswebframework.web.authorization.exception.AuthenticationException;
import org.hswebframework.web.id.IDGenerator;
import org.hswebframework.web.id.RandomIdGenerator;
import org.hswebframework.web.logging.RequestInfo;
import org.hswebframework.web.utils.DigestUtils;
import org.jetlinks.community.utils.CryptoUtils;
import org.jetlinks.core.utils.Reactors;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.springframework.context.event.EventListener;
import org.springframework.data.redis.core.ReactiveRedisOperations;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.nio.charset.StandardCharsets;
import java.security.KeyPair;
import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
//import jakarta.servlet.http.HttpServletRequest;
//import jakarta.servlet.http.HttpServletResponse;
import org.jetlinks.community.auth.entity.WeChatSubscribeEntity;
import org.springframework.http.server.reactive.ServerHttpRequest;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.jetlinks.community.auth.service.WeChatSubsribeService;
@RestController
@Authorize(ignore = true)
@RequestMapping
@Tag(name = "微信公众号认证接口")
@Hidden
@AllArgsConstructor
@Slf4j
public class WeiXinVerify {

    private final WebClient webClient = WebClient.create();
    private final WeChatSubsribeService weChatSubsribeService;
    @GetMapping(value = "/verify_wx_token")
    public Mono<Void> verifyWXToken(ServerHttpRequest request, ServerHttpResponse response) throws AesException {
        String msgSignature = request.getQueryParams().getFirst("signature");
        String msgTimestamp = request.getQueryParams().getFirst("timestamp");
        String msgNonce = request.getQueryParams().getFirst("nonce");
        String echostr = request.getQueryParams().getFirst("echostr");
        //下面这个方法里面有自定义的token 需要和服务器配置定义的一致
        if (WXPublicUtils.verifyUrl(msgSignature, msgTimestamp, msgNonce)) {
//            return Mono.just(echostr);
            return response.writeWith(Mono.just(response.bufferFactory().wrap(echostr.getBytes())));
//            return response.writeWith(Mono.just(echostr));
        }
        return null;
    }

    //关注或取消公众号事件
    @PostMapping(value = "/verify_wx_token")
    public Object notify(ServerHttpRequest request, ServerHttpResponse response) {
//        try {
//            request.setCharacterEncoding("UTF-8");
//        } catch (UnsupportedEncodingException e) {
//            e.printStackTrace();
//        }
//        response.setCharacterEncoding("UTF-8");
        return request.getBody().switchIfEmpty(Mono.error(new RuntimeException("Request body is empty")))  // 请求体为空时抛出错误doOnNext(body -> System.out.println("Received body chunk: " + body)) // 调试日志doOnSubscribe(subscription -> System.out.println("getBody subscribed"))
               .map(dataBuffer -> {
                   System.out.println("ff");

                   byte[] bytes = new byte[dataBuffer.readableByteCount()];
                   dataBuffer.read(bytes);
                   System.out.println(new String(bytes, StandardCharsets.UTF_8));
                   return new String(bytes, StandardCharsets.UTF_8);
               }).flatMap(XmlUtil::xmlToMap)
                .flatMap(map -> {
                    System.out.println("微信接收到的消息为:" + map.toString());
                    String fromUserName = map.get("FromUserName");//消息来源用户标识 openid
                    String toUserName = map.get("ToUserName");//消息目的用户标识 公众号id
                    String msgType = map.get("MsgType");//消息类型(event或者text)
                    System.out.println("用户openId:" + fromUserName);
                    System.out.println("公众号:" + toUserName);
                    System.out.println("消息类型为:" + msgType);
                    String eventType = map.get("Event");//事件类型
                    System.out.println(eventType);
        //                String unionid = getUnionid(fromUserName);

                    if ("subscribe".equals(eventType)) {
                        return handleSubscribe(fromUserName);
                    } else if ("unsubscribe".equals(eventType)) {
                        return handleUnsubscribe(fromUserName);
                    } else {
                        return Mono.empty(); // 无需处理其他类型事件
                    }
                }); // 将 XML 转换为 Map;
//        String message = "success";
//        Mono<Map<String, String>> mapMono = XmlUtil.parseXml(request)
//                                                .doOnSuccess(map -> {
////                                                    if (map == null || map.isEmpty()) {
////                                                        System.err.println("解析后的 map 为空");
////                                                    }
//
//                                                    System.out.println(map.get("FromUserName"));
//                                                    System.out.println(map.get("Event"));
//                                                })
//                                                .onErrorResume(e -> {
//                                                    System.err.println("解析 XML 时发生错误: " + e.getMessage());
//                                                    return Mono.empty();
//                                                });
//            //把微信返回的xml信息转义成map
////            Map<String, String> map = null;
////        Mono<Map<String, String>> mapMono = XmlUtil.xmlToMap(request);
//        Mono<Void> result = mapMono.flatMap(map -> {
//            System.out.println("微信接收到的消息为:" + map.toString());
//            String fromUserName = map.get("FromUserName");//消息来源用户标识 openid
//            String toUserName = map.get("ToUserName");//消息目的用户标识 公众号id
//            String msgType = map.get("MsgType");//消息类型(event或者text)
//            System.out.println("用户openId:" + fromUserName);
//            System.out.println("公众号:" + toUserName);
//            System.out.println("消息类型为:" + msgType);
//            String eventType = map.get("Event");//事件类型
//            System.out.println(eventType);
////                String unionid = getUnionid(fromUserName);
//
//            if ("subscribe".equals(eventType)) {
//                return handleSubscribe(fromUserName);
//            } else if ("unsubscribe".equals(eventType)) {
//                return handleUnsubscribe(fromUserName);
//            } else {
//                return Mono.empty(); // 无需处理其他类型事件
//            }
//        });
//
//        // 订阅执行
//        result.subscribe(
//            unused -> System.out.println("流程处理完成"),
//            error -> System.err.println("流程处理出错: " + error.getMessage())
//        );


//        System.out.println("关注微信公众号自动回复的消息内容为:"+message);
//        return message;
    }


    /*
     *这边返回出去的是 公众号的unionid
     **/
//    public static String getUnionid(String openId) {
//        String requestUrl = "https://api.weixin.qq.com/cgi-bin/user/info?access_token=ACCESS_TOKEN&openid=OPENID";
//
//        String url = requestUrl.replace("OPENID", openId).replace("ACCESS_TOKEN", "你自己的公众号token");
//        JSONObject obj = WeixinUtil.httpGet(url);
////        String nickName = (String)obj.get("nickname");
//        String unionid = (String)obj.get("unionid");
//        return unionid;
//    }


    // 处理订阅事件的方法
    private Mono<Void> handleSubscribe(String fromUserName) {
        return getToken("wx22e05e45ecb42885","da4f9951b24d2bdf03439a26f68efb76").flatMap(token->getUnionid(fromUserName,token)
            .flatMap(unionid -> {
                System.out.println("Unionid: " + unionid);
                return weChatSubsribeService.createQuery()
                                            .where(WeChatSubscribeEntity::getId, fromUserName)
                                            .fetchOne()
                                            .flatMap(entity -> {
//                                                if (entity == null) {
//                                                    // 如果实体不存在，创建新实体
//                                                    System.out.println("该用户不存在");
//                                                    WeChatSubscribeEntity newEntity = new WeChatSubscribeEntity();
//                                                    newEntity.setOpenid(fromUserName);
//                                                    newEntity.setStatus(1);
//                                                    newEntity.setUinonid(unionid);
//                                                    return weChatSubsribeService.save(newEntity);
//                                                } else {
                                                    // 如果实体已存在，更新状态
                                                    System.out.println("已存在该用户");
                                                    return weChatSubsribeService.createUpdate()
                                                                                .set(WeChatSubscribeEntity::getStatus, 1)
                                                                                .where(WeChatSubscribeEntity::getId, fromUserName)
                                                                                .execute();
//                                                }
                                            })
                                            .switchIfEmpty(Mono.defer(() -> {
                                                // 如果未找到结果，创建一个新实体
                                                System.out.println("Entity not found, creating a new one");
                                                WeChatSubscribeEntity newEntity = new WeChatSubscribeEntity();
//                                                newEntity.setOpenid(openid);
//                                                newEntity.setStatus(1);
                                                newEntity.setId(fromUserName);
                                                newEntity.setStatus(1);
                                                newEntity.setUinonid(unionid);
                                                return weChatSubsribeService.insert(newEntity);
                                            }));
            }).then()); // 转换为 Mono<Void>


    }

    // 处理取消订阅事件的方法
    private Mono<Void> handleUnsubscribe(String fromUserName) {
        return weChatSubsribeService.createUpdate()
                                    .set(WeChatSubscribeEntity::getStatus, 0)
                                    .where(WeChatSubscribeEntity::getId, fromUserName)
                                    .execute()
                                    .then(); // 转换为 Mono<Void>
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

    public Mono<String> getUnionid(String openId,String access_token) {
        String requestUrl = "https://api.weixin.qq.com/cgi-bin/user/info?access_token=ACCESS_TOKEN&openid=OPENID";
        String url = requestUrl.replace("OPENID", openId).replace("ACCESS_TOKEN", access_token);

        // 使用 WebClient 发起异步 HTTP GET 请求
        return webClient.get()
                        .uri(url)
                        .retrieve()
                        .bodyToMono(JSONObject.class) // 将响应体解析为 JSONObject
                        .flatMap(obj -> {
                            String unionid = obj.getString("unionid"); // 提取 unionid
                            if (unionid != null) {
                                return Mono.just(unionid); // 正常返回 unionid
                            } else {
                                return Mono.error(new RuntimeException("Unionid 不存在")); // 返回错误
                            }
                        });
    }
}
