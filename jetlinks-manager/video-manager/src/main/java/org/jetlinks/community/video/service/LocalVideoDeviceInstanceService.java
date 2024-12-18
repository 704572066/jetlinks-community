package org.jetlinks.community.device.service;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.collections4.CollectionUtils;
import org.apache.commons.collections4.MapUtils;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.ezorm.rdb.mapping.ReactiveUpdate;
import org.hswebframework.ezorm.rdb.mapping.defaults.SaveResult;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.crud.events.EntityDeletedEvent;
import org.hswebframework.web.crud.events.EntityEventHelper;
import org.hswebframework.web.crud.service.GenericReactiveCrudService;
import org.hswebframework.web.exception.BusinessException;
import org.hswebframework.web.exception.I18nSupportException;
import org.hswebframework.web.exception.TraceSourceException;
import org.hswebframework.web.i18n.LocaleUtils;
import org.hswebframework.web.id.IDGenerator;
import org.jetlinks.community.device.entity.*;
import org.jetlinks.community.device.enums.DeviceState;
import org.jetlinks.community.device.events.DeviceDeployedEvent;
import org.jetlinks.community.device.events.DeviceUnregisterEvent;
import org.jetlinks.community.device.response.DeviceDeployResult;
import org.jetlinks.community.device.response.DeviceDetail;
import org.jetlinks.community.device.response.ResetDeviceConfigurationResult;
import org.jetlinks.community.relation.RelationObjectProvider;
import org.jetlinks.community.relation.service.RelationService;
import org.jetlinks.community.relation.service.response.RelatedInfo;
import org.jetlinks.community.utils.ErrorUtils;
import org.jetlinks.core.device.DeviceConfigKey;
import org.jetlinks.core.device.DeviceOperator;
import org.jetlinks.core.device.DeviceProductOperator;
import org.jetlinks.core.device.DeviceRegistry;
import org.jetlinks.core.enums.ErrorCode;
import org.jetlinks.core.exception.DeviceOperationException;
import org.jetlinks.core.message.DeviceMessageReply;
import org.jetlinks.core.message.FunctionInvokeMessageSender;
import org.jetlinks.core.message.ReadPropertyMessageSender;
import org.jetlinks.core.message.WritePropertyMessageSender;
import org.jetlinks.core.message.codec.Transport;
import org.jetlinks.core.message.function.FunctionInvokeMessageReply;
import org.jetlinks.core.message.property.ReadPropertyMessageReply;
import org.jetlinks.core.message.property.WritePropertyMessageReply;
import org.jetlinks.core.metadata.ConfigMetadata;
import org.jetlinks.core.metadata.DeviceMetadata;
import org.jetlinks.core.metadata.MergeOption;
import org.jetlinks.core.utils.CyclicDependencyChecker;
import org.jetlinks.reactor.ql.utils.CastUtils;
import org.jetlinks.supports.official.JetLinksDeviceMetadataCodec;
import org.reactivestreams.Publisher;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Service;
import org.springframework.transaction.reactive.TransactionalOperator;
import org.springframework.util.ObjectUtils;
import org.springframework.util.StringUtils;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;
import reactor.util.function.Tuple2;
import reactor.util.function.Tuple3;
import reactor.util.function.Tuples;

import java.util.*;
import java.util.concurrent.ConcurrentHashMap;
import java.util.function.Function;
import java.util.stream.Collectors;

@Service
@Slf4j
public class LocalVideoDeviceInstanceService extends GenericReactiveCrudService<DeviceInstanceEntity, String> {

    private final DeviceRegistry registry;

    private final LocalDeviceProductService deviceProductService;

    private final ReactiveRepository<DeviceTagEntity, String> tagRepository;

    private final ApplicationEventPublisher eventPublisher;

    private final DeviceConfigMetadataManager metadataManager;

    private final RelationService relationService;

    private final TransactionalOperator transactionalOperator;

    public LocalVideoDeviceInstanceService(DeviceRegistry registry,
                                      LocalDeviceProductService deviceProductService,
                                      @SuppressWarnings("all")
                                      ReactiveRepository<DeviceTagEntity, String> tagRepository,
                                      ApplicationEventPublisher eventPublisher,
                                      DeviceConfigMetadataManager metadataManager,
                                      RelationService relationService,
                                      TransactionalOperator transactionalOperator) {
        this.registry = registry;
        this.deviceProductService = deviceProductService;
        this.tagRepository = tagRepository;
        this.eventPublisher = eventPublisher;
        this.metadataManager = metadataManager;
        this.relationService = relationService;
        this.transactionalOperator = transactionalOperator;
    }

