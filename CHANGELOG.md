# Changelog

All notable changes (starting from v1.7.3) to stable releases will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

## [2.6.0] - 2026-04-27

### 🔒 Security Hardening

* **PBKDF2 PIN Hashing**
  + PIN-based App Lock now hashes passwords using **PBKDF2WithHmacSHA256** with a unique 16-byte random salt and 200,000 iterations — replacing the weak single-pass SHA-256 approach
  + Existing PIN users are automatically migrated to the new secure hash on next unlock
  + Industry-standard protection against brute-force and rainbow-table attacks

* **Removed Overly Broad Storage Permission**
  + `MANAGE_EXTERNAL_STORAGE` has been removed from the manifest
  + All download paths now work without requiring the "All Files Access" permission

* **ADB Backup Protection**
  + MMKV preferences (which store your PIN hash, security settings, and download preferences) are now excluded from ADB backups via `backup_rules.xml`
  + Prevents sensitive configuration from leaking through device backups

* **NotificationActionReceiver Secured**
  + `NotificationActionReceiver` is now explicitly set to `exported=false` in the manifest, preventing third-party apps from sending fake notification action intents

* **MITM Prevention on Proxy**
  + `--no-check-certificate` is now automatically disabled when a proxy is active, preventing man-in-the-middle attacks when routing traffic through a proxy server

* **Replaced Embedded GitHub Token**
  + Sponsor data is now fetched from a public, unauthenticated JSON URL — no API token embedded in the APK

* **Proxy Security Warning Dialog**
  + A security warning `AlertDialog` is now shown before enabling the free proxy mode, informing users of the risks of routing traffic through unknown third-party servers

### 🛡️ Brute-Force Protection

* **30-Second Lockout on App Lock**
  + After 5 failed PIN attempts, the App Lock screen now shows a **30-second countdown timer** before allowing further attempts
  + Countdown is fully animated and stored in state surviving recomposition

### 🐛 Stability & Correctness (20 Fixes)

* **Context Leak Fixed**
  + `MainActivity` no longer overwrites `App.context` with the Activity context — prevents memory leaks and crashes on activity recreation

* **Replaced runBlocking Calls**
  + All `runBlocking` usages in `MainActivity` and `QuickDownloadActivity` replaced with `lifecycleScope.launch(Dispatchers.IO)`, preventing ANRs on slow devices
  + `PreferenceUtil.getTemplate()` retry loop migrated from `runBlocking` to proper coroutine

* **OkHttp Client Improvements**
  + Added explicit connect/read/write timeouts to the `UpdateUtil` HTTP client
  + `OkHttpClient` singletons now shared across `ProxyManager`, `ProxyValidator`, `SponsorUtil`, and `FormatValidator` — eliminates per-call thread pool churn and reduces memory pressure

* **Resource Leak Fixes**
  + `SQLiteDatabase` and `Cursor` now properly closed with `.use{}` blocks even on exception
  + `DownloadUtil` archive file now streamed line-by-line instead of fully read into memory

* **Coroutine & State Fixes**
  + 5-minute deadline added to `addToDownloadQueue` to prevent coroutine leaks
  + `ConcurrentHashMap` used for `resumedProgressMap` and `retryCountMap` in `DownloaderV2` to prevent race conditions
  + `updateJob` in the updater page now held in `remember { mutableStateOf }` to survive recomposition
  + `distinctUntilChanged` added and `flowOn` position corrected in `VideoListViewModel`
  + Koin `get<DownloaderV2>()` moved from field initializer to `onReceive()` body in `NotificationActionReceiver`

* **Error & Notification Fixes**
  + Error notifications now use `throwable.message` instead of `stackTraceToString()` to avoid truncated/ugly system notifications
  + `isServiceRunning` flag in `App` now `@Volatile` and correctly reset in `onServiceDisconnected`
  + `COMMAND_DIRECTORY` path now correctly persisted when calling `updateDownloadDir`
  + Null-safe response body handling in `UpdateUtil.getLatestRelease` and `downloadApk`
  + OkHttp response socket properly closed in `ProxyValidator.testConnection`

* **Download & Format Fixes**
  + `av01` codec removed from the unsupported video codecs blacklist — AV1 downloads now work correctly
  + `FormatValidator.filterValidFormats` parallelized with `async/awaitAll` for faster format page loading
  + `makeKey()` now uses a null-char (`\0`) delimiter to prevent hash collisions between URL+template combos
  + Fixed literal `"null"` string being stored in download records when URL was null
  + `HomePageViewModel.updateState(Idle)` now only called inside `onSuccess`, not on every state change
  + DB write moved before `StateFlow.update{}` in `CookiesViewModel` to prevent stale UI state

