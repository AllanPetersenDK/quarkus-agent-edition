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
BODY_SIZE = 10.2
BODY_LEADING = 15.4
SMALL_SIZE = 8.2
CODE_SIZE = 8.5
CODE_LEADING = 12.0

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
        page.add_text(LEFT_MARGIN, page.cursor_y, section.title, BODY_BOLD, 18.0)
        page.cursor_y -= 28
        page.add_text(
            LEFT_MARGIN,
            page.cursor_y,
            f"Source: {section.source.as_posix()}",
            BODY_ITALIC,
            SMALL_SIZE,
        )
        page.cursor_y -= 22
        page.add_rule(page.cursor_y, 0.7)
        page.cursor_y -= 24

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
            page.cursor_y = PAGE_HEIGHT - TOP_MARGIN - 8
        return page

    def _add_heading(self, page: Page, level: int, text: str) -> Page:
        size_map = {1: 15.2, 2: 13.4, 3: 12.2}
        size = size_map.get(level, 11.6)
        needed = size + 18
        page = self._ensure_space(page, needed)
        page.add_text(LEFT_MARGIN, page.cursor_y, text, BODY_BOLD, size)
        page.cursor_y -= needed
        return page

    def _add_paragraph(self, page: Page, text: str) -> Page:
        wrapped = wrap_text(normalize_inline(text), BODY_SIZE, CONTENT_WIDTH)
        needed = len(wrapped) * BODY_LEADING + 8
        page = self._ensure_space(page, needed)
        for line in wrapped:
            if page.cursor_y < BOTTOM_MARGIN + BODY_LEADING:
                page = self.new_page(len(self.content_pages) + 1, page.section_title)
                page.cursor_y = PAGE_HEIGHT - TOP_MARGIN - 8
            page.add_text(LEFT_MARGIN, page.cursor_y, line, BODY_FONT, BODY_SIZE)
            page.cursor_y -= BODY_LEADING
        page.cursor_y -= 6
        return page

    def _add_quote(self, page: Page, text: str) -> Page:
        wrapped = wrap_text(normalize_inline(text), BODY_SIZE, CONTENT_WIDTH - 18)
        needed = len(wrapped) * BODY_LEADING + 12
        page = self._ensure_space(page, needed)
        box_height = needed - 4
        page.add_box(LEFT_MARGIN - 4, page.cursor_y - box_height + 4, CONTENT_WIDTH + 8, box_height, 0.96)
        page.cursor_y -= 4
        for line in wrapped:
            page.add_text(LEFT_MARGIN + 8, page.cursor_y, line, BODY_ITALIC, BODY_SIZE)
            page.cursor_y -= BODY_LEADING
        page.cursor_y -= 6
        return page

    def _add_bullets(self, page: Page, items: list[str]) -> Page:
        for item in items:
            wrapped = wrap_text(normalize_inline(item), BODY_SIZE, CONTENT_WIDTH - 22)
            needed = len(wrapped) * BODY_LEADING + 6
            page = self._ensure_space(page, needed)
            first = True
            for line in wrapped:
                prefix = "• " if first else "  "
                page.add_text(LEFT_MARGIN + 8, page.cursor_y, prefix + line, BODY_FONT, BODY_SIZE)
                page.cursor_y -= BODY_LEADING
                first = False
            page.cursor_y -= 4
        page.cursor_y -= 4
        return page

    def _add_code_block(self, page: Page, lines: list[str]) -> Page:
        if not lines:
            return page
        prepared = [line.rstrip("\n") for line in lines]
        needed = len(prepared) * CODE_LEADING + 18
        page = self._ensure_space(page, needed)
        block_height = needed - 2
        page.add_box(LEFT_MARGIN - 4, page.cursor_y - block_height + 4, CONTENT_WIDTH + 8, block_height, 0.95)
        page.cursor_y -= 8
        for line in prepared:
            page.add_text(LEFT_MARGIN + 6, page.cursor_y, line, MONO_FONT, CODE_SIZE)
            page.cursor_y -= CODE_LEADING
        page.cursor_y -= 6
        return page

    def _draw_page_number(self, page: Page) -> None:
        x = PAGE_WIDTH / 2 - 4
        page.add_text(x, 20, str(page.number), BODY_FONT, 8.8)

    def _build_cover_page(self) -> Page:
        page = Page(number=0, section_title="Cover")
        page.add_box(0, 0, PAGE_WIDTH, PAGE_HEIGHT, 0.98)
        page.add_box(0, 498, PAGE_WIDTH, 150, 0.89)
        page.add_text(LEFT_MARGIN, 578, SMALL_CAPS_LINE, BODY_BOLD, 10)
        page.add_text(LEFT_MARGIN, 548, TITLE_LINE, BODY_BOLD, 23)
        page.add_text(LEFT_MARGIN, 514, SUBTITLE_LINE, BODY_BOLD, 17)
        page.add_rule(488, 1.2)
        page.add_text(LEFT_MARGIN, 454, SUBTITLE_DESC, BODY_FONT, 11.6)
        page.add_text(LEFT_MARGIN, 430, "Java 21  |  Quarkus  |  Maven", BODY_BOLD, 10.6)
        page.add_text(LEFT_MARGIN, 400, "Companion PDF compiled from the chapter docs", BODY_FONT, 10.0)
        page.add_text(LEFT_MARGIN, 376, BOOK_AUTHOR_LINE, BODY_ITALIC, 10.1)
        page.add_text(LEFT_MARGIN, 362, BOOK_SUBJECT_LINE, BODY_FONT, 9.4)
        page.add_text(LEFT_MARGIN, 336, "Included inside:", BODY_BOLD, 11.0)
        bullets = [
            "Chapter-by-chapter Quarkus translations of the Python reference zip",
            "Architecture notes and Python-to-Quarkus mapping",
            "Demo-first implementations with explicit production placeholders",
            "RAG, memory, planning, code agents, multi-agent routing, and evaluation",
        ]
        y = 316
        for bullet in bullets:
            page.add_text(LEFT_MARGIN + 12, y, f"• {bullet}", BODY_FONT, 9.8)
            y -= 21
        page.add_text(LEFT_MARGIN, 124, "Generated from the repository docs", BODY_ITALIC, 9.2)
        page.add_text(LEFT_MARGIN, 106, "Reference book materials are included in docs/book", BODY_FONT, 8.8)
        page.add_text(LEFT_MARGIN, 88, "Source repository: quarkus-agent-edition", BODY_FONT, 8.9)
        page.add_text(LEFT_MARGIN, 68, "Companion edition, not original book code", BODY_BOLD, 8.9)
        return page

    def _build_toc_page(self, sections: list[Section]) -> Page:
        page = Page(number=0, section_title="Contents")
        page.add_text(LEFT_MARGIN, 582, "Contents", BODY_BOLD, 18)
        page.add_rule(566, 0.9)
        y = 540
        for section in sections:
            display = self._toc_label(section)
            page.add_text(LEFT_MARGIN, y, display, BODY_FONT, 10.0)
            page.add_text(320, y, self._toc_leader(display, section.page_start), BODY_FONT, 10.0)
            page.add_text(365, y, str(section.page_start), BODY_BOLD, 10.0)
            y -= 21
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
                    "modules. The result is a companion text, not a literal reproduction, so the "
                    "examples are adapted where Java and Quarkus give us a better fit."
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
    chapters = [
        make_companion_chapter(
            title="Chapter 01 - Agent vs Workflow",
            source=docs / "chapter-01-agent-vs-workflow.md",
            book_thread=(
                "The opening idea in the book is that not every AI product needs an agent. "
                "Some problems are best solved with a predictable workflow, while others need "
                "an agent that can inspect context, call tools, and iterate. In Quarkus, that "
                "distinction becomes a clean architectural choice between explicit workflow code "
                "and an orchestration layer that loops until the task is complete."
            ),
            python_files=[
                "chapter_01 does not exist as a standalone zip folder, so this chapter is the conceptual bridge into the reference structure.",
                "The Quarkus edition pairs the concept with /workflow-demo and /agent REST endpoints.",
            ],
            quarkus_translation=(
                "The Quarkus version keeps the comparison tangible. `WorkflowResource` shows a "
                "fixed path; `AgentResource` shows the iterative path. The `DemoToolCallingLlmClient` "
                "is intentionally small so readers can see the control flow rather than a hidden "
                "framework."
            ),
            central_classes=[
                "dk.ashlan.agent.api.WorkflowResource",
                "dk.ashlan.agent.api.AgentResource",
                "dk.ashlan.agent.core.AgentOrchestrator",
                "dk.ashlan.agent.llm.DemoToolCallingLlmClient",
            ],
            design_choices=[
                "Use REST endpoints so the workflow-versus-agent contrast is visible in a running app.",
                "Keep the loop explicit instead of burying it in a large abstraction.",
                "Mark the demo client clearly as fake so the learning path remains honest.",
            ],
            demo_vs_production=[
                "Demo: hard-coded tool choices and prompt-driven behavior for the learning exercises.",
                "Production placeholder: a real provider-backed LLM client with tool calling and retries.",
            ],
        ),
        make_companion_chapter(
            title="Chapter 02 - LLM Integration",
            source=docs / "chapter-02-llm.md",
            book_thread=(
                "This chapter in the book teaches the smallest useful LLM surface: chat, structured "
                "output, asynchronous calls, and a few deliberately awkward edge cases. The Quarkus "
                "edition keeps those learning goals intact, but turns them into typed request and "
                "response objects that fit the Java toolchain."
            ),
            python_files=[
                "chapter_02_llm/01_llm_chat.py",
                "chapter_02_llm/02_conversation_management.py",
                "chapter_02_llm/03_structured_output.py",
                "chapter_02_llm/04_asynchronous_llm_call.py",
                "chapter_02_llm/05_potato_problem.py",
                "scratch_agents/models/base_llm.py",
                "scratch_agents/models/llm_request.py",
                "scratch_agents/models/llm_response.py",
                "scratch_agents/models/openai.py",
            ],
            quarkus_translation=(
                "The Java version uses `LlmRequest`, `LlmResponse`, `LlmClient`, and "
                "`StructuredOutputParser` as the core vocabulary. The demo client reproduces the "
                "book's early learning steps without pretending to be a production LLM provider."
            ),
            central_classes=[
                "dk.ashlan.agent.llm.LlmRequest",
                "dk.ashlan.agent.llm.LlmResponse",
                "dk.ashlan.agent.llm.LlmClient",
                "dk.ashlan.agent.llm.OpenAiLlmClient",
                "dk.ashlan.agent.llm.StructuredOutputParser",
                "dk.ashlan.agent.chapters.chapter02.*",
            ],
            design_choices=[
                "Use records and small DTOs so message flow stays easy to inspect.",
                "Model structured output explicitly instead of hoping free-form text can be parsed later.",
                "Keep async examples in the companion because Quarkus can express them naturally.",
            ],
            demo_vs_production=[
                "Demo: `DemoToolCallingLlmClient` returns deterministic answers for the exercises.",
                "Production placeholder: `OpenAiLlmClient` is the real integration seam.",
            ],
        ),
        make_companion_chapter(
            title="Chapter 03 - Tool Use",
            source=docs / "chapter-03-tool-use.md",
            book_thread=(
                "The book's tool chapter introduces the idea that agents gain power when they can "
                "reach beyond the prompt and call a calculator, search service, or domain-specific "
                "helper. In Quarkus, tools become CDI-friendly, testable classes with explicit "
                "definitions and a generic executor."
            ),
            python_files=[
                "chapter_03_tool_use/calculator.py",
                "chapter_03_tool_use/tavily_search_tool.py",
                "chapter_03_tool_use/wikipedia.py",
                "chapter_03_tool_use/tool_definition.py",
                "chapter_03_tool_use/tool_abstraction.py",
                "chapter_03_tool_use/tool_decorator.py",
                "scratch_agents/tools/base_tool.py",
                "scratch_agents/tools/function_tool.py",
                "scratch_agents/tools/decorator.py",
                "scratch_agents/tools/schema_utils.py",
            ],
            quarkus_translation=(
                "The companion maps the tool ideas into `Tool`, `ToolDefinition`, `ToolRegistry`, "
                "`ToolExecutor`, and concrete implementations such as `CalculatorTool`, `ClockTool`, "
                "and `WebSearchTool`. The `FunctionToolAdapter` and `ToolDecorator` classes keep the "
                "model open for future extensions."
            ),
            central_classes=[
                "dk.ashlan.agent.tools.Tool",
                "dk.ashlan.agent.tools.ToolDefinition",
                "dk.ashlan.agent.tools.ToolRegistry",
                "dk.ashlan.agent.tools.ToolExecutor",
                "dk.ashlan.agent.tools.CalculatorTool",
                "dk.ashlan.agent.tools.ClockTool",
                "dk.ashlan.agent.tools.WebSearchTool",
            ],
            design_choices=[
                "Expose tools as plain Java objects so they are easy to register and unit test.",
                "Keep the executor generic so new tools do not require a new control path.",
                "Treat Wikipedia and web search as adapters or placeholders where the original book expects external APIs.",
            ],
            demo_vs_production=[
                "Demo: calculator, clock, and mocked search flows for deterministic tests.",
                "Production placeholder: a real search API client with credentials and retry handling.",
            ],
        ),
        make_companion_chapter(
            title="Chapter 04 - Basic Agent",
            source=docs / "chapter-04-basic-agent.md",
            book_thread=(
                "The basic agent chapter is where the idea becomes operational: think, act, observe, repeat. "
                "The Quarkus edition preserves that rhythm, but moves the state into explicit execution "
                "contexts and trace objects so the loop is easy to understand and easy to test."
            ),
            python_files=[
                "chapter_04_basic_agent/01_solve_kipchoge_problem.py",
                "chapter_04_basic_agent/02_agent_structured_output.py",
                "chapter_04_basic_agent/03_human_in_the_loop.py",
                "scratch_agents/agents/execution_context_ch4.py",
                "scratch_agents/agents/tool_calling_agent_ch4_base.py",
                "scratch_agents/agents/tool_calling_agent_ch4_callback.py",
                "scratch_agents/agents/tool_calling_agent_ch4_structured_output.py",
                "scratch_agents/types/contents.py",
                "scratch_agents/types/events.py",
            ],
            quarkus_translation=(
                "The core loop lives in `AgentOrchestrator`, while callback and structured-output "
                "variants are factored into dedicated helpers. `ExecutionContext`, `ConversationTranscript`, "
                "and the `ContentItem`/`Event` hierarchy make the evolving state visible rather than "
                "hidden inside a monolith."
            ),
            central_classes=[
                "dk.ashlan.agent.core.ExecutionContext",
                "dk.ashlan.agent.core.AgentOrchestrator",
                "dk.ashlan.agent.core.CallbackAwareAgentOrchestrator",
                "dk.ashlan.agent.core.StructuredOutputAgentOrchestrator",
                "dk.ashlan.agent.types.ContentItem",
                "dk.ashlan.agent.types.Event",
                "dk.ashlan.agent.agents.ConversationTranscript",
            ],
            design_choices=[
                "Prefer explicit event objects over stringly typed state transitions.",
                "Keep human-in-the-loop behavior separate from the core loop so it can be reused.",
                "Let the tests prove the loop converges rather than relying on the prompt alone.",
            ],
            demo_vs_production=[
                "Demo: the chapter demos and callback helpers that make the loop visible.",
                "Production placeholder: a policy layer that would govern retries, safety, and escalation.",
            ],
        ),
        make_companion_chapter(
            title="Chapter 05 - RAG",
            source=docs / "chapter-05-rag.md",
            book_thread=(
                "Retrieval-augmented generation adds knowledge to the agent without making the model "
                "remember everything forever. In the Quarkus companion, the RAG chapter is the bridge "
                "from prompt-driven behavior to a service that can ingest, chunk, embed, and retrieve "
                "documents in a predictable way."
            ),
            python_files=[
                "This chapter is a Quarkus companion extension rather than a direct Python zip chapter.",
                "It is inspired by the book's retrieval and context engineering ideas.",
            ],
            quarkus_translation=(
                "`Chunker`, `FakeEmbeddingClient`, `InMemoryVectorStore`, `Retriever`, and `RagService` "
                "form a complete local pipeline. The `KnowledgeBaseTool` turns retrieval into something "
                "the agent can call like any other tool."
            ),
            central_classes=[
                "dk.ashlan.agent.rag.Chunker",
                "dk.ashlan.agent.rag.FakeEmbeddingClient",
                "dk.ashlan.agent.rag.InMemoryVectorStore",
                "dk.ashlan.agent.rag.Retriever",
                "dk.ashlan.agent.rag.RagService",
                "dk.ashlan.agent.rag.KnowledgeBaseTool",
            ],
            design_choices=[
                "Use fake embeddings so the chapter remains self-contained and runnable offline.",
                "Keep top-K retrieval and cosine similarity explicit for teaching value.",
                "Make ingestion a service so test fixtures can load data the same way production code would.",
            ],
            demo_vs_production=[
                "Demo: in-memory vector storage and fake embeddings.",
                "Production placeholder: Postgres/pgvector or another durable vector backend.",
            ],
        ),
        make_companion_chapter(
            title="Chapter 06 - Memory",
            source=docs / "chapter-06-memory.md",
            book_thread=(
                "Memory turns a stateless chat into an ongoing relationship. The book explores short-term, "
                "sliding-window, summary, and long-term memory strategies, and the Quarkus edition keeps "
                "those distinctions while translating them into sessions and stores that are easy to test."
            ),
            python_files=[
                "chapter_06_memory/01_session_agent.py",
                "chapter_06_memory/02_core_memory_strategy.py",
                "chapter_06_memory/03_core_memory_update.py",
                "chapter_06_memory/04_sliding_window.py",
                "chapter_06_memory/05_summarization.py",
                "chapter_06_memory/06_conversation_search.py",
                "chapter_06_memory/07_task_long_term.py",
                "chapter_06_memory/08_user_long_term.py",
                "scratch_agents/memory/base_memory_strategy.py",
                "scratch_agents/memory/core_memory_strategy.py",
                "scratch_agents/memory/sliding_window_strategy.py",
                "scratch_agents/memory/summarization_strategy.py",
                "scratch_agents/sessions/base_session_manager.py",
                "scratch_agents/sessions/base_cross_session_manager.py",
                "scratch_agents/sessions/session.py",
            ],
            quarkus_translation=(
                "`SessionManager`, `CrossSessionManager`, `MemoryStrategy`, and `MemoryService` "
                "capture the same ideas in Java. Short-term context lives in the execution context, "
                "while task and user memories are maintained as separate stores so retrieval can stay "
                "focused and deterministic."
            ),
            central_classes=[
                "dk.ashlan.agent.memory.MemoryStrategy",
                "dk.ashlan.agent.memory.CoreMemoryStrategy",
                "dk.ashlan.agent.memory.SlidingWindowStrategy",
                "dk.ashlan.agent.memory.SummarizationStrategy",
                "dk.ashlan.agent.sessions.SessionManager",
                "dk.ashlan.agent.sessions.CrossSessionManager",
                "dk.ashlan.agent.memory.MemoryService",
            ],
            design_choices=[
                "Separate short-term, summary, and long-term memory so each concern is testable.",
                "Use in-memory stores first to keep the chapter runnable without external services.",
                "Keep retrieval logic explicit so the memory path is understandable from the tests.",
            ],
            demo_vs_production=[
                "Demo: local session continuity and in-memory cross-session retrieval.",
                "Production placeholder: durable persistence and search-backed memory stores.",
            ],
        ),
        make_companion_chapter(
            title="Chapter 07 - Planning and Reflection",
            source=docs / "chapter-07-planning-reflection.md",
            book_thread=(
                "Planning and reflection take the agent from reactive to deliberate. Instead of answering "
                "immediately, the system first builds a plan, then evaluates the output, then decides "
                "whether another pass is required."
            ),
            python_files=[
                "This chapter is a Quarkus companion extension rather than a direct Python zip chapter.",
                "It continues the book's reasoning theme with plan generation and reflection loops.",
            ],
            quarkus_translation=(
                "`ExecutionPlan`, `PlanStep`, `PlannerService`, `ReflectionService`, and "
                "`PlannedAgentOrchestrator` encode the thinking loop in explicit Java types."
            ),
            central_classes=[
                "dk.ashlan.agent.planning.ExecutionPlan",
                "dk.ashlan.agent.planning.PlanStep",
                "dk.ashlan.agent.planning.PlannerService",
                "dk.ashlan.agent.planning.ReflectionService",
                "dk.ashlan.agent.planning.PlannedAgentOrchestrator",
            ],
            design_choices=[
                "Let planning happen before execution so the agent can be inspected and tested.",
                "Treat reflection as a gate that can reject outputs that are too thin or incomplete.",
                "Use a re-entry loop when a better answer is justified by the evaluation step.",
            ],
            demo_vs_production=[
                "Demo: deterministic planning and reflection rules suitable for tests.",
                "Production placeholder: model-backed plan generation with richer scoring.",
            ],
        ),
        make_companion_chapter(
            title="Chapter 08 - Code Agents",
            source=docs / "chapter-08-code-agents.md",
            book_thread=(
                "Code agents need a workspace, file safety, and a way to verify changes. The Quarkus companion "
                "turns that into a constrained workspace service and explicit file and test execution tools."
            ),
            python_files=[
                "This chapter is a Quarkus companion extension rather than a direct Python zip chapter.",
                "It extends the book's agent patterns into safe workspace automation.",
            ],
            quarkus_translation=(
                "`WorkspaceService`, `FileReadTool`, `FileWriteTool`, `TestExecutionTool`, and "
                "`CodeGenerationTool` keep every action rooted inside a safe workspace. The chapter "
                "demos show how the agent can work on files without giving it unrestricted access."
            ),
            central_classes=[
                "dk.ashlan.agent.code.WorkspaceService",
                "dk.ashlan.agent.code.FileReadTool",
                "dk.ashlan.agent.code.FileWriteTool",
                "dk.ashlan.agent.code.TestExecutionTool",
                "dk.ashlan.agent.code.CodeGenerationTool",
                "dk.ashlan.agent.code.CodeAgentOrchestrator",
            ],
            design_choices=[
                "Enforce path safety so code generation cannot escape the workspace root.",
                "Keep file operations and test execution separate for clearer review and security.",
                "Make the workspace configurable so the same logic can run locally or in CI.",
            ],
            demo_vs_production=[
                "Demo: local workspace operations and deterministic safety tests.",
                "Production placeholder: hardened sandboxes, containerized execution, and stricter policy controls.",
            ],
        ),
        make_companion_chapter(
            title="Chapter 09 - Multi-Agent",
            source=docs / "chapter-09-multi-agent.md",
            book_thread=(
                "When one agent is not enough, the system needs specialization. The multi-agent chapter turns "
                "the single-agent loop into a coordinator that routes work to research, coding, and review "
                "specialists."
            ),
            python_files=[
                "This chapter is a Quarkus companion extension rather than a direct Python zip chapter.",
                "It generalizes the book's orchestration ideas into specialist agents and routing.",
            ],
            quarkus_translation=(
                "`SpecialistAgent`, `AgentRouter`, `CoordinatorAgent`, `ResearchAgent`, `CodingAgent`, and "
                "`ReviewerAgent` give the application a team-based structure."
            ),
            central_classes=[
                "dk.ashlan.agent.multiagent.SpecialistAgent",
                "dk.ashlan.agent.multiagent.AgentRouter",
                "dk.ashlan.agent.multiagent.CoordinatorAgent",
                "dk.ashlan.agent.multiagent.ResearchAgent",
                "dk.ashlan.agent.multiagent.CodingAgent",
                "dk.ashlan.agent.multiagent.ReviewerAgent",
            ],
            design_choices=[
                "Route tasks by capability instead of asking one model to do everything.",
                "Keep reviewer feedback separate so quality gates are explicit.",
                "Return task results as structured values instead of free-form narrative where possible.",
            ],
            demo_vs_production=[
                "Demo: deterministic routing and reviewer scoring for the companion examples.",
                "Production placeholder: policy-driven orchestration with queueing and audit trails.",
            ],
        ),
        make_companion_chapter(
            title="Chapter 10 - Evaluation and Monitoring",
            source=docs / "chapter-10-evaluation-monitoring.md",
            book_thread=(
                "The final chapter closes the loop by asking whether the system is any good. Evaluation, "
                "tracing, and metrics turn the agent from a demo into something that can be measured."
            ),
            python_files=[
                "This chapter is a Quarkus companion extension rather than a direct Python zip chapter.",
                "It extends the book's ideas into repeatable evaluation and traceability.",
            ],
            quarkus_translation=(
                "`EvalCase`, `EvalResult`, `EvaluationRunner`, `AgentTrace`, `AgentTraceService`, and "
                "`RunMetrics` provide a compact monitoring story. The admin endpoint makes the evaluation "
                "path observable from the outside."
            ),
            central_classes=[
                "dk.ashlan.agent.eval.EvalCase",
                "dk.ashlan.agent.eval.EvalResult",
                "dk.ashlan.agent.eval.EvaluationRunner",
                "dk.ashlan.agent.eval.AgentTrace",
                "dk.ashlan.agent.eval.AgentTraceService",
                "dk.ashlan.agent.eval.RunMetrics",
            ],
            design_choices=[
                "Treat evaluation cases as first-class inputs so agent quality can be regression-tested.",
                "Record traces separately from metrics so debugging and reporting stay distinct.",
                "Expose admin endpoints only for the companion demo, with production auth left as a hardening step.",
            ],
            demo_vs_production=[
                "Demo: in-memory evaluation runs and trace capture.",
                "Production placeholder: persistent metrics, dashboards, and secure admin access.",
            ],
        ),
    ]

    sections = [reference_materials, how_to_use, *chapters]

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

    architecture_map = Section(
        title="Companion Architecture Map",
        source=docs / "architecture.md",
        category="appendix",
        blocks=[
            Block(
                kind="paragraph",
                text=(
                    "The Quarkus edition is organized into a runtime core and a set of companion "
                    "extensions. The runtime core covers LLM integration, tools, orchestration, "
                    "memory, sessions, and typed conversation events. The companion extensions add "
                    "RAG, planning, code agents, multi-agent routing, and evaluation."
                ),
            ),
            Block(
                kind="bullets",
                items=[
                    "Runtime core: `llm`, `tools`, `core`, `types`, `memory`, `sessions`.",
                    "Companion extensions: `rag`, `planning`, `code`, `multiagent`, `eval`.",
                    "API surface: `AgentResource`, `WorkflowResource`, `CodeAgentResource`, `MultiAgentResource`, `AdminEvaluationResource`.",
                    "Test strategy: unit tests for small services and smoke tests for the REST layer.",
                ],
            ),
            Block(
                kind="paragraph",
                text=(
                    "The design keeps demo implementations clearly labeled while leaving room for real "
                    "providers, persistence, and observability if the companion is hardened further."
                ),
            ),
        ],
    )
    sections.append(architecture_map)
    return sections


def make_companion_chapter(
    *,
    title: str,
    source: Path,
    book_thread: str,
    python_files: list[str],
    quarkus_translation: str,
    central_classes: list[str],
    design_choices: list[str],
    demo_vs_production: list[str],
) -> Section:
    blocks: list[Block] = [
        Block(kind="paragraph", text=book_thread),
        Block(kind="heading", level=2, text="Python Reference"),
        Block(kind="bullets", items=python_files),
        Block(kind="heading", level=2, text="Quarkus Translation"),
        Block(kind="paragraph", text=quarkus_translation),
        Block(kind="heading", level=2, text="Central Classes"),
        Block(kind="bullets", items=central_classes),
        Block(kind="heading", level=2, text="Design Choices"),
        Block(kind="bullets", items=design_choices),
        Block(kind="heading", level=2, text="Demo vs Production"),
        Block(kind="bullets", items=demo_vs_production),
    ]
    return Section(title=title, source=source, category="chapter", blocks=blocks)


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
