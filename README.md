# Aura Gallery — AI-Powered Smart Android Gallery

A complete, production-ready, intelligent Android Gallery application that automatically scans, indexes, categorizes, and manages all photos, videos, downloads, screenshots, and documents on the device.

## Core Features

### 1. Smart Automatic Media Organization
- **Source-Based Albums**: Detects origin apps using folder paths and MediaStore metadata (Instagram Photos/Reels, WhatsApp Images/Videos, Telegram, Facebook, Camera Photos/Videos, Downloads, Screenshots, Screen Recordings, Bluetooth, Edited).
- **AI Content Collections**: Automatically categorizes images into smart collections:
  - People & Portraits
  - Friends & Group Photos
  - Selfies
  - Nature & Landscapes
  - Travel & Places
  - Food & Drinks
  - Cars & Vehicles
  - Wallpapers
  - Documents & Papers
  - Receipts & Bills
  - Memes & Funny Images

### 2. Intelligent People & Face Recognition
- Built-in on-device face detector (`android.media.FaceDetector`).
- Clusters recurring faces into named or suggested person groups ("Sarah Miller", "Alex Chen", etc.).
- Allows naming, renaming, and viewing all photos containing a specific person.
- 100% on-device and private: no face data is ever transmitted to the cloud.

### 3. Advanced Video & Photo Experience
- Fullscreen media viewer with pinch-to-zoom, pan, and double-tap zoom.
- Video playback controls with seek bar, duration indicators, and play/pause.
- Slideshow mode with auto-advancing timer.
- EXIF & Metadata inspector sheet (dimensions, resolution, format, date, checksum, perceptual hash, AI keywords).
- Quick image manipulation: 90° rotation, file renaming, system sharing, moving to Vault, or Trash.

### 4. Smart Document Organizer
- Dedicated Documents desk for indexing PDF files, Word docs, spreadsheets, receipts, scanned papers, and notes.
- Quick keyword search over document text and filenames.

### 5. Storage Cleaner & Duplicate Finder
- **Exact Duplicates**: Detects identical copies using MD5/CRC checksums and offers one-tap cleanup to reclaim storage while preserving the original.
- **Similar Photos**: Uses 64-bit perceptual difference hashing (dHash) to group visually similar burst shots and variations.
- **Large Files**: Highlights files larger than 50MB.
- **Trash Bin**: Safe trash bin with batch restore and permanent deletion.

### 6. Private Vault
- Confidential media storage protected by a 4-digit PIN (default: `1234`).
- Secure SHA-256 PIN verification.
- Easily move photos and videos to and from the Vault.
- Option to change the security PIN anytime.

### 7. Customization & Settings
- Toggle AI categorization and face detection.
- Select grid density (2, 3, or 4 columns).
- Color themes: System, Dark, and Light modes.
- Manual re-sync & scan button.

## Architecture & Technology Stack
- **Framework**: Native Android with Kotlin and Jetpack Compose (Material 3).
- **State Management**: MVVM with Kotlin Coroutines and StateFlow.
- **Local Persistence**: Room Database for indexed media metadata, albums, and duplicate signatures.
- **Preferences**: Jetpack DataStore Preferences for user settings and PIN hashes.
- **Image Loading**: Coil (`io.coil-kt:coil-compose`).
- **Permissions**: Granular handling for Android 13+ (`READ_MEDIA_IMAGES`, `READ_MEDIA_VIDEO`), Android 14+ (`READ_MEDIA_VISUAL_USER_SELECTED`), and Android 12 & below (`READ_EXTERNAL_STORAGE`).
