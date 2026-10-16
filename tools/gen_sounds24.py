"""2.4 sounds: Yan's voice (an old man's mumble, a cough, the clink of his lantern), music for the Underside and for
boss fights, and the stings of night events and onslaughts. Run from the repo root: python3 tools/gen_sounds24.py
Procedural and deterministic (fixed seeds); needs numpy, scipy and ffmpeg with libvorbis."""
import os
import subprocess
import tempfile
import sys

import numpy as np
from scipy.io import wavfile

sys.path.insert(0, os.path.dirname(__file__))
import gen_sounds as g  # noqa: E402

SR = g.SR
ROOT = "src/main/resources/assets/umbra/sounds"


def save(folder, name, x, peak=0.8, quality=3):
    x = np.asarray(x, dtype=np.float64)
    x = x / (np.max(np.abs(x)) + 1e-9) * peak
    os.makedirs(os.path.join(ROOT, folder), exist_ok=True)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as f:
        wavfile.write(f.name, SR, (x * 32767).astype(np.int16))
        tmp = f.name
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", tmp, "-c:a", "libvorbis", "-q:a", str(quality),
                    os.path.join(ROOT, folder, name + ".ogg")], check=True)
    os.remove(tmp)


def env_adsr(n, a, r):
    e = np.ones(n)
    a, r = int(a * SR), int(r * SR)
    if a: e[:a] = np.linspace(0, 1, a)
    if r: e[-r:] *= np.linspace(1, 0, r)
    return e


FORMANTS = {"m": (250, 1700, 2300), "o": (450, 800, 2600), "a": (700, 1100, 2500), "e": (420, 1900, 2500), "u": (320, 700, 2300)}


def voiced(seconds, f0, seed, jitter=0.03):
    """A rough old voice: a buzzing source with jitter and a slow tremble."""
    rnd = np.random.default_rng(seed)
    tt = g.t(seconds)
    f = f0 * (1 + 0.04 * np.sin(2 * np.pi * 5.2 * tt)) * (1 + jitter * np.cumsum(rnd.standard_normal(len(tt))) / np.sqrt(len(tt)))
    ph = 2 * np.pi * np.cumsum(f) / SR
    src = np.sign(np.sin(ph)) * 0.4 + np.sin(ph) * 0.6 + 0.05 * rnd.standard_normal(len(tt))
    return src


def vowel(src, v):
    f1, f2, f3 = FORMANTS[v]
    return g.bp(src, f1 * 0.8, f1 * 1.25) * 1.0 + g.bp(src, f2 * 0.85, f2 * 1.15) * 0.45 + g.bp(src, f3 * 0.9, f3 * 1.1) * 0.15


def mumble(seed, syllables):
    rnd = np.random.default_rng(seed)
    out = []
    for i, v in enumerate(syllables):
        d = 0.18 + rnd.random() * 0.22
        src = voiced(d, 96 + rnd.random() * 18 - i * 3, seed + i)
        s = vowel(src, v) * env_adsr(len(src), 0.03, 0.08)
        out.append(s)
        out.append(np.zeros(int(SR * (0.03 + rnd.random() * 0.08))))
    x = np.concatenate(out)
    return g.reverb(g.lp(x, 3200), 0.6, 0.15, seed)


def cough(seed):
    rnd = np.random.default_rng(seed)
    parts = []
    for k in range(2):
        n = int(SR * (0.16 + 0.05 * k))
        burst = rnd.standard_normal(n) * np.exp(-np.linspace(0, 6, n))
        burst = g.bp(burst, 250, 2200) + 0.3 * vowel(voiced(n / SR, 120, seed + k), "a") * np.exp(-np.linspace(0, 5, n))
        parts += [burst, np.zeros(int(SR * 0.12))]
    return g.reverb(np.concatenate(parts), 0.5, 0.12, seed)


def clink(seed):
    rnd = np.random.default_rng(seed)
    tt = g.t(1.2)
    x = sum(np.sin(2 * np.pi * f * tt) * np.exp(-tt * d) * a for f, d, a in
            ((1870 + rnd.random() * 40, 6, 1.0), (2960, 9, 0.6), (4410, 12, 0.4), (980, 4, 0.3)))
    return x * env_adsr(len(tt), 0.002, 0.2)


