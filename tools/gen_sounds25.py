"""2.5: Yan's voice, again. The 2.4 one was a buzz through vowel filters and came out like a villager's "hrrm".
This one is an old man's half-whispered muttering: breath first, a low creaky voice under it, real consonants
(sh, s, h, t, k, p) between the vowels, falling intonation, a dry chuckle, a sigh and a rasping cough.
Run from the repo root: python3 tools/gen_sounds25.py   (numpy, scipy, ffmpeg with libvorbis)"""
import os
import sys

import numpy as np

sys.path.insert(0, os.path.dirname(__file__))
import gen_sounds as g  # noqa: E402
from gen_sounds24 import save, env_adsr  # noqa: E402

SR = g.SR

# formants of an old male voice (a little lower and duller than a young one)
F = {"a": (650, 1080, 2400), "o": (430, 780, 2450), "u": (320, 720, 2250), "e": (420, 1750, 2400),
     "i": (300, 2050, 2700), "y": (330, 1450, 2350)}


def creak(n, f0, seed, fall=0.25, fry=0.35):
    """Glottal pulses with jitter and shimmer; part of the time it drops into vocal fry (slow, uneven clicks)."""
    rnd = np.random.default_rng(seed)
    out = np.zeros(n)
    pos = 0.0
    k = 0
    while pos < n:
        prog = pos / n
        f = f0 * (1 - fall * prog) * (1 + 0.06 * rnd.standard_normal())
        if rnd.random() < fry * (0.4 + prog):          # fry grows towards the end of a phrase
            f *= 0.45 + 0.15 * rnd.random()
        period = SR / max(f, 30)
        i = int(pos)
        L = min(int(period * 0.6), n - i)
        if L > 0:
            amp = (0.7 + 0.3 * rnd.random())
            out[i:i + L] += amp * np.exp(-np.linspace(0, 7, L)) * np.sin(np.linspace(0, np.pi * 3, L))
        pos += period
        k += 1
    return out


def formant(src, v, breath):
    f1, f2, f3 = F[v]
    voiced = g.bp(src, f1 * 0.8, f1 * 1.25) + 0.5 * g.bp(src, f2 * 0.85, f2 * 1.15) + 0.18 * g.bp(src, f3 * 0.9, f3 * 1.1)
    noise = np.random.default_rng(len(src)).standard_normal(len(src))
    whisper = g.bp(noise, f1 * 0.8, f1 * 1.3) * 0.5 + g.bp(noise, f2 * 0.85, f2 * 1.2) * 0.35
    return voiced * (1 - breath) + whisper * breath * 0.6


def consonant(c, seed):
    rnd = np.random.default_rng(seed)
    if c in "sš":
        n = int(SR * (0.09 + 0.05 * rnd.random()))
        lo, hi = (3800, 8000) if c == "s" else (1900, 5200)
        return g.bp(rnd.standard_normal(n), lo, hi) * env_adsr(n, 0.02, 0.04) * 0.5
    if c == "h":
        n = int(SR * 0.08)
        return g.bp(rnd.standard_normal(n), 500, 2500) * env_adsr(n, 0.02, 0.04) * 0.3
    if c in "tkp":
        gap = np.zeros(int(SR * 0.045))
        n = int(SR * 0.025)
        lo, hi = {"t": (2500, 6500), "k": (1200, 3500), "p": (400, 1600)}[c]
        burst = g.bp(rnd.standard_normal(n), lo, hi) * np.exp(-np.linspace(0, 5, n)) * 0.7
        return np.concatenate([gap, burst])
    if c in "mn":
        n = int(SR * 0.07)
        src = creak(n, 95, seed, fall=0, fry=0.1)
        return g.lp(src, 400) * env_adsr(n, 0.01, 0.02) * 0.8
    return np.zeros(int(SR * 0.03))


