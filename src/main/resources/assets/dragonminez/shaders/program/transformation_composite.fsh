#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D ColoredMaskSampler;
uniform sampler2D GlowSampler;
uniform sampler2D OuterSampler;
uniform sampler2D BloomSampler;
uniform vec2 OutSize;
uniform float BloomStrength;
uniform float GlowStrength;
uniform float OutlineBloomStrength;
uniform float AnimationTime;
uniform float OutlineRadius;
uniform float FireIntensity;
uniform float ColorMode;
uniform float BodyGlow;

in vec2 texCoord;
out vec4 fragColor;

const vec3 LUMA = vec3(0.2126, 0.7152, 0.0722);

const float FLAME_CELL = 0.5;
const float FLAME_STRETCH = 2.2;
const float FLAME_RISE = 2.4;
const float FLAME_LIFT = 1.1;
const float FLAME_SWAY = 0.6;
const float FLAME_BASE_KEEP = 0.8;
const float FLAME_BREAKUP = 0.8;
const float FLAME_FLICKER = 0.15;
const float FLAME_CORE_WHITEN = 0.35;
const float FLAME_TIP_DEEPEN = 1.8;
const float FLAME_RING_MIX = 0.9;
const float FLAME_CALM_DEPTH = 0.6;
const float FLAME_CALM_PERIOD = 9.0;

float hash(vec2 p) {
	vec3 p3 = fract(vec3(p.xyx) * 0.1031);
	p3 += dot(p3, p3.yzx + 33.33);
	return fract((p3.x + p3.y) * p3.z);
}

float valueNoise(vec2 p) {
	vec2 i = floor(p);
	vec2 f = fract(p);
	float a = hash(i);
	float b = hash(i + vec2(1.0, 0.0));
	float c = hash(i + vec2(0.0, 1.0));
	float d = hash(i + vec2(1.0, 1.0));
	vec2 u = f * f * (3.0 - 2.0 * f);
	return mix(mix(a, b, u.x), mix(c, d, u.x), u.y);
}

float flameNoise(vec2 q, float t) {
	float v = 0.55 * valueNoise(q - vec2(0.0, t));
	v += 0.30 * valueNoise(q * 2.03 + vec2(17.3, -2.6 * t));
	v += 0.15 * valueNoise(q * 4.07 + vec2(-8.1, -6.1 * t));
	return v;
}

