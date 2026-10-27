package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.local.AppDatabase
import com.example.data.model.MediaItemEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

    @Test
    fun `read string from context`() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val appName = context.getString(R.string.app_name)
        assertEquals("Aura Gallery", appName)
    }

    @Test
    fun `test database media insertion and query`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val db = AppDatabase.getInstance(context)
        val dao = db.mediaDao()

        val sampleItem = MediaItemEntity(
            uri = "content://media/external/images/media/9999",
            displayName = "Robolectric_Sunset.jpg",
            sourceAlbum = "Camera Photos",
            aiCategory = "Nature and Landscapes",
            size = 1024 * 1024,
            isFavorite = true
        )

        dao.insert(sampleItem)
        val favorites = dao.getFavorites().first()
        val found = favorites.any { it.displayName == "Robolectric_Sunset.jpg" }
        assertEquals(true, found)
    }
}
