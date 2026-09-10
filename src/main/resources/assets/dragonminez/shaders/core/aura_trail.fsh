#version 150

in vec4 vColor;
in vec2 vUv;

// vUv.x runs 0 at the player to 1 at the oldest sample; vUv.y runs across the ribbon.
uniform vec3 color1;
uniform vec3 color2;
uniform float alp1;
uniform float time;

out vec4 fragColor;

float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    f = f * f * (3.0 - 2.0 * f);
    return mix(mix(hash(i), hash(i + vec2(1.0, 0.0)), f.x),
               mix(hash(i + vec2(0.0, 1.0)), hash(i + vec2(1.0, 1.0)), f.x), f.y);
}

void main() {
    float across = abs(vUv.y * 2.0 - 1.0);
    float body = 1.0 - across;

    // Burn the ribbon away from the edges inwards, more the older the section is. That is what
    // keeps it from reading as a solid tube and makes the far end come apart instead of just
    // getting shorter.
    float n = noise(vec2(vUv.x * 7.0 - time * 1.6, vUv.y * 2.5));
    float burn = body - n * 0.55 * vUv.x - 0.10 * vUv.x;
    float dissolve = smoothstep(0.0, 0.35, burn);

    vec3 color = mix(color1, color2, across * 0.8 + vUv.x * 0.2);
    float alpha = vColor.a * alp1 * dissolve;

    if (alpha < 0.004) discard;
    fragColor = vec4(color, alpha);
}
