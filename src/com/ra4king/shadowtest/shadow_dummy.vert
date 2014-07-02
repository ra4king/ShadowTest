#version 130

in vec4 position;
in vec3 norm;

uniform mat4 projectionMatrix, viewMatrix, modelMatrix;

// The dummy program only cares about the position of all vertices, as their depth is all that's needed
void main() {
	gl_Position = projectionMatrix * viewMatrix * modelMatrix * position;
}
