package cn.oa.platform.common.context;

import java.util.List;

/**
 * 当前用户 ThreadLocal 上下文.
 */
public class UserContext {

    private static final ThreadLocal<UserInfo> THREAD_LOCAL = new ThreadLocal<>();

    public static void set(UserInfo userInfo) {
        THREAD_LOCAL.set(userInfo);
    }

    public static UserInfo get() {
        return THREAD_LOCAL.get();
    }

    public static Long getCurrentEmpId() {
        UserInfo info = get();
        return info == null ? null : info.getEmpId();
    }

    public static String getCurrentUsername() {
        UserInfo info = get();
        return info == null ? null : info.getUsername();
    }

    public static void clear() {
        THREAD_LOCAL.remove();
    }

    public static class UserInfo {
        private Long empId;
        private String username;
        private String realName;
        private Long deptId;
        private String deptName;
        private String dataScope;
        private List<String> roles;
        private List<String> permissions;

        public UserInfo() {}

        public UserInfo(Long empId, String username, String realName, Long deptId,
                        String deptName, String dataScope, List<String> roles, List<String> permissions) {
            this.empId = empId;
            this.username = username;
            this.realName = realName;
            this.deptId = deptId;
            this.deptName = deptName;
            this.dataScope = dataScope;
            this.roles = roles;
            this.permissions = permissions;
        }

        public Long getEmpId() { return empId; }
        public void setEmpId(Long empId) { this.empId = empId; }
        public String getUsername() { return username; }
        public void setUsername(String username) { this.username = username; }
        public String getRealName() { return realName; }
        public void setRealName(String realName) { this.realName = realName; }
        public Long getDeptId() { return deptId; }
        public void setDeptId(Long deptId) { this.deptId = deptId; }
        public String getDeptName() { return deptName; }
        public void setDeptName(String deptName) { this.deptName = deptName; }
        public String getDataScope() { return dataScope; }
        public void setDataScope(String dataScope) { this.dataScope = dataScope; }
        public List<String> getRoles() { return roles; }
        public void setRoles(List<String> roles) { this.roles = roles; }
        public List<String> getPermissions() { return permissions; }
        public void setPermissions(List<String> permissions) { this.permissions = permissions; }
    }
}
