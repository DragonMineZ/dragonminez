#version 150

in vec3 vNormalWorld;
in vec3 v_viewDir;
in float vHeight;
in float vWave;

uniform vec3 color1;
uniform vec3 color2;
uniform float alp1;
uniform float alp2;
uniform float power;
uniform float divis;
// Alpha kept on faces pointing away from the camera. Near zero normally, so the far wall of
// the flame does not wash over the near one; raised in first person, where the camera is inside
// the mesh by construction and would otherwise see nothing at all.
uniform float backFace;

out vec4 fragColor;

void main(void) {
    vec3 N = normalize(vNormalWorld);
    vec3 V = normalize(v_viewDir);

    float facingRaw = dot(V, N);
    if (facingRaw < 0.0) N = -N;

    float facing = abs(dot(V, N));
    float edgeFactor = pow(1.0 - facing, power) / divis;
    float blendFactor = clamp(edgeFactor, 0.0, 1.0);

    vec3 color = mix(color1, color2, blendFactor);
    float alpha = mix(alp1, alp2, blendFactor);

    // The moving licks read hotter than the body they grow out of.
    color = mix(color, color2, clamp(vWave * 0.9, 0.0, 1.0));

    // A flame thins out towards the tip instead of ending on a hard edge.
    alpha *= 1.0 - 0.75 * smoothstep(0.40, 1.0, vHeight);

    if (facingRaw < 0.0) alpha *= backFace;

    fragColor = vec4(color, alpha);
}
