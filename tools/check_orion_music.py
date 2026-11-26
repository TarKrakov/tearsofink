"""Verify the approved V2 OGG bytes in the actual packaged game JAR."""
from hashlib import sha256
from pathlib import Path
from zipfile import ZipFile

EXPECTED = {
    "orion_phase1_intro.ogg": "28854f6c257056ae2422d03fc1d53137d580c2a0aac5b490d26a1576a3db1cd2",
    "orion_phase1_loop.ogg": "0e2cb006839495b3485d2adfffdb4b78f01721330063c95b0290b63060f9d38f",
    "orion_phase2_intro.ogg": "a1e7a3af8ca5d37daf3a283d874f1d8b6a4541a6864e5ed92abc62089ce6ce0d",
    "orion_phase2_loop.ogg": "6a38d1e4496c608ad1174376a8e3e2e516aa2edc97e1b520921f63a9889acfbb",
}
PREFIX = "assets/tears-of-ink/sounds/"
jars = [p for p in Path("build/libs").glob("*.jar")
        if not p.stem.endswith(("-sources", "-dev", "-javadoc"))]
if not jars:
    raise SystemExit("No game JAR in build/libs")
for jar in sorted(jars):
    with ZipFile(jar) as archive:
        for name, expected in EXPECTED.items():
            path = PREFIX + name
            if archive.namelist().count(path) != 1:
                raise SystemExit(f"{jar}: missing or duplicate {path}")
            actual = sha256(archive.read(path)).hexdigest()
            if actual != expected:
                raise SystemExit(f"{jar}: {name}: expected {expected}, got {actual}")
            print(f"{jar.name}: {name}: SHA-256 OK")
