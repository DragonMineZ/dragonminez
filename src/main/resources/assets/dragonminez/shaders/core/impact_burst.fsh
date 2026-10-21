#version 150

in vec2 vLocal;

uniform float progress;
uniform float seed;
uniform float dirAngle;
uniform float dirStrength;
uniform float twoTone;
uniform vec3 color1;
uniform vec3 color2;

out vec4 fragColor;

const float TAU = 6.2831853;
const float STREAK_CELLS = 48.0;

float hash1(float n) {
    return fract(sin(n * 127.1 + seed * 311.7) * 43758.5453);
}

float hash2(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7)) + seed * 17.13) * 43758.5453);
}

float vnoise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    float a = hash2(i);
    float b = hash2(i + vec2(1.0, 0.0));
    float c = hash2(i + vec2(0.0, 1.0));
    float d = hash2(i + vec2(1.0, 1.0));
    return mix(mix(a, b, f.x), mix(c, d, f.x), f.y);
}

float fbm(vec2 p) {
    float sum = 0.0;
    float amp = 0.5;
    for (int i = 0; i < 4; i++) {
        sum += vnoise(p) * amp;
        p = p * 2.03 + vec2(13.7, 7.1);
        amp *= 0.5;
    }
    return sum;
}

void main() {
    float cs = cos(dirAngle);
    float sn = sin(dirAngle);
    vec2 q = vec2(vLocal.x * cs + vLocal.y * sn, -vLocal.x * sn + vLocal.y * cs);

    float r = length(q);
    if (r > 1.0) discard;

    float t = clamp(progress, 0.0, 1.0);
    float eased = 1.0 - pow(1.0 - t, 3.0);
    float fade = 1.0 - t;
    float edge = 1.0 - smoothstep(0.86, 1.0, r);
    vec2 dirv = q / max(r, 1.0e-4);

    float frontBias = mix(1.0, 0.3 + 0.7 * smoothstep(-0.35, 0.8, dirv.x), dirStrength);
    float axisBias = mix(1.0, 0.25 + 0.75 * pow(abs(dirv.x), 3.0), dirStrength);

    float core = exp(-(r * r) / (0.015 + 0.06 * eased)) * pow(fade, 1.6) * 2.2;
    float flare = exp(-(q.y * q.y) / 0.0012) * exp(-(q.x * q.x) / 0.40) * pow(fade, 2.2) * 1.4;
    float flareCross = exp(-(q.x * q.x) / 0.0012) * exp(-(q.y * q.y) / 0.10) * pow(fade, 3.0) * 0.6;

    float radius = mix(0.14, 0.84, eased);
    float d = r - radius;
    float shellEdge = exp(-(d * d) / 0.0007);
    float shellTrail = exp(-max(0.0, d) * max(0.0, d) / 0.0007) * exp(-max(0.0, -d) / 0.20);

    vec2 flow = q * (3.6 / (0.3 + radius)) + vec2(seed * 3.1, seed * 1.7);
    float wisps = smoothstep(0.30, 0.75, fbm(flow - dirv * t * 1.5));
    float fine = smoothstep(0.35, 0.9, fbm(flow * 2.7 + 5.0));

    float fire = shellTrail * (0.25 + 0.95 * wisps) * (0.6 + 0.6 * fine);
    float inside = 1.0 - smoothstep(radius - 0.05, radius, r);
    float fill = inside * (0.10 + 0.22 * wisps) * pow(r / max(radius, 1.0e-4), 1.5);

    float ang = atan(q.y, q.x) / TAU + 0.5;
    float cell = mod(floor(ang * STREAK_CELLS), STREAK_CELLS);
    float h1 = hash1(cell + 17.0);
    float h2 = hash1(cell + 57.0);
    float h3 = hash1(cell + 113.0);
    float across = fract(ang * STREAK_CELLS) - 0.5 - (h2 - 0.5) * 0.4;
    float arc = abs(across) * (TAU / STREAK_CELLS) * r;
    float lineWidth = mix(0.016, 0.005, h3);
    float line = exp(-(arc * arc) / (lineWidth * lineWidth));
    float start = radius * (0.15 + 0.35 * h2);
    float end = min(0.98, radius * (1.0 + 0.7 * h3) + 0.1);
    float along = smoothstep(start, mix(start, end, 0.3), r) * (1.0 - smoothstep(mix(start, end, 0.55), end, r));
    float streak = step(0.4, h1) * line * along * axisBias;

    vec2 grid = q / (0.35 + eased * 0.9) * 13.0;
    vec2 gid = floor(grid);
    float gh = hash2(gid + 3.0);
    vec2 gp = fract(grid) - 0.5 - (vec2(hash2(gid + 9.0), hash2(gid + 21.0)) - 0.5) * 0.6;
    float embers = step(0.84, gh) * (1.0 - smoothstep(0.03, 0.13, length(gp))) * pow(fade, 0.8)
                 * (1.0 - smoothstep(radius * 1.05, radius * 1.25, r));

    float tone = smoothstep(0.38, 0.62, vnoise(dirv * 2.2 + vec2(seed, seed * 0.37))) * twoTone;
    vec3 primary = mix(color1, color2, tone);
    vec3 secondary = mix(color2, color1, tone);
    vec3 hot = mix(primary, vec3(1.0), 0.75);

    vec3 color = vec3(1.0) * core
               + hot * (flare + flareCross)
               + mix(primary, vec3(1.0), 0.35) * shellEdge * frontBias * pow(fade, 1.1) * 1.5
               + mix(secondary, primary, fine * 0.6) * fire * frontBias * pow(fade, 0.9) * 1.6
               + secondary * fill * frontBias * fade
               + mix(primary, vec3(1.0), 0.45) * streak * pow(fade, 1.3) * 1.1
               + hot * embers;
    color *= edge;

    float alpha = clamp(max(color.r, max(color.g, color.b)) * 0.7, 0.0, 1.0);
    if (alpha < 0.004) discard;

    fragColor = vec4(color, alpha);
}
