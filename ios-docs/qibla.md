# Qibla Finder / Direction - iOS/KMP Implementation Guide

## Overview

Full-screen immersive compass UI that points toward the Kaaba in Makkah. The Android implementation spans 3 files (~580 lines total). The Batoul Adhan library calculates the Qibla bearing; device sensors provide the heading. This document captures every technical decision for replication in iOS/KMP.

Key problems solved:
1. **Smooth compass rotation** without 359→0 jumps (circular wrap interpolation)
2. **Sensor fallback chain** for devices missing the rotation vector sensor
3. **Low-pass filtering** for jitter-free heading updates
4. **Dark mode + RTL** fully integrated with AppColors and Arabic cardinal labels

---

## Architecture

```
QiblaScreen
  |
  |-- QiblaViewModel (ObservableObject)
  |     |-- CLLocationManager heading updates
  |     |-- Adhan Qibla(Coordinates).direction
  |     +-- Haversine distance to Makkah
  |
  +-- Compass Canvas (SwiftUI Canvas / Path)
        |-- Outer ring
        |-- Tick marks (every 5° minor, every 30° major)
        |-- Cardinal letters (N/E/S/W or ش/شر/ج/غ)
        |-- North indicator (red triangle)
        |-- Qibla arrow (gold line + arrowhead + Kaaba marker)
        +-- Center dot (green)
```

---

## Data Models

### QiblaUiState

```swift
struct QiblaUiState {
    var qiblaBearing: Double = 0       // Adhan Qibla direction (degrees from true north)
    var deviceAzimuth: Double = 0      // Smoothed compass heading (0-360)
    var location: UserLocation? = nil
    var distanceToMakkahKm: Double = 0
    var isLoading: Bool = true
    var error: String? = nil
    var needsCalibration: Bool = false
    var isSensorAvailable: Bool = true
}
```

### Constants

```swift
let MAKKAH_LAT = 21.4225
let MAKKAH_LNG = 39.8262
```

---

## File 1: CompassManager (replaces Android's `CompassSensorManager.kt`)

Android uses `SensorManager` with `TYPE_ROTATION_VECTOR` (primary) and `TYPE_ACCELEROMETER + TYPE_MAGNETIC_FIELD` (fallback). On iOS, **`CLLocationManager`** provides heading directly — much simpler.

### iOS Equivalent

```swift
import CoreLocation
import Combine

class CompassManager: NSObject, ObservableObject, CLLocationManagerDelegate {
    private let locationManager = CLLocationManager()

    @Published var heading: Double = 0       // 0-360 degrees
    @Published var accuracy: Double = 0      // heading accuracy in degrees
    @Published var isAvailable: Bool = true
    @Published var needsCalibration: Bool = false

    private var lastHeading: Double = 0

    override init() {
        super.init()
        locationManager.delegate = self
        isAvailable = CLLocationManager.headingAvailable()
    }

    func start() {
        guard CLLocationManager.headingAvailable() else {
            isAvailable = false
            return
        }
        locationManager.headingFilter = 1  // Update on every 1° change
        locationManager.startUpdatingHeading()
    }

    func stop() {
        locationManager.stopUpdatingHeading()
    }

    func locationManager(_ manager: CLLocationManager, didUpdateHeading newHeading: CLHeading) {
        guard newHeading.headingAccuracy >= 0 else { return }

        let rawHeading = newHeading.trueHeading  // Use true north, not magnetic
        let smoothed = lowPassFilter(newValue: rawHeading, oldValue: lastHeading)
        lastHeading = smoothed
        heading = (smoothed + 360).truncatingRemainder(dividingBy: 360)
        accuracy = newHeading.headingAccuracy
        needsCalibration = newHeading.headingAccuracy > 25 || newHeading.headingAccuracy < 0
    }

    func locationManagerShouldDisplayHeadingCalibration(_ manager: CLLocationManager) -> Bool {
        return true  // Show system calibration dialog when needed
    }

    /// Low-pass filter with circular interpolation to handle 359→0 wrap.
    private func lowPassFilter(newValue: Double, oldValue: Double, alpha: Double = 0.15) -> Double {
        var delta = newValue - oldValue
        if delta > 180 { delta -= 360 }
        if delta < -180 { delta += 360 }
        return oldValue + alpha * delta
    }
}
```

### Key Differences from Android

