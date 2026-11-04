package com.wejava.biz.model.vo;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 合同管理 */
@Data
public class BizContractVO {

    /** 合同ID */
    private Long contractId;

    /** 合同编号 */
    private String contractNo;

    /** 合同名称 */
    private String contractName;

    /** 合同类型 */
    private String contractType;

    /** 合同金额 */
    private BigDecimal amount;

    /** 签订日期 */
    private LocalDate signDate;

    /** 到期时间 */
    private LocalDateTime expireTime;

    /** 状态 */
    private Integer status;

    /** 备注 */
    private String remark;

    /** Create By */
    private String createBy;

    /** Create Time */
    private LocalDateTime createTime;

    /** Update By */
    private String updateBy;

    /** Update Time */
    private LocalDateTime updateTime;
}
