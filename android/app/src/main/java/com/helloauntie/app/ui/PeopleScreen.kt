package com.helloauntie.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.helloauntie.app.vm.AuntieViewModel
import com.helloauntie.app.vm.PersonWithTerm

@Composable
fun PeopleScreen(
    viewModel: AuntieViewModel,
    onAddPerson: () -> Unit,
) {
    val people by viewModel.people.collectAsStateWithLifecycle()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Text("亲戚", style = MaterialTheme.typography.headlineSmall)
            Spacer(Modifier.weight(1f))
            Button(onClick = onAddPerson) {
                Text("添加亲戚")
            }
        }
        Text(
            text = "按“我”的关系链计算称呼；同一个人对不同的人称呼不同。",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 4.dp, bottom = 10.dp),
        )

        if (people.isEmpty()) {
            Text(
                "还没有录入亲戚。点右上角“添加亲戚”，先录入最常叫错的几位。",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 24.dp),
            )
        } else {
            LazyColumn(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                items(people, key = { it.person.id }) { item ->
                    PersonCard(
                        item = item,
                        onDelete = { viewModel.deletePerson(item.person.id) },
                    )
                }
            }
        }
    }
}

@Composable
private fun PersonCard(
    item: PersonWithTerm,
    onDelete: () -> Unit,
) {
    Card(modifier = Modifier.fillMaxWidth()) {
        Column(modifier = Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = item.person.name,
                    style = MaterialTheme.typography.titleMedium,
                )
                Spacer(Modifier.width(10.dp))
                Surface(
                    color = MaterialTheme.colorScheme.primaryContainer,
                    shape = RoundedCornerShape(8.dp),
                ) {
                    Text(
                        text = item.term.primary,
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onPrimaryContainer,
                        modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp),
                    )
                }
                Spacer(Modifier.weight(1f))
                if (!item.person.isSelf) {
                    TextButton(onClick = onDelete) {
                        Text("删除", color = Color(0xFFB03A3A))
                    }
                }
            }

            if (item.term.selector.isNotBlank()) {
                Text(
                    text = if (item.term.selector == "非亲属常规称呼") {
                        "关系：非亲属常规称呼"
                    } else {
                        "关系链：${item.term.selector}"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
            Text(
                text = if (item.faceCount > 0) {
                    "人脸样本：${item.faceCount} 张"
                } else {
                    "人脸样本：未采集（可在相机页点“录入当前人脸”）"
                },
                style = MaterialTheme.typography.bodySmall,
                color = if (item.faceCount > 0) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 2.dp),
            )
            if (item.term.alternatives.isNotEmpty()) {
                Text(
                    text = "也可叫：${item.term.alternatives.take(3).joinToString("、")}",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
            if (!item.term.known) {
                Text(
                    text = item.term.explanation.ifBlank { "关系未连接" },
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF9A6A1E),
                    modifier = Modifier.padding(top = 2.dp),
                )
            }
        }
    }
}
