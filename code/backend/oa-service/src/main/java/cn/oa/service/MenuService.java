package cn.oa.service;

import cn.oa.entity.SysMenu;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface MenuService extends IService<SysMenu> {

    List<SysMenu> getMenuTree();

    List<Long> getMenuIdsByRoleId(Long roleId);

    void assignRoleMenus(Long roleId, List<Long> menuIds);
}
