package com.viifo.videocompress

import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4

import org.junit.Test
import org.junit.runner.RunWith

import org.junit.Assert.*

/**
 * Instrumented test, which will execute on an Android device.
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
@RunWith(AndroidJUnit4::class)
class ExampleInstrumentedTest {
    @Test
    fun useAppContext() {
        // Context of the app under test.
//        val appContext = InstrumentationRegistry.getInstrumentation().targetContext
//        assertEquals("com.viifo.videocompress", appContext.packageName)

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