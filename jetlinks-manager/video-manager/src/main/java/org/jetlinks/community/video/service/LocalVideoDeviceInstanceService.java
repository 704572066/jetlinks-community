package org.jetlinks.community.video.service;


import lombok.extern.slf4j.Slf4j;

import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.jetlinks.community.video.entity.*;

import org.jetlinks.community.video.response.VideoDeviceDetail;
import org.springframework.stereotype.Service;

import reactor.core.publisher.Mono;


@Service
@Slf4j
public class LocalVideoDeviceInstanceService extends GenericReactiveCrudService<VideoDeviceInstanceEntity, String> {

    private final TokenService tokenService;
//    public Flux<VideoDeviceInstanceEntity> findByDeviceId(String deviceId) {
//        return createQuery()
//            .and(VideoDeviceInstanceEntity::getId, deviceId)
//            .fetch();
//    }
    public LocalVideoDeviceInstanceService(TokenService tokenService){
        this.tokenService = tokenService;
    }


    public Mono<VideoDeviceDetail> getVideoDeviceDetail(String deviceId) {

        return this
            .findById(deviceId).flatMap(videoDeviceInstanceEntity -> {
                return createVideDeviceDetail(videoDeviceInstanceEntity);
            });
    }

    private Mono<VideoDeviceDetail> createVideDeviceDetail(VideoDeviceInstanceEntity videoDeviceInstanceEntity){
        return tokenService.createQuery().fetchOne().flatMap(tokenEntity -> {
            VideoDeviceDetail videoDeviceDetail = new VideoDeviceDetail();
            videoDeviceDetail.setStreamUrl("https://open.ys7.com/ezopen/h5/iframe?url=ezopen://open.ys7.com/"+videoDeviceInstanceEntity.getId()+"/1.live&autoplay=1&accessToken="+tokenEntity.getAccess_token());
            videoDeviceDetail.setId(videoDeviceInstanceEntity.getId());
            videoDeviceDetail.setCreateTime(videoDeviceInstanceEntity.getCreateTime());
            return Mono.just(videoDeviceDetail);
        });
    }





}
