package org.jetlinks.community.rule.engine.alarm;

import lombok.AllArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.hswebframework.ezorm.rdb.mapping.ReactiveRepository;
import org.hswebframework.web.bean.FastBeanCopier;
import org.hswebframework.web.i18n.LocaleUtils;
import org.hswebframework.web.id.IDGenerator;
import org.hswebframework.web.system.authorization.api.entity.DimensionUserEntity;
import org.hswebframework.web.system.authorization.defaults.service.DefaultDimensionService;
import org.jetlinks.community.auth.service.WeChatSubsribeService;
import org.jetlinks.community.device.response.DeviceDetail;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import org.jetlinks.community.rule.engine.service.UniPushService;
import org.jetlinks.community.rule.engine.service.WeChatPushService;
import org.jetlinks.core.config.ConfigStorageManager;
import org.jetlinks.core.event.EventBus;
import org.jetlinks.core.utils.Reactors;
import org.jetlinks.community.command.rule.data.AlarmInfo;
import org.jetlinks.community.command.rule.data.AlarmResult;
import org.jetlinks.community.command.rule.data.RelieveInfo;
import org.jetlinks.community.command.rule.data.RelieveResult;
import org.jetlinks.community.rule.engine.entity.AlarmHandleHistoryEntity;
import org.jetlinks.community.rule.engine.entity.AlarmHistoryInfo;
import org.jetlinks.community.rule.engine.entity.AlarmRecordEntity;
import org.jetlinks.community.rule.engine.enums.AlarmRecordState;
import org.jetlinks.community.rule.engine.service.AlarmHistoryService;
import org.jetlinks.community.rule.engine.service.AlarmRecordService;
import org.jetlinks.community.topic.Topics;
import org.jetlinks.community.utils.ObjectMappers;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import reactor.core.publisher.Mono;
import sun.reflect.generics.tree.VoidDescriptor;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

@Slf4j
@AllArgsConstructor
@Component
public class DefaultAlarmHandler implements AlarmHandler {

    private final AlarmRecordService alarmRecordService;

    private final AlarmHistoryService historyService;

    private final ReactiveRepository<AlarmHandleHistoryEntity, String> handleHistoryRepository;

    private final EventBus eventBus;

    private final ConfigStorageManager storageManager;

    private final ApplicationEventPublisher eventPublisher;

    private final LocalDeviceInstanceService service;

    private final UniPushService uniPushService;

    private final WeChatPushService weChatPushService;

    private final WeChatSubsribeService weChatSubsribeService;

    private final LocalDeviceInstanceService localDeviceInstanceService;

    private final DefaultDimensionService defaultDimensionService;

