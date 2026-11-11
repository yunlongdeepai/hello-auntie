# -*- coding: utf-8 -*-
"""Generate cover and diagrams for the report (Pillow only)."""
import math
import random
from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ROOT = str(Path(__file__).resolve().parent.parent)
FONT_REG = r"C:\Windows\Fonts\msyh.ttc"
FONT_BOLD = r"C:\Windows\Fonts\msyhbd.ttc"

NAVY = (22, 50, 79)
DEEP = (12, 30, 46)
TEAL = (31, 111, 139)
TEAL_DARK = (23, 84, 105)
TEAL_LIGHT = (234, 243, 247)
GOLD = (198, 162, 92)
GRAY = (92, 106, 122)
LIGHT = (247, 249, 251)
WHITE = (255, 255, 255)
GREEN = (46, 125, 90)
AMBER = (196, 132, 38)
RED = (170, 62, 62)
BORDER = (201, 214, 226)


def font(size, bold=False):
    return ImageFont.truetype(FONT_BOLD if bold else FONT_REG, size)


def text(draw, xy, s, size=28, bold=False, fill=NAVY, anchor="mm", align="center", spacing=8):
    draw.multiline_text(xy, s, font=font(size, bold), fill=fill, anchor=anchor,
                        align=align, spacing=spacing)


def rbox(draw, box, r=18, fill=WHITE, outline=BORDER, width=3):
    draw.rounded_rectangle(box, radius=r, fill=fill, outline=outline, width=width)


def arrow(draw, p1, p2, color=TEAL, width=5, head=16):
    draw.line([p1, p2], fill=color, width=width)
    ang = math.atan2(p2[1] - p1[1], p2[0] - p1[0])
    a1 = ang + math.radians(152)
    a2 = ang - math.radians(152)
    pts = [p2,
           (p2[0] + head * math.cos(a1), p2[1] + head * math.sin(a1)),
           (p2[0] + head * math.cos(a2), p2[1] + head * math.sin(a2))]
    draw.polygon(pts, fill=color)


def dashed_line(draw, p1, p2, color=GOLD, width=4, dash=16, gap=12):
    x1, y1 = p1
    x2, y2 = p2
    dist = math.hypot(x2 - x1, y2 - y1)
    if dist == 0:
        return
    ux, uy = (x2 - x1) / dist, (y2 - y1) / dist
    pos = 0
    while pos < dist:
        end = min(pos + dash, dist)
        draw.line([(x1 + ux * pos, y1 + uy * pos), (x1 + ux * end, y1 + uy * end)],
                  fill=color, width=width)
        pos = end + gap


def arrow_polyline(draw, pts, color=TEAL, width=5, head=16):
    for i in range(len(pts) - 2):
        draw.line([pts[i], pts[i + 1]], fill=color, width=width)
    arrow(draw, pts[-2], pts[-1], color=color, width=width, head=head)


def chip(draw, x, y, w, h, title, sub=None, fill=WHITE, outline=BORDER,
         title_color=NAVY, sub_color=GRAY, ts=27, ss=20):
    rbox(draw, (x, y, x + w, y + h), r=16, fill=fill, outline=outline, width=2)
    if sub:
        text(draw, (x + w / 2, y + h / 2 - 14), title, ts, True, title_color)
        text(draw, (x + w / 2, y + h / 2 + 22), sub, ss, False, sub_color)
    else:
        text(draw, (x + w / 2, y + h / 2), title, ts, True, title_color)


def band(draw, x, y, w, h, label, fill):
    draw.rounded_rectangle((x, y, x + w, y + h), radius=24, fill=fill, outline=None)
    text(draw, (x + 26, y + 24), label, 28, True, TEAL_DARK, anchor="lm")


