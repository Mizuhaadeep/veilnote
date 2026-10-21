# Veilnote · 雾笺 v1.0.1

> 纯本地 Block 笔记。图文、文件、定位混排，单篇笔记独立上锁。
>
> **Made with DeepSeek**

## 下载

| 文件 | 用途 |
| --- | --- |
| `Veilnote-1.0.1-release.apk` | 直接安装到 Android 8.0+ 设备 |
| `Veilnote-1.0.1-release.aab` | 应用商店 / Play Console 上传 |

- versionCode: `2`
- versionName: `1.0.1`
- package: `com.glassnote`
- APK SHA-256: `F67A1A3F49BF8BB73EBDDB3BABDD178ACBA6CFE36469A60A99B91067DAEF0F05`
- AAB SHA-256: `103E70C6C661F32ACDA2AE4AC87F2938D9222FAA1E1A2E47E81104511267AD75`

APK 使用正式 RSA 4096 keystore 签名，APK Signature Scheme v2/v3 校验通过。

## 本版修复

- 底部实时字数统计固定在 LazyColumn 外，不再随列表滚动消失
- 阅读模式点击文字进入编辑时，记录被点击的 Block 与字符 offset
- 移除了焦点请求后的 `animateScrollToItem` 强制滚动，页面不再自动跳到底部
- 进入编辑态时光标落在点击位置，而不是文本末尾

## 核心功能

- Block 文档：文本 / 图片 / 文件 / 定位混排
- 单篇笔记独立密码锁，数据库只保存 SHA-256 哈希
- Room 数据库版本 4，旧 `content` 自动迁移为 `TextBlock`，历史数据不丢失
- 本地优先：无账号、无云同步、无 INTERNET 权限
- 可选 SAF 公共目录与 `.veil` JSON 导出
- 全量 / 极简模式、深浅色、玻璃视觉与自适应图标

## 安全说明

当前「加密」是访问锁：密码哈希用于校验与隐藏 UI，正文仍以原文保存在本地 Room / `.veil` 中，不是 AES 静态密文加密。需要真正密文存储时请关注后续版本。

## 安装

1. 下载 `Veilnote-1.0.1-release.apk`
2. 允许安装未知来源应用
3. 安装并打开 Veilnote
4. 首次启动选择存储方式

## 开源

- Repository: https://github.com/Mizuhaadeep/veilnote
- License: MIT
- Issues: https://github.com/Mizuhaadeep/veilnote/issues

---

**Made with DeepSeek**
