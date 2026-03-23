#!/usr/bin/env python3
"""Build a book-like companion PDF from the repository docs.

This script uses only the Python standard library so it can run in a minimal
developer environment. It renders a simple, readable PDF with:

- a title page
- a table of contents
- chapter-style sections for the companion docs
- a final appendix for the Python-to-Quarkus mapping

The goal is not a perfect typeset replica of the original book. Instead, the
output is a compact companion reference that feels book-like and is easy to
regenerate as the repo evolves.
"""

from __future__ import annotations

import re
from dataclasses import dataclass, field
from pathlib import Path
from typing import Iterable


PAGE_WIDTH = 432.0  # 6 inches
PAGE_HEIGHT = 648.0  # 9 inches
LEFT_MARGIN = 40.0
RIGHT_MARGIN = 40.0
TOP_MARGIN = 42.0
BOTTOM_MARGIN = 38.0
CONTENT_WIDTH = PAGE_WIDTH - LEFT_MARGIN - RIGHT_MARGIN
BODY_FONT = "Helvetica"
BODY_BOLD = "Helvetica-Bold"
BODY_ITALIC = "Helvetica-Oblique"
MONO_FONT = "Courier"

TITLE_SIZE = 24
SUBTITLE_SIZE = 12
SECTION_TITLE_SIZE = 18
SUBSECTION_SIZE = 13
BODY_SIZE = 10.6
BODY_LEADING = 14.0
SMALL_SIZE = 8.5
CODE_SIZE = 8.9
CODE_LEADING = 11.0

TITLE_LINE = "Build an AI Agent from Scratch - Quarkus Edition"
SUBTITLE_LINE = "A book-like companion PDF generated from the Quarkus/Java reference implementation"


@dataclass
class Block:
    kind: str
    text: str = ""
    level: int = 0
    items: list[str] = field(default_factory=list)
    lines: list[str] = field(default_factory=list)


@dataclass
class Section:
    title: str
    source: Path
    category: str
    blocks: list[Block]
    page_start: int = 0


@dataclass
class Page:
    number: int
    section_title: str
    commands: list[str] = field(default_factory=list)
    cursor_y: float = PAGE_HEIGHT - TOP_MARGIN

    def add_raw(self, command: str) -> None:
        self.commands.append(command)

    def add_text(self, x: float, y: float, text: str, font: str, size: float) -> None:
        self.commands.append(
            f"BT /{font} {size:.2f} Tf {x:.2f} {y:.2f} Td ({escape_pdf_text(text)}) Tj ET"
        )

    def add_rule(self, y: float, thickness: float = 0.8) -> None:
        self.commands.append(
            f"0.78 0.78 0.78 RG {thickness:.2f} w {LEFT_MARGIN:.2f} {y:.2f} m {PAGE_WIDTH - RIGHT_MARGIN:.2f} {y:.2f} l S"
        )

    def add_box(self, x: float, y: float, width: float, height: float, gray: float = 0.94) -> None:
        self.commands.append(
            f"{gray:.2f} g {x:.2f} {y:.2f} {width:.2f} {height:.2f} re f 0 G"
        )


