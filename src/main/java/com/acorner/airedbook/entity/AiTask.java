package com.acorner.airedbook.entity;

import com.baomidou.mybatisplus.annotation.IdType;
import com.baomidou.mybatisplus.annotation.TableField;
import com.baomidou.mybatisplus.annotation.TableId;
import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.extension.handlers.JacksonTypeHandler;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.List;

/**
 * AI生成任务表
 */
// 必须加 autoResultMap = true，否则底下的 TypeHandler 不生效！
@TableName(value = "ai_task", autoResultMap = true)
@Data
public class AiTask {

    @TableId(type = IdType.AUTO)
    private Long id;

    private Long userId;

    /**
     * 用户上传的原始图片路径(JSON数组)
     * 强类型 List<String> 更好用！MyBatis-Plus 会自动帮你做 JSON 和 List 的互相转换。
     */
    @TableField(typeHandler = JacksonTypeHandler.class)
    private List<String> sourceImages;

    /**
     * 0:排队中 1:处理中 2:成功 3:失败
     */
    private Integer status;

    private String resultVideoUrl;

    private String resultContent;

    private String errorMsg;

    private LocalDateTime createTime;

    private LocalDateTime updateTime;
}