    @Override
    public Mono<AlarmResult> triggerAlarm(AlarmInfo alarmInfo) {

        return getRecordCache(createRecordId(alarmInfo))
            .map(this::ofRecordCache)
            .defaultIfEmpty(new AlarmResult())
            .flatMap(result -> {
                AlarmRecordEntity record = ofRecord(result, alarmInfo);
                //如果是组织, 添加组织ID
                Mono<DeviceDetail> deviceDetailMono = alarmInfo.getTargetType().equals("org")
                    ? service.getDeviceDetail(alarmInfo.getSourceId())
                    : Mono.empty(); // 如果不满足条件，则返回一个空的 Mono

                return deviceDetailMono
                              .map(deviceDetail -> {
                                  // 使用 deviceDetail 更新 record 或 alarmInfo
                                  record.setTargetId(deviceDetail.getOrgId()); // 假设有 getTypeId 方法
//                                  alarmInfo.setTargetId(deviceDetail.getOrgId());
                                  return record; // 返回更新后的 record
                              })
                              .defaultIfEmpty(record) // 如果没有 deviceDetail，则使用原始 record
                              .flatMap(updatedRecord -> {
                                  //更新告警状态.
                                  return alarmRecordService
                                      .createUpdate()
                                      .set(updatedRecord)
                                      .where(AlarmRecordEntity::getId, updatedRecord.getId())
                                      .and(AlarmRecordEntity::getState, AlarmRecordState.warning)
                                      .execute()
                                      //更新数据库报错,依然尝试触发告警!
                                      .onErrorResume(err -> {
                                          log.error("trigger alarm error", err);
                                          return Reactors.ALWAYS_ZERO;
                                      })
                                      .flatMap(total -> {
                                          AlarmHistoryInfo historyInfo = createHistory(updatedRecord, alarmInfo);

                                          //更新结果返回0 说明是新产生的告警数据
                                          if (total == 0) {
                                              //消息推送 告警只推送一次
                                              // 获取当前日期和时间
                                              LocalDateTime now = LocalDateTime.now();
                                              // 转换为自定义的日期时间格式
                                              DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");
                                              String alarmTime = now.format(formatter);
//                                              localDeviceInstanceService.findByDeviceId(alarmInfo.getSourceId())
//                                                                        .map(dev->dev.getOrgId())
//                                                                        .filter(orgId -> orgId != null)
//                                                                        .flatMap(orgId->defaultDimensionService.getCidByDimensionId(orgId))
//                                                                        .filter(cidList -> !cidList.isEmpty())
//                                                                        .flatMap(cidList->uniPushService.sendPostRequest("https://fc-mp-d03ca3a4-ad75-4a9d-b7e3-05ac8fb91905.next.bspapp.com/sendMessage", cidList, "设备告警 "+alarmTime,alarmInfo.getTargetName()+"["+alarmInfo.getSourceId()+"]: "+alarmInfo.getAlarmName(),"warning"))
//                                                                        .subscribe(e->log.error("triggerAlarm unipush2.0 error: {}", e));

//                                              localDeviceInstanceService.findByDeviceId(alarmInfo.getSourceId())
//                                                                        .map(dev -> dev.getOrgId())
//                                                                        .filter(orgId -> orgId != null)
//                                                                        .flatMap(orgId -> defaultDimensionService.getByDimensionId(orgId))
//                                                                        .filter(entity -> entity != null)
//                                                                        .flatMap(entity -> {
//                                                                            // Send to UniPush if CID is not empty
//                                                                            log.warn("cid: "+entity.getCid());
//                                                                            if (!entity.getCid().isEmpty()) {
//                                                                                List<String> cidList = new ArrayList<>();
//                                                                                cidList.add(entity.getCid());
//                                                                                return uniPushService.sendPostRequest(
//                                                                                    "https://fc-mp-d03ca3a4-ad75-4a9d-b7e3-05ac8fb91905.next.bspapp.com/sendMessage",
//                                                                                    cidList,  // Assuming 'cid' is a List<String> for the POST request
//                                                                                    "设备告警 " + alarmTime,
//                                                                                    alarmInfo.getTargetName() + "[" + alarmInfo.getSourceId() + "]: " + alarmInfo.getAlarmName(),
//                                                                                    "warning")
//                                                                                                     .then(Mono.just(entity));  // Ensure the entity continues down the flow
//                                                                            }
//                                                                            return Mono.just(entity);  // If no CID to send, just return entity
//                                                                        })
//                                                                        .flatMap(entity -> weChatSubsribeService.findByUnionId(entity.getUnionid()))
//                                                                        .filter(Objects::nonNull)
//                                                                        .filter(subscriber -> !subscriber.getId().isEmpty())
//                                                                        .flatMap(subscriber -> weChatPushService.sendPostRequest(
//                                                                            subscriber.getId(),
//                                                                            alarmTime,
//                                                                            alarmInfo.getTargetName(),
//                                                                            alarmInfo.getSourceId(),
//                                                                            alarmInfo.getAlarmName()
//                                                                        ))
//                                                                        .subscribe(e->log.error("triggerAlarm push error: {}", e));
//                                                                        .doOnError(e -> log.error("Error processing alarm push: {}", e))  // Log errors
//                                                                        .onErrorResume(e -> Mono.empty());  // Avoid stream termination due to error
                                              localDeviceInstanceService.findByDeviceId(alarmInfo.getSourceId())
                                                                        .map(dev -> dev.getOrgId())
                                                                        .filter(orgId -> orgId != null)
                                                                        .flatMap(orgId -> defaultDimensionService.getDimensionUserListByDimensionId(orgId))
                                                                        .filter(entityList -> !entityList.isEmpty())
                                                                        .flatMap(entityList -> {
                                                                            // Send to UniPush if CID is not empty
                                                                            List<String> cidList = entityList.stream()
                                                                                                             .map(DimensionUserEntity::getCid) // Apply the transformation: getName() of each Entity
                                                                                                             .filter(cid -> cid != null && !cid.isEmpty())
                                                                                                             .collect(Collectors
                                                                                                                          .toList()); // Collect the result into a new list
//                                                                            log.warn("cid: "+entity.getCid());
//                                                                            if (!cidList.isEmpty()) {
//                                                                                return uniPushService.sendPostRequest(
//                                                                                    "https://fc-mp-d03ca3a4-ad75-4a9d-b7e3-05ac8fb91905.next.bspapp.com/sendMessage",
//                                                                                    cidList,  // Assuming 'cid' is a List<String> for the POST request
//                                                                                    "设备告警 " + alarmTime,
//                                                                                    alarmInfo.getTargetName() + "[" + alarmInfo.getSourceId() + "]: " + alarmInfo.getAlarmName(),
//                                                                                    "warning")
//                                                                                                     .then(Mono.just(entityList));  // Ensure the entity continues down the flow
//                                                                            }
                                                                            return Mono.just(entityList);  // If no CID to send, just return entity
                                                                        })
                                                                        .flatMap(entityList -> {
                                                                            List<String> unionidList = entityList.stream()
                                                                                                             .map(DimensionUserEntity::getUnionid) // Apply the transformation: getName() of each Entity
                                                                                                             .collect(Collectors
                                                                                                                          .toList()); // Collect the result into a new list
//
                                                                            return weChatSubsribeService.findOpenidListByUnionId(unionidList);

                                                                        })
//                                                                        .filter(openidList -> !openidList.isEmpty())
//                                                                        .filter(openid -> !openid.isEmpty())
                                                                        .flatMap(openids -> {
                                                                            // Send requests for each openid asynchronously
                                                                            List<Mono<Void>> sendRequests = openids.stream()
                                                                                                                   .filter(openid -> openid != null && !openid.isEmpty())  // Check if each openid is valid
                                                                                                                   .map(openid -> {
                                                                                                                       return weChatPushService.sendPostRequest(
                                                                                                                           openid,
                                                                                                                           alarmTime,
                                                                                                                           alarmInfo.getSourceName(),
                                                                                                                           alarmInfo.getSourceId(),
                                                                                                                           alarmInfo.getAlarmName()
                                                                                                                       ).onErrorResume(e -> {
                                                                                                                           log.error("Failed to send alarm to openid {}: {}", openid, e.getMessage());
                                                                                                                           return Mono.empty();  // Continue the flow even if one request fails
                                                                                                                       });
                                                                                                                   })
                                                                                                                   .collect(Collectors.toList());

                                                                            // Use Mono.when to combine multiple Mono operations and wait for all to complete
                                                                            return Mono.when(sendRequests);
                                                                        })
                                                                        .doOnTerminate(() -> {
                                                                            log.info("Finished processing all push requests");
                                                                        })
                                                                        .doOnError(e -> {
                                                                            log.error("Error processing alarm push: {}", e.getMessage());
                                                                        })
                                                                        .subscribe();
//                                                                        .doOnError(e -> log.error("Error processing alarm push: {}", e))  // Log errors
//                                                                        .onErrorResume(e -> Mono.empty());  // Avoid stream termination due to error



                                              result.setFirstAlarm(true);
                                              result.setAlarming(false);
                                              result.setAlarmTime(historyInfo.getAlarmTime());
                                              record.setAlarmTime(historyInfo.getAlarmTime());
                                              record.setHandleTime(null);
                                              record.setHandleType(null);

                                              return this
                                                  .saveAlarmRecord(updatedRecord)
                                                  .then(historyService.save(historyInfo))
                                                  .then(publishAlarmRecord(historyInfo, alarmInfo))
                                                  .then(publishEvent(historyInfo))
                                                  .then(saveAlarmCache(result, updatedRecord));
                                          }
                                          result.setFirstAlarm(false);
                                          result.setAlarming(true);
                                          return historyService
                                              .save(historyInfo)
                                              .then(publishEvent(historyInfo))
                                              .then(saveAlarmCache(result, updatedRecord));
                                      });
                              });


            });

    }

