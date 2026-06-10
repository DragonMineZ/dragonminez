#version 150

uniform sampler2D DiffuseSampler;
uniform vec3 colorCore;
uniform vec3 colorBorder;
uniform vec3 colorOutline;
uniform float alphaMult;
uniform float time;
uniform float texBlend;
uniform float bloomMode;
uniform float globalAlpha;
uniform float shapeMode;
uniform float zCut;    // < -0.5 = disabled. Discard fragments with local z below this (cuts the cylinder start so the muzzle ball wins).
uniform float zCutFar; // >= cylinder length (e.g. 2.0) = disabled. Discard fragments with local z above this (cuts the cylinder end so the impact ball wins).

in vec3 vNormal;
in vec3 vViewDir;
in vec3 vLocalPos;
in vec2 vUv;
out vec4 fragColor;

const float CORE_LEVEL    = 0.80;
const float BORDER_LEVEL  = 0.44;
const float OUTLINE_LEVEL = 0.14;
const float EDGE_FADE     = 0.06;

const float WOBBLE_CORE    = 0.17;
const float WOBBLE_BORDER  = 0.11;
const float WOBBLE_OUTLINE = 0.07;

float flameField(vec3 p, float t, float freq, float speed) {
    float v  = sin(p.x *  9.0 * freq + t * 4.0 * speed)                 * 0.50;
    v       += sin(p.y * 11.0 * freq - t * 5.5 * speed + p.x * 3.0)     * 0.32;
    v       += sin(p.z * 13.0 * freq + t * 6.5 * speed + p.y * 4.0)     * 0.24;
    v       += sin((p.x + p.y + p.z) * 19.0 * freq - t * 8.0 * speed)   * 0.16;
    return v;
}

void main() {
    if (zCut > -0.5 && vLocalPos.z < zCut) discard;
    if (vLocalPos.z > zCutFar) discard;

    vec3 p = vLocalPos;
    float t = time;

    float flameCore    = flameField(p, t, 1.35, 1.30);
    float flameBorder  = flameField(p, t, 1.00, 0.95);
    float flameOutline = flameField(p, t, 0.65, 0.65);

    float g;
    if (shapeMode > 0.5) {
        float r = clamp(length(p.xy), 0.0, 1.0);
        g = 1.0 - r;
    } else {
        g = clamp(abs(dot(normalize(vViewDir), normalize(vNormal))), 0.0, 1.0);
    }

    vec3 outCol = colorOutline;
    float whiteness = min(min(outCol.r, outCol.g), outCol.b);
    if (whiteness > 0.86) {
        outCol = clamp(colorBorder * 1.55 + 0.12, 0.0, 1.0);
    }

    vec3 col = outCol;
    float toBorder = smoothstep(OUTLINE_LEVEL, BORDER_LEVEL, g + flameOutline * WOBBLE_OUTLINE);
    col = mix(col, colorBorder, toBorder);
    float toCore = smoothstep(BORDER_LEVEL, CORE_LEVEL, g + flameCore * WOBBLE_CORE);
    col = mix(col, colorCore, toCore);

    float coreFlicker   = 1.0 + flameCore   * 0.22;
    float borderFlicker = 1.0 + flameBorder * 0.10;
    col *= mix(borderFlicker, coreFlicker, toCore);

    vec3 finalColor = col;

    float edgeCoord = (shapeMode > 0.5) ? g : (g + flameOutline * WOBBLE_OUTLINE);
    float finalAlpha = alphaMult * smoothstep(0.0, EDGE_FADE, edgeCoord);

    if (texBlend > 0.0) {
        vec2 animUv = vec2(fract(vUv.x + time * 0.2), fract(vUv.y - time * 0.5));
        vec4 texColor = texture(DiffuseSampler, animUv);

        finalColor = mix(finalColor, texColor.rgb, texBlend * texColor.a);
        finalAlpha *= mix(1.0, texColor.a, texBlend);
    }

    if (bloomMode > 0.5) {
        float halo = pow(1.0 - g, 1.6);
        float bloomA = (halo * 0.7 + toCore * 0.6) * alphaMult * globalAlpha;
        fragColor = vec4(finalColor, bloomA);
        return;
    }

    fragColor = vec4(finalColor, finalAlpha * globalAlpha);
}
