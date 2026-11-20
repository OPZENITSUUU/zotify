package com.music.bitchord

import com.music.bitchord.data.AppUpdateChecker
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonObject
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AppUpdateCheckerTest {
    private val digest = "a".repeat(64)

    @Test
    fun `direct APK selection requires SHA-256 digest`() {
        val release = release(asset(digestValue = null))

        assertNull(AppUpdateChecker.verifiedApkAsset(release))
    }

    @Test
    fun `direct APK selection rejects non-official or insecure asset URLs`() {
        val wrongHost = asset(url = "https://example.invalid/Zotify.Music.apk")
        val cleartext = asset(url = "http://github.com/OPZENITSUUU/zotify-music/releases/download/v2/Zotify.Music.apk")
        val staleRepository = asset(url = "https://github.com/OPZENITSUUU/zotify/releases/download/v2/Zotify.Music.apk")

        assertNull(AppUpdateChecker.verifiedApkAsset(release(wrongHost)))
        assertNull(AppUpdateChecker.verifiedApkAsset(release(cleartext)))
        assertNull(AppUpdateChecker.verifiedApkAsset(release(staleRepository)))
    }

    @Test
    fun `universal official Zotify Music release APK is selected with normalized digest`() {
        val split = asset(name = "Zotify.Music_arm64-v8a.apk")
        val universal = asset(name = "Zotify.Music.apk", digestValue = "sha256:$digest")

        val selected = AppUpdateChecker.verifiedApkAsset(release(split, universal))

        assertEquals("Zotify.Music", selected?.name)
        assertEquals("https://github.com/OPZENITSUUU/zotify-music/releases/download/v2/Zotify.Music.apk", selected?.url)
        assertEquals(digest, selected?.sha256)
    }

    @Test
    fun `phone updater ignores Android TV release APK assets`() {
        val tv = asset(name = "ZotifyMusicTV-v1.8.apk")
        val similarlyPrefixedTv = asset(name = "Zotify.MusicTV-v1.8.apk")

        assertNull(AppUpdateChecker.verifiedApkAsset(release(tv)))
        assertNull(AppUpdateChecker.verifiedApkAsset(release(similarlyPrefixedTv)))
    }

    @Test
    fun `digest parser rejects malformed SHA-256 values`() {
        assertNull(AppUpdateChecker.normalizeSha256Digest("sha256:1234"))
        assertEquals(digest, AppUpdateChecker.normalizeSha256Digest("SHA256:$digest"))
    }

    @Test
    fun `release version code parser requires a positive integer metadata line`() {
        assertEquals(43L, AppUpdateChecker.releaseVersionCode("Release notes\nAndroid version code: 43\n- Fixes"))
        assertNull(AppUpdateChecker.releaseVersionCode("Release notes without version metadata"))
        assertNull(AppUpdateChecker.releaseVersionCode("Android version code: 0"))
        assertNull(AppUpdateChecker.releaseVersionCode("Android version code: 43abc"))
    }

    @Test
    fun `TV release metadata stays separate from phone code and tag semantics`() {
        val body = """Android version code: 43
            |Android TV version code: 27
            |Android TV version name: 1.9
        """.trimMargin()

        assertEquals(43L, AppUpdateChecker.releaseVersionCode(body))
        assertEquals(27L, AppUpdateChecker.tvReleaseVersionCode(body))
        assertEquals("1.9", AppUpdateChecker.tvReleaseVersionName(body))
        assertNull(AppUpdateChecker.tvReleaseVersionCode("Android version code: 43"))
        assertNull(AppUpdateChecker.tvReleaseVersionName("Android TV version name:   "))
        assertTrue(AppUpdateChecker.isTvUpdateTarget("ZotifyMusicTV-v"))
        assertFalse(AppUpdateChecker.isTvUpdateTarget("Zotify.Music"))
        assertFalse(AppUpdateChecker.isMandatoryUpdate(26L, 27L, 27L, supportsMandatoryPolicy = false))
    }

    @Test
    fun `minimum supported version metadata accepts only one positive integer line`() {
        assertEquals(
            42L,
            AppUpdateChecker.minimumSupportedVersionCode("Release notes\nMinimum supported Android phone version code: 42"),
        )
        assertNull(AppUpdateChecker.minimumSupportedVersionCode("Release notes without a support floor"))
        assertNull(AppUpdateChecker.minimumSupportedVersionCode("Minimum supported Android phone version code: 0"))
        assertNull(AppUpdateChecker.minimumSupportedVersionCode("Minimum supported Android phone version code: 50x"))
        assertNull(
            AppUpdateChecker.minimumSupportedVersionCode(
                "Minimum supported Android phone version code: 41\nMinimum supported Android phone version code: 42",
            ),
        )
    }

    @Test
    fun `mandatory update applies only below a valid released minimum version code`() {
        assertTrue(AppUpdateChecker.isMandatoryUpdate(41L, 42L, 43L))
        assertFalse(AppUpdateChecker.isMandatoryUpdate(42L, 42L, 43L))
        assertFalse(AppUpdateChecker.isMandatoryUpdate(41L, null, 43L))
        assertFalse(AppUpdateChecker.isMandatoryUpdate(41L, 44L, 43L))
        assertFalse(AppUpdateChecker.isMandatoryUpdate(41L, 42L, 43L, supportsMandatoryPolicy = false))
    }

    @Test
    fun `mandatory release remains available without a compatible verified APK`() {
        val update = AppUpdateChecker.updateInfoForRelease(
            version = "1.8.2",
            releaseUrl = "https://github.com/OPZENITSUUU/zotify-music/releases/tag/v1.8.2",
            apk = null,
            notes = "- Critical fix",
            mandatory = true,
        )

        assertEquals("1.8.2", update?.version)
        assertTrue(update?.mandatory == true)
        assertNull(update?.apkUrl)
        assertNull(update?.apkSha256)
        assertNull(
            AppUpdateChecker.updateInfoForRelease(
                version = "1.8.2",
                releaseUrl = "https://github.com/OPZENITSUUU/zotify-music/releases/tag/v1.8.2",
                apk = null,
                notes = null,
                mandatory = false,
            ),
        )
    }

    @Test
    fun `pending mandatory update state supports missing APK URL and digest`() {
        val pending = AppUpdateChecker.pendingUpdateInfo(
            version = "1.8.2",
            releaseUrl = "https://github.com/OPZENITSUUU/zotify-music/releases/tag/v1.8.2",
            apkUrl = null,
            apkSha256 = null,
            notes = "- Critical fix",
            mandatory = true,
        )

        assertTrue(pending.mandatory)
        assertNull(pending.apkUrl)
        assertNull(pending.apkSha256)
        assertEquals("- Critical fix", pending.notes)
    }

    @Test
    fun `version code comparison detects only strictly higher codes`() {
        assertTrue(AppUpdateChecker.isNewerVersionCode(candidate = 44L, installed = 43L))
        assertFalse(AppUpdateChecker.isNewerVersionCode(candidate = 43L, installed = 43L))
        assertFalse(AppUpdateChecker.isNewerVersionCode(candidate = 42L, installed = 43L))
    }

    @Test
    fun `release eligibility blocks downgrades and preserves same-code beta to stable updates`() {
        assertTrue(AppUpdateChecker.shouldNotifyUpdate("1.8.2", 44L, "1.8.1", 43L, null))
        assertFalse(AppUpdateChecker.shouldNotifyUpdate("1.9.0", 42L, "1.8.1", 43L, null))
        assertTrue(AppUpdateChecker.shouldNotifyUpdate("1.8.1", 43L, "1.8.1-beta4", 43L, null))
        assertTrue(AppUpdateChecker.shouldNotifyUpdate("1.8.1-beta4", 43L, "1.8.1-beta1", 43L, null))
        assertFalse(AppUpdateChecker.shouldNotifyUpdate("1.8.1-beta2", 43L, "1.8.1-beta4", 43L, null))
        assertFalse(AppUpdateChecker.shouldNotifyUpdate("1.8.2", 44L, "1.8.1", 43L, "1.8.2"))
        assertTrue(AppUpdateChecker.shouldNotifyUpdate("1.8.2", 44L, "1.8.1", 43L, "1.8.2", mandatory = true))
    }

    @Test
    fun `release checks are throttled to once per day`() {
        assertTrue(AppUpdateChecker.shouldCheckNow(0L, 100L))
        assertFalse(AppUpdateChecker.shouldCheckNow(10_000L, 10_000L + AppUpdateChecker.CHECK_INTERVAL_MS - 1))
        assertTrue(AppUpdateChecker.shouldCheckNow(10_000L, 10_000L + AppUpdateChecker.CHECK_INTERVAL_MS))
    }

    @Test
    fun `single signer update requires an installed certificate in candidate lineage`() {
        assertTrue(
            AppUpdateChecker.signingIdentityCompatible(
                installedHasMultipleSigners = false,
                installedCurrentSigners = setOf("old"),
                candidateHasMultipleSigners = false,
                candidateCurrentSigners = setOf("new"),
                candidateSigningHistory = setOf("old", "new"),
            ),
        )
        assertFalse(
            AppUpdateChecker.signingIdentityCompatible(
                installedHasMultipleSigners = false,
                installedCurrentSigners = setOf("old"),
                candidateHasMultipleSigners = false,
                candidateCurrentSigners = setOf("new"),
                candidateSigningHistory = setOf("new"),
            ),
        )
    }

    @Test
    fun `multi signer update requires the exact current signer set`() {
        assertTrue(
            AppUpdateChecker.signingIdentityCompatible(
                installedHasMultipleSigners = true,
                installedCurrentSigners = setOf("a", "b"),
                candidateHasMultipleSigners = true,
                candidateCurrentSigners = setOf("a", "b"),
                candidateSigningHistory = emptySet(),
            ),
        )
        assertFalse(
            AppUpdateChecker.signingIdentityCompatible(
                installedHasMultipleSigners = true,
                installedCurrentSigners = setOf("a", "b"),
                candidateHasMultipleSigners = true,
                candidateCurrentSigners = setOf("a"),
                candidateSigningHistory = setOf("a", "b"),
            ),
        )
    }

    @Test
    fun `installed match requires exact code name and compatible package signing identity`() {
        assertTrue(AppUpdateChecker.installedVersionMetadataMatches(43L, "1.8.1", 43L, "1.8.1", true))
        assertFalse(AppUpdateChecker.installedVersionMetadataMatches(43L, "1.8.1-beta4", 43L, "1.8.1", true))
        assertFalse(AppUpdateChecker.installedVersionMetadataMatches(43L, "1.8.1", 43L, "1.8.1", false))
        assertFalse(AppUpdateChecker.installedVersionMetadataMatches(43L, "1.8.1", 44L, "1.8.2", true))
    }

    private fun asset(
        name: String = "Zotify.Music.apk",
        url: String = "https://github.com/OPZENITSUUU/zotify-music/releases/download/v2/$name",
        digestValue: String? = "sha256:$digest",
    ): String {
        val digestPart = digestValue?.let { "\"digest\":\"$it\"," }.orEmpty()
        return """{"name":"$name","browser_download_url":"$url",${digestPart}"state":"uploaded"}"""
    }

    private fun release(vararg assets: String): JsonObject =
        Json.parseToJsonElement("""{"assets":[${assets.joinToString(",")}] }""") as JsonObject
}
