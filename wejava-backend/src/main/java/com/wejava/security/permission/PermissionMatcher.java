package com.wejava.security.permission;

/**
 * 权限语义的唯一实现处。
 * <p>
 * 只做一件事：判断"持有的权限"是否命中"所需权限"。
 * <p>
 * 从会话推导权限集合的职责在 {@link AuthorityResolver}，两者分开，
 * 避免同一个类既管"有哪些"又管"够不够"。
 */
public final class PermissionMatcher {

    /** 超级管理员的通配权限 */
    public static final String ALL = "*:*:*";

    private static final String SEPARATOR = ":";
    private static final String WILDCARD = "*";

    private PermissionMatcher() {}

    /** 持有的权限 owned 是否满足所需的 required */
    public static boolean matches(String owned, String required) {
        if (owned == null || required == null || required.isEmpty()) {
            return false;
        }
        if (ALL.equals(owned) || owned.equals(required)) {
            return true;
        }
        return wildcardMatch(owned, required);
    }

    /** 支持 system:user:* 这样的单段通配 */
    private static boolean wildcardMatch(String pattern, String target) {
        if (!pattern.contains(WILDCARD)) {
            return false;
        }
        String[] patternParts = pattern.split(SEPARATOR);
        String[] targetParts = target.split(SEPARATOR);
        if (patternParts.length != targetParts.length) {
            return false;
        }
        for (int i = 0; i < patternParts.length; i++) {
            if (!WILDCARD.equals(patternParts[i]) && !patternParts[i].equals(targetParts[i])) {
                return false;
            }
        }
        return true;
    }
}
