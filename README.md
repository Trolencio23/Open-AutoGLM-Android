# Open-AutoGLM-Android

基于 [Open-AutoGLM](https://github.com/zai-org/Open-AutoGLM) 的 Android 原生客户端，通过 Shizuku 实现完全本地化的 AI 手机操控。

## 特性

- 📱 **纯手机端运行** - 无需电脑连接 ADB，通过 Shizuku 直接获取 shell 权限
- 🎨 **Material Design 3** - 现代化 UI，支持动态取色 (Material You)
- 🤖 **自定义 AI API** - 支持智谱 BigModel、ModelScope 等 OpenAI 兼容接口
- ⚡ **流式响应** - 实时显示 AI 思考过程
- 🔒 **敏感操作确认** - 支付、删除等操作需用户确认
- 🛠️ **人工接管** - 登录、验证码等场景支持人工介入

## 系统要求

- Android 8.0+ (API 26+)
- [Shizuku](https://shizuku.rikka.app/) 已安装并运行
- (可选) [ADB Keyboard](https://github.com/senzhk/ADBKeyBoard) 用于中文输入

## 安装

1. 下载并安装 [Shizuku](https://shizuku.rikka.app/)
2. 启动 Shizuku 并授权
3. 安装本应用
4. 在设置页配置 API

## 配置 API

### 智谱 BigModel (推荐)

- **API 地址**: `https://open.bigmodel.cn/api/paas/v4`
- **模型名称**: `autoglm-phone`
- **API Key**: 在 [智谱开放平台](https://bigmodel.cn/) 申请

### ModelScope

- **API 地址**: `https://api-inference.modelscope.cn/v1`
- **模型名称**: `ZhipuAI/AutoGLM-Phone-9B`
- **API Key**: 在 [ModelScope](https://modelscope.cn/) 申请

## 使用方法

1. 确保 Shizuku 运行且已授权
2. 在设置页配置 API
3. 在主页输入任务，例如：
   - "打开微信搜索附近美食"
   - "打开淘宝搜索无线耳机"
   - "打开设置调整屏幕亮度"
4. 点击开始执行

## 项目结构

```
app/src/main/java/com/autoglm/android/
├── action/           # Action 解析与处理
│   ├── ActionParser.kt
│   └── ActionHandler.kt
├── agent/            # Agent 核心逻辑
│   └── PhoneAgent.kt
├── config/           # 配置
│   ├── AppPackages.kt
│   ├── SystemPrompts.kt
│   └── TimingConfig.kt
├── data/             # 数据持久化
│   └── SettingsRepository.kt
├── device/           # 设备控制
│   ├── DeviceController.kt
│   ├── ScreenshotService.kt
│   ├── AppLauncher.kt
│   ├── InputService.kt
│   └── CurrentAppDetector.kt
├── model/            # AI 模型客户端
│   ├── ModelConfig.kt
│   ├── ModelClient.kt
│   └── MessageBuilder.kt
├── service/          # 后台服务
│   └── AgentForegroundService.kt
├── shizuku/          # Shizuku 集成
│   ├── ShizukuManager.kt
│   └── ShizukuExecutor.kt
└── ui/               # UI 界面
    ├── AutoGLMApp.kt
    ├── theme/Theme.kt
    ├── home/
    └── settings/
```

## 支持的操作

| 操作 | 说明 |
|------|------|
| Launch | 启动应用 |
| Tap | 点击 |
| Type | 输入文本 |
| Swipe | 滑动 |
| Back | 返回 |
| Home | 回到桌面 |
| Long Press | 长按 |
| Double Tap | 双击 |
| Wait | 等待 |
| Take_over | 请求人工接管 |

## 构建

```bash
./gradlew assembleDebug
```

## 许可证

本项目基于 Open-AutoGLM，仅供学习研究使用。

## 致谢

- [Open-AutoGLM](https://github.com/zai-org/Open-AutoGLM)
- [Shizuku](https://github.com/RikkaApps/Shizuku)
