package org.jetlinks.community.video.request;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
@AllArgsConstructor
public class TokenRequest {

    //设备ID
    @Schema(description = "设备ID")
    private String appKey;

    //设备名称
    @Schema(description = "设备名称")
    private String appSecret;





}
