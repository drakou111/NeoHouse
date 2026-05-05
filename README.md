Here’s your cleaned **complete `.md` file**:

# NeoHouse

Maintained by: @drakou111

---

## Overview

**NeoHouse** is a standalone replacement for [Housing Editor](https://www.housingeditor.com/) and [HTSL](https://github.com/BusterBrown1218/HTSL).

Existing tools like [Housing Editor](https://www.housingeditor.com/) rely on external dependencies such as ChatTriggers and web-based workflows. While I liked its Scratch-like visual approach, I wanted something more code-oriented and fully in-game.

Tools like [HTSL](https://github.com/BusterBrown1218/HTSL) improved the experience by introducing a coding language, but they still felt incomplete—lacking proper syntax highlighting, stronger compiler abstractions, and a more integrated IDE experience.

**NeoHouse** aims to address these limitations by providing:

- A fully in-game scripting environment  
- A custom-designed coding language  
- An integrated IDE  
- A self-contained workflow (no external websites required)  

---

## Screenshots

TODO

---

## Getting Started

### Download

You can download **NeoHouse** from Modrinth:  
<modrinth-link-TODO>

Choose the version and mod loader that matches your setup, then place the `.jar` file into your `mods` folder.

### Requirements

- Minecraft 1.21.x (1.21 → 1.21.11)  
- Fabric requires [Fabric API](https://modrinth.com/project/P7dR8mSH)

---

## Supported Versions

**NeoHouse** currently supports:

- Minecraft 1.21 → 1.21.11  
- Fabric and NeoForge

Some versions may still contain minor inconsistencies as development continues.

---

## Features

### Scripting Language

**NeoHouse** includes a custom scripting language designed specifically for Hypixel Housing.

Key features:

- Typed functions with parameters  
  ```java
    function long someFunction(double someParameter) { ... }
  ```

* Event, command, and menu modification support
* Multiple functions, events, commands, and menus per file
* Item support in actions and menus
  (Use `/nh copyitem` to retrieve item data)

Additional commands can be explored in-game:

```
/nh help
```

---

### Integrated IDE

**NeoHouse** includes a built-in in-game IDE focused on usability and customization.

Features include:

* Customizable UI themes (editor and interface)
* Code suggestions and autocomplete
* Function argument hints
* Auto-indentation and tab support

---

## Goals

**NeoHouse** is being developed with the following goals:

* Remove dependency on external websites
* Provide a more code-like experience than visual editors
* Keep everything accessible directly in-game
* Support more abstract and typed scripting features

---

## Notes

This project is still in active development. Features, syntax, and compatibility may change between versions.

