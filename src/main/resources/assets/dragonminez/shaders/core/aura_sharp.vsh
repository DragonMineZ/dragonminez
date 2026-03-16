#version 150

in vec3 Position;
in vec4 Color;
in vec3 Normal;

out vec3 vNormalWorld;
out vec3 v_viewDir;

uniform mat4 modelMatrix;
uniform mat4 viewMatrix;
uniform mat4 ProjMat;
uniform mat4 normalMatrix;
uniform vec3 view;
uniform float time;
uniform float auravar;

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

float hash(vec3 p) {
    p  = fract(p * .3183099 + .1);
    p = p * 1.0;
    p = p + dot(p, p.yzx + 19.19);
    return fract((p.x + p.y) * p.z);
}

float triangleWave(float x) {
    return abs(fract(x/1) * 2.0 - 1.0);
}

void main() {
    vec4 pos = vec4(Position, 1.0);

    float freqY = 2.0;
    float speed = 5.5;
    float amp = 0.8;
    float fineDetail = 5.0;
	
	float numPeaks = 2;
	float angle = atan(Position.z, Position.x);
	float angularWave = triangleWave(angle * numPeaks);
	
	
	float layer = triangleWave((Position.y * freqY) - (time * speed));
    float totalWave = layer * 1 * angularWave;
    float falloff = smoothstep(-1.8, 0.0, Position.y) * (1.0 - smoothstep(0.0, 2, Position.y));

    vec3 dir = normalize(pos.xyz);
    vec3 upwardDir = vec3(0.0, 1.0, 0.0);
	vec3 finalDir = normalize(mix(dir, upwardDir, 0.55));

	pos.xz *= (auravar * auravar  * (3 - 2 * auravar));
	pos.xyz += finalDir * (totalWave * amp * falloff) * pow(auravar, 6);
	
    vec4 worldPos = modelMatrix * vec4(Position, 1.0);
	vec4 viewPos = modelMatrix * vec4(pos.xyz, 1.0);
	
	vNormalWorld = normalize((normalMatrix * vec4(pos.xyz, 0.0)).xyz);
	v_viewDir = -viewPos.xyz;
    vec4 clipPos = ProjMat * worldPos;
	
    gl_Position = ProjMat * viewPos;
}
