package cn.oa.admin.mapper;

import cn.oa.admin.entity.AdmAsset;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

/**
 * 资产管理 Mapper.
 */
@Mapper
public interface AdmAssetMapper extends BaseMapper<AdmAsset> {

    /**
     * 按部门和状态查询资产列表.
     */
    @Select("SELECT * FROM adm_asset WHERE dept_id = #{deptId} AND status = #{status} AND del_flag = '0' ORDER BY create_time DESC")
    List<AdmAsset> findByDeptIdAndStatus(@Param("deptId") Long deptId, @Param("status") String status);
}
