# Proxy Settings Architecture Diagram

## System Architecture

```
┌─────────────────────────────────────────────────────────────────┐
│                        Sealplus Application                      │
├─────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │                    UI Layer (Compose)                     │   │
│  │                                                            │   │
│  │  ┌────────────────────────────────────────────────────┐  │   │
│  │  │         ProxySettingsPage.kt                       │  │   │
│  │  │  ┌──────────────┐  ┌──────────────┐               │  │   │
│  │  │  │ Free Proxy   │  │ Custom Proxy │               │  │   │
│  │  │  │   Section    │  │   Section    │               │  │   │
│  │  │  └──────────────┘  └──────────────┘               │  │   │
│  │  │  ┌──────────────┐  ┌──────────────┐               │  │   │
│  │  │  │  Connection  │  │  Speed Test  │               │  │   │
│  │  │  │    Status    │  │   Results    │               │  │   │
│  │  │  └──────────────┘  └──────────────┘               │  │   │
│  │  └────────────────────────────────────────────────────┘  │   │
│  └────────────────────┬────────────────────────────────────┘   │
│                       │                                          │
│  ┌────────────────────▼────────────────────────────────────┐   │
│  │                Business Logic Layer                      │   │
│  │                                                           │   │
│  │  ┌──────────────────┐        ┌──────────────────┐       │   │
│  │  │  ProxyManager    │        │  ProxyValidator  │       │   │
│  │  │                  │        │                  │       │   │
│  │  │ • Config Mgmt    │        │ • Connection Test│       │   │
│  │  │ • API Client     │        │ • Speed Test     │       │   │
│  │  │ • Persistence    │        │ • Quick Ping     │       │   │
│  │  │ • Type Enums     │        │ • Network Check  │       │   │
│  │  └────────┬─────────┘        └──────────┬───────┘       │   │
│  └───────────┼───────────────────────────────┼─────────────┘   │
│              │                               │                  │
│  ┌───────────▼───────────────────────────────▼─────────────┐   │
│  │              Persistence Layer (MMKV)                    │   │
│  │                                                           │   │
│  │  ┌────────────────────────────────────────────────────┐ │   │
│  │  │         PreferenceUtil.kt                          │ │   │
│  │  │                                                    │ │   │
│  │  │  PROXY_ENABLED         PROXY_USE_FREE            │ │   │
│  │  │  PROXY_FREE_COUNTRY    PROXY_FREE_ADDRESS        │ │   │
│  │  │  PROXY_CUSTOM_HOST     PROXY_CUSTOM_PORT         │ │   │
│  │  │  PROXY_CUSTOM_TYPE     PROXY_LAST_VALIDATED      │ │   │
│  │  │  PROXY_IS_WORKING                                │ │   │
│  │  └────────────────────────────────────────────────────┘ │   │
│  └───────────────────────────────────────────────────────────┘   │
│                                                                   │
│  ┌──────────────────────────────────────────────────────────┐   │
│  │           Network Integration Layer                       │   │
│  │                                                            │   │
│  │  ┌──────────────┐  ┌──────────────┐  ┌──────────────┐   │   │
│  │  │DownloadUtil  │  │ UpdateUtil   │  │ SponsorUtil  │   │   │
│  │  │              │  │              │  │              │   │   │
│  │  │ yt-dlp proxy │  │ OkHttp proxy │  │ OkHttp proxy │   │   │
│  │  └──────────────┘  └──────────────┘  └──────────────┘   │   │
│  │  ┌──────────────┐                                        │   │
│  │  │FormatValidator│                                       │   │
│  │  │              │                                        │   │
│  │  │ OkHttp proxy │                                        │   │
│  │  └──────────────┘                                        │   │
│  └───────────────────────┬──────────────────────────────────┘   │
│                          │                                       │
└──────────────────────────┼───────────────────────────────────────┘
                           │
┌──────────────────────────▼───────────────────────────────────────┐
│                    External Services                              │
├───────────────────────────────────────────────────────────────────┤
│                                                                   │
│  ┌────────────────────┐  ┌────────────────────┐                 │
│  │  ProxyScrape API   │  │  Test URLs         │                 │
│  │                    │  │                    │                 │
│  │  • Free proxies    │  │  • google.com      │                 │
│  │  • 5 countries     │  │  • bing.com        │                 │
│  │  • HTTP protocol   │  │  • robots.txt      │                 │
│  └────────────────────┘  └────────────────────┘                 │
│                                                                   │
│  ┌────────────────────┐  ┌────────────────────┐                 │
│  │  Download Sources  │  │  GitHub API        │                 │
│  │                    │  │                    │                 │
│  │  • YouTube         │  │  • Releases        │                 │
│  │  • Instagram       │  │  • Sponsors        │                 │
│  │  • TikTok          │  │                    │                 │
│  │  • 1000+ sites     │  │                    │                 │
│  └────────────────────┘  └────────────────────┘                 │
│                                                                   │
└───────────────────────────────────────────────────────────────────┘
```

