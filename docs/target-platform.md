# Target Platform — verified facts

All facts below were verified on this machine on 2026-10-17, not assumed.

## Game and loader

| Item | Value | How verified |
| --- | --- | --- |
| Minecraft | **1.21.1** | PrismLauncher instance `All the Mods 10 - ATM10/mmc-pack.json` (`net.minecraft` 1.21.1) |
| NeoForge | **21.1.259** | same `mmc-pack.json` (`net.neoforged` 21.1.259); installer JAR `neoforge-21.1.259-installer.jar` SHA-256 `bb971287ab44fc43f27da77ac34e263a3885d73fb842ceeb20ee5c517ff4378c` |
| ATM10 pack | All the Mods 10, **8.3** (ManagedPackVersionID 9190157) | `instance.cfg`; isolated copy `~/minecraft mods/atm10-test` verified earlier (TL-022) with pack manifest: MC 1.21.1 / neoforge-21.1.259 exact |
| Java (game) | Java 21 (PrismLauncher-managed Microsoft OpenJDK 21.0.7; pack runtime Temurin 21.0.12.1) | `instance.cfg` JavaVersion=21.0.7; TL-022 manifest |

## Build toolchain (this machine)

| Item | Value |
| --- | --- |
| JDK 21 | `~/opt/jdk21` (Temurin 21.0.12.1+1 LTS, includes javac/javap). Not on PATH; use absolute path or `JAVA_HOME`. |
| Gradle | Not system-wide; the committed wrapper (Gradle 9.2.1) bootstraps its own. Wrapper distribution already cached in `~/.gradle/wrapper`. |
| ModDevGradle | `net.neoforged.moddev` **2.0.148** — proven on this exact NeoForge version by the sibling project `thaumaturgical-legacy` (same MC/Neo pair builds, runs, and game-tests here). |
| Parchment | `parchment_minecraft_version=1.21.1`, `parchment_mappings_version=2024.11.17` (verified pair). |
| Optional accessory API | Curios `9.5.1+1.21.1` from `maven.theillusivec4.top` — the version ATM10 8.3 ships; compileOnly/localRuntime only. |
| Decompiler | Vineflower 1.11.1 at `~/opt/tools/vineflower-1.11.1.jar` (Apache-2.0), proven on both original JARs. |
| GitHub CLI | `gh` 2.46+ authenticated as JonnyUtah12 (token in `~/.config/gh/hosts.yml`, never printed). |

## Platform conventions this project must follow (1.21.1 / NeoForge 21.1)

- Mod metadata: `META-INF/neoforge.mods.toml` (generated from `src/main/templates`).
- Registration: `DeferredRegister` + `DeferredHolder`; holders must initialize before RegisterEvents.
- Item data: **data components** replace 1.7.10 item NBT field access (wand vis storage, focus metadata, research keys).
- Player/world state: **data attachments** (`AttachmentType`) replace 1.7.10 `IAttachment`/`PlayerKnowledge` entity fields; sync via `sync()` payloads.
- Networking: `RegisterPayloadHandlersEvent` + `CustomPacketPayload`.
- Recipes: data-driven JSON recipe types with custom recipe serializers (arcane workbench, infusion, alchemy).
- World gen: biome modifiers / `WorldCarver`/feature JSON for ore & node generation; aura/node state lives in saved data, not chunk NBT hacks.
- Client/server: `@OnlyIn` is not relied on; dist-safe code paths, dedicated-server safety is a test target.
- Resources: `assets/<ns>/...`, `data/<ns>/...` lowercase only — the original `Thaumcraft:` namespace must be lowercased (documented adaptation).

## What is NOT used

- Forge 1.7.10 / the original JARs at runtime (never loaded).
- Baubles 1.7.10 (replaced by the accessory abstraction; optional Curios bridge).
- 1.20.x / 1.21.4+ / Fabric APIs — the exact 1.21.1 NeoForge API surface only.
