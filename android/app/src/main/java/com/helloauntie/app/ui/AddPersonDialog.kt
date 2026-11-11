package com.helloauntie.app.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import com.helloauntie.app.vm.AuntieViewModel
import com.helloauntie.core.kinship.RelationPreset
import com.helloauntie.core.kinship.RelationPresets
import com.helloauntie.core.model.Gender

@Composable
fun AddPersonDialog(
    viewModel: AuntieViewModel,
    onDismiss: () -> Unit,
) {
    var name by remember { mutableStateOf("") }
    var gender by remember { mutableStateOf(Gender.UNKNOWN) }
    var birthYear by remember { mutableStateOf("") }
    var birthOrder by remember { mutableStateOf("") }
    var preset by remember { mutableStateOf(RelationPresets.all.first()) }
    var expanded by remember { mutableStateOf(false) }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("添加亲戚") },
        text = {
            Column(
                modifier = Modifier.verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                OutlinedTextField(
                    value = name,
                    onValueChange = { name = it },
                    label = { Text("姓名 / 叫法") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth(),
                )

                Text("与我的关系", style = MaterialTheme.typography.labelLarge)
                Box {
                    OutlinedButton(
                        onClick = { expanded = true },
                        modifier = Modifier.fillMaxWidth(),
                    ) {
                        Text("${preset.label}  ▾")
                    }
                    DropdownMenu(
                        expanded = expanded,
                        onDismissRequest = { expanded = false },
                    ) {
                        RelationPresets.all.forEach { option ->
                            DropdownMenuItem(
                                text = { Text(option.label) },
                                onClick = {
                                    preset = option
                                    gender = option.steps.last().gender
                                    expanded = false
                                },
                            )
                        }
                    }
                }
                Text(
                    "关系链决定称呼。例如选“爸爸的哥哥”，App 会自动建立“爸爸”和“伯父”两个节点。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )

                Text("性别", style = MaterialTheme.typography.labelLarge)
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    GenderChip("男", gender == Gender.MALE) { gender = Gender.MALE }
                    GenderChip("女", gender == Gender.FEMALE) { gender = Gender.FEMALE }
                    GenderChip("未知", gender == Gender.UNKNOWN) { gender = Gender.UNKNOWN }
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    OutlinedTextField(
                        value = birthYear,
                        onValueChange = { birthYear = it.filter(Char::isDigit).take(4) },
                        label = { Text("出生年（可选）") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                    OutlinedTextField(
                        value = birthOrder,
                        onValueChange = { birthOrder = it.filter(Char::isDigit).take(2) },
                        label = { Text("排行（可选）") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        singleLine = true,
                        modifier = Modifier.weight(1f),
                    )
                }
                Text(
                    "排行用于区分大伯/二伯、大姨/二姨；出生年用于判断堂哥/堂弟。",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    viewModel.addPerson(
                        name = name,
                        gender = gender,
                        birthYear = birthYear.toIntOrNull(),
                        birthOrder = birthOrder.toIntOrNull(),
                        preset = preset,
                    )
                    onDismiss()
                },
                enabled = name.isNotBlank(),
            ) {
                Text("保存")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("取消")
            }
        },
    )
}

@Composable
private fun GenderChip(
    label: String,
    selected: Boolean,
    onClick: () -> Unit,
) {
    FilterChip(
        selected = selected,
        onClick = onClick,
        label = { Text(label) },
    )
}
