"""2.8: the arranging side of the boss music: a song is bars of a tempo and a metre; parts are laid into buses note by
note; the buses are mixed (reverb sends, sidechain pumping from the kick), mastered, and the tail past the last bar is
folded back onto the first so the track loops without a seam."""
import numpy as np

import synth as s

NOTE = {'C': 0, 'C#': 1, 'Db': 1, 'D': 2, 'D#': 3, 'Eb': 3, 'E': 4, 'F': 5, 'F#': 6, 'Gb': 6, 'G': 7, 'G#': 8,
        'Ab': 8, 'A': 9, 'A#': 10, 'Bb': 10, 'B': 11, 'Cb': 11}
QUAL = {'': (0, 4, 7), 'm': (0, 3, 7), '7': (0, 4, 7, 10), 'm7': (0, 3, 7, 10), 'maj7': (0, 4, 7, 11), 'dim': (0, 3, 6),
        'm7b5': (0, 3, 6, 10), 'sus4': (0, 5, 7), 'sus2': (0, 2, 7), '5': (0, 7), 'add9': (0, 4, 7, 14), 'madd9': (0, 3, 7, 14)}


def note(name):
    """'C#4' -> 61."""
    i = 2 if len(name) > 2 and name[1] in '#b' else 1
    return NOTE[name[:i]] + 12 * (int(name[i:]) + 1)


def parse_chord(c):
    bass = None
    if '/' in c:
        c, bass = c.split('/')
    i = 2 if len(c) > 1 and c[1] in '#b' else 1
    root, qual = NOTE[c[:i]], c[i:]
    return root, QUAL[qual], (NOTE[bass] if bass else root)


def chord_tones(c, lo, hi):
    """Every tone of the chord between lo and hi (MIDI), ascending."""
    root, iv, _ = parse_chord(c)
    pcs = {(root + x) % 12 for x in iv}
    return [m for m in range(lo, hi + 1) if m % 12 in pcs]


def voicing(c, centre=60, size=4):
    """A close voicing of the chord around a centre note."""
    root, iv, _ = parse_chord(c)
    tones = chord_tones(c, centre - 7, centre + 12)
    tones.sort(key=lambda m: abs(m - centre))
    return sorted(tones[:size])


def bass_of(c, octave=2):
    _, _, b = parse_chord(c)
    return b + 12 * (octave + 1)


def mel(text):
    """'A4:1 D5:1.5 r:.5 | ...' -> [(bar, beat, midi or None, dur)], bars split on '|'."""
    out = []
    for b, bar in enumerate(text.split('|')):
        beat = 0.0
        for tok in bar.split():
            n, d = tok.split(':')
            d = float(d)
            out.append((b, beat, None if n == 'r' else note(n), d))
            beat += d
    return out


# 2.8.1: more drive: every track a touch faster, and a driving layer over every bar that has drums (see drive())
TEMPO = 1.07


