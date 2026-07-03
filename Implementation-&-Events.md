# Implementation & Events

Once your addon builds against DMZ (see [[Getting Started|Getting-Started]]), this page covers the actual hook surface: events, reading player state, and where to plug in your own networking/registries.

## Listening to a DMZ event

All events are nested static classes on `com.dragonminez.common.events.DMZEvent`, posted to the standard Forge event bus:

```java
@Mod.EventBusSubscriber(modid = "your_addon_modid", bus = Mod.EventBusSubscriber.Bus.FORGE)
public class DmzHooks {
    @SubscribeEvent
    public static void onTpGain(DMZEvent.TPGainEvent event) {
        int boosted = Math.round(event.getTpGain() * 1.10f);
        event.setTpGain(boosted);
    }

    @SubscribeEvent
    public static void onFormChange(DMZEvent.FormChangeEvent event) {
        if (event.isTransform()) {
            // player transformed into event.getNewGroup() / event.getNewForm()
        }
    }
}
```

Cancelable events (marked below) can be cancelled with `event.setCanceled(true)` to block the underlying action — check that the specific event class extends a cancelable base and is annotated accordingly before relying on this.

## Full event list

Source: `src/main/java/com/dragonminez/common/events/DMZEvent.java`.

| Event | Cancelable | Fires when | Key fields |
| :-- | :-- | :-- | :-- |
| `StatChangeEvent` | Yes | A stat (STR/SKP/RES/VIT/PWR/ENE) is set via `Stats.set*()`. | `player`, `stat`, `oldValue`, `newValue` |
| `KiChargeEvent` | Yes | Player is actively charging ki, per tick. | `player`, `currentEnergy`, `maxEnergy` |
| `TPGainEvent` | Yes | Before training points are added (`Resources.addTrainingPoints`). | `player`, `oldValue`, `tpGain` (settable), `shareWithParty` (settable) |
| `PlayerBlockEvent` | Yes | Player successfully blocks/parries an attack. | `victim`, `attacker`, `originalDamage`, `finalDamage` (settable), `isParry` (settable), `poiseDamage` (settable) |
| `PlayerDashEvent` | Yes | Before a dash executes. | `player`, `dashType` (`NORMAL`/`DOUBLE`), `distance` (settable), `kiCost` (settable) |
| `PlayerEvasionEvent` | Yes | Before an evasion triggers. | `player`, `attacker`, `originalDamage`, `kiCost` (settable) |
| `FusionEvent` | Yes | Before a fusion attempt (`METAMORU`/`POTHALA`/`ABSORPTION`/`ASSIMILATION`). | `initiator`, `target`, `type` |
| `DragonSummonedEvent` | No | After a dragon is summoned from a completed ball set. | `player`, `level`, `position`, `dragonDefinition`, `ballSetDefinition` |
| `QuestStartEvent` | Yes | Before a quest is accepted. | `player`, `questKey`, `saga`, `quest`, `difficulty` (settable) |
| `QuestObjectiveProgressEvent` | Yes | Before objective progress is stored. | `objectiveIndex`, `oldProgress`, `newProgress` (settable), `objectiveRequired` |
| `QuestFailEvent` | Yes | Before a quest is marked failed. | `reason` (`PLAYER_DEATH`/`FORCED_RESET`/`SCRIPT`) |
| `QuestTurnInEvent` | Yes | On turning in a quest. | `npcId` |
| `QuestRewardClaimEvent` | Yes | Before an individual reward is claimed. | `rewardIndex` |
| `QuestCompletedEvent` | No | After a quest completes. | (inherited quest fields only) |
| `PlayerDataSaveEvent` | No | Player data serialized to NBT. | `player`, `data` |
| `PlayerDataLoadEvent` | No | Player data deserialized from NBT. | `player`, `data` |
| `HealthRegenEvent` / `EnergyRegenEvent` / `StaminaRegenEvent` | Yes | Per-second resource regen tick. | `player`, `statsData`, `amount` (settable) |
| `DamageModifyEvent` | Yes | Pre-mitigation damage calculation. | `attacker`, `victim`, `amount` (settable), `defensePenetration` (settable), `sourceType` (`MELEE`/`KI`/`STRIKE`) |
| `DamageDealtEvent` | No | Post-mitigation damage confirmation. | `attacker`, `victim`, `amount`, `blocked`, `parried`, `sourceType` |
| `CritChanceEvent` | No | Resolving crit chance. | `player`, `chance` (settable, 0..1) |
| `KiAttackCastEvent` / `KiAttackFireEvent` | No | Ki attack cast/fired. | `player`, `statsData`, `kiAttack`, `cooldownTicks` (settable, fire only) |
| `StrikeAttackCastEvent` / `StrikeAttackFireEvent` | No | Strike attack initiated/connects. | `player`, `statsData`, `strike`, `target` (fire only) |
| `FormChangeEvent` / `StackFormChangeEvent` | No | Race form / stack form changes. | `player`, `oldGroup`, `oldForm`, `newGroup`, `newForm` — helpers `isTransform()`/`isUntransform()` |

