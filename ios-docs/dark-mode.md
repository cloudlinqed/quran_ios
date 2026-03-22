# Dark Mode Implementation Guide (iOS)

Reference implementation from Android (Jetpack Compose). Adapt patterns to SwiftUI `@Environment(\.colorScheme)` and `Color` assets.

---

## 1. Architecture Overview

Two **independent** color systems coexist:

| System | Scope | Controlled by |
|--------|-------|---------------|
| **AppColors** | All app chrome: nav bars, cards, backgrounds, text, icons, buttons | `DarkModePreference` (Light / Dark / Auto) |
| **ReadingThemeColors** | Quran reader only: page background, ayah text, highlights | User-selected reading theme (Sepia, Light, Night, Paper, Ocean, Tajweed, Custom) |

They do **not** override each other. A user can have the app in dark mode while reading in Sepia, or the app in light mode with the Night reading theme.

**Smart auto-switch:** When the user explicitly selects Dark mode, the reading theme auto-switches to Night (unless already Night or Custom). When switching back to Light, if reading theme is Night it reverts to Sepia.

---

## 2. DarkModePreference

Three-state enum stored in UserDefaults (or equivalent). Default: **Auto**.

| Value | id | Arabic | English | Behavior |
|-------|----|--------|---------|----------|
| OFF | `"off"` | فاتح | Light | Always light |
| ON | `"on"` | داكن | Dark | Always dark |
| AUTO | `"auto"` | تلقائي | Auto | Follows system appearance |

### Resolution logic (in root App/Scene)

```
if preference == .on       -> dark
if preference == .off      -> light
if preference == .auto     -> system colorScheme
```

### Auto-switch reading theme logic

```
when user picks ON:
    if currentReadingTheme != .night && currentReadingTheme != .custom:
        set readingTheme = .night

when user picks OFF:
    if currentReadingTheme == .night:
        set readingTheme = .sepia

when user picks AUTO:
    do not touch readingTheme
```

### Backward compatibility

If migrating from an older `darkMode: Bool` setting:
- `darkMode == true` → `DarkModePreference.ON`
- `darkMode == false` → `DarkModePreference.AUTO` (not OFF, to give users the system-follow behavior)

### Dynamic Colors (Android 12+ / iOS equivalent)

The app has a `dynamicColors: Boolean = true` setting. On Android 12+ this uses Material You wallpaper-based colors. Default is enabled. On iOS, ignore this or map to a user accent color preference if desired.

---

## 3. AppColors — Complete Color Tables

26 tokens total. All defined in `AppColors.kt` (Android) / `AppColors.swift` (iOS).

### 3.1 Brand Greens

| Token | Light | Dark | Notes |
|-------|-------|------|-------|
| `islamicGreen` | `#2E7D32` | `#6BAF6F` | Primary accent for buttons, FABs, text accents. Readable green in dark |
| `darkGreen` | `#1B5E20` | `#8CB88E` | Body text green (surah names, headers). Readable sage in dark |
| `lightGreen` | `#4CAF50` | `#8FD094` | Bright sage highlights in dark |
| `goldAccent` | `#D4AF37` | `#A89050` | Dimmed gold in dark (Juz headers, badges — less glaring) |

### 3.2 Surfaces & Backgrounds

| Token | Light | Dark | Notes |
|-------|-------|------|-------|
| `screenBackground` | `#FDFBF7` | `#1A1A1A` | Warm cream / warm dark grey (not pure black) |
| `cardBackground` | `#FFFFFF` | `#2A2A2A` | Elevated card surfaces |
| `surfaceVariant` | `#F5F0E8` | `#333333` | Search bars, input fields, chips |
| `surfaceOverlay` | `#000000` 40% alpha | `#000000` 60% alpha | Modal/dialog scrim (`0x66000000` / `0x99000000`) |

### 3.3 Text

| Token | Light | Dark | Notes |
|-------|-------|------|-------|
| `textPrimary` | `#3E2723` | `#DDD8D3` | Coffee brown / warm off-white |
| `textSecondary` | `#A1887F` | `#9E9590` | Subtitles, hints, captions |
| `textOnPrimary` | `#FFFFFF` | `#E8E4DF` | Text on green/accent buttons |
| `textOnHeader` | `#FFFFFF` | `#E8E4DF` | Text/icons on header gradients & nav bars |

