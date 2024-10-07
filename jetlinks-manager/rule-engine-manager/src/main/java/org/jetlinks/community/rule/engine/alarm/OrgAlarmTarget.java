package org.jetlinks.community.rule.engine.alarm;

import org.jetlinks.community.device.response.DeviceDetail;
import org.springframework.beans.factory.annotation.Autowired;
import reactor.core.publisher.Flux;
import org.jetlinks.community.device.service.LocalDeviceInstanceService;
import reactor.core.publisher.Mono;

import java.util.Map;
/**
 * @author bestfeng
 */
public class OrgAlarmTarget extends AbstractAlarmTarget {

//    private final LocalDeviceInstanceService service;

//    private final UserService userService;

//    @Autowired
//    public OrgAlarmTarget(LocalDeviceInstanceService service) {
//        this.service = service;
//    }

    @Override
    public String getType() {
        return "org";
    }

    @Override
    public String getName() {
        return "组织";
    }

    @Override
    public Flux<AlarmTargetInfo> doConvert(AlarmData data) {
        Map<String, Object> output = data.getOutput();
//        String deviceId = AbstractAlarmTarget.getFromOutput("deviceId", output).map(String::valueOf).orElse(null);
//
//        Mono<DeviceDetail> deviceDetail = service.getDeviceDetail(deviceId);
//        String orgId = deviceDetail.map(DeviceDetail::getOrgId).block();
        String orgId = AbstractAlarmTarget.getFromOutput("orgId", output).map(String::valueOf).orElse(null);
        String orgName = AbstractAlarmTarget.getFromOutput("orgName", output).map(String::valueOf).orElse(orgId);

        return Flux.just(AlarmTargetInfo.of(orgId, orgName, getType()));
    }


}
