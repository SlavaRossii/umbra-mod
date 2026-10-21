#!/usr/bin/env python3
"""2.8: the boss music. Eight tracks, one for each boss, each its own tempo, metre, key and colour; all of them loop
(the tail of the last bar is folded onto the first). Everything is synthesised (tools/music/synth.py); the melodies
are original. Style is borrowed from film and series scoring, notes are not.

  Watcher            118, 4/4, D minor   pulsing synth-thriller arpeggios; the music freezes for a beat now and then
  Mimic Queen        6/8, E minor        a sly danse macabre: harpsichord, pizzicato, chest-lid snaps, a reed tune
  Shepherd           96, 4/4, C phryg.   a relentless low ostinato, taiko, bell tolls, horns and a duduk
  Doppelganger       164, 4/4, F# minor  a breakbeat duel: every phrase is answered by its mirror image, left and right
  Architect          7/8, A phrygian     clockwork: an organ ostinato in 2+2+3, ticks, anvils, braams, it builds up
  Primordial Shadow  152, 4/4, D->E min  the finale: string ostinato, taiko, choir, brass; it climbs a key
  Drowned Keeper     6/8, G minor        rolling waves of low strings, horns, timpani; it sinks under water and rises
  Fallen Keeper      124, 4/4, B minor   tragic and driving: piano ostinato, cello then violins, choir at the top

Run from the repo root: python3 tools/gen_music28.py [out_dir_for_mp3_previews] [only_this_track]
Writes src/main/resources/assets/umbra/sounds/music/boss_<id>.ogg."""
import os
import subprocess
import sys

import numpy as np

sys.path.insert(0, os.path.join(os.path.dirname(__file__), 'music'))
import synth as s  # noqa: E402
from arrange import Song, mel, mixdown, write  # noqa: E402

OUT = 'src/main/resources/assets/umbra/sounds/music/'


# ------------------------------------------------------------------ drum kit shorthands
def K(p=1.0, low=44.0):
    return lambda v: s.kick(v, p, low=low)


def SN(tail=0.16):
    return lambda v: s.snare(v, tail)


CLAP = s.clap
HAT = lambda v: s.hat(v)  # noqa: E731
OHAT = lambda v: s.hat(v, True)  # noqa: E731
SHAKE = s.shaker


def TAIKO(p=58.0):
    return lambda v: s.taiko(v, p)


def TOM(p=110.0):
    return lambda v: s.tom(v, p)


def mute(song, buses, bar, beat0, beat1):
    i, j = int(song.at(bar, beat0) * s.SR), int(song.at(bar, beat1) * s.SR)
    for b in buses:
        if b in song.buses:
            fade = np.linspace(1, 0, min(300, j - i))
            song.buses[b][:, i:i + len(fade)] *= fade
            song.buses[b][:, i + len(fade):j] = 0.0


