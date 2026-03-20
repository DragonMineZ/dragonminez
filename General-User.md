# general-user.json

`general-user.json` stores user-facing configuration for the DragonMineZ client HUD and related visual interface behavior. This file focuses on how information is displayed to the player, not on server balance or world systems.

## File structure

Top-level keys:

- `configVersion`
- `hud`

## Full example

```json
{
  "configVersion": 3,
  "hud": {
    "firstPersonAnimated": true,
    "xenoverseHudPosX": 5,
    "xenoverseHudPosY": 5,
    "advancedDescription": true,
    "advancedDescriptionPercentage": true,
    "alternativeHud": false,
    "hexagonStatsDisplay": false,
    "menuScaleMultiplier": 1.0,
    "healthBarPosX": 10,
    "healthBarPosY": 20,
    "energyBarPosX": 10,
    "energyBarPosY": 10,
    "staminaBarPosX": 10,
    "staminaBarPosY": 10,
    "storyHardDifficulty": false,
    "cameraMovementDuringFlight": true
  }
}
```


## Top-level keys

| Key | Type | Default | Description |
| :-- | :-- | :-- | :-- |
| `configVersion` | Integer | `3` | Internal version marker for the user config format. DO NOT CHANGE PLS. |
| `hud` | Object | See below | Groups all HUD and interface-related settings. |

## hud

The `hud` section contains visual and interface options for the client.

### HUD settings

| Key | Type | Default | Description |
| :-- | :-- | :-- | :-- |
| `firstPersonAnimated` | Boolean | `true` | Enables animated first-person visual behavior where supported by the client HUD or player view systems. |
| `xenoverseHudPosX` | Integer | `5` | Horizontal position value for the Xenoverse-style HUD layout. |
| `xenoverseHudPosY` | Integer | `5` | Vertical position value for the Xenoverse-style HUD layout. |
| `advancedDescription` | Boolean | `true` | Enables advanced descriptions in supported HUD or menu displays. |
| `advancedDescriptionPercentage` | Boolean | `true` | Changes advanced description numeric formatting to percentage mode where supported. |
| `alternativeHud` | Boolean | `false` | Switches the client to the alternative HUD layout. |
| `hexagonStatsDisplay` | Boolean | `false` | Uses a hexagon-style stat display in supported UI screens. |
| `menuScaleMultiplier` | Number | `1.0` | Scale multiplier applied to menus and related interface elements. |
| `healthBarPosX` | Integer | `10` | Horizontal position value for the health bar. |
| `healthBarPosY` | Integer | `20` | Vertical position value for the health bar. |
| `energyBarPosX` | Integer | `10` | Horizontal position value for the energy bar. |
| `energyBarPosY` | Integer | `10` | Vertical position value for the energy bar. |
| `staminaBarPosX` | Integer | `10` | Horizontal position value for the stamina bar. |
| `staminaBarPosY` | Integer | `10` | Vertical position value for the stamina bar. |
| `storyHardDifficulty` | Boolean | `false` | User config toggle related to Story Hard Difficulty. |
| `cameraMovementDuringFlight` | Boolean | `true` | Enables camera movement effects during flight. |

## Health display formatting

`advancedDescriptionPercentage` changes how at least one advanced HUD description is rendered.

When `advancedDescriptionPercentage` is `true`, the text is formatted as a rounded percentage:

```text
75%
```

This comes from logic equivalent to:

```java
String.format("%.0f", health / maxHealth * 100) + "%"
```

When `advancedDescriptionPercentage` is `false`, the text is formatted as current value over maximum value:

```text
150 / 200
```

This comes from logic equivalent to:

```java
numberFormat.format(health) + " / " + numberFormat.format(maxHealth)
```


## Position fields

The following keys are position values used to place HUD elements on screen:

- `xenoverseHudPosX`
- `xenoverseHudPosY`
- `healthBarPosX`
- `healthBarPosY`
- `energyBarPosX`
- `energyBarPosY`
- `staminaBarPosX`
- `staminaBarPosY`

In general:

- `PosX` controls horizontal placement.
- `PosY` controls vertical placement.
- Higher or lower values move the corresponding HUD element relative to its UI anchor logic.


## Visual toggles

Several HUD settings act as simple feature toggles.


| Key | Default | Effect |
| :-- | :-- | :-- |
| `firstPersonAnimated` | `true` | Turns first-person animation behavior on or off. |
| `advancedDescription` | `true` | Turns advanced descriptions on or off. |
| `advancedDescriptionPercentage` | `true` | Switches advanced numeric display to percentage mode where used. |
| `alternativeHud` | `false` | Turns the alternative HUD layout on or off. |
| `hexagonStatsDisplay` | `false` | Turns the hexagon-style stat display on or off. |
| `storyHardDifficulty` | `false` | Stores the Story Hard Difficulty user toggle. |
| `cameraMovementDuringFlight` | `true` | Turns flight camera movement on or off. |

## Numeric fields

The numeric entries in this file fall into two groups.

### Position values

These values define UI placement:

- `xenoverseHudPosX`
- `xenoverseHudPosY`
- `healthBarPosX`
- `healthBarPosY`
- `energyBarPosX`
- `energyBarPosY`
- `staminaBarPosX`
- `staminaBarPosY`


### Scale value

This value controls interface scaling:

- `menuScaleMultiplier`

A value of `1.0` represents the normal menu scale defined by the current implementation.