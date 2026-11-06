#version 150

in vec2 vLocal;

uniform float progress;
uniform float seed;
uniform vec3 slashColor;

out vec4 fragColor;

const vec2 ARC_CENTER = vec2(0.0, -1.2);
const float ARC_RADIUS = 1.2;
const float CLAW_GAP = 0.2;
const float THETA_MAX = 0.62;
const float CLAW_WIDTH = 0.075;

void main() {
    float t = clamp(progress, 0.0, 1.0);

    vec2 p = vLocal - ARC_CENTER;
    float rho = length(p);
    float u = atan(p.x, p.y) / THETA_MAX;

    float head = mix(-1.3, 1.4, 1.0 - pow(1.0 - clamp(t / 0.4, 0.0, 1.0), 2.5));
    float tail = mix(-1.4, 1.3, smoothstep(0.3, 1.0, t));
    float window = (1.0 - smoothstep(head - 0.3, head, u)) * smoothstep(tail - 0.35, tail, u);
    float thin = 1.0 - 0.5 * smoothstep(0.4, 1.0, t);

    float coreSum = 0.0;
    float glowSum = 0.0;
    for (int k = -1; k <= 1; k++) {
        float fk = float(k);
        float len = (k == 0) ? 1.0 : 0.78;
        float uk = (u - fk * 0.12) / len;
        float taper = pow(max(0.0, 1.0 - uk * uk), 0.7) * mix(0.55, 1.0, smoothstep(-1.0, 0.6, uk));
        float w = CLAW_WIDTH * taper * thin;
        if (w < 0.0005) continue;

        float d = abs(rho - (ARC_RADIUS + fk * CLAW_GAP));
        coreSum = max(coreSum, 1.0 - smoothstep(w * 0.45, w, d));
        glowSum = max(glowSum, exp(-(d * d) / (w * w * 6.0)));
    }

    float brush = 0.8 + 0.2 * sin(rho * 140.0 + seed * 13.0);
    coreSum *= brush;

    float vis = window * (1.0 - smoothstep(0.75, 1.0, t));
    vec3 color = (mix(slashColor, vec3(1.0), 0.85) * coreSum + slashColor * glowSum * 0.9) * vis;
    float alpha = clamp(coreSum * 0.92 + glowSum * 0.35, 0.0, 1.0) * vis;
    if (alpha < 0.004) discard;

    fragColor = vec4(color, alpha);
}