def phrase(seed, text, f0=92, breath=0.45):
    """Mutters a made-up phrase: consonants as noise, vowels as a creaky, breathy voice, pitch falling over it."""
    rnd = np.random.default_rng(seed)
    out = []
    n_vowels = sum(ch in F for ch in text)
    vi = 0
    for i, ch in enumerate(text):
        if ch == " ":
            out.append(np.zeros(int(SR * (0.06 + 0.1 * rnd.random()))))
        elif ch in F:
            d = 0.09 + 0.11 * rnd.random()
            if vi == n_vowels - 1:
                d *= 1.8                                   # the last vowel is drawn out
            n = int(SR * d)
            pitch = f0 * (1.08 - 0.22 * vi / max(1, n_vowels - 1)) * (1 + 0.05 * rnd.standard_normal())
            src = creak(n, pitch, seed * 31 + i, fall=0.08, fry=0.25 if vi < n_vowels - 1 else 0.6)
            out.append(formant(src, ch, breath) * env_adsr(n, 0.015, 0.05))
            vi += 1
        else:
            out.append(consonant(ch, seed * 17 + i))
    x = np.concatenate(out)
    x = g.hp(x, 90) + 0.04 * g.bp(np.random.default_rng(seed).standard_normal(len(x)), 300, 3000)   # a rasp of breath all along
    return g.reverb(g.lp(x, 5200), 0.5, 0.12, seed, tone=4000)


def sigh(seed):
    rnd = np.random.default_rng(seed)
    n = int(SR * 1.1)
    noise = rnd.standard_normal(n)
    e = np.concatenate([np.linspace(0, 1, int(n * 0.25)), np.linspace(1, 0, n - int(n * 0.25))]) ** 1.5
    x = (g.bp(noise, 400, 1800) * 0.6 + g.bp(noise, 1800, 3500) * 0.2) * e
    x[: len(x) // 3] += 0.15 * formant(creak(len(x) // 3, 80, seed, fall=0.3, fry=0.8), "a", 0.7)
    return g.reverb(x, 0.6, 0.15, seed)


def chuckle(seed):
    rnd = np.random.default_rng(seed)
    parts = []
    for k in range(4):
        n = int(SR * (0.07 + 0.02 * rnd.random()))
        h = g.bp(rnd.standard_normal(n), 600, 2600) * 0.35
        v = formant(creak(n, 110 - k * 8, seed + k, fall=0.1, fry=0.5), "e", 0.6)
        parts += [(h + v) * env_adsr(n, 0.005, 0.03), np.zeros(int(SR * (0.06 + 0.03 * rnd.random())))]
    return g.reverb(np.concatenate(parts), 0.4, 0.12, seed)


def cough(seed):
    rnd = np.random.default_rng(seed)
    parts = []
    for k in range(3):
        n = int(SR * (0.13 + 0.04 * rnd.random()))
        burst = g.bp(rnd.standard_normal(n), 180, 2400) * np.exp(-np.linspace(0, 4.5, n))
        burst += 0.4 * formant(creak(n, 100, seed + k, fry=0.7), "a", 0.5) * np.exp(-np.linspace(0, 5, n))
        parts += [burst, np.zeros(int(SR * (0.1 + 0.08 * rnd.random())))]
    return g.reverb(np.concatenate(parts), 0.5, 0.12, seed)


def hurt(seed):
    n = int(SR * 0.4)
    x = formant(creak(n, 140, seed, fall=0.45, fry=0.4), "o", 0.35) * env_adsr(n, 0.01, 0.2)
    return g.reverb(np.concatenate([consonant("h", seed), x]), 0.4, 0.12, seed)


if __name__ == "__main__":
    save("voice", "yan_mumble1", phrase(301, "tak tak... nu šo"), peak=0.7)
    save("voice", "yan_mumble2", phrase(302, "hm sveta malo ešo"), peak=0.7)
    save("voice", "yan_mumble3", phrase(303, "ki pa tu so sa"), peak=0.7)
    save("voice", "yan_sigh", sigh(310), peak=0.6)
    save("voice", "yan_chuckle", chuckle(311), peak=0.65)
    save("voice", "yan_cough", cough(312), peak=0.7)
    save("voice", "yan_hurt", hurt(320), peak=0.8)
    print("ok")
