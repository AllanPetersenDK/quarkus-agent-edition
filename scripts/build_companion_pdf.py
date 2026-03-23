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
import zlib
from dataclasses import dataclass, field
from functools import lru_cache
from pathlib import Path
from typing import Iterable


PAGE_WIDTH = 432.0  # 6 inches
PAGE_HEIGHT = 648.0  # 9 inches
LEFT_MARGIN = 42.0
RIGHT_MARGIN = 42.0
TOP_MARGIN = 46.0
BOTTOM_MARGIN = 40.0
CONTENT_WIDTH = PAGE_WIDTH - LEFT_MARGIN - RIGHT_MARGIN
BODY_FONT = "Times-Roman"
BODY_BOLD = "Times-Bold"
BODY_ITALIC = "Times-Italic"
MONO_FONT = "Courier"

TITLE_SIZE = 25
SUBTITLE_SIZE = 12
SECTION_TITLE_SIZE = 19
SUBSECTION_SIZE = 13
BODY_SIZE = 10.7
BODY_LEADING = 14.4
SMALL_SIZE = 8.4
CODE_SIZE = 8.9
CODE_LEADING = 11.0

TITLE_LINE = "Build an AI Agent (From Scratch)"
SUBTITLE_LINE = "Quarkus Edition"
SUBTITLE_DESC = "A companion PDF generated from the Quarkus/Java reference implementation"
SMALL_CAPS_LINE = "Companion Edition"
BOOK_AUTHOR_LINE = "Jungjun Hur and Younghee Song"
BOOK_SUBJECT_LINE = "MEAP V03"
PDF_PRODUCER_LINE = "Codex standard library PDF builder"


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
        page.add_text(LEFT_MARGIN, top_y, page.section_title, BODY_BOLD, 8.2)
        page.add_text(PAGE_WIDTH - RIGHT_MARGIN - 30, top_y, str(page.number), BODY_BOLD, 8.2)
        page.add_rule(top_y - 6, 0.55)

    def _new_section_page(self, section: Section) -> Page:
        number = len(self.content_pages) + 1
        page = self.new_page(number, section.title)
        self._draw_section_intro(page, section)
        return page

    def _draw_section_intro(self, page: Page, section: Section) -> None:
        if section.category == "chapter":
            chapter_label = self._chapter_label(section.title)
            if chapter_label:
                page.add_text(LEFT_MARGIN, page.cursor_y + 24, chapter_label, BODY_BOLD, 9.0)
        page.add_text(LEFT_MARGIN, page.cursor_y, section.title, BODY_BOLD, SECTION_TITLE_SIZE)
        page.cursor_y -= 24
        page.add_text(
            LEFT_MARGIN,
            page.cursor_y,
            f"Source: {section.source.as_posix()}",
            BODY_ITALIC,
            SMALL_SIZE,
        )
        page.cursor_y -= 20
        page.add_rule(page.cursor_y, 0.7)
        page.cursor_y -= 20

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
        page.add_box(0, 0, PAGE_WIDTH, PAGE_HEIGHT, 0.98)
        page.add_box(0, 498, PAGE_WIDTH, 150, 0.89)
        page.add_text(LEFT_MARGIN, 578, SMALL_CAPS_LINE, BODY_BOLD, 10)
        page.add_text(LEFT_MARGIN, 544, TITLE_LINE, BODY_BOLD, 24)
        page.add_text(LEFT_MARGIN, 510, SUBTITLE_LINE, BODY_BOLD, 18)
        page.add_rule(488, 1.2)
        page.add_text(LEFT_MARGIN, 456, SUBTITLE_DESC, BODY_FONT, 12)
        page.add_text(LEFT_MARGIN, 430, "Java 21  |  Quarkus  |  Maven", BODY_BOLD, 10.8)
        page.add_text(LEFT_MARGIN, 398, "Companion PDF compiled from the chapter docs", BODY_FONT, 10.5)
        page.add_text(LEFT_MARGIN, 374, BOOK_AUTHOR_LINE, BODY_ITALIC, 10.3)
        page.add_text(LEFT_MARGIN, 360, BOOK_SUBJECT_LINE, BODY_FONT, 9.6)
        page.add_text(LEFT_MARGIN, 334, "Included inside:", BODY_BOLD, 11.3)
        bullets = [
            "Chapter-by-chapter Quarkus translations of the Python reference zip",
            "Architecture notes and Python-to-Quarkus mapping",
            "Demo-first implementations with explicit production placeholders",
            "RAG, memory, planning, code agents, multi-agent routing, and evaluation",
        ]
        y = 314
        for bullet in bullets:
            page.add_text(LEFT_MARGIN + 12, y, f"• {bullet}", BODY_FONT, 10.1)
            y -= 20
        page.add_text(LEFT_MARGIN, 124, "Generated from the repository docs", BODY_ITALIC, 9.4)
        page.add_text(LEFT_MARGIN, 106, "Reference book materials are included in docs/book", BODY_FONT, 8.9)
        page.add_text(LEFT_MARGIN, 88, "Source repository: quarkus-agent-edition", BODY_FONT, 9.0)
        page.add_text(LEFT_MARGIN, 68, "Companion edition, not original book code", BODY_BOLD, 9.0)
        return page

    def _build_toc_page(self, sections: list[Section]) -> Page:
        page = Page(number=0, section_title="Contents")
        page.add_text(LEFT_MARGIN, 582, "Contents", BODY_BOLD, 18)
        page.add_rule(566, 0.9)
        y = 540
        for section in sections:
            display = self._toc_label(section)
            page.add_text(LEFT_MARGIN, y, display, BODY_FONT, 10.4)
            page.add_text(320, y, self._toc_leader(display, section.page_start), BODY_FONT, 10.4)
            page.add_text(365, y, str(section.page_start), BODY_BOLD, 10.4)
            y -= 19
            if y < 58:
                break
        page.add_text(LEFT_MARGIN, 42, "Page numbers refer to the content pages after the front matter.", BODY_ITALIC, 8.6)
        return page

    def _toc_label(self, section: Section) -> str:
        if section.category == "preface":
            return section.title
        if section.category == "chapter":
            match = re.match(r"Chapter\s+(\d+)\s*[-–]\s*(.*)", section.title)
            if match:
                return f"Chapter {match.group(1)}  {match.group(2)}"
        if section.category == "appendix":
            return f"Appendix  {section.title}"
        return section.title

    def _toc_leader(self, label: str, page_number: int) -> str:
        leader_len = max(8, 44 - len(label))
        return "." * leader_len

    def _chapter_label(self, title: str) -> str:
        match = re.match(r"Chapter\s+(\d+)\s*[-–]\s*(.*)", title)
        if not match:
            return ""
        return f"Chapter {match.group(1)}"


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
        f"<< /Producer ({PDF_PRODUCER_LINE}) /Title ({TITLE_LINE} - Quarkus Edition) "
        f"/Author ({BOOK_AUTHOR_LINE}) /Subject (Book-like companion reference based on {BOOK_SUBJECT_LINE}) >>"
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
    book_dir = docs / "book"
    book_pdf = book_dir / "Build_an_AI_Agent_(From_Scratch)_v3_MEAP.pdf"
    book_meta = read_pdf_metadata(book_pdf)
    book_samples = extract_book_samples(book_pdf)
    source_repo_url = "https://github.com/shangrilar/ai-agent-from-scratch"
    reference_materials = Section(
        title="Source Book Snapshot",
        source=book_dir,
        category="preface",
        blocks=[
            Block(
                kind="paragraph",
                text=(
                    "This companion PDF is derived from the local book PDF in docs/book and the "
                    "official Python reference zip. The original source code is available from "
                    f"{source_repo_url}. The Quarkus edition rewrites the learning path with Java 21 "
                    "and Quarkus-native examples."
                ),
            ),
            Block(
                kind="bullets",
                items=[
                    f"Title: {book_meta.get('Title', 'Build an AI Agent (From Scratch) MEAP V03')}",
                    f"Author(s): {book_meta.get('Author', 'Jungjun Hur and Younghee Song')}",
                    f"Subject: {book_meta.get('Subject', 'MEAP V03')}",
                    "docs/book/Build_an_AI_Agent_(From_Scratch)_v3_MEAP.pdf - source book PDF",
                    "docs/book/ai-agent-from-scratch-main.zip - official Python source reference",
                ],
            ),
            Block(
                kind="heading",
                level=2,
                text="How the Rewrite Works",
            ),
            Block(
                kind="paragraph",
                text=(
                    "The book PDF defines the learning order, and the Python zip defines the concrete "
                    "source files. The Quarkus edition keeps that progression, then rewrites the "
                    "examples into Java classes, Quarkus REST resources, CDI beans, and companion "
                    "modules."
                ),
            ),
        ],
    )
    how_to_use = Section(
        title="How to Use This Companion",
        source=repo_root / "README.md",
        category="preface",
        blocks=[
            Block(
                kind="paragraph",
                text=(
                    "Read this companion in chapter order if you want the same learning progression "
                    "as the book. Use the README for the practical Quarkus run instructions and use "
                    "the mapping appendix when you want to jump from Python filenames to Java classes."
                ),
            ),
            Block(
                kind="bullets",
                items=[
                    "Chapter docs explain the Quarkus translation and the design tradeoffs.",
                    "The companion modules extend the book with Quarkus-native features such as RAG and evaluation.",
                    "Demo and fake implementations are clearly marked so you can distinguish learning code from placeholders.",
                ],
            ),
        ],
    )
    chapter_specs = [
        (parse_markdown_doc(repo_root / "README.md", "preface", "Build an AI Agent from Scratch - Quarkus Edition"), []),
        (parse_markdown_doc(docs / "architecture.md", "overview"), ["architecture", "agent", "workflow"]),
        (parse_markdown_doc(docs / "chapter-status.md", "overview"), ["chapter", "agent", "memory"]),
        (parse_markdown_doc(docs / "chapter-01-agent-vs-workflow.md", "chapter"), ["workflow", "agent", "tool"]),
        (parse_markdown_doc(docs / "chapter-02-llm.md", "chapter"), ["conversation", "structured output", "async", "llm"]),
        (parse_markdown_doc(docs / "chapter-03-tool-use.md", "chapter"), ["tool calling", "tool", "web search", "custom tools"]),
        (parse_markdown_doc(docs / "chapter-04-basic-agent.md", "chapter"), ["agent loop", "human-in-the-loop", "tool call"]),
        (parse_markdown_doc(docs / "chapter-05-rag.md", "chapter"), ["retrieval", "rag", "context engineering"]),
        (parse_markdown_doc(docs / "chapter-06-memory.md", "chapter"), ["session", "memory", "long-term"]),
        (parse_markdown_doc(docs / "chapter-07-planning-reflection.md", "chapter"), ["planning", "reflection"]),
        (parse_markdown_doc(docs / "chapter-08-code-agents.md", "chapter"), ["code", "workspace", "safety"]),
        (parse_markdown_doc(docs / "chapter-09-multi-agent.md", "chapter"), ["multi-agent", "reviewer", "coordinator"]),
        (parse_markdown_doc(docs / "chapter-10-evaluation-monitoring.md", "chapter"), ["evaluation", "metrics", "trace"]),
        (parse_markdown_doc(docs / "python-to-quarkus-mapping.md", "appendix"), ["python", "quarkus", "mapping"]),
    ]

    sections = [reference_materials, how_to_use]
    for section, keywords in chapter_specs:
        if keywords:
            excerpt = find_book_excerpt(book_samples, keywords)
            if excerpt:
                section.blocks.insert(
                    0,
                    Block(
                        kind="paragraph",
                        text="Book anchor: an extracted snippet from the source PDF that motivates the Quarkus rewrite.",
                    ),
                )
                section.blocks.insert(1, Block(kind="quote", text=excerpt))
        sections.append(section)

    python_index = Section(
        title="Python Reference File Index",
        source=book_dir / "ai-agent-from-scratch-main.zip",
        category="appendix",
        blocks=[
            Block(
                kind="paragraph",
                text=(
                    "The following list captures the Python reference zip structure in chapter order. "
                    "It is included here so readers can cross-check the Quarkus edition against the "
                    "book's actual source tree."
                ),
            ),
            Block(
                kind="code",
                lines=python_zip_index_lines(book_dir / "ai-agent-from-scratch-main.zip"),
            ),
        ],
    )
    sections.append(python_index)
    return sections


