package com.acorner.airedbook.service.impl;

import com.acorner.airedbook.common.BusinessException;
import com.acorner.airedbook.config.BailianProperties;
import com.acorner.airedbook.entity.dto.AiGenerationResult;
import com.acorner.airedbook.entity.Post;
import com.acorner.airedbook.mapper.PostMapper;
import com.acorner.airedbook.service.AiService;
import com.alibaba.fastjson2.JSON;
import com.alibaba.fastjson2.JSONArray;
import com.alibaba.fastjson2.JSONObject;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate; // 🌟 引入 Redis
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.io.File;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.Set;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiServiceImpl implements AiService {

    private final RestTemplate restTemplate;
    private final BailianProperties bailianProperties;
    private final PostMapper postMapper;
    // 🌟 1. 注入 StringRedisTemplate
    private final StringRedisTemplate stringRedisTemplate;

    private static final String TEMP_DIR = System.getProperty("user.dir") + File.separator + "temp";
    // 🌟 2. 定义 Redis Key 前缀
    private static final String CHAT_CONTEXT_KEY_PREFIX = "ai:chat:context:task:";

    @Override
    // 🌟 3. 方法签名增加 taskId，用于精准定位是哪一条草稿的对话
    public AiGenerationResult generateRedbookCopywriting(Long taskId, Long userId, String imageUrl, String userPrompt) {
        log.info("🎬 导演 AI 开始工作，为用户 {}，任务 {} 生成内容...", userId, taskId);

        String redisKey = CHAT_CONTEXT_KEY_PREFIX + taskId;

        // ==========================================
        // 🌟 4. 双擎 RAG 上下文组装 (保持不变)
        // ==========================================
        LambdaQueryWrapper<Post> wrapper = new LambdaQueryWrapper<>();
        wrapper.eq(Post::getUserId, userId).orderByDesc(Post::getLikeCount).last("LIMIT 3");
        List<Post> topPosts = postMapper.selectList(wrapper);

        StringBuilder historyStyle = new StringBuilder("用户的历史发帖风格参考：\n");
        if (topPosts != null && !topPosts.isEmpty()) {
            for (Post p : topPosts) {
                historyStyle.append("- ").append(p.getContent()).append("\n");
            }
        } else {
            historyStyle.append("（暂无，保持亲切的网感）\n");
        }
        Set<String> topTags = stringRedisTemplate.opsForZSet().reverseRange("system:hot:tags", 0, 2);

        String hotHashtagsStr;
        if (topTags != null && !topTags.isEmpty()) {
            // 如果 Redis 里有数据，就把真实的标签拼接进去
            hotHashtagsStr = "当前热门标签：" + String.join(" ", topTags) + "。请在文案末尾选用1-2个。";
            log.info("📈 成功为大模型注入动态热榜：{}", hotHashtagsStr);
        } else {
            // 如果系统刚上线，Redis 是空的，给一个兜底配置
            hotHashtagsStr = "当前热门标签：#日常 #生活记录 #分享。请在文案末尾选用1-2个。";
            log.info("⚠️ 当前无热榜数据，使用兜底标签");
        }

        String systemPrompt = "你是一个精通小红书爆款逻辑的文案大师，同时也是个视频配乐导演。\n" +
                "请仔细观察用户提供的图片，生成吸引眼球的标题和正文。排版要清晰（穿插Emoji）。\n" +
                historyStyle.toString() + hotHashtagsStr + "\n" +
                "【绝密任务】：你必须判断图片的情绪，并在整篇文案的最末尾，严格按照格式输出配乐暗号，只能是以下四个之一：\n" +
                "[BGM:happy]\n" +
                "[BGM:sad]\n" +
                "[BGM:healing]\n" +
                "[BGM:default]";

        String baseUserPrompt = (userPrompt != null && !userPrompt.isEmpty())
                ? userPrompt : "请帮我根据这张图片写文案，并选一首合适的 BGM。";
        String finalUserPrompt = baseUserPrompt + "\n\n⚠️注意：无论如何，请务必在整篇文案的最末尾（独立占一行），严格输出配乐暗号，格式必须是 [BGM:happy]、[BGM:sad]、[BGM:healing] 或 [BGM:default] 中的一个！不要漏掉！";

        // ==========================================
        // 🌟 5. Redis List 记忆加载与判断
        // ==========================================
        JSONArray messages = new JSONArray();
        messages.add(createMessage("system", systemPrompt)); // 系统人设永远在第一位

        List<String> historyList = stringRedisTemplate.opsForList().range(redisKey, 0, -1);
        boolean isFirstTurn = (historyList == null || historyList.isEmpty());
        JSONObject currentUserMessage = new JSONObject();
        currentUserMessage.put("role", "user");

        if (isFirstTurn) {
            log.info("🆕 检测到是首轮对话，携带图片 URL 发送...");
            // 首轮对话：必须携带图片和文字 (多模态结构)
            JSONArray userContent = new JSONArray();
            JSONObject imageContent = new JSONObject();
            imageContent.put("type", "image_url");
            JSONObject imageUrlObj = new JSONObject();
            imageUrlObj.put("url", imageUrl);
            imageContent.put("image_url", imageUrlObj);
            userContent.add(imageContent);

            JSONObject textContent = new JSONObject();
            textContent.put("type", "text");
            textContent.put("text", finalUserPrompt);
            userContent.add(textContent);

            currentUserMessage.put("content", userContent);
        } else {
            log.info("🔄 检测到是多轮对话，加载之前 {} 条历史记忆...", historyList.size());
            // 多轮对话：加载历史
            for (String hist : historyList) {
                messages.add(JSON.parseObject(hist));
            }
            // 多轮对话中，用户通常只是输入修改意见（纯文本），不需要重新传图，节省 Token
            currentUserMessage.put("content", finalUserPrompt);
        }

        messages.add(currentUserMessage);

        // ==========================================
        // 🌟 6. 发送请求给大模型
        // ==========================================
        JSONObject requestBody = new JSONObject();
        requestBody.put("model", bailianProperties.getModel());
        requestBody.put("messages", messages);

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(bailianProperties.getApiKey());

        try {
            HttpEntity<String> entity = new HttpEntity<>(requestBody.toJSONString(), headers);
            String responseStr = restTemplate.postForObject(bailianProperties.getEndpoint(), entity, String.class);
            JSONObject responseJson = JSON.parseObject(responseStr);

            String rawContent = responseJson.getJSONArray("choices").getJSONObject(0).getJSONObject("message").getString("content");

            // ==========================================
            // 🌟 7. 拦截暗号，分配 BGM (保持不变)
            // ==========================================
            String bgmPath = TEMP_DIR + File.separator + "default_bgm.mp3";
            String finalContent = rawContent;

            Pattern pattern = Pattern.compile("\\[BGM:\\s*([a-zA-Z]+)\\]", Pattern.CASE_INSENSITIVE);
            Matcher matcher = pattern.matcher(rawContent);

            if (matcher.find()) {
                String mood = matcher.group(1).toLowerCase();
                bgmPath = searchLocalBgmMock(mood);
                finalContent = matcher.replaceFirst("").trim(); // 擦除暗号
            }

            // ==========================================
            // 🌟 8. 将本次对话存入 Redis List，形成记忆闭环
            // ==========================================
            // 存入用户的提问
            stringRedisTemplate.opsForList().rightPush(redisKey, currentUserMessage.toJSONString());
            // 存入 AI 的回答 (存净文案，不存暗号，防止 AI 下次学坏把暗号直接写在正文里)
            JSONObject assistantMessage = createMessage("assistant", finalContent);
            stringRedisTemplate.opsForList().rightPush(redisKey, assistantMessage.toJSONString());

            // 设置 24 小时过期，防止草稿箱数据无限制膨胀
            stringRedisTemplate.expire(redisKey, 24, TimeUnit.HOURS);

            AiGenerationResult result = new AiGenerationResult();
            result.setContent(finalContent);
            result.setBgmPath(bgmPath);
            return result;

        } catch (Exception e) {
            log.error("调用 AI 大模型接口失败", e);
            throw new BusinessException("AI 服务暂时不可用：" + e.getMessage());
        }
    }

    private JSONObject createMessage(String role, String content) {
        JSONObject msg = new JSONObject();
        msg.put("role", role);
        msg.put("content", content);
        return msg;
    }

    private String searchLocalBgmMock(String mood) {
        if (mood.contains("happy")) return TEMP_DIR + File.separator + "happy_bgm.mp3";
        if (mood.contains("sad")) return TEMP_DIR + File.separator + "sad_bgm.mp3";
        if (mood.contains("healing")) return TEMP_DIR + File.separator + "healing_bgm.mp3";
        return TEMP_DIR + File.separator + "default_bgm.mp3";
    }
}