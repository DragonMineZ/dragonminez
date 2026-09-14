#version 150

in vec3 vNormalWorld;
in vec3 v_viewDir;
in float vHeight;
in float vWave;
in vec2 vDir;

uniform vec3 color1;
uniform vec3 color2;
uniform float alp1;
uniform float alp2;
uniform float power;
uniform float divis;
uniform float time;
uniform float backFace;
uniform float bloomMode;
uniform float layerPass;

out vec4 fragColor;

const float COLOR_SPIKE_MIN   = 0.08;
const float COLOR_SPIKE_FULL  = 0.35;
const float COLOR_SPIKE_ALPHA = 0.95;
const float COLOR_SPIKE_BLOOM = 0.60;

const float TAU = 6.28318530718;
const float STREAK_COLUMNS = 44.0;

const float INTERIOR_ALPHA = 0.40;
const float INTERIOR_SATURATION = 1.35;
const float EDGE_WHITE = 0.55;
const float LIP_WHITE = 0.60;

const float BAND_START   = 0.50;
const float TONGUE_DEPTH = 0.45;
const float TONGUE_ROWS    = 20.0;
const float TONGUE_FINE    = 2.0;
const float TONGUE_BURSTS  = 3.0;
const float TONGUE_RISE    = 1.5;
const float TONGUE_HEAT    = 0.55;
const float TONGUE_FLOOR   = 0.55;

const float TONGUE_WHITE_END = 0.60;
const float SPIKE_WHITE_END  = 0.30;
const float TIP_BRIGHT       = 1.20;

float hash11(float n) {
    return fract(sin(n * 91.3 + 17.1) * 43758.5453);
}

float needles(float y, float seed, float sharp) {
    float row = floor(y);
    float f = fract(y);
    float tip = 0.30 + 0.45 * hash11(row * 3.7 + seed);
    float tri = f < tip ? f / tip : (1.0 - f) / (1.0 - tip);

    float t = time * TONGUE_BURSTS * (0.7 + 0.6 * hash11(row + seed + 4.0)) + hash11(row + seed + 13.0) * 9.0;
    float k = floor(t);
    float life = fract(t);
    float burst = smoothstep(0.0, 0.15, life) * (1.0 - smoothstep(0.40, 1.0, life));
    float len = hash11(row * 7.1 + seed + k * 1.3);
    return pow(tri, sharp) * (0.30 + 0.70 * len) * (0.35 + 0.65 * burst);
}

float tongueField(float h, float angle) {
    float wobble = 2.0 * sin(angle * 2.0 + 1.3) + 1.3 * sin(angle * 3.0 - 0.7);
    float y = h * TONGUE_ROWS - time * TONGUE_RISE + wobble;

    float coarse = needles(y, 1.0, 1.4);
    float fine = needles(y * TONGUE_FINE + 0.37, 7.0, 1.8) * 0.50;

    float envelope = 0.5 + 0.5 * sin(h * 7.0 - time * 0.9 + 2.0 * sin(angle * 2.0));
    envelope *= 0.6 + 0.4 * sin(h * 3.1 + time * 0.6 + angle * 3.0);
    envelope = mix(TONGUE_FLOOR, 1.0, clamp(envelope, 0.0, 1.0));

    return max(coarse, fine) * envelope;
}

void main(void) {
    vec3 N = normalize(vNormalWorld);
    vec3 V = normalize(v_viewDir);

    float facingRaw = dot(V, N);
    if (facingRaw < 0.0) N = -N;

    float facing = abs(dot(V, N));
    float rim = 1.0 - facing;
    float angle = atan(vDir.y, vDir.x);

    if (layerPass > 0.5) {
        float lumaC = dot(color2, vec3(0.299, 0.587, 0.114));
        vec3 spikeTint = clamp(mix(vec3(lumaC), color2, INTERIOR_SATURATION), 0.0, 1.0);
        float fringe = smoothstep(COLOR_SPIKE_MIN, COLOR_SPIKE_FULL, vWave) * smoothstep(0.30, 0.65, rim);
        float spikeAlpha = alp2 * COLOR_SPIKE_ALPHA * fringe;
        if (facingRaw < 0.0) spikeAlpha *= max(backFace, 0.45);
        if (spikeAlpha < 0.004) discard;
        fragColor = vec4(spikeTint, bloomMode > 0.5 ? spikeAlpha * COLOR_SPIKE_BLOOM : spikeAlpha);
        return;
    }

    float tongue = tongueField(vHeight, angle);
    float threshold = BAND_START - TONGUE_DEPTH * tongue;
    float edgeDist = rim - threshold;
    float edgeAa = max(fwidth(edgeDist), 1e-4);
    float band = smoothstep(-edgeAa, edgeAa, edgeDist);
    float tongueBody = band * (1.0 - smoothstep(threshold, BAND_START + 0.001, rim)) * step(0.02, tongue);

    float x = angle / TAU * STREAK_COLUMNS;
    float idx = mod(floor(x), STREAK_COLUMNS);
    float r = hash11(idx);
    float across = 1.0 - abs(fract(x) * 2.0 - 1.0);
    float line = smoothstep(0.62 + 0.30 * r, 1.0, across);
    float runner = fract(vHeight * (0.9 + r) - time * (1.3 + 1.5 * r) + r * 11.0);
    float streak = line * smoothstep(0.0, 0.18, runner) * (1.0 - smoothstep(0.30, 0.62, runner));
    streak *= smoothstep(0.10, 0.35, vHeight);

    float luma = dot(color2, vec3(0.299, 0.587, 0.114));
    vec3 tint = clamp(mix(vec3(luma), color2, INTERIOR_SATURATION), 0.0, 1.5);
    vec3 tipCol = clamp(tint * TIP_BRIGHT, 0.0, 1.0);
    vec3 glowCol = mix(tint, vec3(1.0), EDGE_WHITE);
    float hot = clamp(band + streak, 0.0, 1.0);
    vec3 color = mix(tint, glowCol, hot);
    float lip = smoothstep(0.86, 1.0, rim);
    color = mix(color, vec3(1.0), lip * LIP_WHITE);

    float tongueAlong = clamp((BAND_START - rim) / max(BAND_START - threshold, 1e-3), 0.0, 1.0);
    vec3 tongueCol = mix(vec3(1.0), tipCol, smoothstep(TONGUE_WHITE_END, 1.0, tongueAlong));
    color = mix(color, tongueCol, tongueBody);

    vec3 spikeCol = mix(vec3(1.0), tipCol, smoothstep(SPIKE_WHITE_END, 1.0, vWave));
    color = mix(color, spikeCol, clamp(vWave * 2.0, 0.0, 1.0));

    float alpha = mix(alp1 * INTERIOR_ALPHA, alp2, band);
    alpha = max(alpha, alp2 * clamp(vWave * 1.1 + streak * 0.85, 0.0, 1.0));

    alpha *= 1.0 - 0.45 * smoothstep(0.95, 1.0, vHeight) * (1.0 - vWave);

    if (facingRaw < 0.0) alpha *= max(backFace, 0.45);

    if (bloomMode > 0.5) {
        float glow = clamp(hot + vWave + tongueBody * TONGUE_HEAT, 0.0, 1.0);
        fragColor = vec4(color, alpha * (0.35 + 0.65 * glow));
        return;
    }

    fragColor = vec4(color, alpha);
}
