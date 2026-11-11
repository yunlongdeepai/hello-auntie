# -*- coding: utf-8 -*-
"""Build the Chinese research report DOCX from Markdown sources."""
import os
import re
import shutil
import subprocess
import sys
from pathlib import Path

from docx import Document
from docx.enum.section import WD_SECTION
from docx.enum.table import WD_ALIGN_VERTICAL, WD_TABLE_ALIGNMENT
from docx.enum.text import WD_ALIGN_PARAGRAPH, WD_TAB_ALIGNMENT, WD_TAB_LEADER, WD_BREAK
from docx.oxml import OxmlElement
from docx.oxml.ns import qn
from docx.shared import Cm, Pt, RGBColor, Emu

ROOT = Path(__file__).resolve().parent.parent
SRC = ROOT / "src"
OUT = ROOT / "out"
ASSETS = ROOT / "assets"

NAVY = "16324F"
TEAL = "1F6F8B"
TEAL_DARK = "175469"
GOLD = "C6A25C"
GRAY = "5C6A7A"
LIGHT_GRAY = "8A98A8"
BORDER = "D9E1EA"
ZEBRA = "F5F8FA"
CALLOUT = "EAF3F7"
CODE_BG = "F4F6F8"

BODY_FONT = "宋体"
HEAD_FONT = "微软雅黑"
MONO_FONT = "Consolas"


# ------------------------------------------------------------------ XML helpers
def _insert(parent, elm, successors):
    parent.insert_element_before(elm, *successors)


def shade_cell(cell, fill):
    tcPr = cell._tc.get_or_add_tcPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:val"), "clear")
    shd.set(qn("w:color"), "auto")
    shd.set(qn("w:fill"), fill)
    _insert(tcPr, shd, (
        "w:noWrap", "w:tcMar", "w:textDirection", "w:tcFitText", "w:vAlign",
        "w:hideMark", "w:headers", "w:cellIns", "w:cellDel", "w:cellMerge", "w:tcPrChange",
    ))


def cell_margins(cell, top=40, bottom=40, left=80, right=80):
    tcPr = cell._tc.get_or_add_tcPr()
    mar = OxmlElement("w:tcMar")
    for side, val in (("top", top), ("left", left), ("bottom", bottom), ("right", right)):
        el = OxmlElement(f"w:{side}")
        el.set(qn("w:w"), str(val))
        el.set(qn("w:type"), "dxa")
        mar.append(el)
    _insert(tcPr, mar, (
        "w:textDirection", "w:tcFitText", "w:vAlign", "w:hideMark",
        "w:headers", "w:cellIns", "w:cellDel", "w:cellMerge", "w:tcPrChange",
    ))


def table_borders(table, color=BORDER, sz=4):
    tblPr = table._tbl.tblPr
    borders = OxmlElement("w:tblBorders")
    for edge in ("top", "left", "bottom", "right", "insideH", "insideV"):
        el = OxmlElement(f"w:{edge}")
        el.set(qn("w:val"), "single")
        el.set(qn("w:sz"), str(sz))
        el.set(qn("w:space"), "0")
        el.set(qn("w:color"), color)
        borders.append(el)
    _insert(tblPr, borders, (
        "w:shd", "w:tblLayout", "w:tblCellMar", "w:tblLook",
        "w:tblCaption", "w:tblDescription", "w:tblPrChange",
    ))


def row_height(row, cm=0.6, at_least=True):
    trPr = row._tr.get_or_add_trPr()
    h = OxmlElement("w:trHeight")
    h.set(qn("w:val"), str(int(cm * 567)))
    h.set(qn("w:hRule"), "atLeast" if at_least else "exact")
    _insert(trPr, h, ("w:tblHeader", "w:tblCellSpacing", "w:jc", "w:hidden", "w:ins", "w:del"))


def row_no_split(row):
    trPr = row._tr.get_or_add_trPr()
    el = OxmlElement("w:cantSplit")
    _insert(trPr, el, ("w:trHeight", "w:tblHeader", "w:tblCellSpacing", "w:jc", "w:hidden", "w:ins", "w:del"))


