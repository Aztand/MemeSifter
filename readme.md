# 🧹 Meme-Sifter (表情包筛选器)

> **"不记得当年存的Meme放哪了？现在终于可以整理出来了！"**

Meme-Sifter 是一款基于 Android 的本地工具应用，旨在解决手机相册杂乱，找不到想要的Meme的问题。通过集成本地 OCR（文字识别）技术，它能自动从你的相册中“挖掘”出包含文字的图片，并协助你一键将它们归档到独立文件夹。

---

## ✨ 核心功能

- **🚀 智能全量扫描**
    - 高效遍历手机存储中的所有图片，自动跳过已整理的 `Pictures/Memes` 文件夹，避免重复劳动。

- **🧠 本地 AI 识别 (OCR)**
    - 内置 Google ML Kit (Chinese V2) 引擎，完全离线运行。
    - 自动识别图片中的中文/英文文本，精准定位表情包和截图。
    - **隐私安全**：所有识别过程均在本地完成，图片绝不上传服务器。

- **✅ 高效筛选工作流**
    - **只看有用的**：自动过滤纯风景、人像等无文字图片，只展示“嫌疑对象”。
    - **人工把关**：提供清晰的网格视图，点击即可 选中/取消选中，误判图片一秒剔除。
    - **批量操作**：支持一键处理选中的多张图片。

- **📂 物理归档**
    - 将选中的图片物理移动至系统相册的 `Memes` 分类（`/Pictures/Memes`）。
    - 完美适配 Android 10/11+ 的 Scoped Storage（分区存储）规范，合规申请文件修改权限。

---

## 🛠️ 技术栈

本项目完全使用现代 Android 开发技术构建：

- **语言**: 100% [Kotlin](https://kotlinlang.org/)
- **UI 框架**: [Jetpack Compose](https://developer.android.com/jetbrains/compose) (Material 3 Design)
- **架构**: MVVM (Model-View-ViewModel) + Repository Pattern
- **异步处理**: [Kotlin Coroutines](https://kotlinlang.org/docs/coroutines-overview.html) & Flow
- **AI 引擎**: [Google ML Kit](https://developers.google.com/ml-kit) (On-device Text Recognition)
- **图片加载**: [Coil](https://coil-kt.github.io/coil/)
- **权限管理**: Activity Result API + IntentSender (适配 Scoped Storage)

---

## 📱 食用指南

1. **授予权限**：首次启动需授予存储访问权限（Android 13+ 为读取媒体图片权限）。
2. **等待挖掘**：APP 会自动开始扫描并识别全机图片（速度取决于图片数量和手机性能）。
3. **开始筛选**：
    - 界面上会显示所有**包含文字**的图片。
    - 点击图片进行 **勾选**（再次点击取消）。
4. **一键归档**：
    - 点击右下角的 **"移动 X 张 Meme 图"** 按钮。
    - 如果是 Android 10+ 系统，会弹出系统级确认框，点击“允许”即可。
    - 归档成功的图片会自动从列表中消失。

---

## 📦 安装与构建

### 预备环境
- Android Studio Hedgehog 或更新版本
- JDK 17
- Android SDK API 33+

### 构建步骤
1. 克隆仓库：
   ```bash
   git clone [https://github.com/Aztand/MemeSifter.git](https://github.com/Aztand/MemeSifter.git)
