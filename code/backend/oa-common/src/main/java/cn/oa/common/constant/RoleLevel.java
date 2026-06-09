package cn.oa.common.constant;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

/**
 * Numeric hierarchy levels for role keys defined in {@link RoleConstants}.
 * Higher number = higher authority. Used by workflow routing rules
 * (e.g. "skip GM if initiator is ADMIN") and the {@code initiator_level_match}
 * assignee type.
 *
 * <p>Order is intentional: USER(1) &lt; TEAM_LEAD(2) &lt; DEPT_MANAGER(3)
 * &lt; DIRECTOR(4) &lt; GM(5) &lt; ADMIN(6).
 */
public final class RoleLevel {

    public static final int USER = 1;
    public static final int TEAM_LEAD = 2;
    public static final int DEPT_MANAGER = 3;
    public static final int DIRECTOR = 4;
    public static final int GM = 5;
    public static final int ADMIN = 6;

    private static final Map<String, Integer> LEVEL_BY_KEY;

    static {
        Map<String, Integer> m = new HashMap<>();
        m.put(RoleConstants.USER, USER);
        m.put(RoleConstants.TEAM_LEAD, TEAM_LEAD);
        m.put(RoleConstants.DEPT_MANAGER, DEPT_MANAGER);
        m.put(RoleConstants.DIRECTOR, DIRECTOR);
        m.put(RoleConstants.GM, GM);
        m.put(RoleConstants.ADMIN, ADMIN);
        LEVEL_BY_KEY = Collections.unmodifiableMap(m);
    }

    private RoleLevel() {
    }

    /**
     * Map a role key (e.g. {@code "DEPT_MANAGER"}) to its numeric level.
     * Returns {@link #USER} for unknown keys so legacy / custom roles still resolve.
     */
    public static int of(String roleKey) {
        if (roleKey == null) return USER;
        Integer level = LEVEL_BY_KEY.get(roleKey);
        return level == null ? USER : level;
    }

    /**
     * Pick the highest level among a list of role keys. Returns {@link #USER} for empty input.
     */
    public static int maxLevel(java.util.Collection<String> roleKeys) {
        int best = USER;
        for (String key : roleKeys) {
            int lvl = of(key);
            if (lvl > best) best = lvl;
        }
        return best;
    }
}