def repeat_header(row):
    trPr = row._tr.get_or_add_trPr()
    el = OxmlElement("w:tblHeader")
    trPr.append(el)


def para_bottom_border(p, color=TEAL, sz=12):
    pPr = p._p.get_or_add_pPr()
    pBdr = OxmlElement("w:pBdr")
    bottom = OxmlElement("w:bottom")
    bottom.set(qn("w:val"), "single")
    bottom.set(qn("w:sz"), str(sz))
    bottom.set(qn("w:space"), "4")
    bottom.set(qn("w:color"), color)
    pBdr.append(bottom)
    _insert(pPr, pBdr, (
        "w:shd", "w:tabs", "w:suppressAutoHyphens", "w:kinsoku", "w:wordWrap",
        "w:overflowPunct", "w:topLinePunct", "w:autoSpaceDE", "w:autoSpaceDN",
        "w:bidi", "w:adjustRightInd", "w:snapToGrid", "w:spacing", "w:ind",
        "w:contextualSpacing", "w:mirrorIndents", "w:suppressOverlap", "w:jc",
        "w:textDirection", "w:textAlignment", "w:textboxTightWrap", "w:outlineLvl",
        "w:divId", "w:cnfStyle", "w:rPr", "w:sectPr", "w:pPrChange",
    ))


def shade_paragraph(p, fill=CODE_BG):
    pPr = p._p.get_or_add_pPr()
    shd = OxmlElement("w:shd")
    shd.set(qn("w:val"), "clear")
    shd.set(qn("w:color"), "auto")
    shd.set(qn("w:fill"), fill)
    _insert(pPr, shd, (
        "w:tabs", "w:suppressAutoHyphens", "w:kinsoku", "w:wordWrap",
        "w:overflowPunct", "w:topLinePunct", "w:autoSpaceDE", "w:autoSpaceDN",
        "w:bidi", "w:adjustRightInd", "w:snapToGrid", "w:spacing", "w:ind",
        "w:contextualSpacing", "w:mirrorIndents", "w:suppressOverlap", "w:jc",
        "w:textDirection", "w:textAlignment", "w:textboxTightWrap", "w:outlineLvl",
        "w:divId", "w:cnfStyle", "w:rPr", "w:sectPr", "w:pPrChange",
    ))


def add_field(p, instr):
    fld = OxmlElement("w:fldSimple")
    fld.set(qn("w:instr"), instr)
    r = OxmlElement("w:r")
    rPr = OxmlElement("w:rPr")
    rFonts = OxmlElement("w:rFonts")
    rFonts.set(qn("w:ascii"), "微软雅黑")
    rFonts.set(qn("w:hAnsi"), "微软雅黑")
    rFonts.set(qn("w:eastAsia"), "微软雅黑")
    rPr.append(rFonts)
    sz = OxmlElement("w:sz")
    sz.set(qn("w:val"), "17")
    rPr.append(sz)
    color = OxmlElement("w:color")
    color.set(qn("w:val"), LIGHT_GRAY)
    rPr.append(color)
    r.append(rPr)
    t = OxmlElement("w:t")
    t.text = "1"
    r.append(t)
    fld.append(r)
    p._p.append(fld)


# ------------------------------------------------------------------ text helpers
def set_run_font(run, font=BODY_FONT, size=10.5, bold=False, color=NAVY, italic=False):
    run.font.name = font
    run.font.size = Pt(size)
    run.bold = bold
    run.italic = italic
    run.font.color.rgb = RGBColor.from_string(color)
    rPr = run._element.get_or_add_rPr()
    rFonts = rPr.find(qn("w:rFonts"))
    if rFonts is None:
        rFonts = OxmlElement("w:rFonts")
        rPr.insert(0, rFonts)
    rFonts.set(qn("w:ascii"), font)
    rFonts.set(qn("w:hAnsi"), font)
    rFonts.set(qn("w:eastAsia"), font)


