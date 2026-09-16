#version 150

// Sparking 3D aura. The mesh is an egg-shaped shell of revolution (AuraMeshFactory#getSparkingFlameMesh)
// carrying baked normals; every spike is pushed out of it here.
//
// The shell is covered by a grid of cells, and each cell fires its own spike on its own beat, holds it and
// kills it. The grid climbs, more slowly than the smooth aura, so the spikes break up the round crown the
// way the smooth tongues do; but a spike lives about a third of a second against a climb that takes over a
// second to cross the shell, so it still pops in and out along the way instead of reading as a flame drawn
// upwards. The grid, the beats and their jitter are the identity of the style; only their density, rate,
// rarity and speed are data.

in vec3 Position;
in vec4 Color;
in vec3 Normal;

out vec3 vNormal;
out vec3 vView;
out float vHeight;
out float vWave;
out float vUp;
out vec2 vDir;

uniform mat4 ModelViewMat;
uniform mat4 ProjMat;
uniform mat3 NormalMat;

uniform vec3 Size;
uniform float Time;
uniform float Growth;
uniform float SpikeDensity;
uniform float SpikeBurstRate;
uniform float SpikeRarity;
uniform float WaveSpeed;
uniform float WaveAmplitude;
uniform float NoiseDetail;
uniform float LayerPass;

const float TAU = 6.28318530718;

// Thin spikes fill in the detail, big ones punch through. Both counts have to stay whole numbers so the
// grid closes where atan() wraps, and well under the mesh segment count or the spikes alias into noise
// instead of resolving as spikes.
const float THIN_ROWS = 18.0;
const float THIN_SHARP = 2.4;
const float BIG_ROWS = 12.0;
const float BIG_SHARP = 2.8;
// Sized so the spikes you actually read (the 95th percentile) are twice as long as before. A plain doubling
// of these would not get there: the wider spread below drags the average down, so the reach has to carry it.
const float THIN_REACH = 1.84;
const float BIG_REACH = 3.36;
// Height fractions per second, per unit of WaveSpeed. At the default speed the grid crosses the shell in
// 1.25s against the smooth aura's 0.8s, which is the point: the same camouflage, read as slower.
const float CLIMB_RATE = 0.16;

const float CLUSTER_SCALE = 1.3;
const float CLUSTER_FREQ_Y = 2.4;
const float CLUSTER_SPEED = 0.9;
const float CLUSTER_FLOOR = 0.20;

// The second pass is a sparser, slower, longer-reaching copy that fringes the first one.
const float FRINGE_SEED = 37.0;
const float FRINGE_RATE = 0.63;
const float FRINGE_PHASE = 5.3;
const float FRINGE_REACH = 1.25;

const float SPIKE_LIFT = 0.65;
const float SURGE_FREQ = 2.4;
const float SURGE_DEPTH = 0.55;
const float SIL_POWER = 1.2;
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

// One beat of a single cell: the spike shoots out, holds, then dies, and each whole number of t reshuffles
// its length. Raising rarity biases the draw towards short spikes, so long ones stay rare and read as accents.
//
// The two floors set how alike the spikes are. They are low on purpose: a high floor gives every cell a
// decent spike on every beat, which lines the shell with an even fringe. Dropping them widens the spread to
// a coefficient of variation of about 0.97, so most spikes are stubs, a few dominate, and cells go properly
// dark between beats. That spread is the irregularity; it also pulls the average down, which is why the
// reaches above carry it.
float spikeLength(float id, float t, float rarity) {
    float k = floor(t);
    float life = fract(t);
    float burst = smoothstep(0.0, 0.18, life) * (1.0 - smoothstep(0.40, 1.0, life));
    float len = pow(hash11(id * 7.13 + k * 1.71), rarity);
    return (0.10 + 0.90 * len) * (0.18 + 0.82 * burst);
}

float spikeLayer(float angle, float h, float columns, float rows, float sharp, float rarity, float seed, float t) {
    float x = angle / TAU * columns;
    float col = mod(floor(x), columns);
    float across = 1.0 - abs(fract(x) * 2.0 - 1.0);

    // Subtracting the climb scrolls the grid upwards. A cell keeps its row, so a spike carries its own
    // length and beat all the way up. The per-column offset is a free fraction of a row rather than one of
    // four quarter steps: quarters left a faint banding that read as regularity from a distance.
    float climb = t * CLIMB_RATE * WaveSpeed;
    float y = (h - climb) * rows + hash11(col * 3.1 + seed);
    float row = floor(y);
    float f = fract(y);
    float id = col * 131.0 + row + seed * 17.0;
    // Where the spike peaks inside its cell, anywhere from leaning down to leaning up.
    float tip = 0.30 + 0.45 * hash11(id + 2.0);
    float along = f < tip ? f / tip : (1.0 - f) / (1.0 - tip);

    // Each cell runs its beats at 0.55x to 1.55x the nominal rate, so neighbours drift out of step fast.
    float len = spikeLength(id, t * SpikeBurstRate * (0.55 + 1.0 * hash11(id + 9.0)) + hash11(id + 5.0) * 9.0, rarity);

    // The one travelling element: a slow wave that trims whatever it passes over.
    float surge = abs(fract(h * SURGE_FREQ - t * WaveSpeed * 0.32 + hash11(col + seed * 5.0)) * 2.0 - 1.0);
    surge = pow(surge, 1.6);
    len *= 1.0 - SURGE_DEPTH * (1.0 - surge);

    return pow(across, sharp) * pow(along, sharp * 0.6) * len;
}