# ================================================================== 1. the Watcher
def watcher():
    S = Song('watcher', 118, 4, 44)
    A = ['Dm', 'Dm', 'Bb', 'Bb', 'Gm', 'Gm', 'A', 'A']
    B = ['Dm', 'Bb', 'F', 'C', 'Dm', 'Bb', 'Gm', 'A']
    C = ['Gm', 'Dm', 'Eb', 'Bb', 'Gm', 'Dm', 'Eb', 'A']
    BR = ['Dm', 'Dm', 'Bb', 'A']
    plan = [(0, A, 'A'), (8, B, 'B'), (16, C, 'C'), (24, BR, 'BR'), (28, B, 'D'), (36, A, 'E')]
    theme = mel('A4:1 D5:1.5 C5:.5 A4:1 | F4:1 G4:.5 A4:.5 Bb4:2 | A4:1 C5:1.5 Bb4:.5 A4:1 | G4:3 E4:1 |'
                'A4:1 D5:1.5 E5:.5 F5:1 | E5:1 D5:1 C5:1 Bb4:1 | A4:1.5 G4:.5 Bb4:1 D5:1 | C#5:3 A4:1')
    counter = mel('D4:2 F4:2 | G4:4 | F4:2 Eb4:2 | D4:4 | D4:2 F4:2 | A4:4 | G4:2 Bb4:2 | A4:4')
    stare = mel('D4:1 F4:1 E4:2')
    for bar0, ch, kind in plan:
        n = len(ch)
        # the arpeggio that never stops looking
        if kind != 'BR':
            pat = [0, 1, 2, 3, 2, 1, 2, 1] if kind in ('A', 'B', 'E') else [0, 2, 1, 3, 1, 2, 0, 2]
            S.ostinato('arp', s.arp_synth, ch, bar0, pat, 0.25, 62, 76, 0.8, [1, .55, .75, .55], pan=-0.15 if kind != 'D' else 0.0)
        else:
            S.ostinato('arp', s.arp_synth, ch, bar0, [0, 2, 1, 2], 1.0, 62, 76, 0.55, length=0.6)
        # the bass pulse
        if kind in ('A', 'E'):
            S.bassline('bass', s.bass, ch, bar0, 'xoxoxoxo', 0.5, 2, 0.95)
        elif kind in ('B', 'C'):
            S.bassline('bass', s.bass, ch, bar0, 'x.xoxoxo', 0.5, 2, 1.0, octaves=[0, 0, 1, 0, 0, 0, 1, 0])
        elif kind == 'D':
            S.bassline('bass', s.bass, ch, bar0, 'xxoxxoxoxxoxxoxo', 0.25, 2, 1.0, cut=900)
        else:
            S.bassline('bass', s.bass, ch, bar0, 'x-------', 0.5, 2, 0.8)
        # pads and the themes
        if kind in ('B', 'C', 'BR', 'D'):
            S.pads('pad', s.strings_sus, ch, bar0, 57, 4, 0.7 if kind != 'D' else 0.85)
        if kind == 'B':
            S.melody('lead', s.brass, theme, bar0, 0, 0.85)
        if kind == 'D':
            S.melody('lead', s.brass, theme, bar0, 0, 1.0)
            S.melody('choir', s.choir, theme, bar0, -12, 0.8)
            S.pads('choir', s.choir, ch, bar0, 55, 3, 0.55)
        if kind == 'C':
            S.melody('lead', s.strings_sus, counter, bar0, -12, 0.95, attack=0.08)
            for b in (0, 4):
                S.melody('keys', s.bell, stare, bar0 + b, 0, 0.8)
        if kind == 'E':
            S.melody('lead', s.strings_sus, counter, bar0, 0, 0.6, attack=0.1)
        if kind == 'BR':
            for b in range(4):
                S.melody('keys', s.bell, stare, bar0 + b, 0, 0.9)
        # drums
        if kind in ('A', 'E'):
            S.drums(bar0, n, 0.25, {
                'k': ('x.....x.x.......', K(), 'drums', 0, True),
                's': ('....x.......x...', SN(), 'drums', 0, False),
                'c': ('....x.......x...', CLAP, 'drums', 0.1, False),
                'h': ('o.o.o.o.o.o.o.o.', HAT, 'perc', 0.3, False)})
        elif kind == 'B':
            S.drums(bar0, n, 0.25, {
                'k': ('x.....x.x.....x.', K(), 'drums', 0, True),
                's': ('....x.......x...', SN(0.2), 'drums', 0, False),
                'h': ('ogogogogogogogog', HAT, 'perc', 0.3, False)})
        elif kind == 'C':
            S.drums(bar0, n, 0.25, {
                'k': ('x.........x.....', K(1.2), 'drums', 0, True),
                's': ('........X.......', SN(0.3), 'drums', 0, False),
                't': (['x.......x.....o.', 'x.......x..o.o..'], TAIKO(62), 'perc', 0, False),
                'h': ('..o...o...o...o.', OHAT, 'perc', -0.3, False)})
        elif kind == 'D':
            S.drums(bar0, n, 0.25, {
                'k': ('x..x..x.x..x..x.', K(), 'drums', 0, True),
                's': ('....x.......x..o', SN(0.2), 'drums', 0, False),
                'c': ('....x.......x...', CLAP, 'drums', 0.1, False),
                'h': ('xgogxgogxgogxgog', HAT, 'perc', 0.3, False),
                't': ('x.......x.......', TAIKO(55), 'perc', 0, False)})
        else:
            S.drums(bar0, n, 0.25, {
                'k': ('x..x............', K(0.6), 'drums', 0, True),
                'h': ('g...g...g...g...', HAT, 'perc', 0.3, False)})
            S.roll('drums', SN(0.12), bar0 + 3, 2, 2, 4, 0.2, 1.0)
    # the eye freezes: the last beat of every fourth bar in the A parts falls silent
    for bar in (3, 7, 39, 43):
        mute(S, ['arp', 'bass', 'drums', 'perc', 'lead'], bar, 3.0, 4.0)
        S.hit('fx', s.reverse_cymbal(0.5, 0.8), bar, 3.0 + 0.0, 0.0)
    for b in (0, 8, 16, 28, 36):
        S.boom(b)
    S.rise(8, 2)
    S.rise(28, 4)
    S.rise(16, 1, 0.7)
    for b in (27,):
        for k in range(4):
            S.hit('perc', s.tom(0.6 + 0.1 * k, 140 - 18 * k), b, 2 + k * 0.5)
    return S, dict(arp=dict(gain=0.8, verb=0.25, duck=0.3, hp=150, delay=0.75 * 60 / 118))


