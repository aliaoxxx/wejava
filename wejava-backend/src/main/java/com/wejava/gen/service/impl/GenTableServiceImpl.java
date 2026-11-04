package com.wejava.gen.service.impl;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import com.wejava.common.core.PageResult;
import com.wejava.common.exception.BizException;
import com.wejava.common.exception.ErrorCode;
import com.wejava.gen.mapper.GenTableColumnMapper;
import com.wejava.gen.mapper.GenTableMapper;
import com.wejava.gen.mapper.GenTemplateMapper;
import com.wejava.gen.model.entity.GenTable;
import com.wejava.gen.model.entity.GenTableColumn;
import com.wejava.gen.model.entity.GenTemplate;
import com.wejava.gen.model.form.GenTableColumnForm;
import com.wejava.gen.model.form.GenTableForm;
import com.wejava.gen.model.query.GenTableQuery;
import com.wejava.gen.model.vo.DbColumn;
import com.wejava.gen.model.vo.DbTableVO;
import com.wejava.gen.model.vo.GenTableColumnVO;
import com.wejava.gen.model.vo.GenTableVO;
import com.wejava.gen.model.vo.GenTemplateVO;
import com.wejava.gen.service.GenTableService;
import com.wejava.gen.support.ColumnTypes;
import com.wejava.gen.support.GenModel;
import com.wejava.gen.support.GenNaming;
import com.wejava.gen.support.TemplateRenderer;
import com.wejava.security.SecurityUtils;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.core.io.support.PathMatchingResourcePatternResolver;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

@Service
@RequiredArgsConstructor
public class GenTableServiceImpl implements GenTableService {

    /** 单次批量生成的上限，避免一次拉爆内存 */
    private static final int MAX_BATCH = 20;

    /** 查询条件推断用的列名 */
    private static final Set<String> QUERY_COLUMNS =
            Set.of("name", "code", "title", "status", "type", "no", "sn", "phone", "email");

    private final GenTableMapper tableMapper;
    private final GenTableColumnMapper columnMapper;
    private final GenTemplateMapper templateMapper;
    private final GenModel genModel;
    private final TemplateRenderer renderer;

    @Override
    public PageResult<GenTableVO> page(GenTableQuery query) {
        Page<GenTable> page = query.toPage();
        Page<GenTable> result = tableMapper.selectPage(
                page,
                Wrappers.<GenTable>lambdaQuery()
                        .like(StringUtils.hasText(query.getTableName()), GenTable::getTableName, query.getTableName())
                        .like(
                                StringUtils.hasText(query.getFunctionName()),
                                GenTable::getFunctionName,
                                query.getFunctionName())
                        .orderByDesc(GenTable::getTableId));
        return PageResult.of(result.convert(this::toVO));
    }