Quest events all share common fields (`player`, `questKey`, `saga`, `quest`, `partyMembers`) from a shared `QuestLifecycleEvent` base.

There is no separate event class outside `DMZEvent` for wishes or storage — everything addon-relevant is on this one class.

## Reading player state

Get the capability from any `Player`:

```java
LazyOptional<StatsData> opt = player.getCapability(StatsCapability.INSTANCE);
opt.ifPresent(data -> {
    String race = data.getCharacter().getRaceName();
    String activeForm = data.getCharacter().getActiveForm();
    float currentKi = data.getResources().getCurrentEnergy();
    float maxKi = data.getMaxEnergy();
    int level = data.getLevel();
});
```

Useful reads on `StatsData`:

| Getter | Returns |
| :-- | :-- |
| `getCharacter().getRaceName() / getCharacterClass() / getGender()` | Race/class/gender |
| `getCharacter().getActiveForm() / getActiveFormGroup()` | Current race form |
| `getCharacter().getActiveStackForm() / getActiveStackFormGroup()` | Current stack form (e.g. Kaioken tier) |
| `getStats().getStrength() / getStrikePower() / getResistance() / getVitality() / getKiPower() / getEnergy()` | Base stat values |
| `getResources().getCurrentEnergy() / getCurrentStamina() / getCurrentPoise()` | Current resource pools |
| `getMaxEnergy() / getMaxStamina() / getMaxPoise()` | Calculated resource caps |
| `getBattlePower() / getMeleeDamage() / getStrikeDamage() / getKiDamage() / getDefense()` | Calculated combat stats |
| `getLevel()` | Computed player level |
| `getPlayerQuestData()` | Quest progress (`getQuestStatus(questKey)`, `isSagaLocked(sagaId)`, `getTrackedQuestId()`, `getDifficulty()`) |
| `getSkills()`, `getStatus()`, `getEffects()` | Skill levels, status flags, active effects |

## Mutating state safely

DMZ's own setters (e.g. `Stats.setStrength()`) post a cancelable event first and only apply the change if it isn't cancelled:

```java
DMZEvent.StatChangeEvent event = new DMZEvent.StatChangeEvent(player, StatType.STRENGTH, oldValue, newValue);
if (!MinecraftForge.EVENT_BUS.post(event)) {
    // apply the change
}
```

For your addon: prefer **reacting to** events (modifying their settable fields, or cancelling them) over calling DMZ setters directly. Not every setter validates or posts an event (e.g. some `Resources` setters just clamp and apply with no event), so going through the event path is the one guaranteed-stable integration point. If you need to change something DMZ owns and there's no event for it, prefer DMZ's existing commands or network packets over reflection into internal state.

## Networking

DMZ's channel is public: `NetworkHandler.INSTANCE` (a Forge `SimpleChannel` at `dragonminez:network`). Don't register your own packets on it — register your own `SimpleChannel` in your addon instead, and use `NetworkHandler.INSTANCE` only as a reference for the pattern (`messageBuilder`/encoder/decoder/handler, matching direction). If you need to react to a DMZ S2C packet client-side (e.g. `SyncQuestRegistryS2C`, `ProgressionSyncS2C`), do it by listening to the resulting DMZ event instead of trying to intercept the packet directly.

## Using DMZ's registries

Import the `Main*` registry holders directly instead of hardcoding resource locations:

```java
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.MainAttributes;

Item redCapsule = MainItems.RED_CAPSULE.get();
Attribute strength = MainAttributes.STRENGTH.get();
```

Available: `MainItems`, `MainBlocks`, `MainEntities`, `MainAttributes`, `MainEffects`, `MainEnchants` (all under `common/init/`).

## Optional dependency pattern

If your addon should work with or without DMZ installed, follow DMZ's own pattern for optional integrations (`common/compat/WorldGuardCompat.java`):

1. Probe for a DMZ class with `Class.forName("com.dragonminez....")` in a try/catch during setup.
2. Store the result in a static `boolean available` flag.
3. Guard every DMZ-touching call behind that flag, with a safe fallback/default when it's `false`.
4. Log a clear info/warn message either way so server owners can tell at a glance whether integration is active.

## Next step

See [[Creating a DMZ Addon|Creating-a-DMZ-Addon]] for the full Gradle project setup and `mods.toml` dependency declaration.
