# Dependency Update Report — Seal Plus

Generated: 2026-06-09
Scope: All libraries and plugins in `libs.versions.toml`, `build.gradle.kts`, `settings.gradle.kts`

---

## 1. Project Constraint Summary

| Constraint | Current Value |
|---|---|
| **Gradle wrapper** | 9.4.1 |
| **AGP** | 9.2.0 |
| **Kotlin** | 2.3.21 |
| **KSP** | 2.3.9 |
| **compileSdk** | 37 (Android 17) |
| **targetSdk** | 37 |
| **minSdk (app)** | 24 |
| **minSdk (color)** | 21 |
| **JDK** | 21 (JVM toolchain) |
| **Java source compat** | VERSION_21 (app), VERSION_1_8 (color) |
| **Configuration cache** | Enabled |

### Key constraints affecting all updates:
- **KSP version must match Kotlin version** — upgrading Kotlin first requires a corresponding KSP release
- **Compose Compiler plugin** is bundled into the Kotlin plugin (`org.jetbrains.kotlin.plugin.compose`) — no separate version; tied to Kotlin version
- **Room** (via KSP) also ties Kotlin/AGP together — Room 2.8.4 supports KSP
- **color module** uses `compileOnly`-like Java 8 source compat (unlikely to break)
- **Open source project** — no commercial/license restrictions

---

## 2. Full Dependency Table

| # | Dependency | Current | Latest | Compatible? | Blockers / Notes |
|---|---|---|---|---|---|
| 1 | **Gradle wrapper** | 9.4.1 | 9.5.1 | ✅ | Trivial upgrade, safe to do at any time |
| 2 | **AGP** | 9.2.0 | 9.2.1 | ✅ | Minor patch, safe |
| 3 | **Kotlin** | 2.3.21 | **2.4.0** | ⚠️ **BLOCKED** | No KSP release for Kotlin 2.4.0 yet (KSP 2.3.9 targets 2.3.x). **Must wait for KSP 2.4.x** |
| 4 | **Compose BOM** | 2025.01.00 | 2026.05.01 | ⚠️ Pipeline | Safe to update independently of Kotlin, but BOM changes affect Foundation/Material3 APIs |
| 5 | **KSP** | 2.3.9 | 2.3.9 | ✅ | Stays at 2.3.9 until Kotlin 2.4.0 is ready. No newer release yet |
| 6 | **Room** | 2.8.4 | 2.8.4 | ✅ | No newer stable |
| 7 | **Activity Compose** | 1.12.2 | **1.13.0** | ✅ | Safe minor upgrade |
| 8 | **Lifecycle Runtime Compose** | 2.10.0 | 2.10.0 | ✅ | Current is latest |
| 9 | **Navigation Compose** | 2.9.6 | **2.9.8** | ✅ | Safe patch upgrade |
| 10 | **Core KTX** | 1.17.0 | **1.19.0** | ✅ | Major jump (1.17 → 1.19) within AndroidX — safe, but check API diffs |
| 11 | **AppCompat** | 1.7.1 | 1.7.1 | ✅ | No newer stable |
| 12 | **Google Material** | 1.12.0 | **1.14.0** | ⚠️ | Needs review: Material 1.13/1.14 changelog for breaking changes |
| 13 | **Biometric** | 1.2.0-alpha05 | **1.1.0** (stable) / **1.4.0-alpha07** (latest alpha) | ⚠️ **Unusual case** | Current `1.2.0-alpha05` is an alpha between latest stable `1.1.0` and latest alpha `1.4.0-alpha07`. No stable version newer than 1.1.0 exists. Options: (A) stay on current alpha, (B) downgrade to stable 1.1.0, (C) jump to latest alpha 1.4.0-alpha07. **Hold for decision.** |
| 14 | **Espresso Core** | 3.6.1 | **3.7.0** | ✅ | Minor |
| 15 | **Test Ext JUnit** | 1.2.1 | **1.3.0** | ✅ | Minor |
| 16 | **ConstraintLayout Compose** | 1.1.0 | **1.1.1** | ✅ | Patch |
| 17 | **Graphics Shapes** | 1.0.4 | **1.1.0** | ✅ | Minor |
| 18 | **Accompanist** | 0.36.0 | **0.37.3** | ⚠️ **SKIP / Replace** | **Heavily deprecated.** Only permissions module still recommended. All other modules upstreamed to Compose. This project only uses `permissions`, `webview`, and `pager-indicators`. Should be replaced rather than upgraded |
| 19 | **Coil Compose** | 2.7.0 | **3.4.0** | ⚠️ **BLOCKER for major** | Major version jump (2 → 3). Significant API changes (AsyncImage, ImageLoader API, package changes). Full migration required. Safe upgrade path: stay on 2.7.0 or do dedicated migration |
| 20 | **Koin** | 4.0.0 | **4.2.1** | ✅ | Minor-to-moderate upgrade (4.0 → 4.2). Check for Compose API changes |
| 21 | **OkHttp** | 4.12.0 | **5.4.0** | ⚠️ **BLOCKER for major** | OkHttp 5.x is now stable. Uses Kotlin coroutines natively. Package `okhttp3` unchanged, but internal APIs differ. If app uses synchronous-only OkHttp calls, 4.12.0 (last 4.x) is fine; 5.x migration is optional |
| 22 | **Coroutines** | 1.10.1 | **1.11.0** | ⚠️ Pipeline | Minor bump. Built with Kotlin 2.2.20 but binary-compatible with 2.3.x. Safe to update |
| 23 | **Serialization** | 1.7.3 | **1.11.0** | ✅ | Jump from 1.7 → 1.11. Should be binary-compatible, but check for deprecated/ removed APIs |
| 24 | **Datetime** | 0.6.1 | **0.8.0** | ⚠️ | `TimeZone` serialization deprecated. Check if project uses `TimeZone` serialization |
| 25 | **JUnit 4** | 4.13.2 | 4.13.2 | ✅ | No newer version |
| 26 | **ktfmt plugin** | 0.20.1 | **0.63** | ⚠️ **BLOCKER** | Plugin ID likely changed (`com.ncorti.ktfmt.gradle` → `com.facebook.ktfmt`). DSL and configuration format changed. ktfmt CLI args differ. **Must verify plugin ID and API before upgrading** |
| 27 | **MMKV** | 1.3.12 | **1.3.16** (LTS) or **2.4.0** | ⚠️ **SKIP 2.x** | 1.3.x LTS (1.3.16) is safe. 2.4.0 has breaking `MMKVHandler` callback changes and project is pinned to v1.3.x for 32-bit support. Stay on 1.3.x LTS |
| 28 | **yt-dlp android** | 0.18.1 | 0.18.1 | ✅ | No newer release |
| 29 | **Gradle Develocity** | 3.19 | **4.4.2** | ✅ | Major jump (3 → 4). Develocity configuration API may have changed in version 4. Check DSL compatibility |
| 30 | **Fo下课jay resolver** | 0.4.0 | **1.0.0** | ✅ | Safe update, minor DSL changes |

