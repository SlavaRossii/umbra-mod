"""Procedural ambience of the Underside. Run from the repo root: python3 tools/gen_sounds.py
Writes mono OGG files to assets/umbra/sounds/ambient/ (needs numpy, scipy and ffmpeg with libvorbis).
Deterministic: fixed seeds."""
import os
import subprocess
import tempfile

import numpy as np
from scipy import signal
from scipy.io import wavfile

SR = 22050
OUT = "src/main/resources/assets/umbra/sounds/ambient"


def t(seconds):
    return np.arange(int(SR * seconds)) / SR


def lp(x, hz, order=4):
    b, a = signal.butter(order, hz / (SR / 2), "low")
    return signal.lfilter(b, a, x)


def hp(x, hz, order=2):
    b, a = signal.butter(order, hz / (SR / 2), "high")
    return signal.lfilter(b, a, x)


def bp(x, lo, hi, order=2):
    b, a = signal.butter(order, [lo / (SR / 2), hi / (SR / 2)], "band")
    return signal.lfilter(b, a, x)


def reverb(x, seconds=1.6, wet=0.35, seed=0, tone=3000):
    rnd = np.random.default_rng(seed)
    n = int(SR * seconds)
    ir = rnd.standard_normal(n) * np.exp(-np.linspace(0, 7, n))
    ir = lp(ir, tone)
    ir /= np.sqrt(np.sum(ir ** 2))
    y = signal.fftconvolve(np.concatenate([x, np.zeros(n)]), ir)[: len(x) + n]
    dry = np.concatenate([x, np.zeros(n)])
    return dry * (1 - wet) + y * wet


def env(n, attack, release):
    e = np.ones(n)
    a, r = int(SR * attack), int(SR * release)
    if a:
        e[:a] = np.linspace(0, 1, a)
    if r:
        e[-r:] *= np.linspace(1, 0, r)
    return e


def save(name, x, peak=0.8):
    x = np.asarray(x, dtype=np.float64)
    x = x - np.mean(x)
    m = np.max(np.abs(x)) or 1.0
    x = x / m * peak
    os.makedirs(OUT, exist_ok=True)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as f:
        wavfile.write(f.name, SR, (x * 32767).astype(np.int16))
        tmp = f.name
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", tmp, "-c:a", "libvorbis", "-q:a", "3", os.path.join(OUT, name + ".ogg")], check=True)
    os.remove(tmp)


def seamless(x, fade=1.5):
    """Loop-safe: crossfade the tail into the head."""
    n = int(SR * fade)
    head, body, tail = x[:n], x[n:-n] if n else x, x[-n:]
    w = np.linspace(0, 1, n)
    return np.concatenate([tail * (1 - w) + head * w, x[n:-n]])


def noise(seconds, seed):
    return np.random.default_rng(seed).standard_normal(int(SR * seconds))


def brown(seconds, seed):
    x = np.cumsum(noise(seconds, seed))
    return hp(x, 15)


# ---------------------------------------------------------------- loops

def hum(name, freqs, noise_lo, noise_hi, seed, length=24.0, shimmer=None):
    tt = t(length)
    x = np.zeros_like(tt)
    for i, f in enumerate(freqs):
        f = round(f * length) / length  # whole cycles: loops cleanly
        lfo = 0.6 + 0.4 * np.sin(2 * np.pi * (i + 1) / length * tt + i)
        x += np.sin(2 * np.pi * f * tt + i) * lfo / (i + 1)
    n = bp(brown(length + 3, seed), noise_lo, noise_hi)[: len(tt)]
    x = x + n / np.max(np.abs(n)) * 0.5
    if shimmer:
        rnd = np.random.default_rng(seed + 1)
        for _ in range(shimmer):
            at = rnd.uniform(1, length - 4)
            f = rnd.uniform(1800, 3800)
            s = t(3.0)
            ping = np.sin(2 * np.pi * f * s) * np.exp(-s * 2.2) * 0.12
            i0 = int(at * SR)
            x[i0:i0 + len(ping)] += ping
    return seamless(x)


# ---------------------------------------------------------------- one-shots