class Song:
    def __init__(self, name, bpm, beats=4, bars=44, tail=6.0):
        """bpm counts `beats` per bar (quarters in 4/4; eighths in 6/8 and 7/8)."""
        bpm = bpm * TEMPO      # 2.8.1: everything a little faster, for more drive
        self.name, self.bpm, self.beats, self.bars = name, bpm, beats, bars
        self.beat = 60.0 / bpm
        self.length = bars * beats * self.beat
        self.n = s.n_of(self.length + tail)
        self.buses = {}
        self.kicks = []

    def at(self, bar, beat=0.0):
        return (bar * self.beats + beat) * self.beat

    def put(self, bus, x, t, pan=0.0, gain=1.0):
        st = s.pan_stereo(x, pan) if x.ndim == 1 else x
        i = int(round(t * s.SR))
        if i >= self.n:
            return
        j = min(self.n, i + st.shape[1])
        if bus not in self.buses:
            self.buses[bus] = np.zeros((2, self.n))
        self.buses[bus][:, i:j] += st[:, :j - i] * gain

    def play(self, bus, inst, m, bar, beat, dur, vel=1.0, pan=0.0, gain=1.0, **kw):
        if m is None:
            return
        self.put(bus, inst(m, dur * self.beat, vel, **kw), self.at(bar, beat), pan, gain)

    def hit(self, bus, sound, bar, beat, pan=0.0, gain=1.0, kick=False):
        self.put(bus, sound, self.at(bar, beat), pan, gain)
        if kick:
            self.kicks.append(self.at(bar, beat))

    # ------------------------------------------------------------ parts
    def melody(self, bus, inst, notes, bar0, shift=0, vel=1.0, pan=0.0, gain=1.0, legato=1.0, **kw):
        for b, beat, m, d in notes:
            if m is not None:
                self.play(bus, inst, m + shift, bar0 + b, beat, d * legato, vel, pan, gain, **kw)

    def pads(self, bus, inst, chords, bar0, centre=60, size=4, vel=0.8, gain=1.0, per=1, spread=0.5, **kw):
        """Held chords; a chord repeated in the next bar is held on."""
        i = 0
        while i < len(chords):
            c, j = chords[i], i + 1
            while j < len(chords) and chords[j] == c:
                j += 1
            dur = (j - i) * per * self.beats
            for k, m in enumerate(voicing(c, centre, size)):
                pan = (k / max(1, size - 1) - 0.5) * spread
                self.play(bus, inst, m, bar0 + i * per, 0, dur, vel, pan, gain, **kw)
            i = j

    def ostinato(self, bus, inst, chords, bar0, pattern, step, lo, hi, vel=0.8, accents=None, pan=0.0, gain=1.0,
                 per=1, length=0.9, **kw):
        """Each bar: the pattern of indices into the chord's tones between lo and hi, one per step (in beats)."""
        for i, c in enumerate(chords):
            tones = chord_tones(c, lo, hi)
            steps = int(round(per * self.beats / step))
            for k in range(steps):
                idx = pattern[k % len(pattern)]
                if idx is None:
                    continue
                m = tones[min(idx, len(tones) - 1)]
                v = vel * (accents[k % len(accents)] if accents else 1.0)
                self.play(bus, inst, m, bar0 + i * per, k * step, step * length, v, pan, gain, **kw)

    def bassline(self, bus, inst, chords, bar0, rhythm, step, octave=2, vel=0.9, gain=1.0, per=1, octaves=None, **kw):
        """rhythm: a string per bar, 'x' a note, 'o' a softer one, '-' held on, '.' rest; octaves: per-step jumps."""
        for i, c in enumerate(chords):
            root = bass_of(c, octave)
            k = 0
            steps = len(rhythm)
            while k < steps:
                ch = rhythm[k]
                if ch in 'xo':
                    d = 1
                    while k + d < steps and rhythm[k + d] == '-':
                        d += 1
                    jump = octaves[k] if octaves else 0
                    self.play(bus, inst, root + 12 * jump, bar0 + i * per, k * step, d * step * 0.95,
                              vel * (1.0 if ch == 'x' else 0.65), 0.0, gain, **kw)
                    k += d
                else:
                    k += 1

    def drums(self, bar0, bars, step, parts):
        """parts: name -> (pattern string per bar, sound factory(vel), bus, pan, is_kick)."""
        for b in range(bars):
            for name, (pat, make, bus, pan, is_kick) in parts.items():
                if isinstance(pat, (list, tuple)):
                    pat = pat[b % len(pat)]
                for k, ch in enumerate(pat):
                    if ch == '.':
                        continue
                    v = {'x': 1.0, 'X': 1.25, 'o': 0.6, 'g': 0.3}.get(ch, 1.0)
                    self.hit(bus, make(v), bar0 + b, k * step, pan, 1.0, is_kick)

    def roll(self, bus, make, bar, beat, beats, per_beat=4, v0=0.3, v1=1.0, pan=0.0):
        n = int(beats * per_beat)
        for k in range(n):
            self.hit(bus, make(v0 + (v1 - v0) * k / max(1, n - 1)), bar, beat + k / per_beat, pan)

    def boom(self, bar, beat=0.0, vel=1.0):
        self.hit('fx', s.impact(vel), bar, beat, kick=True)
        self.hit('fx', s.cymbal(0.8 * vel, 2.5), bar, beat, 0.2)

    def rise(self, bar_end, bars=2, vel=1.0):
        length = bars * self.beats * self.beat
        self.put('fx', s.riser(length, vel), self.at(bar_end) - length, 0.0)
        self.put('fx', s.reverse_cymbal(min(length, 1.8), vel), self.at(bar_end) - min(length, 1.8), 0.0)


# ------------------------------------------------------------------ the mix
DEFAULT_BUS = {
    'drums': dict(gain=1.0, verb=0.10, duck=0.0, hp=25),
    'perc': dict(gain=0.8, verb=0.18, duck=0.0, hp=150),
    'bass': dict(gain=1.0, verb=0.02, duck=0.45, hp=28, lp=4000),
    'ost': dict(gain=0.85, verb=0.22, duck=0.35, hp=120),
    'pad': dict(gain=0.75, verb=0.45, duck=0.5, hp=140),
    'lead': dict(gain=1.0, verb=0.30, duck=0.15, hp=150),
    'choir': dict(gain=0.8, verb=0.55, duck=0.25, hp=180),
    'fx': dict(gain=0.9, verb=0.35, duck=0.0, hp=20),
    'keys': dict(gain=0.9, verb=0.35, duck=0.2, hp=90),
    'drive': dict(gain=1.0, verb=0.10, duck=0.0, hp=320, lp=6500),
    'fill': dict(gain=1.0, verb=0.22, duck=0.0, hp=55),
}


def sidechain(song, depth):
    n = song.n
    imp = np.zeros(n)
    for t in song.kicks:
        i = int(round(t * s.SR))
        if i < n:
            imp[i] = 1.0
    a = np.exp(-1.0 / (0.16 * s.SR))
    env = s.signal.lfilter([1.0], [1.0, -a], imp)
    env = np.minimum(env, 1.0)
    att = np.exp(-1.0 / (0.004 * s.SR))
    env = s.signal.lfilter([1 - att], [1, -att], env) * 1.0
    return 1.0 - depth * np.clip(env * 1.6, 0, 1)


