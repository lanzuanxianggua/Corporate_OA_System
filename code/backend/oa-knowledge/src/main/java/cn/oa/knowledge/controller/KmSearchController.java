package cn.oa.knowledge.controller;

import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.knowledge.dto.KmEntryQueryDTO;
import cn.oa.knowledge.service.KmEntryService;
import cn.oa.knowledge.vo.KmEntryVO;
import cn.oa.knowledge.vo.KmSearchResultVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

/**
 * 知识搜索 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge/search")
@Tag(name = "知识搜索")
@RequiredArgsConstructor
public class KmSearchController {

    private final KmEntryService kmEntryService;

    @GetMapping
    @Operation(summary = "搜索知识条目（仅PUBLISHED状态）")
    public R<PageResult<KmEntryVO>> search(
            @RequestParam String keyword,
            @RequestParam(defaultValue = "1") int pageNum,
            @RequestParam(defaultValue = "10") int pageSize) {
        KmEntryQueryDTO queryDTO = new KmEntryQueryDTO();
        queryDTO.setKeyword(keyword);
        queryDTO.setStatus("PUBLISHED");
        queryDTO.setPageNum(pageNum);
        queryDTO.setPageSize(pageSize);
        IPage<KmEntryVO> page = kmEntryService.pageQuery(queryDTO);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }
}
