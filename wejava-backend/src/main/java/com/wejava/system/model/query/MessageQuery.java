package com.wejava.system.model.query;

import com.wejava.common.core.PageQuery;
import lombok.Data;
import lombok.EqualsAndHashCode;

@Data
@EqualsAndHashCode(callSuper = true)
public class MessageQuery extends PageQuery {

    private String keyword;

    /** 1 公告 2 系统 3 个人，空表示全部 */
    private Integer msgType;

    /** 0 未读 1 已读，空表示全部 */
    private Integer readFlag;
}
