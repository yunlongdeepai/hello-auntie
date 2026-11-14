# “阿姨你好” Android 工程

本目录是调研报告对应的 Android MVP 骨架，按“**远距只做人体检测 + 跟踪，近距才做人脸识别**”“**只适配 ColorOS 14–17**”实现。

> 已在本机完成 **debug APK 构建**，并通过 `core` 模块单元测试（18 个测试）。构建产物在 `app/build/outputs/apk/debug/app-debug.apk`，仓库内交付副本为 `out/auntie-hello-0.1.0-debug.apk`（含模型）。首次打开 Android Studio 时请把 **Gradle JDK** 指到本仓库自带的 JDK 17（见下方“构建”）。

## 环境

- Android Studio 2026.2.2（已安装于 `D:\Program Files\Android\Android Studio`）
- JDK 17：`D:\DeepSeek Harness\toolchain\jdk17\jdk-17.0.21+9`（已随本仓库准备，Android Studio 的 Gradle JDK 请选它）
- Android SDK 35：`D:\Android\Sdk`（`local.properties` 已配置 sdk.dir）
- 一台 ColorOS 14/15/16/17 的 OPPO / 一加 / 真我手机（真机调试）

> 目标 SDK 暂定 35，是为了匹配当前稳定的 AGP 8.7.3 / Compose 工具链。ColorOS 16/17 可以运行 targetSdk 35 的应用；正式版建议在 AGP/SDK 支持后升到 36/37，并回归 ColorOS 17 的相机、权限、前台服务行为。

## 构建

1. 用 Android Studio 打开 `android/` 目录（不是仓库根目录）。
2. **Settings → Build, Execution, Deployment → Build Tools → Gradle → Gradle JDK** 选择  
   `D:\DeepSeek Harness\toolchain\jdk17\jdk-17.0.21+9`。  
   不要用 Android Studio 自带的 JBR 25：当前 AGP 8.7.3 / Gradle 8.9 不支持 Java 25。
3. 等待 Gradle Sync 完成；首次会从 Google Maven / Maven Central 下载依赖。
4. 连接 ColorOS 手机，开启开发者选项/USB 调试。
5. 运行 `app`。

命令行（工程已带 `gradlew`，Gradle 8.9 会自动下载）：

```powershell
cd android
$env:JAVA_HOME = "D:\DeepSeek Harness\toolchain\jdk17\jdk-17.0.21+9"
$env:GRADLE_USER_HOME = "D:\DeepSeek Harness\.gradle-home"
.\gradlew.bat :app:assembleDebug
.\gradlew.bat :core:test
```

`gradle-wrapper.properties` 的 distributionUrl 指向腾讯云 Gradle 镜像，便于国内网络下载。

## 已实现

| 模块 | 状态 |
| --- | --- |
| 远/近两档状态机（65% 进入、55% 退回） | 已实现，带单元测试 |
| ByteTrack 风格两阶段关联 + 轨迹保持 | 已实现，带单元测试 |
| 亲属关系图 / BFS 路径 / 称呼词典 / 方言变体 | 已实现，带单元测试 |
| 关系推理（共同父母 → 兄弟姐妹，带置信度） | 已实现 |
| 非血缘常规称呼（大爷/大妈/叔叔/阿姨） | 已实现，不建立亲属边 |
| 开集人脸匹配（top-1 + margin + 拒识） | 已实现，带单元测试 |
| SQLite 本地库（person/relation/face_sample） | 已实现 |
| Compose 相机页、亲戚页、设置页、添加亲戚向导 | 已实现 |
| CameraX 预览 + 图像分析 + YUV→Bitmap | 已实现 |
| 远距人体检测 | 已放入 `person.tflite`（YOLOv8n 640 NCHW，自动解码） |
| 近距人脸检测 | 系统 `android.media.FaceDetector` 兜底；`face.tflite` 适配待补 |
| 近距人脸特征 | 已放入 `mobileface.tflite`（112×112，192 维） |
| 录入时采集人脸样本 | 已实现：相机页“录入当前人脸” → 选择亲戚 → 存 embedding |
| 数据库加密 / SQLCipher | 未实现（MVP 使用普通 SQLite，正式版必须补） |

## 目录

```
app/
  src/main/java/com/helloauntie/app/
    ui/            Compose 界面
    camera/        相机相关（在 ui/CameraScreen.kt 内）
    domain/        AnalysisEngine：检测→跟踪→远近→识别
    ml/            TFLite / 系统 FaceDetector / 演示检测器
    data/          SQLite + 仓库
    vm/            ViewModel
core/              纯 Kotlin 逻辑（无 Android 依赖）
  kinship/         关系图、BFS、称呼词典、关系预设
  track/           ByteTracker
  state/           FarNearController
  face/            FaceMatcher、QualityGate
  model/           Person/Relation/Box/Detection
```

## 模型文件

当前工作区已放入一版测试权重：

```
app/src/main/assets/models/person.tflite
app/src/main/assets/models/mobileface.tflite
```

- `person.tflite`：YOLOv8n，640 NCHW，输出 `[1,84,8400]`；代码自动识别输入尺寸/布局
- `mobileface.tflite`：MobileFaceNet，112×112 NHWC，输出 192 维

当前两份权重已随仓库分发（约 18 MB）；替换模型或查看许可证/日志说明见 `MODELS.md` 与 `MODEL-LICENSES.md`。

## 测试

纯 Kotlin 逻辑的单元测试：

```powershell
cd android
$env:JAVA_HOME = "D:\DeepSeek Harness\toolchain\jdk17\jdk-17.0.21+9"
.\gradlew.bat :core:test
```

测试覆盖：

- `KinshipEngineTest`：爸爸/伯父/伯母/堂弟/姨妈/表姐/外婆/排行/未知关系
- `RelationPresetsTest`：堂/表兄弟姐妹、非血缘称呼预设与关系链方向
- `ByteTrackerTest`：跨帧 ID 保持、短暂丢失后恢复、低分检测保活
- `FarNearControllerTest`：65/55 滞回
- `FaceMatcherTest`：清晰命中、模糊拒绝、未知拒绝

## 关键设计说明

- **远距不加载人脸模型**：`AnalysisEngine` 只有在 `FarNearMode.NEAR` 才调用 `FaceDetector`/`FaceEmbedder`；FAR 只跑人体检测和 ByteTracker。
- **65% / 55% 不是通用常数**：它是 1080p、60° 水平视场下的经验值（约 4.0 m / 4.7 m）。换焦段、分辨率、屏幕比例必须重新标定。
- **称呼不是字符串**：数据库存关系边，称呼由 `KinshipEngine` 从“我”到目标做 BFS 实时计算；同一人对不同使用者有不同称呼。
- **宁可拒识，不要误认**：`FaceMatcher` 需要 top-1 超过阈值且与 top-2 有 margin；不满足就返回“未知/候选”。

## 下一步

1. 已完成 debug 构建与 18 个单测；下一步在 ColorOS 真机做模型识别与性能验证。
2. 接入 `person.tflite`（YOLOv8/YOLO11 person）并真机测 10–20 m 召回。
3. 接入 SCRFD 或 MediaPipe Face Detector，替换系统 FaceDetector。
4. 用真实 `mobileface.tflite` 验证“录入 → embedding → 识别”闭环，并补多张样本/质量筛选。
5. 补 SQLCipher、同意记录、PIPIA 模板、模型许可证说明。
6. 按 ColorOS 14/15/16/17 各 1–2 台做真机矩阵。