    @Override
    public Mono<SaveResult> save(Publisher<DeviceInstanceEntity> entityPublisher) {
        return Flux
            .from(entityPublisher)
            .flatMap(instance -> {
                instance.setState(null);
                if (StringUtils.isEmpty(instance.getId())) {
                    return handleCreateBefore(instance);
                }
                return registry
                    .getDevice(instance.getId())
                    .flatMap(DeviceOperator::getState)
                    .map(DeviceState::of)
                    .onErrorReturn(DeviceState.offline)
                    .defaultIfEmpty(DeviceState.notActive)
                    .doOnNext(instance::setState)
                    .thenReturn(instance);
            })
            .as(super::save);
    }

    public Flux<DeviceInstanceEntity> findByDeviceId(String deviceId) {
        return createQuery()
            .and(DeviceInstanceEntity::getId, deviceId)
            .fetch();
    }

    private Flux<DeviceInstanceEntity> findByProductId(String productId) {
        return createQuery()
            .and(DeviceInstanceEntity::getProductId, productId)
            .fetch();
    }

    private Set<String> getProductConfigurationProperties(DeviceProductEntity product) {
        if (MapUtils.isNotEmpty(product.getConfiguration())) {
            return product.getConfiguration()
                          .keySet();
        }
        return new HashSet<>();
    }

    private Mono<Map<String, Object>> resetConfiguration(DeviceProductEntity product, DeviceInstanceEntity device) {
        return metadataManager
            .getProductConfigMetadataProperties(product.getId())
            .defaultIfEmpty(getProductConfigurationProperties(product))
            .flatMap(set -> {
                if (set.size() > 0) {
                    if (MapUtils.isNotEmpty(device.getConfiguration())) {
                        set.forEach(device.getConfiguration()::remove);
                    }
                    //重置注册中心里的配置
                    return registry
                        .getDevice(device.getId())
                        .flatMap(opts -> opts.removeConfigs(set))
                        .then();
                }
                return Mono.empty();
            })
            .then(
                //更新数据库
                createUpdate()
                    .set(device::getConfiguration)
                    .where(device::getId)
                    .execute()
            )
            .then(Mono.fromSupplier(device::getConfiguration));
    }




    //分页查询设备详情列表
    public Mono<PagerResult<DeviceDetail>> queryDeviceDetail(QueryParamEntity entity) {

        return this
            .queryPager(entity)
            .filter(e -> CollectionUtils.isNotEmpty(e.getData()))
            .flatMap(result -> this
                .convertDeviceInstanceToDetail(result.getData(),
                                               hasContext(entity, "includeTags"),
                                               hasContext(entity, "includeBind"),
                                               hasContext(entity, "includeRelations"),
                                               hasContext(entity, "includeFirmwareInfos"))
                .collectList()
                .map(detailList -> PagerResult.of(result.getTotal(), detailList, entity)))
            .defaultIfEmpty(PagerResult.empty());
    }

    //查询设备详情列表
    public Flux<DeviceDetail> queryDeviceDetailList(QueryParamEntity entity) {
        return this
            .query(entity)
            .collectList()
            .flatMapMany(list -> this
                .convertDeviceInstanceToDetail(list,
                                               hasContext(entity, "includeTags"),
                                               hasContext(entity, "includeBind"),
                                               hasContext(entity, "includeRelations"),
                                               hasContext(entity, "includeFirmwareInfos")));
    }

    private Mono<Map<String, List<DeviceTagEntity>>> queryDeviceTagGroup(Collection<String> deviceIdList) {
        return tagRepository
            .createQuery()
            .where()
            .in(DeviceTagEntity::getDeviceId, deviceIdList)
            .fetch()
            .collect(Collectors.groupingBy(DeviceTagEntity::getDeviceId))
            .defaultIfEmpty(Collections.emptyMap());
    }

