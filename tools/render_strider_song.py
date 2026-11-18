"""Render the original D-058 wordless miniature. Requires NumPy and ffmpeg."""
from pathlib import Path
import subprocess

import numpy as np

RATE = 48000
DEST = Path(__file__).resolve().parents[1] / "src/main/resources/assets/tears-of-ink/sounds"
CHORDS = [(0, 3.8, (38, 45, 53)), (3.2, 3.6, (41, 48, 57)),
          (6.2, 3.3, (43, 50, 58)), (9, 3.4, (46, 53, 62)),
          (11.9, 2.9, (38, 45, 53))]
MELODY = [(1.1, 1.65, 65), (2.6, 1.2, 64), (3.65, 1.7, 69),
          (5.15, 1.35, 67), (6.35, 1.8, 65), (8.0, 1.05, 69),
          (8.95, 2.0, 72), (10.75, 1.3, 69), (11.9, 2.65, 65)]


def render(voice):
    rng = np.random.default_rng(87 + voice)
    dry = np.zeros(16 * RATE)

    def sing(start, duration, note, gain, high=False):
        t = np.arange(int(duration * RATE)) / RATE
        envelope = np.sin(np.pi * t / duration) ** 1.35
        base = 440 * 2 ** ((note - 69) / 12)
        vibrato = 1 + 0.0035 * np.sin(2 * np.pi * (4.3 + voice * .2) * t)
        scoop = 1 - .035 * np.exp(-t * 12) + .007 * np.sin(np.pi * t / duration)
        phase = 2 * np.pi * np.cumsum(base * scoop * vibrato) / RATE
        vowel = 340 + 200 * np.sin(np.pi * t / duration) ** 2
        wave = .63 * np.sin(phase)
        for harmonic in range(2, 19):
            f = harmonic * base
            formant = np.exp(-((f - vowel) / 220) ** 2)
            formant += .24 * np.exp(-((f - 920) / 310) ** 2)
            wave += formant * np.sin(harmonic * phase + rng.uniform(-np.pi, np.pi)) / harmonic
        noise = rng.normal(size=t.size)
        breath = np.convolve(noise, np.ones(13) / 13, mode="same")
        signal = gain * envelope * (wave + (.1 if high else .055) * breath)
        i = int(start * RATE)
        dry[i:i + t.size] += signal

    for start, duration, chord in CHORDS:
        for index, note in enumerate(chord):
            sing(start + index * .11, duration - index * .11, note,
                 (.29, .19, .14)[index] * (1 if index == voice else .65))
    for i, (start, duration, note) in enumerate(MELODY):
        if voice == 1:
            note = CHORDS[min(4, int(start / 3))][1] + 12
        if voice == 2:
            note -= 12
        sing(start + voice * .07, duration, note, (.21 + .018 * i) * (1 if voice == 0 else .73), True)
    wet = dry.copy()
    for delay, gain in ((.067, .13), (.139, .17), (.228, .15), (.353, .13),
                        (.491, .10), (.683, .072), (.913, .045), (1.21, .027)):
        d = int(delay * RATE)
        wet[d:] += gain * dry[:-d]
    wet[-RATE:] *= np.linspace(1, 0, RATE) ** 2
    name = "pale_strider_song" + (f"_{voice + 1}" if voice else "")
    subprocess.run(["ffmpeg", "-hide_banner", "-loglevel", "error", "-y", "-f", "f32le",
                    "-ar", str(RATE), "-ac", "1", "-i", "-", "-af",
                    "loudnorm=I=-21:TP=-6.5:LRA=10", "-ar", str(RATE), "-c:a", "libvorbis",
                    "-q:a", "6", str(DEST / (name + ".ogg"))],
                   input=wet.astype("<f4").tobytes(), check=True)


if __name__ == "__main__":
    for part in range(3):
        render(part)
