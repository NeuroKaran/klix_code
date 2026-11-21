package com.klix.code

import com.klix.code.ui.components.agent.AgentSettingsUiHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AgentSettingsUiHelperTest {

    @Test
    fun `test provider shows Ready only when API key is set`() {
        // When key is configured, whether selected or unselected, it must be Ready
        assertEquals("Ready", AgentSettingsUiHelper.getProviderStatusLabel(hasKey = true, isSelected = true))
        assertEquals("Ready", AgentSettingsUiHelper.getProviderStatusLabel(hasKey = true, isSelected = false))
    }

    @Test
    fun `test provider shows Not Ready when selected and API key is not configured`() {
        // When key is NOT set and provider is selected, must show "Not Ready" (negation of ready)
        val status = AgentSettingsUiHelper.getProviderStatusLabel(hasKey = false, isSelected = true)
        assertEquals("Not Ready", status)
        assertNotEquals("Ready", status)
    }

    @Test
    fun `test provider shows Add Key when unselected and API key is not configured`() {
        // When key is NOT set and provider is unselected, shows "Add Key" prompt
        val status = AgentSettingsUiHelper.getProviderStatusLabel(hasKey = false, isSelected = false)
        assertEquals("Add Key", status)
        assertNotEquals("Ready", status)
    }

    @Test
    fun `test provider never shows Ready under any condition if API key is not configured`() {
        // Invariant: If hasKey is false, it MUST NEVER show "Ready"
        assertNotEquals("Ready", AgentSettingsUiHelper.getProviderStatusLabel(hasKey = false, isSelected = true))
        assertNotEquals("Ready", AgentSettingsUiHelper.getProviderStatusLabel(hasKey = false, isSelected = false))
    }
}
