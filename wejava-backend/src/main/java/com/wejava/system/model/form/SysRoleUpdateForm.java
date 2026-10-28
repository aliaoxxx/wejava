package com.wejava.system.model.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

@Data
public class SysRoleUpdateForm {

    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    @NotBlank(message = "角色名称不能为空")
    @Size(max = 30, message = "角色名称最长 30")
    private String roleName;

    @NotBlank(message = "角色标识不能为空")
    @Size(max = 100, message = "角色标识最长 100")
    @Pattern(regexp = "^[a-zA-Z][a-zA-Z0-9_:]*$", message = "角色标识只能包含字母、数字、下划线和冒号")
    private String roleKey;

    private Integer roleSort;

    private Integer status;

    @Size(max = 500, message = "备注最长 500")
    private String remark;

    private List<Long> menuIds;
}