class PdfDocument:
    def __init__(self) -> None:
        self.content_pages: list[Page] = []

    def new_page(self, number: int, section_title: str) -> Page:
        page = Page(number=number, section_title=section_title)
        self._draw_running_header(page)
        self.content_pages.append(page)
        return page

    def _draw_running_header(self, page: Page) -> None:
        top_y = PAGE_HEIGHT - TOP_MARGIN + 12
        page.add_text(LEFT_MARGIN, top_y, page.section_title, BODY_BOLD, 8.4)
        page.add_rule(top_y - 6, 0.55)

    def _new_section_page(self, section: Section) -> Page:
        number = len(self.content_pages) + 1
        page = self.new_page(number, section.title)
        self._draw_section_intro(page, section)
        return page

    def _draw_section_intro(self, page: Page, section: Section) -> None:
        page.add_text(LEFT_MARGIN, page.cursor_y, section.title, BODY_BOLD, SECTION_TITLE_SIZE)
        page.cursor_y -= 22
        page.add_text(
            LEFT_MARGIN,
            page.cursor_y,
            f"Source: {section.source.as_posix()}",
            BODY_ITALIC,
            SMALL_SIZE,
        )
        page.cursor_y -= 18
        page.add_rule(page.cursor_y, 0.7)
        page.cursor_y -= 18

    def render(self, output_path: Path, sections: list[Section]) -> None:
        for section in sections:
            page = self._new_section_page(section)
            page = self._render_blocks(page, section.blocks)
            section.page_start = page.number
        for page in self.content_pages:
            self._draw_page_number(page)
        cover = self._build_cover_page()
        toc = self._build_toc_page(sections)
        all_pages = [cover, toc, *self.content_pages]
        output_path.parent.mkdir(parents=True, exist_ok=True)
        pdf_bytes = build_pdf_bytes(all_pages)
        output_path.write_bytes(pdf_bytes)

    def _render_blocks(self, page: Page, blocks: list[Block]) -> Page:
        for block in blocks:
            if block.kind == "heading":
                page = self._add_heading(page, block.level, block.text)
            elif block.kind == "paragraph":
                page = self._add_paragraph(page, block.text)
            elif block.kind == "bullets":
                page = self._add_bullets(page, block.items)
            elif block.kind == "code":
                page = self._add_code_block(page, block.lines)
            elif block.kind == "quote":
                page = self._add_quote(page, block.text)
        return page

    def _ensure_space(self, page: Page, needed: float) -> Page:
        if page.cursor_y - needed < BOTTOM_MARGIN:
            page = self.new_page(len(self.content_pages) + 1, page.section_title)
            page.cursor_y = PAGE_HEIGHT - TOP_MARGIN - 4
        return page

    def _add_heading(self, page: Page, level: int, text: str) -> Page:
        size_map = {1: 15.2, 2: 13.4, 3: 12.2}
        size = size_map.get(level, 11.6)
        needed = size + 14
        page = self._ensure_space(page, needed)
        page.add_text(LEFT_MARGIN, page.cursor_y, text, BODY_BOLD, size)
        page.cursor_y -= needed
        return page

    def _add_paragraph(self, page: Page, text: str) -> Page:
        wrapped = wrap_text(normalize_inline(text), BODY_SIZE, CONTENT_WIDTH)
        needed = len(wrapped) * BODY_LEADING + 4
        page = self._ensure_space(page, needed)
        for line in wrapped:
            if page.cursor_y < BOTTOM_MARGIN + BODY_LEADING:
                page = self.new_page(len(self.content_pages) + 1, page.section_title)
                page.cursor_y = PAGE_HEIGHT - TOP_MARGIN - 4
            page.add_text(LEFT_MARGIN, page.cursor_y, line, BODY_FONT, BODY_SIZE)
            page.cursor_y -= BODY_LEADING
        page.cursor_y -= 4
        return page

    def _add_quote(self, page: Page, text: str) -> Page:
        wrapped = wrap_text(normalize_inline(text), BODY_SIZE, CONTENT_WIDTH - 18)
        needed = len(wrapped) * BODY_LEADING + 8
        page = self._ensure_space(page, needed)
        box_height = needed - 4
        page.add_box(LEFT_MARGIN - 4, page.cursor_y - box_height + 4, CONTENT_WIDTH + 8, box_height, 0.96)
        page.cursor_y -= 2
        for line in wrapped:
            page.add_text(LEFT_MARGIN + 8, page.cursor_y, line, BODY_ITALIC, BODY_SIZE)
            page.cursor_y -= BODY_LEADING
        page.cursor_y -= 2
        return page

    def _add_bullets(self, page: Page, items: list[str]) -> Page:
        for item in items:
            wrapped = wrap_text(normalize_inline(item), BODY_SIZE, CONTENT_WIDTH - 22)
            needed = len(wrapped) * BODY_LEADING + 2
            page = self._ensure_space(page, needed)
            first = True
            for line in wrapped:
                prefix = "• " if first else "  "
                page.add_text(LEFT_MARGIN + 8, page.cursor_y, prefix + line, BODY_FONT, BODY_SIZE)
                page.cursor_y -= BODY_LEADING
                first = False
            page.cursor_y -= 2
        page.cursor_y -= 2
        return page

    def _add_code_block(self, page: Page, lines: list[str]) -> Page:
        if not lines:
            return page
        prepared = [line.rstrip("\n") for line in lines]
        needed = len(prepared) * CODE_LEADING + 14
        page = self._ensure_space(page, needed)
        block_height = needed - 2
        page.add_box(LEFT_MARGIN - 4, page.cursor_y - block_height + 4, CONTENT_WIDTH + 8, block_height, 0.95)
        page.cursor_y -= 6
        for line in prepared:
            page.add_text(LEFT_MARGIN + 6, page.cursor_y, line, MONO_FONT, CODE_SIZE)
            page.cursor_y -= CODE_LEADING
        page.cursor_y -= 4
        return page

    def _draw_page_number(self, page: Page) -> None:
        x = PAGE_WIDTH / 2 - 4
        page.add_text(x, 20, str(page.number), BODY_FONT, 8.8)

    def _build_cover_page(self) -> Page:
        page = Page(number=0, section_title="Cover")
        page.add_raw("0 0 0 rg")
        page.add_text(LEFT_MARGIN, 540, "Build an AI", BODY_BOLD, 20)
        page.add_text(LEFT_MARGIN, 512, "Agent from Scratch", BODY_BOLD, 24)
        page.add_text(LEFT_MARGIN, 474, "Quarkus Edition", BODY_BOLD, 20)
        page.add_rule(450, 1.2)
        page.add_text(LEFT_MARGIN, 424, SUBTITLE_LINE, BODY_FONT, 12)
        page.add_text(LEFT_MARGIN, 392, "Java 21  |  Quarkus  |  Maven", BODY_FONT, 11)
        page.add_text(LEFT_MARGIN, 350, "Companion PDF compiled from the chapter docs", BODY_FONT, 10.8)
        page.add_text(LEFT_MARGIN, 312, "Included inside:", BODY_BOLD, 11.5)
        bullets = [
            "Chapter-by-chapter Quarkus translations of the Python reference zip",
            "Architecture notes and Python-to-Quarkus mapping",
            "Demo-first implementations with explicit production placeholders",
            "RAG, memory, planning, code agents, multi-agent routing, and evaluation",
        ]
        y = 292
        for bullet in bullets:
            page.add_text(LEFT_MARGIN + 12, y, f"• {bullet}", BODY_FONT, 10.2)
            y -= 20
        page.add_text(LEFT_MARGIN, 112, "Generated from the repository docs", BODY_ITALIC, 9.5)
        page.add_text(LEFT_MARGIN, 92, "Source repository: quarkus-agent-edition", BODY_FONT, 9.2)
        page.add_text(LEFT_MARGIN, 72, "Companion edition, not original book code", BODY_BOLD, 9.2)
        return page

    def _build_toc_page(self, sections: list[Section]) -> Page:
        page = Page(number=0, section_title="Contents")
        page.add_text(LEFT_MARGIN, 582, "Contents", BODY_BOLD, 18)
        page.add_rule(566, 0.9)
        y = 540
        for section in sections:
            display = f"{section.title}"
            if section.category == "appendix":
                display = f"Appendix - {display}"
            page.add_text(LEFT_MARGIN, y, display, BODY_FONT, 10.6)
            page.add_text(360, y, str(section.page_start), BODY_BOLD, 10.6)
            y -= 20
            if y < 58:
                break
        page.add_text(LEFT_MARGIN, 42, "Page numbers refer to the content pages after the front matter.", BODY_ITALIC, 8.6)
        return page