@lru_cache(maxsize=1)
def extract_book_samples(pdf_path: Path) -> list[str]:
    data = pdf_path.read_bytes()
    samples: list[str] = []
    for stream in iter_flate_streams(data):
        chunks = extract_text_chunks(stream)
        if not chunks:
            continue
        paragraph = normalize_book_text(" ".join(chunks))
        if len(paragraph) > 80:
            samples.append(paragraph)
    return dedupe_preserve_order(samples)


def iter_flate_streams(pdf_bytes: bytes) -> Iterable[bytes]:
    pattern = re.compile(rb'<<[^>]*?/FlateDecode[^>]*?>>\s*stream\r?\n')
    for match in pattern.finditer(pdf_bytes):
        start = match.end()
        end = pdf_bytes.find(b'endstream', start)
        if end == -1:
            continue
        yield pdf_bytes[start:end].strip(b'\r\n')


def extract_text_chunks(compressed_stream: bytes) -> list[str]:
    try:
        decoded = zlib.decompress(compressed_stream)
    except Exception:
        return []
    chunks: list[str] = []
    for hx in re.findall(rb'<([0-9A-Fa-f]{8,})>', decoded):
        text = decode_pdf_hex_string(hx)
        if text:
            chunks.append(text)
    for lit in re.findall(rb'\(([^()]*)\)\s*Tj', decoded):
        text = decode_pdf_literal_bytes(lit)
        if text:
            chunks.append(text)
    return chunks