### 3.4 Borders & Dividers

| Token | Light | Dark | Notes |
|-------|-------|------|-------|
| `divider` | `#A1887F` 30% alpha | `#3D3D3D` | Section dividers |
| `border` | `#D7CCC8` | `#444444` | Card borders, outlines |

### 3.5 Interactive Elements

| Token | Light | Dark | Notes |
|-------|-------|------|-------|
| `iconDefault` | `#757575` | `#9E9590` | Default icon tint |
| `switchTrackOff` | `#BDBDBD` 40% alpha | `#555555` | Toggle off-state track |
| `chipBackground` | `#F5F5F5` | `#333333` | Unselected chip/tag fill |
| `chipSelectedBackground` | `#2E7D32` 15% alpha | `#6BAF6F` 25% alpha | Selected chip/tag fill |

### 3.6 Header / Navigation Bar

| Token | Light | Dark | Notes |
|-------|-------|------|-------|
| `topBarBackground` | `#2E7D32` | `#2C452E` | Flat nav bar color. Muted olive in dark |
| `headerGradientStart` | `#1B5E20` | `#1E2E1F` | Gradient left/top |
| `headerGradientMid` | `#2E7D32` | `#2C452E` | Gradient center |
| `headerGradientEnd` | `#4CAF50` | `#3A6B3E` | Gradient right/bottom |

### 3.7 Semantic Accent Colors

| Token | Light | Dark | Notes |
|-------|-------|------|-------|
| `teal` | `#00897B` | `#5BA8A0` | Athkar category, Daily Tracker |
| `purple` | `#7B1FA2` | `#B39DDB` | Ramadan/Imsakiya. Soft lavender in dark |
| `orange` | `#E65100` | `#E0A155` | Warm amber in dark. Used for Bookmarks |
| `blue` | `#1565C0` | `#7EAAD4` | Muted sky in dark |
| `grey` | `#757575` | `#BDBDBD` | Neutral |

---

## 4. Reading Themes — Complete Color Tables

Default theme: **Sepia**. The `isDark` property drives status bar style.

### 4.1 Sepia (default)

| Property | Value |
|----------|-------|
| `background` | `#F0EDE4` (warm Mushaf paper) |
| `surface` | `#F5F2EB` |
| `textPrimary` | `#000000` |
| `textSecondary` | `#444444` |
| `accent` | `#D6C4A6` (warm beige) |
| `accentLight` | `#E2D5BD` |
| `divider` | `#D8D4CA` |
| `cardBackground` | `#F5F2EB` |
| `highlightBackground` | `#F5EFE0` |
| `highlight` | `#BF360C` (deep orange — playing ayah) |
| `ayahMarker` | `#D6C4A6` |
| `topBarBackground` | `#D6C4A6` |
| `topBarContent` | `#000000` |
| `bottomBarBackground` | `#F0EDE4` |
| `isDark` | `false` |

### 4.2 Light

| Property | Value |
|----------|-------|
| `background` | `#FAF8F3` (cream) |
| `surface` | `#FFFFFF` |
| `textPrimary` | `#1B5E20` (dark green) |
| `textSecondary` | `#666666` |
| `accent` | `#2E7D32` (Islamic green) |
| `accentLight` | `#66BB6A` |
| `divider` | `#E0E0E0` |
| `cardBackground` | `#FFFFFF` |
| `highlightBackground` | `#FFF8E1` (light amber — contrasts with green text) |
| `highlight` | `#FF6F00` (orange — playing ayah) |
| `ayahMarker` | `#D4AF37` (gold) |
| `topBarBackground` | `#2E7D32` |
| `topBarContent` | `#FFFFFF` |
| `bottomBarBackground` | `#FFFFFF` |
| `isDark` | `false` |

### 4.3 Night

