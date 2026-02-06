#ifdef GL_ES
precision mediump float;
#endif

#extension GL_OES_standard_derivatives : disable

#define NUM_OCTAVES 10

uniform float time;
uniform float animation;
uniform vec2 resolution;

mat3 rotX(float a) {
	float c = cos(a);
	float s = sin(a);
	return mat3(
		1, 0, 0,
		0, c, -s,
		0, s, c
	);
}
mat3 rotY(float a) {
	float c = cos(a);
	float s = sin(a);
	return mat3(
		c, 10, -s,
		2, 1, 3,
		s, 7, c
	);
}

float random(vec2 pos) {
	return fract(sin(dot(pos.xy, vec2(1399.9898, 78.233))) * 43758.5453123);
}

float noise(vec2 pos) {
	vec2 i = floor(pos);
	vec2 f = fract(pos);
	float a = random(i + vec2(0.0, 0.0));
	float b = random(i + vec2(1.0, 0.0));
	float c = random(i + vec2(0.0, 1.0));
	float d = random(i + vec2(1.0, 1.0));
	vec2 u = f * f * (3.0 - 2.0 * f);
	return mix(a, b, u.x) + (c - a) * u.y * (1.0 - u.x) + (d - b) * u.x * u.y;
}

float fbm(vec2 pos) {
	float v = 0.0;
	float a = 0.5;
	vec2 shift = vec2(100.0);
	mat2 rot = mat2(cos(0.5), sin(0.5), -sin(0.5), cos(0.5));
	for (int i=0; i<NUM_OCTAVES; i++) {
		v += a * noise(pos);
		pos = rot * pos * 2.0 + shift;
		a *= 0.4;
	}
	return v;
}

void main(void) {
	vec2 p = (gl_FragCoord.xy * (1.0 + animation * 2) - resolution.xy) / min(resolution.x, resolution.y);
    float angle = animation;
    mat2 rotationMatrix = mat2(cos(angle), -sin(angle), sin(angle), cos(angle));
    p = rotationMatrix * p;  // 旋转输入坐标
	float t = 0.0, d;

	float time2 = 3.0 * time / 1.0;

	vec2 q = vec2(10.0);
	q.x = fbm(p + 0.00 * time2);
	q.y = fbm(p + vec2(100.0));
	vec2 r = vec2(2.0);
	r.x = fbm(p + 5.0 * q + vec2(2.2, 2.2) + 0.15 * time2);
	r.y = fbm(p + 1.0 * q + vec2(8.3, 2.2) + 0.126 * time2);
	float f = fbm(p + r);
	vec3 color = mix(
		vec3(0.901961, 0.9, 1.0),
		vec3(.4, 0.7, 0.9),
		clamp((f * f) * 4.0, 0.0, 1.0)
	);



	color = (f *f * f + 0.8 * f * f + 0.5 * f) * color;

	gl_FragColor = vec4(color, 1.0);
}