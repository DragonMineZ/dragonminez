#version 150

uniform sampler2D Sampler0;
uniform vec4 ColorModulator;
uniform float GameTime;

in vec2 texCoord0;
in vec4 vertexColor;

out vec4 fragColor;

// Ruido para el efecto de lava
float hash(vec2 p) {
    p = fract(p * vec2(123.34, 456.21));
    p += dot(p, p + 45.32);
    return fract(p.x * p.y);
}

float noise(vec2 p) {
    vec2 i = floor(p);
    vec2 f = fract(p);
    float a = hash(i);
    float b = hash(i + vec2(1.0, 0.0));
    float c = hash(i + vec2(0.0, 1.0));
    float d = hash(i + vec2(1.0, 1.0));
    vec2 u = f * f * (3.0 - 2.0 * f);
    return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

void main() {
    float time = GameTime * 1000.0;

    // Como tu textura usa 1/4 (arriba-izquierda), limitamos las UVs
    // Si tu plano en Java manda UVs de 0 a 1, esto las ajusta al cuadrante del círculo
    vec2 uv = texCoord0 * 0.5;

    // Distorsión de Lava
    float n = noise(uv * 20.0 + time * 0.5);
    vec2 distortedUV = uv + (n * 0.015);

    vec4 color = texture(Sampler0, distortedUV);

    // Si el píxel es negro en tu imagen (fondo), lo descartamos
    if (color.r < 0.1 && color.g < 0.1 && color.b < 0.1) discard;

    // Aplicamos el color de la Genkidama (Azul/Blanco)
    // El vertexColor trae el 'borderColor' o 'coreColor' que mandas desde Java
    color *= vertexColor * ColorModulator;

    // --- EFECTO BRILLO NUCLEAR ---
    // Hacemos que el centro sea mucho más blanco e incandescente
    float dist = distance(texCoord0, vec2(0.5));
    float glow = pow(1.0 - dist, 4.0);
    color.rgb += glow * 0.6; // Resplandor central

    fragColor = color;
}