void main() {
    vec3 base = Position;
    float h = clamp(base.y * 0.5 + 0.5, 0.0, 1.0);
    float angle = atan(base.z, base.x);
    vec3 n = normalize(Normal);

    vec3 pos = base * Size;

    // Ignition: the shell swells out of the body, then a front climbs from the feet and lights the spike
    // field on its way up. Running Growth back down collapses both, so the aura dies the way it was born.
    float swell = Growth * Growth * (3.0 - 2.0 * Growth);
    pos.xz *= swell;
    float front = Growth * 1.45;
    float lit = 1.0 - smoothstep(front - 0.35, front, h);
    // A single overshoot as the front passes: every spike fires long once, then settles.
    float flash = 1.0 + 0.85 * smoothstep(0.25, 0.55, Growth) * (1.0 - smoothstep(0.55, 1.0, Growth));
    float grow = Growth * Growth * lit * flash;

    vec4 restView = ModelViewMat * vec4(pos, 1.0);
    vec3 nView = normalize(NormalMat * n);
    float sil = pow(1.0 - abs(dot(normalize(-restView.xyz), nView)), SIL_POWER);
    float silWeight = mix(FRONT_SPIKE, 1.0, sil);

    // Rooted at the base, and kept alive over the crown: that is what breaks up the round top of the egg,
    // the way the smooth tongues do. It only gives out at the very tip.
    float envelope = smoothstep(0.02, 0.16, h) * (1.0 - smoothstep(0.86, 1.06, h));

    float fringeSeed = LayerPass * FRINGE_SEED;
    float spikeTime = Time * mix(1.0, FRINGE_RATE, LayerPass) + LayerPass * FRINGE_PHASE;
    float thinColumns = max(4.0, SpikeDensity);
    float bigColumns = max(2.0, floor(thinColumns * 0.5));
    float thin = spikeLayer(angle, h, thinColumns, THIN_ROWS, THIN_SHARP, 1.5, 3.0 + fringeSeed, spikeTime);
    float big = spikeLayer(angle, h, bigColumns, BIG_ROWS, BIG_SHARP, SpikeRarity, 11.0 + fringeSeed, spikeTime);

    vec2 around = normalize(base.xz + n.xz * 0.05 + vec2(1e-6, 0.0));
    float clusterNoise = snoise(vec3(around.x * CLUSTER_SCALE + fringeSeed, h * CLUSTER_FREQ_Y - spikeTime * CLUSTER_SPEED, around.y * CLUSTER_SCALE));
    float cluster = mix(CLUSTER_FLOOR, 1.0, smoothstep(-0.3, 0.6, clusterNoise));

    float reach = WaveAmplitude * mix(1.0, FRINGE_REACH, LayerPass);
    float spike = max(thin * THIN_REACH, big * BIG_REACH) * reach * cluster * envelope;
    // Longest through the upper body and only lightly trimmed over the crown: enough that the egg still
    // tapers, not so much that the top goes bare and reads as a smooth dome again.
    float amp = mix(0.80, 1.05, smoothstep(0.10, 0.55, h)) * mix(1.0, 0.85, smoothstep(0.75, 1.0, h));

    // While the aura dies the spikes are pulled straight up, so they evaporate instead of shrinking in place.
    float lift = mix(1.0, SPIKE_LIFT, smoothstep(0.0, 0.55, Growth));
    vec3 dir = normalize(n + vec3(0.0, lift * smoothstep(0.10, 0.45, h), 0.0));
    pos += dir * spike * amp * silWeight * grow;

    float flicker = snoise(vec3(base.x * 1.6, base.y * 1.2 - Time * 7.0, base.z * 1.6));
    vec3 radial = normalize(vec3(base.x, 0.0, base.z) + vec3(1e-4, 0.0, 0.0));
    pos += radial * flicker * NoiseDetail * 0.3 * envelope * grow;

    float spikeNorm = clamp(spike / max(BIG_REACH * WaveAmplitude, 1.0e-3), 0.0, 1.0);
    vec3 nrm = normalize(mix(n, dir, 0.25 * spikeNorm));

    vec4 viewPos = ModelViewMat * vec4(pos, 1.0);
    vNormal = NormalMat * nrm;
    vView = -viewPos.xyz;
    vHeight = h;
    vWave = clamp(spikeNorm * silWeight * 1.6, 0.0, 1.0);
    // Object-space verticality, so the fragment stage can tell the shell caps from its sides.
    vUp = nrm.y;
    vDir = base.xz;

    gl_Position = ProjMat * viewPos;
}
