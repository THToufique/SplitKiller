# SplitKiller

<div align="center">

![Android](https://img.shields.io/badge/Platform-Android-green.svg)
![Min SDK](https://img.shields.io/badge/Min%20SDK-24-blue.svg)
![Target SDK](https://img.shields.io/badge/Target%20SDK-35-blue.svg)
![Kotlin](https://img.shields.io/badge/Language-Kotlin-purple.svg)
![License](https://img.shields.io/badge/License-MIT-orange.svg)

**Merge split APKs (XAPK/APKM) into single installable APKs with proper v1+v2+v3 signatures**

</div>

## 📱 Features

- **APK Merging**: Merge XAPK/APKM/ZIP files containing split APKs into a single installable APK
- **Proper Signing**: Sign merged APKs with v1 (JAR) + v2 + v3 signature schemes for maximum compatibility
- **App Extraction**: Extract installed apps (including split APKs) and create XAPK files
- **Custom Keys**: Generate and manage custom RSA 2048-bit signing keys with BKS keystores
- **File Browser**: Built-in file picker with modern Material You design
- **Real-time Logs**: Live console logs for merge and extraction operations
- **Material You**: Beautiful UI with dynamic theming and amber/gunmetal color scheme
- **Batch Operations**: Extract multiple apps at once

## 🎯 Why SplitKiller?

Modern Android apps often come as split APKs (XAPK, APKM) which cannot be installed directly. SplitKiller merges these splits into a single APK that:
- ✅ Installs on any Android device (7.0+)
- ✅ Preserves all resources, libraries, and assets
- ✅ Properly signed with industry-standard tools
- ✅ Works with large APKs (800MB+) without memory issues

## 🛠️ Tech Stack

- **Language**: Kotlin
- **UI**: Jetpack Compose with Material 3
- **Architecture**: MVVM (Model-View-ViewModel)
- **Navigation**: Navigation Compose
- **Storage**: DataStore for preferences
- **Signing**: Google's official `apksig` library (v8.7.2)
- **APK Manipulation**: APKEditor library
- **Cryptography**: BouncyCastle (bcprov, bcpkix)

## 📋 Requirements

- **Min SDK**: 24 (Android 7.0 Nougat)
- **Target SDK**: 35 (Android 15)
- **Compile SDK**: 36
- **Permissions**:
  - `READ_EXTERNAL_STORAGE` / `READ_MEDIA_IMAGES` (Android 13+)
  - `WRITE_EXTERNAL_STORAGE` (Android 12 and below)
  - `MANAGE_EXTERNAL_STORAGE` (Android 11+)
  - `REQUEST_INSTALL_PACKAGES`
  - `QUERY_ALL_PACKAGES`

## 🚀 Getting Started

### Installation

1. Clone the repository:
```bash
git clone https://github.com/THToufique/SplitKiller.git
cd SplitKiller
```

2. Open in Android Studio (Ladybug or later)

3. Build and run on your device

### Usage

#### Merging Split APKs

1. Navigate to **Merge** screen
2. Tap **Select File** to choose an XAPK/APKM/ZIP file
3. Select signature scheme (default: v1+v2+v3)
4. Choose signing key (default: testkey)
5. Tap **Merge APK**
6. Find merged APK in `/storage/emulated/0/SplitKiller/`

#### Extracting Installed Apps

1. Navigate to **Extract** screen
2. Browse installed apps (use search to filter)
3. Select apps to extract
4. Tap **Extract Selected**
5. Find XAPK files in `/storage/emulated/0/SplitKiller/`

#### Managing Signing Keys

1. Navigate to **Key Manager** screen
2. Tap **Create New Key**
3. Enter key details (alias, password, organization)
4. Generated keys are stored in app's private directory
5. Use custom keys when merging APKs

## 📁 Project Structure

```
app/src/main/java/com/ripp3r/splitkiller/
├── data/
│   ├── ApkMerger.kt          # Core APK merging logic
│   ├── KeystoreManager.kt    # APK signing with apksig
│   ├── AppExtractor.kt       # Extract installed apps
│   └── SettingsManager.kt    # DataStore preferences
├── model/
│   ├── AppInfo.kt            # App information data class
│   ├── SignatureScheme.kt    # Signature scheme enum
│   └── SigningKey.kt         # Signing key data class
├── ui/
│   ├── Navigation.kt         # Navigation setup
│   ├── screens/
│   │   ├── MainScreen.kt     # Dashboard + extraction logs
│   │   ├── MergeScreen.kt    # File selection + merge logs
│   │   ├── ExtractScreen.kt  # App list with search
│   │   ├── FileBrowserSheet.kt # Custom file picker
│   │   ├── KeyManagerScreen.kt # Key management
│   │   └── SettingsScreen.kt # App settings
│   └── theme/
│       ├── Color.kt          # Material You colors
│       ├── Theme.kt          # Theme configuration
│       └── Type.kt           # Typography
├── viewmodel/
│   ├── MergeViewModel.kt     # Merge operations
│   ├── ExtractViewModel.kt   # Extract operations
│   ├── KeyManagerViewModel.kt # Key management
│   └── SettingsViewModel.kt  # Settings
├── util/
│   └── AppLogger.kt          # Logging system
└── MainActivity.kt           # Entry point
```

## 🔑 Key Technical Decisions

### Why Google's apksig?

Initially, we implemented custom v2/v3 signing, but it caused `OutOfMemoryError` on large APKs (800MB+). Google's official `apksig` library:
- ✅ Memory-efficient streaming
- ✅ Proper v1+v2+v3 implementation
- ✅ Same library used by Android Studio
- ✅ Battle-tested and maintained

### Why APKEditor?

APKEditor provides:
- Proper resource table merging using `ApkBundle.loadApkDirectory()`
- Manifest manipulation
- Efficient handling of large APK files
- No memory issues with 800MB+ APKs

### Output Directory

All merged APKs and extracted XAPKs are saved to:
```
/storage/emulated/0/SplitKiller/
```

## 🎨 UI/UX Highlights

- **Material You Design**: Dynamic theming with system colors
- **Bottom Sheets**: Modern popup sheets for file selection and options
- **Real-time Logs**: Color-coded console logs with Material Icons
- **Dark/Light Themes**: Supports system, light, and dark themes
- **Responsive**: Adapts to different screen sizes

## 🐛 Known Issues & Solutions

### Issue: "Not signed" after merging
**Solution**: We use Google's apksig library which properly signs APKs with v1+v2+v3 schemes.

### Issue: OutOfMemoryError on large APKs
**Solution**: apksig handles memory efficiently with streaming.

### Issue: Icons missing after merge
**Solution**: We use `ApkBundle.loadApkDirectory()` for proper resource table merging.

## 🤝 Contributing

Contributions are welcome! Please feel free to submit a Pull Request.

1. Fork the repository
2. Create your feature branch (`git checkout -b feature/AmazingFeature`)
3. Commit your changes (`git commit -m 'Add some AmazingFeature'`)
4. Push to the branch (`git push origin feature/AmazingFeature`)
5. Open a Pull Request

## 📄 License

This project is licensed under the MIT License - see the [LICENSE](LICENSE) file for details.

## 🙏 Acknowledgments

- [Google's apksig](https://android.googlesource.com/platform/tools/apksig/) - Official APK signing library
- [APKEditor](https://github.com/REAndroid/APKEditor) - APK manipulation library
- [BouncyCastle](https://www.bouncycastle.org/) - Cryptography library
- [Material Design 3](https://m3.material.io/) - UI design system

---

<div align="center">

**Made with ❤️ using Kotlin and Jetpack Compose**

⭐ Star this repo if you find it useful!

</div>
