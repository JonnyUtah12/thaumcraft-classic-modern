#!/usr/bin/env bash
# worktree-audit.sh — the single vetted worktree audit used by the repo-hygiene
# cleanup gate (docs/kanban-workflow.md, "A D-012 exhaustion makes its worktree
# a salvage site").
#
#   scripts/worktree-audit.sh <worktree-path>     one worktree
#   scripts/worktree-audit.sh --all               primary checkout + every linked worktree
#   scripts/worktree-audit.sh --all --json        JSON lines
#
# Emits the verified tuple per site:
#   path  branch  head  ahead  behind  dirty  on_origin_by_sha  reachable_from_origin_ref
#   fast_forwardable  diff_files  diff_ins  diff_del  gate  gate_reason
#
# Rules baked in — every misleading number on this board came from violating one:
#   * EVERY git call carries `-C "$wt"`. Without it, HEAD resolves in whichever
#     checkout the shell happens to sit in (this silently "verified" three pushes
#     that were never checked).
#   * ahead/behind are THREE-DOT. Two-dot (`A..B`) on a branch whose base is an
#     ancestor of main renders main's later work as mass deletions — it produced
#     "30,765 deletions" for a branch whose real delta was 4,209 insertions.
#   * Every check runs on an explicit SHA, never a branch name: a detached-HEAD
#     worktree has branch name `HEAD`, which can never match a remote ref, so a
#     name-based check always reads "unpushed".
#   * Reachability is reported twice: the exact SHA as the tip of a live ref on
#     origin (`git ls-remote`, authoritative, no fetch), and ancestry reachability
#     from any fetched `origin/*` ref. A head can be safe with no ref of its own.
#   * The gate never removes anything. It reports PASS/FAIL.
#
# Exit 0 always for --all (a FAIL row is data, not an error). For a single path,
# exit 0 = gate PASS, 1 = gate FAIL — so it can be used as a pre-removal check.

set -uo pipefail

BASE_REF="${WT_AUDIT_BASE:-origin/main}"
MODE=""
TARGET=""
JSON=0
while [ $# -gt 0 ]; do
  case "$1" in
    --all)   MODE=all ;;
    --json)  JSON=1 ;;
    -h|--help) sed -n '2,32p' "$0"; exit 0 ;;
    -*)      echo "unknown flag: $1" >&2; exit 2 ;;
    *)       MODE=one; TARGET="$1" ;;
  esac
  shift
done

# ---- locate the repository / superproject root ------------------------------
ROOT="$(git rev-parse --show-toplevel 2>/dev/null || true)"
[ -n "$ROOT" ] || { echo "not inside a git repository" >&2; exit 2; }
BASE_SHA="$(git -C "$ROOT" rev-parse --verify "$BASE_REF" 2>/dev/null)"
[ -n "$BASE_SHA" ] || { echo "cannot resolve $BASE_REF" >&2; exit 2; }

TMP="$(mktemp -d "${TMPDIR:-/tmp}/wt-audit.XXXXXX")"
trap 'rm -rf "$TMP"' EXIT

# One ls-remote for the whole run: it is the live-truth of "present on origin".
git -C "$ROOT" ls-remote origin 'refs/heads/*' > "$TMP/remote-refs" 2>/dev/null \
  || : > "$TMP/remote-refs"

# Remote-tracking tips fetched locally, so ancestry checks need no per-row fetch.
git -C "$ROOT" for-each-ref --format='%(objectname) %(refname:short)' refs/remotes/origin \
  > "$TMP/tracking-refs" 2>/dev/null || : > "$TMP/tracking-refs"

die_row() { # path branch head dirty reason
  printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
    "$1" "$2" "$3" "-" "-" "$4" "?" "?" "?" "-" "-" "-" "FAIL" "$5"
}

