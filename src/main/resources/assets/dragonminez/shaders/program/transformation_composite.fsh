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
	float exteriorGlowMask = max(0.0, blurred.a - entityMask);

	vec3 glow = blurred.rgb * exteriorGlowMask * max(0.0, BloomStrength);
	vec3 outline = coloredMask.rgb * edge * max(0.0, GlowStrength);

	vec3 result = scene + glow + outline;
	fragColor = vec4(clamp(result, 0.0, 1.0), 1.0);
}
