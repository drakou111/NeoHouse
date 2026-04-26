# Neo House
## Introduction

Hello! Neo House is a project that acts as a replacement over Housing Editor. The issues with Housing Editor is that it isn't quite up to date anymore.
It also depends on other mods like chattriggers, and relies on an external website to be used properly. I liked the scratch-like UI it had, but I would
rather have something closer to code. HTSL is a good program to use Housing Editor with a coding language, however I felt like it missed a couple things
that it could have added.

I therefore decided to code my own version of Housing Editor as its standalone mod, along with a coding language and (probably) a syntax highlighter on Visual Studio Code!

## Dependencies

This version currently only supports NeoForge 1.21.4, but is planned to work for fabric too, and maybe some older/newer versions.

## Installation

(NONE YET)

## Utilisation

(NONE YET)

## TODO List
- ✅ Make navigation system
    - ✅ Make the navigation logic
    - ✅ Map all housing slots into constants
      - ✅ Actions
      - ✅ Conditions
- 🚧 Make coding language (.nh)
  - ✅ Create syntax
  - ✅ Create AST Tree
  - ✅ Create tokenizer (Raw text -> Tokens)
  - ✅ Create parser (Tokens -> AST Tree)
  - 🚧 Create AST optimizations (AST Tree -> Optimized AST Tree)
  - ✅ Create code translation (Optimized AST Tree -> Output)

Note: I will obviously need to rework some parts, so ✅ doesn't mean 100% done, just mostly done.