#version 150

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in vec3 Normal;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec3 vNormal;
out vec3 vViewDir;
out vec3 vLocalPos;
out vec2 vUv;

void main() {
    vec4 viewPos = ModelViewMat * vec4(Position, 1.0);
    gl_Position = ProjMat * viewPos;

    vNormal = normalize(mat3(ModelViewMat) * Normal);
    vViewDir = normalize(-viewPos.xyz);
    vLocalPos = Position;
    vUv = UV0;
}