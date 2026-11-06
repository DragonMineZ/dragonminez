const float AURA_WHITE_CAP = 0.10;
const float AURA_KEEP_SATURATION = 0.75;

float auraSaturation(vec3 c) {
    float peak = max(c.r, max(c.g, c.b));
    float low = min(c.r, min(c.g, c.b));
    return peak > 1.0e-4 ? (peak - low) / peak : 0.0;
}

vec3 auraBrightest(vec3 c) {
    return c / max(max(c.r, max(c.g, c.b)), 1.0e-4);
}

vec3 auraHighlight(vec3 c, float t) {
    t = clamp(t, 0.0, 1.0);
    return mix(mix(c, auraBrightest(c), t), vec3(1.0), t * AURA_WHITE_CAP);
}

vec3 auraShade(vec3 c, float depth) {
    return c * (1.0 - clamp(depth, 0.0, 1.0));
}

vec3 auraKeepSaturation(vec3 c, vec3 base) {
    float minSat = auraSaturation(base) * AURA_KEEP_SATURATION;
    float peak = max(c.r, max(c.g, c.b));
    float sat = auraSaturation(c);
    if (sat >= minSat || peak < 1.0e-4) return c;
    if (sat < 1.0e-3) return mix(vec3(peak), auraBrightest(base) * peak, AURA_KEEP_SATURATION);
    return vec3(peak) - (vec3(peak) - c) * (minSat / sat);
}
