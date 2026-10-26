# DragonMineZ translations

The mod downloads these files from the `main` branch at runtime, when a player selects the language in Minecraft. A merged pull request reaches players the next time they start the game or switch language; no new mod release is needed.

- Source (English) file: [`src/main/resources/assets/dragonminez/lang/en_us.json`](../src/main/resources/assets/dragonminez/lang/en_us.json). It ships inside the mod jar and is never downloaded.
- Each file is named after its Minecraft language code (`es_es.json`, `pt_br.json`, ...).
- `es_mx.json` is shared by every Latin American Spanish variant: `es_ar`, `es_cl`, `es_ec`, `es_mx`, `es_uy` and `es_ve`.
- Keep the keys from `en_us.json` and translate only the values. Missing keys fall back to English.
- Use `§` formatting codes (for example `§e`) exactly as they appear in `en_us.json`.

## Contributing

1. Edit the file for your language, or add a new `<language_code>.json` copied from `en_us.json`.
2. Check the JSON syntax with `scripts/check_lang.sh` (needs `jq`). The same check runs on every pull request.
3. Open a pull request against `main`.
