package com.wejava.gen;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import com.wejava.gen.mapper.GenTemplateMapper;
import com.wejava.gen.model.entity.GenTable;
import com.wejava.gen.model.entity.GenTableColumn;
import com.wejava.gen.support.GenModel;
import com.wejava.gen.support.TemplateRenderer;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.Test;

/**
 * 模板渲染冒烟测试。
 *
 * <p>生成的代码能不能编译不在单测范围内（由 CI 的 spotless + 真实生成验证），
 * 这里只保证：12 个模板都能渲染出来，且关键片段符合预期。
 */
class GenTemplateTest {

    private final GenModel genModel = new GenModel();
    /** 没有覆盖模板时走 classpath，mock 返回 null 即代表未覆盖 */
    private final TemplateRenderer renderer = new TemplateRenderer(mock(GenTemplateMapper.class));

    private GenTable table() {
        GenTable table = new GenTable();
        table.setTableId(1L);
        table.setTableName("demo_product");
        table.setTableComment("演示商品");
        table.setClassName("DemoProduct");
        table.setModuleName("demo");
        table.setBusinessName("product");
        table.setFunctionName("演示商品");
        table.setPackageName("com.wejava.demo");
        table.setAuthor("tester");
        return table;
    }

    private GenTableColumn column(
            String name, String type, String javaType, boolean pk, boolean audit, boolean required) {
        GenTableColumn column = new GenTableColumn();
        column.setColumnName(name);
        column.setColumnType(type);
        column.setColumnComment(name);
        column.setJavaType(javaType);
        column.setJavaField(com.wejava.gen.support.GenNaming.toCamel(name));
        column.setIsPk(pk ? 1 : 0);
        column.setIsIncrement(pk ? 1 : 0);
        column.setIsRequired(required ? 1 : 0);
        column.setIsInsert(pk || audit ? 0 : 1);
        column.setIsEdit(pk || audit ? 0 : 1);
        column.setIsList(1);
        column.setIsQuery("product_name".equals(name) || "status".equals(name) ? 1 : 0);
        column.setQueryType("product_name".equals(name) ? "LIKE" : "EQ");
        column.setHtmlType("status".equals(name) ? "switch" : "input");
        column.setDictType("status".equals(name) ? "sys_normal_disable" : null);
        column.setSort(1);
        return column;
    }

    private List<GenTableColumn> columns() {
        return List.of(
                column("product_id", "bigint", "Long", true, false, false),
                column("product_name", "varchar(100)", "String", false, false, true),
                column("price", "decimal(10,2)", "BigDecimal", false, false, false),
                column("status", "tinyint", "Integer", false, false, false),
                column("remark", "varchar(500)", "String", false, false, false),
                column("create_time", "datetime", "LocalDateTime", false, true, false));
    }

    private Map<String, String> render(String template) {
        Map<String, Object> model = genModel.build(table(), columns());
        return Map.of("out", renderer.render(template, model));
    }

    @Test
    void 实体模板_排除审计字段且带主键注解() {
        String code = render("entity.java.ftl").get("out");
        assertThat(code).contains("@TableName(\"demo_product\")");
        assertThat(code).contains("@TableId(value = \"product_id\", type = IdType.AUTO)");
        assertThat(code).contains("private String productName;");
        assertThat(code).contains("import java.math.BigDecimal;");
        // 审计字段来自 BaseEntity，不能重复生成
        assertThat(code).doesNotContain("private LocalDateTime createTime;");
    }

    @Test
    void 表单模板_必填字段带校验注解() {
        String code = render("form.java.ftl").get("out");
        assertThat(code).contains("@NotBlank(message = \"product_name不能为空\")");
        assertThat(code).contains("@Size(max = 100");
        // 主键保留在表单里：更新接口要靠它定位记录
        assertThat(code).contains("private Long productId;");
    }

    @Test
    void 查询模板_字符串用like_数字用eq() {
        String query = render("query.java.ftl").get("out");
        assertThat(query).contains("private String productName;");
        assertThat(query).contains("private Integer status;");

        String impl = render("serviceImpl.java.ftl").get("out");
        // productName 这行按 palantir 的 120 列规则会折行，短字段名则保持单行
        assertThat(impl).contains("StringUtils.hasText(query.getProductName())");
        assertThat(impl).contains(".eq(query.getStatus() != null");
        assertThat(impl).contains("orderByDesc(DemoProduct::getProductId)");
    }

    @Test
    void 控制器模板_权限前缀正确() {
        String code = render("controller.java.ftl").get("out");
        assertThat(code).contains("@RequestMapping(\"/demo/product\")");
        assertThat(code).contains("@ps.has('demo:product:list')");
        assertThat(code).contains("@ps.has('demo:product:remove')");
    }

    @Test
    void 转换器模板_忽略主键() {
        String code = render("converter.java.ftl").get("out");
        assertThat(code).contains("@Mapping(target = \"productId\", ignore = true)");
        assertThat(code).contains("void update(DemoProductForm form, @MappingTarget DemoProduct entity)");
    }

    @Test
    void 前端模板_生成接口与schema() {
        String api = render("api.ts.ftl").get("out");
        assertThat(api).contains("export function listProduct");
        assertThat(api).contains("'/demo/product/list'");
        assertThat(api).contains("`/demo/product/${productId}`");

        String schema = render("schema.ts.ftl").get("out");
        assertThat(schema).contains("export const productSchema");
        assertThat(schema).contains("rowKey: 'productId'");
        assertThat(schema).contains("permissionPrefix: 'demo:product'");
        assertThat(schema).contains("dict: 'sys_normal_disable'");
    }

    @Test
    void 菜单模板_包含五个权限() {
        String sql = render("menu.sql.ftl").get("out");
        assertThat(sql).contains("'demo:product:list'");
        assertThat(sql).contains("'demo:product:query'");
        assertThat(sql).contains("'demo:product:add'");
        assertThat(sql).contains("'demo:product:edit'");
        assertThat(sql).contains("'demo:product:remove'");
    }
}
