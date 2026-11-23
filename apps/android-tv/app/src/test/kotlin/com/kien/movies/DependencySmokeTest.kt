package com.kien.movies

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** The two pure JVM libraries of slice A12b load and work on the test classpath, not only compile. */
class DependencySmokeTest {
    @Test
    fun zxingEncodesAQrMatrixOfTheAskedSize() {
        val matrix = DependencySmoke.qrMatrix("http://192.168.1.2:8080/?t=abc", size = 180)

        assertEquals(180, matrix.width)
        assertEquals(180, matrix.height)
    }

    @Test
    fun nanoHttpdStartsAndStopsOnAFreePort() {
        val server = DependencySmoke.server(port = 0)

        server.start()
        val listening = server.listeningPort
        server.stop()

        assertTrue(listening > 0)
    }
}