# ================================================================== 2. the Mimic Queen
def mimic_queen():
    S = Song('mimic_queen', 240, 6, 52)      # 6/8: an eighth a beat, 0.25 s
    A = ['Em', 'Em', 'Am', 'Am', 'B7', 'B7', 'Em', 'Em']
    B = ['Em', 'C', 'Am', 'B', 'Em', 'C', 'F#dim', 'B7']
    C = ['G', 'D/F#', 'Em', 'B/D#', 'C', 'G/B', 'Am', 'B7']
    BR = ['Em', 'Em', 'Am', 'B7']
    plan = [(0, A, 'A'), (8, B, 'B'), (16, A, 'A2'), (24, C, 'C'), (32, BR, 'BR'), (36, B, 'D'), (44, A, 'E')]
    tune = mel('B4:1 C5:1 B4:1 A#4:1 B4:2 | G4:1 A4:1 G4:1 E4:3 | A4:1 B4:1 C5:1 E5:2 D#5:1 | F#4:3 B4:3 |'
               'B4:1 C5:1 B4:1 A#4:1 B4:1 E5:1 | E5:2 C5:1 G4:3 | A4:1 C5:1 D#5:1 E5:2 C5:1 | B4:3 r:1 A4:1 F#4:1')
    sly = mel('E4:2 D#4:1 E4:3 | F#4:2 E4:1 D4:3 | E4:2 G4:1 B4:3 | A4:2 G4:1 F#4:3 |'
              'G4:2 F#4:1 E4:3 | D4:2 E4:1 G4:3 | A4:1 B4:1 C5:1 A4:3 | B4:6')
    for bar0, ch, kind in plan:
        n = len(ch)
        hp_pat = [0, 2, 1, 2, 3, 2] if kind != 'C' else [0, 1, 2, 3, 2, 1]
        if kind != 'BR':
            S.ostinato('keys', s.harpsichord, ch, bar0, hp_pat, 1.0, 64, 79, 0.7, [1, .6, .7, .9, .6, .7], pan=0.25)
        else:
            S.ostinato('keys', s.harpsichord, ch, bar0, [0, None, 2, None, 1, None], 1.0, 64, 79, 0.55, pan=0.25)
        # oom-pa-pa: the bass on the dotted beats, the plucked chord between
        S.bassline('bass', s.pizz, ch, bar0, 'x..o..', 1.0, 2, 1.0)
        S.ostinato('ost', s.pizz, ch, bar0, [None, None, 1, None, None, 2], 1.0, 55, 67, 0.8, pan=-0.25)
        if kind in ('A2', 'D', 'E'):
            S.ostinato('ost', s.strings_stac, ch, bar0, [0, 0, 1, 0, 0, 1], 1.0, 40, 52, 0.75, [1, .5, .8, 1, .5, .8])
        if kind == 'B':
            S.melody('lead', s.reed, tune, bar0, 0, 0.85)
        if kind == 'D':
            S.melody('lead', s.reed, tune, bar0, 0, 0.9)
            S.melody('lead', s.brass, tune, bar0, -12, 0.75)
            S.pads('choir', s.choir, ch, bar0, 59, 3, 0.55, vowel='o')
        if kind == 'C':
            S.melody('lead', s.strings_sus, sly, bar0, 0, 0.8, attack=0.06)
            S.pads('pad', s.strings_sus, ch, bar0, 55, 3, 0.5)
        if kind == 'E':
            S.melody('lead', s.reed, sly, bar0, 0, 0.6)
        if kind == 'BR':
            S.melody('lead', s.reed, mel('B4:1 C5:1 B4:1 A#4:1 B4:2 | E4:6 | A4:1 C5:1 E5:1 D#5:3 | B4:6'), bar0, 0, 0.7)
        # drums and the snapping lids
        if kind in ('A', 'A2', 'E'):
            S.drums(bar0, n, 1.0, {
                't': ('x..o..', TAIKO(70), 'drums', 0, True),
                'sh': ('ogoogo', SHAKE, 'perc', 0.35, False),
                'w': (['......', '.....x'], lambda v: s.woodblock(v, 760), 'perc', -0.4, False)})
        elif kind in ('B', 'C'):
            S.drums(bar0, n, 1.0, {
                't': ('x..x..', TAIKO(66), 'drums', 0, True),
                's': ('...o..', SN(0.12), 'drums', 0, False),
                'sh': ('ogoogo', SHAKE, 'perc', 0.35, False),
                'w': (['..x...', '..x.xx'], lambda v: s.woodblock(v, 700), 'perc', -0.4, False)})
        elif kind == 'D':
            S.drums(bar0, n, 1.0, {
                't': ('x.ox..', TAIKO(62), 'drums', 0, True),
                's': ('...x..', SN(0.16), 'drums', 0, False),
                'c': ('...x..', CLAP, 'drums', 0.1, False),
                'sh': ('xgoxgo', SHAKE, 'perc', 0.35, False),
                'w': ('.....x', lambda v: s.woodblock(v, 700), 'perc', -0.4, False)})
        else:
            S.drums(bar0, n, 1.0, {
                'w': (['x.....', 'x..x..', 'x.x.x.', 'xxxxxx'], lambda v: s.woodblock(v * 0.8, 820), 'perc', -0.3, False)})
    for b in (0, 8, 24, 36):
        S.boom(b, 0, 0.9)
    S.rise(8, 2, 0.8)
    S.rise(36, 4)
    # a lid slams shut at the end of each phrase
    for b in (7, 15, 23, 31, 43, 51):
        S.hit('perc', s.tom(1.0, 95), b, 5)
        S.hit('perc', s.woodblock(1.0, 560), b, 5.5)
    return S, dict(keys=dict(gain=0.85, verb=0.22, duck=0.15, hp=140), bass=dict(gain=1.2, duck=0.2, lp=3000))


# ================================================================== 3. the Shepherd
def shepherd():
    S = Song('shepherd', 96, 4, 40)
    A = ['Cm', 'Cm', 'Db', 'Cm', 'Ab', 'Bb', 'Db', 'Cm']
    B = ['Cm', 'Ab', 'Fm', 'G', 'Cm', 'Ab', 'Db', 'G']
    C = ['Ab', 'Eb', 'Fm', 'Cm', 'Ab', 'Eb', 'Db', 'G']
    BR = ['Cm', 'Cm', 'Db', 'G']
    E = ['Cm', 'Db', 'Cm', 'G']
    plan = [(0, A, 'A'), (8, B, 'B'), (16, C, 'C'), (24, BR, 'BR'), (28, B, 'D'), (36, E, 'E')]
    horns = mel('C4:2 Eb4:1 G4:1 | Ab4:3 G4:.5 F4:.5 | Eb4:1 F4:1 Ab4:1 C5:1 | B4:3 G4:1 |'
                'C5:2 Bb4:1 G4:1 | Ab4:1 G4:1 F4:1 Eb4:1 | F4:2 Db4:1 Eb4:1 | D4:1 B3:1 G3:2')
    duduk = mel('G4:3 Ab4:1 | G4:2 F4:1 Eb4:1 | F4:3 Eb4:.5 Db4:.5 | C4:4 |'
                'Eb4:3 F4:1 | G4:2 Ab4:1 Bb4:1 | Ab4:2 G4:1 F4:1 | G4:4')
    for bar0, ch, kind in plan:
        n = len(ch)
        # the relentless low ostinato: root, root, root, minor third, root, root, flat second, root
        for i, c in enumerate(ch):
            from arrange import bass_of
            r = bass_of(c, 2)
            offs = [0, 0, 0, 3, 0, 0, 1, 0] if kind != 'C' else [0, 0, 3, 0, 7, 0, 3, 1]
            acc = [1, .55, .7, .85, 1, .55, .8, .6]
            if kind != 'BR':
                for k in range(8):
                    S.play('ost', s.strings_stac, r + offs[k], bar0 + i, k * 0.5, 0.45, 0.95 * acc[k], 0.0, 1.0, bright=1800)
                    S.play('ost', s.strings_stac, r + 12 + offs[k], bar0 + i, k * 0.5, 0.45, 0.5 * acc[k], 0.2, 1.0, bright=2200)
            S.play('bass', s.bass, r - 12 if r > 40 else r, bar0 + i, 0, 3.8, 0.8 if kind != 'BR' else 0.6)
        if kind in ('A', 'D'):
            for b in range(0, n, 2 if kind == 'A' else 1):
                S.play('keys', s.bell, 48, bar0 + b, 0, 4, 0.9)
        if kind in ('B', 'D', 'C'):
            S.pads('choir', s.choir, ch, bar0, 55, 3, 0.6 if kind != 'D' else 0.8)
        if kind == 'B':
            S.melody('lead', s.brass, horns, bar0, 0, 0.8)
        if kind == 'D':
            S.melody('lead', s.brass, horns, bar0, 0, 1.0)
            S.melody('lead', s.brass, horns, bar0, -12, 0.8)
        if kind == 'C':
            S.melody('lead', s.reed, duduk, bar0, -12, 0.85)
        if kind == 'BR':
            S.melody('lead', s.reed, mel('G4:3 Ab4:1 | G4:2 F4:1 Eb4:1 | F4:3 Eb4:.5 Db4:.5 | D4:4'), bar0, -12, 0.85)
            S.pads('pad', s.strings_sus, ch, bar0, 48, 3, 0.6, bright=1200)
            for b in range(4):
                S.play('keys', s.bell, 36, bar0 + b, 0, 4, 0.8)
        if kind == 'E':
            S.pads('choir', s.choir, ch, bar0, 55, 3, 0.5, vowel='u')
        # drums
        if kind == 'A':
            S.drums(bar0, n, 0.5, {
                't': (['x..xx...', 'x..x.xx.'], TAIKO(50), 'drums', 0, True),
                'tm': ('......o.', TOM(95), 'perc', -0.2, False)})
        elif kind in ('B', 'C'):
            S.drums(bar0, n, 0.5, {
                't': (['x..xx...', 'x..x.xx.'], TAIKO(50), 'drums', 0, True),
                's': ('..x...x.', SN(0.25), 'drums', 0, False),
                'tm': ('.o....o.', TOM(120), 'perc', -0.2, False)})
        elif kind == 'D':
            S.drums(bar0, n, 0.25, {
                't': (['x.....x.x.......', 'x.....x.x...x.x.'], TAIKO(48), 'drums', 0, True),
                's': ('....x..g....x.gg', SN(0.2), 'drums', 0, False),
                'r': ('g.g.g.g.g.g.g.g.', lambda v: s.snare(v * 0.5, 0.06), 'perc', 0.25, False),
                'tm': ('..........o.o...', TOM(140), 'perc', -0.2, False)})
        elif kind == 'E':
            S.drums(bar0, n, 0.5, {
                't': ('x..xx.x.', TAIKO(50), 'drums', 0, True),
                's': ('....x...', SN(0.25), 'drums', 0, False)})
        else:
            S.drums(bar0, n, 1.0, {'t': ('x...', TAIKO(46), 'drums', 0, True)})
            S.roll('perc', TOM(100), bar0 + 3, 2, 2, 4, 0.3, 1.0)
    for b in (0, 8, 16, 28, 36):
        S.boom(b)
    S.rise(28, 4)
    S.rise(8, 2, 0.7)
    for b in (4, 12, 32):
        S.hit('fx', s.braam(36, 2.0 * 60 / 96 * 2, 0.8), b, 0)
    return S, dict(ost=dict(gain=0.9, verb=0.2, duck=0.2, hp=60), choir=dict(gain=0.9, verb=0.6, duck=0.15, hp=120))


