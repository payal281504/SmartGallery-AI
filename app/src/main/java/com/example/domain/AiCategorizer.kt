package com.example.domain

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.media.FaceDetector
import android.net.Uri
import android.util.Log
import java.io.InputStream
import java.security.MessageDigest
import kotlin.math.abs

object AiCategorizer {
    private const val TAG = "AiCategorizer"

    data class AiAnalysisResult(
        val category: String,
        val detectedFaces: Int = 0,
        val perceptualHash: Long = 0L,
        val checksum: String = "",
        val suggestedPersonName: String? = null,
        val extractedKeywords: String = ""
    )

    /**
     * Categorizes source album based on file path, folder name, and MIME type
     */
    fun detectSourceAlbum(path: String, mimeType: String, isVideo: Boolean): String {
        val lowerPath = path.lowercase()
        return when {
            lowerPath.contains("instagram") -> {
                if (isVideo) "Instagram Videos and Reels" else "Instagram Photos"
            }
            lowerPath.contains("whatsapp") -> {
                if (isVideo) "WhatsApp Videos" else "WhatsApp Images"
            }
            lowerPath.contains("telegram") -> "Telegram Media"
            lowerPath.contains("facebook") || lowerPath.contains("messenger") -> "Facebook Media"
            lowerPath.contains("screen_recording") || (isVideo && lowerPath.contains("screenshot")) -> "Screen Recordings"
            lowerPath.contains("screenshot") -> "Screenshots"
            lowerPath.contains("bluetooth") -> "Bluetooth Received Files"
            lowerPath.contains("download") -> "Downloads"
            lowerPath.contains("edit") || lowerPath.contains("snapseed") || lowerPath.contains("lightroom") -> {
                if (isVideo) "Edited Videos" else "Edited Photos"
            }
            lowerPath.contains("dcim") || lowerPath.contains("camera") -> {
                if (isVideo) "Camera Videos" else "Camera Photos"
            }
            isVideo -> "Camera Videos"
            else -> "Camera Photos"
        }
    }

    /**
     * Fast 64-bit difference hash (dHash) for visual similarity matching
     */
    fun computeDHash(bitmap: Bitmap): Long {
        return try {
            val scaled = Bitmap.createScaledBitmap(bitmap, 9, 8, true)
            var hash = 0L
            for (y in 0 until 8) {
                for (x in 0 until 8) {
                    val leftPixel = scaled.getPixel(x, y)
                    val rightPixel = scaled.getPixel(x + 1, y)
                    val leftLum = (leftPixel shr 16 and 0xFF) * 299 + (leftPixel shr 8 and 0xFF) * 587 + (leftPixel and 0xFF) * 114
                    val rightLum = (rightPixel shr 16 and 0xFF) * 299 + (rightPixel shr 8 and 0xFF) * 587 + (rightPixel and 0xFF) * 114
                    if (leftLum < rightLum) {
                        hash = hash or (1L shl (y * 8 + x))
                    }
                }
            }
            if (scaled != bitmap) scaled.recycle()
            hash
        } catch (e: Exception) {
            0L
        }
    }

    /**
     * Compute Hamming distance between two 64-bit hashes
     */
    fun hammingDistance(hash1: Long, hash2: Long): Int {
        var xor = hash1 xor hash2
        var distance = 0
        while (xor != 0L) {
            distance += (xor and 1L).toInt()
            xor = xor ushr 1
        }
        return distance
    }

    /**
     * Fast checksum for exact duplicate detection
     */
    fun computeChecksum(context: Context, uri: Uri): String {
        return try {
            val md = MessageDigest.getInstance("MD5")
            context.contentResolver.openInputStream(uri)?.use { stream ->
                val buffer = ByteArray(8192)
                var bytesRead: Int
                var total = 0
                // Read up to 256KB to keep scanning fast and responsive
                while (stream.read(buffer).also { bytesRead = it } != -1 && total < 256 * 1024) {
                    md.update(buffer, 0, bytesRead)
                    total += bytesRead
                }
            }
            md.digest().fold("") { str, it -> str + "%02x".format(it) }
        } catch (e: Exception) {
            ""
        }
    }

