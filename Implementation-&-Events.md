# Implementation & Events

Once your addon builds against DMZ (see [[Getting Started|Getting-Started]]), this page covers the actual hook surface: events, reading player state, extension registries, and where to plug in your own networking/registries.

## Listening to a DMZ event

All server/common events are nested static classes on `com.dragonminez.common.events.DMZEvent`, posted to the standard Forge event bus (`MinecraftForge.EVENT_BUS`). Each nested class extends Forge's `Event` directly (`DMZEvent` itself is only the container), so subscribe to the specific nested class:

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

Events marked **Cancelable** below carry Forge's `@Cancelable` annotation: `event.setCanceled(true)` blocks the underlying action. Fields marked *settable* have a setter (`setTpGain`, `setAmount`, ...) and DMZ uses the value after all listeners ran. All getters follow the field names (`getPlayer()`, `getOldValue()`, ...); boolean fields use `is...` (`isParry()`, `isBlocked()`).

## Full event list

Source: `src/main/java/com/dragonminez/common/events/DMZEvent.java`.

### Stats, resources and progression

| Event | Cancelable | Fires when | Key fields |
| :-- | :-- | :-- | :-- |
| `StatChangeEvent` | Yes | A stat (STR/SKP/RES/VIT/PWR/ENE) changes through a `Stats.set*()` call — commands, items, menus, and also when stats are loaded or copied (on both server and client). | `player`, `stat` (`STRENGTH`/`STRIKE_POWER`/`RESISTANCE`/`VITALITY`/`KI_POWER`/`ENERGY`), `oldValue`, `newValue` |
| `TPGainEvent` | Yes | Before a positive amount of training points is added (`Resources.addTrainingPoints`). | `player`, `oldValue`, `tpGain` (settable), `shareWithParty` (settable); helper `getNewTpsValue()` |
| `KiChargeEvent` | Yes | Once per second while the player is actively charging ki. Cancel to stop that second's ki gain. | `player`, `currentEnergy`, `maxEnergy`; helper `isEnergyFull()` |
| `HealthRegenEvent` / `EnergyRegenEvent` / `StaminaRegenEvent` | Yes | Before the per-second health/ki/stamina regeneration is applied. They share the `ResourceRegenEvent` base. | `player`, `statsData`, `amount` (settable) |
| `FormChangeEvent` / `StackFormChangeEvent` | No | After the player's race form / stack form changes (server side). | `player`, `oldGroup`, `oldForm`, `newGroup`, `newForm` — helpers `isTransform()`/`isUntransform()` |
| `PlayerDataSaveEvent` | No | Player data is about to be written to the `JSON` or `DATABASE` storage backend. You may add your own keys to `data`. | `player`, `data` |
| `PlayerDataLoadEvent` | No | Player data was read from the `JSON` or `DATABASE` backend, right before it is applied to the player. | `player`, `data` |

> ℹ️ `StatChangeEvent` also fires while DMZ loads saved data and applies client syncs. If you cancel it, check `event.getPlayer().level().isClientSide()` and your own conditions first, or you can block a player's saved stats from loading.

> ℹ️ `PlayerDataSaveEvent`/`PlayerDataLoadEvent` do not fire with the default `NBT` backend, where DMZ data is saved by vanilla with the player file. See [[General Server|General-Server]] for the `storage` settings.

### Combat