def decode_pdf_hex_string(hex_bytes: bytes) -> str:
    try:
        raw = bytes.fromhex(hex_bytes.decode())
    except Exception:
        return ""
    return try_decode_pdf_bytes(raw)


def decode_pdf_literal_bytes(raw: bytes) -> str:
    return try_decode_pdf_bytes(raw)


def try_decode_pdf_bytes(raw: bytes) -> str:
    candidates: list[str] = []
    for enc in ("utf-16-be", "utf-16-le", "latin1"):
        try:
            decoded = raw.decode(enc)
        except Exception:
            continue
        candidates.append(decoded)
        candidates.append(rot3_letters(decoded))
    if not candidates:
        return ""
    best = max(candidates, key=readability_score)
    return normalize_book_text(best)


def readability_score(text: str) -> int:
    lower = text.lower()
    score = sum(ch.isalpha() for ch in text)
    for word in ("the", "and", "agent", "tool", "memory", "planning", "reflection", "workflow", "session", "chapter", "book", "you", "with", "this"):
        score += lower.count(word) * 20
    return score


def normalize_book_text(text: str) -> str:
    if not text:
        return ""
    text = text.lower()
    replacements = {
        "\x03": " ",
        "\u2019": "'",
        "\u2018": "'",
        "\u201c": '"',
        "\u201d": '"',
    }
    for old, new in replacements.items():
        text = text.replace(old, new)
    token_map = {
        "lw": "it",
        "lv": "is",
        "zh": "we",
        "wkdw": "that",
        "wkh": "the",
        "ior": "for",
        "rx": "you",
        "duh": "are",
        "rq": "on",
        "xvh": "use",
        "jhw": "get",
        "qhhg": "need",
        "ru": "or",
        "dq": "an",
        "lq": "in",
        "wkruxjk": "through",
        "zhe": "web",
        "vhdufk": "search",
        "ixqfwlrq": "function",
        "ixqfwlrqv": "functions",
        "hfxwlrq": "execution",
        "ghy": "dev",
        "doo": "all",
        "frq": "con",
        "xvinj": "using",
        "zklfk": "which",
        "sureohpv": "problems",
        "vwhsv": "steps",
        "prgho": "model",
        "suhglfw": "predict",
        "dssur": "appro",
        "uhjlrq": "region",
        "frpsohwinj": "completing",
        "wkdqnv": "thanks",
        "qrz": "now",
        "ghflghg": "decided",
        "glj": "dig",
        "ghhshu": "deeper",
        "hdf": "each",
        "gdu": "our",
        "vkrxog": "should",
        "frpsohwh": "complete",
        "edvlfv": "basics",
        "frpiruwdeoh": "comfortable",
        "zulwlqj": "writing",
        "vlpsoh": "simple",
        "pdfklqh": "machine",
        "wv": "ts",
    }
    for old, new in token_map.items():
        text = re.sub(rf"\b{re.escape(old)}\b", new, text)
    text = re.sub(r"[\x00-\x1f\x7f-\uffff]", " ", text)
    text = re.sub(r"\s+", " ", text).strip()
    text = text.replace(" ,", ",").replace(" .", ".").replace(" ;", ";").replace(" :", ":")
    return text


