package cn.oa.hr.employee.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

/**
 * 员工档案查询 DTO.
 */
@Data
@Schema(description = "员工档案查询参数")
public class HrEmployeeProfileQueryDTO {

    @Schema(description = "关键词 (姓名/工号/用户名)")
    private String keyword;

    @Schema(description = "状态: ACTIVE/LEAVE/RESIGN")
    private String status;

    @Schema(description = "合同类型: PROBATION/REGULAR/CONTRACT/INTERN")
    private String contractType;

    @Schema(description = "页码(从 1 开始)", example = "1")
    private Integer pageNum = 1;

    @Schema(description = "每页大小", example = "10")
    private Integer pageSize = 10;
}
