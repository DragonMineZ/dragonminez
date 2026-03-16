#version 150

in vec3 Position;
in vec4 Color;
in vec3 Normal;

out vec3 vNormalWorld;
out vec3 v_viewDir;
out vec2 vLocalXZ;

uniform mat4 modelMatrix;
uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;
uniform mat4 normalMatrix;
uniform vec3 view;
uniform float time;
uniform float speedModifier;

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

void main() {
    vLocalXZ = Position.xz;

    float height = 3.0;
    float t = clamp(Position.y / height, 0.0, 1.0);

    float activeSpeed = 12.0 * speedModifier;
    float snapTime = floor(time * activeSpeed);

    vec3 noiseCoord = vec3(0.0, Position.y * 1.5, snapTime);
    float nx = snoise(noiseCoord) * 1.5;
    float nz = snoise(noiseCoord + vec3(10.0, 0.0, 0.0)) * 1.5;

    vec4 pos = vec4(Position, 1.0);

    float thickness = 0.3 * (1.0 - (t * 0.4));
    pos.x = Position.x * thickness;
    pos.z = Position.z * thickness;

    pos.x += nx * t;
    pos.z += nz * t;

    pos.x += max(0.0, t - 0.25) * 0.5;
    pos.z += max(0.0, t - 0.50) * -0.4;

    vec4 viewPos = modelMatrix * vec4(pos.xyz, 1.0);

    vNormalWorld = normalize((normalMatrix * vec4(Normal, 0.0)).xyz);
    v_viewDir = -viewPos.xyz;

    gl_Position = projectionMatrix * viewPos;
}