import android.content.Context
import android.opengl.GLES20
import android.opengl.GLSurfaceView
import android.opengl.Matrix
import com.example.myapplication.R
import com.example.myapplication.ui.theme.PhongObject
import com.example.myapplication.ui.theme.ShaderCompiler
import javax.microedition.khronos.egl.EGLConfig
import javax.microedition.khronos.opengles.GL10

class PhongRenderer(private val context: Context, private val selectedPlanetIndex: Int) : GLSurfaceView.Renderer {

    private lateinit var celestiaObject: PhongObject
    private val viewMatrix = FloatArray(16)
    private val projectionMatrix = FloatArray(16)
    private val mvpMatrix = FloatArray(16)
    private val modelMatrix = FloatArray(16)

    private val lightPosition = floatArrayOf(2.0f, 2.0f, 2.0f)

    private val ambientColor = floatArrayOf(0.3f, 0.3f, 0.3f, 1.0f)  // Окружающий свет
    private val diffuseColor = floatArrayOf(0.8f, 0.8f, 0.8f, 1.0f)  // Диффузное освещение
    private val specularColor = floatArrayOf(1.0f, 1.0f, 1.0f, 1.0f) // Зеркальное освещение
    private val shininess = 32.0f                                     // Блеск

    private var time: Float = 0f

    override fun onSurfaceCreated(gl: GL10?, config: EGLConfig?) {
        GLES20.glClearColor(0f, 0f, 0f, 1f)
        GLES20.glEnable(GLES20.GL_DEPTH_TEST)

        val textureResId = getObjectTexture(selectedPlanetIndex)
        celestiaObject = PhongObject(
            context,
            radius = 1.0f,
            lightPosition = lightPosition,
            ambientColor = ambientColor,
            diffuseColor = diffuseColor,
            specularColor = specularColor,
            shininess = shininess,
            textureResId = textureResId
        )
        if (selectedPlanetIndex == 7) {
            celestiaObject.shaderCompiler = ShaderCompiler(VERTEX_SHADER_CODE, FRAGMENT_SHADER_CODE)
        }
    }

    override fun onDrawFrame(gl: GL10?) {
        GLES20.glClear(GLES20.GL_COLOR_BUFFER_BIT or GLES20.GL_DEPTH_BUFFER_BIT)

        Matrix.setLookAtM(viewMatrix, 0, 0f, 0f, 5f, 0f, 0.5f, 0f, 0f, 1f, 0f)
        Matrix.multiplyMM(mvpMatrix, 0, projectionMatrix, 0, viewMatrix, 0)

        Matrix.setIdentityM(modelMatrix, 0)
        Matrix.rotateM(modelMatrix, 0, 20f, 0f, 1f, 0f)

        time += 0.03f
        if (selectedPlanetIndex == 7) {
            GLES20.glUniform1f(GLES20.glGetUniformLocation(celestiaObject.shaderCompiler.programId, "u_Time"), time)
        }
        celestiaObject.draw(mvpMatrix, modelMatrix, viewMatrix)
    }

    override fun onSurfaceChanged(gl: GL10?, width: Int, height: Int) {
        GLES20.glViewport(0, 0, width, height)
        val ratio: Float = width.toFloat() / height.toFloat()
        Matrix.frustumM(projectionMatrix, 0, -ratio, ratio, -1f, 1f, 2f, 7f)
    }

    fun getObjectTexture(index: Int): Int {
        return when (index) {
            0 -> R.drawable._k_mercury
            1 -> R.drawable._k_venus_surface
            2 -> R.drawable.earth
            3 -> R.drawable.kmars
            4 -> R.drawable.kjupiter
            5 -> R.drawable._ksaturn
            6 -> R.drawable.kuranus
            7 -> R.drawable.volna
            8 -> R.drawable.moon
            9 -> R.drawable.ksun
            else -> R.drawable.galaktik
        }
    }

    companion object {
        private const val VERTEX_SHADER_CODE = """
            uniform mat4 u_MVPMatrix;
            uniform mat4 u_ModelMatrix;
            uniform mat4 u_ViewMatrix;
            uniform float u_Time; // Время для анимации волн
            
            attribute vec3 a_Position;
            attribute vec3 a_Normal;
            attribute vec2 a_TexCoord;
            
            varying vec3 v_Normal;
            varying vec3 v_FragPos;
            varying vec2 v_TexCoord;
            
            void main() {
                // Позиция вершины на основе синусоидальной функции (волны)
                float waveHeight = 0.05 * sin(a_Position.x * 10.0 + u_Time) * sin(a_Position.z * 10.0 + u_Time);
                
                // Изменяем высоту позиции вершины
                vec3 modifiedPosition = a_Position;
                modifiedPosition.y += waveHeight;
            
                // Передаем данные во фрагментный шейдер
                v_FragPos = vec3(u_ModelMatrix * vec4(modifiedPosition, 1.0));
                v_Normal = normalize(vec3(u_ModelMatrix * vec4(a_Normal, 0.0)));
                v_TexCoord = a_TexCoord;
            
                // Вычисляем финальную позицию
                gl_Position = u_MVPMatrix * vec4(modifiedPosition, 1.0);
            }
        """

        private const val FRAGMENT_SHADER_CODE = """
            precision mediump float;

            uniform vec3 u_LightPos;
            uniform vec4 u_AmbientColor;
            uniform vec4 u_DiffuseColor;
            uniform vec4 u_SpecularColor;
            uniform float u_Shininess;
            
            uniform sampler2D u_Texture; // Текстура для воды
            varying vec3 v_Normal;
            varying vec3 v_FragPos;
            varying vec2 v_TexCoord;
            
            void main() {
                // Нормаль
                vec3 norm = normalize(v_Normal);
                vec3 lightDir = normalize(u_LightPos - v_FragPos);
                
                // Окружающий свет
                vec4 ambient = u_AmbientColor;
                
                // Диффузное освещение
                float diff = max(dot(norm, lightDir), 0.0);
                vec4 diffuse = diff * u_DiffuseColor;
                
                // Зеркальное освещение
                vec3 viewDir = normalize(-v_FragPos);  // Вектор взгляда
                vec3 reflectDir = reflect(-lightDir, norm);
                float spec = pow(max(dot(viewDir, reflectDir), 0.0), u_Shininess);
                vec4 specular = spec * u_SpecularColor;
                
                // Получаем цвет из текстуры
                vec4 textureColor = texture2D(u_Texture, v_TexCoord);
                
                // Итоговый цвет: комбинация освещения и текстуры
                vec4 finalColor = (ambient + diffuse + specular) * textureColor;
                gl_FragColor = finalColor;
            }
        """
    }
}