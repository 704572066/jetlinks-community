package org.jetlinks.community.video.entity;

import io.swagger.v3.oas.annotations.Hidden;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;

import org.hswebframework.ezorm.rdb.mapping.annotation.*;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.api.crud.entity.RecordCreationEntity;
import org.hswebframework.web.api.crud.entity.RecordModifierEntity;
import org.hswebframework.web.crud.annotation.EnableEntityEvent;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.validator.CreateGroup;


import javax.persistence.Column;
import javax.persistence.GeneratedValue;

import javax.persistence.Table;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;


@Getter
@Setter
@Table(name = "video_device_instance")
@Comment("视频设备信息表")
@EnableEntityEvent
public class VideoDeviceInstanceEntity extends GenericEntity<String> implements RecordCreationEntity, RecordModifierEntity {

    @Override
    @GeneratedValue(generator = Generators.SNOW_FLAKE)
    @Pattern(
//        regexp = "^[0-9a-zA-Z_\\-]+$",
//        message = "ID只能由数字,字母,下划线和中划线组成",
        regexp = "^[0-9a-zA-Z_\\-:]+$",
        message = "ID只能由数字、字母、下划线、中划线和冒号组成",
        groups = CreateGroup.class)
    @Schema(description = "ID")
    public String getId() {
        return super.getId();
    }

    @Column(name = "name")
    @NotBlank(message = "视频设备名称不能为空", groups = CreateGroup.class)
    @Schema(description = "设备名称")
    private String name;

//    @Column(name = "access_token")
//    @NotBlank(message = "视频设备令牌不能为空", groups = CreateGroup.class)
//    @Schema(description = "设备令牌")
//    private String name;

    @Column(name = "photo_url", length = 1024)
    @Schema(description = "图片地址")
    private String photoUrl;

    @Column(name = "describe")
    @Schema(description = "说明")
    private String describe;

//    @Column(name = "state")
//    @DefaultValue("0")
//    @Schema(description = "设备状态 1正常,0禁用")
//    private Byte state;

    @Column(name = "creator_id", updatable = false)
    @Schema(description = "创建者ID(只读)")
    private String creatorId;

    @Column(name = "create_time", updatable = false)
    @Schema(description = "创建者时间(只读)")
    @DefaultValue(generator = Generators.CURRENT_TIME)
    private Long createTime;

    @Column(name = "org_id", length = 64)
    @Schema(description = "机构ID")
    @Deprecated
    @Hidden
    private String orgId;

    @Column(length = 64)
    @Schema(description = "修改人")
    private String modifierId;

    @Column
    @Schema(description = "修改时间")
    private Long modifyTime;

    public void validateId() {
        tryValidate(VideoDeviceInstanceEntity::getId, CreateGroup.class);
    }
}
