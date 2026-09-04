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
uniform float zCut;
uniform float zCutFar;
uniform float flameMode;
uniform float blotchMode;
uniform float shellMode;
uniform float shellCut;

in vec3 vNormal;
in vec3 vViewDir;
in vec3 vLocalPos;
in vec2 vUv;
out vec4 fragColor;

const float CORE_LEVEL    = 0.58;
const float BORDER_LEVEL  = 0.26;
const float OUTLINE_LEVEL = 0.10;
const float EDGE_FADE     = 0.05;

const float WOBBLE_CORE    = 0.17;
const float WOBBLE_BORDER  = 0.11;
const float WOBBLE_OUTLINE = 0.07;

const float BLOTCH_STEPS = 4.0;
const float FBM_MAX_INV  = 1.0667;

float flameField(vec3 p, float t, float freq, float speed) {
    float v  = sin(p.x *  9.0 * freq + t * 4.0 * speed)                 * 0.50;
    v       += sin(p.y * 11.0 * freq - t * 5.5 * speed + p.x * 3.0)     * 0.32;
    v       += sin(p.z * 13.0 * freq + t * 6.5 * speed + p.y * 4.0)     * 0.24;
    v       += sin((p.x + p.y + p.z) * 19.0 * freq - t * 8.0 * speed)   * 0.16;
    return v;
}

// --- Value-noise FBM, used only by the flame muzzle so the classic look is untouched. ---
float hash13(vec3 p) {
    p = fract(p * 0.3183099 + vec3(0.71, 0.113, 0.419));
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float vnoise(vec3 x) {
    vec3 i = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash13(i + vec3(0.0, 0.0, 0.0)), hash13(i + vec3(1.0, 0.0, 0.0)), f.x),
                   mix(hash13(i + vec3(0.0, 1.0, 0.0)), hash13(i + vec3(1.0, 1.0, 0.0)), f.x), f.y),
               mix(mix(hash13(i + vec3(0.0, 0.0, 1.0)), hash13(i + vec3(1.0, 0.0, 1.0)), f.x),
                   mix(hash13(i + vec3(0.0, 1.0, 1.0)), hash13(i + vec3(1.0, 1.0, 1.0)), f.x), f.y), f.z);
}

float fbm(vec3 p) {
    float amp = 0.5;
    float sum = 0.0;
    for (int i = 0; i < 4; i++) {
        sum += amp * vnoise(p);
        p *= 2.03;
        amp *= 0.5;
    }
    return sum;
}

// Two-octave variant for the domain warp and fine detail, where four octaves are wasted.
float fbm2(vec3 p) {
    return 0.5 * vnoise(p) + 0.25 * vnoise(p * 2.03);
}

