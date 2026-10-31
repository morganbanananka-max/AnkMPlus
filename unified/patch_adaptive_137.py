"""Use tested, evidence-gated variant selection in existing server profiles.

The UI still owns manual choice and each module still measures its own actual
observations. This replaces the old 'one positive sample wins' policy with
a family-scoped decision. No new hack engine or copied client JARs.
"""
from pathlib import Path
import shutil

root=Path("upstream/src/main/java/meteordevelopment/meteorclient")
profile=root/"AnkVariantProfiles.java"
source=profile.read_text(encoding="utf-8")
before='''    public static String choose(String server,String module){
        // No evidence -> safest expected creative-like choice. Not a fabricated benchmark.
        String[] available=variants(module);
        if(available.length==0)return "";
        String choice=available[0];int max=Integer.MIN_VALUE;
        for(String variant:available) {
            if(!hasEvidence(server,module,variant))continue;
            int score=score(server,module,variant);
            if(score>max){choice=variant;max=score;}
        }
        return choice;
    }'''
after='''    public static String choose(String server,String module){
        String[] available=variants(module);
        if(available.length==0)return "";
        if(server==null || "unknown".equals(server))return available[0];
        Map<String,AnkVariantDecision.Evidence> evidence=new HashMap<>();
        for(String variant:available) {
            evidence.put(variant,new AnkVariantDecision.Evidence(
                get(key(server,module,variant,"stable")),
                get(key(server,module,variant,"correction")),
                get(key(server,module,variant,"motionSamples")),
                get(key(server,module,variant,"motion"))
            ));
        }
        return AnkVariantDecision.choose(available,evidence);
    }'''
if source.count(before)!=1:
    raise RuntimeError("Variant choose() source changed; do not patch blindly")
source=source.replace(before,after,1)
profile.write_text(source,encoding="utf-8")
sourcefile=Path("unified/native/AnkVariantDecision.java")
code=sourcefile.read_text(encoding="utf-8")
if "public final class AnkVariantDecision" not in code:
    raise RuntimeError("Source module absent")
shutil.copy2(sourcefile,root/"AnkVariantDecision.java")
assert source.count("AnkVariantDecision.choose(")==1
assert "if(available.length==0)return" in source
print("Integrated evidence-gated Auto variant decision, per server and module.")