# ================================================================== 4. the Doppelganger
def doppelganger():
    S = Song('doppelganger', 164, 4, 48)
    A = ['F#m', 'F#m', 'D', 'E', 'F#m', 'F#m', 'Bm', 'C#']
    B = ['F#m', 'D', 'A', 'E', 'F#m', 'D', 'Bm', 'C#']
    C = ['D', 'E', 'C#m', 'F#m', 'Bm', 'D', 'E', 'C#7']
    E = ['F#m', 'F#m', 'D', 'E', 'Bm', 'D', 'E', 'C#']
    plan = [(0, A, 'A'), (8, B, 'B'), (16, C, 'C'), (24, A, 'A2'), (32, B, 'D'), (40, E, 'E')]
    call = mel('F#4:.5 A4:.5 C#5:1 B4:.5 A4:.5 G#4:1 | A4:.5 B4:.5 C#5:.5 E5:.5 C#5:2')
    scale = [6, 8, 9, 11, 1, 2, 4]   # F# minor

    def mirror(notes, axis=66):
        out = []
        for b, beat, m, d in notes:
            if m is None:
                out.append((b, beat, None, d))
                continue
            mm = 2 * axis - m
            while mm % 12 not in scale:
                mm -= 1
            out.append((b, beat, mm, d))
        return out
    answer = mirror(call)
    for bar0, ch, kind in plan:
        n = len(ch)
        # the mirrored ostinato: up on the left, down on the right
        if kind != 'C':
            S.ostinato('ost', s.strings_stac, ch, bar0, [0, 1, 2, 3, 4, 3, 2, 1], 0.25, 54, 73, 0.7, [1, .6, .8, .6], pan=-0.55)
            S.ostinato('ost', s.strings_stac, ch, bar0, [4, 3, 2, 1, 0, 1, 2, 3], 0.25, 54, 73, 0.7, [1, .6, .8, .6], pan=0.55)
        else:
            S.ostinato('ost', s.strings_stac, ch, bar0, [0, None, 2, None], 0.5, 54, 73, 0.8, pan=-0.4)
            S.ostinato('ost', s.strings_stac, ch, bar0, [None, 2, None, 0], 0.5, 54, 73, 0.8, pan=0.4)
        S.bassline('bass', s.bass, ch, bar0, 'x--.x-.x--.x-.x.' if kind != 'C' else 'x---.---x---.-x-', 0.25, 1, 1.0, reese=True,
                   cut=600)
        if kind in ('B', 'D'):
            # the call from the left, then its mirror image from the right
            for k in range(0, n, 4):
                S.melody('lead', s.brass if kind == 'D' else s.arp_synth, call, bar0 + k, 0, 0.9, pan=-0.45)
                S.melody('lead', s.reed if kind == 'B' else s.brass, answer, bar0 + k + 2, 0, 0.85, pan=0.45)
        if kind in ('C', 'D'):
            from arrange import voicing
            S.pads('pad', s.strings_sus, ch, bar0, 59, 4, 0.7)
            for i in range(n):
                for beat in (1.5, 3.5):
                    for m in voicing(ch[i], 52, 3):
                        S.play('lead', s.brass, m, bar0 + i, beat, 0.4, 0.8, 0.0, 0.8)
        if kind == 'D':
            S.pads('choir', s.choir, ch, bar0, 57, 3, 0.55)
        if kind == 'E':
            for k in (0, 4):
                S.melody('lead', s.arp_synth, call, bar0 + k, 0, 0.7, pan=-0.45)
                S.melody('lead', s.arp_synth, answer, bar0 + k + 2, 0, 0.7, pan=0.45)
        # a two-step breakbeat
        if kind in ('A', 'A2', 'B', 'E', 'D'):
            S.drums(bar0, n, 0.25, {
                'k': (['x.........x.....', 'x.........x..x..'], K(1.1), 'drums', 0, True),
                's': ('....x..g.g..x..g' if kind != 'D' else '....x..g.g..x.gx', SN(0.14), 'drums', 0, False),
                'h': ('o.o.o.o.o.o.o.o.', HAT, 'perc', 0.3, False),
                'sh': ('.g.g.g.g.g.g.g.g', SHAKE, 'perc', -0.3, False)})
        else:
            S.drums(bar0, n, 0.25, {
                'k': ('x.......x.......', K(1.2), 'drums', 0, True),
                's': ('........X.......', SN(0.3), 'drums', 0, False),
                't': ('x.....o.x.....o.', TAIKO(60), 'perc', 0, False),
                'h': ('..o...o...o...o.', OHAT, 'perc', 0.3, False)})
    for b in (0, 8, 16, 24, 32, 40):
        S.boom(b, 0, 0.9)
    S.rise(16, 2)
    S.rise(32, 4)
    for b in (15, 31, 47):
        S.roll('drums', SN(0.1), b, 3, 1, 8, 0.3, 1.0)
    return S, dict(ost=dict(gain=0.8, verb=0.2, duck=0.35, hp=150), lead=dict(gain=0.95, verb=0.25, duck=0.15, hp=150, delay=0.375 * 60 / 164))


