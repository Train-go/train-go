"""Render a Markdown document in this folder to a self-contained HTML page.

Run it after editing the Markdown so both files stay in sync:

    python docs/session/render-doc.py                          # default document
    python docs/session/render-doc.py some-other-file.md       # any file here

No third-party packages and no network access: the HTML must open from disk with
no wifi. The converter only handles the Markdown subset these documents use —
headings, tables, fenced code, lists, blockquotes, bold, inline code, links,
horizontal rules. It is deliberately small, not a general Markdown engine.
"""

import html
import pathlib
import re
import sys

HERE = pathlib.Path(__file__).parent
DEFAULT_SOURCE = "tai-lieu-phan-tich-traingo.md"

CSS = """
:root {
	--ink: #16232e;
	--muted: #5b6b7a;
	--primary: #0f6cbd;
	--primary-soft: #eaf3fb;
	--surface: #ffffff;
	--surface-alt: #f5f8fb;
	--border: #dde5ec;
	--code-bg: #f2f5f8;
	--sidebar: 17rem;
}

* { box-sizing: border-box; }

body {
	margin: 0;
	font-family: system-ui, "Segoe UI", Roboto, Arial, sans-serif;
	/* 1.7 keeps Vietnamese tone marks clear of the line above. */
	line-height: 1.7;
	color: var(--ink);
	background: var(--surface);
}

.layout { display: flex; align-items: flex-start; }

/* ---------- Table of contents ---------- */

.toc {
	position: sticky;
	top: 0;
	flex: 0 0 var(--sidebar);
	width: var(--sidebar);
	height: 100vh;
	overflow-y: auto;
	padding: 1.5rem 1rem;
	background: var(--surface-alt);
	border-right: 1px solid var(--border);
	font-size: 0.86rem;
}

.toc__title {
	font-weight: 700;
	margin: 0 0 0.75rem;
	font-size: 0.78rem;
	letter-spacing: 0.06em;
	text-transform: uppercase;
	color: var(--muted);
}

.toc a {
	display: block;
	padding: 0.24rem 0.5rem;
	color: var(--ink);
	text-decoration: none;
	border-radius: 0.35rem;
	line-height: 1.4;
}

.toc a:hover { background: var(--primary-soft); color: var(--primary); }
.toc a.lvl-3 { padding-left: 1.35rem; color: var(--muted); font-size: 0.82rem; }

/* ---------- Content ---------- */

main {
	flex: 1 1 auto;
	min-width: 0;
	max-width: 60rem;
	padding: 2.5rem 2rem 5rem;
}

h1, h2, h3, h4 { line-height: 1.3; }

h1 {
	font-size: 2rem;
	margin: 0 0 1.5rem;
	padding-bottom: 0.75rem;
	border-bottom: 3px solid var(--primary);
}

h2 {
	font-size: 1.4rem;
	margin: 2.75rem 0 1rem;
	padding-bottom: 0.4rem;
	border-bottom: 1px solid var(--border);
}

h3 { font-size: 1.1rem; margin: 2rem 0 0.6rem; }
h4 { font-size: 0.98rem; margin: 1.5rem 0 0.5rem; color: var(--primary); }

p, ul, ol { margin: 0 0 1rem; }
li { margin-bottom: 0.3rem; }

a { color: var(--primary); }

hr {
	border: none;
	border-top: 1px solid var(--border);
	margin: 2.5rem 0;
}

code {
	font-family: "Cascadia Mono", Consolas, "SF Mono", monospace;
	font-size: 0.88em;
	background: var(--code-bg);
	padding: 0.12em 0.36em;
	border-radius: 0.25rem;
	/*
	 * Identifiers like confirmBooking_shouldFail_whenSeatAlreadyBooked have no
	 * space to break at and are wider than a phone screen, so without this the
	 * whole page scrolls sideways.
	 */
	overflow-wrap: anywhere;
	word-break: break-word;
}

pre {
	background: var(--code-bg);
	border: 1px solid var(--border);
	border-radius: 0.5rem;
	padding: 1rem;
	overflow-x: auto;
	line-height: 1.45;
	font-size: 0.84rem;
}

/* Wireframes must keep their exact columns, so no breaking inside pre. */
pre code {
	background: none;
	padding: 0;
	font-size: inherit;
	overflow-wrap: normal;
	word-break: normal;
}

blockquote {
	margin: 1rem 0;
	padding: 0.75rem 1rem;
	border-left: 4px solid var(--primary);
	background: var(--primary-soft);
	border-radius: 0 0.4rem 0.4rem 0;
}

blockquote p:last-child { margin-bottom: 0; }

.table-wrap { overflow-x: auto; margin: 0 0 1.25rem; }

table {
	border-collapse: collapse;
	width: 100%;
	font-size: 0.9rem;
}

th, td {
	border: 1px solid var(--border);
	padding: 0.5rem 0.7rem;
	text-align: left;
	vertical-align: top;
}

th { background: var(--surface-alt); font-weight: 600; }
tbody tr:nth-child(even) { background: #fbfcfd; }

/* ---------- Narrow screens ---------- */

@media (max-width: 900px) {
	.layout { display: block; }
	.toc {
		position: static;
		width: auto;
		height: auto;
		border-right: none;
		border-bottom: 1px solid var(--border);
		max-height: 16rem;
	}
	main { padding: 1.5rem 1rem 3rem; }
}

/* ---------- Print (Ctrl+P to PDF) ---------- */

@media print {
	.toc { display: none; }
	main { max-width: none; padding: 0; }
	body { font-size: 11pt; line-height: 1.5; }
	h2 { break-before: page; }
	h1 + h2, h2:first-of-type { break-before: auto; }
	h2, h3, h4 { break-after: avoid; }
	pre, table, blockquote { break-inside: avoid; }
	a { color: inherit; text-decoration: none; }
}
"""


