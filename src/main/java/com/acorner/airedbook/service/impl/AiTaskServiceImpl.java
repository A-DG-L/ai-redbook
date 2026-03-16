package com.acorner.airedbook.service.impl;

import com.acorner.airedbook.entity.AiTask;
import com.acorner.airedbook.entity.Post;
import com.acorner.airedbook.entity.dto.AiGenerationResult;
import com.acorner.airedbook.mapper.AiTaskMapper;
import com.acorner.airedbook.mapper.PostMapper;
import com.acorner.airedbook.service.AiService;
import com.acorner.airedbook.service.AiTaskService;
import com.acorner.airedbook.service.FileService;
import com.acorner.airedbook.service.VideoFfmpegService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate; // 🌟 新增 Redis
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiTaskServiceImpl extends ServiceImpl<AiTaskMapper, AiTask> implements AiTaskService {

    private final FileService fileService;
    private final VideoFfmpegService videoFfmpegService;
    private final AiService aiService;
    private final PostMapper postMapper;
    private final StringRedisTemplate stringRedisTemplate; // 🌟 注入 Redis

    private static final String PROJECT_ROOT = System.getProperty("user.dir");
    private static final String TEMP_DIR = PROJECT_ROOT + File.separator + "temp";
    private static final String DEFAULT_BGM_PATH = TEMP_DIR + File.separator + "default_bgm.mp3";
    private static final String WORKSPACE_BASE = TEMP_DIR + File.separator + "task_";

    // ==========================================
    // 🌟 阶段一：极速版 - 仅生成文案与挑选 BGM
    // ==========================================
    @Async("videoTaskExecutor")
    @Override
    public void generateTextDraft(Long taskId, String userPrompt) {
        log.info("📝 [图文草稿] 任务 {} 开始生成文案...", taskId);
        try {
            AiTask task = this.getById(taskId);
            if (task == null) return;

            task.setStatus(1); // 1: 正在生成文案
            this.updateById(task);

            AiGenerationResult aiResult = aiService.generateRedbookCopywriting(taskId, task.getUserId(), task.getSourceImages().get(0), userPrompt);

            // 🌟 核心：将 AI 选好的 BGM 存入 Redis，留给后续“视频合成”阶段使用！
            String bgmRedisKey = "ai:task:bgm:" + taskId;
            stringRedisTemplate.opsForValue().set(bgmRedisKey, aiResult.getBgmPath(), 24, TimeUnit.HOURS);

            task.setResultContent(aiResult.getContent());
            task.setStatus(2); // 2: 文案草稿已就绪，等待用户选择发图文还是生视频
            this.updateById(task);
            log.info("✅ [图文草稿] 任务 {} 文案已就绪！可以极速返回给用户了。", taskId);

        } catch (Exception e) {
            log.error("❌ 任务 {} 文案生成失败", taskId, e);
            markTaskFailed(taskId, e.getMessage());
        }
    }

    @Async("videoTaskExecutor")
    @Override
    public void refineTextDraft(Long taskId, String userPrompt) {
        log.info("🔄 [打回修改] 任务 {} 重新生成文案，利用大模型多轮记忆...", taskId);
        this.generateTextDraft(taskId, userPrompt); // 直接复用上面的方法即可！
    }

    // ==========================================
    // 🌟 阶段二：重算力版 - 仅当用户主动要求时才合成视频
    // ==========================================
    @Async("videoTaskExecutor")
    @Override
    public void generateVideoDraft(Long taskId) {
        log.info("🎬 [视频合成] 任务 {} 用户请求生成视频，开始拉起 FFmpeg...", taskId);
        String workspacePath = WORKSPACE_BASE + taskId;
        try {
            AiTask task = this.getById(taskId);
            if (task == null || task.getStatus() != 2) throw new RuntimeException("任务状态非法，必须是文案就绪状态才能生视频");

            task.setStatus(3); // 3: 正在合成视频
            this.updateById(task);

            // 🌟 核心：从 Redis 提取第一阶段定好的 BGM
            String bgmRedisKey = "ai:task:bgm:" + taskId;
            String selectedBgmPath = stringRedisTemplate.opsForValue().get(bgmRedisKey);
            if (selectedBgmPath == null) selectedBgmPath = DEFAULT_BGM_PATH;

            File workspace = new File(workspacePath);
            if (!workspace.exists()) workspace.mkdirs();

            List<String> imageUrls = task.getSourceImages();
            for (int i = 0; i < imageUrls.size(); i++) {
                String imgUrl = imageUrls.get(i).replaceAll("\\s+", "");
                fileService.downloadUrlToFile(imgUrl, workspacePath + File.separator + "img" + (i + 1) + ".png");
            }

            String outputVideoPath = workspacePath + File.separator + "output.mp4";
            boolean isVideoSuccess = videoFfmpegService.generateVideoSync(taskId, workspacePath, selectedBgmPath, outputVideoPath);
            if (!isVideoSuccess) throw new RuntimeException("FFmpeg 底层合成失败");

            String ossVideoUrl = fileService.uploadLocalFile(new File(outputVideoPath), ".mp4");

            task.setResultVideoUrl(ossVideoUrl);
            task.setStatus(4); // 4: 视频草稿已就绪，等待最终发布
            this.updateById(task);
            log.info("✅ [视频合成] 任务 {} 视频已生成，URL: {}", taskId, ossVideoUrl);

        } catch (Exception e) {
            log.error("❌ 任务 {} 视频合成失败", taskId, e);
            markTaskFailed(taskId, e.getMessage());
        } finally {
            deleteDirectory(new File(workspacePath));
        }
    }

    // ==========================================
    // 🌟 阶段三：灵活发布 - 图文与视频双管齐下
    // ==========================================
    @Override
    public Long publishImagePost(Long taskId) {
        AiTask task = this.getById(taskId);
        if (task.getStatus() < 2) throw new RuntimeException("文案还未就绪，无法发布");

        Post post = createBasePost(task);
        // 图文帖子：取第一张图作为封面
        post.setMediaUrl(task.getSourceImages().get(0));
        post.setMediaType(1); // 1: 图文
        postMapper.insert(post);

        extractAndRecordTags(post.getContent());

        finishTask(task);
        return post.getId();
    }

    @Override
    public Long publishVideoPost(Long taskId) {
        AiTask task = this.getById(taskId);
        if (task.getStatus() != 4) throw new RuntimeException("视频还未合成完毕，无法发布");

        Post post = createBasePost(task);
        post.setMediaUrl(task.getResultVideoUrl());
        post.setMediaType(2); // 2: 视频
        postMapper.insert(post);

        extractAndRecordTags(post.getContent());

        finishTask(task);
        return post.getId();
    }

    // --- 私有辅助方法 ---
    private Post createBasePost(AiTask task) {
        Post post = new Post();
        post.setUserId(task.getUserId());
        post.setContent(task.getResultContent());
        post.setIsAiGenerated(1);
        post.setLikeCount(0);
        post.setCreateTime(java.time.LocalDateTime.now());
        return post;
    }

    private void finishTask(AiTask task) {
        task.setStatus(5); // 5: 生命周期结束
        this.updateById(task);
        log.info("🎉 任务 {} 成功落库为正式帖子！", task.getId());
    }

    private void markTaskFailed(Long taskId, String errorMsg) {
        AiTask failedTask = new AiTask();
        failedTask.setId(taskId);
        failedTask.setStatus(9); // 9: 失败 (避免和原有的 3 冲突)
        failedTask.setErrorMsg(errorMsg);
        this.updateById(failedTask);
    }

    private void deleteDirectory(File dir) {
        if (dir.isDirectory()) {
            File[] files = dir.listFiles();
            if (files != null) for (File file : files) deleteDirectory(file);
        }
        dir.delete();
    }

    // 🌟 动态热榜核心逻辑：正则提取文案中的标签，并为其在 Redis 中加分
    private void extractAndRecordTags(String content) {
        if (content == null || content.isEmpty()) return;

        // 匹配 # 开头，后面跟着中文、字母或数字的标签
        Pattern pattern = Pattern.compile("#([\\w\\u4e00-\\u9fa5]+)");
        Matcher matcher = pattern.matcher(content);

        while (matcher.find()) {
            String tag = matcher.group(0); // 拿到完整的标签，例如 "#周末去哪儿"
            // 在 Redis 的 ZSet 中，给这个标签的 Score 增加 1.0
            stringRedisTemplate.opsForZSet().incrementScore("system:hot:tags", tag, 1.0);
            log.info("🔥 发现热词：{}，当前热度 +1", tag);
        }
    }
}