#version 150

in vec3 Position;
in vec4 Color;
in vec3 Normal;

out vec3 vNormalWorld;
out vec3 v_viewDir;
out float vHeight;
out float vWave;
out vec2 vDir;

uniform mat4 modelMatrix;
uniform mat4 ProjMat;
uniform mat4 normalMatrix;
uniform float time;
uniform float auravar;
uniform float layerPass;

const float TAU = 6.28318530718;

const float THIN_COLUMNS = 64.0;
const float THIN_ROWS    = 24.0;
const float THIN_AMP     = 0.30;
const float THIN_SHARP   = 2.4;
const float BIG_COLUMNS  = 32.0;
const float BIG_ROWS     = 12.0;
const float BIG_AMP      = 0.55;
const float BIG_SHARP    = 2.8;
const float ENVELOPE_SCALE = 1.3;
const float ENVELOPE_FREQ_Y = 2.4;
const float ENVELOPE_SPEED = 0.9;
const float ENVELOPE_FLOOR = 0.30;
const float COLOR_LAYER_SEED  = 37.0;
const float COLOR_LAYER_RATE  = 0.63;
const float COLOR_LAYER_PHASE = 5.3;
const float COLOR_LAYER_AMP   = 1.25;
const float BIG_RARITY   = 3.0;

const float SPIKE_BURSTS = 3.0;
const float SPIKE_LIFT = 0.65;
const float SURGE_FREQ  = 2.4;
const float SURGE_SPEED = 1.6;
const float SURGE_DEPTH = 0.55;
const float SIL_POWER   = 1.2;
const float FRONT_SPIKE = 0.15;

