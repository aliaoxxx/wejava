package com.wejava.system.service;

import com.wejava.common.core.PageResult;
import com.wejava.system.model.form.SysPostForm;
import com.wejava.system.model.query.SysPostQuery;
import com.wejava.system.model.vo.PostOptionVO;
import com.wejava.system.model.vo.SysPostVO;
import java.util.List;

public interface SysPostService {

    PageResult<SysPostVO> page(SysPostQuery query);

    SysPostVO getById(Long postId);

    Long create(SysPostForm form);

    void update(SysPostForm form);

    void remove(List<Long> postIds);

    List<PostOptionVO> optionSelect();
}
