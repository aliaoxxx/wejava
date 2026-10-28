package com.wejava.system.controller;

import com.wejava.common.core.R;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.SQLException;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import lombok.RequiredArgsConstructor;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 冒烟接口：验证应用与数据库连通性。M1 完成后可保留，用于健康检查。
 */
@RestController
@RequestMapping("/ping")
@RequiredArgsConstructor
public class PingController {

    private final DataSource dataSource;

    @GetMapping
    public R<String> ping() {
        return R.ok("pong");
    }

    /** 权限校验冒烟：需要 system:user:list 权限 */
    @PreAuthorize("@ps.has('system:user:list')")
    @GetMapping("/secure")
    public R<String> secure() {
        return R.ok("you have system:user:list");
    }

    @GetMapping("/db")
    public R<Map<String, Object>> db() throws SQLException {
        try (Connection connection = dataSource.getConnection()) {
            DatabaseMetaData meta = connection.getMetaData();
            Map<String, Object> info = new LinkedHashMap<>();
            info.put("valid", connection.isValid(2));
            info.put("product", meta.getDatabaseProductName());
            info.put("version", meta.getDatabaseProductVersion());
            info.put("catalog", connection.getCatalog());
            info.put("url", meta.getURL());
            return R.ok(info);
        }
    }
}
