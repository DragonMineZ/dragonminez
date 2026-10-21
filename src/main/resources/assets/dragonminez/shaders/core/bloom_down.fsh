#version 150

// 2:1 downsample. Four bilinear taps one source texel off-centre average a 4x4 block, so thin rims
// survive the reduction instead of flickering in and out as the aura moves.

uniform sampler2D Source;
uniform vec2 SourceTexel;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 d = SourceTexel;
    vec4 sum = texture(Source, texCoord + vec2(-d.x, -d.y))
             + texture(Source, texCoord + vec2( d.x, -d.y))
             + texture(Source, texCoord + vec2(-d.x,  d.y))
             + texture(Source, texCoord + vec2( d.x,  d.y));
    fragColor = sum * 0.25;
}
