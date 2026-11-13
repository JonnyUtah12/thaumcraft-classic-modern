#!/usr/bin/env python3
"""BD ledger reconcile tool (t_9a2c9f91).

`docs/BEHAVIORAL_DEVIATIONS.md` diverged across live heads: one file, many
ledgers, the same id claimed by different rows on different sides of a
landing. Resolving that file with a wholesale "take ours"/"take theirs"
silently deletes rows.

The rule this tool implements, in code so it is not re-litigated per landing:

1. `main` is the INCUMBENT. Its rows keep their ids and their text verbatim.
   Nothing here ever renumbers, edits or drops an incumbent row.
2. A row a head carries that main does NOT carry is an INCOMING row. It is
   appended on the END of main's id space, at the first free id after the
   incumbent block. Never renumbered to match a head, never trimmed to make
   counts agree.
3. A candidate is REJECTED (main already owns that deviation) when
   (a) its text appears in ANY historical blob of the ledger in main's
       history - i.e. it is a stale predecessor of a row main rewrote, and
       main's version is the record,
   (b) its System cell is textually the same deviation as a main row
       (token Jaccard >= REJECT_SIM) - main's row covers it, or
   (c) main's ledger already narrates it: the fraction of the candidate's
       distinctive tokens (System label + backticked identifiers) that appear
       in main's ledger is >= COVERAGE_REJECT and the token set is big enough
       to be a fair test (>= COVERAGE_MIN_TOKENS).
   Rejections measured at the reconcile base are pinned in ADJUDICATED with
   the main id that covers them, so the decision is auditable and a re-run
   reproduces it.
4. Variants of one deviation across several heads collapse to ONE row, keyed
   by the deviation's label (the System cell before its first parenthesis):
   the most complete variant (most cell text, newest commit as the tie-break)
   is canonical, and every head that carries it is recorded in the map with
   its own old id, so each landing knows which id to cite.
5. Every appended row carries provenance in its last cell: the source head,
   its sha, the id the head used, and the card that lands the code.
6. `docs/bd-id-map.csv` is the machine-readable old->new mapping, generated.

Subcommands:
  audit     measure every head's ledger: count, novel rows, reverting rows
  emit      write the reconciled ledger + id map (main incumbent, append)
  check     assert the ledger invariants (the JUnit contract's python twin)
  revert    count incumbent rows a head's merge would delete (must be 0)
"""

import argparse
import collections
import csv
import os
import re
import subprocess
import sys

REPO = os.path.dirname(os.path.dirname(os.path.abspath(__file__)))
LEDGER = "docs/BEHAVIORAL_DEVIATIONS.md"
MAPFILE = "docs/bd-id-map.csv"
INCUMBENT_FLOOR = 45          # main's rows at the reconcile base 7933db8
REJECT_SIM = 0.50             # System-cell token Jaccard that means "same deviation"
COVERAGE_REJECT = 0.55        # fraction of a candidate's tokens main's ledger already narrates
COVERAGE_MIN_TOKENS = 12      # below this the coverage test is not a fair test
BDID = re.compile(r"^BD-(\d+)$")
ROWRE = re.compile(r"^\|\s*BD-(\d+)\s*\|")
LOOSERE = re.compile(r"^\|\s*BD-")

# Rejections adjudicated by measurement at the reconcile base: deviation label ->
# (main id that owns it, evidence). Pinned so a re-run reproduces the decision and
# a reader can audit it without re-deriving it.
ADJUDICATED = {
    "container screen backgrounds and client-visible machine state": (
        "BD-37",
        "main's BD-37 is this row: its System cell records `authored as BD-17 on wt/m7-gui, "
        "renumbered at landing`; measured token coverage of main's ledger 1.00 (20/20) for the "
        "wt/m7-gui-tau-glyphs variant and 0.69 (20/29) for the wt/m7-gui-state variant, whose "
        "only delta is `state sync closed by TC-M7-04-m4` - that fact belongs in BD-37's cells "
        "when the GUI fleet lands, not in a new row"),
    "wisp block collision": (
        "BD-19",
        "main's BD-19 documents the ported push verbatim (`the ported push (FxWispPush) reads "
        "the mote's own cell`, FxWisp.java:223 cited in the row); measured coverage 0.57"),
}

