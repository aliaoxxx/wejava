package com.wejava.ai.service;

import com.wejava.ai.model.bo.AiChatConfig;
import com.wejava.ai.model.form.AiConfigForm;
import com.wejava.ai.model.vo.AiConfigVO;
import com.wejava.ai.provider.AiProvider;
import java.util.List;

public interface AiConfigService {

    /** 端点列表（密钥掩码），默认端点排在最前 */
    List<AiConfigVO> list();

    AiConfigVO get(Long id);

    Long create(AiConfigForm form);

    void update(Long id, AiConfigForm form);

    /** 删除端点；默认端点与唯一端点不允许删除 */
    void delete(Long id);

    /** 设为聊天默认端点 */
    void setDefault(Long id);

    /** 厂商预设，供前端下拉 */
    List<AiProvider> providers();

    /** 拉取该端点支持的模型列表 */
    List<String> listModels(Long id);

    /** 连通性测试，返回耗时毫秒 */
    long test(Long id);

    /**
     * 对话链路使用：{@code configId} 为空时取默认端点。
     *
     * @return 未配置任何可用端点时返回 {@code null}
     */
    AiChatConfig resolve(Long configId);
}
