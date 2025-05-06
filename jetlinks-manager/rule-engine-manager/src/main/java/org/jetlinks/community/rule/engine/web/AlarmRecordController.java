package org.jetlinks.community.rule.engine.web;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.AllArgsConstructor;
import org.hswebframework.ezorm.core.param.Term;
import org.hswebframework.web.api.crud.entity.PagerResult;
import org.hswebframework.web.api.crud.entity.QueryParamEntity;
import org.hswebframework.web.authorization.annotation.Authorize;
import org.hswebframework.web.authorization.annotation.QueryAction;
import org.hswebframework.web.authorization.annotation.Resource;
import org.hswebframework.web.authorization.annotation.SaveAction;
import org.hswebframework.web.crud.query.QueryHelper;
import org.hswebframework.web.crud.service.ReactiveCrudService;
import org.hswebframework.web.crud.web.reactive.ReactiveServiceQueryController;
import org.jetlinks.community.rule.engine.alarm.AlarmHandleInfo;
import org.jetlinks.community.rule.engine.entity.AlarmHandleHistoryEntity;
import org.jetlinks.community.rule.engine.entity.AlarmRecordEntity;
import org.jetlinks.community.rule.engine.service.AlarmConfigService;
import org.jetlinks.community.rule.engine.service.AlarmHandleHistoryService;
import org.jetlinks.community.rule.engine.service.AlarmRecordService;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@RestController
@RequestMapping(value = "/alarm/record")
@Resource(id = "alarm-record", name = "告警记录")
@Authorize
@Tag(name = "告警记录")
@AllArgsConstructor
public class AlarmRecordController implements ReactiveServiceQueryController<AlarmRecordEntity, String> {

    private final AlarmRecordService recordService;

    private final AlarmConfigService configService;

    private final AlarmHandleHistoryService handleHistoryService;

    private final QueryHelper queryHelper;

    @Override
    public ReactiveCrudService<AlarmRecordEntity, String> getService() {
        return recordService;
    }

    @PostMapping("/_handle")
    @Operation(summary = "处理告警")
    @SaveAction
    public Mono<Void> handleAlarm(@RequestBody Mono<AlarmHandleInfo> handleInfo) {
        return handleInfo
            .flatMap(configService::handleAlarm);
    }

    @PostMapping("/handle-history/_query")
    @Operation(summary = "告警处理历史查询")
    @QueryAction
    public Mono<PagerResult<AlarmHandleHistoryEntity>> queryHandleHistoryPager(@RequestBody Mono<QueryParamEntity> query) {
        return query.flatMap(handleHistoryService::queryPager);
    }

    //原生SQL方式
    @PostMapping("/_query_native")
    @Operation(summary = "根据用户所属组织查询告警记录")
    @QueryAction
    public Mono<PagerResult<AlarmRecordEntity>> nativeJoinExample(@RequestBody Mono<QueryParamEntity> queryParam) {
        return queryParam.flatMap(param -> {
//            param.getTerms().stream().findFirst();
            // 使用stream获取orgId的value
            Optional<Object> orgIds = param.getTerms().stream()
                                           .filter(term -> "orgId".equals(term.getColumn())) // 过滤出column为orgId的term
                                           .map(term -> term.getValue()) // 提取value字段
                                           .findFirst(); // 只取第一个匹配的值

            String ids = (String) orgIds.orElse("");
            //得到orgid后terms再次过滤掉,防止在where(param)中报错
            param.setTerms(param.getTerms().stream()
                                .filter(term -> !"orgId".equals(term.getColumn())).collect(Collectors.toList()) );

            return queryHelper
            .select("select ar.* from alarm_record ar" +
                        " inner join (SELECT id FROM dev_device_instance where org_id in ('"+ids+"')) device on ar.source_id = device.id",
                    AlarmRecordEntity::new)
            //根据前端的动态条件参数自动构造查询条件以及分页排序等信息
            .where(param)
            .fetchPaged();
        });
    }

    //原生SQL方式
    @PostMapping("/handle-history/_query_native")
    @Operation(summary = "根据用户所属组织查询所有告警处理记录")
    @QueryAction
    public Mono<PagerResult<AlarmRecordEntity>> getHandleHistoryByOrgId(@RequestBody Mono<QueryParamEntity> queryParam) {
        return queryParam.flatMap(param -> {
//            param.getTerms().stream().findFirst();
            // 使用stream获取orgId的value
            Optional<Object> orgIds = param.getTerms().stream()
                                           .filter(term -> "orgId".equals(term.getColumn())) // 过滤出column为orgId的term
                                           .map(term -> term.getValue()) // 提取value字段
                                           .findFirst(); // 只取第一个匹配的值

            String ids = (String) orgIds.orElse("");
            //得到orgid后terms再次过滤掉,防止在where(param)中报错
            param.setTerms(param.getTerms().stream()
                                .filter(term -> !"orgId".equals(term.getColumn())).collect(Collectors.toList()) );

            return queryHelper
                .select("select his.alarm_time, dev.alarm_name, dev.target_name, dev.state, dev.source_id from alarm_handle_history his INNER JOIN (select ar.* from alarm_record ar inner join (SELECT id FROM dev_device_instance where org_id in ('"+ids+"')) device on ar.source_id = device.id) dev on his.alarm_record_id = dev.id order by alarm_time desc",
                        AlarmRecordEntity::new)
                //根据前端的动态条件参数自动构造查询条件以及分页排序等信息
                .where(param)
                .fetchPaged();
        });
    }

    //原生SQL方式
    @PostMapping("/all-handle-history/_query_native")
    @Operation(summary = "admin用户查询所有告警处理记录")
    @QueryAction
    public Mono<PagerResult<AlarmRecordEntity>> getAllHandleHistory(@RequestBody Mono<QueryParamEntity> queryParam) {
        return queryParam.flatMap(param -> {
//            param.getTerms().stream().findFirst();
            // 使用stream获取orgId的value
            Optional<Object> orgIds = param.getTerms().stream()
                                           .filter(term -> "orgId".equals(term.getColumn())) // 过滤出column为orgId的term
                                           .map(term -> term.getValue()) // 提取value字段
                                           .findFirst(); // 只取第一个匹配的值

            String ids = (String) orgIds.orElse("");
            //得到orgid后terms再次过滤掉,防止在where(param)中报错
            param.setTerms(param.getTerms().stream()
                                .filter(term -> !"orgId".equals(term.getColumn())).collect(Collectors.toList()) );

            return queryHelper
                .select("select his.alarm_time, dev.alarm_name, dev.target_name, dev.state, dev.source_id from alarm_handle_history his INNER JOIN (select ar.* from alarm_record ar inner join (SELECT id FROM dev_device_instance ) device on ar.source_id = device.id) dev on his.alarm_record_id = dev.id order by alarm_time desc",
                        AlarmRecordEntity::new)
                //根据前端的动态条件参数自动构造查询条件以及分页排序等信息
                .where(param)
                .fetchPaged();
        });
    }
}
