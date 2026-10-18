# Changelog

All notable changes to Thaumcraft Classic (Unofficial) are documented here.
Format: Keep a Changelog; versioning: Semantic Versioning (see
`docs/release-process.md`). Project versions are independent of the pinned
Minecraft 1.21.1 / NeoForge 21.1.259 targets.

## [Unreleased]

### Added
- Research graph data foundation (TC-M3-01): the complete original research
  graph — 6 categories and 201 entries with pages, triggers, prerequisites,
  warp and flags — mechanically extracted from the decompiled original into
  `data/thaumcraft/research/{categories,entries}.json`
  (`scripts/extract_research_graph.py`), with a Minecraft-free model,
  loader and registry (`research/ResearchGraph*`) and tests proving the
  extraction matches the TC-M0-04 inventory entry-for-entry, the lang
  strings are verbatim from the original `en_US.lang`, all aspect/item/
  recipe references resolve, and the prerequisite graph is acyclic
  (see docs/research-graph-notes.md).
- Project foundation: NeoForge 21.1.259 / MC 1.21.1 Gradle build (ModDevGradle
  2.0.148, committed wrapper), mod metadata with explicit unofficial-port
  disclaimer, unit-test harness (metadata/pin guards), in-game smoke gametest,
  CI (build + gametest server with explicit pass assertion), documentation
  skeleton, and the local original-asset overlay pipeline (assets never
  committed; see docs/licensing-and-attribution.md).

### Known limitations
- No gameplay content yet — the port is in the foundation phase. See
  docs/compatibility-matrix.md for the authoritative status.
