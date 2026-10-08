#version 150

in vec3 Position;
in vec2 UV0;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat3 NormalMat;
uniform sampler2D NoiseTex;

uniform vec3 Size;
uniform float Time;
uniform float Growth;
uniform float Peaks;
uniform float WaveFrequency;
uniform float WaveAmplitude;
uniform float NoiseDetail;
uniform float UpwardBias;
uniform float Layer;

out vec3 vNormal;
out vec3 vView;
out vec2 vFlame;
out float vLift;

const float TAU = 6.28318530718;
const float RADIUS = 0.82;
const float HEIGHT = 2.20;

const float SWAY = 0.20;
const float SWAY_SPEED = 0.11;
const float WIGGLE = 0.10;
const float TONGUE_REACH = 0.50;
const float TONGUE_SHARP = 2.2;
const float TONGUE_CLIMB = 1.1;
const float CROWN_START = 0.40;
const float CROWN_REACH = 0.95;
const float CROWN_NARROW = 0.40;
const float CROWN_TWIST = 0.35;
const float SPIRE = 0.26;
const float LOBES = 5.0;
const float EDGE_JITTER = 0.12;
const float EPS = 0.004;

const float INNER_RADIUS = 0.80;
const float INNER_HEIGHT = 0.93;
const float INNER_TIME = 13.7;
const float INNER_TURN = 0.37;

float tri(float x) {
    return abs(fract(x) * 2.0 - 1.0);
}

float hash(float n) {
    return fract(sin(n * 127.1 + 311.7) * 43758.5453);
}

float signedNoise(vec2 p) {
    return texture(NoiseTex, p).g * 2.0 - 1.0;
}

float shellRadius(float t) {
    float s = sqrt(max(0.0, 1.0 - (2.0 * t - 1.0) * (2.0 * t - 1.0)));
    return pow(s, 0.9) * (0.86 + 0.30 * pow(1.0 - t, 1.3));
}

float lobes(float u, float time) {
    float best = 0.0;
    for (int k = 0; k < 5; k++) {
        float fk = float(k);
        float centre = (fk + 0.5) / LOBES + 0.07 * sin(time * 0.53 + fk * 2.4);
        float d = u - centre;
        d -= floor(d + 0.5);
        float width = 0.085 + 0.03 * sin(time * 0.9 + fk * 1.3);
        float beat = 0.5 + 0.5 * sin(time * (1.9 + 0.37 * fk) + fk * 1.7);
        float height = 0.30 + 0.70 * beat * beat;
        best = max(best, height * exp(-(d * d) / (width * width)));
    }
    return best;
}

vec3 flame(float u, float t, out float lift) {
    float inner = 1.0 - Layer;
    float time = Time + inner * INNER_TIME;
    float turn = u + inner * INNER_TURN;
    float grow = Growth * Growth * Growth;
    float reach = WaveAmplitude / 0.75;
    float r = shellRadius(t);
    float swell = Growth * Growth * (3.0 - 2.0 * Growth);
    float crown = smoothstep(CROWN_START, 1.0, t);

    float a = u * TAU + CROWN_TWIST * crown * sin(Time * 0.6);
    vec2 radial = vec2(cos(a), sin(a));
    float radius = RADIUS * mix(1.0, INNER_RADIUS, inner);
    float height = HEIGHT * mix(1.0, INNER_HEIGHT, inner);

    vec3 p = vec3(radial.x * r * radius * Size.x, t * height * Size.y, radial.y * r * radius * Size.z);
    p.xz *= swell;

    float tongue = mix(0.45, lobes(turn, time), smoothstep(0.0, 0.35, r));
    float spire = SPIRE * (0.65 + 0.35 * sin(time * 2.3) * sin(time * 1.37 + 1.0));
    lift = crown * crown * (CROWN_REACH * tongue + spire) * reach * grow;
    p.xz *= 1.0 - CROWN_NARROW * crown * crown * (0.5 + 0.5 * tongue);
    p.y += lift;

    float wiggle = WIGGLE * sin(t * 8.0 - time * 4.0 + a * 2.0);
    float ridgeX = turn * Peaks + wiggle;
    float ridge = mod(floor(ridgeX + 0.5), Peaks);
    float ridges = tri(ridgeX);
    float bands = tri(t * WaveFrequency * 1.5 - time * TONGUE_CLIMB * (0.8 + 0.4 * hash(ridge)) + hash(ridge + 7.0));
    float noise = texture(NoiseTex, vec2(turn, t * 0.8 - time * 0.5)).r;
    float wave = pow(ridges, TONGUE_SHARP) * bands * bands + (noise - 0.45) * NoiseDetail * 3.0;
    float envelope = smoothstep(0.05, 0.40, t) * (1.0 - 0.5 * crown);
    vec3 direction = normalize(mix(vec3(radial.x, 0.0, radial.y), vec3(0.0, 1.0, 0.0), UpwardBias));
    p += direction * wave * TONGUE_REACH * reach * envelope * grow;

    float jitter = texture(NoiseTex, vec2(turn * 3.0, t * 1.6 - time * 0.9)).g - 0.5;
    p.xz += radial * jitter * EDGE_JITTER * (0.3 + t) * smoothstep(0.03, 0.2, t) * grow;

    float root = smoothstep(0.10, 1.0, t);
    vec2 sway = vec2(signedNoise(vec2(0.17, t * 0.35 - Time * SWAY_SPEED)),
            signedNoise(vec2(0.61, t * 0.30 - Time * SWAY_SPEED * 0.9 + 0.5)));
    p.xz += sway * SWAY * root * root * grow;

    return p;
}

void main() {
    float u = UV0.x;
    float t = UV0.y;

    float lift;
    float liftU;
    float liftT;
    vec3 p = flame(u, t, lift);
    vec3 pu = flame(u + EPS, t, liftU);
    vec3 pt = flame(u, t + EPS, liftT);

    vec3 normal = cross(pt - p, pu - p);
    if (dot(normal, normal) < 1.0e-12) normal = vec3(0.0, t > 0.5 ? 1.0 : -1.0, 0.0);

    vec4 viewPos = ModelViewMat * vec4(p, 1.0);
    vNormal = NormalMat * normalize(normal);
    vView = -viewPos.xyz;
    vFlame = vec2(u, t);
    vLift = clamp(lift / max(CROWN_REACH + SPIRE, 1.0e-3), 0.0, 1.0);

    gl_Position = ProjMat * viewPos;
}