## Data Flow Diagrams

### Free Proxy Configuration Flow

```
User Action                  UI Layer              Business Logic           External API
────────────                 ────────              ──────────────           ────────────
    │                            │                        │                      │
    │ Select Country             │                        │                      │
    ├──────────────────────────► │                        │                      │
    │                            │ fetchFreeProxies()     │                      │
    │                            ├──────────────────────► │                      │
    │                            │                        │ API Request          │
    │                            │                        ├────────────────────► │
    │                            │                        │                      │
    │                            │                        │ ◄────────────────────┤
    │                            │                        │   Proxy List         │
    │                            │ ◄──────────────────────┤                      │
    │                            │   List<String>         │                      │
    │ ◄──────────────────────────┤                        │                      │
    │   Display Dialog           │                        │                      │
    │                            │                        │                      │
    │ Select Proxy               │                        │                      │
    ├──────────────────────────► │                        │                      │
    │                            │ saveProxyConfig()      │                      │
    │                            ├──────────────────────► │                      │
    │                            │                        │ Save to MMKV         │
    │                            │                        ├────────────────────► │
    │                            │ ◄──────────────────────┤                      │
    │ ◄──────────────────────────┤                        │                      │
    │   Configuration Saved      │                        │                      │
```

### Connection Testing Flow

```
User Action                  UI Layer              Business Logic           Test Server
────────────                 ────────              ──────────────           ───────────
    │                            │                        │                      │
    │ Test Connection            │                        │                      │
    ├──────────────────────────► │                        │                      │
    │                            │ validateProxyConnection│                      │
    │                            ├──────────────────────► │                      │
    │                            │                        │ HEAD Request         │
    │                            │                        ├────────────────────► │
    │                            │                        │                      │
    │                            │                        │ ◄────────────────────┤
    │                            │                        │   200 OK (45ms)      │
    │                            │ ◄──────────────────────┤                      │
    │                            │   Success(45ms)        │                      │
    │ ◄──────────────────────────┤                        │                      │
    │   Show Success Badge       │                        │                      │
    │                            │ updateConfig(working)  │                      │
    │                            ├──────────────────────► │                      │
    │                            │                        │ Save Status          │
    │                            │                        ├────────────────────► │
    │                            │ ◄──────────────────────┤                      │
```

### Download with Proxy Flow

```
User Action                  Download System       Proxy Layer              Target Server
────────────                 ───────────────       ───────────              ─────────────
    │                            │                        │                      │
    │ Start Download             │                        │                      │
    ├──────────────────────────► │                        │                      │
    │                            │ Check ProxyManager     │                      │
    │                            ├──────────────────────► │                      │
    │                            │ ◄──────────────────────┤                      │
    │                            │   getActiveProxy()     │                      │
    │                            │                        │                      │
    │                            │ Create Request         │                      │
    │                            │ with Proxy             │                      │
    │                            │                        │                      │
    │                            │ Execute via yt-dlp     │                      │
    │                            │ --proxy host:port      │                      │
    │                            ├──────────────────────► │                      │
    │                            │                        │ Forward Request      │
    │                            │                        ├────────────────────► │
    │                            │                        │                      │
    │                            │                        │ ◄────────────────────┤
    │                            │                        │   File Data          │
    │                            │ ◄──────────────────────┤                      │
    │                            │   Downloaded           │                      │
    │ ◄──────────────────────────┤                        │                      │
    │   Download Complete        │                        │                      │
```

