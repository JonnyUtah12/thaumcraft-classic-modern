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

   Restated 2026-10-28 as the **NeoForge `1.20.1` branch** specifically — a
   real and separate branch (last commit 2024-05-28), so it is not the same
   string as "Forge 1.20.1", but it is the same error under a new label.
   Refused for a stronger reason than a pin mismatch: the ATM10 8.3
   integration target is itself `net.minecraft=1.21.1` /
   `net.neoforged=21.1.259`, read from the instance's `mmc-pack.json`, so a
   1.20.1 artifact cannot load in ATM10 **at all**. Retargeting would make the
   project's stated primary objective impossible, not merely expensive.
   `docs/target-platform.md` already bars that surface: "1.20.x / 1.21.4+ /
   Fabric APIs — the exact 1.21.1 NeoForge API surface only." The substrate
   this port is built on is also absent there: data components and the codecs
   built on them postdate 1.20.1, and the 1.21 recipe/loot path conventions
   differ. Cost if it were ever chosen deliberately: 6 landed commits, the
   `v0.1.0-alpha.2` release, and every 1.21.1-specific adaptation (BD-04,
   D-006, D-007) invalidated.

1b. **The Thaumcraft API `1.7.10` repo is not in dispute — it is already the
   guideline.** Confirmed in use, not merely proposed: cloned at
   `../reference/api-repos/thaumcraft-api` on branch `1.7.10` at commit
   `ced30ed35097d53cdb2a843d705477e52896e54b`, the exact revision cited in
   `docs/licensing-and-attribution.md` as MIT, Copyright (c) 2013 Azanor.
   API interface shapes and constants are adapted from it with attribution
   per D-003; it does not license the implementation JAR or its assets.

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

5. **A third-party generative "persona" offered as a code-writing capability**
   (LobeHub "Minecraft Senior Developer", `java-development`, from the
   lobe-chat-agents collection, dated 2025-11-11). Rejected on three grounds.
   It is a prompt for a different host (LobeChat + MCP) whose counters are
   usage stats, not capabilities, so adopting it installs documentation and
   adds a new way to produce plausible wrong code. Its stated value is
   greenfield invention — its own showcase is "add a special item that
   triggers an explosion within a radius", alongside "enhance gameplay" and
   "maintain server stability" — which is precisely what the preservation
   brief forbids; and "complete, executable, well-commented" is
   confidence-shaped rather than verifiable, indistinguishable in tone from a
   correct port. It carries no version boundary at all (no loader, no 1.21.1 /
   NeoForge pin, and it predates the 26.x refresh), and part of its domain is
   server-plugin work this client modpack never does. Where this project wants
   that kind of help, `~/.hermes/skills/mc-style-api-migration` (derived from
   this tree) already covers it in a version-correct form: `javap` against the
   compiled NeoForge jar rather than guessing or trusting decompiled sources,
   the accessor-clash rename gotchas that cost wasted builds, per-package
   compile proof instead of drifting whole-project totals, and the rule that a
   test unreachable because main fails to compile is not a pass.

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

## D-014 — Authoritative platform documentation, and the "latest" banner trap

The binding NeoForge documentation for this project is the **versioned** page,
https://docs.neoforged.net/docs/1.21.1/gettingstarted/, which matches the pin
(`minecraft_version=1.21.1`, `neo_version=21.1.259`).

**The banner on that page is a trap, not guidance.** It reads: *"This is
documentation for NeoForged 1.21 - 1.21.1, which is no longer actively
maintained. For up-to-date documentation, see the latest version (26.1)."*
Following that link lands on 26.1 documentation, describing a loader and pack
surface this project does not target — the same contamination class as the
26.3 agent skills fenced by D-013. **Authority follows the pin.** Where the
versioned page and the "latest" page disagree, the 1.21.1 page wins, and any
difference is recorded here rather than adopted.

Verified against that page rather than assumed:

- Prerequisite "Java 21 JDK and 64-bit JVM" — satisfied: Temurin
  `21.0.12.1+1 LTS`, reporting `OpenJDK 64-Bit Server VM`.
- Build-output convention `<archivesBaseName>-<version>.jar` in `build/libs`
  — satisfied: `thaumcraft-0.1.0-alpha.2.jar` (`archivesBaseName` defaults to
  `mod_id` = `thaumcraft`).
- Workspace template — satisfied by ModDevGradle (the page covers both
  ModDevGradle and NeoGradle; this project is ModDevGradle).
- Integration issue tracker — https://github.com/AllTheMods/ATM-10 is the
  official repository and issue-tracker for *All the Mods 10 1.21.1*, i.e.
  the right place to check known conflicts during ATM10 triage (TC-M9-02).

**One gap the page exposes, tracked as its own card rather than claimed as
done:** the page states *"You should always test your mod in a dedicated
server environment."* CI currently runs `build`, `runGameTestServer`, and
`-Ptc.noCurios runGameTestServer`. A game-test server is **not** a plain
dedicated-server smoke — it does not cover `runs/server`, EULA acceptance, or
`online-mode=false`. There is no `runServer` gate in CI.

**Fidelity impact:** none directly. This record fixes which documentation is
authoritative and closes the prerequisite checks.


## D-015 — Banner colour as a registered data component (TC-M4-03-s-b)

The 16 generated banner recipe instances name `thaumcraft:banner_color` in
`result.components`. Nothing registers that component id, so the RecipeManager
rejects all 16 payloads (`No component with type: 'thaumcraft:banner_color'`)
and the dedicated-server smoke gate is red. Two ways out, chosen deliberately:

- **Register `thaumcraft:banner_color`** (chosen, `ModDataComponents.BANNER_COLOR`).
  The original writes exactly this value onto the result stack:
  `banner.func_77990_d.func_74774_a("color", (byte)a)` in
  `ConfigRecipes.initializeArcaneRecipes` (the `a < 16` loop, line 751). D-006
  already fixes the rule "original ItemStack NBT key → typed data component", and
  the other members of this same block/item family already ride that rule
  (`minecraft:block_state` for block metas, `thaumcraft:item_meta` for item
  metas). The new component has the same shape as those, value range 0..15 = the
  original's colour index.
- **Stop emitting the component** (rejected). The colour would then only be
  implied by which coloured wool the recipe asked for; the crafted banner itself
  would carry no colour, the 16 instances would collapse onto one identical
  result, and TC-M6-04d's `TileBanner.color` — an entity field, i.e. placed-tile
  state, not stack state — would have nothing to read from the crafted item. That
  deletes original data instead of representing it.

**Why not the block's `variant` property instead?** `blockwoodendevice`'s variant
property is `IntegerProperty(0, 8)`, the original meta map. Colour is 0..15 — a
different axis, and not a legal value of that property, so it cannot be smuggled
through it.

Verified against the built 21.1.259 jars rather than assumed: in an ingredient /
`key` position the only valid way to carry components is NeoForge's own
custom-ingredient object `{"type": "neoforge:components", "items": …,
"components": …}` (`DataComponentIngredient`, dispatched on a `"type"` key by
`CraftingHelper.makeIngredientMapCodec()` = `Ingredient.MAP_CODEC_NONEMPTY`); the
bare `{"neoforge:components": …}` wrapper this card inherited is not an
ingredient at all, which is why 15 more payloads were rejected.

**Fidelity impact:** the 16 banner recipes now encode the colour the originals
encoded; nothing consumes it on this card (TC-M6-04d owns the banner tile), so no
in-game behaviour changes here. The observable change is the data contract: the
RecipeManager's parse errors for our mod go from 31 to 0.