# ---------------------------------------------------------------- cover
def make_cover():
    W, H = 2480, 3508
    img = Image.new("RGB", (W, H), DEEP)
    d = ImageDraw.Draw(img)
    # vertical gradient
    for yy in range(H):
        t = yy / H
        c = (int(10 + 10 * t), int(28 + 18 * t), int(44 + 24 * t))
        d.line([(0, yy), (W, yy)], fill=c)

    # subtle network motif on the right
    overlay = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    od = ImageDraw.Draw(overlay)
    random.seed(7)
    nodes = []
    for _ in range(26):
        nx = random.randint(1350, 2360)
        ny = random.randint(250, 2300)
        nodes.append((nx, ny))
    for i, a in enumerate(nodes):
        for b in nodes[i + 1:]:
            if math.hypot(a[0] - b[0], a[1] - b[1]) < 420:
                od.line([a, b], fill=(70, 150, 170, 46), width=3)
    for a in nodes:
        r = random.randint(6, 15)
        od.ellipse((a[0] - r, a[1] - r, a[0] + r, a[1] + r), fill=(90, 190, 205, 85))
    # a large soft ring
    od.ellipse((1500, 950, 2500, 1950), outline=(90, 190, 205, 55), width=6)
    od.ellipse((1650, 1100, 2350, 1800), outline=(198, 162, 92, 60), width=4)
    overlay = overlay.filter(ImageFilter.GaussianBlur(1))
    img = Image.alpha_composite(img.convert("RGBA"), overlay).convert("RGB")
    d = ImageDraw.Draw(img)

    # top label
    text(d, (180, 240), "技 术 调 研 报 告    /    TECHNOLOGY RESEARCH", 44, True, (150, 196, 208), anchor="lm")
    d.line([(180, 300), (900, 300)], fill=GOLD, width=6)

    # title
    text(d, (172, 760), "阿姨你好", 250, True, WHITE, anchor="lm")
    text(d, (180, 1050), "安卓端离线亲属称呼助手", 86, False, (214, 230, 238), anchor="lm")
    d.line([(186, 1160), (1200, 1160)], fill=(198, 162, 92), width=5)

    # tagline
    text(d, (184, 1260), "看见人 · 认出人 · 叫对人", 56, False, (168, 206, 218), anchor="lm")

    # four modules
    modules = ["目标检测", "目标跟踪", "人脸识别", "亲属关系推理"]
    x = 184
    for m in modules:
        w = 76 + len(m) * 44
        d.rounded_rectangle((x, 1420, x + w, 1520), radius=16,
                            fill=(28, 62, 86), outline=(72, 128, 150), width=2)
        text(d, (x + w / 2, 1470), m, 36, False, (226, 238, 243))
        x += w + 22

    text(d, (184, 1600), "适配 ColorOS 14–17  ｜  端侧离线  ｜  数据不出手机", 30, False, (150, 196, 208), anchor="lm")

    # bottom meta
    d.line([(184, 1700), (1100, 1700)], fill=(60, 96, 118), width=2)
    text(d, (184, 1780), "一份关于可行性、技术选型、合规与落地路径的调研", 36, False, (150, 180, 196), anchor="lm")
    text(d, (184, 1850), "回答三个问题：能不能做 · 怎么做 · 风险在哪", 32, False, (120, 150, 168), anchor="lm")

    d.line([(180, 3120), (2300, 3120)], fill=(60, 96, 118), width=2)
    text(d, (184, 3200), "版本 v1.0    ｜    2026 年 11 月", 40, False, (188, 210, 220), anchor="lm")
    text(d, (184, 3270), "内部调研资料 · 数据与结论以实测为准", 30, False, (130, 158, 174), anchor="lm")

    img.save(f"{ROOT}/assets/cover.png", quality=95)


# ---------------------------------------------------------------- architecture
def make_architecture():
    W, H = 2400, 1010
    img = Image.new("RGB", (W, H), LIGHT)
    d = ImageDraw.Draw(img)

    main_x, main_w = 80, 1760
    bands = [
        (90, 160, "① 采集层", (242, 247, 250)),
        (300, 190, "② 感知层", (234, 243, 247)),
        (540, 190, "③ 认知层", (232, 241, 246)),
        (780, 160, "④ 交互层", (244, 247, 249)),
    ]
    for y, h, label, fill in bands:
        band(d, main_x, y, main_w, h, label, fill)

    # band 1
    chip(d, 150, 135, 560, 85, "相机采集", None, ts=38)
    chip(d, 760, 135, 560, 85, "相册导入", None, ts=38)

    # band 2
    names = ["预处理", "人体检测", "多目标跟踪", "人脸检测", "对齐 / 质量", "特征提取"]
    x = 110
    for i, t in enumerate(names):
        chip(d, x, 345, 270, 100, t, None, ts=36)
        if i < len(names) - 1:
            arrow(d, (x + 270 + 4, 395), (x + 270 + 14, 395), width=4, head=10)
        x += 288

    # band 3
    names3 = ["1:N 匹配", "身份融合", "亲属关系图", "称呼推理"]
    x = 110
    for i, t in enumerate(names3):
        chip(d, x, 585, 400, 100, t, None, ts=36)
        if i < len(names3) - 1:
            arrow(d, (x + 400 + 6, 635), (x + 400 + 24, 635), width=4, head=10)
        x += 430

    # band 4
    names4 = ["AR 标注", "详情与纠正", "TTS 播报"]
    x = 140
    for i, t in enumerate(names4):
        chip(d, x, 815, 520, 90, t, None, ts=36)
        if i < len(names4) - 1:
            arrow(d, (x + 520 + 6, 860), (x + 520 + 34, 860), width=4, head=10)
        x += 560

    # vertical main arrows
    arrow(d, (960, 250), (960, 300), width=5)
    text(d, (1010, 275), "图像帧", 28, False, GRAY, anchor="lm")
    arrow(d, (960, 490), (960, 540), width=5)
    text(d, (1010, 515), "人脸 / 轨迹 / 特征", 28, False, GRAY, anchor="lm")
    arrow(d, (960, 730), (960, 780), width=5)
    text(d, (1010, 755), "称呼 / 候选 / 置信度", 28, False, GRAY, anchor="lm")

    # data panel
    px, py, pw, ph = 1900, 90, 420, 850
    d.rounded_rectangle((px, py, px + pw, py + ph), radius=24, fill=(255, 249, 236),
                        outline=GOLD, width=3)
    text(d, (px + 30, py + 30), "⑤ 数据层（本地）", 30, True, (150, 112, 38), anchor="lm")
    data_items = ["本地加密库", "人脸模板", "关系边 / 别名", "同意 / 审计"]
    yy = py + 80
    for t in data_items:
        chip(d, px + 30, yy, pw - 60, 115, t, None, fill=WHITE, outline=(226, 205, 160),
             title_color=(110, 82, 30), ts=34)
        yy += 150

    # dashed connections data -> cognition / interaction
    dashed_line(d, (px, 620), (1846, 620), color=GOLD, width=4)
    arrow(d, (1846, 620), (1856, 620), color=GOLD, width=4, head=8)
    dashed_line(d, (px, 845), (1846, 845), color=GOLD, width=4)
    arrow(d, (1846, 845), (1856, 845), color=GOLD, width=4, head=8)

    text(d, (80, 955), "实线：主链路；虚线：本地数据读写。远距只跑人体检测 + 跟踪，近距才启用人脸检测/识别。",
         24, False, GRAY, anchor="lm")

    img.save(f"{ROOT}/assets/fig1_architecture.png")


