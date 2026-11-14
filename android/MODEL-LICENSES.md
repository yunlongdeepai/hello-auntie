# 模型来源与许可证

本项目代码采用根目录 [`LICENSE`](../LICENSE) 中的 **MIT License**；模型权重是独立的第三方资产，适用下列各自许可证。

本文件记录 `android/app/src/main/assets/models/` 中两份测试权重的来源，方便后续替换或发行前复核。

## person.tflite

- 文件：`person.tflite`
- 来源：`opencv/opencv_contribution` 的 YOLOv8n TFLite 导出（Hugging Face 镜像下载）
- 架构：YOLOv8n，COCO 80 类，class 0 = person
- 输入/输出：`[1,3,640,640]` NCHW → `[1,84,8400]`
- 许可证：**AGPL-3.0**（Ultralytics YOLOv8）
- 商用提示：闭源商用需要 Ultralytics 企业授权；或改用 YOLOX-Nano / NanoDet-Plus（Apache-2.0）并适配解码器。
- 上游：<https://github.com/ultralytics/ultralytics>

## mobileface.tflite

- 文件：`mobileface.tflite`
- 来源：`ngtrphuong/facerecognition` 仓库的 `assets/mobilefacenet.tflite`（MIT 仓库）
- 架构：MobileFaceNet，112×112，输出 192 维 embedding
- 输入/输出：`[1,112,112,3]` NHWC → `[1,192]`
- 许可证：仓库为 MIT；模型权重来源/再分发前请自行复核上游训练数据与权重条款。
- 上游：<https://github.com/ngtrphuong/facerecognition>

## 分发注意

- 两份 `.tflite` 已随仓库和 APK 分发，仅用于当前 MVP 验证；对外发布前必须重新确认模型许可证与训练数据条款。
- APK/模型对外分发时，必须同时满足模型许可证（尤其是 YOLOv8 的 AGPL-3.0）和隐私合规要求。
- 本文只是工程记录，不构成法律意见。
