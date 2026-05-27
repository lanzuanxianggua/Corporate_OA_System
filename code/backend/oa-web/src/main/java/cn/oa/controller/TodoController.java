package cn.oa.controller;

import cn.oa.common.result.PageResult;
import cn.oa.common.result.R;
import cn.oa.entity.OaTodo;
import cn.oa.service.TodoService;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import jakarta.servlet.http.HttpServletRequest;

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
        Long empId = (Long) request.getAttribute("empId");
        IPage<OaTodo> page = todoService.myTodos(empId, status, pageNum, pageSize);
        return R.ok(PageResult.of(page.getTotal(), page.getRecords()));
    }

    @GetMapping("/count")
    @Operation(summary = "待办数量")
    public R<Long> count(HttpServletRequest request) {
        Long empId = (Long) request.getAttribute("empId");
        return R.ok(todoService.countPending(empId));
    }

    @PostMapping("/done/{id}")
    @Operation(summary = "标记待办完成")
    public R<Void> done(@PathVariable Long id) {
        todoService.doneTodo(id);
        return R.ok();
    }

    @PostMapping("/ignore/{id}")
    @Operation(summary = "忽略待办")
    public R<Void> ignore(@PathVariable Long id) {
        todoService.ignoreTodo(id);
        return R.ok();
    }
}
