#version 330

layout(location = 0) in vec3 position;
layout(location = 1) in vec3 normal;

out vec3 modelSpacePosition;
out vec3 modelSpaceNormal;

uniform mat4 modelToCameraMatrix;
uniform mat3 normalModelToCameraMatrix;
uniform mat4 cameraToClipMatrix;

void main() {
    gl_Position = cameraToClipMatrix * (modelToCameraMatrix * vec4(position, 1));
    modelSpacePosition = position;
    modelSpaceNormal = normal;
}
