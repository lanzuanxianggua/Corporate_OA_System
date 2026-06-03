package cn.oa.platform.core.constant;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * 通用状态枚举
 *
 * @author oa-platform
 */
@Getter
@AllArgsConstructor
public enum StatusEnum {

    /**
     * 启用
     */
    ENABLE(0, "启用"),

    /**
     * 禁用
     */
    DISABLE(1, "禁用"),

    /**
     * 删除
     */
    DELETED(2, "已删除"),

    /**
     * 待审核
     */
    PENDING(10, "待审核"),

    /**
     * 已通过
     */
    APPROVED(11, "已通过"),

    /**
     * 已拒绝
     */
    REJECTED(12, "已拒绝"),

    /**
     * 已取消
     */
    CANCELED(13, "已取消"),

    /**
     * 进行中
     */
    PROCESSING(20, "进行中"),

    /**
     * 已完成
     */
    COMPLETED(21, "已完成"),

    /**
     * 已关闭
     */
    CLOSED(22, "已关闭");

    /**
     * 状态码
     */
    private final int code;

    /**
     * 状态描述
     */
    private final String desc;

    /**
     * 根据状态码获取枚举
     */
    public static StatusEnum getByCode(int code) {
        for (StatusEnum status : values()) {
            if (status.getCode() == code) {
                return status;
            }
        }
        return null;
    }

    /**
     * 判断是否为有效状态（非删除）
     */
    public static boolean isValid(int code) {
        return code != DELETED.getCode();
    }

    /**
     * 判断是否为启用状态
     */
    public static boolean isEnable(int code) {
        return code == ENABLE.getCode();
    }
}
