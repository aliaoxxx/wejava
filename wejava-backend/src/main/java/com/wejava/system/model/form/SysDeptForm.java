package com.wejava.system.model.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class SysDeptForm {

    private Long deptId;

    @NotNull(message = "上级部门不能为空")
    private Long parentId;

    @NotBlank(message = "部门名称不能为空")
    @Size(max = 30, message = "部门名称最长 30")
    private String deptName;

    private Integer orderNum;

    @Size(max = 20, message = "负责人最长 20")
    private String leader;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    @Email(message = "邮箱格式不正确")
    private String email;

    private Integer status;
}
