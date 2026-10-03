# 🐉 Contributing to DragonMineZ

Thank you for your interest in contributing to DragonMineZ! Whether you want to report a bug, improve documentation,
propose features, or help with development, we’re excited to collaborate with you. If you want to donate to the project,
you can jump to the [Patreon](#-supporting-the-project-on-patreon) section.

Please follow the guidelines below to ensure a smooth process for all contributors.

## Getting Started

1. **Read the Code of Conduct**

   - By contributing, you agree to uphold the [Code of Conduct](https://github.com/DragonMineZ/DragonMineZ/blob/main/.github/CODE_OF_CONDUCT.md).
2. **Check Existing Issues**

   - Look through the [Issues](https://github.com/orgs/DragonMineZ/projects/5) to see if someone has already reported
     your concern or proposed your idea.
3. **Fork the Repository**

   - Clone your fork to your local environment for testing and development:

   ```bash
   git clone https://github.com/your-username/DragonMineZ.git
   ```
4. **Set Up Your Environment** — Ensure you have the necessary tools installed:

   - Java Development Kit (JDK) 17 (the build uses a Java 17 toolchain)
   - Git
   - An IDE with Gradle support (IntelliJ IDEA or Eclipse)

   You don't need to install Gradle or Forge yourself: the repository ships the Gradle wrapper (`gradlew` / `gradlew.bat`),
   which downloads Gradle, Minecraft Forge (`1.20.1-47.4.10`) and every dependency on the first build.

If you are not forking and/or creating an **addon**, there is no need to follow step 3. For addon development, see
[[Getting Started|Getting-Started]] instead.

## 🛠️ Building and Running

Run these from the repository root (`./gradlew` on Linux/macOS, `.\gradlew.bat` on Windows):

| Command | What it does |
| :-- | :-- |
| `./gradlew build` | Full build. The shippable jar is `build/libs/dragonminez-<version>.jar` (it bundles the database libraries); the `-slim` jar is not meant to be shipped. |
| `./gradlew runClient` | Starts a dev client (working directory `run/`). |
| `./gradlew runServer` | Starts a dev dedicated server (working directory `run/`). |
| `./gradlew runData` | Runs data generation (working directory `run-data/`) and writes to `src/generated/resources/`. Run it after changing datagen providers and review the generated diff. |

- There is **no automated test suite**. Validate your change with `build` and, when it affects gameplay, by testing it in
  `runClient` and/or `runServer`.
- Client-only dev mods (JEI runtime, shaders, etc.) are only added to `runClient`; keep it that way so they can't leak into
  generated data or the dedicated server.
- Resource optimization (PackSquash) runs on CI and is off for local builds by default. Force it with
  `-PoptimizeResources=true` or `-PoptimizeResources=false`.

## 🌐 Translations

- `src/main/resources/assets/dragonminez/lang/en_us.json` is the source language file. Add an entry there for every new
  piece of player-visible text, and keep existing keys stable.
- Other languages are translated by the community on Crowdin. The English file is uploaded to Crowdin automatically
  when it changes on `main`.
- Check that every language file is valid JSON before opening a PR with `scripts/check_lang.sh` (requires `jq`).

## How to Contribute

### 🐛 Reporting Bugs

If you’ve encountered a bug, please:

- Open a [Bug Report](https://github.com/DragonMineZ/DragonMineZ/issues/new?template=bug_report.yml).
- Include as much detail as possible:
  - A clear description of the issue
  - Steps to reproduce
  - Expected vs. actual results
  - Screenshots, logs, or crash reports (if applicable)

### 🎮 Game Testing

Anyone is free to test our public releases and share what they find with us — bugs, exploits, balance issues,
crashes, or anything else that seems off. You don't need to be a developer or open a PR to help this way.

- Test against **public releases only**. Do not test against unreleased or internal builds unless you've been given
  explicit access.
- Share your findings with us via a [Bug Report](https://github.com/DragonMineZ/DragonMineZ/issues/new?template=bug_report.yml)
  or on Discord.
- If what you find is a **security vulnerability** (e.g. something that could be exploited to harm other players or
  servers), follow our [[Security Policy|Security]] instead of opening a public issue.

### ⭐ Suggesting Features

Have a great idea? Open
a [Feature Request](https://github.com/DragonMineZ/DragonMineZ/issues/new?template=feature_request.yml) and provide:

- A clear and concise explanation of the feature
- Potential benefits and impact
- Any alternatives or related concepts

###### Alternatively, you can suggest features in our Discord server.

### ⏳ Submitting Pull Requests (PRs)

#### Step 1: Create a Branch

`main` holds the current stable release line; the next version is developed on its own version branch (for example
`v2.2`). Base your branch on the one your change is meant for — if you're unsure, ask on Discord.

Use a descriptive branch name:

```bash
git checkout -b fix/short-description
```

#### Step 2: Make Your Changes

- Write clean and concise code.
- Test your changes locally to ensure they work as intended.
- Follow the project's coding conventions and standards.

#### Step 3: Commit Your Changes

Write a meaningful commit message:

```bash
git commit -m "Fix: Resolved issue with XYZ feature"
```

#### Step 4: Push Your Changes

```bash
git push origin fix/short-description
```

#### Step 5: Open a Pull Request

Go to your fork on GitHub and submit a [Pull Request](https://github.com/DragonMineZ/DragonMineZ/pulls).

- Describe the changes and link related issues.
- Be prepared to discuss and revise your code based on feedback.
- Every pull request is compiled automatically by the **Compile Check** workflow (`./gradlew build`). If it fails, your
  PR will not be merged. Workflow runs for first-time contributors may need a maintainer's approval before they start.

#### Automated checks

| Workflow | When it runs | What it does |
| :-- | :-- | :-- |
| Compile Check | Every pull request, and pushes to any branch except `main` that touch code, resources or Gradle files | Runs `./gradlew build` |
| Java CI | Pull requests and pushes to `main` that touch Java or `build.gradle.kts` | Runs `./gradlew build` and submits the dependency graph |
| Crowdin Check & Upload | Pushes to `main` that touch the lang folder, every 2 days, or manually | Validates the language files with `scripts/check_lang.sh` and uploads `en_us.json` to Crowdin |
| CodeQL | Daily | Security analysis of the Java code and workflows |

Releases are published to Modrinth and CurseForge from `main` by the maintainers' release workflows.

## 📚 Contributor Guidelines

1. **Coding Standards**

   - Use descriptive variable and method names.
   - Format code consistently (e.g., indentations, spacing).
   - Write comments for complex logic.
2. **Documentation**

   - Update or add documentation for new or modified features.
   - Ensure the [[Wiki|Home]] reflects major changes.
3. **Testing**

   - Test thoroughly across relevant scenarios, including a dedicated server (`runServer`) when your change touches
     client classes, networking or server logic.
   - Fix any linting or compilation issues before submitting your PR.
4. **Compatibility**

   - Many systems read JSON that players, server owners and addon authors edit (configs, quests, wishes, dragon ball
     packs). Don't rename or remove config keys, JSON fields, NBT save keys or registry names without discussing it
     first — older worlds and configs must still load.
   - Network packets are identified by registration order: add new packets at the end of `NetworkHandler.register()`,
     never in the middle.
   - Validate everything a client sends on the server; never trust the client for stats, unlocks, quests, wishes or
     progression.
5. **Project docs**

   - `CLAUDE.md` and the `AI/` folder (`Agents.md`, `Context.md`, `Memory.md`, `QuestAddonAPI.md`) describe the
     architecture, data systems, build and release automation in detail. They are written for AI coding assistants but
     are just as useful for human contributors — read `AI/Context.md` before larger changes.

## 💬 Community Communication

- Join our [Discord server](https://discord.gg/b5MgRNb3D7) for real-time discussions.

## 🫴 Supporting the Project on Patreon

DragonMineZ is a community-driven project, and your support helps us keep improving and delivering new features!
Consider becoming a patron on [Patreon](https://patreon.com/DragonMineZ) to support the development and maintenance of
the mod.

Patrons receive exclusive benefits, such as:

- Early access to new features
- Behind-the-scenes updates
- Recognition in our community

Every contribution, no matter how small, helps us continue improving DragonMineZ. Thank you for your support!

## 🧾 Licensing

By contributing, you agree that your contributions will be licensed under the same [license](https://github.com/DragonMineZ/DragonMineZ/blob/main/LICENSE) that governs the
project.

## 🙌 Thank You!

We’re excited to see your contributions and ideas! If you have any questions or need assistance, please reach out to the
team.
