package com.acorner.airedbook.controller;

import com.acorner.airedbook.common.Result;
import com.acorner.airedbook.common.context.UserContext;
import com.acorner.airedbook.entity.AiTask;
import com.acorner.airedbook.entity.Post;
import com.acorner.airedbook.entity.dto.PostPublishDTO;
import com.acorner.airedbook.service.AiTaskService;
import com.acorner.airedbook.service.FileService;
import com.acorner.airedbook.service.PostLikeService;
import com.acorner.airedbook.service.PostService;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@RestController
@RequestMapping("/api/post")
@RequiredArgsConstructor
public class PostController {


    private  final PostService postService;
    @Autowired
    private AiTaskService aiTaskService;
    @Autowired
    private FileService fileService;
    @Autowired
    private PostLikeService postLikeService;

    /**
     * 1. 发布帖子接口
     */
    @PostMapping("/publish")
    public Result<String> publish(@RequestBody PostPublishDTO dto) {
        Post post = new Post();
        BeanUtils.copyProperties(dto, post);

        // 从 ThreadLocal 获取当前登录用户ID
        post.setUserId(UserContext.getUserId());
        post.setCreateTime(java.time.LocalDateTime.now());

        postService.save(post);
        return Result.success("发布成功");
    }

    /**
     * 2. 获取帖子列表接口 (这是新加的部分)
     */
    @GetMapping("/list")
    public Result<Page<Post>> list(
            // 明确指定参数名 pageNum
            @RequestParam(value = "pageNum", defaultValue = "1") int pageNum,
            // 明确指定参数名 pageSize
            @RequestParam(value = "pageSize", defaultValue = "10") int pageSize) {

        Page<Post> postPage = postService.getPostList(pageNum, pageSize);
        return Result.success(postPage);
    }

    /**
     * 一键 AI 智能发帖
     */
//    @PostMapping("/ai-publish")
//    public Result<Post> aiPublish(
//            @RequestParam("file") MultipartFile file,
//            @RequestParam(value = "prompt", required = false) String prompt) {
//
//        // 1. 从 ThreadLocal 中获取当前登录的用户 ID (绝对安全，防越权！)
//        Long userId = UserContext.getUserId();
//
//        // 2. 文件非空校验
//        if (file == null || file.isEmpty()) {
//            return Result.error("必须上传一张图片才能让 AI 帮你写文案哦");
//        }
//
//        // 3. 调用 Service 编排业务
//        Post post = postService.publishWithAi(userId, file, prompt);
//
//        // 4. 返回成功，将完整帖子信息返回给前端展示
//        return Result.success(post);
//    }

    /**
     * 升级版：AI 智能发帖第一阶段 (生成文案与挑选BGM)
     */
    @PostMapping("/ai-publish-async")
    public Result<Long> aiPublishAsync(
            @RequestParam("files") List<MultipartFile> files, // 接收多图
            @RequestParam(value = "prompt", required = false) String prompt) {

        Long userId = UserContext.getUserId();

        if (files == null || files.isEmpty()) {
            return Result.error("必须上传至少一张图片");
        }

        // 1. 同步循环上传图片到 OSS，获取 URL 列表
        List<String> imageUrls = new ArrayList<>();
        for (MultipartFile file : files) {
            String url = fileService.uploadImage(file);
            imageUrls.add(url);
        }

        // 2. 创建排队任务落库 (状态 0: 排队中)
        AiTask aiTask = new AiTask();
        aiTask.setUserId(userId);
        aiTask.setSourceImages(imageUrls);
        aiTask.setStatus(0);
        aiTaskService.save(aiTask);

        // 3. 🌟 核心修改：只触发阶段一（极速生成文案）
        aiTaskService.generateTextDraft(aiTask.getId(), prompt);

        // 4. 秒回前端 TaskID！
        return Result.success(aiTask.getId());
    }

    /**
     * 查询 AI 任务执行状态
     * 供前端轮询使用：前端拿到 taskId 后，每隔 2-3 秒调用一次此接口
     */
    @GetMapping("/task-status/{taskId}")
    public Result<AiTask> getTaskStatus(@PathVariable("taskId") Long taskId) {
        // 1. 查出任务最新状态
        AiTask task = aiTaskService.getById(taskId);

        if (task == null) {
            return Result.error("任务不存在");
        }

        // 2. 权限校验：防止用户越权偷看别人的任务 (防御性编程！)
        Long currentUserId = UserContext.getUserId();
        if (!task.getUserId().equals(currentUserId)) {
            return Result.error("无权查看此任务进度");
        }

        // 3. 返回给前端
        // task.getStatus() 的含义 -> 0:排队中 1:处理中 2:成功 3:失败
        return Result.success(task);
    }

    // 🌟 记得在类头部引入：
    // private final PostLikeService postLikeService;

    /**
     * 高并发点赞接口 (Redis)
     */
    @PostMapping("/{postId}/like")
    public Result<String> likePost(@PathVariable("postId") Long postId) {
        Long userId = UserContext.getUserId();

        // 调用 Service 进行点赞状态切换
        boolean currentStatus = postLikeService.toggleLike(postId, userId);

        return Result.success(currentStatus ? "点赞成功" : "已取消点赞");
    }
}