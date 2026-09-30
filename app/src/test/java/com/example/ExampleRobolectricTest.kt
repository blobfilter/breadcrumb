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
    assertEquals("Breadcrumb", appName)
  }

  @Test
  fun `validate capture entity creation and single tag constraint`() {
    val entity = com.example.data.model.CaptureEntity(
      itemType = com.example.data.model.ItemType.TEXT,
      content = "Sample tweet content",
      context = "Interesting insight on user onboarding",
      tag = "design"
    )
    assertEquals(com.example.data.model.ItemType.TEXT, entity.itemType)
    assertEquals("Interesting insight on user onboarding", entity.context)
    assertEquals("design", entity.tag)
    org.junit.Assert.assertNotNull(entity.id)
  }
}
