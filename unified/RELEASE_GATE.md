# AnkHack complete-release gate

This project is NOT COMPLETE as of 2026-10-30. Compilation by itself does not certify hacks.

Source material: all 15 user-supplied client binaries / archives as documented in `unified/USER_SUBMITTED_ORIGINALS.md`. Modules in proprietary / protected clients require independent implementation; do not copy incompatible binaries or unsafe launchers.

## Definition of done

1. Every submitted client independently inventoried, versioned and inspected for actual functions. Source-only reports are not enough.
2. Each distinct function integrated as a real, enabled/disabled, source-backed module within ONE registry and Fabric identity.
3. Duplicate functions merged into one module with at least two **actual working** implementations where behavior differs; manual mode and per-server adaptive Auto. No cosmetic duplicate modes.
4. Per-world / per-server profiles safely isolate scores and user preferences; auto mode measures meaningful conditions and avoids endless cycling / packet spam.
5. A cohesive GUI with category browsing, live search, per-module sliders/modes/keybinds, HUD editing, dark/gold visual polish and accessible layout, verified with screenshots.
6. Baritone pathfinding actually works and is integrated without nesting a separate hack-client engine.
7. Test each module in Minecraft 1.21.11 with Java 21 / Fabric 0.19.3, record outcome; log compatibility with Essential and other mods.
8. Run startup, configuration persistence, rendering, movement, combat, inventory, server corrections, performance and multiplayer recovery QA.
9. Publish source, modifications, GPL notices and attribution. Do not merge or distribute protected proprietary client internals.
10. No user testing requested while any required port or QA is outstanding.

## Current status

**NOT RELEASE-READY.** The 1.0.7 CI verifies compilation and module registration, not gameplay behavior or complete integration.

## Actual recent changes

- ThunderHack Reborn 1.21.11 ToolSaver and NoServerSlot natively ported with safety checks.
- ThunderHack Reborn HotbarReplenish SWAP implementation combined with Meteor AutoReplenish MERGE under one module; configurable manual selection and server-local heuristic feedback.
- Earlier native integrations: Wurst AutoSwim / Glide / BoatFly, LiquidBounce-inspired Sprint / CombatRod, ThunderHack AutoSoup / AutoLeave / AntiAFK, native SmartMace and double-tap Flight.

No imaginary module counts or false completion claims: consult the machine-produced source inventory and compile artifacts.
