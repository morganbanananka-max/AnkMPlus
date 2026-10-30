#!/usr/bin/env python3
"""Harden AnkHack's shared adaptive variant registry.

Previously, an unregistered family silently inherited Flight variants.
That caused bogus module choices. Unknown families now return no candidates.
Registered families retain their explicit ordered fallback and persisted
per-world evidence. This patch runs after all source-derived variant patches.
"""
from pathlib import Path

path=Path("upstream/src/main/java/meteordevelopment/meteorclient/AnkVariantProfiles.java")
text=path.read_text()
old='default -> new String[]{"Abilities","Velocity","WurstMotion"};'
new='case "flight" -> new String[]{"Abilities","Velocity","WurstMotion"};\n            default -> new String[0];'
assert text.count(old)==1, f"Variant registry unknown fallback anchor: {text.count(old)}"
text=text.replace(old,new,1)

old='String choice=variants(module)[0];int max=Integer.MIN_VALUE;'
new='String[] available=variants(module);\n        if(available.length==0)return "";\n        String choice=available[0];int max=Integer.MIN_VALUE;'
assert text.count(old)==1, f"Unsafe choose() anchor: {text.count(old)}"
text=text.replace(old,new,1)

old='''        for(String variant:variants(module)) {
            if(!hasEvidence(server,module,variant))continue;'''
new='''        for(String variant:available) {
            if(!hasEvidence(server,module,variant))continue;'''
assert text.count(old)==1, f"Evidence variant loop anchor: {text.count(old)}"
text=text.replace(old,new,1)
path.write_text(text)

families={
 "flight":("Abilities","Velocity","WurstMotion"),
 "sprint":("Strict","Rage","LiquidLegit","LiquidOmni"),
 "auto-fish":("MeteorEntity","WurstMotion"),
 "auto-gap":("Meteor","ThunderOffhand"),
 "bow-spam":("Meteor","Thunder")
}
for family,options in families.items():
    assert f'case "{family}"' in text,(family,"missing registered variant")
    assert all(f'"{x}"' in text for x in options),(family,"missing variant names")
assert "default -> new String[0]" in text
assert "if(available.length==0)return" in text
print("Variant safety checks: unregistered modules cannot inherit Flight modes.")
