#version 150

// Smooth 3D aura. The mesh is a plain egg-shaped shell (AuraMeshFactory#getDropletMesh); every flame
// tongue is pushed out of it here, so the silhouette reshapes every frame instead of being baked in.
//
// Tongues come from crossing two triangle waves: Peaks vertical ridges around the body and horizontal
// bands climbing the shell. Their product is a lattice of diamonds that travels upwards, and a scrolling
// noise field is added on top so the lattice never reads as regular.

in vec3 Position;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat3 NormalMat;
uniform sampler2D NoiseTex;

uniform vec3 Size;
uniform float Time;
uniform float Phase;
uniform float Growth;
uniform float Peaks;
uniform float WaveFrequency;
uniform float WaveAmplitude;
uniform float NoiseDetail;
uniform float UpwardBias;

out vec3 vNormal;
out vec3 vView;

const float TAU = 6.28318530718;

float triangleWave(float x) {
    return abs(fract(x) * 2.0 - 1.0);
}

void main() {
    vec3 p = Position;

    // Measured in whole turns: Peaks is an integer and the noise repeats once per turn, so both close up
    // where atan() wraps at the back instead of leaving a seam.
    float around = atan(p.z, p.x + 1.0e-6) / TAU;
    float ridges = triangleWave(around * Peaks);
    float bands = triangleWave(p.y * WaveFrequency - Phase);

    float noise = texture(NoiseTex, vec2(around, p.y * 1.1 - Time)).r * 1.5 - 0.5;
    float wave = ridges * bands + noise * NoiseDetail * 1.08;

    // Rooted at the base, strongest through the body, fading out over the top.
    float envelope = smoothstep(-1.5, -0.2, p.y) * (1.0 - smoothstep(0.0, 2.3, p.y));

    vec3 shell = p * Size;
    vec3 outward = normalize(shell + vec3(0.0, 1.0e-5, 0.0));
    vec3 direction = normalize(mix(outward, vec3(0.0, 1.0, 0.0), UpwardBias));

    // Ignition: the shell swells out of the body first and the tongues only burst once it is nearly full.
    float swell = Growth * Growth * (3.0 - 2.0 * Growth);
    shell.xz *= swell;
    shell += direction * wave * WaveAmplitude * envelope * pow(Growth, 6.0);

    vec4 viewPos = ModelViewMat * vec4(shell, 1.0);
    // The shell is a sphere at heart, so its displaced position doubles as the normal: the rim follows
    // the tongues without the mesh carrying normals at all.
    vNormal = NormalMat * shell;
    vView = -viewPos.xyz;

    gl_Position = ProjMat * viewPos;
}
