#version 150
#extension GL_ARB_explicit_attrib_location : enable

#moj_import <dragonminez:aura_color.glsl>

// Hollow shell shading: nearly clear where the shell faces the camera so the body stays readable, solid
// along the silhouette where the tongues are.

in vec3 vNormal;
in vec3 vView;

uniform sampler2D NoiseTex;
uniform float Time;
uniform vec3 CoreColor;
uniform vec3 RimColor;
uniform vec3 NoiseColor;
uniform float NoiseFactor;
uniform float CoreAlpha;
uniform float RimAlpha;
uniform float RimPower;
uniform float RimThreshold;
uniform float Growth;
uniform float Alpha;
uniform float BackFace;
uniform float BloomIntensity;
// Set when this draw feeds the bloom mask instead of the scene.
uniform float BloomPass;

layout(location = 0) out vec4 fragColor;

const float INTERIOR_TINT = 0.25;

void main() {
    vec3 N = normalize(vNormal);
    vec3 V = normalize(vView);

    float facing = dot(V, N);
    float rim = clamp(pow(1.0 - abs(facing), RimPower) / RimThreshold, 0.0, 1.0);

    vec3 color = mix(CoreColor, RimColor, rim);
    float alpha = mix(CoreAlpha, RimAlpha, rim);

    // Shimmer keyed to the view-space normal, so it drifts across the shell as it turns and climbs.
    vec2 shimmerUv = vec2(N.x * 0.25 + 0.5, (N.y * 0.5 + 0.5) * 0.25 - Time * 0.25);
    float shimmer = texture(NoiseTex, shimmerUv).g;
    vec3 tinted = mix(NoiseColor, color * mix(0.8, 1.0, shimmer), shimmer);
    color = mix(color, tinted, NoiseFactor);
    color = auraKeepSaturation(color, RimColor);

    // A floor of coverage so the player and the background read through the shell tinted.
    alpha = max(alpha, INTERIOR_TINT * clamp(Growth, 0.0, 1.0));

    // The far wall would wash over the near one; it is only raised when the camera sits inside the shell.
    if (facing < 0.0) alpha *= BackFace;
    alpha *= Alpha;

    vec4 glow = vec4(color, clamp(alpha * BloomIntensity, 0.0, 1.0));
    fragColor = BloomPass > 0.5 ? glow : vec4(color, alpha);
}