TOKEN = re.compile(r"(\*\*.+?\*\*|`[^`]+`)")


def add_runs(p, text, size=10.5, color=NAVY, bold=False, font=BODY_FONT):
    for part in TOKEN.split(text):
        if not part:
            continue
        if part.startswith("**") and part.endswith("**") and len(part) > 4:
            r = p.add_run(part[2:-2])
            set_run_font(r, font=font, size=size, bold=True, color=color)
        elif part.startswith("`") and part.endswith("`") and len(part) > 2:
            r = p.add_run(part[1:-1])
            set_run_font(r, font=MONO_FONT, size=size - 1.2, bold=False, color="B03A2E")
            r.font.name = MONO_FONT
            r._element.get_or_add_rPr().rFonts.set(qn("w:eastAsia"), "微软雅黑")
        else:
            r = p.add_run(part)
            set_run_font(r, font=font, size=size, bold=bold, color=color)


# ------------------------------------------------------------------ markdown parse
def normalize(s):
    return re.sub(r"\s+", "", s)


def parse_markdown(paths):
    text = "\n\n".join(Path(p).read_text(encoding="utf-8") for p in paths)
    lines = text.splitlines()
    blocks = []
    headings = []
    i = 0
    while i < len(lines):
        raw = lines[i]
        line = raw.rstrip()
        if not line.strip():
            i += 1
            continue
        if line.startswith("```"):
            i += 1
            code = []
            while i < len(lines) and not lines[i].startswith("```"):
                code.append(lines[i])
                i += 1
            i += 1
            blocks.append(("code", "\n".join(code)))
            continue
        if line.startswith("### "):
            blocks.append(("h3", line[4:].strip()))
            i += 1
            continue
        if line.startswith("## "):
            blocks.append(("h2", line[3:].strip()))
            headings.append((2, line[3:].strip()))
            i += 1
            continue
        if line.startswith("# "):
            blocks.append(("h1", line[2:].strip()))
            headings.append((1, line[2:].strip()))
            i += 1
            continue
        if line.startswith("!["):
            m = re.match(r"!\[(.*?)\]\((.*?)\)", line)
            if m:
                blocks.append(("image", (m.group(1), m.group(2))))
                i += 1
                continue
        if line.startswith(">"):
            quote = []
            while i < len(lines) and lines[i].startswith(">"):
                quote.append(lines[i][1:].lstrip())
                i += 1
            blocks.append(("quote", "\n".join(quote)))
            continue
        if line.strip().startswith("|"):
            table = []
            while i < len(lines) and lines[i].strip().startswith("|"):
                table.append(lines[i].strip())
                i += 1
            blocks.append(("table", table))
            continue
        if re.match(r"^- ", line):
            items = []
            while i < len(lines) and re.match(r"^- ", lines[i]):
                items.append(lines[i][2:].strip())
                i += 1
            blocks.append(("ul", items))
            continue
        if re.match(r"^\d+\. ", line):
            items = []
            while i < len(lines) and re.match(r"^\d+\. ", lines[i]):
                items.append(lines[i])
                i += 1
            blocks.append(("ol", items))
            continue
        para = [line.strip()]
        i += 1
        while i < len(lines) and lines[i].strip():
            nxt = lines[i]
            if re.match(r"^(#|\||>|```|!\[|- |\d+\. )", nxt):
                break
            para.append(nxt.strip())
            i += 1
        blocks.append(("p", " ".join(para)))
    return blocks, headings


def parse_table(lines):
    rows = []
    for idx, line in enumerate(lines):
        cells = [c.strip() for c in line.strip().strip("|").split("|")]
        if idx == 1 and cells and all(re.match(r"^:?-{2,}:?$", c) for c in cells):
            continue
        rows.append(cells)
    n = max(len(r) for r in rows) if rows else 0
    rows = [r + [""] * (n - len(r)) for r in rows]
    return rows


