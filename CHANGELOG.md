# Changelog

All notable changes (starting from v1.7.3) to stable releases will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [1.2.4] - 2026-01-07

### 🔒 Password Protection

* **App Lock with PIN/Fingerprint/Face Authentication**
  * **Multi-factor authentication** - Secure your app with PIN (4-6 digits), fingerprint, or face recognition
  * **Comprehensive security settings** - Enable/disable app lock, change PIN, configure timeout
  * **Biometric integration** - Use fingerprint or face unlock on supported devices
  * **Smart timeout management** - Auto-lock after configurable inactivity (1-60 minutes)
  * **Background protection** - App locks when sent to background
  * **Beautiful lock screen UI** - Animated number pad with Material Design 3
  * **Secure PIN storage** - SHA-256 hashed and securely stored using MMKV
  * **Attempt limiting** - Prevents brute force attacks with max 5 attempts
  * **Customizable options**:
    - Toggle biometric authentication
    - Require authentication on app launch
    - Set inactivity timeout duration
    - Change PIN anytime
  * **Full theme support** - Works with all themes including Gradient Dark
  * **Privacy focused** - All authentication data stored locally on device
  * Access via: Settings → Seal Plus Extras → Security & Privacy → App Lock

### 🎨 UI/UX Improvements

* **Enhanced security settings page** with comprehensive options
* **Animated lock screen** with smooth transitions and haptic feedback
* **Clear visual feedback** for authentication success/failure
* **Accessibility compliant** - Proper content descriptions and contrast ratios

## [1.2.3] - 2026-01-07

### 🌐 Network Type Restrictions

* **Smart Network Control**
  * **Download only on WiFi, Mobile Data, or Any network** - Full control over network usage
  * Network type selector in download settings
  * Automatic pause when network type changes
  * Resume downloads when preferred network becomes available
  * Battery-friendly and data-conscious downloading
  * Prevents unwanted mobile data usage for large files
  * Configure per-download or globally in settings

### 🔔 Smart Notifications

* **Customizable Notification System**
  * **Customizable notifications** with sound, vibration, and LED settings per task status
  * Different notification profiles for:
    - Download started
    - Download in progress
    - Download completed (success)
    - Download failed (error)
    - Download paused/queued
  * **Per-task status customization**: Choose notification sound, enable/disable vibration, set LED color
  * Rich notification controls: Priority levels, channel management
  * Battery-efficient notification updates
  * Persistent notifications for active downloads
  * Actionable notifications (Pause, Cancel, Retry)
  * Grouped notifications for multiple downloads
  * Silent mode support with visual-only indicators

### 🧹 Codebase Cleanup

* **Documentation Improvements**
  * Removed redundant GRADIENT_DARK documentation files (7 files)
  * Streamlined project structure for better maintainability
  * Consolidated theme documentation in main README

## [1.2.1] - 2026-01-06

### 🎨 Branding Updates

* **New App Logo**
  * **Updated app launcher icon** with new Seal Plus branding
  * New adaptive icons with foreground, background, and monochrome layers
  * Updated all density folders (mdpi, hdpi, xhdpi, xxhdpi, xxxhdpi)
  * Modern Material You themed icon support for Android 13+
  * Properly configured round launcher icons for all devices
  * Enhanced visual identity across all Android launchers

## [1.2.0] - 2026-01-05

### 🔄 Auto-Update System Overhaul

* **Repository Migration**
  * **Updated auto-update system** to point to new repository: `https://github.com/MaheshTechnicals/Sealplus`
  * Changed release URL from `JunkFood02/Seal Plus` to `MaheshTechnicals/Sealplus`
  * All version checks now fetch from `https://github.com/MaheshTechnicals/Sealplus/releases`
  * In-app update notifications correctly display new repository releases
  * APK download URLs properly match new release structure

* **Auto-Update Enabled by Default**
  * **Auto-update is now ON by default** for all new installations
  * Users can still manually disable in Settings → About → Auto update
  * Seamless in-app updates without manual APK downloads
  * Automatic version checking and changelog display

