#version 330

in vec3 modelSpacePosition;
in vec3 modelSpaceNormal;

out vec4 outputColor;

uniform mat4 modelToCameraMatrix;
uniform vec4 diffuseColor;
uniform vec4 lightIntensity;
uniform vec4 ambientIntensity;
uniform vec3 modelSpaceLightPosition;
uniform float lightAttenuation;

vec4 getLightIntensity(out vec3 lightDirection) {
    vec3 lightDifference = modelSpaceLightPosition - modelSpacePosition;
    float lightDistanceSquared = dot(lightDifference, lightDifference);
    lightDirection = lightDifference * inversesqrt(lightDistanceSquared);
    return lightIntensity * (1 / (1 + lightAttenuation * sqrt(lightDistanceSquared)));
}

void main() {
    vec3 modelSpaceCameraPosition = vec3(inverse(modelToCameraMatrix) * vec4(0, 0, 0, 1));
    vec3 lightDirection;
    vec4 lightIntensity = getLightIntensity(lightDirection);

    vec4 diffuse = diffuseColor * lightIntensity
        * clamp(dot(modelSpaceNormal, lightDirection), 0, 1);

    vec4 specular = diffuseColor * lightIntensity
        * pow(clamp(dot(reflect(-lightDirection, modelSpaceNormal), normalize(modelSpaceCameraPosition - modelSpacePosition)), 0, 1), 2);

    vec4 ambient = diffuseColor * ambientIntensity;

    outputColor = ambient + diffuse + specular;
}