# ------------------------------------------------------------------ document build
def setup_styles(doc):
    st = doc.styles["Normal"]
    st.font.name = BODY_FONT
    st.font.size = Pt(10.5)
    st.font.color.rgb = RGBColor.from_string("1F2933")
    st.element.rPr.rFonts.set(qn("w:eastAsia"), BODY_FONT)
    pf = st.paragraph_format
    pf.line_spacing = 1.62
    pf.space_before = Pt(0)
    pf.space_after = Pt(4)

    for name, size, color, before, after in (
        ("Heading 1", 19, NAVY, 0, 12),
        ("Heading 2", 13.5, TEAL_DARK, 14, 6),
        ("Heading 3", 11.5, "2E4A62", 10, 4),
    ):
        h = doc.styles[name]
        h.font.name = HEAD_FONT
        h.font.size = Pt(size)
        h.font.bold = True
        h.font.color.rgb = RGBColor.from_string(color)
        h.element.rPr.rFonts.set(qn("w:eastAsia"), HEAD_FONT)
        h.paragraph_format.space_before = Pt(before)
        h.paragraph_format.space_after = Pt(after)
        h.paragraph_format.keep_with_next = True
        h.paragraph_format.line_spacing = 1.15


def anchor_picture(run, w_cm=21, h_cm=29.7):
    drawing = run._r.find(qn("w:drawing"))
    inline = drawing.find(qn("wp:inline"))
    if inline is None:
        return
    anchor = OxmlElement("wp:anchor")
    for k, v in {
        "distT": "0", "distB": "0", "distL": "0", "distR": "0",
        "simplePos": "0", "relativeHeight": "1", "behindDoc": "1",
        "locked": "0", "layoutInCell": "1", "allowOverlap": "1",
    }.items():
        anchor.set(k, v)
    simple = OxmlElement("wp:simplePos")
    simple.set("x", "0")
    simple.set("y", "0")
    anchor.append(simple)
    for tag, rel in (("wp:positionH", "page"), ("wp:positionV", "page")):
        pos = OxmlElement(tag)
        pos.set("relativeFrom", rel)
        off = OxmlElement("wp:posOffset")
        off.text = "0"
        pos.append(off)
        anchor.append(pos)
    for tag in ("wp:extent", "wp:effectExtent", "wp:docPr", "wp:cNvGraphicFramePr", "a:graphic"):
        el = inline.find(qn(tag))
        if el is not None:
            if tag == "wp:extent":
                el.set("cx", str(Cm(w_cm).emu))
                el.set("cy", str(Cm(h_cm).emu))
            anchor.append(el)
        if tag == "wp:effectExtent":
            wrap = OxmlElement("wp:wrapNone")
            anchor.append(wrap)
    drawing.remove(inline)
    drawing.append(anchor)


def add_cover(doc):
    sec = doc.sections[0]
    sec.page_width = Cm(21)
    sec.page_height = Cm(29.7)
    for attr in ("top_margin", "bottom_margin", "left_margin", "right_margin"):
        setattr(sec, attr, Cm(0))
    sec.header_distance = Cm(0)
    sec.footer_distance = Cm(0)
    sec.header.is_linked_to_previous = False
    sec.footer.is_linked_to_previous = False

    p = doc.add_paragraph()
    pf = p.paragraph_format
    pf.space_before = Pt(0)
    pf.space_after = Pt(0)
    pf.line_spacing = 1.0
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    run = p.add_run()
    run.add_picture(str(ASSETS / "cover.png"), width=Cm(21), height=Cm(29.7))
    anchor_picture(run, 21, 29.7)


