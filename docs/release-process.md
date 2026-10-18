# Release Process

## Versioning

- `mod_version` in `gradle.properties` is the single source of truth;
  `VERSION` mirrors it; unit tests assert metadata consistency.
- Dev builds: `X.Y.Z-SNAPSHOT`. Pre-releases: `vX.Y.Z-alpha.N`,
  `vX.Y.Z-beta.N`. Stable: `vX.Y.Z`. Mod version is independent of the
  pinned Minecraft/NeoForge versions (which are asserted separately).
- Version bumps happen in one commit that updates gradle.properties,
  VERSION, and CHANGELOG together, tagged on the same commit.

## Release gate (all required)

1. CI green: `./gradlew build` + `./gradlew runGameTestServer` (with the
   explicit pass assertion).
2. Unit + gametest suites pass locally, including the no-Curios
   configuration (`-Ptc.noCurios`).
3. The JAR is verified: metadata correct, expected resources present
   (asset overlay when built with the reference tree).
4. `docs/compatibility-matrix.md` updated; changelog entries match what is
   actually true; no incomplete feature described as implemented.
5. Known limitations section present.

## GitHub releases

Only created when the gate passes. Include the built JAR and release notes.
Never release from an unverified commit.

## ATM10 installation

A release JAR may be installed into the user's primary ATM10 instance only
after: version match verified (MC 1.21.1 / NeoForge 21.1.259), the
disposable-instance battery passed, and the user authorized the change.
Back up anything touched. Never remove other mods to force compatibility.
