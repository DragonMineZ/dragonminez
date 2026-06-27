#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D OutlineSampler;
uniform sampler2D EntityMaskSampler;
uniform vec2 InSize;
uniform float BloomStrength;
uniform float BloomRadius;

in vec2 texCoord;
out vec4 fragColor;

// Noise function to jitter samples
float hash(vec2 p) {
    return fract(sin(dot(p, vec2(127.1, 311.7))) * 43758.5453);
}

vec2 jitter(vec2 uv, int x, int y) {
    vec2 seed = uv * 1000.0 + vec2(float(x), float(y));
    return (vec2(hash(seed), hash(seed + 41.0)) - 0.5) * 0.7;
}

float blurMask(vec2 uv, vec2 texel, float pixelRadius) {
    float sum = 0.0;
    float totalWeight = 0.0;

    // Cap step size so samples always overlap (max ~2-3 pixels apart)
    float maxStep = 2.5;
    int samples = clamp(int(ceil(pixelRadius / maxStep)), 4, 24);
    float stepSize = pixelRadius / float(samples);

    for (int x = -samples; x <= samples; x++) {
        for (int y = -samples; y <= samples; y++) {
            vec2 jitterOffset = jitter(uv, x, y) * stepSize * 0.4;
            vec2 offset = (vec2(float(x), float(y)) * stepSize + jitterOffset) * texel;

            float distSq = float(x * x + y * y);
            float maxDistSq = float(samples * samples);
            float weight = exp(-distSq / (maxDistSq * 0.6));

            sum += texture(EntityMaskSampler, clamp(uv + offset, 0.0, 1.0)).a * weight;
            totalWeight += weight;
        }
    }

    return sum / totalWeight;
}

vec3 findOutlineColor(vec2 uv, vec2 texel, float searchRadius) {
    vec3 colorSum = vec3(0.0);
    float weightSum = 0.0;

    // Cap step size to prevent holes
    float maxStep = 3.0;
    int samples = clamp(int(ceil(searchRadius / maxStep)), 4, 20);
    float stepSize = searchRadius / float(samples);

    for (int x = -samples; x <= samples; x++) {
        for (int y = -samples; y <= samples; y++) {
            vec2 jitterOffset = jitter(uv, x + 100, y + 100) * stepSize * 0.4;
            vec2 offset = (vec2(float(x), float(y)) * stepSize + jitterOffset) * texel;
            vec4 s = texture(OutlineSampler, clamp(uv + offset, 0.0, 1.0));

            if (s.a > 0.05) {
                float distSq = float(x * x + y * y);
                float weight = s.a * exp(-distSq / float(samples * samples));
                colorSum += s.rgb * weight;
                weightSum += weight;
            }
        }
    }

    return weightSum > 0.01 ? colorSum / weightSum : vec3(0.0);
}

void main() {
    vec2 uv = texCoord;
    vec2 texel = 1.0 / InSize;

    vec3 scene = texture(DiffuseSampler, uv).rgb;
    vec4 outline = texture(OutlineSampler, uv);
    float entityMask = texture(EntityMaskSampler, uv).a;

    // Blur silhouette at multiple radii for smooth falloff
    float inner = blurMask(uv, texel, BloomRadius * 6.0);
    float mid   = blurMask(uv, texel, BloomRadius * 14.0);
    float outer = blurMask(uv, texel, BloomRadius * 28.0);

    // Exterior only - smooth edge transition
    float softMask = smoothstep(0.4, 0.6, entityMask);
    float glowInner = max(0.0, inner - softMask);
    float glowMid   = max(0.0, mid - softMask * 0.9);
    float glowOuter = max(0.0, outer - softMask * 0.7);

    // Combine layers with smooth falloff curve
    float glowIntensity = glowInner + glowMid * 0.5 + glowOuter * 0.25;
    glowIntensity = pow(smoothstep(0.0, 0.8, glowIntensity), 0.85);

    // Find outline color from nearby edge pixels
    vec3 bloomColor = findOutlineColor(uv, texel, BloomRadius * 25.0);

    // Composite: scene + bloom + sharp outline on top
    vec3 result = scene;
    result += bloomColor * glowIntensity * BloomStrength * 1.5;
    result += outline.rgb * outline.a;
    
    fragColor = vec4(clamp(result, 0.0, 1.0), 1.0);
}