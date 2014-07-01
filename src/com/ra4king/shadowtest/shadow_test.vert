#version 120

attribute vec4 position;
attribute vec3 norm;

varying vec3 cameraSpacePosition;
varying vec3 normal;
varying vec4 shadowPosition;

uniform mat3 normalMatrix;
uniform mat4 projectionMatrix, viewMatrix, modelMatrix;

uniform mat4 shadowProjectionMatrix, shadowViewMatrix;

void main() {
	cameraSpacePosition = vec3(viewMatrix * modelMatrix * position);
	gl_Position = projectionMatrix * vec4(cameraSpacePosition, 1);
	normal = mat3(viewMatrix) * normalMatrix * norm;
	
	shadowPosition = shadowProjectionMatrix * shadowViewMatrix * modelMatrix * position;
}