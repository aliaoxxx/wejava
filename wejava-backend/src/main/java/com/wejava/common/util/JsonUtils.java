package com.wejava.common.util;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import tools.jackson.databind.json.JsonMapper;

/**
 * JSON 序列化工具。SSE 广播、日志等场景需要把对象转成字符串。
 */
@Component
@RequiredArgsConstructor
public class JsonUtils {

    private final JsonMapper jsonMapper;

    public String toJson(Object value) {
        if (value == null) {
            return "null";
        }
        return jsonMapper.writeValueAsString(value);
    }
}
