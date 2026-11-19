package com.music.bitchord

import com.music.bitchord.data.updates.UpdateNotificationWorker
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class UpdateNotificationWorkerTest {
    @Test
    fun `background notification is sent once for each available version`() {
        assertTrue(UpdateNotificationWorker.shouldNotifyVersion("1.8.1-beta7", null))
        assertTrue(UpdateNotificationWorker.shouldNotifyVersion("1.8.1-beta7", "1.8.1-beta6"))
        assertFalse(UpdateNotificationWorker.shouldNotifyVersion("1.8.1-beta7", "1.8.1-beta7"))
    }

    @Test
    fun `empty release version never creates an update notification`() {
        assertFalse(UpdateNotificationWorker.shouldNotifyVersion("", null))
        assertFalse(UpdateNotificationWorker.shouldNotifyVersion("   ", null))
    }

    @Test
    fun `mandatory policy change can notify once after an optional notice for the same release`() {
        assertTrue(UpdateNotificationWorker.shouldNotifyVersion("1.8.1:mandatory", "1.8.1"))
        assertFalse(UpdateNotificationWorker.shouldNotifyVersion("1.8.1:mandatory", "1.8.1:mandatory"))
    }
}
