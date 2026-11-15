# c5 sibling WIP custody (moved out of src/ by t_1c7d7967, c5-1)

The give-up tree at `wt/t_0532f205 @ b5dc5d99` carried uncommitted WIP for the WHOLE
boss family. c5-1's scope is the shared boss base + CultistLeader only, so the
sibling files were moved here (tracked, never deleted) to keep the tree compiling:

| file | owner |
|---|---|
| EntityCultistPortal.java, CultistPortalAiLogic.java | c5-2 (row 328) |
| EntityEldritchGolem.java, EldritchgolemAiLogic.java, AIBossAttackOnCollide.java | c5-3 (row 329 repoint; rename of `ai/boss/AIAttackOnCollide` — the tracked original is restored in src/) |
| EntityEldritchWarden.java, EldritchWardenAiLogic.java | c5-4 (row 324) |
| CatalogBosses.java | family-wide registration holder (c5-2/3/4 decision; c5-1 registers cultistleader in `CatalogEntities` append-only per card body) |
| sibling-tracked.patch | the tracked-file diffs these WIP files needed (import repoints in ModEntityRenderers/ModelEldritchGolem/RenderEldritchGolem/GolemMenuGameTest/CatalogGolems, the ModRegistration `CatalogBosses.init` line, the AIAttackOnCollide deletion) — apply with `git apply wip-c5/sibling-tracked.patch` after restoring the files to their src paths |

A sibling card restores its files with `git mv wip-c5/<File>.java src/main/java/com/jonnyutah12/thaumcraftclassic/<path>/` in its own commit.
These WIP files were never compiled green; treat them as drafts, not custody-verified.
