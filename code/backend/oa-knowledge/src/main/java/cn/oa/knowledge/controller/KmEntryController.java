package cn.oa.knowledge.controller;

import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.WebUtil;
import cn.oa.knowledge.dto.KmEntryCreateDTO;
import cn.oa.knowledge.dto.KmEntryQueryDTO;
import cn.oa.knowledge.dto.KmEntryUpdateDTO;
import cn.oa.knowledge.dto.KmVersionCreateDTO;
import cn.oa.knowledge.entity.KmEntry;
import cn.oa.knowledge.entity.KmVersion;
import cn.oa.knowledge.service.KmEntryService;
import cn.oa.knowledge.service.KmRelationService;
import cn.oa.knowledge.service.KmTagService;
import cn.oa.knowledge.vo.KmEntryVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * 知识条目 Controller
 */
@Slf4j
@RestController
@RequestMapping("/api/knowledge/entries")
@Tag(name = "知识条目管理")
@RequiredArgsConstructor
public class KmEntryController {

    private final KmEntryService kmEntryService;
    private final KmTagService kmTagService;
    private final KmRelationService kmRelationService;

    @GetMapping("/page")
    @Operation(summary = "分页查询知识条目")
    public R<PageResult<KmEntryVO>> page(KmEntryQueryDTO queryDTO) {
        IPage<KmEntryVO> page = kmEntryService.pageQuery(queryDTO);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @GetMapping("/{id}")
    @Operation(summary = "获取知识条目详情")
    public R<KmEntryVO> detail(@PathVariable Long id) {
        return R.ok(kmEntryService.getDetail(id));
    }

    @PostMapping
    @Operation(summary = "创建知识条目")
    public R<Long> create(@RequestBody @Valid KmEntryCreateDTO createDTO, HttpServletRequest request) {
        Long createById = WebUtil.getEmpId(request);
        KmEntry entry = kmEntryService.create(createDTO, createById);
        log.info("知识条目创建成功: id={}, title={}", entry.getId(), entry.getTitle());
        return R.ok(entry.getId());
    }

    @PutMapping
    @Operation(summary = "更新知识条目")
    public R<Void> update(@RequestBody @Valid KmEntryUpdateDTO updateDTO) {
        kmEntryService.update(updateDTO);
        log.info("知识条目更新成功: id={}", updateDTO.getId());
        return R.ok();
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "删除知识条目")
    public R<Void> delete(@PathVariable Long id) {
        kmEntryService.removeById(id);
        log.info("知识条目删除成功: id={}", id);
        return R.ok();
    }

    @PostMapping("/{id}/publish")
    @Operation(summary = "发布知识条目")
    public R<Void> publish(@PathVariable Long id) {
        kmEntryService.publish(id);
        log.info("知识条目发布成功: id={}", id);
        return R.ok();
    }

    @PostMapping("/{id}/archive")
    @Operation(summary = "归档知识条目")
    public R<Void> archive(@PathVariable Long id) {
        kmEntryService.archive(id);
        log.info("知识条目归档成功: id={}", id);
        return R.ok();
    }

    @PostMapping("/{id}/view")
    @Operation(summary = "增加浏览次数")
    public R<Void> incrementView(@PathVariable Long id) {
        kmEntryService.incrementViewCount(id);
        return R.ok();
    }

    @PostMapping("/{id}/versions")
    @Operation(summary = "上传新版本")
    public R<KmVersion> uploadVersion(
            @PathVariable Long id,
            @RequestParam(required = false) String comment,
            @RequestParam(required = false) MultipartFile file,
            HttpServletRequest request) {
        Long uploaderId = WebUtil.getEmpId(request);
        KmVersionCreateDTO createDTO = new KmVersionCreateDTO();
        createDTO.setEntryId(id);
        createDTO.setComment(comment);
        KmVersion version = kmEntryService.uploadVersion(createDTO, file, uploaderId);
        return R.ok(version);
    }

    @GetMapping("/{id}/versions")
    @Operation(summary = "获取版本历史")
    public R<List<KmVersion>> versions(@PathVariable Long id) {
        return R.ok(kmEntryService.getVersions(id));
    }

    @GetMapping("/{id}/tags")
    @Operation(summary = "获取条目标签")
    public R<List<String>> tags(@PathVariable Long id) {
        List<String> tagNames = kmTagService.getTagsByEntryId(id).stream()
                .map(tag -> tag.getTagName())
                .toList();
        return R.ok(tagNames);
    }

    @GetMapping("/{id}/relations")
    @Operation(summary = "获取条目关联")
    public R<?> relations(@PathVariable Long id) {
        return R.ok(kmRelationService.getRelations(id));
    }

    @GetMapping("/{id}/recommended")
    @Operation(summary = "获取推荐关联条目")
    public R<List<Long>> recommended(
            @PathVariable Long id,
            @RequestParam(defaultValue = "10") int limit) {
        return R.ok(kmRelationService.getRecommendedEntryIds(id, limit));
    }
}
