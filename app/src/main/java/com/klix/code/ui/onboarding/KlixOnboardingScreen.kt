package com.klix.code.ui.onboarding

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutLinearInEasing
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.klix.code.data.sandbox.SandboxManager
import com.klix.code.data.security.KeyManager
import com.klix.code.data.security.KeyManagerImpl
import kotlinx.coroutines.delay

@Composable
fun KlixOnboardingScreen(
    isDark: Boolean,
    sandboxManager: SandboxManager,
    onComplete: () -> Unit,
    modifier: Modifier = Modifier,
    keyManager: KeyManager = KeyManagerImpl(LocalContext.current),
    onboardingViewModel: OnboardingViewModel = viewModel()
) {
    val context = LocalContext.current
    var currentPage by rememberSaveable { mutableIntStateOf(0) }
    var isCtaReady by remember { mutableStateOf(true) }

    // Smooth initial fade-in of onboarding foreground over ambient gradient
    var hasEntered by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) {
        hasEntered = true
    }
    val screenAlpha by animateFloatAsState(
        targetValue = if (hasEntered) 1f else 0f,
        animationSpec = tween(durationMillis = 380, easing = FastOutSlowInEasing),
        label = "onboardingScreenFadeIn"
    )

    // Guard against accidental double-taps on the shared CTA while a page is fading out
    LaunchedEffect(currentPage) {
        isCtaReady = false
        delay(220)
        isCtaReady = true
    }

    val selectedTools by onboardingViewModel.selectedTools.collectAsState()
    val installState by onboardingViewModel.installState.collectAsState()
    val selectedProvider by onboardingViewModel.selectedProvider.collectAsState()
    val apiKeyInput by onboardingViewModel.apiKeyInput.collectAsState()
    val showKeyInputDialog by onboardingViewModel.showKeyInputDialog.collectAsState()

    val isInstalling = installState is SandboxInstallState.DownloadingRootFs ||
        installState is SandboxInstallState.ExtractingRootFs ||
        installState is SandboxInstallState.InstallingPackages

    val goToPage: (Int) -> Unit = { target ->
        val clamped = target.coerceIn(0, 3)
        if (clamped != currentPage) {
            currentPage = clamped
        }
    }

    // Intercept hardware back button: step back through pages when not installing
    BackHandler(enabled = currentPage > 0 && !isInstalling) {
        goToPage(currentPage - 1)
    }

    val swipeThresholdPx = with(LocalDensity.current) { 56.dp.toPx() }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OnboardingColors.backgroundGradient(isDark))
    ) {
        // Shared Minimal Drifting Ambient Background
        DriftingAmbientBackground(isDark = isDark)

        Column(
            modifier = Modifier
                .fillMaxSize()
                .navigationBarsPadding()
                .padding(bottom = 24.dp)
                .graphicsLayer { alpha = screenAlpha }
                .pointerInput(currentPage, isInstalling, showKeyInputDialog) {
                    if (isInstalling || showKeyInputDialog) return@pointerInput
                    var totalDrag = 0f
                    var handled = false
                    detectHorizontalDragGestures(
                        onDragStart = {
                            totalDrag = 0f
                            handled = false
                        },
                        onHorizontalDrag = { change, dragAmount ->
                            change.consume()
                            totalDrag += dragAmount
                            if (!handled) {
                                if (totalDrag <= -swipeThresholdPx && currentPage < 3) {
                                    handled = true
                                    goToPage(currentPage + 1)
                                } else if (totalDrag >= swipeThresholdPx && currentPage > 0) {
                                    handled = true
                                    goToPage(currentPage - 1)
                                }
                            }
                        }
                    )
                },
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Shared Top Bar (Back & Skip smoothly fade in/out as pages change)
            OnboardingTopBar(
                isDark = isDark,
                onBack = if (currentPage > 0 && !isInstalling) {
                    { goToPage(currentPage - 1) }
                } else null,
                onSkip = when {
                    isInstalling -> null
                    currentPage == 0 -> { { goToPage(3) } }
                    currentPage == 1 -> { { goToPage(2) } }
                    currentPage == 2 -> { { goToPage(3) } }
                    else -> null
                }
            )

            // Smooth Fade Out + Fade In across onboarding pages
            AnimatedContent(
                targetState = currentPage,
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f),
                transitionSpec = {
                    fadeIn(
                        animationSpec = tween(
                            durationMillis = 340,
                            delayMillis = 110,
                            easing = FastOutSlowInEasing
                        )
                    ) togetherWith fadeOut(
                        animationSpec = tween(
                            durationMillis = 210,
                            easing = FastOutLinearInEasing
                        )
                    )
                },
                label = "onboardingPageFade"
            ) { page ->
                when (page) {
                    0 -> OnboardingPage1(isDark = isDark)
                    1 -> OnboardingPage2(
                        isDark = isDark,
                        selectedTools = selectedTools,
                        installState = installState,
                        onToggleTool = { tool ->
                            onboardingViewModel.toggleTool(tool)
                        },
                        onRetry = {
                            onboardingViewModel.retryInstallation(
                                context = context,
                                sandboxManager = sandboxManager,
                                onSuccess = { goToPage(2) }
                            )
                        }
                    )
                    2 -> OnboardingPage3(
                        isDark = isDark,
                        selectedProvider = selectedProvider,
                        onSelectProvider = { provider ->
                            onboardingViewModel.selectProvider(provider)
                        }
                    )
                    3 -> OnboardingPage4(isDark = isDark)
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Shared 4-dot Progress Rail with smooth sliding & expanding capsule animation
            OnboardingProgressRail(
                currentPage = currentPage,
                isDark = isDark
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Bottom Primary Action Pill + Optional Secondary Action on Page 3
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 24.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                val ctaText = when (currentPage) {
                    0 -> "Prove Me Wrong"
                    1 -> if (isInstalling) "Provisioning Alpine..." else "Install & Continue"
                    2 -> "Connect My Key"
                    else -> "Let's Ship Something"
                }

                OnboardingPillButton(
                    text = ctaText,
                    onClick = {
                        if (!isCtaReady) return@OnboardingPillButton
                        when (currentPage) {
                            0 -> goToPage(1)
                            1 -> onboardingViewModel.installSelectedPackages(
                                context = context,
                                sandboxManager = sandboxManager,
                                onSuccess = { goToPage(2) }
                            )
                            2 -> onboardingViewModel.setShowKeyInputDialog(true)
                            3 -> onboardingViewModel.completeOnboarding(
                                context = context,
                                onComplete = onComplete
                            )
                        }
                    },
                    isLoading = currentPage == 1 && isInstalling,
                    backgroundColor = OnboardingColors.ObsidianInk,
                    contentColor = OnboardingColors.WhitePure,
                    chevronAccentColor = OnboardingColors.LimeAccent
                )

                AnimatedVisibility(
                    visible = currentPage == 2,
                    enter = fadeIn(
                        animationSpec = tween(durationMillis = 320, delayMillis = 80, easing = FastOutSlowInEasing)
                    ) + expandVertically(
                        animationSpec = tween(durationMillis = 320, easing = FastOutSlowInEasing)
                    ),
                    exit = fadeOut(
                        animationSpec = tween(durationMillis = 160, easing = FastOutLinearInEasing)
                    ) + shrinkVertically(
                        animationSpec = tween(durationMillis = 260, easing = FastOutSlowInEasing)
                    )
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Spacer(modifier = Modifier.height(10.dp))
                        Text(
                            text = "I'll Code Offline First",
                            fontFamily = OnboardingFonts.PlusJakartaSans,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp,
                            textDecoration = TextDecoration.Underline,
                            color = OnboardingColors.textPrimary(isDark).copy(alpha = 0.85f),
                            modifier = Modifier
                                .clip(RoundedCornerShape(8.dp))
                                .clickable(enabled = isCtaReady) {
                                    onboardingViewModel.chooseOfflineMode(
                                        context = context,
                                        onDone = { goToPage(3) }
                                    )
                                }
                                .padding(horizontal = 12.dp, vertical = 6.dp)
                        )
                    }
                }
            }
        }

        // Modal API Key Input Dialog
        if (showKeyInputDialog) {
            ApiKeyInputDialog(
                provider = selectedProvider,
                initialKey = keyManager.getApiKey(selectedProvider) ?: apiKeyInput,
                isDark = isDark,
                onDismiss = {
                    onboardingViewModel.setShowKeyInputDialog(false)
                },
                onSave = { enteredKey ->
                    onboardingViewModel.setApiKeyInput(enteredKey)
                    onboardingViewModel.saveProviderAndKey(
                        context = context,
                        keyManager = keyManager,
                        onDone = { goToPage(3) }
                    )
                }
            )
        }
    }
}


