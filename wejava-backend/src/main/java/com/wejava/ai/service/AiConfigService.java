package com.wejava.ai.service;

import com.wejava.ai.model.form.AiConfigForm;
import com.wejava.ai.model.vo.AiConfigVO;

public interface AiConfigService {

    /** 管理页读取（密钥掩码） */
    AiConfigVO get();

    void update(AiConfigForm form);

    /** 对话链路使用：返回解密后的明文密钥，未配置时返回 null */
    String plainApiKey();

    String model();

    String baseUrl();

    Integer maxTokens();

    double temperature();

    boolean enabled();
}