    /**
     * Analyze image bitmap for faces, colors, document text signatures, and categorize
     */
    fun analyzeImage(
        context: Context,
        uri: Uri,
        path: String,
        displayName: String,
        sourceAlbum: String
    ): AiAnalysisResult {
        val lowerName = displayName.lowercase()
        val lowerPath = path.lowercase()

        // Document heuristic check
        if (lowerPath.contains("doc") || lowerName.endsWith(".pdf") || lowerName.endsWith(".docx") || lowerName.endsWith(".txt")) {
            return AiAnalysisResult(category = "Documents and Papers", extractedKeywords = "document pdf page text")
        }
        if (lowerName.contains("receipt") || lowerPath.contains("receipt") || lowerName.contains("invoice") || lowerName.contains("bill")) {
            return AiAnalysisResult(category = "Receipts and Bills", extractedKeywords = "receipt bill invoice payment tax total")
        }
        if (lowerName.contains("card") || lowerPath.contains("card") || lowerName.contains("business")) {
            return AiAnalysisResult(category = "Business Cards", extractedKeywords = "business card contact phone email")
        }
        if (lowerName.contains("note") || lowerPath.contains("note") || lowerName.contains("whiteboard")) {
            return AiAnalysisResult(category = "Notes and Whiteboards", extractedKeywords = "notes memo ideas meeting whiteboard")
        }
        if (lowerName.contains("qr") || lowerName.contains("barcode")) {
            return AiAnalysisResult(category = "QR Codes and Barcodes", extractedKeywords = "qr code scanner barcode link")
        }

        var dHash = 0L
        val checksum = computeChecksum(context, uri)
        var detectedFaceCount = 0
        var category = "Other Images"
        var suggestedPerson: String? = null
        var keywords = ""

        try {
            // Load downsampled bitmap for fast analysis
            val options = BitmapFactory.Options().apply {
                inJustDecodeBounds = true
            }
            context.contentResolver.openInputStream(uri)?.use {
                BitmapFactory.decodeStream(it, null, options)
            }

            val targetSize = 256
            var sampleSize = 1
            if (options.outHeight > targetSize || options.outWidth > targetSize) {
                val halfHeight = options.outHeight / 2
                val halfWidth = options.outWidth / 2
                while (halfHeight / sampleSize >= targetSize && halfWidth / sampleSize >= targetSize) {
                    sampleSize *= 2
                }
            }

            val decodeOptions = BitmapFactory.Options().apply {
                inSampleSize = sampleSize
                inPreferredConfig = Bitmap.Config.RGB_565 // Required for native FaceDetector
            }

            var bitmap: Bitmap? = null
            context.contentResolver.openInputStream(uri)?.use {
                bitmap = BitmapFactory.decodeStream(it, null, decodeOptions)
            }

            bitmap?.let { bmp ->
                dHash = computeDHash(bmp)

                // Face Detection with Android FaceDetector
                // FaceDetector requires even width
                val evenWidth = if (bmp.width % 2 == 0) bmp.width else bmp.width - 1
                val safeBmp = if (evenWidth == bmp.width) bmp else Bitmap.createBitmap(bmp, 0, 0, evenWidth, bmp.height)

                try {
                    val maxFaces = 5
                    val faces = arrayOfNulls<FaceDetector.Face>(maxFaces)
                    val detector = FaceDetector(safeBmp.width, safeBmp.height, maxFaces)
                    detectedFaceCount = detector.findFaces(safeBmp, faces)
                } catch (e: Exception) {
                    Log.w(TAG, "Face detector error: ${e.message}")
                }

                // Color histogram / Feature classification
                var totalR = 0L
                var totalG = 0L
                var totalB = 0L
                var whitePixelCount = 0
                var highContrastCount = 0
                val totalPixels = bmp.width * bmp.height

                val step = 4
                var sampledCount = 0
                for (y in 0 until bmp.height step step) {
                    for (x in 0 until bmp.width step step) {
                        val pixel = bmp.getPixel(x, y)
                        val r = (pixel shr 16) and 0xFF
                        val g = (pixel shr 8) and 0xFF
                        val b = pixel and 0xFF
                        totalR += r
                        totalG += g
                        totalB += b
                        sampledCount++

                        // Document detection: high brightness and high white count
                        if (r > 220 && g > 220 && b > 220) {
                            whitePixelCount++
                        }
                        if (abs(r - g) < 15 && abs(g - b) < 15 && (r < 40 || r > 210)) {
                            highContrastCount++ // B&W text / paper
                        }
                    }
                }

                val avgR = totalR / sampledCount
                val avgG = totalG / sampledCount
                val avgB = totalB / sampledCount
                val whiteRatio = whitePixelCount.toFloat() / sampledCount
                val highContrastRatio = highContrastCount.toFloat() / sampledCount

                // Categorization decision tree
                when {
                    detectedFaceCount == 1 -> {
                        category = if (lowerName.contains("selfie") || (bmp.height > bmp.width * 1.3)) "Selfies" else "People"
                        suggestedPerson = "Person ${(abs(dHash % 4) + 1)}"
                        keywords = "person face portrait smile"
                    }
                    detectedFaceCount > 1 -> {
                        category = "Group Photos"
                        suggestedPerson = "Friends & Group"
                        keywords = "group friends people crowd celebration"
                    }
                    whiteRatio > 0.45 && highContrastRatio > 0.35 -> {
                        category = "Documents and Papers"
                        keywords = "document receipt paper text printed invoice note"
                    }
                    lowerPath.contains("wallpaper") || lowerName.contains("wallpaper") || (bmp.width == 1080 && bmp.height == 1920) -> {
                        category = "Wallpapers"
                        keywords = "wallpaper background lockscreen art digital"
                    }
                    lowerPath.contains("food") || lowerName.contains("food") || (avgR > 140 && avgG > 100 && avgB < 90 && avgR > avgB * 1.4) -> {
                        category = "Food and Drinks"
                        keywords = "food delicious meal restaurant dish coffee"
                    }
                    lowerPath.contains("car") || lowerName.contains("car") || lowerPath.contains("bike") || lowerName.contains("auto") -> {
                        category = "Cars and Bikes"
                        keywords = "car vehicle automobile speed drive motor"
                    }
                    lowerPath.contains("nature") || (avgG > avgR * 1.15 && avgG > avgB * 1.15) || (avgB > avgR * 1.25 && avgG > avgR) -> {
                        category = "Nature and Landscapes"
                        keywords = "nature landscape outdoors sky mountain greenery sunset forest"
                    }
                    lowerPath.contains("meme") || lowerName.contains("meme") -> {
                        category = "Memes and Funny Images"
                        keywords = "meme funny humor joke graphic"
                    }
                    sourceAlbum == "Screenshots" -> {
                        category = "Documents and Papers"
                        keywords = "screenshot screen capture mobile app"
                    }
                    else -> {
                        category = "Travel and Places"
                        keywords = "travel places memory view outdoor photo"
                    }
                }

                if (safeBmp != bmp) safeBmp.recycle()
                bmp.recycle()
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error analyzing image: ${e.message}")
        }

        return AiAnalysisResult(
            category = category,
            detectedFaces = detectedFaceCount,
            perceptualHash = dHash,
            checksum = checksum,
            suggestedPersonName = suggestedPerson,
            extractedKeywords = keywords
        )
    }
}