| Event | Cancelable | Fires when | Key fields |
| :-- | :-- | :-- | :-- |
| `DamageModifyEvent` | Yes | Pre-mitigation damage calculation for a hit dealt by a player (melee hits and strike attacks). Cancel to deal no DMZ damage. | `attacker`, `victim`, `amount` (settable), `defensePenetration` (settable), `sourceType` (`MELEE`/`STRIKE`) |
| `DamageDealtEvent` | No | After blocking is resolved for a player's melee hit (pre-mitigation amount). | `attacker`, `victim`, `amount`, `blocked`, `parried`, `sourceType` |
| `PlayerDamageMitigatedEvent` | No | After DMZ finished mitigating a hit taken by a player. Read-only — do not change damage here. | `victim`, `source` (`DamageSource`), `rawDamage`, `defenseMitigated`, `blockMitigated`, `finalDamage` |
| `PlayerBlockEvent` | Yes | Player successfully blocks/parries an attack. Cancel and the player takes the full hit. | `victim`, `attacker`, `originalDamage`, `finalDamage` (settable), `isParry` (settable), `poiseDamage` (settable) |
| `BarrierAbsorbEvent` | No | A ki barrier (barrier technique or Android barrier) absorbs a hit before it reaches the protected entity. | `owner` (barrier creator), `protectedEntity`, `attacker`, `amount` |
| `PlayerDashEvent` | Yes | Before a dash executes. | `player`, `dashType` (`NORMAL`/`DOUBLE`), `distance` (settable), `kiCost` (settable) |
| `PlayerEvasionEvent` | Yes | Before an evasion triggers. | `player`, `attacker`, `originalDamage`, `kiCost` (settable) |
| `CritChanceEvent` | No | Resolving a player's crit chance. | `player`, `chance` (settable, 0..1) |
| `KiAttackCastEvent` | No | A player starts charging (casts) a ki attack. | `player`, `statsData`, `kiAttack` |
| `KiAttackFireEvent` | No | A charged ki attack is released. | `player`, `statsData`, `kiAttack`, `chargeMultiplier`, `cooldownTicks` (settable, minimum 1) |
| `StrikeAttackCastEvent` | No | A player initiates a strike attack. | `player`, `statsData`, `strike` |
| `StrikeAttackFireEvent` | No | A strike attack connects and starts on a target. | `player`, `statsData`, `strike`, `target` |

`DamageSourceType` also contains a `KI` value; in v2.2 ki attacks do not post `DamageModifyEvent`/`DamageDealtEvent`.

### Fusions and dragons

| Event | Cancelable | Fires when | Key fields |
| :-- | :-- | :-- | :-- |
| `FusionEvent` | Yes | Before a fusion attempt. DMZ posts it for `METAMORU`, `POTHALA` and `BEETLE` (Medi-Bug Beetle) fusions; the enum also contains `ABSORPTION` and `ASSIMILATION`. | `initiator`, `target`, `type` |
| `DragonSummonedEvent` | No | After a dragon is summoned from a completed ball set. | `player`, `level`, `position`, `dragonDefinition`, `ballSetDefinition`, `consumedPositions`; helpers `getDragonId()`/`getBallSetId()` |

See [[Fusions|Fusions]] and [[Dragons and Wishes|Dragons-and-Wishes]] for the gameplay side.

### Quests

All quest events extend `QuestLifecycleEvent` and share its fields: `player`, `questKey` (`sagaId:questId` for saga quests, the string id for side quests), `saga` (null for side quests), `quest`, `partyMembers`.

| Event | Cancelable | Fires when | Extra fields |
| :-- | :-- | :-- | :-- |
| `QuestStartEvent` | Yes | Before a quest is accepted. | `difficulty` (settable) |
| `QuestObjectiveProgressEvent` | Yes | Before objective progress is stored. | `objectiveIndex`, `oldProgress`, `newProgress` (settable), `objectiveRequired` |
| `QuestFailEvent` | Yes | Before a quest is marked failed. | `reason` (`PLAYER_DEATH`/`FORCED_RESET`/`SCRIPT`/`TIME_EXPIRED`/`ESCORT_FAILED`) |
| `QuestTurnInEvent` | Yes | Before a quest turn-in is applied. | `npcId` |
| `QuestRewardClaimEvent` | Yes | Before an individual reward is claimed. | `rewardIndex` |
| `QuestCompletedEvent` | No | When a quest completes. | (shared fields only) |

