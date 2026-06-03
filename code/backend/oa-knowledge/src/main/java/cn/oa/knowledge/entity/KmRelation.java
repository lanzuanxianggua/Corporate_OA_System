package cn.oa.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.time.LocalDateTime;

/**
 * 知识关联实体
 */
@Data
@TableName("km_relation")
public class KmRelation {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long entryId;

    private Long relatedEntryId;

    private String relationType; // REFERENCE/SIMILAR/PARENT/SEE_ALSO

    private Double score;

    private LocalDateTime createdAt;
}
