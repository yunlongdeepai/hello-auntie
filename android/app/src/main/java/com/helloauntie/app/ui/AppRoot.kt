package com.helloauntie.app.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import com.helloauntie.app.AppContainer
import com.helloauntie.app.vm.AuntieViewModel

@Composable
fun AppRoot(
    vm: AuntieViewModel = viewModel(),
) {
    var tab by rememberSaveable { mutableIntStateOf(0) }
    var showAdd by rememberSaveable { mutableStateOf(false) }

    Scaffold(
        bottomBar = {
            NavigationBar {
                NavigationBarItem(
                    selected = tab == 0,
                    onClick = { tab = 0 },
                    icon = { Text("相") },
                    label = { Text("相机") },
                )
                NavigationBarItem(
                    selected = tab == 1,
                    onClick = { tab = 1 },
                    icon = { Text("亲") },
                    label = { Text("亲戚") },
                )
                NavigationBarItem(
                    selected = tab == 2,
                    onClick = { tab = 2 },
                    icon = { Text("设") },
                    label = { Text("设置") },
                )
            }
        },
    ) { padding ->
        Box(Modifier.fillMaxSize().padding(padding)) {
            when (tab) {
                0 -> CameraScreen(
                    engine = AppContainer.analysisEngine,
                    viewModel = vm,
                    onAddPerson = { showAdd = true },
                )
                1 -> PeopleScreen(
                    viewModel = vm,
                    onAddPerson = { showAdd = true },
                )
                else -> SettingsScreen(vm)
            }
        }
    }

    if (showAdd) {
        AddPersonDialog(
            viewModel = vm,
            onDismiss = { showAdd = false },
        )
    }
}
