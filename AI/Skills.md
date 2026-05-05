# AI Skills Workflow

This document defines how DragonMineZ AI agents should turn repeated instructions into reusable workflows. Use it when creating, updating, or applying AI-facing process docs, skills, memory, and context for this repo.

## Core Rule

Prefer repeatable workflows over one-off explanations. If an instruction will be useful again, package it where future agents can find and apply it.

Use the right layer:

- `AI/Agents.md`: Local onboarding, repo rules, and operating procedure every AI agent must follow.
- `AI/Skills.md`: How to design and apply repeatable AI workflows.
- `AI/Context.md`: Current repo map, architecture notes, commands, and known hotspots.
- `AI/Memory.md`: Durable lessons, decisions, pitfalls, and conventions learned from past work.
- Codex/Superpowers skills: Runtime-loaded procedural workflows for broadly reusable techniques.

## DragonMineZ Constraints

DragonMineZ is a Forge `1.20.1` Java 17 mod with mod id `dragonminez`. Runtime data matters: many JSON files are intentionally user-editable by players, addon authors, and server owners.

When designing AI workflows for this repo:

- Preserve user-editability of JSON formats unless the task explicitly says otherwise.
- Do not assume backward compatibility is required; ask when compatibility expectations are unclear.
- Prefer replacing old code with the new intended approach when backward compatibility was not requested.
- Avoid editing generated/runtime folders unless explicitly asked: `build/`, `run/`, `run-data/`.
- Keep generated resources in sync when changing datagen providers.
- If files are changed, stage changed tracked files before finishing unless the user explicitly excludes them.

## When To Use A Skill

Use an existing skill when the task matches its trigger. Do this before exploring files or proposing a solution.

Common Superpowers triggers:

- `superpowers:brainstorming`: Use before creative feature work, behavior changes, or new components.
- `superpowers:systematic-debugging`: Use before fixing bugs, test failures, or unexpected behavior.
- `superpowers:test-driven-development`: Use before implementing features or bugfixes when tests are feasible.
- `superpowers:writing-plans`: Use when converting requirements into a multi-step implementation plan.
- `superpowers:executing-plans`: Use when following a written implementation plan.
- `superpowers:verification-before-completion`: Use before claiming work is complete, fixed, or passing.
- `superpowers:writing-skills`: Use when creating or editing reusable skill instructions.
- `superpowers:requesting-code-review`: Use after substantial implementation before merge/PR readiness.
- `superpowers:receiving-code-review`: Use when addressing review feedback.

If multiple skills apply, use process skills first, then implementation skills.

## When To Create Or Update A Workflow

Create or update a reusable workflow when:

- The same instruction has been repeated across tasks.
- Agents keep missing the same repo-specific constraint.
- A process has non-obvious ordering, validation, or failure modes.
- The task depends on DragonMineZ-specific systems such as quests, wishes, capabilities, networking, configs, storage, worldgen, localization, or generated resources.
- A workflow requires consistent verification commands or manual gameplay checks.

Do not create a skill for:

- One-off task notes.
- Facts that belong in `AI/Context.md`.
- Durable lessons that belong in `AI/Memory.md`.
- Mechanical checks that should be automated by scripts.

## Skill Design Rules

A reusable skill should be concise, triggerable, and procedural.

Recommended structure:

```markdown
---
name: short-verb-led-name
description: Use when [specific trigger/symptom/context]
---

# Skill Name

## Purpose
One or two sentences explaining the workflow.

## When To Use
Concrete triggers and exclusions.

## Workflow
Ordered steps the agent must follow.

## DragonMineZ Notes
Repo-specific constraints, paths, and validation.

## Verification
Commands, manual checks, and evidence required before completion.

## Common Mistakes
Known failure modes and how to avoid them.
```

Skill authoring guidelines:

- Start descriptions with `Use when...`.
- Describe triggering conditions, not a full process summary.
- Use names with lowercase letters, digits, and hyphens.
- Keep the main skill small; move heavy references into separate files only when needed.
- Include searchable terms future agents might use.
- Avoid narrative "what happened once" writeups.
- Include only instructions that change future behavior.

