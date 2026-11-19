#!/usr/bin/env python3
"""Install a test fixture ONLY inside a clean disposable Actions checkout."""
import os
from pathlib import Path
import shutil

if os.environ.get("GITHUB_ACTIONS") != "true":
    raise SystemExit("This fixture must not be installed in an author's working copy")
root = Path(__file__).resolve().parents[3]
src = root / "src/main/java/com/tarkrakov/tearsofink"
dst = src / "travel/D060RespawnProbe.java"
if dst.exists():
    raise SystemExit("Test fixture already installed")
edits = [
    (src / "TearsOfInk.java",
     "com.tarkrakov.tearsofink.travel.Travel.init();",
     "com.tarkrakov.tearsofink.travel.Travel.init();\n\t\tcom.tarkrakov.tearsofink.travel.D060RespawnProbe.init();"),
    (src / "world/ModDimensions.java",
     "public static boolean chunksReady(ServerLevel level, BlockPos center, int radius) {",
     "public static boolean chunksReady(ServerLevel level, BlockPos center, int radius) {\n"
     "\t\tif (com.tarkrakov.tearsofink.travel.D060RespawnProbe.hold(level, center, radius)) return false;"),
]
for path, old, new in edits:
    source = path.read_text()
    if source.count(old) != 1:
        raise SystemExit("Fixture anchor changed: " + str(path))
shutil.copyfile(Path(__file__).with_name("D060RespawnProbe.java"), dst)
for path, old, new in edits:
    path.write_text(path.read_text().replace(old, new))
