# 🎛️ Controlled Release System

This repository uses a controlled release system to manage when builds are published as GitHub Releases.

## 📋 How It Works

The workflow behavior is controlled by the `release.json` file in the root directory.

### Configuration File: `release.json`

```json
{
  "release": false,
  "description": "Control release publishing. Set to true to publish releases, false to upload APKs as artifacts only."
}
```

## 🚀 Usage

### Testing Mode (Default) - `release: false`

When `release` is set to `false`:
- ✅ Build completes successfully
- ✅ APKs are signed
- ✅ Each APK variant is uploaded as a **separate artifact**:
  - `SealPlus-{version}-universal` - Universal APK (all architectures)
  - `SealPlus-{version}-arm64-v8a` - 64-bit ARM devices
  - `SealPlus-{version}-armeabi-v7a` - 32-bit ARM devices  
  - `SealPlus-{version}-x86_64` - 64-bit x86 devices
  - `SealPlus-{version}-x86` - 32-bit x86 devices
- ❌ **No GitHub Release is created**
- 📦 Artifacts are kept for **30 days** for testing

### Release Mode - `release: true`

When `release` is set to `true`:
- ✅ Build completes successfully
- ✅ APKs are signed
- ✅ **GitHub Release is created** automatically
- ✅ All APK variants are attached to the release
- ✅ Release is published with full changelog
- 📦 Backup artifacts are also created (kept for 20 days)

## 🔄 Workflow

### 1. Development & Testing Phase

```bash
# Keep release.json with release: false (default)
git add .
git commit -m "Your changes"
git push
```

Then:
1. Go to **Actions** tab in GitHub
2. Run the "Build Release APK" workflow
3. Wait for completion
4. Download individual APK artifacts from the workflow run
5. Test the APKs on your devices
6. Repeat as needed

### 2. Official Release Phase

When you're ready to publish:

```bash
# Edit release.json
{
  "release": true,
  "description": "Control release publishing. Set to true to publish releases, false to upload APKs as artifacts only."
}

# Commit and push
git add release.json
git commit -m "Enable release publishing"
git push
```

Then:
1. Go to **Actions** tab in GitHub
2. Run the "Build Release APK" workflow
3. Wait for completion
4. 🎉 Release is automatically created and published!

### 3. Return to Testing Mode

After publishing, immediately switch back to testing mode:

```bash
# Edit release.json back to false
{
  "release": false,
  "description": "Control release publishing. Set to true to publish releases, false to upload APKs as artifacts only."
}

# Commit and push
git add release.json
git commit -m "Disable release publishing"
git push
```

## 📊 Workflow Logs

The workflow will show clear messages in the logs:

**When `release: false`:**
```
⚠️ Release publishing is disabled - APKs will be uploaded as artifacts only
```

**When `release: true`:**
```
✅ Release will be published
```

## 🎯 Benefits

1. **Safe Testing**: Test builds without polluting your releases
2. **Clear Artifacts**: Each APK variant has its own artifact entry
3. **Easy Access**: Download specific APK variants directly from artifacts
4. **Version Control**: Release decision is tracked in git
5. **No Accidents**: Explicit flag prevents accidental releases
6. **Flexible**: Can test multiple builds before releasing

## 📱 Artifact Naming

Artifacts follow this naming pattern:
```
SealPlus-{version}-{architecture}
```

Examples:
- `SealPlus-1.2.4-universal`
- `SealPlus-1.2.4-arm64-v8a`
- `SealPlus-1.2.4-armeabi-v7a`

## ⚙️ Technical Details

- **Workflow File**: `.github/workflows/android.yml`
- **Config File**: `release.json`
- **JSON Parser**: Uses `jq` (pre-installed on GitHub runners)
- **Conditional Steps**: Uses `if` conditions based on release flag
- **Artifact Retention**: 30 days for testing, 20 days for backup

## 🔒 Security Note

The `release.json` file is committed to the repository, making release decisions transparent and auditable through git history.

---

**Last Updated**: January 9, 2026  
**Workflow Version**: 2.0 - Controlled Release System
