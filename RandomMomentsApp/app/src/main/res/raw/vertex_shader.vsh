// #version 300 es
//
// uniform mat4 uMVPMatrix;
// uniform mat4 uSTMatrix;
//
// in vec3 inPosition;
// in vec2 inTextureCoord;
//
// out vec2 textureCoord;
//
// void main() {
//     gl_Position = uMVPMatrix * vec4(inPosition.xyz, 1);
//     textureCoord = (uSTMatrix * vec4(inTextureCoord.xy, 0, 0)).xy;
// }

private const val COORDINATES_PER_VERTEX = 2
private const val VERTEX_STRIDE: Int = COORDINATES_PER_VERTEX * 4
private var quadPositionHandle = -1

private val QUADRANT_COORDINATES = floatArrayOf(
    //x,    y
    -0.5f, 0.5f,
    -0.5f, -0.5f,
    0.5f, -0.5f,
    0.5f, 0.5f
)

private val quadrantCoordinatesBuffer: FloatBuffer = ByteBuffer.allocateDirect(QUADRANT_COORDINATES.size * 4).run {
    order(ByteOrder.nativeOrder())
    asFloatBuffer().apply {
        put(QUADRANT_COORDINATES)
        position(0)
    }
}

//Define quadrant position handler
quadPositionHandle = GLES20.glGetAttribLocation(program, "a_Position")

//Pass quadrant position to shader
GLES20.glVertexAttribPointer(
    quadPositionHandle,
    COORDINATES_PER_VERTEX,
    GLES20.GL_FLOAT,
    false,
    VERTEX_STRIDE,
    quadrantCoordinatesBuffer
)


//In Vertex shader
attribute vec4 a_Position;
void main(void)
{
    gl_Position = a_Position;
}