#version 150

in vec3 vNormalWorld;
in vec3 v_viewDir;

uniform vec3 color1;
uniform vec3 color2;
uniform float alp1;
uniform float alp2;
uniform float power;
uniform float divis;

out vec4 fragColor;

void main(void) {
    vec3 N = normalize(vNormalWorld);
    vec3 V = normalize(v_viewDir);

    float facingRaw = dot(V, N);
	if (facingRaw < 0.0) {
        N = -N;
    }

    float facingAbs = abs(dot(V, N));

    float facingRatio = facingAbs;

    float edgeFactor = 1.0 - facingRatio;

    edgeFactor = pow(edgeFactor, power)/divis;
	float blendFactor = clamp(edgeFactor, 0.0, 1.0);

    vec3 innerColor = color1;
    vec3 outerColor = color2;
    vec3 color = mix(innerColor, outerColor, blendFactor);

    float alphaInner = alp1;
    float alphaOuter = alp2;
    float alpha = mix(alphaInner, alphaOuter, blendFactor);

	if (facingRaw < 0.0) {
        alpha = 0.01;
    }

    fragColor = vec4(color, alpha);
}