* **Backup & File Fixes**
  + Backup filename now uses `SimpleDateFormat("yyyy-MM-dd_HH-mm-ss")` for consistent cross-platform filenames
  + `importBackup` correctly handles template import logic
  + `hideItem`/`unhideItem` now uses atomic `renameTo()` with copy+delete fallback for reliability
  + `FileUtil.getRealPath` improved with `Log.w` for non-primary storage paths

### 🎨 Material3 1.3.1 Stable Migration

* **Upgraded from Alpha to Stable Compose BOM**
  + Switched from `compose-bom-alpha` to the stable `compose-bom` (Material3 1.3.1)
  + Resolved all breaking API changes introduced in the stable release

* **Fixed API Breaking Changes**
  + `SheetState`: replaced deprecated `velocityThreshold` / `positionalThreshold` parameters with `density` in `ModalBottomSheetM3`, `ActionSheet`, `DownloadDialogV2`, and `SponsorPage`
  + `TooltipDefaults`: replaced removed `rememberTooltipPositionProvider()` with `plainTooltipPositionProvider()` in `DownloadPage` and `PlaylistSelectionPage`
  + `ExposedDropdownMenu`: replaced alpha-only `ExposedDropdownMenuAnchorType` with stable `MenuAnchorType` in `YtdlpUpdateDialog`

* **Dependency Downgrades to Stable**
  + `OkHttp` downgraded from `5.0.0-alpha.14` → `4.12.0` (stable)
  + `androidx.biometric` upgraded to `1.2.0-alpha05` for better OEM compatibility

### 🏠 Home Page Sync Fix

* **Instant Recent Downloads Sync**
  + Replaced lifecycle-gated `collectAsStateWithLifecycle` with a composition-lifetime `LaunchedEffect` collector for the download history flow
  + Deletions made on the VideoList page are now reflected in **Recent Downloads instantly** without needing a process restart
  + Added `LaunchedEffect(recentDownloads)` to prune stale `localHiddenIds` entries when DB records are removed

* **Real-Time Missing File Detection**
  + File existence for each Recent Download card is now re-checked on every `ON_RESUME` event using `produceState` with a `lifecycleRefreshTrigger`
  + Files deleted from the OS file manager now **gray out immediately** the next time you return to the home screen — no process kill required

### 🌐 Sponsor Data Refactor

* **New Social Account & Sponsorship Data Classes**
  + `SocialAccount`, `SocialAccounts`, `SponsorEntity`, `Tier`, and `SponsorShip` data classes added for structured sponsor data
  + Sponsor page now renders richer profile information from the new model

* **Thread-Safe Sponsor Cache**
  + Sponsor data cache in `SponsorUtil` is now protected with `@Volatile` + synchronized double-checked locking
  + Compose state mutations dispatched to the Main thread in `SponsorPage` to prevent `IllegalStateException`

### 🌍 Website & SEO Improvements

* **Official Sealplus Website**
  + CNAME configured for the official Sealplus domain
  + `sitemap.xml` added and linked in `index.html` and `privacy.html`
  + `robots.txt` added to manage web crawler access
  + Google site verification meta tag added
  + Canonical and sitemap links updated to reflect the new domain
  + Title, description, and keywords updated for Sealplus branding

---

### 📦 Installation

Download the appropriate APK for your device:

* **Universal APK**: Works on all devices (recommended)
* **arm64-v8a**: For 64-bit ARM devices (most modern phones)
* **armeabi-v7a**: For 32-bit ARM devices
* **x86_64**: For 64-bit x86 devices
* **x86**: For 32-bit x86 devices

### ✨ Key Features (v2.6)

* 🔒 **PBKDF2 PIN Hashing** - Industry-standard 200K-iteration secure PIN protection with salt
* 🛡️ **30-Second Brute-Force Lockout** - Countdown timer after 5 failed PIN attempts
* 🔐 **Removed Overly Broad Storage Permission** - No more MANAGE_EXTERNAL_STORAGE required
* 💾 **ADB Backup Protection** - MMKV sensitive data excluded from device backups
* 🚫 **MITM Prevention** - SSL checking re-enabled when proxy is active
* 🧹 **20 Stability & Correctness Fixes** - Context leak, runBlocking, timeouts, resource leaks, race conditions
* 🎨 **Material3 1.3.1 Stable** - Migrated from alpha BOM, OkHttp stable 4.12.0
* 🏠 **Instant Home Page Sync** - Recent Downloads instantly updated on deletion or file removal
* 🌐 **Sponsor Data Refactor** - Thread-safe cache, richer profile data
* ⏯️ **Pause/Resume downloads** with queue support
* 🌐 Download from 1000+ sites via yt-dlp