    private Mono<Void> saveAlarmRecord(AlarmRecordEntity record){
        return alarmRecordService
            .createUpdate()
            .set(record)
            .setNull(AlarmRecordEntity::getHandleTime)
            .setNull(AlarmRecordEntity::getHandleType)
            .where(AlarmRecordEntity::getId, record.getId())
            .execute()
            .flatMap(update -> {
                // 如果是首次告警需要手动保存
                if (update == 0) {
                    return alarmRecordService.save(record).then();
                }
                return Mono.empty();
            });
    }

    @Override
    public Mono<RelieveResult> relieveAlarm(RelieveInfo relieveInfo) {
        return getRecordCache(createRecordId(relieveInfo))
            .map(this::ofRecordCache)
            .defaultIfEmpty(new AlarmResult())
            .flatMap(result -> {
                AlarmRecordEntity record = this.ofRecord(result, relieveInfo);
                return Mono
                    .zip(alarmRecordService.changeRecordState(
                             relieveInfo.getAlarmRelieveType()
                             , AlarmRecordState.normal,
                             record.getId()),
                         this.updateRecordCache(record.getId(), DefaultAlarmRuleHandler.RecordCache::withNormal),
                         (total, ignore) -> total)
                    .flatMap(total -> {
                        //如果有数据被更新说明是正在告警中
                        if (total > 0) {
                            result.setAlarming(true);
                            AlarmHistoryInfo historyInfo = this.createHistory(record, relieveInfo);
                            RelieveResult relieveResult = FastBeanCopier.copy(record, new RelieveResult());
                            relieveResult.setRelieveTime(System.currentTimeMillis());
                            relieveResult.setActualDesc(historyInfo.getActualDesc());
                            relieveResult.setRelieveReason(relieveInfo.getRelieveReason());
                            relieveResult.setDescribe(relieveInfo.getDescription());
                            return saveAlarmHandleHistory(relieveInfo, record)
                                .then(publishAlarmRelieve(historyInfo, relieveInfo))
                                .thenReturn(relieveResult);
                        }
                        return Mono.empty();
                    });
            });
    }

