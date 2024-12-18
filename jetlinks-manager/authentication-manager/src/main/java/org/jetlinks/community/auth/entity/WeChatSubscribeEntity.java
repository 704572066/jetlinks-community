package org.jetlinks.community.auth.entity;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.validator.constraints.Length;
import org.hswebframework.ezorm.rdb.mapping.annotation.ColumnType;
import org.hswebframework.ezorm.rdb.mapping.annotation.Comment;
import org.hswebframework.ezorm.rdb.mapping.annotation.DefaultValue;
import org.hswebframework.web.api.crud.entity.GenericEntity;
import org.hswebframework.web.crud.generator.Generators;
import org.hswebframework.web.utils.DigestUtils;
import org.hswebframework.web.validator.CreateGroup;
import org.springframework.util.StringUtils;

import javax.persistence.Column;
import javax.persistence.Index;
import javax.persistence.Table;
import java.sql.JDBCType;

@Getter
@Setter
@Table(name = "wx_subscribe")
@Comment("微信公众号订阅信息")
public class WeChatSubscribeEntity extends GenericEntity<String> {

//    @Schema(description = "用户openID")
//    @Column(length = 64, nullable = false, updatable = true)
////    @Length(max = 64, groups = CreateGroup.class)
//    private String id;

    @Schema(description = "uinonid")
    @Column(length = 64, nullable = false, updatable = true)
    @Length(max = 64)
    private String uinonid;

    @Column(name = "status", length = 16)
    @Schema(description = "用户状态。1订阅，0取消订阅")
    private int status;

}
