#version 130

in vec4 position;
in vec3 norm;

out vec3 cameraSpacePosition;
out vec3 normal;
out vec4 shadowPosition;

uniform mat3 normalMatrix;
uniform mat4 projectionMatrix, viewMatrix, modelMatrix;

uniform mat4 shadowProjectionMatrix, shadowViewMatrix;

void main() {
	cameraSpacePosition = vec3(viewMatrix * modelMatrix * position);
	gl_Position = projectionMatrix * vec4(cameraSpacePosition, 1);
	normal = mat3(viewMatrix) * normalMatrix * norm;
	
	shadowPosition = shadowProjectionMatrix * shadowViewMatrix * modelMatrix * position;
}
