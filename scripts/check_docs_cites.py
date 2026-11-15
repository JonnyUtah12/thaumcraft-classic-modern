#!/usr/bin/env python3
"""Cite audit: print the TARGET line behind every docs/testing.md cite.

Convention (minted by t_867b394f): a Rules bullet in docs/testing.md is cited by
ANCHOR — `docs/testing.md#<kebab>` — and the cited bullet carries
`<a id="<kebab>"></a>` on the line above it. An insertion cannot rot an anchor
cite, which is exactly what line-number cites did (see the removal-age rule in
docs/testing.md for the shift history). Line-number cites `docs/testing.md:NNN`
are DEPRECATED: the audit prints each one marked `LEGACY[...]`.

Every cite in `src/` and in docs/testing.md itself prints one line in the fixed
shape

    file:line -> :NNN TARGET[<the cited line>]

where `:NNN` is the CURRENT line the cite resolves to (an anchor resolves to the
bullet directly below its anchor line) and `TARGET[...]` is the text sitting
there. A stale cite is a one-command check: the line whose TARGET is not the
rule the citing comment describes. Exit codes:

    0  every cite resolves (anchors exist exactly once, lines in range)
    1  a cite is STALE — missing/duplicated anchor, line out of range,
       or an anchor not directly above a bullet

Usage: python3 scripts/check_docs_cites.py
"""
import pathlib
import re
import sys

REPO = pathlib.Path(__file__).resolve().parents[1]
DOC = REPO / "docs" / "testing.md"
CITE = re.compile(r"docs/testing\.md#([a-z0-9][a-z0-9-]*)|docs/testing\.md:(\d+)")
ANCHOR = re.compile(r'<a id="([a-z0-9][a-z0-9-]*)"></a>')


def resolve_anchor(doc_lines, hits):
    """Return (rule_line, target_text) or None when the anchor layout is broken."""
    if len(hits) != 1:
        return None
    a_line = hits[0]
    for i in range(a_line, len(doc_lines)):  # first non-blank line below the anchor
        line = doc_lines[i]
        if not line.strip():
            continue
        if not line.lstrip().startswith("- "):
            return None
        return i + 1, line.strip()
    return None


def main():
    doc_lines = DOC.read_text(encoding="utf-8").splitlines()
    anchors = {}
    for i, line in enumerate(doc_lines, 1):
        for m in ANCHOR.finditer(line):
            anchors.setdefault(m.group(1), []).append(i)

    files = sorted((REPO / "src").rglob("*.java")) + [DOC]
    sites = []
    for f in files:
        for n, line in enumerate(f.read_text(encoding="utf-8").splitlines(), 1):
            for m in CITE.finditer(line):
                sites.append((f, n, m.group(1), m.group(2)))

    stale = legacy = 0
    for f, n, anchor, num in sites:
        rel = f.relative_to(REPO)
        if anchor is not None:
            got = resolve_anchor(doc_lines, anchors.get(anchor, []))
            if got is None:
                stale += 1
                found = len(anchors.get(anchor, []))
                print(f"{rel}:{n} -> :0 TARGET[STALE anchor '{anchor}' "
                      f"found {found}x, not exactly one bullet directly below]")
                continue
            rule_line, target = got
            print(f"{rel}:{n} -> :{rule_line} TARGET[{target}]")
        else:
            k = int(num)
            legacy += 1
            if 1 <= k <= len(doc_lines):
                print(f"{rel}:{n} -> :{k} TARGET[{doc_lines[k - 1].strip()}] "
                      f"LEGACY[line-number cite, convert to docs/testing.md#<kebab>]")
            else:
                stale += 1
                print(f"{rel}:{n} -> :{k} TARGET[STALE line out of range "
                      f"(docs/testing.md has {len(doc_lines)} lines)]")

    print(f"{len(sites)} cites, {stale} stale, {legacy} legacy")
    return 1 if stale else 0


if __name__ == "__main__":
    sys.exit(main())
