#version 150

in vec2 v_texCoord;
out vec4 fragColor;

uniform sampler2D Sampler0;
uniform float speed;
uniform float alp1;

uniform vec4 color1;
uniform vec4 color2;
uniform vec4 color3;
uniform vec4 color4;

const float frameWidth = 1.0 / 4.0;
const float thresholds[4] = float[](0.0, 0.5, 0.7, 0.97);

vec4 adjustColor(vec4 colorTex) {
    vec3 newColor = vec3(0.0);
    float alphaOut = 0.0;
    float intensity = colorTex.r;

    if (intensity >= thresholds[3]) {
        newColor = color1.rgb;
        alphaOut = color1.a;
    } else if (intensity >= thresholds[2]) {
        newColor = color2.rgb;
        alphaOut = color2.a;
    } else if (intensity > thresholds[1]) {
        newColor = color3.rgb;
        alphaOut = color3.a;
    } else if (intensity > 0.0) {
        newColor = color4.rgb;
        alphaOut = color4.a;
    }

    return vec4(newColor, alphaOut * intensity);
}

void main() {
    // Aislar índices de los frames
    float currentFrameIdx = floor(mod(speed, 4.0));
    float nextFrameIdx = floor(mod(speed + 1.0, 4.0));

    // Calcular UV offset para el spritesheet (1 fila x 4 columnas)
    vec2 currentUV = vec2((v_texCoord.x * frameWidth) + (currentFrameIdx * frameWidth), v_texCoord.y);
    vec2 nextUV = vec2((v_texCoord.x * frameWidth) + (nextFrameIdx * frameWidth), v_texCoord.y);

    // Mapear gradientes
    vec4 currentFrameColor = adjustColor(texture(Sampler0, currentUV));
    vec4 nextFrameColor = adjustColor(texture(Sampler0, nextUV));

    // Interpolación suave tipo DBKakarot
    vec4 finalColor = mix(currentFrameColor, nextFrameColor, fract(speed));

    if (finalColor.a < 0.01 || alp1 < 0.01) {
        discard;
    }

    fragColor = vec4(finalColor.rgb, finalColor.a * alp1);
}