def normalize_inline(text: str) -> str:
    text = re.sub(r"\[(.*?)\]\((.*?)\)", r"\1 (\2)", text)
    text = re.sub(r"`([^`]*)`", r"\1", text)
    text = re.sub(r"\*\*(.*?)\*\*", r"\1", text)
    text = re.sub(r"\*(.*?)\*", r"\1", text)
    return text.strip()


def wrap_text(text: str, font_size: float, max_width: float) -> list[str]:
    if not text:
        return [""]
    words = text.split()
    lines: list[str] = []
    current = ""
    for word in words:
        candidate = word if not current else f"{current} {word}"
        if estimate_width(candidate, font_size) <= max_width:
            current = candidate
            continue
        if current:
            lines.append(current)
        if estimate_width(word, font_size) <= max_width:
            current = word
        else:
            chunks = break_long_word(word, font_size, max_width)
            if chunks:
                lines.extend(chunks[:-1])
                current = chunks[-1]
            else:
                current = word
    if current:
        lines.append(current)
    return lines


def break_long_word(word: str, font_size: float, max_width: float) -> list[str]:
    chunks: list[str] = []
    current = ""
    for char in word:
        candidate = current + char
        if estimate_width(candidate, font_size) <= max_width or not current:
            current = candidate
        else:
            chunks.append(current)
            current = char
    if current:
        chunks.append(current)
    return chunks


