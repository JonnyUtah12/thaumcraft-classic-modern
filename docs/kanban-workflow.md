# Kanban Workflow

Board: Hermes Kanban board slug **`thaumcraft-classic-port`**
(displayed as "Thaumcraft Classic — 1.21.1 Port").

## Columns (Hermes Kanban statuses → project columns)

| Project column | Kanban status | Meaning |
| --- | --- | --- |
| BACKLOG | `triage`/`todo` | Specified tasks, not yet startable (parents incomplete) |
| READY | `ready` | All dependencies done; dispatcher may start |
| IN PROGRESS | `running` | A worker is executing |
| REVIEW | `review` | Implemented, handed off for review via kanban_request_review |
| TESTING | (task-body gate) | Verification stages run inside the task; results recorded in the card + docs |
| BLOCKED | `blocked` | External blocker (kind: dependency/needs_input/capability/transient) |
| DONE | `done` | Acceptance criteria met, evidence in completion metadata |

Hermes Kanban has fixed statuses; TESTING is enforced as a task-body
requirement (evidence in the completion summary) rather than a column.

## Milestones (task-id prefix)

- `TC-M0` Environment and baseline
- `TC-M1` Native mod foundation
- `TC-M2` Core gameplay infrastructure (registration, persistence, networking, config)
- `TC-M3` Research and knowledge (graph, Thaumonomicon, progression)
- `TC-M4` Magic, aspects, and crafting (aspects, vis, arcane/infusion/alchemy, essentia)
- `TC-M5` World, aura, nodes, flux, taint
- `TC-M6` Blocks, items, entities, equipment
- `TC-M7` Rendering, particles, audio
- `TC-M8` Baubles compatibility and integrations
- `TC-M9` ATM10 compatibility
- `TC-M10` Regression testing and release

## Task rules

- Every task: unique id, scope, dependencies (Kanban parents), acceptance
  criteria, test requirements, doc requirements, risks, source classes.
- A task is DONE only with evidence: test output, game-log excerpts, or
  in-game verification. Compilation alone never completes a gameplay task.
- `docs/feature-tracking.md` maps every original feature to its task id.
- Parallel work only on non-overlapping files; the dependency graph enforces
  ordering on shared subsystems.
- Test/probe construction rules live in `docs/testing.md` (`## Rules`) — read it
  before writing a `@GameTest`. It carries the two failures that have cost this
  repo the most: probes in a batch share one level and a packing grid that
  **moves** when probes are added (a green run does not protect the next run),
  and a probe must never argue coverage from a single seeded `RandomSource`
  stream — assert the set of outcomes actually drawn.
- **Prefer prescriptive cards for mechanical work.** When a task is the
  registration of already-grounded identifiers, give the exact ids in the card
  and state that reading `../reference` is forbidden. Measured on the
  enchantment lane: three cards covered one slice — the two that were free to
  re-derive were archived having burned their entire iteration budgets, while
  the prescriptive one finished. It removes both this repo's most expensive
  failure mode (fabricated class names, D-017) and the cost of re-discovery.

## One milestone code, one live card

A duplicated card code makes two workers do one job. Measured 2026-11-09: two
live cards both carried `TC-M3-04-s-iii` — one burned `150/150` twice (plus two
`7200 s` wall-clock runs on its split child) re-authoring files that the other
had already landed, with the landed class eventually citing its own superseding
card in its javadoc. The cost was four dead runs for work that was already on
`main`.

- Before filing, **grep the board for the code**, not only for the title. The
  duplicate here had a different title and a different card id.
- When a scope is re-filed, **retire the original in the same breath** — point it
  at the card that supersedes it. An un-retired original reads as live work.
- **A `done` or `archived` parent RELEASES its children; what gates them is a
  parent that is *not* terminal.** Read off the source, not inferred:
  `recompute_ready` promotes a `todo`/`blocked` child once every parent is in
  `('done','archived')` (`kanban_db.py:2067`); `complete_task` and `archive_task`
  both call it, so the release is immediate (`:2739`, `:3829`); `_parents_satisfied`
  (`:2106`) is the same predicate. So **never fix a stuck child by completing or
  archiving its parent** — that is the operation that frees it. A `blocked` parent
  (retries exhausted) never reaches a terminal state, which is why such a child
  sits in `todo` cycle after cycle. Measured 2026-11-09: an umbrella card that had
  exhausted its retries gated its own two split children across three notifications.