    public AlarmRecordEntity ofRecord(AlarmResult result, AlarmInfo alarmData) {
        AlarmRecordEntity entity = new AlarmRecordEntity();
        entity.setAlarmConfigId(alarmData.getAlarmConfigId());
        entity.setAlarmTime(result.getAlarmTime());
        entity.setState(AlarmRecordState.warning);
        entity.setLevel(alarmData.getLevel());
        entity.setFireInvoke(alarmData.getFireInvoke());
        entity.setTargetType(alarmData.getTargetType());
        entity.setTargetName(alarmData.getTargetName());
        entity.setTargetId(alarmData.getTargetId());

        entity.setSourceType(alarmData.getSourceType());
        entity.setSourceName(alarmData.getSourceName());
        entity.setSourceId(alarmData.getSourceId());

        entity.setAlarmName(alarmData.getAlarmName());
        entity.setDescription(alarmData.getDescription());
        entity.setAlarmConfigSource(alarmData.getAlarmConfigSource());
        if (alarmData.getTermSpec() != null) {
            entity.setTermSpec(alarmData.getTermSpec());
            entity.setTriggerDesc(alarmData.getTermSpec().getTriggerDesc());
            entity.setActualDesc(alarmData.getTermSpec().getActualDesc());
        }
        entity.generateId();
        return entity;
    }

    public AlarmHistoryInfo createHistory(AlarmRecordEntity record, AlarmInfo alarmInfo) {
        AlarmHistoryInfo info = new AlarmHistoryInfo();
        info.setId(IDGenerator.RANDOM.generate());
        info.setAlarmConfigId(record.getAlarmConfigId());
        info.setAlarmConfigName(record.getAlarmName());
        info.setDescription(record.getDescription());
        info.setAlarmRecordId(record.getId());
        info.setLevel(record.getLevel());
        info.setFireInvoke(record.getFireInvoke());
//        info.set(record.getLevel());
        info.setAlarmTime(System.currentTimeMillis());
        info.setTriggerDesc(record.getTriggerDesc());
        info.setAlarmConfigSource(alarmInfo.getAlarmConfigSource());
        info.setActualDesc(record.getActualDesc());

        info.setTargetName(record.getTargetName());
        info.setTargetId(record.getTargetId());
        info.setTargetType(record.getTargetType());

        info.setSourceType(record.getSourceType());
        info.setSourceName(record.getSourceName());
        info.setSourceId(record.getSourceId());

        info.setAlarmInfo(ObjectMappers.toJsonString(alarmInfo.getData()));
        return info;
    }