def estimate_width(text: str, font_size: float) -> float:
    total = 0.0
    for char in text:
        if char == " ":
            total += 0.28
        elif char in "ilI1|!.,;:'`":
            total += 0.23
        elif char in "frt":
            total += 0.31
        elif char in "mwMW@#":
            total += 0.86
        elif char.isupper():
            total += 0.62
        elif char.isdigit():
            total += 0.55
        else:
            total += 0.49
    return total * font_size


def escape_pdf_text(text: str) -> str:
    return text.replace("\\", "\\\\").replace("(", "\\(").replace(")", "\\)")


def parse_markdown_doc(path: Path, category: str, title_override: str | None = None) -> Section:
    raw_lines = path.read_text(encoding="utf-8").splitlines()
    blocks: list[Block] = []
    title = title_override or path.stem
    current_paragraph: list[str] = []
    current_bullets: list[str] = []
    current_code: list[str] = []
    current_quote: list[str] = []
    in_code = False

    def flush_paragraph() -> None:
        nonlocal current_paragraph
        if current_paragraph:
            blocks.append(Block(kind="paragraph", text=" ".join(current_paragraph).strip()))
            current_paragraph = []

    def flush_bullets() -> None:
        nonlocal current_bullets
        if current_bullets:
            blocks.append(Block(kind="bullets", items=current_bullets))
            current_bullets = []

    def flush_code() -> None:
        nonlocal current_code
        if current_code:
            blocks.append(Block(kind="code", lines=current_code))
            current_code = []

    def flush_quote() -> None:
        nonlocal current_quote
        if current_quote:
            blocks.append(Block(kind="quote", text=" ".join(current_quote).strip()))
            current_quote = []

    for line in raw_lines:
        stripped = line.rstrip()
        if stripped.startswith("```"):
            if in_code:
                flush_code()
                in_code = False
            else:
                flush_paragraph()
                flush_bullets()
                flush_quote()
                in_code = True
            continue
        if in_code:
            current_code.append(line)
            continue

        heading = re.match(r"^(#{1,6})\s+(.*)$", stripped)
        bullet = re.match(r"^\s*-\s+(.*)$", stripped)
        quote = re.match(r"^\s*>\s?(.*)$", stripped)

        if heading:
            flush_paragraph()
            flush_bullets()
            flush_quote()
            level = len(heading.group(1))
            text = normalize_inline(heading.group(2))
            if level == 1 and (title_override is None or text == title_override):
                title = text
                continue
            blocks.append(Block(kind="heading", level=level, text=text))
            continue
        if bullet:
            flush_paragraph()
            flush_quote()
            current_bullets.append(bullet.group(1).strip())
            continue
        if quote:
            flush_paragraph()
            flush_bullets()
            current_quote.append(quote.group(1).strip())
            continue
        if not stripped.strip():
            flush_paragraph()
            flush_bullets()
            flush_quote()
            continue
        flush_bullets()
        flush_quote()
        current_paragraph.append(normalize_inline(stripped))

    flush_paragraph()
    flush_bullets()
    flush_quote()
    flush_code()
    if title_override is not None:
        title = title_override
    return Section(title=title, source=path, category=category, blocks=blocks)


