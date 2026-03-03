#version 150

uniform sampler2D DiffuseSampler;
uniform vec2 InSize;
uniform float BloomRadius;

in vec2 texCoord;
out vec4 fragColor;

const int MAX_SAMPLES = 24;

void main() {
	vec2 texel = vec2(1.0 / max(InSize.x, 1.0), 0.0);
	float sampleRadius = max(0.0, BloomRadius);
	if (sampleRadius <= 0.001) {
		fragColor = texture(DiffuseSampler, clamp(texCoord, vec2(0.0), vec2(1.0)));
		return;
	}

	int sampleCount = int(clamp(ceil(sampleRadius), 1.0, float(MAX_SAMPLES)));
	float sigma = max(0.001, sampleRadius * 0.5);
	float invSigma2 = 1.0 / (2.0 * sigma * sigma);

	vec4 sum = vec4(0.0);
	float weightSum = 0.0;

	for (int i = -MAX_SAMPLES; i <= MAX_SAMPLES; i++) {
		if (abs(i) > sampleCount) {
			continue;
		}

		float fi = float(i);
		float weight = exp(-(fi * fi) * invSigma2);
		vec2 sampleUv = clamp(texCoord + texel * fi, vec2(0.0), vec2(1.0));
		sum += texture(DiffuseSampler, sampleUv) * weight;
		weightSum += weight;
	}

	fragColor = sum / max(weightSum, 0.0001);
}