* **GitHub Actions Release Workflow Improvements**
  * **Fixed release publication status** - releases now properly marked as "published"
  * Added `make_latest: true` to ensure releases appear as latest on GitHub
  * Improved release notes with Gradient Dark theme mention
  * Auto-update feature now highlighted in release descriptions
  * Proper release tagging and asset upload synchronization

* **About Page Updates - Seal Plus Branding**
  * **Updated README link** to open `https://github.com/MaheshTechnicals/Sealplus/blob/main/README.md`
  * **Updated Latest release** to fetch from `https://github.com/MaheshTechnicals/Sealplus/releases`
  * **Changed Telegram channel** to `https://t.me/maheshtechnicals`
  * **Removed Matrix space** option from About screen
  * **Added YouTube channel** at `https://youtube.com/@maheshtechnicals` with custom icon
  * All community links now point to Mahesh Technicals channels
  * Maintained consistent spacing, icons, and text alignment

### 🎨 UI/UX Improvements

* **Navigation Drawer & Theming Consistency**
  * **Removed distracting footer** from navigation drawer (app name, version, route display)
  * **Enabled Gradient Dark mode by default** for all users (Settings → Look & Feel)
  * **Fixed Download Queue tab colors** to use gradient primary accent matching FAB
  * Tabs now use `primary` color for selected state (bold gradient accent) instead of subtle `primaryContainer`
  * Selected tab text uses `onPrimary` (high contrast white) for excellent readability
  * Unselected tabs use `surfaceContainerHigh` for better elevation and visual hierarchy
  * Achieved unified premium dark-gradient experience across all surfaces with proper contrast
  * Smooth color transitions with clear selected/unselected states

* **Comprehensive UI/UX Overhaul**
  * Fixed Download Queue page gradient dark theme integration
  * Improved tab visibility with better color contrast (primaryContainer vs fixed color roles)
  * Standardized card elevations using surfaceContainerHigh across all components
  * Normalized button styles: 48dp height, consistent padding (24dp horizontal, 12dp vertical)
  * Enhanced text hierarchy: upgraded to titleMedium for headings, bodyMedium for content
  * Standardized spacing: 16-20dp card padding, 24dp button padding
  * Improved corner radius consistency: 24dp (shapes.large) across all cards
  * Increased format selection card padding for better readability
  * Enhanced action sheet button styling with better spacing and typography

* **Format Selection Page - Major Improvements**
  * **Fixed critical text contrast bug**: Selected cards now use proper onPrimaryContainer/onSecondaryContainer colors instead of low-contrast accent colors
  * **Reordered sections** for better UX: Video → Audio → Video (no audio) instead of Audio → Video only → Video
  * **Enhanced section headers**: Upgraded to titleMedium (16sp), added subtle dividers (30% opacity) for clear visual grouping
  * **Improved card layout**: 18dp padding (up from 16dp), 2dp borders when selected (up from 1.5dp), 12dp grid spacing (up from 8dp)
  * **Better icon visibility**: All icons standardized to 18dp with 4dp spacing, animated colors for selected/unselected states
  * **Smooth animations**: 150ms color transitions (up from 100ms) for polished feel
  * **Applied gradient dark theme**: surfaceContainerHigh backgrounds, proper containerColor on Scaffold
  * **Reduced visual clutter**: 50% opacity borders when unselected, softer shadows, balanced accent usage

### 🐛 Bug Fixes

* **Build Configuration**
  * Fixed AndroidManifest.xml warning about extractNativeLibs attribute placement
  * Removed android:extractNativeLibs from manifest (now managed automatically by AGP via useLegacyPackaging)
  * Resolved AGP warning: "android:extractNativeLibs should not be specified in this source AndroidManifest.xml file"

### 🎨 Component Updates

