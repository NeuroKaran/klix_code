package com.klix.code.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.klix.code.ui.components.glass.glassBorder
import com.klix.code.ui.components.glass.pressEffect
import com.klix.code.ui.theme.CyanBrandColor
import com.klix.code.ui.theme.CyanDimColor
import com.klix.code.ui.theme.GlassSurfaceColor
import com.klix.code.ui.theme.GlassSurfaceElevatedColor
import com.klix.code.ui.theme.TextPrimary
import com.klix.code.ui.theme.TextSecondary

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
        // Numbers
        KeyItem("0") { controller.insertText("0") },
        KeyItem("1") { controller.insertText("1") },
        KeyItem("2") { controller.insertText("2") },
        KeyItem("3") { controller.insertText("3") },
        KeyItem("4") { controller.insertText("4") },
        KeyItem("5") { controller.insertText("5") },
        KeyItem("6") { controller.insertText("6") },
        KeyItem("7") { controller.insertText("7") },
        KeyItem("8") { controller.insertText("8") },
        KeyItem("9") { controller.insertText("9") },
        // Brackets
        KeyItem("(") { controller.insertText("(") },
        KeyItem(")") { controller.insertText(")") },
        KeyItem("{") { controller.insertText("{") },
        KeyItem("}") { controller.insertText("}") },
        KeyItem("[") { controller.insertText("[") },
        KeyItem("]") { controller.insertText("]") },
        KeyItem("<") { controller.insertText("<") },
        KeyItem(">") { controller.insertText(">") },
        // Symbols & Operators
        KeyItem("=") { controller.insertText("=") },
        KeyItem(";") { controller.insertText(";") },
        KeyItem(":") { controller.insertText(":") },
        KeyItem("\"") { controller.insertText("\"") },
        KeyItem("'") { controller.insertText("'") },
        KeyItem("`") { controller.insertText("`") },
        KeyItem(",") { controller.insertText(",") },
        KeyItem(".") { controller.insertText(".") },
        KeyItem("+") { controller.insertText("+") },
        KeyItem("-") { controller.insertText("-") },
        KeyItem("*") { controller.insertText("*") },
        KeyItem("/") { controller.insertText("/") },
        KeyItem("!") { controller.insertText("!") },
        KeyItem("?") { controller.insertText("?") },
        KeyItem("&") { controller.insertText("&") },
        KeyItem("|") { controller.insertText("|") },
        KeyItem("$") { controller.insertText("$") },
        KeyItem("_") { controller.insertText("_") },
        KeyItem("@") { controller.insertText("@") },
        KeyItem("#") { controller.insertText("#") },
        KeyItem("%") { controller.insertText("%") },
        KeyItem("\\") { controller.insertText("\\") },
        KeyItem("~") { controller.insertText("~") }
    )

    Row(
        modifier = modifier
            .height(42.dp)
            .background(GlassSurfaceColor)
            .glassBorder(RoundedCornerShape(0.dp), strokeWidth = 0.5.dp, highlightAlpha = 0.18f, midAlpha = 0.06f)
            .horizontalScroll(scrollState)
            .padding(horizontal = 8.dp, vertical = 5.dp),
        horizontalArrangement = Arrangement.spacedBy(6.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        val keyShape = RoundedCornerShape(8.dp)

        quickKeys.forEach { key ->
            Box(
                modifier = Modifier
                    .fillMaxHeight()
                    .widthIn(min = if (key.label.length > 1) 44.dp else 34.dp)
                    .clip(keyShape)
                    .background(if (key.isAction) CyanDimColor else GlassSurfaceElevatedColor)
                    .glassBorder(
                        shape = keyShape,
                        strokeWidth = 0.8.dp,
                        highlightAlpha = if (key.isAction) 0.35f else 0.22f,
                        midAlpha = 0.08f
                    )
                    .pressEffect(targetScale = 0.92f)
                    .clickable { key.onClick() }
                    .padding(horizontal = 6.dp),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = key.label,
                    color = if (key.isAction) CyanBrandColor else TextPrimary,
                    fontSize = if (key.label.length > 2) 11.5.sp else 13.5.sp,
                    fontWeight = if (key.isAction) FontWeight.Bold else FontWeight.Medium,
                    fontFamily = FontFamily.Monospace
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
