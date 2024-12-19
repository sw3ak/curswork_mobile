package com.example.myapplication.ui.theme

import android.content.Context
import android.graphics.BitmapFactory
import android.opengl.GLES20
import android.opengl.GLUtils
import android.os.Build
import androidx.annotation.RequiresApi
import com.example.myapplication.ui.theme.ShaderCompiler
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.cos
import kotlin.math.sin

@RequiresApi(Build.VERSION_CODES.FROYO)
class PhongObject(
    private val context: Context,
    private val radius: Float,
    private val lightPosition: FloatArray,  // Позиция источника света
    private val ambientColor: FloatArray,   // Окружающий свет
    private val diffuseColor: FloatArray,   // Диффузный свет
    private val specularColor: FloatArray,  // Зеркальный свет
    private val shininess: Float,           // Коэффициент блеска
    private val textureResId: Int
) {
    private val vertexBuffer: FloatBuffer
    private val indexBuffer: ShortBuffer
    private val normalBuffer: FloatBuffer
    private val textureBuffer: FloatBuffer
    private val vertices: FloatArray
    private val normals: FloatArray
    private val textureCoords: FloatArray
    private val indices: ShortArray

    private var textureId: Int = 0

    var shaderCompiler: ShaderCompiler

    init {
        val latitudeBands = 40
        val longitudeBands = 40

        val vertexList = mutableListOf<Float>()
        val normalList = mutableListOf<Float>()
        val textureList = mutableListOf<Float>()
        val indexList = mutableListOf<Short>()

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

                normalList.add(x)
                normalList.add(y)
                normalList.add(z)

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
        normals = normalList.toFloatArray()
        textureCoords = textureList.toFloatArray()
        indices = indexList.toShortArray()

        vertexBuffer = ByteBuffer.allocateDirect(vertices.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(vertices)
                position(0)
            }
        }

        normalBuffer = ByteBuffer.allocateDirect(normals.size * 4).run {
            order(ByteOrder.nativeOrder())
            asFloatBuffer().apply {
                put(normals)
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

        indexBuffer = ByteBuffer.allocateDirect(indices.size * 2).run {
            order(ByteOrder.nativeOrder())
            asShortBuffer().apply {
                put(indices)
                position(0)
            }
        }

        shaderCompiler = ShaderCompiler(VERTEX_SHADER_CODE, FRAGMENT_SHADER_CODE)

        textureId = loadTexture(textureResId)
    }

    @RequiresApi(Build.VERSION_CODES.FROYO)
    private fun loadTexture(resId: Int): Int {
        val textureHandle = IntArray(1)
        GLES20.glGenTextures(1, textureHandle, 0)

        if (textureHandle[0] != 0) {
            val options = BitmapFactory.Options()
            options.inScaled = false

            val bitmap = BitmapFactory.decodeResource(context.resources, resId, options)

            GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureHandle[0])

            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MIN_FILTER, GLES20.GL_LINEAR)
            GLES20.glTexParameteri(GLES20.GL_TEXTURE_2D, GLES20.GL_TEXTURE_MAG_FILTER, GLES20.GL_LINEAR)

            GLUtils.texImage2D(GLES20.GL_TEXTURE_2D, 0, bitmap, 0)

            bitmap.recycle()
        }

        return textureHandle[0]
    }

    @RequiresApi(Build.VERSION_CODES.FROYO)
    fun draw(mvpMatrix: FloatArray, modelMatrix: FloatArray, viewMatrix: FloatArray) {
        shaderCompiler.use()

        val positionHandle = GLES20.glGetAttribLocation(shaderCompiler.programId, "a_Position")
        val normalHandle = GLES20.glGetAttribLocation(shaderCompiler.programId, "a_Normal")
        val texCoordHandle = GLES20.glGetAttribLocation(shaderCompiler.programId, "a_TexCoord")

        GLES20.glEnableVertexAttribArray(positionHandle)
        GLES20.glVertexAttribPointer(positionHandle, 3, GLES20.GL_FLOAT, false, 0, vertexBuffer)

        GLES20.glEnableVertexAttribArray(normalHandle)
        GLES20.glVertexAttribPointer(normalHandle, 3, GLES20.GL_FLOAT, false, 0, normalBuffer)

        GLES20.glEnableVertexAttribArray(texCoordHandle)
        GLES20.glVertexAttribPointer(texCoordHandle, 2, GLES20.GL_FLOAT, false, 0, textureBuffer)

        GLES20.glActiveTexture(GLES20.GL_TEXTURE0)
        GLES20.glBindTexture(GLES20.GL_TEXTURE_2D, textureId)

        val mvpMatrixHandle = GLES20.glGetUniformLocation(shaderCompiler.programId, "u_MVPMatrix")
        GLES20.glUniformMatrix4fv(mvpMatrixHandle, 1, false, mvpMatrix, 0)

        val modelMatrixHandle = GLES20.glGetUniformLocation(shaderCompiler.programId, "u_ModelMatrix")
        GLES20.glUniformMatrix4fv(modelMatrixHandle, 1, false, modelMatrix, 0)

        val viewMatrixHandle = GLES20.glGetUniformLocation(shaderCompiler.programId, "u_ViewMatrix")
        GLES20.glUniformMatrix4fv(viewMatrixHandle, 1, false, viewMatrix, 0)

        val lightPosHandle = GLES20.glGetUniformLocation(shaderCompiler.programId, "u_LightPos")
        GLES20.glUniform3fv(lightPosHandle, 1, floatArrayOf(lightPosition[0] * 1.5f, lightPosition[1] * 1.5f, lightPosition[2]), 0)


        val ambientColorHandle = GLES20.glGetUniformLocation(shaderCompiler.programId, "u_AmbientColor")
        GLES20.glUniform4fv(ambientColorHandle, 1, floatArrayOf(ambientColor[0] * 0.5f, ambientColor[1] * 0.5f, ambientColor[2] * 0.5f, ambientColor[3]), 0)


        val diffuseColorHandle = GLES20.glGetUniformLocation(shaderCompiler.programId, "u_DiffuseColor")
        GLES20.glUniform4fv(diffuseColorHandle, 1, floatArrayOf(diffuseColor[0] * 1.5f, diffuseColor[1] * 1.5f, diffuseColor[2] * 1.5f, diffuseColor[3]), 0)

        val specularColorHandle = GLES20.glGetUniformLocation(shaderCompiler.programId, "u_SpecularColor")
        GLES20.glUniform4fv(specularColorHandle, 1, floatArrayOf(specularColor[0] * 1.5f, specularColor[1] * 1.5f, specularColor[2] * 1.5f, specularColor[3]), 0)

        val shininessHandle = GLES20.glGetUniformLocation(shaderCompiler.programId, "u_Shininess")
        GLES20.glUniform1f(shininessHandle, shininess)



        GLES20.glDrawElements(GLES20.GL_TRIANGLES, indices.size, GLES20.GL_UNSIGNED_SHORT, indexBuffer)

        GLES20.glDisableVertexAttribArray(positionHandle)
        GLES20.glDisableVertexAttribArray(normalHandle)
        GLES20.glDisableVertexAttribArray(texCoordHandle)
    }


    companion object {
        private const val VERTEX_SHADER_CODE = """
uniform mat4 u_MVPMatrix;   // Матрица проекции и модели
uniform mat4 u_ModelMatrix; // Матрица модели
uniform mat4 u_ViewMatrix;  // Матрица камеры

attribute vec3 a_Position;
attribute vec3 a_Normal;
attribute vec2 a_TexCoord;

varying vec3 v_Normal;
varying vec3 v_FragPos;
varying vec2 v_TexCoord;

void main() {
    v_FragPos = vec3(u_ModelMatrix * vec4(a_Position, 1.0));
    v_Normal = normalize(mat3(u_ModelMatrix) * a_Normal);  // Преобразуем нормали
    v_TexCoord = a_TexCoord;

    gl_Position = u_MVPMatrix * vec4(a_Position, 1.0);
}

        """

        private const val FRAGMENT_SHADER_CODE = """
precision mediump float;

uniform vec3 u_LightPos;       // Позиция источника света
uniform vec4 u_AmbientColor;   // Окружающий свет
uniform vec4 u_DiffuseColor;   // Диффузный свет
uniform vec4 u_SpecularColor;  // Зеркальное отражение
uniform float u_Shininess;     // Блеск (shininess)

uniform sampler2D u_Texture;   // Текстура объекта
varying vec3 v_Normal;
varying vec3 v_FragPos;
varying vec2 v_TexCoord;

void main() {
    vec3 norm = normalize(v_Normal);  // Нормаль
    vec3 lightDir = normalize(u_LightPos - v_FragPos);  // Направление к источнику света

    // Окружающее освещение
    vec4 ambient = u_AmbientColor;

    // Диффузное освещение
    float diff = max(dot(norm, lightDir), 0.0);
    vec4 diffuse = diff * u_DiffuseColor;

    // Зеркальное освещение
    vec3 viewDir = normalize(-v_FragPos);  // Вектор взгляда
    vec3 reflectDir = reflect(-lightDir, norm);
    float spec = pow(max(dot(viewDir, reflectDir), 0.0), u_Shininess);
    vec4 specular = spec * u_SpecularColor;

    // Цвет из текстуры
    vec4 textureColor = texture2D(u_Texture, v_TexCoord);

    // Итоговый цвет: комбинация освещения и текстуры
vec4 finalColor = (ambient + diffuse + specular) * textureColor;


    gl_FragColor = finalColor;
}

        """
    }
}