| Property | Value |
|----------|-------|
| `background` | `#1A1A1A` (warm dark grey, not pure black) |
| `surface` | `#242424` |
| `textPrimary` | `#CCC8C3` (warm off-white for comfort) |
| `textSecondary` | `#8A8580` |
| `accent` | `#4A6B4D` (dimmed muted green for header) |
| `accentLight` | `#3D5C40` |
| `divider` | `#333333` |
| `cardBackground` | `#242424` |
| `highlightBackground` | `#1B3D1E` (very dark green) |
| `highlight` | `#E6A500` (gold — playing ayah) |
| `ayahMarker` | `#CDAD00` (dimmed gold) |
| `topBarBackground` | `#242424` |
| `topBarContent` | `#CCC8C3` |
| `bottomBarBackground` | `#242424` |
| `isDark` | `true` |

### 4.4 Paper

| Property | Value |
|----------|-------|
| `background` | `#FFFDF7` (off-white paper) |
| `surface` | `#FFFEFA` |
| `textPrimary` | `#2C2C2C` (near black) |
| `textSecondary` | `#555555` |
| `accent` | `#1565C0` (blue ink) |
| `accentLight` | `#42A5F5` |
| `divider` | `#E8E8E8` |
| `cardBackground` | `#FFFEFA` |
| `highlightBackground` | `#FFF9C4` (light yellow) |
| `highlight` | `#D84315` (deep orange — playing ayah) |
| `ayahMarker` | `#1565C0` (blue) |
| `topBarBackground` | `#37474F` (blue-grey) |
| `topBarContent` | `#FFFDF7` |
| `bottomBarBackground` | `#FFFEFA` |
| `isDark` | `false` |

### 4.5 Ocean

| Property | Value |
|----------|-------|
| `background` | `#E3F2FD` (light blue) |
| `surface` | `#BBDEFB` |
| `textPrimary` | `#0D47A1` (dark blue) |
| `textSecondary` | `#1565C0` |
| `accent` | `#1976D2` |
| `accentLight` | `#64B5F6` |
| `divider` | `#90CAF9` |
| `cardBackground` | `#E3F2FD` |
| `highlightBackground` | `#B3E5FC` (light cyan) |
| `highlight` | `#FF6D00` (orange — playing ayah) |
| `ayahMarker` | `#0288D1` (blue) |
| `topBarBackground` | `#1565C0` |
| `topBarContent` | `#FFFFFF` |
| `bottomBarBackground` | `#E3F2FD` |
| `isDark` | `false` |

### 4.6 Tajweed

| Property | Value |
|----------|-------|
| `background` | `#FFFFFF` (pure white — Tajweed rule colors are the content) |
| `surface` | `#FFFFFF` |
| `textPrimary` | `#000000` (base; overridden by Tajweed rule colors per-letter) |
| `textSecondary` | `#666666` |
| `accent` | `#2E7D32` (Islamic green) |
| `accentLight` | `#66BB6A` |
| `divider` | `#E0E0E0` |
| `cardBackground` | `#FFFFFF` |
| `highlightBackground` | `#FFF8E1` |
| `highlight` | `#FF6F00` (orange — playing ayah) |
| `ayahMarker` | `#D4AF37` (gold) |
| `topBarBackground` | `#2E7D32` |
| `topBarContent` | `#FFFFFF` |
| `bottomBarBackground` | `#FFFFFF` |
| `isDark` | `false` |

### 4.7 Custom Theme

Generated dynamically from 3 user-picked colors: `backgroundColor`, `textColor`, `headerColor`.

```
surface           = backgroundColor
textSecondary     = textColor at 70% alpha
accentLight       = headerColor at 70% alpha
divider           = white 12% (if isDark) | black 12% (if light)
highlightBackground = headerColor at 10% alpha
highlight         = #FFB300 (if isDark) | #FF6F00 (if light)
ayahMarker        = headerColor
topBarBackground  = headerColor
topBarContent     = white (if headerColor is dark) | black (if light)
bottomBarBackground = backgroundColor
isDark            = backgroundColor luminance < 0.5  (formula: 0.299*R + 0.587*G + 0.114*B)
```

---

## 5. Screen-by-Screen Component Guide

### 5.1 Navigation Bars (all screens)

Every screen's navigation bar uses `topBarBackground`.