---

## 3. Per-Dependency Change Log

### 3.1 Gradle Wrapper (9.4.1 → 9.5.1)

**Files to change:**
- `gradle/wrapper/gradle-wrapper.properties`: change `distributionUrl` to `https\://services.gradle.org/distributions/gradle-9.5.1-bin.zip`
- No code changes needed.

**Commands:**
```bash
./gradlew wrapper --gradle-version=9.5.1
```

---

### 3.2 AGP (9.2.0 → 9.2.1)

**Files to change:**
- `gradle/libs.versions.toml` line 3: `androidGradlePlugin = "9.2.1"`
- No API or DSL changes expected.

---

### 3.3 Kotlin (2.3.21 → 2.4.0)

**⚠️ BLOCKED — see Section 5.**

**Required companion upgrades:**
- KSP must release for 2.4.0 first
- Compose Compiler plugin version matches Kotlin (automatic via `org.jetbrains.kotlin.plugin.compose`)
- All Kotlinx libraries (coroutines, serialization, datetime) must verify compatibility

**Files to change when unblocked:**
- `gradle/libs.versions.toml` line 24: `kotlin = "2.4.0"`
- `gradle/libs.versions.toml` line 32: verify KSP version matches `ksp = "2.4.0-..."`

---

### 3.4 Compose BOM (2025.01.00 → 2026.05.01)

