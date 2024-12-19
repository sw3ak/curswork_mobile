package com.example.myapplication.ui.theme

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import android.opengl.Matrix
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.cos
import kotlin.math.sin

abstract class TexturedSphere(
    val context: Context,
    val radius: Float,
    private val textureResId: Int
) {
    companion object {
        private const val VERTEX_SHADER_CODE = """
            attribute vec4 a_Position;
            attribute vec2 a_TexCoord;
            uniform mat4 u_MVPMatrix;
            varying vec2 v_TexCoord;

            void main() {
                gl_Position = u_MVPMatrix * a_Position;
                v_TexCoord = a_TexCoord;
            }
        """

        private const val FRAGMENT_SHADER_CODE = """
            precision mediump float;
            varying vec2 v_TexCoord;
            uniform sampler2D u_Texture;

            void main() {
                gl_FragColor = texture2D(u_Texture, v_TexCoord);
            }
        """
    }

    private var shaderProgram: ShaderCompiler
    private var vertexBuffer: FloatBuffer
    private var indexBuffer: ShortBuffer
    private var textureBuffer: FloatBuffer
    private var textureId: Int

    private val vertices: FloatArray
    private val indices: ShortArray
    private val textureCoords: FloatArray
    private val latitudeBands: Int = 40
    private val longitudeBands: Int = 40


    init {
        val vertexList = mutableListOf<Float>()
        val indexList = mutableListOf<Short>()
        val textureList = mutableListOf<Float>()

        for (lat in 0..latitudeBands) {
            val theta = lat * Math.PI / latitudeBands
            val sinTheta = sin(theta).toFloat()
            val cosTheta = cos(theta).toFloat()

            for (long in 0..longitudeBands) {
                val phi = long * 2 * Math.PI / longitudeBands
                val sinPhi = sin(phi).toFloat()
                val cosPhi = cos(phi).toFloat()

                val x = cosPhi * sinTheta
                val y = cosTheta
                val z = sinPhi * sinTheta

                vertexList.add(x * radius)
                vertexList.add(y * radius)
                vertexList.add(z * radius)

                val u = 1f - (long / longitudeBands.toFloat())
                val v = 1f - (lat / latitudeBands.toFloat())
                textureList.add(u)
                textureList.add(v)
            }
        }

        for (lat in 0 until latitudeBands) {
            for (long in 0 until longitudeBands) {
                val first = (lat * (longitudeBands + 1) + long).toShort()
                val second = (first + longitudeBands + 1).toShort()

                indexList.add(first)
                indexList.add(second)
                indexList.add((first + 1).toShort())

                indexList.add(second)
                indexList.add((second + 1).toShort())
                indexList.add((first + 1).toShort())
            }
        }

        vertices = vertexList.toFloatArray()
        indices = indexList.toShortArray()
        textureCoords = textureList.toFloatArray()


        shaderProgram = ShaderCompiler(VERTEX_SHADER_CODE, FRAGMENT_SHADER_CODE)

        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(vertices)
                position(0)
            }
        }

        indexBuffer = ByteBuffer.allocateDirect(indices.size * 2).run {
            order(ByteOrder.nativeOrder())
            asShortBuffer().apply {
                put(indices)
                position(0)
            }
        }

        textureBuffer = ByteBuffer.allocateDirect(textureCoords.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(textureCoords)
                position(0)
            }
        }

        textureId = loadTexture()
    }

    open fun draw(mvpMatrix: FloatArray) {
        shaderProgram.use()

        val positionHandle = GLES20.glGetAttribLocation(shaderProgram.programId, "a_Position")
        val texCoordHandle = GLES20.glGetAttribLocation(shaderProgram.programId, "a_TexCoord")
        val mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram.programId, "u_MVPMatrix")

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glEnableVertexAttribArray(texCoordHandle)

        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        GLES20.glDrawElements(
            GLES20.GL_TRIANGLES,
            indices.size,
            GLES20.GL_UNSIGNED_SHORT,
            indexBuffer
        )

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }

    private fun loadTexture(): Int {
        val textureHandle = IntArray(1)

        GLES20.glGenTextures(1, textureHandle, 0)

        if (textureHandle[0] != 0) {
            val options = BitmapFactory.Options()
            options.inScaled = false

            val bitmap = BitmapFactory.decodeResource(context.resources, textureResId, options)

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0])

            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MIN_FILTER,
                GLES20.GL_LINEAR
            )
            GLES20.glTexParameteri(
                GLES20.GL_TEXTURE_2D,
                GLES20.GL_TEXTURE_MAG_FILTER,
                GLES20.GL_LINEAR
            )

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

            bitmap.recycle()
        }

        return textureHandle[0]
    }
}

class Sun(context: Context, radius: Float, textureResId: Int) : TexturedSphere(context, radius, textureResId) {
    private var rotationAngle: Float = 0.0f

    override fun draw(mvpMatrix: FloatArray) {
        rotationAngle += 1.0f

        val modelMatrix = FloatArray(16)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, rotationAngle, 0f, 1f, 0f)
        val finalMatrix = FloatArray(16)
        Matrix.multiplyMM(finalMatrix, 0, mvpMatrix, 0, modelMatrix, 0)