- **Two things strand a child — check both before blaming the edge.** (1) A parent
  that is not `done`/`archived`. (2) The child's **own** breaker:
  `recompute_ready` skips a `blocked` child whose `consecutive_failures` has
  reached the effective limit (`:2079`), so a card that exhausted its retries does
  not auto-recover even when its parents *are* terminal. Measured 2026-11-09: a
  split child of an archived card sat `blocked` not because of the archive
  (archiving releases children) but because its own counter was at the limit.
- **When an edge is false, `unlink` it — do not re-file.** `unlink_tasks` (`:1611`)
  deletes the link and re-gates the child immediately: fixed in place, no card
  recreated, no lineage lost. This is the right repair when a child was gated on a
  card whose scope *is* the children's own (an umbrella/split parent), because the
  edge is false rather than stale. **Do not complete or archive the parent to
  "free" the child**: both also call `_cleanup_workspace` (`:2740`, `:3832`) and
  will reap the worktree.
- **Do not `unblock` a card to unstick its child.** `unblock_task` resets
  `consecutive_failures` to `0` (`:3582`), re-arming a fresh retry on a card whose
  work may already be on `main`.
- **An assignee that is not a profile is silently UNSPAWNABLE.** The dispatcher
  buckets a card whose `assignee` fails `profile_exists` into
  `skipped_nonspawnable` (`kanban_db_dispatch.py:2077`): no crash, no error, and
  health telemetry suppresses the stuck signal (`:2074`), so the card reads `ready`
  forever and the condition never expires on its own (`:2081`). Measured
  2026-11-09: **15 live cards** on one board carried `assignee='@default'` — the
  *display* form the notifications render — and had produced **zero** runs, against
  **184** for `default`; no other board on the machine used the value. Every one
  was created by copying the notification template. **Pass the profile name
  (`default`), never `@default`.** Repair: `hermes kanban assign <task_id> default`.

## A notification is not a summary of card state

The board's notifications render a **trailing event line**, not a description of
where the card stands. The same card emits different templates as different
trailing events land, and the header can be flatly false about the card's state.

- **`dispatcher will retry` on a `done` card is template text.** Every claim path
  CASes from a non-terminal status — `claim_task` from `ready`, and
  `recompute_ready` promotes only from `todo`/`blocked` — so a `done` card is never
  dispatched again, however many times the template fires.
- **A `timed_out` event with `run_id = NULL` is not a run.** Measured 2026-11-09:
  run 335 `completed`, the worker was reaped, and a late run-less `timed_out` was
  recorded ~11 minutes later — which then emitted a *second* notification
  ("timed out; dispatcher will retry") for a card that had been `done` throughout.
  It is an accounting artifact, and a **rare** one: board-wide, 1 of 103
  `timed_out` events carried `run_id = NULL`. Do not generalize it into a class.
- **Test the claim, never the header.** A retry is real only when
  `status = 'running'` **and** `current_run_id IS NOT NULL` **and** a run row is
  open (no `ended_at`, outcome not in `(completed, timed_out)`). `hermes kanban show`
  plus the `task_runs` rows settle it; the notification cannot.

## `done` is not `landed`

A card reaching `done` means its worker finished and pushed. It does **not** mean
its work is on `main`. Measured 2026-11-09: **58 local branches carry commits
ahead of `origin/main`** (plus 11 remote-only refs with no local branch) against
**137 `done` cards** — roughly half the finished work is unlanded, and the
pattern has produced a `done` parent whose `done` child is *also* unlanded.

- A `done` card with a **pushed, unmerged** branch is an **open** item. It needs
  a named consolidation owner — an existing landing card where one exists, not a
  new card per discovery.
