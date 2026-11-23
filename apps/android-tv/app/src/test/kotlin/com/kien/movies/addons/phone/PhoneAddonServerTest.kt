package com.kien.movies.addons.phone

import com.nuvio.tvos.phone.AddonInstallOutcome
import com.nuvio.tvos.phone.AddonPairingPhase
import java.net.ConnectException
import java.net.HttpURLConnection
import java.net.Socket
import java.net.URI
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

/** The phone server on the JVM, over a real loopback socket (docs/android-tv-rebuild.md, 5b). */
class PhoneAddonServerTest {
    private val installed = mutableListOf<List<String>>()
    private val server = checkNotNull(
        PhoneAddonServer.start(
            host = "127.0.0.1",
            clock = { 0L },
            install = { urls, onDone ->
                installed += urls
                onDone(urls.map { url -> AddonInstallOutcome.Installed(url, "Cinemeta") })
            },
            ports = listOf(0)
        )
    )
    private val base = "http://127.0.0.1:${server.listeningPort}"
    private val token = server.address.substringAfter("?t=")

    @After
    fun stopServer() = server.close()

    @Test
    fun aq4_aProposalWithoutTheTokenIsForbiddenAndChangesNothing() {
        val reply = post("/api/addons", PROPOSAL)

        assertEquals(403, reply.status)
        assertEquals(AddonPairingPhase.Waiting, server.session.value.phase)
    }

    @Test
    fun aq4_aWrongTokenCannotEvenReadThePage() {
        val reply = get("/?t=0000")

        assertEquals(403, reply.status)
    }

    @Test
    fun aq4_aSecondProposalWhileOneIsOpenGets409() {
        post("/api/addons?t=$token", PROPOSAL)

        val second = post("/api/addons?t=$token", """{"urls": ["https://example.org/manifest.json"]}""")

        assertEquals(409, second.status)
    }

    @Test
    fun aq3_proposeThenConfirmInstallsAndThePhoneReadsTheResult() {
        val proposal = post("/api/addons?t=$token", PROPOSAL)
        val id = Regex("\"id\":(\\d+)").find(proposal.body)!!.groupValues[1].toLong()

        server.confirm(id)

        val status = get("/api/status/$id?t=$token")
        assertEquals(200, proposal.status)
        assertEquals(listOf(listOf(MANIFEST)), installed)
        assertEquals("""{"status":"done","results":[{"url":"$MANIFEST","ok":true,"name":"Cinemeta"}]}""", status.body)
    }

    @Test
    fun aBodyDeclaredOver16KbIsRefusedBeforeItIsRead() {
        val head = "POST /api/addons?t=$token HTTP/1.1\r\nHost: 127.0.0.1\r\nContent-Length: 16385\r\n\r\n"

        val statusLine = Socket("127.0.0.1", server.listeningPort).use { socket ->
            socket.soTimeout = TIMEOUT_MS
            socket.getOutputStream().write(head.encodeToByteArray())
            socket.getInputStream().bufferedReader().readLine()
        }

        assertEquals("HTTP/1.1 413", statusLine.take(12))
        assertEquals(AddonPairingPhase.Waiting, server.session.value.phase)
    }

    @Test
    fun aq5_afterCloseTheAddressNoLongerAnswers() {
        server.close()

        assertThrows(ConnectException::class.java) { get("/?t=$token") }
        assertTrue(server.session.value.phase == AddonPairingPhase.Off)
    }

    private data class Reply(val status: Int, val body: String)

    private fun get(path: String): Reply = request(path) {}

    private fun post(path: String, body: String): Reply = request(path) {
        val bytes = body.encodeToByteArray()
        requestMethod = "POST"
        doOutput = true
        setRequestProperty("Content-Type", "application/json")
        setFixedLengthStreamingMode(bytes.size)
        outputStream.use { stream -> stream.write(bytes) }
    }

    private fun request(path: String, configure: HttpURLConnection.() -> Unit): Reply {
        val connection = URI("$base$path").toURL().openConnection() as HttpURLConnection
        connection.connectTimeout = TIMEOUT_MS
        connection.readTimeout = TIMEOUT_MS
        return try {
            connection.configure()
            val status = connection.responseCode
            val stream = if (status < 400) connection.inputStream else connection.errorStream
            Reply(status, stream?.bufferedReader()?.use { reader -> reader.readText() }.orEmpty())
        } finally {
            connection.disconnect()
        }
    }

    private companion object {
        const val MANIFEST = "https://v3-cinemeta.strem.io/manifest.json"
        const val PROPOSAL = """{"urls": ["$MANIFEST"]}"""
        const val TIMEOUT_MS = 5_000
    }
}
