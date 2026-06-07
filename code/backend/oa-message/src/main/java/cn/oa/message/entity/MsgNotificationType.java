package cn.oa.message.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableLogic;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.io.Serializable;
import java.time.LocalDateTime;
import java.util.Objects;

/**
 * 消息通知类型字典.
 *
 * <p>对应表 msg_notification_types (V988 已建).
 * 物理表字段与 BaseEntity 部分对齐, 不继承 BaseEntity 是因为不需要 IdType.ASSIGN_ID (雪花);
 * 改用 AUTO_INCREMENT + 手写审计字段.
 */
@Data
@TableName("msg_notification_types")
@Schema(description = "消息通知类型")
public class MsgNotificationType implements Serializable {

    private static final long serialVersionUID = 1L;

    @TableId(type = IdType.AUTO)
    @Schema(description = "主键")
    private Long id;

    @Schema(description = "类型编码, 唯一")
    @TableField("code")
    private String code;

    @Schema(description = "类型名称")
    @TableField("name")
    private String name;

    @Schema(description = "描述")
    @TableField("description")
    private String description;

    @Schema(description = "0=禁用 1=启用")
    @TableField("enabled")
    private Integer enabled;

    @Schema(description = "排序")
    @TableField("sort_order")
    private Integer sortOrder;

    @TableLogic
    @TableField(select = false)
    @Schema(description = "0=正常 1=删除")
    private String delFlag;

    @Schema(description = "创建人")
    @TableField("create_by")
    private String createBy;

    @Schema(description = "创建时间")
    @TableField("create_time")
    private LocalDateTime createTime;

    @Schema(description = "更新人")
    @TableField("update_by")
    private String updateBy;

    @Schema(description = "更新时间")
    @TableField("update_time")
    private LocalDateTime updateTime;

    @Version
    @Schema(description = "乐观锁版本号")
    private Integer version;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        MsgNotificationType that = (MsgNotificationType) o;
        return Objects.equals(id, that.id);
    }

    @Override
    public int hashCode() { return Objects.hash(id); }
}
