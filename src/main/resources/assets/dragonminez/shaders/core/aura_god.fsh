#version 150
#extension GL_ARB_explicit_attrib_location : enable

#moj_import <dragonminez:aura_color.glsl>

in vec3 vNormal;
in vec3 vView;
in vec2 vFlame;
in float vLift;

uniform sampler2D NoiseTex;
uniform float Time;
uniform float Growth;
uniform vec3 RimColor;
uniform float RimAlpha;
uniform float Alpha;
uniform float Hollow;
uniform float Layer;
uniform float Faces;
uniform float BackFace;
uniform float BloomIntensity;
uniform float BloomPass;

layout(location = 0) out vec4 fragColor;

const float FLOW = 0.32;
const float EDGE = 0.41;
const float WALL_BOOST = 0.20;
const float BASE_BOOST = 0.14;
const float SILHOUETTE_CARVE = 0.38;
const float TOP_THIN = 0.07;
const float TIP_THIN = 0.12;
const float OUTLINE = 0.07;
const float FRONT_DENSITY = 0.42;
const float TOP_DENSITY = 1.00;
const float CORE_HEAT = 0.16;
const float INNER_TIME = 13.7;
const float INNER_TURN = 0.37;
const float INNER_BIAS = 0.04;
const float INNER_HEAT = 0.10;
const float INNER_ALPHA = 0.42;
const float INNER_BODY_ALPHA = 0.22;
const float INTERIOR_TINT = 0.14;
const float HOT_WARMTH = 0.85;
const float HOT_LIFT = 0.15;
const float COOL_LIFT = 0.35;
const float DEEP_SHADE = 0.90;
const vec3 HOT_GOLD = vec3(1.0, 0.87, 0.40);
const vec3 DEEP_CRIMSON = vec3(0.80, 0.02, 0.20);

float warmth(vec3 c) {
    float peak = max(c.r, max(c.g, c.b));
    float low = min(c.r, min(c.g, c.b));
    float d = peak - low;
    if (d < 1.0e-4) return 0.0;
    float h;
    if (peak == c.r) h = mod((c.g - c.b) / d, 6.0);
    else if (peak == c.g) h = (c.b - c.r) / d + 2.0;
    else h = (c.r - c.g) / d + 4.0;
    h *= 60.0;
    float gap = abs(mod(h - 30.0 + 180.0, 360.0) - 180.0);
    return (1.0 - smoothstep(55.0, 100.0, gap)) * clamp(d / peak * 1.5, 0.0, 1.0);
}

void main() {
    vec3 N = normalize(vNormal);
    vec3 V = normalize(vView);
    float facingRaw = dot(V, N);
    if (Faces > 0.5 && (Faces < 1.5) == (facingRaw >= 0.0)) discard;
    float frontness = abs(facingRaw);
    float rim = 1.0 - frontness;
    float inner = 1.0 - Layer;
    float wall = smoothstep(0.15, 0.55, rim);
    float outer = smoothstep(0.55, 1.0, rim);

    float u = vFlame.x + inner * INNER_TURN;
    float t = vFlame.y;
    float flow = (Time + inner * INNER_TIME) * FLOW;
    float warp = texture(NoiseTex, vec2(u, t * 0.45 - flow * 0.8)).g - 0.5;
    float big = texture(NoiseTex, vec2(u * 2.0 + warp * 0.30, t * 0.60 - flow)).r;
    float fine = texture(NoiseTex, vec2(u * 5.0 - warp * 0.45, t * 1.40 - flow * 2.6)).g;
    float fire = big * 0.66 + fine * 0.34;
    float base = 1.0 - smoothstep(0.0, 0.30, t);

    float density = fire + (WALL_BOOST * wall - SILHOUETTE_CARVE * smoothstep(0.72, 1.0, rim)) * Layer
            + BASE_BOOST * base + INNER_BIAS * inner - TOP_THIN * t * t - TIP_THIN * vLift;
    float front = Growth * 1.35;
    density -= 0.6 * smoothstep(front - 0.30, front, t);
    density -= 0.5 * (1.0 - smoothstep(0.0, 1.0, Growth));

    float aa = max(fwidth(density), 1.0e-3);
    float flame = smoothstep(EDGE - aa, EDGE + aa, density);

    float heat = 0.22 + 1.10 * (fire - 0.48) + 0.55 * (1.0 - outer) - 0.30 * t - 0.35 * vLift + 0.25 * base;
    heat -= 0.30 * (1.0 - smoothstep(EDGE, EDGE + OUTLINE, density));
    heat += CORE_HEAT * smoothstep(EDGE + 0.02, EDGE + 0.30, density - WALL_BOOST * wall * Layer) + INNER_HEAT * inner;
    heat = clamp(mix(heat, max(heat, 0.55), Hollow), 0.0, 1.0);

    vec3 bright = auraBrightest(RimColor);
    float warm = warmth(bright);
    vec3 hot = mix(bright, HOT_GOLD, HOT_WARMTH * warm);
    hot = mix(hot, vec3(1.0), mix(COOL_LIFT, HOT_LIFT, warm));
    vec3 deep = mix(bright, DEEP_CRIMSON, 0.35 * warm) * DEEP_SHADE;
    vec3 color = mix(deep, RimColor, smoothstep(0.10, 0.45, heat));
    color = mix(color, hot, smoothstep(0.45, 0.95, heat));
    color = auraKeepSaturation(color, RimColor);

    float thickness = mix(FRONT_DENSITY, TOP_DENSITY, smoothstep(0.72, 0.95, t)) / max(frontness, 0.12);
    float shell = mix(1.0 - exp(-thickness), wall, Hollow);
    float alpha = flame * RimAlpha * mix(INNER_ALPHA * smoothstep(0.08, 0.55, frontness) * mix(INNER_BODY_ALPHA, 1.0, smoothstep(0.62, 0.90, t)), shell, Layer);
    float glowTint = INTERIOR_TINT * clamp(Growth, 0.0, 1.0) * (1.0 - wall) * (1.0 - t) * (1.0 - Hollow) * Layer;
    if (glowTint > alpha) {
        color = mix(color, hot, 1.0 - flame);
        alpha = glowTint;
    }
    alpha *= smoothstep(0.0, 0.10, t);

    if (facingRaw < 0.0) alpha *= BackFace;
    alpha *= Alpha;
    if (alpha < 0.003) discard;

    vec4 glow = vec4(color, clamp(alpha * (0.25 + 0.55 * heat) * BloomIntensity, 0.0, 1.0));
    fragColor = BloomPass > 0.5 ? glow : vec4(color, alpha);
}
