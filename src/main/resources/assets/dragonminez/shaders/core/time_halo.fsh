#version 150

in vec2 vHalo;
in vec4 vInfo;

uniform float Time;
uniform float Intensity;
uniform float BloomMode;
uniform vec3 CameraPos;
uniform vec4 Bounds;
uniform float Thickness;
uniform vec3 CoreColor;
uniform vec3 EdgeColor;
uniform vec3 RimColor;
uniform vec3 GlowColor;

out vec4 fragColor;

const float RING_INNER = 0.71;
const float RING_OUTER = 0.87;
const float TOOTH_OUTER = 1.0;
const float TOOTH_COUNT = 10.0;
const float TOOTH_HALF = 0.15;
const float TOOTH_ROUND = 0.012;
const float TOOTH_SINK = 0.06;
const float TOOTH_PHASE = 1.5707963;

const vec2 ARM_DIR = vec2(0.7431448, 0.6691306);
const float ARM_LENGTH = 1.85;
const float ARM_HALF = 0.105;
const float ARM_TAPER_START = 0.86;
const float ARM_TAPER_CURVE = 1.15;
const float STEM_HALF = 0.09;
const float STEM_BOTTOM = -0.82;

const float WOBBLE_FREQ = 3.2;
const float WOBBLE_AMOUNT = 0.022;
const float WOBBLE_RISE = 0.35;
const vec2 LICK_FREQ = vec2(2.1, 1.2);
const float LICK_LIFT = 0.03;
const float LICK_RISE = 0.55;
const float CALM_PERIOD = 7.0;
const float CALM_DEPTH = 0.45;

const float RIM_WIDTH = 0.045;
const float RIM_KEEP = 0.75;
const float RIM_SHIMMER = 0.06;
const float CORE_FADE = 0.05;
const float GLOW_FALLOFF = 0.07;
const float GLOW_REACH = 0.2;
const float GLOW_STRENGTH = 0.75;
const float GLOW_BREATH = 0.08;
const float BEVEL_WIDTH = 0.05;
const float BEVEL_TILT = 1.6;
const vec3 BEVEL_LIGHT = vec3(-0.45, 0.7, 0.55);
const float BEVEL_GAIN = 2.2;
const float BEVEL_LIFT = 0.55;
const float BEVEL_DEEPEN = 0.7;
const float BEVEL_EPS = 0.004;
const float WALL_DEEPEN = 0.75;
const float WALL_LIT = 0.45;
const int MARCH_STEPS = 32;
const float MARCH_MIN_STEP = 0.004;
const float MARCH_REACH = 3.0;
const float IGNITE_THIN = 0.07;
const float IGNITE_PRESENCE = 0.4;
const float BLOOM_RIM = 0.85;
const float BLOOM_CORE_WHITEN = 0.5;
const float BLOOM_ALPHA = 0.85;

float hash(vec2 p) {
    vec3 p3 = fract(vec3(p.xyx) * 0.1031);
    p3 += dot(p3, p3.yzx + 33.33);
    return fract((p3.x + p3.y) * p3.z);
}

float valueNoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 u = f * f * (3.0 - 2.0 * f);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float sdBox(vec2 p, vec2 b) {
    vec2 q = abs(p) - b;
    return length(max(q, 0.0)) + min(max(q.x, q.y), 0.0);
}

float sdGear(vec2 p) {
    float r = length(p);
    float ring = abs(r - 0.5 * (RING_INNER + RING_OUTER)) - 0.5 * (RING_OUTER - RING_INNER);
    float sector = 6.2831853 / TOOTH_COUNT;
    float angle = TOOTH_PHASE + floor((atan(p.y, p.x) - TOOTH_PHASE) / sector + 0.5) * sector;
    vec2 q = vec2(cos(angle) * p.x + sin(angle) * p.y, cos(angle) * p.y - sin(angle) * p.x);
    float base = RING_OUTER - TOOTH_SINK;
    vec2 extent = vec2(0.5 * (TOOTH_OUTER - base), TOOTH_HALF) - TOOTH_ROUND;
    float tooth = sdBox(q - vec2(0.5 * (base + TOOTH_OUTER), 0.0), extent) - TOOTH_ROUND;
    return min(ring, tooth);
}

float sdArm(vec2 p) {
    vec2 w = vec2(abs(p.x), p.y);
    float s = clamp(dot(w, ARM_DIR), 0.0, ARM_LENGTH);
    float t = clamp((s - ARM_TAPER_START) / (ARM_LENGTH - ARM_TAPER_START), 0.0, 1.0);
    return length(w - ARM_DIR * s) - ARM_HALF * pow(1.0 - t, ARM_TAPER_CURVE);
}

float sdStem(vec2 p) {
    return sdBox(p - vec2(0.0, 0.5 * STEM_BOTTOM), vec2(STEM_HALF, -0.5 * STEM_BOTTOM));
}

float halo(vec2 p) {
    return min(sdGear(p), min(sdArm(p), sdStem(p)));
}

vec2 gradient(vec2 p, float raw) {
    vec2 g = vec2(halo(p + vec2(BEVEL_EPS, 0.0)), halo(p + vec2(0.0, BEVEL_EPS))) - raw;
    return g / max(length(g), 1.0e-5);
}