# ================================================================== 5. the Architect
def architect():
    S = Song('architect', 300, 7, 52)      # 7/8: an eighth a beat, 0.2 s; grouped 2+2+3
    A = ['Am', 'Am', 'F', 'Bb', 'Am', 'Am', 'Dm', 'E']
    B = ['Am', 'F', 'C', 'G', 'Am', 'F', 'Bb', 'E']
    C = ['Dm', 'Bb', 'F', 'C', 'Dm', 'Bb', 'Gm', 'A']
    BR = ['Am', 'Am', 'Bb', 'E']
    plan = [(0, A, 'A1'), (8, A, 'A2'), (16, B, 'B'), (24, C, 'C'), (32, BR, 'BR'), (36, B, 'D'), (44, A, 'E')]
    theme = mel('A4:2 C5:2 E5:3 | F5:2 E5:2 C5:3 | G4:2 C5:2 E5:3 | D5:4 B4:3 |'
                'A4:2 C5:2 E5:3 | F5:3 E5:2 D5:2 | D5:2 C5:2 Bb4:3 | G#4:4 B4:3')
    hymn = mel('D4:4 F4:3 | Bb4:4 A4:3 | A4:4 G4:3 | E4:7 | D4:4 F4:3 | Bb4:4 D5:3 | C5:4 Bb4:3 | A4:7')
    for bar0, ch, kind in plan:
        n = len(ch)
        # the organ: 2 + 2 + 3
        S.ostinato('keys', s.organ, ch, bar0, [0, 2, 0, 2, 0, 3, 2], 1.0, 57, 70, 0.85 if kind != 'BR' else 0.6, [1, .7, .9, .7, 1, .8, .7])
        # the clockwork
        for i in range(n):
            for k in range(7):
                acc = 1.0 if k in (0, 2, 4) else 0.45
                S.hit('perc', s.metal(0.25 * acc, 1250 if k in (0, 2, 4) else 1100, 0.18), bar0 + i, k, 0.35 if k % 2 else -0.35)
        if kind != 'A1':
            S.bassline('bass', s.bass, ch, bar0, 'x-x-x--', 1.0, 2, 0.95)
        if kind in ('A2', 'D', 'E', 'C'):
            S.drums(bar0, n, 1.0, {
                't': ('x.x.x..' if kind != 'D' else 'x.x.xoo', TAIKO(54), 'drums', 0, True),
                'a': (['....x..', '.......'], lambda v: s.metal(v * 0.9, 330, 0.9), 'perc', 0.2, False)})
        if kind == 'B':
            S.drums(bar0, n, 1.0, {
                'k': ('x...x..', K(), 'drums', 0, True),
                's': ('..x...x', SN(0.2), 'drums', 0, False),
                't': ('x.x.x..', TAIKO(54), 'perc', 0, False)})
        if kind == 'D':
            S.drums(bar0, n, 1.0, {
                'k': ('x...x..', K(), 'drums', 0, True),
                's': ('..x...x', SN(0.2), 'drums', 0, False),
                'c': ('..x...x', CLAP, 'drums', 0.1, False)})
        if kind in ('B', 'D'):
            S.pads('pad', s.strings_sus, ch, bar0, 57, 4, 0.7)
            S.melody('lead', s.brass, theme, bar0, 0, 0.9 if kind == 'D' else 0.8)
        if kind == 'D':
            S.melody('choir', s.choir, theme, bar0, -12, 0.8)
        if kind == 'C':
            S.pads('choir', s.choir, ch, bar0, 55, 3, 0.7)
            S.melody('lead', s.brass, hymn, bar0, -12, 0.85)
        if kind in ('A2', 'E'):
            for b in range(0, n, 4):
                S.hit('fx', s.braam(33, 7 * 0.2 * 2, 0.75), bar0 + b, 0)
        if kind == 'BR':
            S.hit('fx', s.braam(33, 7 * 0.2 * 3, 0.9), bar0, 0)
            S.melody('lead', s.organ, mel('E5:7 | F5:7 | D5:7 | E5:7'), bar0, -12, 0.7)
    for b in (0, 8, 16, 24, 36, 44):
        S.boom(b, 0, 0.85)
    S.rise(16, 2)
    S.rise(36, 4)
    return S, dict(keys=dict(gain=0.9, verb=0.4, duck=0.2, hp=80), perc=dict(gain=0.75, verb=0.25, duck=0.0, hp=200))


