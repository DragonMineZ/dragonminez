#version 150

in vec3 vNormalWorld;
in vec3 v_viewDir;
in vec2 vLocalXZ;

uniform vec3 color1;
uniform vec3 color2;
uniform float alp1;
uniform float alp2;

out vec4 fragColor;

void main(void) {
    float vDistFromCenter = length(vLocalXZ);
    float distNorm = clamp(vDistFromCenter / 0.15, 0.0, 1.0);

    float coreFactor = 1.0 - smoothstep(0.0, 0.3, distNorm);
    float glowFactor = 1.0 - smoothstep(0.2, 1.0, distNorm);

    vec3 coreColor = mix(color1, vec3(1.0), 0.7);
    vec3 finalColor = mix(color2, coreColor, coreFactor);

    float finalAlpha = mix(0.0, alp1, glowFactor) * 1.5;

    if (finalAlpha <= 0.01) discard;

    fragColor = vec4(finalColor, clamp(finalAlpha, 0.0, 1.0));
}