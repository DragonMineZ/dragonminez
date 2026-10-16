#version 150

#moj_import <light.glsl>

in vec3 Position;
in vec4 Color;
in vec2 UV0;
in ivec2 UV1;
in ivec2 UV2;
in vec3 Normal;

uniform sampler2D Sampler2;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform vec3 Light0_Direction;
uniform vec3 Light1_Direction;

out vec4 vertexColor;
out vec4 lightMapColor;
out vec2 texCoord0;
out float progress;
out float mode;
out float shade;

void main() {
    gl_Position = ProjMat * ModelViewMat * vec4(Position, 1.0);
    vertexColor = Color;
    shade = minecraft_mix_light(Light0_Direction, Light1_Direction, Normal, vec4(1.0)).r;
    lightMapColor = texelFetch(Sampler2, UV2 / 16, 0);
    texCoord0 = UV0;
    progress = clamp(float(UV1.x) / 1000.0, 0.0, 1.0);
    mode = float(UV1.y);
}
