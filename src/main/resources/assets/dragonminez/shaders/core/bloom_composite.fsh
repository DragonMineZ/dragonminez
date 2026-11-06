#version 150

// Lays the blurred bloom mask back over the scene, drawn with ONE / ONE_MINUS_SRC_ALPHA.
//
// The three pyramid levels are summed into one wide halo, then split by brightness:
//   - bright colours glow additively, tone-mapped so the hot core rolls off instead of clipping to white;
//   - dark colours cannot brighten anything, so they are laid over the scene as a tinted veil instead,
//     which is what keeps a black or deep-red aura visible in daylight.

uniform sampler2D Bloom0;
uniform sampler2D Bloom1;
uniform sampler2D Bloom2;
uniform float Intensity;
uniform float Spread;

in vec2 texCoord;
out vec4 fragColor;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

// 0 favours the tight level, 1 the wide one.
float levelWeight(float tight) {
    return mix(tight, 1.2 - tight, Spread);
}

vec3 tonemap(vec3 c) {
    float l = dot(c, LUMA);
    vec3 tc = c / (c + 1.0);
    return mix(c / (l + 1.0), tc, tc);
}

void main() {
    vec4 bloom = texture(Bloom0, texCoord) * levelWeight(0.8)
               + texture(Bloom1, texCoord) * levelWeight(0.6)
               + texture(Bloom2, texCoord) * levelWeight(0.4);

    if (bloom.a < 0.0005) discard;

    // The mask is premultiplied by coverage; divide it back out to get the colour the aura actually is.
    vec3 pure = bloom.rgb / bloom.a;
    float luminance = clamp(dot(pure, LUMA), 0.0, 1.0);
    float additive = smoothstep(0.0, 1.0, luminance);
    float veil = clamp(1.0 - additive, 0.3, 1.0);

    float veilAlpha = clamp(bloom.a * Intensity * veil, 0.0, 1.0);
    vec3 glow = tonemap(bloom.rgb * Intensity * additive);

    fragColor = vec4(pure * veilAlpha + glow, veilAlpha);
}
