# AnkHack user-provided input inventory (2026-10-30)

**Never claim a client is integrated based on its name, JAR size, module-list labels, or a successful compile.**

The following original client files were located and statically inspected in the conversation workspace. This is a file inventory, not a declaration that they are safe to execute or that all their features have been ported.

| Original provided file | Fabric mod ID found | Observed contents | Integration status |
|---|---|---|---|
| `meteor-client-1.21.11-86.jar` | `meteor-client` | 1,349 class files | Primary integrated source base, version lineage not byte-for-byte equivalent |
| `Wurst-Client-v7.56.1-MC1.21.11.jar` | `wurst` | 815 class files | A few native GPL ports; most not integrated |
| `ThunderHack-Reborn-main.zip` | source archive | **536 Java files; source explicitly targets 1.21.11** | AutoSoup port; larger source integration incomplete |
| `liquidbounce-e77a669.zip` | binary archive | Includes `liquidbounce.jar`, no Kotlin source | Several GPL source-derived variants; user-supplied binary not merged |
| `baritone-meteor-1.21.11.jar` | `baritone-meteor` | 465 class files; mixins and nested pathfinder | Full pathfinding engine not integrated |
| `NexonClient 1.21.11.jar` | `nexonclient` | 446 class files | Not integrated |
| `WaterClientV6 1.21.11` | `water` | 199 class files | Not integrated |
| `cipherclient-1.2.1+mc1.21.11.jar` | `ciphermod` | 866 class files | Not integrated |
| `anubis-beta-0.9.9.1.jar` | `anubisplus` | 1,118 class files | Not integrated |
| `aurora client 1.21.11.jar` | `jimfs` | 307 class files | Not integrated; loading metadata requires separate review |
| `Gooba 1.21.11.jar` | `white` | 616 class files | Not integrated; names obfuscated |
| `Krypton Client 1.21.11.jar` | `krloader` | 496 class files | Not integrated; loader/native code must be reviewed |
| `LarpClient 1.21.11.jar` | `larp-client` | 1,316 class files | Not integrated; protected classes and updater present |
| `Echo Client 1.21.11.jar` | `echoloader` | 12 class files | Not integrated; loader-style package |
| `rusherhack 1.21.11.jar` | `rusherhack` | 2,151 class files | Not integrated; proprietary content / protected classes |

Additional task constraints:
- One functional native client, not several original clients inside a JAR.
- Distinct hacks should appear as independent working features.
- Duplicate hacks need explicit manual mode selection and evidence-driven Auto per world/server (with conservative fallback).
- Preserve real advanced settings, test gameplay and compatibility. Do not inflate module counts with HUD entries.
- Do not copy protected/proprietary code simply because a binary was submitted. Independent reimplementations may cover the behavior after a complete functional analysis.
- Do not ask the user to test before all user-requested integrations are accounted for.
- No build artifact may be advertised as the completed AnkHack until the coverage checklist and in-game tests actually pass.

This document is grounded in the locally supplied files, not merely public GitHub upstream names. The supplied ThunderHack Reborn 1.21.11 source is **not** the same source as archived Pan4ur/ThunderHack-Recode from 2024.