## Component Interaction Matrix

```
┌────────────────────┬──────────┬──────────┬──────────┬───────────┬──────────┐
│    Component       │  Proxy   │  Proxy   │Preference│ OkHttp   │ yt-dlp   │
│                    │ Manager  │Validator │  Util    │ Client   │          │
├────────────────────┼──────────┼──────────┼──────────┼───────────┼──────────┤
│ ProxySettingsPage  │    ●     │    ●     │    ○     │    ○     │    ○     │
│ ProxyManager       │    -     │    ○     │    ●     │    ●     │    ○     │
│ ProxyValidator     │    ○     │    -     │    ○     │    ●     │    ○     │
│ DownloadUtil       │    ●     │    ○     │    ●     │    ○     │    ●     │
│ UpdateUtil         │    ●     │    ○     │    ○     │    ●     │    ○     │
│ SponsorUtil        │    ●     │    ○     │    ○     │    ●     │    ○     │
│ FormatValidator    │    ●     │    ○     │    ○     │    ●     │    ○     │
└────────────────────┴──────────┴──────────┴──────────┴───────────┴──────────┘

Legend:
  ●  Direct dependency / Strong coupling
  ○  Indirect dependency / Weak coupling
  -  Self reference
```

## State Machine Diagram

### Proxy Configuration State

```
                    ┌─────────────┐
                    │   Disabled  │
                    │  (Initial)  │
                    └──────┬──────┘
                           │
                           │ User enables toggle
                           ▼
                    ┌─────────────┐
                    │   Enabled   │
           ┌────────┤  (No Config)├────────┐
           │        └─────────────┘        │
           │                                │
           │ Free Proxy          Custom Proxy
           ▼                                ▼
    ┌─────────────┐                ┌──────────────┐
    │ Free Config │                │Custom Config │
    │  Selected   │                │  Entered     │
    └──────┬──────┘                └──────┬───────┘
           │                               │
           │ Test Connection   Test Connection
           ▼                               ▼
    ┌─────────────┐                ┌──────────────┐
    │   Testing   │                │   Testing    │
    └──────┬──────┘                └──────┬───────┘
           │                               │
    ┌──────┴──────┐               ┌───────┴────────┐
    │             │               │                │
Success         Failed       Success            Failed
    │             │               │                │
    ▼             ▼               ▼                ▼
┌───────┐    ┌────────┐      ┌───────┐      ┌────────┐
│Working│    │Not Work│      │Working│      │Not Work│
│  ✓    │    │   ✗    │      │  ✓    │      │   ✗    │
└───────┘    └────────┘      └───────┘      └────────┘
```

## Class Relationship Diagram

```
                    ┌──────────────────┐
                    │ ProxySettingsPage│
                    │   (Composable)   │
                    └────────┬─────────┘
                             │
                    ┌────────┴────────┐
                    │                 │
                    ▼                 ▼
         ┌──────────────────┐  ┌──────────────────┐
         │  ProxyManager    │  │ ProxyValidator   │
         │                  │  │                  │
         │ + ProxyConfig    │  │ +ValidationResult│
         │ + ProxyType      │  │ +SpeedTestResult │
         │ + ProxyCountry   │  │                  │
         │                  │  │                  │
         │ +fetchFreeProxies│  │ +validateProxy() │
         │ +saveConfig()    │  │ +performSpeedTest│
         │ +loadConfig()    │  │ +quickPing()     │
         │ +getActiveProxy()│  │                  │
         └────────┬─────────┘  └──────────┬───────┘
                  │                       │
                  │                       │
                  ▼                       ▼
         ┌──────────────────┐  ┌──────────────────┐
         │ PreferenceUtil   │  │   OkHttpClient   │
         │                  │  │                  │
         │ +PROXY_ENABLED   │  │ +Builder()       │
         │ +PROXY_USE_FREE  │  │ +proxy()         │
         │ +...             │  │ +build()         │
         └──────────────────┘  └──────────────────┘
                  │
                  │
                  ▼
         ┌──────────────────┐
         │      MMKV        │
         │  (Persistence)   │
         └──────────────────┘
```

