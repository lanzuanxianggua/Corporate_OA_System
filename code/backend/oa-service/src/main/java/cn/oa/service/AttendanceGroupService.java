package cn.oa.service;

import cn.oa.entity.OaAttendanceGroup;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface AttendanceGroupService extends IService<OaAttendanceGroup> {

    IPage<OaAttendanceGroup> pageList(int pageNum, int pageSize, String groupName);

    void assignEmployees(Long groupId, List<Long> empIds);

    void removeEmployees(Long groupId, List<Long> empIds);
}