## Workflow For Packaging Instructions

1. Identify the repeated problem.
Capture the exact situation that caused the instruction to be needed. Prefer concrete examples from DragonMineZ tasks.

2. Choose the storage layer.
Put repo-wide guardrails and local onboarding in `AI/Agents.md`, current architecture in `AI/Context.md`, durable lessons in `AI/Memory.md`, and procedural reusable workflows in a skill or `AI/Skills.md`.

3. Define the trigger.
Write the condition that tells an AI agent, "use this workflow now." Include filenames, systems, symptoms, and task types where useful.

4. Write the smallest useful procedure.
Include only the steps needed to prevent mistakes and complete the task reliably.

5. Add DragonMineZ-specific context.
Mention relevant files and systems, such as:

- `src/main/java/com/dragonminez/common/DMZCommon.java`
- `src/main/java/com/dragonminez/common/network/NetworkHandler.java`
- `src/main/java/com/dragonminez/common/quest/QuestRegistry.java`
- `src/main/java/com/dragonminez/common/wish/WishManager.java`
- `src/main/java/com/dragonminez/server/storage/StorageManager.java`
- `src/generated/resources/`
- `scripts/check_lang.sh`

6. Define verification.
Require evidence, not assumptions. Prefer `.\gradlew.bat build` for broad validation, `.\gradlew.bat runData` for generated data changes, `.\gradlew.bat runClient` or `.\gradlew.bat runServer` for gameplay behavior, and targeted JSON/lang checks where relevant.

7. Document update points.
State what should be added to `AI/Memory.md` or `AI/Context.md` after the workflow reveals a reusable lesson.

8. Review for future-agent usability.
A future agent should be able to discover the workflow from the trigger, follow it without hidden context, and know how to verify completion.

## Planning Work

Before code changes, build enough context to avoid guessing.

Planning checklist:

- Read `AI/Agents.md`.
- Check `AI/Memory.md`.
- Check `AI/Context.md`.
- Inspect the files closest to the requested system.
- Identify whether JSON formats, addons, configs, generated data, or network packets are involved.
- Ask only when a decision changes behavior, compatibility, or user-facing data formats.
- Write a short plan for multi-step work before editing.

Plans should include:

- Scope of files/systems affected.
- Data compatibility expectations.
- Verification commands or manual gameplay scenario.
- Documentation or memory updates needed afterward.

## Verification Workflow

Before saying work is complete:

- Run the most relevant automated checks available.
- If a check cannot be run, state why.
- For gameplay-sensitive changes, describe the manual `runClient` or `runServer` scenario needed.
- For generated data changes, run or recommend `.\gradlew.bat runData`.
- For localization JSON, use `scripts/check_lang.sh` when the environment supports it.
- For user-editable JSON, verify missing/corrupt/custom files are handled safely.
- Use `superpowers:verification-before-completion` before final success claims.

Do not claim "fixed," "complete," or "passing" without verification evidence.

## Updating Context.md

Update `AI/Context.md` when the repo map or current working knowledge changes.

Good candidates:

- New subsystem entry points.
- New registries, packet flows, lifecycle hooks, or config managers.
- New generated-data workflow.
- Important commands or environment assumptions.
- Known places where runtime files are generated or loaded.

Keep `Context.md` factual and current. Do not store long narratives or one-off debugging history there.

## Updating Memory.md

Update `AI/Memory.md` when a lesson should persist across future sessions.

Good candidates:

- A mistake agents are likely to repeat.
- A project convention not obvious from code.
- A design decision and its reason.
- A compatibility rule for user-editable JSON.
- A verification scenario that caught a real issue.
- A recurring dependency or environment constraint.

Memory entries should be short, dated when useful, and actionable.

## Final Response Expectations

When finishing a task:

- Summarize what changed.
- Mention verification performed.
- Mention any verification not run.
- Mention updates made or recommended for `AI/Memory.md` and `AI/Context.md`.
- If files changed, stage changed tracked files before final response unless explicitly excluded.
- If no files changed, say no files were edited.
