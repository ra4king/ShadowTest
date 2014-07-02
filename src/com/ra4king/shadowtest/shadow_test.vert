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
	vec4 cameraPos = viewMatrix * modelMatrix * position;
	cameraSpacePosition = vec3(cameraPos);
	gl_Position = projectionMatrix * cameraPos;
	normal = mat3(viewMatrix) * normalMatrix * norm;
	
	// The same position generated in the dummy program is calculated to compare depth values
	shadowPosition = shadowProjectionMatrix * shadowViewMatrix * modelMatrix * position;
}