## Sequence Diagram: Complete User Journey

```
User    UI        ProxyManager    ProxyScrape    ProxyValidator    MMKV       OkHttp
 │      │              │               │                │            │           │
 │ Open Settings       │               │                │            │           │
 ├────► │              │               │                │            │           │
 │      │ Load Config  │               │                │            │           │
 │      ├──────────────►│               │                │            │           │
 │      │              │ Read Prefs    │                │            │           │
 │      │              ├───────────────────────────────────────────► │           │
 │      │              │ ◄──────────────────────────────────────────┤           │
 │      │ ◄────────────┤               │                │            │           │
 │ ◄────┤              │               │                │            │           │
 │      │              │               │                │            │           │
 │ Enable Proxy        │               │                │            │           │
 ├────► │              │               │                │            │           │
 │      │ Save(enabled)│               │                │            │           │
 │      ├──────────────►│               │                │            │           │
 │      │              │ Write MMKV    │                │            │           │
 │      │              ├───────────────────────────────────────────► │           │
 │      │              │ ◄──────────────────────────────────────────┤           │
 │      │ ◄────────────┤               │                │            │           │
 │ ◄────┤              │               │                │            │           │
 │      │              │               │                │            │           │
 │ Fetch Proxies       │               │                │            │           │
 ├────► │              │               │                │            │           │
 │      │ Fetch(US)    │               │                │            │           │
 │      ├──────────────►│               │                │            │           │
 │      │              │ HTTP GET      │                │            │           │
 │      │              ├──────────────►│                │            │           │
 │      │              │ ◄──────────────┤                │            │           │
 │      │              │  Proxy List   │                │            │           │
 │      │ ◄────────────┤               │                │            │           │
 │ ◄────┤              │               │                │            │           │
 │ Show Dialog         │               │                │            │           │
 │      │              │               │                │            │           │
 │ Select Proxy        │               │                │            │           │
 ├────► │              │               │                │            │           │
 │      │ Save Config  │               │                │            │           │
 │      ├──────────────►│               │                │            │           │
 │      │              │ Write MMKV    │                │            │           │
 │      │              ├───────────────────────────────────────────► │           │
 │      │ ◄────────────┤               │                │            │           │
 │ ◄────┤              │               │                │            │           │
 │      │              │               │                │            │           │
 │ Test Connection     │               │                │            │           │
 ├────► │              │               │                │            │           │
 │      │ Validate()   │               │                │            │           │
 │      ├──────────────────────────────────────────────►│            │           │
 │      │              │               │                │ Build OkHttp│           │
 │      │              │               │                ├────────────────────────►│
 │      │              │               │                │            │  HEAD Req │
 │      │              │               │                │            │───────────►│
 │      │              │               │                │            │◄───────────┤
 │      │              │               │                │◄───────────────────────┤│
 │      │              │               │                │  Success   │           │
 │      │ ◄──────────────────────────────────────────────┤            │           │
 │      │  Success(45ms)              │                │            │           │
 │ ◄────┤              │               │                │            │           │
 │ Show Success        │               │                │            │           │
 │      │              │               │                │            │           │
 │ Download Video      │               │                │            │           │
 ├────► │              │               │                │            │           │
 │      │ Get Proxy    │               │                │            │           │
 │      ├──────────────►│               │                │            │           │
 │      │ ◄────────────┤               │                │            │           │
 │      │  192.168.1.1:8080            │                │            │           │
 │      │              │               │                │            │           │
 │      │ Download via yt-dlp --proxy 192.168.1.1:8080  │            │           │
 │      ├───────────────────────────────────────────────────────────────────────►│
 │      │              │               │                │            │           │
 │      │ ◄──────────────────────────────────────────────────────────────────────┤
 │ ◄────┤              │               │                │            │           │
 │ Download Complete   │               │                │            │           │
```

This architecture provides a clean separation of concerns, maintainable code structure, and efficient data flow throughout the application.
