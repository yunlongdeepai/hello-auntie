# 首次构建 / 真机运行指南

## 1. 环境（已安装）

这台机器已经安装好：

- Android Studio：`D:\Program Files\Android\Android Studio`
- Android SDK：`D:\Android\Sdk`（`local.properties` 已写入）
- JDK 17：`D:\DeepSeek Harness\toolchain\jdk17\jdk-17.0.21+9`
- Gradle 8.9：`D:\DeepSeek Harness\toolchain\gradle-8.9`，工程也已生成 `gradlew`

> 关键：Android Studio 2026 自带的是 JBR 25，而当前 AGP 8.7.3 / Gradle 8.9 不支持 Java 25。**Gradle JDK 必须手动选 JDK 17**，见第 3 步。

## 2. SDK（已安装）

已安装：

- Android SDK Platform 35（`compileSdk = 35`）
- Android SDK Build-Tools 36.0.0（工程已显式指定，兼容 compileSdk 35）
- Platform-Tools / Emulator

如果换机器，用 SDK Manager 安装 **Android 15.0 (API 35)** 和 **Build-Tools 36.0.0**。

## 3. 打开工程

用 Android Studio 打开：

```
D:\DeepSeek Harness\hello-auntie\android
```

注意：打开的是 `android/` 子目录，不是仓库根目录 `hello-auntie/`。

首次打开会触发 Gradle Sync，可能需要几分钟下载依赖。**必须先设置 Gradle JDK**：

- **Settings → Build, Execution, Deployment → Build Tools → Gradle**
- Gradle JDK 选择 **Add JDK…**，路径填：  
  `D:\DeepSeek Harness\toolchain\jdk17\jdk-17.0.21+9`
- 不要用 Android Studio 自带的 JBR 25；不要把 Gradle JDK 留成默认值。
- 设置完成后点 Sync。`gradle-wrapper.properties` 已指向腾讯云 Gradle 8.9 镜像。

## 4. 模型（已随仓库包含）

仓库已包含两份测试权重：

```
android/app/src/main/assets/models/person.tflite
android/app/src/main/assets/models/mobileface.tflite
```

APK 会直接打包这两个文件；设置页应显示“已就绪”。替换模型或查看来源/许可证见 [MODELS.md](MODELS.md) 与 [MODEL-LICENSES.md](MODEL-LICENSES.md)。

如果删除模型，App 仍能启动：远距使用演示框，近距用系统 FaceDetector 找人脸，身份显示“未知”。

## 5. 连接 ColorOS 手机

1. 手机：**设置 → 关于本机 → 版本信息**，连续点击“版本号”7 次，开启开发者选项。
2. **设置 → 系统设置/其他设置 → 开发者选项**，打开 **USB 调试**。
3. 部分 ColorOS 还需要打开 **“USB 调试（安全设置）”**，或关闭“权限监控”。
4. USB 连接电脑，手机弹窗选择“允许 USB 调试”。
5. Android Studio 顶部设备列表应出现手机；没有的话安装 Platform-Tools 后执行 `adb devices` 检查。

## 6. 运行

1. 选择 `app` 配置，点击 Run。
2. 首次启动允许相机权限。
3. 相机页：
   - 远距：只显示“有人（远距）”，不跑人脸识别。
   - 走近：进入“近距模式”，才检测/识别人脸。
   - “+”：添加亲戚。
   - “录入当前人脸”：需要 `mobileface.tflite`，选择亲戚后保存样本。
4. 亲戚页可查看称呼、关系链、人脸样本数量；设置页可切换地区、填写“我的出生年”。

## 7. 跑单元测试

纯 Kotlin 逻辑测试在 `core` 模块，已全部通过（13 个）：

- Android Studio：Gradle 工具窗口 → `core` → Tasks → verification → test。
- 或命令行：

```powershell
cd android
$env:JAVA_HOME = "D:\DeepSeek Harness\toolchain\jdk17\jdk-17.0.21+9"
$env:GRADLE_USER_HOME = "D:\DeepSeek Harness\.gradle-home"
.\gradlew.bat :core:test
```

## 8. 常见问题

| 现象 | 处理 |
| --- | --- |
| `SDK location not found` | 检查 `local.properties` 是否为 `sdk.dir=D\:\\Android\\Sdk` |
| Gradle Sync 卡住/失败 | 检查网络；Gradle JDK 必须选 `toolchain\jdk17`；File → Invalidate Caches |
| `Unsupported class file major version 69` | 用了 JBR 25，改 Gradle JDK 为 JDK 17 |
| `Could not initialize native services` | 检查 Gradle JDK 权限/临时目录；命令行可设 `GRADLE_USER_HOME` 到可写目录 |
| 相机黑屏 | 确认相机权限、没有被其他 App 占用；ColorOS 电池/后台限制 |
| 没有识别结果 | 没放 `person.tflite`/`mobileface.tflite`，或没录入人脸样本；看设置页模型状态 |
| 录入按钮提示“未安装模型” | 放入 `mobileface.tflite` 后重启 App |
| 安装被拦截 | ColorOS 允许“通过 USB 安装应用”/关闭权限监控 |

## 9. 当前状态

- 已生成 debug APK：`app/build/outputs/apk/debug/app-debug.apk`；可安装包作为 GitHub Release 附件发布，不进入 Git。
- 已通过 `core` 的 18 个单元测试。
- 已在 OnePlus 9 / ColorOS 14 完成基础安装、相机和模型加载验证；ColorOS 15–17 机型矩阵待补。
- 模型权重已随仓库包含（许可证见 `MODELS.md` 与 `MODEL-LICENSES.md`）。
