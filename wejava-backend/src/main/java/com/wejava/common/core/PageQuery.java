package com.wejava.common.core;

import com.baomidou.mybatisplus.core.metadata.OrderItem;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import java.io.Serializable;
import java.util.regex.Pattern;
import lombok.Data;
import org.springframework.util.StringUtils;

/**
 * 分页查询基类。所有列表接口的 Query 对象都继承它。
 * <p>
 * 两个保护：
 * 1. pageSize 上限 100，防止 pageSize=999999 拖垮数据库
 * 2. orderBy 只允许字母开头、字母数字下划线组成的列名，且**必须**在 Service 里做字段白名单映射
 */
@Data
public class PageQuery implements Serializable {

    /** 单页最大条数 */
    public static final long MAX_PAGE_SIZE = 100;

    private static final Pattern SAFE_COLUMN = Pattern.compile("^[a-zA-Z][a-zA-Z0-9_]{0,63}$");

    private long pageNum = 1;
    private long pageSize = 10;

    /** 排序字段，必须是数据库列名；前端传的驼峰字段需在 Service 里转换 */
    private String orderBy;

    /** asc / desc */
    private String orderDirection = "desc";

    public <T> Page<T> toPage() {
        long current = Math.max(pageNum, 1);
        long size = Math.min(Math.max(pageSize, 1), MAX_PAGE_SIZE);
        Page<T> page = new Page<>(current, size);
        if (StringUtils.hasText(orderBy) && SAFE_COLUMN.matcher(orderBy).matches()) {
            page.addOrder("asc".equalsIgnoreCase(orderDirection) ? OrderItem.asc(orderBy) : OrderItem.desc(orderBy));
        }
        return page;
    }
}
