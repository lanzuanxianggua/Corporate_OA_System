package cn.oa.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 收文承办DTO
 *
 * @author oa-document
 */
@Data
public class DocReceiveHandleDTO {

    @NotNull(message = "收文ID不能为空")
    private Long id;

    /** 承办意见 */
    @NotBlank(message = "承办意见不能为空")
    private String handledOpinion;
}
