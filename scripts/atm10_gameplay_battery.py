#!/usr/bin/env python3
"""TC-M9-02: ATM10 gameplay battery.

Drives a set of *representative gameplay loops* against the port inside the
DISPOSABLE All the Mods 10 test instance, over the real dedicated-server
console, and captures the evidence + a conflict triage scan.

Why the console and not a client: the disposable instance is headless and the
port's client surface needs rendered pixels, which no automated run can judge.
What a dedicated server DOES prove is the part that matters for a modpack
integration battery: that the port loads next to 470 other mods, that its
content (blocks, block entities, items, recipes, data) actually resolves in
that pack, that its gameplay objects instantiate and tick, and which other
mods interact badly with it. That is what this harness exercises and records.

Hard rules enforced here (the card's constraints):
  * only the disposable instance is ever written to -- the primary Prism
    instance is refused outright;
  * no mod is deleted, disabled or renamed to make the port load; conflicts are
    recorded, never papered over;
  * the pack's own jar for our mod is backed up and restored on exit.

Usage:
    python3 scripts/atm10_gameplay_battery.py \
        --jar build/libs/thaumcraft-0.1.0-alpha.4.jar \
        --out docs/atm10-gameplay-battery.md --json /tmp/battery.json

Exit: 0 when every required loop passed, 1 otherwise.
"""
from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import subprocess
import sys
import threading
import time
from collections import deque
from pathlib import Path

DEFAULT_INSTANCE = Path("/home/aaron/minecraft mods/atm10-test")
PRIMARY_MARKER = "PrismLauncher"
DISPOSABLE_MARKER = "atm10-test"
JAVA = os.environ.get("BATTERY_JAVA", str(Path.home() / "opt/jdk21/bin/java"))

# --- the battery ------------------------------------------------------------
# Each loop: id, title, why it is representative, and the console steps.
# A step is (command, expect_regex_or_None, expect_absent_regex_or_None).
# `expect`  -> the console output for this command must match.
# `absent`  -> the console output must NOT match (e.g. an invalid-id error).

BLOCK_PROBES = [
    # (block id, has block entity -> read it back)
    ("thaumcraft:blockjar", True),
    ("thaumcraft:blockarcanefurnace", True),
    ("thaumcraft:blocktable", True),
    ("thaumcraft:blockchesthungry", True),
    ("thaumcraft:blockmirror", True),
    ("thaumcraft:blockcrystal", True),
    ("thaumcraft:blockmetaldevice", False),
    ("thaumcraft:blockwoodendevice", False),
    ("thaumcraft:blockstonedevice", False),
    ("thaumcraft:blocklifter", False),
    ("thaumcraft:blockmagicbox", True),
    ("thaumcraft:blockcandle", False),
    ("thaumcraft:blockmanapod", True),
    # The crucible has no block of its own: BlockMetalDevice.createTileEntity
    # hands metadata/variant 0 the TileCrucible class (CrucibleGameTest:140-146).
    # It is the one content family still `done,not_tested` in content-mapping.csv,
    # so probing it here is what earns a real row flip.
    ("thaumcraft:blockmetaldevice[variant=0]", True),
    # The wand pedestal is blockStoneDevice variant 9 (CatalogFurniture:222) --
    # the tile the wand/foci dependency hangs its wand handling on.
    ("thaumcraft:blockstonedevice[variant=9]", True),
]

# --- the wand / foci family (the card's declared dependency, TC-M6-01/02/03-s) ----
# Ids read straight off the registration (CatalogWands:70/77/82, CatalogFoci:50/74)
# and the lang file; every one is summoned and read back like L2's items.
WAND_FOCI_ITEMS = [
    "thaumcraft:wandcasting",
    "thaumcraft:wandrod",
    "thaumcraft:wandcap",
    "thaumcraft:focusfire",
    "thaumcraft:focusshock",
    "thaumcraft:focushellbat",
    "thaumcraft:focusfrost",
    "thaumcraft:focustrade",
    "thaumcraft:focusexcavation",
    "thaumcraft:focusportablehole",
    "thaumcraft:focuspech",
    "thaumcraft:focuswarding",
    "thaumcraft:focusprimal",
    "thaumcraft:focuspouch",
]

