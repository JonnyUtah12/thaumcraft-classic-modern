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
