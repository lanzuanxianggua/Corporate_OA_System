package cn.oa.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 收文拟办DTO
 *
 * @author oa-document
 */
@Data
public class DocReceiveProposeDTO {

    @NotNull(message = "收文ID不能为空")
    private Long id;

    /** 拟办意见 */
    @NotBlank(message = "拟办意见不能为空")
    private String proposedOpinion;
}