# The status classes the ledger declares, measured over every row of the
# reconciled file. The contract test parses the ledger's own vocabulary
# paragraph, so this is the list that paragraph must carry.
VOCABULARY = [
    ("UNAVOIDABLE", "target platform cannot express the original"),
    ("TEMPORARY", "a tracked task exists to close it"),
    ("ACCEPTED", "decision recorded in `porting-decisions.md`"),
    ("RESOLVED", "was a deviation, now matches the original"),
    ("ADAPTED", "the ported seam is algebraically equivalent where the original's "
                "constant is not measurable from `reference/`"),
    ("N/A", "the original has no counterpart, so parity is preserved by construction"),
    ("PERMANENT", "closed by decision; it will not be eliminated"),
    ("PARTIAL", "the shipped half is named, the remainder is a filed card"),
    ("CLOSED", "this half of the row is done; the row stays for the rest"),
    ("BY POLICY", "the shape follows a recorded decision, not a platform limit"),
    ("DEFERRED", "the row ships a contract and names what it defers"),
]

VOCAB_PARAGRAPH = "Status vocabulary: " + ", ".join(
    f"**{t}** ({d})" for t, d in VOCABULARY) + (
    ".\nMeasured over the whole ledger, every row's status uses one of these tokens;\n"
    "`BehavioralDeviationLedgerContractTest` parses this paragraph and requires every row\nto carry a declared token.")

# The rule section the reconcile appends to the ledger's preamble.
RULE_SECTION = """
**Ledger id space is `main`'s (ledger reconcile t_9a2c9f91, base `7933db8`).** The
same file carried 14-30 rows across 100+ live heads while `main` carried 45, and
three different rows claimed `BD-19` on three sides of three landings. The rule is
mechanical in `scripts/bd_ledger.py` and machine-checked by
`BehavioralDeviationLedgerContractTest` (in `./gradlew test`, CI's `build` job):

- `main`'s rows are the **incumbent**: their ids and their text are kept verbatim.
  A landing never renumbers to match a branch's numbering and never trims rows to
  make a count agree — those two moves are what produced the divergent ledgers.
- A deviation a head carries that `main` does not is appended at the first free id
  after the incumbent block (`BD-46` and above), verbatim, with its source head,
  sha, the id that head used and the landing card named in the row's last cell.
- A head's row is **rejected** — not appended — when `main` already owns that
  deviation: as a row main rewrote, as a re-wording of a main row, or as text
  main's ledger narrates. The adjudications measured at this base are pinned in
  `ADJUDICATED` in the tool: `Container screen backgrounds and client-visible
  machine state` -> **BD-37** and `Wisp block collision` -> **BD-19**.
- `docs/bd-id-map.csv` (generated by `python3 scripts/bd_ledger.py emit`) is the
  old -> new mapping every pending landing cites: rewrite your branch's citations
  to the allocated id, and where the map says your row is covered, edit that
  incumbent row's cells at landing instead of adding a row.
- The contract asserts the row count never decreases, the incumbent rows survive
  verbatim, ids are unique / zero-padded / contiguous, every `BD-NN` citation in
  the tree resolves to exactly one row, every row carries a declared status
  token, and the map resolves. A merge that takes one side of this file
  wholesale goes red.
- Land one ledger edit at a time: this file is in the conflict set of every
  queued landing, and `python3 scripts/bd_ledger.py revert` reports the
  incumbent rows a head's merge would delete (it must stay 0).
"""


def repair_id_cells(lines):
    """Move prose out of an id cell into the System cell.

    `main`'s BD-13 row carries `BD-13 (see D-019 for the GUI port posture)` in its
    id cell: the row counts in the `^| BD-` line count every card quotes and
    vanishes from a strict parse, so a merge audit never sees it. The text is
    preserved; only its cell moves. This is the only edit this card makes to an
    incumbent row.
    """
    out = []
    for line in lines:
        m = re.match(r"^\|\s*(BD-\d+)\s+\(([^)]*)\)\s*\|", line)
        if m:
            cells = [c.strip() for c in line.split("|")][1:-1]
            cells[0] = m.group(1)
            cells[1] = f"{cells[1]} (see {m.group(2).replace('see ', '', 1)})"
            line = "| " + " | ".join(cells) + " |"
        out.append(line)
    return out



