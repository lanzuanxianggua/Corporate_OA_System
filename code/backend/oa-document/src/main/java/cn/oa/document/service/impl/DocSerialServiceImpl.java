package cn.oa.document.service.impl;

import cn.oa.document.entity.DocSerial;
import cn.oa.document.mapper.DocSerialMapper;
import cn.oa.document.service.DocSerialService;
import cn.oa.platform.core.exception.BusinessException;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

/**
 * 文号管理服务实现
 *
 * @author oa-document
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class DocSerialServiceImpl extends ServiceImpl<DocSerialMapper, DocSerial> implements DocSerialService {

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String lockSerial(String orgCode, Integer year, Long lockBy) {
        // 查找该年代字的最大流水号
        LambdaQueryWrapper<DocSerial> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(DocSerial::getOrgCode, orgCode)
                .eq(DocSerial::getYear, year)
                .orderByDesc(DocSerial::getSerialNo)
                .last("LIMIT 1");
        DocSerial last = baseMapper.selectOne(wrapper);

        int nextNo = (last != null) ? last.getSerialNo() + 1 : 1;

        DocSerial serial = new DocSerial();
        serial.setOrgCode(orgCode);
        serial.setYear(year);
        serial.setSerialNo(nextNo);
        serial.setStatus("LOCKED");
        serial.setLockedBy(lockBy);
        serial.setLockedAt(LocalDateTime.now());
        baseMapper.insert(serial);

        log.info("文号已锁定: orgCode={}, year={}, serialNo={}, lockBy={}", orgCode, year, nextNo, lockBy);
        return formatSerialNo(orgCode, year, nextNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void releaseSerial(Long id) {
        DocSerial serial = baseMapper.selectById(id);
        if (serial == null) {
            throw new BusinessException("文号不存在");
        }
        if (!"LOCKED".equals(serial.getStatus())) {
            throw new BusinessException("仅锁定状态的文号可释放");
        }
        serial.setStatus("ACTIVE");
        serial.setLockedBy(null);
        serial.setLockedAt(null);
        baseMapper.updateById(serial);
        log.info("文号已释放: id={}", id);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void useSerial(Long id, Long dispatchId) {
        DocSerial serial = baseMapper.selectById(id);
        if (serial == null) {
            throw new BusinessException("文号不存在");
        }
        if (!"LOCKED".equals(serial.getStatus())) {
            throw new BusinessException("仅锁定状态的文号可使用");
        }
        serial.setStatus("ACTIVE");
        serial.setUsedAt(LocalDateTime.now());
        baseMapper.updateById(serial);
        log.info("文号已使用: id={}, dispatchId={}", id, dispatchId);
    }

    @Override
    public String formatSerialNo(String orgCode, Integer year, Integer serialNo) {
        return orgCode + "〔" + year + "〕" + serialNo + "号";
    }
}