    @Override
    public List<DbTableVO> listDbTables() {
        List<DbTableVO> tables = tableMapper.selectDbTables();
        List<String> imported =
                tableMapper.selectList(Wrappers.<GenTable>lambdaQuery().select(GenTable::getTableName)).stream()
                        .map(GenTable::getTableName)
                        .toList();
        tables.forEach(item -> item.setImported(imported.contains(item.getTableName())));
        return tables;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public int importTables(List<String> tableNames) {
        if (tableNames == null || tableNames.isEmpty()) {
            return 0;
        }
        if (tableNames.size() > MAX_BATCH) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "单次最多导入 " + MAX_BATCH + " 张表");
        }
        int count = 0;
        for (String tableName : tableNames) {
            if (tableMapper.exists(Wrappers.<GenTable>lambdaQuery().eq(GenTable::getTableName, tableName))) {
                continue;
            }
            importOne(tableName);
            count++;
        }
        return count;
    }

    private void importOne(String tableName) {
        List<DbColumn> dbColumns = tableMapper.selectDbColumns(tableName);
        if (dbColumns.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "表不存在或没有字段：" + tableName);
        }
        DbTableVO dbTable = tableMapper.selectDbTables().stream()
                .filter(item -> tableName.equals(item.getTableName()))
                .findFirst()
                .orElseThrow(() -> new BizException(ErrorCode.BAD_REQUEST.getCode(), "表不存在：" + tableName));

        GenTable table = new GenTable();
        table.setTableName(tableName);
        table.setTableComment(dbTable.getTableComment());
        table.setClassName(GenNaming.toClassName(tableName));
        table.setModuleName(GenNaming.moduleName(tableName));
        table.setBusinessName(GenNaming.businessName(tableName));
        table.setFunctionName(GenNaming.defaultFunctionName(tableName, dbTable.getTableComment()));
        table.setPackageName("com.wejava." + GenNaming.moduleName(tableName));
        table.setAuthor(SecurityUtils.currentUsername());
        table.setCreateTime(LocalDateTime.now());
        table.setUpdateTime(LocalDateTime.now());
        tableMapper.insert(table);

        int sort = 0;
        for (DbColumn dbColumn : dbColumns) {
            GenTableColumn column = toColumn(table, dbColumn, sort++);
            columnMapper.insert(column);
        }
    }

    private GenTableColumn toColumn(GenTable table, DbColumn dbColumn, int sort) {
        String columnName = dbColumn.getColumnName();
        String columnType = dbColumn.getColumnType();
        String javaType = ColumnTypes.javaType(columnType);
        boolean pk = "PRI".equalsIgnoreCase(dbColumn.getColumnKey());
        boolean increment = dbColumn.getExtra() != null && dbColumn.getExtra().contains("auto_increment");
        boolean audit = ColumnTypes.isAuditColumn(columnName);
        boolean blob = "text".equals(ColumnTypes.baseType(columnType))
                || "mediumtext".equals(ColumnTypes.baseType(columnType))
                || "longtext".equals(ColumnTypes.baseType(columnType));

        GenTableColumn column = new GenTableColumn();
        column.setTableId(table.getTableId());
        column.setColumnName(columnName);
        column.setColumnType(columnType);
        column.setColumnComment(
                StringUtils.hasText(dbColumn.getColumnComment())
                        ? dbColumn.getColumnComment()
                        : GenNaming.camelToLabel(columnName));
        column.setJavaType(javaType);
        column.setJavaField(GenNaming.toCamel(columnName));
        column.setIsPk(pk ? 1 : 0);
        column.setIsIncrement(increment ? 1 : 0);
        column.setIsRequired(isRequired(dbColumn, pk, audit) ? 1 : 0);
        column.setIsInsert(pk || audit ? 0 : 1);
        column.setIsEdit(pk || audit ? 0 : 1);
        column.setIsList(blob ? 0 : (audit && !ColumnTypes.isListVisibleAuditColumn(columnName) ? 0 : 1));
        column.setIsQuery(isQueryCandidate(columnName, javaType, pk, audit) ? 1 : 0);
        column.setQueryType(ColumnTypes.queryType(javaType, columnName));
        column.setHtmlType(ColumnTypes.htmlType(javaType, columnName, columnType));
        String dict = dictType(table, columnName);
        column.setDictType(dict);
        // 绑定了字典的字符串列默认用下拉/单选，而不是文本框
        if (dict != null && "input".equals(column.getHtmlType())) {
            column.setHtmlType("select");
        }
        column.setSort(sort);
        return column;
    }

    private boolean isRequired(DbColumn dbColumn, boolean pk, boolean audit) {
        if (pk || audit) {
            return false;
        }
        return "NO".equalsIgnoreCase(dbColumn.getIsNullable()) && dbColumn.getColumnDefault() == null;
    }

    private boolean isQueryCandidate(String columnName, String javaType, boolean pk, boolean audit) {
        if (pk || audit) {
            return false;
        }
        String lower = columnName.toLowerCase();
        if (QUERY_COLUMNS.contains(lower)) {
            return true;
        }
        if (lower.contains("name") || lower.endsWith("_code") || lower.contains("title")) {
            return true;
        }
        // 编号、类型、状态、日期时间都是常见的筛选条件
        return lower.endsWith("_no")
                || lower.endsWith("_sn")
                || lower.endsWith("_type")
                || lower.endsWith("_status")
                || lower.endsWith("_date")
                || lower.endsWith("_time")
                || lower.endsWith("_at");
    }

    /**
     * 字典推断。
     *
     * <p>状态类列绑定系统通用字典；{@code xxx_type} 按项目惯例推断为
     * {@code {module}_{business}_type}（如 biz_contract 的 contract_type
     * → biz_contract_type），导入后可在页面上改成实际字典。
     */
    private String dictType(GenTable table, String columnName) {
        String lower = columnName.toLowerCase();
        return switch (lower) {
            case "status", "enabled", "visible" -> "sys_normal_disable";
            case "sex" -> "sys_user_sex";
            default -> lower.endsWith("_type") ? table.getModuleName() + "_" + table.getBusinessName() + "_type" : null;
        };
    }

    @Override
    public GenTableVO detail(Long tableId) {
        GenTable table = require(tableId);
        GenTableVO vo = toVO(table);
        vo.setColumns(columnMapper
                .selectList(Wrappers.<GenTableColumn>lambdaQuery()
                        .eq(GenTableColumn::getTableId, tableId)
                        .orderByAsc(GenTableColumn::getSort))
                .stream()
                .map(this::toColumnVO)
                .toList());
        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void update(Long tableId, GenTableForm form) {
        GenTable table = require(tableId);
        table.setTableComment(form.getTableComment());
        table.setClassName(form.getClassName());
        table.setModuleName(form.getModuleName());
        table.setBusinessName(form.getBusinessName());
        table.setFunctionName(form.getFunctionName());
        table.setPackageName(
                StringUtils.hasText(form.getPackageName()) ? form.getPackageName() : table.getPackageName());
        table.setAuthor(form.getAuthor());
        table.setParentMenuId(form.getParentMenuId());
        table.setUpdateTime(LocalDateTime.now());
        tableMapper.updateById(table);

        if (form.getColumns() == null) {
            return;
        }
        for (GenTableColumnForm columnForm : form.getColumns()) {
            GenTableColumn column = columnMapper.selectOne(Wrappers.<GenTableColumn>lambdaQuery()
                    .eq(GenTableColumn::getTableId, tableId)
                    .eq(GenTableColumn::getColumnName, columnForm.getColumnName())
                    .last("LIMIT 1"));
            if (column == null) {
                continue;
            }
            column.setColumnComment(columnForm.getColumnComment());
            column.setJavaType(
                    StringUtils.hasText(columnForm.getJavaType()) ? columnForm.getJavaType() : column.getJavaType());
            column.setJavaField(
                    StringUtils.hasText(columnForm.getJavaField()) ? columnForm.getJavaField() : column.getJavaField());
            column.setIsRequired(bool(columnForm.getIsRequired()));
            column.setIsInsert(bool(columnForm.getIsInsert()));
            column.setIsEdit(bool(columnForm.getIsEdit()));
            column.setIsList(bool(columnForm.getIsList()));
            column.setIsQuery(bool(columnForm.getIsQuery()));
            column.setQueryType(
                    StringUtils.hasText(columnForm.getQueryType()) ? columnForm.getQueryType() : column.getQueryType());
            column.setHtmlType(
                    StringUtils.hasText(columnForm.getHtmlType()) ? columnForm.getHtmlType() : column.getHtmlType());
            column.setDictType(columnForm.getDictType());
            column.setSort(columnForm.getSort() == null ? column.getSort() : columnForm.getSort());
            columnMapper.updateById(column);
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void remove(List<Long> tableIds) {
        if (tableIds == null || tableIds.isEmpty()) {
            return;
        }
        tableMapper.deleteByIds(tableIds);
        columnMapper.delete(Wrappers.<GenTableColumn>lambdaQuery().in(GenTableColumn::getTableId, tableIds));
    }

    @Override
    public Map<String, String> preview(Long tableId) {
        GenTable table = require(tableId);
        List<GenTableColumn> columns = columnMapper.selectList(Wrappers.<GenTableColumn>lambdaQuery()
                .eq(GenTableColumn::getTableId, tableId)
                .orderByAsc(GenTableColumn::getSort));
        return generate(table, columns);
    }

    @Override
    public byte[] download(Long tableId) {
        Map<String, String> files = preview(tableId);
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buffer, StandardCharsets.UTF_8)) {
            for (Map.Entry<String, String> entry : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(entry.getKey()));
                zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
            }
        } catch (IOException e) {
            throw new IllegalStateException("打包失败：" + e.getMessage(), e);
        }
        return buffer.toByteArray();
    }

    @Override
    public byte[] batchDownload(List<Long> tableIds) {
        if (tableIds == null || tableIds.isEmpty()) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "请选择要生成的表");
        }
        if (tableIds.size() > MAX_BATCH) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "单次最多生成 " + MAX_BATCH + " 张表");
        }
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(buffer, StandardCharsets.UTF_8)) {
            for (Long tableId : tableIds) {
                GenTable table = require(tableId);
                // 每张表一个子目录，避免同名文件互相覆盖
                zip.putNextEntry(new ZipEntry(table.getBusinessName() + "/README.txt"));
                zip.write(readme(table).getBytes(StandardCharsets.UTF_8));
                zip.closeEntry();
                for (Map.Entry<String, String> entry : preview(tableId).entrySet()) {
                    zip.putNextEntry(new ZipEntry(table.getBusinessName() + "/" + entry.getKey()));
                    zip.write(entry.getValue().getBytes(StandardCharsets.UTF_8));
                    zip.closeEntry();
                }
            }
        } catch (IOException e) {
            throw new IllegalStateException("打包失败：" + e.getMessage(), e);
        }
        return buffer.toByteArray();
    }

    @Override
    public List<GenTemplateVO> templates() {
        List<String> names = builtinTemplateNames();
        List<GenTemplate> overrides = templateMapper.selectList(null);
        return names.stream()
                .map(name -> {
                    GenTemplateVO vo = new GenTemplateVO();
                    vo.setTemplateName(name);
                    overrides.stream()
                            .filter(item -> name.equals(item.getTemplateName()))
                            .findFirst()
                            .ifPresent(item -> {
                                vo.setOverridden(true);
                                vo.setContentLength(
                                        item.getContent() == null
                                                ? 0
                                                : item.getContent().length());
                                vo.setUpdateTime(item.getUpdateTime());
                            });
                    if (vo.getOverridden() == null) {
                        vo.setOverridden(false);
                        vo.setContentLength(renderer.readBuiltin(name).length());
                    }
                    return vo;
                })
                .toList();
    }

    @Override
    public String templateContent(String templateName) {
        GenTemplate override = templateMapper.selectOne(Wrappers.<GenTemplate>lambdaQuery()
                .eq(GenTemplate::getTemplateName, templateName)
                .last("LIMIT 1"));
        if (override != null) {
            return override.getContent();
        }
        return renderer.readBuiltin(templateName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void saveTemplate(String templateName, String content) {
        if (!builtinTemplateNames().contains(templateName)) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "未知模板：" + templateName);
        }
        if (content == null || content.isBlank()) {
            throw new BizException(ErrorCode.BAD_REQUEST.getCode(), "模板内容不能为空");
        }
        GenTemplate exist = templateMapper.selectOne(Wrappers.<GenTemplate>lambdaQuery()
                .eq(GenTemplate::getTemplateName, templateName)
                .last("LIMIT 1"));
        if (exist == null) {
            GenTemplate entity = new GenTemplate();
            entity.setTemplateName(templateName);
            entity.setContent(content);
            entity.setCreateTime(LocalDateTime.now());
            entity.setUpdateTime(LocalDateTime.now());
            templateMapper.insert(entity);
        } else {
            exist.setContent(content);
            exist.setUpdateTime(LocalDateTime.now());
            templateMapper.updateById(exist);
        }
        renderer.invalidate(templateName);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resetTemplate(String templateName) {
        templateMapper.delete(Wrappers.<GenTemplate>lambdaQuery().eq(GenTemplate::getTemplateName, templateName));
        renderer.invalidate(templateName);
    }

    /** 内置模板清单：扫 classpath 目录，新增 .ftl 自动出现 */
    private List<String> builtinTemplateNames() {
        try {
            Resource[] resources = new PathMatchingResourcePatternResolver()
                    .getResources("classpath:" + TemplateRenderer.BUILTIN_DIR + "/*.ftl");
            return java.util.Arrays.stream(resources)
                    .map(resource -> {
                        String filename = resource.getFilename();
                        return filename == null ? "" : filename;
                    })
                    .filter(name -> !name.isEmpty())
                    .sorted(Comparator.naturalOrder())
                    .toList();
        } catch (IOException e) {
            throw new IllegalStateException("读取内置模板列表失败：" + e.getMessage(), e);
        }
    }

    private Map<String, String> generate(GenTable table, List<GenTableColumn> columns) {
        Map<String, Object> model = genModel.build(table, columns);
        String basePackage = table.getPackageName().replace('.', '/');
        String module = table.getModuleName();
        String business = table.getBusinessName();
        String className = table.getClassName();

        Map<String, String> files = new LinkedHashMap<>();
        files.put(
                "backend/src/main/java/" + basePackage + "/model/entity/" + className + ".java",
                renderer.render("entity.java.ftl", model));
        files.put(
                "backend/src/main/java/" + basePackage + "/model/vo/" + className + "VO.java",
                renderer.render("vo.java.ftl", model));
        files.put(
                "backend/src/main/java/" + basePackage + "/model/form/" + className + "Form.java",
                renderer.render("form.java.ftl", model));
        files.put(
                "backend/src/main/java/" + basePackage + "/model/query/" + className + "Query.java",
                renderer.render("query.java.ftl", model));
        files.put(
                "backend/src/main/java/" + basePackage + "/mapper/" + className + "Mapper.java",
                renderer.render("mapper.java.ftl", model));
        files.put(
                "backend/src/main/java/" + basePackage + "/service/" + className + "Service.java",
                renderer.render("service.java.ftl", model));
        files.put(
                "backend/src/main/java/" + basePackage + "/service/impl/" + className + "ServiceImpl.java",
                renderer.render("serviceImpl.java.ftl", model));
        files.put(
                "backend/src/main/java/" + basePackage + "/converter/" + className + "Converter.java",
                renderer.render("converter.java.ftl", model));
        files.put(
                "backend/src/main/java/" + basePackage + "/controller/" + className + "Controller.java",
                renderer.render("controller.java.ftl", model));
        files.put("frontend/src/api/" + module + "/" + business + ".ts", renderer.render("api.ts.ftl", model));
        files.put(
                "frontend/src/views/" + module + "/" + business + "/schema.ts",
                renderer.render("schema.ts.ftl", model));
        files.put(
                "frontend/src/views/" + module + "/" + business + "/index.vue",
                renderer.render("index.vue.ftl", model));
        files.put("sql/menu_" + table.getTableName() + ".sql", renderer.render("menu.sql.ftl", model));
        files.put("README.txt", readme(table));
        return files;
    }

    private String readme(GenTable table) {
        return """
                %s —— 代码生成产物

                目录说明：
                  backend/     放到 wejava-backend/src/main/java 下（包路径已对齐）
                  frontend/    放到 wejava-frontend/src 下
                  sql/         菜单与按钮权限，执行前确认父菜单 ID

                接下来要做的事：
                  1. 重启后端，确认编译通过
                  2. 执行 sql/menu_%s.sql，给角色授权
                  3. 刷新前端，菜单里即可看到「%s」
                  4. 把 schema.ts 里 type: 'select' 的字段补上 options 或 dict

                注意：生成的是普通代码，可以直接改，不会被覆盖。
                """
                .formatted(table.getFunctionName(), table.getTableName(), table.getFunctionName());
    }

    private GenTable require(Long tableId) {
        GenTable table = tableMapper.selectById(tableId);
        if (table == null) {
            throw new BizException(ErrorCode.DATA_NOT_FOUND);
        }
        return table;
    }

    private int bool(Boolean value) {
        return Boolean.TRUE.equals(value) ? 1 : 0;
    }

    private GenTableVO toVO(GenTable table) {
        GenTableVO vo = new GenTableVO();
        vo.setTableId(table.getTableId());
        vo.setTableName(table.getTableName());
        vo.setTableComment(table.getTableComment());
        vo.setClassName(table.getClassName());
        vo.setModuleName(table.getModuleName());
        vo.setBusinessName(table.getBusinessName());
        vo.setFunctionName(table.getFunctionName());
        vo.setPackageName(table.getPackageName());
        vo.setAuthor(table.getAuthor());
        vo.setParentMenuId(table.getParentMenuId());
        vo.setCreateTime(table.getCreateTime());
        vo.setUpdateTime(table.getUpdateTime());
        return vo;
    }

    private GenTableColumnVO toColumnVO(GenTableColumn column) {
        GenTableColumnVO vo = new GenTableColumnVO();
        vo.setColumnId(column.getColumnId());
        vo.setTableId(column.getTableId());
        vo.setColumnName(column.getColumnName());
        vo.setColumnType(column.getColumnType());
        vo.setColumnComment(column.getColumnComment());
        vo.setJavaType(column.getJavaType());
        vo.setJavaField(column.getJavaField());
        vo.setIsPk(column.getIsPk() != null && column.getIsPk() == 1);
        vo.setIsIncrement(column.getIsIncrement() != null && column.getIsIncrement() == 1);
        vo.setIsRequired(column.getIsRequired() != null && column.getIsRequired() == 1);
        vo.setIsInsert(column.getIsInsert() != null && column.getIsInsert() == 1);
        vo.setIsEdit(column.getIsEdit() != null && column.getIsEdit() == 1);
        vo.setIsList(column.getIsList() != null && column.getIsList() == 1);
        vo.setIsQuery(column.getIsQuery() != null && column.getIsQuery() == 1);
        vo.setQueryType(column.getQueryType());
        vo.setHtmlType(column.getHtmlType());
        vo.setDictType(column.getDictType());
        vo.setSort(column.getSort());
        return vo;
    }
}
