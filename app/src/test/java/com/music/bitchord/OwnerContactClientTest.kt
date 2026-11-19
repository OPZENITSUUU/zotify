package com.music.bitchord.data

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
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
        val body = Json.parseToJsonElement(request.body.readUtf8()).jsonObject
        assertEquals("bug_report", body["type"]?.jsonPrimitive?.content)
        assertEquals("Playback stops after 10 seconds", body["message"]?.jsonPrimitive?.content)
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
