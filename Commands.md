# Commands

DMZ's in-game commands. Most require operator/permission level — see [[Permissions|Permissions]] for who can run what.

> In the examples, `@s` = yourself, `@p` = nearest player, or use a player's name. Stat keys are `STR`, `SKP`, `RES`, `VIT`, `PWR`, `ENE`.

| Command | What it does | Example |
| :-- | :-- | :-- |
| **`/dmzstats`** | Set / add / remove a core stat; also `relocate` and `reset`. | `/dmzstats set STR 5000 @s` |
| **`/dmzpoints`** | Set / add / remove attribute & training points. | `/dmzpoints add 10000 @s` |
| **`/dmzbonus`** | Add / remove / clear a named bonus stat modifier. | `/dmzbonus add STR + 500 eventbuff true @s` |
| **`/dmzform`** | Set / add / remove a transformation. | `/dmzform add supersaiyan @s` |
| **`/dmzmastery`** | Set form/stack/all mastery, or show `current`. | `/dmzmastery @s all 100` |
| **`/dmzskill`** | Set / add / remove a skill. | `/dmzskill add kamehameha @s` |
| **`/dmztech`** | Add / remove a technique, or grant `experience`. | `/dmztech add kamehameha @s` |
| **`/dmzracial`** | Reset a player's racial-skill stacks. | `/dmzracial reset @s` |
| **`/dmzeffect`** | Give / remove / clear a DMZ effect (see [[Effects|Effects]]). | `/dmzeffect give tp_gain 600 @s` |
| **`/dmzalignment`** | Set / add / remove alignment (0–100). | `/dmzalignment set 100 @s` |
| **`/dmztail`** | Grow or cut a Saiyan tail. | `/dmztail grow @s` |
| **`/dmzweight`** | Give training weights of a type & value. | `/dmzweight turtle_shell 500` |
| **`/dmzhalo`** | Turn a player's Otherworld halo on / off. | `/dmzhalo on @s` |
| **`/dmzrevive`** | Revive a player from the Otherworld. | `/dmzrevive @s` |
| **`/dmzlocate`** | Locate a DMZ structure. | `/dmzlocate goku_house` |
| **`/dmzparty`** | Party: invite/accept/reject/leave/list/kick/disband/pvp. | `/dmzparty invite Steve` |
| **`/dmzquest`** | Quests/sagas: list/info/start/finish/fail/reset/track. | `/dmzquest start saiyan_saga @s` |
| **`/dmzraid`** | Start / stop / info a raid (see [[Raids (W.I.P)|Raids]]). | `/dmzraid start <type>` |
| **`/dmzconfig`** | Read/change a config value in-game. | `/dmzconfig general-server gameplay.tpPerHit 5` |
| **`/dmzreload`** | Reload DMZ config/data and resync clients. | `/dmzreload config` |
| **`/dmzrestoreupdate`** | Restore/migrate player data after an update. | `/dmzrestoreupdate confirm` |
| **`/dmzdebug`** | Developer debug utilities. | `/dmzdebug <value> @s` |

> Type any command with no arguments in-game to see its full argument list and sub-commands.

---

Related: [[Permissions|Permissions]] · [[Gamerules|Gamerules]] · [[Stats & Attributes|Stats-and-Attributes]] · [[Effects|Effects]]
