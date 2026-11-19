package com.music.bitchord.wear

import org.junit.Assert.assertEquals
import org.junit.Test

class WatchInstallerAssetNamesTest {
    @Test
    fun `bundled APK asset names match BundleWearApk output`() {
        assertEquals("ZotifyMusicWear.apk", WatchInstallerAssetNames.APK_FILE)
        assertEquals("ZotifyMusicWear.sha256", WatchInstallerAssetNames.SHA_FILE)
        assertEquals("wear/ZotifyMusicWear.apk", WatchInstallerAssetNames.APK_ASSET)
        assertEquals("wear/ZotifyMusicWear.sha256", WatchInstallerAssetNames.SHA_ASSET)
    }
}