* **AppEntry.kt**: Removed footer from NavigationDrawer (cleaner UI)
* **NavigationDrawer.kt**: Removed footer parameter and implementation
* **SelectionGroup.kt**: Updated to use primary/onPrimary colors for gradient accent tabs, removed LocalFixedColorRoles import
* **PreferenceUtil.kt**: Changed GRADIENT_DARK_MODE default from false to true
* **DownloadPageV2.kt**: Applied background color to Scaffold, improved tab padding
* **DownloadQueueItem.kt**: surfaceContainerHigh surface, titleMedium typography, 20dp padding, shapes.large
* **Buttons.kt**: 48dp defaultMinSize, standardized padding (24dp horizontal, 12dp vertical)
* **ModalBottomSheetM3.kt**: Added surfaceContainerHigh containerColor
* **VideoCardV2.kt**: surfaceContainerHigh in dark mode, titleMedium typography, shapes.large
* **FormatItem.kt**: Complete visual overhaul - proper selected/unselected colors, improved spacing, animated states
* **FormatPage.kt**: Reordered sections, improved headers with dividers, better grid spacing (12dp), gradient dark theme
* **ActionSheetItems.kt**: Improved button styling with shapes.large, 12dp text spacing, labelLarge typography

## [v1.1.0][1.1.0] - 2026-01-04

### ✨ New Features

