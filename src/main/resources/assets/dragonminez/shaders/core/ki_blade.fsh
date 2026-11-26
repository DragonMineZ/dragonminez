#version 150

in vec2 vUv;
in vec3 vAura;
in float vSeed;

uniform float GameTime;
uniform float bloomMode;

out vec4 fragColor;

const float TAU = 6.28318530718;

const float BLADE_WIDTH = 0.40;
const float TIP = 0.97;
const float HAND_END = 0.111111;
const float PEAK_U = 0.3;
const float HILT_WIDTH = 0.75;
const float LEAF_POW = 1.6;
const float LEAF_BELLY = 1.2;
const float SPIKE_WIDTH_REF = 0.24;

const float THIN_COLS = 20.0;
const float THIN_SHARP = 1.6;
const float THIN_REACH = 0.12;
const float BIG_COLS = 9.0;
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
const float BURST_U = 0.111111;
const float BURST_U_SCALE = 2.7;
const float BURST_V_SCALE = 0.75;
const float UNION_BLEND = 0.06;

const float SCYTHE_U = 20.0;
const float SCYTHE_V_UNIT = 0.46875;
const float SCYTHE_SHAFT_LEN = 3.4375;
const float SCYTHE_SHAFT_HALF = 0.05;
const float SCYTHE_SHAFT_TIP = 0.22;
const vec2 SCYTHE_JOINT = vec2(0.14, 0.0);
const vec2 SCYTHE_JOINT_INNER = vec2(0.28, 0.0);
const vec2 SCYTHE_BLADE_TIP = vec2(2.1, 0.95);
const float SCYTHE_OUTER_BULGE = 0.2;
const float SCYTHE_INNER_BULGE = 0.5;
const float SCYTHE_BLADE_E = 0.5;
const float SCYTHE_BLADE_BAND = 0.16;
const float SCYTHE_FIRE_COLS = 6.0;
const float SCYTHE_FIRE_LIFT = 0.14;
const float SCYTHE_SHAFT_FIRE = 0.3;

const float CLAW_ROD_U = 60.0;
const float CLAW_ROD_STRIDE = 4.0;
const float CLAW_SHAFT_LEN = 1.875;
const float CLAW_SHAFT_R = 0.055;
const float CLAW_BUTT_TIP = 0.25;
const float CLAW_ELBOW_LEN = 0.5634;
const float CLAW_ELBOW_R0 = 0.055;
const float CLAW_ELBOW_R1 = 0.045;
const float CLAW_TINE_LEN = 0.7344;
const float CLAW_TINE_R = 0.05;
const float CLAW_ROD_ALPHA = 0.9;
const float CLAW_FIRE_COLS = 8.0;
const float CLAW_FIRE_LIFT = 0.12;
const float CLAW_SHAFT_FIRE = 0.3;
const float CLAW_SHARP_RADIUS = 0.06;
const float CLAW_JOINT_CALM = 0.1;

const float OUTLINE_WIDTH = 0.05;
const float CORE_RATIO = 0.5;
const float BURST_CORE_RATIO = 0.78;
const float HAND_ALPHA = 0.35;
const float CORE_ALPHA = 0.55;
const float BURST_CORE_ALPHA = 0.4;
const float BURST_RIM_ALPHA = 0.85;
const float INTERIOR_LIGHTEN = 0.12;
const float AXIS_LIGHTEN = 0.22;

const float FIRE_INTENSITY = 1.0;
const float FIRE_COLS = 11.5;
const float FIRE_ROWS = 4.0;
const float FIRE_FLOW = 0.6;
const float FIRE_RISE = 2.2;
const float FIRE_LIFT = 0.16;
const float FIRE_BURST_COLS = 14.0;
const float FIRE_BURST_LIFT = 0.12;
const float FIRE_GLOW_FLICKER = 0.8;

const float BLOOM_BORDER_ALPHA = 0.8;
const float BLOOM_CORE_ALPHA = 0.35;
const float BLOOM_WHITEN = 0.2;
const float BLOOM_BURST_DIM = 0.3;
const float BLOOM_HAND_DIM = 0.6;

float hash(float n) {
    return fract(sin(n * 127.1 + vSeed * 311.7) * 43758.5453);
}

