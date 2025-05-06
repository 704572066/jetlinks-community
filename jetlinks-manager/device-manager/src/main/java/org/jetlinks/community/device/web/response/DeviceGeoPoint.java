package org.jetlinks.community.device.web.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public class DeviceGeoPoint {
    private String id;
    private String name;
    private String geoPoint;
    private long timestamp;

    public DeviceGeoPoint(String deviceId, String name, String geoPoint, long timestamp) {
        this.id = deviceId;
        this.name = name;
        this.geoPoint = geoPoint;
        this.timestamp = timestamp;
    }

    // Getters & setters
}

