package cn.oa.document.service;

import cn.oa.document.dto.DocReceiveCreateDTO;
import cn.oa.document.dto.DocReceiveProposeDTO;
import cn.oa.document.dto.DocReceiveApproveDTO;
import cn.oa.document.dto.DocReceiveHandleDTO;
import cn.oa.document.vo.DocReceiveVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

/**
 * 收文管理服务接口
 *
 * @author oa-document
 */
public interface DocReceiveService {

    /**
     * 登记收文
     *
     * @param dto     收文登记DTO
     * @param creator 登记人
     * @return 收文ID
     */
    Long register(DocReceiveCreateDTO dto, String creator);

    /**
     * 拟办
     *
     * @param dto 拟办DTO
     */
    void propose(DocReceiveProposeDTO dto);

    /**
     * 批办
     *
     * @param dto 批办DTO
     */
    void approve(DocReceiveApproveDTO dto);

    /**
     * 承办
     *
     * @param dto 承办DTO
     */
    void handle(DocReceiveHandleDTO dto);

    /**
     * 分页查询收文
     *
     * @param pageNum  页码
     * @param pageSize 每页大小
     * @param keyword  关键字
     * @param status   状态
     * @return 分页结果
     */
    IPage<DocReceiveVO> pageReceive(int pageNum, int pageSize, String keyword, String status);

    /**
     * 查询收文详情
     *
     * @param id 收文ID
     * @return 收文详情
     */
    DocReceiveVO getReceiveDetail(Long id);

    /**
     * 删除收文
     *
     * @param id 收文ID
     */
    void deleteReceive(Long id);
}
