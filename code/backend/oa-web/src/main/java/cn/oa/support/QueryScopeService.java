package cn.oa.support;

import cn.oa.common.exception.BusinessException;
import cn.oa.common.resolver.RoleResolver;
import cn.oa.common.utils.WebUtil;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class QueryScopeService {

    @Autowired(required = false)
    private RoleResolver roleResolver;

    public Long resolveEmpId(HttpServletRequest request, Long requestedEmpId) {
        Long currentEmpId = WebUtil.getEmpId(request);
        if (currentEmpId == null) {
            throw new BusinessException("???");
        }
        if (requestedEmpId == null || requestedEmpId.equals(currentEmpId)) {
            return currentEmpId;
        }
        if (isAdmin(currentEmpId)) {
            return requestedEmpId;
        }
        throw new BusinessException("????????");
    }

    private boolean isAdmin(Long empId) {
        if (roleResolver == null) {
            return false;
        }
        List<String> roles = roleResolver.resolveRoles(empId);
        return roles != null && roles.stream().anyMatch(role -> "ADMIN".equalsIgnoreCase(role));
    }
}
