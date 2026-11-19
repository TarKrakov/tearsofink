#!/usr/bin/env python3
"""Actions only: fresh V3 worlds, real Fabric dedicated server, no author saves."""
import argparse
import hashlib
import json
import os
import queue
import shutil
import signal
import subprocess
import threading
import time
from pathlib import Path

ROOT = Path(__file__).resolve().parents[3]
RUN = ROOT / "run-server"
OUT = ROOT / "out/d060"
DIM = ROOT / "src/main/resources/data/tears-of-ink/dimension/ink_lands.json"
JAVA = ROOT / "src/main/java/com/tarkrakov/tearsofink"
WORLD = RUN / "toi-d060-disposable"

def setup(seed):
    if os.environ.get("GITHUB_ACTIONS") != "true":
        raise RuntimeError("Run in an isolated GitHub Actions checkout only")
    if WORLD.exists():
        raise RuntimeError("Refusing to reuse any existing world")
    OUT.mkdir(parents=True, exist_ok=True)
    RUN.mkdir(exist_ok=True)
    # Keep an explicit V1 fixture even after the production default is V3.
    # All edits below affect only this disposable CI checkout.
    dimension = json.loads(DIM.read_bytes())
    dimension["generator"]["biome_source"].pop("temple_layout", None)
    original = json.dumps(dimension).encode("utf-8")
    dimension["generator"]["biome_source"]["temple_layout"] = 3
    DIM.write_text(json.dumps(dimension))
    for name, target in [
        ("D060Probe.java", JAVA / "world/D060Probe.java"),
        ("D060AditProbe.java", JAVA / "world/adit/D060AditProbe.java"),
    ]:
        if target.exists():
            raise RuntimeError("Refusing to overwrite production Java " + str(target))
        shutil.copyfile(ROOT / "tools/tests/d060" / name, target)
    init = JAVA / "TearsOfInk.java"
    source = init.read_text()
    anchor = "com.tarkrakov.tearsofink.world.TemplePaths.init();"
    if source.count(anchor) != 1:
        raise RuntimeError("Test initialization anchor moved")
    init.write_text(source.replace(anchor, anchor + "\n\t\tcom.tarkrakov.tearsofink.world.D060Probe.init();"))
    # Gradle runs on 25. The game process runs on 21.
    with (ROOT / "build.gradle").open("a") as f:
        f.write("\n tasks.matching { it.name == 'runServer' }.configureEach {\n"
                "    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(21) }\n}\n")
    # Don't expose the server or accept any connections from outside the runner.
    (RUN / "server.properties").write_text(
        "level-name=toi-d060-disposable\nlevel-seed=" + seed +
        "\nserver-ip=127.0.0.1\nserver-port=25565\nonline-mode=false\n"
        "view-distance=3\nsimulation-distance=3\nmax-tick-time=-1\n"
        "generate-structures=true\nspawn-protection=0\n"
        "initial-enabled-packs=vanilla,fabric,fabric-convention-tags-v2,tears-of-ink,fabric-gametest-api-v1\n")
    if (RUN / "eula.txt").read_text().strip() != "eula=true":
        raise RuntimeError("Existing project server EULA acceptance not found")
    return original

