package org.jetlinks.community.video.service;


import lombok.extern.slf4j.Slf4j;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.community.video.entity.TokenEntity;
import org.jetlinks.community.video.entity.VideoDeviceInstanceEntity;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;


@Service
@Slf4j
public class TokenService extends GenericReactiveCrudService<TokenEntity, String> {

//    public Flux<VideoDeviceInstanceEntity> findByDeviceId(String deviceId) {
//        return createQuery()
//            .and(VideoDeviceInstanceEntity::getId, deviceId)
//            .fetch();
//    }







}