def setup_body_section(doc):
    sec = doc.add_section(WD_SECTION.NEW_PAGE)
    sec.page_width = Cm(21)
    sec.page_height = Cm(29.7)
    sec.top_margin = Cm(2.2)
    sec.bottom_margin = Cm(2.0)
    sec.left_margin = Cm(2.4)
    sec.right_margin = Cm(2.4)
    sec.header_distance = Cm(1.0)
    sec.footer_distance = Cm(0.9)

    header = sec.header
    header.is_linked_to_previous = False
    hp = header.paragraphs[0]
    hp.alignment = WD_ALIGN_PARAGRAPH.RIGHT
    hp.paragraph_format.space_after = Pt(0)
    r = hp.add_run("阿姨你好 · 技术调研报告")
    set_run_font(r, font=HEAD_FONT, size=8, color=LIGHT_GRAY)
    para_bottom_border(hp, color="D9E1EA", sz=4)

    footer = sec.footer
    footer.is_linked_to_previous = False
    fp = footer.paragraphs[0]
    fp.alignment = WD_ALIGN_PARAGRAPH.CENTER
    fp.paragraph_format.space_before = Pt(0)
    r = fp.add_run("第 ")
    set_run_font(r, font=HEAD_FONT, size=8.5, color=LIGHT_GRAY)
    add_field(fp, " PAGE ")
    r = fp.add_run(" 页 / 共 ")
    set_run_font(r, font=HEAD_FONT, size=8.5, color=LIGHT_GRAY)
    add_field(fp, " NUMPAGES ")
    r = fp.add_run(" 页")
    set_run_font(r, font=HEAD_FONT, size=8.5, color=LIGHT_GRAY)
    return sec


def add_h1(doc, text, first=False):
    p = doc.add_paragraph(style="Heading 1")
    p.paragraph_format.page_break_before = not first
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(12)
    add_runs(p, text, size=19, color=NAVY, bold=True, font=HEAD_FONT)
    para_bottom_border(p, color=TEAL, sz=12)
    return p


def add_h2(doc, text):
    p = doc.add_paragraph(style="Heading 2")
    add_runs(p, text, size=13.5, color=TEAL_DARK, bold=True, font=HEAD_FONT)
    return p


def add_h3(doc, text):
    p = doc.add_paragraph(style="Heading 3")
    add_runs(p, text, size=11.5, color="2E4A62", bold=True, font=HEAD_FONT)
    return p


def add_para(doc, text, indent=True):
    p = doc.add_paragraph(style="Normal")
    p.alignment = WD_ALIGN_PARAGRAPH.JUSTIFY
    if indent:
        p.paragraph_format.first_line_indent = Cm(0.74)
    add_runs(p, text, size=10.5)
    return p


def add_bullets(doc, items):
    for it in items:
        p = doc.add_paragraph(style="Normal")
        p.paragraph_format.left_indent = Cm(0.7)
        p.paragraph_format.first_line_indent = Cm(-0.35)
        p.paragraph_format.space_after = Pt(2)
        p.alignment = WD_ALIGN_PARAGRAPH.LEFT
        add_runs(p, "• " + it, size=10.5)


def add_numbered(doc, items):
    for it in items:
        p = doc.add_paragraph(style="Normal")
        p.paragraph_format.left_indent = Cm(0.7)
        p.paragraph_format.first_line_indent = Cm(-0.35)
        p.paragraph_format.space_after = Pt(2)
        p.alignment = WD_ALIGN_PARAGRAPH.LEFT
        add_runs(p, it, size=10.5)


def add_code(doc, code):
    p = doc.add_paragraph()
    p.paragraph_format.left_indent = Cm(0.3)
    p.paragraph_format.space_before = Pt(6)
    p.paragraph_format.space_after = Pt(8)
    p.paragraph_format.line_spacing = 1.12
    shade_paragraph(p, CODE_BG)
    lines = code.split("\n")
    for i, line in enumerate(lines):
        r = p.add_run(line if line else " ")
        set_run_font(r, font=MONO_FONT, size=8.2, color="2B3A45")
        r._element.get_or_add_rPr().rFonts.set(qn("w:eastAsia"), "微软雅黑")
        if i < len(lines) - 1:
            r.add_break(WD_BREAK.LINE)