| Property | Token |
|----------|-------|
| Bar background | `topBarBackground` |
| Title text | `textOnHeader` |
| Back/nav icon tint | `textOnHeader` |
| Action icon tint | `textOnHeader` |

**Screens with nav bars:**
AboutScreen, AthkarCategoriesScreen, AthkarListScreen, BookmarksScreen, DownloadsScreen, PlayerScreen, AthanSettingsScreen, PrayerTimesScreen, QuranIndexScreen (+ TabRow), RecitersScreen, SurahsScreen, TrackerScreen, **QiblaScreen**

**Exception:** ImskaiyaScreen uses its own Ramadan-themed purple (`#1A1A2E`) — left unchanged.

### 5.2 Scaffold / Screen Background

| Property | Token |
|----------|-------|
| Screen background | `screenBackground` |

### 5.3 Header Gradients (Home, Settings)

Used for hero/header sections with vertical or horizontal gradients.

```
gradient: headerGradientStart → headerGradientMid → headerGradientEnd
text on gradient: textOnHeader
icons on gradient: textOnHeader
```

### 5.4 Cards

| Property | Token |
|----------|-------|
| Card background | `cardBackground` |
| Card border | `border` |
| Card title text | `textPrimary` |
| Card subtitle/body | `textSecondary` |
| Card elevation shadow | Platform default |

### 5.5 Feature Cards (Home Screen)

The Home screen has 6 colored feature cards. Each receives a `cardColor` from AppColors:

| Feature | `cardColor` token |
|---------|------------------|
| Prayer Times | `textPrimary` |
| Ramadan Imsakiya | `purple` |
| Athkar | `teal` |
| Daily Tracker | `teal` |
| Bookmarks | `orange` |
| Downloads | `lightGreen` |

Each card uses `cardColor` for:
- **Card background**: horizontal gradient `cardColor` 15% → `cardColor` 5%
- **Card background container**: `cardBackground` (the Card composable itself)
- **Icon circle background**: vertical gradient `cardColor` 90% → `cardColor` 100%
- **Icon tint**: `textOnPrimary`
- **Title text**: `cardColor` at 90% alpha
- **Elevation shadow**: `cardColor` at 30% alpha (ambient + spot)

Since all `cardColor` values come from `AppTheme.colors`, they are automatically theme-aware.

### 5.6 Buttons

| Component | Background | Text |
|-----------|------------|------|
| Primary button | `islamicGreen` | `textOnPrimary` |
| FAB | `islamicGreen` | `textOnPrimary` |
| Outlined button | transparent / `border` | `islamicGreen` |

Buttons keep `islamicGreen` (not `topBarBackground`). Only nav bars use the muted olive.

### 5.7 Filter Chips / Tags

| State | Background | Text | Border |
|-------|------------|------|--------|
| Unselected | `chipBackground` | `textPrimary` | `border` |
| Selected | `chipSelectedBackground` | `islamicGreen` | `islamicGreen` |

### 5.8 Text Fields / Search Bars

| Property | Token |
|----------|-------|
| Background | `surfaceVariant` |
| Text | `textPrimary` |
| Placeholder | `textSecondary` |
| Border | `border` |

### 5.9 Toggles / Switches

| State | Track | Thumb |
|-------|-------|-------|
| Off | `switchTrackOff` | Platform default |
| On | `islamicGreen` | Platform default |

### 5.10 Dropdown Menus

| Property | Token |
|----------|-------|
| Menu background | `cardBackground` |
| Selected item background | `chipSelectedBackground` |
| Text color | `textPrimary` |
| Selected text | `islamicGreen` |

### 5.11 Dialogs / Modals

| Property | Token |
|----------|-------|
| Scrim overlay | `surfaceOverlay` |
| Dialog background | `cardBackground` |
| Title text | `textPrimary` |
| Body text | `textSecondary` |
| Action button | `islamicGreen` |

### 5.12 Dividers

| Property | Token |
|----------|-------|
| Divider line | `divider` |

### 5.13 Icons

