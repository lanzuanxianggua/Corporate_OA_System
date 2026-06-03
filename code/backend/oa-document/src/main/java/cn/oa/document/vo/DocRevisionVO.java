package cn.oa.document.vo;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 公文修订版本VO
 *
 * @author oa-document
 */
@Data
public class DocRevisionVO {

    private Long id;

    /** 发文ID */
    private Long dispatchId;

    /** 版本号 */
    private Integer versionNo;

    /** 正文内容/附件路径 */
    private String content;

    /** 编辑人ID */
    private Long editorId;

    /** 编辑人姓名 */
    private String editorName;

    /** 编辑时间 */
    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime editTime;

    /** 版本备注 */
    private String comment;

    /** 是否清稿(0-否 1-是) */
    private Integer isClean;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createTime;
}