def sh(args, cwd=REPO):
    return subprocess.run(args, cwd=cwd, capture_output=True, text=True)


def blob(ref, path=LEDGER):
    r = sh(["git", "show", f"{ref}:{path}"])
    return r.stdout if r.returncode == 0 else None


def rows_of(text):
    """Every `| BD-NN |` row as (id, cells, raw_line). cells excludes the id cell."""
    out = []
    for line in (text or "").split("\n"):
        m = ROWRE.match(line)
        if not m:
            continue
        cells = [c.strip() for c in line.split("|")][1:-1]
        out.append((f"BD-{m.group(1)}", cells, line))
    return out


def norm(s):
    return re.sub(r"\s+", " ", s).strip()


def sig(cells):
    """Content fingerprint of a row: every cell except the id, whitespace-folded."""
    return norm(" ".join(cells[1:])).lower()


def system_key(cells):
    return norm(cells[1]).lower()


def label(cells):
    """The deviation's label: the System cell before its first parenthesis.

    Lanes suffix the same deviation label with their own card ids and gui ids
    (`Thaumatorium window (TC-M7-04-m2c / t_8f57a102, ...)` vs `Thaumatorium
    window (TC-M7-04-m2c / t_8f57a102 and TC-M7-04-m2e / ...)`) — the label is
    what makes those one deviation, so it is the dedup key.
    """
    s = norm(cells[1]).split("(")[0].strip(" `*").strip()
    return (s or norm(cells[1]))[:80].lower()


def distinctive(cells):
    """Tokens that identify a row's subject: its label plus its backticked identifiers."""
    ids = set(x.lower() for x in re.findall(r"`([^`]+)`", " ".join(cells[1:4])))
    toks_ = set(re.findall(r"[a-z0-9_]+", label(cells))) | ids
    return {t for t in toks_ if len(t) > 2}


def coverage(cells, maintxt):
    """Fraction of a candidate's distinctive tokens that main's ledger already narrates."""
    d = distinctive(cells)
    if not d:
        return 0.0, 0
    low = maintxt.lower()
    return sum(1 for t in d if t in low) / len(d), len(d)


def candidates(heads, main_ref="origin/main", hist=None):
    """Incoming rows across `heads`: absent from main, not a stale predecessor,
    not a deviation main already owns. One entry per deviation, with every head
    that carries it, plus the rejected candidates with the reason each was cut."""
    main = incumbent_rows(main_ref)
    maintxt = blob(main_ref)
    main_sigs = {sig(c) for _i, c, _l in main}
    main_systems = [(c[0], toks(c[1])) for _i, c, _l in main]
    hist = hist if hist is not None else historical_sigs(main_ref)

    groups = collections.OrderedDict()
    rejected = []
    for ref in heads:
        t = blob(ref)
        if t is None:
            continue
        for rid, cells, _l in rows_of(t):
            s = sig(cells)
            lab = label(cells)
            if s in main_sigs:
                continue                       # main has this row verbatim
            if s in hist:
                continue                       # stale predecessor, main rewrote it
            if lab in ADJUDICATED:
                mid, why = ADJUDICATED[lab]
                rejected.append((lab, ref, rid, f"covered by {mid}: {why}"))
                continue
            st = toks(system_key(cells))
            covered = [mid for mid, mt in main_systems if jaccard(st, mt) >= REJECT_SIM]
            if covered:
                rejected.append((lab, ref, rid, f"System cell is main's {covered[0]} (Jaccard "
                                 f">= {REJECT_SIM})"))
                continue
            cov, n = coverage(cells, maintxt)
            if cov >= COVERAGE_REJECT and n >= COVERAGE_MIN_TOKENS:
                rejected.append((lab, ref, rid, f"main's ledger narrates {cov:.0%} of its "
                                 f"{n} distinctive tokens"))
                continue
            key = lab
            g = groups.setdefault(key, {"variants": [], "heads": []})
            g["heads"].append((ref, rid))
            g["variants"].append((ref, rid, cells, head_date(ref)))
    # canonical variant per deviation: most cell text, newest commit tie-break
    for g in groups.values():
        g["variants"].sort(key=lambda v: (-sum(len(c) for c in v[2]), -v[3]))
        g["canonical"] = g["variants"][0]
    # stable global order: by the canonical head's name, then its old id number
    ordered = sorted(groups.items(),
                     key=lambda kv: (kv[1]["canonical"][0],
                                     int(kv[1]["canonical"][1].split("-")[1])))
    return ordered, rejected


