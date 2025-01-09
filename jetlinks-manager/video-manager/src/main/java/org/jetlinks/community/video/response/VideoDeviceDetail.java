package org.jetlinks.community.video.response;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.jetlinks.community.video.entity.VideoDeviceInstanceEntity;
import java.util.*;

@Getter
@Setter
public class VideoDeviceDetail {

    //设备ID
    @Schema(description = "设备ID")
    private String id;

    //设备名称
    @Schema(description = "设备名称")
    private String name;

    //设备图片
    @Schema(description = "图片地址")
    private String photoUrl;


    //所属机构ID
    @Schema(description = "机构ID")
    private String orgId;

    //所属机构名称
    @Schema(description = "机构名称")
    private String orgName;

    //创建时间
    @Schema(description = "创建时间")
    private long createTime;

    //激活时间
    @Schema(description = "激活时间")
    private long registerTime;

    @Schema(description = "设备描述")
    private String description;

    @Schema(description = "萤石视频URL")
    private String streamUrl;



//    public VideoDeviceDetail notActive() {
//
//        state = DeviceState.notActive;
//        initTags();
//        return this;
//    }

//    private DeviceMetadata decodeMetadata() {
//        if (StringUtils.hasText(metadata)) {
//            return JetLinksDeviceMetadataCodec.getInstance().doDecode(metadata);
//        }
//        return null;
//    }
//
//    private void mergeDeviceMetadata(String deviceMetadata) {
//        mergeDeviceMetadata(JetLinksDeviceMetadataCodec.getInstance().doDecode(deviceMetadata));
//    }

//    private void mergeDeviceMetadata(DeviceMetadata deviceMetadata) {
//        if (!StringUtils.hasText(productMetadata)) {
//            metadata = JetLinksDeviceMetadataCodec.getInstance().doEncode(deviceMetadata);
//            return;
//        }
//
//        //合并物模型
//        metadata = JetLinksDeviceMetadataCodec
//            .getInstance()
//            .doEncode(new CompositeDeviceMetadata(
//                JetLinksDeviceMetadataCodec.getInstance().doDecode(productMetadata),
//                deviceMetadata
//            ));
//
//    }

//    private void initTags() {
//        DeviceMetadata metadata = decodeMetadata();
//        if (null != metadata) {
//            with(metadata
//                     .getTags()
//                     .stream()
//                     .map(DeviceTagEntity::of)
//                     .collect(Collectors.toList()));
//        }
//    }


//    public VideoDeviceDetail with(DeviceProductEntity productEntity) {
//        if (productEntity == null) {
//            return this;
//        }
//        setProductMetadata(productEntity.getMetadata());
//        if (CollectionUtils.isEmpty(configuration) && !CollectionUtils.isEmpty(productEntity.getConfiguration())) {
//            configuration.putAll(productEntity.getConfiguration());
//        }
//        setProtocol(productEntity.getMessageProtocol());
//        setTransport(productEntity.getTransportProtocol());
//        setPhotoUrl(productEntity.getPhotoUrl());
//        setProductId(productEntity.getId());
//        setProductName(productEntity.getName());
//        setDeviceType(productEntity.getDeviceType());
//        setProtocolName(productEntity.getProtocolName());
//        setAccessProvider(productEntity.getAccessProvider());
//        setAccessId(productEntity.getAccessId());
//        setAccessName(productEntity.getAccessName());
//        setClassifiedId(productEntity.getClassifiedId());
//        setClassifiedName(productEntity.getClassifiedName());
//        return this;
//    }

    public VideoDeviceDetail with(VideoDeviceInstanceEntity device) {

        setId(device.getId());
        setName(device.getName());
//        setState(device.getState());
//        setParentId(device.getParentId());
        setDescription(device.getDescribe());
        setOrgId(device.getOrgId());
//        if (device.getFeatures() != null) {
//            withFeatures(Arrays.asList(device.getFeatures()));
//        }
//        Optional.ofNullable(device.getRegistryTime())
//                .ifPresent(this::setRegisterTime);

        Optional.ofNullable(device.getCreateTime())
                .ifPresent(this::setCreateTime);

//        if (MapUtils.isNotEmpty(device.getConfiguration())) {
//            boolean hasConfig = device
//                .getConfiguration()
//                .keySet()
//                .stream()
//                .map(configuration::get)
//                .anyMatch(Objects::nonNull);
//            if (hasConfig) {
//                setAloneConfiguration(true);
//            }
//            configuration.putAll(device.getConfiguration());
//        }
//        if (StringUtils.hasText(device.getDeriveMetadata())) {
//            mergeDeviceMetadata(device.getDeriveMetadata());
//            setIndependentMetadata(true);
//        }
//
//        for (DeviceTagEntity tag : getTags()) {
//            tag.setId(DeviceTagEntity.createTagId(id, tag.getKey()));
//        }

        return this;
    }

//    public VideoDeviceDetail withFeatures(Collection<? extends Feature> features) {
//        for (Feature feature : features) {
//            this.features.add(new SimpleFeature(feature.getId(), feature.getName()));
//        }
//        return this;
//    }

//    public Mono<VideoDeviceDetail> with(DeviceProductOperator product) {
//        return Mono
//            .zip(
//                product
//                    .getProtocol()
//                    .mapNotNull(ProtocolSupport::getName)
//                    .defaultIfEmpty(""),
//                product
//                    .getConfig(DeviceConfigKey.metadata)
//                    .defaultIfEmpty(""))
//            .doOnNext(tp2 -> {
//                setProtocolName(tp2.getT1());
//                //物模型以产品缓存里的为准
//                if (!this.independentMetadata && StringUtils.hasText(tp2.getT2())) {
//                    setMetadata(tp2.getT2());
//                }
//            })
//            .thenReturn(this);
//    }


}
