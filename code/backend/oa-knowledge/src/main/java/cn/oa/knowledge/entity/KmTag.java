package cn.oa.knowledge.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

/**
 * 知识标签实体
 */
@Data
@TableName("km_tag")
public class KmTag {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long entryId;

    private String tagName;

    private String tagType; // CUSTOM/SYSTEM/CATEGORY
}
