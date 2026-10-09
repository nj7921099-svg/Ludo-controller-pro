package com.example.activation

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.ui.ControllerScreen
import com.example.ui.ControllerViewModel
import com.example.ui.theme.PitchBlack

/**
 * Access gate coordinating access to the existing Controller Screen.
 *
 * Checks persistent local activation status before allowing access.
 * While license is active: Renders existing ControllerScreen without changes.
 * When license is expired or not activated: Renders ActivationScreen mandatory gate.
 */
@Composable
fun ActivationGate(
    activationViewModel: ActivationViewModel,
    controllerViewModel: ControllerViewModel,
    modifier: Modifier = Modifier
) {
    val licenseState by activationViewModel.licenseState.collectAsState()

    // When locked or expired, disconnect background socket so no traffic flows while locked
    LaunchedEffect(licenseState) {
        if (licenseState !is LicenseState.Active) {
            controllerViewModel.disconnect()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack)
    ) {
        Crossfade(
            targetState = licenseState is LicenseState.Active,
            animationSpec = tween(400),
            label = "activation_gate_crossfade"
        ) { isAccessGranted ->
            if (isAccessGranted) {
                // Existing Controller Home screen - completely untouched functionality
                ControllerScreen(viewModel = controllerViewModel)
            } else {
                // Mandatory access gate
                ActivationScreen(
                    viewModel = activationViewModel,
                    isExpired = licenseState is LicenseState.Expired
                )
            }
        }
    }
}
