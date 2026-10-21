#version 150

in vec2 vLocal;

uniform float time;
uniform float seed;
uniform float intensity;
uniform float innerRadius;
uniform vec3 colorCore;
uniform vec3 colorBorder;

out vec4 fragColor;

void main() {
    float r = length(vLocal);
    if (r > 1.0) discard;

    float a = atan(vLocal.y, vLocal.x);

    float broad = pow(0.5 + 0.5 * sin(a * 5.0 + time * 0.6 + seed), 5.0);
    float mid = pow(0.5 + 0.5 * sin(a * 9.0 - time * 0.95 + seed * 2.3), 9.0);
    float thin = pow(0.5 + 0.5 * sin(a * 21.0 + time * 1.7 + seed * 4.1), 16.0);
    float flicker = 0.75 + 0.25 * sin(time * 9.0 + a * 4.0);
    float beams = (broad * 0.85 + mid * 0.6 + thin * 0.35 * (broad + mid)) * flicker;

    float reach = 0.62 + 0.38 * sin(a * 3.0 + time * 0.8 + seed * 1.3);
    float start = smoothstep(innerRadius * 0.6, innerRadius * 1.2, r);
    float falloff = pow(max(0.0, 1.0 - r / reach), 1.7);
    float shaft = beams * start * falloff;

    float halo = exp(-max(0.0, r - innerRadius) / 0.10) * 0.35 * start;

    float total = (shaft + halo) * intensity;
    if (total < 0.004) discard;

    vec3 hot = mix(colorCore, vec3(1.0), 0.5);
    vec3 color = mix(colorBorder, hot, clamp(shaft * 1.2 * (1.0 - r), 0.0, 1.0));

    fragColor = vec4(color * total, clamp(total * 0.2, 0.0, 1.0));
}
