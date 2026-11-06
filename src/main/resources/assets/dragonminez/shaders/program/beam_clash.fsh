#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 OutSize;
uniform float Seconds;
uniform float Intensity;

in vec2 texCoord;
out vec4 fragColor;

const float PI = 3.14159265;
const float SLOTS_A = 150.0;
const float SLOTS_B = 96.0;
const float INNER_MIN = 0.80;
const float INNER_MAX = 0.93;
const float REROLL_HZ = 9.0;
const float INK_ALPHA = 0.60;
const float EDGE_DARKEN = 0.10;

float hash(float n) {
    return fract(sin(n * 12.9898 + 78.233) * 43758.5453);
}

float wedges(float u, float edge, float slotPx, float frame, float shift,
             float widthMin, float widthMax, float density, float reach) {
    float k = floor(u);
    float f = fract(u);
    float s = k * 1.7 + frame * 13.1 + shift;
    if (hash(s) > density) return 0.0;
    float halfW = mix(widthMin, widthMax, hash(s + 1.3));
    float centre = mix(0.3, 0.7, hash(s + 2.9));
    float inner = mix(INNER_MIN, INNER_MAX, hash(s + 4.1));
    inner = mix(1.02, inner, reach);
    float along = clamp((edge - inner) / max(1.0 - inner, 0.02), 0.0, 1.0);
    if (along <= 0.0) return 0.0;
    float w = halfW * (0.12 + 0.88 * along);
    float aa = 0.75 / max(slotPx, 1.0);
    float line = 1.0 - smoothstep(w - aa, w + aa, abs(f - centre));
    return line * smoothstep(0.0, 0.08, along);
}

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    float i = clamp(Intensity, 0.0, 1.0);
    if (i <= 0.001) {
        fragColor = vec4(color.rgb, 1.0);
        return;
    }

    vec2 p = texCoord - 0.5;
    float aspect = OutSize.x / max(OutSize.y, 1.0);
    vec2 q = vec2(p.x * aspect, p.y);
    float ang = atan(q.y, q.x);
    float edge = max(abs(p.x), abs(p.y)) * 2.0;
    float radiusPx = max(length(q) * OutSize.y, 1.0);
    float frame = mod(floor(Seconds * REROLL_HZ), 256.0);
    float reach = smoothstep(0.0, 1.0, i);

    float turn = ang / (2.0 * PI) + 0.5;
    float a = wedges(turn * SLOTS_A, edge, 2.0 * PI * radiusPx / SLOTS_A, frame, 0.0, 0.05, 0.22, 0.55, reach);
    float b = wedges(turn * SLOTS_B + 0.37, edge, 2.0 * PI * radiusPx / SLOTS_B, frame, 91.7, 0.08, 0.30, 0.38, reach);
    float lines = max(a, b);

    vec3 result = mix(color.rgb, vec3(0.02), lines * INK_ALPHA * i);
    result *= 1.0 - EDGE_DARKEN * i * smoothstep(0.86, 1.0, edge);
    fragColor = vec4(result, 1.0);
}
