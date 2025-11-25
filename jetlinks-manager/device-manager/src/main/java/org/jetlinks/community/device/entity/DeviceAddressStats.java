package org.jetlinks.community.device.entity;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class DeviceAddressStats {
    private String address;
    private long online;
    private long offline;
    private long total;
}
