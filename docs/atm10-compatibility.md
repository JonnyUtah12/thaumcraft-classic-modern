# ATM10 Compatibility

## Verified target environment

- Primary instance: `~/.local/share/PrismLauncher/instances/All the Mods 10 - ATM10/`
  — ATM10 **8.3** (ManagedPackVersionID 9190157), MC **1.21.1**, NeoForge
  **21.1.259** (mmc-pack.json), Java 21 (instance.cfg). 496 mods installed.
  **Untouched during development** (project rule).
- Disposable test instance: `~/minecraft mods/atm10-test` (client 496 /
  server 470 mods; pack identity verified from manifest.json; pack zips
  SHA-256 recorded by the sibling project's TL-022: client
  `24440fb355de69b2…`, server `64c703d0f3bfbe6c…`). Launch scripts
  `launch-client.sh` / `launch-server.sh` exist there.
- Curios 9.5.1+1.21.1 is present in the pack → our optional accessory
  bridge activates there; the no-Curios path is tested separately.

## Known integration surface (to be tested, not assumed)

- Magic-mod overlap: Botania, Ars Nouveau, and others exist in the pack —
  no registry collisions expected because we use the `thaumcraft` namespace
  (no other mod in the pack uses it; verify at first load).
- Common libraries: Curios (optional dep), Patchouli exists in-pack but our
  book is our own (original Thaumonomicon, not Patchouli).
- Recipe/datapack interactions: KubeJS/scripts in-pack can reference our
  ids — keeping original ids helps.
- Enchantments/effects/particles: register under our namespace; verify no
  duplicate-name errors in startup logs.

## Results

### TC-M9-01 startup battery — 2026-10-22 (EDT)

Build under test: `thaumcraft-0.1.0-alpha.1.jar` (sha256 `e014203c…`)
built from a clean worktree at `d1d7d5f + f86a444` (the TC-M9-01 fix on
top of the current main). A baseline run at `c44b219` first surfaced two
defects (below); they were fixed in `f86a444` and the whole battery was
re-run against the final jar. Instance: `~/minecraft mods/atm10-test`
(server 470 mods = 469 pack + ours; client 494 = 493 + ours). The sibling
project's `thaumaturgicallegacy-0.2.0-SNAPSHOT.jar` found installed on both
sides was moved to `disabled-mods/` — two Thaumcraft ports in one instance
is not the target environment. Harness: each run under
`systemd-run --user --scope` (server MemoryMax=10G, client 12G) escaping
the 4 GiB worker scope; client `-Xmx8G` via `launch-client.sh` default.

Dedicated server (fresh world each cold start; `eula=true`, pack defaults):

| run | build | JVM start → "Done" | Done line | shutdown |
|---|---|---|---|---|
| cold (baseline) | c44b219 | 14:23:31 → 14:25:18 (~107 s) | `Done (16.104s)` 14:25:18 | clean, "All dimensions are saved" 14:25:37 |
| cold (final) | d1d7d5f+f86a444 | 00:07:18 → 00:08:58 (~100 s) | `Done (17.147s)` 00:08:58 | clean, saved 00:09:16 |
| warm reload (same world) | d1d7d5f+f86a444 | 00:24:37 → 00:26:01 (~84 s) | `Done (2.630s)` 00:26:01 | clean |

Clean-save check: `world/data/thaumcraft_world_magic.dat` written at the
cold shutdown (319 B), loaded without any error on the warm reload, file
intact afterwards. Worlds kept at `server/world-tc-m9-baseline-c44b219`
and `server/world-tc-m9-fixed-1ea674a`.

Headless client to title screen (xvfb-run, `-Xmx8G`, offline session
`TC-M9-Battery`): JVM 00:32:07 → mod init + Curios bridge 00:32:37 →
`Sound engine started` 00:34:37 (~150 s to title). The pack's
memorysettings mod did not gate startup at 8 GB heap. Terminated with
SIGTERM after a 10 s settle.

Curios bridge: active in every run — `Accessory layer: Curios bridge active
(slots [thaumcraft.ring, thaumcraft.amulet, thaumcraft.belt,
thaumcraft.charm])` on both server and client dists; the four slot types
register at the contracted sizes (also pinned by the 42/42 gametest runs at
the final tree in both Curios and no-Curios configurations, plus 229 unit
tests green).

Error scan (every run, ERROR/FATAL lines grouped by logging mod and
attributed):

- Baseline cold (c44b219): 138 ERROR/FATAL total, **4 attributable to us**
  (fixed in `f86a444`):
  1. Astral Sorcery parse-errored our aggregate research files — AS globs
     `data/<ns>/research/*.json` across every namespace. Fixed by moving
     the aggregates to `data/thaumcraft/research_graph/` (the old path is
     also kept free for a future `thaumcraft:research` datapack registry).
  2. Vanilla/KubeJS tag validation failed `curios:thaumcraft.ring` and
     `curios:thaumcraft.amulet` — the shipped tags referenced test-probe
     item ids that only exist under the gametest property. Fixed with
     `"required": false` entries (probe gametests unaffected).
- Final cold: 134 ERROR/FATAL, **0 attributable to us**.
- Final warm reload: 134, **0 ours**.
- Final client: 47, **0 ours**.
- Registry/recipe scan: no duplicate-registry or sync errors; zero
  recipe-load errors mention our namespace.
- Model scan (client): 4 WARN lines from `ModelManager` —
  `Missing textures in model thaumcraft:blockcustomore#…` (inventory +
  variants 0/5/7). These are the expected consequence of the D-004
  asset-overlay policy: TC-M5-05 committed the port-authored blockstate/
  model JSONs while the ARR textures stay out of the jar, so a bare
  install shows missing-texture warnings for the custom ore. Not a startup
  failure; closes when the asset overlay lands (asset milestone scope).
- The 134/47 remainder is pack-internal noise, verified not ours by
  attribution: loot-table parse errors from other mods, the
  Mekanism/Aether/TwilightForest `ItemAttributeModifiers` config-timing
  EventBus errors, RuntimeDistCleaner dist noise (server only), Bumblezone
  trades, AlmostUnified tag bindings, refinedstorage's own Curios slot
  parse error, GeckoLib/iceandfire/oracle_index on the client. No mixin or
  class-loading error names a `com.jonnyutah12.thaumcraftclassic` class;
  our jar contains no mixins.

Harness note (host constraint, do not relearn): the first cold attempt was
killed by the host OOM killer during spawn-area prep — an abandoned
`runClient` dev process from Oct 18 (≈600% CPU for 4 days) plus a stale
Gradle daemon had swap fully used. Killing them left the 5 GB-heap server
comfortable inside a 10G scope on the 14 GiB host. Client and server were
run sequentially, never simultaneously.

Logs: `/tmp/tc-m9-server-{cold,cold2,cold3,warm,warm2}.log`,
`/tmp/tc-m9-client.log` (on this machine; the instance's own
`logs/latest.log` rotates per run).