class Server:
    def __init__(self, label):
        self.label = label
        self.lines = queue.Queue()
        self.p = subprocess.Popen(
            ["bash", "./gradlew", "runServer", "--console=plain", "--no-daemon",
             "--max-workers=2", "--no-configuration-cache"],
            cwd=ROOT, stdin=subprocess.PIPE, stdout=subprocess.PIPE,
            stderr=subprocess.STDOUT, text=True, encoding="utf-8", errors="replace",
            bufsize=1, start_new_session=True,
        )
        self.reader = threading.Thread(target=self.read, daemon=True)
        self.reader.start()

    def read(self):
        with (OUT / (self.label + ".log")).open("w", encoding="utf-8") as log:
            for line in self.p.stdout:
                log.write(line)
                log.flush()
                print(line, end="", flush=True)
                self.lines.put(line)
        self.lines.put(None)

    def expect(self, marker, seconds):
        deadline = time.monotonic()+seconds
        while time.monotonic() < deadline:
            try:
                line = self.lines.get(timeout=min(2, max(.01, deadline-time.monotonic())))
            except queue.Empty:
                if self.p.poll() is not None:
                    raise RuntimeError(f"{self.label}: process exited before {marker}")
                continue
            if line is None:
                raise RuntimeError(f"{self.label}: server stopped before {marker}")
            if marker in line:
                return line.split(marker,1)[1].strip()
        raise TimeoutError(f"{self.label}: waiting for {marker}")

    def send(self, command):
        self.p.stdin.write(command+"\n")
        self.p.stdin.flush()

    def close(self, normal):
        if self.p.poll() is None:
            try:
                self.send("stop")
                code = self.p.wait(timeout=120)
            except (subprocess.TimeoutExpired, BrokenPipeError):
                os.killpg(self.p.pid, signal.SIGTERM)
                try:
                    self.p.wait(timeout=15)
                except subprocess.TimeoutExpired:
                    os.killpg(self.p.pid, signal.SIGKILL)
                    self.p.wait()
                if normal:
                    raise RuntimeError("Server failed to shut down normally")
            else:
                if normal and code != 0:
                    raise RuntimeError(f"Gradle/server returned {code}")
        elif normal and self.p.returncode:
            raise RuntimeError(f"Gradle/server returned {self.p.returncode}")
        self.reader.join(timeout=10)

def session(label, physical):
    server = Server(label)
    normal = False
    try:
        server.expect('For help, type "help"', 600)
        arrival = None
        if physical:
            server.send("toi-d060-probe arrival")
            arrival = json.loads(server.expect("D060_ARRIVAL ", 270))
        server.send("toi-d060-probe " + ("physical" if physical else "plan"))
        result = json.loads(server.expect("D060_RESULT ", 480))
        if arrival is not None:
            result["arrival"] = arrival
            if not arrival["ready"]:
                result["failures"].append("Arrival timed out")
                result["pass"] = False
        if not str(result.get("java", "")).startswith("21"):
            result["failures"].append("Server did not use Java 21")
            result["pass"] = False
        (OUT / (label+".json")).write_text(json.dumps(result, indent=2))
        normal = True
        return result
    finally:
        server.close(normal)

def digest(path):
    return hashlib.sha256(path.read_bytes()).hexdigest()

def main():
    parser = argparse.ArgumentParser()
    parser.add_argument("--seed", required=True)
    args = parser.parse_args()
    seed = str(int(args.seed))
    original = setup(seed)
    physical = seed in ("777", "42")
    first = session("first", physical)
    results = {"first": first}
    if physical:
        # Restart using the explicit legacy V1 fixture. The saved
        # marker must preserve V3 and the authoritative plan must be read, not recomputed.
        plan_file = WORLD / "data/tearsofink_temple_network_v3.bin"
        before = digest(plan_file)
        DIM.write_bytes(original)
        second = session("restart", True)
        results["restart"] = second
        after = digest(plan_file)
        restored_log = (OUT / "restart.log").read_text(encoding="utf-8")
        identical = before == after and first.get("sites") == second.get("sites") \
            and first.get("adits",{}).get("sites") == second.get("adits",{}).get("sites")
        restored = "D-060: restored " in restored_log and "D-060: planned " not in restored_log
        results["restart_check"] = {
            "identical": identical, "restored": restored, "sha256": after,
            "same_arrival": first.get("arrival", {}).get("position") == second.get("arrival", {}).get("position"),
        }
    (OUT / "result.json").write_text(json.dumps(results,indent=2))
    print("D060_SUMMARY " + json.dumps(results),flush=True)
    success = first.get("pass",False) and first.get("seed")==seed
    if physical:
        success = success and second.get("pass",False) and all(
            results["restart_check"][k] for k in ("identical","restored","same_arrival"))
    if not success:
        raise SystemExit("D060 acceptance checks failed; see D060_SUMMARY and artifacts")

if __name__ == "__main__":
    main()
