package com.example

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import com.example.activation.ActivationGate
import com.example.activation.ActivationViewModel
import com.example.ui.ControllerViewModel
import com.example.ui.theme.MyApplicationTheme

class MainActivity : ComponentActivity() {
    private val controllerViewModel: ControllerViewModel by viewModels()
    private val activationViewModel: ActivationViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            MyApplicationTheme {
                ActivationGate(
                    activationViewModel = activationViewModel,
                    controllerViewModel = controllerViewModel
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()
        // Check activation and expiry status every time the app returns to foreground
        activationViewModel.checkStatusOnResume()
    }
}
