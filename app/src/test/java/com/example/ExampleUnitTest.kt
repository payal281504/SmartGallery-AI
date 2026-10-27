package com.example

import com.example.data.local.PreferencesManager
import com.example.data.model.MediaItemEntity
import com.example.domain.AiCategorizer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class ExampleUnitTest {

    @Test
    fun testSourceAlbumDetection() {
        assertEquals(
            "Instagram Photos",
            AiCategorizer.detectSourceAlbum("/storage/emulated/0/Pictures/Instagram/IMG_1.jpg", "image/jpeg", false)
        )
        assertEquals(
            "Instagram Videos and Reels",
            AiCategorizer.detectSourceAlbum("/storage/emulated/0/Movies/Instagram/VID_1.mp4", "video/mp4", true)
        )
        assertEquals(
            "WhatsApp Images",
            AiCategorizer.detectSourceAlbum("/storage/emulated/0/Android/media/com.whatsapp/WhatsApp/Media/WhatsApp Images/IMG.jpg", "image/jpeg", false)
        )
        assertEquals(
            "Screenshots",
            AiCategorizer.detectSourceAlbum("/storage/emulated/0/Pictures/Screenshots/Screenshot_1.png", "image/png", false)
        )
        assertEquals(
            "Camera Photos",
            AiCategorizer.detectSourceAlbum("/storage/emulated/0/DCIM/Camera/IMG_2026.jpg", "image/jpeg", false)
        )
        assertEquals(
            "Downloads",
            AiCategorizer.detectSourceAlbum("/storage/emulated/0/Download/document_receipt.jpg", "image/jpeg", false)
        )
    }

    @Test
    fun testHammingDistance() {
        val hash1 = 0x2AAAAAAAAAAAAAAAL
        val hash2 = 0x2AAAAAAAAAAAAAAAL
        assertEquals(0, AiCategorizer.hammingDistance(hash1, hash2))

        val hash3 = 0x2AAAAAAAAAAAAAABL // 1 bit differs
        assertEquals(1, AiCategorizer.hammingDistance(hash1, hash3))

        val hash4 = 0x1555555555555555L
        val dist = AiCategorizer.hammingDistance(hash1, hash4)
        assertNotEquals(0, dist)
    }

    @Test
    fun testPinHashing() {
        val hash1 = PreferencesManager.hashPin("1234")
        val hash2 = PreferencesManager.hashPin("1234")
        val hash3 = PreferencesManager.hashPin("5678")

        assertEquals(hash1, hash2)
        assertNotEquals(hash1, hash3)
        assertEquals(64, hash1.length) // SHA-256 hex string length
    }

    @Test
    fun testMediaItemFormatting() {
        val item = MediaItemEntity(
            uri = "content://test",
            size = 5_242_880L, // 5MB
            durationMs = 125_000L // 2 min 5 sec
        )

        assertEquals("5.0 MB", item.formattedSize())
        assertEquals("2:05", item.formattedDuration())
    }
}
