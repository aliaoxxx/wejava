package com.wejava.system.model.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SysDictDataForm {

    private Long dictCode;

    private Integer dictSort;

    @NotBlank(message = "字典标签不能为空")
    @Size(max = 100, message = "字典标签最长 100")
    private String dictLabel;

    @NotBlank(message = "字典值不能为空")
    @Size(max = 100, message = "字典值最长 100")
    private String dictValue;

    @NotBlank(message = "字典类型不能为空")
    private String dictType;

    private String cssClass;

    private String listClass;

    private Integer isDefault;

    private Integer status;

    @Size(max = 500, message = "备注最长 500")
    private String remark;
}