---

## [2.5.0] - 2026-04-06

### 🔒 Hidden Content & Privacy

* **Hidden Content Management System**
  + New **Hidden Content** page accessible directly from the navigation drawer
  + Mark any downloaded video as hidden — it disappears from the home page and download list instantly
  + Hidden files are physically relocated to a **private isolated directory** inaccessible from the system file manager
  + Access requires **PIN or biometric authentication** via the existing App Lock system
  + If App Lock is not set up, the app shows a clear error message guiding you to Settings → SealPlus Extras → App Lock
  + Optimistic UI: the card vanishes immediately on hide action before the database write completes

### 🎨 Format Selection UI Overhaul

* **Redesigned Format Cards**
  + File size and bitrate are now shown in dedicated sections on every format card
  + Improved visual hierarchy for codec, resolution, and quality information at a glance

* **List View for Format Cards**
  + New list-style layout option for format cards in the format selection page
  + Toggle it on or off from Settings → SealPlus Extras

* **Merge Audio Stream Option**
  + New toggle on the Format Page to merge a separate audio stream into the selected video format
  + Hint text is displayed when the merge option is active so you always know what will happen

* **Removed Suggested Section**
  + The "Suggested" section has been removed from the format selection page for a cleaner layout

* **Download Dialog Loading State**
  + Loading screen in the download dialog now shows a thumbnail image alongside the progress indicator instead of a plain spinner

* **SealPlus Extras Expanded**
  + New format-related preference options added to the SealPlus Extras settings page

### ⬇️ Download Engine Reliability

* **Auto-Retry on Network Error**
  + Downloads that fail due to a network drop are automatically retried up to **3 times** with a 5-second delay between each attempt
  + Progress text shows `Retrying (1/3)...` during backoff so you always know what's happening

* **Resume Partial Downloads**
  + `--continue` is now always passed to yt-dlp so interrupted downloads pick up from the partial file instead of restarting from scratch

* **WiFi-Only Mode Fully Enforced**
  + The network restriction setting now enforces inside the download engine itself, not just at the enqueue step
  + Queued tasks **automatically resume** the moment a suitable network becomes available again

* **Fixed Task ID Collision**
  + Downloading the same URL twice with identical settings would previously overwrite the first task in the queue
  + Each task now receives a guaranteed-unique ID based on timestamp

### 📊 Download Progress UI

* **Active Download List Sorting**
  + Downloads are now sorted in a strict, stable order: Running → ReadyWithInfo → Idle → Paused → Canceled/Error → Completed
  + Within each group, newer tasks appear first
  + Completed tasks are shown in the active list only until they appear in the recent-downloads section — no duplicates

* **Speed & ETA on Home Cards**
  + Active download cards now show live speed and estimated time, e.g. `2.50 MiB/s  •  ETA 00:03`

* **Merging Phase Detection**
  + Download cards now correctly show **"Merging..."** while yt-dlp is post-processing instead of staying frozen at 100%

* **Fixed -1% / 0% Progress Bug**
  + During the initial phase before yt-dlp reports real progress, the percentage is now hidden instead of showing `-1%` or `Paused 0%`
  + Progress bar shows a smooth **indeterminate/pulsing animation** when no progress data is available yet

### 🗂️ Completed Downloads

* **Missing File Detection**
  + If a downloaded file has been deleted or moved, its home page card now renders **grayed out** with a broken-image icon and "File no longer available" label

### ⚡ Performance Improvements

* **Reduced CPU Usage During Downloads**
  + The internal task scheduler now only fires when a task actually changes state (starts, finishes, errors, pauses) — not on every progress update

* **Reduced Battery & Storage Writes**
  + Full task list serialization to MMKV storage now only happens on meaningful structural state changes, cutting storage I/O to near zero during normal download progress

### 🌍 Translations

* **61 Languages Updated**
  + New `video_audio_merge_hint` and `download_and_merge` strings added across all 61 supported languages

* **Build-Breaking Escape Fix**
  + Corrected unescaped apostrophes in Belarusian (`be`), Punjabi (`pa`), and Ukrainian (`uk`) string files

