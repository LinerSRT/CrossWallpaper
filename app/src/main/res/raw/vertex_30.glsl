#version 300 es

layout(location = 0) in vec2 in_position;
layout(location = 1) in vec2 in_tex_coord;

uniform mat4 mvp;

out vec2 tex_coord;

void main() {
    gl_Position = mvp * vec4(in_position, 1.0f, 1.0f);
    tex_coord = in_tex_coord;
}