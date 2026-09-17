#version 150
#extension GL_ARB_explicit_attrib_location : enable

#moj_import <dragonminez:aura_color.glsl>

// Sparking shading: a translucent shell whose silhouette is cut into needles, plus a brighter fringe pass
// over the spikes.
//
// The band that draws the outline is damped where the shell faces up or down (vUp): without that the caps
// sit at grazing incidence from any side view and light up as solid plates, which is what used to close the
// silhouette into a rounded box.

in vec3 vNormal;
in vec3 vView;
in float vHeight;
in float vWave;
in float vUp;
in vec2 vDir;

uniform vec3 CoreColor;
uniform vec3 RimColor;
uniform float CoreAlpha;
uniform float RimAlpha;
uniform float RimPower;
uniform float RimThreshold;
uniform float BandStart;
uniform float SpikeBurstRate;
uniform float WaveSpeed;
uniform float Time;
uniform float Growth;
uniform float Alpha;
uniform float BackFace;
uniform float BloomIntensity;
uniform float BloomPass;
uniform float LayerPass;

layout(location = 0) out vec4 fragColor;

const float TAU = 6.28318530718;
const float STREAK_COLUMNS = 44.0;

const float FRINGE_MIN = 0.08;
const float FRINGE_FULL = 0.35;
const float FRINGE_ALPHA = 0.95;
const float FRINGE_BLOOM = 0.60;

const float SATURATION = 1.35;
const float EDGE_LIGHT = 0.55;
const float LIP_LIGHT = 0.60;

const float TONGUE_DEPTH = 0.45;
const float TONGUE_ROWS = 20.0;
// Must match CLIMB_RATE in aura_sparking.vsh: the needles cut into the outline band travel with the spikes,
// otherwise the band reads as a frozen texture under moving geometry.
const float CLIMB_RATE = 0.16;
const float TONGUE_FINE = 2.0;
const float TONGUE_HEAT = 0.55;
const float TONGUE_FLOOR = 0.55;
const float TONGUE_LIGHT_END = 0.60;
const float SPIKE_LIGHT_END = 0.30;
const float TIP_BRIGHT = 1.20;

// How far the shell has to tilt away from vertical before its band fades, and what is left on the caps.
const float CAP_START = 0.55;
const float CAP_END = 0.92;
const float CAP_FLOOR = 0.25;

const float INTERIOR_TINT = 0.25;

float hash11(float n) {
    return fract(sin(n * 91.3 + 17.1) * 43758.5453);
}

float needles(float y, float seed, float sharp) {
    float row = floor(y);
    float f = fract(y);
    float tip = 0.30 + 0.45 * hash11(row * 3.7 + seed);
    float tri = f < tip ? f / tip : (1.0 - f) / (1.0 - tip);

    // Same spread as the spike grid in the vertex stage: without it the outline stays evenly ragged while
    // the geometry over it swings between stubs and long accents.
    float t = Time * SpikeBurstRate * (0.55 + 1.0 * hash11(row + seed + 4.0)) + hash11(row + seed + 13.0) * 9.0;
    float k = floor(t);
    float life = fract(t);
    float burst = smoothstep(0.0, 0.15, life) * (1.0 - smoothstep(0.40, 1.0, life));
    float len = hash11(row * 7.1 + seed + k * 1.3);
    return pow(tri, sharp) * (0.12 + 0.88 * len) * (0.20 + 0.80 * burst);
}

float tongueField(float h, float angle) {
    float wobble = 2.0 * sin(angle * 2.0 + 1.3) + 1.3 * sin(angle * 3.0 - 0.7);
    float y = (h - Time * CLIMB_RATE * WaveSpeed) * TONGUE_ROWS + wobble;

    float coarse = needles(y, 1.0, 1.4);
    float fine = needles(y * TONGUE_FINE + 0.37, 7.0, 1.8) * 0.50;

    float envelope = 0.5 + 0.5 * sin(h * 7.0 - Time * 0.9 + 2.0 * sin(angle * 2.0));
    envelope *= 0.6 + 0.4 * sin(h * 3.1 + Time * 0.6 + angle * 3.0);
    envelope = mix(TONGUE_FLOOR, 1.0, clamp(envelope, 0.0, 1.0));

    return max(coarse, fine) * envelope;
}

