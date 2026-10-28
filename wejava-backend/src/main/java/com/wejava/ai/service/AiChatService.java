package com.wejava.ai.service;

import com.wejava.ai.model.form.AiChatForm;
import com.wejava.ai.model.vo.AiMessageVO;
import java.util.List;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

public interface AiChatService {

    /** 流式对话：增量文本通过 SSE 推送，结束后落库 */
    SseEmitter stream(Long userId, AiChatForm form);

    /** 某个会话的历史消息 */
    List<AiMessageVO> history(Long userId, Long sessionId);
}