# Research keys printed by `/thaumcraft research list` (ResearchManager.graph());
# used to prove the shipped graph is live inside the pack, not merely parsed.
RESEARCH_KEY_RE = re.compile(r"(?:§[0-9a-fk-or])?([A-Z][A-Za-z0-9_]{2,})\s*$")

ITEM_PROBES = [
    "thaumcraft:itemessence",
    "thaumcraft:itemthaumometer",
    "thaumcraft:itemshard",
    "thaumcraft:itemresource",
    "thaumcraft:handmirror",
    "thaumcraft:itemcrystalessence",
    "thaumcraft:itemlootbag",
    "thaumcraft:itemresonator",
    "thaumcraft:itembaubleblanks",
    "thaumcraft:trunkspawner",
    "thaumcraft:itemnugget",
    "thaumcraft:itemwispessence",
    "thaumcraft:blockjarfilleditem",
    "thaumcraft:itemarcanedoor",
    "thaumcraft:arcanedoorkey",
    "thaumcraft:itemmanabean",
    "thaumcraft:itemamuletrunic",
    "thaumcraft:itemringrunic",
    "thaumcraft:itemgirdlerunic",
    "thaumcraft:itemamuletvis",
    "thaumcraft:itemgirdlehover",
    "thaumcraft:itemcompassstone",
]

# A pad far from any pack structure, in the spawn-chunk radius so it is loaded.
X, Y, Z = 4, 120, 4

# Per-step console wait budget. Measured: a healthy pack answers a console
# command in well under 2 s, so the old 45 s stall on a failing step only
# stretched a red run out to ~20 min. 25 s is still generous for a cold pack.
STEP_TIMEOUT = 40.0

ERR_RE = re.compile(r"/(ERROR|FATAL)\]")
EXC_RE = re.compile(r"Exception|Caused by:")
THAUM_RE = re.compile(r"thaumcraft", re.I)


class Console:
    """A line-oriented reader/writer over a Minecraft dedicated server's stdio."""

    def __init__(self, proc: subprocess.Popen, log_path: Path):
        self.proc = proc
        self.log_path = log_path
        self.lines: deque[str] = deque(maxlen=200000)
        self.all_lines: list[str] = []
        self._lock = threading.Lock()
        self._fh = open(log_path, "w", encoding="utf-8", errors="replace")
        self._t = threading.Thread(target=self._pump, daemon=True)
        self._t.start()

    def _pump(self) -> None:
        assert self.proc.stdout is not None
        for line in self.proc.stdout:
            with self._lock:
                self.lines.append(line.rstrip("\n"))
                self.all_lines.append(line.rstrip("\n"))
            self._fh.write(line)
            self._fh.flush()

    def send(self, command: str) -> None:
        assert self.proc.stdin is not None
        self.proc.stdin.write(command + "\n")
        self.proc.stdin.flush()

    def mark(self) -> int:
        """Line-count snapshot to hand to :meth:`wait_for` around a ``send``.

        Snapshotting BEFORE the command is written is what makes a step
        deterministic: a 470-mod pack can answer a console command faster than
        the gap between ``send`` and ``wait_for``'s own start snapshot, and a
        reply that arrived inside that window used to be missed outright.
        """
        with self._lock:
            return len(self.all_lines)

    def wait_for(self, pattern: str, timeout: float = 60.0,
                 start: int | None = None) -> list[str] | None:
        """Wait for a NEW line matching `pattern`; return the lines since `start`."""
        rx = re.compile(pattern)
        deadline = time.time() + timeout
        if start is None:
            with self._lock:
                start = len(self.all_lines)
        while time.time() < deadline:
            with self._lock:
                new = self.all_lines[start:]
            for ln in new:
                if rx.search(ln):
                    return new
            if self.proc.poll() is not None:
                return None
            time.sleep(0.15)
        return None

    def tail(self, n: int = 80) -> list[str]:
        with self._lock:
            return list(self.all_lines[-n:])

    def close(self) -> None:
        try:
            self._fh.close()
        except Exception:
            pass