    private Flux<DeviceDetail> convertDeviceInstanceToDetail(List<DeviceInstanceEntity> instanceList,
                                                             boolean includeTag,
                                                             boolean includeBinds,
                                                             boolean includeRelations,
                                                             boolean includeFirmwareInfos) {
        if (CollectionUtils.isEmpty(instanceList)) {
            return Flux.empty();
        }
        List<String> deviceIdList = new ArrayList<>(instanceList.size());
        //按设备产品分组
        Map<String, List<DeviceInstanceEntity>> productGroup = instanceList
            .stream()
            .peek(device -> deviceIdList.add(device.getId()))
            .collect(Collectors.groupingBy(DeviceInstanceEntity::getProductId));
        //标签
        Mono<Map<String, List<DeviceTagEntity>>> tags = includeTag
            ? this.queryDeviceTagGroup(deviceIdList)
            : Mono.just(Collections.emptyMap());

        //关系信息
        Mono<Map<String, List<RelatedInfo>>> relations = includeRelations ? relationService
            .getRelationInfo(RelationObjectProvider.TYPE_DEVICE, deviceIdList)
            .collect(Collectors.groupingBy(RelatedInfo::getObjectId))
            .defaultIfEmpty(Collections.emptyMap())
            : Mono.just(Collections.emptyMap());


        return Mono
            .zip(
                //T1:查询出所有设备的产品信息
                deviceProductService
                    .findById(productGroup.keySet())
                    .collect(Collectors.toMap(DeviceProductEntity::getId, Function.identity())),
                //T2:查询出标签并按设备ID分组
                tags,
                //T3: 关系信息
                relations
            )
            .flatMapMany(tp5 -> Flux
                //遍历设备,将设备信息转为详情.
                .fromIterable(instanceList)
                .flatMap(instance -> this
                    .createDeviceDetail(
                        // 设备
                        instance
                        //产品
                        , tp5.getT1().get(instance.getProductId())
                        //标签
                        , tp5.getT2().get(instance.getId())
                        //关系信息
                        , tp5.getT3().get(instance.getId())
                    )
                ))
            //createDeviceDetail是异步操作,可能导致顺序错乱.进行重新排序.
            .sort(Comparator.comparingInt(detail -> deviceIdList.indexOf(detail.getId())))
            ;
    }

    private Mono<DeviceDetail> createDeviceDetail(DeviceInstanceEntity device,
                                                  DeviceProductEntity product,
                                                  List<DeviceTagEntity> tags,
                                                  List<RelatedInfo> relations) {
        if (product == null) {
            log.warn("device [{}] product [{}] does not exists", device.getId(), device.getProductId());
            return Mono.empty();
        }
        DeviceDetail detail = new DeviceDetail()
            .with(product)
            .with(device)
            .with(tags)
            .withRelation(relations);

        return Mono
            .zip(
                //产品注册信息
                registry
                    .getProduct(product.getId()),
                //feature信息
                metadataManager
                    .getProductFeatures(product.getId())
                    .collectList())
            .flatMap(t2 -> {
                //填充产品中feature信息
                detail.withFeatures(t2.getT2());
                //填充注册中心里的产品信息
                return detail.with(t2.getT1());
            })
            .then(Mono.zip(
                //设备信息
                registry
                    .getDevice(device.getId())
                    //先刷新配置缓存
                    .flatMap(operator -> operator.refreshAllConfig().thenReturn(operator))
                    .flatMap(operator -> operator
                        //检查设备的真实状态,可能出现设备已经离线,但是数据库状态未及时更新的.
                        .checkState()
                        .map(DeviceState::of)
                        //检查失败,则返回原始状态
                        .onErrorReturn(device.getState())
                        //如果状态不一致,则需要更新数据库中的状态
                        .filter(state -> state != detail.getState())
                        .doOnNext(detail::setState)
                        .flatMap(state -> createUpdate()
                            .set(DeviceInstanceEntity::getState, state)
                            .where(DeviceInstanceEntity::getId, device.getId())
                            .execute())
                        .thenReturn(operator)),
                //配置定义
                metadataManager
                    .getDeviceConfigMetadata(device.getId())
                    .flatMapIterable(ConfigMetadata::getProperties)
                    .collectList()
            ))
            //填充详情信息
            .flatMap(tp2 -> detail
                .with(tp2.getT1(), tp2.getT2()))
            .switchIfEmpty(
                Mono.defer(() -> {
                    //如果设备注册中心里没有设备信息,并且数据库里的状态不是未激活.
                    //可能是因为注册中心信息丢失,修改数据库中的状态信息.
                    if (detail.getState() != DeviceState.notActive) {
                        return createUpdate()
                            .set(DeviceInstanceEntity::getState, DeviceState.notActive)
                            .where(DeviceInstanceEntity::getId, detail.getId())
                            .execute()
                            .thenReturn(detail.notActive());
                    }
                    return Mono.just(detail.notActive());
                }).thenReturn(detail))
            .onErrorResume(err -> {
                log.warn("get device detail error", err);
                return Mono.just(detail);
            });
    }