### Client events

Client-only events live on `com.dragonminez.client.events.DMZClientEvent` and are posted on the Forge bus of the physical client. Register these listeners with `value = Dist.CLIENT`. None of them is cancelable, and they are informational — the server still decides what actually happens.

| Event | Fires when | Key fields |
| :-- | :-- | :-- |
| `PlayerAttackStart` | The local player starts the attack wind-up. | `player`, `attackHand` |
| `PlayerAttackHit` | The local player's attack resolves its targets (zero or more). | `player`, `attackHand`, `targets`, `cursorTarget` (nullable) |
| `KiAttackCast` | The local player starts charging the ki attack in a slot. | `player`, `slot` |
| `KiAttackRelease` | The local player releases a charged ki attack. | `player` |
| `StrikeAttack` | The local player initiates a strike attack. | `player`, `targetId` |

## Reading player state

Get the capability from any `Player` (server player, or the local player on the client — the client copy is kept in sync by DMZ):

```java
StatsProvider.get(StatsCapability.INSTANCE, player).ifPresent(data -> {
    String race = data.getCharacter().getRaceName();
    String activeForm = data.getCharacter().getActiveForm();
    float currentKi = data.getResources().getCurrentEnergy();
    float maxKi = data.getMaxEnergy();
    int level = data.getLevel();
});
```

`StatsProvider.get(cap, entity)` is a shortcut for `player.getCapability(StatsCapability.INSTANCE)`; both return a `LazyOptional<StatsData>`. DMZ's getters are generated by Lombok at compile time, so you don't need Lombok in your addon to call them.

Useful reads on `StatsData`:

| Getter | Returns |
| :-- | :-- |
| `getCharacter().getRaceName() / getCharacterClass() / getGender()` | Race/class/gender |
| `getCharacter().getActiveForm() / getActiveFormGroup()` | Current race form (empty string in base form) |
| `getCharacter().getActiveStackForm() / getActiveStackFormGroup()` | Current stack form (e.g. Kaioken tier) |
| `getStats().getStrength() / getStrikePower() / getResistance() / getVitality() / getKiPower() / getEnergy()` | Base stat values |
| `getResources().getCurrentEnergy() / getCurrentStamina() / getCurrentPoise() / getAlignment()` | Current resource pools and alignment |
| `getMaxHealth() / getMaxEnergy() / getMaxStamina() / getMaxPoise()` | Calculated resource caps |
| `getBattlePower() / getMeleeDamage() / getStrikeDamage() / getKiDamage() / getDefense()` | Calculated combat stats |
| `getLevel()` | Computed player level |
| `getPlayerQuestData()` | Quest progress (`getQuestStatus(questKey)`, `isQuestAccepted(questKey)`, `isQuestCompleted(questKey)`, `isSagaLocked(sagaId)`, `getTrackedQuestId()`, `getDifficulty()`) |
| `getSkills()` | Skill levels (`hasSkill(name)`, `getSkillLevel(name)`) |
| `getStatus()`, `getEffects()`, `getTechniques()`, `getBonusStats()` | Status flags, active effects, ki/strike techniques, bonus stat modifiers |

Race, form and stat math reads the live config every time, so values always reflect the current server config (including after `/dmzreload`).

## Mutating state safely

DMZ's own stat setters (e.g. `Stats.setStrength()`) post a cancelable event first and only apply the change if it isn't cancelled:

```java
DMZEvent.StatChangeEvent event = new DMZEvent.StatChangeEvent(player, StatType.STRENGTH, oldValue, newValue);
if (!MinecraftForge.EVENT_BUS.post(event)) {
    // apply the change
}
```

For your addon: prefer **reacting to** events (modifying their settable fields, or cancelling them) over calling DMZ setters directly. Not every setter validates or posts an event (e.g. most `Resources` setters just clamp and apply with no event), so going through the event path is the one guaranteed-stable integration point.

