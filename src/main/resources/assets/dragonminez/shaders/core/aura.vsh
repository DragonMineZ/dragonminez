#version 150

in vec3 Position;
in vec4 Color;
in vec2 TexCoord0;
in vec2 LightmapUV;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;

out vec4 vertexColor;
out vec2 texCoord0;
out vec2 lightmapUV;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    texCoord0 = TexCoord0;
    lightmapUV = LightmapUV;
}