# ================================================================== 6. the Primordial Shadow
def primordial():
    S = Song('primordial_shadow', 152, 4, 52)
    A = ['Dm', 'Dm', 'Bb', 'Bb', 'Gm', 'Gm', 'A', 'A']
    B = ['Dm', 'Bb', 'Gm', 'A', 'Dm', 'Bb', 'C', 'A']
    C = ['Bb', 'F', 'Gm', 'Dm', 'Bb', 'F', 'Gm', 'A']
    BR = ['Dm', 'Bb', 'Gm', 'A']
    D = ['Em', 'C', 'Am', 'B', 'Em', 'C', 'D', 'B']
    E = ['Dm', 'Bb', 'Gm', 'A', 'Dm', 'Bb', 'Gm', 'A']
    plan = [(0, A, 'A'), (8, B, 'B'), (16, C, 'C'), (24, BR, 'BR'), (28, D, 'D'), (36, E, 'E'), (44, A, 'F')]
    theme = mel('D4:1 A4:1 F4:.5 G4:.5 A4:1 | Bb4:2 A4:1 G4:1 | G4:1 Bb4:1 D5:1.5 C5:.5 | C#5:2 A4:2 |'
                'D5:1.5 C5:.5 A4:1 F4:1 | G4:1 A4:1 Bb4:1 D5:1 | E5:1.5 D5:.5 C5:1 G4:1 | A4:3 r:1')
    ost = [0, 0, 3, 0, 1, 0, 2, 0, 0, 0, 3, 0, 1, 2, 3, 1]
    for bar0, ch, kind in plan:
        n = len(ch)
        lo, hi = (50, 62) if kind != 'D' else (52, 64)
        if kind in ('A', 'B', 'D', 'E', 'F'):
            S.ostinato('ost', s.strings_stac, ch, bar0, ost, 0.25, lo, hi, 0.85, [1, .55, .8, .55], pan=-0.3)
            S.ostinato('ost', s.strings_stac, ch, bar0, ost, 0.25, lo + 12, hi + 12, 0.5, [1, .55, .8, .55], pan=0.3)
            S.bassline('bass', s.bass, ch, bar0, 'x.xx.xx.x.xx.xxx', 0.25, 2, 1.0, cut=800)
        if kind == 'C':
            S.ostinato('ost', s.strings_stac, ch, bar0, [0, None, None, 0, None, None, 1, None], 0.5, lo, hi, 0.9)
            S.bassline('bass', s.bass, ch, bar0, 'x-----x-', 0.5, 2, 1.0)
            S.pads('choir', s.choir, ch, bar0, 57, 4, 0.85)
        if kind in ('B', 'D'):
            S.pads('pad', s.strings_sus, ch, bar0, 57 if kind == 'B' else 59, 4, 0.75)
            S.melody('lead', s.brass, theme, bar0, 0 if kind == 'B' else 2, 0.9 if kind == 'B' else 1.0)
        if kind == 'D':
            S.melody('choir', s.choir, theme, bar0, -10, 0.9)
            S.pads('choir', s.choir, ch, bar0, 52, 3, 0.55)
        if kind == 'E':
            S.melody('lead', s.strings_sus, mel('A4:4 | Bb4:4 | D5:2 C5:2 | C#5:4 | A4:4 | Bb4:2 D5:2 | E5:2 D5:2 | C#5:4'), bar0, 0, 0.8,
                     attack=0.05)
            for b in (0, 4):
                S.hit('fx', s.braam(26, 4 * 60 / 152 * 2, 0.8), bar0 + b, 0)
        if kind == 'F':
            S.pads('choir', s.choir, ch, bar0, 55, 3, 0.6, vowel='o')
        if kind == 'BR':
            S.melody('lead', s.strings_sus, mel('D4:4 | F4:2 E4:2 | D4:2 Bb3:2 | C#4:4'), bar0, -12, 0.95, attack=0.15)
            S.pads('pad', s.strings_sus, ch, bar0, 50, 3, 0.55, bright=1100)
        # drums
        if kind in ('A', 'F'):
            S.drums(bar0, n, 0.25, {
                'k': ('x..x..x...x..x..', K(), 'drums', 0, True),
                's': ('....x.......x...', SN(0.18), 'drums', 0, False),
                'h': ('ogogogogogogogog', HAT, 'perc', 0.3, False),
                't': ('x.......x.......', TAIKO(56), 'perc', 0, False)})
        elif kind in ('B', 'E'):
            S.drums(bar0, n, 0.25, {
                'k': ('x..x..x...x..x..', K(), 'drums', 0, True),
                's': ('....x.......x..g', SN(0.2), 'drums', 0, False),
                'c': ('....x.......x...', CLAP, 'drums', 0.1, False),
                'h': ('xgogxgogxgogxgog', HAT, 'perc', 0.3, False),
                't': ('x.....x.x.......', TAIKO(52), 'perc', 0, False)})
        elif kind == 'C':
            S.drums(bar0, n, 0.25, {
                'k': ('x.........x.....', K(1.2), 'drums', 0, True),
                's': ('........X.......', SN(0.35), 'drums', 0, False),
                't': (['x.......x.....o.', 'x.......x..o.o..'], TAIKO(48), 'perc', 0, False),
                'h': ('..o...o...o...o.', OHAT, 'perc', 0.3, False)})
        elif kind == 'D':
            S.drums(bar0, n, 0.25, {
                'k': ('x.xx..x.x.xx..x.', K(), 'drums', 0, True),
                's': ('....x.......x.gg', SN(0.2), 'drums', 0, False),
                'c': ('....x.......x...', CLAP, 'drums', 0.1, False),
                'h': ('xgxgxgxgxgxgxgxg', HAT, 'perc', 0.3, False),
                't': ('x...x...x...x...', TAIKO(50), 'perc', 0, False)})
        else:
            S.drums(bar0, n, 1.0, {'t': ('x...', TAIKO(46), 'drums', 0, True)})
            S.roll('drums', SN(0.1), bar0 + 3, 0, 4, 8, 0.15, 1.0)
        for b in range(1, n, 2):
            if kind in ('A', 'B', 'D', 'E', 'F'):
                S.roll('perc', HAT, bar0 + b, 3.5, 0.5, 8, 0.3, 0.8, 0.3)   # a trap roll at every other bar's end
    for b in (0, 8, 16, 28, 36, 44):
        S.boom(b)
    S.rise(8, 2)
    S.rise(28, 4, 1.1)
    S.rise(44, 2, 0.8)
    S.hit('fx', s.braam(26, 4 * 60 / 152 * 4, 1.0), 16, 0)
    return S, dict(ost=dict(gain=0.85, verb=0.2, duck=0.4, hp=110))


