import argparse
import json
from pathlib import Path
import subprocess
import numpy as np
from scipy import signal

parser = argparse.ArgumentParser(description='Render D-058 auditions from the author’s WAV.')
parser.add_argument('source', type=Path)
parser.add_argument('output', type=Path)
args = parser.parse_args()
source = args.source
args.output.mkdir(parents=True, exist_ok=True)
rate = 44100
ff = ['ffmpeg', '-hide_banner', '-loglevel', 'error']

def decode(filters):
    data = subprocess.check_output(ff + ['-i', str(source), '-af', filters,
                                        '-ar', str(rate), '-ac', '1', '-f', 'f32le', '-'])
    return np.frombuffer(data, '<f4').astype(float)

original = decode('anull')
if len(original) != 16 * rate:
    parser.error("Expected the 16-second reference WAV.")
time = np.arange(len(original)) / rate

def octave(ratio):
    shifted = decode('apad=pad_dur=0.3,rubberband='
                     f'pitch={ratio}:formant=preserved:transients=smooth:pitchq=quality')
    return np.pad(shifted, (0, max(0, len(original)-len(shifted))))[:len(original)]

lower, upper = octave(.5), octave(2)

def band(samples, low, high):
    sos = signal.butter(2, [low, high], 'bandpass', fs=rate, output='sos')
    return signal.sosfiltfilt(sos, samples)

def rms(samples):
    return np.sqrt(np.mean(samples**2))

def match(samples, reference):
    return samples * rms(reference) / rms(samples)

def voice(register, anchors, delay, octave_mix, width):
    if register == 'low':
        body = band(original, 110, 900)
        octave_body = match(band(lower, 105, 700), body)
        phase = .8
    else:
        body = band(original, 240, 3600)
        octave_body = match(band(upper, 300, 2300), body)
        phase = 2.4
    swell = np.interp(time, [0, 2.2, 4.6, 6.7, 9.2, 11.5, 14, 16], anchors)
    octave_amount = octave_mix * (.52 + .48 * swell)
    texture = (1-octave_amount)*body + octave_amount*octave_body
    displacement = delay + width * np.sin(2*np.pi*.19*time+phase)
    displaced = np.interp(time-displacement, time, texture, left=0, right=0)
    return displaced * swell

def room(samples, pan, wet, decay, seed):
    rng = np.random.default_rng(seed)
    tail = int(rate * decay)
    result = np.zeros((len(samples) + tail - 1, 2))
    result[:len(samples), :] = samples[:, None] * np.sqrt([1-pan, 1+pan])
    for channel in range(2):
        impulse = np.zeros(tail)
        predelay = int(rate*(.026 + .008*channel))
        for reflection, gain in [(.018,.21),(.041,-.15),(.068,.13),(.103,.09)]:
            offset = predelay+int(rate*(reflection+rng.uniform(-.005,.005)))
            impulse[offset] = gain
        length = tail-predelay
        seconds = np.arange(length)/rate
        diffuse = band(rng.normal(size=length), 135, 3600 if seed%2 else 2700)
        diffuse *= (1-np.exp(-seconds/.042))*10**(-3*seconds/(decay-.04))
        impulse[predelay:] += diffuse * (.82/np.linalg.norm(diffuse))
        result[:, channel] += wet * signal.fftconvolve(samples, impulse)
    return result

def encode(path, audio, channels, gain_db):
    subprocess.run(ff + ['-y', '-f', 'f32le', '-ar', str(rate), '-ac', str(channels),
                         '-i', '-', '-af', f'volume={gain_db:.4f}dB',
                         '-c:a', 'libvorbis', '-q:a', '7', str(path)],
                   input=audio.astype('<f4').tobytes(), check=True)

def save(path, audio, channels):
    report = subprocess.run(
        ['ffmpeg', '-hide_banner', '-nostats', '-f', 'f32le', '-ar', str(rate),
         '-ac', str(channels), '-i', '-', '-af', 'loudnorm=print_format=json',
         '-f', 'null', '-'],
        input=audio.astype('<f4').tobytes(), capture_output=True, check=True,
    ).stderr.decode()
    levels = json.JSONDecoder().raw_decode(report[report.rindex('{'):])[0]
    gain = min(-20.5-float(levels['input_i']), -6.3-float(levels['input_tp']))
    encode(path, audio, channels, gain)

variants = [
    ('01_Close_Prayer', 1.28, .10, -.19, .22, .25, .22, .10, .09,
     [.12,.44,.89,.68,.84,.78,.48,0], [.07,.25,.38,.69,.8,.55,.71,0]),
    ('02_Echoes_of_Grief', 2.8, .23, -.32, .36, .47, .22, .53, .15,
     [.05,.55,.84,.93,.88,.92,.68,0], [.03,.14,.42,.55,.82,.74,.5,0]),
    ('03_Light_in_Exile', 3.6, .25, -.39, .43, .29, .43, .21, .48,
     [.05,.40,.64,.76,.66,.8,.49,0], [.04,.17,.68,.67,.94,.81,.92,0]),
]

for index, variant in enumerate(variants):
    name, decay, wet, pan_low, pan_high, gain_low, gain_high, oct_low, oct_high, low_shape, high_shape = variant
    low = voice('low', low_shape, .054+.014*index, oct_low, .007+.004*index)
    high = voice('high', high_shape, .085+.024*index, oct_high, .010+.004*index)
    low = match(low, original)*gain_low
    high = match(high, original)*gain_high
    stems = [
        room(original, 0, wet*.72, decay, 100+index),
        room(low, pan_low, wet*1.15, decay, 200+index),
        room(high, pan_high, wet*1.35, decay, 300+index),
    ]
    choir = sum(stems)
    save(args.output / f'{name}_Choir.ogg', choir, 2)
    fade = np.cos(np.linspace(0, np.pi / 2, int(rate * .45)))**2
    for label, stem in zip(('Voice_1_Lead', 'Voice_2_Low', 'Voice_3_High'), stems):
        mono = stem[:len(original)].mean(axis=1)
        mono[-len(fade):] *= fade
        save(args.output / f'{name}_{label}.ogg', mono, 1)
