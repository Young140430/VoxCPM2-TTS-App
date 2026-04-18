# VoxCPM2 TTS - Android 语音合成 App

基于 OpenAI 兼容的 `/v1/audio/speech` 接口的 Android 语音合成应用，支持文本转语音和参考音色克隆。

## 功能

- **文本转语音**：输入文字，一键生成语音（推荐）
- **音色克隆**：上传参考音频或录制音频，克隆音色生成语音（生成的音频可能会有乱码）
- **多格式输出**：支持 WAV / MP3 / FLAC / OGG 格式
- **允许 HTTP**：支持明文 HTTP 连接，适用于内网/自部署服务器
- **即时播放**：生成完成后自动播放，支持重复播放和保存

## 前置要求

- Android 8.0 (API 26) 及以上
- 可访问的 VoxCPM2 vllm_omni API 地址

## 下载安装

直接下载 [VoxCPM2-TTS.apk](./VoxCPM2-TTS.apk) 安装即可。

或使用 Android Studio 打开项目目录自行编译安装。

## 使用

### 1. 配置 API 地址

打开 App → 点击右上角齿轮图标进入设置页：

- **API 地址**：填写 vllm_omni 服务地址，如 `http://192.168.1.100:8000`
- **API Key**：如果服务器需要认证，填写 API Key；否则保持默认
- **模型名称**：默认 `voxcpm2`
- **输出格式**：WAV / MP3 / FLAC / OGG

### 2. 文本转语音

在主页输入框中输入文字 → 点击「生成语音」→ 自动播放

### 3. 音色克隆

- **上传参考音频**：点击「上传音频」选择本地音频文件
- **录制参考音频**：点击「录制音频」现场录制参考音频
- 上传或录制后，点击「生成语音」即可使用参考音色合成

### 4. 保存结果

生成后点击保存图标，音频文件保存到 `/Android/data/com.voxcpm.tts/files/Music/VoxCPM2/`

## 服务器部署

如需自行部署 vllm_omni 服务器：

```bash
python -m vllm_omni.entrypoints.openai.api_server \
    --model openbmb/VoxCPM2 \
    --stage-configs-path vllm_omni/model_executor/stage_configs/voxcpm2.yaml \
    --host 0.0.0.0 --port 8000
```

使用 curl 测试接口：

```bash
curl -X POST http://localhost:8000/v1/audio/speech \
  -H "Content-Type: application/json" \
  -d '{"model": "voxcpm2", "input": "Hello, this is VoxCPM2.", "voice": "default"}' \
  --output output.wav
```

## 项目结构

```
app/
├── VoxCPM2-TTS.apk                 # 预编译 APK 安装包
├── build.gradle                        # 项目构建配置
├── settings.gradle                     # 项目设置
├── gradle.properties                   # Gradle 属性
├── gradlew / gradlew.bat              # Gradle Wrapper
├── gradle/wrapper/                     # Wrapper 配置
└── app/
    ├── build.gradle                    # 模块构建配置（OkHttp + Gson）
    ├── proguard-rules.pro              # 混淆规则
    └── src/main/
        ├── AndroidManifest.xml         # 清单文件
        ├── java/com/voxcpm/tts/
        │   ├── AppApplication.java     # Application 类
        │   ├── Constants.java          # 常量定义
        │   ├── TTSApi.java             # /v1/audio/speech API 客户端
        │   ├── AudioPlayer.java        # 音频播放器
        │   ├── MainActivity.java       # 主界面
        │   └── SettingsActivity.java   # 设置界面
        └── res/                        # 资源文件（布局、图标、样式等）
```

## 注意事项

1. **参考音频兼容性**：通过上传参考音频或录制音频进行音色克隆时，生成的音频可能会出现乱码，建议优先使用纯文本转语音功能。
2. **API 获取**：需要 API 的用户可加入 QQ 群 **321776831** 并联系群主获取。

## 相关链接

- **GitHub**: https://github.com/OpenBMB/VoxCPM
- **ModelScope**: https://www.modelscope.cn/models/OpenBMB/VoxCPM2
