#version 150

in vec3 Position;
in vec2 UV;

uniform mat4 modelMatrix;
uniform mat4 ProjMat;
uniform float time;
uniform float seed;
uniform float mode;
uniform float height;
uniform float radiusBase;
uniform float radiusTop;
uniform float skirt;
uniform float swayAmp;

out vec2 vUv;
out vec3 vNormal;
out vec3 vViewDir;

void main() {
    vec3 pos;
    vec3 normal;

    if (mode > 0.5) {
        pos = vec3(Position.x * radiusBase, 0.0, Position.z * radiusBase);
        normal = vec3(0.0, 1.0, 0.0);
    } else {
        float v = UV.y;
        float a = UV.x * 6.2831853;

        float r = radiusBase + (radiusTop - radiusBase) * pow(v, 1.35) + skirt * exp(-v * 9.0);
        r *= 1.0 + 0.05 * sin(v * 11.0 - time * 5.0 + a * 2.0 + seed)
                 + 0.035 * sin(v * 23.0 - time * 7.5 - a * 3.0 + seed * 1.7);

        vec2 sway = swayAmp * pow(v, 1.4) * vec2(sin(time * 1.7 + v * 3.1), cos(time * 1.3 + v * 2.6));

        pos = vec3(cos(a) * r + sway.x, v * height, sin(a) * r + sway.y);
        normal = vec3(cos(a), 0.0, sin(a));
    }

    vec4 viewPos = modelMatrix * vec4(pos, 1.0);
    gl_Position = ProjMat * viewPos;

    vUv = UV;
    vNormal = normalize(mat3(modelMatrix) * normal);
    vViewDir = normalize(-viewPos.xyz);
}
