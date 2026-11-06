# Project Audit: Eyes

## 1. Architecture & Flow
- **Android Client (`EyesService.java`)**: 
  - Runs as an Android `Service` utilizing a Foreground Notification to prevent system termination.
  - Periodic loop (`Handler` + `Runnable`, 60-second interval) fetches battery telemetry via `BatteryManager` and latest GPS/Network coordinates via `LocationManager`.
  - Transmits data securely via direct HTTP POST to Supabase REST endpoints using anonymous API keys.
- **Backend & Database (Supabase USRC)**:
  - `eyes_logs`: Stores telemetry records (location, battery, network, timestamps).
  - `eyes_commands`: Stores remote trigger commands (`capture_photo`, `record_audio`, `panic_alert`).
- **Dashboard (`App.jsx`)**:
  - React component featuring Supabase Realtime subscriptions (`postgres_changes`) for instant telemetry updates.
  - Interactive control panel for issuing remote commands.

## 2. Security & Compliance
- **Data Transport**: TLS encryption over HTTPS to Supabase REST API.
- **Privilege & Stealth**: Foreground service keeps telemetry alive without conspicuous UI activity; launcher icon suppression needs to be configured in `AndroidManifest.xml` (`android:excludeFromRecents="true"`).

## 3. Recommendations
- Add local fallback queue (SQLite) on Android if network connection drops.
- Implement token authentication or row-level security (RLS) policies on Supabase tables to restrict unauthorized write access.
