#!/usr/bin/env python3
"""Bundle a replaceable LGPL Nether Pathfinder library at the first Fabric mod
dependency level; Fabric Loader does not recurse nested JAR dependencies.

Run after Gradle remapJar. No recompiled or modified Baritone/library bytecode.
"""
from pathlib import Path
from tempfile import NamedTemporaryFile
from zipfile import ZipFile, ZIP_DEFLATED
import json, os, io, hashlib

lib=Path("upstream/libs/nether-pathfinder-1.4.1.jar")
assert lib.is_file(), "Reproducible pathfinder extraction did not produce a jar"
lib_bytes=lib.read_bytes()
with ZipFile(io.BytesIO(lib_bytes)) as j:
    assert "fabric.mod.json" in j.namelist(), "Library not a Fabric dependency"
    assert "dev/babbaj/pathfinder/NetherPathfinder.class" in j.namelist(), "Missing actual pathfinder code"
    libmod=json.loads(j.read("fabric.mod.json"))
    assert libmod["id"]=="dev_babbaj_nether-pathfinder",libmod

jar_dir=Path("upstream/build/libs")
jars=[p for p in jar_dir.glob("AnkHack-Unified-*.jar") if not any(
    suffix in p.name for suffix in ("-sources","-dev","-javadoc"))]
assert jars, "No distribution JAR produced"
nested="META-INF/jars/nether-pathfinder-1.4.1.jar"
for jar in jars:
    with NamedTemporaryFile(dir=jar_dir,prefix="ankhack-",suffix=".jar",delete=False) as tmp:
        temp=Path(tmp.name)
    try:
        with ZipFile(jar,"r") as original,ZipFile(temp,"w") as output:
            assert nested not in original.namelist(), "Already bundled pathfinder"
            for member in original.infolist():
                data=original.read(member.filename)
                if member.filename=="fabric.mod.json":
                    mod=json.loads(data)
                    assert mod["id"]=="ankhack"
                    deps=mod.setdefault("jars",[])
                    if not any(d.get("file")==nested for d in deps):
                        deps.append({"file":nested})
                    data=(json.dumps(mod,indent=2,ensure_ascii=False)+"\n").encode()
                output.writestr(member,data)
            output.writestr(nested,lib_bytes,compress_type=ZIP_DEFLATED)
        with ZipFile(temp) as z:
            found=json.loads(z.read("fabric.mod.json"))
            assert any(d.get("file")==nested for d in found["jars"])
            with ZipFile(io.BytesIO(z.read(nested))) as inner:
                assert "dev/babbaj/pathfinder/NetherPathfinder.class" in inner.namelist()
        os.replace(temp,jar)
        print("Bundled first-level LGPL pathfinder:",jar,jar.stat().st_size)
    finally:
        temp.unlink(missing_ok=True)
print("Nether Pathfinder SHA-256:",hashlib.sha256(lib_bytes).hexdigest())
