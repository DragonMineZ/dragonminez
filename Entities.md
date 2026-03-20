## Keys

| Key | Type | Description |
| :-- | :-- | :-- |
| `configVersion` | Integer | Internal version marker for the entity config format. PLS DO NOT CHANGE. |
| `hardModeSettings` | Object | Stores global multipliers used by hard mode entity scaling. |
| `defaultEntityStats` | Object | Stores default stat values for each configured entity. |

## configVersion

`configVersion` is the internal schema version for this config file. PLS DO NOT CHANGE.


| Key | Type | Default | Description |
| :-- | :-- | :-- | :-- |
| `configVersion` | Integer | `3` | Internal version used to track the config structure. |

## hardModeSettings

`hardModeSettings` contains the global multipliers used when hard mode scaling is applied.


| Key | Type | Default | Description |
| :-- | :-- | :-- | :-- |
| `hpMultiplier` | Number | `3.0` | Multiplies entity health in hard mode. |
| `damageMultiplier` | Number | `2.0` | Multiplies entity damage in hard mode. |

### hardModeSettings details

- `hpMultiplier` affects health scaling.
- `damageMultiplier` affects damage scaling.
- Both values are numeric multipliers.


## defaultEntityStats

`defaultEntityStats` maps each entity registry name to a stat object.

Each entity entry contains the following fields:


| Key | Type | Description |
| :-- | :-- | :-- |
| `health` | Number | Base health value for the entity. |
| `meleeDamage` | Number | Base melee damage value for the entity. |
| `kiDamage` | Number | Base ki damage value for the entity. |
