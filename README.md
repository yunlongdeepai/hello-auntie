<p align="center">
  <img src="assets/logo_mark.png" width="120" alt="阿姨你好 logo">
</p>

<h1 align="center">hello auntie · 阿姨你好</h1>

> 一款**完全离线**的 Android 亲属称呼助手：用相机发现亲戚，走近后识别人脸，并结合“我”的家庭关系图给出正确称呼。

![Platform](https://img.shields.io/badge/Android-14%E2%80%9317-3DDC84?logo=android&logoColor=white)
![Kotlin](https://img.shields.io/badge/Kotlin-2.0.21-7F52FF?logo=kotlin&logoColor=white)
![Compose](https://img.shields.io/badge/Jetpack%20Compose-Material3-4285F4?logo=jetpackcompose&logoColor=white)
![minSdk](https://img.shields.io/badge/minSdk-34-blue)
![Status](https://img.shields.io/badge/status-MVP-orange)

---

## 项目简介

过年走亲戚时，面对一年只见一次的远房亲戚，很多人叫不出“伯母、婶婶、姨父、堂弟”这类称呼。

“阿姨你好”尝试用手机端侧 AI 解决这个问题：

- **远距离**：只做人体检测 + 多目标跟踪，不跑人脸识别，保证帧率和隐私。
- **近距离**：人走近后，才启用人脸检测、特征提取和身份匹配。
- **称呼推理**：数据库存亲属关系边，称呼由“我”到目标做 BFS 实时计算，而不是把称呼写死在人身上。
- **完全离线**：不申请网络权限，照片、人脸特征、关系图只存在本机。

当前为 **MVP 阶段**：已完成 debug 构建、18 个单元测试、OnePlus 9 / ColorOS 14 基础验收；ColorOS 15–17 机型矩阵待补。

> 仓库根目录是 `hello-auntie/`；Android 工程在 `hello-auntie/android/`，Android Studio 请打开 `android/` 子目录。

---

## 核心功能

- [x] 远/近两档状态机：人体框高度 ≥ 65% 进入近距，< 55% 退回远距（滞回，避免来回跳变）
- [x] 人体检测 + ByteTracker 风格多目标跟踪
- [x] 亲属关系图：父母、配偶、兄弟姐妹、子女，支持共同父母推断兄弟姐妹
- [x] 称呼词典：伯/叔/姑/舅/姨/堂/表、排行、方言变体
- [x] 关系链录入向导：爸爸的哥哥、妈妈的姐妹、伯母、嫂子、儿媳、堂/表兄弟姐妹等预设
- [x] 非血缘常规称呼：大爷、大妈、叔叔、阿姨（不建立亲属关系，只保存叫法）
- [x] 开集人脸匹配：top-1 阈值 + top-2 margin，未知脸拒绝
- [x] 人脸样本录入：相机页“录入当前人脸”，选择亲戚后保存 embedding
- [x] 本地 SQLite：person / relation / face_sample
- [x] 设置：称呼地区、我的出生年、模型状态、清空数据
- [ ] SQLCipher 数据库加密
- [ ] SCRFD / MediaPipe 人脸检测适配
- [ ] 完整同意记录、PIPIA、未成年人监护人同意流程

---

## 工作流程

```text
CameraX Preview
      │
      ▼
ImageAnalysis (YUV_420_888 → Bitmap)
      │
      ▼
PersonDetector
      │
      ▼
ByteTracker ──► 每个 track 独立维护 FarNearController
      │
      ├── FAR 模式：只画人体框和“有人（远距）”，不调用人脸模型
      │
      └── NEAR 模式：
             FaceDetector
                 │
                 ▼
             QualityGate（人脸尺寸/角度/亮度）
                 │
                 ▼
             FaceEmbedder（MobileFaceNet）
                 │
                 ▼
             FaceMatcher（按人聚合，top-1 + margin）
                 │
                 ▼
             KinshipEngine（BFS 关系路径 → 称呼）
                 │
                 ▼
             “三姨 · 张三 92%”
```

### 为什么远距不做人脸识别？

1080p、60° 水平视场下，10 m 外的人脸只有几十像素，已经低于可用识别下限。远距强行跑人脸模型只会：

- 浪费算力、拉低帧率；
- 把模糊人脸误认成亲戚；
- 增加不必要的隐私风险。

所以远距只做“发现 + 跟踪”，近距才做“识别 + 称呼”。

---

## 技术栈

| 类别 | 选型 |
| --- | --- |
| 语言 | Kotlin 2.0.21 |
| UI | Jetpack Compose + Material 3 |
| 相机 | CameraX 1.4.1（Preview + ImageAnalysis） |
| 人体检测 | LiteRT / TensorFlow Lite 2.16.1，YOLOv8/YOLO11 person（可选模型） |
| 人脸检测 | 系统 `android.media.FaceDetector` 兜底；SCRFD 适配待补 |
| 人脸特征 | MobileFaceNet，112×112 输入，512/128 维输出 |
| 跟踪 | 自研 ByteTracker 风格两阶段 IoU 关联 |
| 关系推理 | 自研关系图 + BFS + 称呼词典 |
| 本地存储 | SQLite（MVP），后续 SQLCipher |
| 构建 | AGP 8.7.3 / Gradle 8.9 / JDK 17 |
| 平台 | Android 14–17（API 34–37），优先 ColorOS 14–17 |

### SDK 版本

```text
compileSdk = 35
targetSdk  = 35
minSdk     = 34
```

> 目标 SDK 暂定 35，是为了匹配当前稳定的 AGP 8.7.3 / Compose 工具链。ColorOS 16/17 可以运行 targetSdk 35 的应用；正式版建议后续升到 36/37，并回归 ColorOS 17 的相机、权限、前台服务行为。

---

## 工程结构

```text
hello-auntie/
├── android/                         # Android Gradle 工程（Android Studio 打开这里）
│   ├── app/
│   │   └── src/main/
│   │       ├── java/com/helloauntie/app/
│   │       │   ├── ui/              # Compose：相机页、亲戚页、设置页、添加向导
│   │       │   ├── domain/          # AnalysisEngine：检测→跟踪→远近→识别
│   │       │   ├── ml/              # TFLite / 系统 FaceDetector / 演示检测器
│   │       │   ├── data/            # SQLite + Repository
│   │       │   └── vm/              # ViewModel
│   │       ├── assets/models/       # person.tflite / mobileface.tflite（随仓库分发）
│   │       └── res/
│   ├── core/                        # 纯 Kotlin 逻辑，无 Android 依赖
│   │   └── src/main/kotlin/com/helloauntie/core/
│   │       ├── kinship/             # 关系图、BFS、称呼词典、关系预设
│   │       ├── track/               # ByteTracker
│   │       ├── state/               # FarNearController
│   │       ├── face/                # FaceMatcher、QualityGate
│   │       └── model/               # Person、Relation、Box、Detection
│   ├── gradlew / gradlew.bat
│   └── settings.gradle.kts
├── out/                             # 调研报告 DOCX/PDF + debug APK
├── src/                             # 报告 Markdown 源文件与生成脚本
├── assets/                          # 报告封面/示意图
├── .gitignore
└── README.md
```

### 关键类

| 文件 | 作用 |
| --- | --- |
| `core/kinship/KinshipGraph.kt` | 邻接表、BFS 最短关系路径 |
| `core/kinship/TermBook.kt` | 关系 token → 称呼、排行、方言变体 |
| `core/kinship/KinshipInference.kt` | 共同父母 → 兄弟姐妹推断 |
| `core/track/ByteTracker.kt` | 两阶段关联、轨迹保持、短暂丢失恢复 |
| `core/state/FarNearController.kt` | 65%/55% 滞回状态机 |
| `core/face/FaceMatcher.kt` | 按人聚合的开集人脸匹配 |
| `app/domain/AnalysisEngine.kt` | 单帧分析总编排 |
| `app/ui/CameraScreen.kt` | CameraX 预览、叠加框、录入入口 |

---

## 环境要求

- **Android Studio** 2026.2.2 或更新版本
- **JDK 17**（AGP 8.7.3 / Gradle 8.9 需要；不要用 Android Studio 自带的 JBR 25 直接跑 Gradle）
- **Android SDK 35**，Build-Tools 36.0.0
- 一台 **ColorOS 14/15/16/17** 的 OPPO / 一加 / 真我手机（真机调试）
- 可选：Android Emulator（用于 UI 冒烟测试，相机能力有限）

> 本机已验证环境：Android Studio 2026.2.2 + JDK 17 + SDK 35 + Build-Tools 36.0.0。

完整步骤（含 ColorOS USB 调试、Gradle JDK、常见报错）见 [`android/BUILD.md`](android/BUILD.md)。

---

## 快速开始

### 1. 克隆

```bash
git clone git@github.com:yunlongdeepai/hello-auntie.git
cd hello-auntie
```

Gitee 镜像：

```bash
git clone git@gitee.com:chenyunlong_ucas/hello-auntie.git
```

### 2. 用 Android Studio 打开

```text
File → Open → hello-auntie/android
```

注意：打开 `android/`，不是仓库根目录。

### 3. 配置 Gradle JDK

```text
Settings
  → Build, Execution, Deployment
  → Build Tools
  → Gradle
  → Gradle JDK
```

选择 **JDK 17**。如果本机没有，Android Studio 可以下载 Temurin/Corretto 17。

### 4. 配置 SDK

`android/local.properties` 不提交到仓库，首次打开时 Android Studio 会自动生成；也可以手动写：

```properties
sdk.dir=D\:\\Android\\Sdk
```

### 5. 构建

```bash
cd android
./gradlew :app:assembleDebug
```

Windows：

```powershell
cd android
.\gradlew.bat :app:assembleDebug
```

产物：

```text
android/app/build/outputs/apk/debug/app-debug.apk
```

已构建的 debug APK（含模型）也在仓库中：

[`out/auntie-hello-0.1.0-debug.apk`](out/auntie-hello-0.1.0-debug.apk)（约 61 MB）

校验和：[`out/SHA256SUMS.txt`](out/SHA256SUMS.txt)

### 6. 安装到手机

```bash
adb install -r out/auntie-hello-0.1.0-debug.apk
```

首次启动允许相机权限；无需网络权限。

---

## 模型文件

当前仓库已随代码分发一版用于 MVP 验证的 float32 权重（约 18 MB，已取消 `.gitignore` 排除）：

```text
android/app/src/main/assets/models/person.tflite
android/app/src/main/assets/models/mobileface.tflite
```

### `person.tflite`

- 当前权重：YOLOv8n，输入 `[1, 3, 640, 640]` NCHW，输出 `[1, 84, 8400]`
- 代码会自动识别 320/640 输入、NHWC/NCHW 布局，并按 class 0（person）解码 + NMS
- 替换为 320 NHWC 或单类 5 通道模型也可以，解码器已兼容

### `mobileface.tflite`

- 当前权重：MobileFaceNet，输入 `[1, 112, 112, 3]` NHWC，输出 `[1, 192]`
- 预处理：`(pixel - 128) / 128`；代码使用等价的 `(pixel / 127.5) - 1`
- 输出 128/192/512 维均可，代码自动读取并做 L2 归一化

### 没有模型时

App 仍可启动：

- 远距：显示演示目标框；
- 近距：系统 `FaceDetector` 找人脸，身份显示“未知”；
- “录入当前人脸”：提示“未安装 mobileface.tflite”。

模型格式、来源、许可证和日志检查见 [`android/MODELS.md`](android/MODELS.md) 与 [`android/MODEL-LICENSES.md`](android/MODEL-LICENSES.md)。

---

## 测试

纯 Kotlin 逻辑的单元测试：

```bash
cd android
./gradlew :core:test
```

Windows：

```powershell
cd android
.\gradlew.bat :core:test
```

覆盖 18 个用例：

| 测试类 | 覆盖内容 |
| --- | --- |
| `KinshipEngineTest` | 爸爸/伯父/伯母/堂弟/姨妈/表姐/外婆/排行/非亲属称呼/未知关系 |
| `RelationPresetsTest` | 堂/表兄弟姐妹、非血缘称呼预设与关系链方向 |
| `ByteTrackerTest` | 跨帧 ID 保持、短暂丢失后恢复、低分检测保活 |
| `FarNearControllerTest` | 65/55 滞回 |
| `FaceMatcherTest` | 清晰命中、同人多模板、模糊拒绝、未知拒绝 |

当前状态：**18/18 通过**。

> 单元测试不等于真机验证；相机、远近切换、人脸录入和识别仍需 ColorOS 真机走查。

---

## 已实现 / 待完成

| 模块 | 状态 |
| --- | --- |
| 远/近两档状态机 | ✅ 已实现，带单测 |
| ByteTracker 风格跟踪 | ✅ 已实现，带单测 |
| 关系图 / BFS / 称呼词典 | ✅ 已实现，带单测 |
| 关系推理 | ✅ 共同父母 → 兄弟姐妹，带置信度 |
| 开集人脸匹配 | ✅ 已实现，带单测 |
| CameraX 预览 + 图像分析 | ✅ 已实现 |
| 人脸样本录入 / 计数 | ✅ 已实现，需 `mobileface.tflite` |
| SQLite 本地库 | ✅ 已实现 |
| SQLCipher 加密 | ❌ 未实现 |
| SCRFD 人脸检测 | ❌ 未实现，当前用系统 FaceDetector |
| 真实模型权重 | ✅ 已随仓库分发（约 18 MB） |
| 同意记录 / PIPIA / 未成年人流程 | ❌ 未实现 |
| ColorOS 真机矩阵 | ❌ 未完成 |

---

## 隐私与合规

产品设计原则：

- **端侧处理**：不申请 `INTERNET` 权限，图像不上传。
- **本地存储**：关系图、人脸 embedding 存本机 SQLite。
- **禁止备份**：`android:allowBackup="false"`。
- **不出原图**：当前只存人脸 embedding，不存完整相机帧或缩略图。
- **可删除**：支持删除单个亲戚/清空全部数据。

正式上线前仍需补：

1. 首次使用单独同意（不能和普通用户协议捆在一起）；
2. 未成年人（<14 岁）监护人同意；
3. 人脸识别个人信息保护影响评估（PIPIA）；
4. SQLCipher 加密与密钥管理；
5. 删除、导出、撤回同意的完整闭环。

> 当前仓库是工程 MVP，不构成可直接上架的合规版本。

---

## 性能与适配说明

- 65% / 55% 不是通用常数：它是 1080p、60° 水平视场下的经验值（约 4.0 m / 4.7 m）。换焦段、分辨率、屏幕比例需要重新标定。
- 远距目标：人体检测 + 跟踪 25–30 FPS；近距：人脸检测 + 识别 15–20 FPS（目标机型实测为准）。
- 仅在 **ColorOS 14–17** 上做适配与验证；其他 Android 系统暂不适配。
- 16 KB 页对齐：debug APK 已通过 `zipalign -c -P 16` 检查；TFLite 版本升级后需重新检查。
- 目前移除未使用的 `tensorflow-lite-gpu` 依赖；APK 约 61 MB（其中模型约 18 MB）。

---

## 调研报告

本仓库同时保留立项调研报告，记录选型、光学测算、许可证和合规分析：

- [`out/阿姨你好_技术调研报告_v1.0.pdf`](out/阿姨你好_技术调研报告_v1.0.pdf)
- [`out/阿姨你好_技术调研报告_v1.0.docx`](out/阿姨你好_技术调研报告_v1.0.docx)
- 报告源文件：[`src/part1.md`](src/part1.md) ~ [`src/part4.md`](src/part4.md)
- 重建脚本：[`src/build_docx.py`](src/build_docx.py)、[`src/make_figures.py`](src/make_figures.py)

报告中的数据与结论为工程估算，不能替代目标机型实测和法律意见。

---

## 路线图

- [ ] ColorOS 14/15/16/17 真机安装与相机走查
- [ ] 接入 `person.tflite`，测 10–20 m 人体召回
- [ ] 接入 `mobileface.tflite`，跑通“录入 → embedding → 识别”
- [ ] SCRFD / MediaPipe Face Detector 替换系统检测
- [ ] 多张样本、质量筛选、重复样本去重
- [ ] SQLCipher + 同意记录 + PIPIA
- [ ] Release 签名与版本发布
- [ ] 6–8 台 ColorOS 机型性能矩阵

---

## 品牌资源

<p align="center">
  <img src="assets/logo_wordmark.png" width="620" alt="阿姨你好 横版组合标">
</p>

- 圆形徽章：[`assets/logo_mark.png`](assets/logo_mark.png)
- 横版组合标：[`assets/logo_wordmark.png`](assets/logo_wordmark.png)
- 设计说明、色板与使用规范：[`assets/logo-notes.md`](assets/logo-notes.md)
- 生成脚本：[`tools/make_logo.py`](tools/make_logo.py)

Logo 为原创文字标识：`hello` 细字重 + `auntie` 粗字重 + 金色句点，不使用人物插画或徽章，也没有复刻任何现有品牌的图形、字体或商标组合。

## 贡献

当前项目处于 MVP 阶段。提交代码前请先开 issue 说明改动范围；新增模型或第三方素材时，请同时补充来源和许可证说明。模型权重已随仓库分发，更新权重时不要提交真实人脸数据或本机 `local.properties`。

## 许可证

当前仓库暂未声明开源许可证，代码与文档版权归作者所有。

- 当前仓库随代码分发两份测试模型：`person.tflite`（YOLOv8n，**AGPL-3.0**）与 `mobileface.tflite`（MIT 仓库版本）。对外分发/商用前必须确认对应许可证；来源见 [`android/MODEL-LICENSES.md`](android/MODEL-LICENSES.md)。
- 调研报告引用的第三方资料遵循原出处许可。
- 如需开源，建议在正式公开前补充 `LICENSE`（如 MIT / Apache-2.0）并确认模型许可证。

## 致谢

- AndroidX、CameraX、Jetpack Compose
- TensorFlow Lite / LiteRT
- `relationship.js` 的称呼路径思路
- ByteTrack 的跟踪关联思路
