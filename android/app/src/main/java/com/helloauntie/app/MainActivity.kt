package com.helloauntie.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.helloauntie.app.ui.AppRoot
import com.helloauntie.app.ui.theme.HelloAuntieTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            HelloAuntieTheme {
                AppRoot()
            }
        }
    }
}
