package com.klix.code

import android.content.Context
import android.content.SharedPreferences
import com.klix.code.data.agent.model.ModelProvider
import com.klix.code.data.agent.model.ProviderSettings
import com.klix.code.data.security.KeyManager
import com.klix.code.ui.onboarding.OnboardingViewModel
import com.klix.code.ui.onboarding.ToolchainOption
import com.klix.code.ui.onboarding.computeProgressRailLayout
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.mockito.ArgumentMatchers.anyInt
import org.mockito.ArgumentMatchers.anyString
import org.mockito.Mockito.mock
import org.mockito.Mockito.`when`

class OnboardingFlowTest {

    private lateinit var viewModel: OnboardingViewModel
    private lateinit var mockContext: Context
    private lateinit var mockPrefs: SharedPreferences
    private lateinit var mockEditor: SharedPreferences.Editor

    private val prefValues = mutableMapOf<String, Any>()

    @Before
    fun setUp() {
        viewModel = OnboardingViewModel()
        mockContext = mock(Context::class.java)
        mockPrefs = mock(SharedPreferences::class.java)
        mockEditor = mock(SharedPreferences.Editor::class.java)
        prefValues.clear()

        `when`(mockContext.getSharedPreferences(anyString(), anyInt())).thenReturn(mockPrefs)
        `when`(mockPrefs.edit()).thenReturn(mockEditor)

        `when`(mockEditor.putBoolean(anyString(), org.mockito.ArgumentMatchers.anyBoolean())).thenAnswer { inv ->
            prefValues[inv.getArgument(0)] = inv.getArgument(1)
            mockEditor
        }
        `when`(mockEditor.putString(anyString(), anyString())).thenAnswer { inv ->
            prefValues[inv.getArgument(0)] = inv.getArgument(1)
            mockEditor
        }
    }

    @Test
    fun testDefaultToolchainIsPython() {
        val selected = viewModel.selectedTools.value
        assertEquals(1, selected.size)
        assertTrue(selected.contains(ToolchainOption.PYTHON))
    }

    @Test
    fun testToggleToolchains() {
        // Toggle Node.js on
        viewModel.toggleTool(ToolchainOption.NODEJS)
        assertTrue(viewModel.selectedTools.value.contains(ToolchainOption.NODEJS))
        assertTrue(viewModel.selectedTools.value.contains(ToolchainOption.PYTHON))
        assertEquals(2, viewModel.selectedTools.value.size)

        // Toggle Python off
        viewModel.toggleTool(ToolchainOption.PYTHON)
        assertFalse(viewModel.selectedTools.value.contains(ToolchainOption.PYTHON))
        assertTrue(viewModel.selectedTools.value.contains(ToolchainOption.NODEJS))
        assertEquals(1, viewModel.selectedTools.value.size)

        // Toggle Git and C++ on
        viewModel.toggleTool(ToolchainOption.GIT)
        viewModel.toggleTool(ToolchainOption.CPP)
        assertEquals(3, viewModel.selectedTools.value.size)
    }

    @Test
    fun testDefaultProviderIsGemini() {
        assertEquals(ModelProvider.GEMINI, viewModel.selectedProvider.value)
    }

    @Test
    fun testSelectProvider() {
        viewModel.selectProvider(ModelProvider.ANTHROPIC)
        assertEquals(ModelProvider.ANTHROPIC, viewModel.selectedProvider.value)

        viewModel.selectProvider(ModelProvider.OPENAI)
        assertEquals(ModelProvider.OPENAI, viewModel.selectedProvider.value)
    }

