package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import com.example.security.configureAppCheck
import com.example.ui.GhostMalliApp

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Configure Firebase App Check with launch Intent BEFORE any UI, ViewModel, or Firebase call
        configureAppCheck(this, intent)

        enableEdgeToEdge()
        setContent {
            GhostMalliApp()
        }
    }
}
