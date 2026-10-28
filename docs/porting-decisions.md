# Porting Decisions

Every decision here is driven by the source of truth: the Thaumcraft
4.2.3.5 JAR (decompiled), the MIT-licensed Thaumcraft API 1.7.10 repo, and
the Baubles 1.7.10 repo. Adaptations are listed with reason and fidelity
impact, per the project brief.

## D-001 — Mod id and content namespace: `thaumcraft` (lowercased)

- Original: mod id `Thaumcraft`, content ids `Thaumcraft:elementium_ore`, etc.
- Minecraft 1.21.1 requires lowercase resource locations; `Thaumcraft:*` is
  impossible. Decision: mod id and content namespace `thaumcraft`, original
  paths preserved verbatim (`thaumcraft:elementium_ore`).
- Fidelity impact: ids differ from the original only in case — the closest
  the platform permits. Saves/scripts/expectations stay maximally compatible.
- The mod is clearly labelled "Unofficial" in display name and metadata to
  avoid impersonating Azanor's official release.
- TC-M2-01 addendum: "paths preserved" means lowercased only. 1.21.1 rejects
  uppercase in the path as well as the namespace, so the canonical id for
  every content entry is `thaumcraft:` + lower(original id path) — a
  mechanical, auditable rule pinned by docs/content-mapping.csv's `new_id`
  column and guarded by ContentMappingIdContractTest (unit) and
  RegistrationGameTest (in-game). Ids are frozen once shipped. Two
  documented exceptions: (1) the two original sound event ids containing
  uppercase letters were renamed to snake_case (`runicShieldCharge` →
  `runic_shield_charge`, `runicShieldEffect` → `runic_shield_effect`) —
  recorded in docs/asset-mapping.csv and docs/sound-events.md; (2) the
  original wand-trigger "ids" are registration call signatures, not resource
  locations, so those content-mapping rows share the pseudo-id
  `thaumcraft:wandtrigger` (reference-only, never registered).

## D-002 — Java package `com.jonnyutah12.thaumcraftclassic`

Original code lived in `thaumcraft.*` packages. The Java package is ours
(namespace hygiene); it has zero gameplay effect. Class names are kept
recognizable where they implement the same behavior (e.g. `ThaumcraftApi`
equivalents), but no original class is copied (see D-003).

## D-003 — Code provenance: reconstruction, not copying

Thaumcraft 4.2.3.5's implementation JAR carries no license grant
(All Rights Reserved, conservative default). Decompiled output is used as
analysis evidence only; committed code is written against documented
behavior. Exception: the **Thaumcraft API 1.7.10 repo is MIT (Azanor, 2013)**
— API interface shapes and constants may be adapted with attribution
(see docs/licensing-and-attribution.md).

## D-004 — Original assets are not committed; the local build overlays them

Textures/models/sounds/lang are All Rights Reserved and stay out of the
public repository. They are extracted from the user's own JAR into
`../reference/assets/` (git-ignored). `./gradlew build` overlays that tree
into the output JAR when present, so the artifact installed into the user's
instance carries the original visuals; CI builds a code-only JAR and tests
assert the asset mapping (docs/asset-mapping.csv) instead.
Fidelity impact: none for the user's local release; the public repo omits
assets until redistribution rights are established.

## D-005 — Baubles replacement: provider-abstracted accessory layer

