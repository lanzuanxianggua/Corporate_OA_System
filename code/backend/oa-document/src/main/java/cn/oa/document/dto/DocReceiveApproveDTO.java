package cn.oa.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 收文批办DTO
 *
 * @author oa-document
 */
@Data
public class DocReceiveApproveDTO {

    @NotNull(message = "收文ID不能为空")
    private Long id;

    /** 批办意见 */
    @NotBlank(message = "批办意见不能为空")
    private String approvedOpinion;

    /** 承办人ID */
    @NotNull(message = "承办人不能为空")
    private Long handlerId;
}