void main(void) {
    vec3 N = normalize(vNormal);
    vec3 V = normalize(vView);

    float facingRaw = dot(V, N);
    if (facingRaw < 0.0) N = -N;

    float facing = abs(dot(V, N));
    float rim = 1.0 - facing;
    float angle = atan(vDir.y, vDir.x);

    float luma = dot(RimColor, vec3(0.299, 0.587, 0.114));
    vec3 tint = clamp(mix(vec3(luma), RimColor, SATURATION), 0.0, 1.5);

    if (LayerPass > 0.5) {
        vec3 fringeTint = clamp(tint, 0.0, 1.0);
        float fringe = smoothstep(FRINGE_MIN, FRINGE_FULL, vWave) * smoothstep(0.30, 0.65, rim);
        float fringeAlpha = RimAlpha * FRINGE_ALPHA * fringe * Alpha;
        if (facingRaw < 0.0) fringeAlpha *= max(BackFace, 0.45);
        if (fringeAlpha < 0.004) discard;
        vec4 glow = vec4(fringeTint, clamp(fringeAlpha * FRINGE_BLOOM * BloomIntensity, 0.0, 1.0));
        fragColor = BloomPass > 0.5 ? glow : vec4(fringeTint, fringeAlpha);
        return;
    }

    // The outline thins away as the aura dies instead of fading out flat.
    float bandStart = mix(1.0, BandStart, smoothstep(0.0, 0.7, Growth));
    float sideways = mix(CAP_FLOOR, 1.0, 1.0 - smoothstep(CAP_START, CAP_END, abs(vUp)));

    float tongue = tongueField(vHeight, angle);
    float threshold = bandStart - TONGUE_DEPTH * tongue;
    float edgeDist = rim - threshold;
    float edgeAa = max(fwidth(edgeDist), 1.0e-4);
    float band = smoothstep(-edgeAa, edgeAa, edgeDist) * sideways;
    float tongueBody = band * (1.0 - smoothstep(threshold, bandStart + 0.001, rim)) * step(0.02, tongue);

    float x = angle / TAU * STREAK_COLUMNS;
    float idx = mod(floor(x), STREAK_COLUMNS);
    float r = hash11(idx);
    float across = 1.0 - abs(fract(x) * 2.0 - 1.0);
    float line = smoothstep(0.62 + 0.30 * r, 1.0, across);
    float runner = fract(vHeight * (0.9 + r) - Time * (1.3 + 1.5 * r) + r * 11.0);
    float streak = line * smoothstep(0.0, 0.18, runner) * (1.0 - smoothstep(0.30, 0.62, runner));
    streak *= smoothstep(0.10, 0.35, vHeight) * sideways;

    vec3 tipCol = clamp(tint * TIP_BRIGHT, 0.0, 1.0);
    vec3 glowCol = auraHighlight(tint, EDGE_LIGHT);
    float rimGrad = clamp(pow(rim, RimPower) / RimThreshold, 0.0, 1.0);
    vec3 body = mix(CoreColor, tint, rimGrad);

    float hot = clamp(band + streak, 0.0, 1.0);
    vec3 color = mix(body, glowCol, hot);
    float lip = smoothstep(0.86, 1.0, rim) * sideways;
    color = mix(color, auraHighlight(color, 1.0), lip * LIP_LIGHT);

    float tongueAlong = clamp((bandStart - rim) / max(bandStart - threshold, 1.0e-3), 0.0, 1.0);
    vec3 tongueCol = mix(auraHighlight(tipCol, 1.0), tipCol, smoothstep(TONGUE_LIGHT_END, 1.0, tongueAlong));
    color = mix(color, tongueCol, tongueBody);

    vec3 spikeCol = mix(auraHighlight(tipCol, 1.0), tipCol, smoothstep(SPIKE_LIGHT_END, 1.0, vWave));
    color = mix(color, spikeCol, clamp(vWave * 2.0, 0.0, 1.0));
    color = auraKeepSaturation(color, RimColor);

    float alpha = mix(CoreAlpha, RimAlpha, band);
    alpha = max(alpha, RimAlpha * clamp(vWave * 1.1 + streak * 0.85, 0.0, 1.0));
    alpha *= 1.0 - 0.45 * smoothstep(0.95, 1.0, vHeight) * (1.0 - vWave);

    // A floor of coverage so the player and the background read through the shell tinted.
    alpha = max(alpha, INTERIOR_TINT * clamp(Growth, 0.0, 1.0));

    if (facingRaw < 0.0) alpha *= max(BackFace, 0.45);
    alpha *= Alpha;

    float glowWeight = clamp(hot + vWave + tongueBody * TONGUE_HEAT, 0.0, 1.0);
    vec4 glow = vec4(color, clamp(alpha * (0.35 + 0.65 * glowWeight) * BloomIntensity, 0.0, 1.0));
    fragColor = BloomPass > 0.5 ? glow : vec4(color, alpha);
}