| Context | Token |
|---------|-------|
| Default/standalone | `iconDefault` |
| On header/nav bar | `textOnHeader` |
| On primary button | `textOnPrimary` |
| Accent icons | `islamicGreen` or semantic color |

---

## 6. Settings UI — Appearance Selector

Replace any dark mode toggle with a 3-option chip selector.

**Label:** "المظهر" (Arabic) / "Appearance" (English)
**Icon:** Moon/DarkMode icon, tinted `islamicGreen`

**Three chips in a horizontal row:**

| Chip | Label (AR) | Label (EN) |
|------|-----------|------------|
| Light | فاتح | Light |
| Dark | داكن | Dark |
| Auto | تلقائي | Auto |

Selected chip shows a checkmark leading icon.

---

## 7. Screens NOT Affected by Dark Mode

These use their own independent color systems:

| Screen | Color System | Reason |
|--------|-------------|--------|
| QuranReaderScreen | `ReadingThemeColors` | Has its own 7 reading themes |
| SVGPageComposable | `ReadingThemeColors` | Quran SVG rendering |
| QCFPageComposable | `ReadingThemeColors` | QCF font rendering |
| ColorPickerDialog | Hardcoded palette swatches | Color picker for custom theme — palette shouldn't change |
| ImskaiyaScreen | Hardcoded Ramadan theme (`#1A1A2E` purple) | Temporary Ramadan screen, always dark |

---

## 8. Design Principles

1. **Warm, not cold.** Dark mode uses warm dark grey (`#1A1A1A`) not pure black. Text is warm off-white (`#DDD8D3`) not blinding white. This creates a comfortable, paper-like feel that matches the Islamic aesthetic.

2. **Readable greens, not murky.** Greens shift from vibrant (`#2E7D32`) to readable sage (`#6BAF6F` / `#8CB88E`). These are bright enough to read comfortably on dark backgrounds without straining the eyes. Nav bars use a separate, darker olive (`#2C452E`) for visual hierarchy.

3. **Elevated surfaces.** Cards (`#2A2A2A`) are slightly lighter than the background (`#1A1A1A`) to maintain visual hierarchy without borders being the only differentiator.

4. **Gold dims, not glares.** The gold accent (`#D4AF37` → `#A89050`) becomes warmer and less bright so Juz badges and highlights don't pop aggressively on dark backgrounds.

5. **Semantic colors adapt.** Purple, teal, orange, blue all shift to softer pastel variants in dark mode. This maintains visual identity while being comfortable on dark surfaces.

6. **Two systems stay independent.** App dark mode and Quran reader themes are separate. A user in light mode can still use the Night reading theme. The auto-switch is a convenience, not a coupling.

7. **Buttons stay branded.** `islamicGreen` is used for all interactive elements (buttons, FABs, toggles). Only the nav bar/header shifts to `topBarBackground` olive to avoid the bright green header clashing with the dark UI.

---

## 9. SwiftUI Implementation Notes

### Color Asset Catalogs

Define all 26 tokens in an Asset Catalog with `Any Appearance` and `Dark Appearance` variants, or use a `ColorScheme`-aware struct:

