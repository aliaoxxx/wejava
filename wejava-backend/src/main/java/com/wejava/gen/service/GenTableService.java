package com.wejava.gen.service;

import com.wejava.common.core.PageResult;
import com.wejava.gen.model.form.GenTableForm;
import com.wejava.gen.model.query.GenTableQuery;
import com.wejava.gen.model.vo.DbTableVO;
import com.wejava.gen.model.vo.GenTableVO;
import java.util.List;
import java.util.Map;

public interface GenTableService {

    PageResult<GenTableVO> page(GenTableQuery query);

    /** 数据库中可导入的表（含已导入标记） */
    List<DbTableVO> listDbTables();

    /** 导入指定表，返回导入数量 */
    int importTables(List<String> tableNames);

    GenTableVO detail(Long tableId);

    void update(Long tableId, GenTableForm form);

    void remove(List<Long> tableIds);

    /** 预览：文件名 → 代码内容 */
    Map<String, String> preview(Long tableId);

    /** 下载 zip */
    byte[] download(Long tableId);
}
