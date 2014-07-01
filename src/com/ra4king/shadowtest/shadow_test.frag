#version 130

in vec3 cameraSpacePosition;
in vec3 normal;
in vec4 shadowPosition;

uniform vec4 lightPosition;
uniform vec4 diffuse, ambient;

uniform sampler2D shadowMap;

out vec4 fragColor;

void main() {
	vec3 shadowCoords = vec3(shadowPosition / shadowPosition.w);
	shadowCoords = (shadowCoords + 1) / 2;
	shadowCoords.z -= 0.00015;
	
	float a = 0.0;
	if(shadowCoords.x < 0 || shadowCoords.x > 1 || shadowCoords.y < 0 || shadowCoords.y > 1 || shadowCoords.z <= texture2D(shadowMap, shadowCoords.xy).r) {
        vec3 lightDir = lightPosition.w == 0.0 ? -vec3(lightPosition) : lightPosition.xyz - cameraSpacePosition;
        a = max(0, dot(normalize(normal), normalize(lightDir)));
    }
	
	fragColor = diffuse * a + ambient;
	
	vec4 gamma = vec4(1 / 2.2);
	gamma.w = 1;
	fragColor = pow(fragColor, gamma);
}
