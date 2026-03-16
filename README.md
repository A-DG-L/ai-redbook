# AI Redbook (AI 小红书) 📱

基于 Spring Boot 3 + AI 大模型的小红书风格内容创作平台，支持 AI 智能生成文案、图文/视频帖子发布等功能。

## ✨ 核心功能

- **🤖 AI 智能文案生成**：上传图片和简单的提示词，AI 自动生成小红书风格文案
- **🎬 视频合成**：支持将图片与 BGM 合成为短视频
- **📝 草稿箱管理**：支持文案修改、打回重写、多轮优化
- **🚀 多渠道发布**：支持一键发布为图文帖子或视频帖子
- **❤️ 点赞互动**：基于 Redis 的高并发点赞功能
- **🔐 用户认证**：JWT Token 身份验证
- **☁️ 云存储**：阿里云 OSS 图片/视频存储

## 🛠️ 技术栈

### 后端框架
- **Spring Boot 3.2.5** - 核心框架
- **Java 21** - JDK 版本
- **MyBatis-Plus 3.5.5** - ORM 框架
- **MySQL** - 关系型数据库
- **Redis** - 缓存与高并发点赞计数

### AI 与大模型
- **阿里云百炼 (Bailian)** - AI 大模型服务
- **Qwen-Omni-Turbo** - 通义千问多模态模型
- **OpenAI 兼容接口** - 标准化的 API 调用方式

### 云服务与存储
- **阿里云 OSS** - 对象存储服务
- **FFmpeg** - 视频处理与合成

### 其他技术
- **JWT (java-jwt)** - Token 认证
- **Lombok** - 代码简化
- **FastJSON2** - JSON 处理
- **Redis Lettuce** - Redis 连接池

## 📋 快速开始

### 环境要求

- JDK 21+
- MySQL 8.0+
- Redis 7.0+
- Maven 3.6+
- FFmpeg（视频合成可选）

### 配置说明

#### 1. 数据库配置

在 `src/main/resources/application.yml` 中配置数据库连接：

```
yaml
spring:
  datasource:
    url: jdbc:mysql://localhost:3306/ai_redbook?serverTimezone=Asia/Shanghai&useUnicode=true&characterEncoding=utf-8
    username: your_username
    password: your_password
```
#### 2. Redis 配置

```
yaml
spring:
  data:
    redis:
      host: 127.0.0.1
      port: 6379
      password: your_redis_password
```
#### 3. 阿里云服务配置

```
yaml
aliyun:
  oss:
    endpoint: oss-cn-hangzhou.aliyuncs.com
    access-key-id: YOUR_ACCESS_KEY_ID
    access-key-secret: YOUR_ACCESS_KEY_SECRET
    bucket-name: ai-redbook-media
  
  bailian:
    api-key: YOUR_BAILIAN_API_KEY
    endpoint: https://dashscope.aliyuncs.com/compatible-mode/v1/chat/completions
    model: qwen-omni-turbo
```
### 启动项目

```
bash
# 克隆项目
git clone https://github.com/your-username/ai-redbook.git

# 进入项目目录
cd ai-redbook

# 使用 Maven 构建并运行
mvn spring-boot:run
```
项目默认运行在 `http://localhost:8080`

## 📡 API 接口

### AI 相关接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/ai/generate` | GET | AI 文案生成测试接口 |
| `/api/ai/task/refine` | POST | 修改文案草稿 |
| `/api/ai/task/video/start` | POST | 开始视频合成 |
| `/api/ai/task/publish/image` | POST | 发布图文帖子 |
| `/api/ai/task/publish/video` | POST | 发布视频帖子 |

### 帖子相关接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/post/publish` | POST | 手动发布帖子 |
| `/api/post/list` | GET | 获取帖子列表（分页） |
| `/api/post/ai-publish-async` | POST | AI 异步发帖（第一阶段） |
| `/api/post/task-status/{taskId}` | GET | 查询 AI 任务状态 |
| `/api/post/{postId}/like` | POST | 点赞/取消点赞 |