Baubles 1.7.10 is CC BY-NC-SA and cannot run on 1.21.1. The port keeps the
original slot semantics (RING/AMULET/BELT/CHARM as used by TC4) behind an
internal provider abstraction; the default backend is an optional Curios
9.5.1 bridge (ATM10 ships Curios), with a built-in fallback (player data
attachment) so the core mod never requires any accessory mod.
Fidelity impact: TC equipment behaves identically; the inventory GUI comes
from Curios when present (documented UX difference vs. Baubles' own GUI).

## D-006 — Item NBT → data components; entity fields → data attachments

1.7.10 stored wand vis, focus metadata, and player research knowledge in item
NBT and player entity fields. 1.21.1 mechanisms: data components (stack
state) and player data attachments (player state, synced, death-aware).
Behavior is preserved field-by-field from the decompiled originals; only the
storage substrate changes.

## D-007 — Research data: from hardcoded Java to data-driven JSON

TC4 hardcodes its research graph in `Research.java` / page classes. The port
extracts the full graph (entries, prerequisites, stages, requirements,
page text) from the decompiled source into generated JSON data, loaded at
runtime. The graph itself is preserved exactly; the representation changes
because 1.21.1 data-driven content is the platform convention and keeps the
graph auditable. Any difference between the JSON and the original is a bug.

## D-008 — Versioning

`mod_version` in gradle.properties is the single source of truth; VERSION
mirrors it. Dev builds are `X.Y.Z-SNAPSHOT`; pre-releases are tagged
`vX.Y.Z-alpha.N` / `-beta.N`; stable is `vX.Y.Z`. Mod version is independent
of MC/NeoForge versions (pinned separately and asserted by unit tests).

## D-009 — Sibling project `thaumaturgical-legacy`

A prior, deliberately clean-room prototype (new ids, no original assets,
subset of systems) exists at `~/minecraft mods/thaumaturgical-legacy`
(MIT, same author). This port is NOT that mod: it targets full fidelity and
original ids. Its build harness, test infrastructure, accessory design, and
reverse-engineering documents are reused as engineering inputs; its gameplay
classes are re-implemented against the original behavior.

## D-010 — Configuration: one COMMON spec; numeric-id options not ported

The original config is a single Forge 1.7.10 `Configuration` file
(`Thaumcraft.cfg`, 62 options in 8 categories, all reads in
`Config.java`). The port implements the same option set as one NeoForge
`ModConfigSpec` registered as `ModConfig.Type.COMMON`
(`thaumcraft-common.toml`): same category names, option names, types,
defaults and comments, so a config file reads the same on both platforms.
Nine options that exist only to assign numeric registry ids
(`biome_*`, `outer_lands_dim`, `ench_*`, `thaumcraft_*_id`) have no
target-platform equivalent — 1.21.1 registries are name-keyed — and are
not implemented; they remain listed, marked not-ported, in
docs/config-reference.md so the parity record stays complete. The
original's read-time value clamps (golem/runic/node options) are
reproduced at read time, not by rewriting the file, exactly like the
original. Fidelity impact: none for gameplay options; the nine id options
were plumbing for 1.7.10's id-based registries.

## D-011 — Rejected orchestrator directives (target-fact errors + an unsound guardrail)

Recorded so future sessions do not re-propose them. Each item was checked
against the repository, not against recollection.

1. **"Forge 1.20.1 / Modern Forge Connect Loader (MFCL)" as the target.**
   Rejected: factually wrong. `gradle.properties` pins
   `minecraft_version=1.21.1`, `minecraft_version_range=[1.21.1]`,
   `neo_version=21.1.259`; the loader is NeoForge, verified against the
   user's ATM10 8.3 instance. There is no Forge, no 1.20.1, and no such
   component as "MFCL" in any recorded decision (D-001…D-010). Acting on
   it would fail `compileJava` and void the verified green state.

2. **Mixin injections as a verification subject.** Rejected: this project
   ships **zero** Mixins. Introducing injection points would be a design
   change requiring its own record, not a guardrail step. "Builds cleanly
   without mixin crashes" is also not this project's acceptance standard —
   the standard is 65 required game tests plus per-card test evidence.

3. **"Legacy aspect registrations `Antiquitas`, `Modernitas`, `Echo`".**
   Rejected: not Thaumcraft 4.2.3.5 aspects. The verified set is 48
   definitions at 1:1 parity with the decompiled `Aspect.java`
   (TC-M4-01 → `5ce3dee`). A task to "validate compatibility" of these
   would invent gameplay — forbidden by the preservation brief — and would
   corrupt the combination graph, since they have no original rules to port.

4. **A generative "calibrated probability" gate (Jev) over worker actions,
   wired to blocking.** Rejected as unsound here on two independent grounds.
   (a) Two of its three gates have empty subject domains (see 2 and 3), so
   they can only ever pass — that is false confidence, not a guardrail.
   (b) Scoring a model's own proposal with that same model measures
   agreement, not correctness. Every defect found during this port was found
   by deterministic checks whose output could not be predicted in advance
   (two JARs compared byte-for-byte and found identical; `link` returning
   rc=0 while parents stayed empty; an unlink pass that silently ran zero
   unlinks; a ledger reading 1028 `not_started`). A flag-and-block gate
   would also reintroduce the block-storm failure mode that stalled this
   board for three days (see D-012).

   **Correction and verified state (2026-10-27).** Jev is genuinely installed
   on this profile, and an earlier summary of this decision that called it a
   tool which "does not exist in this environment" was wrong. Verified:
   `hermes-jev` is enabled in `~/.hermes/config.yaml`, `~/.hermes/skills/jev/`
   holds 13 Jev skills, a `jev` binary is on PATH (`~/.hermes/bin/jev` →
   `~/hermes-jev-skills/bin/jev`), and the plugin catalog carries `jev-judge`,
   `jev-approvals`, `jev-skill-router` and `jev-model-router`. Its existence is
   not in question. What is rejected is narrower and unchanged: adopting a
   calibrated-probability gate as this project's **blocking** verification of
   correctness, for the two reasons above.

**What is used instead** — deterministic and free: `compileJava` / `build`
exit codes; `runGameTestServer` asserting "required tests passed"; CI green
on the pushed SHA; `git merge-base --is-ancestor` before claiming a commit
is on main; `git status` for tracked worktree deletions; the feature-matrix
generator exiting non-zero on an unparseable table; and the verified-instance
check before any ATM10 install.

**Fidelity impact:** none. This record changes no game behaviour; it exists
to prevent the loss of verified state.

## D-012 — Chain concurrency: serial integration, and the stall hazard it creates

The re-dispatch lane (the `-s` cards, created after 16 original cards hit the
150/150 iteration ceiling) is a **strictly serial** chain: each card's parent
is the previous card, so exactly one integration runs against `main` at a
time. The reason is this project's dominant historical failure: parallel
workers sharing one tree — and, in worktrees, concurrently merging to `main`
— repeatedly broke `compileJava` for each other, collided on untracked WIP,
and once produced a poisoned index with staged deletions of live code.

Observed cost, 2026-10-24 → 2026-10-27. The chain head `t_49883b51`
exhausted its iteration budget twice, `gave_up`, and sat `blocked`. Because
every downstream card is gated on the head, the dispatcher had **nothing
ready to spawn** for three days: `gateway.log` shows no dispatcher spawn for
`thaumcraft-classic-port` between 10-25 02:19 and 10-27 18:26, and
`git rev-list --count 378e8c6..origin/main` was **0** — three days of zero
commits while the board looked merely "running". Nothing escalates a
long-blocked chain head; it waits for a human to notice.

Consequences adopted:

- A blocked chain head is an **incident, not a resting state**. Re-dispatch
  it, or split it, rather than letting it sit.
- Every integration card carries `--max-runtime` (currently 2h). A **second**
  iteration-exhaustion on the same card forces a split into smaller cards —
  never a whole re-run at the same scope.
- A stall longer than a few hours, with a blocked/idle head and no new commits,
  should raise an alert rather than depend on someone checking by hand.

**Fidelity impact:** none (process only).

## D-013 — Third-party Minecraft agent skills: installed subset and authority limits

Installed 2026-10-27 from https://github.com/Jahrome907/minecraft-agent-skills
at pinned commit `30137f93fdbd4446910ceaa8776d561fd291aed1` (2026-10-11), MIT.
The clone lives outside the repository at `~/.hermes/cache/minecraft-agent-skills`.

Placed in the agent-skills tree, **not** committed to this repository:
`~/.hermes/skills/minecraft/{minecraft-modding, minecraft-testing,
minecraft-world-generation, minecraft-resource-pack}`, plus a
`PROVENANCE.md` in the same directory. Four of the thirteen. Only the
canonical `.agents/skills/` tree was copied — `.claude/skills/` and
`.codex/skills/` are generated mirrors, so copying all three would create 39
duplicate copies competing for the same skill names. Discovery was verified
rather than assumed (`skills_list` resolves all four; `skill_view` loads
`minecraft-modding` with its four references and `setup_needed: false`).

Not installed, and why: `minecraft-plugin-dev` (Paper/Bukkit/Spigot — wrong
target for a mod), `minecraft-multiloader` (Architectury/Fabric — a loader and
dependency change; D-001…D-010 pin native NeoForge with no extra runtime
dependencies), `minecraft-datapack` and `minecraft-commands-scripting` (this
project's JSON is generated from the decompiled sources, not authored as
datapack schemas), `minecraft-ci-release` (the ModDevGradle harness is proven
and both real defects in it were found by inspection), and `imagegen` /
`server-admin` / `worldedit-ops` / `essentials-ops` (different domain).

**Authority — the load-bearing part of this record.** The bundle is reviewed
against Minecraft 26.3 / Java 25, and its own audit states "No Minecraft,
Gradle, or server runtime was launched" — validation is static and was never
run against a target. This project is pinned `minecraft_version=1.21.1`,
`neo_version=21.1.259`, Java 21. Therefore:

- The skills are **advisory documentation for platform mechanics only**. They
  know nothing about Thaumcraft behaviour: they have never seen the decompiled
  tree, `docs/balance-values.md`, or `docs/content-mapping.csv`. Ground truth
  for behaviour remains the decompiled 4.2.3.5 JAR, this repo's pins, and
  green CI.
- Where a skill contradicts a pin, a recorded decision, or the decompiled
  original, **the pin/decision/original wins**, and the conflict is recorded
  in `BEHAVIORAL_DEVIATIONS.md` rather than adopted. A concrete instance
  already visible: `minecraft-modding` teaches
  `Identifier.fromNamespaceAndPath(...)` for "26.x official names" and
  1.21.4+ item-definition JSON, whereas this tree uses `ResourceLocation`
  under Mojang mappings on 1.21.1.
- Do not adopt 26.x-only idioms (pack formats, loot/predicate/worldgen JSON
  shapes, NeoForge datagen routing) into this tree, and do not "modernise" the
  recorded 1.21.1 adaptations — NBT → data components/codecs (BD-04, D-006)
  and hardcoded research → data-driven JSON (BD-05, D-007).
- A skill's bundled validator is **not** evidence and must never be cited as
  verification. Authoritative gates remain `./gradlew build` and
  `runGameTestServer` ("required tests passed"), CI green on the pushed SHA,
  `git merge-base --is-ancestor`, `git status` for tracked worktree
  deletions, and per-card test evidence.
- The repository's npm tooling (`npm install` / `run check` / `run
  sync:skills`) was deliberately **not** executed: the brief forbids running
  arbitrary scripts from reference repositories, and the README states the
  copied skill directories do not need it.

**Expected value, stated honestly.** These help with generic 1.21-adjacent
plumbing on the M5/M7 edges — worldgen feature and structure JSON, model and
blockstate wiring, GameTest shape. They cannot help the hard part of this
port, which is Thaumcraft-specific behavioural fidelity: essentia matching,
vis all-or-nothing payment ordering, the research hidden-parent quirk, and
original RNG ordering. Modest benefit on platform mechanics; none on fidelity.

**Loading policy:** per-card opt-in via `kanban --skill`, on matching cards
only, never a global auto-load. Blanket-loading thirteen skills across ~23
live cards inflates every worker's context against a fixed iteration budget —
the same budget that already produced three consecutive 150/150 ceilings
(D-012).

**Fidelity impact:** none on game behaviour. This record exists so the version
boundary is visible where the skills are used, and so they are never mistaken
for authority.
