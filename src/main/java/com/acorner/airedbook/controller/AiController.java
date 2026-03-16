package com.acorner.airedbook.controller;

import com.acorner.airedbook.common.Result;
import com.acorner.airedbook.entity.dto.AiGenerationResult;
import com.acorner.airedbook.service.AiService;
import com.acorner.airedbook.service.AiTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/ai")
@RequiredArgsConstructor
public class AiController {

    private final AiService aiService;
    private final AiTaskService aiTaskService;

    @GetMapping("/generate")
    public Result<AiGenerationResult> testGenerate(
            @RequestParam(defaultValue = "999") Long taskId,
            @RequestParam(defaultValue = "1") Long userId,
            @RequestParam String imageUrl,
            @RequestParam(required = false) String prompt) {

        AiGenerationResult result = aiService.generateRedbookCopywriting(taskId, userId, imageUrl, prompt);
        return Result.success(result);
    }

    // =========================================================
    // 🌟 以下是升级后的“草稿箱与多模态分布发布”接口
    // =========================================================

    /**
     * 🌟 草稿箱功能 1：打回修改文案 (状态 2 -> 1 -> 2)
     */
    @PostMapping("/task/refine")
    public Result<String> refineTextDraft(
            @RequestParam("taskId") Long taskId,
            @RequestParam("prompt") String prompt) {

        aiTaskService.refineTextDraft(taskId, prompt);
        return Result.success("已提交文案修改请求，极速重写中...");
    }

    /**
     * 🌟 草稿箱功能 2：对文案满意，请求合成视频 (状态 2 -> 3 -> 4)
     */
    @PostMapping("/task/video/start")
    public Result<String> startVideoGeneration(@RequestParam("taskId") Long taskId) {

        aiTaskService.generateVideoDraft(taskId);
        return Result.success("已开始后台合成带 BGM 的视频，请稍后查询状态");
    }

    /**
     * 🌟 发布分支 A：直接发布为图文帖子 (提取原图+文案落库)
     */
    @PostMapping("/task/publish/image")
    public Result<Long> publishImagePost(@RequestParam("taskId") Long taskId) {

        Long postId = aiTaskService.publishImagePost(taskId);
        return Result.success(postId);
    }

    /**
     * 🌟 发布分支 B：发布为视频帖子 (提取合成好的视频+文案落库)
     */
    @PostMapping("/task/publish/video")
    public Result<Long> publishVideoPost(@RequestParam("taskId") Long taskId) {

        Long postId = aiTaskService.publishVideoPost(taskId);
        return Result.success(postId);
    }
}