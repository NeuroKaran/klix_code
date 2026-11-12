package com.klix.code

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.File
import javax.xml.parsers.DocumentBuilderFactory

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

    private fun getXmlRootTagName(file: File): String {
        val factory = DocumentBuilderFactory.newInstance()
        factory.isNamespaceAware = true
        val builder = factory.newDocumentBuilder()
        val doc = builder.parse(file)
        return doc.documentElement.localName ?: doc.documentElement.tagName
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
            assertTrue("Expected $name.png to be a non-empty asset", pngFile.length() > 0)
            assertFalse(
                "Expected $name.xml to NOT exist in drawable directory (avoid Compose vector parse crash)",
                xmlFile.exists()
            )
        }

        // Verify dead legacy duplicates were removed to avoid APK size bloat
        val deadLegacyAssets = listOf(
            "klix_mascot_buddy",
            "klix_mascot_laptop",
            "klix_mascot_peeking",
            "klix_mascot_running",
            "klix_mascot_streak",
            "klix_mascot_terminal"
        )
        for (legacy in deadLegacyAssets) {
            val legacyFile = File(drawableDir, "$legacy.png")
            assertFalse(
                "Obsolete asset $legacy.png should not be retained (prevents APK bloat)",
                legacyFile.exists()
            )
        }
    }

    @Test
    fun testNoNonVectorXmlDrawablesUsedInCompose() {
        val drawableDir = getResDrawableDir()
        val javaDir = getMainJavaDir()

        // Scan all UI files, MainActivity, and any Composable files for R.drawable.<name>
        val regex = Regex("""R\.drawable\.([a-zA-Z0-9_]+)""")
        val referencedDrawables = mutableSetOf<String>()

        javaDir.walkTopDown().filter { it.extension == "kt" }.forEach { file ->
            val text = file.readText()
            val isUiFile = file.path.contains("ui") || file.name == "MainActivity.kt"
            val hasCompose = text.contains("painterResource") || text.contains("@Composable")

            if (isUiFile || hasCompose) {
                regex.findAll(text).forEach { match ->
                    referencedDrawables.add(match.groupValues[1])
                }
            }
        }

        assertTrue("Should have found referenced drawables in UI files", referencedDrawables.isNotEmpty())

        val violations = mutableListOf<String>()

        for (drawableName in referencedDrawables) {
            val matchingFiles = drawableDir.listFiles { _, name ->
                name.startsWith("$drawableName.")
            } ?: emptyArray()

            if (matchingFiles.isEmpty()) {
                violations.add("Resource R.drawable.$drawableName does not exist in ${drawableDir.path}")
                continue
            }

            for (file in matchingFiles) {
                if (file.extension == "xml") {
                    try {
                        val rootTag = getXmlRootTagName(file)
                        if (rootTag != "vector" && rootTag != "animated-vector") {
                            violations.add(
                                "Resource R.drawable.$drawableName is an XML drawable (${file.name}) with root <$rootTag>. " +
                                    "Jetpack Compose painterResource() will crash with IllegalArgumentException! Only <vector> is supported."
                            )
                        }
                    } catch (e: Exception) {
                        violations.add("Resource R.drawable.$drawableName (${file.name}) failed XML parsing: ${e.message}")
                    }
                } else if (file.extension in listOf("png", "jpg", "jpeg", "webp")) {
                    if (file.length() == 0L) {
                        violations.add("Raster resource R.drawable.$drawableName (${file.name}) is empty (0 bytes)")
                    }
                }
            }
        }

        if (violations.isNotEmpty()) {
            fail("Found invalid drawable resources used in Compose UI:\n" + violations.joinToString("\n"))
        }
    }

    @Test
    fun testAllXmlDrawablesInResAreWellFormed() {
        val drawableDir = getResDrawableDir()
        val xmlFiles = drawableDir.listFiles { _, name -> name.endsWith(".xml") } ?: emptyArray()
        assertTrue("Expected XML drawables in res/drawable", xmlFiles.isNotEmpty())

        for (file in xmlFiles) {
            val rootTag = getXmlRootTagName(file)
            assertTrue(
                "XML file ${file.name} has unexpected root tag <$rootTag>",
                rootTag in listOf("vector", "animated-vector", "shape", "layer-list", "selector", "ripple", "inset")
            )
        }
    }
}