| Aspect | Android | iOS |
|--------|---------|-----|
| API | `SensorManager` + `SensorEventListener` | `CLLocationManager` + `CLLocationManagerDelegate` |
| Primary sensor | `TYPE_ROTATION_VECTOR` at `SENSOR_DELAY_GAME` | `CLHeading.trueHeading` |
| Fallback | `TYPE_ACCELEROMETER` + `TYPE_MAGNETIC_FIELD` | Not needed — CLLocationManager handles internally |
| Raw output | Rotation matrix → orientation[0] → azimuth radians | `trueHeading` in degrees directly |
| Filter alpha | `0.15` | `0.15` (match Android) |
| Heading filter | N/A | `headingFilter = 1` (1° minimum change) |
| Calibration | `onAccuracyChanged()` with `SENSOR_STATUS_ACCURACY_LOW` | `headingAccuracy > 25` or `< 0` |

### Low-Pass Filter — Circular Interpolation

Critical for smooth rotation. Without this, the compass jumps wildly when crossing the 0°/360° boundary.

```
delta = new - old
if delta > 180  → delta -= 360    // e.g., 350→10: delta=−340 → +20
if delta < -180 → delta += 360    // e.g., 10→350: delta=+340 → −20
result = old + 0.15 * delta
```

The alpha value `0.15` balances responsiveness vs. smoothness. Lower = smoother but laggier. Higher = snappier but jittery.

---

## File 2: QiblaViewModel (replaces Android's `QiblaViewModel.kt`)

### Dependencies

- **Saved location**: From your location repository (same as Prayer Times uses)
- **Settings**: App language, Indo-Arabic numeral preference
- **CompassManager**: Heading updates
- **Adhan library**: `Qibla(Coordinates(lat, lng)).direction`

### iOS Implementation

```swift
import SwiftUI
import Adhan  // Batoul Apps Adhan library

class QiblaViewModel: ObservableObject {
    @Published var state = QiblaUiState()

    private let compassManager = CompassManager()
    private var cancellables = Set<AnyCancellable>()

    init() {
        observeCompass()
        loadLocation()
    }

    private func loadLocation() {
        // Load saved location from your repository (same source as PrayerTimes)
        guard let location = savedLocation else {
            state.isLoading = false
            state.error = "no_location"
            return
        }

        let coordinates = Coordinates(latitude: location.latitude, longitude: location.longitude)
        let qiblaDirection = Qibla(coordinates: coordinates).direction
        let distance = haversineDistance(
            lat1: location.latitude, lng1: location.longitude,
            lat2: MAKKAH_LAT, lng2: MAKKAH_LNG
        )

        state.location = location
        state.qiblaBearing = qiblaDirection
        state.distanceToMakkahKm = distance
        state.isLoading = false
    }

    private func observeCompass() {
        compassManager.$heading
            .receive(on: DispatchQueue.main)
            .sink { [weak self] heading in
                self?.state.deviceAzimuth = heading
            }
            .store(in: &cancellables)

        compassManager.$isAvailable
            .receive(on: DispatchQueue.main)
            .sink { [weak self] available in
                self?.state.isSensorAvailable = available
            }
            .store(in: &cancellables)

        compassManager.$needsCalibration
            .receive(on: DispatchQueue.main)
            .sink { [weak self] needs in
                self?.state.needsCalibration = needs
            }
            .store(in: &cancellables)
    }

    func startCompass() { compassManager.start() }
    func stopCompass() { compassManager.stop() }

    // MARK: - Haversine Distance

    static func haversineDistance(lat1: Double, lng1: Double, lat2: Double, lng2: Double) -> Double {
        let R = 6371.0  // Earth radius in km
        let dLat = (lat2 - lat1) * .pi / 180
        let dLng = (lng2 - lng1) * .pi / 180
        let a = sin(dLat / 2) * sin(dLat / 2) +
                cos(lat1 * .pi / 180) * cos(lat2 * .pi / 180) *
                sin(dLng / 2) * sin(dLng / 2)
        let c = 2 * atan2(sqrt(a), sqrt(1 - a))
        return R * c
    }
}
```

### Qibla Bearing

The Adhan library's `Qibla` class returns the direction in degrees from true north, measured clockwise. This is the same bearing convention as `CLHeading.trueHeading`, so no conversion is needed.

```swift
let coordinates = Coordinates(latitude: 40.7128, longitude: -74.0060)  // New York
let qibla = Qibla(coordinates: coordinates)
print(qibla.direction)  // ~58.48° (northeast toward Makkah)
```

---

## File 3: QiblaScreen (replaces Android's `QiblaScreen.kt`, ~450 lines)

### Screen Layout (top → bottom)

