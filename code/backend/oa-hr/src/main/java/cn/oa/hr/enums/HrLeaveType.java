package cn.oa.hr.enums;

import lombok.AllArgsConstructor;
import lombok.Getter;

/**
 * HR请假类型枚举
 * 对应 hr_leave_apply.leave_type 字段
 *
 * @author oa-hr
 */
@Getter
@AllArgsConstructor
public enum HrLeaveType {

    PERSONAL("PERSONAL", "事假"),
    ANNUAL("ANNUAL", "年假"),
    SICK("SICK", "病假"),
    MARRIAGE("MARRIAGE", "婚假"),
    FUNERAL("FUNERAL", "丧假"),
    MATERNITY("MATERNITY", "产假"),
    PATERNITY("PATERNITY", "陪产假"),
    COMPENSATORY("COMPENSATORY", "调休"),
    OTHER("OTHER", "其他");

    private final String code;
    private final String name;

    /**
     * 根据code获取枚举
     */
    public static HrLeaveType fromCode(String code) {
        if (code == null) {
            return null;
        }
        for (HrLeaveType type : values()) {
            if (type.getCode().equals(code)) {
                return type;
            }
        }
        return null;
    }
}
