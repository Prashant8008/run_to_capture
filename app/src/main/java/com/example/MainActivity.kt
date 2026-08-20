package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.ui.Modifier
import com.example.core.designsystem.Run2CaptureTheme
import com.example.core.di.AppModule
import com.example.feature.navigation.RunNavGraph

class MainActivity : ComponentActivity() {

    private lateinit var appModule: AppModule

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        appModule = AppModule(applicationContext)

        setContent {
            Run2CaptureTheme(darkTheme = false) {
                Surface(
                    modifier = Modifier.fillMaxSize(),
                    color = MaterialTheme.colorScheme.background
                ) {
                    RunNavGraph(appModule = appModule)
                }
            }
        }
    }
}

