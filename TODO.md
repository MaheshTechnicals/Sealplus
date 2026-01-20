# Seal Plus - Bug Fixes Implementation

## Issues Fixed

### 1. Real-time Progress Not Updating ✅ COMPLETED
- **Problem**: Video download progress on home page fails to update in real-time
- **Solution**: Convert `taskStateMap.toList()` to use `collectAsStateWithLifecycle()` with proper state observation

### 2. Duplicate Cards ✅ COMPLETED
- **Problem**: After download completion, two duplicate cards appear for the same content
- **Solution**: Filter out completed tasks from active downloads that already exist in recentDownloads

### 3. Format Selection - Suggested Section ✅ COMPLETED
- **Problem**: "Suggested" section doesn't prioritize highest resolution
- **Solution**: Added resolution extraction from format strings and sort formats by quality score (resolution × width)

## Implementation Summary

### Fix 1 & 2: NewHomePage.kt
- Added proper state collection using `collectAsStateWithLifecycle()` for real-time updates
- Added `derivedStateOf` to filter active downloads and prevent duplicates
- Logic: Only show non-completed tasks OR completed tasks not in recent downloads

### Fix 3: FormatPage.kt
- Added `extractResolution()` function to parse resolution from format strings (e.g., "1920x1080")
- Added `getQualityScore()` function to calculate quality based on resolution area
- Added `remember` with proper dependencies to sort formats by quality score

## Files Modified
1. `app/src/main/java/com/junkfood/seal/ui/page/home/NewHomePage.kt`
2. `app/src/main/java/com/junkfood/seal/ui/page/downloadv2/configure/FormatPage.kt`

## Testing Required
- Test download progress updates in real-time
- Test that completed downloads don't show duplicate cards
- Test that "Suggested" section shows highest available resolution

