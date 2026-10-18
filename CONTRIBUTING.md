# Contributing

This is an unofficial preservation port of Thaumcraft 4.2.3.5 to
Minecraft 1.21.1 / NeoForge 21.1.259. The governing rule for every change:
**reproduce the original behavior; never invent gameplay.**

## Ground rules

1. The original JAR (decompiled) and the Thaumcraft API 1.7.10 repo are the
   behavioral source of truth. Balance values, ids, recipes, research graph,
   and mechanics come from evidence, not preference.
2. Do not add new features, machines, spells, or progression.
3. Do not commit decompiled Thaumcraft source or original assets
   (All Rights Reserved) — see `docs/licensing-and-attribution.md`.
4. Every system change needs a test: pure logic → unit test under
   `src/test`; game behavior → gametest under `gametest/`.
5. Update `docs/compatibility-matrix.md` and `docs/content-mapping.csv` in
   the same change when a feature's status changes.

## Workflow

- Work is tracked on the Hermes Kanban board `thaumcraft-classic-port`
  (see `docs/kanban-workflow.md`); each task lists its acceptance criteria.
- Branch from `main`, one task per branch/PR; keep history linear
  (rebase/squash on merge). No force-pushes to `main`.
- CI must pass: `./gradlew build` and `./gradlew runGameTestServer`.
- Never install unverified builds into the user's primary ATM10 instance;
  integration testing happens in the disposable instance
  (`~/minecraft mods/atm10-test`).

## Environment

JDK 21 (`~/opt/jdk21` on the development host), committed Gradle wrapper.
Reference material (decompiled sources, original assets, API clones) lives
outside the repo in `../reference/` and is never committed.