def slugify(text):
	"""Match the anchor style the Markdown table of contents already links to."""
	text = re.sub(r"[`*]", "", text)
	text = text.strip().lower()
	text = re.sub(r"[^\w\s-]", "", text, flags=re.UNICODE)
	text = re.sub(r"[\s]+", "-", text)
	return text


def inline(text):
	"""Bold, inline code, and links. Escapes first so raw HTML never leaks in."""
	placeholders = []

	def stash(match):
		placeholders.append(match.group(1))
		return "\x00%d\x00" % (len(placeholders) - 1)

	text = re.sub(r"`([^`]+)`", stash, text)
	text = html.escape(text)
	text = re.sub(r"\*\*([^*]+)\*\*", r"<strong>\1</strong>", text)
	text = re.sub(r"(?<!\*)\*([^*]+)\*(?!\*)", r"<em>\1</em>", text)
	text = re.sub(
		r"\[([^\]]+)\]\(([^)]+)\)",
		lambda m: '<a href="%s">%s</a>' % (html.escape(m.group(2)), m.group(1)),
		text,
	)

	def unstash(match):
		return "<code>%s</code>" % html.escape(placeholders[int(match.group(1))])

	return re.sub(r"\x00(\d+)\x00", unstash, text)


def convert(md):
	out = []
	toc = []       # only h2/h3 — what the sidebar shows
	toc_all = []   # every heading, used to find the page title
	lines = md.split("\n")
	i = 0

	while i < len(lines):
		line = lines[i]

		# Fenced code block — emitted verbatim, no inline processing.
		if line.startswith("```"):
			i += 1
			block = []
			while i < len(lines) and not lines[i].startswith("```"):
				block.append(lines[i])
				i += 1
			i += 1
			out.append("<pre><code>%s</code></pre>" % html.escape("\n".join(block)))
			continue

		# Heading.
		heading = re.match(r"^(#{1,4})\s+(.*)$", line)
		if heading:
			level = len(heading.group(1))
			text = heading.group(2)
			anchor = slugify(text)
			out.append("<h%d id=\"%s\">%s</h%d>" % (level, anchor, inline(text), level))
			toc_all.append((level, text, anchor))
			if level in (2, 3):
				toc.append((level, text, anchor))
			i += 1
			continue

		# Table: a header row followed by a separator row.
		if line.startswith("|") and i + 1 < len(lines) and re.match(r"^\|[\s:|-]+\|$", lines[i + 1]):
			header = [c.strip() for c in line.strip("|").split("|")]
			i += 2
			rows = []
			while i < len(lines) and lines[i].startswith("|"):
				rows.append([c.strip() for c in lines[i].strip("|").split("|")])
				i += 1
			cells = "".join("<th>%s</th>" % inline(c) for c in header)
			body = "".join(
				"<tr>%s</tr>" % "".join("<td>%s</td>" % inline(c) for c in row)
				for row in rows
			)
			out.append(
				'<div class="table-wrap"><table><thead><tr>%s</tr></thead>'
				"<tbody>%s</tbody></table></div>" % (cells, body)
			)
			continue

		# Blockquote.
		if line.startswith("> "):
			block = []
			while i < len(lines) and lines[i].startswith(">"):
				block.append(lines[i].lstrip(">").strip())
				i += 1
			out.append("<blockquote><p>%s</p></blockquote>" % inline(" ".join(block)))
			continue

		# Horizontal rule.
		if line.strip() == "---":
			out.append("<hr>")
			i += 1
			continue

		# List, ordered or not. Continuation lines are indented.
		bullet = re.match(r"^(\s*)([-*]|\d+\.)\s+(.*)$", line)
		if bullet:
			ordered = bool(re.match(r"^\d+\.$", bullet.group(2)))
			tag = "ol" if ordered else "ul"
			items = []
			while i < len(lines):
				m = re.match(r"^(\s*)([-*]|\d+\.)\s+(.*)$", lines[i])
				if m:
					items.append(m.group(3))
					i += 1
				elif lines[i].startswith("  ") and lines[i].strip() and items:
					items[-1] += " " + lines[i].strip()
					i += 1
				else:
					break
			out.append(
				"<%s>%s</%s>" % (tag, "".join("<li>%s</li>" % inline(x) for x in items), tag)
			)
			continue

		# Blank line.
		if not line.strip():
			i += 1
			continue

		# Paragraph: join until a blank line or a construct starts.
		para = []
		while i < len(lines) and lines[i].strip():
			if re.match(r"^(#{1,4}\s|```|\||>\s|\s*([-*]|\d+\.)\s)", lines[i]):
				break
			if lines[i].strip() == "---":
				break
			para.append(lines[i].strip())
			i += 1
		if para:
			out.append("<p>%s</p>" % inline(" ".join(para)))

	toc_html = "".join(
		'<a class="lvl-%d" href="#%s">%s</a>' % (level, anchor, html.escape(re.sub(r"[`*]", "", text)))
		for level, text, anchor in toc
	)

	# The browser tab takes its name from the document's own first heading.
	page_title = next((t for lvl, t, _ in toc_all if lvl == 1), "TrainGo")

	return (
		"<!DOCTYPE html>\n<html lang=\"vi\">\n<head>\n"
		"<meta charset=\"UTF-8\">\n"
		"<meta name=\"viewport\" content=\"width=device-width, initial-scale=1\">\n"
		"<title>%s</title>\n"
		"<style>%s</style>\n</head>\n<body>\n"
		'<div class="layout">\n'
		'<nav class="toc"><p class="toc__title">Mục lục</p>%s</nav>\n'
		"<main>\n%s\n</main>\n</div>\n</body>\n</html>\n"
		% (html.escape(re.sub(r"[`*]", "", page_title)), CSS, toc_html, "\n".join(out))
	)


def main():
	name = sys.argv[1] if len(sys.argv) > 1 else DEFAULT_SOURCE
	source = HERE / name
	if not source.exists():
		print("Khong tim thay %s" % source, file=sys.stderr)
		return 1
	target = source.with_suffix(".html")
	target.write_text(convert(source.read_text(encoding="utf-8")), encoding="utf-8")
	print("Da ghi %s (%d bytes)" % (target.name, target.stat().st_size))
	return 0


if __name__ == "__main__":
	sys.exit(main())
