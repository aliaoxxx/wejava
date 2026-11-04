package com.wejava.gen.controller;

import com.wejava.common.annotation.BusinessType;
import com.wejava.common.annotation.OperLog;
import com.wejava.common.core.PageResult;
import com.wejava.common.core.R;
import com.wejava.gen.model.form.GenTableForm;
import com.wejava.gen.model.query.GenTableQuery;
import com.wejava.gen.model.vo.DbTableVO;
import com.wejava.gen.model.vo.GenTableVO;
import com.wejava.gen.model.vo.GenTemplateVO;
import com.wejava.gen.service.GenTableService;
import jakarta.validation.Valid;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * 代码生成器。
 *
 * <p>只读数据库元数据并渲染模板，不会写入服务器文件系统 —— 产物一律以 zip 下载，
 * 避免路径穿越和误覆盖。
 */
@RestController
@RequestMapping("/system/gen/table")
@RequiredArgsConstructor
public class GenTableController {

    private final GenTableService genTableService;

    @PreAuthorize("@ps.has('system:gen:list')")
    @GetMapping("/list")
    public R<PageResult<GenTableVO>> list(GenTableQuery query) {
        return R.ok(genTableService.page(query));
    }

    /** 数据库中可导入的表 */
    @PreAuthorize("@ps.has('system:gen:list')")
    @GetMapping("/db/list")
    public R<List<DbTableVO>> dbList() {
        return R.ok(genTableService.listDbTables());
    }

    @PreAuthorize("@ps.has('system:gen:import')")
    @OperLog(title = "代码生成", businessType = BusinessType.INSERT)
    @PostMapping("/import")
    public R<Integer> importTables(@RequestBody List<String> tableNames) {
        return R.ok(genTableService.importTables(tableNames));
    }

    @PreAuthorize("@ps.has('system:gen:query')")
    @GetMapping("/{tableId}")
    public R<GenTableVO> detail(@PathVariable Long tableId) {
        return R.ok(genTableService.detail(tableId));
    }

    @PreAuthorize("@ps.has('system:gen:edit')")
    @OperLog(title = "代码生成", businessType = BusinessType.UPDATE)
    @PutMapping("/{tableId}")
    public R<Void> update(@PathVariable Long tableId, @RequestBody @Valid GenTableForm form) {
        genTableService.update(tableId, form);
        return R.ok();
    }

    @PreAuthorize("@ps.has('system:gen:remove')")
    @OperLog(title = "代码生成", businessType = BusinessType.DELETE)
    @DeleteMapping("/{tableIds}")
    public R<Void> remove(@PathVariable List<Long> tableIds) {
        genTableService.remove(tableIds);
        return R.ok();
    }

    @PreAuthorize("@ps.has('system:gen:code')")
    @GetMapping("/preview/{tableId}")
    public R<Map<String, String>> preview(@PathVariable Long tableId) {
        return R.ok(genTableService.preview(tableId));
    }

    @PreAuthorize("@ps.has('system:gen:code')")
    @OperLog(title = "代码生成", businessType = BusinessType.EXPORT)
    @PostMapping("/batch-download")
    public ResponseEntity<byte[]> batchDownload(@RequestBody List<Long> tableIds) {
        return zip(genTableService.batchDownload(tableIds), "wejava-code-batch.zip");
    }

    @PreAuthorize("@ps.has('system:gen:code')")
    @GetMapping("/template/list")
    public R<List<GenTemplateVO>> templateList() {
        return R.ok(genTableService.templates());
    }

    @PreAuthorize("@ps.has('system:gen:code')")
    @GetMapping("/template/{templateName}")
    public R<String> templateContent(@PathVariable String templateName) {
        return R.ok(genTableService.templateContent(templateName));
    }

    @PreAuthorize("@ps.has('system:gen:code')")
    @OperLog(title = "代码生成模板", businessType = BusinessType.UPDATE)
    @PutMapping("/template/{templateName}")
    public R<Void> saveTemplate(@PathVariable String templateName, @RequestBody Map<String, String> body) {
        genTableService.saveTemplate(templateName, body.get("content"));
        return R.ok();
    }

    @PreAuthorize("@ps.has('system:gen:code')")
    @OperLog(title = "代码生成模板", businessType = BusinessType.DELETE)
    @DeleteMapping("/template/{templateName}")
    public R<Void> resetTemplate(@PathVariable String templateName) {
        genTableService.resetTemplate(templateName);
        return R.ok();
    }

    @PreAuthorize("@ps.has('system:gen:code')")
    @OperLog(title = "代码生成", businessType = BusinessType.EXPORT)
    @GetMapping("/download/{tableId}")
    public ResponseEntity<byte[]> download(@PathVariable Long tableId) {
        return zip(genTableService.download(tableId), "wejava-code-" + tableId + ".zip");
    }

    private ResponseEntity<byte[]> zip(byte[] bytes, String rawName) {
        String fileName = URLEncoder.encode(rawName, StandardCharsets.UTF_8);
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename*=UTF-8''" + fileName)
                .contentType(MediaType.APPLICATION_OCTET_STREAM)
                .body(bytes);
    }
}
