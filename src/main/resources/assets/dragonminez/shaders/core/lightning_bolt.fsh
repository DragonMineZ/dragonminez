#version 150

uniform vec3 CoreColor;
uniform vec3 EdgeColor;
uniform float Alpha;
uniform float BloomPass;

in vec2 boltCoord;
in vec4 boltColor;

out vec4 fragColor;

const float CORE_EDGE = 0.34;
const float OUTLINE_EDGE = 0.58;
const float GLOW_STRENGTH = 0.55;

void main() {
    float across = abs(boltCoord.x);
    float soft = max(fwidth(across), 0.02);

    float core = 1.0 - smoothstep(CORE_EDGE - soft, CORE_EDGE + soft, across);
    float outline = 1.0 - smoothstep(OUTLINE_EDGE - soft, OUTLINE_EDGE + soft, across);
    float halo = 1.0 - smoothstep(OUTLINE_EDGE, 1.0, across);
    halo = halo * halo * GLOW_STRENGTH;

    vec3 color = mix(EdgeColor, CoreColor, core);
    float alpha = max(outline, halo) * boltColor.a * Alpha;
    if (alpha <= 0.004) discard;

    if (BloomPass > 0.5) {
        vec3 glow = mix(EdgeColor, CoreColor, core * 0.4);
        fragColor = vec4(glow, clamp(alpha * (0.7 + 0.3 * core), 0.0, 1.0));
        return;
    }

    fragColor = vec4(color, clamp(alpha, 0.0, 1.0));
}
