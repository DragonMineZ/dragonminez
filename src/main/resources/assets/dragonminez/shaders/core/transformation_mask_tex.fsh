#version 150

uniform sampler2D Sampler0;

in vec4 vertexColor;
in vec2 texCoord0;

out vec4 fragColor;

void main() {
    float texAlpha = texture(Sampler0, texCoord0).a;
    if (texAlpha < 0.1) {
        discard;
    }
    fragColor = vec4(vertexColor.rgb, vertexColor.a);
}
