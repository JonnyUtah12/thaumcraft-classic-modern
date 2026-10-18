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
