#!/usr/bin/env python3
"""Integrate the actual Baritone 1.21.11 pathfinding engine as an internal library.

This is a nested library dependency (not a second hack client), automatically
detected by Meteor's PathManagers and exposed to AnkHack's existing GUI/modules.
Baritone is LGPL-3.0; retain standalone library and license so it is replaceable.
"""
from pathlib import Path

root=Path("upstream")
v=root/"gradle/libs.versions.toml"
s=v.read_text()
old='baritone = "1.21.10-SNAPSHOT"'
assert s.count(old)==1
v.write_text(s.replace(old,'baritone = "1.21.11-SNAPSHOT"'))

build=root/"build.gradle.kts"
s=build.read_text()
old="    modCompileOnly(libs.baritone)"
assert s.count(old)==1
s=s.replace(old,'''    // Baritone is a real 1.21.11 LGPL-3.0 pathfinding library. Loom's
    // modInclude keeps a single installable AnkHack JAR and preserves
    // Baritone as a separable/replaceable library for LGPL compliance.
    modInclude(libs.baritone)''')
build.write_text(s)

notice=root/"ANKHACK_LICENSE_AND_ATTRIBUTIONS.md"
with notice.open("a") as out:
    out.write("""
Baritone 1.21.11 pathfinder:
  Library: meteordevelopment:baritone:1.21.11-SNAPSHOT, LGPL-3.0.
  https://github.com/MeteorDevelopment/baritone
  Baritone is an internal, separable Fabric library, not a nested hack
  CLIENT. It is used by the existing AnkHack PathManagers, Mine, Follow,
  AutoWalk and Baritone-supported GUI features. Its corresponding source
  is available from its upstream repository; LGPL replacement allowed.
  Verify actual multiplayer pathfinding in Minecraft before claiming success.
""")
print("Configured true 1.21.11 Baritone engine as an integrated replaceable library")
