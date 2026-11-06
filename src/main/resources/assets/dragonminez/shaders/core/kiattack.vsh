#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform float localPosMode;

out vec3 vNormal;
out vec3 vViewDir;
out vec3 vLocalPos;
out vec2 vUv;
out float vAlpha;
out float vInside;

void main() {
    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPos;

    float meshRadius = length(ModelViewMat[0].xyz);
    float centreDist = length(ModelViewMat[3].xyz);
    vInside = 1.0 - smoothstep(meshRadius * 0.8, meshRadius * 1.05, centreDist);

    vNormal = normalize(mat3(ModelViewMat) * Normal);
    vViewDir = normalize(-viewPos.xyz);
    if (localPosMode > 0.5) {
        vLocalPos = vec3(Color.r * 2.0 - 1.0, Color.g * 2.0 - 1.0, Color.b);
        vAlpha = Color.a;
    } else {
        vLocalPos = Position;
        vAlpha = 1.0;
    }
    vUv = UV0;
}