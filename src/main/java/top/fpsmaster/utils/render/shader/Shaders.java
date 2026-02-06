package top.fpsmaster.utils.render.shader;

public class Shaders {
    public static String font_rainbow = "#version 130\n" +
            "\n" +
            "uniform float offset;\n" +
            "uniform vec2 strength;\n" +
            "uniform sampler2D font_texture;\n" +
            "\n" +
            "void main ()\n" +
            "{\n" +
            "    vec4 tmpvar_1;\n" +
            "\n" +
            "    tmpvar_1 = texture (font_texture, gl_TexCoord[0].xy);\n" +
            "\n" +
            "    if (tmpvar_1.w == 0.0)\n" +
            "        discard;\n" +
            "\n" +
            "\n" +
            "    vec2 tmpvar_2 = (gl_FragCoord.xy * strength);\n" +
            "    vec4 tmpvar_3 = vec4(clamp ((abs(\n" +
            "    ((fract((vec3(\n" +
            "    (float(mod (((tmpvar_2.x + tmpvar_2.y) + offset), 1.0)))\n" +
            "    ) + vec3(1.0, 0.6666667, 0.3333333))) * 6.0) - vec3(3.0, 3.0, 3.0))\n" +
            "    ) - vec3(1.0, 1.0, 1.0)), 0.0, 1.0), tmpvar_1.w);\n" +
            "    gl_FragColor = tmpvar_3;\n" +
            "}\n" +
            "\n";
    public static String rainbow = "#version 130\n" +
            "uniform float offset;\n" +
            "uniform vec2 strength;\n" +
            "\n" +
            "void main() {\n" +
            "    vec2 tmpvar_1 = (gl_FragCoord.xy * strength);\n" +
            "    vec4 tmpvar_2 = vec4(clamp ((abs(\n" +
            "    ((fract((vec3(\n" +
            "    (float(mod (((tmpvar_1.x + tmpvar_1.y) + offset), 1.0)))\n" +
            "    ) + vec3(1.0, 0.6666667, 0.3333333))) * 6.0) - vec3(3.0, 3.0, 3.0))\n" +
            "    ) - vec3(1.0, 1.0, 1.0)), 0.0, 1.0), 1.0);\n" +
            "    gl_FragColor = tmpvar_2;\n" +
            "}\n" +
            "\n";

    public static String gradient = "#version 120\n" +
            "\n" +
            "uniform vec2 location, rectSize;\n" +
            "uniform sampler2D tex;\n" +
            "uniform vec3 color1, color2, color3, color4;\n" +
            "uniform float alpha;\n" +
            "\n" +
            "#define NOISE .5/255.0\n" +
            "\n" +
            "vec3 createGradient(vec2 coords, vec3 color1, vec3 color2, vec3 color3, vec3 color4){\n" +
            "    vec3 color = mix(mix(color1, color2, coords.y), mix(color3, color4, coords.y), coords.x);\n" +
            "    //Dithering the color\n" +
            "    // from https://shader-tutorial.dev/advanced/color-banding-dithering/\n" +
            "    color += mix(NOISE, -NOISE, fract(sin(dot(coords.xy, vec2(12.9898, 78.233))) * 43758.5453));\n" +
            "    return color;\n" +
            "}\n" +
            "\n" +
            "void main() {\n" +
            "    vec2 coords = (gl_FragCoord.xy - location) / rectSize;\n" +
            "    gl_FragColor = vec4(createGradient(coords, color1, color2, color3, color4), alpha);\n" +
            "}";
    public static String gradientMask = "#version 120\n" +
            "\n" +
            "uniform vec2 location, rectSize;\n" +
            "uniform sampler2D tex;\n" +
            "uniform vec3 color1, color2, color3, color4;\n" +
            "uniform float alpha;\n" +
            "\n" +
            "#define NOISE .5/255.0\n" +
            "\n" +
            "vec3 createGradient(vec2 coords, vec3 color1, vec3 color2, vec3 color3, vec3 color4){\n" +
            "    vec3 color = mix(mix(color1.rgb, color2.rgb, coords.y), mix(color3.rgb, color4.rgb, coords.y), coords.x);\n" +
            "    //Dithering the color from https://shader-tutorial.dev/advanced/color-banding-dithering/\n" +
            "    color += mix(NOISE, -NOISE, fract(sin(dot(coords.xy, vec2(12.9898,78.233))) * 43758.5453));\n" +
            "    return color;\n" +
            "}\n" +
            "\n" +
            "void main() {\n" +
            "    vec2 coords = (gl_FragCoord.xy - location) / rectSize;\n" +
            "    float texColorAlpha = texture2D(tex, gl_TexCoord[0].st).a;\n" +
            "    gl_FragColor = vec4(createGradient(coords, color1, color2, color3, color4), texColorAlpha * alpha);\n" +
            "}";

    public static String vertex = "#version 120 \n" +
            "\n" +
            "void main(void) {\n" +
            "    //Map gl_MultiTexCoord0 to index zero of gl_TexCoord\n" +
            "    gl_TexCoord[0] = gl_MultiTexCoord0;\n" +
            "\n" +
            "    //Calculate position by multiplying model, view and projection matrix by the vertex vector\n" +
            "    gl_Position = gl_ModelViewProjectionMatrix * gl_Vertex;\n" +
            "}\n";


