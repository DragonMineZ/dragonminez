#version 150

uniform sampler2D DiffuseSampler;
uniform float Intensity;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    float i = clamp(Intensity, 0.0, 1.0);

    float luma = dot(color.rgb, vec3(0.299, 0.587, 0.114));

    // Deep, dark blood-red. Keeps some scene structure through luminance but stays
    // dark and saturated rather than bright/pink.
    vec3 darkRed = vec3(0.20 + 0.45 * luma, 0.0, 0.0);

    vec3 result = mix(color.rgb, darkRed, i * 0.92);

    // overall darkening that grows with gravity
    result *= (1.0 - 0.45 * i);

    // strong radial vignette toward the edges
    vec2 d = texCoord - vec2(0.5);
    float vignette = 1.0 - i * 0.9 * dot(d, d) * 4.0;
    result *= clamp(vignette, 0.0, 1.0);

    fragColor = vec4(result, color.a);
}
