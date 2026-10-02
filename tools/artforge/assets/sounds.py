"""
Spell sounds, synthesised from code (numpy) and encoded to Ogg Vorbis with ffmpeg:

    python3 -m artforge sounds

Each element has a cast and an impact sound, built from the same few ingredients - shaped noise
(whoosh, roar, hiss), crackles, struck bells and FM tones - so the set sounds like one family:
fire roars and crackles, shock buzzes and snaps, frost chimes and shatters, holy and healing ring
like bells, shadow and arcane hum and swell. Outputs are mono (positional in game), 44.1 kHz.
"""
import os
import subprocess
import tempfile
import wave

import numpy as np

RATE = 44100


def _t(seconds):
    return np.arange(int(RATE * seconds)) / RATE


def env(n, attack, release, curve=3.0):
    """Attack/decay envelope over n samples: linear attack, then a curved fall to 0."""
    e = np.ones(n)
    a = max(1, int(attack * RATE))
    e[:a] = np.linspace(0, 1, a)
    r = max(1, n - a)
    e[a:] = (1 - np.linspace(0, 1, r)) ** curve
    if release < 1:
        pass
    return e


def band(x, lo, hi):
    """Band-pass by zeroing FFT bins outside lo..hi Hz (with soft edges)."""
    spec = np.fft.rfft(x)
    f = np.fft.rfftfreq(len(x), 1 / RATE)
    w = np.clip((f - lo * 0.7) / (lo * 0.3 + 1e-9), 0, 1) * np.clip((hi * 1.3 - f) / (hi * 0.3), 0, 1)
    return np.fft.irfft(spec * w, len(x))