def drip(seed):
    rnd = np.random.default_rng(seed)
    s = t(0.12)
    f0 = rnd.uniform(700, 1100)
    f = f0 * (1 + 1.2 * s / s[-1])
    x = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-s * 45)
    x = np.concatenate([np.zeros(int(SR * 0.02)), x])
    return reverb(x, 1.2, 0.5, seed, 5000)


def bell(seed, base):
    s = t(7.0)
    x = np.zeros_like(s)
    for r, a, d in [(0.5, 0.6, 0.5), (1.0, 1.0, 0.8), (1.19, 0.5, 1.1), (1.56, 0.45, 1.3), (2.0, 0.35, 1.6),
                    (2.74, 0.25, 2.0), (3.0, 0.2, 2.4), (4.1, 0.12, 3.0)]:
        x += a * np.sin(2 * np.pi * base * r * s + r) * np.exp(-s * d)
    x *= env(len(x), 0.003, 0.5)
    x = lp(x, 1400)  # far away
    return reverb(x, 3.5, 0.55, seed, 1800)


def thumps(seed, count, gap, f=55.0, click=True):
    rnd = np.random.default_rng(seed)
    total = t(count * gap + 1.0)
    x = np.zeros_like(total)
    for i in range(count):
        s = t(0.25)
        thump = np.sin(2 * np.pi * f * s * (1 - 0.3 * s)) * np.exp(-s * 18)
        if click:
            thump += lp(rnd.standard_normal(len(s)), 600) * np.exp(-s * 40) * 0.6
        i0 = int((0.1 + i * gap + rnd.uniform(-0.04, 0.04)) * SR)
        amp = rnd.uniform(0.75, 1.0)
        x[i0:i0 + len(thump)] += thump * amp
    x = lp(x, 400)  # through stone
    return reverb(x, 1.0, 0.3, seed, 800)


def knock(seed):
    rnd = np.random.default_rng(seed)
    x = np.zeros(int(SR * 2.2))
    for i, at in enumerate([0.1, 0.45, 0.8]):
        s = t(0.09)
        k = bp(rnd.standard_normal(len(s)), 250, 1400) * np.exp(-s * 70) + np.sin(2 * np.pi * 180 * s) * np.exp(-s * 50)
        i0 = int(at * SR)
        x[i0:i0 + len(k)] += k
    return reverb(lp(x, 1500), 1.2, 0.35, seed, 1500)


def whisper(seed, seconds=2.6):
    rnd = np.random.default_rng(seed)
    n = noise(seconds, seed)
    x = np.zeros_like(n)
    # three formant bands that glide, like a voice without a voice
    for lo, hi in [(500, 1100), (1300, 2600), (2800, 6000)]:
        x += bp(n, lo * rnd.uniform(0.9, 1.1), hi * rnd.uniform(0.9, 1.1))
    # syllables
    tt = t(seconds)
    syll = np.zeros_like(tt)
    at = 0.15
    while at < seconds - 0.3:
        d = rnd.uniform(0.12, 0.3)
        c = int(at * SR)
        w = int(d * SR)
        syll[c:c + w] += np.hanning(w) * rnd.uniform(0.5, 1.0)
        at += d + rnd.uniform(0.02, 0.18)
    x *= lp(syll, 30)
    x += bp(n, 4000, 8000) * lp(syll, 30) * 0.4  # sibilance
    return reverb(x, 0.8, 0.2, seed, 6000)


def breath(seed):
    n = noise(4.0, seed)
    tt = t(4.0)
    e = np.clip(np.sin(np.pi * tt / 1.6), 0, None) ** 2 * (tt < 1.6) + np.clip(np.sin(np.pi * (tt - 1.9) / 1.9), 0, None) ** 2 * (tt > 1.9) * 0.8
    x = bp(n, 300, 1600) * e + bp(n, 1600, 3500) * e * 0.3
    return x


def creak(seed):
    rnd = np.random.default_rng(seed)
    tt = t(2.4)
    f = 70 + 40 * np.sin(2 * np.pi * 0.3 * tt + rnd.uniform(0, 6)) + lp(rnd.standard_normal(len(tt)), 8) * 30
    ph = np.cumsum(f) / SR
    pulses = (np.mod(ph, 1.0) < 0.08).astype(float)  # stick-slip clicks
    x = bp(pulses, 150, 2500) * env(len(tt), 0.3, 0.6)
    return reverb(x, 1.5, 0.4, seed, 2000)


