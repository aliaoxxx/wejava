package com.wejava.gen.support;

import com.baomidou.mybatisplus.core.toolkit.Wrappers;
import com.wejava.gen.mapper.GenTemplateMapper;
import com.wejava.gen.model.entity.GenTemplate;
import freemarker.cache.StringTemplateLoader;
import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.InputStream;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;

/**
 * FreeMarker 渲染器，支持「在线覆盖」。
 *
 * <p>模板默认来自 {@code classpath:template/gen}（打包在 WAR 内，只读）。
 * 在线编辑后内容写入 {@code gen_template} 表，渲染时优先生效；
 * 删除该记录即恢复默认模板。
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class TemplateRenderer {

    /** 内置模板目录 */
    public static final String BUILTIN_DIR = "template/gen";

    private final GenTemplateMapper templateMapper;

    /** 覆盖模板的编译缓存，键为模板名 */
    private final Map<String, Template> overrideCache = new ConcurrentHashMap<>();

    private final Configuration classpathConfiguration = buildClasspathConfiguration();

    private static Configuration buildClasspathConfiguration() {
        Configuration configuration = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        configuration.setClassLoaderForTemplateLoading(TemplateRenderer.class.getClassLoader(), BUILTIN_DIR);
        configuration.setDefaultEncoding(StandardCharsets.UTF_8.name());
        configuration.setNumberFormat("computer");
        configuration.setLogTemplateExceptions(false);
        return configuration;
    }

    public String render(String templateName, Object model) {
        try {
            Template template = resolve(templateName);
            StringWriter writer = new StringWriter();
            template.process(model, writer);
            return writer.toString();
        } catch (IOException e) {
            throw new IllegalStateException("模板读取失败：" + templateName, e);
        } catch (TemplateException e) {
            throw new IllegalStateException("模板渲染失败：" + templateName + "，" + e.getMessage(), e);
        }
    }

    /** 读库里的覆盖版本，没有则用内置版本 */
    private Template resolve(String templateName) throws IOException {
        Template cached = overrideCache.get(templateName);
        if (cached != null) {
            return cached;
        }
        GenTemplate override = templateMapper.selectOne(Wrappers.<GenTemplate>lambdaQuery()
                .eq(GenTemplate::getTemplateName, templateName)
                .last("LIMIT 1"));
        if (override == null) {
            return classpathConfiguration.getTemplate(templateName);
        }
        StringTemplateLoader loader = new StringTemplateLoader();
        loader.putTemplate(templateName, override.getContent());
        Configuration configuration = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        configuration.setTemplateLoader(loader);
        configuration.setDefaultEncoding(StandardCharsets.UTF_8.name());
        configuration.setNumberFormat("computer");
        configuration.setLogTemplateExceptions(false);
        Template template = configuration.getTemplate(templateName);
        overrideCache.put(templateName, template);
        return template;
    }

    /** 保存覆盖后清缓存，让下次渲染生效 */
    public void invalidate(String templateName) {
        overrideCache.remove(templateName);
    }

    /** 读取内置模板原文，用于「恢复默认」和首次在线编辑 */
    public String readBuiltin(String templateName) {
        ClassPathResource resource = new ClassPathResource(BUILTIN_DIR + "/" + templateName);
        try (InputStream in = resource.getInputStream()) {
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("模板不存在：" + templateName, e);
        }
    }
}
