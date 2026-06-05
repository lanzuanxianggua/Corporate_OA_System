package cn.oa.admin.mapper;

import cn.oa.admin.entity.AdmSeal;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 印章管理 Mapper.
 */
@Mapper
public interface AdmSealMapper extends BaseMapper<AdmSeal> {

    /**
     * 按部门ID查询印章列表.
     */
    @Select("SELECT * FROM adm_seal WHERE dept_id = #{deptId} AND del_flag = '0' ORDER BY create_time DESC")
    List<AdmSeal> findByDeptId(@Param("deptId") Long deptId);
}
