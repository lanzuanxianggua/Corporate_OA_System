package cn.oa.hr.dto;

import lombok.Data;

/**
 * HR请假查询DTO
 *
 * @author oa-hr
 */
@Data
public class HrLeaveQueryDTO {

    /**
     * 页码
     */
    private Integer pageNum = 1;

    /**
     * 每页条数
     */
    private Integer pageSize = 10;

    /**
     * 员工ID（管理端可传，普通用户忽略）
     */
    private Long empId;

    /**
     * 部门ID（管理端/数据权限过滤）
     */
    private Long deptId;

    /**
     * 状态
     */
    private String status;

    /**
     * 假期类型
     */
    private String leaveType;

    /**
     * 查询起始日期
     */
    private String startDate;

    /**
     * 查询结束日期
     */
    private String endDate;

    /**
     * 排序字段
     */
    private String sortField;

    /**
     * 排序方式(asc/desc)
     */
    private String sortOrder;
}
