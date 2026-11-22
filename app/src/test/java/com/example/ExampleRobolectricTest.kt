package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

    @Test
    fun `read app strings from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertEquals("Target Tabungan", context.getString(R.string.app_name))
        assertEquals(
            "Selamat Datang di Aplikasi Hermanto",
            context.getString(R.string.welcome_marquee_text)
        )
    }
}
