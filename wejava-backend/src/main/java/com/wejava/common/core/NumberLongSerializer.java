package com.wejava.common.core;

import tools.jackson.core.JacksonException;
import tools.jackson.core.JsonGenerator;
import tools.jackson.databind.SerializationContext;
import tools.jackson.databind.ValueSerializer;

/**
 * 把 long 序列化成 JSON 数字而不是字符串。
 * <p>
 * 全局配置把 Long 转成了字符串（避免雪花 ID 精度丢失），但分页的 total 是数量，
 * 前端组件要求它是数字（Element Plus 用 typeof total === 'number' 校验），
 * 所以这个字段单独声明用本序列化器输出数字。
 */
public class NumberLongSerializer extends ValueSerializer<Long> {

    @Override
    public void serialize(Long value, JsonGenerator gen, SerializationContext context) throws JacksonException {
        if (value == null) {
            gen.writeNull();
        } else {
            gen.writeNumber(value);
        }
    }
}
