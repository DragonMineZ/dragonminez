#version 150

uniform sampler2D Mask;
uniform vec2 MaskTexel;
uniform float Footprint;
uniform float Id;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    float stride = Footprint * 0.25;
    float sum = 0.0;
    for (int y = 0; y < 4; y++) {
        for (int x = 0; x < 4; x++) {
            vec2 offset = (vec2(float(x), float(y)) - 1.5) * stride;
            vec4 m = texture(Mask, texCoord + offset * MaskTexel);
            sum += (m.a > 0.5 && abs(m.r - Id) < 0.002) ? 1.0 : 0.0;
        }
    }
    fragColor = vec4(sum / 16.0);
}