If you do change DMZ data from the server, do it on the server thread and send the client a fresh copy afterwards, the same way DMZ does:

```java
NetworkHandler.sendToTrackingEntityAndSelf(new StatsSyncS2C(serverPlayer), serverPlayer);
```

`StatsSyncS2C` sends the full player data; DMZ also uses `ProgressionSyncS2C` (stats, bonus stats, skills, techniques, quest data) and `ResourceSyncS2C` (resources and status) for smaller updates.

## Custom quest objectives and rewards

Addons can add new quest objective and reward **types** that quest JSON files can then use, without touching DMZ's parser. Register them during mod setup (e.g. `FMLCommonSetupEvent`) on **both** sides — the client rebuilds quests from the synced registry with the same factories:

```java
QuestObjectiveRegistry.register("MEDITATE",
    json -> new MeditateObjective(json.get("seconds").getAsInt()),                                 // parse quest JSON
    (objective, out) -> out.addProperty("seconds", ((MeditateObjective) objective).getSeconds()),  // client sync fields
    objective -> Component.translatable("myaddon.quest.meditate.obj"));                            // HUD/journal text

QuestRewardRegistry.register("MY_CURRENCY",
    json -> new MyCurrencyReward(json.get("amount").getAsInt()),
    (reward, out) -> out.addProperty("amount", ((MyCurrencyReward) reward).getAmount()));
```

Contract:

