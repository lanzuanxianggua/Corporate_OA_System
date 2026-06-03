package cn.oa.document.service;

import cn.oa.document.dto.DocRevisionCreateDTO;
import cn.oa.document.vo.DocRevisionVO;

import java.util.List;

/**
 * 公文修订版本服务接口
 *
 * @author oa-document
 */
public interface DocRevisionService {

    /**
     * 新建版本
     *
     * @param dto      版本创建DTO
     * @param editorId 编辑人ID
     * @return 修订版本ID
     */
    Long addRevision(DocRevisionCreateDTO dto, Long editorId);

    /**
     * 获取发文的历史版本列表
     *
     * @param dispatchId 发文ID
     * @return 版本列表
     */
    List<DocRevisionVO> getHistory(Long dispatchId);

    /**
     * 清稿（将当前版本标记为清稿版）
     *
     * @param id 修订版本ID
     */
    void clean(Long id);
}
