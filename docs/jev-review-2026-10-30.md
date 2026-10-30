# Jev review — 2026-10-30

**Scope:** review this project with the Jev skills, then keep the cards moving,
applying the installed Minecraft skills as the working standard on card work.

**Method, and its limit.** Jev is a decision surface — it *picks, ranks and
gates*; it cannot adjudicate whether a port is faithful to a decompiled
original. So every finding below was produced or confirmed by a deterministic
check (a log line, a file count, a `git` object, a decompile grep). Jev is used
for triage, and a Jev verdict is recorded as a suggestion, never as evidence.
This is the D-011 posture, restated because a "review with Jev" invites the
opposite reading.

## Verified this session

1. **The dispatch lane had stopped, and the cause was infrastructure, not
   content.** Board went from `ready=0, running=0, blocked=13` to
   `ready=5, running=4, blocked=4` once nine transient blocks were cleared.
   Failure text, quoted in full:
   `Kanban owner refused: Gateway disconnected; turn outcome is unknown.` /
   `Kanban owner refused: Gateway connection failed; no local fallback`, with
   `consecutive_failures=2 | failure_limit=2`. `gateway-exit-diag.log` shows the
   gateway exiting cleanly at `2026-10-30T10:55:35Z` and again at
   `2026-10-30T11:41:53Z` — the worker crashes fall inside that window. Two
   spawn failures auto-blocked each card. Clearing them is correct: there was
   nothing wrong with the work.
2. **`hermes kanban daemon` is deprecated** — the dispatcher runs inside the
   gateway, so a dead lane is a *gateway* problem, and the gateway's logs are at
   `~/.hermes/logs/`, **not** `~/.hermes/gateway.log`. Earlier in this session I
   grepped the latter and reported "no spawns" from a file that does not exist;
   that negative was worthless and is retracted here.
3. **Dispatcher confirmed live after the fix:** `19:40:24 kanban dispatcher
   [thaumcraft-classic-port]: spawned=4`.
4. **A real Jev pass was run** — `jev triage --preset blockcheck` over the
   still-blocked cards. Output recorded in this session; treated as triage
   advice only.
5. **Jev is reachable on this machine** (`jev doctor`: key present, provider
   typesafe, `reachable: true`, `latency_ms: 107`, privacy_mode
   `redacted-text`). The 13 `jev-*` skills and the CLI's real subcommands
   (`doctor`, `gate`, `supervise`, `score`, `triage`, `route`, `rerank`,
   `pick-skill`, `choose`, `plan`, `spend`, `ladder`, …) were enumerated rather
   than assumed. There is **no** `jev.evaluate`, and no code-compatibility
   oracle — the "Jev Guardrail" that scores mixin/API conformance (D-011's
   rejected premise) does not exist in this toolset.
6. **Golem gating re-examined, and my own card is citing the wrong reason.**
   The golem package has **zero** references to taint/elemental/monster classes
   (`EntityTaint|Firebat|Pech|Cultist|Eldritch|BrainyZombie|MindSpider|Wisp` →
   none), so "gated behind the taint work" is not the real dependency. It does
   import `common.entities.ai` (**31 references**) and
   `common.entities.InventoryMob`, and the port has **0 goal classes** and no
   mob-inventory base. So the gate on the entities card is *substantially*
   justified — for the shared entity base, not for taint mobs. The gate stays;
   the rationale needs correcting in the card.
7. **Test integrity, restated as a rule:** a test that cannot fail is not
   evidence. The texture card therefore requires its guard test to be shown
   **failing before** the fix.

## The texture fix, verified by mutation (2026-10-30)

`t_2c24f643` is `done` (completed 2026-10-28 18:13) and the remap is on `main`.
Because a green suite proves nothing on its own — and because the first attempt
at this proof was **invalid** — the fix was verified by mutating the mechanism
and recording the real Gradle exit codes:

- **Baseline as landed:** `./gradlew test --tests '*OriginalTextureOverlayTest*'`
  → `BUILD SUCCESSFUL`, exit **0**.
- **Mutated** — the plural→singular rename in
  `build.gradle` (`def textureDirectoryRenames = ['blocks': 'block', …]`)
  commented out → `BUILD FAILED`, exit **1**, "There were failing tests".
  So the probe genuinely detects the defect.
- **Restored** → `diff -q` against the pre-mutation copy reports the files
  **byte-identical**; re-run → `BUILD SUCCESSFUL`, exit **0**;
  `git status --short build.gradle` clean.

**A failed first attempt, recorded because it is the exact trap this project
keeps hitting.** The first mutation matched **0 lines** — the pattern assumed
the remap and `relativePath` sat on one line, and they do not. `build.gradle`
was therefore untouched, Gradle reported `12 actionable tasks: 12 up-to-date`,
and the run printed `MUTATED exit: 0`. Read naively that reads as
"the test passes either way"; in fact **nothing had been mutated**. A mutation
check that does not confirm it changed something is not a mutation check, and
`exit: 0` from an unmutated tree is not evidence for or against the test.

**Consequence for the user's report:** the magenta missing-texture defect is
fixed on `main` but is **not in any release** — the fix completed *after* the
`v0.1.0-alpha.4` cut. Testing it requires an `alpha.5` (or a local build).

## Rejected as contamination (not port defects)

- **The mixin/MFCL/Forge-1.20.1 "compatibility gate" premise** — D-011. Pins
  stay MC 1.21.1 / NeoForge 21.1.259 / Java 21.
- **26.x skill guidance** — D-013. `minecraft-world-generation` carries 26.3
  worldgen references and `minecraft-testing` is Bukkit/MockBukkit-contaminated
  (39/35), so neither may drive a gametest card. Ground truth remains this
  repo's own compiling gametests plus
  `javap -classpath <compiledWithNeoForge…output.jar> -s <qualified.Name>`.
- **A golem card set naming ten classes that do not exist** — D-017.

## Open, needing a human decision (a Jev verdict is not consent)

- **D-016 licensing:** 231 of 241 shipped lang values are verbatim from the ARR
  `en_US.lang`, committed and riding in the public code-only jar.
- **`v0.1.0-alpha.1`** still carries its ~10.38 MB asset-included jar on a public
  release.
- **No watchdog (D-012).** This session's stall was found only because a human
  asked for a review; nothing escalates a blocked head. A cron watchdog from
  this TUI session is local-only and cannot reach the user — it needs a
  gateway-connected target.