audit_one() {
  local wt="$1" branch head dirty ahead behind on_origin reachable ff df di dd gate reason
  local base

  branch="$(git -C "$wt" rev-parse --abbrev-ref HEAD 2>/dev/null || echo '?')"
  head="$(git -C "$wt" rev-parse HEAD 2>/dev/null || true)"
  if [ -z "$head" ]; then
    die_row "$wt" "$branch" "NOHEAD" 0 "no HEAD (unborn branch / corrupt worktree) — cannot audit"
    return 0
  fi

  dirty="$(git -C "$wt" status --porcelain 2>/dev/null | wc -l | tr -d ' ')"

  # THREE-DOT only. left = commits on base not in head (behind),
  # right = commits in head not on base (ahead).
  read -r behind ahead < <(git -C "$wt" rev-list --left-right --count \
    "$BASE_SHA"..."$head" 2>/dev/null | tr '\t' ' ')
  [ -n "${ahead:-}" ] || { ahead=0; behind=0; }

  # exact SHA as a tip of a live ref on origin (from the ls-remote snapshot)
  if grep -q "^$head" "$TMP/remote-refs"; then on_origin=yes; else on_origin=no; fi

  # ancestry reachability from any fetched origin/* ref — the test a detached
  # HEAD passes when its commit is inside someone else's pushed branch.
  if git -C "$wt" for-each-ref --contains "$head" --format='%(refname:short)' refs/remotes/origin \
       2>/dev/null | grep -q .; then
    reachable=yes
  else
    reachable=no
  fi

  # is the base an ancestor of the head (=> the landing is a fast-forward)?
  if git -C "$wt" merge-base --is-ancestor "$BASE_SHA" "$head" 2>/dev/null; then
    ff=yes
  else
    ff=no
  fi

  base="$(git -C "$wt" merge-base "$BASE_SHA" "$head" 2>/dev/null || echo "$BASE_SHA")"
  # numstat, summed — NOT `diff --shortstat`, whose prose needs fragile parsing,
  # and NOT a two-dot diff. Counts are vs the merge-base, so main's later work
  # never shows up as deletions.
  df="$(git -C "$wt" diff --numstat "$base" "$head" 2>/dev/null | wc -l | tr -d ' ')"
  di="$(git -C "$wt" diff --numstat "$base" "$head" 2>/dev/null | awk '{ if ($1 ~ /[0-9]+/) s += $1 } END { print s+0 }')"
  dd="$(git -C "$wt" diff --numstat "$base" "$head" 2>/dev/null | awk '{ if ($2 ~ /[0-9]+/) s += $2 } END { print s+0 }')"

  gate=PASS; reason="clean+ahead0+head-on-origin"
  if [ "$dirty" -gt 0 ] && [ "$ahead" -gt 0 ] && [ "$on_origin" = no ] && [ "$reachable" = no ]; then
    gate=FAIL; reason="dirty=$dirty AND ahead=$ahead AND head not on origin (worktree-only work)"
  elif [ "$dirty" -gt 0 ]; then
    gate=FAIL; reason="dirty=$dirty (uncommitted work)"
  elif [ "$ahead" -gt 0 ] && [ "$on_origin" = no ] && [ "$reachable" = no ]; then
    gate=FAIL; reason="ahead=$ahead and head $head is on no origin ref (commits exist only here)"
  elif [ "$ahead" -gt 0 ]; then
    gate=WARN; reason="ahead=$ahead (unlanded; durable on origin)"
  fi

  if [ "$JSON" = 1 ]; then
    printf '{"path":"%s","branch":"%s","head":"%s","ahead":%s,"behind":%s,"dirty":%s,"on_origin_by_sha":"%s","reachable_from_origin_ref":"%s","fast_forwardable":"%s","diff_files":%s,"diff_ins":%s,"diff_del":%s,"gate":"%s","gate_reason":"%s"}\n' \
      "$wt" "$branch" "$head" "$ahead" "$behind" "$dirty" "$on_origin" "$reachable" "$ff" "$df" "$di" "$dd" "$gate" "$reason"
  else
    printf '%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\t%s\n' \
      "$wt" "$branch" "$head" "$ahead" "$behind" "$dirty" "$on_origin" "$reachable" "$ff" "$df" "$di" "$dd" "$gate" "$reason"
  fi
}

if [ "$MODE" = one ]; then
  [ -d "$TARGET" ] || { echo "no such worktree: $TARGET" >&2; exit 2; }
  out="$(audit_one "$TARGET")"
  echo "$out"
  case "$out" in
    *PASS*) exit 0 ;;
    *)      exit 1 ;;
  esac
fi

# ---- --all: primary checkout, then every linked worktree -------------------
if [ "$JSON" = 1 ]; then
  echo "{\"head_sha_of_$BASE_REF\":\"$BASE_SHA\"}"
else
  printf '# path\tbranch\thead\tahead\tbehind\tdirty\ton_origin_by_sha\treachable_from_origin_ref\tfast_forwardable\tdiff_files\tdiff_ins\tdiff_del\tgate\tgate_reason\n'
  printf '# base %s = %s\n' "$BASE_REF" "$BASE_SHA"
fi

audit_one "$ROOT"

# git worktree list --porcelain: 'worktree <path>' then 'branch refs/heads/<name>'
# (linked worktrees only; the primary is the first block, already audited).
git -C "$ROOT" worktree list --porcelain | awk -v root="$ROOT" '
  /^worktree / { p = substr($0, 10); if (p != root) print p }
' | while IFS= read -r wt; do
  audit_one "$wt"
done
