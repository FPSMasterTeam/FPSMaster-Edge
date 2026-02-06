#version 150

uniform sampler2D DiffuseSampler;
uniform sampler2D PrevSampler;

in vec2 texCoord;
in vec2 oneTexel;

uniform vec2 InSize;

uniform vec3 Phosphor = vec3(0.7, 0.0, 0.0);

out vec4 fragColor;

void main() {
    vec4 CurrTexel = texture(DiffuseSampler, texCoord);
    vec4 PrevTexel = texture(PrevSampler, texCoord);
    float factor = Phosphor.r;

    if (Phosphor.g == 0) {
        fragColor = vec4(max(PrevTexel.rgb * vec3(factor), CurrTexel.rgb), 1.0);
    } else if (Phosphor.g == 1) {
        fragColor = vec4(mix(PrevTexel.rgb, CurrTexel.rgb, factor), 1.0);
    } else {
        PrevTexel.a = max(0.0, min(PrevTexel.a - 0.325, PrevTexel.a * factor * 0.95));

        vec3 blendedRGB = PrevTexel.rgb * PrevTexel.a + CurrTexel.rgb * (1.0 - PrevTexel.a);
        fragColor = vec4(blendedRGB, 1.0);
    }
}
