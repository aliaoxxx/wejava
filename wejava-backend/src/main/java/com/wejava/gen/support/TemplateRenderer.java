package com.wejava.gen.support;

import freemarker.template.Configuration;
import freemarker.template.Template;
import freemarker.template.TemplateException;
import java.io.IOException;
import java.io.StringWriter;
import java.nio.charset.StandardCharsets;
import org.springframework.stereotype.Component;

/**
 * FreeMarker 渲染器。
 *
 * <p>模板放 {@code classpath:template/gen}，用户可以直接改模板文件来匹配自己的代码风格，
 * 不需要动 Java 代码。
 */
@Component
public class TemplateRenderer {

    private final Configuration configuration;

    public TemplateRenderer() {
        configuration = new Configuration(Configuration.DEFAULT_INCOMPATIBLE_IMPROVEMENTS);
        configuration.setClassLoaderForTemplateLoading(GenModel.class.getClassLoader(), "template/gen");
        configuration.setDefaultEncoding(StandardCharsets.UTF_8.name());
        configuration.setNumberFormat("computer");
        configuration.setLogTemplateExceptions(false);
    }

    public String render(String templateName, Object model) {
        try {
            Template template = configuration.getTemplate(templateName);
            StringWriter writer = new StringWriter();
            template.process(model, writer);
            return writer.toString();
        } catch (IOException e) {
            throw new IllegalStateException("模板读取失败：" + templateName, e);
        } catch (TemplateException e) {
            throw new IllegalStateException("模板渲染失败：" + templateName + "，" + e.getMessage(), e);
        }
    }
}
