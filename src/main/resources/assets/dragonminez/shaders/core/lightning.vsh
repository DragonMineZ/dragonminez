#version 150

in vec3 Position;
in vec4 Color;
in vec3 Normal;

out vec3 vNormalWorld;
out vec3 v_viewDir;
out vec2 vLocalXZ;

uniform mat4 modelMatrix;
uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;
uniform mat4 normalMatrix;
uniform vec3 view;
uniform float time;
uniform float speedModifier;

const float HEIGHT = 3.0;
const float AMPLITUDE = 0.95;
const float THICKNESS = 1.3;

float hash11(float n) {
    return fract(sin(n * 127.1) * 43758.5453);
}

float kink(float y, float stepSize, float salt) {
    float x = y / stepSize;
    float i = floor(x + 1.0e-4);
    float f = clamp(x - i, 0.0, 1.0);
    float a = hash11(i * 1.31 + salt) * 2.0 - 1.0;
    float b = hash11((i + 1.0) * 1.31 + salt) * 2.0 - 1.0;
    return mix(a, b, f);
}

float jagged(float y, float salt) {
    return kink(y, 0.5, salt) * 0.62 + kink(y, 0.3, salt + 31.0) * 0.32 + kink(y, 0.1, salt + 77.0) * 0.06;
}

void main() {
    vLocalXZ = Position.xz;

    float t = clamp(Position.y / HEIGHT, 0.0, 1.0);

    float activeSpeed = 12.0 * speedModifier;
    float salt = mod(floor(time * activeSpeed), 4096.0) * 17.0;

    float envelope = smoothstep(0.0, 0.10, t) * mix(t, 1.0, 0.35);
    float offsetX = jagged(Position.y, salt) * AMPLITUDE * envelope;
    float offsetZ = jagged(Position.y, salt + 211.0) * AMPLITUDE * envelope;

    float widthNoise = 0.5 + 0.5 * kink(Position.y, 0.4, salt + 113.0);
    float rootTaper = mix(0.3, 1.0, smoothstep(0.0, 0.2, t));
    float tipTaper = pow(max(0.0, 1.0 - t), 0.6);
    float thickness = THICKNESS * (0.22 + 1.0 * pow(widthNoise, 1.4)) * rootTaper * tipTaper;

    vec4 pos = vec4(Position, 1.0);
    pos.x = Position.x * thickness + offsetX;
    pos.z = Position.z * thickness + offsetZ;

    pos.x += max(0.0, t - 0.25) * 0.5;
    pos.z += max(0.0, t - 0.50) * -0.4;

    vec4 viewPos = modelMatrix * vec4(pos.xyz, 1.0);

    vNormalWorld = normalize((normalMatrix * vec4(Normal, 0.0)).xyz);
    v_viewDir = -viewPos.xyz;

    gl_Position = projectionMatrix * viewPos;
}