def wail(seed):
    rnd = np.random.default_rng(seed)
    tt = t(3.5)
    f0 = 380 * (1 + 0.35 * np.sin(np.pi * tt / 3.5)) * (1 + 0.012 * np.sin(2 * np.pi * 5.5 * tt))
    ph = 2 * np.pi * np.cumsum(f0) / SR
    x = sum(np.sin(ph * k) / k ** 1.3 for k in range(1, 8))
    x += bp(rnd.standard_normal(len(tt)), 800, 3000) * 0.15
    x *= env(len(tt), 0.4, 1.4)
    x = lp(x, 900)
    return reverb(x, 4.0, 0.7, seed, 1200)


def chime(seed):
    rnd = np.random.default_rng(seed)
    s = t(3.5)
    x = np.zeros_like(s)
    for i in range(rnd.integers(3, 6)):
        at = int(rnd.uniform(0, 1.2) * SR)
        f = rnd.uniform(1800, 4200)
        p = np.sin(2 * np.pi * f * s[: len(s) - at]) * np.exp(-s[: len(s) - at] * rnd.uniform(1.5, 3.0))
        x[at:] += p * rnd.uniform(0.4, 1.0)
    return reverb(x, 2.5, 0.5, seed, 7000)


def gust(seed):
    n = noise(5.0, seed)
    tt = t(5.0)
    e = np.sin(np.pi * tt / 5.0) ** 2
    cutoff = 300 + 1200 * e
    # time-varying lowpass by blocks
    out = np.zeros_like(n)
    block = 1024
    zi = None
    for i in range(0, len(n), block):
        b, a = signal.butter(2, cutoff[i] / (SR / 2), "low")
        if zi is None:
            zi = signal.lfilter_zi(b, a) * 0
        out[i:i + block], zi = signal.lfilter(b, a, n[i:i + block], zi=zi)
    return out * e


def splash(seed):
    rnd = np.random.default_rng(seed)
    s = t(1.8)
    x = lp(rnd.standard_normal(len(s)), 2500) * np.exp(-s * 6)
    for _ in range(8):  # bubbles
        at = int(rnd.uniform(0.15, 1.2) * SR)
        bs = t(0.05)
        f = rnd.uniform(400, 900) * (1 + 3 * bs)
        bub = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-bs * 60) * 0.5
        x[at:at + len(bub)] += bub
    return reverb(lp(x, 1800), 1.2, 0.4, seed, 2000)


def heartbeat_far(seed):
    return thumps(seed, 6, 0.42, f=48.0, click=False)


if __name__ == "__main__":
    save("hum", hum("hum", [41, 55, 61.7, 82], 40, 220, 1))
    save("crystal_hum", hum("crystal_hum", [55, 82.5, 110, 165], 120, 900, 2, shimmer=9), 0.7)
    save("lake_hum", hum("lake_hum", [36.7, 49, 73.4], 60, 500, 3))
    save("forest_hum", hum("forest_hum", [43.6, 58.3, 65.4], 200, 1600, 4))
    for i in range(3):
        save(f"drip{i + 1}", drip(10 + i))
    save("bell1", bell(20, 196.0))
    save("bell2", bell(21, 164.8))
    save("steps_above1", thumps(30, 6, 0.55))
    save("steps_above2", thumps(31, 9, 0.38))
    save("steps_behind", lp(thumps(32, 4, 0.5, f=90.0) * 1.0, 1800))
    save("knock1", knock(40))
    save("knock2", knock(41))
    for i in range(4):
        save(f"whisper{i + 1}", whisper(50 + i))
    save("breath", breath(60))
    save("creak1", creak(70))
    save("creak2", creak(71))
    save("wail", wail(80), 0.6)
    save("chime1", chime(90))
    save("chime2", chime(91))
    save("gust1", gust(100))
    save("gust2", gust(101))
    save("splash", splash(110))
    save("heartbeat_far", heartbeat_far(120))
    print("sounds written")