def toks(s):
    return set(re.findall(r"[a-z0-9]+", s.lower()))


def jaccard(a, b):
    if not a or not b:
        return 0.0
    return len(a & b) / len(a | b)


def head_date(ref):
    out = sh(["git", "log", "-1", "--format=%ct", ref]).stdout.strip()
    return int(out) if out.isdigit() else 0


def head_sha(ref):
    return sh(["git", "log", "-1", "--format=%H", ref]).stdout.strip()


def all_heads():
    out = sh(["git", "for-each-ref", "--format=%(refname:short)", "refs/remotes/origin"]).stdout
    return [r for r in (x.strip() for x in out.split("\n"))
            if r and r != "origin/main" and not r.endswith("/HEAD")]


def historical_sigs(main_ref="origin/main"):
    """Row fingerprints of the ledger in EVERY blob of the file in main's history.

    A row that appears in one of these blobs is a stale predecessor of a row
    main already rewrote: main's version is the incumbent record, so the
    candidate is not a new deviation.
    """
    commits = sh(["git", "rev-list", main_ref, "--", LEDGER]).stdout.split()
    seen = set()
    for c in commits:
        t = blob(c)
        if t is None:
            continue
        for _id, cells, _l in rows_of(t):
            seen.add(sig(cells))
    return seen


def incumbent_rows(main_ref="origin/main"):
    return rows_of(blob(main_ref))


CARDS = {  # head -> the kanban card that lands its code (measured from the board)
    "origin/wt/m7-gui": "t_e4e67fd6",
    "origin/wt/m7-gui-crafting": "t_e4e67fd6",
    "origin/wt/m7-gui-devices": "t_e4e67fd6",
    "origin/wt/m7-gui-golem": "t_e4e67fd6",
    "origin/wt/m7-gui-pouch": "t_e4e67fd6",
    "origin/wt/m7-gui-research-table": "t_590193c0",
    "origin/wt/m7-gui-tagdraw": "t_e4e67fd6",
    "origin/wt/m7-gui-tau-glyphs": "t_e4e67fd6",
    "origin/wt/m7-gui-thaumatorium": "t_e4e67fd6",
    "origin/wt/m7-gui-state": "t_e4e67fd6",
    "origin/wt/t_fee89bb7": "t_e4e67fd6",
    "origin/wt/inkwell-recipe": "t_35921e6e",
    "origin/wt/knowfrag-recipe": "t_9eefdde6",
    "origin/wt/thaumonomicon-recipe-page": "t_9eefdde6",
    "origin/wt/book3a": "t_eaa49d35",
    "origin/wt/t_28ce4d49": "t_28ce4d49",
    "origin/wt/t_6799fd3f": "t_28ce4d49",
    "origin/wt/t_6d464a8b": "t_6d464a8b",
    "origin/wt/t_f04c44fb": "t_f04c44fb",
    "origin/wt/t_065502ba": "t_6563ce50",
    "origin/wt/t_42864caf": "t_42864caf",
    "origin/wt/t_45b89dd2": "t_6563ce50",
    "origin/wt/t_47316a9b": "t_6563ce50",
    "origin/wt/t_4f36e7d7": "t_4f36e7d7",
    "origin/wt/t_5eb27d02": "t_6563ce50",
    "origin/wt/t_6563ce50": "t_6563ce50",
    "origin/wt/t_fd5265f3": "t_6563ce50",
    "origin/wt/t_20ad0211": "t_20ad0211",
    "origin/wt/t_7bf6a4d0": "t_20ad0211",
    "origin/wt/t_aebd14cf": "t_20ad0211",
    "origin/wt/m7-renderers": "t_215d6fd4",
    "origin/wt/m7-fx-wisps": "t_ed46eb80",
}


