#version 150

uniform sampler2D DiffuseSampler;
uniform float Time;
uniform float Intensity;

in vec2 texCoord;
out vec4 fragColor;

void main() {
    vec4 color = texture(DiffuseSampler, texCoord);
    float i = clamp(Intensity, 0.0, 1.0);

    float luma = dot(color.rgb, vec3(0.299, 0.587, 0.114));
    vec3 result = mix(color.rgb, vec3(luma), 0.65 * i);
    result *= 1.0 - 0.18 * i;

    vec2 centered = abs(texCoord - vec2(0.5)) * 2.0;
    float edge = max(centered.x, centered.y);
    float radial = length(texCoord - vec2(0.5)) * 1.4142;
    float border = smoothstep(0.55, 1.0, max(edge, radial));

    float pulse = 0.82 + 0.18 * sin(Time * 6.2831853 * 0.75);
    vec3 red = vec3(0.62, 0.02, 0.02);
    result = mix(result, red, border * 0.8 * i * pulse);
    result *= 1.0 - border * 0.35 * i;

    fragColor = vec4(result, 1.0);
}
