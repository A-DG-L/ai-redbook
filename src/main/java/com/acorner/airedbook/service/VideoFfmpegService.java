package com.acorner.airedbook.service;

public interface VideoFfmpegService {

    /**
     * 异步执行视频合成任务
     * @param taskId     任务ID（对应 ai_task 表主键）
     * @param imageDir   存放序列化图片的临时目录
     * @param bgmPath    背景音乐本地路径
     * @param outputPath 输出视频的本地路径
     */
    boolean generateVideoSync(Long taskId, String imageDir, String bgmPath, String outputPath);
}