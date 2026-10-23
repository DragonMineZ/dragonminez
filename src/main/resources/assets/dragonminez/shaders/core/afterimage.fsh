#version 150

uniform sampler2D Sampler0;

uniform float bandPass;
uniform float centerWeight;
uniform float bandHeight;
uniform float dropout;
uniform float whiten;
uniform float globalAlpha;
uniform float time;

in vec4 vertexColor;
in vec4 lightMapColor;
in vec2 texCoord0;

out vec4 fragColor;

float hash(float n) {
    return fract(sin(n * 12.9898) * 43758.5453);
}

void main() {
    vec4 color = texture(Sampler0, texCoord0);
    if (color.a < 0.1) discard;

    float row = floor(gl_FragCoord.y / max(bandHeight, 1.0));
    float shuffle = floor(time * 10.0);

    float pick = hash(row + shuffle * 17.0);
    float sideSplit = centerWeight + (1.0 - centerWeight) * 0.5;
    float band = pick < centerWeight ? 0.0 : (pick < sideSplit ? 1.0 : 2.0);
    if (abs(band - bandPass) > 0.5) discard;

    float keep = hash(row * 3.7 + shuffle * 5.3 + 11.0);
    if (keep < dropout) discard;

    color *= vertexColor * lightMapColor;
    color.rgb = mix(color.rgb, vec3(1.0), whiten);
    color.a *= globalAlpha;
    fragColor = color;
}