# ---------------------------------------------------------------- state machine
def make_state():
    W, H = 2400, 960
    img = Image.new("RGB", (W, H), LIGHT)
    d = ImageDraw.Draw(img)

    # FAR / NEAR / IDENTIFIED
    boxes = [
        (200, 240, 360, 210, "FAR", "人体检测 + 跟踪\n不加载人脸模型", (238, 241, 244), GRAY),
        (900, 240, 360, 210, "NEAR", "人体 + 人脸 + 识别", (230, 243, 247), TEAL_DARK),
        (1600, 240, 360, 210, "IDENTIFIED", "稳定显示称呼", (232, 245, 238), GREEN),
    ]
    for x, y, w, h, t, s, fill, col in boxes:
        rbox(d, (x, y, x + w, y + h), r=20, fill=fill, outline=col, width=4)
        text(d, (x + w / 2, y + 72), t, 40, True, col)
        text(d, (x + w / 2, y + 145), s, 25, False, GRAY, spacing=6)

    # forward FAR -> NEAR
    arrow(d, (560, 320), (900, 320))
    text(d, (730, 185), "人体框 ≥ 65%\n（约 ≤4 m）", 27, False, GRAY, spacing=4)
    # hysteresis NEAR -> FAR (dashed)
    dashed_line(d, (900, 390), (560, 390), color=GRAY, width=3)
    arrow(d, (566, 390), (560, 390), color=GRAY, width=3, head=10)
    text(d, (730, 472), "人体框 < 55%（约 >4.7 m）", 23, False, GRAY)

    # NEAR -> IDENTIFIED
    arrow(d, (1260, 320), (1600, 320))
    text(d, (1430, 185), "连续 3 帧\n≥ 阈值", 27, False, GRAY, spacing=4)

    # uncertain
    rbox(d, (900, 650, 1260, 810), r=20, fill=(255, 247, 235), outline=AMBER, width=4)
    text(d, (1080, 705), "UNCERTAIN", 34, True, AMBER)
    text(d, (1080, 762), "top-1 / margin 不足", 23, False, GRAY)
    arrow(d, (1080, 450), (1080, 650), color=AMBER, width=4, head=14)
    text(d, (1110, 550), "质量低 / 候选", 26, False, AMBER, anchor="lm")
    arrow(d, (990, 650), (990, 450), color=AMBER, width=4, head=14)
    text(d, (960, 550), "用户确认", 26, False, AMBER, anchor="rm")

    # lost
    rbox(d, (1600, 650, 1960, 810), r=20, fill=(247, 240, 240), outline=RED, width=4)
    text(d, (1780, 705), "LOST", 34, True, RED)
    text(d, (1780, 762), "30 帧未检出 / 离屏", 23, False, GRAY)
    arrow(d, (1780, 450), (1780, 650), color=RED, width=4, head=14)
    text(d, (1810, 550), "丢失", 26, False, RED, anchor="lm")

    # lost -> far
    pts = [(1600, 730), (1600, 890), (380, 890), (380, 452)]
    for i in range(len(pts) - 2):
        dashed_line(d, pts[i], pts[i + 1], color=RED, width=3)
    arrow(d, (380, 890), (380, 452), color=RED, width=3, head=12)
    text(d, (1000, 920), "重新进入：从 FAR 重新建立轨迹", 24, False, RED)

    text(d, (120, 65), "远/近两档状态机（每个 track 独立）", 34, True, NAVY, anchor="lm")
    text(d, (120, 115), "远距只跑人体检测 + 跟踪；近距再加人脸检测/识别。65% 进入，55% 退回。",
         24, False, GRAY, anchor="lm")

    img.save(f"{ROOT}/assets/fig2_state.png")