    public Mono<Void> publishAlarmRecord(AlarmHistoryInfo historyInfo, AlarmInfo alarmInfo) {
        String topic = Topics.alarm(historyInfo.getTargetType(), historyInfo.getTargetId(), historyInfo.getAlarmConfigId());

        return doPublishAlarmHistoryInfo(topic, historyInfo, alarmInfo);
    }

    public Mono<Void> doPublishAlarmHistoryInfo(String topic, AlarmHistoryInfo historyInfo, AlarmInfo alarmInfo) {
        return Mono.just(topic)
            .flatMap(assetTopic -> eventBus.publish(assetTopic, historyInfo))
            .then();
    }

    private Mono<Void> publishEvent(AlarmHistoryInfo historyInfo) {
        return Mono.fromRunnable(() -> eventPublisher.publishEvent(historyInfo));
    }

    private Mono<AlarmResult> saveAlarmCache(AlarmResult result,
                                             AlarmRecordEntity record) {
        return this
            .updateRecordCache(record.getId(), cache -> cache.with(result))
            .thenReturn(result);
    }

    public Mono<Void> publishAlarmRelieve(AlarmHistoryInfo historyInfo, AlarmInfo alarmInfo) {
        String topic = Topics.alarmRelieve(historyInfo.getTargetType(), historyInfo.getTargetId(), historyInfo.getAlarmConfigId());

        return this.doPublishAlarmHistoryInfo(topic, historyInfo, alarmInfo);
    }

    private Mono<Void> saveAlarmHandleHistory(RelieveInfo relieveInfo, AlarmRecordEntity record) {
        AlarmHandleInfo alarmHandleInfo = new AlarmHandleInfo();
        alarmHandleInfo.setHandleTime(System.currentTimeMillis());
        alarmHandleInfo.setAlarmRecordId(record.getId());
        alarmHandleInfo.setAlarmConfigId(record.getAlarmConfigId());
        alarmHandleInfo.setAlarmTime(record.getAlarmTime());
        alarmHandleInfo.setState(AlarmRecordState.normal);
        alarmHandleInfo.setType(relieveInfo.getAlarmRelieveType());
        alarmHandleInfo.setDescribe(getLocaleDescribe());
        // TODO: 2022/12/22 批量缓冲保存
        return handleHistoryRepository
            .save(AlarmHandleHistoryEntity.of(alarmHandleInfo))
            .then();
    }

    private String getLocaleDescribe() {
        return LocaleUtils.resolveMessage("message.scene_triggered_relieve_alarm", "场景触发解除告警");
    }


    private String createRecordId(AlarmInfo alarmInfo) {
        return AlarmRecordEntity.generateId(alarmInfo.getTargetId(), alarmInfo.getTargetType(), alarmInfo.getAlarmConfigId());
    }

    private Mono<DefaultAlarmRuleHandler.RecordCache> getRecordCache(String recordId) {
        return storageManager
            .getStorage("alarm-records")
            .flatMap(store -> store
                .getConfig(recordId)
                .map(val -> val.as(DefaultAlarmRuleHandler.RecordCache.class)));
    }

    private Mono<DefaultAlarmRuleHandler.RecordCache> updateRecordCache(String recordId, Function<DefaultAlarmRuleHandler.RecordCache, DefaultAlarmRuleHandler.RecordCache> handler) {
        return storageManager
            .getStorage("alarm-records")
            .flatMap(store -> store
                .getConfig(recordId)
                .map(val -> val.as(DefaultAlarmRuleHandler.RecordCache.class))
                .switchIfEmpty(Mono.fromSupplier(DefaultAlarmRuleHandler.RecordCache::new))
                .mapNotNull(handler)
                .flatMap(cache -> store.setConfig(recordId, cache)
                                       .thenReturn(cache)));
    }


    private AlarmResult ofRecordCache(DefaultAlarmRuleHandler.RecordCache cache) {
        AlarmResult result = new AlarmResult();
        result.setAlarmTime(cache.alarmTime);
        result.setLastAlarmTime(cache.lastAlarmTime);
        result.setAlarming(cache.isAlarming());
        return result;
    }
}
