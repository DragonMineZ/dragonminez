#version 150

in vec2 vUv;
in vec3 vNormal;
in vec3 vViewDir;

uniform float time;
uniform float seed;
uniform float mode;
uniform float spin;
uniform float twist;
uniform float arms;
uniform float bodyAlpha;
uniform float intensity;
uniform float bloomMode;
uniform vec3 colorCore;
uniform vec3 colorBorder;
uniform vec3 colorOutline;

out vec4 fragColor;

const float TAU = 6.2831853;

float hash(vec3 p) {
    p = fract(p * 0.3183099 + vec3(0.71, 0.113, 0.419));
    p *= 17.0;
    return fract(p.x * p.y * p.z * (p.x + p.y + p.z));
}

float vnoise(vec3 x) {
    vec3 i = floor(x);
    vec3 f = fract(x);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(mix(hash(i + vec3(0.0, 0.0, 0.0)), hash(i + vec3(1.0, 0.0, 0.0)), f.x),
                   mix(hash(i + vec3(0.0, 1.0, 0.0)), hash(i + vec3(1.0, 1.0, 0.0)), f.x), f.y),
               mix(mix(hash(i + vec3(0.0, 0.0, 1.0)), hash(i + vec3(1.0, 0.0, 1.0)), f.x),
                   mix(hash(i + vec3(0.0, 1.0, 1.0)), hash(i + vec3(1.0, 1.0, 1.0)), f.x), f.y), f.z);
}

float loopNoise(float s, float y, float freq, float yFreq, float offset) {
    float ang = s * TAU;
    return vnoise(vec3(cos(ang) * freq + offset, sin(ang) * freq - offset, y * yFreq));
}

float armBands(float s, float widthNoise) {
    float b = fract(s * arms);
    float w = 0.05 + 0.11 * widthNoise;
    float d = (b - 0.5) / w;
    return exp(-d * d);
}

void main() {
    float cloud;
    float wisps;
    float arc;
    float shape;
    float edge;

    if (mode > 0.5) {
        vec2 p = vUv * 2.0 - 1.0;
        float r = length(p);
        if (r > 1.0) discard;

        float a = atan(p.y, p.x) / TAU;
        float spiral = a - log(r + 0.08) * 0.42;

        float n1 = loopNoise(spiral - time * spin, r * 2.0 - time * 0.5, 2.2, 1.5, seed);
        float n2 = loopNoise(spiral * 1.0 - time * spin * 1.4, r * 3.0 - time * 0.8, 4.5, 2.5, seed + 7.0);
        cloud = n1 * 0.62 + n2 * 0.38;

        float n3 = loopNoise(spiral - time * spin * 1.15, r * 2.4, 6.0, 2.0, seed + 3.0);
        wisps = pow(1.0 - abs(n3 * 2.0 - 1.0), 7.0) * smoothstep(0.35, 0.7, cloud);

        float breakup = smoothstep(0.3, 0.65, loopNoise(spiral - time * spin, r * 1.5 + time * 0.2, 1.3, 1.2, seed + 11.0));
        arc = armBands(spiral - time * spin * 1.3 + seed * 0.13, n1) * breakup;

        shape = (1.0 - smoothstep(0.45, 1.0, r)) * smoothstep(0.0, 0.10, r);
        edge = 0.6;
    } else {
        float u = vUv.x;
        float v = vUv.y;
        float s = u + v * twist - time * spin;

        float n1 = loopNoise(s, v - time * 0.25, 2.0, 1.8, seed);
        float n2 = loopNoise(u + v * twist * 1.7 - time * spin * 1.4, v - time * 0.4, 4.5, 3.5, seed + 7.0);
        cloud = n1 * 0.62 + n2 * 0.38;

        float n3 = loopNoise(u + v * twist * 1.25 - time * spin * 1.15, v - time * 0.3, 6.0, 2.2, seed + 3.0);
        wisps = pow(1.0 - abs(n3 * 2.0 - 1.0), 7.0) * smoothstep(0.35, 0.7, cloud);

        float breakup = smoothstep(0.3, 0.65, loopNoise(s, v * 0.8 + time * 0.2, 1.3, 1.2, seed + 11.0));
        arc = armBands(u + v * twist - time * spin * 1.3 + seed * 0.13, n1) * breakup;

        float top = 1.0 - smoothstep(0.55, 1.0, v + (cloud - 0.5) * 0.3);
        float bottom = smoothstep(0.0, 0.05, v);
        shape = top * bottom;

        float nv = abs(dot(normalize(vNormal), normalize(vViewDir)));
        edge = pow(max(1.0 - nv, 0.0), 1.6);
    }

    float bodyA = bodyAlpha * (0.30 + 0.70 * edge) * (0.45 + 1.1 * cloud) * shape;
    float glowAmt = (wisps * 0.55 + arc * 0.9) * shape * (0.6 + 0.4 * edge);

    vec3 col = mix(colorOutline, colorBorder, smoothstep(0.2, 0.8, cloud));
    col = mix(col, colorCore, edge * 0.3);
    vec3 hot = mix(colorBorder, colorCore, 0.8);

    float glowCover = clamp(glowAmt * 0.9, 0.0, 1.0);
    float cover = clamp(bodyA + glowCover * (1.0 - bodyA), 0.0, 1.0);
    vec3 surface = mix(col * bodyA, hot, glowCover);

    if (bloomMode > 0.5) {
        float a = clamp(bodyA * 0.7 + glowCover, 0.0, 1.0) * intensity;
        if (a < 0.004) discard;
        vec3 bloomCol = mix(col, hot, glowCover / max(bodyA * 0.7 + glowCover, 0.0001));
        fragColor = vec4(bloomCol * a, a);
        return;
    }

    float alpha = cover * intensity;
    if (alpha < 0.004) discard;
    vec3 rgb = (surface + hot * glowCover * 0.12) * intensity;

    fragColor = vec4(rgb, alpha);
}
