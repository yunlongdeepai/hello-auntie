# 模型接入说明

当前仓库已放入一版可直接跑通的 float32 模型，用于本地 MVP 验证：

```text
app/src/main/assets/models/person.tflite
app/src/main/assets/models/mobileface.tflite
```

> 模型权重较大，且许可证不同；正式发布/商用前必须重新确认许可证，不建议直接把当前权重作为最终发行资产。来源与许可证细节见 [`MODEL-LICENSES.md`](MODEL-LICENSES.md)。

## 1. 当前模型规格

### person.tflite

- 来源：OpenCV contribution 的 YOLOv8n TFLite 导出
- 输入：`[1, 3, 640, 640]` **NCHW**，float32，RGB，0..1
- 输出：`[1, 84, 8400]`（4 个框参数 + 80 类分数，class 0 = person）
- 代码会自动识别 320/640 输入和 NCHW/NHWC；当前这版是 640 NCHW。
- 坐标按 0..1 归一化，代码内部会映射回原图尺寸。

> 这版权重来自 YOLOv8，受 AGPL-3.0 约束；闭源商用需要 Ultralytics 企业授权，或替换为 YOLOX-Nano / NanoDet-Plus（Apache-2.0，但解码器不同）。

### mobileface.tflite

- 来源：`ngtrphuong/facerecognition` 仓库的 MIT 版本 MobileFaceNet
- 输入：`[1, 112, 112, 3]` **NHWC**，float32，RGB
- 输出：`[1, 192]` embedding（L2 归一化后使用）
- 预处理：`(pixel - 128) / 128`；当前代码用 `(pixel / 127.5) - 1`，两者等价到 0.4% 以内。
- 代码会按输出最后一维自适应 128/192/512。

> 上游仓库为 MIT；模型权重来源/再分发前请自行复核。InsightFace 的 `buffalo_l` 权重是另一条路线，但模型包需要商业授权。

## 2. 替换模型时

### 人体检测

- 支持 `[1,S,S,3]` NHWC 或 `[1,3,S,S]` NCHW，S=320/640。
- 支持输出 `[1,4+C,N]` 或 `[1,N,4+C]`，C=1（单类 person）或 80（COCO）。
- 如果输出带 NMS（如 `[1,300,6]`），需要改解码器。
- 如果模型是 SSD/EfficientDet，需另写 `PersonDetector` 实现。

导出参考（Ultralytics YOLOv8，AGPL）：

```bash
yolo export model=yolov8n.pt format=tflite imgsz=320 nms=False int8=False
```

### 人脸特征

- 输入必须是 `[1,112,112,3]` float32；ONNX 常见的 NCHW 需要先转 NHWC。
- 输出 `[1,512]` / `[1,192]` / `[1,128]` 均可。
- 如果模型用 `0..1` 归一化，需要把 `TfliteFaceEmbedder` 的 `(pixel / 127.5) - 1` 改为 `pixel / 255f`。

## 3. 人脸检测

当前仍使用系统 `android.media.FaceDetector` 兜底，只检测正脸，不需要模型。

`face.tflite` 路径已预留，但 `TfliteFaceDetector` 尚未实现。下一步可替换为：

- SCRFD-2.5G（InsightFace，权重需授权）
- MediaPipe Face Detector（Apache-2.0）
- YuNet（MIT，OpenCV Zoo）

替换方式：实现 `com.helloauntie.app.ml.FaceDetector`，在 `AppContainer` 中把 `AndroidFaceDetector()` 换掉。

## 4. 首次运行没有模型

- 远距：`DemoPersonDetector` 演示框；
- 近距：系统 FaceDetector 找人脸，身份“未知”；
- 设置页显示“未安装”。

当前已放入模型，设置页应显示两个模型“已就绪”。

## 5. 模型检查清单

- [ ] 输入尺寸/布局与 App 一致（NHWC/NCHW）
- [ ] 输入归一化一致（0..1 / -1..1）
- [ ] 输出 shape 与解码器一致
- [ ] INT8 量化后 recall/rank-1 回归
- [ ] 16 KB 页对齐（Android 15+）
- [ ] 许可证允许分发/商用

## 6. 日志

App 启动后会输出：

```text
HelloAuntieML: person input=[1, 3, 640, 640] nchw=true size=640 output=[1, 84, 8400]
HelloAuntieML: person first result: candidates=... kept=...
HelloAuntieML: face input=[1, 112, 112, 3] output=[1, 192]
```

可用：

```powershell
adb logcat -s HelloAuntieML:I AndroidRuntime:E
```
