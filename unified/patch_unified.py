#!/usr/bin/env python3
from pathlib import Path
import json,struct,zlib,shutil
ROOT=Path("upstream")
def update(path,old,new):
 p=ROOT/path
 s=p.read_text(encoding="utf-8")
 if old not in s:raise RuntimeError("Missing patch anchor: "+path+" "+old[:50])
 p.write_text(s.replace(old,new),encoding="utf-8")
p=ROOT/"src/main/resources/fabric.mod.json"
obj=json.loads(p.read_text())
obj["id"]="ankhack"
obj["name"]="AnkHack Unified"
obj["description"]="AnkHack 1.0, one integrated client codebase with Wurst and ThunderHack ports"
obj["icon"]="assets/ankhack/icon.png"
obj["contact"]={"sources":"https://github.com/morganbanananka-max/AnkMPlus"}
obj["custom"]["ankhack:build_number"]="$"+"{build_number}"
obj["depends"]["fabricloader"]=">=0.19.3"
p.write_text(json.dumps(obj,indent=2)+"\n")
update("src/main/java/meteordevelopment/meteorclient/MeteorClient.java",
 'public static final String MOD_ID = "meteor-client";',
 'public static final String MOD_ID = "ankhack";')
update("gradle.properties","archives_base_name=meteor-client","archives_base_name=AnkHack-Unified")
update("gradle/libs.versions.toml",'fabric-loader = "0.18.2"','fabric-loader = "0.19.3"')
assets=ROOT/"src/main/resources/assets"
shutil.copytree(assets/"meteor-client",assets/"ankhack",dirs_exist_ok=True)
theme="src/main/java/meteordevelopment/meteorclient/gui/themes/meteor/MeteorGuiTheme.java"
update(theme,'super("Meteor");','super("AnkHack Gold");')
colors={
 "new SettingColor(145, 61, 226)":"new SettingColor(255, 201, 55)",
 "new SettingColor(45, 125, 245, 100)":"new SettingColor(255, 201, 55, 100)",
 "new SettingColor(20, 20, 20, 200)":"new SettingColor(10, 14, 21, 245)",
 "new SettingColor(30, 30, 30, 200)":"new SettingColor(17, 24, 34, 245)",
 "new SettingColor(40, 40, 40, 200)":"new SettingColor(29, 40, 55, 245)",
 "new SettingColor(50, 50, 50)":"new SettingColor(43, 55, 69)",
 "new SettingColor(0, 0, 0)":"new SettingColor(39, 52, 68)",
 "new SettingColor(10, 10, 10)":"new SettingColor(52, 68, 86)",
 "new SettingColor(20, 20, 20)":"new SettingColor(74, 89, 109)",
 "new SettingColor(130, 0, 255)":"new SettingColor(255, 198, 42)",
 "new SettingColor(140, 30, 255)":"new SettingColor(255, 207, 60)",
 "new SettingColor(150, 60, 255)":"new SettingColor(255, 217, 88)",
 "new SettingColor(100,35,170)":"new SettingColor(222,167,35)"
}
p=ROOT/theme
s=p.read_text()
for a,b in colors.items():s=s.replace(a,b)
p.write_text(s)
update("src/main/java/meteordevelopment/meteorclient/gui/GuiThemes.java",'select("Meteor");','select("AnkHack Gold");')
key="src/main/java/meteordevelopment/meteorclient/utils/misc/input/KeyBinds.java"
update(key,'KeyBinding.Category.create(MeteorClient.identifier("meteor-client"))',
 'KeyBinding.Category.create(MeteorClient.identifier("ankhack"))')
update(key,'"key.meteor-client.open-gui"','"key.ankhack.open-gui"')
update(key,'"key.meteor-client.open-commands"','"key.ankhack.open-commands"')
for lang in (assets/"ankhack"/"lang").glob("*.json"):
 try:
  d=json.loads(lang.read_text())
  d["key.ankhack.open-gui"]="Open AnkHack"
  d["key.ankhack.open-commands"]="AnkHack commands"
  lang.write_text(json.dumps(d,ensure_ascii=False,indent=2)+"\n")
 except Exception: pass
target=ROOT/"src/main/java/meteordevelopment/meteorclient/systems/modules"
shutil.copy2("unified/native/AnkBoatFly.java",target/"movement/AnkBoatFly.java")
shutil.copy2("unified/native/AnkAutoLeave.java",target/"misc/AnkAutoLeave.java")
shutil.copy2("unified/native/AnkSmartMace.java",target/"combat/AnkSmartMace.java")
modules="src/main/java/meteordevelopment/meteorclient/systems/modules/Modules.java"
update(modules,"        add(new AutoWalk());","        add(new AutoWalk());\n        add(new AnkBoatFly());")
update(modules,"        add(new AutoCrystal());","        add(new AutoCrystal());\\n        add(new AnkSmartMace());")
# Register Thunder module in misc category, avoiding duplicates.
p=ROOT/modules
s=p.read_text()
pos=s.find("private void initMisc()")
if pos<0:raise RuntimeError("No misc module init")
start=s.find("        add(new ",pos)
if start<0:raise RuntimeError("No misc add")
s=s[:start]+"        add(new AnkAutoLeave());\n"+s[start:]
p.write_text(s)
size=64
pixels=[[(9,14,22,255) for x in range(size)] for y in range(size)]
for y in range(size):
 for x in range(size):
  dx=x-32;dy=y-31
  if dx*dx+dy*dy<27*27:pixels[y][x]=(255,203,56,255)
  if 13<x<49 and 33<y<42:pixels[y][x]=(241,164,41,255)
  if 23<x<34 and 20<y<32:pixels[y][x]=(255,243,185,255)
  if 29<x<35 and 22<y<28:pixels[y][x]=(7,11,16,255)
  if 21<x<47 and 36<y<43:pixels[y][x]=(244,145,39,255)
def chunk(tag,data):
 return struct.pack(">I",len(data))+tag+data+struct.pack(">I",zlib.crc32(tag+data)&0xffffffff)
raw=b''.join(b'\x00'+b''.join(bytes(px) for px in row) for row in pixels)
png=b'\x89PNG\r\n\x1a\n'+chunk(b'IHDR',struct.pack(">IIBBBBB",size,size,8,6,0,0,0))+chunk(b'IDAT',zlib.compress(raw,9))+chunk(b'IEND',b'')
(assets/"ankhack"/"icon.png").write_bytes(png)
(ROOT/"ANKHACK_LICENSE_AND_ATTRIBUTIONS.md").write_text("""AnkHack Unified: GPL-3.0 source fork from Meteor Client
https://github.com/MeteorDevelopment/meteor-client
Additional port from Wurst BoatFly:
https://github.com/Wurst-Imperium/Wurst7 (GPL-3.0)
Additional port from ThunderHack AutoLeave:
https://github.com/Pan4ur/ThunderHack-Recode (GPL-3.0)
Copyright notices and LICENSE remain in source. No cracked proprietary client binaries.
Build experimental, NOT equivalent to all original clients.
""")
print("Patched one unified client source project")
