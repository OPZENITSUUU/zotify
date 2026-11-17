package com.music.bitchord.data

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.json.JSONObject
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class OwnerContactClientTest {
    private lateinit var server: MockWebServer

    @Before
    fun setUp() {
        server = MockWebServer()
        server.start()
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    @Test
    fun sendsCategoryAndMessageToBackend() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(202).setBody("""{"ok":true}"""))
        val client = OwnerContactClient(server.url("/api/contact").toString(), OkHttpClient())

        val result = client.send("bug_report", "Playback stops after 10 seconds")

        assertTrue(result.delivered)
        assertEquals(null, result.failure)
        val request = server.takeRequest()
        assertEquals("POST", request.method)
        assertEquals("/api/contact", request.path)
        val body = JSONObject(request.body.readUtf8())
        assertEquals("bug_report", body.getString("type"))
        assertEquals("Playback stops after 10 seconds", body.getString("message"))
    }

    @Test
    fun reportsMissingServerConfiguration() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(503))
        val client = OwnerContactClient(server.url("/api/contact").toString(), OkHttpClient())

        val result = client.send("feedback", "Nice app")

        assertFalse(result.delivered)
        assertEquals(OwnerContactClient.Failure.NOT_CONFIGURED, result.failure)
    }
}
