package cn.oa.service.impl;

import cn.oa.common.service.RedisService;
import cn.oa.entity.SysDept;
import cn.oa.entity.SysEmployee;
import cn.oa.vo.OnlineUserVO;
import cn.oa.mapper.SysDeptMapper;
import cn.oa.mapper.SysEmployeeMapper;
import cn.oa.service.OnlineUserService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class OnlineUserServiceImpl implements OnlineUserService {

    private static final String ONLINE_KEY_PREFIX = "online:user:";
    private static final long ONLINE_TTL_MINUTES = 30;
    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

    @Autowired
    private RedisTemplate<String, Object> redisTemplate;

    @Autowired
    private RedisService redisService;

    @Autowired
    private SysEmployeeMapper employeeMapper;

    @Autowired
    private SysDeptMapper deptMapper;

    @Override
    public void userLogin(Long empId, String empName, String ip, String browser) {
        String key = ONLINE_KEY_PREFIX + empId;
        Map<String, String> info = new HashMap<>();
        info.put("empId", empId.toString());
        info.put("empName", empName);
        info.put("ip", ip);
        info.put("browser", browser);
        info.put("loginTime", LocalDateTime.now().format(FMT));
        redisTemplate.opsForHash().putAll(key, info);
        redisTemplate.expire(key, ONLINE_TTL_MINUTES, TimeUnit.MINUTES);
    }

    @Override
    public void userLogout(Long empId) {
        redisTemplate.delete(ONLINE_KEY_PREFIX + empId);
    }

    @Override
    public void refreshTTL(Long empId) {
        String key = ONLINE_KEY_PREFIX + empId;
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            redisTemplate.expire(key, ONLINE_TTL_MINUTES, TimeUnit.MINUTES);
        }
    }

    @Override
    public List<OnlineUserVO> getOnlineUsers() {
        Set<String> keys = redisTemplate.keys(ONLINE_KEY_PREFIX + "*");
        if (keys == null || keys.isEmpty()) return Collections.emptyList();

        List<OnlineUserVO> result = new ArrayList<>();
        for (String key : keys) {
            Map<Object, Object> entries = redisTemplate.opsForHash().entries(key);
            OnlineUserVO vo = new OnlineUserVO();
            vo.setEmpId(Long.parseLong(entries.get("empId").toString()));
            vo.setEmpName(entries.get("empName").toString());
            vo.setIp(entries.get("ip").toString());
            vo.setBrowser(entries.getOrDefault("browser", "").toString());
            vo.setLoginTime(entries.getOrDefault("loginTime", "").toString());

            SysEmployee emp = employeeMapper.selectById(vo.getEmpId());
            if (emp != null && emp.getDeptId() != null) {
                SysDept dept = deptMapper.selectById(emp.getDeptId());
                if (dept != null) {
                    vo.setDeptName(dept.getDeptName());
                }
            }

            result.add(vo);
        }
        result.sort(Comparator.comparing(OnlineUserVO::getLoginTime).reversed());
        return result;
    }
}
