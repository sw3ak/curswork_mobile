package com.example.myapplication.ui.theme

import android.content.Context
import android.opengl.GLES20
import android.opengl.Matrix
import kotlin.math.sin

class BlackHole(context: Context, radius: Float, textureResId: Int) : TexturedSphere(context, radius, textureResId) {

    private var positionX = 20f
    private var positionZ = -10f
    private val speed = 0.05f
    private var rotationAngle = 0f
    private val rotationSpeed = 1f
    private var alpha = 1f
    private val alphaSpeed = 0.05f

    private var shaderProgram: ShaderCompiler? = null
    private var shaderAlphaHandle: Int = 0

    init {
        val vertexShaderCode = """
            attribute vec4 a_Position;
            attribute vec2 a_TextureCoordinate;
            varying vec2 v_TextureCoordinate;
            uniform mat4 u_MVPMatrix;

            void main() {
                gl_Position = u_MVPMatrix * a_Position;
                v_TextureCoordinate = a_TextureCoordinate;
            }
        """.trimIndent()

        val fragmentShaderCode = """
            precision mediump float;
            varying vec2 v_TextureCoordinate;
            uniform sampler2D u_Texture;
            uniform float u_Alpha;

            void main() {
                vec4 textureColor = texture2D(u_Texture, v_TextureCoordinate);
                gl_FragColor = vec4(textureColor.rgb, textureColor.a * u_Alpha);  
            }
        """.trimIndent()

        shaderProgram = ShaderCompiler(vertexShaderCode, fragmentShaderCode)
        shaderAlphaHandle = GLES20.glGetUniformLocation(shaderProgram?.programId ?: 0, "u_Alpha")
    }

    override fun draw(mvpMatrix: FloatArray) {
        positionX -= speed
        positionZ += speed

        if (positionX < -25f || positionZ > 25f) {
            positionX = 20f
            positionZ = -10f
        }

        rotationAngle += rotationSpeed

        alpha = 0.5f + 0.5f * sin(alphaSpeed * positionX)

        val modelMatrix = FloatArray(16)
        Matrix.setIdentityM(modelMatrix, 0)

        Matrix.translateM(modelMatrix, 0, positionX, 0f, positionZ)

        Matrix.rotateM(modelMatrix, 0, rotationAngle, 0f, 1f, 0f)

        val finalMatrix = FloatArray(16)
        Matrix.multiplyMM(finalMatrix, 0, mvpMatrix, 0, modelMatrix, 0)

        shaderProgram?.use()

        val mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram?.programId ?: 0, "u_MVPMatrix")
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, finalMatrix, 0)

        GLES20.glEnable(GLES20.GL_BLEND)
        GLES20.glBlendFunc(GLES20.GL_SRC_ALPHA, GLES20.GL_ONE_MINUS_SRC_ALPHA)
        GLES20.glUniform1f(shaderAlphaHandle, alpha)

        super.draw(finalMatrix)

        GLES20.glDisable(GLES20.GL_BLEND)
    }
}
