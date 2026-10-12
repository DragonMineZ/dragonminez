#version 150

in vec2 vUv;
in vec3 vAura;
in float vSeed;

uniform float GameTime;

out vec4 fragColor;

const float TAU = 6.28318530718;

const float BLADE_WIDTH = 0.40;
const float TIP = 0.97;
const float HAND_END = 0.143;

const float THIN_COLS = 16.0;
const float THIN_SHARP = 1.6;
const float THIN_REACH = 0.12;
const float BIG_COLS = 7.0;
const float BIG_SHARP = 2.2;
const float BIG_REACH = 0.32;
const float BIG_RARITY = 2.5;
const float BURST_RATE = 2.6;
const float CLIMB_RATE = 0.35;

const float BURST_RADIUS = 0.42;
const float BURST_THIN_COLS = 18.0;
const float BURST_BIG_COLS = 9.0;
const float BURST_THIN_REACH = 0.18;
const float BURST_BIG_REACH = 0.48;
const float BURST_SPIN = 0.06;

const float OUTLINE_WIDTH = 0.05;
const float CORE_RATIO = 0.5;
const float BURST_CORE_RATIO = 0.78;
const float HAND_ALPHA = 0.25;
const float CORE_ALPHA = 0.30;
const float BURST_RIM_ALPHA = 0.65;

float hash(float n) {
    return fract(sin(n * 127.1 + vSeed * 311.7) * 43758.5453);
}

float beatLength(float id, float t, float rarity) {
    float k = floor(t);
    float life = fract(t);
    float burst = smoothstep(0.0, 0.18, life) * (1.0 - smoothstep(0.40, 1.0, life));
    float len = pow(hash(id * 7.13 + k * 1.71), rarity);
    return (0.10 + 0.90 * len) * (0.15 + 0.85 * burst);
}

float spikeCells(float x, float cols, float sharp, float rarity, float seed, float t) {
    float c = mod(floor(x), cols);
    float f = fract(x);
    float id = c * 131.0 + seed * 17.0;
    float tip = 0.30 + 0.45 * hash(id + 2.0);
    float along = f < tip ? f / tip : (1.0 - f) / (1.0 - tip);
    float len = beatLength(id, t * BURST_RATE * (0.55 + hash(id + 9.0)) + hash(id + 5.0) * 9.0, rarity);
    return pow(along, sharp) * len;
}

float bladeSpikes(float u, float side, float t) {
    float x = u - t * CLIMB_RATE;
    float thin = spikeCells(x * THIN_COLS, 1.0e4, THIN_SHARP, 1.5, 3.0 + side * 29.0, t);
    float big = spikeCells(x * BIG_COLS, 1.0e4, BIG_SHARP, BIG_RARITY, 11.0 + side * 29.0, t);
    float fade = smoothstep(HAND_END * 0.6, HAND_END + 0.08, u) * (1.0 - smoothstep(0.70, 0.90, u));
    return max(thin * THIN_REACH, big * BIG_REACH) * fade;
}

float bladeProfile(float u, float t) {
    float base = mix(0.85, 1.0, smoothstep(0.0, 0.25, u));
    float taper = 1.0 - pow(clamp(u / TIP, 0.0, 1.0), 2.4);
    float pulse = 0.95 + 0.05 * sin(t * 24.0 + vSeed * TAU);
    float cap = smoothstep(0.0, 0.035, u) * (1.0 - smoothstep(TIP - 0.01, TIP, u));
    return BLADE_WIDTH * base * taper * pulse * cap;
}

vec3 interiorColor() { return mix(vAura, vec3(1.0), 0.72); }

vec3 borderColor() {
    float lum = dot(vAura, vec3(0.299, 0.587, 0.114));
    return clamp((vAura + (vAura - lum) * 0.35) * 1.05, 0.0, 1.0);
}

vec3 outlineColor() {
    float lum = dot(vAura, vec3(0.299, 0.587, 0.114));
    return clamp(vAura + (vAura - lum) * 0.6, 0.0, 1.0) * 0.6;
}

vec4 shade(float dist, float e, float body, float coreRatio, float coreAlpha, float handTint, float glowFade, float burst) {
    float aa = max(fwidth(dist), 1.0e-4);
    float outlineW = min(OUTLINE_WIDTH, e * 0.35);
    float coreEdge = e * coreRatio;

    float inShape = 1.0 - smoothstep(e - aa, e + aa, dist);
    float inBorder = 1.0 - smoothstep(e - outlineW - aa, e - outlineW + aa, dist);
    float inCore = 1.0 - smoothstep(coreEdge - aa, coreEdge + aa, dist);

    vec3 border = mix(borderColor(), interiorColor(), burst);
    vec3 interior = mix(interiorColor(), border, handTint * 0.45);

    vec3 col = outlineColor();
    col = mix(col, border, inBorder);
    col = mix(col, interior, inCore);

    float alpha = mix(1.0, mix(0.95, BURST_RIM_ALPHA, burst), inBorder);
    alpha = mix(alpha, coreAlpha, inCore);
    alpha *= inShape;

    float d = max(dist - body, 0.0);
    float glow = exp(-d * 9.0) * 0.4 * glowFade * (1.0 - inShape);

    return vec4(mix(border, col, inShape), max(alpha, glow));
}

void main() {
    float t = GameTime * 1200.0;
    vec4 result;

    if (vUv.x > 5.0) {
        vec2 p = vec2(vUv.x - 10.0, vUv.y);
        float r = length(p);
        float a = atan(p.y, p.x) / TAU + 0.5 + t * BURST_SPIN;
        float thin = spikeCells(a * BURST_THIN_COLS, BURST_THIN_COLS, THIN_SHARP, 1.5, 41.0, t);
        float big = spikeCells(a * BURST_BIG_COLS, BURST_BIG_COLS, BIG_SHARP, BIG_RARITY, 53.0, t);
        float pulse = 0.94 + 0.06 * sin(t * 20.0 + vSeed * TAU);
        float body = BURST_RADIUS * pulse;
        float e = body + max(thin * BURST_THIN_REACH, big * BURST_BIG_REACH);
        float fade = 1.0 - smoothstep(0.85, 1.0, r);
        result = shade(r, e, body, BURST_CORE_RATIO, CORE_ALPHA, 0.0, 1.0, 1.0);
        result.a *= fade;
    } else {
        float u = vUv.x;
        float side = vUv.y > 0.0 ? 1.0 : 0.0;
        float body = bladeProfile(u, t);
        float e = body + bladeSpikes(u, side, t) * step(0.001, body);
        float overHand = 1.0 - smoothstep(HAND_END * 0.7, HAND_END * 1.4, u);
        float coreAlpha = mix(CORE_ALPHA, HAND_ALPHA, overHand);
        float glowFade = smoothstep(0.0, 0.08, u) * (1.0 - smoothstep(0.9, 1.0, u));
        result = shade(abs(vUv.y), e, max(body, e * 0.6), CORE_RATIO, coreAlpha, overHand, glowFade, 0.0);
    }

    if (result.a < 0.004) discard;
    fragColor = result;
}
