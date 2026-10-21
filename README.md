<div align="center">

<img src="docs/icon.svg" width="128" height="128" alt="Veilnote icon" />

# Veilnote · 雾笺

**一页会呼吸的本地笔记。**

纯本地 · Block 混排 · 单篇加密 · 玻璃质感

[![Made with DeepSeek](https://img.shields.io/badge/Made%20with-DeepSeek-4D6BFE?style=flat-square)](https://www.deepseek.com/)
[![Android](https://img.shields.io/badge/Android-8.0%2B-3DDC84?style=flat-square&logo=android&logoColor=white)](#)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.1-7F52FF?style=flat-square&logo=kotlin&logoColor=white)](https://kotlinlang.org/)
[![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material%203-4285F4?style=flat-square)](https://developer.android.com/jetpack/compose)
[![License](https://img.shields.io/badge/License-MIT-8A7BFF?style=flat-square)](LICENSE)

[下载 APK](https://github.com/Mizuhaadeep/veilnote/releases/latest/download/Veilnote-1.0.1-release.apk) ·
[最新 Release](https://github.com/Mizuhaadeep/veilnote/releases/latest) ·
[项目主页](https://mizuhaadeep.github.io/veilnote/) ·
[问题反馈](https://github.com/Mizuhaadeep/veilnote/issues)

</div>

---

> **Made with DeepSeek**  
> 本项目的产品设计、代码实现、测试与发布流程由 **DeepSeek** 辅助完成。

## 这是什么

Veilnote（中文名「雾笺」）是一个纯本地的 Android 笔记应用。

它不想做另一个云端知识库，而是把你真正想留下的东西，安静地放在设备上：

- 一段文字、一张图片、一个文件、一个地点，可以自由混排
- 不上传、不登录、不追踪
- 每篇笔记都可以单独上锁
- 玻璃与雾的视觉，尽量不打扰内容本身

## 下载

| 渠道 | 版本 | 文件 |
| --- | --- | --- |
| GitHub Releases | `1.0.1` | [Veilnote-1.0.1-release.apk](https://github.com/Mizuhaadeep/veilnote/releases/latest/download/Veilnote-1.0.1-release.apk) |
| GitHub Releases | `1.0.1` | [Veilnote-1.0.1-release.aab](https://github.com/Mizuhaadeep/veilnote/releases/latest/download/Veilnote-1.0.1-release.aab) |
| GitHub | 最新 | [Releases](https://github.com/Mizuhaadeep/veilnote/releases/latest) |

要求：Android 8.0（API 26）及以上。

APK 已使用正式 keystore 签名，可直接安装。AAB 用于 Play Console / 应用商店上传。

> 只从本仓库 Releases 下载。不要使用来源不明的二次打包版本。

## 功能

### Block 文档

正文不是一段大字符串，而是有序的 Block 列表：

- **文本**：自适应高度，独立焦点
- **图片**：相册选择，复制到应用私有目录，点击全屏预览
- **文件**：保留文件名与大小，复制到私有目录
- **位置**：获取经纬度与逆地理编码地址，点击打开地图

底部工具栏可随时插入图片、文件或定位；插入后会自动生成新的空文本 Block 并聚焦。

### 阅读与编辑分离

- 默认进入阅读模式，页面无输入框、无光标
- 点击文字任意位置进入编辑
- 点击位置通过 `TextLayoutResult.getOffsetForPosition()` 转成字符 offset
- 只聚焦被点击的 Block，不强制滚动列表
- 顶部「完成」或收起键盘回到阅读

### 单篇笔记加密

- 每篇笔记独立密码
- 数据库只保存密码的 SHA-256 哈希，不存明文
- 锁定卡片隐藏标题、正文、字数、相对时间与绝对日期
- 输入正确密码后才允许打开
- 密码弹窗使用完全不透明卡片，不泄漏底层内容

> 当前实现是「访问锁」：密码哈希用于校验，正文仍以原文保存在本地 Room / `.veil` 中。它不是 AES 静态密文加密；如果需要真正的密文存储，需要新增 salt/IV/ciphertext 字段与解密链路。

### 本地优先

- Room 数据库
- DataStore 设置
- 可选 SAF 公共目录，自动导出 `.veil` JSON
- 无账号、无云同步、无 INTERNET 权限
- 位置逆编码失败时回退为经纬度

### 外观

- 全量 / 极简两种应用模式
- 全量模式可独立开关过渡动画、网址识别
- 极简模式强制关闭装饰性转场与网址识别
- 深色 / 浅色 / 跟随系统
- 自定义渐变或背景图
- 全局无 Ripple，弹簧按压反馈
- 自适应图标：玻璃纸页、雾层、折角与星芒

## 技术栈

| 层 | 选择 |
| --- | --- |
| 语言 | Kotlin 2.1.10 |
| UI | Jetpack Compose + Material 3 |
| 架构 | 单 Activity + Navigation Compose + MVVM |
| 数据库 | Room 2.7.0 + KSP |
| 设置 | Preferences DataStore |
| 图片 | Coil 2.7.0 |
| 序列化 | Gson |
| 构建 | Gradle 8.11.1 + AGP 8.9.1 |
| SDK | minSdk 26 / compileSdk 35 / targetSdk 35 |

### 数据模型

```kotlin
sealed interface NoteBlock {
    val id: String

    data class TextBlock(
        override val id: String = UUID.randomUUID().toString(),
        val content: String,
    ) : NoteBlock

    data class ImageBlock(
        override val id: String = UUID.randomUUID().toString(),
        val localPath: String,
    ) : NoteBlock

    data class FileBlock(
        override val id: String,
        val localPath: String,
        val fileName: String,
        val fileSize: String,
    ) : NoteBlock

    data class LocationBlock(
        override val id: String,
        val latitude: Double,
        val longitude: Double,
        val address: String,
    ) : NoteBlock
}
```

Room 使用 `NoteBlockListConverter` 将 blocks 序列化为 JSON。数据库版本 `4`，迁移会把旧的 `content` 自动包装成 `listOf(TextBlock(content = oldContent))`，不丢历史数据。

## 构建

环境：JDK 17、Android SDK Platform 35、Build-Tools 35+。

```bash
git clone https://github.com/Mizuhaadeep/veilnote.git
cd veilnote

./gradlew :app:assembleDebug
./gradlew :app:lintDebug
```

Windows：

```powershell
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :app:lintDebug
```

Debug APK：

```text
app/build/outputs/apk/debug/app-debug.apk
```

## 签名

正式包使用项目维护者持有的 keystore 签名。仓库不包含 keystore、密码或 `release/` 产物。

发布新版本：

```bash
./gradlew :app:assembleRelease :app:bundleRelease
```

签名命令与证书指纹会随 Release 说明提供；不会把私钥上传到 Git。

## 项目结构

```text
app/src/main/kotlin/com/glassnote/
├── data/            Room、SAF、DataStore、Repository
├── domain/model/    Note、NoteBlock、设置模型
├── security/        PasswordHasher
├── ui/
│   ├── editor/      Block 编辑器、焦点与插入逻辑
│   ├── home/        卡片拦截、解锁弹窗、列表
│   ├── settings/    模式、外观、存储
│   └── theme/       玻璃主题与颜色
└── util/            FileUtils、LocationUtils
```

## 已知限制

- 图片/文件使用本地绝对路径，`.veil` 跨设备同步只携带路径，不传输附件二进制。
- 加密为访问锁，正文不是 AES 密文；详见上方说明。
- 位置服务依赖系统 LocationManager；模拟器或无网络环境可能回退为经纬度。
- 当前正式签名由维护者本地保管，仓库不提供私钥。

## 贡献

欢迎 Issue 与 Pull Request。

- 不要提交 keystore、密码、`local.properties`、构建产物或用户笔记。
- 提交前运行 `./gradlew :app:testDebugUnitTest :app:lintDebug`。
- 涉及加密、迁移或附件路径的改动，请在 PR 中说明数据兼容性。

## License

[MIT](LICENSE) © 2026 Mizuhaadeep

---

<div align="center">

**Veilnote · 雾笺**

Made with **DeepSeek**

</div>
