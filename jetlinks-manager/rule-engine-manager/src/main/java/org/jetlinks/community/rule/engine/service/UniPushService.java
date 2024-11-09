package org.jetlinks.community.rule.engine.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.jetlinks.community.rule.engine.entity.UniPushEntity;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.stereotype.Service;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.http.MediaType;
import org.springframework.web.reactive.function.client.WebClient;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Mono;

import java.util.List;

@Component
@Slf4j
public class UniPushService {
    private final WebClient webClient;
    private final ObjectMapper objectMapper;
//    private final LocalDeviceInstanceService instanceService;

    public UniPushService() {
        this.webClient = WebClient.builder().build();  // 创建 WebClient 实例
        this.objectMapper = new ObjectMapper();        // 创建 ObjectMapper 实例
//        this.localDeviceInstanceService = new LocalDeviceInstanceService();
    }

    public Mono<Void> sendPostRequest(String url, List<String> list, String name, String description,String push_type) {
        try {
            // 创建 JSON 数据
//            UniPushEntity.Options.HW hw = new UniPushEntity.Options.HW("DEVICE_REMINDER");
            UniPushEntity.Payload payload = new UniPushEntity.Payload(push_type);
//            log.warn("unipush2.0 message: {}", list.get(0));
            String jsonData = objectMapper.writeValueAsString(new UniPushEntity(
                list,
                name,
                description,
                payload
            ));
            log.warn("unipush2.0 message: {}", jsonData);

            // 发送 POST 请求
            return webClient.post()
                            .uri(url)
                            .contentType(MediaType.APPLICATION_JSON)
                            .bodyValue(jsonData)
                            .retrieve()
                            .bodyToMono(Void.class) // 不处理响应，返回 Mono<Void>
                            .doOnError(error -> log.error("Error occurred: {}", error.getMessage()));

        } catch (JsonProcessingException e) {
            log.error("Error occurred: {}", e.getMessage());
            return Mono.error(e); // 返回错误信号，便于在订阅时处理异常
        }
    }
}
