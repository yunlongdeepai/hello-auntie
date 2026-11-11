# 模型接入说明

App 不内置模型权重（体积 + 许可证原因）。把文件放到 `app/src/main/assets/models/` 即可。

## 1. 人体检测：person.tflite

- 推荐：YOLOv8n / YOLO11n，输入 320×320，单类 person；或 COCO 80 类的 person（class 0）。
- App 期望输出：`[1, 84, N]` 或 `[1, N, 84]`（84 = 4 个框参数 + 80 类分数），坐标为 0..1 归一化。
- 如果模型输出是 `[1, 84, 8400]`，无需修改；如果是 `[1, 8400, 84]`，`TflitePersonDetector` 也会自动识别。
- 如果模型是 SSD/EfficientDet 输出，需要改 `TflitePersonDetector.decode*`，或另写一个 `PersonDetector` 实现。

参考导出（Ultralytics YOLOv8，注意 AGPL-3.0 / 企业授权）：

```bash
yolo export model=yolov8n.pt format=tflite imgsz=320 int8=True
```

放到：

```
app/src/main/assets/models/person.tflite
```

> 商用前请确认 Ultralytics 许可；闭源产品需要企业授权，或换 YOLOX-Nano / NanoDet-Plus（Apache-2.0）。

## 2. 人脸特征：mobileface.tflite

- 推荐：MobileFaceNet / MobileFaceNet 蒸馏版，输入 112×112 RGB。
- 输入归一化：`(pixel / 127.5) - 1`。
- 输出：`[1, 512]` 或 `[1, 128]` 的 embedding；App 会做 L2 归一化。
- 输出维度由模型自动读取。

```
app/src/main/assets/models/mobileface.tflite
```

> InsightFace 的预训练权重仅限非商业研究；商用需自训/购买授权。详见调研报告第 7 章。

## 3. 人脸检测

当前 MVP 使用系统 `android.media.FaceDetector` 兜底，只检测正脸，不需要模型。后续可替换为：

- SCRFD-2.5G（InsightFace，权重非商用）
- MediaPipe Face Detector（Apache-2.0）
- YuNet（MIT，OpenCV Zoo）

替换方式：实现 `com.helloauntie.app.ml.FaceDetector`，在 `AppContainer` 中替换 `AndroidFaceDetector()` 即可。`face.tflite` 路径已预留，但 `TfliteFaceDetector` 尚未实现（SCRFD 输出解析较复杂，建议先跑通现有链路）。

## 4. 首次运行没有模型会怎样？

- 远距：`DemoPersonDetector` 生成一个缓慢变大/变小的演示框，用来验证 FAR/NEAR 切换和 UI。
- 近距：系统 FaceDetector 尝试找人脸；找不到就只显示“未知（近距）”。
- 设置页会显示“未安装”，并提示这是演示模式。
- 真实识别必须接入 `person.tflite` + `mobileface.tflite`，并录入人脸样本。

## 5. 模型转换检查清单

- [ ] 输入尺寸/布局与 App 一致（NCHW vs NHWC；本工程按 NHWC）
- [ ] 输入归一化一致（0..1 或 -1..1）
- [ ] 输出 shape 与解码器一致
- [ ] INT8 量化后 recall/rank-1 回归
- [ ] 16 KB 页对齐（Android 15+；LiteRT/NCNN 用最新版本）
- [ ] 许可证允许商用/分发
