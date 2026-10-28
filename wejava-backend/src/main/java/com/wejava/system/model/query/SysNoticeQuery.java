package com.wejava.system.model.query;

import com.wejava.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class SysNoticeQuery extends PageQuery {

    private String noticeTitle;

    private Integer noticeType;

    private Integer status;
}