        super.draw(finalMatrix)
    }
}

class Planet(
    context: Context,
    radius: Float,
    textureResId: Int,
    val orbitRadius: Float,
    private val orbitSpeed: Float,
    private val rotationSpeed: Float
) : TexturedSphere(context, radius, textureResId) {
    var orbitAngle: Float = 0.0f
    private var rotationAngle: Float = 0.0f

    override fun draw(mvpMatrix: FloatArray) {
        orbitAngle += orbitSpeed
        rotationAngle += rotationSpeed

        val modelMatrix = FloatArray(16)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, orbitAngle, 0f, 1f, 0f)
        Matrix.translateM(modelMatrix, 0, orbitRadius, 0f, 0f)
        Matrix.rotateM(modelMatrix, 0, rotationAngle, 0f, 1f, 0f)
        val finalMatrix = FloatArray(16)
        Matrix.multiplyMM(finalMatrix, 0, mvpMatrix, 0, modelMatrix, 0)

        super.draw(finalMatrix)
    }

    fun getPosition(): FloatArray {
        val adjustedAngle = orbitAngle + 90f
        val x = orbitRadius * sin(Math.toRadians(adjustedAngle.toDouble())).toFloat()
        val z = orbitRadius * cos(Math.toRadians(adjustedAngle.toDouble())).toFloat()
        return floatArrayOf(x, 0f, z)
    }
}

class Moon(
    context: Context,
    radius: Float,
    textureResId: Int,
    private val planet: Planet,
    val orbitRadius: Float,
    private val orbitSpeed: Float
) : TexturedSphere(context, radius, textureResId) {

    var angle: Float = 0.0f

    override fun draw(mvpMatrix: FloatArray) {
        angle += orbitSpeed

        // Позиция планеты, вокруг которой вращается Луна
        val planetPosition = planet.getPosition()

        // Позиция Луны
        val moonX = planetPosition[0] + orbitRadius * cos(Math.toRadians(angle.toDouble())).toFloat()
        val moonY = orbitRadius * sin(Math.toRadians(angle.toDouble())).toFloat()
        val moonZ = planetPosition[2]

        // Обновляем модельную матрицу для Луны
        val modelMatrix = FloatArray(16)
        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.translateM(modelMatrix, 0, moonX, moonY, moonZ)

        val finalMatrix = FloatArray(16)
        Matrix.multiplyMM(finalMatrix, 0, mvpMatrix, 0, modelMatrix, 0)

        super.draw(finalMatrix)
    }

    fun getAbsolutePosition(): FloatArray {
        // Позиция планеты
        val planetPosition = planet.getPosition()
        val moonX = planetPosition[0] + orbitRadius * cos(Math.toRadians(angle.toDouble())).toFloat()
        val moonY = orbitRadius * sin(Math.toRadians(angle.toDouble())).toFloat()
        val moonZ = planetPosition[2]

        return floatArrayOf(moonX, moonY, moonZ)
    }
}

class Orbit(private val radius: Float, private val segments: Int = 100, private val isVertical: Boolean = false) {
    private var shaderProgram: ShaderCompiler
    private val vertexBuffer: FloatBuffer

    init {
        val vertices = FloatArray(segments * 3)
        val angleStep = (2.0 * Math.PI / segments).toFloat()

        for (i in 0 until segments) {
            val angle = i * angleStep
            vertices[i * 3] = (radius * Math.cos(angle.toDouble())).toFloat()
            vertices[i * 3 + 1] = if (isVertical) (radius * Math.sin(angle.toDouble())).toFloat() else 0f
            vertices[i * 3 + 2] = if (isVertical) 0f else (radius * Math.sin(angle.toDouble())).toFloat()
        }

        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4)
            .order(ByteOrder.nativeOrder())
            .asFloatBuffer()
            .put(vertices)
        vertexBuffer.position(0)
        shaderProgram = ShaderCompiler(VERTEX_SHADER_CODE, FRAGMENT_SHADER_CODE)
    }

    fun draw(mvpMatrix: FloatArray) {
        shaderProgram.use()
        // Отключаем цветовую маску для всех каналов, чтобы линия орбиты не отображалась
        GLES20.glColorMask(false, false, false, false)
        val positionHandle = GLES20.glGetAttribLocation(shaderProgram.programId, "a_Position")
        val mvpMatrixHandle = GLES20.glGetUniformLocation(shaderProgram.programId, "u_MVPMatrix")

        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)
        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glDrawArrays(GLES20.GL_LINE_LOOP, 0, segments)

        GLES20.glDisableVertexAttribArray(positionHandle)
        // Включаем цветовую маску обратно после рисования орбит
        GLES20.glColorMask(true, true, true, true)
    }

    companion object {
        private const val VERTEX_SHADER_CODE = """
            attribute vec4 a_Position;
            uniform mat4 u_MVPMatrix;

            void main() {
                gl_Position = u_MVPMatrix * a_Position;
            }
        """

        private const val FRAGMENT_SHADER_CODE = """
            precision mediump float;
            void main() {
                gl_FragColor = vec4(0.0, 0.0, 0.0, 0.0);
            }
        """
    }
}