- Objective classes extend `QuestObjective` and call the `QuestObjective(String customType, int required)` constructor; reward classes extend `QuestReward`, call `QuestReward(String customType)` and implement `giveReward(ServerPlayer)` and `getDescription()`. Objectives must also implement `checkProgress(Object... params)`.
- In quest JSON, use your key as the `"type"` (e.g. `{"type": "MEDITATE", "seconds": 60}`). Type keys are case-insensitive (stored upper-case). Treat them like registry names: don't rename them once quests use them.
- The sync writer must write every field your factory reads — the client re-parses the synced JSON with your factory.
- Unknown types are reported in the JSON load report (see [[Developer Scope and Prerequisites|Developer-Scope-and-Prerequisites#finding-json-mistakes]]) and the objective/reward is skipped.
- Progress is server-authoritative and event-driven: listen to your own game events on the server and store progress with `PlayerQuestData.setObjectiveProgress(questKey, objectiveIndex, progress)`, then sync with `ProgressionSyncS2C`. Use `DMZEvent.QuestObjectiveProgressEvent`/`QuestCompletedEvent` to react to built-in quests.

See [[Custom Quests, Sagas and Sidequests|Custom-Quests-Sagas-and-Sidequests]] for the quest JSON format.

## Custom text placeholders

Quest, dialogue and NPC text supports `%placeholders%` such as `%player%`, `%race%`, `%class%`, `%level%`, `%alignment%`, `%deaths%`, `%form%` and `%dimension%`. Add your own during mod setup:

```java
DMZTextPlaceholders.register("zeni", (player, data) -> String.valueOf(MyAddon.getZeni(player)));
// "%zeni%" in quest/dialogue text now resolves to your value
```

Keys are case-insensitive. The resolver runs on whichever side renders the text (usually the client), and must handle a `null` `StatsData`.

## Utility menu buttons (client)

The utility (radial) menu has two free slots, on the left and right of the wheel, reserved for addons. Add a button from client setup:

```java
UtilityMenuScreen.addMenuSlot(new IUtilityMenuSlot() {
    @Override
    public ButtonInfo render(StatsData statsData) {
        return new ButtonInfo(Component.literal("My Addon"), Component.literal("Open"));
    }

    @Override
    public void handle(StatsData statsData, boolean rightClick) {
        // client side: open your screen or send your own packet to the server
    }
});
```

Slots are filled in registration order; extra registrations beyond the two free slots are not shown. Override `hasRightClickAction` to return `true` if your button also reacts to right-click. `IUtilityMenuSlot` and `ButtonInfo` live in `com.dragonminez.client.gui.utilitymenu`.

## Armor and render layers

- **Armor from your addon** — DMZ's armor item class `DbzArmorItem` (`common/init/armor`) has a constructor `DbzArmorItem(material, type, properties, modId, itemId)`. Pass your own `modId` and the textures are then read from your own namespace: `assets/<modId>/textures/armor/<itemId>_layer1.png` (`_layer2` for leggings), plus optional `<itemId>_damaged_layer1.png`/`_damaged_layer2.png` used once the item has lost more than half of its durability.
- **Other armor** — regular `ArmorItem`s from other mods are drawn on DMZ's player model with their normal armor texture.
- **Third-party render layers** — render layers that other mods add to the vanilla player renderer are forwarded onto DMZ's player model (vanilla layers and cosmetic-armor layers are skipped, and nothing is forwarded while the player is an Oozaru).

## Networking

DMZ's channel is `NetworkHandler.INSTANCE` (a Forge `SimpleChannel` at `dragonminez:network`). Don't register your own packets on it: DMZ assigns packet ids by registration order, so anything you add would shift or collide with DMZ's ids. Register your own `SimpleChannel` in your addon instead, and use DMZ's as a reference for the pattern (`messageBuilder`/encoder/decoder/handler, explicit direction).

You can use DMZ's send helpers (`sendToPlayer`, `sendToTrackingEntityAndSelf`, ...) to send DMZ's own sync packets, as shown above.

Never trust the client in your own packets either: validate on the server anything that affects stats, unlocks, quests, wishes or progression.

There is no client-side "data synced" event. On the client, read the capability when you need it (e.g. while rendering) instead of trying to intercept DMZ packets.

## Using DMZ's registries

Import the `Main*` registry holders directly instead of hardcoding resource locations:

```java
import com.dragonminez.common.init.MainItems;
import com.dragonminez.common.init.MainAttributes;

Item redCapsule = MainItems.RED_CAPSULE.get();
Attribute strength = MainAttributes.STRENGTH.get();
```

Available under `common/init/`: `MainItems`, `MainBlocks`, `MainBlockEntities`, `MainEntities`, `MainAttributes`, `MainEffects`, `MainEnchants`, `MainPotions`, `MainSounds`, `MainParticles`, `MainFluids`, `MainMenus`, `MainRecipes`, `MainTabs`, `MainTags`, `MainVillagers`, `MainDamageTypes`, `MainGameRules`, `MainLootModifiers`.

## Optional dependency pattern

If your addon should work with or without DMZ installed, follow DMZ's own pattern for optional integrations (`common/compat/WeaponLevelingCompat.java`):

```java
public final class DmzCompat {
    private static Boolean loaded;

    public static boolean isLoaded() {
        if (loaded == null) loaded = ModList.get().isLoaded("dragonminez");
        return loaded;
    }

    public static int getLevel(Player player) {
        if (!isLoaded()) return 0;   // safe fallback without DMZ
        return Impl.getLevel(player);
    }

    // Only this nested class imports DMZ classes, so it is never loaded without DMZ.
    private static final class Impl {
        static int getLevel(Player player) {
            return StatsProvider.get(StatsCapability.INSTANCE, player).map(StatsData::getLevel).orElse(0);
        }
    }
}
```

1. Check `ModList.get().isLoaded("dragonminez")` once and cache it.
2. Keep every DMZ import inside classes that are only touched after that check (event listeners for DMZ events included — register them manually only when DMZ is loaded).
3. Guard every DMZ-touching call behind that flag, with a safe fallback/default when it's `false`.
4. Log a clear info/warn message either way so server owners can tell at a glance whether integration is active.

## Next step

See [[Creating a DMZ Addon|Creating-a-DMZ-Addon]] for the full Gradle project setup and `mods.toml` dependency declaration.