float hash2(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031 + vSeed * 0.37);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float fireNoise(vec2 p, float period) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    float x0 = mod(i.x, period);
    float x1 = mod(i.x + 1.0, period);
    float a = hash2(vec2(x0, i.y));
    float b = hash2(vec2(x1, i.y));
    float c = hash2(vec2(x0, i.y + 1.0));
    float d = hash2(vec2(x1, i.y + 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float flameField(vec2 q, float t, float period) {
    float v = 0.55 * fireNoise(q - vec2(0.0, t), period);
    v += 0.30 * fireNoise(q * 2.0 + vec2(17.0, -2.6 * t), period * 2.0);
    v += 0.15 * fireNoise(q * 4.0 + vec2(-8.0, -6.1 * t), period * 4.0);
    return smoothstep(0.2, 0.8, v);
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
    float base = mix(HILT_WIDTH, 1.0, smoothstep(0.0, PEAK_U, u));
    float x = clamp((u - PEAK_U) / (TIP - PEAK_U), 0.0, 1.0);
    float taper = pow(1.0 - x, LEAF_POW) * (1.0 + LEAF_BELLY * x);
    float pulse = 0.95 + 0.05 * sin(t * 24.0 + vSeed * TAU);
    float cap = smoothstep(0.0, 0.035, u) * (1.0 - smoothstep(TIP - 0.01, TIP, u));
    return BLADE_WIDTH * base * taper * pulse * cap;
}

vec3 borderColor() {
    float lum = dot(vAura, vec3(0.299, 0.587, 0.114));
    return clamp((vAura + (vAura - lum) * 0.35) * 1.05, 0.0, 1.0);
}

vec3 interiorColor() { return mix(borderColor(), vec3(1.0), INTERIOR_LIGHTEN); }

vec3 outlineColor() {
    float lum = dot(vAura, vec3(0.299, 0.587, 0.114));
    return clamp(vAura + (vAura - lum) * 0.6, 0.0, 1.0) * 0.6;
}

vec4 shade(float s, float s0, float e, float band, float glowOffset, float coreAlpha, float rimAlpha, float handTint, float glowFade, float burst, float flame, float axis, float outlineMix) {
    float aa = max(fwidth(s), 1.0e-4);
    float aa0 = max(fwidth(s0), 1.0e-4);
    float outlineW = min(OUTLINE_WIDTH, e * 0.35);

    float inShape = 1.0 - smoothstep(-aa, aa, s);
    float inBorder = mix(inShape, 1.0 - smoothstep(-outlineW - aa, -outlineW + aa, s), outlineMix);
    float inCore = 1.0 - smoothstep(-band - aa0, -band + aa0, s0);

    vec3 border = borderColor();
    vec3 interior = mix(interiorColor(), vec3(1.0), AXIS_LIGHTEN * axis * axis);
    interior = mix(interior, border, handTint * 0.45);

    if (bloomMode > 0.5) {
        vec3 bright = mix(mix(border, interior, inCore), vec3(1.0), BLOOM_WHITEN);
        float bloomAlpha = inBorder * mix(BLOOM_BORDER_ALPHA, BLOOM_CORE_ALPHA, inCore) * (1.0 - handTint * BLOOM_HAND_DIM) * (1.0 - burst * BLOOM_BURST_DIM);
        return vec4(bright, bloomAlpha);
    }

    vec3 col = outlineColor();
    col = mix(col, border, inBorder);
    col = mix(col, interior, inCore);

    float alpha = mix(1.0, rimAlpha, inBorder);
    alpha = mix(alpha, coreAlpha, inCore);
    alpha *= inShape;

    float d = max(s + glowOffset, 0.0);
    float glow = exp(-d * 9.0) * 0.4 * glowFade * (1.0 - inShape) * mix(1.0, 0.6 + FIRE_GLOW_FLICKER * flame, FIRE_INTENSITY);

    return vec4(mix(border, col, inShape), max(alpha, glow));
}

vec3 arcCenter(vec2 a, vec2 b, float bulge) {
    vec2 mid = (a + b) * 0.5;
    vec2 chord = b - a;
    vec2 inward = normalize(vec2(chord.y, -chord.x));
    vec2 c = mid + inward * bulge;
    return vec3(c, length(a - c));
}

vec4 scythe(float s, float tv, float t) {
    float ends = min(s, SCYTHE_SHAFT_LEN - s);
    float shaftHalf = SCYTHE_SHAFT_HALF * clamp(ends / SCYTHE_SHAFT_TIP, 0.0, 1.0);
    float sShaft = max(abs(tv) - shaftHalf, max(-s, s - SCYTHE_SHAFT_LEN));

    vec2 p = vec2(s, tv);
    vec3 outer = arcCenter(SCYTHE_JOINT, SCYTHE_BLADE_TIP, SCYTHE_OUTER_BULGE);
    vec3 inner = arcCenter(SCYTHE_JOINT_INNER, SCYTHE_BLADE_TIP, SCYTHE_INNER_BULGE);
    float sBlade = max(length(p - outer.xy) - outer.z, inner.z - length(p - inner.xy));
    sBlade = max(sBlade, -(tv + SCYTHE_SHAFT_HALF));

    float w = smoothstep(-0.03, 0.03, sBlade - sShaft);
    float s0 = min(sShaft, sBlade) / SCYTHE_V_UNIT;

    float flame = flameField(vec2((s + tv) * SCYTHE_FIRE_COLS - t * FIRE_FLOW, max(s0, 0.0) * FIRE_ROWS), t * FIRE_RISE, 1.0e4);
    float reach = smoothstep(-0.25, -0.05, s0);
    float sw = s0 - flame * SCYTHE_FIRE_LIFT * FIRE_INTENSITY * reach * mix(1.0, SCYTHE_SHAFT_FIRE, w);

    float shaftE = max(shaftHalf, 1.0e-3) / SCYTHE_V_UNIT;
    float e = mix(SCYTHE_BLADE_E, shaftE, w);
    float band = mix(SCYTHE_BLADE_BAND, shaftE * 0.5, w);
    float axis = w * (1.0 - clamp(abs(tv) / max(shaftHalf, 1.0e-3), 0.0, 1.0));

    return shade(sw, s0, e, band, 0.0, CORE_ALPHA, 0.95, 0.0, 1.0, 0.0, flame, axis, 1.0);
}

vec4 clawRod(float piece, float s, float tv, float t) {
    float len = CLAW_TINE_LEN;
    float r0 = CLAW_TINE_R;
    float r1 = 0.0;
    float fire = 1.0;
    if (piece < 0.5) {
        len = CLAW_SHAFT_LEN;
        r0 = CLAW_SHAFT_R;
        r1 = CLAW_SHAFT_R;
        fire = CLAW_SHAFT_FIRE;
    } else if (piece < 4.5) {
        len = CLAW_ELBOW_LEN;
        r0 = CLAW_ELBOW_R0;
        r1 = CLAW_ELBOW_R1;
    }

    float h = clamp(s / len, 0.0, 1.0);
    float radius = mix(r0, r1, h);
    if (piece < 0.5) radius *= clamp((len - s) / CLAW_BUTT_TIP, 0.0, 1.0);
    float d = length(vec2(s - h * len, tv)) - radius;

    float safeRadius = max(radius, 1.0e-3);
    float s0 = d / SCYTHE_V_UNIT;
    float side = tv > 0.0 ? 1.0 : 0.0;
    float flame = flameField(vec2(s * CLAW_FIRE_COLS + piece * 7.3 - t * FIRE_FLOW + side * 23.0, max(s0, 0.0) * FIRE_ROWS), t * FIRE_RISE, 1.0e4);
    float reach = smoothstep(-0.25, -0.05, s0);
    float sw = s0 - flame * CLAW_FIRE_LIFT * FIRE_INTENSITY * reach * fire * smoothstep(0.0, CLAW_SHARP_RADIUS, radius) * smoothstep(0.0, CLAW_JOINT_CALM, s) * smoothstep(0.0, CLAW_JOINT_CALM, len - s);

    float e = safeRadius / SCYTHE_V_UNIT;
    float axis = 1.0 - clamp((d + radius) / safeRadius, 0.0, 1.0);
    return shade(sw, s0, e, e * 0.55, 0.0, CLAW_ROD_ALPHA, CLAW_ROD_ALPHA, 0.0, 1.0, 0.0, flame, axis, 0.0);
}

void main() {
    float t = GameTime * 1200.0;
    float u = vUv.x;
    float v = vUv.y;

    if (u > CLAW_ROD_U - 1.0) {
        float piece = floor((u - CLAW_ROD_U + 0.5) / CLAW_ROD_STRIDE);
        vec4 cl = clawRod(piece, u - CLAW_ROD_U - piece * CLAW_ROD_STRIDE, v, t);
        if (cl.a < 0.004) discard;
        fragColor = cl;
        return;
    }

    if (u > SCYTHE_U - 1.0) {
        vec4 sc = scythe(u - SCYTHE_U, v, t);
        if (sc.a < 0.004) discard;
        fragColor = sc;
        return;
    }

    float side = v > 0.0 ? 1.0 : 0.0;
    float dist = abs(v);
    float bBody = bladeProfile(u, t);
    float bE = bBody + bladeSpikes(u, side, t) * step(0.001, bBody) * clamp(bBody / SPIKE_WIDTH_REF, 0.0, 1.0);
    float bFlame = flameField(vec2(u * FIRE_COLS - t * FIRE_FLOW + side * 31.0, dist * FIRE_ROWS), t * FIRE_RISE, 1.0e4);
    float along = smoothstep(HAND_END * 0.6, HAND_END + 0.1, u) * (1.0 - smoothstep(0.82, TIP, u));
    float bReach = smoothstep(0.3, 1.0, dist / max(bE, 1.0e-3));
    float bValid = step(1.0e-3, bE);
    float sB = mix(1.0, dist - bFlame * FIRE_LIFT * FIRE_INTENSITY * along * bReach - bE, bValid);
    float sB0 = mix(1.0, dist - bE, bValid);
    float overHand = 1.0 - smoothstep(HAND_END * 0.7, HAND_END * 1.4, u);
    float bGlowFade = smoothstep(0.0, 0.08, u) * (1.0 - smoothstep(0.9, 1.0, u));
    float bAxis = 1.0 - clamp(dist / max(bE * CORE_RATIO, 1.0e-4), 0.0, 1.0);

    vec2 p = vec2((u - BURST_U) * BURST_U_SCALE, v * BURST_V_SCALE);
    float r = length(p);
    float angle = atan(p.y, p.x) / TAU + 0.5;
    float a = angle + t * BURST_SPIN;
    float thin = spikeCells(a * BURST_THIN_COLS, BURST_THIN_COLS, THIN_SHARP, 1.5, 41.0, t);
    float big = spikeCells(a * BURST_BIG_COLS, BURST_BIG_COLS, BIG_SHARP, BIG_RARITY, 53.0, t);
    float rBody = BURST_RADIUS * (0.94 + 0.06 * sin(t * 20.0 + vSeed * TAU));
    float rE = rBody + max(thin * BURST_THIN_REACH, big * BURST_BIG_REACH);
    float rFlame = flameField(vec2(angle * FIRE_BURST_COLS, r * FIRE_ROWS), t * FIRE_RISE, FIRE_BURST_COLS);
    float rReach = smoothstep(0.3, 1.0, r / max(rE, 1.0e-3));
    float toBlade = 1.0 / BURST_V_SCALE;
    float sR = (r - rFlame * FIRE_BURST_LIFT * FIRE_INTENSITY * rReach - rE) * toBlade;
    float sR0 = (r - rE) * toBlade;
    float rFade = 1.0 - smoothstep(0.85, 1.0, r);

    float w = smoothstep(-UNION_BLEND, UNION_BLEND, sR - sB);
    float s = min(sB, sR);
    float s0 = min(sB0, sR0);
    float e = mix(rE * toBlade, bE, w);
    float band = mix(rE * (1.0 - BURST_CORE_RATIO) * toBlade, bE * (1.0 - CORE_RATIO), w);
    float glowOffset = mix((rE - rBody) * toBlade, bE - max(bBody, bE * 0.6), w);
    float coreAlpha = mix(BURST_CORE_ALPHA, mix(CORE_ALPHA, HAND_ALPHA, overHand), w);
    float rimAlpha = mix(BURST_RIM_ALPHA, 0.95, w);

    vec4 result = shade(s, s0, e, band, glowOffset, coreAlpha, rimAlpha, overHand * w, mix(1.0, bGlowFade, w), 1.0 - w, mix(rFlame, bFlame, w), bAxis * w, 1.0);
    result.a *= mix(rFade, 1.0, w);

    if (result.a < 0.004) discard;
    fragColor = result;
}
