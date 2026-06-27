#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D BloomSampler;
uniform float BloomStrength;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec3 scene = texture(DiffuseSampler, texCoord).rgb;
    vec4 bloom = texture(BloomSampler, texCoord);
    vec3 bloomCol = bloom.rgb * bloom.a * BloomStrength;
    bloomCol = bloomCol / (1.0 + bloomCol);
    vec3 result = scene + bloomCol;
    fragColor = vec4(result, 1.0);
}
