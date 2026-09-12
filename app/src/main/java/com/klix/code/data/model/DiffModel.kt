package com.klix.code.data.model

import android.net.Uri

enum class DiffLineType {
    UNCHANGED,
    ADDED,
    REMOVED
}

data class DiffLine(
    val type: DiffLineType,
    val oldLineNumber: Int?,
    val newLineNumber: Int?,
    val content: String
)

data class FileDiff(
    val fileUri: Uri? = null,
    val filePath: String,
    val originalText: String,
    val modifiedText: String,
    val lines: List<DiffLine>
) {
    val additionsCount: Int = lines.count { it.type == DiffLineType.ADDED }
    val deletionsCount: Int = lines.count { it.type == DiffLineType.REMOVED }
}

object DiffCalculator {
    /**
     * Computes a line-by-line diff using Longest Common Subsequence (LCS).
     */
    fun computeDiff(
        filePath: String,
        originalText: String,
        modifiedText: String,
        fileUri: Uri? = null
    ): FileDiff {
        val oldLines = if (originalText.isEmpty()) emptyList() else originalText.split("\n")
        val newLines = if (modifiedText.isEmpty()) emptyList() else modifiedText.split("\n")

        val m = oldLines.size
        val n = newLines.size

        val dp = Array(m + 1) { IntArray(n + 1) }
        for (i in 1..m) {
            for (j in 1..n) {
                dp[i][j] = if (oldLines[i - 1] == newLines[j - 1]) {
                    dp[i - 1][j - 1] + 1
                } else {
                    maxOf(dp[i - 1][j], dp[i][j - 1])
                }
            }
        }

        val resultLines = mutableListOf<DiffLine>()
        var i = m
        var j = n

        val backtrack = mutableListOf<DiffLine>()
        while (i > 0 || j > 0) {
            if (i > 0 && j > 0 && oldLines[i - 1] == newLines[j - 1]) {
                backtrack.add(
                    DiffLine(
                        type = DiffLineType.UNCHANGED,
                        oldLineNumber = i,
                        newLineNumber = j,
                        content = oldLines[i - 1]
                    )
                )
                i--
                j--
            } else if (j > 0 && (i == 0 || dp[i][j - 1] >= dp[i - 1][j])) {
                backtrack.add(
                    DiffLine(
                        type = DiffLineType.ADDED,
                        oldLineNumber = null,
                        newLineNumber = j,
                        content = newLines[j - 1]
                    )
                )
                j--
            } else if (i > 0 && (j == 0 || dp[i][j - 1] < dp[i - 1][j])) {
                backtrack.add(
                    DiffLine(
                        type = DiffLineType.REMOVED,
                        oldLineNumber = i,
                        newLineNumber = null,
                        content = oldLines[i - 1]
                    )
                )
                i--
            }
        }

        resultLines.addAll(backtrack.reversed())
        return FileDiff(
            fileUri = fileUri,
            filePath = filePath,
            originalText = originalText,
            modifiedText = modifiedText,
            lines = resultLines
        )
    }
}
