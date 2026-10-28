package com.wejava.security.permission;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

/**
 * 权限匹配规则的单元测试。纯逻辑，不启动 Spring 上下文。
 */
class PermissionMatcherTest {

    @Test
    void allPermissionMatchesAnything() {
        assertThat(PermissionMatcher.matches(PermissionMatcher.ALL, "system:user:add"))
                .isTrue();
        assertThat(PermissionMatcher.matches(PermissionMatcher.ALL, "monitor:online:list"))
                .isTrue();
    }

    @Test
    void exactPermissionMatches() {
        assertThat(PermissionMatcher.matches("system:user:add", "system:user:add"))
                .isTrue();
    }

    @Test
    void differentPermissionDoesNotMatch() {
        assertThat(PermissionMatcher.matches("system:user:add", "system:role:add"))
                .isFalse();
        assertThat(PermissionMatcher.matches("system:user:add", "system:user:edit"))
                .isFalse();
    }

    @Test
    void singleSegmentWildcardMatches() {
        assertThat(PermissionMatcher.matches("system:user:*", "system:user:add"))
                .isTrue();
        assertThat(PermissionMatcher.matches("system:*:add", "system:user:add")).isTrue();
        assertThat(PermissionMatcher.matches("*:*:*", "system:user:add")).isTrue();
    }

    @Test
    void wildcardDoesNotCrossSegmentCount() {
        assertThat(PermissionMatcher.matches("system:*", "system:user:add")).isFalse();
        assertThat(PermissionMatcher.matches("system:user:*", "system:user")).isFalse();
    }

    @Test
    void nullAndBlankAreSafe() {
        assertThat(PermissionMatcher.matches(null, "system:user:add")).isFalse();
        assertThat(PermissionMatcher.matches("system:user:add", null)).isFalse();
        assertThat(PermissionMatcher.matches("system:user:add", "")).isFalse();
    }
}
