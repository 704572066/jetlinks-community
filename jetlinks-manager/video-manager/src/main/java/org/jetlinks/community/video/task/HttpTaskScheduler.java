package org.jetlinks.community.video.task;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.video.entity.TokenEntity;
import org.jetlinks.community.video.request.TokenRequest;
import org.jetlinks.community.video.response.TokenResult;
import org.jetlinks.community.video.service.TokenService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

@Component
@Slf4j
public class HttpTaskScheduler {

    private final WebClient webClient;
    private final ObjectMapper objectMapper;
    private final TokenService tokenService;

    public HttpTaskScheduler(TokenService tokenService) {
        this.webClient = WebClient.create(); // 创建一个默认的 WebClient
        this.objectMapper = new ObjectMapper();        // 创建 ObjectMapper 实例
        this.tokenService = tokenService;
    }

    @Scheduled(fixedRate = 600000000) // 每隔7天执行一次
    public void sendHttpRequest() {
        try{
//            log.warn("接口响应: task");
                String url = "https://open.ys7.com/api/lapp/token/get";
                String jsonData = objectMapper.writeValueAsString(new TokenRequest(
                    "901b9d64d14b4bc5b6709b6ce7f4d7c4","5af552f52cd742875e54ac8aed2d4783"
                ));
//                log.warn("unipush2.0 message: {}", jsonData);

                // 发送 POST 请求
                webClient.post()
                                .uri(url)
                                .contentType(MediaType.APPLICATION_FORM_URLENCODED)
                                .bodyValue("appKey=901b9d64d14b4bc5b6709b6ce7f4d7c4&appSecret=5af552f52cd742875e54ac8aed2d4783")
                                .retrieve()
                                .onStatus(status -> !status.is2xxSuccessful(), ClientResponse::createException)
                                .bodyToMono(TokenResult.class) // 转换响应为 TokenResponse
                                .filter(response -> response.getCode().equals("200"))
                                .doOnNext(response -> {
//                                    TokenEntity entity = new TokenEntity();
//                                    entity.setAccess_token(response.getData().getAccessToken());
//                                    entity.setExpire_time(response.getData().getExpireTime());
//                                    entity.setId("1");
//                                    log.warn("99: {}",entity);
//                                    tokenService.createQuery().fetchOne().flatMap()
//                                    tokenService.insert(Mono.just(entity)).subscribe();
                                      tokenService.createQuery()
                                                .fetchOne() // 尝试获取记录
                                                .flatMap(existingEntity -> {
                                                    // 如果获取到记录，更新数据
                                                    log.warn("exist: {}",response.getData().getAccessToken());
//                                                    existingEntity.setAccess_token(response.getData().getAccessToken());
//                                                    existingEntity.setExpire_time(response.getData().getExpireTime());
                                                    return tokenService.createUpdate()
                                                                       .set(TokenEntity::getAccess_token, response.getData().getAccessToken())
                                                                       .set(TokenEntity::getExpire_time, response.getData().getExpireTime())
                                                                       .where(TokenEntity::getId, existingEntity.getId())
                                                                       .execute();
                                                })
                                                .switchIfEmpty(
                                                    // 如果未获取到记录，则插入新数据
                                                    Mono.defer(() -> {
                                                        TokenEntity newEntity = new TokenEntity();
                                                        newEntity.setAccess_token(response.getData().getAccessToken());
                                                        newEntity.setExpire_time(response.getData().getExpireTime());
                                                        newEntity.setId("1");
                                                        return tokenService.insert(Mono.just(newEntity)); // 调用插入逻辑
                                                    })
                                                )
                                                .subscribe(
                                                    success -> log.info("ys token 操作成功"),
                                                    error -> log.error("ys token 操作失败: {}", error.getMessage())
                                                );

                                })
                                .doOnError(error -> log.error("Error occurred: {}", error.getMessage()))
                                .subscribe();

        } catch (JsonProcessingException e) {
            log.error("Error occurred: {}", e.getMessage());
//            return Mono.error(e); // 返回错误信号，便于在订阅时处理异常
        }

    }
}
