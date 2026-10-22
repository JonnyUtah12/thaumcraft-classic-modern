# Regression Report — 2026-10-22 (TC-M10-01)

Full regression pass over `main` at **c44b219** (TC-M8-01, accessory
provider layer). This is the baseline the release gate (TC-M10-02)
builds from.

## Verdict

**PASS.** All unit and game-test suites are green in both required
configurations; the identifier-stability harness over
`docs/content-mapping.csv` is green; previously fixed bugs carry
guards (one pending integration, listed below); the compatibility
matrix was audited row-by-row and corrected (7 stale rows found).

## Environment

- Isolated `git worktree` at c44b219 (the shared working tree carries
  six concurrent workers' uncommitted work and was not used for
  measurement).
- JDK 21 (`~/opt/jdk21`), NeoForge 21.1.259 / MC 1.21.1, Gradle
  wrapper.
- Commands: `./gradlew test`, `./gradlew runGameTestServer`,
  `./gradlew -Ptc.noCurios runGameTestServer` — the same three
  invocations CI runs (`build` job + `game-test-no-curios` job).

## Results

| Suite | Result |
| --- | --- |
| Unit (`test`) | 32 suites, **216 tests, 0 failures, 0 errors**, 3 skipped |
| Game tests, Curios config | **All 39 required tests passed** |
| Game tests, `-Ptc.noCurios` (fallback provider) | **All 39 required tests passed** |
| CI | build + game-test + game-test-no-curios jobs run on every push; the run for this pass is recorded in the TC-M10-01 board handoff |

The 3 skipped unit tests are the original-asset-overlay checks
(`OriginalSoundOverlayTest`, 2 of 3 `SoundRegistryParityTest` cases);
they require the local `../reference` asset tree (D-004) and skip
identically in CI.

### Unit suites (216)

metadata 5 · accessory 19 (codec 4, rules 5, slot model 4, bridge
isolation 1, slot data 3, curio adapter 2) · aspects 26 (combination
table 6, definitions parity 6, discovery 5, essentia tags 9) · assets 2
(1 skipped) · command 1 · config 15 (catalog integrity 12, reference
table 3) · nodes 4 · player codec 10 · registration contract 3 ·
research 41 (graph data 10, loader 7, point math 9, progression 15) ·
sound parity 3 (2 skipped) · visnet 13 · wand cost rules 11 · vis pool
math 16 · ambient vis 4 · flux model 17 · world-data 9 (node modifier
2, base 7) · worldgen 17 (biome aura parity 6, node generation rolls
11).

### Game tests (39 required, 12 classes)

Registration 3 · Smoke 1 · ConfigParity 2 · Network 2 · Persistence 3 ·
ResearchProgression 6 · AspectSystem 4 · VisCore 1 · AuraNode 6 ·
Flux 3 · SoundRegistry 1 · Accessory 7 (the accessory class runs its
full lifecycle contract in both provider configurations; the
`-Ptc.noCurios` run exercises the built-in fallback).

## Identifier stability over content-mapping.csv

- `ContentMappingIdContractTest` (unit, 3/3): every `new_id` in the CSV
  is a legal `thaumcraft:` resource location and exactly the lowercased
  original path (frozen D-001 contract).
- `RegistrationGameTest.implementedContentIdsResolve` (in-game): every
  CSV row marked `done` resolves to a live registered object; the other
  two registration tests prove registries initialize and probe content
  round-trips through save data.
- Cross-checks: research graph keys (`ResearchGraphDataTest`), essentia
  table keys, node type/modifier ids (`NodeTypeContractTest`), and the
  biome aura table (`BiomeAuraTableParityTest`) are each pinned against
  the same CSV contract. All green.

## Previously fixed bugs — guard status

| Fixed bug | Guard | Status |
| --- | --- | --- |
| Curios 9.5.1 loads slot-type JSONs only from the `curios` pack namespace (c44b219) | `CuriosSlotDataTest` (slot files match frozen model, entity grant, routing tags) + AccessoryGameTest slot registration in both configs | green |
| Provider check class-loading the Curios bridge on a no-Curios runtime (c44b219) | `CuriosBridgeIsolationTest.noCoreClassReferencesCuriosTypes` (bytecode guard) | green |
| Flaky aspect player-scan assertion (seeded HUMANUS merge, c44b219) | deterministic invariant assertion (humanus ≥ 4, vis exactly 16) in `AspectSystemGameTest` | green |
| Frozen content-id contract drift (dc91fe4) | `ContentMappingIdContractTest` + `RegistrationGameTest` | green |
| `thaumcraft:empty` game-test template missing from the jar (575c1b6) | game-test server boots and runs 39 tests from a clean checkout (CI + this pass) | green |
| runicShield sound id / overlay namespacing (fa569b7) | `SoundRegistryParityTest` registry/CSV parity (overlay-file cases skip without the local asset tree) | green (partial in CI by design) |
| Vanilla `OreFeature` places nothing at size=1; biome-modifier key is `biomes` (TC-M5-05) | `WorldGenDataParityTest` + `WorldGenGameTest` | **guards exist only in the uncommitted TC-M5-05 working-tree state — not yet in CI** (see below) |

## Matrix audit (compatibility-matrix.md)

Stale rows found at c44b219 and corrected in this pass: Aspects
(NI → IT, TC-M4-01), Vis/aura/nodes (NI → PARTIAL with TC-M4-02 +
TC-M5-01 evidence), Equipment/accessories (NI → PARTIAL, TC-M8-01),
World generation (NI → PARTIAL: nodes IT; ores marked *implemented,
not yet integrated*), Persistence (NI → PARTIAL, TC-M2-02),
Networking (NI → PARTIAL, TC-M2-03), Rendering/sounds (NI → PARTIAL,
sound-event registry TC-M7-05). A pending uncommitted edit from
TC-M5-05 that claimed ore generation as IT was superseded by the
accurate not-yet-integrated status; its content is preserved in the
new row wording and returns to IT when the integration lands.

## Not covered / integration debt

- **TC-M5-05 ore world generation is done and verified but uncommitted
  in the shared working tree** (its worker explicitly left the
  integration commit to the orchestrator). Until integrated it is not
  in CI, not in the matrix as IT, and not in built JARs. Its 13 unit
  tests + 3 game tests must be re-run at integration time.
- Six sibling workers are mid-flight (TC-M5-02, M5-04, M6-05, M6-07,
  M8-02, M9-01); their uncommitted tree state was excluded from every
  measurement above and left untouched.
- `docs/feature-tracking.md` still carries stale status lines (Vis
  core, Persistence, Sounds, Build/load foundation, and a
  TC-M6-04-row regression introduced by an uncommitted sibling edit).
  It is a known multi-worker hotspot; corrections were left to the
  row owners' integration commits rather than swept into this one.
- `docs/content-mapping.csv` was not modified (hotspot; sibling
  catalog rows in flight).