void main() {
    if (zCut > -0.5 && vLocalPos.z < zCut) discard;
    if (vLocalPos.z > zCutFar) discard;

    vec3 p = vLocalPos;
    float t = time;

    // Detached energy chunks flying OUTSIDE the ball. Drawn on an oversized sphere shell:
    // the noise drives alpha instead of tint, and everything below shellCut is discarded, so
    // only loose blobs survive. The caller sweeps shellCut upward over each shell's life.
    if (shellMode > 0.5) {
        float st = time * 1.1;
        float sn = clamp(fbm(p * 5.2 + vec3(0.0, st * 0.9, st * 0.4)) * FBM_MAX_INV, 0.0, 1.0);

        // shellCut rises as the shell flies outward, so fewer and smaller chunks survive:
        // the cloud thins out and breaks up instead of just drifting at constant density.
        if (sn < shellCut) discard;

        float solid = clamp((sn - shellCut) / max(1.0 - shellCut, 0.001), 0.0, 1.0);
        float level = solid < 0.5 ? 0.45 : 1.0;   // two flat levels keeps the cel look

        vec3 shellCol = mix(colorBorder, colorCore, level);
        float shellAlpha = alphaMult * globalAlpha * level;
        if (bloomMode > 0.5) shellAlpha *= 0.85;
        fragColor = vec4(shellCol, shellAlpha);
        return;
    }

    float flameCore    = flameField(p, t, 1.35, 1.30);
    float flameBorder  = flameField(p, t, 1.00, 0.95);
    float flameOutline = flameField(p, t, 0.65, 0.65);

    float g;
    if (shapeMode > 0.5) {
        float r = clamp(length(p.xy), 0.0, 1.0);
        g = 1.0 - r;
    } else {
        float f = clamp(abs(dot(normalize(vViewDir), normalize(vNormal))), 0.0, 1.0);
        g = 1.0 - sqrt(max(0.0, 1.0 - f * f));
    }

    // Band offsets: gentle shimmer normally, churning fire lobes on the muzzle.
    float wobCore    = flameCore   * WOBBLE_CORE;
    float wobBorder  = flameBorder * WOBBLE_BORDER;
    float wobOutline = flameOutline * WOBBLE_OUTLINE;
    float edgeBite   = 0.0;
    float hot        = 0.0;
    float blotchBand = 1.0;

    if (flameMode > 0.5) {
        vec3 q = p * 2.4;
        float ft = time * 2.3;
        float wx = fbm2(q + vec3(0.0, ft, 0.0));
        float wy = fbm2(q + vec3(5.2, 1.3 - ft, 2.8));
        vec3 warp = vec3(wx, wy, wx * 0.6 + wy * 0.4);
        float turb     = fbm(q * 1.5 + warp * 2.0 + vec3(0.0, -ft * 1.2, 0.0)) * 2.0 - 1.0;
        float turbFine = fbm2(q * 3.9 + warp * 1.2 + vec3(ft * 0.8, -ft * 1.7, 0.0)) * 2.667 - 1.0;

        wobCore    = turb * 0.50 + turbFine * 0.16;
        wobBorder  = turb * 0.42 + turbFine * 0.20;
        wobOutline = turb * 0.34 + turbFine * 0.26;
        edgeBite   = turbFine * 0.13;
        hot        = smoothstep(CORE_LEVEL, CORE_LEVEL + 0.30, g + wobCore);
    }

    vec3 outCol = colorOutline;
    float whiteness = min(min(outCol.r, outCol.g), outCol.b);
    if (whiteness > 0.86) {
        outCol = clamp(colorBorder * 1.55 + 0.12, 0.0, 1.0);
    }

    vec3 col = outCol;
    float toBorder = smoothstep(OUTLINE_LEVEL, BORDER_LEVEL, g + wobOutline);
    col = mix(col, colorBorder, toBorder);
    float toCore = smoothstep(BORDER_LEVEL, CORE_LEVEL, g + wobCore);
    col = mix(col, colorCore, toCore);

    float coreFlicker   = 1.0 + wobCore   * 1.30;
    float borderFlicker = 1.0 + wobBorder * 0.90;
    col *= mix(borderFlicker, coreFlicker, toCore);

    // Cel-style colour patches: posterised 3D noise gives hard-edged blotches instead of a
    // smooth gradient. Evaluated on vLocalPos, so no UVs are involved and nothing pinches
    // at the sphere poles the way a mapped texture would.
    if (blotchMode > 0.5) {
        float bt = time * 1.1;
        float n = clamp(fbm(p * 4.3 + vec3(0.0, bt * 0.8, bt * 0.35)) * FBM_MAX_INV, 0.0, 1.0);
        blotchBand = min(floor(n * BLOTCH_STEPS), BLOTCH_STEPS - 1.0) / (BLOTCH_STEPS - 1.0);

        col = mix(col, colorBorder, (1.0 - blotchBand) * 0.55);
        col *= mix(0.52, 1.32, blotchBand);
    }

    vec3 finalColor = col;

    float edgeCoord = (shapeMode > 0.5) ? g : (g + wobOutline + edgeBite);
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
        if (flameMode > 0.5) bloomA = (halo * 0.80 + toCore * 0.65 + hot * 0.50) * alphaMult * globalAlpha;
        // Dark patches glow less, so the blotches stay readable out at the rim instead of
        // being washed flat by the halo.
        if (blotchMode > 0.5) bloomA *= mix(0.55, 1.15, blotchBand);
        fragColor = vec4(finalColor, bloomA);
        return;
    }

    fragColor = vec4(finalColor, finalAlpha * globalAlpha);
}
