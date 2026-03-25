Companion app for [VibeScout Web](https://github.com/simoneraffaelli/vibescout-web) \
Inspired by [BopSpotter](https://walzr.com/bop-spotter) by [Riley Waltz](https://x.com/rtwlz) \
Uses:
 - [SongRec](https://github.com/marin-m/SongRec) for fingerprinting (Rust native library via JNI)
 - [Shazam](https://www.shazam.com/) for recognizing

---

# VibeScout — Android Companion App

VibeScout is an Android app that continuously listens to ambient audio, recognizes songs via Shazam's fingerprinting algorithm, and pushes matched tracks to a [VibeScout Web](https://github.com/simoneraffaelli/vibescout-web) instance. It is designed to run unattended on a phone or dedicated device, acting as a "music spotter" for a given location.

---

## Architecture Overview

| Component | Technology |
|---|---|
| Language | Kotlin |
| UI | Jetpack Compose (Material 3) |
| Background Processing | WorkManager (CoroutineWorker) |
| Audio Fingerprinting | Native Rust library (`libsongrec_fingerprint.so`) via JNI |
| Networking | Retrofit 3 + OkHttp + Gson |
| Permissions | XXPermissions |
| Logging | Timber |
| Min SDK | 28 (Android 9.0) |
| Target SDK | 36 |

### High-Level Data Flow

```
┌──────────────────────────────────────────────────────────────┐
│                     VibeScout App Flow                        │
└──────────────────────────────────────────────────────────────┘

    MainActivity (Compose UI)
   ├─ API Key Input ──→ SharedPreferences
   ├─ Start Button ───→ WorkerUtils.startWorker()
   └─ Stop Button ────→ WorkerUtils.stopWorker()
             ↓
    VibeScoutWorker (Foreground Service)
             ↓
   ┌─────────────────────────────────────────┐
   │   Main Loop (every ~2 minutes)          │
   │                                         │
   │ 1. Check permissions & network          │
   │ 2. Acquire Wi-Fi lock                   │
   │ 3. Send heartbeat ──→ POST /api/heartbeat│
   │ 4. Record 12s audio ──→ AudioRecorder   │
   │                              ↓          │
   │ 5. Fingerprint + Recognize via JNI      │
   │         SongRecFingerprint (Rust)       │
   │              ↓ (Shazam JSON)            │
   │ 6. Parse match ──→ MatchChecker         │
   │              ↓                          │
   │ 7. POST to API ──→ VibescoutApiClient   │
   │         POST /api/tracks                │
   │              ↓                          │
   │ 8. Release Wi-Fi lock                   │
   │ 9. Sleep 120s (or 60s if no network)    │
   │10. Repeat                               │
   └─────────────────────────────────────────┘
             ↓
    Foreground Notification
   ├─ "Service running" (while active)
   └─ "Service Stopped" (on stop)
```

---

## Project Structure

```
app/src/main/java/ooo/simone/vibescout/
├── MainActivity.kt                  # Compose UI — key input, start/stop controls, status
├── core/
│   ├── Const.kt                     # Global constants (delays, URLs, notification IDs)
│   ├── SongRecFingerprint.kt        # JNI bridge to native Rust fingerprint library
│   ├── audio/
│   │   ├── AudioRecorder.kt         # Microphone capture (16 kHz, mono, PCM float)
│   │   ├── AudioRecognizer.kt       # Wraps SongRecFingerprint for recognition
│   │   └── MatchChecker.kt          # Parses Shazam JSON response into Track
│   ├── api/
│   │   ├── VibescoutApiClient.kt    # Retrofit interface (POST /api/tracks, POST /api/heartbeat)
│   │   ├── ServiceGenerator.kt      # Retrofit/OkHttp factory
│   │   ├── ApiClient.kt             # Singleton accessor
│   │   ├── ApiManager.kt            # Convenience wrapper with auth header
│   │   └── models/
│   │       ├── TrackRequest.kt      # { title, artist }
│   │       ├── TrackResponse.kt     # { id, title, artist, spottedAt, deviceId }
│   │       └── HeartbeatResponse.kt # { ok }
│   ├── workers/
│   │   ├── VibeScoutWorker.kt       # Core background loop (record → recognize → report)
│   │   └── WorkerUtils.kt           # Start, stop, observe worker status
│   ├── notifications/local/
│   │   ├── NotificationBuilder.kt   # Builds foreground service & alert notifications
│   │   └── NotificationHelper.kt    # Posts notifications via NotificationManagerCompat
│   ├── preferences/
│   │   └── SharedPreferencesManager.kt  # API key persistence
│   ├── network/
│   │   └── NetworkUtils.kt          # Checks active validated network
│   ├── log/
│   │   ├── Log.kt                   # Internal Timber wrapper
│   │   └── LogUtils.kt             # Public logging DSL (i/d/w/e functions)
│   ├── data/
│   │   └── Track.kt                 # Internal track model (title, artist)
│   └── exeptions/
│       └── Exceptions.kt            # NoNetwork, NoAuthKey, NoPermissions, ReleaseWifiLock
└── ui/theme/
    ├── Theme.kt                     # Material 3 dynamic color theme
    ├── Color.kt                     # Color palette
    └── Type.kt                      # Typography
```

### Native Libraries

```
app/src/main/jniLibs/
├── arm64-v8a/
│   └── libsongrec_fingerprint.so    # ARM 64-bit (most modern devices)
├── armeabi-v7a/
│   └── libsongrec_fingerprint.so    # ARM 32-bit (legacy devices)
└── x86_64/
    └── libsongrec_fingerprint.so    # Intel 64-bit (emulators)
```

The native library is a Rust implementation of Shazam's audio fingerprinting algorithm, sourced from [SongRec](https://github.com/marin-m/SongRec). It exposes three JNI methods:

| Method | Description |
|---|---|
| `generateFingerprint(samples)` | Returns a Shazam-compatible signature URI from raw PCM |
| `recognizeSong(samples)` | Generates fingerprint and queries Shazam in one step |
| `recognizeFromSignature(uri)` | Queries Shazam from a previously generated signature |

---

## How It Works

### 1. Audio Capture

`AudioRecorder` uses Android's `AudioRecord` API to capture **12 seconds** of raw audio:

- **Sample rate**: 16,000 Hz (required by Shazam's algorithm)
- **Channel**: Mono
- **Format**: 32-bit float PCM
- **Source**: Device microphone (`MediaRecorder.AudioSource.MIC`)

### 2. Fingerprinting & Recognition

The captured `FloatArray` is passed to `SongRecFingerprint.recognizeSong()` via JNI. The native Rust library:

1. Computes a spectral fingerprint of the audio
2. Encodes it into Shazam's signature format
3. Sends it to Shazam's recognition API
4. Returns the raw JSON response

### 3. Match Parsing

`MatchChecker.checkIfMatched()` parses the Shazam JSON response:

- Checks for a non-empty `matches` array
- Extracts `title` and `artist` (subtitle) from the `track` object
- Returns a `Track` object, or `null` if no match

### 4. Heartbeat

Before each recognition cycle, the worker sends a heartbeat to the backend to signal the device is alive:

```
POST /api/heartbeat
Authorization: Bearer srk_<your-api-key>
```

### 5. API Reporting

When a track is matched, `ApiManager` sends it to the VibeScout Web backend:

```
POST /api/tracks
Authorization: Bearer srk_<your-api-key>
Content-Type: application/json

{ "title": "Song Title", "artist": "Artist Name" }
```

### 6. Loop

The worker sleeps for **120 seconds**, then repeats. If the network is unavailable, the delay is reduced to **60 seconds** to retry sooner.

---

## Permissions

| Permission | Purpose |
|---|---|
| `INTERNET` | Communicate with Shazam API and VibeScout Web backend |
| `RECORD_AUDIO` | Capture ambient audio from the microphone |
| `POST_NOTIFICATIONS` | Display foreground service and status notifications |
| `FOREGROUND_SERVICE` | Keep the worker running as a persistent foreground service |
| `FOREGROUND_SERVICE_MICROPHONE` | Record audio while in the foreground service |
| `FOREGROUND_SERVICE_MEDIA_PROCESSING` | Process audio in the foreground service |

Permissions are requested at launch via [XXPermissions](https://github.com/getActivity/XXPermissions).

---

## Usage

### 1. Set Up VibeScout Web

Before using the app, you need a running [VibeScout Web](https://github.com/simoneraffaelli/vibescout-web) instance. Log in to the admin panel and **create a device** to get an API key (starts with `srk_`).

### 2. Configure the App

1. Open VibeScout on your Android device.
2. Paste the API key into the text field.
3. Tap **Save**.

The key is stored locally in SharedPreferences and is used for all subsequent API calls.

### 3. Start Listening

Tap **Start**. The app will:

- Start a foreground service with a persistent notification
- Begin the record → recognize → report cycle every ~2 minutes
- Show the current worker status on the main screen (RUNNING, ENQUEUED, etc.)

### 4. Stop Listening

Tap **Stop** on the main screen, or tap the **Stop** action in the notification.

---

## Configuration Constants

| Constant | Value | Description |
|---|---|---|
| Audio duration | 12 seconds | Length of each recording sample |
| Sample rate | 16,000 Hz | Required by Shazam fingerprint algorithm |
| Worker cycle delay | 120 seconds | Time between recognition attempts |
| No-network retry delay | 60 seconds | Reduced delay when network is unavailable |
| API base URL | `https://vibescout.simone.ooo/` | Default VibeScout Web backend |

These are defined in `core/Const.kt`.

---

## Building

### Prerequisites

- Android Studio (latest stable)
- JDK 11+
- Android SDK with API level 36

### Build & Run

```bash
# Debug build
./gradlew assembleDebug

# Install on connected device
./gradlew installDebug
```

### Supported Architectures

The app ships native libraries for:

- **arm64-v8a** — modern phones and tablets (ARM 64-bit)
- **armeabi-v7a** — legacy devices (ARM 32-bit)
- **x86_64** — emulators and Intel-based devices

Configured via `ndk.abiFilters` in `app/build.gradle.kts`.

---

## Dependencies

| Library | Version | Purpose |
|---|---|---|
| Jetpack Compose BOM | 2026.03.00 | UI toolkit |
| AndroidX Core KTX | 1.18.0 | Kotlin extensions for Android |
| AndroidX Lifecycle | 2.10.0 | Lifecycle-aware components |
| AndroidX Activity Compose | 1.13.0 | Compose integration with Activities |
| AndroidX AppCompat | 1.7.1 | Backward compatibility |
| AndroidX Work Runtime KTX | 2.11.1 | Background task scheduling (WorkManager) |
| AndroidX Work Multiprocess | 2.11.1 | Multi-process WorkManager support |
| Compose Material Icons Extended | (via BOM) | Extended Material icon set |
| Compose Runtime LiveData | (via BOM) | LiveData integration for Compose |
| Retrofit | 3.0.0 | Type-safe HTTP client |
| Retrofit Gson Converter | 3.0.0 | JSON serialization |
| OkHttp Logging Interceptor | 5.3.2 | HTTP request/response logging |
| Timber | 5.0.1 | Logging |
| XXPermissions | 28.0 | Runtime permission requests |
| DeviceCompat | 2.3 | Device compatibility checks |

---

## Background Service Details

The recognition loop runs as a `CoroutineWorker` managed by WorkManager:

- **Execution mode**: One-time work request, expedited on API 31+ with `OutOfQuotaPolicy.RUN_AS_NON_EXPEDITED_WORK_REQUEST` fallback
- **Foreground type**: `MICROPHONE` (API 29+); plain foreground service on older versions
- **Wi-Fi lock**: Acquired during each cycle to prevent Wi-Fi sleep
- **Notification**: Persistent foreground notification with a "Stop" action button

### Error Handling

| Exception | Behavior |
|---|---|
| `NoAuthKeyException` | Logs warning, skips cycle |
| `NoPermissionsException` | Logs warning, skips cycle |
| `NoNetworkException` | Reduces delay to 60s, retries |
| General exception | Logs error, continues loop |

When the worker is stopped (by user or system), it posts a "Service Stopped" notification and releases the Wi-Fi lock.

---

## API Level Compatibility

The app targets API 36 but supports devices back to **API 28 (Android 9.0)**. Several features are version-gated:

| Feature | Available From | Behavior on Older APIs |
|---|---|---|
| Dynamic color theming | API 31 | Falls back to static Material 3 colors |
| Expedited WorkManager | API 31 | Runs as standard (non-expedited) work |
| Foreground service type (microphone) | API 29 | Runs as plain foreground service |
| `FOREGROUND_SERVICE_IMMEDIATE` notification | API 31 | Omitted; default behavior used |
| `PendingIntent.FLAG_IMMUTABLE` | API 23 | Uses `FLAG_UPDATE_CURRENT` only on older |
| `POST_NOTIFICATIONS` permission | API 33 | Not needed; notifications allowed by default |
| Foreground service type permissions | API 34 | Not needed; base `FOREGROUND_SERVICE` suffices |
| `ConnectivityManager.activeNetwork` | API 29 | Falls back to iterating `allNetworks` |

---

## Security Notes

- **API key storage**: The key is stored in plaintext in SharedPreferences on the device. The VibeScout Web backend only stores a SHA-256 hash.
- **Transport**: All API communication uses HTTPS.
- **Authentication**: Each request includes the API key as a `Bearer` token in the `Authorization` header.

---

## Related Projects

- **[VibeScout Web](https://github.com/simoneraffaelli/vibescout-web)** — The web dashboard and API backend that receives and displays spotted tracks
- **[SongRec](https://github.com/marin-m/SongRec)** — Open-source Shazam client written in Rust, from which the native fingerprinting library is derived
- **[BopSpotter](https://walzr.com/bop-spotter)** — The original inspiration for the project
