package cn.oa.controller;

import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.common.utils.PageParamUtil;
import cn.oa.common.utils.WebUtil;
import cn.oa.entity.OaTodo;
import cn.oa.service.TodoService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

@Slf4j
@RestController
@RequestMapping("/api/todo")
@Tag(name = "待办中心")
public class TodoController {

    @Autowired
    private TodoService todoService;

    @GetMapping("/page")
    @Operation(summary = "我的待办列表")
    public R<PageResult<OaTodo>> page(@RequestParam int pageNum,
                                       @RequestParam int pageSize,
                                       @RequestParam(required = false) Integer status,
                                       HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        IPage<OaTodo> page = todoService.myTodos(empId, status, PageParamUtil.pageNum(pageNum), PageParamUtil.pageSize(pageSize));
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @GetMapping("/count")
    @Operation(summary = "待办数量")
    public R<Long> count(HttpServletRequest request) {
        Long empId = WebUtil.getEmpId(request);
        return R.ok(todoService.countPending(empId));
    }

    @PostMapping("/done/{id}")
    @Operation(summary = "标记待办完成")
    public R<Void> done(@PathVariable Long id, HttpServletRequest request) {
        Long currentEmpId = WebUtil.getEmpId(request);
        OaTodo todo = todoService.getById(id);
        if (todo == null) {
            return R.fail("待办不存在");
        }
        if (!todo.getEmpId().equals(currentEmpId)) {
            return R.fail("无权操作此待办");
        }
        todoService.doneTodo(id);
        log.info("Todo marked done: id={}", id);
        return R.ok();
    }

    @PostMapping("/ignore/{id}")
    @Operation(summary = "忽略待办")
    public R<Void> ignore(@PathVariable Long id, HttpServletRequest request) {
        Long currentEmpId = WebUtil.getEmpId(request);
        OaTodo todo = todoService.getById(id);
        if (todo == null) {
            return R.fail("待办不存在");
        }
        if (!todo.getEmpId().equals(currentEmpId)) {
            return R.fail("无权操作此待办");
        }
        todoService.ignoreTodo(id);
        log.info("Todo ignored: id={}", id);
        return R.ok();
    }
}