**Files to change:**
- `gradle/libs.versions.toml` line 4: `androidxComposeBom = "2026.05.01"`

**Potential code changes:**
- BOM 2026.05.01 includes Foundation ~1.9.x, Material3 ~1.5.x, UI ~1.9.x
- The `FlowRow` → `Row`/`Column` fix already done addresses Foundation 1.8+ API changes
- Material3 1.5+ may have additional API changes (check `@ExperimentalMaterial3Api` annotations)
- **Recommendation:** apply BOM update first, then compile, then fix any new deprecation/experimental warnings

---

### 3.5 Activity Compose (1.12.2 → 1.13.0)

**Files to change:**
- `gradle/libs.versions.toml` line 8: `androidxActivity = "1.13.0"`

**Potential code changes:**
- `androidx.activity:activity-compose` 1.13.0 may have changed `setContent` API or edge-to-edge handling
- Check `enableEdgeToEdge()` if used

---

### 3.6 Navigation Compose (2.9.6 → 2.9.8)

**Files to change:**
- `gradle/libs.versions.toml` line 14: `androidxNavigation = "2.9.8"`
- No expected code changes (patch release).

---

### 3.7 Core KTX (1.17.0 → 1.19.0)

**Files to change:**
- `gradle/libs.versions.toml` line 5: `androidxCore = "1.19.0"`

**Potential code changes:**
- Jump from 1.17 → 1.19. Check `core-ktx` changelog for deprecated APIs
- `BuildCompat`, `ContentResolverCompat`, or other utility deprecations
- Unlikely to break usage in this project

---

### 3.8 Google Material (1.12.0 → 1.14.0)

**Files to change:**
- `gradle/libs.versions.toml` line 6: `androidMaterial = "1.14.0"`

**Potential code changes:**
- Material 1.13.0 removed some deprecated BottomNavigation APIs (now `NavigationBar`)
- Material 1.14.0 may have further deprecations
- Project uses Compose Material3 for most UI, so `com.google.android.material` usage is likely minimal

---

### 3.9 Biometric (1.2.0-alpha05 → ???)

**⚠️ Unusual case — no newer stable release.**

Verified via Google Maven:
- Latest **stable**: `1.1.0`
- Latest **alpha**: `1.4.0-alpha07`
- Current: `1.2.0-alpha05` (an alpha release, newer than stable but older than latest alpha)

**Options:**
| Option | Version | Risk |
|---|---|---|
| A — Stay on current | 1.2.0-alpha05 | Uses an alpha in production (current behavior) |
| B — Downgrade to stable | 1.1.0 | Safe but loses any features from 1.2.0 alpha |
| C — Jump to latest alpha | 1.4.0-alpha07 | May have breaking API changes, still alpha |

**Files to change (if updating):**
- `gradle/libs.versions.toml` line 9: set to chosen version

**Recommendation:** Option A (stay on current) unless there's a specific need to change. The app lock feature works with the current alpha. If stability is preferred, Option B (1.1.0 stable).

---

### 3.10 Espresso + Test Ext (minor)

**Files to change:**
- `gradle/libs.versions.toml` lines 16-17: `androidxEspresso = "3.7.0"`, `androidxTestExt = "1.3.0"`
- No code changes expected.

---

### 3.11 ConstraintLayout Compose (1.1.0 → 1.1.1)

**Files to change:**
- `gradle/libs.versions.toml` line 11: `constraintLayout = "1.1.1"`
- No code changes (patch).

---

### 3.12 Graphics Shapes (1.0.4 → 1.1.0)

**Files to change:**
- `gradle/libs.versions.toml` line 10: `graphics = "1.1.0"`
- No code changes expected.

---

### 3.13 Accompanist (0.36.0 → 0.37.3)

**⚠️ Recommend replacement, not upgrade.**

Current usage (from `bundles.accompanist`):
- `accompanist-permissions`
- `accompanist-webview`
- `accompanist-pager-indicators`

**With 0.37.3:**
- `accompanist-pager` and `pager-indicators` were removed (upstreamed to Compose Foundation)
- `accompanist-webview` was removed (upstreamed)
- Only `accompanist-permissions` (and `drawablepainter`/`adaptive`) remain

