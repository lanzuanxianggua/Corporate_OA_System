package cn.oa.admin.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@TableName("adm_asset_log")
public class AdmAssetLog {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long assetId;

    private String operation;

    private Long operatorId;

    private Long fromUserId;

    private Long toUserId;

    private Long fromDeptId;

    private Long toDeptId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime operationTime;

    private String remark;

    private Long processInstanceId;

    @JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime createdAt;
}