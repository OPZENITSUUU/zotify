package com.music.bitchord.data

import android.content.Context
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.platform.app.InstrumentationRegistry
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertNotNull
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AppUpdatePersistenceInstrumentedTest {
    private val context: Context
        get() = InstrumentationRegistry.getInstrumentation().targetContext

    private val preferences
        get() = context.getSharedPreferences("bitchord_update_state", Context.MODE_PRIVATE)

    @Before
    fun clearUpdaterState() {
        preferences.edit().clear().commit()
        AppUpdateChecker.restorePending(context)
    }

    @After
    fun cleanUpdaterState() {
        preferences.edit().clear().commit()
        AppUpdateChecker.restorePending(context)
    }

    @Test
    fun optionalUpdateLaterPersistsDismissalAndClearsRestoredPrompt() {
        val update = update(mandatory = false)
        AppUpdateChecker.persistPending(context, update)

        assertEquals(update, AppUpdateChecker.restorePending(context))
        AppUpdateChecker.dismiss(context, update.version)

        assertNull(AppUpdateChecker.restorePending(context))
        assertEquals(update.version, AppUpdateChecker.dismissedVersion(context))
    }

    @Test
    fun mandatoryUpdateCannotBeDismissedOrClearedFromPersistedState() {
        val update = update(mandatory = true)
        AppUpdateChecker.persistPending(context, update)

        assertEquals(update, AppUpdateChecker.restorePending(context))
        AppUpdateChecker.dismiss(context, update.version)

        assertEquals(update, AppUpdateChecker.restorePending(context))
        assertNull(AppUpdateChecker.dismissedVersion(context))
        assertNotNull(AppUpdateChecker.available.value)
    }

    private fun update(mandatory: Boolean) = AppUpdateChecker.UpdateInfo(
        version = "1.0.12",
        releaseUrl = "https://github.com/OPZENITSUUU/zotify-music/releases/tag/v1.0.12",
        apkUrl = "https://github.com/OPZENITSUUU/zotify-music/releases/download/v1.0.12/Zotify.Music.apk",
        apkSha256 = "a".repeat(64),
        notes = "- Stability improvements",
        mandatory = mandatory,
    )
}
