#version 150

uniform sampler2D DiffuseSampler;
uniform float AlphaThreshold;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    float alpha = texture(DiffuseSampler, texCoord).a;
    float mask = step(AlphaThreshold, alpha);
    fragColor = vec4(mask, mask, mask, mask);
}