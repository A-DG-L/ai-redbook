package com.acorner.airedbook.service;

import com.acorner.airedbook.entity.dto.AiGenerationResult;

public interface AiService {
    /**
     * 多模态生成红书文案，并智能匹配 BGM (带 RAG 和 Function Call)
     */
    AiGenerationResult generateRedbookCopywriting(Long taskId, Long userId, String imageUrl, String userPrompt);

}