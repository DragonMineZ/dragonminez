# Permissions

Every DMZ command node is registered through Forge's `PermissionAPI`, which means server owners running a permissions mod (LuckPerms, PermissionsEx, etc.) can grant fine-grained access instead of relying on blanket OP. If you're not running a permissions mod, DMZ falls back to vanilla OP.

Source: `src/main/java/com/dragonminez/server/commands/DMZPermissions.java`.

## How a permission check resolves

For every gated command, DMZ checks in this order — first match wins:

1. **Hardcoded bypass** for three specific accounts (see below) — always passes, regardless of everything else.
2. **`dragonminez:admin`** node — grants every DMZ permission at once.
3. **The specific node** for that action.
4. **Vanilla OP level 2+** — `player.hasPermissions(2)`.

```java
if (player is ezShokkoh, ImYuseix, or MrBrunoh) return true;
return PermissionAPI.getPermission(player, ADMIN)
    || PermissionAPI.getPermission(player, node)
    || player.hasPermissions(2);
```

**Transparency note:** three specific usernames (`ezShokkoh`, `ImYuseix`, `MrBrunoh` — DMZ team/maintainer accounts) always pass every permission check on any server running this code, regardless of your permission grants or OP configuration. This is hardcoded in `DMZPermissions.hasPermission()`.

## If you don't run a permissions mod

Without LuckPerms/PermissionsEx registering these nodes, `PermissionAPI.getPermission()` calls fall through to vanilla behavior, which effectively means: only OP level 2+ (and the three bypass accounts) can use anything gated by a `false`-default node. Everyone else only gets the handful of nodes that default to `true` (see table below — mostly "view/list your own stuff").

## Granting nodes with a permissions mod

Example using LuckPerms syntax (adjust for whatever mod you run):

```
/lp user <player> permission set dragonminez:dmzstats.set.self true
/lp group <group> permission set dragonminez:dmzquest.list.others true
/lp user <player> permission set dragonminez:admin true
```

DMZ doesn't ship its own grant/revoke command — node management is entirely up to your permissions mod.

## Full node reference

Nodes default to `false` (OP/admin-only) unless marked **default: true** below.

### Admin

| Node | Grants |
| :-- | :-- |
| `dragonminez:admin` | Everything below, at once. |

### Stats (`StatsCommand`)

| Node | Grants |
| :-- | :-- |
| `dragonminez:dmzstats.set.self` | Set your own stats. |
| `dragonminez:dmzstats.set.others` | Set another player's stats. |
| `dragonminez:dmzstats.add.self` | Add to your own stats. |
| `dragonminez:dmzstats.add.others` | Add to another player's stats. |
| `dragonminez:dmzstats.info.self` | **default: true** — view your own stats. |
| `dragonminez:dmzstats.info.others` | View another player's stats. |
| `dragonminez:dmzstats.reset.self` | Reset your own stats. |
| `dragonminez:dmzstats.reset.others` | Reset another player's stats. |

### Skills (`SkillsCommand`)

| Node | Grants |
| :-- | :-- |
| `dragonminez:dmzskill.set.self` / `.others` | Set skill levels. |
| `dragonminez:dmzskill.add.self` / `.others` | Add a skill. |
| `dragonminez:dmzskill.remove.self` / `.others` | Remove a skill. |
| `dragonminez:dmzskill.list.self` | **default: true** — list your own skills. |
| `dragonminez:dmzskill.list.others` | List another player's skills. |

### Techniques (`TechCommand`)

| Node | Grants |
| :-- | :-- |
| `dragonminez:dmztech.add.self` / `.others` | Grant a ki technique. |
| `dragonminez:dmztech.remove.self` / `.others` | Remove a ki technique. |
| `dragonminez:dmztech.list.self` | **default: true** — list your own techniques. |
| `dragonminez:dmztech.list.others` | List another player's techniques. |
| `dragonminez:dmztech.experience.self` / `.others` | Change technique experience. |

### Technique cooldowns (`CooldownsCommand`)

| Node | Grants |
| :-- | :-- |
| `dragonminez:dmzcooldowns.self` | Change your own ki/strike technique cooldowns. |
| `dragonminez:dmzcooldowns.others` | Change another player's ki/strike technique cooldowns. |

### Bonus stats (`BonusCommand`)

| Node | Grants |
| :-- | :-- |
| `dragonminez:dmzbonus.add.self` / `.others` | Add bonus stats. |
| `dragonminez:dmzbonus.clear.self` / `.others` | Clear bonus stats. |

### Effects (`EffectsCommand`)

