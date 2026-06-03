package cn.oa.document.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

/**
 * 公文修订版本创建DTO
 *
 * @author oa-document
 */
@Data
public class DocRevisionCreateDTO {

    /** 发文ID */
    @NotNull(message = "发文ID不能为空")
    private Long dispatchId;

    /** 正文内容/附件路径 */
    @NotBlank(message = "正文内容不能为空")
    private String content;

    /** 版本备注 */
    private String comment;
}
