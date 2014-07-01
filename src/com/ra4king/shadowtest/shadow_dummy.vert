#version 120

attribute vec4 position;
attribute vec3 norm;

uniform mat4 projectionMatrix, viewMatrix, modelMatrix;

void main() {
	gl_Position = projectionMatrix * viewMatrix * modelMatrix * position;
}
