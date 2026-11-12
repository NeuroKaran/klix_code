package com.klix.code

import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File

class DrawableResourceValidationTest {

    private fun getResDrawableDir(): File {
        val candidates = listOf(
            File("src/main/res/drawable"),
            File("app/src/main/res/drawable"),
            File("../app/src/main/res/drawable")
        )
        return candidates.firstOrNull { it.exists() && it.isDirectory }
            ?: throw IllegalStateException("Could not locate res/drawable directory")
    }

    private fun getMainJavaDir(): File {
        val candidates = listOf(
            File("src/main/java"),
            File("app/src/main/java"),
            File("../app/src/main/java")
        )
        return candidates.firstOrNull { it.exists() && it.isDirectory }
            ?: throw IllegalStateException("Could not locate src/main/java directory")
    }

    @Test
    fun testAllMascotDrawablesAreRasterAssets() {
        val drawableDir = getResDrawableDir()
        val expectedMascots = listOf(
            "mascot_coding_laptop",
            "mascot_peeking",
            "mascot_running_terminal",
            "mascot_streak_fire",
            "mascot_waving"
        )

        for (name in expectedMascots) {
            val pngFile = File(drawableDir, "$name.png")
            val xmlFile = File(drawableDir, "$name.xml")

            assertTrue("Expected $name.png to exist in drawable directory", pngFile.exists())
            assertTrue("Expected $name.xml to NOT exist in drawable directory (avoid Compose vector parse crash)", !xmlFile.exists())
        }
    }

    @Test
    fun testNoNonVectorXmlDrawablesUsedWithPainterResource() {
        val drawableDir = getResDrawableDir()
        val javaDir = getMainJavaDir()

        // Scan all Kotlin files for R.drawable.<name>
        val regex = Regex("""R\.drawable\.([a-zA-Z0-9_]+)""")
        val referencedDrawables = mutableSetOf<String>()

        javaDir.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            val text = file.readText()
            if (text.contains("painterResource")) {
                regex.findAll(text).forEach { match ->
                    referencedDrawables.add(match.groupValues[1])
                }
            }
        }

        assertTrue("Should have found referenced drawables in Compose files", referencedDrawables.isNotEmpty())

        val violations = mutableListOf<String>()

        for (drawableName in referencedDrawables) {
            val matchingFiles = drawableDir.listFiles { _, name ->
                name.startsWith(drawableName + ".")
            } ?: emptyArray()

            if (matchingFiles.isEmpty()) {
                violations.add("Resource R.drawable.$drawableName does not exist in ${drawableDir.path}")
                continue
            }

            for (file in matchingFiles) {
                if (file.extension == "xml") {
                    val content = file.readText().trim()
                    // Check that the root element is <vector>
                    val isVector = content.contains("<vector")
                    if (!isVector) {
                        violations.add(
                            "Resource R.drawable.$drawableName is an XML drawable (${file.name}) but root element is NOT <vector>. " +
                                "Jetpack Compose painterResource() will crash with IllegalArgumentException!"
                        )
                    }
                }
            }
        }

        if (violations.isNotEmpty()) {
            fail("Found invalid drawable resources used in Compose:\n" + violations.joinToString("\n"))
        }
    }
}

