#version 150

uniform sampler2D Sampler0;
uniform float GameTime;

in vec4 vertexColor;
in vec4 lightMapColor;
in vec2 texCoord0;
in float progress;
in float mode;
in float shade;

out vec4 fragColor;

const float WHITE_END = 0.75;
const float VANISH_START = 0.85;
const float TELEPORT_VANISH_START = 0.35;
const vec3 WHITE = vec3(0.95, 0.97, 1.0);
const vec3 STREAK_DARK = vec3(0.10, 0.11, 0.16);
const vec3 STREAK_LIGHT = vec3(0.80, 0.90, 1.0);

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}

void main() {
    vec4 tex = texture(Sampler0, texCoord0);
    if (tex.a < 0.1) discard;
    vec4 base = tex * vertexColor;
    float p = progress;

    vec3 lit = base.rgb * shade * lightMapColor.rgb;
    bool teleport = mode > 0.5;
    float white = teleport ? 0.0 : smoothstep(0.0, WHITE_END, p);
    vec3 pale = WHITE * (0.80 + 0.20 * shade) * mix(vec3(1.0), lightMapColor.rgb, 0.5);
    vec3 color = mix(lit, pale, white);

    vec2 frag = gl_FragCoord.xy;
    float flicker = floor(GameTime * 24000.0);
    float vanishStart = teleport ? TELEPORT_VANISH_START : VANISH_START;
    float vanish = clamp((p - vanishStart) / (1.0 - vanishStart), 0.0, 1.0);

    if (vanish > 0.0) {
        float column = floor(frag.x / 3.0);
        float order = hash(vec2(column, 3.0));
        float cut = vanish * 1.15 - 0.05;
        if (order < cut) discard;
        if (order < cut + 0.10) color = mix(color, STREAK_LIGHT, 0.75);
    }

    float lineColumn = floor(frag.x / 2.0);
    float lineSeed = hash(vec2(lineColumn, 7.0));
    float density = (teleport ? smoothstep(0.0, 0.3, p) * 0.45 : smoothstep(0.02, 0.6, p) * 0.30) + vanish * 0.15;
    if (lineSeed < density) {
        float run = noise(vec2(lineColumn * 1.37 + flicker * 0.31, frag.y * 0.025 + lineSeed * 40.0));
        float body = smoothstep(0.45, 0.62, run);
        if (body > 0.0) {
            vec3 streak = hash(vec2(lineColumn, 11.0)) < 0.7 ? STREAK_DARK : STREAK_LIGHT;
            color = mix(color, streak, body * 0.9);
        }
    }

    fragColor = vec4(color, base.a);
}
