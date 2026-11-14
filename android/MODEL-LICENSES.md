# 模型来源与许可证

本目录记录 `android/app/src/main/assets/models/` 中两份测试权重的来源，方便后续替换或发行前复核。

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

- 当前仓库的 `.gitignore` 默认不提交 `.tflite`，这两份权重只用于本地 MVP 验证。
- 如果把 APK 对外分发，必须同时满足模型许可证（尤其是 YOLOv8 的 AGPL-3.0）和隐私合规要求。
- 本文只是工程记录，不构成法律意见。
