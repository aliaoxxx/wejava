package com.wejava.biz.model.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.wejava.common.core.BaseEntity;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;
import lombok.EqualsAndHashCode;

/** 合同管理 */
@Data
@EqualsAndHashCode(callSuper = true)
@TableName("biz_contract")
public class BizContract extends BaseEntity {

    @TableId(value = "contract_id", type = IdType.AUTO)
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
}
