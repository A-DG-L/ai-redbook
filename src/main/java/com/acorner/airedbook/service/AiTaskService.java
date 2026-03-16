package com.acorner.airedbook.service;

import com.acorner.airedbook.entity.AiTask;
import com.baomidou.mybatisplus.extension.service.IService;

public interface AiTaskService extends IService<AiTask> {

    // ================== 阶段一：图文与草稿 ==================
    /**
     * 1. 首次生成文案草稿 (状态 0 -> 1 -> 2)
     */
    void generateTextDraft(Long taskId, String userPrompt);

    /**
     * 2. 打回修改文案草稿 (多轮对话互动，极速响应)
     */
    void refineTextDraft(Long taskId, String userPrompt);


    // ================== 阶段二：视频与发布 ==================
    /**
     * 3. 用户确认文案后，请求合成视频 (状态 2 -> 3 -> 4)
     */
    void generateVideoDraft(Long taskId);

    /**
     * 4. 分支 A：直接发布图文帖子 (提取原图+文案落库)
     */
    Long publishImagePost(Long taskId);

    /**
     * 5. 分支 B：发布视频帖子 (提取合成后的视频+文案落库)
     */
    Long publishVideoPost(Long taskId);

}