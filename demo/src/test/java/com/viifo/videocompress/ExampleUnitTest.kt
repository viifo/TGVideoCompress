package com.viifo.videocompress

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class ExampleUnitTest {
    @Test
    fun addition_isCorrect() {
        // assertEquals(4, 2 + 2)
        val i: Long = -1
        if (i > 0) {
            println("i > 0")
        } else {
            println("i < 0")
        }
        if (i > 0L) {
            println("i > 0L")
        } else {
            println("i < 0L")
        }
    }
}