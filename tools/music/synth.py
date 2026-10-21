"""2.8: a small synthesiser for Umbra's boss music: oscillators, envelopes, filters, drums, the instruments, reverb and a
mixer with sidechain pumping and a master bus. Everything is numpy/scipy; nothing is sampled: every sound is made here.
(Used by tools/gen_music28.py.)"""
import math

import numpy as np
from scipy import signal

SR = 44100
RNG = np.random.default_rng(1234)


def midi_hz(m):
    return 440.0 * 2.0 ** ((m - 69) / 12.0)


def n_of(sec):
    return max(1, int(round(sec * SR)))


def tt(n):
    return np.arange(n) / SR


# ------------------------------------------------------------------ oscillators
def _blep(ph, dt):
    """PolyBLEP: rounds off the jump of a saw or square so it does not alias into a hiss."""
    out = np.zeros_like(ph)
    m = ph < dt
    t = ph[m] / dt[m]
    out[m] = t + t - t * t - 1.0
    m2 = ph > 1.0 - dt
    t2 = (ph[m2] - 1.0) / dt[m2]
    out[m2] = t2 * t2 + t2 + t2 + 1.0
    return out


def phase(freq, n, ph0=None):
    f = np.asarray(freq, dtype=float)[:n] if np.ndim(freq) else np.full(n, float(freq))
    inc = f / SR
    ph = np.cumsum(inc)
    ph += RNG.random() if ph0 is None else ph0
    return np.mod(ph, 1.0), inc


def saw(freq, n, ph0=None):
    ph, inc = phase(freq, n, ph0)
    return 2.0 * ph - 1.0 - _blep(ph, inc)


def square(freq, n, ph0=None, width=0.5):
    ph, inc = phase(freq, n, ph0)
    sq = np.where(ph < width, 1.0, -1.0)
    sq += _blep(ph, inc)
    sq -= _blep(np.mod(ph + (1.0 - width), 1.0), inc)
    return sq


def sine(freq, n, ph0=0.0):
    ph, _ = phase(freq, n, ph0)
    return np.sin(2 * np.pi * ph)


def tri(freq, n, ph0=None):
    ph, _ = phase(freq, n, ph0)
    return 2.0 * np.abs(2.0 * ph - 1.0) - 1.0


def noise(n):
    return RNG.standard_normal(n)


def vibrato(freq, n, rate=5.0, depth=0.004, delay=0.25):
    t = tt(n)
    ramp = np.clip((t - delay) / 0.4, 0, 1)
    return freq * (1.0 + depth * ramp * np.sin(2 * np.pi * rate * t + RNG.random() * 6.28))


# ------------------------------------------------------------------ envelopes
def adsr(n, a=0.01, d=0.1, s=0.7, r=0.1, gate=None):
    """Attack, decay, sustain, release; the note is held for `gate` seconds (default: all but the release)."""
    gate = (n / SR - r) if gate is None else gate
    t = tt(n)
    env = np.empty(n)
    a = max(a, 1e-4)
    att = t < a
    env[att] = t[att] / a
    dec = (t >= a) & (t < a + d)
    env[dec] = 1.0 - (1.0 - s) * (t[dec] - a) / max(d, 1e-4)
    sus = (t >= a + d)
    env[sus] = s
    # release from wherever the gate closes
    g = gate
    if g < a:
        lvl = g / a
    elif g < a + d:
        lvl = 1.0 - (1.0 - s) * (g - a) / max(d, 1e-4)
    else:
        lvl = s
    rel = t >= g
    env[rel] = lvl * np.clip(1.0 - (t[rel] - g) / max(r, 1e-4), 0, 1)
    return env


def expdec(n, tau, a=0.002):
    t = tt(n)
    e = np.exp(-t / tau)
    if a > 0:
        e *= np.clip(t / a, 0, 1)
    return e


# ------------------------------------------------------------------ filters
def lp(x, fc, order=2):
    fc = min(fc, SR * 0.45)
    sos = signal.butter(order, fc, 'low', fs=SR, output='sos')
    return signal.sosfilt(sos, x)


def hp(x, fc, order=2):
    sos = signal.butter(order, max(fc, 10), 'high', fs=SR, output='sos')
    return signal.sosfilt(sos, x)


def bp(x, lo, hi, order=2):
    sos = signal.butter(order, [max(lo, 10), min(hi, SR * 0.45)], 'band', fs=SR, output='sos')
    return signal.sosfilt(sos, x)


