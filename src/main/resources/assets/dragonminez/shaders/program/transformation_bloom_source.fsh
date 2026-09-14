#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D GlowSampler;
uniform float BloomStrength;
uniform float GlowStrength;

in vec2 texCoord;
out vec4 fragColor;

void main() {
	vec4 coloredMask = texture(DiffuseSampler, texCoord);
	vec4 blurred = texture(GlowSampler, texCoord);

	float coverage = clamp(coloredMask.a, 0.0, 1.0);
	float blurredAlpha = clamp(blurred.a, 0.0, 1.0);
	vec3 blurredColor = blurredAlpha > 0.0001 ? clamp(blurred.rgb / blurredAlpha, 0.0, 1.0) : vec3(0.0);

	float exterior = max(0.0, blurredAlpha - coverage);
	float ring = smoothstep(0.12, 0.5, exterior);
	ring = clamp(ring * max(0.0, GlowStrength), 0.0, 1.0);

	vec3 source = blurredColor * ring + blurredColor * exterior * max(0.0, BloomStrength);
	fragColor = vec4(clamp(source, 0.0, 1.0), 1.0);
}
