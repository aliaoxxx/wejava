package com.wejava.biz.model.query;

import com.wejava.common.core.PageQuery;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;
import org.springframework.format.annotation.DateTimeFormat;

/** 合同管理查询条件 */
@Data
@EqualsAndHashCode(callSuper = true)
public class BizContractQuery extends PageQuery {

    /** 合同编号 */
    private String contractNo;

    /** 合同名称 */
    private String contractName;

    /** 合同类型 */
    private String contractType;

    /** 签订日期起 */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate beginSignDate;

    /** 签订日期止 */
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate endSignDate;

    /** 到期时间起 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime beginExpireTime;

    /** 到期时间止 */
    @DateTimeFormat(pattern = "yyyy-MM-dd HH:mm:ss")
    private LocalDateTime endExpireTime;

    /** 状态 */
    private Integer status;
}
