#version 150

in vec2 vLocal;

uniform sampler2D Scene;
uniform float progress;
uniform float seed;
uniform float distort;
uniform vec2 screenSize;
uniform vec3 waveColor;

out vec4 fragColor;

const float TAU = 6.2831853;
const float STREAK_CELLS = 36.0;
const float RING_CELLS = 9.0;

float hash(float n) {
    return fract(sin(n * 127.1 + seed * 311.7) * 43758.5453);
}

float wrapNoise(float ang, float cells) {
    float x = ang * cells;
    float i = floor(x);
    float f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(hash(mod(i, cells)), hash(mod(i + 1.0, cells)), f);
}

void main() {
    float r = length(vLocal);
    vec2 grad = vec2(dFdx(r), dFdy(r));
    if (r > 1.0) discard;

    float t = clamp(progress, 0.0, 1.0);
    float eased = 1.0 - pow(1.0 - t, 3.0);
    float fade = 1.0 - t;
    float edge = 1.0 - smoothstep(0.88, 1.0, r);

    float radius = mix(0.10, 0.80, eased);
    float ang = atan(vLocal.y, vLocal.x) / TAU + 0.5;

    float width = mix(0.055, 0.014, eased);
    float d = (r - radius) / width;
    float ring = exp(-d * d) * (0.55 + 0.45 * wrapNoise(ang, RING_CELLS));
    float halo = exp(-abs(r - radius) / (width * 4.0)) * 0.25;

    float inside = 1.0 - smoothstep(radius - width * 2.0, radius, r);
    float dome = inside * pow(clamp(r / radius, 0.0, 1.0), 2.5) * 0.22;

    float cell = mod(floor(ang * STREAK_CELLS), STREAK_CELLS);
    float h1 = hash(cell + 17.0);
    float h2 = hash(cell + 57.0);
    float h3 = hash(cell + 113.0);
    float across = fract(ang * STREAK_CELLS) - 0.5 - (h2 - 0.5) * 0.4;
    float arc = abs(across) * (TAU / STREAK_CELLS) * r;
    float lineWidth = mix(0.011, 0.004, h3);
    float line = exp(-(arc * arc) / (lineWidth * lineWidth));
    float start = radius * (0.35 + 0.35 * h2);
    float end = min(0.97, radius * (1.05 + 0.55 * h3));
    float along = smoothstep(start, mix(start, end, 0.35), r) * (1.0 - smoothstep(mix(start, end, 0.6), end, r));
    float streak = step(0.45, h1) * line * along;

    float core = exp(-(r * r) / 0.012) * pow(fade, 5.0) * 1.5;

    vec3 hot = mix(waveColor, vec3(1.0), 0.65);
    vec3 warm = mix(waveColor, vec3(1.0), 0.35);
    vec3 glow = hot * (ring + halo) * pow(fade, 1.2) * 1.3
              + warm * streak * pow(fade, 1.5) * 0.9
              + hot * dome * fade
              + vec3(1.0) * core;
    glow *= edge;

    float mask = 0.0;
    vec3 scene = vec3(0.0);
    float gradLen = length(grad);
    if (distort > 0.5 && gradLen > 1.0e-6) {
        vec2 dir = grad / gradLen;
        float s = (r - radius) / 0.10;
        float profile = s * exp(-s * s) * 2.332;
        vec2 offset = dir * profile * (0.07 * fade * edge) / (gradLen * screenSize);
        vec2 uv = gl_FragCoord.xy / screenSize;
        scene.r = texture(Scene, uv - offset * 1.15).r;
        scene.g = texture(Scene, uv - offset).g;
        scene.b = texture(Scene, uv - offset * 0.85).b;
        mask = (1.0 - smoothstep(2.0, 3.0, abs(s))) * edge;
    }

    float glowMax = clamp(max(glow.r, max(glow.g, glow.b)), 0.0, 1.0);
    float alpha = max(mask, glowMax * (1.0 - distort));
    if (alpha < 0.003 && glowMax < 0.003) discard;

    fragColor = vec4(scene * mask + glow, alpha);
}
