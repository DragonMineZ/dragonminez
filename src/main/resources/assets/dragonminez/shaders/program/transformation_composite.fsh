#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D ColoredMaskSampler;
uniform sampler2D GlowSampler;
uniform sampler2D EdgeSampler;
uniform float BloomStrength;
uniform float GlowStrength;

in vec2 texCoord;
out vec4 fragColor;

void main() {
	vec3 scene = texture(DiffuseSampler, texCoord).rgb;
	vec4 coloredMask = texture(ColoredMaskSampler, texCoord);
	vec4 blurred = texture(GlowSampler, texCoord);
	float edge = texture(EdgeSampler, texCoord).r;

	float entityMask = coloredMask.a;
	float blurredAlpha = clamp(blurred.a, 0.0, 1.0);
	float exteriorGlowMask = max(0.0, blurredAlpha - entityMask);
	vec3 blurredColor = blurredAlpha > 0.0001 ? (blurred.rgb / blurredAlpha) : vec3(0.0);
	blurredColor = clamp(blurredColor, 0.0, 1.0);

	vec3 glow = blurredColor * exteriorGlowMask * max(0.0, BloomStrength);
	vec3 outline = coloredMask.rgb * edge * max(0.0, GlowStrength);

	vec3 result = scene + glow + outline;
	fragColor = vec4(clamp(result, 0.0, 1.0), 1.0);
}
