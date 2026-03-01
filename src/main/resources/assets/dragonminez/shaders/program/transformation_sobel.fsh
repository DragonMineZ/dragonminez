#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float OutlineThickness;
uniform float EdgeThreshold;
uniform float EdgeStrength;

in vec2 texCoord;
out vec4 fragColor;

float sampleMask(vec2 uv) {
    // Threshold alpha to binary: anything visible = 1.0
    float alpha = texture(DiffuseSampler, clamp(uv, vec2(0.0), vec2(1.0))).a;
    return step(0.01, alpha);  // Returns 1.0 if alpha > 0.01, else 0.0
}

void main() {
    vec2 uv = clamp(texCoord, vec2(0.0), vec2(1.0));
    vec2 texel = 1.0 / max(InSize, vec2(1.0));
    vec2 offset = texel * max(0.1, OutlineThickness);

    float tl = sampleMask(uv + offset * vec2(-1.0, -1.0));
    float tc = sampleMask(uv + offset * vec2(0.0, -1.0));
    float tr = sampleMask(uv + offset * vec2(1.0, -1.0));
    float ml = sampleMask(uv + offset * vec2(-1.0, 0.0));
    float mr = sampleMask(uv + offset * vec2(1.0, 0.0));
    float bl = sampleMask(uv + offset * vec2(-1.0, 1.0));
    float bc = sampleMask(uv + offset * vec2(0.0, 1.0));
    float br = sampleMask(uv + offset * vec2(1.0, 1.0));

    float gx = -tl - 2.0 * ml - bl + tr + 2.0 * mr + br;
    float gy = -tl - 2.0 * tc - tr + bl + 2.0 * bc + br;

    float edge = length(vec2(gx, gy));
    float edgeMask = smoothstep(EdgeThreshold, EdgeThreshold + 0.2, edge);
    edgeMask = clamp(edgeMask * max(0.0, EdgeStrength), 0.0, 1.0);

    fragColor = vec4(edgeMask, edgeMask, edgeMask, edgeMask);
}