# how loud each bus sits in the mix (RMS while it plays); a bus's 'gain' in the spec is a trim on this
TARGET = {'drums': 0.19, 'perc': 0.08, 'bass': 0.12, 'ost': 0.11, 'arp': 0.095, 'keys': 0.10, 'pad': 0.07,
          'lead': 0.14, 'choir': 0.09, 'fx': 0.085, 'drive': 0.042, 'fill': 0.08}


def drive(song):
    """2.8.1: action. Over every bar where the drums play: a running shaker and hat in sixteenths (eighths in 6/8 and
    7/8), a tom fill into every fourth bar and into every break, and a crash where the drums come back in."""
    if 'drums' not in song.buses:
        return
    d = song.buses['drums']
    energy = []
    for b in range(song.bars):
        i, j = int(song.at(b) * s.SR), int(song.at(b + 1) * s.SR)
        energy.append(float(np.sqrt(np.mean(d[:, i:j] ** 2))) if j > i else 0.0)
    ref = float(np.median([e for e in energy if e > 1e-4] or [0.0]))
    on = [e > 0.45 * ref for e in energy]
    sub = 4 if song.beats <= 4 else 2
    steps = song.beats * sub
    run = 0
    for b in range(song.bars):
        if not on[b]:
            run = 0
            continue
        if run == 0 or run % 8 == 0:
            song.hit('fill', s.cymbal(0.9, 2.0), b, 0, 0.25)
        run += 1
        for k in range(steps):
            accent = 1.0 if k % sub == 0 else (0.75 if k % sub == sub // 2 else 0.5)
            song.hit('drive', s.shaker(accent), b, k / sub, -0.25 if k % 2 else 0.25)
            if k % sub == sub // 2:
                song.hit('drive', s.hat(0.7 * accent), b, k / sub, 0.35)
        last = b + 1 >= song.bars or not on[b + 1]
        if run % 4 == 0 or last:
            fill = [190.0, 160.0, 130.0, 105.0, 90.0, 78.0]
            n = 4 if sub == 4 else 4
            for k in range(n):
                beat = song.beats - 1 + k / n
                song.hit('fill', s.tom(0.55 + 0.45 * k / (n - 1), fill[k]), b, beat, (k / (n - 1) - 0.5) * 0.6)


def active_rms(x):
    on = np.abs(x).sum(axis=0) > 1e-4
    return float(np.sqrt(np.mean(x[:, on] ** 2))) if on.any() else 0.0


def mixdown(song, overrides=None, ir_seconds=2.6, master_lp=11000.0, underwater=None):
    spec = {k: dict(v) for k, v in DEFAULT_BUS.items()}
    for k, v in (overrides or {}).items():
        spec.setdefault(k, {}).update(v)
    dry = np.zeros((2, song.n))
    send = np.zeros((2, song.n))
    for name, buf in song.buses.items():
        p = spec.get(name, dict(gain=1.0, verb=0.2, duck=0.0, hp=60))
        level = active_rms(buf)
        x = buf * (TARGET.get(name, 0.1) * p.get('gain', 1.0) / level if level > 0 else 0.0)
        if p.get('hp'):
            x = s.hp(x, p['hp'])
        if p.get('lp'):
            x = s.lp(x, p['lp'])
        if p.get('duck'):
            x = x * sidechain(song, p['duck'])[None, :]
        if p.get('delay'):
            x = s.delay(x, p['delay'], 0.32, 3)
        dry += x
        send += x * p.get('verb', 0.2)
    ir = s.make_ir(ir_seconds, 0.025, 3200.0, 0.4)
    wet = s.reverb(send, ir)[:, :song.n]
    master = dry + wet * 0.9
    master = s.hp(master, 30)
    master = s.lp(master, master_lp, order=1)
    # less boom below the bass, less mud in the low middle, a little more of the tunes (nothing shrill up top)
    master = s.peak(master, 50, 0.7, -3.0)
    master = s.peak(master, 280, 1.0, -2.0)
    master = s.peak(master, 1800, 0.8, 2.5)
    if underwater is not None:
        master = underwater(song, master)
    master = s.compress(master, 0.5, 3.0, 0.008, 0.15)
    # fold the tail back onto the start: the loop has no seam
    L = s.n_of(song.length)
    out = master[:, :L].copy()
    tail = master[:, L:]
    k = min(L, tail.shape[1])
    out[:, :k] += tail[:, :k]
    return s.limit(out, 0.92)


def write(stereo, path_wav):
    import wave
    x = np.clip(stereo.T, -1, 1)
    pcm = (x * 32767).astype('<i2')
    with wave.open(path_wav, 'wb') as w:
        w.setnchannels(2)
        w.setsampwidth(2)
        w.setframerate(s.SR)
        w.writeframes(pcm.tobytes())