# ================================================================== 7. the Drowned Keeper
def drowned():
    S = Song('drowned_keeper', 216, 6, 44)   # 6/8: an eighth a beat
    A = ['Gm', 'Gm', 'Eb', 'D', 'Gm', 'Gm', 'Cm', 'D']
    B = ['Gm', 'Eb', 'Bb', 'F', 'Gm', 'Eb', 'Cm', 'D']
    C = ['Eb', 'Bb', 'Cm', 'Gm', 'Eb', 'Bb', 'Am7b5', 'D']
    BR = ['Gm', 'Gm', 'Eb', 'D']
    plan = [(0, A, 'A'), (8, B, 'B'), (16, C, 'C'), (24, BR, 'BR'), (28, B, 'D'), (36, A, 'E')]
    horns = mel('G4:3 Bb4:2 A4:1 | Bb4:2 G4:1 Eb4:3 | F4:1 G4:1 Bb4:1 D5:3 | C5:3 A4:3 |'
                'G4:2 D5:1 D5:2 C5:1 | Bb4:2 A4:1 G4:3 | Eb5:2 D5:1 C5:2 Bb4:1 | A4:3 F#4:3')
    tide = mel('D4:6 | Eb4:3 D4:3 | C4:6 | D4:6 | Eb4:3 F4:3 | D4:6 | Eb4:3 D4:3 | C4:3 D4:3')
    for bar0, ch, kind in plan:
        n = len(ch)
        if kind != 'BR':
            S.ostinato('ost', s.strings_stac, ch, bar0, [0, 1, 2, 3, 2, 1], 1.0, 43, 58, 0.9, [1, .6, .75, .95, .6, .75], length=1.3, bright=1600)
            S.ostinato('ost', s.strings_stac, ch, bar0, [3, 2, 1, 0, 1, 2], 1.0, 55, 67, 0.45, pan=0.35, length=1.2)
        else:
            S.ostinato('ost', s.strings_stac, ch, bar0, [0, None, None, 2, None, None], 1.0, 43, 58, 0.7, length=2.5, bright=1200)
        S.bassline('bass', s.bass, ch, bar0, 'x--o--', 1.0, 1, 0.95, cut=500)
        if kind in ('B', 'D'):
            S.melody('lead', s.brass, horns, bar0, 0, 0.9 if kind == 'B' else 1.0)
            S.pads('pad', s.strings_sus, ch, bar0, 55, 4, 0.7)
        if kind == 'D':
            S.melody('lead', s.brass, horns, bar0, -12, 0.85)
            S.pads('choir', s.choir, ch, bar0, 55, 3, 0.75, vowel='o')
        if kind == 'C':
            S.pads('choir', s.choir, ch, bar0, 55, 3, 0.7, vowel='u')
            S.melody('lead', s.strings_sus, tide, bar0, 12, 0.75, attack=0.12)
        if kind in ('E', 'BR'):
            for b in range(0, n, 2):
                S.play('keys', s.bell, 43, bar0 + b, 0, 12, 0.8)
        if kind == 'E':
            S.melody('lead', s.strings_sus, tide, bar0, 12, 0.6, attack=0.15)
        # drums: timpani and waves
        if kind in ('A', 'E'):
            S.drums(bar0, n, 1.0, {
                't': ('x..o..', TAIKO(46), 'drums', 0, True),
                'tm': (['......', '....oo'], TOM(90), 'perc', 0.2, False)})
        elif kind in ('B', 'C'):
            S.drums(bar0, n, 1.0, {
                't': ('x..x..', TAIKO(46), 'drums', 0, True),
                's': ('...o..', SN(0.3), 'drums', 0, False),
                'tm': (['......', '...ooo'], TOM(100), 'perc', 0.2, False)})
        elif kind == 'D':
            S.drums(bar0, n, 1.0, {
                't': ('x.ox.o', TAIKO(44), 'drums', 0, True),
                's': ('...x..', SN(0.25), 'drums', 0, False),
                'c': ('...x..', CLAP, 'drums', 0.1, False),
                'sh': ('ogoogo', SHAKE, 'perc', 0.3, False)})
        if kind != 'BR':
            for b in range(0, n, 2):
                S.hit('fx', s.reverse_cymbal(1.6, 0.6), bar0 + b, 6 - 1.6 / (60 / 216), 0.0)
    for b in (0, 8, 16, 28, 36):
        S.boom(b, 0, 0.95)
    S.rise(28, 2, 1.0)

    def underwater(song, master):
        i0, i1, i2 = (int(song.at(b) * s.SR) for b in (24, 27, 28))
        cut = np.full(master.shape[1], 16000.0)
        cut[i0:i1] = 450.0
        cut[i1:i2] = np.geomspace(450.0, 16000.0, i2 - i1)
        x = master.copy()
        seg = slice(max(0, i0 - 2000), min(master.shape[1], i2 + 2000))
        for ch in range(2):
            x[ch, seg] = s.sweep_lp(master[ch, seg], cut[seg], 512, 2)
        return x
    return S, dict(ost=dict(gain=0.9, verb=0.25, duck=0.25, hp=50), underwater=underwater)


