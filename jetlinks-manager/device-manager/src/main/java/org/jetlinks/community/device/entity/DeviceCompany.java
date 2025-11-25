package org.jetlinks.community.device.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;


@Data
@NoArgsConstructor
@AllArgsConstructor
@Schema(description = "组织及设备数量统计结果")
public class DeviceCompany {
    @Schema(description = "组织ID")
    private String orgId;

    @Schema(description = "组织名称")
    private String orgName;

    @Schema(description = "设备数量")
    private int deviceNum;

    @Schema(description = "在线数量")
    private int onlineCount;

    @Schema(description = "离线数量")
    private int offlineCount;
}
