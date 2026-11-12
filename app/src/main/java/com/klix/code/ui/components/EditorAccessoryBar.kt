package com.klix.code.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klix.code.ui.components.glass.pressEffect
import com.klix.code.ui.theme.KlixBorderHairline
import com.klix.code.ui.theme.KlixInkPrimary
import com.klix.code.ui.theme.KlixInkSecondary
import com.klix.code.ui.theme.KlixSurfaceBentoGroup
import com.klix.code.ui.theme.KlixSurfaceCardItem
import com.klix.code.ui.theme.KlixTypography
import com.klix.code.ui.theme.LimeBrandColor

@Composable
fun EditorAccessoryBar(
    controller: EditorController,
    modifier: Modifier = Modifier
) {
    val scrollState = rememberScrollState()

    val quickKeys = listOf(
        // Actions
        KeyItem("Tab", isAction = true) { controller.insertText("  ") },
        KeyItem("↶", isAction = true) { controller.undo() },
        KeyItem("↷", isAction = true) { controller.redo() },
        // Brackets & Symbols matching Design.md
        KeyItem("(") { controller.insertText("(") },
        KeyItem(")") { controller.insertText(")") },
        KeyItem(":") { controller.insertText(":") },
        KeyItem("\"") { controller.insertText("\"") },
        KeyItem("{") { controller.insertText("{") },
        KeyItem("}") { controller.insertText("}") },
        KeyItem("[") { controller.insertText("[") },
        KeyItem("]") { controller.insertText("]") },
        KeyItem("=") { controller.insertText("=") },
        KeyItem(";") { controller.insertText(";") },
        KeyItem("'") { controller.insertText("'") },
        KeyItem(",") { controller.insertText(",") },
        KeyItem(".") { controller.insertText(".") },
        KeyItem("+") { controller.insertText("+") },
        KeyItem("-") { controller.insertText("-") },
        KeyItem("*") { controller.insertText("*") },
        KeyItem("/") { controller.insertText("/") },
        KeyItem("<") { controller.insertText("<") },
        KeyItem(">") { controller.insertText(">") },
        KeyItem("!") { controller.insertText("!") },
        KeyItem("$") { controller.insertText("$") },
        KeyItem("_") { controller.insertText("_") }
    )

    Row(
        modifier = modifier
            .height(42.dp)
            .background(KlixSurfaceBentoGroup)
            .border(0.5.dp, KlixBorderHairline)
            .horizontalScroll(scrollState)
            .padding(horizontal = 8.dp, vertical = 4.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val keyShape = RoundedCornerShape(10.dp)

        quickKeys.forEach { key ->
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(min = if (key.label.length > 1) 44.dp else 34.dp)
                    .clip(keyShape)
                    .background(KlixSurfaceCardItem)
                    .border(1.dp, KlixBorderHairline, keyShape)
                    .pressEffect(targetScale = 0.92f)
                    .clickable { key.onClick() }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = key.label,
                    color = if (key.isAction) LimeBrandColor else KlixInkPrimary,
                    style = KlixTypography.codePill,
                    fontWeight = if (key.isAction) FontWeight.Bold else FontWeight.Medium
                )
            }
        }
    }
}

private data class KeyItem(
    val label: String,
    val isAction: Boolean = false,
    val onClick: () -> Unit
)