def drone_music(seed, seconds=64):
    """The Underside: a deep drone that breathes, cave wind, now and then a far bell."""
    rnd = np.random.default_rng(seed)
    tt = g.t(seconds)
    swell = 0.6 + 0.4 * np.sin(2 * np.pi * tt / 16 - 1.5)
    x = (np.sin(2 * np.pi * 55 * tt) + 0.7 * np.sin(2 * np.pi * 55.4 * tt) + 0.5 * np.sin(2 * np.pi * 82.5 * tt)
         + 0.25 * np.sin(2 * np.pi * 110.6 * tt)) * swell
    wind = g.lp(rnd.standard_normal(len(tt)), 600) * (0.4 + 0.3 * np.sin(2 * np.pi * tt / 11))
    x = x * 0.6 + wind * 0.5
    for start in (9.0, 27.5, 46.0):
        b = g.bell(seed + int(start), 98.0 * (1 + rnd.integers(0, 3) / 4))
        i = int(start * SR)
        x[i:i + len(b)] += b[:max(0, len(x) - i)] * 0.6
    return g.reverb(x * env_adsr(len(x), 4.0, 6.0), 2.4, 0.35, seed)


def boss_music(seed, seconds=48):
    """A fight: a heavy pulse at 100 beats a minute, a sawing dissonant pad that climbs, a heartbeat under it."""
    rnd = np.random.default_rng(seed)
    tt = g.t(seconds)
    beat = 60 / 100
    x = np.zeros(len(tt))
    for k in range(int(seconds / beat)):
        i = int(k * beat * SR)
        n = int(0.35 * SR)
        hit = np.sin(2 * np.pi * (60 * np.exp(-np.linspace(0, 6, n))) * np.arange(n) / SR * 3) * np.exp(-np.linspace(0, 7, n))
        if k % 4 == 3:
            hit += g.bp(rnd.standard_normal(n), 800, 4000) * np.exp(-np.linspace(0, 12, n)) * 0.5
        x[i:i + n] += hit[:max(0, len(x) - i)] * (1.0 if k % 2 == 0 else 0.6)
    climb = 1 + 0.12 * (tt / seconds)
    pad = sum(np.sign(np.sin(2 * np.pi * f * climb * tt)) * a for f, a in ((73.4, 0.5), (77.8, 0.4), (110.0, 0.3), (155.6, 0.2)))
    pad = g.lp(pad, 900) * (0.5 + 0.5 * np.abs(np.sin(2 * np.pi * tt * 3.3)))
    x = x + pad * 0.35
    return g.reverb(x * env_adsr(len(x), 1.5, 2.5), 1.4, 0.25, seed)


def horn(seed):
    tt = g.t(3.2)
    f = 73.4 * (1 + 0.01 * np.sin(2 * np.pi * 4.5 * tt))
    ph = 2 * np.pi * np.cumsum(f) / SR
    x = sum(np.sin(ph * k) / k for k in range(1, 9))
    x = g.lp(x, 1200) * env_adsr(len(tt), 0.5, 1.0)
    return g.reverb(x, 2.5, 0.45, seed)


def blood_moon(seed):
    rnd = np.random.default_rng(seed)
    tt = g.t(5.0)
    rumble = g.lp(rnd.standard_normal(len(tt)), 120) * 2.0
    choir = sum(vowel(voiced(5.0, f, seed + i, 0.01), "o") for i, f in enumerate((110, 130.8, 164.8)))
    return g.reverb((rumble + choir * 0.6) * env_adsr(len(tt), 1.5, 2.0), 2.8, 0.4, seed)


def silence(seed):
    tt = g.t(6.0)
    x = np.sin(2 * np.pi * 7300 * tt) * 0.3 + np.sin(2 * np.pi * 7330 * tt) * 0.2
    return x * env_adsr(len(tt), 2.0, 3.0)


if __name__ == "__main__":
    save("voice", "yan_mumble1", mumble(200, "moa"))
    save("voice", "yan_mumble2", mumble(201, "mumo"))
    save("voice", "yan_mumble3", mumble(202, "ememu"))
    save("voice", "yan_cough", cough(210))
    save("voice", "yan_hurt", mumble(220, "ae"))
    save("voice", "lantern_clink", clink(230))
    save("music", "underside", drone_music(300), 0.7, 2)
    save("music", "boss", boss_music(310), 0.75, 2)
    save("event", "onslaught_horn", horn(400))
    save("event", "blood_moon", blood_moon(410))
    save("event", "silence", silence(420), 0.4)
    print("2.4 sounds written")