    public Mono<DeviceDetail> getDeviceDetail(String deviceId) {

        return this
            .findById(deviceId)
            .map(Collections::singletonList)
            .flatMapMany(list -> convertDeviceInstanceToDetail(list, true, true, true, true))
            .next();
    }

    public Mono<DeviceState> getDeviceState(String deviceId) {
        return registry.getDevice(deviceId)
                       .flatMap(DeviceOperator::checkState)
                       .flatMap(state -> {
                           DeviceState deviceState = DeviceState.of(state);
                           return createUpdate()
                               .set(DeviceInstanceEntity::getState, deviceState)
                               .where(DeviceInstanceEntity::getId, deviceId)
                               .execute()
                               .thenReturn(deviceState);
                       })
                       .defaultIfEmpty(DeviceState.notActive);
    }


    private static <R extends DeviceMessageReply, T> Function<R, Mono<T>> mapReply(Function<R, T> function) {
        return reply -> {
            if (ErrorCode.REQUEST_HANDLING.name().equals(reply.getCode())) {
                throw new DeviceOperationException(ErrorCode.REQUEST_HANDLING, reply.getMessage());
            }
            if (!reply.isSuccess()) {
                if (StringUtils.isEmpty(reply.getMessage())) {
                    throw new BusinessException("error.reply_is_error");
                }
                throw new BusinessException(reply.getMessage(), reply.getCode());
            }
            return Mono.justOrEmpty(function.apply(reply));
        };
    }


    @Override
    public Mono<Integer> insert(DeviceInstanceEntity data) {
        return this
            .handleCreateBefore(data)
            .flatMap(super::insert);
    }

    @Override
    public Mono<Integer> insert(Publisher<DeviceInstanceEntity> entityPublisher) {
        return super.insert(Flux.from(entityPublisher).flatMap(this::handleCreateBefore));
    }

    @Override
    public Mono<Integer> insertBatch(Publisher<? extends Collection<DeviceInstanceEntity>> entityPublisher) {
        return Flux.from(entityPublisher)
                   .flatMapIterable(Function.identity())
                   .as(this::insert);
    }

    private Mono<DeviceInstanceEntity> handleCreateBefore(DeviceInstanceEntity instanceEntity) {
        return Mono
            .zip(
                deviceProductService.findById(instanceEntity.getProductId()),
                registry
                    .getProduct(instanceEntity.getProductId())
                    .flatMap(DeviceProductOperator::getProtocol),
                (product, protocol) -> protocol.doBeforeDeviceCreate(
                    Transport.of(product.getTransportProtocol()), instanceEntity.toDeviceInfo(false))
            )
            .flatMap(Function.identity())
            .doOnNext(info -> {
                if (ObjectUtils.isEmpty(instanceEntity.getId())) {
                    instanceEntity.setId(info.getId());
                }
                instanceEntity.mergeConfiguration(info.getConfiguration());
            })
            .thenReturn(instanceEntity);

    }

    private final CyclicDependencyChecker<DeviceInstanceEntity, Void> checker = CyclicDependencyChecker
        .of(DeviceInstanceEntity::getId, DeviceInstanceEntity::getParentId, this::findById);

    public Mono<Void> checkCyclicDependency(DeviceInstanceEntity device) {
        return checker.check(device);
    }

    public Mono<Void> checkCyclicDependency(String id, String parentId) {
        DeviceInstanceEntity instance = new DeviceInstanceEntity();
        instance.setId(id);
        instance.setParentId(parentId);
        return checker.check(instance);
    }

    public Mono<Void> mergeConfiguration(String deviceId,
                                         Map<String, Object> configuration,
                                         Function<ReactiveUpdate<DeviceInstanceEntity>,
                                             ReactiveUpdate<DeviceInstanceEntity>> updateOperation) {
        if (MapUtils.isEmpty(configuration)) {
            return Mono.empty();
        }
        return this
            .findById(deviceId)
            .flatMap(device -> {
                //合并更新配置
                device.mergeConfiguration(configuration);
                return createUpdate()
                    .set(device::getConfiguration)
                    .set(device::getFeatures)
                    .set(device::getDeriveMetadata)
                    .as(updateOperation)
                    .where(device::getId)
                    .execute();
            })
            .then(
                //更新缓存里到信息
                registry
                    .getDevice(deviceId)
                    .flatMap(device -> device.setConfigs(configuration))
            )
            .then();

    }

}
