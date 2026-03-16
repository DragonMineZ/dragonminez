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
    float edgeFactor = 1.0 - facingAbs;

    edgeFactor = pow(edgeFactor, power * 1.5) / divis;
    float blendFactor = clamp(edgeFactor, 0.0, 1.0);

    vec3 color = mix(color1, color2, blendFactor);
    float alpha = mix(alp1, alp2, blendFactor);

    if (facingRaw < 0.0) {
        alpha = 0.01;
    }

    fragColor = vec4(color, alpha);
}