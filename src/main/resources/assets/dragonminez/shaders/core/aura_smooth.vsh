#version 150

// Smooth 3D aura: steady, "clean" flame. The silhouette comes from the flame-profile mesh
// (AuraMeshFactory#getFlameMesh); this shader only adds the licks that travel up it.

in vec3 Position;
in vec4 Color;
in vec3 Normal;

out vec3 vNormalWorld;
out vec3 v_viewDir;
out float vHeight;
out float vWave;

uniform mat4 modelMatrix;
uniform mat4 ProjMat;
uniform mat4 normalMatrix;
uniform float time;
uniform float auravar;

const float PI = 3.14159265359;

// Period 1, so an integer multiplier keeps the atan() seam at +-PI continuous.
float triangleWave(float x) {
    return abs(fract(x) * 2.0 - 1.0);
}

void main() {
    vec3 base = Position;
    float h = clamp(base.y * 0.5 + 0.5, 0.0, 1.0);

    // Angular ridges, twisted with height so the licks spiral up instead of running straight.
    float angle = atan(base.z, base.x);
    float ridges = triangleWave(angle * (11.0 / (2.0 * PI)) + h * 0.85 + time * 0.35);
    ridges = pow(ridges, 1.35);

    // Bands travelling up the body, sharpened into points rather than soft bulges.
    float travel = triangleWave(h * 2.6 - time * 5.5);
    travel = pow(travel, 1.6);

    // Nothing moves at the root or right at the tip, so the flame silhouette stays readable.
    float falloff = smoothstep(0.0, 0.14, h) * (1.0 - smoothstep(0.90, 1.0, h));
    float rise = 0.45 + 1.05 * h;

    float wave = travel * ridges * falloff * rise;

    // Outwards low down, upwards near the tip.
    vec3 radial = normalize(vec3(base.x, 0.0, base.z) + vec3(1e-4, 0.0, 0.0));
    vec3 dir = normalize(mix(radial, vec3(0.0, 1.0, 0.0), 0.30 + 0.55 * h));

    vec3 pos = base;
    pos.xz *= (auravar * auravar * (3.0 - 2.0 * auravar));
    pos += dir * (wave * 0.55) * pow(auravar, 6.0);

    vec3 n = normalize(mix(normalize(Normal), dir, 0.35));

    vec4 viewPos = modelMatrix * vec4(pos, 1.0);
    vNormalWorld = normalize((normalMatrix * vec4(n, 0.0)).xyz);
    v_viewDir = -viewPos.xyz;
    vHeight = h;
    vWave = wave;

    gl_Position = ProjMat * viewPos;
}