def peak(x, fc, q, gain_db):
    """A peaking EQ band."""
    A = 10 ** (gain_db / 40)
    w = 2 * np.pi * fc / SR
    al = np.sin(w) / (2 * q)
    b = [1 + al * A, -2 * np.cos(w), 1 - al * A]
    a = [1 + al / A, -2 * np.cos(w), 1 - al / A]
    return signal.lfilter(b, a, x)


def sweep_lp(x, cut, block=256, order=2):
    """A lowpass whose cutoff follows the array `cut` (Hz per sample), applied in short blocks with carried state."""
    out = np.empty_like(x)
    zi = None
    for i in range(0, len(x), block):
        fc = float(np.clip(cut[min(i + block // 2, len(cut) - 1)], 40, SR * 0.45))
        sos = signal.butter(order, fc, 'low', fs=SR, output='sos')
        if zi is None:
            zi = np.zeros((sos.shape[0], 2))
        out[i:i + block], zi = signal.sosfilt(sos, x[i:i + block], zi=zi)
    return out


def sweep_bp(x, centre, width=1.0, block=256):
    out = np.empty_like(x)
    zi = None
    for i in range(0, len(x), block):
        c = float(np.clip(centre[min(i + block // 2, len(centre) - 1)], 60, SR * 0.4))
        lo, hi = c / (1 + width), min(c * (1 + width), SR * 0.45)
        sos = signal.butter(2, [lo, hi], 'band', fs=SR, output='sos')
        if zi is None:
            zi = np.zeros((sos.shape[0], 2))
        out[i:i + block], zi = signal.sosfilt(sos, x[i:i + block], zi=zi)
    return out


def drive(x, k=1.5):
    return np.tanh(k * x) / np.tanh(k)


# ------------------------------------------------------------------ drums
def kick(vel=1.0, punch=1.0, length=0.55, low=44.0):
    n = n_of(length)
    t = tt(n)
    f = low + 120.0 * punch * np.exp(-t / 0.032) + 30 * np.exp(-t / 0.2)
    body = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t / 0.30)
    click = hp(noise(n), 1500) * np.exp(-t / 0.005) * 0.35
    return drive(body + click, 1.8) * vel * 0.9


def snare(vel=1.0, tail=0.16, tone=190.0):
    n = n_of(0.45)
    t = tt(n)
    body = (np.sin(2 * np.pi * tone * t) * 0.6 + np.sin(2 * np.pi * tone * 1.72 * t) * 0.3) * np.exp(-t / 0.06)
    nz = bp(noise(n), 1400, 7500) * np.exp(-t / tail)
    return drive(body * 0.8 + nz * 0.9, 1.3) * vel * 0.7


def clap(vel=1.0):
    n = n_of(0.35)
    t = tt(n)
    env = np.zeros(n)
    for k, d in enumerate((0.0, 0.011, 0.023)):
        env += np.exp(-np.clip(t - d, 0, None) / 0.008) * (t >= d) * (0.8 if k < 2 else 1.0)
    env += np.exp(-np.clip(t - 0.023, 0, None) / 0.12) * (t >= 0.023) * 0.5
    return bp(noise(n), 900, 4200) * env * vel * 0.55


def hat(vel=1.0, open_=False):
    n = n_of(0.35 if open_ else 0.09)
    t = tt(n)
    x = bp(noise(n), 6000, 10500, order=2)
    return x * np.exp(-t / (0.16 if open_ else 0.022)) * vel * 0.32


def shaker(vel=1.0):
    n = n_of(0.08)
    t = tt(n)
    env = np.clip(t / 0.012, 0, 1) * np.exp(-t / 0.03)
    return bp(noise(n), 4000, 8500) * env * vel * 0.3


def taiko(vel=1.0, pitch=58.0, length=1.2):
    n = n_of(length)
    t = tt(n)
    f = pitch * (1 + 0.7 * np.exp(-t / 0.04))
    body = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t / 0.42)
    skin = lp(noise(n), 1600) * np.exp(-t / 0.05) * 0.5
    room = lp(noise(n), 400) * np.exp(-t / 0.35) * 0.18
    return drive(body + skin + room, 1.6) * vel


def tom(vel=1.0, pitch=110.0):
    n = n_of(0.6)
    t = tt(n)
    f = pitch * (1 + 0.5 * np.exp(-t / 0.03))
    body = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t / 0.22)
    skin = bp(noise(n), 300, 3000) * np.exp(-t / 0.03) * 0.3
    return drive(body + skin, 1.4) * vel * 0.8


def cymbal(vel=1.0, length=2.2):
    n = n_of(length)
    t = tt(n)
    x = bp(noise(n), 3500, 10000)
    x += bp(noise(n), 2000, 5000) * 0.4
    return x * np.exp(-t / (length * 0.35)) * np.clip(t / 0.003, 0, 1) * vel * 0.22


def reverse_cymbal(length=1.6, vel=1.0):
    return cymbal(vel, length)[::-1] * 0.9


def riser(length, vel=1.0, lo=250.0, hi=4500.0):
    n = n_of(length)
    t = tt(n)
    c = lo * (hi / lo) ** (t / length)
    x = sweep_bp(noise(n), c, 0.6)
    env = (t / length) ** 2
    tone = saw(110 * 2 ** (2 * t / length), n) * 0.12
    return (x * 0.8 + lp(tone, 1500)) * env * vel * 0.5


def impact(vel=1.0, length=2.5):
    n = n_of(length)
    t = tt(n)
    f = 55 * np.exp(-t / 0.8) + 28
    sub = np.sin(2 * np.pi * np.cumsum(f) / SR) * np.exp(-t / 0.9)
    boom = lp(noise(n), 700) * np.exp(-t / 0.25) * 0.6
    crack = bp(noise(n), 1000, 6000) * np.exp(-t / 0.04) * 0.3
    return drive(sub + boom + crack, 1.5) * vel


def woodblock(vel=1.0, pitch=880.0):
    n = n_of(0.12)
    t = tt(n)
    x = np.sin(2 * np.pi * pitch * t) * np.exp(-t / 0.025) + np.sin(2 * np.pi * pitch * 1.6 * t) * np.exp(-t / 0.012) * 0.4
    return x * vel * 0.45


def metal(vel=1.0, pitch=420.0, length=0.6):
    """A struck piece of metal (an anvil, a clock's escapement): FM with an inharmonic modulator."""
    n = n_of(length)
    t = tt(n)
    idx = 3.0 * np.exp(-t / 0.06)
    mod = np.sin(2 * np.pi * pitch * 2.76 * t) * idx
    x = np.sin(2 * np.pi * pitch * t + mod) * np.exp(-t / (length * 0.3))
    return lp(x, 5200) * vel * 0.5


# ------------------------------------------------------------------ instruments (a note of each)
def bass(m, dur, vel=1.0, cut=700.0, reese=False):
    n = n_of(dur + 0.08)
    f = midi_hz(m)
    x = saw(f, n) * 0.6 + sine(f / 2 if m > 40 else f, n) * 0.5
    if reese:
        x = saw(f * 1.006, n) * 0.4 + saw(f * 0.994, n) * 0.4 + sine(f, n) * 0.45
    t = tt(n)
    cutoff = cut * (1.0 + 2.5 * vel * np.exp(-t / 0.07))
    x = sweep_lp(x, cutoff, order=2)
    return drive(x, 1.6) * adsr(n, 0.004, 0.08, 0.85, 0.06, gate=dur) * vel * 0.55


def strings_stac(m, dur, vel=1.0, bright=2600.0):
    n = n_of(dur + 0.12)
    f = midi_hz(m)
    x = sum(saw(f * c, n) for c in (0.9958, 1.0, 1.0045)) / 3
    x = lp(x, bright * (0.7 + 0.3 * vel))
    return x * adsr(n, 0.006, 0.09, 0.5, 0.08, gate=min(dur, 0.14)) * vel * 0.5


def strings_sus(m, dur, vel=1.0, bright=1900.0, attack=0.22):
    n = n_of(dur + 0.45)
    f = midi_hz(m)
    x = sum(saw(vibrato(f * c, n, 5.2, 0.0035), n) for c in (0.994, 0.998, 1.002, 1.006)) / 4
    x = lp(x, bright)
    return x * adsr(n, attack, 0.3, 0.85, 0.4, gate=dur) * vel * 0.45


def brass(m, dur, vel=1.0, bright=1.0):
    n = n_of(dur + 0.2)
    f = midi_hz(m)
    fv = vibrato(f, n, 5.0, 0.003, 0.35)
    x = (saw(fv * 1.003, n) + saw(fv * 0.997, n)) * 0.5
    t = tt(n)
    top = (900.0 + 1900.0 * vel) * bright * (1 + 0.15 * np.log2(max(f, 60) / 220))
    cut = 280.0 + (top - 280.0) * (1 - np.exp(-t / 0.045)) * (0.82 + 0.18 * np.exp(-t / 0.3))
    x = sweep_lp(x, cut)
    x += lp(noise(n), 2500) * np.exp(-t / 0.05) * 0.03
    return drive(x, 1.3) * adsr(n, 0.03, 0.15, 0.85, 0.16, gate=dur) * vel * 0.5


def braam(root, dur, vel=1.0):
    n = n_of(dur + 0.5)
    t = tt(n)
    x = np.zeros(n)
    for m, a in ((root, 1.0), (root + 7, 0.7), (root + 12, 0.6), (root - 12, 0.7)):
        f = midi_hz(m)
        x += (saw(f * 1.004, n) + saw(f * 0.996, n)) * 0.5 * a
    cut = 150 + 1600 * (1 - np.exp(-t / 0.5)) * np.exp(-t / (dur * 1.5))
    x = sweep_lp(x, cut)
    return drive(x * 0.6, 2.2) * adsr(n, 0.08, 0.4, 0.8, 0.5, gate=dur) * vel * 0.55


def choir(m, dur, vel=1.0, vowel="a"):
    n = n_of(dur + 0.6)
    f = midi_hz(m)
    src = sum(saw(vibrato(f * c, n, 4.6, 0.006, 0.1), n) for c in (0.993, 0.998, 1.003, 1.008)) / 4
    F = {"a": ((750, 1.0), (1150, 0.55), (2700, 0.2)), "o": ((480, 1.0), (850, 0.6), (2600, 0.15)),
         "u": ((350, 1.0), (750, 0.5), (2400, 0.1))}[vowel]
    x = sum(bp(src, fc / 1.18, fc * 1.18) * g for fc, g in F)
    x += lp(noise(n), 3000) * 0.01
    return x * adsr(n, 0.35, 0.3, 0.9, 0.55, gate=dur) * vel * 1.5


def pluck(m, dur, vel=1.0, bright=0.5, decay=0.996):
    """Karplus-Strong: a burst of noise in a delay line that keeps losing its highs."""
    f = midi_hz(m)
    N = max(2, int(round(SR / f)))
    n = n_of(dur + 0.3)
    exc = np.zeros(n)
    burst = noise(N)
    burst = lp(burst, 2000 + 6000 * bright)
    exc[:N] = burst
    a = np.zeros(N + 2)
    a[0] = 1.0
    a[N] = -0.5 * decay
    a[N + 1] = -0.5 * decay
    y = signal.lfilter([1.0], a, exc)
    env = adsr(n, 0.001, 0.0, 1.0, 0.12, gate=dur)
    return y * env * vel * 0.35


def harpsichord(m, dur, vel=1.0):
    x = pluck(m, dur, vel, 0.85, 0.997) + pluck(m + 12, dur, vel * 0.35, 0.9, 0.995)
    return hp(x, 120)


def pizz(m, dur, vel=1.0):
    return lp(pluck(m, min(dur, 0.35), vel, 0.3, 0.990), 2500) * 1.3


def piano(m, dur, vel=1.0):
    f = midi_hz(m)
    n = n_of(dur + 1.2)
    t = tt(n)
    x = np.zeros(n)
    B = 0.0003
    bright = 0.5 + 0.5 * vel
    for k in range(1, 14):
        fk = k * f * math.sqrt(1 + B * k * k)
        if fk > 9000:
            break
        amp = (1.0 / k ** (1.4 - 0.5 * bright))
        tau = 2.8 / (1 + 0.35 * k) * (440 / max(f, 110)) ** 0.35
        x += amp * np.sin(2 * np.pi * fk * t + RNG.random()) * np.exp(-t / tau)
    x += lp(noise(n), 3000) * np.exp(-t / 0.01) * 0.05
    rel = np.clip(1 - (t - dur) / 0.25, 0, 1)
    return x * np.clip(t / 0.002, 0, 1) * rel * vel * 0.18


def bell(m, dur, vel=1.0):
    f = midi_hz(m)
    n = n_of(max(dur, 3.0))
    t = tt(n)
    x = np.zeros(n)
    for r, a, tau in ((0.5, 0.6, 3.5), (1.0, 1.0, 2.6), (1.19, 0.5, 1.8), (1.5, 0.45, 1.6), (2.0, 0.35, 1.2),
                      (2.52, 0.25, 0.9), (2.67, 0.2, 0.8), (3.01, 0.15, 0.6)):
        if f * r > 7000:
            continue
        x += a * np.sin(2 * np.pi * f * r * t) * np.exp(-t / tau)
    return x * np.clip(t / 0.003, 0, 1) * vel * 0.16


def organ(m, dur, vel=1.0):
    f = midi_hz(m)
    n = n_of(dur + 0.15)
    t = tt(n)
    x = np.zeros(n)
    for r, a in ((0.5, 0.6), (1, 1.0), (2, 0.55), (3, 0.3), (4, 0.25), (6, 0.08)):
        if f * r < 6000:
            x += a * np.sin(2 * np.pi * f * r * t + RNG.random())
    x *= 1 + 0.12 * np.sin(2 * np.pi * 5.8 * t)
    return x * adsr(n, 0.02, 0.05, 0.95, 0.1, gate=dur) * vel * 0.16


def reed(m, dur, vel=1.0):
    """A duduk / oboe-ish voice: a filtered saw with a nasal peak and a slow vibrato. Kept in the middle register."""
    f = midi_hz(m)
    n = n_of(dur + 0.2)
    x = saw(vibrato(f, n, 4.8, 0.007, 0.2), n)
    x = bp(x, 300, 3200)
    x = peak(x, 1250, 2.0, 6.0)
    x += bp(noise(n), 800, 2500) * 0.03
    return x * adsr(n, 0.05, 0.1, 0.85, 0.12, gate=dur) * vel * 0.4


def arp_synth(m, dur, vel=1.0, cut=2100.0):
    f = midi_hz(m)
    n = n_of(dur + 0.08)
    x = square(f * 1.002, n, width=0.42) * 0.5 + saw(f * 0.998, n) * 0.5
    t = tt(n)
    x = sweep_lp(x, cut * (0.6 + 0.8 * np.exp(-t / 0.05)))
    return x * adsr(n, 0.002, 0.08, 0.4, 0.05, gate=dur) * vel * 0.32


def pad(m, dur, vel=1.0, bright=1500.0):
    f = midi_hz(m)
    n = n_of(dur + 0.8)
    x = sum(saw(f * c, n) for c in (0.991, 0.997, 1.003, 1.009)) / 4
    x = lp(x, bright)
    return x * adsr(n, 0.5, 0.5, 0.9, 0.8, gate=dur) * vel * 0.35


# ------------------------------------------------------------------ space
def make_ir(seconds=2.4, predelay=0.02, damp=3500.0, early=0.0, seed=7):
    rng = np.random.default_rng(seed)
    n = n_of(seconds)
    t = tt(n)
    ir = []
    for ch in range(2):
        x = rng.standard_normal(n) * np.exp(-6.9 * t / seconds)
        bright = lp(x, damp * 1.8)
        dark = lp(x, damp * 0.5)
        mixk = np.clip(t / (seconds * 0.4), 0, 1)
        x = bright * (1 - mixk) + dark * mixk
        if early:
            for d, g in ((0.011, 0.5), (0.019, 0.4), (0.027, 0.35), (0.041, 0.25)):
                i = n_of(d + ch * 0.003)
                if i < n:
                    x[i] += g * early * 8
        pre = np.zeros(n_of(predelay))
        ir.append(np.concatenate([pre, x])[:n])
    ir = np.array(ir)
    return ir / np.sqrt(np.sum(ir ** 2, axis=1, keepdims=True))


def reverb(stereo, ir):
    out = np.zeros((2, stereo.shape[1] + ir.shape[1] - 1))
    for ch in range(2):
        out[ch] = signal.fftconvolve(stereo[ch], ir[ch])
    return out


def delay(stereo, seconds, feedback=0.35, repeats=4, pingpong=True, damp=3500.0):
    d = n_of(seconds)
    out = stereo.copy()
    for k in range(1, repeats + 1):
        if d * k >= stereo.shape[1]:
            break
        shifted = np.zeros_like(stereo)
        shifted[:, d * k:] = stereo[:, :-d * k]
        if pingpong and k % 2:
            shifted = shifted[::-1]
        out += lp(shifted, damp) * feedback ** k
    return out


def pan_stereo(x, pan=0.0):
    a = (pan + 1) * np.pi / 4
    return np.vstack([x * np.cos(a), x * np.sin(a)])


def compress(stereo, threshold=0.5, ratio=3.0, attack=0.005, release=0.12):
    level = np.max(np.abs(stereo), axis=0)
    a_att = np.exp(-1 / (attack * SR))
    a_rel = np.exp(-1 / (release * SR))
    # an envelope follower: fast up, slow down (two passes of a one-pole on the rectified signal)
    env = signal.lfilter([1 - a_rel], [1, -a_rel], level)
    env = np.maximum(env, signal.lfilter([1 - a_att], [1, -a_att], level))
    over = np.maximum(env / threshold, 1.0)
    gain = over ** (1.0 / ratio - 1.0)
    return stereo * gain


def limit(stereo, ceiling=0.93):
    peak = np.max(np.abs(stereo))
    if peak > 0:
        stereo = stereo / peak * 1.25
    return np.tanh(stereo) * ceiling / np.tanh(1.25)
