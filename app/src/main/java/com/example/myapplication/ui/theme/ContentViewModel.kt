package com.example.myapplication.ui.theme

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class ContentViewModel : ViewModel() {

    private val _contentList = List(4) { MutableStateFlow(ContentState(NEWS_LIST[it], 0)) }
    private val contentList = _contentList.map { it.asStateFlow() }

    private val textsAndLikesList = mutableListOf(
        mutableListOf(Pair(NEWS_LIST[0], 0), Pair(NEWS_LIST[1], 0), Pair(NEWS_LIST[2], 0)),
        mutableListOf(Pair(NEWS_LIST[3], 0), Pair(NEWS_LIST[4], 0), Pair(NEWS_LIST[5], 0)),
        mutableListOf(Pair(NEWS_LIST[6], 0), Pair(NEWS_LIST[7], 0), Pair(NEWS_LIST[8], 0)),
        mutableListOf(Pair(NEWS_LIST[9], 0), Pair(NEWS_LIST[0], 0), Pair(NEWS_LIST[1], 0))
    )

    init {
        for (i in _contentList.indices) {
            startUpdatingContent(i)
        }
    }

    private fun startUpdatingContent(index: Int) {
        viewModelScope.launch {
            var currentIndex = 0
            while (true) {
                delay(5000L)
                currentIndex = (currentIndex + 1) % textsAndLikesList[index].size
                val (text, likes) = textsAndLikesList[index][currentIndex]
                _contentList[index].value = ContentState(text, likes)
            }
        }
    }

    fun incrementLikesForCurrentContent(index: Int) {
        val currentIndex = getCurrentContentIndex(index)
        val currentPair = textsAndLikesList[index][currentIndex]
        val updatedPair = currentPair.copy(second = currentPair.second + 1)

        textsAndLikesList[index][currentIndex] = updatedPair

        _contentList[index].value = ContentState(updatedPair.first, updatedPair.second)
    }

    fun getContentAndLikes(index: Int): StateFlow<ContentState> {
        return contentList[index]
    }

    private fun getCurrentContentIndex(index: Int): Int {
        val currentText = _contentList[index].value.text
        return textsAndLikesList[index].indexOfFirst { it.first == currentText }
    }
}

data class ContentState(val text: String, val likes: Int)

val NEWS_LIST = listOf(
    "Ученые обнаружили новый вид животных в Амазонии.",
    "Новая технология уменьшит выбросы углекислого газа.",
    "Запуск нового космического корабля состоялся успешно.",
    "Исследование показало пользу от регулярных прогулок.",
    "Важная находка археологов в Древнем Египте.",
    "Роботы начали помогать в домах престарелых.",
    "Открытие революционного лекарства от рака.",
    "Крупнейшее наводнение в Европе за последние 50 лет.",
    "Новые виды зеленой энергетики набирают популярность.",
    "Инженеры создали более эффективные солнечные батареи."
)