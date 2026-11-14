Bundled demo models (for local MVP testing; check licenses before distribution).

- person.tflite      YOLOv8n, input [1,3,640,640] NCHW, output [1,84,8400]
- mobileface.tflite  MobileFaceNet, input [1,112,112,3] NHWC, output [1,192]

The app auto-detects 320/640 input and NHWC/NCHW for person.tflite.
See ../../MODELS.md for source, license and replacement notes.
