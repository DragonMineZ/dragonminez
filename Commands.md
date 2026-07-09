# Commands

DMZ's in-game commands. Most require operator/permission level — see [[Permissions|Permissions]] for who can run what.

> In the examples, `@s` = yourself, `@p` = nearest player, or use a player's name/selector. Unless noted, `[targets]` is **optional and defaults to yourself**, and always comes **last**.

## Core stat / progression commands

| Command | Syntax | Example |
| :-- | :-- | :-- |
| **`/dmzstats`** | `set\|add\|remove <STR\|SKP\|RES\|VIT\|PWR\|ENE\|ALL> <amount\|min> [targets]` · `relocate [targets]` · `reset [targets] [keepPercentage] [keepSkills]` | `/dmzstats set STR 5000 @s` |
| **`/dmzpoints`** | `set\|add\|remove <amount> [targets]` — **Training Points only** (there's no separate "attribute points" pool) | `/dmzpoints add 10000 @s` |
| **`/dmzbonus`** | `add <STR\|SKP\|DEF\|STM\|VIT\|PWR\|ENE\|ALL> <+\|-\|*> <value> <bonusName> [applyMultipliers] [targets]` · `remove <stat> <bonusName> [targets]` · `clear <stat\|ALL> [targets]` — note: bonus stat keys are **DEF/STM**, not RES (they don't exactly match `/dmzstats`' core keys) | `/dmzbonus add STR + 500 eventbuff true @s` |
| **`/dmzform`** | `set <form> <level> [targets]` · `add <form> [targets]` (sets level 1) · `remove <form> [targets]` | `/dmzform add supersaiyan @s` |
| **`/dmzmastery`** | `set\|add <targetPlayer> <group> <form> <value>` — **target is required** (no self-default) and comes **before** the group/form, unlike other commands. `<group>` is either a race/stack name, or the literal `current`/`ALL`; when using `current`/`ALL` the next argument is the literal `form` or `stack` (or `all` after `ALL`) | `/dmzmastery Steve current form 100` |
| **`/dmzskill`** | `set <skill> <level> [targets]` · `add <skill> [targets]` (level 1) · `remove <skill> [targets]` — only "plain" skills (not ki/stack/form/strike techniques) | `/dmzskill add ki_control @s` |
| **`/dmztech`** | `add\|remove <technique> [targets]` · `experience add\|set\|remove <technique> <amount> [targets]` | `/dmztech add kamehameha @s` |
| **`/dmzracial`** | `reset [targets]` — clears a player's racial-skill stack counter | `/dmzracial reset @s` |
| **`/dmzalignment`** | `set <0-100> [targets]` · `add <amount> [targets]` · `remove <amount> [targets]` | `/dmzalignment set 100 @s` |
| **`/dmztail`** | `grow [targets]` · `cut [targets]` | `/dmztail grow @s` |
| **`/dmzweight`** | `<turtle_shell\|workout_weights\|piccolo_cape> <weight>` (self only, no `[targets]`) · bare `<weight>` defaults to `workout_weights` | `/dmzweight turtle_shell 500` |

### Resetting a character without losing stats

`/dmzstats` has two different "reset" tools — pick the right one depending on what you actually want to keep:

- **`/dmzstats relocate [targets]`** — a pure **stat respec**. It sets STR/SKP/RES/VIT/PWR/ENE back down to the race's base starting values and refunds every point you'd invested above that as **pending attribute points**, so you lose nothing — you just get to redistribute them from scratch. Nothing else (forms, skills, quests, techniques, master relationships) is touched.
- **`/dmzstats reset [targets] [keepPercentage] [keepSkills]`** — a **progress reset**. With no `keepPercentage` it wipes stats, Training Points, forms, skills, quests, techniques, cooldowns, bonus stats, master interactions and dynamic growth back to zero. To reset progress **without losing your stats**, pass `keepPercentage 100` (keeps 100% of your current STR/SKP/RES/VIT/PWR/ENE and Training Points) and `keepSkills true` (also keeps your learned skills, techniques and effects) — everything else (forms, quests, cooldowns, bonus stats, master interactions, dynamic growth) still gets cleared.

```
/dmzstats relocate @s
/dmzstats reset @s 100 true
```

## Effects, halo & revive

| Command | Syntax | Example |
| :-- | :-- | :-- |
| **`/dmzeffect`** | `give <effect> <durationSeconds\|-1> [targets]` (`-1` = permanent) · `remove <effect> [targets]` · `clear [targets]` | `/dmzeffect give tp_gain 600 @s` |
| **`/dmzhalo`** | `on [targets]` · `off [targets]` | `/dmzhalo on @s` |
| **`/dmzrevive`** | `[targets]` (no args = revive yourself) | `/dmzrevive @p` |

## Parties, quests & raids

| Command | Syntax | Example |
| :-- | :-- | :-- |
| **`/dmzparty`** | `invite <player>` · `accept [confirm]` · `reject` · `leave` · `list` · `kick <player>` · `disband` · `pvp` — no args lists your party | `/dmzparty invite Steve` |
| **`/dmzquest`** | `list [player]` · `info <quest>` · `start\|finish\|fail\|reset <quest\|all> [player]` · `track <quest\|none> [player]` · `startsaga\|finishsaga\|resetsaga <saga\|all> [player]` · `questnpc spawn <npcId> [model] [texture]` \| `questnpc list` \| `questnpc remove` — quest/saga IDs use `.` where JSON uses `:` (e.g. `saiyan_saga.1`) | `/dmzquest start saiyan_saga.1 @s` |
| **`/dmzraid`** | `start [type]` (defaults to the default raid type) · `stop` · `info` | `/dmzraid start <type>` |

## Server / admin utility

| Command | Syntax | Example |
| :-- | :-- | :-- |
| **`/dmzlocate`** | `<structureId>` (accepts `namespace:path` or bare path) | `/dmzlocate goku_house` |
| **`/dmzconfig`** | `<configFile> <keyOrSubtype> <rest>` — fully positional, no sub-literals; `<rest>` is a greedy string so nested keys/values are typed as-is | `/dmzconfig general-server gameplay.tpPerHit 5` |
| **`/dmzreload`** | `[all\|config\|story\|wishes]` (no args = `all`) | `/dmzreload config` |
| **`/dmzrestoreupdate`** | `[confirm]` (no args prompts for confirmation first) | `/dmzrestoreupdate confirm` |
| **`/dmzdebug`** | `[target] [scope]` — **target (a player) comes before scope** when both are given; scope alone (no target) defaults target to yourself. Scope: `ALL\|STATS\|CHARACTER\|TECHNIQUES\|QUESTS` | `/dmzdebug @s STATS` |

> Type any command with no arguments in-game to see its full argument list and sub-commands.

---

Related: [[Permissions|Permissions]] · [[Gamerules|Gamerules]] · [[Stats & Attributes|Stats-and-Attributes]] · [[Effects|Effects]]