def card_for(ref):
    return CARDS.get(ref, "unassigned")


def provenance(ref, rid, newid, canonical_sha):
    return (f"Id allocated by the ledger reconcile (t_9a2c9f91): this row is "
            f"`{rid}` on `{ref}` @ {canonical_sha[:7]}, kept verbatim and renumbered to "
            f"{newid} because `main` is the incumbent id space; the code lands with "
            f"{card_for(ref)}.")


def build(main_ref, heads):
    main = incumbent_rows(main_ref)
    ordered, rejected = candidates(heads, main_ref)
    rows = [line for _i, _c, line in main]
    mapping = []
    # number from the highest incumbent id, not from the parsed row count: a
    # row whose id cell carries prose (BD-13) is invisible to the strict parse,
    # and counting rows would then reuse a live id
    n = max(int(i.split("-")[1]) for i, _c, _l in main)
    for key, g in ordered:
        n += 1
        newid = f"BD-{n:02d}"
        ref, rid, cells, _d = g["canonical"]
        sha = head_sha(ref)
        cells = list(cells)          # index 0 is the head's own id cell
        cells[0] = newid             # main-incumbent rule: the id cell is main's; the text is verbatim
        last = norm(cells[-1]).rstrip(".")
        if "|" in last:
            last = last.replace("|", "/")
        cells[-1] = f"{last}. {provenance(ref, rid, newid, sha)}"
        if len(g["heads"]) > 1:
            cells[-1] += (" Canonical of %d variants (fullest record); also carried as %s."
                          % (len(g["heads"]), ", ".join(
                              f"`{old}` on `{h}`" for h, old in g["heads"] if h != ref)))
        rows.append("| " + " | ".join(cells) + " |")
        for h, old in g["heads"]:
            mapping.append({
                "head": h, "head_sha": head_sha(h), "old_id": old, "new_id": newid,
                "card": card_for(h), "canonical": "yes" if h == ref else "no",
                "system": norm(cells[1])[:160],
            })
    return main, rows, mapping, rejected


def write_ledger(main_ref, rows):
    lines = repair_id_cells((blob(main_ref) or "").split("\n"))
    # the ledger declares its own status vocabulary; make the declaration match
    # the classes the rows actually use (measured over the reconciled file)
    for i, l in enumerate(lines):
        if l.startswith("Status vocabulary:"):
            j = i
            while j < len(lines) and lines[j].strip():
                j += 1
            lines[i:j] = VOCAB_PARAGRAPH.split("\n")
            break
    # document the rule where the next merge conflict gets resolved
    for i, l in enumerate(lines):
        if re.match(r"^\|\s*(#|Id)\s*\|\s*System", l):
            lines[i:i] = RULE_SECTION.split("\n")
            break
    # insert the appended rows directly after the last incumbent row line
    last = max(i for i, l in enumerate(lines) if ROWRE.match(l))
    lines[last + 1:last + 1] = rows[len(incumbent_rows(main_ref)):]
    return "\n".join(lines)


