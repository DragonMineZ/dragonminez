#version 150

uniform sampler2D DiffuseSampler;
uniform float AnimationTime;

in vec2 texCoord;
out vec4 fragColor;

const float NoiseScale = 1.5;
const float ColorMixSpeed = 0.2;
const float NoiseIntensity = 0.25;
const vec2 NoiseScroll = vec2(0.2, 0.15);

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

vec2 unpackChannel(float channel) {
    float packedVal = floor(channel * 255.0 + 0.5);
    float primary = floor(packedVal / 16.0) / 15.0;
    float secondary = mod(packedVal, 16.0) / 15.0;
    return vec2(primary, secondary);
}

void main() {
    vec4 packedMask = texture(DiffuseSampler, texCoord);
    if (packedMask.a < 0.01) {
       fragColor = vec4(0.0);
       return;
    }

    vec2 rPair = unpackChannel(packedMask.r);
    vec2 gPair = unpackChannel(packedMask.g);
    vec2 bPair = unpackChannel(packedMask.b);

    vec3 primaryColor = vec3(rPair.x, gPair.x, bPair.x);
    vec3 secondaryColor = vec3(rPair.y, gPair.y, bPair.y);

    vec2 noiseUv = texCoord * max(0.01, NoiseScale) + NoiseScroll * AnimationTime * 20.0;
    float noise = valueNoise(noiseUv);
    float pulse = 0.5 + 0.5 * sin(AnimationTime * 6.2831853 * max(0.01, ColorMixSpeed));
    float mixValue = clamp(pulse + (noise - 0.5) * max(0.0, NoiseIntensity) * 2.0, 0.0, 1.0);

    vec3 mixedColor = mix(primaryColor, secondaryColor, mixValue);
    fragColor = vec4(mixedColor, packedMask.a);
}
