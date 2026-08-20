package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertTrue
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
    // Der Name wurde beim Umbau auf "Mikes 420 Grindhouse" geaendert; die Erwartung blieb auf
    // dem alten Wert stehen. Aufgefallen ist es nie, weil dieser Test unter Java 17 schon beim
    // Aufbau der Robolectric-Sandbox abbrach und der Vergleich nie ausgefuehrt wurde.
    //
    // Die Vollversion haengt " Full" an, damit beide Fassungen auf einem Geraet unterscheidbar
    // sind — deshalb wird der gemeinsame Stamm geprueft und nicht der ganze String.
    assertTrue(
      "Unerwarteter App-Name: $appName",
      appName.startsWith("Mikes 420 Grindhouse")
    )
  }
}