vec3 mod289(vec3 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec4 mod289(vec4 x) { return x - floor(x * (1.0 / 289.0)) * 289.0; }
vec4 permute(vec4 x) { return mod289(((x*34.0)+1.0)*x); }

float snoise(vec3 v) {
    const vec2  C = vec2(1.0/6.0, 1.0/3.0);
    const vec4  D = vec4(0.0, 0.5, 1.0, 2.0);

    vec3 i  = floor(v + dot(v, C.yyy));
    vec3 x0 = v - i + dot(i, C.xxx);
    vec3 g = step(x0.yzx, x0.xyz);
    vec3 l = 1.0 - g;
    vec3 i1 = min(g.xyz, l.zxy);
    vec3 i2 = max(g.xyz, l.zxy);

    vec3 x1 = x0 - i1 + C.xxx;
    vec3 x2 = x0 - i2 + C.yyy;
    vec3 x3 = x0 - D.yyy;

    i = mod289(i);
    vec4 p = permute( permute( permute(i.z + vec4(0.0, i1.z, i2.z, 1.0 )) + i.y + vec4(0.0, i1.y, i2.y, 1.0 )) + i.x + vec4(0.0, i1.x, i2.x, 1.0 ));

    float n_ = 0.142857142857;
    vec3 ns = D.wyz - D.xyz * n_;
    vec4 j = p - 49.0 * floor(p * ns.z * ns.z);

    vec4 x_ = floor(j * ns.z);
    vec4 y_ = floor(j - 7.0 * x_);
    vec4 x = x_ *ns.x + ns.yyyy;
    vec4 y = y_ *ns.x + ns.xxxx;
    vec4 h = 1.0 - abs(x) - abs(y);

    vec4 b0 = vec4(x.xy, y.xy);
    vec4 b1 = vec4(x.zw, y.zw);

    vec4 s0 = floor(b0)*2.0 + 1.0;
    vec4 s1 = floor(b1)*2.0 + 1.0;
    vec4 sh = -step(h, vec4(0.0));

    vec4 a0 = b0.xzyw + s0.xzyw*sh.xzyw;
    vec4 a1 = b1.xzyw + s1.xzyw*sh.xzyw;

    vec3 p0 = vec3(a0.xy, h.x);
    vec3 p1 = vec3(a0.zw, h.y);
    vec3 p2 = vec3(a1.xy, h.z);
    vec3 p3 = vec3(a1.zw, h.w);

    vec4 norm = inversesqrt(vec4(dot(p0,p0), dot(p1,p1), dot(p2, p2), dot(p3,p3)));
    p0 *= norm.x;
    p1 *= norm.y;
    p2 *= norm.z;
    p3 *= norm.w;

    vec4 m = max(0.6 - vec4(dot(x0,x0), dot(x1,x1), dot(x2,x2), dot(x3,x3)), 0.0);
    m = m * m;
    return 42.0 * dot(m*m, vec4( dot(p0,x0), dot(p1,x1), dot(p2,x2), dot(p3,x3)));
}

float hash11(float n) {
    return fract(sin(n * 127.1 + 311.7) * 43758.5453);
}

float spikeLength(float id, float t, float rarity) {
    float k = floor(t);
    float life = fract(t);
    float burst = smoothstep(0.0, 0.18, life) * (1.0 - smoothstep(0.40, 1.0, life));
    float len = pow(hash11(id * 7.13 + k * 1.71), rarity);
    return (0.25 + 0.75 * len) * (0.30 + 0.70 * burst);
}

float spikeLayer(float angle, float h, float columns, float rows, float sharp, float rarity, float seed, float t) {
    float x = angle / TAU * columns;
    float col = mod(floor(x), columns);
    float across = 1.0 - abs(fract(x) * 2.0 - 1.0);

    float y = h * rows + floor(hash11(col * 3.1 + seed) * 4.0) * 0.25;
    float row = floor(y);
    float f = fract(y);
    float id = col * 131.0 + row + seed * 17.0;
    float tip = 0.50 + floor(hash11(id + 2.0) * 2.0) * 0.25;
    float along = f < tip ? f / tip : (1.0 - f) / (1.0 - tip);

    float len = spikeLength(id, t * SPIKE_BURSTS * (0.7 + 0.6 * hash11(id + 9.0)) + hash11(id + 5.0) * 9.0, rarity);

    float surge = abs(fract(h * SURGE_FREQ - t * SURGE_SPEED + hash11(col + seed * 5.0)) * 2.0 - 1.0);
    surge = pow(surge, 1.6);
    len *= 1.0 - SURGE_DEPTH * (1.0 - surge);

    return pow(across, sharp) * pow(along, sharp * 0.6) * len;
}

void main() {
    vec3 base = Position;
    float h = clamp(base.y * 0.5 + 0.5, 0.0, 1.0);
    float angle = atan(base.z, base.x);
    vec3 n = normalize(Normal);

    float grow = pow(auravar, 6.0);
    vec3 pos = base;
    pos.xz *= (auravar * auravar * (3.0 - 2.0 * auravar));

    vec4 restView = modelMatrix * vec4(pos, 1.0);
    vec3 nView = normalize((normalMatrix * vec4(n, 0.0)).xyz);
    float sil = pow(1.0 - abs(dot(normalize(-restView.xyz), nView)), SIL_POWER);
    float silWeight = mix(FRONT_SPIKE, 1.0, sil);

    float heightMask = smoothstep(0.02, 0.08, h) * (1.0 - smoothstep(0.95, 1.0, h));
    float layerSeed = layerPass * COLOR_LAYER_SEED;
    float spikeTime = time * mix(1.0, COLOR_LAYER_RATE, layerPass) + layerPass * COLOR_LAYER_PHASE;
    float thin = spikeLayer(angle, h, THIN_COLUMNS, THIN_ROWS, THIN_SHARP, 1.5, 3.0 + layerSeed, spikeTime);
    float big = spikeLayer(angle, h, BIG_COLUMNS, BIG_ROWS, BIG_SHARP, BIG_RARITY, 11.0 + layerSeed, spikeTime);
    vec2 around = normalize(base.xz + n.xz * 0.05 + vec2(1e-6, 0.0));
    float clusterNoise = snoise(vec3(around.x * ENVELOPE_SCALE + layerSeed, h * ENVELOPE_FREQ_Y - spikeTime * ENVELOPE_SPEED, around.y * ENVELOPE_SCALE));
    float envelope = mix(ENVELOPE_FLOOR, 1.0, smoothstep(-0.3, 0.6, clusterNoise));
    float spike = max(thin * THIN_AMP, big * BIG_AMP) * envelope * heightMask * mix(1.0, COLOR_LAYER_AMP, layerPass);
    float amp = mix(0.85, 1.55, smoothstep(0.45, 0.95, h));

    vec3 dir = normalize(n + vec3(0.0, SPIKE_LIFT * smoothstep(0.10, 0.45, h), 0.0));
    pos += dir * spike * amp * silWeight * grow;

    float flicker = snoise(vec3(base.x * 1.6, base.y * 1.2 - time * 7.0, base.z * 1.6));
    vec3 radial = normalize(vec3(base.x, 0.0, base.z) + vec3(1e-4, 0.0, 0.0));
    pos += radial * flicker * 0.03 * heightMask * grow;

    float spikeNorm = clamp(spike / BIG_AMP, 0.0, 1.0);
    vec3 nrm = normalize(mix(n, dir, 0.25 * spikeNorm));

    vec4 viewPos = modelMatrix * vec4(pos, 1.0);
    vNormalWorld = normalize((normalMatrix * vec4(nrm, 0.0)).xyz);
    v_viewDir = -viewPos.xyz;
    vHeight = h;
    vWave = clamp(spikeNorm * silWeight * 1.6, 0.0, 1.0);
    vDir = base.xz;

    gl_Position = ProjMat * viewPos;
}
