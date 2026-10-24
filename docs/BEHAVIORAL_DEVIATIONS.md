# Behavioral Deviations

Every confirmed difference between the original Thaumcraft 4.2.3.5 /
Baubles 1.0.1.10 behavior and the modern port, with cause and impact.
Per the project brief, a deviation is an exception that requires evidence
and documentation — not a redesign. The default is to preserve original
behavior; each row below records why exact preservation was not achieved
and what remains to eliminate it.

Status vocabulary: **UNAVOIDABLE** (target platform cannot express the
original), **TEMPORARY** (a tracked task exists to close it), **ACCEPTED**
(decision recorded in `porting-decisions.md`).

| # | System | Original behavior | Current behavior | Cause | Gameplay impact | Alternatives investigated | Why exact preservation failed | Temporary/Unavoidable | Next steps |
| --- | --- | --- | --- | --- | --- | --- | --- | --- | --- |
| BD-01 | Content ids | `Thaumcraft:elementium_ore` (mixed case) | `thaumcraft:elementium_ore` (lowercased) | 1.21.1 rejects uppercase in namespace and path | None (mechanical case change; saves/scripts stay maximally compatible) | Keep original case via a mapping layer | Platform constraint | UNAVOIDABLE | None (D-001) |
| BD-02 | Sound event ids | `runicShieldCharge`, `runicShieldEffect` (mixed case) | `runic_shield_charge`, `runic_shield_effect` (snake_case) | 1.21.1 resource-location rules | None (internal ids; play sites rewired) | Keep mixed case | Platform constraint | UNAVOIDABLE | None (D-001, docs/sound-events.md) |
| BD-03 | Wand-trigger "ids" | Registration call signatures | Pseudo-id `thaumcraft:wandtrigger` (reference-only, never registered) | Not resource locations in the original | None (documentation-only) | — | Not a real id in the original | UNAVOIDABLE | None (D-001) |
| BD-04 | Item/player state storage | Item NBT + player entity fields | Data components + player data attachments | 1.21.1 storage substrate | None (field-by-field preserved) | — | Platform convention | UNAVOIDABLE | None (D-006) |
| BD-05 | Research data | Hardcoded in `Research.java` / page classes | Data-driven JSON loaded at runtime | 1.21.1 data-driven convention; auditable graph | None (graph preserved exactly; any JSON/original diff is a bug) | Keep hardcoded | Platform convention | UNAVOIDABLE | None (D-007) |
| BD-06 | Accessory inventory GUI | Baubles' own GUI | Curios GUI when present; built-in fallback otherwise | Baubles 1.7.10 cannot run on 1.21.1 | UX difference only; slot semantics identical (RING×2/AMULET/BELT/CHARM) | Port Baubles standalone; embed Baubles | CC BY-NC-SA + 1.7.10-only | UNAVOIDABLE | None (D-005) |
| BD-07 | Config numeric-id options | `biome_*`, `outer_lands_dim`, `ench_*`, `thaumcraft_*_id` assign registry ids | Not implemented (listed, marked not-ported) | 1.21.1 registries are name-keyed | None (were plumbing for 1.7.10 id registries) | Emulate numeric ids | Platform constraint | UNAVOIDABLE | None (D-010) |
| BD-08 | Vis storage | Original vis pool model | Centi-vis data component, all-or-nothing payment, derived capacity | 1.21.1 data-component substrate | Preserved per TC-M0-06 cost chain; see docs/architecture/vis.md | — | Platform convention | UNAVOIDABLE | None (TC-M4-02) |
| BD-09 | sounds.json | 1.7.10 semantics (unnamespaced event keys, unprefixed file refs) | Rewritten at pipeline level: file refs prefixed `thaumcraft:`, event keys kept unnamespaced | 1.21.1 sound registry rules | None (66 events 1:1, play sites rewired) | — | Platform constraint | UNAVOIDABLE | None (TC-M7-05, fa569b7) |
| BD-10 | Research hidden-parent quirk | Empty-list prerequisite quirk in original | Reproduced | Original behavior | Preserved exactly | — | — | N/A (preserved) | None (TC-M3-02) |
| BD-11 | Persistence codecs | Original strict NBT | Lenient codecs + copyOnDeath + S2C sync | 1.21.1 attachment substrate | Preserved; lenient decode tolerates partial data | Strict codecs | Platform convention | UNAVOIDABLE | None (TC-M2-02) |
| BD-12 | Ore world-gen | Original frequencies | Implemented but NOT YET INTEGRATED into main (TC-M5-05) | Integration pending | None once integrated | — | — | TEMPORARY | Integrate TC-M5-05 (t_833087c1 → d1d7d5f precedent) |
| BD-13 | Thaumonomicon GUI | Original pages/minigames/navigation | Not implemented | Not yet ported | Missing feature | — | — | TEMPORARY | TC-M3-03/04 (t_38e57cb0, t_4ebb83af) |
| BD-14 | Interaction layer (workbench/infusion/crucible/essentia/taint/enchantments/armor/entities/foci/M7 visuals) | Original mechanics | Not implemented (WIP on shared tree, uncommitted) | Not yet ported | Missing features | — | — | TEMPORARY | Salvage card t_8aea81b5; re-dispatch BLOCKED cards |

## How to add a deviation

A new row is required whenever a committed change makes the port behave
differently from the original in any observable way (gameplay, visuals,
timing, balance, persistence, multiplayer). Record the original behavior
first, then the current behavior, then the cause. Do not classify a
difference as "modernized" or "adapted" without filling in the cause and
impact columns. Link the row to its Kanban task and commit.

## Release honesty

v0.1.0-alpha.1 (2026-10-22) is a **Gate 2 functional alpha**: the
foundation and data systems (aspects, vis, research, aura/flux/nodes,
worldgen-ores, accessories, sounds) are test-verified; the interaction
layer (BD-13, BD-14) is not. It is not a behavioral-fidelity release
candidate. See `docs/feature-tracking.md` for the full status map.
