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
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Samvad Studio", appName)
  }

  @Test
  fun `split hindi dialogue into caption phrases`() {
    val phrases = com.example.model.splitIntoCaptionPhrases(
      "हे लक्ष्मण! कठिन समय हमें तोड़ने नहीं, हमारे भीतर के धर्म को तराशने आता है।"
    )
    org.junit.Assert.assertTrue(phrases.size >= 2)
  }
}