    @Test
    fun testSaveProviderAndKey() {
        val mockKeyManager = object : KeyManager {
            val keys = mutableMapOf<ModelProvider, String>()
            var currentSettings = ProviderSettings()

            override fun getApiKey(provider: ModelProvider): String? = keys[provider]
            override fun setApiKey(provider: ModelProvider, key: String?) {
                if (key != null) keys[provider] = key else keys.remove(provider)
            }
            override fun hasApiKey(provider: ModelProvider): Boolean = keys.containsKey(provider)
            override fun getSettings(): ProviderSettings = currentSettings
            override fun updateSettings(s: ProviderSettings) { currentSettings = s }
            override fun clearAllKeys() { keys.clear() }
        }

        viewModel.selectProvider(ModelProvider.ANTHROPIC)
        viewModel.setApiKeyInput("sk-ant-test-key-12345")

        var callbackCalled = false
        viewModel.saveProviderAndKey(mockContext, mockKeyManager) {
            callbackCalled = true
        }

        assertTrue(callbackCalled)
        assertEquals("sk-ant-test-key-12345", mockKeyManager.getApiKey(ModelProvider.ANTHROPIC))
        assertEquals(ModelProvider.ANTHROPIC, mockKeyManager.getSettings().activeProvider)
        assertEquals(false, prefValues["offline_mode"])
        assertEquals("ANTHROPIC", prefValues["active_ai_provider"])
    }

    @Test
    fun testOfflineModePersisted() {
        var callbackCalled = false
        viewModel.chooseOfflineMode(mockContext) {
            callbackCalled = true
        }

        assertTrue(callbackCalled)
        assertEquals(true, prefValues["offline_mode"])
    }

    @Test
    fun testCompleteOnboardingPersisted() {
        var callbackCalled = false
        viewModel.completeOnboarding(mockContext) {
            callbackCalled = true
        }

        assertTrue(callbackCalled)
        assertEquals(true, prefValues["onboarding_completed"])
    }

    @Test
    fun testProgressRailRestingLayoutsPreserveSpacingAndWidth() {
        for (page in 0 until 4) {
            val layout = computeProgressRailLayout(
                leftIndex = page.toFloat(),
                rightIndex = page.toFloat()
            )
            assertEquals(69f, layout.totalWidthDp, 0.001f)
            assertEquals(30f, layout.pillRightDp - layout.pillLeftDp, 0.001f)
            assertEquals(page * 13f, layout.pillLeftDp, 0.001f)
            assertEquals(3, layout.inactiveDots.size)
            for (dot in layout.inactiveDots) {
                assertEquals(7f, dot.rightDp - dot.leftDp, 0.001f)
                assertTrue(dot.leftDp >= 0f && dot.rightDp <= layout.totalWidthDp)
            }
        }
    }

    @Test
    fun testProgressRailForwardAndBackwardSlideExpandsPillWithinBounds() {
        // Forward step (Next: 0 -> 1): both dot 0 (left) and dot 1 (right) partially peek outside the sliding pill
        val forwardMid = computeProgressRailLayout(leftIndex = 0.25f, rightIndex = 0.85f)
        assertTrue(forwardMid.pillRightDp - forwardMid.pillLeftDp > 30f)
        assertTrue(forwardMid.pillLeftDp >= 0f && forwardMid.pillRightDp <= forwardMid.totalWidthDp)
        assertEquals(4, forwardMid.inactiveDots.size)
        assertEquals(0f, forwardMid.inactiveDots[0].leftDp, 0.001f)
        assertEquals(36f, forwardMid.inactiveDots[1].leftDp, 0.001f)

        // Backward step (Back: 1 -> 0): reverse symmetry keeps pill expanded and dots in bounds
        val backwardMid = computeProgressRailLayout(leftIndex = 0.15f, rightIndex = 0.75f)
        assertTrue(backwardMid.pillRightDp - backwardMid.pillLeftDp > 30f)
        assertTrue(backwardMid.pillLeftDp >= 0f && backwardMid.pillRightDp <= backwardMid.totalWidthDp)
        assertEquals(4, backwardMid.inactiveDots.size)

        // Multi-page skip sweep (0 -> 3): intermediate dots 1 & 2 are covered by the sweeping capsule
        val skipMid = computeProgressRailLayout(leftIndex = 0.4f, rightIndex = 2.6f)
        assertTrue(skipMid.pillRightDp - skipMid.pillLeftDp > 50f)
        assertTrue(skipMid.pillLeftDp >= 0f && skipMid.pillRightDp <= skipMid.totalWidthDp)
        assertEquals(2, skipMid.inactiveDots.size) // dot 0 on left, dot 3 on right
    }
}
