#version 130

in vec4 position;
in vec3 norm;

uniform mat4 projectionMatrix, viewMatrix, modelMatrix;

void main() {
	gl_Position = projectionMatrix * viewMatrix * modelMatrix * position;
}
