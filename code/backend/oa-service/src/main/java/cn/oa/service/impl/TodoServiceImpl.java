package cn.oa.service.impl;

import cn.oa.common.exception.BusinessException;
import cn.oa.entity.OaTodo;
import cn.oa.mapper.OaTodoMapper;
import cn.oa.service.TodoService;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
public class TodoServiceImpl extends ServiceImpl<OaTodoMapper, OaTodo> implements TodoService {

    @Override
    @Transactional
    public void addTodo(Long empId, String title, String todoType, Long businessId, String businessType) {
        OaTodo todo = new OaTodo();
        todo.setEmpId(empId);
        todo.setTitle(title);
        todo.setTodoType(todoType);
        todo.setBusinessId(businessId);
        todo.setBusinessType(businessType);
        todo.setStatus("0");
        todo.setCreateTime(LocalDateTime.now());
        this.save(todo);
    }

    @Override
    @Transactional
    public void doneTodo(Long todoId) {
        OaTodo todo = this.getById(todoId);
        if (todo == null) {
            throw new BusinessException("待办不存在");
        }
        todo.setStatus("1");
        todo.setDoneTime(LocalDateTime.now());
        this.updateById(todo);
    }

    @Override
    @Transactional
    public void ignoreTodo(Long todoId) {
        OaTodo todo = this.getById(todoId);
        if (todo == null) {
            throw new BusinessException("待办不存在");
        }
        todo.setStatus("2");
        todo.setDoneTime(LocalDateTime.now());
        this.updateById(todo);
    }

    @Override
    public IPage<OaTodo> myTodos(Long empId, Integer status, int pageNum, int pageSize) {
        Page<OaTodo> page = new Page<>(pageNum, pageSize);
        LambdaQueryWrapper<OaTodo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaTodo::getEmpId, empId);
        if (status != null) {
            wrapper.eq(OaTodo::getStatus, String.valueOf(status));
        }
        wrapper.orderByDesc(OaTodo::getCreateTime);
        return this.page(page, wrapper);
    }

    @Override
    public Long countPending(Long empId) {
        LambdaQueryWrapper<OaTodo> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(OaTodo::getEmpId, empId)
                .eq(OaTodo::getStatus, "0");
        return this.count(wrapper);
    }
}
