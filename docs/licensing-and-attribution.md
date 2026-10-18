# Licensing and Attribution

Investigated 2026-10-17 (this project) and 2026-10-16 (sibling project
`thaumaturgical-legacy`, findings re-verified against the same artifacts).

## Original artifacts

| Artifact | License status | Evidence | Consequence |
| --- | --- | --- | --- |
| `Thaumcraft-1.7.10-4.2.3.5.jar` (implementation) | **No license grant found** — treated as All Rights Reserved | No LICENSE/NOTICE entry in the JAR (verified via archive listing); decompiled classes carry no headers; CurseForge page shows no explicit license | Static analysis and behavioral documentation are fine; decompiled source and assets are NOT committed or published |
| `thaumcraft-api` repo, branch `1.7.10` (commit `ced30ed35097d53cdb2a843d705477e52896e54b`) | **MIT, Copyright (c) 2013 Azanor** | `LICENSE` file in the repo (verified) | API interface shapes/constants may be adapted with attribution. NOTE: this is the API only — it does not license the implementation JAR or assets. |
| `Baubles` repo, branch `1.7.10` (commit `2ad0bd6ee34752ab0a81562ed59803ae804d9fec`) | **CC BY-NC-SA 3.0** (repo README) | Verified 2026-10-16; JAR itself embeds no license text | NC/SA incompatible with this MIT project; Baubles code is not vendored or adapted. Slot semantics are re-implemented from documented behavior. |
| Textures/models/sounds/lang inside the TC JAR | All Rights Reserved (same as implementation) | Part of the unlicensed artifact | Not committed. Overlaid locally at build time from the user's own JAR (docs/porting-decisions.md D-004). |

## This repository

- Our code and docs: **MIT** (matches the account owner's other projects and
  maximizes modpack compatibility).
- Attribution: Thaumcraft is Azanor's work; the mod display name and
  `neoforge.mods.toml` description state the project is unofficial and not
  affiliated with Azanor.
- Where MIT API material is adapted, the file header credits
  `thaumcraft-api (c) 2013 Azanor, MIT`.

## Open questions (kept local, not blocking)

- Whether Azanor would grant asset redistribution for an unofficial port is
  unknown; no claim is made. If permission is ever obtained, assets move into
  the repo and this document is updated.
- "Thaumcraft" is used descriptively (naming what is ported); the project
  does not present itself as official.

## Reference material policy

Decompiled output (`../reference/decompiled/`), extracted assets
(`../reference/assets/`), and the cloned API repos
(`../reference/api-repos/`) live outside the repository and are never
committed. Decompiled code is cited as analysis evidence (class + method)
in docs and task notes, never pasted into committed sources.
