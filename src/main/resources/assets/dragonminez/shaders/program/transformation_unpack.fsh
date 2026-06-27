#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D ParamsSampler;
uniform float AnimationTime;

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

vec2 unpackChannel(float channel) {
    float packedVal = floor(channel * 255.0 + 0.5);
    float primary = floor(packedVal / 16.0) / 15.0;
    float secondary = mod(packedVal, 16.0) / 15.0;
    return vec2(primary, secondary);
}

float unpackByte(float channel) {
    return floor(channel * 255.0 + 0.5) / 255.0;
}

float unpackSignedByte(float channel, float range) {
    float normalized = unpackByte(channel);
    return (normalized * 2.0 - 1.0) * range;
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
    vec4 packedParams = texture(ParamsSampler, texCoord);
    vec2 scaleAndMix = unpackChannel(packedParams.r);

    vec3 primaryColor = vec3(rPair.x, gPair.x, bPair.x);
    vec3 secondaryColor = vec3(rPair.y, gPair.y, bPair.y);
    float noiseScale = mix(0.25, 16.0, scaleAndMix.x);
    float colorMixSpeed = mix(0.0, 4.0, scaleAndMix.y);
    float noiseIntensity = unpackByte(packedParams.g);
    vec2 noiseScroll = vec2(
          unpackSignedByte(packedParams.b, 1.0),
          unpackSignedByte(packedParams.a, 1.0)
    );

    vec2 noiseUv = texCoord * max(0.01, noiseScale) + noiseScroll * AnimationTime * 20.0;
    float noise = valueNoise(noiseUv);
    float pulse = 0.5 + 0.5 * sin(AnimationTime * 6.2831853 * max(0.01, colorMixSpeed));
    float mixValue = clamp(pulse + (noise - 0.5) * max(0.0, noiseIntensity) * 2.0, 0.0, 1.0);

    vec3 mixedColor = mix(primaryColor, secondaryColor, mixValue);
    fragColor = vec4(mixedColor, packedMask.a);
}