def add_quote(doc, text):
    table = doc.add_table(rows=1, cols=1)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    table.columns[0].width = Cm(16.2)
    cell = table.cell(0, 0)
    cell.width = Cm(16.2)
    shade_cell(cell, CALLOUT)
    cell_margins(cell, top=100, bottom=100, left=160, right=160)
    # left accent border on the cell
    tcPr = cell._tc.get_or_add_tcPr()
    borders = OxmlElement("w:tcBorders")
    left = OxmlElement("w:left")
    left.set(qn("w:val"), "single")
    left.set(qn("w:sz"), "30")
    left.set(qn("w:space"), "0")
    left.set(qn("w:color"), TEAL)
    borders.append(left)
    _insert(tcPr, borders, ("w:shd", "w:noWrap", "w:tcMar", "w:textDirection", "w:tcFitText", "w:vAlign", "w:hideMark"))
    p = cell.paragraphs[0]
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(0)
    p.paragraph_format.line_spacing = 1.45
    add_runs(p, text, size=9.6, color="1F3B4A")
    # spacing after
    sp = doc.add_paragraph()
    sp.paragraph_format.space_before = Pt(0)
    sp.paragraph_format.space_after = Pt(0)
    sp.paragraph_format.line_spacing = Pt(4)
    r = sp.add_run("")
    set_run_font(r, size=4, color="FFFFFF")


def add_table(doc, lines, keep_together=False):
    rows = parse_table(lines)
    if not rows:
        return
    n = len(rows[0])
    table = doc.add_table(rows=0, cols=n)
    table.alignment = WD_TABLE_ALIGNMENT.CENTER
    table.autofit = False
    table.allow_autofit = False
    table_borders(table)

    total = 16.2
    weights = []
    for c in range(n):
        mx = max(len(r[c]) for r in rows)
        weights.append(max(1.0, min(3.2, (mx ** 0.55) / 2.2)))
    s = sum(weights)
    widths = [total * w / s for w in weights]
    for ci, w in enumerate(widths):
        table.columns[ci].width = Cm(w)

    # fixed table width
    tblPr = table._tbl.tblPr
    tblW = OxmlElement("w:tblW")
    tblW.set(qn("w:w"), str(int(total * 567)))
    tblW.set(qn("w:type"), "dxa")
    _insert(tblPr, tblW, ("w:jc", "w:tblCellSpacing", "w:tblInd", "w:tblBorders", "w:shd", "w:tblLayout", "w:tblCellMar", "w:tblLook"))

    font_size = 9.0 if n <= 4 else (8.5 if n == 5 else 8.0)

    for ri, rowdata in enumerate(rows):
        row = table.add_row()
        row_no_split(row)
        row_height(row, 0.55 if ri else 0.65)
        if ri == 0:
            repeat_header(row)
        for ci, val in enumerate(rowdata):
            cell = row.cells[ci]
            cell.width = Cm(widths[ci])
            cell.vertical_alignment = WD_ALIGN_VERTICAL.CENTER
            cell_margins(cell, top=35, bottom=35, left=70, right=70)
            if ri == 0:
                shade_cell(cell, NAVY)
            elif ri % 2 == 0:
                shade_cell(cell, ZEBRA)
            p = cell.paragraphs[0]
            p.paragraph_format.space_before = Pt(0)
            p.paragraph_format.space_after = Pt(0)
            p.paragraph_format.line_spacing = 1.12
            p.alignment = WD_ALIGN_PARAGRAPH.CENTER if ri == 0 else WD_ALIGN_PARAGRAPH.LEFT
            add_runs(p, val, size=font_size, color="FFFFFF" if ri == 0 else "1F2933", bold=(ri == 0))

    # keep small captioned tables on one page
    if keep_together and len(rows) <= 11:
        for r in table.rows[:-1]:
            for c in r.cells:
                for p in c.paragraphs:
                    p.paragraph_format.keep_with_next = True

    # spacing after
    sp = doc.add_paragraph()
    sp.paragraph_format.space_before = Pt(0)
    sp.paragraph_format.space_after = Pt(0)
    sp.paragraph_format.line_spacing = Pt(5)
    r = sp.add_run("")
    set_run_font(r, size=5, color="FFFFFF")


