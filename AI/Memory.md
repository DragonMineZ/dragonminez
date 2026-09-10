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

### 2026-09-10 - 1.21 Render Conventions The 1.20.1 Port Carried Over

- Type: pitfall
- Status: active
- Source: debugging
- Scope: `src/main/java/com/dragonminez/client/`
- Summary: Ki Sense's entity overlay rendered nothing, then had clouds bleed through it, and its search auras drifted off their mobs, because three Minecraft 1.21 render conventions differ from 1.20.1 while the ported code still used the old ones.
- Guidance: (1) Camera#rotation gained an extra 180 degrees about Y in 1.21, and vanilla answered by flipping the X sign of the nameplate billboard - `EntityRenderer#renderNameTag` now scales `(0.025F, -0.025F, 0.025F)`, not 1.20.1's `(-0.025F, -0.025F, 0.025F)`. Anything drawing above an entity must use `RenderBufferUtil.nameplateBillboard`, which owns those signs; the mirrored variant inverts quad winding and is silently back-face culled during the entity pass. (2) `RenderLevelStageEvent#getPoseStack()` is an identity stack in 1.21 - `LevelRenderer` keeps the camera rotation on `RenderSystem.getModelViewStack()` instead. `MultiBufferSource` and `BufferUploader` draws pick that up automatically, but `VertexBuffer#drawWithShader` takes the model-view matrix explicitly, so it needs the view supplied through `RenderBufferUtil.stageModelViewPose(event)`. (3) The entity pass is not the end of the main target - clouds, weather and the fabulous composites still follow it - so a world-space overlay drawn there with the depth test off (which also stops it writing depth) gets painted over; capture its pose during the entity pass and replay it at `AFTER_LEVEL`, and set blending explicitly there because the level render ends with blending disabled.
- Do Not: Do not rebuild the view matrix from camera yaw and pitch. It lines up in X and Y but produces a different view-space Z, so terrain behind an effect wrongly wins the depth test.
- Verification: With Ki Sense COMBAT active the bars, BP label and damage popups draw above mobs and players with readable, unmirrored text; with SEARCH active the auras stay locked to their entities while the camera turns, including with view bobbing on.
- Related: `client/events/KiSenseEvent.java`, `client/render/effects/KiSenseAuraRenderer.java`, `client/render/util/RenderBufferUtil.java`, `client/events/PlayerEffectsRenderHandler.java`

### 2026-07-12 - Post Shaders Must Output Alpha 1.0 Before The Vanilla Blit Pass

- Type: pitfall
- Status: active
- Source: debugging
- Scope: `src/main/resources/assets/dragonminez/shaders/`
- Summary: Vanilla's `blit` post program blends with `srcalpha`/`1-srcalpha` (it is not an overwrite), and undrawn horizon/fog pixels in the main buffer carry alpha 0, so a post pass that forwards `color.a` makes the swap-back blit drop those pixels and show the render target's clear color (black horizon, hardware/scene dependent).
- Guidance: Any `.fsh` used in a `shaders/post` chain whose result is blitted back to `minecraft:main` via the vanilla `blit` pass must write `fragColor` with alpha `1.0` (see `gravity_red.fsh`, `kisense_grayscale.fsh`, `impact_frame.fsh`, `taiyoken_flash.fsh`).
- Do Not: Do not propagate the sampled framebuffer alpha (`color.a`) through a post pass that feeds the vanilla `blit`.
- Verification: Stand in an active Gravity Device zone at high gravity and look at the horizon; it must tint red instead of turning black.

### 2026-05-27 - Contiguous Kill Objectives Track Together

- Type: pitfall
- Status: active
- Source: debugging
- Scope: `src/main/java/com/dragonminez/server/events/QuestEvents.java`
- Summary: Sequential quests may still present multiple kill objectives at once because quest-spawned enemies are spawned for all kill objectives when the quest starts, and natural mixed-kill quests expect simultaneous counting.
- Guidance: Treat contiguous kill objectives as one tracking block; preserve sequencing between blocks with intervening non-kill objectives instead of gating each kill objective behind earlier kill progress.
- Related: `QuestEvents.isKillObjectiveUnlocked`
