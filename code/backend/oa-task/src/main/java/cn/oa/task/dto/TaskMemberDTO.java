package cn.oa.task.dto;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
@Schema(description = "添加项目成员请求")
public class TaskMemberDTO {

    @NotNull(message = "项目ID不能为空")
    @Schema(description = "项目ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long projectId;

    @NotNull(message = "员工ID不能为空")
    @Schema(description = "员工ID", requiredMode = Schema.RequiredMode.REQUIRED)
    private Long empId;

    @Schema(description = "角色：OWNER, ADMIN, MEMBER")
    private String role;
}