```
┌─────────────────────────────────────┐
│  ← اتجاه القبلة / Qibla Direction  │  TopAppBar (topBarBackground)
├─────────────────────────────────────┤
│  ⚠ Move phone in figure-8 pattern  │  Calibration banner (orange, conditional)
├─────────────────────────────────────┤
│        📍 Amman, Jordan             │  Location row (islamicGreen icon)
│                                     │
│          ┌─────────┐                │
│         /  COMPASS  \               │  Compass Canvas (weight fills center)
│        |   CANVAS    |              │  Rotates by -animatedAzimuth
│         \           /               │  Qibla arrow fixed at bearing angle
│          └─────────┘                │
│                                     │
│     Heading        Qibla            │  Bearing info row
│      245°          58°              │  (textPrimary / goldAccent)
│                                     │
│       8,432 km to Makkah            │  Distance (textSecondary)
│       Hold phone flat               │  Hint (textSecondary 70% alpha)
└─────────────────────────────────────┘
```

### Orientation Lock

Lock to portrait on entry, restore on exit.

**Android:**
```kotlin
DisposableEffect(Unit) {
    val activity = context as? Activity
    val originalOrientation = activity?.requestedOrientation
    activity?.requestedOrientation = ActivityInfo.SCREEN_ORIENTATION_PORTRAIT
    onDispose {
        activity?.requestedOrientation = originalOrientation ?: SCREEN_ORIENTATION_UNSPECIFIED
    }
}
```

**iOS equivalent:**
```swift
.onAppear {
    UIDevice.current.setValue(UIInterfaceOrientation.portrait.rawValue, forKey: "orientation")
    AppDelegate.orientationLock = .portrait
}
.onDisappear {
    AppDelegate.orientationLock = .all
}
```

Or use the `Info.plist` + `supportedInterfaceOrientations` approach per-view-controller.

### Sensor Lifecycle

Start sensors on appear, stop on disappear to save battery.

```swift
.onAppear { viewModel.startCompass() }
.onDisappear { viewModel.stopCompass() }
```

Android calls `startCompass()` / `stopCompass()` in `DisposableEffect(Unit)`.

---

## Compass Canvas — Full Drawing Specification

### Overview

The entire compass rotates by `-animatedAzimuth` so that North always points physically North on the device. The Qibla arrow is drawn at a fixed angle equal to `qiblaBearing` — since the canvas rotates, the arrow naturally points toward Makkah.

```swift
Canvas { context, size in
    // ... drawing code
}
.rotationEffect(.degrees(-animatedAzimuth))
```

### 1. Outer Ring

```
Shape: Circle
Stroke: 2pt, borderColor
Radius: min(width, height) / 2 - 16pt padding
Center: (width/2, height/2)
```

### 2. Tick Marks

Every 5° around the ring (72 total ticks):

| Type | Condition | Length | Width | Color |
|------|-----------|--------|-------|-------|
| Major | `angle % 30 == 0` | 16pt | 2pt | `textPrimary` |
| Minor | `angle % 30 != 0` | 8pt | 1pt | `textSecondary` |

Ticks extend inward from the outer ring edge.

```
for i in stride(from: 0, to: 360, by: 5) {
    let angle = Double(i) * .pi / 180
    let isMajor = i % 30 == 0
    let tickLength: CGFloat = isMajor ? 16 : 8
    let outerX = center.x + radius * sin(angle)
    let outerY = center.y - radius * cos(angle)
    let innerX = center.x + (radius - tickLength) * sin(angle)
    let innerY = center.y - (radius - tickLength) * cos(angle)
    // Draw line from (outerX, outerY) to (innerX, innerY)
}
```

### 3. Cardinal Letters

Positioned at `radius - 32pt` from center, at 0°/90°/180°/270°:

| Angle | English | Arabic | Color |
|-------|---------|--------|-------|
| 0° (North) | N | ش | `NorthRed` (#D32F2F), bold |
| 90° (East) | E | شر | `textPrimary` |
| 180° (South) | S | ج | `textPrimary` |
| 270° (West) | W | غ | `textPrimary` |

Font size: 18sp. Text is center-aligned both horizontally and vertically.

**Arabic cardinal abbreviations:**
- ش = شمال (North)
- شر = شرق (East)
- ج = جنوب (South)
- غ = غرب (West)

### 4. North Indicator

Red filled triangle just outside the ring at 0°:

```
Triangle size: 10pt per side
Tip: (center.x, center.y - radius - 4pt)
Base: 10pt below tip, centered
Color: NorthRed = #D32F2F (same in light & dark mode)
```

### 5. Qibla Arrow (gold)

Thick line from center toward Qibla bearing, with arrowhead and Kaaba marker:

```
Arrow line:
  Start: center
  End: center + (radius - 40pt) in Qibla bearing direction
  Width: 4pt
  Color: goldAccent
  Cap: round

Arrowhead:
  Filled triangle at arrow tip
  Size: 14pt
  Spread angle: 25° (each side)
  Color: goldAccent

Kaaba marker (at arrow tip):
  Outer circle: radius 8pt, filled goldAccent
  Inner square: 5pt side, filled Color.black at 70% alpha
  Centered on arrow endpoint
```

### 6. Center Dot

```
Circle at center
Radius: 6pt
Color: islamicGreen
```

---

## Compass Animation — Circular Wrap Handling

### The Problem

Direct angle animation (e.g., from 350° to 10°) would animate through 340° of rotation backwards instead of 20° forward. This makes the compass spin wildly when crossing North.

### The Solution: Accumulated Delta

Android uses an `animationBase` float that accumulates small deltas, avoiding the 0°/360° boundary entirely:

```swift
@State private var previousTarget: Double = 0
@State private var animationBase: Double = 0

// On each heading update:
func updateAnimationBase(newTarget: Double) {
    var delta = newTarget - previousTarget
    if delta > 180 { delta -= 360 }
    if delta < -180 { delta += 360 }
    animationBase += delta
    previousTarget = newTarget
}
```

The `animationBase` grows unbounded (e.g., 720°, 1080°) but the animation system only cares about the difference, not the absolute value.

### SwiftUI Animation

```swift
// Animate with spring for natural compass feel
.rotationEffect(.degrees(-animationBase))
.animation(
    .spring(response: 0.5, dampingFraction: 0.7),
    value: animationBase
)
```

**Android spring parameters → iOS mapping:**
| Android | Value | iOS equivalent |
|---------|-------|----------------|
| `dampingRatio` | 0.7 | `dampingFraction: 0.7` |
| `stiffness` | 100 | `response: ~0.5` (response ≈ 2π / √stiffness) |

---

## Dark Mode Color Mapping

| UI Element | AppColors Token | Light | Dark |
|---|---|---|---|
| Screen background | `screenBackground` | `#FDFBF7` | `#1A1A1A` |
| TopAppBar background | `topBarBackground` | `#1B5E20` | `#1A3A1C` |
| TopAppBar text/icons | `textOnHeader` | `#FFFFFF` | `#E8F5E9` |
| Compass ring | `border` | `#E0E0E0` | `#555555` |
| Major ticks + cardinal letters | `textPrimary` | `#1A1A1A` | `#E0E0E0` |
| Minor ticks | `textSecondary` | `#757575` | `#9E9E9E` |
| North indicator | hardcoded | `#D32F2F` | `#D32F2F` |
| Qibla arrow + Kaaba circle | `goldAccent` | `#D4AF37` | `#A89050` |
| Kaaba square (inside circle) | hardcoded | `black 70%` | `black 70%` |
| Center dot | `islamicGreen` | `#2E7D32` | `#6BAF6F` |
| Location icon | `islamicGreen` | `#2E7D32` | `#6BAF6F` |
| Calibration banner bg | `orange` 15% alpha | orange 15% | orange 15% |
| Calibration banner text | `orange` | orange | orange |
| Cards | `cardBackground` | `#FFFFFF` | `#2A2A2A` |
| Body text | `textSecondary` | `#757575` | `#9E9E9E` |
| Heading value | `textPrimary` | `#1A1A1A` | `#E0E0E0` |
| Qibla value | `goldAccent` | `#D4AF37` | `#A89050` |
| Loading spinner | `islamicGreen` | `#2E7D32` | `#6BAF6F` |
| Hint text | `textSecondary` 70% alpha | — | — |

---

## RTL / Arabic Support

### Layout

Use `Environment(\.layoutDirection)` — when Arabic, the entire layout mirrors. The `TopAppBar` back arrow uses `chevron.right` (auto-mirrored) instead of `chevron.left`.

### Text

- All labels switch between English and Arabic based on `settings.appLanguage`
- Arabic text uses **Scheherazade** font (same as rest of app)
- Numbers: if `settings.useIndoArabicNumerals` is true, convert digits via `ArabicNumeralUtils.toIndoArabic()`

### Cardinal Labels on Compass

| Language | 0° | 90° | 180° | 270° |
|----------|----|-----|------|------|
| English | N | E | S | W |
| Arabic | ش | شر | ج | غ |

### Bilingual Strings

| Context | English | Arabic |
|---------|---------|--------|
| Screen title | Qibla Direction | اتجاه القبلة |
| Heading label | Heading | الاتجاه |
| Qibla label | Qibla | القبلة |
| Distance suffix | km to Makkah | كم إلى مكة المكرمة |
| Hold hint | Hold phone flat | أمسك الهاتف بشكل مسطح |
| Calibration | Move your phone in a figure-8 pattern to calibrate | حرّك هاتفك على شكل رقم ٨ لمعايرة البوصلة |
| No location error | Please set your location from Prayer Times | يرجى تحديد الموقع من صفحة مواقيت الصلاة |
| No compass error | Compass sensor not available | مستشعر البوصلة غير متوفر |

---

## Entry Points

### 1. Prayer Times Screen — Qibla Card

A clickable card between NextPrayerCard and PrayerTimesCard:

```
┌──────────────────────────────────────┐
│  🧭  Qibla Direction            →   │
│      (Icons.Default.Explore)         │
└──────────────────────────────────────┘
```

- Icon: compass/explore icon, tint `islamicGreen`
- Text: "اتجاه القبلة" / "Qibla Direction", color `textPrimary`
- Forward arrow: `ArrowForward` icon (auto-mirrored for RTL)
- Card: `cardBackground`, corner radius 10pt
- On tap: navigate to QiblaScreen

### 2. Three-Dot Overflow Menu

Added to `CommonOverflowMenu` (shared across 7+ screens):

- Position: after "Prayer Times", before "Ramadan Imsakiya"
- Icon: `Icons.Default.Explore` (compass)
- Text: "اتجاه القبلة" / "Qibla Direction"
- Parameters: `onNavigateToQibla: () -> Void`, `hideQibla: Bool = false`

Screens that include this menu item:
- BookmarksScreen
- DownloadsScreen
- QuranIndexScreen (uses custom DropdownMenu, not CommonOverflowMenu)
- AthkarCategoriesScreen
- AthkarListScreen
- TrackerScreen
- PrayerTimesScreen

---

## State Handling

### Loading State
Full-screen centered `ProgressView` with `islamicGreen` tint. No compass drawn.

### Error State — No Location
Full-screen centered text: "Please set your location from Prayer Times" / Arabic equivalent. Directs user to Prayer Times screen to set location.

### Error State — No Compass
Full-screen centered text: "Compass sensor not available" / Arabic equivalent. Rare on real devices.

### Calibration Banner
Orange-tinted card (orange at 15% alpha background) at top of compass area. Text in orange. Shown when `needsCalibration == true`. On iOS, the system may also show its own calibration dialog if `locationManagerShouldDisplayHeadingCalibration` returns true.

---

## iOS-Specific Considerations

### Permissions

`CLLocationManager.headingAvailable()` doesn't require location permission. However, `trueHeading` requires location authorization (for declination correction). If only `authorizationStatus == .notDetermined`, `magneticHeading` is used instead (slightly less accurate).

Add to `Info.plist` if not already present:
```xml
<key>NSLocationWhenInUseUsageDescription</key>
<string>Required for Qibla direction and prayer times</string>
```

### True North vs Magnetic North

Always prefer `trueHeading` over `magneticHeading`. True heading accounts for magnetic declination and is more accurate for Qibla bearing (which is calculated relative to true north by the Adhan library).

```swift
let heading = newHeading.trueHeading > 0 ? newHeading.trueHeading : newHeading.magneticHeading
```

### Background Behavior

Compass should NOT run in background. `stop()` on disappear. No background location updates needed for this feature.

### Simulator

The iOS Simulator does not have compass hardware. Use `Features > Location > Custom Location` for GPS, but heading must be tested on a real device. Consider adding a debug slider for heading during development.

---

## Verification Checklist

1. Open Prayer Times → tap Qibla card → compass screen opens with live rotation
2. Rotate phone → compass follows device heading, Qibla arrow stays fixed toward Makkah
3. Test in dark mode — all colors readable, no bright spots
4. Test in Arabic — RTL layout, Arabic cardinal letters, Scheherazade font, Indo-Arabic numerals
5. Navigate to Qibla from 3-dot menu on any screen that has it
6. Lock phone orientation → screen stays portrait
7. Leave screen → sensors stop (verify via Instruments or console logs)
8. No location saved → should show error/loading state with helpful message
9. Magnetic interference → calibration banner appears
10. Cross the 0°/360° boundary → compass rotates smoothly (no 340° backward spin)
