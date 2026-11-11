package com.helloauntie.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.helloauntie.app.ml.ModelAssets
import com.helloauntie.app.vm.AuntieViewModel
import com.helloauntie.core.kinship.Region

@Composable
fun SettingsScreen(viewModel: AuntieViewModel) {
    val region by viewModel.region.collectAsStateWithLifecycle()
    val context = LocalContext.current
    val personModel = remember { ModelAssets.exists(context, ModelAssets.PERSON) }
    val faceModel = remember { ModelAssets.exists(context, ModelAssets.FACE) }
    val embedderModel = remember { ModelAssets.exists(context, ModelAssets.EMBEDDER) }
    var confirmClear by remember { mutableStateOf(false) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp),
    ) {
        Text("设置", style = MaterialTheme.typography.headlineSmall)

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("我的出生年", style = MaterialTheme.typography.titleMedium)
                Text(
                    "用于判断堂哥/堂弟、表哥/表弟。只存在本机，可留空。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                val selfYear by viewModel.selfBirthYear.collectAsStateWithLifecycle()
                var yearText by remember { mutableStateOf("") }
                LaunchedEffect(selfYear) {
                    yearText = selfYear?.toString() ?: ""
                }
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = yearText,
                        onValueChange = { yearText = it.filter(Char::isDigit).take(4) },
                        label = { Text("如 2000") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.weight(1f),
                    )
                    Button(
                        onClick = { viewModel.setSelfBirthYear(yearText.toIntOrNull()) },
                    ) {
                        Text("保存")
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                Text("称呼地区", style = MaterialTheme.typography.titleMedium)
                Text(
                    "同一个亲戚，不同地区叫法不同。切换后会重新计算“亲戚”页的称呼。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Region.entries.forEach { item ->
                        FilterChip(
                            selected = region == item,
                            onClick = { viewModel.setRegion(item) },
                            label = { Text(item.label) },
                        )
                    }
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("端侧模型", style = MaterialTheme.typography.titleMedium)
                ModelRow("人体检测 person.tflite", personModel)
                ModelRow("人脸检测（系统 FaceDetector）", true)
                ModelRow("人脸特征 mobileface.tflite", embedderModel)
                Text(
                    if (!personModel) {
                        "未安装 person.tflite：当前使用演示框验证远近切换，不会做真实识别。模型放置方式见 android/MODELS.md。"
                    } else {
                        "人体模型已安装；人脸特征模型${if (embedderModel) "已安装" else "未安装，近距只显示“未知”"}。"
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                if (!faceModel) {
                    Text(
                        "face.tflite 尚未接入；近距人脸框使用系统 FaceDetector 兜底。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
        }

        Card(modifier = Modifier.fillMaxWidth()) {
            Column(
                modifier = Modifier.padding(14.dp),
                verticalArrangement = Arrangement.spacedBy(6.dp),
            ) {
                Text("隐私", style = MaterialTheme.typography.titleMedium)
                Text(
                    "所有计算在本机完成；人脸数据默认只存本机，不上传；可随时删除。MVP 尚未接入 SQLCipher，正式版需补数据库加密。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Text(
                    "适配范围：ColorOS 14–17（Android 14–17）。其他 Android 系统暂不适配。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }

        Button(
            onClick = { confirmClear = true },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("清空亲戚数据")
        }
    }

    if (confirmClear) {
        AlertDialog(
            onDismissRequest = { confirmClear = false },
            title = { Text("清空数据？") },
            text = { Text("会删除所有亲戚、关系和人脸样本，只保留“我”。此操作不可恢复。") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAll()
                        confirmClear = false
                    },
                ) {
                    Text("确认清空")
                }
            },
            dismissButton = {
                TextButton(onClick = { confirmClear = false }) {
                    Text("取消")
                }
            },
        )
    }
}

@Composable
private fun ModelRow(label: String, installed: Boolean) {
    Row(modifier = Modifier.fillMaxWidth()) {
        Text(label, style = MaterialTheme.typography.bodyMedium, modifier = Modifier.weight(1f))
        Text(
            text = if (installed) "已就绪" else "未安装",
            style = MaterialTheme.typography.bodySmall,
            color = if (installed) MaterialTheme.colorScheme.secondary else MaterialTheme.colorScheme.error,
        )
    }
}
