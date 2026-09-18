#version 150

in vec3 vNormalWorld;
in vec3 v_viewDir;
in vec2 vLocalXZ;

uniform vec3 color1;
uniform vec3 color2;
uniform float alp1;
uniform float alp2;
uniform float bloomMode;

out vec4 fragColor;

const float CORE_END = 0.40;
const float BORDER_END = 0.54;

vec3 saturateColor(vec3 c, float amount) {
    float luma = dot(c, vec3(0.299, 0.587, 0.114));
    return clamp(mix(vec3(luma), c, amount), 0.0, 1.0);
}

void main(void) {
    float distNorm = clamp(length(vLocalXZ) / 0.15, 0.0, 1.0);

    float core = 1.0 - smoothstep(CORE_END - 0.10, CORE_END + 0.04, distNorm);
    float body = 1.0 - smoothstep(BORDER_END - 0.05, BORDER_END + 0.05, distNorm);
    float haloT = clamp((distNorm - BORDER_END) / (1.0 - BORDER_END), 0.0, 1.0);
    float halo = pow(1.0 - haloT, 2.4);

    vec3 edgeColor = saturateColor(color2, 1.35);
    vec3 hotColor = mix(color1, vec3(1.0), 0.7);
    vec3 tintColor = mix(edgeColor, hotColor, 0.6);

    if (bloomMode > 0.5) {
        float glowAlpha = alp1 * clamp(body * 0.9 + halo * 0.5 * (1.0 - body), 0.0, 1.0);
        if (glowAlpha <= 0.01) discard;
        fragColor = vec4(edgeColor, glowAlpha);
        return;
    }

    vec3 coreColor = mix(hotColor, tintColor, smoothstep(0.0, CORE_END, distNorm));
    vec3 finalColor = mix(edgeColor, coreColor, core);
    float finalAlpha = alp1 * clamp(body + halo * 0.5 * (1.0 - body), 0.0, 1.0);

    if (finalAlpha <= 0.01) discard;

    fragColor = vec4(finalColor, finalAlpha);
}
