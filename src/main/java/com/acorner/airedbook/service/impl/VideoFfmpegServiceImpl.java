package com.acorner.airedbook.service.impl;

import com.acorner.airedbook.service.VideoFfmpegService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.File;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
@RequiredArgsConstructor
public class VideoFfmpegServiceImpl implements VideoFfmpegService {

    private static final String FFMPEG_CMD = "ffmpeg";

    @Override
    public boolean generateVideoSync(Long taskId, String imageDir, String bgmPath, String outputPath) {
        log.info("[Task ID: {}] 开始执行 FFmpeg 视频合成任务, 目录: {}", taskId, imageDir);

        Process process = null;
        try {
            List<String> command = new ArrayList<>();
            command.add(FFMPEG_CMD);
            command.add("-y");
            command.add("-framerate");
            command.add("1/2");
            command.add("-f");
            command.add("image2");
            command.add("-i");
            command.add(imageDir + File.separator + "img%d.png");

            if (bgmPath != null && new File(bgmPath).exists()) {
                command.add("-i");
                command.add(bgmPath);
                command.add("-c:a");
                command.add("aac");
                command.add("-shortest");
            } else {
                log.warn("[Task ID: {}] 未找到 BGM 文件或路径为空，将生成无声视频！路径: {}", taskId, bgmPath);
            }

            command.add("-c:v");
            command.add("libx264");
            command.add("-pix_fmt");
            command.add("yuv420p");

            // 🌟 核心优化：强制裁剪与黑边填充，统一输出为 1080x1920 的竖屏短视频格式！
            command.add("-vf");
            command.add("scale=1080:1920:force_original_aspect_ratio=decrease,pad=1080:1920:(ow-iw)/2:(oh-ih)/2:black");

            command.add("-r");
            command.add("25");
            command.add(outputPath);

            ProcessBuilder pb = new ProcessBuilder(command);
            pb.redirectErrorStream(true);
            process = pb.start();

            try (BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream(), "GBK"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    log.debug("[FFmpeg Output] {}", line);
                }
            }

            boolean finished = process.waitFor(5, TimeUnit.MINUTES);

            if (!finished) {
                log.error("[Task ID: {}] 任务超时！强制杀掉进程", taskId);
                process.destroyForcibly();
                return false;
            }

            int exitCode = process.exitValue();
            if (exitCode == 0) {
                log.info("🎉 [Task ID: {}] 视频合成大功告成！文件位置: {}", taskId, outputPath);
                return true;
            } else {
                log.error("❌ [Task ID: {}] FFmpeg 报错退出，退出码: {}", taskId, exitCode);
                return false;
            }

        } catch (Exception e) {
            log.error("[Task ID: {}] 发生未知异常", taskId, e);
            if (process != null) {
                process.destroyForcibly();
            }
            return false;
        }
    }
}