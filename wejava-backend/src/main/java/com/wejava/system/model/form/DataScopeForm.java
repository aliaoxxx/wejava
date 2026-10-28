package com.wejava.system.model.form;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import lombok.Data;

@Data
public class DataScopeForm {

    @NotNull(message = "角色ID不能为空")
    private Long roleId;

    /** 1全部 2自定义 3本部门 4本部门及以下 5仅本人 */
    @NotBlank(message = "数据范围不能为空")
    @Pattern(regexp = "^[1-5]$", message = "数据范围只能是 1~5")
    private String dataScope;

    /** dataScope = 2 时必填 */
    private List<Long> deptIds;
}
