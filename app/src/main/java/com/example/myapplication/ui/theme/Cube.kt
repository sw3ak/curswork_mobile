package com.example.myapplication.ui.theme

import android.opengl.GLES20
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer

class Cube {
    private var program: Int = 0
    private var edgeProgram: Int = 0
    private val vertexBuffer: FloatBuffer
    private val indexBuffer: ShortBuffer
    private val edgeBuffer: ShortBuffer

    private val vertices = floatArrayOf(
        -0.5f, 0.5f, 0.5f,   // Передняя грань (верхний левый)
        0.5f, 0.5f, 0.5f,    // Передняя грань (верхний правый)
        0.5f, -0.5f, 0.5f,   // Передняя грань (нижний правый)
        -0.5f, -0.5f, 0.5f,  // Передняя грань (нижний левый)
        -0.5f, 0.5f, -0.5f,  // Задняя грань (верхний левый)
        0.5f, 0.5f, -0.5f,   // Задняя грань (верхний правый)
        0.5f, -0.5f, -0.5f,  // Задняя грань (нижний правый)
        -0.5f, -0.5f, -0.5f  // Задняя грань (нижний левый)
    )

    private val indices = shortArrayOf(
        0, 1, 2, 0, 2, 3,  // Передняя грань
        4, 5, 6, 4, 6, 7,  // Задняя грань
        0, 3, 7, 0, 7, 4,  // Левая грань
        1, 5, 6, 1, 6, 2,  // Правая грань
        0, 1, 5, 0, 5, 4,  // Верхняя грань
        3, 2, 6, 3, 6, 7   // Нижняя грань
    )

    private val edges = shortArrayOf(
        0, 1, 1, 2, 2, 3, 3, 0,  // Передняя грань
        4, 5, 5, 6, 6, 7, 7, 4,  // Задняя грань
        0, 4, 1, 5, 2, 6, 3, 7   // Соединения передней и задней грани
    )

    init {
        vertexBuffer = createFloatBuffer(vertices)
        indexBuffer = createShortBuffer(indices)
        edgeBuffer = createShortBuffer(edges)

        val vertexShaderCode = """
            attribute vec4 vPosition;
            uniform mat4 uMVPMatrix;
            void main() {
                gl_Position = uMVPMatrix * vPosition;
            }
        """.trimIndent()

        val fragmentShaderCode = """
            precision mediump float;
            uniform float uAlpha;
            void main() {
                gl_FragColor = vec4(0.0, 0.0, 0.5, uAlpha);
            }
        """.trimIndent()

        val vertexShader = loadShader(GLES20.GL_VERTEX_SHADER, vertexShaderCode)
        val fragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, fragmentShaderCode)

        program = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, fragmentShader)
            GLES20.glLinkProgram(it)
        }

        val edgeFragmentShaderCode = """
            precision mediump float;
            void main() {
                gl_FragColor = vec4(1.0, 1.0, 1.0, 0.0);
            }
        """.trimIndent()

        val edgeFragmentShader = loadShader(GLES20.GL_FRAGMENT_SHADER, edgeFragmentShaderCode)

        edgeProgram = GLES20.glCreateProgram().also {
            GLES20.glAttachShader(it, vertexShader)
            GLES20.glAttachShader(it, edgeFragmentShader)
            GLES20.glLinkProgram(it)
        }
    }

    fun draw(mvpMatrix: FloatArray, planetPosition: FloatArray, planetRadius: Float) {
        val scaleFactor = planetRadius * 2.1f

        val finalMatrix = FloatArray(16)
        if (planetPosition.size < 4) {
            val transformationMatrix = FloatArray(16)
            Matrix.setIdentityM(transformationMatrix, 0)
            Matrix.translateM(transformationMatrix, 0, planetPosition[0], planetPosition[1], planetPosition[2])
            Matrix.scaleM(transformationMatrix, 0, scaleFactor, scaleFactor, scaleFactor)
            Matrix.multiplyMM(finalMatrix, 0, mvpMatrix, 0, transformationMatrix, 0)
        } else {
            val planetModelMatrix = FloatArray(16)
            Matrix.setIdentityM(planetModelMatrix, 0)
            Matrix.rotateM(planetModelMatrix, 0, planetPosition[0], 0f, 1f, 0f)
            Matrix.translateM(planetModelMatrix, 0, planetPosition[1], 0f, 0f)

            val moonRotationMatrix = FloatArray(16)
            Matrix.setIdentityM(moonRotationMatrix, 0)
            Matrix.rotateM(moonRotationMatrix, 0, planetPosition[2], 0f, 1f, 0f)
            Matrix.translateM(moonRotationMatrix, 0, planetPosition[3], 0f, 0f)

            Matrix.scaleM(moonRotationMatrix, 0, scaleFactor, scaleFactor, scaleFactor)
            Matrix.multiplyMM(finalMatrix, 0, planetModelMatrix, 0, moonRotationMatrix, 0)
            Matrix.multiplyMM(finalMatrix, 0, mvpMatrix, 0, finalMatrix, 0)

        }

        GLES20.glUseProgram(program)

        val positionHandle = GLES20.glGetAttribLocation(program, "vPosition")
        val mvpMatrixHandle = GLES20.glGetUniformLocation(program, "uMVPMatrix")
        val alphaHandle = GLES20.glGetUniformLocation(program, "uAlpha")

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, finalMatrix, 0)

        GLES20.glUniform1f(alphaHandle, 0.5f)

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)

        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.size, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisable(GLES20.GL_BLEND)

        GLES20.glUseProgram(edgeProgram)

        val edgePositionHandle = GLES20.glGetAttribLocation(edgeProgram, "vPosition")
        val edgeMvpMatrixHandle = GLES20.glGetUniformLocation(edgeProgram, "uMVPMatrix")

        GLES20.glEnableVertexAttribArray(edgePositionHandle)
        GLES20.glVertexAttribPointer(edgePositionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glUniformMatrix4fv(edgeMvpMatrixHandle, 1, false, finalMatrix, 0)
        GLES20.glDrawElements(GLES20.GL_LINES, edges.size, GLES20.GL_UNSIGNED_SHORT, edgeBuffer)

        GLES20.glDisableVertexAttribArray(edgePositionHandle)
    }


    private fun createFloatBuffer(data: FloatArray): FloatBuffer {
        return ByteBuffer.allocateDirect(data.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(data)
                position(0)
            }
        }
    }

    private fun createShortBuffer(data: ShortArray): ShortBuffer {
        return ByteBuffer.allocateDirect(data.size * 2).run {
            order(ByteOrder.nativeOrder())
            asShortBuffer().apply {
                put(data)
                position(0)
            }
        }
    }

    private fun loadShader(type: Int, shaderCode: String): Int {
        return GLES20.glCreateShader(type).also { shader ->
            GLES20.glShaderSource(shader, shaderCode)
            GLES20.glCompileShader(shader)
        }
    }
}
