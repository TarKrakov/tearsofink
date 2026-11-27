"""Keep directional troll sound delivery, attenuation and mixer wiring in sync."""
import json
import re
from pathlib import Path

assets = Path("src/main/resources/assets/tears-of-ink")
sounds = json.loads((assets / "sounds.json").read_text())
server = Path("src/main/java/com/tarkrakov/tearsofink/sound/ModSounds.java").read_text()
mixer = Path("src/client/java/com/tarkrakov/tearsofink/client/audio/AudioMixer.java").read_text()
fixed_ranges = {
    name: int(radius) for name, radius in re.findall(r'register\("(troll_[^"]+)", (\d+)\)', server)
}
assert len(fixed_ranges) == 9
assert all(fixed_ranges["troll_" + name] >= 48 for name in ("snort", "snore", "chomp", "laugh"))
assert fixed_ranges["troll_step"] < fixed_ranges["troll_windup"] < fixed_ranges["troll_snort"]
translations = [json.loads((assets / "lang" / language).read_text()) for language in ("ru_ru.json", "en_us.json")]

for event, distance in fixed_ranges.items():
    sound = sounds[event]
    assert sound["sounds"] == [{"name": "tears-of-ink:" + event, "attenuation_distance": distance}]
    assert all(sound["subtitle"] in language for language in translations)
    assert f'id("{event}"), Group.TROLLS, SoundSource.HOSTILE,' in mixer
    ogg = (assets / "sounds" / (event + ".ogg")).read_bytes()
    header = ogg.index(b"\x01vorbis")
    assert ogg.startswith(b"OggS") and ogg[header + 11] == 1, f"{event} must be mono for positional audio"
print(f"Troll audio: {len(fixed_ranges)} positioned mono sounds with matched server and client ranges")