def build_pdf_bytes(pages: list[Page]) -> bytes:
    font_ids = {"F1": 3, "F2": 4, "F3": 5, "F4": 6}
    content_ids: list[int] = []
    page_ids: list[int] = []
    object_bodies: dict[int, bytes] = {
        1: b"<< /Type /Catalog /Pages 2 0 R >>",
        3: b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>",
        4: b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Bold >>",
        5: b"<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica-Oblique >>",
        6: b"<< /Type /Font /Subtype /Type1 /BaseFont /Courier >>",
    }

    next_id = 7
    for page in pages:
        content_id = next_id
        page_id = next_id + 1
        next_id += 2
        content_ids.append(content_id)
        page_ids.append(page_id)
        stream = "\n".join(page.commands).encode("cp1252")
        object_bodies[content_id] = (
            b"<< /Length "
            + str(len(stream)).encode("latin-1")
            + b" >>\nstream\n"
            + stream
            + b"\nendstream"
        )
        object_bodies[page_id] = (
            f"<< /Type /Page /Parent 2 0 R /MediaBox [0 0 {PAGE_WIDTH:.2f} {PAGE_HEIGHT:.2f}] "
            f"/Resources << /Font << /F1 {font_ids['F1']} 0 R /F2 {font_ids['F2']} 0 R "
            f"/F3 {font_ids['F3']} 0 R /F4 {font_ids['F4']} 0 R >> >> "
            f"/Contents {content_id} 0 R >>"
        ).encode("cp1252")

    object_bodies[2] = (
        "<< /Type /Pages /Kids [ "
        + " ".join(f"{page_id} 0 R" for page_id in page_ids)
        + f" ] /Count {len(page_ids)} >>"
    ).encode("latin-1")

    info_id = next_id
    object_bodies[info_id] = (
        "<< /Producer (Codex standard library PDF builder) /Title (Quarkus Edition Companion PDF) "
        "/Author (Codex) /Subject (Book-like companion reference) >>"
    ).encode("cp1252")

    buffer = bytearray()
    buffer.extend(b"%PDF-1.4\n%\xe2\xe3\xcf\xd3\n")
    offsets: list[int] = [0]
    for obj_num in range(1, info_id + 1):
        body = object_bodies[obj_num]
        offsets.append(len(buffer))
        buffer.extend(f"{obj_num} 0 obj\n".encode("cp1252"))
        buffer.extend(body)
        buffer.extend(b"\nendobj\n")
    xref_pos = len(buffer)
    total_objects = info_id
    buffer.extend(f"xref\n0 {total_objects + 1}\n".encode("cp1252"))
    buffer.extend(b"0000000000 65535 f \n")
    for offset in offsets[1:]:
        buffer.extend(f"{offset:010d} 00000 n \n".encode("cp1252"))
    buffer.extend(
        (
            "trailer\n"
            f"<< /Size {total_objects + 1} /Root 1 0 R /Info {info_id} 0 R >>\n"
            f"startxref\n{xref_pos}\n%%EOF\n"
        ).encode("cp1252")
    )
    return bytes(buffer)


def build_sections(repo_root: Path) -> list[Section]:
    docs = repo_root / "docs"
    sections = [
        parse_markdown_doc(repo_root / "README.md", "preface", "Build an AI Agent from Scratch - Quarkus Edition"),
        parse_markdown_doc(docs / "architecture.md", "overview"),
        parse_markdown_doc(docs / "chapter-status.md", "overview"),
        parse_markdown_doc(docs / "chapter-01-agent-vs-workflow.md", "chapter"),
        parse_markdown_doc(docs / "chapter-02-llm.md", "chapter"),
        parse_markdown_doc(docs / "chapter-03-tool-use.md", "chapter"),
        parse_markdown_doc(docs / "chapter-04-basic-agent.md", "chapter"),
        parse_markdown_doc(docs / "chapter-05-rag.md", "chapter"),
        parse_markdown_doc(docs / "chapter-06-memory.md", "chapter"),
        parse_markdown_doc(docs / "chapter-07-planning-reflection.md", "chapter"),
        parse_markdown_doc(docs / "chapter-08-code-agents.md", "chapter"),
        parse_markdown_doc(docs / "chapter-09-multi-agent.md", "chapter"),
        parse_markdown_doc(docs / "chapter-10-evaluation-monitoring.md", "chapter"),
        parse_markdown_doc(docs / "python-to-quarkus-mapping.md", "appendix"),
    ]
    return sections


def main() -> int:
    repo_root = Path(__file__).resolve().parents[1]
    output_path = repo_root / "target" / "quarkus-agent-edition-companion.pdf"
    sections = build_sections(repo_root)
    document = PdfDocument()
    document.render(output_path, sections)
    print(f"Wrote {output_path}")
    print(f"Sections: {len(sections)}")
    print(f"Content pages: {len(document.content_pages)}")
    return 0


if __name__ == "__main__":
    raise SystemExit(main())
