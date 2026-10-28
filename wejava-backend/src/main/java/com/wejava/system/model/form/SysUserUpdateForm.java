package com.wejava.system.model.form;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/**
 * 修改用户。密码不在此处修改，走独立的重置密码接口。
 */
@Data
public class SysUserUpdateForm {

    @NotNull(message = "用户ID不能为空")
    private Long userId;

    @NotNull(message = "部门不能为空")
    private Long deptId;

    @NotBlank(message = "昵称不能为空")
    @Size(max = 30, message = "昵称最长 30")
    private String nickname;

    @Email(message = "邮箱格式不正确")
    private String email;

    @Pattern(regexp = "^$|^1[3-9]\\d{9}$", message = "手机号格式不正确")
    private String phone;

    private String sex;

    private Integer status;

    private List<Long> roleIds;

    @Size(max = 500, message = "备注最长 500")
    private String remark;
}
