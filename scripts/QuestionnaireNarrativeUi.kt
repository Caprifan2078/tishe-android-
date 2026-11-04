package ru.tishe.app

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Human-readable evidence accompanies every interpretation: not just a radar chart. */
@Composable
internal fun QuestionnaireNarrativeCard(result: QuestionnaireNarrative) {
    SoftCard(highlight = true) {
        Text(result.headline, fontWeight = FontWeight.Bold, fontSize = 19.sp)
        Spacer(Modifier.height(8.dp))
        Text(result.overview, fontSize = 14.sp, lineHeight = 21.sp)

        if (result.evidence.isNotEmpty()) {
            Spacer(Modifier.height(14.dp))
            Text("Почему появился такой вывод", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(7.dp))
            result.evidence.forEach { item ->
                Text(item.title, fontWeight = FontWeight.SemiBold, color = Mint, fontSize = 14.sp)
                Text(item.observation, fontSize = 13.sp)
                if (item.source.isNotBlank()) {
                    Text("Из твоих ответов: ${item.source}", color = Muted, fontSize = 12.sp)
                }
                Spacer(Modifier.height(9.dp))
            }
        }
        if (result.additionalContext.isNotEmpty()) {
            Text("Другие замеченные темы и опоры", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Spacer(Modifier.height(7.dp))
            result.additionalContext.forEach {
                Text("• $it", fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
            }
        }
        if (result.gentleSteps.isNotEmpty()) {
            Spacer(Modifier.height(7.dp))
            Text("Что можно попробовать", fontWeight = FontWeight.SemiBold, fontSize = 15.sp)
            Text("Это идеи, а не обязательные задания.", color = Muted, fontSize = 12.sp)
            Spacer(Modifier.height(5.dp))
            result.gentleSteps.forEach {
                Text("• $it", fontSize = 13.sp)
                Spacer(Modifier.height(6.dp))
            }
        }
        if (result.limitations.isNotEmpty()) {
            Spacer(Modifier.height(9.dp))
            Text("Что важно учитывать", fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Spacer(Modifier.height(5.dp))
            result.limitations.forEach {
                Text("• $it", color = Muted, fontSize = 12.sp)
                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
