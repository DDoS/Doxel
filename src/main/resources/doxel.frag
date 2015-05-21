// $shader_type: fragment

#version 330

in vec3 positionView;
in vec3 normalView;
in vec3 lightPositionView;

uniform vec3 modelColor;
uniform float diffuseIntensity;
uniform float specularIntensity;
uniform float ambientIntensity;

out vec4 outputColor;

void main() {
    float ambientTerm = ambientIntensity;
    vec3 nNormalView = normalize(normalView);
    vec3 nLightDirectionView = normalize(positionView - lightPositionView);
    if (dot(nNormalView, nLightDirectionView) < 0) {
        nNormalView = -nNormalView;
    }
    float diffuseTerm = diffuseIntensity * max(0, dot(nNormalView, nLightDirectionView));
    float specularTerm;
    if (diffuseTerm > 0) {
        specularTerm = specularIntensity * pow(max(0, dot(reflect(-nLightDirectionView, nNormalView), normalize(positionView))), 50);
    } else {
        specularTerm = 0;
    }
    outputColor = vec4(modelColor * (ambientTerm + diffuseTerm + specularTerm), 1);
}
