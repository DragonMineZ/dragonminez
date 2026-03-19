#version 150

in vec3 Position;
in vec2 UV;

out vec2 v_texCoord;

uniform mat4 modelMatrix;
uniform mat4 ProjMat;

void main() {
    v_texCoord = UV;
    gl_Position = ProjMat * modelMatrix * vec4(Position, 1.0);
}