**Migration plan for each:**
| Module | Compose replacement |
|---|---|
| `accompanist-pager-indicators` | `HorizontalPager` + `TabRow` in Compose Foundation/Material3 |
| `accompanist-webview` | `AndroidView(AndroidViewBinding)` with `WebView` directly |
| `accompanist-permissions` | Still available in accompanist, OR use Compose runtime `rememberLauncherForActivityResult` |

**Recommendation:** Drop accompanist entirely and migrate each usage to Compose/AndroidX equivalents. If not ready, pin at 0.36.0.

---

### 3.14 Coil Compose (2.7.0 → 3.4.0)

**⚠️ Major version break.**

**Known Coil 3.x changes:**
- `AsyncImage` API changed
- `ImageLoader` creation changed (new `Coil` singleton)
- `rememberAsyncImagePainter` deprecated
- Package structure reorganized
- `coil-compose` coordinates changed from `io.coil-kt:coil-compose` to `io.coil-kt.coil3:coil-compose`

**Files to change:**
- `gradle/libs.versions.toml` line 20: `coil = "3.4.0"`
- `gradle/libs.versions.toml` line 88: group may change to `io.coil-kt.coil3`
- All files that use `AsyncImage`, `rememberAsyncImagePainter`, `ImageRequest`, `ImageLoader`
- Source files to check: `app/src/main/java/com/junkfood/seal/ui/common/AsyncImageImpl.kt` and anywhere using Coil APIs

**Code changes needed (estimated: moderate).** Example:
```kotlin
// Coil 2.x
AsyncImage(
    model = imageUrl,
    contentDescription = null,
)

// Coil 3.x
AsyncImage(
    model = ImageRequest.Builder(LocalContext.current)
        .data(imageUrl)
        .build(),
    contentDescription = null,
)
```

---

### 3.15 Koin (4.0.0 → 4.2.1)

**Files to change:**
- `gradle/libs.versions.toml` line 39: `koin = "4.2.1"`

**Potential code changes:**
- `koin-androidx-compose` module API may have changed
- `KoinApplication`, `koinViewModel` etc. API changes in Koin 4.2
- Check `startKoin` DSL, `module { }`, `viewModel { }` — likely backward-compatible
- **Recommendation:** Update version, compile, fix any deprecation warnings

---

### 3.16 OkHttp (4.12.0 → 4.12.0 / 5.4.0)

**Option A — Stay on 4.x (recommended unless coroutine-based calls are desired):**
- Keep at 4.12.0. No changes needed.

**Option B — Upgrade to 5.4.0:**
- OkHttp 5.x uses Kotlin coroutines
- `OkHttpClient.Builder` API largely the same
- `Callback` interfaces still work, but new coroutine `await()` extensions available
- **Recommendation:** Stay on 4.12.0 unless the project needs coroutine-based HTTP calls

**Files to change (Option B):**
- `gradle/libs.versions.toml` line 29: `okhttp = "5.4.0"`

---

### 3.17 Coroutines (1.10.1 → 1.11.0)

**Files to change:**
- `gradle/libs.versions.toml` line 27: `coroutines = "1.11.0"`
- No code changes expected. Binary-compatible with Kotlin 2.3.x.

---

### 3.18 Serialization (1.7.3 → 1.11.0)

**Files to change:**
- `gradle/libs.versions.toml` line 28: `serialization = "1.11.0"`
- No code changes expected. Check for deprecated APIs in `@Serializable` classes.

---

### 3.19 Datetime (0.6.1 → 0.8.0)

**Files to change:**
- `gradle/libs.versions.toml` line 27 (wait, datetime is on different line): `datetime = "0.8.0"`

**Potential code changes:**
- `TimeZone` serialization is deprecated in 0.8.0. Search codebase for `@Serializable` usage with `TimeZone` properties
- If used, migrate to `TimeZone` IDs (string) instead
- Maven also offers `0.8.0-0.6.x-compat` variant for backward compatibility with 0.6.x APIs

---

### 3.20 ktfmt plugin (0.20.1 → 0.63)

**⚠️ Requires verification of plugin ID.**

Current: `com.ncorti.ktfmt.gradle` version `0.20.1`
Latest: The plugin may have moved to `com.facebook.ktfmt` or `org.ktfmt.ktfmt` (community fork).

