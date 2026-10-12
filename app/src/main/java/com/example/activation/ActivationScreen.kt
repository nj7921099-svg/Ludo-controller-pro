package com.example.activation

import android.app.Activity
import android.view.HapticFeedbackConstants
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
import androidx.compose.animation.slideInHorizontally
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutHorizontally
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.DateRange
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Shield
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.ui.theme.AccentGlowPrimary
import com.example.ui.theme.AccentGlowSecondary
import com.example.ui.theme.CyberBorderBright
import com.example.ui.theme.CyberBorderSubtle
import com.example.ui.theme.CyberButtonBg
import com.example.ui.theme.CyberCardBorder
import com.example.ui.theme.NeonAmber
import com.example.ui.theme.NeonCrimson
import com.example.ui.theme.PitchBlack
import com.example.ui.theme.SurfaceCard
import com.example.ui.theme.SurfaceCardElevated
import com.example.ui.theme.TextDim
import com.example.ui.theme.TextMuted
import com.example.ui.theme.TextWhite
import kotlinx.coroutines.launch

/**
 * Root activation view managing the transition between the Password screen
 * and the Duration Selection screen.
 */
@Composable
fun ActivationScreen(
    viewModel: ActivationViewModel,
    isExpired: Boolean,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val activity = context as? Activity
    val screenStep by viewModel.screenStep.collectAsState()

    // Enforce back protection: user cannot back into the un-activated Controller Home
    BackHandler(enabled = true) {
        if (screenStep == ActivationScreenStep.DURATION_SELECTION) {
            viewModel.backToPassword()
        } else {
            // Minimize or finish activity so app cannot be bypassed
            activity?.finish()
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(PitchBlack)
    ) {
        AnimatedContent(
            targetState = screenStep,
            transitionSpec = {
                if (targetState == ActivationScreenStep.DURATION_SELECTION) {
                    (slideInHorizontally { it } + fadeIn(tween(300))) togetherWith
                            (slideOutHorizontally { -it } + fadeOut(tween(300)))
                } else {
                    (slideInHorizontally { -it } + fadeIn(tween(300))) togetherWith
                            (slideOutHorizontally { it } + fadeOut(tween(300)))
                }
            },
            label = "activation_step_transition"
        ) { step ->
            when (step) {
                ActivationScreenStep.PASSWORD_REQUIRED -> {
                    PasswordActivationContent(
                        viewModel = viewModel,
                        isExpired = isExpired
                    )
                }

                ActivationScreenStep.DURATION_SELECTION -> {
                    DurationSelectionContent(
                        viewModel = viewModel,
                        onBack = { viewModel.backToPassword() }
                    )
                }
            }
        }
    }
}

/**
 * Password verification screen with app branding, password field, show/hide,
 * error notification, and unlock action.
 */
@Composable
private fun PasswordActivationContent(
    viewModel: ActivationViewModel,
    isExpired: Boolean
) {
    val view = LocalView.current
    val passwordInput by viewModel.passwordInput.collectAsState()
    val passwordVisible by viewModel.passwordVisible.collectAsState()
    val passwordError by viewModel.passwordError.collectAsState()
    val scrollState = rememberScrollState()

    val safePadding = WindowInsets.safeDrawing.asPaddingValues()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(
                top = safePadding.calculateTopPadding() + 24.dp,
                bottom = safePadding.calculateBottomPadding() + 24.dp,
                start = 24.dp,
                end = 24.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Spacer(modifier = Modifier.height(16.dp))

            // App branding with Neon Dice Pad icon
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .size(96.dp)
                    .clip(RoundedCornerShape(26.dp))
                    .background(
                        Brush.radialGradient(
                            colors = listOf(
                                AccentGlowPrimary.copy(alpha = 0.25f),
                                Color(0xFF0F1424)
                            )
                        )
                    )
                    .border(
                        BorderStroke(1.5.dp, AccentGlowPrimary.copy(alpha = 0.8f)),
                        RoundedCornerShape(26.dp)
                    )
                    .shadow(16.dp, RoundedCornerShape(26.dp), spotColor = AccentGlowPrimary)
            ) {
                Image(
                    painter = painterResource(id = R.drawable.app_icon),
                    contentDescription = "Application Icon",
                    modifier = Modifier
                        .size(76.dp)
                        .clip(RoundedCornerShape(20.dp))
                )
            }

            Spacer(modifier = Modifier.height(20.dp))

            Text(
                text = "LUDO CONTROLLER",
                fontSize = 22.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp,
                color = TextWhite,
                fontFamily = FontFamily.Monospace
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Security badge
            Surface(
                color = SurfaceCardElevated,
                shape = RoundedCornerShape(12.dp),
                border = BorderStroke(1.dp, CyberCardBorder)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 6.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Shield,
                        contentDescription = null,
                        tint = AccentGlowPrimary,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = "SECURE ACCESS GATEWAY",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = AccentGlowPrimary,
                        letterSpacing = 1.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Expired Alert banner if applicable
            AnimatedVisibility(
                visible = isExpired,
                enter = fadeIn() + slideInVertically(),
                exit = fadeOut() + slideOutVertically()
            ) {
                Card(
                    colors = CardDefaults.cardColors(containerColor = Color(0xFF261014)),
                    shape = RoundedCornerShape(14.dp),
                    border = BorderStroke(1.dp, NeonCrimson.copy(alpha = 0.7f)),
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 16.dp)
                ) {
                    Row(
                        modifier = Modifier.padding(14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Warning,
                            contentDescription = null,
                            tint = NeonCrimson,
                            modifier = Modifier.size(24.dp)
                        )
                        Spacer(modifier = Modifier.width(12.dp))
                        Column {
                            Text(
                                text = "License Expired",
                                color = TextWhite,
                                fontWeight = FontWeight.Bold,
                                fontSize = 14.sp
                            )
                            Text(
                                text = "Previous duration ended. Please enter password to set a new activation period.",
                                color = TextMuted,
                                fontSize = 12.sp,
                                lineHeight = 16.sp
                            )
                        }
                    }
                }
            }

            // Password Input Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCardElevated),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.dp, CyberCardBorder),
                modifier = Modifier.fillMaxWidth()
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Text(
                        text = "ENTER ACCESS KEY",
                        color = AccentGlowPrimary,
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.2.sp
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = "Authorized activation password is required to access controller functionality.",
                        color = TextMuted,
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    OutlinedTextField(
                        value = passwordInput,
                        onValueChange = { viewModel.onPasswordChanged(it) },
                        modifier = Modifier.fillMaxWidth(),
                        singleLine = true,
                        placeholder = {
                            Text(
                                text = "Enter 8-digit password",
                                color = TextDim,
                                fontSize = 14.sp
                            )
                        },
                        leadingIcon = {
                            Icon(
                                imageVector = Icons.Default.Lock,
                                contentDescription = null,
                                tint = if (passwordError != null) NeonCrimson else AccentGlowPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        },
                        trailingIcon = {
                            IconButton(
                                onClick = {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    viewModel.togglePasswordVisibility()
                                }
                            ) {
                                Icon(
                                    imageVector = if (passwordVisible) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                                    contentDescription = if (passwordVisible) "Hide password" else "Show password",
                                    tint = TextMuted,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                        },
                        visualTransformation = if (passwordVisible) VisualTransformation.None else PasswordVisualTransformation(),
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Password,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                                viewModel.submitPassword()
                            }
                        ),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedBorderColor = AccentGlowPrimary,
                            unfocusedBorderColor = CyberBorderSubtle,
                            errorBorderColor = NeonCrimson,
                            focusedTextColor = TextWhite,
                            unfocusedTextColor = TextWhite,
                            cursorColor = AccentGlowPrimary,
                            focusedContainerColor = CyberButtonBg,
                            unfocusedContainerColor = CyberButtonBg
                        ),
                        isError = passwordError != null,
                        shape = RoundedCornerShape(14.dp)
                    )

                    // Error feedback
                    AnimatedVisibility(
                        visible = passwordError != null,
                        enter = fadeIn() + slideInVertically(),
                        exit = fadeOut() + slideOutVertically()
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(top = 10.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Warning,
                                contentDescription = null,
                                tint = NeonCrimson,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = passwordError ?: "",
                                color = NeonCrimson,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Unlock Button
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            Button(
                onClick = {
                    view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                    viewModel.submitPassword()
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(54.dp)
                    .shadow(12.dp, RoundedCornerShape(16.dp), spotColor = AccentGlowPrimary),
                colors = ButtonDefaults.buttonColors(
                    containerColor = AccentGlowPrimary
                ),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "UNLOCK ACCESS",
                        color = Color(0xFF04060C),
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        letterSpacing = 1.2.sp
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = null,
                        tint = Color(0xFF04060C),
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Text(
                text = "Local Hardware-Tied Controller Lock",
                color = TextDim,
                fontSize = 11.sp,
                letterSpacing = 0.5.sp
            )
        }
    }
}

/**
 * Duration Selection Screen allowing independent configuration of:
 * - YEAR (0 to 10)
 * - MONTH (0 to 12)
 * - DAYS (1 to 31)
 *
 * Shows real-time duration summary and calculated expiration date/time.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DurationSelectionContent(
    viewModel: ActivationViewModel,
    onBack: () -> Unit
) {
    val view = LocalView.current
    val years by viewModel.selectedYears.collectAsState()
    val months by viewModel.selectedMonths.collectAsState()
    val days by viewModel.selectedDays.collectAsState()
    val summaryText by viewModel.summaryText.collectAsState()
    val previewExpiry by viewModel.previewExpiryFormatted.collectAsState()
    val isActivating by viewModel.isActivating.collectAsState()

    val scrollState = rememberScrollState()
    val safePadding = WindowInsets.safeDrawing.asPaddingValues()

    // Quick selection sheet state
    var quickSelectType by remember { mutableStateOf<String?>(null) }
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    val coroutineScope = rememberCoroutineScope()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .verticalScroll(scrollState)
            .padding(
                top = safePadding.calculateTopPadding() + 16.dp,
                bottom = safePadding.calculateBottomPadding() + 24.dp,
                start = 20.dp,
                end = 20.dp
            ),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.SpaceBetween
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Header with Back button and Titles
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        onBack()
                    },
                    modifier = Modifier
                        .size(44.dp)
                        .background(SurfaceCardElevated, CircleShape)
                        .border(1.dp, CyberCardBorder, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        tint = TextWhite,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Spacer(modifier = Modifier.width(14.dp))

                Column {
                    Text(
                        text = "Set App Duration",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextWhite,
                        letterSpacing = 0.5.sp
                    )
                    Text(
                        text = "Select how long this app should remain active.",
                        fontSize = 13.sp,
                        color = TextMuted,
                        lineHeight = 17.sp
                    )
                }
            }

            Spacer(modifier = Modifier.height(26.dp))

            // Three separate selectable containers: YEAR, MONTH, DAYS
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                DurationPickerCard(
                    title = "YEAR",
                    value = years,
                    range = "0 - 10",
                    unitLabel = if (years == 1) "yr" else "yrs",
                    accentColor = AccentGlowPrimary,
                    onDecrement = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        viewModel.onYearChanged(years - 1)
                    },
                    onIncrement = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        viewModel.onYearChanged(years + 1)
                    },
                    onCardTap = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        quickSelectType = "YEAR"
                    },
                    modifier = Modifier.weight(1f)
                )

                DurationPickerCard(
                    title = "MONTH",
                    value = months,
                    range = "0 - 12",
                    unitLabel = if (months == 1) "mo" else "mos",
                    accentColor = AccentGlowSecondary,
                    onDecrement = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        viewModel.onMonthChanged(months - 1)
                    },
                    onIncrement = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        viewModel.onMonthChanged(months + 1)
                    },
                    onCardTap = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        quickSelectType = "MONTH"
                    },
                    modifier = Modifier.weight(1f)
                )

                DurationPickerCard(
                    title = "DAYS",
                    value = days,
                    range = "1 - 31",
                    unitLabel = if (days == 1) "day" else "days",
                    accentColor = NeonAmber,
                    onDecrement = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        viewModel.onDayChanged(days - 1)
                    },
                    onIncrement = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        viewModel.onDayChanged(days + 1)
                    },
                    onCardTap = {
                        view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                        quickSelectType = "DAYS"
                    },
                    modifier = Modifier.weight(1f)
                )
            }

            Spacer(modifier = Modifier.height(24.dp))

            // Duration Summary Card
            Card(
                colors = CardDefaults.cardColors(containerColor = SurfaceCardElevated),
                shape = RoundedCornerShape(20.dp),
                border = BorderStroke(1.2.dp, AccentGlowPrimary.copy(alpha = 0.5f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .shadow(12.dp, RoundedCornerShape(20.dp), spotColor = AccentGlowPrimary.copy(alpha = 0.2f))
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp)
                ) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(36.dp)
                                .background(AccentGlowPrimary.copy(alpha = 0.15f), CircleShape)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Timer,
                                contentDescription = null,
                                tint = AccentGlowPrimary,
                                modifier = Modifier.size(20.dp)
                            )
                        }

                        Spacer(modifier = Modifier.width(12.dp))

                        Column {
                            Text(
                                text = "SELECTED DURATION",
                                color = AccentGlowPrimary,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 1.2.sp
                            )
                            Text(
                                text = summaryText,
                                color = TextWhite,
                                fontSize = 16.sp,
                                fontWeight = FontWeight.ExtraBold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(CyberButtonBg, RoundedCornerShape(12.dp))
                            .border(1.dp, CyberBorderSubtle, RoundedCornerShape(12.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Row(
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.DateRange,
                                contentDescription = null,
                                tint = TextMuted,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Expires on: ",
                                color = TextMuted,
                                fontSize = 12.sp
                            )
                            Text(
                                text = previewExpiry,
                                color = TextWhite,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Quick Info Note
            Surface(
                color = SurfaceCard,
                shape = RoundedCornerShape(14.dp),
                border = BorderStroke(1.dp, CyberBorderSubtle),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(14.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = AccentGlowPrimary,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(modifier = Modifier.width(10.dp))
                    Text(
                        text = "Duration calculation uses calendar arithmetic and persists across restarts and reboots.",
                        color = TextMuted,
                        fontSize = 11.sp,
                        lineHeight = 15.sp
                    )
                }
            }
        }

        Spacer(modifier = Modifier.height(24.dp))

        // Activate Button
        Button(
            onClick = {
                view.performHapticFeedback(HapticFeedbackConstants.CONFIRM)
                viewModel.activate()
            },
            enabled = !isActivating && (years > 0 || months > 0 || days > 0),
            modifier = Modifier
                .fillMaxWidth()
                .height(56.dp)
                .shadow(16.dp, RoundedCornerShape(18.dp), spotColor = AccentGlowPrimary),
            colors = ButtonDefaults.buttonColors(
                containerColor = AccentGlowPrimary,
                disabledContainerColor = SurfaceCardElevated
            ),
            shape = RoundedCornerShape(18.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = "ACTIVATE",
                    color = Color(0xFF04060C),
                    fontWeight = FontWeight.Black,
                    fontSize = 16.sp,
                    letterSpacing = 1.5.sp
                )
                Spacer(modifier = Modifier.width(8.dp))
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = Color(0xFF04060C),
                    modifier = Modifier.size(20.dp)
                )
            }
        }
    }

    // Modal quick-selector bottom sheet if user taps a picker card
    quickSelectType?.let { type ->
        val range = when (type) {
            "YEAR" -> (0..10).toList()
            "MONTH" -> (0..12).toList()
            else -> (1..31).toList()
        }
        val currentValue = when (type) {
            "YEAR" -> years
            "MONTH" -> months
            else -> days
        }

        ModalBottomSheet(
            onDismissRequest = { quickSelectType = null },
            sheetState = sheetState,
            containerColor = SurfaceCardElevated,
            contentColor = TextWhite
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "Select $type",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold,
                    color = AccentGlowPrimary
                )
                Spacer(modifier = Modifier.height(16.dp))

                LazyRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 4.dp, vertical = 8.dp)
                ) {
                    items(range) { num ->
                        val isSelected = num == currentValue
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(54.dp)
                                .clip(RoundedCornerShape(14.dp))
                                .background(if (isSelected) AccentGlowPrimary else CyberButtonBg)
                                .border(
                                    BorderStroke(
                                        1.dp,
                                        if (isSelected) AccentGlowPrimary else CyberBorderBright
                                    ),
                                    RoundedCornerShape(14.dp)
                                )
                                .clickable {
                                    view.performHapticFeedback(HapticFeedbackConstants.CLOCK_TICK)
                                    when (type) {
                                        "YEAR" -> viewModel.onYearChanged(num)
                                        "MONTH" -> viewModel.onMonthChanged(num)
                                        "DAYS" -> viewModel.onDayChanged(num)
                                    }
                                    coroutineScope.launch {
                                        sheetState.hide()
                                        quickSelectType = null
                                    }
                                }
                        ) {
                            Text(
                                text = num.toString(),
                                fontSize = 18.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isSelected) Color(0xFF04060C) else TextWhite
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(20.dp))
            }
        }
    }
}

/**
 * Modern touch-friendly picker container for an individual time unit (YEAR, MONTH, DAYS).
 */
@Composable
private fun DurationPickerCard(
    title: String,
    value: Int,
    range: String,
    unitLabel: String,
    accentColor: Color,
    onDecrement: () -> Unit,
    onIncrement: () -> Unit,
    onCardTap: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        colors = CardDefaults.cardColors(containerColor = SurfaceCardElevated),
        shape = RoundedCornerShape(18.dp),
        border = BorderStroke(1.dp, CyberCardBorder),
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Label
            Text(
                text = title,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold,
                color = accentColor,
                letterSpacing = 1.sp
            )

            Spacer(modifier = Modifier.height(6.dp))

            // Value Display (tappable for quick sheet)
            Box(
                contentAlignment = Alignment.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) { onCardTap() }
                    .padding(vertical = 4.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.Bottom,
                    horizontalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = value.toString(),
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Black,
                        color = TextWhite,
                        fontFamily = FontFamily.Monospace
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = unitLabel,
                        fontSize = 11.sp,
                        color = TextMuted,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Stepper controls: [-] and [+]
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceEvenly,
                verticalAlignment = Alignment.CenterVertically
            ) {
                IconButton(
                    onClick = onDecrement,
                    modifier = Modifier
                        .size(38.dp)
                        .background(CyberButtonBg, CircleShape)
                        .border(1.dp, CyberBorderSubtle, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Remove,
                        contentDescription = "Decrease $title",
                        tint = TextWhite,
                        modifier = Modifier.size(16.dp)
                    )
                }

                IconButton(
                    onClick = onIncrement,
                    modifier = Modifier
                        .size(38.dp)
                        .background(CyberButtonBg, CircleShape)
                        .border(1.dp, CyberBorderSubtle, CircleShape)
                ) {
                    Icon(
                        imageVector = Icons.Default.Add,
                        contentDescription = "Increase $title",
                        tint = accentColor,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = range,
                fontSize = 9.sp,
                color = TextDim
            )
        }
    }
}