vec3 shadeBody(vec2 p, float raw, float d) {
    vec2 g = gradient(p, raw);
    float plateau = clamp(-d / BEVEL_WIDTH, 0.0, 1.0);
    vec3 n = normalize(vec3(g * BEVEL_TILT * (1.0 - plateau), 1.0));
    vec3 l = normalize(BEVEL_LIGHT);
    float shade = clamp((dot(n, l) - l.z) * BEVEL_GAIN, -1.0, 1.0);
    vec3 body = mix(EdgeColor, CoreColor, smoothstep(0.0, CORE_FADE, -d));
    return shade > 0.0 ? mix(body, vec3(1.0), shade * BEVEL_LIFT) : mix(body, RimColor, -shade * BEVEL_DEEPEN);
}

vec3 shadeWall(vec2 p, float raw) {
    vec2 g = gradient(p, raw);
    float lit = clamp(0.5 + 0.5 * dot(g, normalize(BEVEL_LIGHT.xy)), 0.0, 1.0);
    return mix(mix(EdgeColor, RimColor, WALL_DEEPEN), EdgeColor, lit * WALL_LIT);
}

void main() {
    float thin = (1.0 - Intensity) * IGNITE_THIN;
    float presence = smoothstep(0.0, IGNITE_PRESENCE, Intensity);

    vec2 entry = vHalo;
    float march = 0.0;
    vec2 along = vec2(0.0);
    bool covered = false;
    if (BloomMode < 0.5) {
        float side = CameraPos.z >= 0.0 ? 1.0 : -1.0;
        float nearZ = side * 0.5 * Thickness;
        vec3 frag = vec3(vHalo, vInfo.x > 0.5 ? nearZ : -nearZ);
        vec3 dir = frag - CameraPos;
        dir.z = abs(dir.z) < 1.0e-5 ? -side * 1.0e-5 : dir.z;
        entry = CameraPos.xy + dir.xy * ((nearZ - CameraPos.z) / dir.z);
        covered = vInfo.x < 0.5 && all(greaterThanEqual(entry, Bounds.xy)) && all(lessThanEqual(entry, Bounds.zw));
        float planar = length(dir.xy);
        along = planar > 1.0e-6 ? dir.xy / planar : vec2(0.0);
        march = min(Thickness * planar / abs(dir.z), MARCH_REACH);
    }

    float raw = halo(entry);
    float d = raw + thin;
    float aa = max(fwidth(d), 1.0e-4);
    float core = 1.0 - smoothstep(-aa, aa, d);
    if (covered) discard;

    float calm = 6.2831853 / CALM_PERIOD;
    float wave = 0.6 * sin(calm * Time) + 0.4 * sin(2.37 * calm * Time);
    float motion = 1.0 - CALM_DEPTH * (0.5 - 0.5 * wave);

    vec2 q = entry * WOBBLE_FREQ - vec2(0.0, Time * WOBBLE_RISE);
    vec2 wobble = vec2(valueNoise(q), valueNoise(q + vec2(5.2, 1.3))) - 0.5;
    float lick = smoothstep(0.25, 0.85, valueNoise(entry * LICK_FREQ + vec2(11.0, -Time * LICK_RISE)));
    vec2 warp = (wobble * 2.0 * WOBBLE_AMOUNT - vec2(0.0, lick * LICK_LIFT)) * motion;
    float dw = halo(entry + warp) + thin;

    float rim = max((1.0 - smoothstep(0.0, RIM_WIDTH * 0.7, d)) * RIM_KEEP, 1.0 - smoothstep(0.0, RIM_WIDTH, dw));
    rim = clamp(rim * (1.0 + RIM_SHIMMER * sin(Time * 2.3 + entry.x * 3.0)), 0.0, 1.0);

    if (BloomMode > 0.5) {
        float cover = max(core, rim * BLOOM_RIM) * presence;
        if (cover < 0.004) discard;
        fragColor = vec4(mix(RimColor, CoreColor, core * BLOOM_CORE_WHITEN), cover * BLOOM_ALPHA);
        return;
    }

    float closest = d;
    vec2 closestPoint = entry;
    float closestRaw = raw;
    if (d > 0.0 && march > 0.0) {
        float t = 0.0;
        float stride = d;
        for (int i = 0; i < MARCH_STEPS; i++) {
            t += max(stride, MARCH_MIN_STEP);
            if (t > march) break;
            vec2 at = entry + along * t;
            float atRaw = halo(at);
            stride = atRaw + thin;
            if (stride < closest) {
                closest = stride;
                closestPoint = at;
                closestRaw = atRaw;
            }
            if (stride < 0.0) break;
        }
    }
    float wall = (1.0 - smoothstep(-aa, aa, closest)) * (1.0 - core);

    float glow = exp(-max(dw, 0.0) / GLOW_FALLOFF) * (1.0 - smoothstep(GLOW_REACH * 0.6, GLOW_REACH, d));
    glow *= GLOW_STRENGTH * (1.0 + GLOW_BREATH * sin(Time * 1.7));

    vec3 color = shadeWall(closestPoint, closestRaw) * wall;
    float alpha = wall;
    color = GlowColor * glow + color * (1.0 - glow);
    alpha = glow + alpha * (1.0 - glow);
    color = RimColor * rim + color * (1.0 - rim);
    alpha = rim + alpha * (1.0 - rim);
    color = shadeBody(entry, raw, d) * core + color * (1.0 - core);
    alpha = core + alpha * (1.0 - core);

    float visible = clamp(alpha * presence, 0.0, 1.0);
    if (visible < 0.003) discard;
    fragColor = vec4(color / max(alpha, 1.0e-4), visible);
}
