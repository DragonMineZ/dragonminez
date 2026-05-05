# AI Memory: DragonMineZ

Durable memory for future AI agents working in DragonMineZ.

This file records instructions, preferences, prohibitions, decisions, and durable facts learned after reading `AI/Agents.md`. It is not a task log, changelog, scratchpad, or replacement for source inspection.

## Relationship To Other AI Docs

- `AI/Agents.md` is the local AI onboarding entry point. Always read it first.
- `AI/Context.md` stores current project architecture, systems, commands, and nuanced repo facts.
- `AI/Skills.md` describes how to package repeatable workflows.
- `AI/Memory.md` stores durable behavior guidance that remains useful across future sessions.
- If information belongs only to the current task, put it in `AI/Context.md` or a task note, not here.
- Do not duplicate broad architecture already documented elsewhere unless the memory changes future behavior.

## What To Record

Record an entry when the information is likely to affect future AI behavior across sessions.

Good candidates:

- User preferences that are stable and repo-specific.
- Explicit prohibitions or required workflows not already covered by `AI/Agents.md`.
- Durable project decisions, especially decisions that explain why code or data should be handled a certain way.
- Conventions discovered from the repo that are not obvious and not already documented elsewhere.
- Compatibility decisions that future edits must respect.
- Known pitfalls that caused bugs, failed builds, broken generated data, or user-visible regressions.
- Important repo facts that are stable and hard to rediscover quickly.

## What Not To Record

Do not record:

- Secrets, tokens, passwords, private keys, credentials, or personal data.
- Temporary task status, TODOs, command output, build logs, or debugging traces.
- Speculation, guesses, or unverified assumptions.
- Information that is already clear from `AI/Agents.md`, README files, Gradle files, or nearby source code.
- Generic coding advice that is not specific to DragonMineZ.
- One-off user requests unless the user says they should apply in the future.
- Large pasted snippets of source code.
- Sensitive details from local runtime folders such as `run/`, world saves, configs containing private server data, or database settings.
- Instructions that conflict with higher-priority system, developer, or user instructions.

## Entry Rules

- Add new entries at the top of the `Entries` section.
- Keep each entry concise and actionable.
- Prefer durable rules over narrative history.
- Include the source of the memory: user instruction, repo inspection, bug investigation, PR review, or decision.
- Include a date in `YYYY-MM-DD` format.
- Mark uncertain information as `Status: provisional` and include what would verify it.
- Remove or update stale entries when they become wrong.
- If a memory conflicts with `AI/Agents.md`, do not silently override it. Record the conflict and ask the user before relying on it.
- If an entry affects JSON formats editable by players or addon authors, explicitly call out compatibility and user-editability impact.

## Entry Template

Use this schema for each memory entry:

```markdown
### YYYY-MM-DD - Short Title

- Type: preference | prohibition | decision | durable-fact | pitfall | workflow
- Status: active | provisional | superseded
- Source: user | repo-inspection | implementation | review | debugging
- Scope: repo-wide | subsystem/path | file/path
- Summary: One sentence describing the memory.
- Guidance: What future AI agents should do because of this.
- Do Not: Optional. What future AI agents should avoid.
- Verification: Optional. How to confirm the memory is still true.
- Related: Optional. Link to `AI/Agents.md`, `AI/Context.md`, source files, issues, or PRs.
```

## Seed Entries

### 2026-05-05 - Player-Editable JSON Is A Compatibility Boundary

- Type: durable-fact
- Status: active
- Source: user
- Scope: repo-wide
- Summary: Many DragonMineZ JSON files are intended to be edited by players and addon authors.
- Guidance: Preserve readability, clear field names, lenient loading where appropriate, useful errors, and migration/backfill paths for JSON-backed user data.
- Do Not: Do not replace user-facing JSON formats with opaque or generated-only structures unless the user explicitly approves the break.
- Related: `AI/Agents.md`, `AI/Context.md`

### 2026-05-05 - Prefer Replacing Old Code Over Compatibility Branching

- Type: preference
- Status: active
- Source: user
- Scope: repo-wide
- Summary: If backward compatibility was not requested, prefer editing old code into the new behavior instead of adding parallel old/new paths.
- Guidance: Ask only when compatibility expectations are ambiguous or the change could affect users, addons, configs, saves, or public data.
- Do Not: Do not preserve old behavior by default when the user requested a direct change.
- Related: `AI/Agents.md`

### 2026-05-05 - Stage Changed Tracked Files Before Finishing

- Type: workflow
- Status: active
- Source: user
- Scope: repo-wide
- Summary: When files are edited, added, deleted, or regenerated, stage changed tracked files before finishing.
- Guidance: Use `git status --short` and stage relevant tracked changes. Respect explicit user exclusions such as "do not stage this file."
- Do Not: Do not stage unrelated dirty user changes.
- Related: `AI/Agents.md`

## Entries

Add durable memories below this line, newest first.