# ---------------------------------------------------------------- family tree
def make_tree():
    W, H = 2200, 1200
    img = Image.new("RGB", (W, H), LIGHT)
    d = ImageDraw.Draw(img)

    nodes = {
        "爷爷": (700, 180, 200, 100),
        "奶奶": (1000, 180, 200, 100),
        "伯母": (120, 430, 200, 100),
        "伯父": (380, 430, 200, 100),
        "爸爸": (760, 430, 200, 100),
        "妈妈": (1080, 430, 200, 100),
        "堂哥": (380, 780, 200, 100),
        "我": (920, 780, 200, 100),
    }

    def center(name):
        x, y, _, _ = nodes[name]
        return (x, y)

    # ---- gray edges (drawn under boxes) ----
    edges = [
        (center("爷爷"), center("奶奶")),
        (center("爷爷"), center("伯父")),
        (center("爷爷"), center("爸爸")),
        (center("伯父"), center("伯母")),
        (center("爸爸"), center("妈妈")),
        (center("伯父"), center("堂哥")),
        (center("爸爸"), center("我")),
        (center("妈妈"), center("我")),
        (center("伯父"), center("爸爸")),
    ]
    for a, b in edges:
        d.line([a, b], fill=(150, 162, 174), width=5)

    # ---- highlighted path: ego -> dad -> uncle -> aunt ----
    path = [center("我"), center("爸爸"), center("伯父"), center("伯母")]
    for i in range(len(path) - 1):
        d.line([path[i], path[i + 1]], fill=TEAL, width=11)

    # ---- boxes on top ----
    for name, (x, y, w, h) in nodes.items():
        if name == "我":
            fill, outline, col = (232, 245, 238), GREEN, GREEN
        elif name in ("伯父", "伯母"):
            fill, outline, col = (230, 243, 247), TEAL, TEAL_DARK
        else:
            fill, outline, col = WHITE, BORDER, NAVY
        rbox(d, (x - w / 2, y - h / 2, x + w / 2, y + h / 2), r=16,
             fill=fill, outline=outline, width=4)
        text(d, (x, y), name, 38, True, col)

    # arrow head at target, just outside the box
    arrow(d, (330, 430), (226, 430), color=TEAL, width=10, head=20)

    # ---- edge labels ----
    text(d, (790, 640), "爸爸", 26, True, TEAL_DARK, anchor="rm")
    text(d, (560, 398), "哥哥", 26, True, TEAL_DARK, anchor="lm")
    text(d, (300, 350), "妻子", 26, True, TEAL_DARK, anchor="lm")

    # ---- explanation card ----
    d.rounded_rectangle((1400, 620, 2100, 980), radius=24, fill=WHITE, outline=BORDER, width=3)
    text(d, (1450, 675), "从“我”到“伯母”的推理", 36, True, NAVY, anchor="lm")
    lines = [
        ("路径", "我 → 爸爸 → 哥哥 → 妻子"),
        ("选择器", "爸爸的哥哥的妻子"),
        ("称呼", "伯母 / 大娘"),
        ("按排行", "二伯母（若爸爸排行老二）"),
        ("置信度", "高（血缘 + 婚姻，均已确认）"),
    ]
    yy = 745
    for k, v in lines:
        text(d, (1450, yy), k, 28, True, TEAL_DARK, anchor="lm")
        text(d, (1570, yy), v, 32, False, NAVY, anchor="lm")
        yy += 47

    text(d, (1400, 1030), "说明：矩形为人物节点，灰线为亲属/婚姻边；", 26, False, GRAY, anchor="lm")
    text(d, (1400, 1074), "青色高亮为本次推理路径，方向为“我 → 目标”。", 26, False, GRAY, anchor="lm")

    img.save(f"{ROOT}/assets/fig3_tree.png")


if __name__ == "__main__":
    make_cover()
    make_architecture()
    make_state()
    make_tree()
    print("figures done")
