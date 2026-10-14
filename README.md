# NeoHouse

Maintained by: @drakou111

---

# Overview

**NeoHouse** is a standalone replacement for [Housing Editor](https://www.housingeditor.com/) and [HTSL](https://github.com/BusterBrown1218/HTSL).

Existing tools like Housing Editor rely on external dependencies such as ChatTriggers and web-based workflows. NeoHouse is code-oriented and fully in-game - no external websites required.

NeoHouse's language has a bit more abstraction than HTSL, but still allows for 1:1 representations if used properly.

**NeoHouse** provides:

- A fully in-game scripting environment
- A custom-designed coding language
- An integrated IDE with syntax highlighting, autocomplete, and argument hints
- A self-contained workflow

---

# Screenshots

![Screenshot](docs/img_1.png)

![Screenshot](docs/img_3.png)

---

# Getting Started

## Download

You can download **NeoHouse** from [Modrinth](https://modrinth.com/project/neohouse)

Choose the version that matches your Minecraft version, then place the `.jar` file into your `mods` folder.

## Requirements

- [Fabric Loader](https://fabricmc.net/use/) and [Fabric API](https://modrinth.com/project/P7dR8mSH)
- In Housing, turn on chat inputs (NeoHouse types values into chat prompts instead of anvils)

## Supported Versions

- Minecraft 1.21.2 to 1.21.11, 26.1, 26.2 and 26.3
- Fabric only (NeoForge support was dropped)

## Building

`./gradlew chiseledBuild` builds a jar for every supported version into `dist/`. Gradle runs on Java 25
and downloads it (and Java 21 for the 1.21.x builds) when needed. `./gradlew :fabric:test` runs the
compiler tests for the active version (set with `./gradlew stonecutterSwitchTo<version>`).

---

# License

**NeoHouse is All Rights Reserved.** The source is publicly viewable for transparency and reference only - you may view the code and use the official compiled releases, but copying, modifying, redistributing, or re-uploading the mod is not permitted without written permission. See [LICENSE](LICENSE) for the full terms.

---

# Notes

This project is still in active development. Features, syntax, and compatibility may change between versions.
