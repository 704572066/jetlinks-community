package org.jetlinks.community.video.response;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.video.entity.VideoDeviceInstanceEntity;

import java.util.Optional;

@Getter
@Setter
public class TokenResult {
    private String code;
    private String msg;
    private Data data;

    // Getter
    @Getter
    public static class Data {
        private String accessToken;
        private long expireTime;

        // Getter & Setter
    }
}