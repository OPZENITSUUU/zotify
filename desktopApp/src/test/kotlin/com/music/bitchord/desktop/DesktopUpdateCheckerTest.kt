package com.music.bitchord.desktop

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Test
import kotlin.test.assertNull

class DesktopUpdateCheckerTest {

    @Test
    fun `Android APK release is not offered as a desktop installer`() {
        val release = release(asset("Zotify.Music.apk"))

        assertNull(DesktopUpdateChecker.installerUrl(release))
    }

    @Test
    fun `unpublished desktop installer assets are ignored`() {
        val release = release(
            asset("Zotify-Music-1.8-linux-x86_64.AppImage", state = "new"),
        )

        assertNull(DesktopUpdateChecker.installerUrl(release))
    }

    private fun asset(name: String, state: String = "uploaded"): String =
        """{"name":"$name","state":"$state","browser_download_url":"https://github.com/OPZENITSUUU/zotify-music/releases/download/v1.8/$name"}"""

    private fun release(vararg assets: String): JsonObject =
        Json.parseToJsonElement("""{"assets":[${assets.joinToString(",")}] }""") as JsonObject
}
