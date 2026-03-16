package com.acorner.airedbook.controller;

import com.acorner.airedbook.common.Result;
import com.acorner.airedbook.entity.AiTask;
import com.acorner.airedbook.mapper.AiTaskMapper;
import com.acorner.airedbook.service.AiTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import com.acorner.airedbook.service.VideoFfmpegService;

import java.util.List;

@RestController
@RequiredArgsConstructor
public class TestController {

//    private final VideoFfmpegService videoFfmpegService;
    private final AiTaskService aiTaskService;
    private final AiTaskMapper aiTaskMapper;

    @GetMapping("/test")
    public Result<String> hello() {
        // 使用咱们刚刚封装好的 Result.success() 返回数据！
        return Result.success("恭喜你！带有统一规范的接口调用成功啦！");
    }

//    @GetMapping("/test/makeVideo")
//    public String testMakeVideo() {
//        // 1. 指定你的测试目录
//        String imageDir = "F:\\temp\\task_1";
//
//        // 2. 如果你有MP3，就放进去并写名字；如果没有，可以直接传 null
//        // 假设你在文件夹里放了一个 bgm.mp3：
//        // String bgmPath = "F:\\temp\\task_1\\bgm.mp3";
//        String bgmPath = null; // 咱们先不加音乐，跑通纯视频再说
//
//        // 3. 最终视频的名字
//        String outputPath = "F:\\temp\\task_1\\output.mp4";
//
//        // 异步调用！注意，方法会瞬间返回，但后台已经在干活了
//        videoFfmpegService.generateVideoAsync(999L, imageDir, bgmPath, outputPath);
//
//        return "任务已提交！快去看 IDEA 的控制台日志，或者去 F:\\temp\\task_1 等 output.mp4 出现！";
//    }

    @GetMapping("/test/fullPipeline")
    public String testFullPipeline() {
        // 1. 模拟前端往数据库里塞了一个新任务 (包含 3 张网图)
        AiTask mockTask = new AiTask();
        mockTask.setUserId(1001L);
        mockTask.setStatus(0); // 0: 排队中
        // ⚠️ 这里的 URL 必须替换成你 Day 3 已经传到你自己 OSS 里的真实图片链接！
        mockTask.setSourceImages(List.of(
                "https://ai-redbook-media.oss-cn-hangzhou.aliyuncs.com/3d0a63ee5e8e4f27878dda8ef4a96222.png",
                "https://ai-redbook-media.oss-cn-hangzhou.aliyuncs.com/2e7b4d58ac63410ca19207216e4780e4.png"
        ));
        aiTaskMapper.insert(mockTask);

        Long newTaskId = mockTask.getId();

        // 2. 召唤总指挥，开始全自动流水线作业！
//        aiTaskService.executeVideoGeneration(newTaskId);

        return "🎉 任务 " + newTaskId + " 已派发！快去盯着 IDEA 控制台看神迹吧！";
    }
}