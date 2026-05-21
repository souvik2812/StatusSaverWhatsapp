# StatusSaverWhatsapp 📱✨

A modern, native Android application built with **Jetpack Compose** and **Material 3** to seamlessly view, play, and save WhatsApp statuses (images and videos) directly to your device's gallery.

---

## 🌟 Key Features

- **🎨 Multi-Color Theme System**: Customize the app with 8 distinct color palettes (Violet, Red, Orange, Blue, Yellow, Green, Pink, and Dark Green) accessible directly from the top bar.
- **🌗 Dynamic Dark & Light Modes**: Toggle between light and dark modes instantly, with configurations persisting across app restarts (powered by `DataStore`).
- **🖼 Gallery-Style Media Viewer**: Open any media to enter a full-screen gallery view. Swipe left and right smoothly through statuses just like in a native photo gallery.
- **🎬 Native Video Playback**: Integrated `ExoPlayer` for lag-free, high-performance video playing within the app.
- **📥 One-Click Saves**: Save status files to your public storage. Images are stored in `Pictures/StatusSaver/` and videos in `Downloads/StatusSaver/`.
- **🔒 Secure Storage Access**: Utilizes the modern Android **Storage Access Framework (SAF)**, avoiding legacy storage permissions and ensuring strict privacy compliance.

---

## 🛠 Tech Stack & Architecture

The project is structured following clean architecture principles and Android development best practices:

- **UI Framework**: [Jetpack Compose](https://developer.android.com/jetpack/compose) for a fully declarative and modern UI.
- **Design System**: [Material 3](https://developer.android.com/jetpack/compose/designsystems/material3) supporting dynamic color schemes.
- **Image/Video Loading**: [Coil](https://coil-kt.github.io/coil/) with `VideoFrameDecoder` support for high-quality, efficient media thumbnails.
- **Video Engine**: [ExoPlayer (Media3)](https://developer.android.com/guide/topics/media/exoplayer) for optimized media playback.
- **Preferences**: [Jetpack DataStore](https://developer.android.com/topic/libraries/architecture/datastore) for persistent theme and dark mode states.
- **Architecture Pattern**: MVVM (Model-View-ViewModel) with structured StateFlow.

---

## 📁 Repository Structure

```
com.example.statussaver/
├── MainActivity.kt              # Main entry point and SAF permission handler
├── data/
│   ├── StatusItem.kt            # Data model representing a media status
│   └── StorageRepository.kt     # Traversing storage and exporting to MediaStore
├── ui/
│   ├── StatusViewModel.kt       # State management and business logic
│   └── components/
│       ├── GalleryScreen.kt     # The main grid display and state switcher
│       └── MediaViewerScreen.kt # Pager-based full-screen media viewer
└── theme/
    ├── Color.kt                 # Application color palette specifications
    ├── Theme.kt                 # Material 3 setup mapping local state
    └── Type.kt                  # Custom typography settings
```

---

## 🚀 Getting Started

### 📋 Prerequisites
- Android Studio Ladybug (or newer)
- Android SDK 26+ (Android 8.0+)
- A physical device or emulator with WhatsApp installed and active status media.

### 📥 Setup & Build
1. Clone the repository:
   ```bash
   git clone https://github.com/souvik2812/StatusSaverWhatsapp.git
   cd StatusSaverWhatsapp
   ```
2. Open the project in Android Studio.
3. Build the project or run it directly on your connected device.

---

## 📱 How to Use

1. Launch **Status Saver** on your device.
2. Tap **"Grant Folder Access"**.
3. The system file picker will open. Navigate to:
   `Android > media > com.whatsapp > WhatsApp > Media > .Statuses`
4. Tap **"Use this folder"** and allow permissions. The statuses will load immediately.
5. Use the icons on the top-right to change the color theme or toggle Dark/Light mode.
6. Tap any photo or video to open the gallery viewer. Swipe to browse, and tap the download button (↓) to save it forever.
