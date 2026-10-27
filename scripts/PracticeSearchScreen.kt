package ru.tishe.app

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Fast searchable directory; only visible cards are composed. */
@Composable
internal fun PracticeSearchScreen(modifier: Modifier, onStart: (String, Emotion) -> Unit) {
    var search by rememberSaveable { mutableStateOf("") }
    var emotionId by rememberSaveable { mutableStateOf("") }
    var shortOnly by rememberSaveable { mutableStateOf(false) }
    val emotion = Emotion.fromId(emotionId)
    val results = remember(search, emotion, shortOnly) { PracticeIndex.search(search, emotion, shortOnly) }
    val listState = rememberLazyListState()
    LaunchedEffect(search, emotionId, shortOnly) { listState.scrollToItem(0) }

    LazyColumn(
        state = listState,
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(horizontal = 18.dp, vertical = 20.dp),
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        item {
            SectionHeading("Без спешки", "Практики и мини-игры",
                "Найди занятие по чувству или названию. Можно пропустить любое упражнение.")
        }
        item {
            OutlinedTextField(
                value = search, onValueChange = { search = it.take(80) },
                modifier = Modifier.fillMaxWidth(), singleLine = true,
                label = { Text("Поиск упражнения") },
                placeholder = { Text("Гнев, отдых, границы…") },
                trailingIcon = {
                    if (search.isNotEmpty()) TextButton(onClick = { search = "" }) {
                        Text("Очистить")
                    }
                }
            )
        }
        item {
            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.horizontalScroll(rememberScrollState())
            ) {
                FilterChip(selected = emotion == null, onClick = { emotionId = "" }, label = { Text("Все") })
                Emotion.entries.forEach { e ->
                    FilterChip(selected = emotion == e, onClick = { emotionId = e.id },
                        label = { Text(e.icon + " " + e.title) })
                }
            }
        }
        item {
            FilterChip(selected = shortOnly, onClick = { shortOnly = !shortOnly },
                label = { Text("Простые короткие практики") })
            Text("Найдено: " + results.size + ". Никаких баллов и обязательного результата.",
                color = Muted, fontSize = 12.sp)
        }
        if (results.isEmpty()) {
            item {
                SoftCard(highlight = true) {
                    Text("Ничего не нашлось", fontWeight = FontWeight.Bold)
                    Spacer(Modifier.height(6.dp))
                    Text("Можно убрать один из фильтров и выбрать что-нибудь другое.", color = Muted)
                    Spacer(Modifier.height(12.dp))
                    OutlinedButton(onClick = { search = ""; emotionId = ""; shortOnly = false }) {
                        Text("Показать все практики")
                    }
                }
            }
        }
        items(results, key = { it.route }) { option ->
            Surface(
                modifier = Modifier.fillMaxWidth().clickable(role = Role.Button) {
                    onStart(option.route, option.emotion)
                },
                shape = RoundedCornerShape(22.dp), color = Panel,
                border = BorderStroke(1.dp, PanelAlt)
            ) {
                Column(Modifier.padding(17.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    Text(option.title, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                    Text(option.description, color = Muted, fontSize = 13.sp)
                    Text("Начать →", color = Accent, fontWeight = FontWeight.SemiBold, fontSize = 13.sp)
                }
            }
        }
        item { Spacer(Modifier.height(12.dp)) }
    }
}
