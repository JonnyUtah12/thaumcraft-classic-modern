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

(To be filled by TC-M9 tasks with dated evidence: startup logs, gameplay
battery, performance notes, world-safety checks.)
