#version 150

in vec2 vUv;
in vec4 vInfo;

uniform float GameTime;
uniform float bloomMode;

out vec4 fragColor;

const float RADIUS_SCALE = 8.0;
const float WARP_FREQ = 0.22;
const float WARP_AMOUNT = 2.2;
const float WARP_SPEED = 0.7;
const float PUFF_FREQ = 0.55;
const float PUFF_AMOUNT = 0.45;
const float PUFF_SPEED = 1.4;
const float END_FADE = 0.12;
const float CURL_SPACING = 9.0;
const float CURL_SIZE = 0.5;
const float CURL_TURNS = 1.7;
const float CURL_WIDTH = 0.09;
const float CURL_SPIN = 0.8;
const vec2 LIGHT_OFFSET = vec2(-0.55, 0.75);
const float BLOOM_WHITEN = 0.2;
const float BLOOM_ALPHA = 0.85;

const vec3 OUTLINE = vec3(0.788, 0.584, 0.282);
const vec3 DEEP = vec3(0.922, 0.780, 0.486);
const vec3 SHADOW = vec3(0.965, 0.902, 0.737);
const vec3 LIGHT = vec3(1.0, 0.992, 0.957);
const vec3 CURL = vec3(0.878, 0.741, 0.471);

float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    vec2 s = f * f * (3.0 - 2.0 * f);
    float a = hash(i), b = hash(i + vec2(1.0, 0.0)), c = hash(i + vec2(0.0, 1.0)), d = hash(i + vec2(1.0, 1.0));
    return mix(mix(a, b, s.x), mix(c, d, s.x), s.y);
}

float fbm(vec2 p) {
    float v = 0.0, amp = 0.5;
    for (int i = 0; i < 3; i++) { v += noise(p) * amp; p *= 2.03; amp *= 0.5; }
    return v;
}

float billow(vec2 p) {
    float v = 0.0, amp = 0.6;
    for (int i = 0; i < 2; i++) { v += (1.0 - abs(2.0 * noise(p) - 1.0)) * amp; p *= 2.1; amp *= 0.4; }
    return v;
}

float field(vec2 uv, float radius, float along, float closed, float seed, float t) {
    vec2 q = vec2(uv.x, uv.y * radius) + seed * 17.0;
    vec2 warp = vec2(fbm(q * WARP_FREQ + vec2(3.1, -t * WARP_SPEED)), fbm(q * WARP_FREQ + vec2(-7.7, 5.0 - t * WARP_SPEED))) - 0.5;
    float v = uv.y + warp.y * WARP_AMOUNT / radius;
    float base = 1.0 - abs(v);
    if (closed < 0.5) {
        base -= (1.0 - smoothstep(0.0, END_FADE, along)) * 1.2;
        base -= smoothstep(1.0 - END_FADE, 1.0, along) * 1.2;
    }
    float puff = billow(vec2(q.x - t * PUFF_SPEED, q.y) * PUFF_FREQ + warp);
    return base + (puff - 0.55) * PUFF_AMOUNT;
}

bool curl(vec2 uv, float radius, float t) {
    if (radius < 1.1) return false;
    float cell = floor(uv.x / CURL_SPACING);
    vec2 c = vec2((cell + 0.5) * CURL_SPACING, 0.0);
    vec2 d = vec2(uv.x - c.x, uv.y * radius);
    float size = radius * CURL_SIZE;
    float rr = length(d);
    if (rr > size || rr < 0.12) return false;
    float k = size / (6.28318530718 * CURL_TURNS);
    float th = mod(atan(d.y, d.x) + 3.14159265 + t * CURL_SPIN, 6.28318530718);
    for (int n = 0; n < 3; n++) {
        if (abs(rr - k * (th + 6.28318530718 * float(n))) < CURL_WIDTH) return true;
    }
    return false;
}

void main() {
    float radius = max(vInfo.r * RADIUS_SCALE, 0.2);
    float seed = vInfo.g;
    float along = vInfo.b;
    float closed = vInfo.a;
    float t = GameTime * 1200.0;

    float val = field(vUv, radius, along, closed, seed, t);
    if (val <= 0.0) discard;

    if (bloomMode > 0.5) {
        fragColor = vec4(mix(LIGHT, vec3(1.0), BLOOM_WHITEN), BLOOM_ALPHA * clamp(val * 2.5, 0.0, 1.0));
        return;
    }

    vec3 col;
    if (val < 0.08) {
        col = OUTLINE;
    } else {
        vec2 lightUv = vUv + vec2(LIGHT_OFFSET.x, LIGHT_OFFSET.y / radius);
        float facing = val - field(lightUv, radius, along, closed, seed, t);
        if (val > 0.18 && curl(vUv, radius, t)) col = CURL;
        else if (facing < -0.2 && val < 0.22) col = DEEP;
        else if (facing < -0.09) col = SHADOW;
        else col = LIGHT;
    }
    fragColor = vec4(col, 1.0);
}
