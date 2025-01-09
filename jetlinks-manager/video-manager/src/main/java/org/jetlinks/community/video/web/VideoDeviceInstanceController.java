package org.jetlinks.community.video.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;

import lombok.extern.slf4j.Slf4j;

import org.hswebframework.ezorm.rdb.exception.DuplicateKeyException;

import org.hswebframework.web.authorization.Authentication;

import org.hswebframework.web.authorization.annotation.*;

import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.exception.NotFoundException;
import org.hswebframework.web.exception.ValidationException;
import org.hswebframework.web.i18n.LocaleUtils;

import org.jetlinks.community.video.entity.*;
import org.jetlinks.community.video.response.VideoDeviceDetail;
import org.jetlinks.community.video.service.LocalVideoDeviceInstanceService;

import org.jetlinks.community.web.response.ValidationResult;


import org.springframework.web.bind.annotation.*;

import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;



import java.util.List;


@RestController
@RequestMapping({"/video-device-instance", "/video/device/instance"})
@Authorize
@Resource(id = "video-device-instance", name = "视频设备实例")
@Slf4j
@Tag(name = "视频设备实例接口")
public class VideoDeviceInstanceController implements
    ReactiveServiceCrudController<VideoDeviceInstanceEntity, String> {

    @Getter
    private final LocalVideoDeviceInstanceService service;

    @SuppressWarnings("all")
    public VideoDeviceInstanceController(LocalVideoDeviceInstanceService service
                                    ) {
        this.service = service;
    }


    //获取设备详情
    @GetMapping("/{id:.+}/detail")
    @QueryAction
    @Operation(summary = "获取指定ID设备详情")
    public Mono<VideoDeviceDetail> getVideoDeviceDetailInfo(@PathVariable @Parameter(description = "设备ID") String id) {
        return service
            .getVideoDeviceDetail(id)
            .switchIfEmpty(Mono.error(NotFoundException::new));
    }


    //新建设备
    @PostMapping
    @Operation(summary = "新建设备")
    public Mono<VideoDeviceInstanceEntity> add(@RequestBody Mono<VideoDeviceInstanceEntity> payload) {
        return Mono
            .zip(payload, Authentication.currentReactive(), this::applyAuthentication)
            .flatMap(entity -> service.insert(Mono.just(entity)).thenReturn(entity))
            .onErrorMap(DuplicateKeyException.class, err -> new BusinessException("设备ID已存在", err));
    }

    /**
     * 批量删除设备,只会删除未激活的设备.
     *
     * @param idList ID列表
     * @return 被删除数量
     * @since 1.1
     */
    @PutMapping("/batch/_delete")
    @DeleteAction
    @Operation(summary = "批量删除设备")
    public Mono<Integer> deleteBatch(@RequestBody Mono<List<String>> idList) {
        return idList.flatMapMany(Flux::fromIterable)
                     .as(service::deleteById);
    }

//    /**
//     * 批量注销设备
//     *
//     * @param idList ID列表
//     * @return 被注销的数量
//     * @since 1.1
//     */
//    @PutMapping("/batch/_unDeploy")
//    @SaveAction
//    @Operation(summary = "批量注销设备")
//    public Mono<Integer> unDeployBatch(@RequestBody Mono<List<String>> idList) {
//        return idList.flatMap(list -> service.unregisterDevice(Flux.fromIterable(list)));
//    }

//    /**
//     * 批量激活设备
//     *
//     * @param idList ID列表
//     * @return 被激活的数量
//     */
//    @PutMapping("/batch/_deploy")
//    @SaveAction
//    @Operation(summary = "批量激活设备")
//    public Mono<Integer> deployBatch(@RequestBody Mono<List<String>> idList) {
//        return idList.flatMapMany(service::findById)
//                     .as(service::deploy)
//                     .map(DeviceDeployResult::getTotal)
//                     .reduce(Math::addExact);
//    }

    @GetMapping("/{id:.+}/exists")
    @QueryAction
    @Operation(summary = "验证设备ID是否存在")
    public Mono<Boolean> deviceIdValidate(@PathVariable @Parameter(description = "设备ID") String id) {
        return service.findById(id)
                      .hasElement();
    }

    @GetMapping("/id/_validate")
    @QueryAction
    @Operation(summary = "验证设备ID是否合法")
    public Mono<ValidationResult> deviceIdValidate2(@RequestParam @Parameter(description = "设备ID") String id) {
        return LocaleUtils.currentReactive()
                          .flatMap(locale -> {
                              VideoDeviceInstanceEntity entity = new VideoDeviceInstanceEntity();
                              entity.setId(id);
                              entity.validateId();

                              return service.findById(id)
                                            .map(device -> ValidationResult.error(
                                                LocaleUtils.resolveMessage("error.device_ID_already_exists", locale)))
                                            .defaultIfEmpty(ValidationResult.success());
                          })
                          .onErrorResume(ValidationException.class, e -> Mono.just(e.getI18nCode())
                                                                             .map(ValidationResult::error));
    }


}
