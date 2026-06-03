package cn.oa.document.service;

import cn.oa.document.dto.DocDispatchCreateDTO;
import cn.oa.document.dto.DocDispatchQueryDTO;
import cn.oa.document.dto.DocDispatchUpdateDTO;
import cn.oa.document.entity.DocDispatch;
import cn.oa.document.vo.DocDispatchVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 发文管理服务接口
 *
 * @author oa-document
 */
public interface DocDispatchService {

    /**
     * 创建发文
     *
     * @param dto     发文创建DTO
     * @param creator 创建人
     * @return 发文ID
     */
    Long createDispatch(DocDispatchCreateDTO dto, String creator);

    /**
     * 更新发文
     *
     * @param dto 发文更新DTO
     */
    void updateDispatch(DocDispatchUpdateDTO dto);

    /**
     * 删除发文
     *
     * @param id 发文ID
     */
    void deleteDispatch(Long id);

    /**
     * 分页查询发文
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param query    查询条件
     * @return 分页结果
     */
    IPage<DocDispatchVO> pageDispatch(int pageNum, int pageSize, DocDispatchQueryDTO query);

    /**
     * 查询发文详情
     *
     * @param id 发文ID
     * @return 发文详情
     */
    DocDispatchVO getDispatchDetail(Long id);

    /**
     * 提交到工作流
     *
     * @param id 发文ID
     */
    void submitToWorkflow(Long id);

    /**
     * 锁定文号
     *
     * @param orgCode 发文机关代字
     * @param year    年份
     * @param lockBy  锁定人ID
     * @return 文号信息
     */
    String lockSerial(String orgCode, Integer year, Long lockBy);

    /**
     * 释放文号
     *
     * @param id 文号ID
     */
    void releaseSerial(Long id);

    /**
     * 使用文号（确认发文并使用流水号）
     *
     * @param id        文号ID
     * @param dispatchId 发文ID
     */
    void useSerial(Long id, Long dispatchId);
}
