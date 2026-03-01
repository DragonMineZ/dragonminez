#version 150

uniform sampler2D DiffuseSampler;
uniform float Time;

uniform vec3 PrimaryColor;
uniform vec3 SecondaryColor;
uniform float GlowStrength;
uniform float NoiseScale;
uniform float NoiseIntensity;
uniform vec2 NoiseScroll;
uniform float ColorMixSpeed;

in vec2 texCoord;
out vec4 fragColor;

float hash(vec2 value) {
    return fract(sin(dot(value, vec2(127.1, 311.7))) * 43758.5453123);
}

float valueNoise(vec2 value) {
    vec2 i = floor(value);
    vec2 f = fract(value);

    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));

    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

void main() {
    vec2 uv = clamp(texCoord, vec2(0.0), vec2(1.0));
    float edgeMask = texture(DiffuseSampler, uv).r;
    edgeMask = smoothstep(0.02, 0.98, edgeMask) * max(0.0, GlowStrength);

    vec2 noiseUv = uv * max(0.01, NoiseScale) + NoiseScroll * Time * 20.0;
    float noise = valueNoise(noiseUv);
    float pulse = 0.5 + 0.5 * sin(Time * 6.2831853 * max(0.01, ColorMixSpeed));
    float mixValue = clamp(pulse + (noise - 0.5) * max(0.0, NoiseIntensity) * 2.0, 0.0, 1.0);

    vec3 outlineColor = mix(PrimaryColor, SecondaryColor, mixValue);
    vec3 color = outlineColor * edgeMask;

    fragColor = vec4(color, clamp(edgeMask, 0.0, 1.0));
}
