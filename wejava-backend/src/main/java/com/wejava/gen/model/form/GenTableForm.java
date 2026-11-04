package com.wejava.gen.model.form;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.util.List;
import lombok.Data;

/** 表配置提交 */
@Data
public class GenTableForm {

    @NotBlank(message = "表名不能为空")
    private String tableName;

    @Size(max = 500, message = "表描述最长 500")
    private String tableComment;

    @NotBlank(message = "实体类名不能为空")
    @Size(max = 100, message = "实体类名最长 100")
    private String className;

    @NotBlank(message = "模块名不能为空")
    @Size(max = 50, message = "模块名最长 50")
    private String moduleName;

    @NotBlank(message = "业务名不能为空")
    @Size(max = 50, message = "业务名最长 50")
    private String businessName;

    @NotBlank(message = "功能名不能为空")
    @Size(max = 100, message = "功能名最长 100")
    private String functionName;

    @Size(max = 200, message = "包名最长 200")
    private String packageName;

    private String author;

    private Long parentMenuId;

    @Valid
    private List<GenTableColumnForm> columns;
}
