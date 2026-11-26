#version 150

// Flight trail. The geometry is a camera-facing ribbon built each frame from the player's recent
// positions, so this shader only has to shade it: no displacement, the shape is already the path.

in vec3 Position;
in vec4 Color;
in vec2 UV0;

out vec4 vColor;
out vec2 vUv;

uniform mat4 modelMatrix;
uniform mat4 ProjMat;

void main() {
    vColor = Color;
    vUv = UV0;
    gl_Position = ProjMat * modelMatrix * vec4(Position, 1.0);
}
