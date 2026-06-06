package cn.oa.system.mapper;

import cn.oa.system.entity.SysEmp;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.time.LocalDateTime;

@Mapper
public interface SysEmpMapper extends BaseMapper<SysEmp> {

    /**
     * 按用户名查找员工 (过滤软删).
     */
    @Select("SELECT * FROM sys_employee " +
            "WHERE username = #{username} AND del_flag = '0' " +
            "LIMIT 1")
    SysEmp selectByUsername(@Param("username") String username);

    /**
     * 更新最后登录时间与 IP (带乐观锁).
     */
    @Update("UPDATE sys_employee SET " +
            "last_login_time = #{ts}, last_login_ip = #{ip} " +
            "WHERE id = #{id} AND version = #{version}")
    int updateLastLogin(@Param("id") Long id,
                        @Param("ts") LocalDateTime ts,
                        @Param("ip") String ip,
                        @Param("version") Integer version);

    /**
     * 更新密码.
     */
    @Update("UPDATE sys_employee SET " +
            "password = #{hash} " +
            "WHERE id = #{id} AND del_flag = '0'")
    int updatePassword(@Param("id") Long id,
                       @Param("hash") String hash);
}
