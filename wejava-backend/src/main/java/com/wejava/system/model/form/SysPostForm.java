package com.wejava.system.model.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SysPostForm {

    private Long postId;

    @NotBlank(message = "岗位编码不能为空")
    @Size(max = 64, message = "岗位编码最长 64")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_]*$", message = "岗位编码只能是字母、数字、下划线")
    private String postCode;

    @NotBlank(message = "岗位名称不能为空")
    @Size(max = 50, message = "岗位名称最长 50")
    private String postName;

    private Integer postSort;

    private Integer status;

    @Size(max = 500, message = "备注最长 500")
    private String remark;
}
