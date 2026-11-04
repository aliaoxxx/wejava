package com.wejava.gen.support;

import com.wejava.gen.model.entity.GenTable;
import com.wejava.gen.model.entity.GenTableColumn;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * 把「表配置 + 字段配置」整理成模板直接可用的模型。
 *
 * <p>模板里只做展示，所有推导（命名、类型、过滤）都在这里完成，
 * 这样模板文件保持简单，也方便用户替换模板而不影响逻辑。
 */
@Component
public class GenModel {

    public Map<String, Object> build(GenTable table, List<GenTableColumn> columns) {
        Map<String, Object> model = new LinkedHashMap<>();

        List<Map<String, Object>> all = new ArrayList<>();
        List<Map<String, Object>> list = new ArrayList<>();
        List<Map<String, Object>> queries = new ArrayList<>();
        List<Map<String, Object>> inserts = new ArrayList<>();
        List<Map<String, Object>> edits = new ArrayList<>();
        Set<String> entityImports = new LinkedHashSet<>();
        Set<String> voImports = new LinkedHashSet<>();
        Set<String> formImports = new LinkedHashSet<>();
        Set<String> queryImports = new LinkedHashSet<>();
        boolean hasNotBlank = false;
        boolean hasNotNull = false;
        boolean hasSize = false;
        Map<String, Object> pk = null;

        for (GenTableColumn column : columns) {
            Map<String, Object> item = toItem(column, table.getClassName());
            all.add(item);
            if (isTrue(column.getIsList())) {
                list.add(item);
            }
            if (isTrue(column.getIsQuery())) {
                queries.add(item);
            }
            if (isTrue(column.getIsInsert()) && !isTrue(column.getIsPk())) {
                inserts.add(item);
            }
            if (isTrue(column.getIsEdit()) && !isTrue(column.getIsPk())) {
                edits.add(item);
            }
            if (isTrue(column.getIsPk())) {
                pk = item;
            }
            String javaType = column.getJavaType();
            boolean inForm = !isTrue(column.getIsPk()) && (isTrue(column.getIsInsert()) || isTrue(column.getIsEdit()));
            // 实体跳过主键与审计字段，表单跳过主键与审计字段
            if (!isTrue(column.getIsPk()) && !ColumnTypes.isAuditColumn(column.getColumnName())) {
                addImport(entityImports, javaType);
                addImport(formImports, javaType);
            }
            if (isTrue(column.getIsQuery())) {
                addImport(queryImports, javaType);
            }
            addImport(voImports, javaType);
            if (inForm && isTrue(column.getIsRequired())) {
                if (ColumnTypes.JAVA_STRING.equals(javaType)) {
                    hasNotBlank = true;
                } else {
                    hasNotNull = true;
                }
            }
            if (inForm && ColumnTypes.JAVA_STRING.equals(javaType) && ColumnTypes.length(column.getColumnType()) > 0) {
                hasSize = true;
            }
        }

        Map<String, Object> tableMap = new LinkedHashMap<>();
        tableMap.put("tableId", table.getTableId());
        tableMap.put("tableName", table.getTableName());
        tableMap.put("tableComment", table.getTableComment());
        tableMap.put("className", table.getClassName());
        tableMap.put("classVar", GenNaming.toCamel(table.getClassName()));
        tableMap.put("moduleName", table.getModuleName());
        tableMap.put("businessName", table.getBusinessName());
        tableMap.put("businessCap", GenNaming.capitalize(table.getBusinessName()));
        tableMap.put("functionName", table.getFunctionName());
        tableMap.put("packageName", table.getPackageName());
        tableMap.put("author", table.getAuthor() == null ? "wejava" : table.getAuthor());
        tableMap.put("permissionPrefix", table.getModuleName() + ":" + table.getBusinessName());
        tableMap.put("routePath", "/" + table.getModuleName() + "/" + table.getBusinessName());
        tableMap.put("parentMenuId", table.getParentMenuId());
        tableMap.put("pkColumn", pk);

        model.put("table", tableMap);
        model.put("columns", all);
        model.put("listColumns", list);
        model.put("queryColumns", queries);
        model.put("insertColumns", inserts);
        model.put("editColumns", edits);
        model.put("entityImports", new ArrayList<>(entityImports));
        model.put("voImports", new ArrayList<>(voImports));
        model.put("formImports", new ArrayList<>(formImports));
        model.put("queryImports", new ArrayList<>(queryImports));
        model.put("hasNotBlank", hasNotBlank);
        model.put("hasNotNull", hasNotNull);
        model.put("hasSize", hasSize);
        return model;
    }

    private Map<String, Object> toItem(GenTableColumn column, String className) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("columnId", column.getColumnId());
        item.put("columnName", column.getColumnName());
        item.put("columnType", column.getColumnType());
        item.put("columnComment", column.getColumnComment() == null ? "" : column.getColumnComment());
        item.put("javaField", column.getJavaField());
        item.put("javaFieldCap", GenNaming.capitalize(column.getJavaField()));
        item.put("javaType", column.getJavaType());
        item.put("tsType", ColumnTypes.tsType(column.getJavaType()));
        item.put("required", isTrue(column.getIsRequired()));
        item.put("isPk", isTrue(column.getIsPk()));
        item.put("isIncrement", isTrue(column.getIsIncrement()));
        item.put("isList", isTrue(column.getIsList()));
        item.put("isQuery", isTrue(column.getIsQuery()));
        item.put("isInsert", isTrue(column.getIsInsert()));
        item.put("isEdit", isTrue(column.getIsEdit()));
        item.put("audit", ColumnTypes.isAuditColumn(column.getColumnName()));
        item.put("maxLength", ColumnTypes.length(column.getColumnType()));
        item.put("queryType", column.getQueryType() == null ? "EQ" : column.getQueryType());
        item.put("htmlType", column.getHtmlType() == null ? "input" : column.getHtmlType());
        item.put("dictType", column.getDictType());
        item.put("sort", column.getSort());

        // 精确构造查询链单行，超过 palantir 的 120 列就必须折行，
        // 否则 spotless:check 会判定格式不合规
        String fieldCap = GenNaming.capitalize(column.getJavaField());
        String indent = "                        ";
        String get = "::get" + fieldCap;
        String stringLine = indent + "." + column.getQueryType().toLowerCase() + "(StringUtils.hasText(query.get"
                + fieldCap + "()), " + className + get + ", query.get" + fieldCap + "())";
        String numberLine = indent + ".eq(query.get" + fieldCap + "() != null, " + className + get + ", query.get"
                + fieldCap + "())";
        String betweenLine = indent + ".ge(query.getBegin" + fieldCap + "() != null, " + className + get
                + ", query.getBegin" + fieldCap + "())";
        item.put("wrapStringCall", stringLine.length() > 120);
        item.put("wrapNumberCall", numberLine.length() > 120);
        item.put("wrapBetweenCall", betweenLine.length() > 120);
        return item;
    }

    private void addImport(Set<String> target, String javaType) {
        if (ColumnTypes.JAVA_LOCAL_DATE_TIME.equals(javaType)) {
            target.add("java.time.LocalDateTime");
        } else if (ColumnTypes.JAVA_LOCAL_DATE.equals(javaType)) {
            target.add("java.time.LocalDate");
        } else if (ColumnTypes.JAVA_BIG_DECIMAL.equals(javaType)) {
            target.add("java.math.BigDecimal");
        }
    }

    private boolean isTrue(Integer value) {
        return value != null && value == 1;
    }
}