**If plugin ID changed:**
- Update in `gradle/libs.versions.toml` line 122: plugin ID and version
- Update in `build.gradle.kts` files that apply the plugin
- ktfmt configuration DSL may differ (built-in styles, `kotlinlangStyle()` method may have changed)

**Recommended approach:** Verify latest plugin coordinates at https://github.com/facebook/ktfmt or the Gradle Plugin Portal before upgrading.

---

### 3.21 MMKV (1.3.12 → 1.3.16 LTS)

**Files to change:**
- `gradle/libs.versions.toml` line 36: `mmkv = "1.3.16"`
- Update comment on line 37: `# pin to v1.3.x for 32-bit support`

**No upgrade to 2.4.0** — breaking `MMKVHandler` callback changes and 32-bit support concern.

---

### 3.22 Gradle Develocity (3.19 → 4.4.2)

**Files to change:**
- `settings.gradle.kts` line 10: `id("com.gradle.develocity") version("4.4.2")`

**Potential code changes:**
- Develocity 4.x DSL changes: `buildScan { }` configuration may use different property names
- `termsOfUseUrl` / `termsOfUseAgree` → may be replaced with `buildScanPublished { }`
- Check migration guide at docs.gradle.com

---

### 3.23 Foojay Resolver (0.4.0 → 1.0.0)

**Files to change:**
- `settings.gradle.kts` line 9: `id("org.gradle.toolchains.foojay-resolver-convention") version("1.0.0")`
- No code changes expected.

---

## 4. Recommended Update Order

```
Phase 1 — Safe, non‑breaking updates (no code changes needed)
  ──────────────────────────────────────────────────────────
  1. Gradle wrapper          9.4.1  →  9.5.1     (./gradlew wrapper --gradle-version=9.5.1)
  2. AGP                     9.2.0  →  9.2.1     (single line in libs.versions.toml)
  3. Foojay resolver         0.4.0  →  1.0.0     (single line in settings.gradle.kts)
  4. Develocity              3.19   →  4.4.2     (single line + DSL check)
  5. Navigation Compose      2.9.6  →  2.9.8     (single line)
  6. Core KTX                1.17.0 →  1.19.0    (single line)
  7. Google Material         1.12.0 →  1.14.0    (single line)
  8. Espresso                3.6.1  →  3.7.0     (single line)
  9. Test Ext                1.2.1  →  1.3.0     (single line)
 10. ConstraintLayout        1.1.0  →  1.1.1     (single line)
 11. Graphics Shapes         1.0.4  →  1.1.0     (single line)
 12. Coroutines              1.10.1 →  1.11.0    (single line)
 13. MMKV                    1.3.12 →  1.3.16    (single line, stay on LTS)

Phase 2 — Needs compile verification (may need minor code tweaks)
  ──────────────────────────────────────────────────────────
 14. Compose BOM             2025.01.00 → 2026.05.01
 15. Activity Compose        1.12.2 → 1.13.0
 16. Serialization           1.7.3  → 1.11.0
 17. Datetime                0.6.1  → 0.8.0
 18. Koin                    4.0.0  → 4.2.1
 19. Biometric               1.2.0-alpha05 → TBD (see §3.9 — no newer stable exists)

Phase 3 — Major migration (dedicated effort, test thoroughly)
  ──────────────────────────────────────────────────────────
 20. ktfmt plugin            0.20.1 → 0.63      (verify plugin ID first)
 21. Accompanist             remove entirely    (migrate to Compose built-ins)

Phase 4 — Optional / Future
  ──────────────────────────────────────────────────────────
 22. Coil                    2.7.0  → 3.4.0     (dedicated migration project)
 23. OkHttp                  4.12.0 → 5.4.0     (only if coroutine-based HTTP needed)

Phase 5 — BLOCKED (wait for KSP support)
  ──────────────────────────────────────────────────────────
 24. Kotlin                  2.3.21 → 2.4.0     BLOCKER: no KSP for 2.4.0 yet
```

---

## 5. Dependencies to Skip / Hold

