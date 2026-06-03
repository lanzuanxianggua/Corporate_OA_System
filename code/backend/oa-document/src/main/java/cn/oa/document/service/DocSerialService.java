package cn.oa.document.service;

/**
 * 文号管理服务接口
 *
 * @author oa-document
 */
public interface DocSerialService {

    /**
     * 锁定文号
     * 流水号+1并标记LOCKED
     *
     * @param orgCode 发文机关代字
     * @param year    年份
     * @param lockBy  锁定人ID
     * @return 完整文号
     */
    String lockSerial(String orgCode, Integer year, Long lockBy);

    /**
     * 释放文号
     * 取消LOCKED状态
     *
     * @param id 文号ID
     */
    void releaseSerial(Long id);

    /**
     * 使用文号（确认使用并持久化）
     *
     * @param id        文号ID
     * @param dispatchId 发文ID
     */
    void useSerial(Long id, Long dispatchId);

    /**
     * 生成完整文号
     *
     * @param orgCode 发文机关代字
     * @param year    年份
     * @param serialNo 流水号
     * @return 完整文号
     */
    String formatSerialNo(String orgCode, Integer year, Integer serialNo);
}