def cmd_audit(a):
    heads = all_heads() if not a.heads else a.heads
    main = incumbent_rows(a.main)
    hist = historical_sigs(a.main)
    print(f"main {head_sha(a.main)[:7]}: {len(main)} rows "
          f"(BD-01..BD-{len(main):02d}), {len(hist)} historical row fingerprints")
    for ref in sorted(heads):
        t = blob(ref)
        if t is None:
            continue
        rs = rows_of(t)
        ids = [i for i, _c, _l in rs]
        dupes = [i for i, c in collections.Counter(ids).items() if c > 1]
        print(f"  {ref:52} rows={len(rs):3} dupes={','.join(dupes) if dupes else '-'}")
    ordered, rejected = candidates(heads, a.main, hist)
    print(f"\nnovel deviations across {len(heads)} heads: {len(ordered)}")
    for key, g in ordered:
        ref, rid, cells, _ = g["canonical"]
        print(f"  {rid:6} {ref:42} heads={len(g['heads']):3} {norm(cells[1])[:92]}")
    print(f"\nrejected (main already owns the deviation): {len(rejected)}")
    seen = set()
    for lab, ref, rid, why in rejected:
        if lab in seen:
            continue
        seen.add(lab)
        print(f"  {norm(lab)[:60]:62} {ref.replace('origin/wt/', ''):28} {rid}  {why[:110]}")


def cmd_emit(a):
    heads = all_heads() if not a.heads else a.heads
    main, rows, mapping, _rejected = build(a.main, heads)
    if a.dry_run:
        for r in rows[len(main):]:
            print(r[:220])
        print(f"\n{len(rows) - len(main)} appended rows; {len(mapping)} map entries")
        return
    ledger = write_ledger(a.main, rows)
    with open(os.path.join(REPO, LEDGER), "w", encoding="utf-8", newline="\n") as f:
        f.write(ledger)
    with open(os.path.join(REPO, MAPFILE), "w", encoding="utf-8", newline="\n") as f:
        w = csv.DictWriter(f, fieldnames=["head", "head_sha", "old_id", "new_id",
                                          "card", "canonical", "system"])
        w.writeheader()
        w.writerows(mapping)
    print(f"wrote {LEDGER}: {len(rows)} rows ({len(main)} incumbent + "
          f"{len(rows) - len(main)} appended)")
    print(f"wrote {MAPFILE}: {len(mapping)} entries")


def cmd_check(a):
    path = os.path.join(REPO, a.file)
    rows = rows_of(open(path, encoding="utf-8").read())
    ids = [i for i, _c, _l in rows]
    fails = []
    if len(rows) < INCUMBENT_FLOOR:
        fails.append(f"row count {len(rows)} < incumbent floor {INCUMBENT_FLOOR}")
    dupes = [i for i, c in collections.Counter(ids).items() if c > 1]
    if dupes:
        fails.append(f"duplicate ids: {dupes}")
    nums = [int(i.split("-")[1]) for i in ids]
    if nums != list(range(1, len(nums) + 1)):
        fails.append(f"ids not contiguous from 01: {nums}")
    for _i, cells, line in rows:
        if not re.match(r"^\|\s*BD-\d{2,}\s*\|", line):
            fails.append(f"id cell not exactly `BD-NN`: {line[:48]}")
    # the two counting methods must agree, or a row hides from a merge audit
    loose = sum(1 for l in open(path, encoding="utf-8").read().split("\n") if LOOSERE.match(l))
    if loose != len(rows):
        fails.append(f"loose `^| BD-` count {loose} != strict parse {len(rows)}")
    # every row keeps the table's shape, and carries a status the file declares
    text = open(path, encoding="utf-8").read().split("\n")
    header = next((l for l in text if re.match(r"^\|\s*(#|Id)\s*\|\s*System", l)), None)
    vi = next((i for i, l in enumerate(text) if l.startswith("Status vocabulary:")), None)
    vocab = ""
    if vi is not None:
        vend = next((i for i in range(vi, len(text)) if not text[i].strip()), len(text))
        vocab = " ".join(text[vi:vend])
    declared = [m.strip() for m in re.findall(r"\*\*([A-Za-z][A-Za-z /]*?)\*\*", vocab)]
    if header is None:
        fails.append("no table header row found")
    else:
        ncol = len(header.split("|")) - 2
        for _i, _cells, line in rows:
            n = len(line.split("|")) - 2
            if n == ncol:
                continue
            # incumbent rows are kept verbatim by the reconcile rule, so a row
            # main already ships short is reported, not fixed here (measured at
            # the base: BD-13 ships 9 cells, BD-39 ships 8)
            if int(_i.split("-")[1]) > INCUMBENT_FLOOR:
                fails.append(f"appended row {_i} has {n} cells, the table has {ncol}")
            else:
                print(f"  NOTE incumbent {_i} ships {n} cells, the table has {ncol}")
    if not declared:
        fails.append("`Status vocabulary:` paragraph declares no **token** classes")
    for _i, cells, _l in rows:
        body = norm(" ".join(cells[1:])).lower()
        if not any(t.lower() in body for t in declared):
            fails.append(f"{_i} carries no declared status token (declared: {declared})")
    # every citation in the tree resolves to exactly one row
    idset = set(ids)
    cited = collections.Counter()
    out = sh(["git", "grep", "-ohE", r"BD-[0-9]+", "--", "docs", "src", "scripts",
              "README.md", "CHANGELOG.md"]).stdout
    for tok in out.split("\n"):
        tok = tok.strip()
        if tok:
            cited[tok] += 1
    for tok, n in sorted(cited.items()):
        if tok not in idset:
            fails.append(f"cited {tok} resolves to 0 rows ({n} citations)")
    # provenance on appended rows
    for i, cells, _l in rows:
        if int(i.split("-")[1]) > INCUMBENT_FLOOR and "t_9a2c9f91" not in cells[-1]:
            fails.append(f"appended row {i} carries no reconcile provenance")
    print(f"{a.file}: {len(rows)} rows, {len(cited)} distinct cited ids")
    for f in fails:
        print("  FAIL", f)
    return 1 if fails else 0


