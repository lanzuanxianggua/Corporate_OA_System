package cn.oa.service.impl;

import cn.oa.entity.OaNotice;
import cn.oa.entity.SysEmployee;
import cn.oa.mapper.OaNoticeMapper;
import cn.oa.mapper.SysEmployeeMapper;
import cn.oa.service.NoticeService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
public class NoticeServiceImpl extends ServiceImpl<OaNoticeMapper, OaNotice> implements NoticeService {

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private SysEmployeeMapper employeeMapper;

    @Override
    public IPage<OaNotice> pageList(int pageNum, int pageSize) {
        Page<OaNotice> page = new Page<>(pageNum, pageSize);
        IPage<OaNotice> result = this.page(page);

        // 填充 publisher 名称
        fillPublisherNames(result.getRecords());

        return result;
    }

    @Override
    public void markAsRead(Long noticeId, Long empId) {
        redisTemplate.opsForSet().add("notice:read:" + noticeId, empId);
    }

    @Override
    public boolean isRead(Long noticeId, Long empId) {
        Boolean member = redisTemplate.opsForSet().isMember("notice:read:" + noticeId, empId);
        return member != null && member;
    }

    private void fillPublisherNames(List<OaNotice> notices) {
        if (notices == null || notices.isEmpty()) return;
        Set<Long> publisherIds = notices.stream()
                .map(OaNotice::getPublisherId)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
        if (publisherIds.isEmpty()) return;

        List<SysEmployee> publishers = employeeMapper.selectBatchIds(publisherIds);
        Map<Long, String> nameMap = publishers.stream()
                .collect(Collectors.toMap(SysEmployee::getId, SysEmployee::getEmpName, (a, b) -> a));

        for (OaNotice notice : notices) {
            if (notice.getPublisherId() != null) {
                notice.setPublisher(nameMap.getOrDefault(notice.getPublisherId(), ""));
            }
        }
    }
}
