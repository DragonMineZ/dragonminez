#version 150

uniform sampler2D DiffuseSampler;
uniform float treshhold;
uniform float treshholdLerp;
uniform float invert;

in vec2 texCoord;
out vec4 fragColor;

const vec2 points[9] = vec2[9](
vec2(0.25, 0.25), vec2(0.5, 0.25), vec2(0.75, 0.25),
vec2(0.25, 0.5),  vec2(0.5, 0.5),  vec2(0.75, 0.5),
vec2(0.25, 0.75), vec2(0.5, 0.75), vec2(0.75, 0.75)
);

float estimateGrayscale() {
    float gray = 0.0;
    for (int i = 0; i < 9; i++) {
        vec4 pixel = texture(DiffuseSampler, points[i]);
        float g = max(pixel.r, max(pixel.g, pixel.b));
        gray = max(gray, g);
    }
    return max(gray, 0.0001);
}

void main() {
    float estimatedGray = estimateGrayscale();
    vec4 color = texture(DiffuseSampler, texCoord);

    float gray = max(max(color.x, color.y), color.z);
    gray /= estimatedGray;

    if (gray > treshhold) {
        gray = 1.0;
    } else {
        if (treshholdLerp != 0.0) {
            float v = treshhold - gray;
            gray = smoothstep(0.0, 1.0, 1.0 - min(1.0, v / treshholdLerp));
        } else {
            gray = 0.0;
        }
    }

    if (invert > 0.0) {
        gray = 1.0 - gray;
    }

    fragColor = vec4(gray, gray, gray, color.a);
}