package com.example.myapplication.ui.theme

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.Icon
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import kotlinx.coroutines.delay

@Composable
internal fun NewsScreen(onClose: NavHostController) {
    val contentViewModel: ContentViewModel = viewModel()
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Разделение экрана на 4 части
            Row(modifier = Modifier.weight(1f)) {
                // Левая верхняя четверть
                ChangingContentBox(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color.LightGray),
                    viewModel = contentViewModel,
                    index = 0
                )
                // Правая верхняя четверть
                ChangingContentBox(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color.LightGray),
                    viewModel = contentViewModel,
                    index = 1
                )
            }
            Row(modifier = Modifier.weight(1f)) {
                // Левая нижняя четверть
                ChangingContentBox(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color.LightGray),
                    viewModel = contentViewModel,
                    index = 2
                )
                // Правая нижняя четверть
                ChangingContentBox(
                    modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                        .background(Color.LightGray),
                    viewModel = contentViewModel,
                    index = 3
                )
            }
        }

        // Кнопка закрытия (крестик)
        Icon(
            imageVector = Icons.Default.Close,
            contentDescription = "Close",
            modifier = Modifier
                .align(Alignment.TopEnd)
                .clickable { onClose.navigate("opengl") }
                .size(48.dp)
                .padding(8.dp),
            tint = Color.Red
        )
    }
}

@Composable
fun ChangingContentBox(
    modifier: Modifier = Modifier,
    viewModel: ContentViewModel,
    index: Int
) {
    val contentState by viewModel.getContentAndLikes(index).collectAsState()

    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(8.dp)
    ) {
        // Верхняя часть - новость (90%)
        Box(
            modifier = Modifier
                .weight(9f)
                .fillMaxWidth()
                .background(Color.White)
                .padding(8.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(text = contentState.text, fontSize = 16.sp, color = Color.Black)
        }

        // Нижняя часть - лайки (10%)
        Box(
            modifier = Modifier
                .weight(1f)
                .fillMaxWidth()
                .background(Color.Gray)
                .clickable { viewModel.incrementLikesForCurrentContent(index) },
            contentAlignment = Alignment.Center
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(imageVector = Icons.Default.Favorite, contentDescription = "Like", tint = Color.Red)
                Spacer(modifier = Modifier.width(8.dp))
                Text(text = "${contentState.likes}", fontSize = 18.sp, color = Color.Black)
            }
        }
    }
}
