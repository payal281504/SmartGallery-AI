package com.example.domain

import android.content.ContentUris
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.RectF
import android.net.Uri
import android.os.Build
import android.provider.MediaStore
import android.util.Log
import com.example.data.local.MediaDao
import com.example.data.local.PreferencesManager
import com.example.data.model.MediaItemEntity
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

class MediaScanner(
    private val context: Context,
    private val mediaDao: MediaDao,
    private val preferencesManager: PreferencesManager
) {
    companion object {
        private const val TAG = "MediaScanner"
    }

    suspend fun scanDeviceMedia(forceRescan: Boolean = false): Int = withContext(Dispatchers.IO) {
        val existingCount = mediaDao.getCount()
        val mediaList = mutableListOf<MediaItemEntity>()

        try {
            // 1. Scan Photos
            val imageProjection = arrayOf(
                MediaStore.Images.Media._ID,
                MediaStore.Images.Media.DISPLAY_NAME,
                MediaStore.Images.Media.MIME_TYPE,
                MediaStore.Images.Media.SIZE,
                MediaStore.Images.Media.DATE_ADDED,
                MediaStore.Images.Media.DATE_MODIFIED,
                MediaStore.Images.Media.WIDTH,
                MediaStore.Images.Media.HEIGHT,
                MediaStore.Images.Media.DATA
            )

            context.contentResolver.query(
                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                imageProjection,
                null,
                null,
                "${MediaStore.Images.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.SIZE)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_ADDED)
                val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.DATE_MODIFIED)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Images.Media.HEIGHT)
                val dataCol = cursor.getColumnIndex(MediaStore.Images.Media.DATA)

                while (cursor.moveToNext()) {
                    val mediaId = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, mediaId).toString()
                    val name = cursor.getString(nameCol) ?: "Image_$mediaId.jpg"
                    val mime = cursor.getString(mimeCol) ?: "image/jpeg"
                    val size = cursor.getLong(sizeCol)
                    val dateAdded = cursor.getLong(dateAddedCol) * 1000L
                    val dateMod = cursor.getLong(dateModCol) * 1000L
                    val width = cursor.getInt(widthCol)
                    val height = cursor.getInt(heightCol)
                    val path = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""

                    val sourceAlbum = AiCategorizer.detectSourceAlbum(path, mime, false)
                    val aiResult = AiCategorizer.analyzeImage(
                        context = context,
                        uri = Uri.parse(contentUri),
                        path = path,
                        displayName = name,
                        sourceAlbum = sourceAlbum
                    )

                    mediaList.add(
                        MediaItemEntity(
                            mediaStoreId = mediaId,
                            uri = contentUri,
                            path = path,
                            displayName = name,
                            mimeType = mime,
                            size = size,
                            dateTaken = if (dateAdded > 0) dateAdded else System.currentTimeMillis(),
                            dateModified = if (dateMod > 0) dateMod else System.currentTimeMillis(),
                            width = width,
                            height = height,
                            durationMs = 0,
                            isVideo = false,
                            isDocument = false,
                            sourceAlbum = sourceAlbum,
                            aiCategory = aiResult.category,
                            perceptualHash = aiResult.perceptualHash,
                            checksum = aiResult.checksum,
                            detectedFaces = aiResult.detectedFaces,
                            personName = aiResult.suggestedPersonName,
                            extractedText = aiResult.extractedKeywords
                        )
                    )
                }
            }

            // 2. Scan Videos
            val videoProjection = arrayOf(
                MediaStore.Video.Media._ID,
                MediaStore.Video.Media.DISPLAY_NAME,
                MediaStore.Video.Media.MIME_TYPE,
                MediaStore.Video.Media.SIZE,
                MediaStore.Video.Media.DATE_ADDED,
                MediaStore.Video.Media.DATE_MODIFIED,
                MediaStore.Video.Media.WIDTH,
                MediaStore.Video.Media.HEIGHT,
                MediaStore.Video.Media.DURATION,
                MediaStore.Video.Media.DATA
            )

            context.contentResolver.query(
                MediaStore.Video.Media.EXTERNAL_CONTENT_URI,
                videoProjection,
                null,
                null,
                "${MediaStore.Video.Media.DATE_ADDED} DESC"
            )?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.SIZE)
                val dateAddedCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_ADDED)
                val dateModCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DATE_MODIFIED)
                val widthCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.WIDTH)
                val heightCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.HEIGHT)
                val durCol = cursor.getColumnIndexOrThrow(MediaStore.Video.Media.DURATION)
                val dataCol = cursor.getColumnIndex(MediaStore.Video.Media.DATA)

                while (cursor.moveToNext()) {
                    val mediaId = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, mediaId).toString()
                    val name = cursor.getString(nameCol) ?: "Video_$mediaId.mp4"
                    val mime = cursor.getString(mimeCol) ?: "video/mp4"
                    val size = cursor.getLong(sizeCol)
                    val dateAdded = cursor.getLong(dateAddedCol) * 1000L
                    val dateMod = cursor.getLong(dateModCol) * 1000L
                    val width = cursor.getInt(widthCol)
                    val height = cursor.getInt(heightCol)
                    val duration = cursor.getLong(durCol)
                    val path = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""

                    val sourceAlbum = AiCategorizer.detectSourceAlbum(path, mime, true)

                    mediaList.add(
                        MediaItemEntity(
                            mediaStoreId = mediaId,
                            uri = contentUri,
                            path = path,
                            displayName = name,
                            mimeType = mime,
                            size = size,
                            dateTaken = if (dateAdded > 0) dateAdded else System.currentTimeMillis(),
                            dateModified = if (dateMod > 0) dateMod else System.currentTimeMillis(),
                            width = width,
                            height = height,
                            durationMs = duration,
                            isVideo = true,
                            isDocument = false,
                            sourceAlbum = sourceAlbum,
                            aiCategory = "Videos",
                            checksum = "$size-$duration"
                        )
                    )
                }
            }

            // 3. Scan Documents
            val filesUri = MediaStore.Files.getContentUri("external")
            val docSelection = "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                    "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                    "${MediaStore.Files.FileColumns.MIME_TYPE} LIKE ? OR " +
                    "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ? OR " +
                    "${MediaStore.Files.FileColumns.DISPLAY_NAME} LIKE ?"
            val docArgs = arrayOf("application/pdf", "text/plain", "application/msword", "%.pdf", "%.docx")

            val docProjection = arrayOf(
                MediaStore.Files.FileColumns._ID,
                MediaStore.Files.FileColumns.DISPLAY_NAME,
                MediaStore.Files.FileColumns.MIME_TYPE,
                MediaStore.Files.FileColumns.SIZE,
                MediaStore.Files.FileColumns.DATE_ADDED,
                MediaStore.Files.FileColumns.DATA
            )

            context.contentResolver.query(filesUri, docProjection, docSelection, docArgs, null)?.use { cursor ->
                val idCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns._ID)
                val nameCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DISPLAY_NAME)
                val mimeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.MIME_TYPE)
                val sizeCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.SIZE)
                val dateCol = cursor.getColumnIndexOrThrow(MediaStore.Files.FileColumns.DATE_ADDED)
                val dataCol = cursor.getColumnIndex(MediaStore.Files.FileColumns.DATA)

                while (cursor.moveToNext()) {
                    val fileId = cursor.getLong(idCol)
                    val contentUri = ContentUris.withAppendedId(filesUri, fileId).toString()
                    val name = cursor.getString(nameCol) ?: "Document_$fileId"
                    val mime = cursor.getString(mimeCol) ?: "application/pdf"
                    val size = cursor.getLong(sizeCol)
                    val dateAdded = cursor.getLong(dateCol) * 1000L
                    val path = if (dataCol >= 0) cursor.getString(dataCol) ?: "" else ""

                    mediaList.add(
                        MediaItemEntity(
                            mediaStoreId = fileId,
                            uri = contentUri,
                            path = path,
                            displayName = name,
                            mimeType = mime,
                            size = size,
                            dateTaken = if (dateAdded > 0) dateAdded else System.currentTimeMillis(),
                            isVideo = false,
                            isDocument = true,
                            sourceAlbum = "Documents",
                            aiCategory = "Documents and Papers",
                            extractedText = "document receipt agreement contract invoice $name"
                        )
                    )
                }
            }

        } catch (e: Exception) {
            Log.e(TAG, "Error querying MediaStore: ${e.message}")
        }

        // If device has no media (e.g. streaming emulator / fresh test container), seed rich realistic media
        if (mediaList.isEmpty() && existingCount == 0) {
            Log.d(TAG, "No device media found; generating rich demonstration library")
            val sampleItems = generateSampleMedia()
            mediaList.addAll(sampleItems)
        }

        if (mediaList.isNotEmpty()) {
            mediaDao.insertAll(mediaList)
        }

        preferencesManager.setLastScanTime(System.currentTimeMillis())
        mediaList.size
    }

    /**
     * Creates realistic visual sample media items with procedural high-res bitmaps saved to app storage
     */
    private fun generateSampleMedia(): List<MediaItemEntity> {
        val sampleDir = File(context.filesDir, "sample_media").apply { mkdirs() }
        val now = System.currentTimeMillis()
        val dayMs = 86_400_000L

        data class SampleSpec(
            val fileName: String,
            val mime: String,
            val sourceAlbum: String,
            val aiCategory: String,
            val title: String,
            val subTitle: String,
            val bgGradientStart: Int,
            val bgGradientEnd: Int,
            val isVideo: Boolean = false,
            val isDoc: Boolean = false,
            val durationMs: Long = 0,
            val size: Long,
            val daysAgo: Long,
            val faces: Int = 0,
            val person: String? = null,
            val isFav: Boolean = false,
            val duplicateChecksum: String = "",
            val pHash: Long = 0L,
            val keywords: String = ""
        )

        val specs = listOf(
            SampleSpec(
                fileName = "IMG_2026_KYOTO_SUNSET.jpg",
                mime = "image/jpeg",
                sourceAlbum = "Camera Photos",
                aiCategory = "Nature and Landscapes",
                title = "Kyoto Golden Sunset",
                subTitle = "Arashiyama Mountains, Japan",
                bgGradientStart = Color.rgb(249, 115, 22),
                bgGradientEnd = Color.rgb(124, 58, 237),
                size = 3_450_000L,
                daysAgo = 0,
                isFav = true,
                pHash = 0x2A55AA55AA55AA55L,
                keywords = "sunset kyoto mountain japan nature sky orange violet clouds"
            ),
            SampleSpec(
                fileName = "IMG_2026_KYOTO_SUNSET_COPY.jpg",
                mime = "image/jpeg",
                sourceAlbum = "Camera Photos",
                aiCategory = "Nature and Landscapes",
                title = "Kyoto Golden Sunset (Duplicate)",
                subTitle = "Exact Copy for Cleaner Demo",
                bgGradientStart = Color.rgb(249, 115, 22),
                bgGradientEnd = Color.rgb(124, 58, 237),
                size = 3_450_000L,
                daysAgo = 0,
                duplicateChecksum = "md5_kyoto_sunset_exact_hash_1",
                pHash = 0x2A55AA55AA55AA55L,
                keywords = "sunset kyoto mountain duplicate nature"
            ),
            SampleSpec(
                fileName = "IMG_2026_SARAH_PORTRAIT.jpg",
                mime = "image/jpeg",
                sourceAlbum = "Camera Photos",
                aiCategory = "People",
                title = "Sarah Summer Portrait",
                subTitle = "Tokyo Garden - 50mm f/1.4",
                bgGradientStart = Color.rgb(236, 72, 153),
                bgGradientEnd = Color.rgb(244, 63, 94),
                size = 2_890_000L,
                daysAgo = 1,
                faces = 1,
                person = "Sarah Miller",
                isFav = true,
                pHash = 0x1122334455667788L,
                keywords = "sarah person portrait woman smile summer eyes"
            ),
            SampleSpec(
                fileName = "IMG_2026_SARAH_COFFEE.jpg",
                mime = "image/jpeg",
                sourceAlbum = "Instagram Photos",
                aiCategory = "People",
                title = "Sarah at Café Rost",
                subTitle = "Matcha Latte & Book",
                bgGradientStart = Color.rgb(236, 72, 153),
                bgGradientEnd = Color.rgb(251, 146, 60),
                size = 1_920_000L,
                daysAgo = 2,
                faces = 1,
                person = "Sarah Miller",
                pHash = 0x1122334455667789L, // Very close pHash for similar photo detection!
                keywords = "sarah coffee cafe matcha latte book portrait instagram"
            ),
            SampleSpec(
                fileName = "IMG_2026_FRIENDS_BEACH.jpg",
                mime = "image/jpeg",
                sourceAlbum = "WhatsApp Images",
                aiCategory = "Group Photos",
                title = "Annual Beach Reunion",
                subTitle = "Alex, Sarah, David & Maya",
                bgGradientStart = Color.rgb(6, 182, 212),
                bgGradientEnd = Color.rgb(59, 130, 246),
                size = 4_100_000L,
                daysAgo = 3,
                faces = 4,
                person = "Friends & Group",
                isFav = true,
                keywords = "beach friends reunion group sea vacation celebration"
            ),
            SampleSpec(
                fileName = "IMG_2026_ALEX_HIKE.jpg",
                mime = "image/jpeg",
                sourceAlbum = "Camera Photos",
                aiCategory = "People",
                title = "Alex Summit Hike",
                subTitle = "Mt. Rainier Peak",
                bgGradientStart = Color.rgb(16, 185, 129),
                bgGradientEnd = Color.rgb(5, 150, 105),
                size = 3_120_000L,
                daysAgo = 4,
                faces = 1,
                person = "Alex Chen",
                keywords = "alex chen hike mountain summit climb outdoors"
            ),
            SampleSpec(
                fileName = "IMG_2026_ALIS_TACOS.jpg",
                mime = "image/jpeg",
                sourceAlbum = "Instagram Photos",
                aiCategory = "Food and Drinks",
                title = "Artisan Birria Tacos",
                subTitle = "Oaxaca Street Kitchen",
                bgGradientStart = Color.rgb(239, 68, 68),
                bgGradientEnd = Color.rgb(245, 158, 11),
                size = 2_150_000L,
                daysAgo = 5,
                keywords = "tacos food dinner birria mexican delicious meal"
            ),
            SampleSpec(
                fileName = "IMG_2026_TESLA_CYBERTRUCK.jpg",
                mime = "image/jpeg",
                sourceAlbum = "Downloads",
                aiCategory = "Cars and Bikes",
                title = "Electric Supercar Concept",
                subTitle = "Neon Cyberpunk Edition",
                bgGradientStart = Color.rgb(99, 102, 241),
                bgGradientEnd = Color.rgb(168, 85, 247),
                size = 5_600_000L,
                daysAgo = 6,
                keywords = "car electric vehicle supercar speed neon cyberpunk"
            ),
            SampleSpec(
                fileName = "IMG_2026_NEBULA_WALLPAPER.jpg",
                mime = "image/jpeg",
                sourceAlbum = "Downloads",
                aiCategory = "Wallpapers",
                title = "Deep Space Nebula 8K",
                subTitle = "Cosmic Dust and Stars",
                bgGradientStart = Color.rgb(15, 23, 42),
                bgGradientEnd = Color.rgb(88, 28, 135),
                size = 62_400_000L, // Large file for cleaner (> 50MB)
                daysAgo = 35, // Old download (> 30 days)
                keywords = "wallpaper nebula space stars cosmic deep dark 8k"
            ),
            SampleSpec(
                fileName = "SCREENSHOT_2026_FLIGHT_BOOKING.png",
                mime = "image/png",
                sourceAlbum = "Screenshots",
                aiCategory = "Documents and Papers",
                title = "Tokyo Flight Boarding Pass",
                subTitle = "Gate 44B - Seat 12A Confirmation",
                bgGradientStart = Color.rgb(30, 41, 59),
                bgGradientEnd = Color.rgb(51, 65, 85),
                size = 850_000L,
                daysAgo = 7,
                keywords = "screenshot flight boarding pass gate ticket booking airline"
            ),
            SampleSpec(
                fileName = "RECEIPT_2026_APPLE_STORE.jpg",
                mime = "image/jpeg",
                sourceAlbum = "Screenshots",
                aiCategory = "Receipts and Bills",
                title = "Hardware Store Official Receipt",
                subTitle = "Order #99281 - Total $1,249.00",
                bgGradientStart = Color.rgb(241, 245, 249),
                bgGradientEnd = Color.rgb(226, 232, 240),
                size = 450_000L,
                daysAgo = 8,
                keywords = "receipt bill invoice apple store tax hardware payment total"
            ),
            SampleSpec(
                fileName = "DOC_2026_APARTMENT_LEASE.pdf",
                mime = "application/pdf",
                sourceAlbum = "Documents",
                aiCategory = "Documents and Papers",
                title = "Residential Lease Agreement",
                subTitle = "Terms, Signatures & Floor Plan (PDF)",
                bgGradientStart = Color.rgb(220, 38, 38),
                bgGradientEnd = Color.rgb(185, 28, 28),
                size = 1_840_000L,
                daysAgo = 9,
                isDoc = true,
                keywords = "pdf document lease contract legal agreement apartment signatures"
            ),
            SampleSpec(
                fileName = "DOC_2026_Q4_FINANCIAL_REPORT.xlsx",
                mime = "application/vnd.ms-excel",
                sourceAlbum = "Documents",
                aiCategory = "Documents and Papers",
                title = "Q4 Annual Financial Model",
                subTitle = "Revenue Forecast & KPI Spreadsheets",
                bgGradientStart = Color.rgb(22, 163, 74),
                bgGradientEnd = Color.rgb(21, 128, 61),
                size = 3_200_000L,
                daysAgo = 10,
                isDoc = true,
                keywords = "spreadsheet excel financial revenue q4 balance model profit"
            ),
            SampleSpec(
                fileName = "VID_2026_SURFING_MAUI.mp4",
                mime = "video/mp4",
                sourceAlbum = "Camera Videos",
                aiCategory = "Videos",
                title = "Maui High Tide Surfing",
                subTitle = "4K 60FPS Action Recording",
                bgGradientStart = Color.rgb(14, 116, 144),
                bgGradientEnd = Color.rgb(3, 105, 161),
                isVideo = true,
                durationMs = 45_000L,
                size = 78_000_000L, // Large video (> 50MB)
                daysAgo = 11,
                isFav = true,
                keywords = "video surfing ocean waves action maui hawaii water"
            ),
            SampleSpec(
                fileName = "VID_2026_INSTAGRAM_REEL_COOKING.mp4",
                mime = "video/mp4",
                sourceAlbum = "Instagram Videos and Reels",
                aiCategory = "Videos",
                title = "Handmade Pasta Masterclass",
                subTitle = "Trending Reel - 1080x1920",
                bgGradientStart = Color.rgb(217, 70, 239),
                bgGradientEnd = Color.rgb(168, 85, 247),
                isVideo = true,
                durationMs = 28_000L,
                size = 18_400_000L,
                daysAgo = 12,
                keywords = "instagram reel video cooking pasta food recipe kitchen"
            ),
            SampleSpec(
                fileName = "VID_2026_WHATSAPP_FAMILY_GREETING.mp4",
                mime = "video/mp4",
                sourceAlbum = "WhatsApp Videos",
                aiCategory = "Videos",
                title = "Grandma Birthday Toast",
                subTitle = "Family Reunion Video",
                bgGradientStart = Color.rgb(34, 197, 94),
                bgGradientEnd = Color.rgb(16, 185, 129),
                isVideo = true,
                durationMs = 62_000L,
                size = 24_500_000L,
                daysAgo = 14,
                keywords = "whatsapp video birthday grandma family celebration greeting"
            )
        )

        val items = mutableListOf<MediaItemEntity>()

        for (spec in specs) {
            val file = File(sampleDir, spec.fileName)
            if (!file.exists()) {
                // Generate high quality canvas bitmap
                val width = if (spec.isVideo || spec.isDoc) 1080 else 1200
                val height = if (spec.isVideo) 1920 else 1200
                val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
                val canvas = Canvas(bitmap)

                // Background gradient / solid
                val paint = Paint(Paint.ANTI_ALIAS_FLAG)
                val shader = android.graphics.LinearGradient(
                    0f, 0f, width.toFloat(), height.toFloat(),
                    spec.bgGradientStart, spec.bgGradientEnd,
                    android.graphics.Shader.TileMode.CLAMP
                )
                paint.shader = shader
                canvas.drawRect(0f, 0f, width.toFloat(), height.toFloat(), paint)

                // Decorative graphic shapes
                paint.shader = null
                paint.color = Color.argb(40, 255, 255, 255)
                canvas.drawCircle(width * 0.8f, height * 0.2f, width * 0.35f, paint)
                canvas.drawCircle(width * 0.2f, height * 0.8f, width * 0.25f, paint)

                // Draw central card/icon container
                paint.color = Color.argb(70, 0, 0, 0)
                val cardRect = RectF(width * 0.1f, height * 0.35f, width * 0.9f, height * 0.65f)
                canvas.drawRoundRect(cardRect, 48f, 48f, paint)

                // Draw title and category text
                val textPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
                    color = Color.WHITE
                    textSize = 56f
                    isFakeBoldText = true
                    textAlign = Paint.Align.CENTER
                }
                canvas.drawText(spec.title, width / 2f, height * 0.48f, textPaint)

                textPaint.textSize = 34f
                textPaint.isFakeBoldText = false
                textPaint.color = Color.argb(220, 255, 255, 255)
                canvas.drawText(spec.subTitle, width / 2f, height * 0.54f, textPaint)

                // Badge
                textPaint.textSize = 28f
                textPaint.color = Color.argb(190, 255, 255, 255)
                canvas.drawText("AURA SMART GALLERY  •  ${spec.aiCategory.uppercase()}", width / 2f, height * 0.59f, textPaint)

                FileOutputStream(file).use { out ->
                    bitmap.compress(Bitmap.CompressFormat.JPEG, 92, out)
                }
                bitmap.recycle()
            }

            val fileUri = Uri.fromFile(file).toString()
            val checksum = if (spec.duplicateChecksum.isNotEmpty()) spec.duplicateChecksum else "sample_chk_${spec.fileName.hashCode()}"
            val pHash = if (spec.pHash != 0L) spec.pHash else (spec.fileName.hashCode().toLong() and 0xFFFFFFFFL)

            items.add(
                MediaItemEntity(
                    mediaStoreId = Math.abs(spec.fileName.hashCode().toLong()),
                    uri = fileUri,
                    path = file.absolutePath,
                    displayName = spec.fileName,
                    mimeType = spec.mime,
                    size = spec.size,
                    dateTaken = now - (spec.daysAgo * dayMs),
                    dateModified = now - (spec.daysAgo * dayMs),
                    width = 1200,
                    height = 1200,
                    durationMs = spec.durationMs,
                    isVideo = spec.isVideo,
                    isDocument = spec.isDoc,
                    isFavorite = spec.isFav,
                    sourceAlbum = spec.sourceAlbum,
                    aiCategory = spec.aiCategory,
                    perceptualHash = pHash,
                    checksum = checksum,
                    detectedFaces = spec.faces,
                    personName = spec.person,
                    extractedText = spec.keywords,
                    isSample = true
                )
            )
        }

        return items
    }
}
