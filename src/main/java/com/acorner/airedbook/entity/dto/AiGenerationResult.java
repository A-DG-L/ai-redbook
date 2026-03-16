package com.acorner.airedbook.entity.dto;

import lombok.Data;

/**
 * AI 生成结果的封装类 (DTO)
 */
@Data
public class AiGenerationResult {
    // AI 生成的小红书爆款文案
    private String content;

    // 经过 Function Call 后，AI 决定使用的本地 BGM 路径
    private String bgmPath;
}