* **Complete Hebrew Translation** — thanks to [@613avi](https://github.com/613avi)
  + 185 previously missing Hebrew strings have been filled in

* **Improved Turkish Translation** — thanks to [@mikropsoft](https://github.com/mikropsoft)
  + Revised and improved accuracy across dozens of Turkish strings

### 🔧 Other Fixes & Improvements

* Added option to **ignore SSL certificates** for filtered networks (e.g. Netfree) — thanks to [@613avi](https://github.com/613avi)
  + `--no-check-certificate` applied across all 5 request paths: playlist info fetch, video info fetch, video download, and both custom command execution paths
* Navigation drawer header layout polished — improved text hierarchy, spacing, and logo alignment
* Sponsors page and component architecture refactored for cleaner separation of concerns
* Removed & cleaned up unused imports across 24 source files (89 lines removed)

---

## [2.4.0] - 2026-03-10

### ✨ Animated Branding & Visual Polish

* **Animated Glowing "+" Logo**
  + The "+" in "Seal+" on the home page now features a flowing gradient animation (primary → tertiary → secondary)
  + Continuous pulsing glow shadow effect for a premium, eye-catching look
  + Hardware-accelerated infinite transition for smooth 60fps rendering

* **Gradient Circular Progress Indicator**
  + Replaced the plain loading spinner in the download dialog with a premium sweep-gradient spinner
  + Rotating arc with breathing animation and smooth color shifting (primary → tertiary → secondary)
  + 48dp canvas-based implementation with `StrokeCap.Round` for modern aesthetics

### 🎨 UI & Theme Enhancements

* **Typewriter URL Placeholder Animation**
  + The "Enter URL to download" placeholder text now reveals character-by-character with a typewriter effect
  + Flowing gradient brush on the placeholder text matching the Seal+ brand colors
  + One-time animation on page load (50ms per character) for a clean, premium feel

* **Redesigned Navigation Drawer Header**
  + Center-aligned modern header layout with app logo (512×512 splash logo), "Seal Plus" title, "Download Manager" subtitle, and version badge
  + Removed active/selected state highlighting from all drawer items for a cleaner look
  + Reorganized drawer menu items into logical groups for better navigation clarity

* **Dark Theme On by Default**
  + New installations now default to dark theme instead of following the system setting
  + Existing users keep their saved preference; everyone can still switch in Settings → Look & Feel

### 🎯 Consistent Icon Theming

* **Material Theme Color Refactor**
  + Refactored icon tinting across 30+ screens to use `MaterialTheme.colorScheme` instead of hardcoded colors
  + Icons now use `primary`, `secondary`, and `tertiary` colors from the active theme
  + Ensures consistent look across Light, Dark, and Gradient Dark themes

### 🍞 Themed Toast System

* **Custom Themed Toast Manager**
  + Replaced standard Android toasts with a branded themed toast system
  + Toasts now match the app's Material 3 color scheme with rounded corners and proper styling
  + Integrated across all activities for a unified notification experience

### 🔗 Share Intent & Navigation

* **Share Intent URL Handling**
  + URLs shared from other apps (browsers, social media) now automatically pre-fill in the download input field
  + Seamless integration — tap "Share" in any app, select Seal Plus, and the URL is ready to download

* **Navigation Route Updates**
  + Sponsor page navigation now redirects to the Support Developer page for streamlined user flow
  + Settings toggle click behavior and icon animations improved for better UX

---

### 📦 Installation

Download the appropriate APK for your device:

* **Universal APK**: Works on all devices (recommended)
* **arm64-v8a**: For 64-bit ARM devices (most modern phones)
* **armeabi-v7a**: For 32-bit ARM devices
* **x86_64**: For 64-bit x86 devices
* **x86**: For 32-bit x86 devices

### ✨ Key Features (v2.4)

* ✨ **Animated Glowing "+"** - Flowing gradient animation on Seal+ branding
* 🎨 **Typewriter URL Placeholder** - Character-by-character animated input hint
* 🔄 **Gradient Spinner** - Premium sweep-gradient loading indicator
* 🧭 **Redesigned Navigation Drawer** - Center-aligned header with logo and version badge
* 🎯 **30+ Icon Theme Refactor** - Consistent MaterialTheme colors across all screens
* 🍞 **Themed Toast System** - Branded toast notifications matching app theme
* 🔗 **Share Intent Support** - Pre-fill URLs from external app shares
* 🌙 **Dark Theme Default** - Dark mode enabled by default for new users
* ⏯️ **Pause/Resume downloads** with queue support
* 🌐 Download from 1000+ sites via yt-dlp

---

## [2.3.0] - 2026-03-03

### ⚙️ Seal Plus Extras Enhancements

* **Aria2c Connection Control**
  + Added a connection count selector in Settings → Seal Plus Extras
  + Choose between 2, 4, 8, 16, 32 or more simultaneous aria2c connections to maximize download speed
  + Setting takes effect immediately on the next download

* **Sponsor Support Dialog Controls**
  + Added a dedicated *Support & Sponsorship* section in Seal Plus Extras
  + Users can configure how often the sponsor support dialog is shown: Off, Weekly, or Monthly
  + Dialog opens the Support Developer page when tapped, respects the chosen schedule, and persists timing across restarts

### 📊 Download Details Improvements

* **Average Speed Display**
  + Download Details dialog now shows the computed average download speed (e.g. `3.2 MB/s`)
  + Derived from total file size ÷ elapsed download time for accurate reporting
  + Hidden automatically for older history entries that predate this feature

* **Download Time Display**
  + Download Details dialog now shows total time taken (e.g. `2m 34s`)
  + Captured as precise start-to-end duration around the actual yt-dlp execution
  + Available in both the active-download details panel and the history drawer
  + Stored in the database (Room migration v5 → v6, existing rows default to `-1`)

### 🎨 UI & Format Card Improvements

* **Clean Resolution Labels on Format Cards**
  + Format selection cards now show concise human-readable titles (`1080×1920`) instead of raw yt-dlp format strings
  + Priority order: explicit width/height metadata → `Format.resolution` field → regex-extracted `WxH` → audio-only label → format ID fallback
  + Applied to both suggested-format items and individual format list items

* **Redesigned Video Detail Drawer**
  + Overhauled download-history detail sheet with a modern card-based layout
  + 16:9 thumbnail card with rounded corners, source-URL card with copy/open actions
  + Stats grid showing Download Time and Average Speed side-by-side
  + Full-width Delete (outlined, error color) and Share/Re-download action buttons

### 🚀 Smart Stream-Merge Routing

* **Platform-Aware Format Selection**
  + Only YouTube and Reddit natively separate video-only and audio-only streams, so they continue to use the merge path (`videoId` + `audioId`)
  + All other platforms serve fully-muxed video+audio streams; the default download now picks the best combined format directly via `videoAudioFormats`, skipping the redundant merge step entirely
  + Falls back to the merge path automatically when no combined format is available

---

### 📦 Installation

Download the appropriate APK for your device:

* **Universal APK**: Works on all devices (recommended)
* **arm64-v8a**: For 64-bit ARM devices (most modern phones)
* **armeabi-v7a**: For 32-bit ARM devices
* **x86_64**: For 64-bit x86 devices
* **x86**: For 32-bit x86 devices

### ✨ Key Features (v2.3)

* ⚙️ **Aria2c Connection Control** - Tune parallel connections (2–32+) for maximum download speed
* 📊 **Average Speed & Download Time** - See real download metrics in the details dialog
* 🚀 **Smart Stream-Merge Routing** - Automatic best-format selection per platform, no redundant merges
* 🎨 **Clean Format Cards** - Human-readable resolution labels instead of raw format strings
* 💎 **Sponsor Dialog Controls** - Flexible Off/Weekly/Monthly schedule in Seal Plus Extras
* 🏎️ **Aria2c Speed Boost** - Up to 32x parallel connections for ultra-fast downloads
* 🔄 **Retry failed downloads** - One-click recovery for canceled/failed downloads
* ⏯️ **Pause/Resume downloads** with queue support
* 🌐 Download from 1000+ sites via yt-dlp

---

## [2.2.0] - 2026-02-27

### 🚀 Performance Optimization

* **Aria2c Speed Boost & Fixes**
  + Fixed an argument quoting issue that previously forced aria2c into a slow, single-connection fallback mode
  + Doubled parallel connection limits (from 8 to 16 streams) for a massive real-world download speed improvement
  + Cleaned up redundant summary interval arguments for better libaria2c.so compatibility

### 🛠️ Core Downloader Fixes

* **Accurate Resolution Selection**
  + Resolved a major bug where the app would ignore the user's chosen video resolution and fall back to default quality
  + Corrected internal format validation to prevent stale state captures during format list generation
  + Fixed audio codec checks to ensure the exact selected resolution is properly merged and passed to yt-dlp

### 🎨 UI & Notification Improvements

* **Cleaner Progress Tracking**
  + Stripped the unnecessary `[download]` prefix from yt-dlp progress text for a cleaner display in cards and active notifications
  + Fixed progress bar threshold logic so early download progress (0.1–0.9%) shows a real bar instead of an indeterminate spinner

---

### 📦 Installation

Download the appropriate APK for your device:

* **Universal APK**: Works on all devices (recommended)
* **arm64-v8a**: For 64-bit ARM devices (most modern phones)
* **armeabi-v7a**: For 32-bit ARM devices
* **x86_64**: For 64-bit x86 devices
* **x86**: For 32-bit x86 devices

### ✨ Key Features (v2.2)

* 🏎️ **Aria2c Speed Boost** - 16x parallel connections for ultra-fast downloads
* 🎯 **Precise Quality Selection** - Reliable resolution and format merging
* 🎨 **Enhanced Notifications** - Cleaner, accurate progress tracking right from the start
* 🔄 **Retry failed downloads** - One-click recovery for canceled/failed downloads
* 💎 **Sponsors feature** - API integration with dynamic display
* ⏯️ **Pause/Resume downloads** with queue support
* 🌐 Download from 1000+ sites via yt-dlp

---

## [2.1.0] - 2026-02-16

### 🔄 Download Management Enhancements

* **Retry Option for Failed/Canceled Downloads**
  * **Retry button** - Added retry functionality in canceled or failed state download cards
  * **One-click recovery** - Quickly restart failed downloads without re-entering URL
  * **Smart state management** - Maintains download preferences and settings
  * **Error recovery** - Automatically handles temporary failures

### 📊 Download Details & UI Improvements

* **Enhanced Details Dialog**
  * **Resolution section** - Added comprehensive resolution information in details dialog
  * **Direct full-length display** - Details dialog now opens directly at full length for better visibility
  * **Improved information layout** - Better organized video/audio resolution details
  * **Format specifications** - Clear display of codec, bitrate, and quality information

### ⚡ Performance Optimization

* **Download Speed Optimization**
  * **Higher download speeds** - Implemented advanced speed optimization algorithms
  * **Enhanced aria2c configuration** - Optimized multi-connection download parameters
  * **Network efficiency** - Better bandwidth utilization and connection management
  * **Reduced latency** - Faster initial connection and data transfer start

### 💎 Sponsors Feature

* **API Integration & Display**
  * **Sponsors API** - Integrated sponsor data fetching from API endpoint
  * **Dynamic sponsor dialog** - Beautiful sponsor display with profile information
  * **Real-time updates** - Sponsor list updates automatically from server
  * **Enhanced UI** - Premium gradient design for sponsor acknowledgment
  * **Support recognition** - Proper attribution and thanks to project supporters

### 🐛 Bug Fixes & Improvements

* Improved download state management for retry functionality
* Enhanced dialog opening animations for details view
* Better error handling in failed download scenarios
* Optimized memory usage during high-speed downloads
* Fixed UI responsiveness in details dialog
* Improved sponsor data caching and loading

---

## [2.0.0] - 2026-02-05

### 🚀 Major Release - Revolutionary Download Management

**Package Name Changed:** `com.maheshtechnicals.sealplus`

⚠️ **Important Note:** App package name has changed. Please uninstall the old app and install this new version. If using auto-update feature, download the new version from the app, install it, then remove the old app.

### ⏯️ Download Control & Management

* **Pause and Resume Functionality**
  * **Pause/Resume in three-dot menu** - Control downloads from the overflow menu
  * **Icon controls on download cards** - Quick pause/resume buttons directly on cards
  * **Queue support** - Manage multiple downloads with proper queuing
  * **Progress preservation** - Download progress saved when paused
  * **Resume from exact position** - Continue downloads from where you left off

* **Concurrent Downloads Control (1-5 simultaneous downloads)**
  * **Configurable download limits** - Control how many files download at a time
  * **Queue management** - Automatic queuing of additional downloads
  * **Settings location** - Available in Settings → Seal Plus Extras
  * **Performance optimization** - Better resource management with queue system

### 🌐 Advanced Proxy Settings (BETA)

* **Comprehensive Proxy System**
  * **Auto-fetch proxies** - Automatically fetch proxies by country selection
  * **Auto-test functionality** - Test proxies sequentially until finding a working one
  * **Free proxy support** - Access free proxies from multiple countries
  * **Custom proxy configuration** - Add your own HTTP/HTTPS/SOCKS proxies
  * **Beta features** - Advanced proxy management marked as experimental

### 📊 Download Details & Information

* **Comprehensive Details Dialog**
  * **File information** - Name, thumbnail, size, format
  * **Download metadata** - Path, download date, source URL
  * **Interactive elements** - Click to copy URLs, view full paths
  * **Visual improvements** - Better layout with gradient headers

* **Enhanced Download Status Messages**
  * **Clear status indicators** - "Fetching data", "Downloading video", "Downloading audio", "Merging"
  * **Real-time progress** - Live updates on download phases
  * **Better error messages** - More descriptive error information

### 💰 Developer Support Features

* **Payment Integration**
  * **UPI payment support** - Support developers through UPI donations
  * **Multiple payment options** - Various ways to contribute
  * **Support Developer page** - Dedicated page for developer support options

* **Enhanced Sponsors Page**
  * **Fully redesigned UI** - Modern, engaging layout
  * **Better organization** - Clear sections for different support methods
  * **Improved user engagement** - More interactive and informative

### 🔔 Notifications & Permissions

* **Smart Permission Management**
  * **Notification permissions** - Proper Android 13+ notification permission handling
  * **Battery optimization** - Smart prompts for battery optimization settings
  * **Permission dialogs** - Material Design 3 permission request dialogs
  * **Settings integration** - Direct access to system settings when needed

* **Release Notifications**
  * **Telegram integration** - Automatic notifications to Telegram for new releases
  * **Publishing workflow** - Streamlined release publishing to Telegram groups

### ⚙️ Configuration Updates

* **yt-dlp Settings**
  * **Stable build default** - Changed from nightly to stable build for better reliability
  * **Settings location** - Settings → General → yt-dlp settings
  * **Improved stability** - More reliable downloads with stable releases

### 🐛 Bug Fixes & Improvements

* Fixed download progress preservation when pausing/resuming
* Improved error message handling and display
* Enhanced download phase detection and status updates
* Better UI spacing and layout across download cards
* Fixed permission handling flows
* Improved proxy connection handling
* Better resource management with concurrent downloads

---

## [1.2.8] - 2026-02-02

### 🎥 Format Selection Screen Improvements

* **Enhanced Format Filtering**
  * **Valid URLs Only** - Now filters and shows only valid video and audio URLs
  * **Cleaner Interface** - Invalid or unavailable formats are automatically hidden
  * **Improved Reliability** - Better format validation before display

* **Maximum Quality Support**
  * **Highest Resolution Downloads** - Support for maximum resolution available
  * **No Quality Limits** - Download content at the best quality offered by the source
  * **Smart Quality Detection** - Automatically identifies highest available quality

* **Video & Audio (High) Merged**
  * **Best Quality Option** - New "video & audio (high)" merged format in video section
  * **Optimal Output** - Combines highest quality video with highest quality audio
  * **Seamless Merging** - Automatic stream combination for best results

* **Bug Fixes**
  * Fixed video info couldn't fetch error
  * Improved format extraction reliability
  * Better error handling for unavailable formats

### 📥 Recent Downloads Section Updates

* **UI/UX Improvements**
  * **Removed Delete Option** - Delete functionality moved to Downloads page for better organization
  * **Fixed UI Issues** - Resolved layout problems and visual inconsistencies
  * **Cleaner Design** - Streamlined interface for better user experience

* **Additional Improvements**
  * Enhanced performance and stability
  * Better download state management
  * Improved visual feedback

---

## [1.2.7] - 2026-01-24

### ✨ UI Redesign & UX Improvements

* **Fully Redesigned Home Page**
  * **Modern, clutter-free interface** with cleaner layout and improved visual hierarchy
  * **Enhanced navigation** for smoother access to core features
  * **Optimized performance** for faster loading and smoother scrolling
  * **Refined aesthetics** matching the premium Gradient Dark theme

* **Redesigned Navigation Drawer**
  * **Modern Gradient Header** with theme-aware colors (Purple/Blue for Dark, Soft for Light)
  * **App Branding** with prominent "Seal Plus" typography and version badge
  * **Improved Organization** with clear section dividers and better spacing
  * **Visual Enhancements** including new icon containers and hover effects

### 🚀 Video Quality & Formats

* **Enhanced Video/Audio Merging**
  * **Automatic merging** of best video and best audio streams in "Suggested" format
  * **Highest quality guarantee** - Now gets the absolute best resolution available by combining streams
  * **Seamless processing** - Merging happens automatically in the background

* **High Quality Download Options**
  * **"Suggested" section upgrade** - Now explicitly prioritized for highest resolution/bitrate
  * **Smart quality selection** - Automatically picks the top-tier quality available
  * **Better format detection** for complex media sources

### 🎯 Improvements

* **Homepage Delete Fixed**: Resolved issue where delete button in three-dot menu was unresponsive
* **Duplicate Cards Fixed**: Solved issue where downloads appeared twice in active/recent lists
* **Crash Fix**: Fixed NPE crash when deleting items from active list
* **Performance optimization** for list rendering and database operations

---

## [1.2.6] - 2026-01-14

### ✨ New Features

* **Enhanced Video Download Quality**
  * **Highest Resolution MP4 Selection** - Automatically select the best available MP4 format for video downloads
  * Smart format selection prioritizes quality while maintaining compatibility
  * Optimized for maximum video quality with MP4 container support

* **Advanced Download Configuration**
  * **External Downloader Arguments** - Added aria2c external downloader arguments support
  * Enables advanced aria2c configurations for multi-threaded downloads
  * Better control over download behavior and performance tuning

### 📦 Dependencies

* **Updated yt-dlp Engine**
  * Updated youtubedl-android library to 0.18.1
  * Includes yt-dlp 2025.12.08 with latest site support and bug fixes
  * Improved download reliability and compatibility across platforms
  * Enhanced format detection and extraction capabilities

### 🎯 Improvements

* Better video format selection algorithm for optimal quality
* Enhanced aria2c integration for faster multi-segment downloads
* Improved download stability with latest yt-dlp improvements

---

## [1.2.5] - 2026-01-09

### ✨ New Features

* **Professional Onboarding Screens**
  * **4 beautifully designed pages** - Welcome, Downloads, Customizable, and Security
  * **Smooth animations** - Spring bounce, fade transitions, and morphing page indicators
  * **Swipe navigation** - HorizontalPager with intuitive swipe gestures
  * **Skip/Back/Next controls** - Flexible navigation through onboarding flow
  * **Theme integration** - Matches Gradient Dark theme with floating gradient blobs
  * **Glow effects** - Beautiful icon animations with blur effects
  * **First-time detection** - Automatically shows on first launch, then skips
  * **Settings access** - "View Onboarding" option in About page for easy review
  * **Material Design 3** - Professional UI following latest design guidelines

* **Elegant Splash Screen**
  * **Animated logo** - Spring bounce animation with smooth scaling
  * **Gradient branding** - Pulsing glow effect with app theme colors
  * **Theme-aware** - Adapts to Gradient Dark mode and light themes
  * **Professional timing** - 2.4s optimal display duration
  * **Powered by Mahesh Technicals** - Custom branding with copyright notice

### 🐛 Bug Fixes

* **Network Download Fix**
  * Fixed "Any Network" option not allowing mobile data downloads
  * Downloads now work correctly on both WiFi and mobile data when "Any Network" is selected
  * Improved network availability detection in PreferenceUtil

### 🎨 UI/UX Improvements

* Enhanced first-time user experience with guided onboarding
* Smooth app launch with professional splash screen
* Better app branding and identity
* Improved visual feedback during app initialization

## [1.2.4] - 2026-01-08

### 🔒 New Feature: App Lock (Security & Privacy)

* **Complete App Lock System with Multi-Factor Authentication**
  * **4-digit PIN protection** - Fast and secure PIN authentication
  * **Biometric authentication** - Unlock with fingerprint or face recognition on supported devices
  * **Lock screen on access** - Automatically authenticate before accessing security settings when AppLock is enabled
  * **Old PIN verification** - Secure PIN change requires verification of current PIN
  * **Complete reset option** - Reset all AppLock settings and start fresh if needed
  
* **Smart Timeout Management**
  * **Immediately** - Always require authentication every time app is opened (NEW)
  * **Flexible timeouts** - Choose from 1, 2, 5, 10, 15, 30, or 60 minutes of inactivity
  * **Persistent timeout** - Timeout settings survive app restarts using MMKV storage
  * **Require auth on launch** - Option to always authenticate when opening app
  * **Background protection** - Auto-lock when app goes to background
  
* **Secure Implementation**
  * **SHA-256 PIN hashing** - Your PIN is never stored in plain text
  * **MMKV encrypted storage** - All authentication data securely stored locally
  * **Brute force protection** - Maximum 5 attempts with auto-dismiss
  * **No cloud sync** - All data stays on your device for maximum privacy
  
* **Beautiful Material Design 3 UI**
  * **Animated lock screen** - Smooth transitions with number pad
  * **PIN dots indicator** - Clear visual feedback (4 dots for 4-digit PIN)
  * **Error animations** - Shake animation on incorrect PIN
  * **Theme support** - Works with all themes including Gradient Dark
  * **Haptic feedback** - Touch feedback for better user experience
  
* **Comprehensive Settings**
  * Enable/disable App Lock easily
  * Toggle biometric authentication on/off
  * Change PIN anytime (with old PIN verification)
  * Configure authentication timeout (Immediately to 60 minutes)
  * Reset all AppLock settings and data
  * Access via: Settings → Seal Plus Extras → Security & Privacy → App Lock

### 🎨 UI/UX Improvements

* **Enhanced security settings page** with intuitive controls
* **Animated lock screen** with smooth number pad interactions
* **Clear visual feedback** for all authentication states
* **Confirmation dialogs** for critical security actions
* **Accessibility compliant** - Proper content descriptions and contrast ratios
* **Exit Confirmation Dialog** - Stylish and professional confirmation dialog when exiting from Download Queue page
  * Prevents accidental app closure
  * Material Design 3 styled dialog
  * Theme-aware colors matching Gradient Dark and other themes
  * Clear messaging about ongoing downloads continuing in background
  * Smooth animations and haptic feedback

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