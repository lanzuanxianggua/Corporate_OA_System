package cn.oa.platform.common.api;

import io.swagger.v3.oas.annotations.media.Schema;

import java.util.List;
import java.util.Objects;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * 分页响应.
 */
@Schema(description = "分页响应")
public class PageResult<T> {

    @Schema(description = "数据列表")
    private List<T> list;

    @Schema(description = "总记录数")
    private Long total;

    @Schema(description = "页码(从 1 开始)")
    private Integer pageNum;

    @Schema(description = "每页大小")
    private Integer pageSize;

    @Schema(description = "总页数")
    private Integer pages;

    public PageResult() {
    }

    public PageResult(List<T> list, Long total, Integer pageNum, Integer pageSize) {
        this.list = list;
        this.total = total;
        this.pageNum = pageNum;
        this.pageSize = pageSize;
        this.pages = pageSize == null || pageSize == 0 ? 0 : (int) Math.ceil((double) total / pageSize);
    }

    public static <T> PageResult<T> empty() {
        return new PageResult<>(List.of(), 0L, 1, 10);
    }

    public static <T> PageResult<T> of(List<T> list, long total, int pageNum, int pageSize) {
        return new PageResult<>(list, total, pageNum, pageSize);
    }

    public <R> PageResult<R> map(Function<T, R> converter) {
        List<R> mappedList = this.list == null ? List.of()
                : this.list.stream().map(converter).collect(Collectors.toList());
        return new PageResult<>(mappedList, this.total, this.pageNum, this.pageSize);
    }

    public List<T> getList() { return list; }
    public void setList(List<T> list) { this.list = list; }
    public Long getTotal() { return total; }
    public void setTotal(Long total) { this.total = total; }
    public Integer getPageNum() { return pageNum; }
    public void setPageNum(Integer pageNum) { this.pageNum = pageNum; }
    public Integer getPageSize() { return pageSize; }
    public void setPageSize(Integer pageSize) { this.pageSize = pageSize; }
    public Integer getPages() { return pages; }
    public void setPages(Integer pages) { this.pages = pages; }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        PageResult<?> that = (PageResult<?>) o;
        return Objects.equals(total, that.total) && Objects.equals(pageNum, that.pageNum) && Objects.equals(pageSize, that.pageSize);
    }

    @Override
    public int hashCode() { return Objects.hash(total, pageNum, pageSize); }
}
