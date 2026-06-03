package cn.oa.document.dto;

import lombok.Data;

/**
 * 发文查询DTO
 *
 * @author oa-document
 */
@Data
public class DocDispatchQueryDTO {

    /** 关键字(标题) */
    private String keyword;

    /** 状态 */
    private String status;

    /** 密级 */
    private String securityLevel;

    /** 开始日期 */
    private String startDate;

    /** 结束日期 */
    private String endDate;
}
