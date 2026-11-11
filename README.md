# “阿姨你好”安卓端离线亲属称呼助手 · 技术调研报告

> 本版按最新约束修订：**远距离只做目标检测与跟踪，不做人脸识别；近距离才做人脸识别**；平台**只适配 ColorOS 14–17**，其他 Android 系统暂不适配。

> **工程状态**：Android MVP 已完成本机 debug 构建（`android/app/build/outputs/apk/debug/app-debug.apk`），`core` 13 个单元测试全部通过。代码与构建说明见 [`android/README.md`](android/README.md)。

## 交付物

| 文件 | 说明 |
| --- | --- |
| `out/阿姨你好_技术调研报告_v1.0.docx` | 可编辑 Word 版（约 58 页，含封面、目录、58 张表与 3 张示意图） |
| `out/阿姨你好_技术调研报告_v1.0.pdf` | 定稿 PDF 版，适合直接阅读/打印 |
| `android/app/build/outputs/apk/debug/app-debug.apk` | 本地构建的 debug APK（约 43 MB，不入 Git；发布时作为 Release 附件） |
| `src/part1.md` ~ `part4.md` | 报告正文源文件 |
| `assets/` | 封面与 3 张示意图 |
| `src/build_docx.py` | 由 Markdown 生成 DOCX/PDF 的脚本 |
| `src/make_figures.py` | 生成封面与示意图的脚本 |

## 报告回答的核心问题

1. **能不能做？** 能。端侧人体检测、多目标跟踪、人脸检测/特征提取、称呼推理都有成熟方案；ColorOS 14–17 的中端机可做到远距 25–30 FPS、近距 15–20 FPS。
2. **真正的难点在哪？** 不是人脸模型，而是“称呼依赖‘我’的关系图”“20–50 位亲戚的冷启动录入”“远距离人脸像素不足”三件事。
3. **远近怎么分工？** 远距（约 4 m 以外）只跑人体检测 + 跟踪，不加载人脸模型；近距（约 ≤4 m）才启用人脸检测与识别。1080p 下 2–3 m 稳定识别、4 m 出候选；2× 光学变焦/4K 裁切可把近距模式扩展到 6–8 m。
4. **怎么选型？** CameraX + LiteRT + EfficientDet-Lite0 + SCRFD-2.5G + MobileFaceNet + ByteTrack + Room/SQLCipher + relationship.js，平台只适配 ColorOS 14–17（Android 14–17，API 34–37）。
5. **最大的坑？** 许可证（YOLO 的 AGPL、InsightFace 权重非商用、DeepSORT 的 GPL）、隐私合规（本地存储、单独同意、PIPIA）、把“未知”强行认成亲戚。
6. **怎么做？** 2–3 人、10–12 周 MVP；先锁定 ColorOS 14–17 的 6–8 台设备，再用 20–30 人真实家庭数据校准阈值。

## 报告结构

- 摘要与结论速览
- 第 1 章 项目背景与需求定义
- 第 2 章 市场与竞品调研
- 第 3 章 技术可行性总览（含“能看多远”的光学测算）
- 第 4 章 系统架构设计（远/近两档状态机）
- 第 5 章 关键算法与模型选型（检测、识别、跟踪、称呼推理）
- 第 6 章 数据模型与接口
- 第 7 章 开源项目复用清单（含三层许可风险）
- 第 8 章 性能、功耗与 ColorOS 适配
- 第 9 章 隐私、合规与伦理（逐条对照 2025 年《人脸识别技术应用安全管理办法》）
- 第 10 章 风险与应对
- 第 11 章 实施路线图与成本
- 第 12 章 结论与建议
- 附录 A 常见亲属称呼对照表
- 附录 B 核心伪代码
- 附录 C 参考资料
- 附录 D 术语表

## 重建方式

在本目录下执行：

```powershell
$py = "C:\Users\yunlong\.dsh\dsh-runtimes\dsh-primary-runtime\dependencies\python\python.exe"
& $py src/make_figures.py
& $py src/build_docx.py
```

`build_docx.py` 会自动完成两遍排版：第一遍生成 PDF 以取得页码，第二遍回填目录页码并输出最终 DOCX/PDF。

## Android 工程

本仓库同时包含可直接用 Android Studio 打开的 MVP 工程：

```
android/
  app/    Compose UI + CameraX + SQLite + TFLite 适配
  core/   纯 Kotlin：关系图/称呼、ByteTracker、远近状态机、人脸匹配
```

已实现：远/近两档状态机（65%/55%）、ByteTracker 风格跟踪、关系图 BFS + 称呼词典 + 方言变体、开集人脸匹配、人脸样本录入/计数、SQLite 本地库、相机页/亲戚页/设置页/添加亲戚向导。

未实现/待补：SQLCipher、SCRFD 人脸检测适配、真实模型权重（需自行放入 `android/app/src/main/assets/models/`）、完整同意/PIPIA 流程。

详见 [`android/README.md`](android/README.md) 与 [`android/MODELS.md`](android/MODELS.md)。

## 说明与限制

- 调研与数据截至 **2026 年 11 月**；模型版本、许可证、价格可能变化，集成前请复核官方仓库与文档。
- 文中的延迟、功耗、识别距离为**工程估算**，需在 ColorOS 14–17 的目标机型与真实家庭数据上实测。
- 法规部分为产品设计层面的理解，**不构成法律意见**；上线前应由熟悉个人信息保护法的律师复核。
- 报告未开展大规模用户访谈与 ColorOS 14–17 全机型 benchmark，建议立项后 2 周内完成首轮验证（见 12.3 节）。
