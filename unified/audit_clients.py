#!/usr/bin/env python3
"""Reproducible public-upstream source candidate inventory for AnkHack.

A Java/Kotlin source candidate is not proof of a functioning Minecraft module.
This report never marks an unported external module as implemented.
"""
import collections, csv, json, os, re
from pathlib import Path
from urllib.request import urlopen, Request

SOURCES={
 "Meteor":("MeteorDevelopment/meteor-client","02973974afdfb715b193813959a837695049f7c1"),
 "Wurst":("Wurst-Imperium/Wurst7","0395de014ba530c27b32dd9c7ac7131580cba29a"),
 "LiquidBounce":("CCBlueX/LiquidBounce","79c8ee07138431a2a4f5fa0b346b4df942ab5ca9"),
 "ThunderHack":("Pan4ur/ThunderHack-Recode","49c76bcc5a5e4fbd12b06305c09f0c8fb76c510b"),
 "Baritone":("MeteorDevelopment/baritone","1cdb177f196fb49d11b59367a526b237e34dda97")
}
ADAPTATIONS={
 ("Wurst","boatfly"):"BoatFly: Wurst-derived source adaptation",
 ("Wurst","flight"):"Flight: WurstMotion variant",
 ("Wurst","autofish"):"AutoFish: Wurst-derived bite detector",
 ("ThunderHack","autoleave"):"AutoLeave native port",
 ("ThunderHack","antiafk"):"AntiAFK: ThunderTimed variant",
 ("LiquidBounce","sprint"):"Sprint: LiquidLegit / LiquidOmni variants"
}
ALIASES={
 "fly":"flight","flyhack":"flight","autosprint":"sprint",
 "antifishing":"autofish","antiidle":"antiafk",
 "cheststeal":"cheststealer","killaurahack":"killaura"
}
def canonical(name):
 name=name.removeprefix("Module").removesuffix("Hack")
 key=re.sub("[^a-zA-Z0-9]","",name).lower()
 return ALIASES.get(key,key)
def category(path):
 parts=path.split("/")
 for a in ("modules","hacks"):
  if a in parts:
   i=parts.index(a)
   return parts[i+1] if i+1<len(parts) else ""
 return "pathing"
def candidates(src,tree):
 for p in sorted(x["path"] for x in tree if x["type"]=="blob"):
  base=p.rsplit("/",1)[-1].split(".")[0]
  if src=="Wurst" and re.fullmatch("src/main/java/net/wurstclient/hacks/[^/]+Hack[.]java",p):
   yield base.removesuffix("Hack"),p
  elif src=="Meteor" and re.fullmatch("src/main/java/meteordevelopment/meteorclient/systems/modules/[^/]+/[^/]+[.]java",p):
   yield base,p
  elif src=="LiquidBounce" and re.fullmatch("src/main/kotlin/net/ccbluex/liquidbounce/features/module/modules/[^/]+/Module[^/]+[.]kt",p):
   yield base.removeprefix("Module"),p
  elif src=="ThunderHack" and re.fullmatch("src/main/java/thunder/hack/features/modules/[^/]+/[^/]+[.]java",p):
   if category(p)!="base":yield base,p
  elif src=="Baritone" and p.endswith(".java") and any("/"+kind+"/" in p for kind in ("process","behavior","pathing","api","command")):
   yield base,p
def tree(repo,sha):
 u="https://api.github.com/repos/"+repo+"/git/trees/"+sha+"?recursive=1"
 h={"User-Agent":"AnkHack-Inventory","Accept":"application/vnd.github+json"}
 if os.getenv("GITHUB_TOKEN"):h["Authorization"]="Bearer "+os.environ["GITHUB_TOKEN"]
 with urlopen(Request(u,headers=h),timeout=60) as res:data=json.load(res)
 if data.get("truncated") or not data.get("tree"):raise RuntimeError("Missing tree: "+repo)
 return data["tree"]
def main():
 out=Path("audit_results");out.mkdir(exist_ok=True)
 records=[]
 for source,(repo,sha) in SOURCES.items():
  items=list(candidates(source,tree(repo,sha)))
  if not items:raise RuntimeError("Empty audit for "+source)
  for name,p in items:
   family=canonical(name)
   status="not_integrated";notes="Source candidate. Runtime classification pending."
   if source=="Meteor":status,notes="baseline_source","Present in Meteor upstream base; runtime test pending"
   if source=="Baritone":status,notes="engine_not_integrated","Engine component candidate, not a standalone hack"
   if (source,family) in ADAPTATIONS:status,notes="partial_port",ADAPTATIONS[source,family]
   records.append(dict(source=source,revision=sha,category=category(p),source_name=name,
      family=family,path=p,status=status,notes=notes))
 groups=collections.defaultdict(set)
 for x in records:
  if x["source"]!="Baritone":groups[x["family"]].add(x["source"])
 for x in records:
  sources=groups.get(x["family"],set())
  x["duplicate_sources"]=",".join(sorted(sources)) if len(sources)>1 else ""
  x["duplicate_source_count"]=len(sources)
 with (out/"source_inventory.csv").open("w",newline="",encoding="utf8") as f:
  w=csv.DictWriter(f,fieldnames=list(records[0]));w.writeheader();w.writerows(records)
 duplicates={k:sorted(v) for k,v in groups.items() if len(v)>1}
 (out/"duplicate_families.json").write_text(json.dumps(duplicates,indent=2),encoding="utf8")
 c=collections.Counter(x["source"] for x in records)
 statuses=collections.Counter(x["status"] for x in records)
 lines=["# AnkHack: reproducible source coverage inventory","",
 "**CANDIDATE source files, not confirmed working hacks or completed integrations.**","",
 "## Public upstream snapshots"]
 for name,(repo,sha) in SOURCES.items():
  lines.append("- "+name+": "+repo+" at "+sha+"; candidates: "+str(c[name]))
 lines.extend(["","## Current honest status",
 "- Potential duplicates across source trees: "+str(len(duplicates)),
 "- External candidates not integrated: "+str(statuses["not_integrated"]),
 "- Partial source ports documented: "+str(statuses["partial_port"]),
 "- Meteor candidate classes present in source baseline: "+str(statuses["baseline_source"]),
 "- Baritone pathing candidates awaiting engine integration: "+str(statuses["engine_not_integrated"]),
 "","## Completion gates",
 "- [ ] Audit exact versions of all user-supplied clients and other clients not in public list.",
 "- [ ] Confirm which candidate files are real registered gameplay modules versus support classes.",
 "- [ ] Port unique modules, preserving functional settings and gameplay logic.",
 "- [ ] For duplicates, retain multiple actual algorithms under one AnkHack module.",
 "- [ ] Add configurable manual and evidence-based automatic variant selection per server/world.",
 "- [ ] Integrate genuine Baritone pathfinding within appropriate license constraints.",
 "- [ ] Test all modules in Minecraft plus Essential and crash/recovery behavior.",
 "- [ ] Improve the actual GUI, not merely palette colors.",
 "- [ ] Verify each client and module has been accounted for before calling release complete.",
 "","Do not ask the user to test intermediate builds. Compiler success is not gameplay validation."
 ])
 (out/"coverage.md").write_text("\n".join(lines)+"\n",encoding="utf8")
 print("Module candidates:",dict(c))
 print("Potential duplicate families:",len(duplicates))
 print("Status:",dict(statuses))
 if os.getenv("ANKHACK_REQUIRE_COMPLETE")=="1" and (statuses["not_integrated"] or statuses["engine_not_integrated"]):
  raise SystemExit("Release blocked: complete integration has not been verified")
if __name__=="__main__":main()
