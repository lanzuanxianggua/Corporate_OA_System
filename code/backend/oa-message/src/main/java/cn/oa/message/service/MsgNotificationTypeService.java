package cn.oa.message.service;

import cn.oa.message.entity.MsgNotificationType;
import cn.oa.message.mapper.MsgNotificationTypeMapper;
import cn.oa.message.vo.MsgNotificationTypeVO;
import cn.oa.platform.common.api.RCode;
import cn.oa.platform.common.exception.BizException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

/**
 * 通知类型字典 Service.
 *
 * <p>提供类型字典的查询/管理能力, 业务 Service 通过 code 路由到具体类型.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class MsgNotificationTypeService {

    private final MsgNotificationTypeMapper typeMapper;

    /**
     * 列出所有启用的类型 (按 sort_order 升序).
     */
    public List<MsgNotificationTypeVO> listEnabled() {
        List<MsgNotificationType> rows = typeMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MsgNotificationType>()
                        .eq(MsgNotificationType::getEnabled, 1)
                        .orderByAsc(MsgNotificationType::getSortOrder));
        return rows.stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 列出所有类型 (含禁用) — 管理用.
     */
    public List<MsgNotificationTypeVO> listAll() {
        List<MsgNotificationType> rows = typeMapper.selectList(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MsgNotificationType>()
                        .orderByAsc(MsgNotificationType::getSortOrder));
        return rows.stream().map(this::toVO).collect(Collectors.toList());
    }

    /**
     * 按 code 查询 (返回 null 表示不存在).
     */
    public MsgNotificationType findByCode(String code) {
        return typeMapper.selectOne(
                new com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper<MsgNotificationType>()
                        .eq(MsgNotificationType::getCode, code));
    }

    /**
     * 校验类型存在且启用 — 不通过抛 BizException.
     */
    public void requireEnabled(String code) {
        MsgNotificationType t = findByCode(code);
        if (t == null) {
            throw new BizException(RCode.BAD_REQUEST, "通知类型不存在: " + code);
        }
        if (t.getEnabled() == null || t.getEnabled() != 1) {
            throw new BizException(RCode.BAD_REQUEST, "通知类型未启用: " + code);
        }
    }

    @Transactional
    public MsgNotificationType create(MsgNotificationType entity) {
        if (entity.getCode() == null || entity.getCode().isBlank()) {
            throw new BizException(RCode.BAD_REQUEST, "类型编码不能为空");
        }
        if (findByCode(entity.getCode()) != null) {
            throw new BizException(RCode.BAD_REQUEST, "类型编码已存在: " + entity.getCode());
        }
        if (entity.getEnabled() == null) {
            entity.setEnabled(1);
        }
        if (entity.getSortOrder() == null) {
            entity.setSortOrder(0);
        }
        typeMapper.insert(entity);
        log.info("通知类型已创建: code={}, name={}", entity.getCode(), entity.getName());
        return entity;
    }

    @Transactional
    public MsgNotificationType update(Long id, MsgNotificationType patch) {
        MsgNotificationType exist = typeMapper.selectById(id);
        if (exist == null) {
            throw new BizException(RCode.NOT_FOUND, "通知类型不存在: id=" + id);
        }
        if (patch.getName() != null) {
            exist.setName(patch.getName());
        }
        if (patch.getDescription() != null) {
            exist.setDescription(patch.getDescription());
        }
        if (patch.getEnabled() != null) {
            exist.setEnabled(patch.getEnabled());
        }
        if (patch.getSortOrder() != null) {
            exist.setSortOrder(patch.getSortOrder());
        }
        typeMapper.updateById(exist);
        log.info("通知类型已更新: id={}, code={}", id, exist.getCode());
        return exist;
    }

    @Transactional
    public void delete(Long id) {
        MsgNotificationType exist = typeMapper.selectById(id);
        if (exist == null) {
            throw new BizException(RCode.NOT_FOUND, "通知类型不存在: id=" + id);
        }
        typeMapper.deleteById(id);
        log.info("通知类型已删除: id={}, code={}", id, exist.getCode());
    }

    private MsgNotificationTypeVO toVO(MsgNotificationType t) {
        return new MsgNotificationTypeVO(
                t.getCode(), t.getName(), t.getDescription(),
                t.getEnabled(), t.getSortOrder());
    }
}
