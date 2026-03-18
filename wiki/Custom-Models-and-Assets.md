# Custom Models and Assets

You can add custom race models and textures through resource assets and config values. You don't need to build 
a separate addon for this, but you do need to edit JSON config files and add assets to your resource pack.

## How model selection works

DMZ resolves model keys from:

1. Active form `customModel`
2. Race `customModel`
3. Fallback race defaults

Reference:

- `src/main/java/com/dragonminez/client/model/DMZPlayerModel.java`

## Geometry path conventions

Custom model files are looked up in this namespace/path:

- `assets/dragonminez/geo/entity/races/<model_key>.geo.json`

If the race has gender enabled, DMZ can append gender suffixes:

- `assets/dragonminez/geo/entity/races/<model_key>_male.geo.json`
- `assets/dragonminez/geo/entity/races/<model_key>_female.geo.json`

This suffix behavior comes from `DMZPlayerModel#resolveCustomModel`.

## Body texture conventions

For custom model keys (non built-in), DMZ tries:

- `assets/dragonminez/textures/entity/races/<model_key>.png`
- If race has gender: `.../<model_key>_male.png` or `.../<model_key>_female.png`

Reference:

- `src/main/java/com/dragonminez/client/util/SkinGathererProvider.java`

## Face texture conventions for custom races/models

DMZ custom face layer expects this folder:

- `assets/dragonminez/textures/entity/races/<face_key>/faces/`

Naming pattern:

- `<face_key>_eye_<eyesType>_0.png`
- `<face_key>_eye_<eyesType>_1.png`
- `<face_key>_eye_<eyesType>_2.png`
- `<face_key>_eye_<eyesType>_3.png`
- `<face_key>_nose_<noseType>.png`
- `<face_key>_mouth_<mouthType>.png`

Reference:

- `src/main/java/com/dragonminez/client/render/layer/DMZSkinLayer.java` (`renderCustomFace`)

## How to assign your model key

Pick one place:

- Race default model in `races/<race>/character.json` -> `customModel`
- Specific form in `races/<race>/forms/<group>.json` -> `forms.<form>.customModel`

Then add matching geo and textures in your resource pack assets.

## Minimal checklist

1. Set `customModel` in race or form config.
2. Add `geo/entity/races/<model_key>.geo.json`.
3. Add base body texture (and gender variants if needed).
4. Add face textures for all used eye/nose/mouth indices.
5. Reload resources and run `/dmzreload`. (Sometimes you will need to restart the game.)

