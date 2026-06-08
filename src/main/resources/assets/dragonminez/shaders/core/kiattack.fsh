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

in vec3 vNormal;
in vec3 vViewDir;
in vec3 vLocalPos;
in vec2 vUv;
out vec4 fragColor;

const float CORE_LEVEL = 0.98;
const float BORDER_LEVEL = 0.5;
const float OUTLINE_LEVEL = 0.35;
const float OUTLINE_SOFT = 0.08;
const float EDGE_DEFORM = 0.04;
const float FIRE_STRENGTH = 0.18;
const float FIRE_SPEED = 1.5;

void main() {
    float energyFlow = sin(vLocalPos.x * 12.0 + time * 15.0) * EDGE_DEFORM +
    cos(vLocalPos.y * 15.0 - time * 18.0) * EDGE_DEFORM +
    sin(vLocalPos.z * 10.0 + time * 12.0) * EDGE_DEFORM;

    float facing = clamp(abs(dot(normalize(vViewDir), normalize(vNormal))) + energyFlow, 0.0, 1.0);

    vec3 p = vLocalPos;
    float t = time * FIRE_SPEED;
    float flame = sin(p.x * 9.0  + t * 4.0)               * 0.40
                + sin(p.y * 11.0 - t * 5.5 + p.x * 3.0)   * 0.35
                + sin(p.z * 13.0 + t * 6.5 + p.y * 4.0)   * 0.30
                + sin((p.x + p.y + p.z) * 17.0 - t * 8.0) * 0.20;
    flame *= FIRE_STRENGTH;

    float tCore    = CORE_LEVEL + flame;
    float tBorder  = BORDER_LEVEL + flame;
    float tOutline = OUTLINE_LEVEL + flame * 0.5;
    tCore = max(tCore, tBorder + 0.05);

    float coreToBorder = smoothstep(tBorder, tCore, facing);
    vec3 bodyColor = mix(colorBorder, colorCore, coreToBorder);

    float outlineMix = 1.0 - smoothstep(tOutline, tOutline + OUTLINE_SOFT, facing);
    vec3 finalColor = mix(bodyColor, colorOutline, outlineMix);

    float finalAlpha = alphaMult * smoothstep(0.0, 0.06, facing);

    if (texBlend > 0.0) {
        vec2 animUv = vec2(fract(vUv.x + time * 0.2), fract(vUv.y - time * 0.5));
        vec4 texColor = texture(DiffuseSampler, animUv);

        finalColor = mix(finalColor, texColor.rgb, texBlend * texColor.a);
        finalAlpha *= mix(1.0, texColor.a, texBlend);
    }

    if (bloomMode > 0.5) {
        float rim = pow(1.0 - facing, 2.0);
        fragColor = vec4(colorOutline, rim * alphaMult * globalAlpha);
        return;
    }

    fragColor = vec4(finalColor, finalAlpha * globalAlpha);
}