| Node | Grants |
| :-- | :-- |
| `dragonminez:dmzeffect.give.self` / `.others` | Give a status effect. |
| `dragonminez:dmzeffect.remove.self` / `.others` | Remove a status effect. |
| `dragonminez:dmzeffect.clear.self` / `.others` | Clear all effects. |
| `dragonminez:dmzeffect.list.self` | **default: true** — list your own effects. |
| `dragonminez:dmzeffect.list.others` | List another player's effects. |

### Points (`PointsCommand`)

| Node | Grants |
| :-- | :-- |
| `dragonminez:dmzpoints.set.self` / `.others` | Set points. |
| `dragonminez:dmzpoints.add.self` / `.others` | Add points. |
| `dragonminez:dmzpoints.remove.self` / `.others` | Remove points. |
| `dragonminez:dmzpoints.info.self` | **default: true** — view your own points. |
| `dragonminez:dmzpoints.info.others` | View another player's points. |

### Quests & sagas (`StoryCommand`)

| Node | Grants |
| :-- | :-- |
| `dragonminez:dmzquest.list.self` | **default: true** — list your own quest progress. |
| `dragonminez:dmzquest.list.others` | List another player's quest progress. |
| `dragonminez:dmzquest.info` | **default: true** — view quest metadata. |
| `dragonminez:dmzquest.start.self` / `.others` | Force-start a quest. |
| `dragonminez:dmzquest.finish.self` / `.others` | Force-complete a quest. |
| `dragonminez:dmzquest.fail.self` / `.others` | Force-fail a quest. |
| `dragonminez:dmzquest.reset.self` / `.others` | Reset a quest. |
| `dragonminez:dmzquest.track.self` | **default: true** — set your own tracked quest. |
| `dragonminez:dmzquest.track.others` | Set another player's tracked quest. |
| `dragonminez:dmzquest.startsaga.self` / `.others` | Force-start every quest in a saga. |
| `dragonminez:dmzquest.finishsaga.self` / `.others` | Force-complete every quest in a saga. |
| `dragonminez:dmzquest.resetsaga.self` / `.others` | Reset an entire saga. |

### Other commands

| Node | Grants | Command |
| :-- | :-- | :-- |
| `dragonminez:dmzlocate` | Locate special structures. | `LocateCommand` |
| `dragonminez:dmzrevive.self` / `.others` | Revive a player. | `ReviveCommand` |
| `dragonminez:dmzparty.use` | **default: true** — use party commands. | `PartyCommand` |
| `dragonminez:dmzmastery.set` / `.add` | Set/add transformation mastery. | `MasteryCommand` |
| `dragonminez:dmz.reload` | Run `/dmzreload`. | `ReloadCommand` — see [[Reloading and updating changes|Reloading-and-updating-changes]] |
| `dragonminez:dmzform.set.self` / `.others` | Set a form. | `FormsCommand` |
| `dragonminez:dmzform.add.self` / `.others` | Grant a form. | `FormsCommand` |
| `dragonminez:dmzform.remove.self` / `.others` | Remove a form. | `FormsCommand` |
| `dragonminez:dmzform.list.self` | **default: true** — list your own forms. | `FormsCommand` |
| `dragonminez:dmzform.list.others` | List another player's forms. | `FormsCommand` |
| `dragonminez:dmzracial.reset.self` / `.others` | Reset racial skills. | `RacialSkillCommand` |
| `dragonminez:dmzweight.give` | Give Weight training items. | `WeightCommand` |
| `dragonminez:dmzalignment.set.self` / `.others` | Set alignment. | `AlignmentCommand` |
| `dragonminez:dmzalignment.add.self` / `.others` | Add alignment. | `AlignmentCommand` |
| `dragonminez:dmzalignment.remove.self` / `.others` | Remove alignment. | `AlignmentCommand` |
| `dragonminez:dmzalignment.info.self` | **default: true** — view your own alignment. | `AlignmentCommand` |
| `dragonminez:dmzalignment.info.others` | View another player's alignment. | `AlignmentCommand` |
| `dragonminez:dmztail.self` | **default: true** — grow/cut your own tail. | `TailCommand` |
| `dragonminez:dmztail.others` | Grow/cut another player's tail. | `TailCommand` |
| `dragonminez:dmzhalo.self` / `.others` | Toggle a halo. | `HaloCommand` |
| `dragonminez:dmzhair.self` | **default: true** — resync/reset your own hair. | `HairCommand` |
| `dragonminez:dmzhair.others` | Resync/reset another player's hair. | `HairCommand` |

## Reload

Permission node registration happens at mod init and doesn't need a config-style reload — grant/revoke changes take effect as soon as your permissions mod applies them (LuckPerms, for instance, applies most changes immediately).