def sweep_noise(rng, seconds, lo0, lo1, hi0, hi1, slices=24):
    """Noise whose band glides from (lo0, hi0) to (lo1, hi1): whooshes and roars."""
    n = int(RATE * seconds)
    out = np.zeros(n)
    win = np.hanning(2 * (n // slices) + 1)
    step = n // slices
    for i in range(slices):
        u = i / max(1, slices - 1)
        lo, hi = lo0 + (lo1 - lo0) * u, hi0 + (hi1 - hi0) * u
        chunk = band(rng.standard_normal(len(win)), lo, hi) * win
        s = i * step
        e = min(n, s + len(win))
        out[s:e] += chunk[:e - s]
    return out


def crackle(rng, seconds, rate_hz, lo=1500, hi=9000, decay=0.004):
    """Random short clicks (fire crackle, electric snaps)."""
    n = int(RATE * seconds)
    out = np.zeros(n)
    k = int(rate_hz * seconds)
    click = band(rng.standard_normal(int(0.012 * RATE)), lo, hi) * np.exp(-_t(0.012) / decay)
    for _ in range(k):
        s = rng.integers(0, max(1, n - len(click)))
        out[s:s + len(click)] += click * rng.uniform(0.3, 1.0)
    return out


def bell(freqs, seconds, decay, partials=((1, 1.0), (2.76, 0.4), (5.4, 0.2), (8.93, 0.1))):
    """Struck bell: inharmonic partials, exponential decay."""
    t = _t(seconds)
    out = np.zeros(len(t))
    for f in freqs:
        for ratio, amp in partials:
            out += amp * np.sin(2 * np.pi * f * ratio * t) * np.exp(-t / (decay / ratio ** 0.5))
    return out


def fm(seconds, f0, f1, index, mod_ratio=1.5):
    """FM tone gliding from f0 to f1 (arcane hums)."""
    t = _t(seconds)
    f = np.linspace(f0, f1, len(t))
    phase = 2 * np.pi * np.cumsum(f) / RATE
    return np.sin(phase + index * np.sin(phase * mod_ratio))


def reverb(x, mix=0.25, seconds=0.35):
    """A few feedback combs: a small stone room."""
    out = np.concatenate([x, np.zeros(int(seconds * RATE))])
    wet = np.zeros_like(out)
    for delay_ms, g in ((29.7, 0.75), (37.1, 0.72), (41.1, 0.7), (43.7, 0.68)):
        d = int(delay_ms / 1000 * RATE)
        buf = out.copy()
        for i in range(d, len(buf), d):
            buf[i:i + d] += g * buf[i - d:i][:len(buf[i:i + d])]
        wet += buf
    return (1 - mix) * out + mix * wet / 4


def fit(x, n):
    return np.concatenate([x, np.zeros(max(0, n - len(x)))])[:n] if len(x) != n else x


def mix(*parts):
    n = max(len(p) for p in parts)
    return sum(fit(p, n) for p in parts)


def normalize(x, peak=0.9):
    x = x - np.mean(x)
    m = np.max(np.abs(x)) or 1
    fade = min(len(x), int(0.02 * RATE))
    x[-fade:] *= np.linspace(1, 0, fade)
    return x / m * peak


# ----------------------------------------------------------------------------------- the sounds

def fire_cast(rng):
    d = 0.75
    roar = sweep_noise(rng, d, 120, 300, 900, 2600) * env(int(d * RATE), 0.08, 1, 2)
    return reverb(mix(roar, 0.5 * crackle(rng, d, 40) * env(int(d * RATE), 0.1, 1, 1.5)), 0.15)


def fire_impact(rng):
    d = 0.7
    burst = sweep_noise(rng, d, 300, 80, 4000, 900) * env(int(d * RATE), 0.005, 1, 4)
    return reverb(mix(burst, 0.6 * crackle(rng, d, 70) * env(int(d * RATE), 0.01, 1, 2)), 0.2)


def shock_cast(rng):
    d = 0.6
    t = _t(d)
    buzz = np.sign(np.sin(2 * np.pi * 110 * t + 3 * np.sin(2 * np.pi * 7 * t))) * 0.25
    buzz = band(buzz + 0.6 * rng.standard_normal(len(t)), 300, 6000)
    jitter = np.repeat(rng.uniform(0.2, 1.0, len(t) // 400 + 1), 400)[:len(t)]
    snaps = crackle(rng, d, 90, 2500, 12000, 0.002)
    return mix(buzz * jitter * env(len(t), 0.01, 1, 1.5), 1.4 * snaps * env(len(t), 0.0, 1, 1))


def shock_impact(rng):
    d = 0.8
    t = _t(d)
    crack = band(rng.standard_normal(len(t)), 800, 14000) * np.exp(-t / 0.03)
    rumble = band(rng.standard_normal(len(t)), 40, 300) * np.exp(-t / 0.25) * 0.8
    return reverb(mix(crack * 1.5, rumble, 0.6 * crackle(rng, 0.35, 60, 3000, 12000, 0.002)), 0.3, 0.5)


def frost_cast(rng):
    d = 0.8
    t = _t(d)
    hiss = band(rng.standard_normal(len(t)), 3000, 11000) * env(len(t), 0.15, 1, 2) * 0.35
    tones = np.zeros(len(t))
    for i in range(14):
        f = rng.uniform(2200, 5200)
        s = int(rng.uniform(0, 0.55) * RATE)
        seg = _t(0.25)
        tone = np.sin(2 * np.pi * f * seg) * np.exp(-seg / 0.06)
        tones[s:s + len(seg)] += tone[:len(tones) - s] * 0.35
    return reverb(mix(hiss, tones), 0.3, 0.5)


def frost_impact(rng):
    d = 0.7
    t = _t(d)
    shards = np.zeros(len(t))
    for i in range(30):
        f = rng.uniform(1800, 7000)
        s = int(rng.exponential(0.06) * RATE)
        seg = _t(0.15)
        if s >= len(t):
            continue
        tone = np.sin(2 * np.pi * f * seg) * np.exp(-seg / 0.025)
        shards[s:s + len(seg)] += tone[:len(shards) - s] * rng.uniform(0.2, 0.6)
    crunch = band(rng.standard_normal(len(t)), 1500, 9000) * np.exp(-t / 0.05)
    return reverb(mix(shards, crunch), 0.25, 0.4)


def holy_cast(rng):
    chord = bell([523.25, 659.25, 783.99, 1046.5], 2.6, 0.5)
    shimmer = band(rng.standard_normal(len(chord)), 5000, 12000) * env(len(chord), 0.2, 1, 2) * 0.15
    return reverb(mix(chord * 0.5, shimmer), 0.35, 0.6)


def holy_impact(rng):
    hit = bell([1046.5, 1318.5], 1.5, 0.3)
    return reverb(mix(hit * 0.6, crackle(rng, 0.3, 30, 4000, 12000, 0.003) * 0.4), 0.3, 0.5)


def heal_cast(rng):
    out = np.zeros(int(2.2 * RATE))
    for i, f in enumerate([392.0, 493.88, 587.33, 783.99]):
        b = bell([f], 1.8, 0.35, ((1, 1.0), (2.0, 0.3), (3.0, 0.1)))
        s = int(i * 0.09 * RATE)
        out[s:s + len(b)] += b[:len(out) - s] * 0.45
    return reverb(out, 0.4, 0.6)


def shadow_cast(rng):
    d = 1.0
    t = _t(d)
    whoosh = sweep_noise(rng, d, 600, 60, 3000, 400) * env(len(t), 0.25, 1, 2)
    drone = fm(d, 180, 70, 2.5, 0.5) * env(len(t), 0.2, 1, 2) * 0.4
    return reverb(mix(whoosh, drone), 0.45, 0.7)


def arcane_cast(rng):
    d = 0.9
    t = _t(d)
    tone = fm(d, 220, 660, 3.0) * env(len(t), 0.1, 1, 1.5) * 0.5
    tone2 = fm(d, 330, 990, 1.5, 2.01) * env(len(t), 0.2, 1, 1.5) * 0.3
    shimmer = band(rng.standard_normal(len(t)), 4000, 10000) * env(len(t), 0.3, 1, 2) * 0.15
    return reverb(mix(tone, tone2, shimmer), 0.4, 0.6)


def arcane_impact(rng):
    d = 0.6
    t = _t(d)
    pop = fm(d, 900, 200, 4.0) * np.exp(-t / 0.12) * 0.6
    return reverb(mix(pop, band(rng.standard_normal(len(t)), 2000, 9000) * np.exp(-t / 0.05) * 0.4), 0.35, 0.5)


def conjure(rng):
    d = 1.6
    t = _t(d)
    rise = sweep_noise(rng, d, 80, 900, 700, 6000) * env(len(t), 0.9, 1, 3)
    drone = fm(d, 90, 180, 4.0, 0.5) * env(len(t), 0.5, 1, 2) * 0.5
    return reverb(mix(rise, drone), 0.45, 0.8)


SOUNDS = {
    "fire_cast": fire_cast, "fire_impact": fire_impact,
    "shock_cast": shock_cast, "shock_impact": shock_impact,
    "frost_cast": frost_cast, "frost_impact": frost_impact,
    "holy_cast": holy_cast, "holy_impact": holy_impact,
    "heal_cast": heal_cast,
    "shadow_cast": shadow_cast,
    "arcane_cast": arcane_cast, "arcane_impact": arcane_impact,
    "conjure": conjure,
}


def write_ogg(samples, path):
    pcm = (np.clip(samples, -1, 1) * 32767).astype(np.int16)
    with tempfile.NamedTemporaryFile(suffix=".wav", delete=False) as tmp:
        wav_path = tmp.name
    with wave.open(wav_path, "wb") as w:
        w.setnchannels(1)
        w.setsampwidth(2)
        w.setframerate(RATE)
        w.writeframes(pcm.tobytes())
    os.makedirs(os.path.dirname(path), exist_ok=True)
    subprocess.run(["ffmpeg", "-y", "-loglevel", "error", "-i", wav_path, "-c:a", "libvorbis", "-q:a", "5", "-ac", "1",
                    "-map_metadata", "-1", "-fflags", "+bitexact", path], check=True)
    os.unlink(wav_path)


def build(out_dir):
    for i, (name, fn) in enumerate(sorted(SOUNDS.items())):
        rng = np.random.default_rng(1000 + i)
        write_ogg(normalize(fn(rng)), os.path.join(out_dir, name + ".ogg"))
    return sorted(SOUNDS)