def cmd_revert(a):
    """Incumbent rows a head's merge would delete, under the reconcile rule.

    For each head: merge-tree it onto the reconciled main, read the merged
    ledger, and count incumbent ids that are absent or whose fingerprint
    changed. 0 is the requirement; >0 means the head would silently delete
    rows (the hazard this card exists for).
    """
    heads = all_heads() if not a.heads else a.heads
    main = incumbent_rows(a.main)
    fingerprints = {i: sig(c) for i, c, _l in main}
    total = 0
    for ref in sorted(heads):
        t = sh(["git", "merge-tree", "--write-tree", a.main, ref])
        # exit 0 = clean merge, exit 1 = conflicted merge: BOTH print the merged
        # tree oid on the first line, and the conflicted case is exactly the one
        # worth measuring, since that is where a wholesale side-pick happens
        first = t.stdout.strip().split("\n")[0]
        if t.returncode > 1 or not re.fullmatch(r"[0-9a-f]{40}", first):
            print(f"  {ref:52} merge-tree unusable: {t.stdout.strip()[:120]}")
            continue
        merged = sh(["git", "show", f"{first}:{LEDGER}"]).stdout
        mrows = {i: sig(c) for i, c, _l in rows_of(merged)}
        gone = [i for i, f in fingerprints.items() if i not in mrows]
        changed = [i for i, f in fingerprints.items() if i in mrows and mrows[i] != f]
        total += len(gone)
        flag = "OK " if not gone else "DEL"
        conf = "" if t.returncode == 0 else " (CONFLICTED)"
        print(f"  {flag} {ref:52} rows={len(mrows):3} incumbent_rows_deleted={len(gone):3}"
              f" incumbent_rows_rewritten={len(changed):3}{conf}")
    print(f"\ntotal reverting rows across {len(heads)} heads: {total}")
    return 1 if total else 0


def main():
    p = argparse.ArgumentParser(description=__doc__)
    p.add_argument("--main", default="origin/main")
    sp = p.add_subparsers(dest="cmd", required=True)
    for name in ("audit", "emit", "revert"):
        s = sp.add_parser(name)
        s.add_argument("heads", nargs="*")
        if name == "emit":
            s.add_argument("--dry-run", action="store_true")
    s = sp.add_parser("check")
    s.add_argument("--file", default=LEDGER)
    a = p.parse_args()
    rc = {"audit": cmd_audit, "emit": cmd_emit, "check": cmd_check, "revert": cmd_revert}[a.cmd](a)
    sys.exit(rc or 0)


if __name__ == "__main__":
    main()