```swift
struct AppColors {
    // Brand greens
    let islamicGreen: Color
    let darkGreen: Color
    let lightGreen: Color
    let goldAccent: Color
    // Surfaces & backgrounds
    let screenBackground: Color
    let cardBackground: Color
    let surfaceVariant: Color
    let surfaceOverlay: Color
    // Text
    let textPrimary: Color
    let textSecondary: Color
    let textOnPrimary: Color
    let textOnHeader: Color
    // Borders & dividers
    let divider: Color
    let border: Color
    // Interactive
    let iconDefault: Color
    let switchTrackOff: Color
    let chipBackground: Color
    let chipSelectedBackground: Color
    // Header / TopAppBar
    let topBarBackground: Color
    let headerGradientStart: Color
    let headerGradientMid: Color
    let headerGradientEnd: Color
    // Semantic accents
    let teal: Color
    let purple: Color
    let orange: Color
    let blue: Color
    let grey: Color

    static let light = AppColors(
        islamicGreen:           Color(hex: 0x2E7D32),
        darkGreen:              Color(hex: 0x1B5E20),
        lightGreen:             Color(hex: 0x4CAF50),
        goldAccent:             Color(hex: 0xD4AF37),
        screenBackground:       Color(hex: 0xFDFBF7),
        cardBackground:         Color(hex: 0xFFFFFF),
        surfaceVariant:         Color(hex: 0xF5F0E8),
        surfaceOverlay:         Color(hex: 0x000000).opacity(0.40),
        textPrimary:            Color(hex: 0x3E2723),
        textSecondary:          Color(hex: 0xA1887F),
        textOnPrimary:          Color(hex: 0xFFFFFF),
        textOnHeader:           Color(hex: 0xFFFFFF),
        divider:                Color(hex: 0xA1887F).opacity(0.30),
        border:                 Color(hex: 0xD7CCC8),
        iconDefault:            Color(hex: 0x757575),
        switchTrackOff:         Color(hex: 0xBDBDBD).opacity(0.40),
        chipBackground:         Color(hex: 0xF5F5F5),
        chipSelectedBackground: Color(hex: 0x2E7D32).opacity(0.15),
        topBarBackground:       Color(hex: 0x2E7D32),
        headerGradientStart:    Color(hex: 0x1B5E20),
        headerGradientMid:      Color(hex: 0x2E7D32),
        headerGradientEnd:      Color(hex: 0x4CAF50),
        teal:                   Color(hex: 0x00897B),
        purple:                 Color(hex: 0x7B1FA2),
        orange:                 Color(hex: 0xE65100),
        blue:                   Color(hex: 0x1565C0),
        grey:                   Color(hex: 0x757575)
    )

    static let dark = AppColors(
        islamicGreen:           Color(hex: 0x6BAF6F),
        darkGreen:              Color(hex: 0x8CB88E),
        lightGreen:             Color(hex: 0x8FD094),
        goldAccent:             Color(hex: 0xA89050),
        screenBackground:       Color(hex: 0x1A1A1A),
        cardBackground:         Color(hex: 0x2A2A2A),
        surfaceVariant:         Color(hex: 0x333333),
        surfaceOverlay:         Color(hex: 0x000000).opacity(0.60),
        textPrimary:            Color(hex: 0xDDD8D3),
        textSecondary:          Color(hex: 0x9E9590),
        textOnPrimary:          Color(hex: 0xE8E4DF),
        textOnHeader:           Color(hex: 0xE8E4DF),
        divider:                Color(hex: 0x3D3D3D),
        border:                 Color(hex: 0x444444),
        iconDefault:            Color(hex: 0x9E9590),
        switchTrackOff:         Color(hex: 0x555555),
        chipBackground:         Color(hex: 0x333333),
        chipSelectedBackground: Color(hex: 0x6BAF6F).opacity(0.25),
        topBarBackground:       Color(hex: 0x2C452E),
        headerGradientStart:    Color(hex: 0x1E2E1F),
        headerGradientMid:      Color(hex: 0x2C452E),
        headerGradientEnd:      Color(hex: 0x3A6B3E),
        teal:                   Color(hex: 0x5BA8A0),
        purple:                 Color(hex: 0xB39DDB),
        orange:                 Color(hex: 0xE0A155),
        blue:                   Color(hex: 0x7EAAD4),
        grey:                   Color(hex: 0xBDBDBD)
    )
}
```

### Environment-Based Theming

```swift
@main
struct AlFurqanApp: App {
    @AppStorage("darkModePreference") var preference: String = "auto"
    @Environment(\.colorScheme) var systemScheme

    var resolvedScheme: ColorScheme {
        switch preference {
        case "on": return .dark
        case "off": return .light
        default: return systemScheme
        }
    }

    var body: some Scene {
        WindowGroup {
            ContentView()
                .preferredColorScheme(
                    preference == "auto" ? nil : (preference == "on" ? .dark : .light)
                )
                .environmentObject(
                    ThemeManager(colors: resolvedScheme == .dark ? .dark : .light)
                )
        }
    }
}
```

### Status Bar

In dark mode, use light status bar content. In light mode with green nav bar, also use light status bar content (white text on green). The `textOnHeader` token is light in both modes for this reason.
