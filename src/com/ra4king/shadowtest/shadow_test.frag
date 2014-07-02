#version 130

in vec3 cameraSpacePosition;
in vec3 normal;
in vec4 shadowPosition;

uniform vec4 lightPosition;
uniform vec4 diffuse, ambient;

uniform sampler2D shadowMap;

out vec4 fragColor;

void main() {
	vec3 shadowCoords = vec3(shadowPosition / shadowPosition.w); // Do the perspective divide manually
	shadowCoords = (shadowCoords + 1) / 2;
	shadowCoords.z -= 0.00015; // Skew the coords's depth a little, as they are not exact and will show smears on flat surfaces otherwise
	
	float a = 0.0;
	
	// If this fragment's depth from the light source's perspective is less than or equal to the one in the texture, then it is lit
	if(shadowCoords.x < 0 || shadowCoords.x > 1 || shadowCoords.y < 0 || shadowCoords.y > 1 || shadowCoords.z <= texture2D(shadowMap, shadowCoords.xy).r) {
        vec3 lightDir = lightPosition.w == 0.0 ? -vec3(lightPosition) : lightPosition.xyz - cameraSpacePosition;
        a = max(0, dot(normalize(normal), normalize(lightDir)));
    }
	
	fragColor = diffuse * a + ambient;
	
	// Gamma correction
	vec4 gamma = vec4(1 / 2.2);
	gamma.w = 1;
	fragColor = pow(fragColor, gamma);
}
