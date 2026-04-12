#version 150

uniform sampler2D DiffuseSampler;
uniform vec3 colorCore;
uniform vec3 colorBorder;
uniform vec3 colorOutline;
uniform float alphaMult;
uniform float time;
uniform float texBlend;

in vec3 vNormal;
in vec3 vViewDir;
in vec3 vLocalPos;
in vec2 vUv;
out vec4 fragColor;

void main() {
    float energyFlow = sin(vLocalPos.x * 12.0 + time * 15.0) * 0.08 +
    cos(vLocalPos.y * 15.0 - time * 18.0) * 0.08 +
    sin(vLocalPos.z * 10.0 + time * 12.0) * 0.08;

    float facing = clamp(abs(dot(normalize(vViewDir), normalize(vNormal))) + energyFlow, 0.0, 1.0);

    vec3 finalColor;
    float finalAlpha = alphaMult;

    if (facing > 0.65) {
        finalColor = colorCore;
    } else if (facing > 0.25) {
        finalColor = mix(colorBorder, colorCore, smoothstep(0.25, 0.65, facing));
    } else {
        finalColor = mix(colorOutline, colorBorder, smoothstep(0.0, 0.25, facing));
        finalAlpha *= smoothstep(0.0, 0.15, facing);
    }

    if (texBlend > 0.0) {
        vec2 animUv = vec2(fract(vUv.x + time * 0.2), fract(vUv.y - time * 0.5));
        vec4 texColor = texture(DiffuseSampler, animUv);

        finalColor = mix(finalColor, texColor.rgb, texBlend * texColor.a);
        finalAlpha *= mix(1.0, texColor.a, texBlend);
    }

    fragColor = vec4(finalColor, finalAlpha);
}