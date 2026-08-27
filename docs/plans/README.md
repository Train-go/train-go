# Execution Plans

Execution plans are Git-native working memory for complex tasks. They preserve
enough context for another agent or human to resume work without reconstructing
intent from chat history or a partial diff.

## When To Create A Plan

Use an ephemeral plan for bounded, single-session work.

Create one durable plan when work spans sessions, coordinates contributors, has
meaningful dependencies or ordering, requires recovery steps, or would be unsafe
to resume from the diff alone.

Use `docs/templates/exec-plan.md` and place the file under `active/`.
For an explicitly authorized baseline-to-rerun Harness experiment, use
`docs/templates/harness-improvement.md` instead.

## Lifecycle

```text
docs/session/plan-<slug>.md        optional draft, reviewed before it counts
  -> promoted, and the draft is deleted
docs/plans/active/<slug>.md
  -> update progress and decisions during implementation
  -> record final validation and result
  -> move to docs/plans/completed/<slug>.md
```

The draft stage is consumer-owned and optional; see `docs/session/README.md`.
A plan lives in exactly one place at a time — a promoted draft is deleted, never
kept as a copy.

The plan is the primary task artifact. Promote a lasting product or architecture
decision into `docs/decisions/`; keep task-local choices in the plan.

## Active Plans

No active execution plans are currently indexed.