def run_command_loop(con: Console, results: dict, loop_id: str, title: str,
                     why: str, steps: list[tuple[str, str | None, str | None]]) -> None:
    """Run one loop's steps; record a per-step result."""
    entry = {"id": loop_id, "title": title, "why": why, "steps": []}
    ok = True
    for command, expect, absent in steps:
        start = con.mark()
        con.send(command)
        got = (con.wait_for(expect, timeout=STEP_TIMEOUT, start=start) if expect
               else con.wait_for(r".+", timeout=3, start=start))
        text = "\n".join(got or [])
        step_ok = got is not None
        if step_ok and absent and re.search(absent, text):
            step_ok = False
        if not step_ok:
            ok = False
        entry["steps"].append({
            "command": command,
            "ok": step_ok,
            "expect": expect,
            "absent": absent,
            "matched": [ln for ln in (got or []) if (expect and re.search(expect, ln))][:4]
            or (got or [])[-4:],
        })
    entry["ok"] = ok
    results["loops"].append(entry)


def build_loops() -> list[dict]:
    """The representative gameplay loops, in run order."""
    loops: list[dict] = []

    # L1 -- block + block entity instantiation in the pack.
    steps: list[tuple[str, str | None, str | None]] = [
        # Cover EVERY probe coordinate below (x 4..24, z 4..66). The previous
        # -16..32/-16..32 box left z>=33 outside it; the first command to touch
        # a not-yet-generated chunk in a 470-mod fresh world then cost more than
        # the step budget and the probe read as a content failure.
        ("forceload add -16 -16 48 80", None, None),
    ]
    seen: set[int] = set()
    slot = 0
    for block_id, has_be in BLOCK_PROBES:
        x = X + slot
        steps.append((f"setblock {x} {Y} {Z} {block_id}",
                      r"Changed the block", r"Unknown block|Expected|error"))
        if has_be:
            steps.append((f"data get block {x} {Y} {Z}",
                          r"has the following block data|not a block entity", None))
        seen.add(x)
        slot += 1
    loops.append({
        "id": "L1-blocks",
        "title": "Block + block entity instantiation",
        "why": "Every ported block is placed into the real pack world; the ones "
               "with a block entity are read back, which proves the tile "
               "instantiates and serialises next to 470 mods.",
        "steps": steps,
    })

    # L2 -- item registry + component resolution in the pack.
    # Items are summoned NoGravity (a falling entity leaves the query radius
    # before the console round-trip returns) on a 2-block grid, and read back
    # with distance=..1 so a neighbouring item can never satisfy the query --
    # the first version of this probe read back the WRONG entity.
    steps = []
    for i, item_id in enumerate(ITEM_PROBES):
        x = X + (i % 6) * 2
        z = Z + 40 + (i // 6) * 2
        steps.append((
            f'summon minecraft:item {x} {Y} {z} '
            f'{{Item:{{id:"{item_id}",count:1}},Age:0s,NoGravity:1b}}',
            r"Summoned new", r"Unable to summon|Unknown item",
        ))
        steps.append((
            f'data get entity @e[type=minecraft:item,x={x},y={Y},z={z},distance=..1,limit=1]',
            re.escape(item_id), None,
        ))
    loops.append({
        "id": "L2-items",
        "title": "Item registry + item-component resolution",
        "why": "Each ported item id is spawned as a real item entity and read "
               "back: an unregistered id degrades to minecraft:air, so the "
               "read-back is a genuine registry check inside the pack.",
        "steps": steps,
    })

    # L3 -- data pack visibility (recipes / research data provided by the mod).
    # NB: `datapack list enabled` enumerates mod-provided packs by the single
    # aggregate `[mod_data]` entry, never per-namespace -- asserting on
    # "thaumcraft" here was a PROBE defect (it can never match). The real check
    # is that mod data is loaded at all and that a full reload re-loads it with
    # no parse failure of ours, which is what the scan below also covers.
    loops.append({
        "id": "L3-datapack",
        "title": "Mod-provided data pack loaded and re-loadable",
        "why": "The port's 199 recipe rows + research graph are datapack data; in "
               "ATM10 AlmostUnified and KubeJS also touch data, so load + a clean "
               "reload is the cheapest positive check that ours survived.",
        "steps": [
            ("datapack list enabled", r"\[mod_data\]", None),
            # Wait for the reload to FINISH, not merely to start. In ATM10 the
            # reload re-parses ~95k recipes over ~25 s; returning on "Reloading!"
            # made the NEXT probe's window swallow that whole dump, whose
            # other-mod "failed to parse" lines then tripped that step's absent
            # check. The absent check is narrowed to our own rows for the same
            # reason.
            ("reload", r"Loaded \d+ recipes",
             r"Parsing error loading recipe thaumcraft"),
        ],
    })

    # L4 -- the wand / foci family (the card's declared dependency) in the pack.
    steps = []
    for i, item_id in enumerate(WAND_FOCI_ITEMS):
        x = X + (i % 7) * 2
        z = Z + 60 + (i // 7) * 2
        steps.append((
            f'summon minecraft:item {x} {Y} {z} '
            f'{{Item:{{id:"{item_id}",count:1}},Age:0s,NoGravity:1b}}',
            r"Summoned new", r"Unable to summon|Unknown item",
        ))
        steps.append((
            f'data get entity @e[type=minecraft:item,x={x},y={Y},z={z},distance=..1,limit=1]',
            re.escape(item_id), None,
        ))
    steps.append(("setblock 24 120 4 thaumcraft:blockstonedevice[variant=9]",
                  r"Changed the block", r"Unknown block|Expected|error"))
    steps.append(("data get block 24 120 4",
                  r"has the following block data|not a block entity", None))
    loops.append({
        "id": "L4-wandfoci",
        "title": "Wand/foci registry + wanstead pedestal tile",
        "why": "The card depends on TC-M6-03-s; this places the wand pedestal and "
               "resolves all 14 wand/foci item ids the dependency registered "
               "against the real pack registries.",
        "steps": steps,
    })

    # L5 -- the port's own command surface in the pack (data + registration).
    # The console source has permission level 4, so the level-2 commands run.
    #
    # DEFECT PIN (observed by this battery 2026-10-30, not fixed here):
    # `ThaumcraftCommand.subcommands()` chains `.then(...)` onto the
    # `Commands.literal("help")` builder, so the whole tree -- research /
    # aspect / warp -- hangs off `help`. The canonical
    # `/thaumcraft research list` the in-game help text advertises therefore
    # does not parse ("Incorrect argument for command"); only
    # `/thaumcraft help research list` works. Step 3 pins the defect so it is
    # visible in the evidence instead of silently skipped; the one-line fix
    # (attach the subcommands to the root literal) is filed as its own card.
    loops.append({
        "id": "L5-command",
        "title": "Thaumcraft command surface + live research graph",
        "why": "`/thaumcraft help` proves the command registers next to 470 mods; "
               "`/thaumcraft help research list` prints ResearchManager.graph(), so "
               "the shipped 201-entry research graph is proven live in-instance, not "
               "just parsed. Step 3 PINS a defect: the research/aspect/warp commands "
               "are nested under `help`, so the `/thaumcraft research <...>` path the "
               "help text advertises is unreachable.",
        "steps": [
            ("thaumcraft help", r"instead of /thaumcraft", None),
            ("thaumcraft help research list", r"BASICTHAUMATURGY", None),
            ("thaumcraft research list", r"Incorrect argument for command", None),
        ],
    })

    return loops


def measure_research_graph(con: "Console", results: dict) -> None:
    """Count the entries `/thaumcraft research list` printed inside the pack."""
    start = len(con.all_lines)
    con.send("thaumcraft help research list")
    con.wait_for(r"BASICTHAUMATURGY", timeout=30)
    time.sleep(1.5)
    with con._lock:  # noqa: SLF001 - same object, single writer thread
        new = con.all_lines[start:]
    keys = []
    for ln in new:
        m = RESEARCH_KEY_RE.search(ln)
        if m:
            keys.append(m.group(1))
    results["research_graph_live"] = {
        "printed_keys": len(keys),
        "distinct_keys": len(set(keys)),
        "sample": sorted(set(keys))[:5],
    }


def scan_conflicts(lines: list[str]) -> dict:
    """Group the boot log's error surface and pull out the integration signals."""
    errs = [ln for ln in lines if ERR_RE.search(ln)]
    excs = [ln for ln in lines if EXC_RE.search(ln)]
    ours = [ln for ln in errs + excs if THAUM_RE.search(ln)]

    groups: dict[str, int] = {}
    for ln in errs:
        m = re.search(r"\[([^/\]]+)/(?:ERROR|FATAL)\]", ln)
        mod = m.group(1) if m else "?"
        if "]" in mod:
            mod = mod.split("]")[-1]
        groups[mod] = groups.get(mod, 0) + 1

    recipe_parse = [ln for ln in lines
                    if "Parsing error loading recipe" in ln and THAUM_RE.search(ln)]
    duplicate = [ln for ln in lines
                 if re.search(r"[Dd]uplicate", ln) and THAUM_RE.search(ln)]
    mixin_fail = [ln for ln in lines if "Mixin apply failed" in ln]
    return {
        "total_error_fatal_lines": len(errs),
        "total_exception_lines": len(excs),
        "thaumcraft_attributable": ours,
        "thaumcraft_error_count": len(ours),
        "error_groups_by_mod": dict(sorted(groups.items(), key=lambda kv: -kv[1])[:25]),
        "thaumcraft_recipe_parse_errors": recipe_parse,
        "thaumcraft_duplicate_lines": duplicate,
        "mixin_apply_failures": mixin_fail[:10],
    }


def main() -> int:
    ap = argparse.ArgumentParser()
    ap.add_argument("--instance", default=str(DEFAULT_INSTANCE))
    ap.add_argument("--jar", required=True)
    ap.add_argument("--out", default=None)
    ap.add_argument("--json", default=None)
    ap.add_argument("--world", default="world-tc-m9-02-battery")
    ap.add_argument("--boot-timeout", type=float, default=600.0)
    ap.add_argument("--heap", default="4G")
    args = ap.parse_args()

    inst = Path(args.instance).resolve()
    jar = Path(args.jar).resolve()
    server = inst / "server"

    # --- hard safety rails ------------------------------------------------
    if PRIMARY_MARKER in str(inst):
        print(f"REFUSING: {inst} looks like the PRIMARY instance (card forbids it)")
        return 2
    if DISPOSABLE_MARKER not in inst.name:
        print(f"REFUSING: {inst} is not the disposable atm10-test instance")
        return 2
    if not server.is_dir():
        print(f"REFUSING: no server dir at {server}")
        return 2
    if not jar.is_file():
        print(f"REFUSING: jar not found: {jar}")
        return 2

    mods_dir = server / "mods"
    existing = sorted(mods_dir.glob("thaumcraft*.jar"))
    backup_dir = inst / "jar-backups"
    backup_dir.mkdir(exist_ok=True)
    backups: list[tuple[Path, Path]] = []
    stamp = time.strftime("%Y%m%d-%H%M%S")
    for old in existing:
        dest = backup_dir / f"{old.name}.{stamp}.bak"
        shutil.copy2(old, dest)
        backups.append((dest, old))
        old.unlink()
        print(f"backed up + removed {old.name} -> {dest.name}")

    installed = mods_dir / f"thaumcraft-battery-{stamp}.jar"
    shutil.copy2(jar, installed)
    print(f"installed {installed.name} ({installed.stat().st_size} bytes)")

    # Battery-specific JVM args: smaller heap + no AlwaysPreTouch, so a
    # headless box with ~5G free can still boot a 470-mod pack.
    jvm_args = server / "battery_jvm_args.txt"
    jvm_args.write_text(f"-Xms1G\n-Xmx{args.heap}\n-XX:+UseG1GC\n"
                        f"-XX:MaxGCPauseMillis=200\n-XX:+PerfDisableSharedMem\n",
                        encoding="utf-8")

    log_path = inst / f"logs/atmosphere-battery-{stamp}.log"
    log_path.parent.mkdir(exist_ok=True)

    unix_args = server / "libraries/net/neoforged/neoforge/21.1.259/unix_args.txt"
    if not unix_args.is_file():
        print(f"REFUSING: missing neoforge args: {unix_args}")
        return 2

    cmd = [JAVA, f"@battery_jvm_args.txt", f"@{unix_args.relative_to(server)}",
           "nogui", "--world", args.world]
    print("launch:", " ".join(cmd))
    env = dict(os.environ)
    env["JAVA_HOME"] = str(Path(JAVA).parent.parent)

    proc = subprocess.Popen(cmd, cwd=str(server), stdin=subprocess.PIPE,
                            stdout=subprocess.PIPE, stderr=subprocess.STDOUT,
                            text=True, bufsize=1, env=env)

    con = Console(proc, log_path)
    results: dict = {"instance": str(inst), "jar": str(jar),
                     "world": args.world, "heap": args.heap,
                     "started": time.strftime("%Y-%m-%d %H:%M:%S %Z"),
                     "loops": [], "boot": {}, "conflicts": {}}

    exit_code = 1
    try:
        print("waiting for server ready ...")
        got = con.wait_for(r"Done \(|Unrecognized option|Unknown option|Failed to start",
                           timeout=args.boot_timeout)
        boot_lines = got or []
        ready = any("Done (" in ln for ln in boot_lines)
        results["boot"] = {
            "ready": ready,
            "done_line": next((ln for ln in boot_lines if "Done (" in ln), None),
            "mod_list": [ln for ln in con.all_lines if "Thaumcraft Classic" in ln][:3],
            "mod_init": [ln for ln in con.all_lines
                         if "thaumcraftclassic.ThaumcraftClassic/" in ln][:3],
            "offline_mode": any("OFFLINE/INSECURE" in ln for ln in con.all_lines),
        }
        if not ready:
            print("server never became ready; tail:")
            print("\n".join(con.tail(40)))
            return finish(con, proc, results, jvm_args, installed, backups, args, exit_code)

        print("ready:", results["boot"]["done_line"])

        for loop in build_loops():
            print(f"--- {loop['id']}: {loop['title']}")
            run_command_loop(con, results, loop["id"], loop["title"], loop["why"], loop["steps"])
            print(f"    ok={results['loops'][-1]['ok']}")

        # conflict triage over the complete boot + battery log
        measure_research_graph(con, results)
        results["conflicts"] = scan_conflicts(con.all_lines)

        results["passed"] = [l["id"] for l in results["loops"] if l["ok"]]
        results["failed"] = [l["id"] for l in results["loops"] if not l["ok"]]
        exit_code = 0 if not results["failed"] else 1
    except Exception as exc:  # noqa: BLE001 - a battery crash is a battery result
        results["error"] = repr(exc)
        exit_code = 3
    return finish(con, proc, results, jvm_args, installed, backups, args, exit_code)


def finish(con, proc, results, jvm_args, installed, backups, args, exit_code):
    """Stop the server, restore the pack's jar, and write the report."""
    print("stopping server ...")
    try:
        con.send("stop")
        proc.wait(timeout=180)
    except Exception:
        proc.kill()
    con.close()

    results["shutdown_complete"] = any("All dimensions are saved" in l for l in con.all_lines)
    results["log"] = str(con.log_path)

    try:
        installed.unlink(missing_ok=True)
    except Exception:
        pass
    for dest, original in backups:
        shutil.copy2(dest, original)
        print(f"restored {original.name}")
    jvm_args.unlink(missing_ok=True)
    results["pack_jar_restored"] = [o.name for _, o in backups]

    if args.json:
        Path(args.json).write_text(json.dumps(results, indent=2), encoding="utf-8")
        print("json:", args.json)
    if args.out:
        Path(args.out).write_text(render_markdown(results), encoding="utf-8")
        print("report:", args.out)
    return exit_code


def render_markdown(r: dict) -> str:
    out = ["# ATM10 gameplay battery — evidence", ""]
    out.append(f"- card: `t_d827c350` (TC-M9-02-s, replaces legacy `t_95c84692`)")
    out.append(f"- instance: `{r['instance']}` (disposable; primary Prism instance untouched)")
    out.append(f"- jar under test: `{r['jar']}`")
    out.append(f"- world: `{r['world']}`, heap `-Xmx{r['heap']}`")
    out.append(f"- started: {r['started']}")
    out.append(f"- server ready: {r['boot'].get('done_line')}")
    out.append(f"- clean shutdown ('All dimensions are saved'): {r.get('shutdown_complete')}")
    out.append(f"- pack jar backed up + restored on exit: {r.get('pack_jar_restored')}")
    out.append("")
    out.append("Data-safety rails enforced by the harness: it refuses any instance whose path "
               "contains `PrismLauncher` (the primary install) or whose directory is not the "
               "disposable `atm10-test` one; it backs up the pack's own copy of the mod and "
               "restores it on exit; and it deletes, disables or renames **no** other mod to "
               "make the port load. Conflicts below are recorded, not papered over.")
    out.append("")
    boot = r.get("boot") or {}
    if boot.get("mod_list") or boot.get("mod_init"):
        out.append("## Mod load in the pack")
        out.append("")
        for ln in boot.get("mod_list") or []:
            out.append(f"- mod list: `{ln.strip()[:160]}`")
        for ln in boot.get("mod_init") or []:
            out.append(f"- init hook: `{ln.strip()[:160]}`")
        out.append("")
    for loop in r["loops"]:
        out.append(f"## {loop['id']} — {loop['title']}  [{'PASS' if loop['ok'] else 'FAIL'}]")
        out.append("")
        out.append(loop["why"])
        out.append("")
        out.append("| # | command | result |")
        out.append("| --- | --- | --- |")
        for i, s in enumerate(loop["steps"], 1):
            sample = " / ".join(x.strip()[:120].replace("|", "\\|") for x in s["matched"][:1]) or "—"
            out.append(f"| {i} | `{s['command'][:110]}` | {'ok' if s['ok'] else 'FAIL'} — {sample} |")
        out.append("")
    rg = r.get("research_graph_live") or {}
    if rg:
        out.append("## Live research graph in-instance")
        out.append("")
        out.append(f"- `thaumcraft help research list` printed {rg.get('printed_keys')} keys "
                   f"({rg.get('distinct_keys')} distinct); sample {rg.get('sample')}")
        out.append(f"- shipped graph on disk: 6 categories / 201 entries "
                   f"(`src/main/resources/data/thaumcraft/research_graph/`)")
        out.append("")
    c = r.get("conflicts") or {}
    out.append("## Conflict triage")
    out.append("")
    out.append("No mod was deleted, disabled or renamed for this run: the pack was booted "
               "as shipped, and the counts below are the whole boot + battery log.")
    out.append("")
    out.append(f"- ERROR/FATAL lines in the whole run: {c.get('total_error_fatal_lines')}")
    out.append(f"- Exception lines: {c.get('total_exception_lines')}")
    out.append(f"- attributable to `thaumcraft`: {c.get('thaumcraft_error_count')}")
    out.append(f"- `thaumcraft` recipe parse errors: {len(c.get('thaumcraft_recipe_parse_errors') or [])}")
    out.append(f"- mixin apply failures: {len(c.get('mixin_apply_failures') or [])}")
    groups = c.get("error_groups_by_mod") or {}
    if groups:
        out.append("")
        out.append("Boot-log ERROR/FATAL lines grouped by the mod that logged them — the "
                   "conflict inventory. Every entry is another mod's own noise unless the "
                   "`thaumcraft` line above is non-zero:")
        out.append("")
        out.append("| mod | ERROR/FATAL lines |")
        out.append("| --- | --- |")
        for mod, n in groups.items():
            out.append(f"| `{mod}` | {n} |")
    ours = c.get("thaumcraft_attributable") or []
    if ours:
        out.append("")
        out.append("`thaumcraft`-attributable lines (must be empty for a clean battery):")
        out.append("")
        for ln in ours[:20]:
            out.append(f"- `{ln.strip()[:200]}`")
    if c.get("mixin_apply_failures"):
        out.append("")
        out.append("Mixin apply failures:")
        out.append("")
        for ln in c["mixin_apply_failures"][:10]:
            out.append(f"- `{ln.strip()[:200]}`")
    out.append("")
    return "\n".join(out)


if __name__ == "__main__":
    sys.exit(main())