* **Gradient Dark Theme - Premium UI Mode**
  * Added new "Gradient Dark" toggle in Look & Feel settings
  * Deep charcoal/obsidian backgrounds (#0A0A0F, #14141F) for perfect OLED viewing
  * Vibrant linear gradients (deep blues #5B47E5 → purples #8B5CF6)
  * Sophisticated glassmorphism effects with subtle borders and translucent surfaces
  * Premium card-based layouts with 20-24dp rounded corners
  * Smooth micro-animations (300ms fade-in, 150ms button press)
  * 5 new premium UI components:
    - PremiumGlassCard - Glassmorphism cards with animations
    - PremiumGradientButton - Gradient buttons with press effects
    - PremiumSectionHeader - Headers with gradient accents
    - PremiumInfoCard - Info cards with gradient borders
    - AnimatedCardContainer - Smooth entrance animations
  * Hardware-accelerated animations for smooth 60fps performance
  * WCAG AA compliant contrast ratios (4.5:1+)
  * Graceful fallback to Material 3 when disabled

### 📚 Documentation

* Added comprehensive Gradient Dark theme documentation:
  * Complete implementation guide (800+ lines)
  * Visual design reference with color swatches and gradients
  * Quick reference card with code snippets
  * 5 complete integration examples
  * API documentation for all components
  * 2,200+ lines of documentation across 6 files

### 🎨 Design System

* 21 new resource files (10 XML, 6 Kotlin, 5 documentation)
* 15+ carefully selected colors for dark aesthetic
* 4 gradient brushes (Primary, Secondary, Accent, Vibrant)
* Consistent spacing scale (4dp to 56dp)
* Professional animation timings

### Changed

* **Rebranded from "Seal" to "Seal Plus"**
  * Updated application name to "Seal Plus"
  * Changed application ID from `com.junkfood.seal` to `com.junkfood.sealplus`
  * Updated all README files and documentation
  * Debug variant now shows "Seal Plus Debug"
  * Preview variant now shows "Seal Plus Preview"

## [v2.1.1][2.1.1] - 2026-01-04

### 2026 Platform Updates

#### Updated

* **Platform & Build Tools**
  * Updated Android Gradle Plugin to 8.13.2 (latest stable, Dec 2025)
  * Updated Gradle to 8.13 (officially required for AGP 8.13)
  * Updated Kotlin to 2.0.21 (stable) with K2 compiler
  * Migrated from deprecated kotlinOptions to modern compilerOptions
  * Updated KSP to 2.0.21-1.0.28 for stable annotation processing
  * Updated to compileSdk/targetSdk 36 (required by latest AndroidX libraries)

* **Core Dependencies**
  * Updated Jetpack Compose BOM to 2025.01.00 (January 2025 stable release)
  * Updated AndroidX Core to 1.17.0 (Dec 2025)
  * Updated AndroidX AppCompat to 1.7.1 (Jun 2025)
  * Updated AndroidX Activity to 1.12.2 (Dec 2025)
  * Updated Lifecycle to 2.10.0 (Nov 2025)
  * Updated Navigation to 2.9.6 (Nov 2025)
  * Updated Room to 2.8.4 (Nov 2025)
  * Updated Graphics Shapes to 1.0.4 (Dec 2025)
  * Updated Kotlinx Coroutines to 1.10.1
  * Updated Kotlinx Serialization to 1.8.0
  * Updated Accompanist libraries to 0.36.0
  * Updated Coil to 2.7.0 with improved image loading
  * Updated OkHttp to 5.0.0-alpha.14

* **Testing & Quality**
  * Updated Espresso to 3.6.1
  * Updated AndroidX Test Ext to 1.2.1

* **CI/CD Infrastructure**
  * Updated GitHub Actions workflows to latest versions
  * Updated setup-java action to v4
  * Updated gradle/actions to v4
  * Updated android-actions/setup-android to v3.2.1
  * Updated actions/stale to v10 for issue management
  * Updated actions/checkout to v4 across all workflows

### Fixed

* Improved compatibility with Android 16 APIs
* Enhanced stability with latest Kotlin compiler
* Better memory management with updated coroutines

### Performance

* Faster build times with Gradle 8.12
* Improved runtime performance with Compose BOM 2026.01.00
* Optimized database operations with Room 2.7.0
* Enhanced network efficiency with OkHttp 5.0.0 stable

### Developer Experience

* Updated all development tools for 2026
* Improved CI/CD pipeline reliability
* Better code formatting with latest ktfmt
* Enhanced type safety with Kotlin 2.1.0

## [v2.0.0][2.0.0] - unreleased

### Notable changes from v1.13

- Concurrent downloading
- Download queue
- User interface overhaul
- Large screen support
- Resume failed/canceled download
- Backup & restore unfinished tasks in the download queue
- Select from formats/playlists in Quick Download
- Predictive back animation support for Android 14+
- Bump up minimum API level to 24 (Android 7.0)

## [v1.13.0][1.13.0] - 2024-08-18

### Fixed

- Fix the issue where exported command templates could not be imported in v1.12.x
- Fix an unexpected behavior where multiple formats would be selected

### Change

- Update `youtubedl-android` to v0.16.1
- Update translations

## [v1.12.1][1.12.1] - 2024-04-17

### Added

* Add auto update interval for yt-dlp
* Cookies page now shows the current count of cookies stored in the database

### Fixed

* Intercept non-HTTP(s) URLs opened in WebView
* Videos are remuxed to mkv even when download subtitle is disabled
* Use MD2 ModalBottomSheetLayout in devices on API < 30
* Block downloads when updating yt-dlp

### Known issues

* TextFields(IME) fallback to plain character mode when showing a ModalBottomSheet
* yt-dlp might be broken if you tried to download something while it was
  updating (`bad local file header`). To fix it, you just need to update yt-dlp again

## [v1.12.0][1.12.0] - 2024-04-05

### Added

* Search from download history
* Search from subtitles in format selection page
* Export download history to file/clipboard
* Import download history from file/clipboard
* Re-download unavailable videos
* Download auto-translated subtitles
* Remember subtitle selection for next downloads
* Remux videos into mkv container for better compatibility
* Configuration for not using the download type in the last download
* Improve UI/UX for download error handling
* Add splash screen
* Haptic feedback BZZZTT!!1!

### Changed

* Long pressing on an item in download history now selects it
* Use nightly builds for yt-dlp by default
* Migrate `Slider` & `ProgressIndicator` to the new visual styles in MD3
* Use default display name from system for locales
* Metadata of videos is also embedded in the files now
* A few UI changes that I forgot

### Fixed

* Fix a permission issue when using Seal in a different user profile or private space
* Fix an issue where the text cannot be copied in the menu of the download history
* Display approximate file size for formats when there's no exact value available
* Fix an issue causes app to crash when the selected template is not available
* Custom command now ignore empty URLs, which means you can insert URLs along with arguments in
  command templates
* Fix an issue where some formats may be unavailable when downloading playlists

### Known issues

* TextFields(IME) fallback to plain character mode when showing a ModalBottomSheet
* ModalBottomSheet handles insets incorrectly on devices below API 30

## [v1.11.3][1.11.3] - 2024-01-22

### Added

* Merge multiple audio streams into a single file
* Allow downloading with cellular network temporarily

### Fixed

* App creates duplicated command templates on initialization
* Cannot make video clip in FormatPage

## [v1.11.2][1.11.2] - 2024-01-06

### Added

* Keep subtitles files after embedding into videos
* Force all connections via ipv4
* Prefer vp9.2 if av1 hardware decoding unavailable
* Add system locale settings for Android 13+

### Fixed

* User agent gets enabled when refreshing cookies
* Restrict filenames not working in custom commands

### Changed

* Transition animation should look more smooth now

## [v1.11.1][1.11.1] - 2023-12-16

### Added

* Add `--restrict-filenames` option in yt-dlp
* Add playlist title as an option for subdirectory
* Add more thanks to sponsors

### Fixed

* Fix some minor UI bugs
* Fix an issue causing error when parsing video info

## [v1.11.0][1.11.0] - 2023-11-18

### Added

* Custom output template (`-o` option in yt-dlp)
* Export cookies to a text file
* Make embed metadata in audio files optional
* Add the ability to record download archive, and skip duplicate downloads
* Add cancel button to the download page
* Add input chips for sponsorblock categories
* Add subtitle selection dialog in format page, make auto-translated subtitles available in subtitle
  selection
* Add more thanks to sponsors

### Changed

* Move the directory for storing temporary files to external storage (`Seal/tmp`)
* Change the default output template to `%(title)s.%(ext)s`
* Temporary directory now are enabled by default for downloads in general mode
* Move actions in format page to dropdown menu
* Download subtitles are now available when downloading audio files
* `android:enableOnBackInvokedCallback` is changed to `false` due to compatibility issues

### Fixed

* Fix an issue causes sharing videos to fail on certain devices
* Fix an issue causes uploader marked as null, make uploader_id as a fallback to uploader
* Fix an issue when a user performs multiple clicks causing duplicate navigating behaviors

### Removed

* Custom prefix for output template has been removed, please migrate to custom output template

## [v1.10.0][1.10.0] - 2023-08-30

### Added

**Subtitles**

* Convert subtitles to another format
* Select subtitle language in format selection

**Format selection**

* Display icons(video/audio) on `FormatItem`s
* Split video by chapters
* Select subtitle to download by language names/codes

**Custom commands**

* Create custom command tasks in the Running Tasks page
* Configure download directory separately for custom command tasks
* Select multiple command templates to export & remove

**Cookies**

* Add `CookiesQuickSettingsDialog` for refreshing & configuring cookies in configuration menu
* Add user agent header when downloading with cookies enabled

**Other New Features & UI Improvements**

* Show `PlainToolTip` when long-press on `PlaylistItem`
* Add monochrome theme
* Add proxy configuration for network connections
* Add translations in Swedish and Portuguese

### Fixed

* App crashes when being opened in the system share sheet
* Video not shown in YouTube playlist results
* Cookies cannot be disabled after clearing cookies
* Hide video only formats when save as audio enabled
* Parsing error with decimal value in width/height
* Audio codec preference not works as expected
* Could not fetch video info when `originalUrl` is null

### Changed

**Notable Changes**

* Upgrade target API level to 34 (Android 14)
* Preferred video format changed to two options: Legacy and Quality
* UI improvements to the configuration dialog

**Other Changes**

* Update `ColorScheme`s and components to reflect the new MD3 color roles
* Update youtubedl-android version, added pycryptodomex to the library
* Move Video formats to the bottom of the `FormatPage`
* Notifications now are enabled by default
* Minor UI improvements & changes

## [v1.9.2][1.9.2] - 2023-04-27

### Fixed

* Fix a bug causing Incognito mode not working in v1.9.1
* Fix misplaced quality tags in `AudioQuickSettingsDialog`
* Fix mismatched formats when using Save as audio & Download playlist

## [v1.9.1][1.9.1] - 2023-04-11

### Added

* Add Sponsor page: You can now support this app by sponsoring on GitHub!

### Fixed

* Fix a bug causing warnings not shown in logs of completed custom command tasks
* Fix a bug causing videos not scanned into media library when private mode is enabled

### Changed

* Move the directory for temporary files to `cacheDir`

## [v1.9.0][1.9.0] - 2023-03-12

### Added

* Add Preview channel for auto-updating
* Add an option to update to Nightly builds of yt-dlp
* Add a dialog for F-Droid builds in auto-update settings
* Add a switch for auto-updating yt-dlp
* Add the ability to share files in `VideoDetailDrawer`
* Add a badge to the icon to indicate the count of running processes
* Add a switch for disabling the temporary directory
* Add format & quality preference for audio
* Add custom format sorter
* Add the ability to clip video and audio in `FormatSelectionPage` (experimental)
* Add the ability to edit video titles in `FormatSelectionPage` before downloading
* Add the ability to share the thumbnail url in `FormatSelectionPage`
* Implement a new method to extract cookies from the `WebView` database

### Changed

- Change the operation of open link to long pressing the link button in `VideoDetailDrawer`
- Change the thread number range of multi-threaded download to 1-24
- Change the status bar icon to filled icon
- Change the quick settings for media format in the configuration dialog

### Fixed

- Fix a bug causing high-quality audio not downloaded with YT Premium cookies & YT Music URLs
- UI bug in `ShortcutChip` with long template
- Fix a bug causing empty subtitle language breaks downloads
- Fix an issue causing specific languages not visible in system settings on Android 13+
- Fix a UI bug in the format selection page
- Fix a bug causing app to crash when toasting in Android 5.0
- Fix a UI bug causing LTR texts to display incorrectly in RTL locale environment
- Add legacy app icon for API 21~25

### Known issues

- Cookies may not work as expected in some devices, please try to re-generate cookies after this
  occurs. File an issue on GitHub with your device info when experience errors.

## [v1.8.2][1.8.2] - 2023-02-10

### Fixed

- Trimmed ASCII characters filename
- Unexpected error when downloading multiple video to SD card with quick download
- Error when cropping vertical thumbnails as artwork
- ID conflicts when importing custom templates

### Changed

- Add `horizontalScroll` to `LogPage`
- Revert the URL intent filters

## [v1.8.1][1.8.1] - 2023-02-01

### Fixed

- App crashes when downloading in private mode
- Unexpected ImeActions in TextFields
- Disable SD card download when the directory is not set
- Localized strings for file size texts

## [v1.8.0][1.8.0] - 2023-01-29

### Added

- Download to SD card
- Quick download in parallel
- Task dashboard & log page for custom commands
- Custom shortcuts for command templates
- Subtitle preferences
- Apply `--embed-chapters` for video downloads by default
- New color schemes for UI theming

### Changed

- New transition animation between destinations
- Change `minSdkVersion` to 21 (Android 5.0)
- Accessibility improvements to components
- Revert playlist items limit in v1.7.3
- Scan the download directory to the system media library after running commands
- Change the LongClick operations of `FormatItem` to share the stream URLs

## [v1.7.3][1.7.3] - 2023-01-10

### Fixed

- `Webview` captures Cookies from wrong domains
- Notifications of custom commands remain unfinished status
- App crashes when fails to parse video info for format selection
- App crashes when parsing channel info for playlist download

### Added

- Tips about streams merging in `FormatSelectionPage`

### Changed

- Playlist results are limited to 200 videos

[1.7.3]: https://github.com/JunkFood02/Seal/releases/tag/v1.7.3

[1.8.0]: https://github.com/JunkFood02/Seal/releases/tag/v1.8.0

[1.8.1]: https://github.com/JunkFood02/Seal/releases/tag/v1.8.1

[1.8.2]: https://github.com/JunkFood02/Seal/releases/tag/v1.8.2

[1.9.0]: https://github.com/JunkFood02/Seal/releases/tag/v1.9.0

[1.9.1]: https://github.com/JunkFood02/Seal/releases/tag/v1.9.1

[1.9.2]: https://github.com/JunkFood02/Seal/releases/tag/v1.9.2

[1.10.0]: https://github.com/JunkFood02/Seal/releases/tag/v1.10.0

[1.11.0]: https://github.com/JunkFood02/Seal/releases/tag/v1.11.0

[1.11.1]: https://github.com/JunkFood02/Seal/releases/tag/v1.11.1

[1.11.2]: https://github.com/JunkFood02/Seal/releases/tag/v1.11.2

[1.11.3]: https://github.com/JunkFood02/Seal/releases/tag/v1.11.3

[1.12.0]: https://github.com/JunkFood02/Seal/releases/tag/v1.12.0

[1.12.1]: https://github.com/JunkFood02/Seal/releases/tag/v1.12.1

[1.13.0]: https://github.com/JunkFood02/Seal/releases/tag/v1.13.0