package com.wejava.system.model.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SysConfigForm {

    private Long configId;

    @NotBlank(message = "参数名称不能为空")
    @Size(max = 100, message = "参数名称最长 100")
    private String configName;

    @NotBlank(message = "参数键名不能为空")
    @Size(max = 100, message = "参数键名最长 100")
    private String configKey;

    @NotBlank(message = "参数键值不能为空")
    @Size(max = 500, message = "参数键值最长 500")
    private String configValue;

    private Integer configType;

    @Size(max = 500, message = "备注最长 500")
    private String remark;
}
