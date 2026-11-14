# hello auntie · Logo 设计说明

## 概念

纯文字标识，不使用人物插画或徽章：

- **hello** 用细字重（300），**auntie** 用粗字重（900），形成“轻声打招呼 + 亲切强调”的语气。
- 全部小写，字母紧凑，像一个温和的招呼，而不是传统品牌徽章。
- 末尾一个**金色句点**，作为唯一装饰，也是品牌识别点。
- 砖红单色，适合浅色/深色背景；句点用金色，面积很小，不抢文字。

## 色板

| 名称 | Hex | 用途 |
| --- | --- | --- |
| 砖红 Brick Red | `#A62B2A` | 字标、图标背景 |
| 米白 Cream | `#F7E8CF` | 图标字标 |
| 金色 Gold | `#D8A24A` | 句点 |

## 字体

- **Noto Sans SC**（SIL OFL 1.1），字重 300 / 900。
- 不使用微软雅黑等专有字体固化 logo。

## 文件

| 文件 | 用途 |
| --- | --- |
| `assets/logo_wordmark.png` | 1400×360 横版字标（透明背景） |
| `assets/logo_mark.png` | 1024×1024 “ha” 方形图标 |
| `android/app/src/main/res/drawable-nodpi/ic_launcher_foreground.png` | Android 自适应图标前景 |
| `android/app/src/main/res/mipmap-*/ic_launcher*.png` | 各密度启动图标 |
| `android/app/src/main/res/mipmap-anydpi-v26/ic_launcher*.xml` | Android 8.0+ 自适应图标 |

生成脚本：`tools/make_logo.py`（Pillow）。修改字重、字距或颜色后重新运行即可。

## 使用规范

- 横版字标最小宽度建议 ≥ 240 px；再小时改用 `ha` 图标，不要强行缩小整句。
- 保留“hello”和“auntie”的字重差；不要把整句改成同一字重。
- 金色句点是识别点，不要删除、改成红色或加阴影。
- 图标留白建议 ≥ 字标高度的 1/2；不要把 `ha` 拉满画布。
- 本 Logo 为原创文字标识；商用前请自行做商标近似检索。