def add_image(doc, alt, rel):
    p = doc.add_paragraph()
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    p.paragraph_format.space_before = Pt(6)
    p.paragraph_format.space_after = Pt(2)
    p.paragraph_format.keep_with_next = True
    run = p.add_run()
    run.add_picture(str(ASSETS / Path(rel).name), width=Cm(16.2))
    cap = doc.add_paragraph()
    cap.alignment = WD_ALIGN_PARAGRAPH.CENTER
    cap.paragraph_format.space_before = Pt(0)
    cap.paragraph_format.space_after = Pt(10)
    add_runs(cap, alt, size=9, color=GRAY)


def add_toc(doc, headings, pages):
    p = doc.add_paragraph()
    p.paragraph_format.page_break_before = True
    p.paragraph_format.space_before = Pt(0)
    p.paragraph_format.space_after = Pt(2)
    p.alignment = WD_ALIGN_PARAGRAPH.CENTER
    r = p.add_run("目  录")
    set_run_font(r, font=HEAD_FONT, size=22, bold=True, color=NAVY)

    sub = doc.add_paragraph()
    sub.alignment = WD_ALIGN_PARAGRAPH.CENTER
    sub.paragraph_format.space_before = Pt(0)
    sub.paragraph_format.space_after = Pt(12)
    r = sub.add_run("C O N T E N T S")
    set_run_font(r, font=HEAD_FONT, size=8.5, color=LIGHT_GRAY)

    for level, text in headings:
        entry = doc.add_paragraph()
        pf = entry.paragraph_format
        pf.tab_stops.add_tab_stop(Cm(16.2), WD_TAB_ALIGNMENT.RIGHT, WD_TAB_LEADER.DOTS)
        pf.space_before = Pt(3 if level == 1 else 0)
        pf.space_after = Pt(0)
        pf.line_spacing = 1.05
        if level == 1:
            pf.left_indent = Cm(0)
        else:
            pf.left_indent = Cm(0.65)
        r = entry.add_run(text)
        set_run_font(r, font=HEAD_FONT if level == 1 else BODY_FONT,
                     size=9.8 if level == 1 else 8.8,
                     bold=(level == 1), color=NAVY if level == 1 else "2E4A62")
        r = entry.add_run("\t")
        set_run_font(r, font=BODY_FONT, size=8.8, color=GRAY)
        page = str(pages.get(normalize(text), "00"))
        r = entry.add_run(page)
        set_run_font(r, font=BODY_FONT, size=8.8, bold=(level == 1), color=NAVY if level == 1 else GRAY)


def render_blocks(doc, blocks, start=0, first_h1_no_break=True):
    first_h1 = True
    prev = None
    for kind, payload in blocks[start:]:
        if kind == "h1":
            add_h1(doc, payload, first=(first_h1 and first_h1_no_break))
            first_h1 = False
        elif kind == "h2":
            add_h2(doc, payload)
        elif kind == "h3":
            add_h3(doc, payload)
        elif kind == "p":
            add_para(doc, payload)
        elif kind == "ul":
            add_bullets(doc, payload)
        elif kind == "ol":
            add_numbered(doc, payload)
        elif kind == "code":
            add_code(doc, payload)
        elif kind == "quote":
            add_quote(doc, payload)
        elif kind == "table":
            # keep a short caption/lead-in paragraph with its table
            keep = bool(prev and prev[0] == "p" and len(prev[1]) <= 40)
            if keep and doc.paragraphs:
                doc.paragraphs[-1].paragraph_format.keep_with_next = True
            add_table(doc, payload, keep_together=keep)
        elif kind == "image":
            add_image(doc, payload[0], payload[1])
        prev = (kind, payload)