    public static final String roundedRect = "#version 120\n" +
            "\n" +
            "uniform vec2 location, rectSize;\n" +
            "uniform vec4 color;\n" +
            "uniform float radius;\n" +
            "uniform bool blur;\n" +
            "\n" +
            "float roundSDF(vec2 p, vec2 b, float r) {\n" +
            "    return length(max(abs(p) - b, 0.0)) - r;\n" +
            "}\n" +
            "\n" +
            "\n" +
            "void main() {\n" +
            "    vec2 rectHalf = rectSize * .5;\n" +
            "    // Smooth the result (free antialiasing).\n" +
            "    float smoothedAlpha =  (1.0-smoothstep(0.0, 1.0, roundSDF(rectHalf - (gl_TexCoord[0].st * rectSize), rectHalf - radius - 1., radius))) * color.a;\n" +
            "    gl_FragColor = vec4(color.rgb, smoothedAlpha);// mix(quadColor, shadowColor, 0.0);\n" +
            "\n" +
            "}";
    public static final String roundedRectGradient = "#version 120\n" +
            "\n" +
            "uniform vec2 location, rectSize;\n" +
            "uniform vec4 color1, color2, color3, color4;\n" +
            "uniform float radius;\n" +
            "\n" +
            "#define NOISE .5/255.0\n" +
            "\n" +
            "float roundSDF(vec2 p, vec2 b, float r) {\n" +
            "    return length(max(abs(p) - b , 0.0)) - r;\n" +
            "}\n" +
            "\n" +
            "vec3 createGradient(vec2 coords, vec3 color1, vec3 color2, vec3 color3, vec3 color4){\n" +
            "    vec3 color = mix(mix(color1.rgb, color2.rgb, coords.y), mix(color3.rgb, color4.rgb, coords.y), coords.x);\n" +
            "    //Dithering the color\n" +
            "    // from https://shader-tutorial.dev/advanced/color-banding-dithering/\n" +
            "    color += mix(NOISE, -NOISE, fract(sin(dot(coords.xy, vec2(12.9898, 78.233))) * 43758.5453));\n" +
            "    return color;\n" +
            "}\n" +
            "\n" +
            "void main() {\n" +
            "    vec2 st = gl_TexCoord[0].st;\n" +
            "    vec2 halfSize = rectSize * .5;\n" +
            "    \n" +
            "    float smoothedAlpha =  (1.0-smoothstep(0.0, 2., roundSDF(halfSize - (gl_TexCoord[0].st * rectSize), halfSize - radius - 1., radius))) * color1.a;\n" +
            "    gl_FragColor = vec4(createGradient(st, color1.rgb, color2.rgb, color3.rgb, color4.rgb), smoothedAlpha);\n" +
            "}";

    public static final String blurUp="#version 120\n" +
            "\n" +
            "uniform sampler2D inTexture;\n" +
            "uniform vec2 halfpixel, offset;\n" +
            "\n" +
            "void main() {\n" +
            "    vec4 sum = texture2D(inTexture, gl_TexCoord[0].st + vec2(-halfpixel.x * 2.0, 0.0) * offset);\n" +
            "    sum += texture2D(inTexture, gl_TexCoord[0].st + vec2(-halfpixel.x, halfpixel.y) * offset) * 2.0;\n" +
            "    sum += texture2D(inTexture, gl_TexCoord[0].st + vec2(0.0, halfpixel.y * 2.0) * offset);\n" +
            "    sum += texture2D(inTexture, gl_TexCoord[0].st + vec2(halfpixel.x, halfpixel.y) * offset) * 2.0;\n" +
            "    sum += texture2D(inTexture, gl_TexCoord[0].st + vec2(halfpixel.x * 2.0, 0.0) * offset);\n" +
            "    sum += texture2D(inTexture, gl_TexCoord[0].st + vec2(halfpixel.x, -halfpixel.y) * offset) * 2.0;\n" +
            "    sum += texture2D(inTexture, gl_TexCoord[0].st + vec2(0.0, -halfpixel.y * 2.0) * offset);\n" +
            "    sum += texture2D(inTexture, gl_TexCoord[0].st + vec2(-halfpixel.x, -halfpixel.y) * offset) * 2.0;\n" +
            "\n" +
            "    gl_FragColor = vec4(sum.rgb / 12.0, 1.);\n" +
            "}\n";
    public static final String blurDown="#version 120\n" +
            "\n" +
            "uniform sampler2D inTexture;\n" +
            "uniform vec2 offset, halfpixel;\n" +
            "\n" +
            "void main() {\n" +
            "    vec4 sum = texture2D(inTexture, gl_TexCoord[0].st) * 4.0;\n" +
            "    sum += texture2D(inTexture, gl_TexCoord[0].st - halfpixel.xy * offset);\n" +
            "    sum += texture2D(inTexture, gl_TexCoord[0].st + halfpixel.xy * offset);\n" +
            "    sum += texture2D(inTexture, gl_TexCoord[0].st + vec2(halfpixel.x, -halfpixel.y) * offset);\n" +
            "    sum += texture2D(inTexture, gl_TexCoord[0].st - vec2(halfpixel.x, -halfpixel.y) * offset);\n" +
            "    gl_FragColor = vec4(sum.rgb / 8.0, 1.0);\n" +
            "}\n";


}
