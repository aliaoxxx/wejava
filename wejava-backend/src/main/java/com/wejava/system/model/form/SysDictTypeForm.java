package com.wejava.system.model.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SysDictTypeForm {

    private Long dictId;

    @NotBlank(message = "字典名称不能为空")
    @Size(max = 100, message = "字典名称最长 100")
    private String dictName;

    @NotBlank(message = "字典类型不能为空")
    @Size(max = 100, message = "字典类型最长 100")
    @Pattern(regexp = "^[a-z][a-z0-9_]*$", message = "字典类型只能是小写字母、数字、下划线")
    private String dictType;

    private Integer status;

    @Size(max = 500, message = "备注最长 500")
    private String remark;
}
