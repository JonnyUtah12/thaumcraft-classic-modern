# RELEASE_CHECKLIST

**Redirect stub.** The canonical release procedure and gates live in
[`release-process.md`](release-process.md). The Section-19 release gates
(Gate 1 buildable → Gate 2 functional alpha → Gate 3 behavioral fidelity
RC → Gate 4 release readiness) are stated in
[`FEATURE_MATRIX.md`](FEATURE_MATRIX.md).

Release metadata that must agree at tag time: `mod_version` in
`gradle.properties` (single source of truth), `VERSION`, `CHANGELOG.md`,
and the git tag.

This file exists only so the spec'd path resolves; do not add content here.
