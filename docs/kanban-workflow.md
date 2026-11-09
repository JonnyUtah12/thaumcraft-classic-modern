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
- A card that is `done` or `archived` **cannot be a parent**: the toolset has no
  unlink, so any child gated on a terminal card is permanently unreachable and
  still shows as ordinary blocked work. Check a parent's status before relying on
  an edge to drain, and never gate new work on a terminal card.

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
- A **detached-HEAD** worktree's branch is literally named `HEAD`, so it can never match a remote ref and will **always** read "unpushed" — test reachability by SHA instead.
- **`add -A -- src docs` sweeps shared files** (`FEATURE_MATRIX.md`, `compatibility-matrix.md`, `en_us.json`) into a lane branch with no per-hunk check. Prefer explicit paths; if a shared file must go in, verify the diff is single-hunk and single-lane, and **name the caveat on the card** if it was not.

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