def rot3_letters(text: str) -> str:
    out: list[str] = []
    for ch in text:
        o = ord(ch)
        if 65 <= o <= 90:
            out.append(chr((o - 65 - 3) % 26 + 65))
        elif 97 <= o <= 122:
            out.append(chr((o - 97 - 3) % 26 + 97))
        else:
            out.append(ch)
    return "".join(out)


def find_book_excerpt(samples: list[str], keywords: list[str]) -> str:
    lowered_keywords = [k.lower() for k in keywords]
    best = ""
    best_score = -1
    for sample in samples:
        lower = sample.lower()
        score = sum(1 for k in lowered_keywords if k in lower)
        if score > best_score and len(sample) > 100:
            best = sample
            best_score = score
        if score == len(lowered_keywords) and score > 0:
            return trim_excerpt(sample)
    return trim_excerpt(best) if best else ""


def trim_excerpt(text: str, max_len: int = 760) -> str:
    if len(text) <= max_len:
        return text
    cut = text.rfind(" ", 0, max_len)
    if cut < 0:
        cut = max_len
    return text[:cut].rstrip() + "..."


def dedupe_preserve_order(items: list[str]) -> list[str]:
    seen: set[str] = set()
    result: list[str] = []
    for item in items:
        key = item.lower()
        if key not in seen:
            seen.add(key)
            result.append(item)
    return result