def build_full(pages, out_path):
    blocks, headings = parse_markdown([
        SRC / "part1.md", SRC / "part2.md", SRC / "part3.md", SRC / "part4.md",
    ])
    h1_indices = [i for i, (k, _) in enumerate(blocks) if k == "h1"]
    body_start = h1_indices[1] if len(h1_indices) > 1 else len(blocks)

    doc = Document()
    setup_styles(doc)
    add_cover(doc)
    setup_body_section(doc)

    # 摘要
    render_blocks(doc, blocks[:body_start], 0, first_h1_no_break=True)
    # 目录
    add_toc(doc, headings, pages)
    # 正文
    render_blocks(doc, blocks[body_start:], 0, first_h1_no_break=False)

    props = doc.core_properties
    props.title = "“阿姨你好”安卓端离线亲属称呼助手技术调研报告"
    props.author = "调研团队"
    props.subject = "端侧人脸识别、目标跟踪与亲属称呼推理的可行性研究"
    props.keywords = "安卓, 端侧AI, 人脸识别, 目标检测, 多目标跟踪, 亲属称呼, 隐私合规"

    doc.save(str(out_path))


# ------------------------------------------------------------------ pdf / mapping
NODE = r"D:\Program Files\DeepSeek Harness\resources\runtime\primary-runtime\dependencies\node\bin\node.exe"
CLI = r"D:\Program Files\DeepSeek Harness\resources\app.asar.unpacked\dsh\node_modules\@deepseek-ai\libreoffice-kit\lib\cli.js"
PDFTOTEXT = r"D:\Program Files\MiKTeX\miktex\bin\x64\pdftotext.exe"


def to_pdf(docx_path, pdf_path):
    if Path(pdf_path).exists():
        Path(pdf_path).unlink()
    r = subprocess.run([NODE, CLI, "convert", "--input", str(docx_path), "--output", str(pdf_path)],
                       capture_output=True, text=True, encoding="utf-8", errors="replace")
    if not Path(pdf_path).exists():
        raise RuntimeError(f"PDF conversion failed: {r.stdout} {r.stderr}")
    return r.stdout


def extract_pages(pdf_path, txt_path):
    r = subprocess.run([PDFTOTEXT, "-layout", str(pdf_path), str(txt_path)],
                       capture_output=True, text=True, errors="ignore")
    text = Path(txt_path).read_text(encoding="utf-8", errors="ignore")
    return text.split("\f")


def map_pages(pdf_path, headings):
    pages = extract_pages(pdf_path, OUT / "_pages.txt")
    norm_pages = [normalize(p) for p in pages]
    # body starts at the first page containing 第1章 but not the TOC (dot leaders)
    body_start = 0
    for i, p in enumerate(norm_pages):
        if "第1章项目背景与需求定义" in p and "......" not in p:
            body_start = i
            break
    mapping = {}
    for level, text in headings:
        key = normalize(text)
        if key == "摘要":
            mapping[key] = 2
            continue
        found = False
        for i in range(body_start, len(norm_pages)):
            if key and key in norm_pages[i]:
                mapping[key] = i + 1
                found = True
                break
        if not found:
            for i in range(0, body_start):
                if "......" not in norm_pages[i] and key and key in norm_pages[i]:
                    mapping[key] = i + 1
                    break
    return mapping, len(pages)


def main():
    OUT.mkdir(parents=True, exist_ok=True)
    tmp = OUT / "_pass1.docx"
    tmp_pdf = OUT / "_pass1.pdf"
    build_full({}, tmp)
    to_pdf(tmp, tmp_pdf)
    # headings for mapping
    _, headings = parse_markdown([SRC / "part1.md", SRC / "part2.md", SRC / "part3.md", SRC / "part4.md"])
    pages, count = map_pages(tmp_pdf, headings)
    print(f"pass1 pages={count}, mapped={len(pages)}/{len(headings)}")
    missing = [t for _, t in headings if normalize(t) not in pages]
    if missing:
        print("missing:", missing[:10])

    final_docx = OUT / "阿姨你好_技术调研报告_v1.0.docx"
    final_pdf = OUT / "阿姨你好_技术调研报告_v1.0.pdf"
    build_full(pages, final_docx)
    to_pdf(final_docx, final_pdf)
    print("built:", final_docx)
    print("pdf:", final_pdf)


if __name__ == "__main__":
    main()
