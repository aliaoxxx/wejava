package com.wejava.common.core;

import com.baomidou.mybatisplus.core.metadata.IPage;
import java.io.Serializable;
import java.util.Collections;
import java.util.List;
import lombok.Data;

/**
 * 分页结果。前端约定：{ total, list }
 */
@Data
public class PageResult<T> implements Serializable {

    private long total;
    private List<T> list;

    public static <T> PageResult<T> of(IPage<T> page) {
        PageResult<T> result = new PageResult<>();
        result.setTotal(page.getTotal());
        result.setList(page.getRecords());
        return result;
    }

    public static <T> PageResult<T> empty() {
        PageResult<T> result = new PageResult<>();
        result.setTotal(0);
        result.setList(Collections.emptyList());
        return result;
    }
}
