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
