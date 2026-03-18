# Developer Scope and Prerequisites

This wiki is intentionally scoped to developers and technical modders.

## Intended audience

- People who can edit JSON and resource pack assets.
- People who can run Minecraft Forge dev environments.
- People who want to contribute to DMZ-compatible content.

## Not in scope

- General player walkthroughs.
- Lore pages.
- Survival progression guides.

## Baseline setup

1. Run DMZ once so config files are generated.
2. Use this config root for local dev instances:
   - `config/dragonminez`
3. Race configuration lives in:
   - `config/dragonminez/races/<race_name>/`
4. Shared stack forms live in:
   - `config/dragonminez/forms/`
5. Config files for entities' attributes:
   - `config/dragonminez/entities.json`
6. Config files for general server settings:
   - `config/dragonminez/general-server.json`
7. Config files for general user (client) settings:
   - `config/dragonminez/general-user.json`
8. Config files for skills:
   - `config/dragonminez/skills.json`
9. Config files for skill-offerings:
   - `config/dragonminez/skill-offerings.json`

## Reload workflow

After changing JSON config files, you are able to reload in-game:

- Command: `/dmzreload`
- Command source: `src/main/java/com/dragonminez/server/commands/ReloadCommand.java`

Resource pack changes (models/textures) normally require a resource reload or restart.

DragonMineZ automatically retrieves language translations for your model, if you want custom translations, 
you have to create a file for it.
