package cn.oa.system.vo;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;

import java.util.ArrayList;
import java.util.List;

/**
 * 菜单树节点 VO.
 */
@Data
@Schema(description = "菜单树节点")
public class MenuTreeVO {

    @Schema(description = "权限 ID")
    private Long id;

    @Schema(description = "父权限 ID")
    private Long parentId;

    @Schema(description = "权限名称")
    private String name;

    @Schema(description = "前端路由路径")
    private String path;

    @Schema(description = "图标名")
    private String icon;

    @Schema(description = "权限码")
    private String permCode;

    @Schema(description = "排序")
    private Integer sort;

    @Schema(description = "类型: MENU/OUTER_MENU/BUTTON")
    private String type;

    @Schema(description = "是否隐藏")
    private Boolean hidden = false;

    @Schema(description = "子节点")
    private List<MenuTreeVO> children = new ArrayList<>();
}