# ================================================================== 8. the Fallen Keeper
def fallen():
    S = Song('fallen_keeper', 124, 4, 44)
    A = ['Bm', 'G', 'D', 'A', 'Bm', 'G', 'Em', 'F#']
    C = ['G', 'A', 'F#m', 'Bm', 'G', 'A', 'Em', 'F#']
    BR = ['Bm', 'G', 'Em', 'F#']
    plan = [(0, A, 'A'), (8, A, 'B'), (16, C, 'C'), (24, BR, 'BR'), (28, A, 'D'), (36, C, 'E')]
    cello = mel('F#4:1.5 D4:.5 B3:2 | G4:1.5 F#4:.5 E4:1 D4:1 | F#4:1.5 A4:.5 D5:2 | C#5:1.5 B4:.5 A4:2 |'
                'B4:1 A4:.5 F#4:.5 D4:1 F#4:1 | G4:1 B4:1 E5:2 | D5:1 C#5:1 B4:1 G4:1 | A#4:3 F#4:1')
    lift = mel('D5:2 C#5:2 | E5:2 C#5:2 | A4:2 C#5:2 | D5:4 | B4:2 D5:2 | E5:2 C#5:2 | B4:2 G4:2 | A#4:4')
    lamp = mel('F#4:1 B4:1 A4:2')
    for bar0, ch, kind in plan:
        n = len(ch)
        S.ostinato('keys', s.piano, ch, bar0, [0, 2, 1, 2, 0, 2, 1, 2] if kind != 'E' else [0, 2, 1, 3, 2, 1, 2, 3],
                   0.5, 59, 74, 0.75 if kind in ('A', 'BR') else 0.85, [1, .65, .8, .65])
        S.bassline('bass', s.bass, ch, bar0, 'x-------' if kind in ('A', 'BR') else 'x--x-xo-', 0.5, 2, 0.85, cut=600)
        if kind == 'B':
            S.melody('lead', s.strings_sus, cello, bar0, -12, 0.95, attack=0.08)
            S.pads('pad', s.strings_sus, ch, bar0, 62, 4, 0.55)
        if kind == 'C':
            S.melody('lead', s.strings_sus, lift, bar0, -12, 0.9, attack=0.08)
            S.pads('choir', s.choir, ch, bar0, 57, 3, 0.6, vowel='o')
        if kind == 'D':
            S.melody('lead', s.strings_sus, cello, bar0, 0, 0.9, attack=0.06)
            S.melody('lead', s.strings_sus, cello, bar0, -12, 0.8, attack=0.06)
            S.melody('lead', s.brass, cello, bar0, -12, 0.55)
            S.pads('choir', s.choir, ch, bar0, 57, 3, 0.7)
        if kind == 'E':
            S.melody('lead', s.strings_sus, lift, bar0, 0, 0.85, attack=0.06)
            S.melody('lead', s.brass, lift, bar0, -12, 0.6)
            S.pads('pad', s.strings_sus, ch, bar0, 57, 4, 0.7)
        if kind == 'BR':
            for b in range(4):
                S.melody('keys', s.bell, lamp, bar0 + b, 0, 0.75)
            S.pads('pad', s.strings_sus, ch, bar0, 57, 3, 0.5, bright=1300)
        if kind == 'A':
            S.drums(bar0, n, 0.5, {
                'k': ('x.......', K(0.8), 'drums', 0, True),
                'tm': (['....o...', '....o.o.'], TOM(100), 'perc', 0.2, False),
                'sh': ('.g.g.g.g', SHAKE, 'perc', -0.3, False)})
        elif kind in ('B', 'C'):
            S.drums(bar0, n, 0.25, {
                'k': ('x.......x.x.....', K(), 'drums', 0, True),
                's': ('....x.......x...', SN(0.2), 'drums', 0, False),
                'h': ('o.o.o.o.o.o.o.o.', HAT, 'perc', 0.3, False)})
        elif kind in ('D', 'E'):
            S.drums(bar0, n, 0.25, {
                'k': ('x.....x.x.x.....', K(), 'drums', 0, True),
                's': ('....x.......x..g', SN(0.22), 'drums', 0, False),
                'c': ('....x.......x...', CLAP, 'drums', 0.1, False),
                'h': ('xgogxgogxgogxgog', HAT, 'perc', 0.3, False),
                't': ('x.......x.......', TAIKO(56), 'perc', 0, False)})
        else:
            S.roll('perc', TOM(110), bar0 + 3, 2, 2, 4, 0.3, 1.0)
    for b in (0, 8, 16, 28, 36):
        S.boom(b, 0, 0.85)
    S.rise(28, 4)
    S.rise(16, 2, 0.7)
    return S, dict(keys=dict(gain=1.0, verb=0.35, duck=0.15, hp=70))


TRACKS = {'watcher': watcher, 'mimic_queen': mimic_queen, 'shepherd': shepherd, 'doppelganger': doppelganger,
          'architect': architect, 'primordial_shadow': primordial, 'drowned_keeper': drowned, 'fallen_keeper': fallen}


def render(name, preview_dir=None):
    song, over = TRACKS[name]()
    under = over.pop('underwater', None)
    out = mixdown(song, over, underwater=under)
    os.makedirs(OUT, exist_ok=True)
    wav = '/tmp/umbra_%s.wav' % name
    write(out, wav)
    ogg = OUT + 'boss_%s.ogg' % name
    subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav, '-c:a', 'libvorbis', '-q:a', '3', ogg], check=True)
    if preview_dir:
        os.makedirs(preview_dir, exist_ok=True)
        subprocess.run(['ffmpeg', '-y', '-loglevel', 'error', '-i', wav, '-c:a', 'libmp3lame', '-b:a', '192k',
                        os.path.join(preview_dir, 'boss_%s.mp3' % name)], check=True)
    rms = float(np.sqrt(np.mean(out ** 2)))
    print('%-18s %5.1f s  rms %.3f  peak %.3f  %s' % (name, song.length, rms, float(np.max(np.abs(out))), ogg))
    return out


if __name__ == '__main__':
    prev = sys.argv[1] if len(sys.argv) > 1 else None
    only = sys.argv[2] if len(sys.argv) > 2 else None
    for n in TRACKS:
        if only and n != only:
            continue
        render(n, prev)
