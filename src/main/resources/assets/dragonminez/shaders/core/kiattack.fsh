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
uniform float orbMode;
uniform float splatMode;
uniform float splatLife;

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

// Charge orb: fire is penned into the middle so the shell stays a clean round ball.
const float ORB_SHELL_START = 0.02;
const float ORB_SHELL_END   = 0.42;

// Fire-ash sprites. FREQ/ROUGH set how broken the outline is, MORPH how fast it reshapes,
// and the CUT pair how much of the flake has burned away between birth and death.
const float SPLAT_FREQ      = 2.30;
const float SPLAT_ROUGH     = 0.95;
const float SPLAT_ASPECT    = 0.90;
const float SPLAT_MORPH     = 1.40;
const float SPLAT_CUT_YOUNG = 0.28;
const float SPLAT_CUT_OLD   = 0.86;
const float SPLAT_TOP       = 1.15;
const float SPLAT_FEATHER   = 0.06;

// Where the three colours sit inside the flake, as a fraction of its depth from the rim.
const float SPLAT_RIM       = 0.10;
const float SPLAT_MID       = 0.34;
const float SPLAT_HOT       = 0.72;

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

    // Fire-ash embers. The outline is a 2D slice of the value-noise field rather than a sum of
    // angular harmonics: harmonics always close into a symmetric star, so every flake came out
    // looking like the same flower turned around. A noise slice keyed on the seed gives shapes
    // that differ structurally -- some round, some torn, some in pieces.
    //
    // The slice drifts with splatLife and the cut rises as the ember ages, so the flake keeps
    // reshaping and is eaten away into fragments instead of merely scaling down. All three
    // colours are banded on that same field, so they live inside the flake.
    if (splatMode > 0.5) {
        vec2 q = vUv * 2.0 - 1.0;
        float s = time;

        // Frozen per-ember orientation and aspect, so some flakes are round and some streak.
        float rot = fract(s * 0.117) * 6.28318;
        float aspect = 1.0 + fract(s * 0.311) * SPLAT_ASPECT;
        float cs = cos(rot);
        float sn = sin(rot);
        vec2 qr = vec2(q.x * cs - q.y * sn, q.x * sn + q.y * cs);

        float d = length(vec2(qr.x * aspect, qr.y));
        if (d > 1.0) discard;

        float n = clamp(fbm(vec3(qr * SPLAT_FREQ, s + splatLife * SPLAT_MORPH)) * FBM_MAX_INV, 0.0, 1.0);
        float dens = (1.0 - d) + (n - 0.5) * SPLAT_ROUGH;

        // Burning down: the surviving band narrows over the ember's life, so the flake crumbles.
        float cut = mix(SPLAT_CUT_YOUNG, SPLAT_CUT_OLD, splatLife);
        if (dens < cut) discard;

        float depth = clamp((dens - cut) / max(SPLAT_TOP - cut, 0.001), 0.0, 1.0);
        vec3 splatCol = colorOutline;
        splatCol = mix(splatCol, colorBorder, smoothstep(SPLAT_RIM, SPLAT_MID, depth));
        splatCol = mix(splatCol, colorCore, smoothstep(SPLAT_MID, SPLAT_HOT, depth));

        float splatAlpha = alphaMult * globalAlpha * smoothstep(cut, cut + SPLAT_FEATHER, dens);
        if (bloomMode > 0.5) splatAlpha *= 0.35 + 0.65 * depth;
        fragColor = vec4(splatCol, splatAlpha);
        return;
    }

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
        float f = clamp(abs(dot(normalize(vViewDir), normalize(vNormal))), 0.0, 1.0);
        g = 1.0 - sqrt(max(0.0, 1.0 - f * f));
    }

    // Radial mask for the charge orb: 0 at the rim, 1 in the middle. Multiplying the flame
    // turbulence by it keeps the fire churning inside while the silhouette stays perfectly
    // round -- the fired beam leaves it at 1.0 and keeps its torn edge.
    float orbInner = (orbMode > 0.5) ? smoothstep(ORB_SHELL_START, ORB_SHELL_END, g) : 1.0;

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

        wobCore    = (turb * 0.50 + turbFine * 0.16) * orbInner;
        wobBorder  = (turb * 0.42 + turbFine * 0.20) * orbInner;
        wobOutline = (turb * 0.34 + turbFine * 0.26) * orbInner;
        edgeBite   = turbFine * 0.13 * orbInner;
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

        col = mix(col, colorBorder, (1.0 - blotchBand) * 0.55 * orbInner);
        col *= mix(1.0, mix(0.52, 1.32, blotchBand), orbInner);
    }

    vec3 finalColor = col;

    float edgeCoord = (shapeMode > 0.5 || orbMode > 0.5) ? g : (g + wobOutline + edgeBite);
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
