package cn.oa.entity.dto;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.io.Serializable;

/**
 * ID查询DTO - 用于按ID查询的场景
 * 支持通过 id 或 userId 字段传参
 */
@Data
public class IdQueryDTO implements Serializable {

    private static final long serialVersionUID = 1L;

    /** 通用ID */
    private Long id;

    /** 用户ID（兼容前端传参） */
    private Long userId;

    /**
     * 获取有效ID，优先使用id，其次使用userId
     */
    public Long getEffectiveId() {
        return id != null ? id : userId;
    }
}
