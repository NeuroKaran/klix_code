package com.klix.code.ui.onboarding

import android.content.Context
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.klix.code.data.agent.model.ModelProvider
import com.klix.code.data.sandbox.SandboxManager
import com.klix.code.data.security.KeyManager
import com.klix.code.data.security.KeyManagerImpl
import kotlinx.coroutines.launch
import kotlin.math.abs

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
    val scope = rememberCoroutineScope()
    val pagerState = rememberPagerState(initialPage = 0, pageCount = { 4 })

    val selectedTools by onboardingViewModel.selectedTools.collectAsState()
    val installState by onboardingViewModel.installState.collectAsState()
    val selectedProvider by onboardingViewModel.selectedProvider.collectAsState()
    val apiKeyInput by onboardingViewModel.apiKeyInput.collectAsState()
    val showKeyInputDialog by onboardingViewModel.showKeyInputDialog.collectAsState()

    val isInstalling = installState is SandboxInstallState.DownloadingRootFs ||
        installState is SandboxInstallState.ExtractingRootFs ||
        installState is SandboxInstallState.InstallingPackages

    // Intercept hardware back button: step back through pages when not installing
    BackHandler(enabled = pagerState.currentPage > 0 && !isInstalling) {
        scope.launch {
            pagerState.animateScrollToPage(pagerState.currentPage - 1)
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(OnboardingColors.backgroundGradient(isDark))
    ) {
        // Shared Minimal Drifting Ambient Background
        DriftingAmbientBackground(isDark = isDark)

        // 4-Page Horizontal Pager (disabled during active rootfs/package installation)
        HorizontalPager(
            state = pagerState,
            userScrollEnabled = !isInstalling,
            modifier = Modifier.fillMaxSize()
        ) { page ->
            // Subtle page parallax and crossfade
            val pageOffset = (pagerState.currentPage - page) + pagerState.currentPageOffsetFraction
            val pageAlpha = 1f - (abs(pageOffset).coerceIn(0f, 1f) * 0.35f)

            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .graphicsLayer {
                        alpha = pageAlpha
                    }
            ) {
                when (page) {
                    0 -> {
                        OnboardingPage1(
                            isDark = isDark,
                            onNext = {
                                scope.launch { pagerState.animateScrollToPage(1) }
                            },
                            onSkip = {
                                scope.launch { pagerState.animateScrollToPage(3) }
                            }
                        )
                    }
                    1 -> {
                        OnboardingPage2(
                            isDark = isDark,
                            selectedTools = selectedTools,
                            installState = installState,
                            onToggleTool = { tool ->
                                onboardingViewModel.toggleTool(tool)
                            },
                            onInstallAndContinue = {
                                onboardingViewModel.installSelectedPackages(
                                    context = context,
                                    sandboxManager = sandboxManager,
                                    onSuccess = {
                                        scope.launch {
                                            pagerState.animateScrollToPage(2)
                                        }
                                    }
                                )
                            },
                            onRetry = {
                                onboardingViewModel.retryInstallation(
                                    context = context,
                                    sandboxManager = sandboxManager,
                                    onSuccess = {
                                        scope.launch {
                                            pagerState.animateScrollToPage(2)
                                        }
                                    }
                                )
                            },
                            onBack = {
                                scope.launch { pagerState.animateScrollToPage(0) }
                            },
                            onSkip = {
                                scope.launch { pagerState.animateScrollToPage(2) }
                            }
                        )
                    }
                    2 -> {
                        OnboardingPage3(
                            isDark = isDark,
                            selectedProvider = selectedProvider,
                            onSelectProvider = { provider ->
                                onboardingViewModel.selectProvider(provider)
                            },
                            onConnectKey = {
                                onboardingViewModel.setShowKeyInputDialog(true)
                            },
                            onOfflineMode = {
                                onboardingViewModel.chooseOfflineMode(
                                    context = context,
                                    onDone = {
                                        scope.launch {
                                            pagerState.animateScrollToPage(3)
                                        }
                                    }
                                )
                            },
                            onBack = {
                                scope.launch { pagerState.animateScrollToPage(1) }
                            },
                            onSkip = {
                                scope.launch { pagerState.animateScrollToPage(3) }
                            }
                        )
                    }
                    3 -> {
                        OnboardingPage4(
                            isDark = isDark,
                            onComplete = {
                                onboardingViewModel.completeOnboarding(
                                    context = context,
                                    onComplete = onComplete
                                )
                            },
                            onBack = {
                                scope.launch { pagerState.animateScrollToPage(2) }
                            }
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
                onDismiss = {
                    onboardingViewModel.setShowKeyInputDialog(false)
                },
                onSave = { enteredKey ->
                    onboardingViewModel.setApiKeyInput(enteredKey)
                    onboardingViewModel.saveProviderAndKey(
                        context = context,
                        keyManager = keyManager,
                        onDone = {
                            scope.launch {
                                pagerState.animateScrollToPage(3)
                            }
                        }
                    )
                }
            )
        }
    }
}