### 用户相关接口

| 接口 | 方法 | 描述 |
|------|------|------|
| `/api/user/login` | POST | 用户登录 |
| `/api/user/register` | POST | 用户注册 |
| `/api/user/info` | GET | 获取用户信息 |

## 🏗️ 项目结构

```

ai-redbook/
├── src/main/java/com/acorner/airedbook/
│   ├── common/                # 通用模块
│   │   ├── context/          # 上下文（用户信息）
│   │   ├── interceptor/      # 拦截器
│   │   ├── utils/            # 工具类（JWT）
│   │   ├── BusinessException.java
│   │   ├── GlobalExceptionHandler.java
│   │   └── Result.java       # 统一返回结果
│   ├── config/               # 配置类
│   │   ├── AsyncConfig.java  # 异步配置
│   │   ├── BailianProperties.java
│   │   ├── OssConfig.java
│   │   ├── RedisConfig.java
│   │   └── WebMvcConfig.java
│   ├── controller/           # 控制器层
│   │   ├── AiController.java
│   │   ├── PostController.java
│   │   ├── UserInfoController.java
│   │   └── FileController.java
│   ├── entity/               # 实体类
│   │   ├── dto/              # DTO 对象
│   │   ├── AiTask.java
│   │   ├── Post.java
│   │   └── UserInfo.java
│   ├── mapper/               # MyBatis Mapper
│   ├── service/              # 业务逻辑层
│   │   ├── impl/             # 服务实现
│   │   │   ├── AiServiceImpl.java
│   │   │   ├── PostServiceImpl.java
│   │   │   └── VideoFfmpegServiceImpl.java
│   │   └── ...
│   └── AiRedbookApplication.java
└── src/main/resources/
    ├── application.yml       # 配置文件
    └── com/acorner/airedbook/mapper/  # Mapper XML
```
## 🔑 核心业务流程

### AI 智能发帖流程

```

上传图片 → OSS 存储 → 创建 AI 任务 → 
AI 生成文案 → 保存到草稿箱 → 用户确认 → 
发布为图文/视频帖子
```
### 草稿箱工作流

```

状态 0: 排队中 → 状态 1: 处理中 → 状态 2: 文案完成 → 
[可选] 修改文案 → [可选] 生成视频 → 状态 4: 完成
```
## ⚙️ 配置项说明

### 文件上传限制

- 单文件最大：20MB
- 请求总大小：100MB

### 数据库表

- `ai_task` - AI 任务表
- `post` - 帖子表
- `post_like` - 点赞记录表
- `user_info` - 用户信息表

## 📝 开发指南

### 添加新的 AI 模型

修改 `application.yml` 中的模型配置：

```
yaml
aliyun:
  bailian:
    model: qwen-vl-max  # 切换为更强大的视觉模型
```
### 自定义视频合成参数

编辑 `VideoFfmpegServiceImpl.java` 调整 FFmpeg 参数。

## 🐛 常见问题

### 1. 数据库连接失败

检查 MySQL 是否启动，用户名密码是否正确。

### 2. Redis 连接失败

确保 Redis 服务运行正常，检查端口和密码配置。

### 3. AI 接口调用失败

- 检查阿里云 API Key 是否有效
- 确认网络可访问 `dashscope.aliyuncs.com`
- 查看账户余额是否充足

## 📄 开源协议

MIT License

## 👥 贡献指南

欢迎提交 Issue 和 Pull Request！

## 📧 联系方式

如有问题或建议，请通过 GitHub Issues 联系。

---

6. ✅ 核心业务流程
7. ✅ 常见问题解答
8. ✅ 安全提醒

你可以直接使用这个 README.md 文件，根据实际情况修改其中的 GitHub 地址、联系方式等信息。
