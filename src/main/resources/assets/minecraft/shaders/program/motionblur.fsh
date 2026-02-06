#version 120

uniform sampler2D DiffuseSampler;
uniform sampler2D PrevSampler;

varying vec2 texCoord;
varying vec2 oneTexel;

uniform vec2 InSize;

uniform vec3 Phosphor = vec3(0.7, 0.0, 0.0);

void main() {
    vec4 CurrTexel = texture2D(DiffuseSampler, texCoord);
    vec4 PrevTexel = texture2D(PrevSampler, texCoord);
    float factor = Phosphor.r;

    if (Phosphor.g == 0) {
        gl_FragColor = vec4(max(PrevTexel.rgb * vec3(factor), CurrTexel.rgb), 1.0);
    } else if (Phosphor.g == 1) {
        gl_FragColor = vec4(mix(PrevTexel.rgb, CurrTexel.rgb, factor), 1.0);
    } else {
        PrevTexel.a = max(0.0, min(PrevTexel.a - 0.325, PrevTexel.a * factor * 0.95));

        vec3 blendedRGB = PrevTexel.rgb * PrevTexel.a + CurrTexel.rgb * (1.0 - PrevTexel.a);
        gl_FragColor = vec4(blendedRGB, 1.0);
    }
}