- `git merge-base --is-ancestor <tip> origin/main` is the only test of landed. A
  branch's own green CI is **not** evidence: it is green against its own base,
  which may be 80–90 commits stale.
- Enumerate from `git ls-remote origin 'refs/heads/*'`, **not** `git branch` —
  remote-only heads are invisible to `git branch` by construction.
- Detached-HEAD worktrees always read as unpushed (`refs/heads/HEAD` cannot
  exist). Test reachability, never a branch name.
- **Do not prune worktrees until the unlanded inventory is captured.** A
  stale-looking worktree may hold the only copy of uncommitted work.
- **Land on green, never on red.** A landing whose baseline is red cannot
  attribute its own failures; that is how `main` stayed red unnoticed.

## A D-012 exhaustion makes its worktree a salvage site

When a card is blocked for `Iteration budget exhausted (150/150)` — `consecutive_failures=2`,
`effective_limit=2`, `limit_source=dispatcher` — do **not** unblock or re-dispatch it. **Preserve first:**

1. `git -C <worktree> status --short`, then commit the worktree's `src/` and `docs/` — **explicit paths**, on the branch's own name.
2. **Push it**, then confirm **by SHA**: local `HEAD` == `git ls-remote origin refs/heads/<branch>`.
3. Record the SHA on the card and state plainly that it is **uncompiled and ungated**.
4. **Then** split the card.

**Why:** the worker died mid-edit, so its diff exists **only in the worktree**. The branch holds no such commit, no stash carries it, and `wip-salvage` does not either — so one `git worktree remove` + `git clean -fdx` destroys it silently. This preserved ~4,200 lines in one session, twice.

### Cleanup gate — all three, per worktree, recorded BEFORE any sweep

1. `dirty == 0`
2. three-dot `ahead == 0` (`git rev-list --count origin/main...HEAD`)
3. the head is **present on origin**, confirmed by **SHA** (`git ls-remote origin`)

**Two-dot is forbidden.** `git diff --stat origin/main..HEAD` on a branch whose base is an ancestor of main renders main's later work as mass deletions — it reported *30,765 deletions* for a branch whose real delta was **4,209 insertions**.

### Measurement rules that have bitten this board

- **Pass `-C <worktree>` and an explicit SHA.** `git branch -r --contains HEAD` without `-C` silently evaluates the *main checkout's* HEAD and will "verify" pushes that were never checked.
- **`-C` binds the repository, not the shell's cwd — so a `HEAD:` refspec in a `-C` push is the `-C` repo's head.** `git -C "$repo" push origin HEAD:refs/heads/<branch>` run from inside a *temp* worktree pushes **main's tip**, not the worktree you were preserving. Measured 2026-11-09: 24 `salvage/*` refs were created this way, all pointing at main's tip while the 23 salvage commits stayed local; the verification line printed main's SHA, which is only useful if the comparison is a string compare, not a glance. **Push a SHA expanded from the thing being preserved:** `SHA=$(git -C "$wt" rev-parse HEAD); git -C "$repo" push origin "$SHA:refs/heads/<branch>"`, then require `git ls-remote origin refs/heads/<branch>` to equal `$SHA`.
- **Never type a SHA you know only by prefix.** Completing `c84c6a2c3e09…` by hand produced `fatal: bad object` — a fabricated 40-char string. Expand from source (`git rev-parse <ref>`) in the refspec itself.
- **A dirty count is not a measure of unique work.** The 203 dirty entries at `t_5eb27d02` classified per-path: **197 blobs already byte-identical on origin, 5 unique**. Classify per path — hash the worktree file and test the blob against origin's trees (`git ls-tree -r <ref>` over all fetched `refs/remotes/origin/*`) — before calling a site a salvage site, and say which number you are reporting.
- A **detached-HEAD** worktree's branch is literally named `HEAD`, so it can never match a remote ref and will **always** read "unpushed" — test reachability by SHA instead.
- **Re-audit a live site before acting.** `wt/m7-fx-decal` read `dirty=22, ahead=1, not on origin` (the worst exposure in the population) and 30 minutes later read `dirty=0, on origin` — its own worker pushed it. An exposure list is a snapshot, not a queue.
- **`add -A -- src docs` sweeps shared files** (`FEATURE_MATRIX.md`, `compatibility-matrix.md`, `en_us.json`) into a lane branch with no per-hunk check. Prefer explicit paths; if a shared file must go in, verify the diff is single-hunk and single-lane, and **name the caveat on the card** if it was not. `docs/feature-tracking.md` measured **10+ divergent working copies** across the worktree fleet, so a shared tracking file in a *lane* branch is a collision you are creating, not preserving — put such snapshots on a `salvage/*` branch, which is declared uncompiled/ungated by construction.