| Dependency | Reason to Skip/Hold |
|---|---|
| **Kotlin 2.4.0** | **BLOCKED** — KSP 2.3.9 targets Kotlin 2.3.x, no KSP release for 2.4.0 yet (as of June 9, 2026). Must wait for `google/ksp` to publish 2.4.0-compatible version. Also affects: KSP plugin, Room compiler (KSP), and any other KSP-based processors |
| **Accompanist ≥ 0.37.0** | 0.37.0 removed `pager`, `pager-indicators`, `webview`, `swiperefresh`, `flowlayout` modules entirely. Project currently uses `permissions`, `webview`, and `pager-indicators`. Recommend migrating to Compose built-ins and dropping accompanist entirely rather than upgrading |
| **Coil 3.x** | Major API break. AsyncImage/ImageLoader API redesigned. `coil-compose` Maven coordinates changed from `io.coil-kt` to `io.coil-kt.coil3`. Requires a dedicated migration pass |
| **OkHttp 5.x** | No urgent need. 4.12.0 is the last stable 4.x and works fine. 5.x migration optional unless coroutine-based HTTP calls are desired |
| **MMKV 2.x** | Breaking `MMKVHandler` callback change. Project is pinned to v1.3.x for 32-bit ARM support. Stick with 1.3.16 LTS |
| **yt-dlp android** | No newer release available. Version 0.18.1 is current |
| **JUnit 4** | 4.13.2 remains the latest stable. No update available |
| **Lifecycle** | 2.10.0 is already the latest stable |
| **Room** | 2.8.4 is already the latest stable |
| **AppCompat** | 1.7.1 is already the latest stable |

---

## 6. Summary of Build File Changes

### `gradle/libs.versions.toml` — version refs to update

| Line | Key | Current → Latest |
|---|---|---|
| 3 | `androidGradlePlugin` | 9.2.0 → 9.2.1 |
| 4 | `androidxComposeBom` | 2025.01.00 → 2026.05.01 |
| 5 | `androidxCore` | 1.17.0 → 1.19.0 |
| 6 | `androidMaterial` | 1.12.0 → 1.14.0 |
| 8 | `androidxActivity` | 1.12.2 → 1.13.0 |
| 9 | `androidxBiometric` | 1.2.0-alpha05 → TBD (no newer stable; see §3.9) |
| 10 | `graphics` | 1.0.4 → 1.1.0 |
| 11 | `constraintLayout` | 1.1.0 → 1.1.1 |
| 14 | `androidxNavigation` | 2.9.6 → 2.9.8 |
| 16 | `androidxEspresso` | 3.6.1 → 3.7.0 |
| 17 | `androidxTestExt` | 1.2.1 → 1.3.0 |
| 20 | `coil` | 2.7.0 → 3.4.0 (Phase 4) |
| 24 | `kotlin` | 2.3.21 → 2.4.0 (Phase 5 — BLOCKED) |
| 26 | `coroutines` | 1.10.1 → 1.11.0 |
| 27 | `datetime` | 0.6.1 → 0.8.0 |
| 28 | `serialization` | 1.7.3 → 1.11.0 |
| 29 | `okhttp` | 4.12.0 → 4.12.0 (skip) or 5.4.0 |
| 32 | `ksp` | 2.3.9 → TBD (wait for Kotlin 2.4.0 support) |
| 36 | `mmkv` | 1.3.12 → 1.3.16 |
| 39 | `koin` | 4.0.0 → 4.2.1 |
| 41 | `ktfmt` | 0.20.1 → 0.63 (verify plugin ID first) |

### `settings.gradle.kts` — plugin versions

| Line | Plugin | Current → Latest |
|---|---|---|
| 9 | `foojay-resolver-convention` | 0.4.0 → 1.0.0 |
| 10 | `develocity` | 3.19 → 4.4.2 |

### `gradle/wrapper/gradle-wrapper.properties`

| Property | Current → Latest |
|---|---|
| `distributionUrl` | `gradle-9.4.1-bin.zip` → `gradle-9.5.1-bin.zip` |

### Optional: Accompanist removal

If migrating away from accompanist, remove:
- `gradle/libs.versions.toml` lines 2, 54-56 (accompanist version and library entries)
- `bundles.accompanist` block
- `app/build.gradle.kts` line 160: `implementation(libs.bundles.accompanist)`
- Replace each accompanist usage with Compose/AndroidX equivalent

---

**Ready for review.** No files have been modified. Proceed with phase 1 when given the go-ahead.