def read_pdf_metadata(pdf_path: Path) -> dict[str, str]:
    data = pdf_path.read_bytes().decode("latin1", errors="ignore")
    metadata: dict[str, str] = {}
    for key in ("Title", "Author", "Subject"):
        literal = extract_pdf_literal(data, f"/{key}(")
        if literal:
            metadata[key] = unescape_pdf_literal(literal)
    return metadata


def extract_pdf_literal(data: str, marker: str) -> str:
    start = data.find(marker)
    if start == -1:
        return ""
    i = start + len(marker)
    out: list[str] = []
    escaped = False
    depth = 1
    while i < len(data):
        ch = data[i]
        if escaped:
            out.append(ch)
            escaped = False
        elif ch == "\\":
            escaped = True
        elif ch == "(":
            depth += 1
            out.append(ch)
        elif ch == ")":
            depth -= 1
            if depth == 0:
                return "".join(out)
            out.append(ch)
        else:
            out.append(ch)
        i += 1
    return ""


def unescape_pdf_literal(value: str) -> str:
    value = value.replace(r"\\", "\\")
    value = value.replace(r"\(", "(").replace(r"\)", ")")
    value = value.replace(r"\n", "\n").replace(r"\r", "\r").replace(r"\t", "\t")
    return value


def python_zip_index_lines(zip_path: Path) -> list[str]:
    import zipfile

    prefix = "ai-agent-from-scratch-main/"
    with zipfile.ZipFile(zip_path) as zf:
        paths = [name for name in zf.namelist() if name.endswith(".py")]
    chapter_order = [
        "chapter_02_llm/",
        "chapter_03_tool_use/",
        "chapter_04_basic_agent/",
        "chapter_06_memory/",
        "scratch_agents/agents/",
        "scratch_agents/memory/",
        "scratch_agents/models/",
        "scratch_agents/sessions/",
        "scratch_agents/tools/",
        "scratch_agents/types/",
    ]
    grouped: list[str] = []
    for folder in chapter_order:
        folder_paths = [name.removeprefix(prefix) for name in paths if folder in name]
        if folder_paths:
            grouped.append(folder.rstrip("/"))
            grouped.extend(f"  - {name}" for name in folder_paths)
    return grouped


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
