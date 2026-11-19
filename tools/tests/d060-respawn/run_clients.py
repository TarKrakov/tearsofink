#!/usr/bin/env python3
"""Two actual Minecraft clients on Xvfb; server-only controlled death request."""
import json
import os
import runpy
import signal
import subprocess
import sys
import time
from pathlib import Path

HERE = Path(__file__).resolve().parent
sys.path.insert(0, str(HERE.parent / "d060"))
import run as d060

def stop_client(p):
    if p.poll() is None:
        os.killpg(p.pid, signal.SIGTERM)
        try:
            p.wait(timeout=20)
        except subprocess.TimeoutExpired:
            os.killpg(p.pid, signal.SIGKILL)
            p.wait()

def launch(name, config):
    home = d060.ROOT / ("run-d060-" + name)
    home.mkdir(exist_ok=False)
    (home / "options.txt").write_text("renderDistance:2\nsimulationDistance:3\nmaxFps:15\npauseOnLostFocus:false\nonboardAccessibility:false\n")
    # Keep Loom's launch arguments and assets, replace only identity/location.
    args = list(config["args"])
    for key in ("--username","--uuid","--accessToken","--userType","--gameDir","--quickPlayMultiplayer","--width","--height"):
        while key in args:
            i = args.index(key)
            del args[i:i+2]
    args += ["--username",name,"--accessToken","0","--userType","legacy",
             "--gameDir",str(home),"--quickPlayMultiplayer","127.0.0.1:25565",
             "--width","640","--height","360"]
    # Loom's AbstractRunTask injects @.../argFiles/runClient before writing it
    # in exec(). We deliberately stop before exec(); getClasspath() nevertheless
    # returns the real, filtered runtime classpath, so supply it directly.
    jvm = [v for v in config["jvm"] if not v.startswith(("-Xmx","-Xms"))
           and not (v.startswith("@") and Path(v[1:]).name=="runClient"
                    and Path(v[1:]).parent.name=="argFiles")]
    if not config["classpath"] or not config["main"]:
        raise RuntimeError("Incomplete Loom client launch configuration")
    cmd = ["xvfb-run","-a",config["java"],"-Xmx1200m","-Xms256m"] + jvm + \
        ["-cp",config["classpath"],config["main"]] + args
    with (d060.OUT / (name+".log")).open("w") as log:
        return subprocess.Popen(cmd,cwd=home,stdout=log,stderr=subprocess.STDOUT,
                                start_new_session=True,env={**os.environ,"LIBGL_ALWAYS_SOFTWARE":"1"})

def main():
    d060.setup("42")
    runpy.run_path(str(HERE / "install.py"))
    manifest = d060.OUT / "client-launch.json"
    # Reuse the actual Loom JavaExec launcher, rather than reconstructing MC libraries.
    with (d060.ROOT / "build.gradle").open("a") as f:
        f.write("""
tasks.matching { it.name == 'runClient' }.configureEach {
    javaLauncher = javaToolchains.launcherFor { languageVersion = JavaLanguageVersion.of(21) }
    doFirst {
        def out = file('out/d060/client-launch.json')
        out.parentFile.mkdirs()
        out.text = groovy.json.JsonOutput.toJson([
            java: javaLauncher.get().executablePath.asFile.absolutePath,
            jvm: allJvmArgs, classpath: classpath.asPath, main: mainClass.get(),
            args: (args ?: []) + argumentProviders.collectMany { it.asArguments().toList() },
            cwd: workingDir.absolutePath
        ])
        throw new org.gradle.api.tasks.StopExecutionException('D060 recorded client launch; no GUI in Gradle')
    }
}
""")
    subprocess.run(["bash","./gradlew","runClient","--console=plain","--no-daemon",
                    "--max-workers=2","--no-configuration-cache"],cwd=d060.ROOT,check=True)
    config = json.loads(manifest.read_text())
    server = d060.Server("respawn-server")
    clients = []
    normal = False
    try:
        server.expect('For help, type "help"',600)
        # Warm the original arrival before joining; no artificial location is created.
        server.send("toi-d060-probe arrival")
        arrival = json.loads(server.expect("D060_ARRIVAL ",270))
        if not arrival.get("ready"):
            raise RuntimeError("Starter arrival not ready")
        for name in ("TOIRespawnA","TOIRespawnB"):
            clients.append(launch(name,config))
        # Consume the join messages in any arrival order.
        joined = set()
        deadline = time.monotonic()+240
        while len(joined)<2 and time.monotonic()<deadline:
            try:
                line = server.lines.get(timeout=2)
            except d060.queue.Empty:
                if any(p.poll() is not None for p in clients):
                    raise RuntimeError("Client exited before both joined; see client logs")
                continue
            if line is None:
                raise RuntimeError("Server stopped")
            for name in ("TOIRespawnA","TOIRespawnB"):
                if name+" joined the game" in line:
                    joined.add(name)
        if len(joined)!=2:
            raise TimeoutError("Two real clients did not join")
        server.send("gamerule doImmediateRespawn false")
        server.send("gamerule doMobSpawning false")
        server.send("toi-d060-respawn TOIRespawnA TOIRespawnB")
        result = json.loads(server.expect("D060_RESPAWN_RESULT ",360))
        (d060.OUT/"respawn-result.json").write_text(json.dumps(result,indent=2))
        print("D060_RESPAWN_SUMMARY "+json.dumps(result),flush=True)
        normal = True
        if not all(result.get(k) for k in ("pass","held_a","released","a_restored","b_restored")):
            raise RuntimeError("Delayed respawn regression failed")
    finally:
        for p in clients:
            stop_client(p)
        for file in sorted(d060.OUT.glob("TOIRespawn*.log")):
            print("===== "+file.name+" =====",flush=True)
            print("\n".join(file.read_text(errors="replace").splitlines()[-100:]),flush=True)
        server.close(normal)

if __name__ == "__main__":
    main()
