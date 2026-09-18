#version 150

#moj_import <dragonminez:aura_color.glsl>

uniform sampler2D Mask;
uniform sampler2D Field;
uniform sampler2D NoiseTex;
uniform sampler2D SceneDepth;

uniform vec2 ScreenSize;
uniform vec2 Center;
uniform vec2 Up;
uniform float Thickness;
uniform float Id;
uniform float Time;
uniform float Seed;
uniform float Alpha;
uniform vec3 Color;
uniform vec3 DepthParams;
uniform float BloomIntensity;
uniform float BloomPass;

in vec2 texCoord;
out vec4 fragColor;

const float RISE_SPEED = 3.2;
const float LICK_BASE = 0.12;
const float LICK_REACH = 3.60;
const float SWAY = 0.55;
const float EDGE_LOW = 0.125;
const float EDGE_HIGH = 0.175;
const float EROSION = 0.16;
const float RIM_ALPHA = 0.40;
const float CORE_WHITE = 0.45;
const float OCCLUSION_FADE = 0.75;

void main() {
    vec2 frag = texCoord * ScreenSize;
    vec4 mask = texture(Mask, texCoord);
    bool self = mask.a > 0.5 && abs(mask.r - Id) < 0.002;

    vec2 side = vec2(Up.y, -Up.x);
    vec2 rel = (frag - Center) / Thickness;
    vec2 p = vec2(dot(rel, side), dot(rel, Up));
    float rise = Time * RISE_SPEED;

    float big = texture(NoiseTex, vec2(p.x * 0.120 + Seed, (p.y - rise) * 0.045)).r;
    float mid = texture(NoiseTex, vec2(p.x * 0.065 - Seed * 2.0, (p.y - rise * 0.7) * 0.030)).g;
    float fine = texture(NoiseTex, vec2(p.x * 0.260 - Seed, (p.y - rise * 1.4) * 0.130)).g;
    float sway = texture(NoiseTex, vec2(p.x * 0.040 + Seed * 3.0, (p.y - rise * 0.6) * 0.040)).g - 0.5;

    float near = texture(Field, texCoord).r;
    vec3 bright = auraBrightest(Color);
    vec3 hot = mix(bright, vec3(1.0), CORE_WHITE);

    if (self) {
        float rim = (1.0 - smoothstep(0.50, 0.90, near)) * RIM_ALPHA * Alpha;
        rim *= 0.70 + 0.30 * fine;
        fragColor = vec4(mix(bright, hot, 0.5), BloomPass > 0.5 ? rim * BloomIntensity * 0.5 : rim);
        return;
    }

    float tongue = smoothstep(0.30, 0.85, big) * (0.55 + 0.45 * smoothstep(0.25, 0.75, mid));
    float lick = LICK_BASE + LICK_REACH * tongue * tongue;
    vec2 shifted = frag - (Up * lick + side * sway * SWAY * lick) * Thickness;
    float lifted = texture(Field, shifted / ScreenSize).r;
    float density = max(lifted, near * 0.9);

    float shaped = density - fine * EROSION * (1.0 - smoothstep(0.35, 0.6, density));
    float flame = smoothstep(EDGE_LOW, EDGE_HIGH, shaped);
    float heat = smoothstep(0.20, 0.62, shaped);
    float core = smoothstep(0.30, 0.50, near);

    vec3 color = mix(Color * 0.92, bright, heat);
    color = mix(color, hot, max(core, smoothstep(0.75, 1.0, heat) * 0.6));
    float alpha = flame * mix(0.78, 1.0, heat) * Alpha;

    if (DepthParams.z > 0.0) {
        float ndc = texture(SceneDepth, texCoord).r * 2.0 - 1.0;
        float sceneDistance = DepthParams.y / (ndc + DepthParams.x);
        alpha *= smoothstep(DepthParams.z - OCCLUSION_FADE, DepthParams.z, sceneDistance);
    }

    fragColor = vec4(color, BloomPass > 0.5 ? clamp(alpha * BloomIntensity, 0.0, 1.0) : alpha);
}
