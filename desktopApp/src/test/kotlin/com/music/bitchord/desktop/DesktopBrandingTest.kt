package com.music.bitchord.desktop

import java.nio.file.Files
import java.nio.file.Path
import javax.xml.parsers.DocumentBuilderFactory
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class DesktopBrandingTest {
    @Test
    fun desktopLocalizedStringsUseTheCurrentProductName() {
        val workingDirectory = Path.of("").toAbsolutePath()
        val desktopAppDirectory = listOf(workingDirectory, workingDirectory.resolve("desktopApp"))
            .firstOrNull { Files.isDirectory(it.resolve("src/main/resources/strings")) }
            ?: error("Could not locate desktopApp string resources from $workingDirectory")
        val catalogues = listOf(
            desktopAppDirectory.resolve("src/main/resources/strings"),
            desktopAppDirectory.parent.resolve("desktopUi/src/commonMain/composeResources"),
        )
        var checkedValues = 0
        for (directory in catalogues) {
            assertTrue(Files.isDirectory(directory), "Expected desktop string directory: $directory")
            Files.walk(directory).use { paths ->
                paths.filter { Files.isRegularFile(it) && it.fileName.toString().endsWith(".xml") }
                    .forEach { file ->
                        val document = DocumentBuilderFactory.newInstance()
                            .newDocumentBuilder()
                            .parse(file.toFile())
                        val strings = document.getElementsByTagName("string")
                        for (index in 0 until strings.length) {
                            val value = strings.item(index).textContent.orEmpty()
                            checkedValues++
                            assertFalse(
                                value.contains("bitchord", ignoreCase = true),
                                "Legacy product name in $file: $value",
                            )
                        }
                    }
            }
        }
        assertTrue(checkedValues > 0, "Expected to inspect desktop-localized string values")
        assertEquals("Zotify Music", DesktopDiscordSettings.APP_NAME)
        assertEquals("Visit Zotify Music", DesktopDiscordSettings.DEFAULT_BUTTON_2)
    }
}
