package com.wejava.biz.model.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import lombok.Data;

/** 合同管理 */
@Data
public class BizContractForm {

    private Long contractId;

    /** 合同编号 */
    @NotBlank(message = "合同编号不能为空")
    @Size(max = 64, message = "合同编号最长 64")
    private String contractNo;

    /** 合同名称 */
    @NotBlank(message = "合同名称不能为空")
    @Size(max = 200, message = "合同名称最长 200")
    private String contractName;

    /** 合同类型 */
    @NotBlank(message = "合同类型不能为空")
    @Size(max = 32, message = "合同类型最长 32")
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
    @Size(max = 500, message = "备注最长 500")
    private String remark;
}
