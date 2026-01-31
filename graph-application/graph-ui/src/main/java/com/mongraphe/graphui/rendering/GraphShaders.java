package com.mongraphe.graphui.rendering;

public class GraphShaders {

    String vertexShaderSourcePoints = """
            #version 400 core
            layout(location = 0) in vec2 position;
            layout(location = 1) in float size;
            layout(location = 2) in vec3 color;
            layout(location = 3) in float visibility;
            uniform mat4 u_transform;
            out vec3 fragColor;
            out float fragVisibility;
            void main() {
               vec4 pos = vec4(position, 0.0, 1.0);
               gl_Position = u_transform * pos;
               gl_PointSize = size;
               fragColor = color;
               fragVisibility = visibility;
            }
            		""";

    String fragmentShaderSourcePoints = """
            #version 400 core
            in vec3 fragColor;
            in float fragVisibility;
            out vec4 color;
            void main() {
               if (fragVisibility == 0.0) {
                      discard;
                  }
               float dist = length(gl_PointCoord - vec2(0.5, 0.5));
               if (dist < 0.5) {
                   color = vec4(fragColor, 1.0);
               } else {
                   discard;
               }
            }
            """;

    String vertexShaderSourceEdges = """
            #version 400 core
            layout(location = 0) in vec2 position;
            layout(location = 1) in vec3 color;
            layout(location = 2) in float size;
            layout(location = 3) in float visibility;
            uniform mat4 u_transform;
            out vec3 fragColor;
            out float fragVisibility;
            void main() {
                vec4 pos = vec4(position, 0.0, 1.0);
                gl_Position = u_transform * pos;
                fragColor = color;
                fragVisibility = visibility;
            }
             		""";

    String fragmentShaderSourceEdges = """
            #version 400 core
            in vec3 fragColor;
            in float fragVisibility;
            out vec4 color;
            void main() {
                if (fragVisibility == 0.0) {
                    discard;
                }
                color = vec4(fragColor, 1.0);
            }
            """;

}