### The cleanup gate is one script: `scripts/worktree-audit.sh`

`scripts/worktree-audit.sh <worktree>` prints the verified tuple and the gate verdict; `--all` covers the **primary checkout plus every linked worktree** (the primary is the tree every landing writes to, and is *not* in `.worktrees/*`; **stashes are in neither** — `git stash list` + a per-entry blob check belongs in the manifest too).

```
path  branch  head  ahead  behind  dirty  on_origin_by_sha  reachable_from_origin_ref
fast_forwardable  diff_files  diff_ins  diff_del  gate  gate_reason
```

- `ahead`/`behind` come from `git rev-list --left-right --count <base>...<head>` — **three-dot**, split, so a branch whose base is an ancestor of main never reports main's later work as deletions.
- `on_origin_by_sha` = the exact SHA is the tip of a live ref on origin (`git ls-remote`, fetched once per run). `reachable_from_origin_ref` = the SHA is inside some fetched `origin/*` history — the test a detached HEAD passes; a head can be safe with no ref of its own.
- Enumerate remote truth from `git ls-remote origin 'refs/heads/*'`, never from `git branch`: refs that exist only on origin (measured: 8 of 92 unlanded) are **invisible** to a local listing, so a `refs/heads`-based manifest reads them as non-existent.
- `gate=PASS` requires all three conditions (dirty=0, three-dot ahead=0, head present on origin). The script **never removes anything** — it exits 1 on FAIL so it can gate a removal command.
- Count the unlanded refs by **ancestry cluster**, not by ref: 92 unlanded refs = 56 clusters (the 6-head golem lane is ONE landing). Distinct tips disprove aliasing, not independence.
- Exit code: for a single path, 0 = gate PASS, 1 = gate FAIL (usable as the pre-removal check). For `--all` a FAIL row is data.

## A notification can describe an INTERMEDIATE attempt, not the card's end state

A receipt's template text is fixed when the notification is generated and is **not** re-evaluated
against the card's later history. A receipt reading *"completed, gave up (retries exhausted),
timed out; dispatcher will retry"* can therefore arrive for a card that **completed successfully
hours or days earlier**.

**Read the card's own run history before acting on the receipt's verdict** — `hermes kanban show <id>`,
and look for the trailing `[run N] completed`. Exemplar: `t_94bff7e9` (c1 firebat) — four `timed_out`
runs at `7220 / 7221 / 7225 / 7226s` (each within ~6s of the 7200s cap), then `[run 294] completed`
in `1798s`, while the receipt still read "gave up … will retry".

### Three shapes, three remedies — do not confuse them

- **Wall-clock timeouts clustered at the cap** -> the gate is too slow for the budget. Time the first
  gate; do **not** split the card for "scope". (The completing worker's own note: *gate cost is ~90s warm*.)
- **`Iteration budget exhausted (150/150)` with `consecutive_failures=2`** -> D-012: split the card.
- **A receipt whose verdict is contradicted by the run history** -> the receipt is stale: record and close.

### A receipt's SHA is a snapshot, not the branch head

`t_94bff7e9` cited `wt/m6-06sc-monsters @ 50cf600` while the head had since moved to `d795cb1`
(a later salvage on the same branch). Deciding test: `git merge-base --is-ancestor <sha> origin/main`
— landed means stale. Do not conflate the cited SHA with the current head, and do not read a branch's
`ahead=N` as the receipt's work.
