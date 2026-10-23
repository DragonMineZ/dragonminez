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

void main(void) {
    float vDistFromCenter = length(vLocalXZ);
    float distNorm = clamp(vDistFromCenter / 0.15, 0.0, 1.0);

    // The bright interior takes up most of the bolt; the coloured border is only the last sliver.
    float coreFactor = 1.0 - smoothstep(0.0, 0.62, distNorm);
    float glowFactor = 1.0 - smoothstep(0.62, 0.9, distNorm);

    vec3 coreColor = mix(color1, vec3(1.0), 0.7);
    vec3 finalColor = mix(color2, coreColor, coreFactor);

    float finalAlpha = mix(0.0, alp1, glowFactor) * 1.5;

    if (finalAlpha <= 0.01) discard;

    if (bloomMode > 0.5) {
        vec3 glowColor = mix(color2, coreColor, 0.35);
        fragColor = vec4(glowColor, clamp(finalAlpha * (0.75 + 0.25 * coreFactor), 0.0, 1.0));
        return;
    }

    fragColor = vec4(finalColor, clamp(finalAlpha, 0.0, 1.0));
}