void main() {
	vec3 scene = texture(DiffuseSampler, texCoord).rgb;
	float coverage = clamp(texture(ColoredMaskSampler, texCoord).a, 0.0, 1.0);

	vec2 screen = max(OutSize, vec2(1.0));
	float radius = max(OutlineRadius, 3.0);
	float cell = max(2.5, radius * FLAME_CELL);
	vec2 q = texCoord * screen / vec2(cell, cell * FLAME_STRETCH);
	float intensity = clamp(FireIntensity, 0.0, 3.0);
	float w1 = 6.2831853 / max(FLAME_CALM_PERIOD, 0.1);
	float w2 = w1 * 2.37;
	float wave = 0.6 * sin(w1 * AnimationTime) + 0.4 * sin(w2 * AnimationTime);
	float activity = 1.0 - FLAME_CALM_DEPTH * (0.5 - 0.5 * wave);
	float phase = (1.0 - 0.5 * FLAME_CALM_DEPTH) * AnimationTime - FLAME_CALM_DEPTH * (0.3 * cos(w1 * AnimationTime) / w1 + 0.2 * cos(w2 * AnimationTime) / w2);
	float motion = intensity * mix(0.5, 1.0, activity);
	float t = phase * FLAME_RISE * intensity;

	float rise = smoothstep(0.2, 0.8, flameNoise(q, t));
	float sway = valueNoise(q * 0.5 + vec2(3.7, -0.6 * t)) - 0.5;
	vec2 warp = vec2(sway * 2.0 * FLAME_SWAY, -rise * FLAME_LIFT) * motion * radius / screen;

	vec4 baseBlur = texture(GlowSampler, texCoord);
	vec4 flameBlur = texture(GlowSampler, texCoord + warp);

	float baseExterior = max(0.0, clamp(baseBlur.a, 0.0, 1.0) - coverage);
	float flameExterior = max(0.0, clamp(flameBlur.a, 0.0, 1.0) - coverage);

	vec4 combined = baseBlur + flameBlur;
	vec3 innerColor = combined.a > 0.0001 ? clamp(combined.rgb / combined.a, 0.0, 1.0) : vec3(0.0);

	float gradient = step(0.5, ColorMode);
	vec4 outerBlur = texture(OuterSampler, texCoord) + texture(OuterSampler, texCoord + warp);
	vec3 outerColor = outerBlur.a > 0.0001 ? clamp(outerBlur.rgb / outerBlur.a, 0.0, 1.0) : innerColor;
	outerColor = mix(innerColor, outerColor, gradient);

	float tongues = smoothstep(0.2, 0.8, flameNoise(q * 1.6 + vec2(41.0, 9.0), t * 1.35));
	float heat = max(baseExterior * FLAME_BASE_KEEP, flameExterior);
	heat *= max(0.0, 1.0 + (tongues - 0.5) * 2.0 * FLAME_BREAKUP * motion * clamp(1.0 - heat * 1.6, 0.0, 1.0));

	vec3 blurredColor = mix(outerColor, innerColor, smoothstep(0.04, 0.3, heat));

	float flicker = 1.0 + (valueNoise(vec2(texCoord.x * screen.x / (radius * 4.0), phase * intensity * 7.0)) - 0.5) * 2.0 * FLAME_FLICKER * motion;

	float fireAmount = clamp(intensity, 0.0, 1.0);
	float ring = smoothstep(mix(0.08, 0.04, fireAmount), mix(0.13, 0.38, fireAmount), heat);
	ring = clamp(ring * max(0.0, GlowStrength), 0.0, 1.0);
	float hot = smoothstep(mix(0.1, 0.3, fireAmount), mix(0.3, 0.55, fireAmount), heat);

	float bodyAlpha = clamp(baseBlur.a, 0.0, 1.0);
	vec3 bodyColor = bodyAlpha > 0.0001 ? clamp(baseBlur.rgb / bodyAlpha, 0.0, 1.0) : innerColor;
	float rim = (1.0 - smoothstep(0.55, 0.98, bodyAlpha)) * (1.0 + (tongues - 0.5) * 0.8 * fireAmount);
	rim = clamp(rim, 0.0, 1.0) * clamp(BodyGlow, 0.0, 1.0) * coverage;
	scene = mix(scene, bodyColor, rim);

	float lit = smoothstep(0.01, 0.05, dot(blurredColor, LUMA));
	vec3 tipColor = pow(blurredColor, vec3(FLAME_TIP_DEEPEN));
	vec3 coreColor = mix(blurredColor, vec3(1.0), FLAME_CORE_WHITEN * mix(1.0, 0.4, gradient) * hot);
	vec3 fireColor = mix(tipColor, coreColor, hot);

	vec3 glow = mix(tipColor, outerColor, gradient) * heat * max(0.0, BloomStrength) * flicker;
	vec3 outline = fireColor * ring * flicker;
	float paint = clamp(ring * flicker, 0.0, 1.0) * FLAME_RING_MIX * lit;

	vec3 bloom = texture(BloomSampler, texCoord + warp * 0.5).rgb * max(0.0, OutlineBloomStrength) * flicker;
	bloom = bloom / (1.0 + bloom);

	float shade = (1.0 - lit) * clamp(ring + heat * max(0.0, BloomStrength), 0.0, 1.0);

	vec3 lay = mix(scene, fireColor, paint) + (outline * (1.0 - FLAME_RING_MIX) + glow) * lit;
	vec3 result = mix(lay, blurredColor, shade) + bloom;
	fragColor = vec4(clamp(result, 0.0, 1.0), 1.0);
}
