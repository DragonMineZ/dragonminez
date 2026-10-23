#version 150

// Separable gaussian. Neighbouring taps are paired into one bilinear fetch placed at their weighted
// centre, so a radius of R texels costs about R/2 + 1 fetches per direction.

uniform sampler2D Source;
uniform vec2 Texel;
uniform vec2 Direction;
uniform float Radius;

in vec2 texCoord;
out vec4 fragColor;

const int MAX_TAPS = 16;

float gauss(float x, float sigma) {
    return exp(-0.5 * x * x / (sigma * sigma));
}

void main() {
    float radius = clamp(Radius, 1.0, float(MAX_TAPS));
    float sigma = max(radius * 0.5, 0.5);

    float centre = gauss(0.0, sigma);
    vec4 sum = texture(Source, texCoord) * centre;
    float weightSum = centre;

    for (int i = 1; i <= MAX_TAPS; i += 2) {
        float a = float(i);
        if (a > radius) break;
        float wa = gauss(a, sigma);
        float wb = gauss(a + 1.0, sigma);
        float w = wa + wb;
        float offset = (a * wa + (a + 1.0) * wb) / w;
        vec2 step = Direction * Texel * offset;
        sum += (texture(Source, texCoord + step) + texture(Source, texCoord - step)) * w;
        weightSum += 2.0 * w;
    }

    fragColor = sum / weightSum;
}
