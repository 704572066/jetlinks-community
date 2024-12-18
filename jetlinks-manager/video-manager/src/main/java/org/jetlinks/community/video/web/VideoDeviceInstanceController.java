package org.jetlinks.community.device.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.Getter;
import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.hswebframework.ezorm.rdb.exception.DuplicateKeyException;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.reactor.excel.ReactorExcel;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryNoPagingOperation;
import org.hswebframework.web.api.crud.entity.QueryOperation;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.Authentication;
import org.hswebframework.web.authorization.Dimension;
import org.hswebframework.web.authorization.annotation.*;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceCrudController;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.exception.NotFoundException;
import org.hswebframework.web.exception.ValidationException;
import org.hswebframework.web.i18n.LocaleUtils;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.PropertyMetric;
import org.jetlinks.community.device.entity.*;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.device.response.DeviceDeployResult;
import org.jetlinks.community.device.response.DeviceDetail;
import org.jetlinks.community.device.response.ImportDeviceInstanceResult;
import org.jetlinks.community.device.response.ResetDeviceConfigurationResult;
import org.jetlinks.community.device.service.DeviceConfigMetadataManager;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.jetlinks.community.device.service.LocalDeviceProductService;
import org.jetlinks.community.device.service.data.DeviceDataService;
import org.jetlinks.community.device.service.data.DeviceProperties;
import org.jetlinks.community.device.web.excel.DeviceExcelImporter;
import org.jetlinks.community.device.web.excel.DeviceExcelInfo;
import org.jetlinks.community.device.web.excel.DeviceWrapper;
import org.jetlinks.community.device.web.excel.PropertyMetadataExcelInfo;
import org.jetlinks.community.device.web.excel.PropertyMetadataWrapper;
import org.jetlinks.community.device.web.excel.*;
import org.jetlinks.community.device.web.request.AggRequest;
import org.jetlinks.community.io.excel.AbstractImporter;
import org.jetlinks.community.io.excel.ImportExportService;
import org.jetlinks.community.io.file.FileManager;
import org.jetlinks.community.io.utils.FileUtils;
import org.jetlinks.community.relation.RelationObjectProvider;
import org.jetlinks.community.relation.service.RelationService;
import org.jetlinks.community.relation.service.request.SaveRelationRequest;
import org.jetlinks.community.things.impl.metric.DefaultPropertyMetricManager;
import org.jetlinks.community.timeseries.query.AggregationData;
import org.jetlinks.community.web.response.ValidationResult;
import org.jetlinks.core.Values;
import org.jetlinks.core.device.*;
import org.jetlinks.core.device.manager.DeviceBindHolder;
import org.jetlinks.core.device.manager.DeviceBindProvider;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.DeviceMessage;
import org.jetlinks.core.message.Message;
import org.jetlinks.core.message.MessageType;
import org.jetlinks.core.message.RepayableDeviceMessage;
import org.jetlinks.core.metadata.*;
import org.jetlinks.supports.official.JetLinksDeviceMetadataCodec;
import org.springframework.core.io.buffer.DataBufferFactory;
import org.springframework.core.io.buffer.DefaultDataBufferFactory;
import org.springframework.data.util.Lazy;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.server.reactive.ServerHttpResponse;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.util.StringUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.reactive.function.client.WebClient;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.concurrent.Queues;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple4;
import reactor.util.function.Tuples;

import java.io.IOException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import java.util.stream.Collectors;

import static org.hswebframework.reactor.excel.ReactorExcel.read;

@RestController
@RequestMapping({"/video-device-instance", "/video/device/instance"})
@Authorize
@Resource(id = "video-device-instance", name = "视频设备实例")
@Slf4j
@Tag(name = "视频设备实例接口")
public class VideoDeviceInstanceController implements
    ReactiveServiceCrudController<VideoDeviceInstanceEntity, String> {

    @Getter
    private final LocalDeviceInstanceService service;

//    private final DeviceDataService deviceDataService;
//
//    private final DeviceExcelFilterColumns filterColumns;

    @SuppressWarnings("all")
    public DeviceInstanceController(LocalDeviceInstanceService service
                                    ) {
        this.service = service;
//        this.registry = registry;
//        this.productService = productService;
//        this.importExportService = importExportService;
//        this.tagRepository = tagRepository;
//        this.deviceDataService = deviceDataService;
//        this.metadataManager = metadataManager;
//        this.relationService = relationService;
//        this.transactionalOperator = transactionalOperator;
//        this.fileManager = fileManager;
//        this.webClient = builder.build();
//        this.filterColumns = filterColumns;
//        this.metricManager = metricManager;
    }


    //获取设备详情
    @GetMapping("/{id:.+}/detail")
    @QueryAction
    @Operation(summary = "获取指定ID设备详情")
    public Mono<DeviceDetail> getDeviceDetailInfo(@PathVariable @Parameter(description = "设备ID") String id) {
        return service
            .getDeviceDetail(id)
            .switchIfEmpty(Mono.error(NotFoundException::new));
    }


    //新建设备
    @PostMapping
    @Operation(summary = "新建设备")
    public Mono<DeviceInstanceEntity> add(@RequestBody Mono<DeviceInstanceEntity> payload) {
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

    /**
     * 批量注销设备
     *
     * @param idList ID列表
     * @return 被注销的数量
     * @since 1.1
     */
    @PutMapping("/batch/_unDeploy")
    @SaveAction
    @Operation(summary = "批量注销设备")
    public Mono<Integer> unDeployBatch(@RequestBody Mono<List<String>> idList) {
        return idList.flatMap(list -> service.unregisterDevice(Flux.fromIterable(list)));
    }

    /**
     * 批量激活设备
     *
     * @param idList ID列表
     * @return 被激活的数量
     */
    @PutMapping("/batch/_deploy")
    @SaveAction
    @Operation(summary = "批量激活设备")
    public Mono<Integer> deployBatch(@RequestBody Mono<List<String>> idList) {
        return idList.flatMapMany(service::findById)
                     .as(service::deploy)
                     .map(DeviceDeployResult::getTotal)
                     .reduce(Math::addExact);
    }

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
                              DeviceInstanceEntity entity = new DeviceInstanceEntity();
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
