#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float intensity;
uniform float invert;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec2 px = 1.0 / InSize;

    // light blur so the screen is hard to read but motion still reads
    vec3 sum = vec3(0.0);
    float total = 0.0;
    for (int x = -1; x <= 1; x++) {
        for (int y = -1; y <= 1; y++) {
            vec2 off = vec2(float(x), float(y)) * px * 2.0;
            sum += texture(DiffuseSampler, texCoord + off).rgb;
            total += 1.0;
        }
    }
    vec3 blurred = sum / total;

    float gray = max(max(blurred.r, blurred.g), blurred.b);
    // mild detail crush so only soft motion remains
    gray = floor(gray * 8.0) / 8.0;

    // mostly white with only faint dark motion bleeding through
    float flare = mix(gray * 0.25, 1.0, 0.85);

    if (invert > 0.5) flare = 1.0 - flare;

    vec3 orig = texture(DiffuseSampler, texCoord).rgb;
    vec3 outc = mix(orig, vec3(flare), clamp(intensity, 0.0, 1.0));

    fragColor = vec4(outc, 1.0);
}
