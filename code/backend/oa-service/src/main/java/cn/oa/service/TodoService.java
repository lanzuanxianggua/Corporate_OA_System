package cn.oa.service;

import cn.oa.entity.OaTodo;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.service.IService;

public interface TodoService extends IService<OaTodo> {

    void addTodo(Long empId, String title, String todoType, Long businessId, String businessType);

    void doneTodo(Long todoId);

    void ignoreTodo(Long todoId);

    IPage<OaTodo> myTodos(Long empId, Integer status, int pageNum, int pageSize);

    Long countPending(Long empId);
}
