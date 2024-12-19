package com.example.myapplication

import android.content.Context
import android.opengl.GLSurfaceView
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.runtime.*
import androidx.compose.ui.viewinterop.AndroidView
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Button
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.MaterialTheme
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Surface
//noinspection UsingMaterialAndMaterial3Libraries
import androidx.compose.material.Text
//import androidx.compose.material3.Button
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
//import androidx.wear.compose.material.Button
import com.example.myapplication.ui.theme.InfoScreen
import com.example.myapplication.ui.theme.MyGLRenderer
import com.example.myapplication.ui.theme.NewsScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colors.background) {
                MainNavRouter()
            }
        }
    }
}

@Composable
fun MainNavRouter() {
    val navController = rememberNavController()
    NavHost(navController = navController, startDestination = "news") {
        composable("news") { NewsScreen(navController) }
        composable("opengl") { OpenGLScreen(navController) }
        composable("moon_info/{selectedPlanetIndex}") { backStackEntry ->
            val selectedPlanetIndex = backStackEntry.arguments?.getString("selectedPlanetIndex")?.toInt() ?: 0
            InfoScreen(selectedPlanetIndex = selectedPlanetIndex)
        }
    }
}


class MyGLSurfaceView(context: Context, private val renderer: MyGLRenderer) : GLSurfaceView(context) {
    init {
        setEGLContextClientVersion(2)
        setRenderer(renderer)
    }

    fun setSelectedPlanet(index: Int) {
        renderer.setSelectedObjectIndex(index)
    }
}

@Composable
fun OpenGLScreen(navController: NavController) {
    var selectedPlanetIndex by remember { mutableStateOf(0) }
    val planetCount = 10

    val context = LocalContext.current
    val renderer = remember { MyGLRenderer(context) }

    Box() {
        AndroidView(
            factory = { ctx ->
                MyGLSurfaceView(ctx, renderer).apply {
                    setSelectedPlanet(selectedPlanetIndex)
                }
            },
            update = { view ->
                view.setSelectedPlanet(selectedPlanetIndex)
            },
            modifier = Modifier.fillMaxSize()
        )
    }

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
            .background(color = Color.Transparent),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        Button(onClick = {
            selectedPlanetIndex =
                if (selectedPlanetIndex - 1 < 0) planetCount - 1 else selectedPlanetIndex - 1
        }) {
            Text("Влево")
        }

        Button(onClick = {
            navController.navigate("moon_info/$selectedPlanetIndex")
        }) {
            Text("Информация")
        }

        Button(onClick = {
            selectedPlanetIndex = (selectedPlanetIndex + 1) % planetCount
        }) {
            Text("Вправо")
        }
    }
}
