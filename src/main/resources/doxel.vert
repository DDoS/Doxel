#version 330

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 normal;

smooth out vec4 diffuse;
smooth out vec4 specular;
smooth out vec4 ambient;

uniform mat4 viewMatrix;
uniform mat4 projectionMatrix;
uniform vec4 modelColor;
uniform vec3 lightPosition;
uniform float lightAttenuation;

void main() {
    gl_Position = projectionMatrix * viewMatrix * vec4(position, 1);

    vec3 lightDifference = lightPosition - position;
    float lightDistance = length(lightDifference);
    vec3 lightDirection = lightDifference / lightDistance;
    float distanceIntensity = 1 / (1 + lightAttenuation * lightDistance);

    diffuse = modelColor * distanceIntensity *
        clamp(dot(normal, lightDirection), 0, 1);

    specular = modelColor * distanceIntensity *
        pow(clamp(dot(
            reflect(-lightDirection, normal),
            normalize(vec3(inverse(viewMatrix) * vec4(0, 0, 0, 1)) - position)
        ), 0, 1), 2);

    ambient = modelColor;
}
