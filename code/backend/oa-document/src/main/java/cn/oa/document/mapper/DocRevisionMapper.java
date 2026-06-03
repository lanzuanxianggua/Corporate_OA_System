package cn.oa.document.mapper;

import cn.oa.document.entity.DocRevision;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;

/**
 * 公文修订版本Mapper
 *
 * @author oa-document
 */
@Mapper
public interface DocRevisionMapper extends BaseMapper<DocRevision> {
}
