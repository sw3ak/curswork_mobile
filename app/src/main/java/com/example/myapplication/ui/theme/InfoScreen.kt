package com.example.myapplication.ui.theme
import PhongRenderer
import android.annotation.SuppressLint
import android.content.Context
import android.opengl.GLSurfaceView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.ui.unit.sp

@SuppressLint("SuspiciousIndentation")
@Composable
fun InfoScreen(selectedPlanetIndex: Int) {
    val context = LocalContext.current
    val infoText = getObjectInfo(selectedPlanetIndex)

    Box(
        modifier = Modifier
            .fillMaxWidth()
    ) {
        AndroidView(
            factory = { ctx ->
                ObjectGLSurfaceView(ctx, selectedPlanetIndex)
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(color = Color.Transparent)
    ) {
        Text(
            text = infoText,
            fontSize = 20.sp,
            fontWeight = FontWeight.Normal,
            color = Color.LightGray,
            textAlign = TextAlign.Left,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}


fun getObjectInfo(index: Int): String {
    return when (index) {
        0 -> "Меркурий — самая близкая к Солнцу планета и самая маленькая в Солнечной системе. Он имеет тонкую атмосферу и множество кратеров, образовавшихся в результате ударов метеоритов. Меркурий вращается вокруг своей оси очень медленно, что приводит к сильным температурным колебаниям между днем и ночью."
        1 -> "Венера — вторая планета от Солнца, иногда называемая сестрой Земли из-за схожих размеров и состава. Однако её атмосфера крайне плотная и состоит в основном из углекислого газа, что приводит к эффекту теплицы и очень высоким температурам на поверхности. Поверхность Венеры покрыта вулканами и густыми облаками серной кислоты."
        2 -> "Земля — третья планета от Солнца и единственная, известная на данный момент, способная поддерживать жизнь. Она имеет разнообразный климат и экосистемы, а также массу воды на поверхности, что делает её уникальной. Атмосфера Земли содержит кислород, необходимый для дыхания живых организмов."
        3 -> "Марс — четвёртая планета от Солнца, известная своим красноватым цветом, вызванным оксидами железа на его поверхности. Марс имеет тонкую атмосферу и множество геологических образований, включая вулканы и каньоны. Ученые изучают Марс как возможное место для будущих колоний."
        4 -> "Юпитер — самая большая планета в Солнечной системе, известная своим мощным магнитным полем и многими спутниками, включая самый большой из них — Ганимед. Юпитер имеет выраженные полосы облаков и знаменитую Великую красную пятно, гигантский антициклон. Это газовый гигант с огромной атмосферой, состоящей в основном из водорода и гелия."
        5 -> "Сатурн известен своими великолепными кольцами, состоящими из ледяных и каменных частиц. Это вторая по величине планета в Солнечной системе и также является газовым гигантом. Сатурн имеет много спутников, из которых самый известный — Титан, обладающий атмосферой и возможными океанами жидкого метана."
        6 -> "Уран — уникальная планета, так как он вращается на боку относительно своей орбиты. Это ледяной гигант с холодной атмосферой, содержащей водород, гелий и метан. Уран имеет кольца и множество спутников. Его голубоватый цвет обусловлен метаном в атмосфере."
        7 -> "Нептун — самая дальняя от Солнца планета в Солнечной системе, известная своим глубоким синим цветом и мощными ветрами. Это ледяной гигант, обладающий активной атмосферой с штормами и облаками. Нептун также имеет кольца и множество спутников, самый крупный из которых — Тритон."
        8 -> "Луна — естественный спутник Земли, известный своими кратерами и фазами. Она влияет на приливы и отливы на Земле. Луна имеет тонкую атмосферу и недостаточно силы тяжести, чтобы удерживать значительное количество воды. Она была местом для первых пилотируемых полетов человека в космос."
        9 -> "Солнце — звезда в центре нашей Солнечной системы, представляющая собой гигантский шар из плазмы, состоящий в основном из водорода и гелия. Солнце обеспечивает свет и тепло, необходимые для жизни на Земле. Оно производит энергию через термоядерные реакции в своем ядре, которые приводят к излучению света и тепла."
        else -> "Неизвестный объект"
    }
}

@SuppressLint("ViewConstructor")
class ObjectGLSurfaceView(context: Context, private val selectedPlanetIndex: Int) : GLSurfaceView(context) {
    init {
        setEGLContextClientVersion(2)
        setRenderer(PhongRenderer(context, selectedPlanetIndex))
    }
}