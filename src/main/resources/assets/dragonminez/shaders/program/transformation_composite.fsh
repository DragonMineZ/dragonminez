#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D ColoredMaskSampler;
uniform sampler2D GlowSampler;
uniform sampler2D BloomSampler;
uniform float BloomStrength;
uniform float GlowStrength;
uniform float OutlineBloomStrength;

in vec2 texCoord;
out vec4 fragColor;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

void main() {
	vec3 scene = texture(DiffuseSampler, texCoord).rgb;
	vec4 coloredMask = texture(ColoredMaskSampler, texCoord);
	vec4 blurred = texture(GlowSampler, texCoord);

	float coverage = clamp(coloredMask.a, 0.0, 1.0);
	float blurredAlpha = clamp(blurred.a, 0.0, 1.0);
	vec3 blurredColor = blurredAlpha > 0.0001 ? clamp(blurred.rgb / blurredAlpha, 0.0, 1.0) : vec3(0.0);

	float exterior = max(0.0, blurredAlpha - coverage);
	float ring = smoothstep(0.12, 0.5, exterior);
	ring = clamp(ring * max(0.0, GlowStrength), 0.0, 1.0);

	vec3 glow = blurredColor * exterior * max(0.0, BloomStrength);
	vec3 outline = blurredColor * ring;

	vec3 bloom = texture(BloomSampler, texCoord).rgb * max(0.0, OutlineBloomStrength);
	bloom = bloom / (1.0 + bloom);

	float lit = smoothstep(0.01, 0.05, dot(blurredColor, LUMA));
	float shade = (1.0 - lit) * clamp(ring + exterior * max(0.0, BloomStrength), 0.0, 1.0);

	vec3 result = mix(scene + (outline + glow) * lit, blurredColor, shade) + bloom;
	fragColor = vec4(clamp(result, 0.0, 1.0), 1.0);
}
