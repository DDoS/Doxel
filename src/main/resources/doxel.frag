#version 330

in vec3 modelSpacePosition;
in vec3 modelSpaceNormal;

out vec4 outputColor;

uniform vec4 diffuseColor;
uniform vec4 lightIntensity;
uniform vec4 ambientIntensity;
uniform vec3 modelSpaceLightPos;
uniform float lightAttenuation;

vec4 ApplyLightIntensity(in vec3 modelSpacePosition, out vec3 lightDirection) {
    vec3 lightDifference = modelSpaceLightPos - modelSpacePosition;
    float lightDistanceSqr = dot(lightDifference, lightDifference);
    lightDirection = lightDifference * inversesqrt(lightDistanceSqr);
    return lightIntensity * (1 / (1.0 + lightAttenuation * sqrt(lightDistanceSqr)));
}

void main() {
    vec3 lightDir = normalize(modelSpaceLightPos - modelSpacePosition);
    vec4 attenIntensity = ApplyLightIntensity(modelSpacePosition, lightDir);
    float cosAngIncidence = clamp(dot(normalize(modelSpaceNormal), lightDir), 0, 1);
    outputColor = (diffuseColor * attenIntensity * cosAngIncidence) +
        (diffuseColor * ambientIntensity);
}
